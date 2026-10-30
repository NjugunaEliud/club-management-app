# Campers Waiter (Android, Java)

Plain Java + XML screens (RecyclerView, Material 3, Retrofit + Gson). No Kotlin, no Compose.
Talks to the ASP.NET API in ClubManagement.Web (docs/API.md in the backend patch).

Screens: Login -> Sell | Restock | My Sales | My Stats

## Open & run (Android Studio, Ladybug 2024.2.1 or newer)
1. File > Open > pick this folder. Let Gradle sync (downloads Gradle 8.9 + libraries).
2. Run on an emulator or a USB phone (Android 8.0+).
3. Server address on the login screen:
   - Emulator -> your PC:         http://10.0.2.2:5000/
   - Phone on the same Wi-Fi:     http://<your-PC-LAN-IP>:5000/   (start the server with --urls "http://0.0.0.0:5000")
   - Online server:               https://your-domain/
4. Tap "Test server connection", then sign in with a waiter account (created under Users on the website).

## Build an APK for staff phones
Build > Build Bundle(s) / APK(s) > Build APK(s). Copy app/build/outputs/apk/debug/app-debug.apk to the phones.

## Command line (no Android Studio)
Needs the Android SDK (sdkmanager "platforms;android-35" "build-tools;34.0.0"), a file local.properties containing
  sdk.dir=/home/<you>/Android/Sdk
and Gradle: run `gradle wrapper --gradle-version 8.9` once, then `./gradlew assembleDebug`.

## Project map
app/src/main/java/com/campers/waiter/
  data/  Models (JSON shapes) - ApiService (endpoints) - Net (HTTP, token, errors) - SessionStore (saved login) - Fmt
  ui/    LoginActivity - MainActivity (toolbar + bottom tabs) - SellFragment - RestockFragment - SalesFragment - StatsFragment - Adapters

## Good to know
- A sale carries a clientRequestId. If the network drops after the server saved it and the waiter taps SELL again,
  the server returns the same sale instead of selling twice.
- A disabled waiter, or an expired token, sends the app back to the login screen automatically.
- targetSdk is 34 on purpose (classic window, no edge-to-edge surprises). Fine for APKs you install yourself.
- AndroidManifest has usesCleartextTraffic="true" so plain http:// works while testing.
  Put the server behind HTTPS and set it to "false"; otherwise passwords and tokens travel unencrypted.
- Online only for now. Offline sale queuing would be the next feature.
