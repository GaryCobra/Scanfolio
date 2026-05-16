# Scanfolio MVP & V2 Feature Design

## Overview

Scanfolio is an Android app that uses OCR to scan 同花顺 (Tonghuashun) stock portfolio screenshots, recognize stock data, and help users track and analyze their trades. This document covers the essential missing features identified during product review, as well as a full UI redesign inspired by 腾讯理财通 (v2.8) design language.

## UI Redesign (Phase 0 — Design Language Refresh)

### Color Scheme

Reference: 腾讯理财通 v2.8 color palette.

**Light Mode:**

| Token | Current Value | New Value | Usage |
|-------|--------------|-----------|-------|
| Primary | `#1565C0` | `#3a7bd1` | App bars, buttons, links |
| Primary Container | `#D1E4FF` | `#d8e9fb` | Selected states, light highlights |
| Surface / Background | `#F5F5F5` | `#f4f7fb` | Page backgrounds |
| Surface Variant | `#FFFFFF` | `#FFFFFF` | Card backgrounds |
| Secondary | `#546E7A` | `#7380a9` | Less prominent UI elements |
| Secondary Container | `#CFD8DC` | `#e8ecf4` | Secondary element backgrounds |
| Tertiary | — | `#a1a7d9` | Accent highlights, decorative elements |
| UpRed | `#E53935` | `#E53935` | Stock gain |
| DownGreen | `#4CAF50` | `#4CAF50` | Stock loss |

**Dark Mode:**

| Token | New Value |
|-------|-----------|
| Background | `#0d0d11` |
| Surface / Card | `#1a1a1e` |
| Primary | `#83b6ed` |
| On Background | `#f6f0ea` |
| Accent | `#c2a48a` |

### Layout Principles

- **Increased whitespace**: Card spacing 12dp → 16dp, inner padding 16dp → 20dp
- **Card radius**: 12dp → 16dp for more modern feel
- **Typography hierarchy**: Clearer distinction between titles, values, and labels
- **Light backgrounds on cards**: Use `#f4f7fb` for info cards, white for action cards
- **Professional financial tone**: Blue primary with subtle purple/lavender accents

### Page Layout Changes

#### Portfolio Screen (持仓首页)
```
[TopAppBar: Scanfolio]        [⚙️]
[Dashboard Card]
  ┌──────────────────────────────┐
  │ +¥12,350   68%胜率  23只    │
  └──────────────────────────────┘
[🔍 Search...           Sort ▾]
[Stock List Cards (16dp radius)]
```

#### Stock Detail Screen (股票详情)
```
[← 000001 平安银行           ✚]
[OCR Data Card — light blue bg]
  ┌──────────────────────────────┐
  │ 最新价: 42.50  ✎             │
  │ 涨跌幅: +2.3%  ✎             │
  └──────────────────────────────┘
[Trade Records — long-press menu]
  ┌──────────────────────────────┐
  │ 买入 01-15  ¥42  获利 +15%  │
  │ 爆量  ✅成功  长按编辑/删除  │
  └──────────────────────────────┘
```

#### Scan Preview Screen (扫描预览 — 新增)
```
[TopAppBar: 预览识别结果]
[Editable table view]
  ┌──────┬──────────┬───────────┐
  │ 代码 │ 名称     │ 最新价    │  ← header bg #d8e9fb
  ├──────┼──────────┼───────────┤
  │000001│ 平安银行  │ 42.50 ✎  │  ← tap to edit
  │000002│ 万科A    │ 14.30 ✎  │  ← swipe to delete
  └──────┴──────────┴───────────┘
[确认导入]  [取消]
```

### Component Changes

| Component | Change |
|-----------|--------|
| `Theme.kt` | Full color palette replacement |
| `PortfolioScreen.kt` | Add DashboardCard + SearchSortBar at top |
| `PortfolioViewModel.kt` | Add search/sort/dashboard state and logic |
| `StockDetailScreen.kt` | Text fields become click-to-edit; trade list supports long-press |
| `StockDetailViewModel.kt` | Add `updateDataColumn()`, `updateTrade()`, `deleteTrade()` |
| `TradeFormSheet.kt` | Accept optional `TradeRecordEntity` for edit pre-fill |
| `ScanScreen.kt` | After OCR → navigate to PreviewScreen |
| `ScanViewModel.kt` | Add preview state, row edit/deletion methods |
| `AppNavigation.kt` | Add `preview`, `manual_stock_entry` routes |

