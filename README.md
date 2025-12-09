<img src="https://github.com/user-attachments/assets/0e696020-5ff7-435c-a4d8-c626fcbc5b97" align="left" width="50" />

# SilentMate

**A Context-Aware Mobile Application for Automated Sound Profile Management.**

SilentMate intelligently manages your phone's sound profiles based on your schedule, location, and physical context, ensuring you never interrupt a meeting or miss a call again.

## Overview
**SilentMate** solves the problem of forgotten ringtones in quiet places and missed calls after meetings. It is an intelligent Android application that automatically switches your phone's audio profile (Silent, Vibrate, General) based on **where you are**, **what time it is**, and **physical context** (e.g., if the phone is in your pocket or on a desk).

---

## Key Features

### 1. Event-Based Switching (Time + Location)
Unlike standard "Do Not Disturb" modes, SilentMate requires **two** conditions to silence your phone:
* **Time:** Matches your scheduled event (e.g., Meeting, Lecture).
* **Location:** Verifies via GPS that you are physically at the venue.
* *Result:* The phone enters Silent Mode or the mode you prefer only when you are actually at the event. It restores the default profile automatically when the event ends.

### 2. Sensor-Based Switching (Motion & Proximity)
Uses the phone's accelerometer, gyroscope, and proximity sensors to detect context in real-time:
* **Upside down on desk:** 🔇 Switches to **Silent Mode**.
* **In pocket or bag:** 📳 Switches to **Vibration Mode**.
* **In hand/In use:** 🔊 Switches to **General Mode**.

---

## UI

| Dashboard | Event Scheduling | Sensor Switching | Settings |
|:---:|:---:|:---:|:---:|
| <img width="220" alt="Dashboard Screen" src="https://github.com/user-attachments/assets/c9ecbc59-dd8c-4dfd-af90-673f68523868"> | <img width="220" alt="Event Scheduling Screen" src="https://github.com/user-attachments/assets/a605b744-b994-4176-b72f-8bc05894164b"> | <img width="220" alt="Sensor Switching Screen" src="https://github.com/user-attachments/assets/adf67156-cfde-404c-8adf-68049df0f5e9"> | <img width="220" alt="Settings Screen" src="https://github.com/user-attachments/assets/4e6a527f-5a10-4bf6-87b3-ff3302b1992f"> |


## Other Features

### 🔔 Smart Notifications
SilentMate keeps you informed without being intrusive. You receive a notification whenever the audio profile changes automatically.

| Profile Change Notification |
|:---:|
| <img width="300" alt="Notification Screen" src="https://github.com/user-attachments/assets/7e3c6980-8343-496e-9ca1-bbe89a10eeae"> |

### 🌗 Dark & Light Mode Support
Seamlessly adapts to your system theme preferences for a comfortable viewing experience day or night.

| Light Mode | Dark Mode |
|:---:|:---:|
| <img width="220" alt="Light Mode" src="https://github.com/user-attachments/assets/7680f8cd-1f4d-4ff5-b89e-8b3abc834ead"> | <img width="220" alt="Dark Mode" src="https://github.com/user-attachments/assets/1298c53c-4546-4aa1-a71e-fabb7a830c76"> |

### ⚙️ Performance & Background Handling
* **Event Scheduling:** Uses efficient background workers to check location only near scheduled times, preserving battery life.
* **Sensor Monitoring:** Implements a low-latency performance mode to detect physical context changes (pocket vs. desk) in real-time.


## Getting Started

### Prerequisites
* Android Device (Android 8.0 or higher recommended)
* GPS and Sensor permissions enabled

### Installation
1.  Clone the repository:
    ```bash
    git clone [https://github.com/yourusername/silentmate.git](https://github.com/yourusername/silentmate.git)
    ```
2.  Open the project in **Android Studio**.
3.  Build and Run on your device or emulator.
4.  **Important:** Grant "Notification Policy Access", "DND Override" and "Location" permissions upon first launch for full functionality.

---

## User Guide
For detailed instructions, please refer to the documentation:

[**🔗 Access User Guide**](https://lrmanamperi.github.io/Silentmate-Docs/)

