# 📊 Expense Tracker — Jetpack Compose Android App

Expense Tracker is a modern, premium, and fully offline Android application built using **Jetpack Compose** and **Room Database**. It features rich interactive spending trend visualizations, stable category breakdown charts, and seamless transactional logging.

---

## ✨ Features

- 💰 **Expense Logging**: Log transactions with an amount (₹), descriptive notes, a category selector, and a custom date picker.
- 📈 **Spending Trends (Bar Chart)**: Rich Vico-powered bar chart mapping historical trends with an ultra-premium **Day/Month toggle switch** and custom formatted X-axis labels (e.g., `26 May` or `May 2026`).
- 🥧 **Category Breakdown (Pie Chart)**: Beautiful Canvas-based expense distribution chart with stable category colors (`Food`, `Transport`, `Utilities`, `Entertainment`, `Shopping`, `Other`) and clean, circular legend bullet alignments.
- ✍️ **Manage Transactions**: Quick edit and delete operations with instant UI state updates and Material 3 confirmation dialogs.
- 🌓 **Dynamic Themes**: Tailored Material 3 color system with gorgeous light mode and high-contrast dark mode integrations.
- 🗄️ **Persistent Room Database**: Secure, offline-first local data storage using a structured DAO repository and reactive Kotlin Flows.

---

## 🛠️ Technology Stack

- **Core**: [Kotlin](https://kotlinlang.org/) & [Jetpack Compose](https://developer.android.com/compose)
- **Database**: [Room SQLite Database](https://developer.android.com/training/data-storage/room)
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
├── MainActivity.kt                 # Entry point setting up Material theme and navigation
├── data/
│   ├── ExpenseEntity.kt            # Room database Entity mapping schema
│   ├── ExpenseDao.kt               # DAO defining SQLite queries and operations
│   └── ExpenseDatabase.kt          # Main Room Database initializer
├── ui/
│   ├── ExpenseViewModel.kt         # ViewModel managing state flows and DB interactions
│   ├── navigation/
│   │   └── AppNavigation.kt        # App routes and bottom navigation bar
│   ├── screens/
│   │   ├── DashboardScreen.kt      # Main dashboard with bar chart, pie chart, and list
│   │   └── LogExpenseScreen.kt     # Log/Edit expense form screen
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
