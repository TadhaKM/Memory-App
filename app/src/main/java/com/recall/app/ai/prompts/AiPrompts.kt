package com.recall.app.ai.prompts

object AiPrompts {

    const val SYSTEM_PROMPT = """You are an AI assistant that analyzes personal notes and returns ONLY valid JSON.
You must return a JSON object that matches this exact schema. Do not include any markdown formatting, explanations, or commentary.

Schema:
{
  "summary": "string (max 240 characters)",
  "type": "task|idea|reference|journal|question|quote|other",
  "topics": ["Title Case Topic", "Up to 5 topics"],
  "entities": ["People/Organizations/Places/Products", "Up to 8 entities"],
  "action_items": [
    { "text": "actionable item", "due_hint": "optional date/time hint or null" }
  ]
}

Rules:
- summary: Concise summary in 240 chars or less
- type: Choose ONE that best fits the content
- topics: Extract main themes, title case, max 5, dedupe
- entities: Extract proper nouns (people, orgs, places, products), max 8, dedupe
- action_items: ONLY extract if there are clear, actionable tasks. Empty array if none.
- due_hint: Only include if there's a time reference (e.g., "tomorrow", "next week", "by Friday")
- Return ONLY the JSON object, no other text
"""

    fun createUserPrompt(content: String): String {
        return """Analyze this note and return the JSON:

Note content:
$content

Return the JSON object now:"""
    }

    fun createEmbeddingText(
        rawText: String?,
        transcript: String?,
        ocrText: String?,
        summary: String?
    ): String {
        val parts = mutableListOf<String>()

        summary?.let { parts.add("Summary: $it") }
        rawText?.let { parts.add("Text: $it") }
        transcript?.let { parts.add("Transcript: $it") }
        ocrText?.let { parts.add("OCR: $it") }

        return parts.joinToString("\n\n")
    }
}
