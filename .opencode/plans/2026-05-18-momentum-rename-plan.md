# Momentum App Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename Scanfolio to Momentum across all user-visible names, class names, and theme names (package `com.scanfolio` stays unchanged).

**Architecture:** One task, 15 file modifications. No functional changes — display names and class identifiers only.

**Tech Stack:** Android / Kotlin / Gradle

---

### Task 1: Rename Scanfolio → Momentum across all files

**Files:**
- Modify: `settings.gradle.kts:16`
- Modify: `app/src/main/AndroidManifest.xml:12-20`
- Modify: `app/src/main/res/values/themes.xml:3`
- Rename: `app/src/main/java/com/scanfolio/ScanfolioApp.kt` → `MomentumApp.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/theme/Theme.kt:63`
- Modify: `app/src/main/java/com/scanfolio/MainActivity.kt:10,16`
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt:46`
- Modify: `app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt:154`
- Modify: `app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt:11,19`
- Modify: `app/src/main/java/com/scanfolio/ui/settings/SettingsViewModel.kt:6,13`
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt:6,12`
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/StockDetailScreen.kt:22,32`
- Modify: `app/src/main/java/com/scanfolio/ui/analysis/AnalysisViewModel.kt:6,27`
- Modify: `app/src/test/java/com/scanfolio/ui/scan/ScanViewModelTest.kt:7`
- (未创建的文件不需要修改 — 创建时直接用 MomentumApp)

- [ ] **Step 1: Rename ScanfolioApp.kt → MomentumApp.kt + update class**

```bash
git mv app/src/main/java/com/scanfolio/ScanfolioApp.kt app/src/main/java/com/scanfolio/MomentumApp.kt
```

Edit:
```
class ScanfolioApp → class MomentumApp
```

- [ ] **Step 2: Update settings.gradle.kts**

```
rootProject.name = "Scanfolio" → rootProject.name = "Momentum"
```

- [ ] **Step 3: Update AndroidManifest.xml**

```
android:label="Scanfolio" → android:label="Momentum"
android:name=".ScanfolioApp" → android:name=".MomentumApp"
android:theme="@style/Theme.Scanfolio" → android:theme="@style/Theme.Momentum"
```

- [ ] **Step 4: Update themes.xml**

```
<style name="Theme.Scanfolio" → <style name="Theme.Momentum"
```

- [ ] **Step 5: Update Theme.kt**

```
fun ScanfolioTheme( → fun MomentumTheme(
```

- [ ] **Step 6: Update MainActivity.kt**

```
import com.scanfolio.ui.theme.ScanfolioTheme → import com.scanfolio.ui.theme.MomentumTheme
ScanfolioTheme { → MomentumTheme {
```

- [ ] **Step 7: Update PortfolioScreen.kt — title text**

```
title = { Text("Scanfolio" → title = { Text("Momentum"
```

- [ ] **Step 8: Update SettingsScreen.kt — clipboard label**

```
ClipData.newPlainText("Scanfolio" → ClipData.newPlainText("Momentum"
```

- [ ] **Step 9: Update ScanViewModel.kt — import + cast**

```
import com.scanfolio.ScanfolioApp → import com.scanfolio.MomentumApp
val app = application as ScanfolioApp → val app = application as MomentumApp
```

- [ ] **Step 10: Update SettingsViewModel.kt — import + cast**

```
import com.scanfolio.ScanfolioApp → import com.scanfolio.MomentumApp
val app = application as ScanfolioApp → val app = application as MomentumApp
```

- [ ] **Step 11: Update PortfolioViewModel.kt — import + cast**

```
import com.scanfolio.ScanfolioApp → import com.scanfolio.MomentumApp
val app = application as ScanfolioApp → val app = application as MomentumApp
```

- [ ] **Step 12: Update StockDetailScreen.kt — import + cast**

```
import com.scanfolio.ScanfolioApp → import com.scanfolio.MomentumApp
val app = application as ScanfolioApp → val app = application as MomentumApp
```

- [ ] **Step 13: Update AnalysisViewModel.kt — import + cast**

```
import com.scanfolio.ScanfolioApp → import com.scanfolio.MomentumApp
val app = application as ScanfolioApp → val app = application as MomentumApp
```

- [ ] **Step 14: Update ScanViewModelTest.kt — import**

```
import com.scanfolio.ScanfolioApp → import com.scanfolio.MomentumApp
```

- [ ] **Step 15: Verify build**

```bash
cd D:\workspace\Scanfolio; .\gradlew.bat assembleDebug 2>&1
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 16: Commit**

```bash
git add -A
git commit -m "rename: Scanfolio → Momentum (display name, class names, theme)"
```
