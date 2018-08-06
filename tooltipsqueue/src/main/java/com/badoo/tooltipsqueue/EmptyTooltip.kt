package com.badoo.tooltipsqueue

/**
 * In reality it is not tooltip, but special object, that notify developer to hide showing
 * But with such approach contract is more simple and friendly for using
 */
object EmptyTooltip : Tooltip {

    override val priority: Int
        get() = Int.MIN_VALUE

    override val delayMillis: Long
        get() = 0L

}