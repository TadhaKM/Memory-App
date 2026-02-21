package com.recall.app.core.security

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SECURITY: Comprehensive input validation and sanitization
 *
 * Protects against:
 * - Injection attacks (SQL, XSS, command injection)
 * - Buffer overflow / DoS via large inputs
 * - Malformed data causing crashes
 * - Data integrity issues
 *
 * OWASP Reference: A03:2021 - Injection
 * - Validate all user-supplied input
 * - Use allowlists over denylists
 * - Implement proper input length limits
 * - Sanitize data before storage/display
 */
@Singleton
class InputValidator @Inject constructor() {

    // ==========================================================================
    // Validation Limits (OWASP recommends explicit limits)
    // ==========================================================================
    object Limits {
        // Note content
        const val NOTE_TITLE_MAX_LENGTH = 500
        const val NOTE_CONTENT_MAX_LENGTH = 100_000 // ~100KB of text
        const val NOTE_RAW_TEXT_MAX_LENGTH = 50_000

        // User input
        const val EMAIL_MAX_LENGTH = 254 // RFC 5321
        const val PASSWORD_MIN_LENGTH = 8
        const val PASSWORD_MAX_LENGTH = 128
        const val SEARCH_QUERY_MAX_LENGTH = 500

        // AI/Metadata
        const val TAG_MAX_LENGTH = 50
        const val MAX_TAGS_PER_NOTE = 20
        const val SUMMARY_MAX_LENGTH = 500
        const val ENTITY_MAX_LENGTH = 100
        const val MAX_ENTITIES = 50

        // Attachments
        const val FILENAME_MAX_LENGTH = 255
        const val MAX_ATTACHMENT_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        const val MAX_ATTACHMENTS_PER_NOTE = 20

        // IDs
        const val UUID_LENGTH = 36
    }

    // ==========================================================================
    // Regex Patterns (Allowlists)
    // ==========================================================================
    private object Patterns {
        // RFC 5322 compliant email regex (simplified but robust)
        val EMAIL = Regex(
            "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$"
        )

        // UUID v4 format
        val UUID = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$"
        )

        // Safe filename (alphanumeric, dash, underscore, dot)
        val SAFE_FILENAME = Regex("^[a-zA-Z0-9._-]+\$")

