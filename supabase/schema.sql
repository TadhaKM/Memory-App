-- Recall App Supabase Database Schema
-- Run this in your Supabase SQL editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgvector for embeddings (optional, for semantic search)
-- CREATE EXTENSION IF NOT EXISTS vector;

-- Notes table
CREATE TABLE IF NOT EXISTS notes (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    raw_text TEXT,
    source TEXT NOT NULL,
    archived BOOLEAN DEFAULT FALSE,
    pinned BOOLEAN DEFAULT FALSE,
    importance INT DEFAULT 0,
    ai_status TEXT DEFAULT 'pending',
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT notes_source_check CHECK (source IN ('MANUAL', 'SHARE', 'VOICE', 'CAMERA', 'IMPORT')),
    CONSTRAINT notes_ai_status_check CHECK (ai_status IN ('none', 'pending', 'done', 'failed'))
);

-- Attachments table
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY,
    note_id UUID REFERENCES notes(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    type TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    duration_ms BIGINT,
    size_bytes BIGINT NOT NULL,

    CONSTRAINT attachments_type_check CHECK (type IN ('AUDIO', 'IMAGE'))
);

-- AI Metadata table
CREATE TABLE IF NOT EXISTS ai_metadata (
    note_id UUID PRIMARY KEY REFERENCES notes(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    summary TEXT,
    type TEXT,
    topics JSONB,
    entities JSONB,
    action_items JSONB,
    transcript TEXT,
    ocr_text TEXT,
    embedding VECTOR(1536), -- If using pgvector
    processed_at BIGINT,

    CONSTRAINT ai_metadata_type_check CHECK (type IN ('TASK', 'IDEA', 'REFERENCE', 'JOURNAL', 'QUESTION', 'QUOTE', 'OTHER'))
);

-- Resurface state table
CREATE TABLE IF NOT EXISTS resurface (
    note_id UUID PRIMARY KEY REFERENCES notes(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    score DOUBLE PRECISION DEFAULT 0,
    last_shown_at BIGINT,
    never_resurface BOOLEAN DEFAULT FALSE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_created_at ON notes(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notes_ai_status ON notes(ai_status);
CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);
CREATE INDEX IF NOT EXISTS idx_attachments_user_id ON attachments(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_metadata_user_id ON ai_metadata(user_id);
CREATE INDEX IF NOT EXISTS idx_resurface_user_id ON resurface(user_id);
CREATE INDEX IF NOT EXISTS idx_resurface_score ON resurface(score DESC);

-- Row Level Security (RLS) Policies

-- Enable RLS on all tables
ALTER TABLE notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_metadata ENABLE ROW LEVEL SECURITY;
ALTER TABLE resurface ENABLE ROW LEVEL SECURITY;

-- Notes policies
CREATE POLICY "Users can view their own notes"
    ON notes FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own notes"
    ON notes FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own notes"
    ON notes FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own notes"
    ON notes FOR DELETE
    USING (auth.uid() = user_id);

-- Attachments policies
CREATE POLICY "Users can view their own attachments"
    ON attachments FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own attachments"
    ON attachments FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own attachments"
    ON attachments FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own attachments"
    ON attachments FOR DELETE
    USING (auth.uid() = user_id);

-- AI Metadata policies
CREATE POLICY "Users can view their own ai_metadata"
    ON ai_metadata FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own ai_metadata"
    ON ai_metadata FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own ai_metadata"
    ON ai_metadata FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own ai_metadata"
    ON ai_metadata FOR DELETE
    USING (auth.uid() = user_id);

-- Resurface policies
CREATE POLICY "Users can view their own resurface data"
    ON resurface FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own resurface data"
    ON resurface FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own resurface data"
    ON resurface FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own resurface data"
    ON resurface FOR DELETE
    USING (auth.uid() = user_id);

-- Storage bucket setup (run in Storage > Policies)
-- 1. Create bucket: recall-attachments
-- 2. Add policies:

-- Allow authenticated users to upload to their own folder
-- INSERT policy:
-- (bucket_id = 'recall-attachments' AND (storage.foldername(name))[1] = auth.uid()::text)

-- Allow authenticated users to read from their own folder
-- SELECT policy:
-- (bucket_id = 'recall-attachments' AND (storage.foldername(name))[1] = auth.uid()::text)

-- Allow authenticated users to delete from their own folder
-- DELETE policy:
-- (bucket_id = 'recall-attachments' AND (storage.foldername(name))[1] = auth.uid()::text)
