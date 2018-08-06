package com.badoo.tooltipsqueue

interface QueueStrategy {

    /**
     * After add to queue parent tooltip, we can want remove all childs
     * For example you have tooltips "goal_in_progress" inside queue,
     * but accidentally you received goal achieved event. In such situation
     * you don't want to show "goal_in_progress" and want to remove it
     */
    fun isChild(parent: Tooltip, child: Tooltip): Boolean

    /**
     * You are showing tooltip right now, and getting more high prioritized
     * Current will be hidden, and more high prioritized shown.
     * Based on this method current can be added to queue and shown later one more time.
     *
     * @return - null, if you don't want to put tooltip back in queue
     */
    fun putInQueueDelayMillis(hided: Tooltip): Long?

}