        // Dangerous HTML/script patterns to detect
        val DANGEROUS_PATTERNS = listOf(
            Regex("<script[^>]*>", RegexOption.IGNORE_CASE),
            Regex("javascript:", RegexOption.IGNORE_CASE),
            Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE), // onclick, onerror, etc.
            Regex("data:text/html", RegexOption.IGNORE_CASE),
        )
    }

    // ==========================================================================
    // Email Validation
    // ==========================================================================

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()

        if (trimmed.isBlank()) {
            return ValidationResult.Invalid("Email is required")
        }

        if (trimmed.length > Limits.EMAIL_MAX_LENGTH) {
            return ValidationResult.Invalid("Email exceeds maximum length of ${Limits.EMAIL_MAX_LENGTH}")
        }

        if (!Patterns.EMAIL.matches(trimmed)) {
            return ValidationResult.Invalid("Invalid email format")
        }

        return ValidationResult.Valid(trimmed)
    }

    // ==========================================================================
    // Password Validation (OWASP ASVS compliant)
    // ==========================================================================

    fun validatePassword(password: String): ValidationResult {
        if (password.length < Limits.PASSWORD_MIN_LENGTH) {
            return ValidationResult.Invalid("Password must be at least ${Limits.PASSWORD_MIN_LENGTH} characters")
        }

        if (password.length > Limits.PASSWORD_MAX_LENGTH) {
            return ValidationResult.Invalid("Password exceeds maximum length of ${Limits.PASSWORD_MAX_LENGTH}")
        }

        // Check for common weak passwords (OWASP recommendation)
        val commonPasswords = setOf(
            "password", "12345678", "qwerty123", "letmein", "welcome",
            "monkey", "dragon", "master", "login", "passw0rd"
        )
        if (password.lowercase() in commonPasswords) {
            return ValidationResult.Invalid("Password is too common. Please choose a stronger password.")
        }

        return ValidationResult.Valid(password)
    }

    // ==========================================================================
    // Note Content Validation
    // ==========================================================================

    fun validateNoteContent(content: String?): ValidationResult {
        if (content == null) {
            return ValidationResult.Valid("")
        }

        if (content.length > Limits.NOTE_CONTENT_MAX_LENGTH) {
            return ValidationResult.Invalid(
                "Note content exceeds maximum length of ${Limits.NOTE_CONTENT_MAX_LENGTH} characters"
            )
        }

        // Sanitize but don't reject - notes may contain code snippets
        val sanitized = sanitizeText(content)
        return ValidationResult.Valid(sanitized)
    }

    fun validateNoteTitle(title: String?): ValidationResult {
        if (title == null) {
            return ValidationResult.Valid("")
        }

        if (title.length > Limits.NOTE_TITLE_MAX_LENGTH) {
            return ValidationResult.Invalid(
                "Title exceeds maximum length of ${Limits.NOTE_TITLE_MAX_LENGTH} characters"
            )
        }

        val sanitized = sanitizeText(title)
        return ValidationResult.Valid(sanitized)
    }

    // ==========================================================================
    // Search Query Validation
    // ==========================================================================

    fun validateSearchQuery(query: String): ValidationResult {
        val trimmed = query.trim()

        if (trimmed.length > Limits.SEARCH_QUERY_MAX_LENGTH) {
            return ValidationResult.Invalid(
                "Search query exceeds maximum length of ${Limits.SEARCH_QUERY_MAX_LENGTH}"
            )
        }

        // Remove potentially dangerous characters for Room FTS queries
        val sanitized = sanitizeSearchQuery(trimmed)
        return ValidationResult.Valid(sanitized)
    }

    // ==========================================================================
    // ID Validation
    // ==========================================================================

    fun validateUuid(id: String): ValidationResult {
        val trimmed = id.trim()

        if (!Patterns.UUID.matches(trimmed)) {
            Timber.w("Invalid UUID format: ${trimmed.take(20)}...")
            return ValidationResult.Invalid("Invalid ID format")
        }

        return ValidationResult.Valid(trimmed)
    }

    // ==========================================================================
    // Filename Validation
    // ==========================================================================

    fun validateFilename(filename: String): ValidationResult {
        val trimmed = filename.trim()

        if (trimmed.isBlank()) {
            return ValidationResult.Invalid("Filename is required")
        }

        if (trimmed.length > Limits.FILENAME_MAX_LENGTH) {
            return ValidationResult.Invalid(
                "Filename exceeds maximum length of ${Limits.FILENAME_MAX_LENGTH}"
            )
        }

        // Check for path traversal attempts
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            Timber.w("SECURITY: Path traversal attempt detected in filename: $trimmed")
            return ValidationResult.Invalid("Invalid filename")
        }

        return ValidationResult.Valid(trimmed)
    }

    // ==========================================================================
    // Tag Validation
    // ==========================================================================

    fun validateTag(tag: String): ValidationResult {
        val trimmed = tag.trim()

        if (trimmed.isBlank()) {
            return ValidationResult.Invalid("Tag cannot be empty")
        }

        if (trimmed.length > Limits.TAG_MAX_LENGTH) {
            return ValidationResult.Invalid(
                "Tag exceeds maximum length of ${Limits.TAG_MAX_LENGTH}"
            )
        }

        val sanitized = sanitizeText(trimmed)
        return ValidationResult.Valid(sanitized)
    }

    fun validateTags(tags: List<String>): ValidationResult {
        if (tags.size > Limits.MAX_TAGS_PER_NOTE) {
            return ValidationResult.Invalid(
                "Maximum ${Limits.MAX_TAGS_PER_NOTE} tags allowed per note"
            )
        }

        val validatedTags = mutableListOf<String>()
        for (tag in tags) {
            when (val result = validateTag(tag)) {
                is ValidationResult.Valid -> validatedTags.add(result.sanitizedValue)
                is ValidationResult.Invalid -> return result
            }
        }

        return ValidationResult.Valid(validatedTags.joinToString(","))
    }

    // ==========================================================================
    // Sanitization Functions
    // ==========================================================================

    /**
     * Sanitize general text input
     * - Removes null bytes
     * - Normalizes whitespace
     * - Removes control characters (except newlines/tabs)
     */
    fun sanitizeText(input: String): String {
        return input
            .replace("\u0000", "") // Remove null bytes
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "") // Remove control chars
            .trim()
    }

    /**
     * Sanitize search queries for Room FTS
     * - Escapes special FTS characters
     * - Removes dangerous patterns
     */
    private fun sanitizeSearchQuery(query: String): String {
        return query
            .replace("\"", "\"\"") // Escape quotes for FTS
            .replace("*", "") // Remove wildcards that could cause issues
            .replace("'", "''") // Escape single quotes
            .let { sanitizeText(it) }
    }

    /**
     * Check if content contains potentially dangerous patterns
     * Used for logging/alerting, not blocking (notes may contain code)
     */
    fun containsDangerousPatterns(content: String): Boolean {
        return Patterns.DANGEROUS_PATTERNS.any { it.containsMatchIn(content) }
    }

    /**
     * Sanitize for HTML display (if ever needed)
     * Escapes HTML special characters
     */
    fun sanitizeForHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}

/**
 * Result of input validation
 */
sealed class ValidationResult {
    data class Valid(val sanitizedValue: String) : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()

    fun isValid(): Boolean = this is Valid

    fun getValueOrNull(): String? = (this as? Valid)?.sanitizedValue

    fun getValueOrThrow(): String = when (this) {
        is Valid -> sanitizedValue
        is Invalid -> throw InputValidationException(errorMessage)
    }
}

/**
 * Exception thrown when input validation fails
 */
class InputValidationException(
    override val message: String
) : Exception(message)
