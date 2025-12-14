# Supabase Edge Functions for Recall

This directory contains Edge Functions that run serverless processing for the Recall app.

## Functions

### process-note

Processes a note with AI to generate:
- Transcription (from audio attachments)
- Classification (task/idea/reference/etc)
- Summary (max 240 chars)
- Topics extraction (max 5)
- Entities extraction (max 8)
- Action items
- Embeddings (vector for semantic search)

**Endpoint**: `POST /functions/v1/process-note`

**Request Body**:
```json
{
  "note_id": "uuid-of-note"
}
```

**Response**:
```json
{
  "ok": true,
  "status": "processed",
  "note_id": "uuid-of-note"
}
```

## Setup

### 1. Install Supabase CLI

```bash
npm install -g supabase
```

### 2. Link to Your Project

```bash
supabase link --project-ref your-project-ref
```

### 3. Set Environment Variables

```bash
supabase secrets set OPENAI_API_KEY=sk-your-key-here
# Or for other providers:
supabase secrets set ANTHROPIC_API_KEY=your-key
supabase secrets set ASSEMBLYAI_API_KEY=your-key
```

### 4. Deploy Function

```bash
supabase functions deploy process-note
```

### 5. Test Function

```bash
curl -L -X POST \
  'https://your-project.supabase.co/functions/v1/process-note' \
  -H 'Authorization: Bearer YOUR_ANON_KEY' \
  -H 'Content-Type: application/json' \
  --data '{"note_id":"test-uuid"}'
```

## AI Provider Setup

The template function supports multiple AI providers. Choose one and implement:

### Option A: OpenAI

```typescript
// LLM: GPT-4 for classification
const llmResponse = await fetch('https://api.openai.com/v1/chat/completions', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${Deno.env.get('OPENAI_API_KEY')}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    model: 'gpt-4-turbo-preview',
    messages: [/* system and user prompts */],
    temperature: 0.3,
  }),
})

// Transcription: Whisper
const transcriptResponse = await fetch('https://api.openai.com/v1/audio/transcriptions', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${Deno.env.get('OPENAI_API_KEY')}`,
  },
  body: formData, // audio file
})

// Embeddings: ada-002
const embeddingResponse = await fetch('https://api.openai.com/v1/embeddings', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${Deno.env.get('OPENAI_API_KEY')}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    model: 'text-embedding-ada-002',
    input: text,
  }),
})
```

### Option B: Anthropic Claude

```typescript
// LLM: Claude for classification
const llmResponse = await fetch('https://api.anthropic.com/v1/messages', {
  method: 'POST',
  headers: {
    'x-api-key': Deno.env.get('ANTHROPIC_API_KEY'),
    'anthropic-version': '2023-06-01',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    model: 'claude-3-sonnet-20240229',
    max_tokens: 500,
    messages: [{ role: 'user', content: prompt }],
  }),
})
```

### Option C: Mix & Match

Use different providers for different tasks:
- **LLM**: Anthropic Claude (best quality)
- **Transcription**: AssemblyAI (specialized)
- **Embeddings**: Cohere (fast)

## Processing Flow

```
1. Receive note_id from Android app
2. Fetch note + attachments from Supabase
3. If audio → Transcribe with Whisper/AssemblyAI
4. Combine: rawText + transcript + ocrText
5. Send to LLM for classification & summarization
6. Generate embeddings
7. Save to ai_metadata table
8. Update note.ai_status = 'done'
9. Return success
```

## Cost Optimization

- **Batch processing**: Process multiple notes at once
- **Caching**: Cache common phrases/entities
- **Model selection**: Use smaller models when possible
- **Retry logic**: Exponential backoff on failures

## Monitoring

View logs:
```bash
supabase functions logs process-note
```

Monitor invocations:
- Supabase Dashboard → Edge Functions → process-note
- Check success rate and execution time

## Security

- ✅ Verifies user authentication (JWT)
- ✅ Checks user owns the note (user_id match)
- ✅ RLS policies enforced on all queries
- ✅ API keys stored as secrets (not in code)
- ✅ CORS configured properly

## Local Development

Run locally for testing:

```bash
supabase functions serve process-note --env-file .env
```

Create `.env` file:
```
OPENAI_API_KEY=sk-your-key
ANTHROPIC_API_KEY=your-key
```

## Next Steps

1. Choose your AI providers
2. Implement the TODO sections in `index.ts`
3. Test locally with sample data
4. Deploy to production
5. Monitor costs and performance
6. Optimize based on usage patterns

## Alternative: Client-Side Processing

For privacy-focused users or offline capability, you can also process AI locally on Android:
- Use on-device LLMs (Gemini Nano, LLaMA)
- Edge Impulse for embeddings
- MediaPipe for entity extraction

The architecture supports both server and client-side processing!
