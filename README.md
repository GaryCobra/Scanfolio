<p align="center">
  <img src="docs/banner.svg" alt="Momentum" width="600">
</p>

<h1 align="center">Momentum — 股票投资追踪助手</h1>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#faq">FAQ</a>
</p>

<p align="center">
  <b>English</b> | <a href="README.zh.md">中文</a>
</p>

<p align="center">
  <b>Momentum</b> (formerly Scanfolio) is an open-source Android app that helps Chinese A‑share investors track, analyze, and improve their stock trading. Search stocks by code, auto-fill real-time data from free APIs, calculate technical indicators, group by strategy, and visualize your profit curve — all offline-capable after initial data fetch.
</p>

<hr>

## Features

### 🔍 Stock Search & Auto Data Fill
Search any A‑share stock by its 6‑digit code. Momentum automatically fetches real‑time quotes, K‑line data, and money flow from **East Money** and **Sina Finance** free APIs, then populates 20+ data columns including:
- Price, change %, turnover rate, volume, amplitude
- PE ratio, PB ratio, market cap, circulating market cap
- Industry classification, concept tags
- 20‑day/month returns, consecutive up days, limit‑up count

### 📊 Technical Indicators (Auto‑Calculated)
Given 60 days of daily K‑line data, Momentum computes:
- **KDJ** (9‑day stochastic oscillator)
- **MACD** (12/26/9 EMA crossover)
- **RSI** (6/12/24 relative strength index)
- **BOLL** (20‑day Bollinger Bands)

These indicators appear directly in your stock portfolio as analyzable data columns.

### 📈 PnL Tracking & Profit Curve
Record every buy/sell trade with price, quantity, profit ratio, and strategy name. Momentum builds:
- **Realized PnL** from closed trades
- **Unrealized PnL** from open positions (live price via API)
- **Win rate, avg win/loss, largest win/loss** statistics
- **Monthly bar chart** and **cumulative profit curve** powered by MPAndroidChart

### 📂 Stock Grouping by Strategy
Organize your self‑selected stocks into strategy groups (e.g., "突破低吸", "首板打板", "趋势跟踪"). Each group shows its own win rate and total PnL. Collapsible group headers keep your portfolio clean.

### 📐 Success/Failure Analysis
Compare the data values of winning trades vs. losing trades side‑by‑side:
- Which columns correlate with success? (e.g., "winning trades average +3.5% change, losers average -1.2%")
- Market comparison: how often did your stocks outperform the Shanghai/Shenzhen index?
- Filter by virtual/real trades

### 🖼️ Import from Screenshot (OCR)
Use the camera or gallery to import your existing 同花顺 (Tonghuashun) portfolio screenshots. Momentum extracts tabular data via OCR, de‑duplicates by stock code, and merges into your portfolio.

### 🔧 Fully Customizable
- Enable/disable data columns
- Define your own trading strategies
- Add market indices for comparison
- Import/export all data as JSON

---

## Screenshots

| Portfolio | Search & Add | Stock Detail | PnL Detail |
|-----------|--------------|--------------|------------|
| <img src="docs/screenshots/portfolio.png" width="200"> | <img src="docs/screenshots/search.png" width="200"> | <img src="docs/screenshots/detail.png" width="200"> | <img src="docs/screenshots/pnl.png" width="200"> |

| Analysis | Grouping | Settings | OCR Import |
|----------|----------|----------|------------|
| <img src="docs/screenshots/analysis.png" width="200"> | <img src="docs/screenshots/grouping.png" width="200"> | <img src="docs/screenshots/settings.png" width="200"> | <img src="docs/screenshots/ocr.png" width="200"> |

> **Note:** Screenshots are placeholders until the app is published. PRs with actual screenshots are welcome!

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 100% |
| **UI** | Jetpack Compose + Material 3 |
| **Navigation** | Navigation Compose (single‑activity) |
| **Architecture** | Clean Architecture (Repository pattern + ViewModel) |
| **Database** | Room with KSP, Flow‑based reactive queries |
| **Networking** | OkHttp 4.x (East Money & Sina Finance APIs) |
| **Charts** | MPAndroidChart v3.1.0 |
| **Serialization** | Gson |
| **Testing** | JUnit 4 + MockK + kotlinx‑coroutines‑test |
| **Min SDK / Target** | 26 / 35 |

### APIs Used

