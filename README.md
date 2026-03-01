```markdown
# VCC Administrator

Кросплатформенний адміністратор VCC, розроблений на **Kotlin Multiplatform** + **Compose Multiplatform**.

Підтримувані платформи:  
**Android • iOS • Desktop (Windows, macOS, Linux) • Web (Wasm + JS)**

## Збірка та запуск

### Android
```bash
./gradlew :androidApp:assembleDebug
```
APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop
```bash
./gradlew :desktopApp:run
```
Hot-reload:
```bash
./gradlew :desktopApp:hotRun --auto
```

### iOS
Відкрийте `iosApp/iosApp.xcworkspace` у Xcode та запустіть.

### Web

**Розробка (Wasm — рекомендовано):**
```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

**Production-збірка:**
```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
```
Результат: `webApp/build/dist/composeWebCompatibility/productionExecutable`

## Структура проєкту
- `composeApp/` — спільний код (`commonMain` + платформоспецифічні папки)  
- `androidApp/`, `desktopApp/`, `webApp/` — платформенні модулі  
- `iosApp/` — Xcode-проєкт

---

Made with ❤️ Kotlin Multiplatform & Jetpack Compose Multiplatform
```
