package com.casero.cu.model

/** Cuban identity card number (carné de identidad, CI): exactly 11 digits. */
object CubanId {
    const val LENGTH = 11

    fun isValid(value: String): Boolean = value.length == LENGTH && value.all { it in '0'..'9' }

    /** What a CI field keeps from typed or pasted text: digits only, at most 11. */
    fun sanitize(input: String): String = input.filter { it in '0'..'9' }.take(LENGTH)
}