| API | Purpose | Endpoint |
|-----|---------|----------|
| **Sina Finance** | Basic real‑time quote | `hq.sinajs.cn/list=` |
| **East Money** | Full quote (PE, PB, mkt cap, industry) | `push2.eastmoney.com/api/qt/stock/get` |
| **East Money** | Daily K‑line (60 days) | `push2.eastmoney.com/api/qt/stock/kline/get` |
| **East Money** | Money flow (main force / retail) | `push2.eastmoney.com/api/qt/stock/fflow/daykline/get` |

All APIs are **free, public, and require no authentication**.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  Compose Screens ←→ ViewModels (StateFlow)          │
│  Portfolio · StockDetail · PnL · Analysis · Settings│
└───────────────────┬─────────────────────────────────┘
                    │ collects StateFlow
┌───────────────────▼─────────────────────────────────┐
│                   Repository Layer                   │
│  StockRepository · TradeRepository · SettingsRepo    │
│  MarketIndexRepository                               │
└───────┬───────────────────────────────┬─────────────┘
        │                               │
┌───────▼──────────┐          ┌─────────▼───────────┐
│   Room Database   │          │  StockApiClient     │
│ 6 Entities / DAOs │          │  OkHttp + East Money│
│  + TypeConverters │          │  + Sina Finance     │
└───────────────────┘          └─────────────────────┘

  TechnicalIndicatorCalculator (KDJ · MACD · RSI · BOLL)
```

### Key Design Decisions

- **Single‑activity architecture**: One `MainActivity` with Compose Navigation handles all routing. Three bottom tabs (Portfolio / Add / Analysis) plus sub‑routes for detail screens.
- **Repository‑driven data access**: ViewModels never touch DAOs or OkHttp directly. Repositories abstract the data source and return `Flow<T>` for reactive updates.
- **StockApiClient as a facade**: All external API calls (Sina quote, East Money full quote, K‑line, money flow) go through one class. `buildDataColumns()` converts raw API data into the `Map<String,String>` format used by the database.
- **Offline‑capable core**: Stock data is persisted to Room after fetching. The app works with stale data when offline; only live quotes require internet.

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1+) or newer
- JDK 17
- An Android device/emulator running API 26+

### Clone & Build

```bash
git clone https://github.com/yourusername/momentum.git
cd momentum
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Tests

```bash
./gradlew testDebugUnitTest
```

Tests cover:
- `StockApiClientTest` — exchange detection, Sina quote parsing, K‑line parsing, data column building
- `TechnicalIndicatorCalculatorTest` — KDJ, MACD, RSI, BOLL calculations and output formatting

---

## FAQ

**Q: Do I need an API key?**  
No. All stock data comes from free, public APIs (Sina Finance and East Money). No registration required.

**Q: Does it support Hong Kong or US stocks?**  
Currently only A‑shares listed on Shanghai, Shenzhen, and Beijing exchanges (codes starting with 0, 3, 6, 4, 8). HK/US support is not planned.

**Q: Can I use it offline?**  
Partially. Previously fetched stock data and trade records are stored locally in Room. Real‑time quotes, K‑line, and money flow require internet.

**Q: Is my data private?**  
Yes. All data stays on your device. No accounts, no cloud sync, no telemetry. The only network requests are to public stock API endpoints for market data.

**Q: The app is in Chinese only. Any plans for English?**  
Not currently. The app targets Chinese A‑share investors. Internationalization (i18n) PRs are welcome.

---

## Roadmap

- [x] Stock search by code + auto API data fill
- [x] Technical indicators (KDJ, MACD, RSI, BOLL)
- [x] PnL tracking with profit curve chart
- [x] Stock grouping by strategy
- [x] Success/failure analysis with market comparison
- [x] OCR import from screenshots
- [ ] Dark mode refinement
- [ ] Widget (home screen portfolio overview)
- [ ] Notification alerts for price targets
- [ ] Export to Excel/CSV

---

## Contributing

Contributions are welcome! Here's how to help:

1. **Fork** the repository
2. **Create a feature branch**: `git checkout -b feat/amazing-feature`
3. **Commit** your changes: `git commit -m "feat: add amazing feature"`
4. **Push**: `git push origin feat/amazing-feature`
5. **Open a Pull Request**

Please ensure:
- Tests pass (`./gradlew testDebugUnitTest`)
- The app builds successfully (`./gradlew assembleDebug`)
- Code follows existing conventions (Kotlin, Compose, Material 3)
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)

### Development Setup

Open the project in Android Studio, sync Gradle, and run on an emulator or device. All stock APIs work without any configuration.

---

## License

```
MIT License

Copyright (c) 2026 Momentum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
