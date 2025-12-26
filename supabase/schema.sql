-- Recall App Database Schema for Supabase
-- Run this in Supabase SQL Editor

-- Enable pgvector extension for embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Notes table
CREATE TABLE IF NOT EXISTS notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    raw_text TEXT,
    source TEXT NOT NULL DEFAULT 'manual',
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    importance INTEGER NOT NULL DEFAULT 0,
    sync_state INTEGER NOT NULL DEFAULT 0,
    ai_state INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

-- Attachments table
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    local_uri TEXT,
    storage_path TEXT,
    duration_ms INTEGER,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sync_state INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- AI Metadata table
CREATE TABLE IF NOT EXISTS ai_metadata (
    note_id UUID PRIMARY KEY REFERENCES notes(id) ON DELETE CASCADE,
    summary TEXT,
    type TEXT,
    topics JSONB DEFAULT '[]'::jsonb,
    entities JSONB DEFAULT '[]'::jsonb,
    action_items JSONB DEFAULT '[]'::jsonb,
    transcript TEXT,
    ocr_text TEXT,
    embedding vector(1536),
    processed_at TIMESTAMPTZ
);

-- Resurface state table
CREATE TABLE IF NOT EXISTS resurface_state (
    note_id UUID PRIMARY KEY REFERENCES notes(id) ON DELETE CASCADE,
    score REAL NOT NULL DEFAULT 0,
    last_shown_at TIMESTAMPTZ,
    never_resurface BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_created_at ON notes(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);

-- Enable RLS
ALTER TABLE notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_metadata ENABLE ROW LEVEL SECURITY;
ALTER TABLE resurface_state ENABLE ROW LEVEL SECURITY;

-- Notes policies
CREATE POLICY "Users can access own notes" ON notes FOR ALL USING (auth.uid() = user_id);

-- Attachments policies
CREATE POLICY "Users can access own attachments" ON attachments FOR ALL USING (
    EXISTS (SELECT 1 FROM notes WHERE notes.id = attachments.note_id AND notes.user_id = auth.uid())
);

-- AI Metadata policies
CREATE POLICY "Users can access own ai_metadata" ON ai_metadata FOR ALL USING (
    EXISTS (SELECT 1 FROM notes WHERE notes.id = ai_metadata.note_id AND notes.user_id = auth.uid())
);

-- Resurface policies
CREATE POLICY "Users can access own resurface" ON resurface_state FOR ALL USING (
    EXISTS (SELECT 1 FROM notes WHERE notes.id = resurface_state.note_id AND notes.user_id = auth.uid())
);
