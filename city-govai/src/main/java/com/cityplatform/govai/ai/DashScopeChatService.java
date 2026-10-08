package com.cityplatform.govai.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.cityplatform.govai.config.DashScopeProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 阿里云百炼（DashScope）对话服务
 * 封装通义千问大模型的非流式文本生成调用，支持超时控制
 * 调用失败时抛出异常，由上层业务降级为本地模板生成
 */
@Service
@Slf4j
public class DashScopeChatService {

    /**
     * 对话轮次（与持久化的消息角色对应：user / assistant）
     */
    public record DialogueTurn(String role, String content) {}

    /** 单条历史消息注入提示词的最大长度，防止上下文膨胀 */
    private static final int HISTORY_CONTENT_MAX = 500;

    @Resource
    private DashScopeProperties properties;

    /** 单线程执行器：串行化模型调用，超时后可安全取消任务 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "dashscope-call");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 云端大模型是否可用（已启用且配置了API Key）
     */
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    /**
     * 当前AI提供方标识（用于状态接口对外展示）
     */
    public String getProviderName() {
        return isAvailable() ? "dashscope(" + properties.getModel() + ")" : "template-fallback";
    }

    /**
     * 调用大模型生成响应（单轮，阻塞，受超时时间控制）
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @param userPrompt   用户提示词（问题与上下文）
     * @return 模型生成内容
     * @throws Exception 调用失败或超时时抛出，由调用方决定降级策略
     */
    public String chat(String systemPrompt, String userPrompt) throws Exception {
        return chat(systemPrompt, null, userPrompt);
    }

    /**
     * 调用大模型生成响应（多轮对话，阻塞，受超时时间控制）
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @param history      历史对话轮次（user/assistant），可为 null
     * @param userPrompt   当前用户提示词（问题与上下文）
     * @return 模型生成内容
     * @throws Exception 调用失败或超时时抛出，由调用方决定降级策略
     */
    public String chat(String systemPrompt, List<DialogueTurn> history, String userPrompt) throws Exception {
        Future<String> future = executor.submit(() -> callGeneration(systemPrompt, history, userPrompt));
        try {
            return future.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("DashScope调用超时(" + properties.getTimeoutSeconds() + "s)", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw new IllegalStateException("DashScope调用失败", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * 实际执行DashScope非流式生成调用（系统提示词 + 历史轮次 + 当前问题）
     * SDK的call方法抛出受检异常（ApiException/NoApiKeyException等），统一上抛由调用方降级
     */
    private String callGeneration(String systemPrompt, List<DialogueTurn> history, String userPrompt) throws Exception {
        Generation generation = new Generation();
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemPrompt)
                .build());
        if (history != null) {
            for (DialogueTurn turn : history) {
                if (turn.content() == null || turn.content().isBlank()) {
                    continue;
                }
                String content = turn.content();
                if (content.length() > HISTORY_CONTENT_MAX) {
                    content = content.substring(0, HISTORY_CONTENT_MAX) + "…";
                }
                messages.add(Message.builder()
                        .role(turn.role())
                        .content(content)
                        .build());
            }
        }
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(userPrompt)
                .build());

        GenerationParam param = GenerationParam.builder()
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        if (result.getOutput() == null || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            throw new IllegalStateException("DashScope返回内容为空");
        }

        String content = result.getOutput().getChoices().get(0).getMessage().getContent();
        log.info("DashScope生成完成: model={}, 内容长度={}", properties.getModel(),
                content != null ? content.length() : 0);
        return content;
    }
}
