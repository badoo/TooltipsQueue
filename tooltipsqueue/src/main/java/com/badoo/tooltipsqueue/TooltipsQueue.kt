package com.badoo.tooltipsqueue

import io.reactivex.Observable

interface TooltipsQueue {

    /**
     * Return tooltip that should be shown now.
     *
     * In case when you receive tooltip and can't show it(keyboard opened etc),
     * you should put this tooltip back inside queue with some delay, so queue will retry to show it later,
     * If you receive EmptyTooltip, you should hide current one(if you have such) and notify queue
     * with remove() method, so queue will post next Tooltip
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
     * Can be used for implementing functionality with hiding tooltips after press on back button
     * @return - true (with EmptyTooltip posting) if is showing tooltip now, false otherwise
     */
    fun onBackPressed(): Boolean

    /**
     * Start showing tooltips if queue is PAUSED, otherwise do nothing
     */
    fun start()

    /**
     * Stop queue and post EmptyTooltip to hide current if queue is RUNNING, otherwise do nothing
     */
    fun stop()

    /**
     * Clear all pending tooltips from queue
     */
    fun clear()

    /**
     * @return - all pending tooltips, that weren't shown.
     */
    fun pendingTooltips(): List<Tooltip>

}