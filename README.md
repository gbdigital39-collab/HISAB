# Hisaab for Android

A simple expense tracker for cash and online payments, built with Kotlin, Jetpack Compose, and Room.

## Run it on your phone

1. Open this project in Android Studio (Koala or newer) and wait for Gradle sync.
2. Connect an Android phone with USB debugging enabled, or start an emulator.
3. Choose the device in Android Studio and press **Run**.

To build an APK, choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Features

- Add expenses in rupees with a suggested or custom category.
- Mark a payment as cash or online and add an optional note.
- Browse and search your activity; delete an entry with its trash button.
- See monthly spending and category breakdowns in Insights.
- Expense data is stored locally in a Room database. The app does not request internet or SMS permissions.

## Not built yet

Automatic bank SMS or payment notification tracking, receipt scanning, custom dates, budgets, and cloud sync are not implemented. Transactions use the current date and time.
