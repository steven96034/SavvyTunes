# Savvy Tunes



<p align="center">
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-1.9.0-purple?logo=kotlin" alt="Kotlin">
  </a>
  <a href="https://android-arsenal.com/api?level=26">
    <img src="https://img.shields.io/badge/Minimum%20API-34-brightgreen.svg?logo=Android" alt="Android Minimum API">
  </a>
  
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-blue?logo=jetpackcompose" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/DI-Dagger_Hilt-2C2255?logo=android&logoColor=white" alt="Dagger Hilt">
  
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=white" alt="Firebase">
  <img src="https://img.shields.io/badge/API-Spotify_Web-1DB954?logo=spotify&logoColor=white" alt="Spotify API">
  <img src="https://img.shields.io/badge/AI-Google_Gemini-8E75B2?logo=googlebard&logoColor=white" alt="Gemini AI">

  <img src="https://img.shields.io/badge/Testing-JUnit_|_MockK-4CAF50?logo=testinglibrary&logoColor=white" alt="Testing">
  <img src="https://img.shields.io/github/license/steven96034/SavvyTunes?color=blue" alt="License">
</p>


<p align="center">
  <img src="https://github.com/user-attachments/assets/7244aed8-a65e-41f2-962e-296f05e91a67" alt="SavvyTunes Icon"/>
</p>


> A smart, personalized music discovery application powered by the Spotify Web API and Google Gemini AI.



Savvy Tunes is an Android application designed for music enthusiasts. By analyzing your Spotify listening history and combining it with real-time weather data and the semantic power of Gemini AI, it curates highly accurate, context-aware music recommendations. Built with a modern UI and a seamless cross-platform authentication experience.



## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/b61262a5-8936-493f-8617-636eae9b34e8" width="30%" alt="Home Showcase"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/e5ad178a-e741-4bd2-9549-d76f8b61065b" width="30%" alt="Track Detail Screen"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/6653e6fc-524b-421d-9c46-db667c2b28d9" width="30%" alt="AI-powered Discovery"/>
</p>

## 🌟 Core Modules Showcase

<table width="100%">
  <tr>
    <td width="35%" align="center">
      <img src="https://github.com/user-attachments/assets/9fc23a37-7dde-423a-8cbb-d558f657aacd" width="250" alt="Immersive Showcase UI"/>
    </td>
    <td width="65%" valign="center">
      <h3>📱 Immersive Showcase & Daily Mix</h3>
      <p>Experience music discovery like never before with an <b>immersive vertical paging</b> layout. Built entirely with Jetpack Compose, the UI delivers silky-smooth, native animations fully optimized for high-refresh-rate displays (120Hz+).</p>
      <ul>
        <li><b>Context-Aware:</b> Dynamic recommendations based on your real-time GPS location and WMO weather data.</li>
        <li><b>Immersive Feedback:</b> Features dynamic arrow indicators and custom easing animations for a premium feel.</li>
        <li><b>Cloud Automation:</b> Daily mixes are generated and delivered every morning via <b>Firebase Cloud Functions & FCM</b>.</li>
      </ul>
    </td>
  </tr>

  <tr>
    <td width="65%" valign="center">
      <h3>🤖 AI-Powered Discovery via Gemini</h3>
      <p>Go beyond traditional keyword searches. By integrating <b>Firebase Vertex AI</b>, Savvy Tunes understands the semantic vibe of your music.</p>
      <ul>
        <li><b>Semantic Matching:</b> Tap the AI button to generate contextually similar tracks.</li>
        <li><b>Algorithmic Accuracy:</b> Uses the <b>Levenshtein Distance</b> algorithm to perfectly map Gemini's text output to real Spotify track data.</li>
      </ul>
    </td>
    <td width="35%" align="center">
      <img src="https://github.com/user-attachments/assets/2667e059-f329-433d-a6ac-5eb2ecbb899d" width="250" alt="AI Discovery via Gemini"/>
    </td>
  </tr>

  <tr>
    <td width="35%" align="center">
      <img src="https://github.com/user-attachments/assets/22ea4a9a-c313-436c-bfb5-ca6096b489e3" width="250" alt="Personalized Dashboard"/>
    </td>
    <td width="65%" valign="center">
      <h3>☁️ Personalized Dashboard</h3>
      <p>Your Spotify listening habits, beautifully visualized. Seamlessly navigate between your Top Artists, Top Tracks and Recently Played Tracks using horizontal swipe gestures.</p>
      <ul>
        <li><b>Real-Time Stats:</b> Instantly retrieves and beautifully formats your top listening history and recently played tracks directly from the Spotify API.</li>
      </ul>
    </td>
  </tr>
  
  <tr>
    <td width="65%" valign="center">
      <h3>🚀 Seamless Navigation & Smart Autofill</h3>
      <p>A frictionless user journey from exploring to discovering.</p>
      <ul>
        <li><b>Contextual Action:</b> Spot a track you love in your Recently Played or Top Tracks? Simply tap "Find similar tracks and artists!"</li>
        <li><b>Smart Routing:</b> The app instantly navigates to the FindMusic screen, securely passing the track data, autofilling the search bar, and seamlessly executing the query without extra taps.</li>
      </ul>
    </td>
    <td width="35%" align="center">
      <img src="https://github.com/user-attachments/assets/b0049195-0304-47a8-8a26-89a4b3006197" width="250" alt="Seamless Navigation and Search"/>
    </td>
  </tr>

  <tr>
    <td width="35%" align="center">
      <img src="https://github.com/user-attachments/assets/0563ec0e-9bfb-4669-9414-b54e3786fa97" width="250" alt="Deep Customization Settings"/>
    </td>
    <td width="65%" valign="center">
      <h3>⚙️ Deep Customization</h3>
      <p>Total control over your music discovery algorithm.</p>
      <ul>
        <li><b>Granular Filters:</b> Adjust the number of tracks, specify genres, filter by release year, and set language preferences using intuitive Compose sliders and switches.</li>
        <li><b>Taste Regeneration:</b> A dedicated feature to re-analyze your recent top tracks and instantly refresh your recommendation pool.</li>
      </ul>
    </td>
  </tr>
