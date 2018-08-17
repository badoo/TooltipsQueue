package com.badoo.tooltipsqueue

data class LowTooltip(
        override val priority: Int = 0,
        override val delayMillis: Long = 0L
) : Tooltip

data class MiddleTooltip(
        override val priority: Int = 1,
        override val delayMillis: Long = 0L
) : Tooltip

data class HighTooltip(
        override val priority: Int = 2,
        override val delayMillis: Long = 0L
) : Tooltip

fun isSameClass(expected: Tooltip, actual: Tooltip) = (expected::class.java == actual::class.java)