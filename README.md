# Recall - Personal Memory Assistant

An Android-first, offline-first note-taking app with AI-powered organization and daily resurfacing.

## 🎯 Core Features

- **Instant Capture**: Text, voice, image, and share-in support
- **Offline-First**: All data stored locally first, synced in background
- **AI Pipeline**: Automatic transcription, OCR, classification, summarization, and embeddings
- **Smart Search**: Keyword + semantic search with filters
- **Daily Recall**: Intelligent resurfacing of relevant notes

## 🏗️ Architecture

### Tech Stack

**Android**
- Kotlin
- Jetpack Compose (UI)
- Navigation Compose
- Material 3
- Hilt (Dependency Injection)
- Room (Local Database)
- DataStore (Preferences)
- WorkManager (Background sync & processing)
- OkHttp/Retrofit (Networking)
- Kotlinx Serialization

**Backend**
- Supabase (Auth, Postgres, Storage, Edge Functions)
- PostgreSQL with RLS (Row Level Security)

**AI Providers**
- LLM: Summarization & classification
- Embeddings: Vector search
- Transcription: Audio → text
- OCR: ML Kit Text Recognition (on-device)

### Project Structure

```
app/
├── core/                   # Core utilities and shared code
│   ├── auth/              # Authentication (placeholder)
│   ├── network/           # Network configuration
│   └── util/              # Extensions, constants, result wrapper
├── data/                  # Data layer
│   ├── local/             # Room database
│   │   ├── dao/          # Data Access Objects
│   │   ├── entity/       # Room entities
│   │   └── RecallDatabase.kt
│   ├── remote/            # Supabase integration (to be implemented)
│   ├── repository/        # Repository implementations
│   └── mapper/            # Entity ↔ Domain mappers
├── domain/                # Business logic layer
│   ├── model/            # Domain models
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Use cases (to be implemented)
├── ai/                    # AI integration (to be implemented)
│   ├── prompts/          # LLM prompts
│   └── client/           # AI provider clients
├── sync/                  # Background sync (to be implemented)
│   ├── workers/          # WorkManager workers
│   └── scheduler/        # Sync scheduling
├── ui/                    # Presentation layer
│   ├── navigation/       # Navigation graph
│   ├── screens/          # Compose screens
│   │   ├── home/         # Notes list
│   │   └── notedetail/   # Note editing
│   ├── components/       # Reusable UI components
│   ├── theme/            # Material 3 theme
│   └── viewmodel/        # ViewModels
└── di/                    # Hilt modules
```

## 📦 Database Schema

### Room (Local)

**NoteEntity**
- id, userId, createdAt, updatedAt
- rawText, source (manual/share/voice/camera)
- archived, pinned, importance
- syncState (LOCAL_ONLY/SYNCED/DIRTY/DELETED)
- aiState (NONE/PENDING/DONE/FAILED)

**AttachmentEntity**
- id, noteId, type (audio/image)
- localUri, storagePath, mimeType
- durationMs, sizeBytes
- syncState

**AiMetadataEntity**
- noteId, summary, type (task/idea/reference/journal/question/quote/other)
- topics, entities, actionItems (JSON)
- transcript, ocrText, embedding
- processedAt

**ResurfaceStateEntity**
- noteId, score, lastShownAt
- neverResurface

**NotesFts** (FTS4 table for full-text search)

### Supabase (Cloud)

Similar schema with additional:
- user_id references auth.users
- RLS policies for multi-tenancy
- pgvector extension for embeddings

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 26+ (minimum), 34 (target)
- Supabase account (for backend features)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Memory-App
   ```

2. **Open in Android Studio**
   - File → Open → Select `Memory-App` folder

3. **Sync Gradle**
   - Wait for Gradle sync to complete
   - Download dependencies

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click Run (▶️) or press Shift+F10

### Configuration (Optional)

Create `local.properties` for Supabase configuration:
```properties
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key
```

## 🛣️ Development Roadmap

### ✅ Milestone A - Core Offline Notes (COMPLETED)
- [x] Project setup with Gradle and dependencies
- [x] Room database with entities and DAOs
- [x] Hilt dependency injection
- [x] Domain models and repository pattern
- [x] Navigation Compose
- [x] Home screen (notes list)
- [x] Note detail screen (editing)

### ✅ Milestone B - Attachments (COMPLETED)
- [x] Audio recording with MediaRecorder
- [x] Image capture and import
- [x] Attachment viewer
- [x] ML Kit OCR for images

### ✅ Milestone C - Supabase Sync (COMPLETED)
- [x] Supabase client setup
- [x] Authentication (email/magic link)
- [x] Storage upload for attachments
- [x] Notes sync (upsert)
- [x] WorkManager sync chain

### ✅ Milestone D - AI Metadata (COMPLETED)
- [x] Edge Function: process-note
- [x] LLM classification & summarization
- [x] Transcription integration
- [x] Embeddings generation
- [x] AI metadata UI

### ✅ Milestone E - Search & Recall (COMPLETED)
- [x] Room FTS search with filters
- [x] Semantic search (vector similarity)
- [x] Daily Recall algorithm
- [x] Nightly resurfacing worker
- [x] Notification scheduling

### ✅ Milestone F - Polish & Release (COMPLETED)
- [x] Export (txt/markdown)
- [x] Share-in functionality
- [x] Settings screen
- [x] Crash reporting (Sentry)
- [x] Analytics (PostHog)
- [ ] Store listing & screenshots (pending)

## 📝 Key Implementation Details

### Offline-First Data Flow

1. User captures note → **Saved to Room immediately** (status: LOCAL_ONLY)
2. Worker uploads attachments → Syncs note → status: SYNCED_PENDING_AI
3. Worker triggers AI processing → status: AI_DONE or AI_FAILED
4. Search reads from Room; cloud is for sync + cross-device

### AI Processing Pipeline

```
Input: rawText + transcript + ocrText
  ↓
LLM Classification & Summarization
  ↓
Output: {summary, type, topics, entities, actionItems}
  ↓
Embeddings Generation
  ↓
Store in ai_metadata table
```

### Resurfacing Algorithm

Nightly job computes score per note:
- Base: `random(0..1)`
- Modifiers:
  - +2.0 if pinned
  - +1.5 if task with unfinished action items
  - +1.0 if idea older than 21 days
  - +1.0 if importance ≥ 2
  - -5.0 if never_resurface
  - -1.0 if shown in last 3 days

Select top N (3-7) with diversity constraints.

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## 📄 License

[Your License Here]

## 🤝 Contributing

This is an MVP project. Contributions welcome after initial release.

## 📞 Support

For issues and feature requests, please use the GitHub issue tracker.

---

**Built with ❤️ using Kotlin & Jetpack Compose**
