package com.badoo.tooltipsqueue

object DefaultStrategy : QueueStrategy {

    override fun isChild(parent: Tooltip, child: Tooltip) = false

    override fun putInQueueDelayMillis(hided: Tooltip) = 0L

}