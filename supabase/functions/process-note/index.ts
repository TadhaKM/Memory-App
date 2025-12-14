// Supabase Edge Function: process-note
// Deploy with: supabase functions deploy process-note

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Get note_id from request
    const { note_id } = await req.json()

    if (!note_id) {
      throw new Error('note_id is required')
    }

    // Initialize Supabase client
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
    )

    // Get JWT from Authorization header
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')

    // Verify user
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser(token)
    if (userError || !user) {
      throw new Error('Unauthorized')
    }

    // Fetch note
    const { data: note, error: noteError } = await supabaseClient
      .from('notes')
      .select('*')
      .eq('id', note_id)
      .eq('user_id', user.id)
      .single()

    if (noteError) throw noteError

    // Fetch attachments
    const { data: attachments, error: attachmentsError } = await supabaseClient
      .from('attachments')
      .select('*')
      .eq('note_id', note_id)
      .eq('user_id', user.id)

    if (attachmentsError) throw attachmentsError

    // 1. TRANSCRIPTION (if audio attachments exist)
    let transcript = null
    const audioAttachments = attachments?.filter(a => a.type === 'AUDIO') || []

    if (audioAttachments.length > 0) {
      // TODO: Implement transcription
      // Option A: Use OpenAI Whisper API
      // Option B: Use AssemblyAI
      // Option C: Use Deepgram

      // Example with OpenAI Whisper:
      /*
      const transcripts = []
      for (const attachment of audioAttachments) {
        const audioUrl = supabaseClient.storage
          .from('recall-attachments')
          .getPublicUrl(attachment.storage_path).data.publicUrl

        const formData = new FormData()
        formData.append('file', await fetch(audioUrl).then(r => r.blob()))
        formData.append('model', 'whisper-1')

        const response = await fetch('https://api.openai.com/v1/audio/transcriptions', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${Deno.env.get('OPENAI_API_KEY')}`,
          },
          body: formData,
        })

        const result = await response.json()
        transcripts.push(result.text)
      }

      transcript = transcripts.join('\n\n')
      */

      transcript = "[Transcription not implemented - add your provider here]"
    }

    // 2. OCR (already done on device, just read from ai_metadata)
    const { data: existingMetadata } = await supabaseClient
      .from('ai_metadata')
      .select('ocr_text')
      .eq('note_id', note_id)
      .single()

    const ocrText = existingMetadata?.ocr_text

    // 3. COMBINE TEXT
    const combinedText = [note.raw_text, transcript, ocrText]
      .filter(Boolean)
      .join('\n\n')

    if (!combinedText) {
      throw new Error('No content to process')
    }

    // 4. LLM CLASSIFICATION & SUMMARIZATION
    // TODO: Implement with your preferred LLM provider
    // Option A: OpenAI GPT-4
    // Option B: Anthropic Claude
    // Option C: Google Gemini

    // Example with OpenAI:
    /*
    const llmResponse = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${Deno.env.get('OPENAI_API_KEY')}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `You are an AI assistant that analyzes personal notes and returns ONLY valid JSON...` // Use AiPrompts.SYSTEM_PROMPT
          },
          {
            role: 'user',
            content: `Analyze this note and return the JSON:\n\nNote content:\n${combinedText}\n\nReturn the JSON object now:`
          }
        ],
        temperature: 0.3,
        max_tokens: 500,
      }),
    })

    const llmData = await llmResponse.json()
    const aiResult = JSON.parse(llmData.choices[0].message.content)
    */

    // Placeholder AI result (replace with actual LLM call)
    const aiResult = {
      summary: combinedText.substring(0, 240),
      type: 'other',
      topics: [],
      entities: [],
      action_items: []
    }

    // 5. GENERATE EMBEDDINGS
    // TODO: Implement embeddings generation
    // Option A: OpenAI ada-002
    // Option B: Cohere embed
    // Option C: Sentence Transformers

    // Example with OpenAI:
    /*
    const embeddingText = `Summary: ${aiResult.summary}\n\nText: ${combinedText}`

    const embeddingResponse = await fetch('https://api.openai.com/v1/embeddings', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${Deno.env.get('OPENAI_API_KEY')}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: 'text-embedding-ada-002',
        input: embeddingText,
      }),
    })

    const embeddingData = await embeddingResponse.json()
    const embedding = embeddingData.data[0].embedding
    */

    const embedding = null // Replace with actual embedding

    // 6. SAVE AI METADATA
    const { error: metadataError } = await supabaseClient
      .from('ai_metadata')
      .upsert({
        note_id: note_id,
        user_id: user.id,
        summary: aiResult.summary,
        type: aiResult.type,
        topics: aiResult.topics,
        entities: aiResult.entities,
        action_items: aiResult.action_items,
        transcript: transcript,
        ocr_text: ocrText,
        embedding: embedding,
        processed_at: Date.now(),
      })

    if (metadataError) throw metadataError

    // 7. UPDATE NOTE AI STATUS
    const { error: updateError } = await supabaseClient
      .from('notes')
      .update({ ai_status: 'done' })
      .eq('id', note_id)

    if (updateError) throw updateError

    return new Response(
      JSON.stringify({
        ok: true,
        status: 'processed',
        note_id: note_id,
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200,
      },
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      },
    )
  }
})

// Environment variables needed:
// - SUPABASE_URL (auto-provided)
// - SUPABASE_SERVICE_ROLE_KEY (auto-provided)
// - OPENAI_API_KEY (or your chosen LLM provider key)
// - Add via: supabase secrets set OPENAI_API_KEY=your-key
