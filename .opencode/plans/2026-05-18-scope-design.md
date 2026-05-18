# Scanfolio V2 设计方案（完整版）

## 当前版本范围（4大模块）

| 模块 | 优先级 | 依赖 | 手动输入量 |
|---|---|---|---|
| A. OCR弃用 + 搜索添加 | P0 | 无 | 无 |
| B. 技术指标自动计算 | P1 | A (需StockApiClient) | 无 |
| C. 自选股分组 | P1 | A | 极少（选分组） |
| D. 盈亏汇总 + 收益曲线 | P1 | A + C | 无 |

---

## 模块A：OCR弃用 + 搜索添加

详见上轮设计确认，核心变更：
- 新增 `StockApiClient.kt`
- ScanScreen/SearchStockScreen 重写
- 删除整个 ocr/ 包
- 删除 PreviewScreen、Baidu API 配置

## 模块B：技术指标自动计算

**新增 `data/indicator/TechnicalIndicatorCalculator.kt`**

从日K线数据自动计算标准技术指标，填入数据列：

| 指标 | 公式 | 参数 |
|---|---|---|
| KDJ | RSV=(Cn-L9)/(H9-L9)×100, K=2/3×K' + 1/3×RSV, D=2/3×D'+1/3×K, J=3K-2D | 9日 |
| MACD | EMA12, EMA26, DIF=EMA12-EMA26, DEA=9日EMA of DIF, MACD=2×(DIF-DEA) | 12/26/9 |
| RSI | RSI=100×RS/(1+RS), RS=N日涨幅均值/N日跌幅均值 | 6/12/24 |
| BOLL | 中轨=MA20, 上轨=MA20+2×σ, 下轨=MA20-2×σ | 20日 |

调用时机：StockApiClient 获取 K 线数据后自动计算 → 结果存为 dataColumns（"KDJ_K"、"MACD_DIF"、"RSI_6"、"BOLL_UPPER" 等）

已有 Column Manage 系统可控制显示/隐藏。

## 模块C：自选股分组

**设计思路：复用已有 StrategyTypeEntity 作为分组**

不新建表。在 StockRecordEntity 加一个 `strategy_name: String?` 字段：
- 用户在策略管理（StrategyManageScreen）创建策略作为分组名："短线战法"、"波段战法"、"低吸"等
- 添加自选时，从下拉框选择一个分组（可选，不选则归入"未分组"）
- 持仓页按分组展示，折叠/展开，每组显示独立胜率统计
- 需要 DB migration: stock_records 新增 strategy_name 列

## 模块D：盈亏汇总 + 收益曲线

**新增页面：`PnLDetailScreen` + `PnLViewModel`**

从 PortfolioScreen 的 Dashboard 卡片进入。

### 数据来源（全部现有数据，零手动输入）
| 数据类型 | 来源 |
|---|---|
| 已平仓盈亏 | TradeRecordEntity.profit_amount（求和） |
| 持仓浮盈 | 当前价(API) - 买入价，需新增 quantity 字段 |
| 胜率/交易次数 | 直接统计 TradeRecordEntity |
| 月度盈亏 | 按 buy_time 按月分组汇总 |
| 收益曲线 | 按时间轴累计 realizedPnL + unrealizedPnL |

### UI布局
```
[返回]  盈亏统计
─────────────────
总盈亏: +12,580.00  (红色/绿色)
胜率: 65.2%  |  交易次数: 46
平均盈利: +1,280  平均亏损: -580
最大盈利: +5,200  最大亏损: -3,100
持仓浮盈: +2,100.00
─────────────────
月度盈亏柱状图 (MPAndroidChart)
  ██  +3,200
  ██  +1,800
  ██  +1,200   ██  +900
  ██  +500     ██  +400
 ──  ──  ──  ██  ──  ██  ──
  1月  2月  3月  4月  5月  6月
─────────────────
收益曲线 (MPAndroidChart)
  ∧
  │      ╱╲  ╱╲
  │  ╱╲╱  ╲╱  ╲
  │ ╱            ╲
  └──────────────────→
```

**DB变更**：TradeRecordEntity 新增 `quantity: Int?`（持股数量），用于计算持仓浮盈。

---

## DB Migration 计划

当前 v2 → v3：

```sql
-- stock_records 表新增字段
ALTER TABLE stock_records ADD COLUMN strategy_name TEXT DEFAULT NULL;

-- trade_records 表新增字段  
ALTER TABLE trade_records ADD COLUMN quantity INTEGER DEFAULT NULL;
```

## 文件变更汇总

### 新增文件
| 文件 | 模块 |
|---|---|
| `data/api/StockApiClient.kt` | A |
| `data/indicator/TechnicalIndicatorCalculator.kt` | B |
| `data/db/entity/StockGroupEntity.kt` | C（实际使用StrategyTypeEntity，可能不需要） |
| `ui/pnl/PnLDetailScreen.kt` | D |
| `ui/pnl/PnLViewModel.kt` | D |

### 修改文件
| 文件 | 变更 |
|---|---|
| `ui/scan/ScanScreen.kt` | A: 重写为搜索页 |
| `ui/scan/ScanViewModel.kt` | A: 重写为搜索逻辑 |
| `ui/navigation/AppNavigation.kt` | A: 改tab名，增pnl路由 |
| `ScanfolioApp.kt` | A: 移除OCR |
| `ui/settings/SettingsScreen.kt` | A: 移除Baidu Key |
| `ui/settings/SettingsViewModel.kt` | A: 移除Baidu状态 |
| `app/build.gradle.kts` | A: 移除ML Kit |
| `data/db/AppDatabase.kt` | C/D: migration v2→v3 |
| `data/db/entity/StockRecordEntity.kt` | C: 加strategy_name |
| `data/db/entity/TradeRecordEntity.kt` | D: 加quantity |
| `data/db/dao/StockRecordDao.kt` | C: 加按分组查询 |
| `ui/portfolio/PortfolioScreen.kt` | C: 分组展示 |
| `ui/portfolio/PortfolioViewModel.kt` | C: 分组逻辑, D: PnL计算 |

### 删除文件
ocr/ 包全部、PreviewScreen.kt
