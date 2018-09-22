package com.badoo.tooltips

import com.badoo.tooltipsqueue.Tooltip
import java.io.Serializable

interface SerializableTooltip: Tooltip, Serializable

class HighPriorityTooltip : SerializableTooltip {
    override val priority = 2
    override val delayMillis = 1000L
}

class MoneyTooltip(val amount: Int, override val delayMillis: Long) : SerializableTooltip {
    override val priority = 1
}

class LowPriorityTooltip : SerializableTooltip {
    override val priority = 0
    override val delayMillis = 1000L
}