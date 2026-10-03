package com.cityplatform.collector.controller;

import com.cityplatform.collector.source.DataCategory;
import com.cityplatform.collector.source.EnvironmentCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境数据源管理接口
 * 提供数据源列表查询与手动触发采集能力
 */
@RestController
@RequestMapping("/api/collector/sources")
public class EnvironmentSourceController {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentSourceController.class);

    private final EnvironmentCollectionService collectionService;

    public EnvironmentSourceController(EnvironmentCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * 查询所有环境数据源及运行状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listSources() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", OffsetDateTime.now().toString());
        result.put("sources", collectionService.getSourceInfoList());
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发环境数据采集
     * category: weather | airquality | all（默认all）
     */
    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> triggerFetch(
            @RequestParam(required = false, defaultValue = "all") String category) {
        log.info("手动触发环境数据采集: category={}", category);

        List<EnvironmentCollectionService.CollectionResult> results = new ArrayList<>();
        if ("all".equalsIgnoreCase(category)) {
            results.addAll(collectionService.collectAll());
        } else {
            DataCategory dataCategory = DataCategory.fromCode(category);
            if (dataCategory == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "无效的category参数，可选值: weather | airquality | all");
                error.put("timestamp", OffsetDateTime.now().toString());
                return ResponseEntity.badRequest().body(error);
            }
            results.add(collectionService.collectCategory(dataCategory));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("timestamp", OffsetDateTime.now().toString());
        result.put("results", results);
        return ResponseEntity.ok(result);
    }
}