### New Components

| File | Description |
|------|-------------|
| `ui/portfolio/DashboardCard.kt` | Aggregate P&L summary card |
| `ui/portfolio/SearchSortBar.kt` | Search field + sort dropdown |
| `ui/scan/PreviewScreen.kt` | Editable OCR result preview |
| `ui/portfolio/ManualStockEntryScreen.kt` | Manual stock add form |
| `ui/portfolio/EditDataCellDialog.kt` | Inline cell edit dialog |

## MVP Features (Phase 1 — Core UX Completion)

### 1. Scan Preview & Confirmation Flow

**Problem:** OCR scan → direct import with no user preview. Users cannot see or correct what was recognized before data is saved.

**Solution:**
- After OCR completes, show a full table preview (headers + rows) with the recognized data
- Allow inline editing of cell values before confirming import
- Allow deleting individual rows before import
- Only on user "Confirm" button press, save data to DB

**UI Flow:**
1. User selects image → loading spinner during OCR
2. Transition to **PreviewScreen** showing editable table
3. User can tap any cell to edit the text value
4. User can swipe-to-delete or tap delete icon on any row
5. "Confirm Import" button saves all data and navigates to Portfolio
6. "Cancel" returns to scan screen

**Components:**
- `PreviewScreen.kt` — new composable screen
- `ScanViewModel` — add preview state, add/remove row methods, cell edit methods
- `PreviewRowItem` — individual editable row component
- Navigation: add `preview` route to `AppNavigation.kt`

### 2. Stock Data Manual Editing

**Problem:** OCR data is write-only. Users cannot fix misrecognized values or add stocks manually.

**Solution:**
- **Edit mode** on StockDetailScreen: tap any data cell to edit its value inline
- **Re-scan** button to re-OCR the same stock from a new screenshot
- **Manual add stock** button on PortfolioScreen: enters stock code + name, then allows manual data entry

**Components:**
- `StockDetailViewModel` — add `updateDataColumn(name, value)` method
- `PortfolioViewModel` — add `addStockManually(code, name)` method
- `ManualStockEntryScreen.kt` — new simple screen for code/name/data entry
- Navigation: add `manual_stock_entry` route

### 3. Trade Record Edit & Delete

**Problem:** Trade records are create-only. Users cannot correct mistakes or remove records.

**Solution:**
- Long-press on a trade record → show context menu with "Edit" / "Delete"
- **Edit:** Reuse `TradeFormSheet` pre-populated with existing values
- **Delete:** Show confirmation dialog, then delete from DB via cascade
- Pull-to-refresh on trade list to reload

**Changes:**
- `StockDetailViewModel` — add `updateTrade(trade)` and `deleteTrade(trade)` methods
- `TradeFormSheet` — accept optional `TradeRecordEntity` for edit mode
- `StockDetailScreen` — add long-press handler on `TradeListItem`

### 4. Profit/Loss Dashboard

**Problem:** No overall P&L summary. Users see individual trades but not the big picture.

**Solution:**
- Add a **Dashboard card** at the top of PortfolioScreen (collapsible)
- Summary metrics cards:
  - **Total P&L:** sum of all `profitAmount` where `sellTime != null`
  - **Win Rate:** `successCount / totalClosedTrades * 100%`
  - **Avg Win / Avg Loss:** average positive and negative `profitRatio`
  - **Open Positions:** count of stocks with no sell records
  - **Virtual Capital Remaining:** if virtual mode tracks capital

**Components:**
- `PortfolioViewModel` — add `dashboardStats: StateFlow<DashboardStats>` computed from trades
- `DashboardCard` composable in PortfolioScreen
- `DashboardStats` data class

### 5. Search & Sort Portfolio

**Problem:** Stock list has no way to find specific stocks or sort by criteria.

