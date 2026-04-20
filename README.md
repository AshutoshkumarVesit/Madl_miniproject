# MoveMate 🏃‍♂️💨

MoveMate is a comprehensive Android fitness tracking application designed to help users track their runs, monitor their statistics, and achieve their fitness goals. It features real-time cloud synchronization, an intuitive dashboard, and offline support.

## 🌟 Features

- **Run Tracking**: Log your runs with detailed metrics including distance, time, and pace.
- **Cloud Synchronization**: Integrated with Firebase Firestore for seamless cross-device data syncing.
- **Offline Support**: Uses local SQLite (via `DBHelper`) to store run logs when offline, ensuring no data is ever lost.
- **Interactive Dashboards**: View your progress, recent runs, and personal bests on the dashboard and stats screens.
- **Goal Management**: Set, track, and accomplish your fitness goals.
- **Community & Leaderboards**: Engage with other runners and see where you stand on the community leaderboards.
- **Weather Integration**: Check the weather before your run to stay prepared.
- **Automated Notifications**: Receive reminders and goal-check notifications to stay motivated.

## 🛠️ Technology Stack

- **Platform**: Android (Java)
- **Database**: 
  - Local: SQLite
  - Cloud: Firebase Firestore
- **Architecture**: MVC/MVVM patterns tailored for Android

## 🚀 Getting Started

### Prerequisites
- Android Studio
- An active Firebase project with Firestore enabled.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/AshutoshkumarVesit/Madl_miniproject.git
   ```
2. Open the project in Android Studio.
3. Replace the `google-services.json` file in the `app/` directory with your own configuration from Firebase.
4. Build and run the application on an emulator or a physical Android device.

## 📂 Project Structure

- `app/src/main/java/com/movemate/` - Contains all the core Java source code, including Activities, Models, and Adapters.
- `app/src/main/java/com/movemate/firestore/` - Dedicated package for Firebase Firestore repositories and cloud data models.
- `app/src/main/java/com/movemate/notification/` - Services and workers for handling app notifications and reminders.
- `app/src/main/res/` - Contains XML layouts, drawable resources, colors, and themes.

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/AshutoshkumarVesit/Madl_miniproject/issues).
