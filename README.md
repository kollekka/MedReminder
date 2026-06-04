# MedReminder

MedReminder to aplikacja Android do planowania leków i wizyt lekarskich z przypomnieniami oraz prostym przeglądem zdrowotnych zadań dnia.

## Funkcje
- dodawanie, edycja i archiwizacja leków wraz z dawkowaniem, ilością, notatkami i harmonogramem
- przypomnienia o lekach (powiadomienia) z obsługą wielu godzin
- podgląd dzisiejszych dawek, nadchodzących wizyt i niskiego stanu leków
- planowanie wizyt lekarskich z przypomnieniami (3 dni, 1 dzień i 1 godzina przed wizytą)
- tryb ciemny oraz języki: PL/EN
- anonimowe logowanie i per‑użytkownikowe dane w Firebase

## Stos technologiczny
- Kotlin + Jetpack Compose + Material 3
- Firebase Auth (anonimowe logowanie) i Cloud Firestore
- WorkManager (harmonogram przypomnień)
- DataStore (ustawienia języka) i SharedPreferences (motyw)
- Gradle

## Wymagania
- Android Studio
- JDK 11
- Android SDK (minSdk 23, targetSdk 36)

## Konfiguracja Firebase
1. Utwórz projekt w Firebase i dodaj aplikację Android z `applicationId` = `com.elozelo.medreminder`.
2. Włącz **Authentication (Anonymous)** oraz **Cloud Firestore**.
3. Pobierz `google-services.json` i podmień plik w `app/google-services.json`.

## Uruchomienie
1. Otwórz projekt w Android Studio.
2. Zsynchronizuj Gradle.
3. Uruchom aplikację na emulatorze lub urządzeniu:
   ```bash
   ./gradlew assembleDebug
   ```

## Testy i lint
```bash
./gradlew test
./gradlew lint
```
