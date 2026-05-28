package com.ringl.missedcallhook.util

/**
 * Phone number normalization utilities.
 * Converts any Indian phone format to 91XXXXXXXXXX (no +, no spaces, no dashes).
 */
object PhoneUtils {

    /**
     * Normalize to 91XXXXXXXXXX format.
     *
     * Handles:
     *   +91 98765 43210  →  919876543210
     *   091-9876543210   →  919876543210
     *   09876543210      →  919876543210
     *   9876543210       →  919876543210
     *   919876543210     →  919876543210 (no change)
     */
    fun normalize(number: String): String {
        // Strip everything except digits
        var clean = number.replace(Regex("[^0-9]"), "")

        // 091XXXXXXXXXX → 91XXXXXXXXXX
        if (clean.startsWith("091") && clean.length == 13) {
            clean = clean.substring(1)
        }

        // 0XXXXXXXXXX → XXXXXXXXXX (local trunk prefix)
        if (clean.startsWith("0") && clean.length == 11) {
            clean = clean.substring(1)
        }

        // 10 digits → add 91 country code
        if (clean.length == 10) {
            clean = "91$clean"
        }

        return clean
    }
}
