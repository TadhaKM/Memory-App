# Recall

<p align="center">
  <strong>The note app that remembers for you.</strong>
</p>

<p align="center">
  Capture anything. Let AI organize it. Watch the right notes resurface at the right time.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square" alt="Platform" />
  <img src="https://img.shields.io/badge/Kotlin-1.9-purple?style=flat-square" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue?style=flat-square" alt="Compose" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-orange?style=flat-square" alt="Min SDK" />
</p>

---

## Table of Contents

- [Philosophy](#philosophy)
- [Features](#features)
- [How It Works](#how-it-works)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Philosophy

> **Information should fade by default — only ideas that prove their value deserve permanence.**

| Traditional Note Apps | Recall |
|-----------------------|--------|
| Everything is equally important forever | Most thoughts are temporary — the system decides what survives |
| You organize everything manually | AI organizes, you just capture |
| Notes sit forgotten in folders | Important ideas resurface automatically |
| Filing cabinet | Living memory |

**That philosophical difference is everything.**

---

## Features

### Multi-Modal Capture

| Mode | Description |
|------|-------------|
| **Text** | Type with auto-save (600ms debounce) |
| **Voice** | Record audio → automatic transcription |
| **Camera** | Take photos → ML Kit OCR extracts text |
| **Share** | Share from any app directly into Recall |

### AI-Powered Organization

When you save a note, it flows through an intelligent pipeline:

```
Input (text/voice/image)
       ↓
   Transcription (audio → text)
       ↓
   OCR (image → text via ML Kit)
       ↓
   LLM Analysis (Claude Haiku)
       ↓
   Summary + Type + Topics + Entities + Action Items
       ↓
   Stored & Indexed
```

Notes are auto-categorized as:
- **Task** — Action items detected
- **Idea** — Creative thoughts (boosted after 21 days for incubation)
- **Reference** — Information to look up later
- **Journal** — Personal reflections
- **Question** — Things to explore
- **Quote** — Words from others

### Adaptive Memory Decay

Every note moves through four cognitive states over time:

| State | Memory Strength | What User Sees |
|-------|-----------------|----------------|
| **Fresh** | > 3.0 | Full note + transcript + OCR |
| **Condensed** | 2.0 – 3.0 | Summary + key entities |
| **Faded** | 1.0 – 2.0 | Title + 1-line essence |
| **Dormant** | < 1.0 | Invisible unless searched |

**Memory Strength Formula:**
```
memory_strength = resurfacing_score - decay_pressure(time)

decay_pressure = days_since_last_interaction × 0.03
```

Notes **earn their way upward** through:
- User opens/edits note
- User pins or promotes
- AI detects relevance to recent notes
- Appears in Daily Recall and isn't dismissed

Notes **fade faster** when:
- Ignored when resurfaced
- Explicitly dismissed
- No semantic connections over time

### Daily Recall

A calm, non-intrusive way to reconnect with your past thoughts:

```
┌─────────────────────────────────────────────┐
│  "An idea from 6 weeks ago (fading)"        │
│                                             │
│  You once wrote about teaching creativity   │
│  through constraints.                       │
│                                             │
│  [ Revisit ]  [ Let fade ]  [ This matters ]│
└─────────────────────────────────────────────┘
```

### Smart Search

- Full-text search (Room FTS4)
- Semantic search via embeddings
- Filters: type, attachments, date range, archived

### Export

- TXT or Markdown format
- Includes all metadata, transcripts, OCR text
- Share directly to other apps

### Security

- **No hardcoded API keys** — loaded from `local.properties` via BuildConfig
- **Rate limiting** — Token bucket algorithm for all API calls
- **Input validation** — OWASP-compliant sanitization
- **Secure logging** — No PII or secrets logged

---

## How It Works

### Resurfacing Algorithm

A nightly job (2:30 AM) calculates a resurfacing score for each note:

```
Base:     random(0..1)

Modifiers:
  +2.0    if pinned
  +1.5    if task with unfinished items
  +1.0    if idea older than 21 days (incubation boost)
  +1.0    if importance ≥ 2
  +0.5    if AI processed
  -1.0    if shown in last 3 days
  -0.5    if archived
  -∞      if marked "never resurface"
```

Top 3-7 notes shown daily. Dormant notes (memory_strength < 1.0) are excluded.

### Trust Rule

At any time, the user can:
- Search for any note (including dormant)
- Restore full note detail
- Pin permanently
- Export everything

**Nothing is ever destroyed without consent.**

---

## Tech Stack

### Android

| Technology | Purpose |
|------------|---------|
| Kotlin | Language |
| Jetpack Compose | UI Framework |
| Material 3 | Design System |
| Hilt | Dependency Injection |
| Room | Local Database |
| DataStore | Preferences |
| WorkManager | Background Jobs |
| OkHttp/Retrofit | Networking |
| ML Kit | On-device OCR |

### Backend

| Technology | Purpose |
|------------|---------|
| Supabase | Auth, Database, Storage |
| PostgreSQL | Primary Database |
| Row Level Security | Data Protection |
| pgvector | Embeddings Storage |

### AI

| Technology | Purpose |
|------------|---------|
| Anthropic Claude (Haiku) | Classification, Summarization |
| Google ML Kit | On-device OCR |
| Embeddings | Semantic Search |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 26+ (min), 34 (target)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Memory-App.git
   cd Memory-App
   ```

2. **Configure API keys**
   ```bash
   cp local.properties.example local.properties
   ```

3. **Edit `local.properties`** with your credentials (see [Configuration](#configuration))

4. **Open in Android Studio** and sync Gradle

5. **Run the app** (▶️ or `Shift+F10`)

---

## Configuration

Create a `local.properties` file in the project root with the following:

| Variable | Required | Description | Source |
|----------|----------|-------------|--------|
| `SUPABASE_URL` | Yes | Your Supabase project URL | [Supabase Dashboard](https://supabase.com/dashboard) → Settings → API |
| `SUPABASE_ANON_KEY` | Yes | Supabase anonymous key | Same as above |
| `ANTHROPIC_API_KEY` | No* | Anthropic API key for AI features | [Anthropic Console](https://console.anthropic.com/) |
| `POSTHOG_API_KEY` | No | PostHog analytics key | [PostHog](https://posthog.com/) |
| `SENTRY_DSN` | No | Sentry error tracking DSN | [Sentry](https://sentry.io/) |

*Without Anthropic API key, AI features (auto-categorization, summaries) will be disabled but the app remains fully functional.

**Example `local.properties`:**
```properties
sdk.dir=/path/to/android/sdk

SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
ANTHROPIC_API_KEY=sk-ant-api03-...
```

---

## Project Structure

```
app/src/main/java/com/recall/app/
├── core/
│   ├── auth/           # Supabase authentication (AuthManager)
│   ├── security/       # Rate limiter, input validator, config
│   ├── analytics/      # PostHog + Sentry integration
│   ├── export/         # Note export functionality
│   ├── media/          # Audio recording, image handling
│   ├── ocr/            # ML Kit OCR processing
│   ├── share/          # Share intent handling
│   └── util/           # Extensions, constants, Result type
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Supabase data sources, DTOs
│   ├── repository/     # Repository implementations
│   ├── mapper/         # Entity ↔ Domain mappers
│   └── preferences/    # DataStore preferences
├── domain/
│   ├── model/          # Note, Attachment, AiMetadata, MemoryState
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic (ResurfaceScore, MemoryStrength, Search)
├── ai/
│   ├── client/         # LLM clients (Anthropic, Embeddings, Transcription)
│   ├── config/         # API configuration
│   ├── processor/      # Note processing pipeline
│   └── prompts/        # AI prompt templates
├── sync/
│   ├── workers/        # WorkManager jobs (Sync, Upload, AI, Resurface)
│   └── SyncScheduler   # Job scheduling
├── ui/
│   ├── screens/        # Compose screens (Home, Capture, Detail, DailyRecall, Search, Settings)
│   ├── components/     # Reusable UI components
│   ├── theme/          # Material 3 theming
│   └── navigation/     # Navigation graph
├── di/                 # Hilt dependency injection modules
├── MainActivity.kt
└── RecallApplication.kt
```

---

## Architecture

Recall follows **Clean Architecture** with **MVVM** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                         UI Layer                         │
│  (Compose Screens, ViewModels, Navigation)              │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      Domain Layer                        │
│  (Use Cases, Repository Interfaces, Domain Models)      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                       Data Layer                         │
│  (Repository Impl, Room DB, Supabase, DataStore)        │
└─────────────────────────────────────────────────────────┘
```

**Key Principles:**
- **Unidirectional Data Flow** — State flows down, events flow up
- **Single Source of Truth** — Room database for local data
- **Offline-First** — Full functionality without network
- **Dependency Injection** — Hilt for testability

---

## Testing

### Build the App

```bash
./gradlew assembleDebug
```

### Run Unit Tests

```bash
./gradlew test
```

### Run Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

## Contributing

Contributions are welcome! Please follow these guidelines:

### Getting Started

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit with clear messages: `git commit -m "feat: Add your feature"`
6. Push to your fork: `git push origin feature/your-feature`
7. Open a Pull Request

### Commit Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

| Type | Description |
|------|-------------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation |
| `style:` | Formatting (no code change) |
| `refactor:` | Code restructuring |
| `test:` | Adding tests |
| `chore:` | Maintenance |

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable/function names
- Keep functions small and focused
- Add KDoc for public APIs

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Modern Android UI
- [Supabase](https://supabase.com/) — Open source Firebase alternative
- [Anthropic](https://www.anthropic.com/) — Claude AI models
- [Google ML Kit](https://developers.google.com/ml-kit) — On-device ML

---

<p align="center">
  <strong>Built with ❤️ using Kotlin & Jetpack Compose</strong>
</p>

<p align="center">
  <em>You don't manage your notes. Your notes manage themselves.</em>
</p>
