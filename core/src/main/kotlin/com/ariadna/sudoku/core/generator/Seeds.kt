package com.ariadna.sudoku.core.generator

/** SplitMix64 step: derives the next well-mixed seed from [x]. Used to chain generation attempts. */
internal fun splitMix64(x: Long): Long {
    var z = x + -0x61c8864680b583ebL                    // 0x9E3779B97F4A7C15 (golden-ratio gamma)
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L      // 0xBF58476D1CE4E5B9
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L      // 0x94D049BB133111EB
    return z xor (z ushr 31)
}
