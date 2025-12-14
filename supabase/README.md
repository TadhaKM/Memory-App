# Supabase Setup for Recall App

This directory contains the database schema and setup instructions for the Recall app backend.

## Prerequisites

1. Create a Supabase account at [supabase.com](https://supabase.com)
2. Create a new project

## Setup Steps

### 1. Run Database Schema

1. Open your Supabase project dashboard
2. Go to **SQL Editor**
3. Copy the contents of `schema.sql`
4. Paste and run in the SQL editor
5. Verify all tables were created successfully

### 2. Create Storage Bucket

1. Go to **Storage** in your Supabase dashboard
2. Click **New bucket**
3. Name: `recall-attachments`
4. Make it **Private** (not public)
5. Click **Create bucket**

### 3. Configure Storage Policies

Go to **Storage > Policies** and add these policies for `recall-attachments`:

**INSERT Policy** (Allow users to upload to their folder):
```sql
(bucket_id = 'recall-attachments' AND (storage.foldername(name))[1] = auth.uid()::text)
```

**SELECT Policy** (Allow users to read from their folder):
```sql
(bucket_id = 'recall-attachments' AND (storage.foldername(name))[1] = auth.uid()::text)
```

**DELETE Policy** (Allow users to delete from their folder):
```sql
(bucket_id = 'recall-attachments' AND (storage.foldername(name))[1] = auth.uid()::text)
```

### 4. Enable Email Authentication

1. Go to **Authentication > Providers**
2. Enable **Email** provider
3. Configure email templates (optional)
4. For production: Set up SMTP or use Supabase's default

### 5. Get API Credentials

1. Go to **Settings > API**
2. Copy your **Project URL**
3. Copy your **anon/public** key

### 6. Configure Android App

Update `SupabaseConfig.kt` with your credentials:

```kotlin
object SupabaseConfig {
    const val SUPABASE_URL = "https://your-project.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key-here"

    const val ATTACHMENTS_BUCKET = "recall-attachments"
}
```

**Security Note**: For production, store these in `local.properties` or use BuildConfig, never commit credentials!

## Database Schema Overview

### Tables

- **notes**: Main notes table with user_id, text, metadata
- **attachments**: Audio/image file references with storage paths
- **ai_metadata**: AI-generated summaries, topics, entities, action items
- **resurface**: Daily recall scoring data

### Row Level Security (RLS)

All tables have RLS enabled with policies ensuring users can only access their own data. The `user_id` column in each table references `auth.users(id)`.

### Indexes

Optimized indexes on:
- `user_id` for all tables
- `created_at` for notes (DESC for recent first)
- `ai_status` for filtering pending AI processing
- `score` for resurface algorithm

## Optional: pgvector for Semantic Search

To enable semantic search with embeddings:

1. In SQL Editor, run:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

2. The `ai_metadata.embedding` column is already set up as `VECTOR(1536)` for OpenAI ada-002 embeddings

3. Create vector index for performance:
   ```sql
   CREATE INDEX ON ai_metadata USING ivfflat (embedding vector_cosine_ops);
   ```

## Testing

### Create Test User

```sql
-- Sign up via the app, or create manually:
INSERT INTO auth.users (email, encrypted_password)
VALUES ('test@example.com', crypt('password123', gen_salt('bf')));
```

### Query Examples

```sql
-- Get all notes for a user
SELECT * FROM notes WHERE user_id = 'user-uuid';

-- Get notes with AI metadata
SELECT n.*, a.*
FROM notes n
LEFT JOIN ai_metadata a ON n.id = a.note_id
WHERE n.user_id = 'user-uuid';

-- Get top resurface candidates
SELECT n.*, r.score
FROM notes n
JOIN resurface r ON n.id = r.note_id
WHERE n.user_id = 'user-uuid'
  AND r.never_resurface = false
ORDER BY r.score DESC
LIMIT 5;
```

## Edge Functions (Coming Soon)

The `process-note` Edge Function will be added in Milestone D for:
- Audio transcription
- LLM classification and summarization
- Embeddings generation

## Backup & Restore

Supabase provides automatic daily backups for Pro plans. For manual backups:

```bash
# Using supabase CLI
supabase db dump -f backup.sql
```

## Monitoring

- **Dashboard > Database**: Monitor query performance
- **Dashboard > Logs**: View real-time logs for debugging
- **Dashboard > Usage**: Track API usage and storage

## Security Checklist

- ✅ RLS enabled on all tables
- ✅ Policies enforce user_id = auth.uid()
- ✅ Storage bucket is private
- ✅ Storage policies restrict to user folders
- ✅ Email auth configured
- ⚠️ Production: Enable 2FA for Supabase account
- ⚠️ Production: Set up custom SMTP
- ⚠️ Production: Configure password requirements

## Troubleshooting

**Issue**: RLS blocking queries
- Verify user is authenticated: `SELECT auth.uid();`
- Check policies match your query pattern

**Issue**: Storage upload failing
- Verify bucket exists and is private
- Check storage policies are correctly set
- Ensure path format: `userId/noteId/attachmentId.ext`

**Issue**: Can't create tables
- Check if extensions are enabled
- Verify you have database permissions

## Next Steps

After setup:
1. Test auth flow in the app
2. Create a test note and verify sync
3. Upload an attachment and check storage
4. Query tables to verify RLS is working

For questions, see [Supabase Docs](https://supabase.com/docs)
