// ============================================================
// ECharts 按需引入：统一注册本项目用到的图表与组件
// 替代 `import * as echarts from 'echarts'` 全量引入，减小打包体积
// ============================================================
import * as echarts from 'echarts/core';
import {
  BarChart,
  GaugeChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
} from 'echarts/charts';
import {
  AxisPointerComponent,
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  RadarComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import type {
  BarSeriesOption,
  GaugeSeriesOption,
  HeatmapSeriesOption,
  LineSeriesOption,
  PieSeriesOption,
  RadarSeriesOption,
  ScatterSeriesOption,
} from 'echarts/charts';
import type { ComposeOption } from 'echarts/core';

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  RadarChart,
  ScatterChart,
  GaugeChart,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  MarkLineComponent,
  VisualMapComponent,
  RadarComponent,
  AxisPointerComponent,
  LabelLayout,
  CanvasRenderer,
]);

// 本项目全部图表 series 的联合类型
export type SeriesOption =
  | BarSeriesOption
  | GaugeSeriesOption
  | HeatmapSeriesOption
  | LineSeriesOption
  | PieSeriesOption
  | RadarSeriesOption
  | ScatterSeriesOption;

// 与全量包 EChartsOption 对齐的按需类型（含各组件 option）
export type EChartsOption = ComposeOption<
  | SeriesOption
  | import('echarts/components').TitleComponentOption
  | import('echarts/components').TooltipComponentOption
  | import('echarts/components').GridComponentOption
  | import('echarts/components').LegendComponentOption
  | import('echarts/components').DataZoomComponentOption
  | import('echarts/components').MarkLineComponentOption
  | import('echarts/components').VisualMapComponentOption
  | import('echarts/components').RadarComponentOption
  | import('echarts/components').AxisPointerComponentOption
>;

// 透传 echarts/core 的 init/use/graphic/ECharts 等导出，保持原有调用方式不变
export * from 'echarts/core';
