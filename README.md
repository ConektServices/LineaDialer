# Linea — Android Dialer

> A sleek, minimalist default dialer for Android, built with **Jetpack Compose** and **Kotlin DSL**.  
> Theme: dark/light adaptive · Brand gradient: `#FF7D31 → #FE3E4D`

---

## Project Structure

```
app/src/main/
├── java/com/linea/dialer/
│   ├── MainActivity.kt                  ← Entry point, edge-to-edge, splash
│   ├── LineaApp.kt                      ← Application class
│   ├── navigation/
│   │   └── Navigation.kt               ← NavHost + Screen sealed class
│   ├── data/
│   │   └── model/
│   │       └── Models.kt               ← Contact, CallLog, ContactTag, sample data
│   ├── telecom/
│   │   └── LineaInCallService.kt       ← Required InCallService stub
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt                ← Full brand + semantic color system
│       │   ├── Type.kt                 ← Plus Jakarta Sans typography scale
│       │   └── Theme.kt               ← Dark & light Material3 ColorSchemes
│       ├── components/
│       │   ├── Components.kt           ← GradientButton, TagChip, ContactAvatar,
│       │   │                             PulsingAvatar, EndCallButton, etc.
│       │   └── DefaultDialerBanner.kt  ← Banner to prompt default dialer role
│       └── screens/
│           ├── OnboardingScreen.kt     ← 4-page animated pager
│           ├── MainScreen.kt           ← Bottom nav host (Recents | Dial | Contacts)
│           ├── DialpadScreen.kt        ← Full DTMF dialpad + contact match
│           ├── RecentsScreen.kt        ← Call log with search + filters
│           ├── ContactsScreen.kt       ← Tag-filtered contact list
│           ├── ActiveCallScreen.kt     ← Live call UI with timer + controls
│           ├── ContactDetailScreen.kt  ← Notes editor + call history
│           └── PermissionsScreen.kt    ← Runtime permission gate
└── res/
    ├── values/
    │   ├── strings.xml
    │   └── themes.xml
    ├── values-night/
    │   └── themes.xml
    └── font/
        └── plus_jakarta_sans.xml       ← Downloadable font descriptor
```

---

## Quick Setup

### 1. Open in Android Studio
File → Open → select the `linea/` folder. Let Gradle sync.

### 2. Add the font files

Download **Plus Jakarta Sans** from Google Fonts:  
https://fonts.google.com/specimen/Plus+Jakarta+Sans

Place these TTFs in `app/src/main/res/font/`:

| File name | Weight |
|---|---|
| `plus_jakarta_sans_light.ttf` | 300 |
| `plus_jakarta_sans_regular.ttf` | 400 |
| `plus_jakarta_sans_medium.ttf` | 500 |
| `plus_jakarta_sans_semibold.ttf` | 600 |
| `plus_jakarta_sans_bold.ttf` | 700 |
| `plus_jakarta_sans_extrabold.ttf` | 800 |

> **Alternative (no TTF files needed):** Replace `PlusJakartaSans` in `Type.kt` with  
> `GoogleFont("Plus Jakarta Sans")` using the Compose Google Fonts integration.

### 3. Add launcher icons

Run **File → New → Image Asset** in Android Studio to generate:
- `ic_launcher` (adaptive icon)
- `ic_launcher_foreground` (used for splash)
- `ic_launcher_round`

For brand consistency use the gradient `#FF7D31 → #FE3E4D` as the icon background.

### 4. Run the app

Select a device or emulator with API 26+, then hit **Run**.

---

## Screens & Navigation

| Route | Screen | Notes |
|---|---|---|
| `onboarding` | OnboardingScreen | 4 pager cards, swipeable |
| `main` | MainScreen | Bottom nav: Recents / Dial / Contacts |
| `contact_detail/{id}` | ContactDetailScreen | Notes, history, call/message actions |
| `active_call/{id}` | ActiveCallScreen | Timer, mute, speaker, hold, keypad |

**Flow:**  
`Onboarding → Permissions (optional gate) → Main → ContactDetail / ActiveCall`

---

## Color System

```kotlin
// Brand
val BrandOrange = Color(0xFFFF7D31)
val BrandRed    = Color(0xFFFE3E4D)

// Use the gradient pair anywhere:
Brush.linearGradient(listOf(GradientStart, GradientEnd))
```

Tag color mapping (`ui/components/Components.kt`):

| Tag | Color |
|---|---|
| Client | `#34D399` (green) |
| Lead | `#60A5FA` (blue) |
| Partner | `#C084FC` (purple) |
| Supplier | `#FBBF24` (amber) |
| Personal | `#22D3EE` (cyan) |
| Unknown | `#6B7280` (gray) |

---

## Architecture Notes

The project is currently **single-module, state-hoisted in Composables** using `remember`
and sample in-memory data. Ready to evolve into:

```
:app
  ├── :core:data       ← Room DB (contacts, call logs, notes), DataStore (prefs)
  ├── :core:telecom    ← TelecomManager wrapper, CallManager StateFlow
  ├── :feature:dialpad
  ├── :feature:recents
  ├── :feature:contacts
  └── :feature:call
```

**Recommended additions before production:**

| Feature | Library |
|---|---|
| Persistence | `androidx.room` |
| DI | `dagger.hilt` |
| ViewModel | `androidx.lifecycle.viewmodel.compose` |
| Call management | `android.telecom.TelecomManager` |
| Permissions | `accompanist-permissions` ✅ already added |
| Notifications | `NotificationCompat` + `CallStyle` |
| Follow-up reminders | `WorkManager` |
| Analytics | `Firebase Analytics` (optional) |

---

## Setting as Default Dialer

On first launch, show `DefaultDialerBanner` inside your Scaffold, or navigate to
`PermissionsScreen`. The banner handles both API 29+ (`RoleManager`) and older
(`TelecomManager.ACTION_CHANGE_DEFAULT_DIALER`).

---

## License

MIT — build anything you like on top of this.