</table>



## ✨ Key Features



### 📱 Modern & Intuitive UI/UX

* **Smooth Navigation:** Fully built with Jetpack Compose using `Scaffold`, integrated `TopBar`, and customized Snackbars. The main dashboard utilizes a `HorizontalPager` for intuitive swipe gestures and bottom navigation integration.

* **Immersive Showcase:** The home screen features a vertical paging layout for a highly engaging viewing experience. Each page displays album art, track details, and dynamic recommendations based on **real-time GPS location, WMO weather conditions, and temperature**, categorized into "Weather" and "Emotion" tabs. The total number of showcased tracks is fully customizable via user settings.



### 🤖 AI-Powered Music Discovery

* **Smart Search:** Beyond standard Spotify queries, users can trigger Gemini AI to analyze semantic relationships and discover hidden gems. Calls to Gemini are securely routed directly through **Firebase Vertex AI** (`GenerativeBackend.googleAI()`).

* **Personalized Dashboard:** Dedicated screens to visualize the user's Spotify Top Artists, Top Tracks, and Recently Played data.



### ☁️ Cloud Services & Automated Push Notifications

* **Daily Recommendations:** Powered by **Firebase Cloud Functions**, the system automatically generates a personalized "For your everyday recommendation" playlist every morning and delivers it via Firebase Cloud Messaging (FCM).

