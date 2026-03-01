# VCC Administrator

Кросплатформенний адміністратор VCC, розроблений на **Kotlin Multiplatform** та **Compose Multiplatform** (Android, iOS, Desktop, Web).

## Підтримувані платформи
- Android  
- iOS  
- Desktop (Windows, macOS, Linux)  
- Web (Wasm + JS)

## Збірка та запуск

### Android
```bash
./gradlew :androidApp:assembleDebug
APK: androidApp/build/outputs/apk/debug/androidApp-debug.apk
Desktop
Bash./gradlew :desktopApp:run
Hot-reload:
Bash./gradlew :desktopApp:hotRun --auto
iOS
Відкрийте iosApp/iosApp.xcworkspace у Xcode та запустіть.
Web
Розробка (Wasm — рекомендовано):
Bash./gradlew :webApp:wasmJsBrowserDevelopmentRun
Production-збірка:
Bash./gradlew :webApp:composeCompatibilityBrowserDistribution
Результат: webApp/build/dist/composeWebCompatibility/productionExecutable
Структура проєкту

composeApp/ — спільний код (commonMain + платформоспецифічні папки)
androidApp/, desktopApp/, webApp/ — платформенні модулі
iosApp/ — Xcode-проєкт для iOS


Made with Kotlin Multiplatform + Compose Multiplatform
