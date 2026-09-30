package com.ariadna.sudoku.core.model

/** Set of digits as a bitmask: bit `d - 1` means digit `d` is possible. */
@JvmInline
value class Candidates(val bits: Int) {

    val count: Int get() = bits.countOneBits()

    val isEmpty: Boolean get() = bits == 0

    operator fun contains(digit: Int): Boolean = bits and bit(digit) != 0

    /** The only digit in this set, or 0 if the set does not have exactly one digit. */
    val single: Int get() = if (count == 1) bits.countTrailingZeroBits() + 1 else 0

    operator fun plus(digit: Int): Candidates = Candidates(bits or bit(digit))

    operator fun minus(digit: Int): Candidates = Candidates(bits and bit(digit).inv())

    infix fun and(other: Candidates): Candidates = Candidates(bits and other.bits)

    infix fun or(other: Candidates): Candidates = Candidates(bits or other.bits)

    /** Calls [action] for each digit in ascending order. */
    inline fun forEach(action: (digit: Int) -> Unit) {
        var rest = bits
        while (rest != 0) {
            val low = rest and -rest
            action(low.countTrailingZeroBits() + 1)
            rest = rest xor low
        }
    }

    fun toList(): List<Int> = buildList { this@Candidates.forEach { add(it) } }

    override fun toString(): String = toList().joinToString(prefix = "{", postfix = "}")

    companion object {
        val NONE = Candidates(0)

        fun all(size: Int): Candidates = Candidates((1 shl size) - 1)

        fun of(vararg digits: Int): Candidates = Candidates(digits.fold(0) { acc, d -> acc or bit(d) })

        fun bit(digit: Int): Int = 1 shl (digit - 1)
    }
}
