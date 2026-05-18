# Momentum App Rename Design

## 背景

App 原名为 Scanfolio（Scan + Portfolio），最初设计为 OCR 扫描同花顺截图的股票追踪工具。V2 已弃用 OCR，改为股票代码搜索 + 免费 API 数据获取。原名不再合适，需要更名。

## 新名称

**Momentum** — 股市技术分析核心概念"动量/势头"，寓意涨势延续、稳健向上。

## 改动范围

### 不改的
- 包名 `com.scanfolio`（保持 Android applicationId，避免 Play Store / 签名问题）
- 目录结构 `app/src/main/java/com/scanfolio/`
- Gradle module 结构

### 改动的

| 文件 | 改动内容 |
|------|----------|
| `settings.gradle.kts` | `rootProject.name = "Momentum"` |
| `app/src/main/AndroidManifest.xml` | `android:label="Momentum"`, `Theme.Momentum` |
| `app/src/main/res/values/themes.xml` | `<style name="Theme.Momentum">` |
| `app/src/main/java/com/scanfolio/ui/theme/Theme.kt` | `fun MomentumTheme(...)` |
| `app/src/main/java/com/scanfolio/ScanfolioApp.kt` → `MomentumApp.kt` | 类名 `MomentumApp` |
| `app/src/main/java/com/scanfolio/MainActivity.kt` | 引用 `MomentumTheme` |
| `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt` | title 文字 "Momentum" |
| `app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt` | clipboard label "Momentum" |
| `app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt` | import + cast `MomentumApp` |
| `app/src/main/java/com/scanfolio/ui/settings/SettingsViewModel.kt` | import + cast `MomentumApp` |
| `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt` | import + cast `MomentumApp` |
| `app/src/main/java/com/scanfolio/ui/portfolio/StockDetailScreen.kt` | import + cast `MomentumApp` |
| `app/src/main/java/com/scanfolio/ui/analysis/AnalysisViewModel.kt` | import + cast `MomentumApp` |
| `app/src/test/java/com/scanfolio/ui/scan/ScanViewModelTest.kt` | import `MomentumApp` |
| 未来文件（PnLViewModel.kt 等） | import + cast `MomentumApp` |

## 操作步骤

1. Rename `ScanfolioApp.kt` → `MomentumApp.kt` (git mv)
2. 逐个文件更新 import/引用
3. 更新 themes/themes.xml
4. 更新 AndroidManifest.xml
5. 更新 settings.gradle.kts
6. 验证编译：`.\gradlew.bat assembleDebug`

## 风险与注意事项

- 不要全局替换 `Scanfolio` → `Momentum`（包名 `com.scanfolio` 保持不变）
- 每个文件手动确认修改，避免误改
- 改完后运行 assembleDebug 确认编译通过
