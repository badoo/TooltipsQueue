package com.badoo.tooltipsqueue

import io.reactivex.Observable

interface TooltipsQueue {

    /**
     * Return tooltip that should be shown now.
     *
     * In case when you receive tooltip and can't show it(keyboard opened etc),
     * you should put this tooltip back inside queue with some delay, so queue will retry to show it later
     */
    fun onShow(): Observable<Tooltip>

    /**
     * Adding tooltips inside queue, doesn't matter if queue is started or not
     */
    fun add(vararg tooltips: Tooltip)

    /**
     * This method notifies queue, that current tooltip is processed.
     * Only after calling it, queue will start timer for next one.
     * You can also remove tooltips from the middle of the queue.
     *
     * When you just want to notify that you processed shown you can skip passing parameter
     */
    fun remove(tooltip: Class<out Tooltip>? = null)

    /**
     * If currently you have tooltip on this screen, EmptyTooltip will be posted
     * So it can help with implementing functionality with hiding tooltips after press on back button
     *
     * state of queue can be changed
     * @return - true if is showing tooltip now, false otherwise
     */
    fun onBackPressedHandled(): Boolean

    /**
     * Starting showing tooltips
     */
    fun start()

    /**
     * Stopping queue and posting EmptyTooltip to hide current
     */
    fun stop()

    /**
     * Clear all pending tooltips from queue
     */
    fun clear()

    /**
     * Return all pending tooltips, that weren't shown.
     * Useful to put pending tooltips in bundle in case of activity destroy
     *
     * doesn't change state of queue
     */
    fun queueState(): List<Tooltip>

}