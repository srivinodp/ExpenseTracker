# 📊 Expense Tracker — Jetpack Compose Android App

Expense Tracker is a modern, premium, and fully offline Android application built using **Jetpack Compose** and **Room Database**. It features rich interactive spending trend visualizations, stable category breakdown charts, and seamless transactional logging.

---

## ✨ Features

- 🧠 **Rules-Based Heuristic Insights**: Uses a pure local rules-based engine comparing **current month** to **previous month** data (spikes >10%, savings >15%, stayed-at-home Transport warnings, top contributions) to generate actionable insights.
- 💳 **Horizontal Gradient Summary Card**: A beautifully styled top card displaying monthly spent totals with a preview of the topmost critical insight. Clicking the card opens a Material 3 `ModalBottomSheet` showing a color-coded vertical insights feed.
- ➕ **FAB Navigation & Streamlined Scaffold**: Removed persistent bottom navigation to maximize screen space; primary logging is handled through a Scaffold-nested Floating Action Button (FAB).
- 🧹 **Swipe-to-Delete Gestures**: Native Material 3 `SwipeToDismissBox` support allowing users to swipe transactions from right to left with a smooth red background to delete immediately, alongside confirmative dialog safety.
- 🔙 **TopAppBar Back Navigation**: Configured a native M3 `TopAppBar` header with an Arrow Back button on the logging/editing screen for seamless Android screen routing.
- 📈 **Spending Trends (Bar Chart)**: Vico-powered bar chart mapping historical trends with a segmented **Day/Month toggle switch** and custom formatted X-axis labels (e.g., `26 May` or `May 2026`).
- 🥧 **Category Breakdown (Pie Chart)**: Beautiful Canvas-based expense distribution chart with stable category colors (`Food`, `Transport`, `Utilities`, `Entertainment`, `Shopping`, `Other`) and clean, circular legend bullet alignments.
- 🗄️ **Persistent Room Database**: Secure, offline-first local data storage using a structured DAO repository and reactive Kotlin Flows.
- 📱 **Compact Expandable Cards**: Refactored transaction history to render in super-clean, single-line table rows showing only Category, Date, and Amount. Tap rows to smoothly expand details (Description, Date & Time, Category, Amount) and access dynamic Edit and Delete action controls.
- 🏷️ **Dynamic Category Management**: Custom category manager allowing users to add or delete categories dynamically at runtime. Stored securely in SharedPreferences; new categories receive dynamic, elegant HSL color assignments in all pie chart maps.
- ⏰ **Customizable Notification Reminders**: User-defined daily notification reminders with custom time selection (via standard TimePickerDialog) and alert switch toggles. Dynamically reschedules system alarms in real-time, wakes the device cleanly, and deep-links directly to pre-select the 'Transport' category for seamless logging.
- 📤 **Excel-Compatible CSV Export**: Quick range-filtered expense reporting exporting standard, structured CSV files complete with document headers, total spent, category-wise breakdowns, and individual transaction lists. Safely shared with viewer apps (Excel, Google Sheets, Drive) using Android `FileProvider` URIs.

---

## 🛠️ Technology Stack

- **Core**: [Kotlin](https://kotlinlang.org/) & [Jetpack Compose](https://developer.android.com/compose)
- **Database**: [Room SQLite Database](https://developer.android.com/training/data-storage/room)
- **Settings**: [SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences) for persistent customizable user preferences
- **Charts**: [Vico Charts Library](https://patrykandpatrick.github.io/vico/) (1.13.1)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & Flow state management
- **UI System**: Material Design 3 (M3) components, dynamic styling, and custom canvas-based drawing

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Koala / Ladybug or newer recommended)
- **Android SDK** (API Level 24+ supported, targeting SDK 36)
- **JDK 17 or JDK 21** (Pointers are configured to use Android Studio's embedded JDK automatically)

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/srivinodp/ExpenseTracker.git
   cd ExpenseTracker
   ```

2. **Open in Android Studio**:
   - Open Android Studio, click **File > Open**, and select the `ExpenseTracker` root directory.
   - Gradle will sync and fetch all dependencies automatically.

3. **Build & Run**:
   - Connect your physical Android device or launch an emulator.
   - Click the **Run** button (green play icon) or build via CLI:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 📂 Architecture & Directory Structure

```
app/src/main/java/com/example/expensetracker/
├── MainActivity.kt                 # Entry point setting up Material theme, scheduling reminders
├── data/
│   ├── ExpenseEntity.kt            # Room database Entity mapping schema
│   ├── ExpenseDao.kt               # DAO defining SQLite queries and operations
│   └── ExpenseDatabase.kt          # Main Room Database initializer
├── notification/
│   ├── ExpenseNotificationReceiver.kt # Broadcaster handling reminders and boot scheduling
│   └── ExpenseNotificationScheduler.kt # Daily alarm scheduler respecting custom times
├── util/
│   ├── PreferenceUtils.kt          # SharedPreferences helper for custom categories/reminder times
│   └── ExportUtils.kt              # Excel-compatible CSV compiler and FileProvider sharing
├── ui/
│   ├── HeuristicEngine.kt          # Local rules-based comparative insights calculator
│   ├── ExpenseViewModel.kt         # ViewModel managing state flows and DB interactions
│   ├── navigation/
│   │   └── AppNavigation.kt        # App routing and Navigation graph supporting notification deep links
│   ├── screens/
│   │   ├── DashboardScreen.kt      # Dashboard showing charts, compact expandable rows, Settings, & Exports
│   │   └── LogExpenseScreen.kt     # Log/Edit expense form loading dynamic category chip selectors
│   └── theme/
│       └── Theme.kt                # Light and Dark colors and theme configurations
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to open a Pull Request or report issues.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more details.
