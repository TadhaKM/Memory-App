# Recall

**The note app that remembers for you.**

Capture anything. Let AI organize it. Watch the right notes resurface at the right time.

---

## The Philosophy

> **Information should fade by default — only ideas that prove their value deserve permanence.**

**Notion assumes:** "Everything is equally important forever."

**Recall assumes:** "Most thoughts are temporary — the system decides what survives."

That philosophical difference is everything.

---

## What Makes Recall Different

### Adaptive Memory Decay + Resurfacing

This is not a "feature" — it's a foundational behavior that puts Recall in a category Notion cannot enter.

**What "Memory Decay" Actually Means:**

- ❌ Not deletion
- ❌ Not hiding
- ❌ Not archiving
- ✅ **Progressive loss of detail, not existence**

Think human memory:
- You remember that *something* mattered
- You forget the exact wording
- Unless you revisit it

---

## The 4 Memory States

Every note moves through four cognitive states over time:

| State | What User Sees | Purpose |
|-------|----------------|---------|
| **Fresh** | Full note + transcript + OCR | Capture |
| **Condensed** | Summary + key entities | Compression |
| **Faded** | Title + 1-line essence | Signal |
| **Dormant** | Invisible unless searched | Silence |

Notes **earn their way upward** by interaction or relevance.

---

## How Notes "Earn" Attention

### Positive Signals (Fight Decay)
- User opens note
- User edits note
- User pins / promotes
- AI detects relevance to recent notes
- Appears in Daily Recall and isn't dismissed

### Negative Signals (Accelerate Decay)
- Ignored when resurfaced
- Explicitly dismissed
- No semantic connections over time
- Marked "not relevant anymore"

This is **reinforcement learning**, not rules.

---

## Memory Strength Algorithm

```
memory_strength =
    resurfacing_score
  + engagement_score
  + semantic_reinforcement
  - decay_pressure(time)
```

| Memory Strength | State |
|-----------------|-------|
| > 3.5 | Fresh |
| 2.0 – 3.5 | Condensed |
| 1.0 – 2.0 | Faded |
| < 1.0 | Dormant |

This means:
- A resurfaced note that gets **ignored** still fades
- A rarely surfaced note that becomes **relevant** revives

---

## Daily Recall UX

This must feel **calm, not scary**.

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

No warnings. No drama.

---

## Recovery Is Always Possible (Trust Rule)

At any time, the user can:
- Search
- Restore full note
- Pin permanently
- Export

**Nothing is ever destroyed without consent.**

This preserves psychological safety.

---

## Core Features

### Multi-Modal Capture
- **Text** — Type with auto-save (600ms debounce)
- **Voice** — Record audio → automatic transcription
- **Camera** — Take photos → ML Kit OCR extracts text
- **Share** — Share from any app directly into Recall

### AI Processing Pipeline
When you save a note, it flows through:

```
Input (text/voice/image)
       ↓
   Transcription (audio → text)
       ↓
   OCR (image → text via ML Kit)
       ↓
   LLM Analysis (Claude)
       ↓
   Summary + Type + Topics + Entities + Action Items
       ↓
   Embeddings (vector for semantic search)
       ↓
   Stored in ai_metadata
```

### Intelligent Classification
Notes are auto-categorized as:
- **Task** — Action items detected
- **Idea** — Creative thoughts (boosted after 21 days for incubation)
- **Reference** — Information to look up later
- **Journal** — Personal reflections
- **Question** — Things to explore
- **Quote** — Words from others

### Smart Search
- Full-text search (Room FTS4)
- Semantic search via embeddings
- Filters: type, attachments, date range, archived

### Export
- TXT or Markdown format
- Includes all metadata, transcripts, OCR text
- Share directly to other apps

---

## Resurfacing Algorithm

Nightly job (2:30 AM) calculates score per note:

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

Top 3-7 notes shown daily. Dismissed notes fade faster.

---

## Tech Stack

### Android
- Kotlin + Jetpack Compose + Material 3
- MVVM + Clean Architecture + Repository Pattern
- Hilt (DI), Room (local DB), DataStore (preferences)
- WorkManager (background sync, AI processing, nightly jobs)
- OkHttp/Retrofit, Kotlinx Serialization
- ML Kit (on-device OCR)

### Backend
- Supabase (Auth, Postgres, Storage)
- PostgreSQL with Row Level Security
- pgvector for embeddings

### AI
- Anthropic Claude (Haiku) — classification, summarization
- Google ML Kit — OCR (on-device)
- Embeddings for semantic search

---

## Security

- **No hardcoded API keys** — loaded from `local.properties` via BuildConfig
- **Rate limiting** — Token bucket algorithm for all API calls
- **Input validation** — OWASP-compliant sanitization
- **Secure logging** — No PII or secrets logged

---

## Project Structure

```
app/
├── core/
│   ├── auth/           # Supabase authentication
│   ├── security/       # Rate limiter, input validator, config
│   ├── analytics/      # PostHog + Sentry
│   └── util/           # Extensions, constants
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Supabase DTOs
│   ├── repository/     # Repository implementations
│   └── mapper/         # Entity ↔ Domain mappers
├── domain/
│   ├── model/          # Note, Attachment, AiMetadata, etc.
│   ├── repository/     # Repository interfaces
│   └── usecase/        # CalculateResurfaceScore, SearchNotes
├── ai/
│   ├── client/         # AnthropicLlmClient, EmbeddingsClient
│   ├── config/         # AnthropicConfig
│   ├── processor/      # NoteProcessor
│   └── prompts/        # AiPrompts
├── sync/
│   ├── workers/        # Upload, Sync, ProcessAi, Resurface
│   └── SyncScheduler.kt
├── ui/
│   ├── screens/        # Home, Capture, NoteDetail, DailyRecall, Search, Settings
│   ├── components/     # Reusable Compose components
│   ├── theme/          # Colors, Typography, Theme
│   └── navigation/     # NavGraph, Screen definitions
└── di/                 # Hilt modules
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog+
- JDK 17+
- Android SDK 26+ (min), 34 (target)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Memory-App
   ```

2. **Configure API keys**
   ```bash
   cp local.properties.example local.properties
   # Edit local.properties with your keys:
   # SUPABASE_URL=https://your-project.supabase.co
   # SUPABASE_ANON_KEY=your-anon-key
   # ANTHROPIC_API_KEY=sk-ant-your-key
   ```

3. **Open in Android Studio & Sync Gradle**

4. **Run the app** (▶️ or Shift+F10)

---

## Development Status

| Milestone | Status |
|-----------|--------|
| A - Core Offline Notes | ✅ Complete |
| B - Attachments (Audio/Image/OCR) | ✅ Complete |
| C - Supabase Sync | ✅ Complete |
| D - AI Metadata Pipeline | ✅ Complete |
| E - Search & Daily Recall | ✅ Complete |
| F - Polish (Export, Settings, Analytics) | ✅ Complete |
| G - Adaptive Memory Decay | 🔄 In Design |

---

## The Vision

Most note apps are **filing cabinets** — you put things in, they stay exactly where you left them, and you forget they exist.

Recall is a **living memory** — it breathes, it forgets, it reminds. Ideas that matter float to the surface. Thoughts that served their purpose gracefully fade.

You don't manage your notes. **Your notes manage themselves.**

---

## One-Line Pitch

> **Recall**: Capture anything, let AI organize it, watch forgotten ideas resurface when they matter.

---

**Built with Kotlin & Jetpack Compose**
