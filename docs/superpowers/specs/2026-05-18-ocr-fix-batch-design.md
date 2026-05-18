# OCR 修复 & 多图批量识别设计

## 问题

- App 选图后 OCR 总是失败，提示"未识别到有效表格数据"
- 只能单张选图，不支持一次上传多张截图

## 根因

Google ML Kit `ChineseTextRecognizer` 识别同花顺截图时，**不保留表格列之间的空格/制表符**，输出文本缺失列分隔。`TableAnalyzer` 依赖 `split(Regex("\\s+"))` 按空白分列，当整行文本连成一片时 `cells.size` < 2，导致表头检测失败或股票代码无法解析。

## 方案 B：ML Kit + 百度 OCR API 混合路由

### OCR 引擎三层路由

```
OcrEngine.recognizeText(bitmap)
  Layer 1: ML Kit 结构化识别
    - 用 Text.getTextBlocks() 获取每个 block/line
    - 用 bounding box X 坐标聚类 → 推断列对齐
    - 成功 → 返回结构化文本
    - 无结果 → Layer 2
  Layer 2: ImagePreprocessor 增强后重试 ML Kit
    - 灰度化 + 对比度拉升
    - 二值化 (大津法 OTSU)
    - 重试 ML Kit → 成功? → 返回文本
    - 仍失败 → Layer 3
  Layer 3: 百度 OCR API
    - OkHttp POST base64 图片到 api.bdocr.com
    - 通用文字识别接口 (500次/天免费)
    - 返回 JSON → 解析为结构化文本
```

### TableAnalyzer 增强

- 新增基于 ML Kit `Text.Line.getElementText()` + `boundingBox` 的列对齐解析器
- 保留原有空格分隔解析器作为兜底
- 自动检测哪种解析器效果更好

### 多图批量上传

- `ScanScreen` 用 `GetMultipleContents` 替代 `GetContent`
- `ScanViewModel` 新增 `processImages(List<Bitmap>)` 批量处理
- 每张图独立走三层路由
- 识别结果合并去重（同股票代码取最新）
- 预览页显示每张图来源标记

### 新增/修改文件

| 文件 | 变更 |
|------|------|
| `OcrEngine.kt` | 重写：三层路由，结构化 block 解析，百度 API 调用 |
| `ImagePreprocessor.kt` | 实现灰度、OTSU 二值化、对比度拉升 |
| `TableAnalyzer.kt` | 新增 ML Kit bounding box 列对齐解析 |
| `ScanScreen.kt` | GetContent → GetMultipleContents |
| `ScanViewModel.kt` | 新增批量处理、合并去重 |
| `build.gradle.kts` | 添加 okhttp3 依赖 |
| `AndroidManifest.xml` | 添加 INTERNET 权限 |
| `ScanfolioApp.kt` | 可选：注入 OkHttpClient |

### 百度 OCR 配置

- 注册地址：https://console.bce.baidu.com/ai/#/ai/ocr/overview/index
- 免费额度：通用文字识别 500 次/天
- API Key 需用户在设置页面填写
- `SettingsScreen` 新增 API Key 配置

### 边界情况

- **ML Kit 部分成功、部分失败**：成功的用 ML Kit 结果，失败的走 API 回退
- **多图同股票**：以最后一次识别的数据为准
- **无网络时 API 回退不可用**：显示提示，仅用 ML Kit 结果
- **API Key 未配置**：跳过 API 层，仅用 ML Kit