* > **💡 Backend Service:** The Cloud Functions (Python) handling the Daily Mix generation, FCM push notifications, and secure token management are maintained in a separate repository. 
> 👉 **[View the Savvy Tunes Backend Repository Here](https://github.com/steven96034/SavvyTunes-Backend)**



### 🔐 Robust Authentication System

* **Firebase Auth & Spotify OAuth 2.0 (PKCE):** Supports passwordless Email login, Google Sign-In, Anonymous Login, and implements a strict Proof Key for Code Exchange (PKCE) flow for Spotify account authorization.



---



## 🛠️ Tech Stack & Architecture



The project strictly follows **Clean Architecture** principles, separating concerns into UI/Presentation, Domain, and Data layers, combined with the MVVM pattern and Unidirectional Data Flow (UDF).



* **Language:** Kotlin

* **UI Framework:** Jetpack Compose (Material 3), Accompanist

* **Dependency Injection:** Dagger Hilt

* **Networking:** Retrofit, OkHttp3

* **Cloud & AI:** Firebase (Auth, Firestore, Functions, Messaging, Vertex AI)

* **Algorithms:** Apache Commons Text (Levenshtein Distance)

* **Testing:** JUnit 4/5, MockK, Espresso, Truth, Turbine, Coroutines Test



---



## 🏗️ Under the Hood (Advanced Implementations)



1. **Algorithmic Result Matching:** Implemented a `StringSimilarityCalculator` utilizing the **Levenshtein Distance** algorithm (`Apache Commons Text`). This calculates the edit distance between Spotify's raw search API results and Gemini's semantic text outputs, ensuring the highest relevance and accuracy in track matching.

2. **Robust Dependency Injection:** Structured Hilt DI with meticulously separated modules (`NetworkModule` with specific `Qualifiers`, `FirebaseModule`, `LocationModule`, `RepositoryModule`, `StorageModule`) for a highly scalable, decoupled, and testable architecture.

3. **Concurrency Control for Token Management:** Implemented Kotlin `Mutex()` to prevent race conditions and redundant network calls when refreshing expired access tokens.

4. **Network Layer Interception:** Customized OkHttp client with a `TokenInterceptor`, `ErrorHandlingInterceptor`, and `TokenAuthenticator`.

5. **Data Encryption:** Utilized `EncryptedPreferenceManager` to encrypt sensitive user data (Access/Refresh Tokens) at the hardware level.

6. **Lifecycle-Aware Cloud Optimization:** Implemented `DefaultLifecycleObserver` to monitor app usage time, optimizing Cloud Function invocations to prevent quota waste.



---



## 🚀 Future Roadmap



* [ ] **Global Timezone Support:** Distribute Cloud Function scheduling across Asian, European, and American time zones.

* [ ] **Trending Music Showcase:** Integrate web scrapers/APIs from major music charts.

* [ ] **Pagination for Search:** Implement Paging 3 in the FindMusic layout.

* [ ] **Multi-LLM Strategy:** Evaluate and integrate various LLM models.

* [ ] **Cross-Platform Integration:** Expand API support to include Apple Music and KKBOX.



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
    git clone https://github.com/steven96034/SavvyTunes.git
    ```

2.  **Firebase Configuration**
    To run this project perfectly, you need a Firebase project with the following services enabled and configured:
    * **Core Setup:** Register your Android app, add your **SHA-1 & SHA-256 fingerprints** (required for Google Sign-In and dynamic links), and place the downloaded `google-services.json` into the `app/` directory.
    * **Authentication:** Navigate to *Build > Authentication > Sign-in method* and enable:
        * **Google Sign-In**
        * **Email/Password** (Crucial: You MUST enable the **Email link (passwordless sign-in)** toggle).
        * **Anonymous**
    * **Hosting & Authorized Domains:** To make the Passwordless Email Link deep-route back to the app, ensure Firebase Hosting is enabled and your domain is listed under the "Authorized domains" section in the Authentication settings.
    * **Cloud Functions & Vertex AI (Gemini):** *Note: Using Cloud Functions and Firebase Vertex AI requires your Firebase project to be on the **Blaze (pay-as-you-go) plan**.*
        * **Backend Setup:** The serverless logic for generating Daily Mixes, handling Gemini AI requests, and triggering FCM push notifications is maintained in a decoupled repository.
        * **Deploy & Contribute:** To fully enable these features, please clone the 👉 **[Savvy Tunes Backend Repository](https://github.com/steven96034/SavvyTunes-Backend)**. Follow the instructions in its `README.md` to set up your environment variables and deploy the services using `firebase deploy --only functions`. 
        * *Developers are highly encouraged to fork the backend repo to build custom music recommendation algorithms or integrate additional APIs!*
    * **Firestore Database:** Create a database and configure your Security Rules appropriately.

3.  **Environment Variables (local.properties)**
    * Create a `local.properties` file in your root directory (if it doesn't exist).
    * Add your Spotify Client ID to configure the PKCE OAuth flow:
        ```properties
        SPOTIFY_WEB_API_CLIENT_ID=your_spotify_client_id
        ```
    * *Note: Ensure your `build.gradle` is properly configured to read variables from this file.*

4.  **Build and Run**
    * Sync Gradle and run the app on an emulator or physical device.



---



## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
