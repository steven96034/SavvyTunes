# Savvy Tunes 🎵

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue?logo=android)
![Hilt](https://img.shields.io/badge/DI-Hilt-orange)
![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow?logo=firebase)
![Gemini AI](https://img.shields.io/badge/AI-Gemini_Pro-8E75B2?logo=google)

**Savvy Tunes** is an intelligent music discovery application powered by **Gemini AI** and the **Spotify Web API**. It goes beyond simple recommendations by integrating user context—such as current location and weather conditions—to curate the perfect playlist for the moment.

Built with modern Android standards (**Jetpack Compose**, **Clean Architecture**, **MVVM**), it offers a seamless and visually adaptive user experience.

---

## 📱 Screenshots

<p align="center">
  <img src="path/to/home_screen.png" width="22%" alt="Home Screen" />
  <img src="path/to/playlist_generation.png" width="22%" alt="AI Generation" />
  <img src="path/to/analytics.png" width="22%" alt="User Analytics" />
  <img src="path/to/dark_mode.png" width="22%" alt="Dark Mode" />
</p>

---

## ✨ Key Features

### 🤖 1. Context-Aware AI Playlists
Uses **Gemini AI** to generate smart Spotify playlists based on your real-time context.
* **Location & Weather:** Integrates `FusedLocationProviderClient` and **Open-Meteo API** to analyze your environment.
* **Dynamic UI:** Uses `androidx.palette` to extract colors from album art, creating an immersive layout that adapts visually to the music.

### 📅 2. Daily Smart Recommendations
Personalized daily song lists generated via **Firebase Cloud Functions**, tailored specifically to your listening history stored in Firestore.

### 📊 3. Deep Spotify Analytics
View detailed statistics of your Spotify account, including your top tracks and favorite artists over different time ranges.

### 🔍 4. Sonic Discovery
Search for any song or use your favorites to let the AI analyze the style and recommend truly similar tracks and artists ("Vibe Matching").

### 🔐 5. Secure Authentication
Seamless sign-in experience using **Firebase Auth**:
* **OAuth 2.0:** Google Sign-In support.
* **Email/Password:** Custom authentication flow.

### 🛡️ 6. Enterprise-Grade Security
Sensitive user data and tokens are encrypted and protected using the **Android Keystore System**, ensuring maximum security against potential intrusions.

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose, Material Design 3, Palette API
* **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
* **Dependency Injection:** Hilt
* **Asynchronous:** Coroutines & Flow
* **Backend & Cloud:**
    * Firebase Authentication
    * Firebase Firestore
    * Firebase Cloud Functions
* **Network & APIs:**
    * Retrofit / OkHttp
    * Spotify Web API
    * Google Gemini AI (Generative AI)
    * Open-Meteo (Weather Data)
* **Security:** Android Keystore System

---

## 🏗 Architecture

This app follows **Clean Architecture** guidelines to ensure separation of concerns and testability.

* **Presentation Layer:** Jetpack Compose UI + ViewModels.
* **Domain Layer:** Use Cases (Business Logic) + Repository Interfaces.
* **Data Layer:** Repository Implementations + Data Sources (API/Database) + Mappers.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Koala or newer.
* JDK 17+.
* A Spotify Developer Account.
* A Firebase Project.
* A Google Cloud Project (for Gemini API).

### Setup Instructions

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/your-username/savvy-tunes.git](https://github.com/your-username/savvy-tunes.git)
    ```

2.  **Firebase Configuration**
    * Download your `google-services.json` from the Firebase Console.
    * Place it in the `app/` directory.

3.  **API Keys (Secrets)**
    * Create a `local.properties` file in the root directory (if not exists).
    * Add the following keys (do not share this file):
        ```properties
        SPOTIFY_WEB_API_CLIENT_ID=your_spotify_id
        ```
    * *Note: Ensure your `build.gradle` is configured to read these values.*

4.  **Build and Run**
    * Sync Gradle and run the app on an emulator or physical device.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