**Solution:**
- **Search bar** at top of PortfolioScreen with debounced text input
- **Sort dropdown** with options: by name (A-Z), by name (Z-A), by code, by last scanned (newest first), by change % (if available)
- **Filter chips**: show/hide based on column data (e.g., filter by strategy, filter by virtual/real)

**Components:**
- `PortfolioViewModel` — add search/filter/sort state and logic
- `SearchBar` composable
- `SortDropdown` composable

## V2 Features (Phase 2 — Enhancement)

### 6. Data Visualization with Charts

**Problem:** Analysis tab is pure numbers. Visual patterns are hard to spot.

**Solution:**
- Use the existing `MPAndroidChart` dependency
- **P&L distribution histogram** — distribution of profit ratios
- **Win rate trend** — cumulative win rate over time
- **Success/failure bar chart comparison** per column
- **Market comparison scatter plot** — stock change vs index change

### 7. Portfolio Summary Dashboard on Home

**Problem:** Home tab (Portfolio) only lists stocks; no aggregate view.

**Solution:**
- Move the P&L dashboard (MVP item 4) to be always-visible at the top
- Add per-stock mini-chart (sparkline) if we have historical data
- Group stocks by market (SH/SZ) or by strategy
- Show sector/concept distribution as chips

### 8. Data Backup Reminder

**Problem:** Users don't think about backups until data is lost.

**Solution:**
- On first launch, offer to enable backup reminder
- Periodic notification (configurable: daily/weekly/monthly)
- Quick-export button in notification
- Settings: toggle backup reminder on/off, set frequency

## Architecture Changes

### New/Modified Files (MVP)

```
app/src/main/java/com/scanfolio/
├── ui/
│   ├── scan/
│   │   ├── ScanScreen.kt          (modify)
│   │   ├── ScanViewModel.kt       (modify)
│   │   └── PreviewScreen.kt       (NEW)
│   ├── portfolio/
│   │   ├── PortfolioScreen.kt     (modify — add DashboardCard, SearchBar, SortDropdown)
│   │   ├── PortfolioViewModel.kt  (modify — add search/sort/dashboard logic)
│   │   ├── StockDetailScreen.kt   (modify — add edit/delete, data editing)
│   │   ├── StockDetailViewModel.kt(modify)
│   │   ├── TradeFormSheet.kt      (modify — add edit mode)
│   │   ├── DashboardCard.kt       (NEW)
│   │   └── ManualStockEntryScreen.kt(NEW)
│   └── navigation/
│       └── AppNavigation.kt       (modify — add new routes)
```

### New/Modified Files (V2)

```
app/src/main/java/com/scanfolio/
├── ui/
│   └── analysis/
│       ├── AnalysisScreen.kt      (modify — add charts)
│       └── AnalysisViewModel.kt   (modify — add chart data prep)
├── data/
│   └── repository/
│       └── SettingsRepository.kt  (modify — add backup preference)
```

## Data Flow

### Scan Preview Flow
```
User selects image
  → ScanViewModel.processImage()
  → OCR recognizes
  → PreviewScreen shown with editable table
  → User edits cells / deletes rows
  → User clicks "Confirm Import"
  → ScanViewModel.confirmImport() batches inserts
  → Navigate to PortfolioScreen
  → Imported data appears in list
```

### Trade Edit Flow
```
User long-presses trade in StockDetailScreen
  → Context menu: Edit / Delete
  → If Edit: TradeFormSheet opens pre-filled
  → User modifies values, clicks Save
  → StockDetailViewModel.updateTrade()
  → Room Flow updates list automatically
  → If Delete: ConfirmDialog → StockDetailViewModel.deleteTrade() → CASCADE
```

## Error Handling

- **OCR preview:** If user edits a cell to empty text, show warning but allow it (save as "--")
- **Trade form validation:** Show inline error messages for invalid price, date format, etc.
- **Dashboard:** If no trades exist, show "请先添加交易记录" placeholder
- **Edit conflicts:** Single-user app with Room, no conflict expected

## Testing Strategy

- Each new ViewModel method tested via JUnit with in-memory Room database
- Composable preview functions added for new screens
- Manual testing flow for scan → preview → confirm → portfolio display
