package com.badoo.tooltipsqueue

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.reactivex.Observable
import java.util.PriorityQueue
import java.util.Queue
import kotlin.Boolean
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.let
import io.reactivex.subjects.BehaviorSubject

class PriorityTooltipsQueue(private val strategy: QueueStrategy = DefaultStrategy) : TooltipsQueue {

    /*
     * Inside this subject always hidden exact state of tooltip on the screen
     * Tooltip shown -> value != EmptyTooltip
     * Tooltip hidden -> value == EmptyTooltip
     */
    private val behaviorSubject: BehaviorSubject<Tooltip> = BehaviorSubject.createDefault(EmptyTooltip)
    private var queue: Queue<TooltipParams> = PriorityQueue(1, Comparator { first, second ->
        second.type.priority - first.type.priority
    })

    private var removingLowPriority = false
    private var queueState = QueueState.PAUSED
    private val timerHandler = Handler()
    private val showRunnable = Runnable {
        val tooltip = queue.poll()
        if (tooltip != null) {
            behaviorSubject.onNext(tooltip.type)
        } else {
            Log.e(this@PriorityTooltipsQueue.javaClass.simpleName,
                    "Shouldn't occur. Post issue how to achieve.")
        }
    }

    override fun onShow(): Observable<Tooltip> = behaviorSubject.distinctUntilChanged()

    override fun add(vararg tooltips: Tooltip) {
        onMainThread()
        tooltips.forEach { add(it) }
    }

    override fun remove(tooltip: Class<out Tooltip>?) {
        onMainThread()
        validateTooltip(tooltip)
        val removingHead = queue.isNotEmpty() && tooltip == queue.peek().type::class.java
        tooltip?.let { clazz ->
            queue.removeAll { it.type::class.java == clazz }
        }
        val current = behaviorSubject.valueNonNull
        if (tooltip == null || current::class.java == tooltip) {
            removingLowPriority = false
        }
        if (tooltip == null || current::class.java == tooltip || removingHead) {
            behaviorSubject.onNext(EmptyTooltip)
            startTimer()
        }
    }

    override fun onBackPressedHandled(): Boolean {
        onMainThread()
        val isShowing = behaviorSubject.valueNonNull != EmptyTooltip
        if (isShowing) {
            behaviorSubject.onNext(EmptyTooltip)
        }
        return isShowing
    }

    override fun start() {
        onMainThread()
        if (queueState != QueueState.RUNNING) {
            queueState = QueueState.RUNNING
            startTimer()
        }
    }

    override fun stop() {
        onMainThread()
        if (queueState != QueueState.PAUSED) {
            queueState = QueueState.PAUSED
            timerHandler.removeCallbacks(showRunnable)
            behaviorSubject.onNext(EmptyTooltip)
        }
    }

    override fun clear() {
        onMainThread()
        timerHandler.removeCallbacks(showRunnable)
        queue.clear()
    }

    override fun pendingTooltips() = ArrayList<Tooltip>(queue.map { it.type })

    private fun needToIgnoreAdded(tooltip: Tooltip): Boolean {
        // If we have such tooltip in queue or subject -> ignoring it
        val showing = behaviorSubject.valueNonNull
        if (showing != EmptyTooltip && showing.javaClass == tooltip.javaClass) {
            return true
        }
        return queue.any { it.type::class.java == tooltip::class.java }
    }

    private fun removeChildTooltips(parent: Tooltip) {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val child = iterator.next()
            if (strategy.isChild(parent, child.type)) {
                iterator.remove()
            }
        }
    }

    private fun add(tooltip: Tooltip) {
        if (needToIgnoreAdded(tooltip)) {
            return
        }
        removeChildTooltips(tooltip)
        if (queue.isEmpty() || queue.peek().type.priority < tooltip.priority) {
            // priority is higher than first elem, or queue is empty -> check showing
            compareWithShowing(tooltip)
        } else {
            // Queue is not empty and priority less than first -> add to queue
            queue.add(TooltipParams(tooltip, tooltip.delayMillis))
        }
    }

    private fun compareWithShowing(added: Tooltip) {
        val showing = behaviorSubject.valueNonNull
        if (showing.priority <= added.priority && !removingLowPriority) {
            queue.add(TooltipParams(added, added.delayMillis))
            val reputDelay = strategy.putInQueueDelayMillis(showing)
            if (showing != EmptyTooltip
                    && showing::class.java != added::class.java
                    && reputDelay != null
                    && !strategy.isChild(added, showing)) {
                queue.add(TooltipParams(showing, reputDelay))
            }
            if (showing != EmptyTooltip) {
                // If we are showing something, we should notify clients to hide current
                removingLowPriority = true
                behaviorSubject.onNext(EmptyTooltip)
            } else {
                startTimer()
            }
        } else {
            // Showing priority is higher
            queue.add(TooltipParams(added, added.delayMillis))
        }
    }

    private fun startTimer() {
        timerHandler.removeCallbacks(showRunnable)
        if (queueState == QueueState.RUNNING && queue.isNotEmpty()) {
            timerHandler.postDelayed(showRunnable, queue.first().delayMillis)
        }
    }

    private fun onMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            throw IllegalThreadStateException("Method should be called on main thread!")
        }
    }

    private fun validateTooltip(tooltip: Class<out Tooltip>?) {
        if (tooltip == EmptyTooltip::class.java) {
            throw IllegalArgumentException("Posting EmptyTooltip can possible break queue. Don't do this")
        }
    }

    private val <T> BehaviorSubject<T>.valueNonNull: T
        get() = this.value!!

    private enum class QueueState {
        PAUSED, RUNNING
    }

    private class TooltipParams(val type: Tooltip, val delayMillis: Long)

}