package com.badoo.tooltipsqueue

import io.reactivex.observers.TestObserver
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TooltipsQueueImplStrategiesTest {

    private lateinit var queue: TooltipsQueue
    private lateinit var allTooltips: TestObserver<Tooltip>

    @Test
    fun afterAddParent_childWillNotBeShown() {
        initQueue(childStrategy)
        queue.add(LowTooltip())
        queue.add(MiddleTooltip())
        queue.start()
        queue.remove()

        allTooltips.assertNever { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun afterAddParentWhenShowingChild_childWillNotBeAddedToQueue() {
        val afterAddParent = TestObserver<Tooltip>()
        initQueue(childStrategy)
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())
        queue.remove()
        queue.onShow().subscribe(afterAddParent)
        queue.remove()

        afterAddParent.assertNever { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun whenStrategyIsNotToPutBack_lowPriorityWillNotBeAddedBackToQueue() {
        val afterAddHigh = TestObserver<Tooltip>()
        initQueue(createPutInQueueStrategy(null))
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())
        queue.remove()
        queue.onShow().subscribe(afterAddHigh)
        queue.remove()

        afterAddHigh.assertNever { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun whenStrategyIsPutBackWithDelayAndTimeNotPassed_tooltipWillNotBePosted() {
        val afterAddHigh = TestObserver<Tooltip>()
        initQueue(createPutInQueueStrategy(1000))
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())
        queue.remove()
        queue.onShow().subscribe(afterAddHigh)
        queue.remove()
        Robolectric.getForegroundThreadScheduler().advanceBy(999, TimeUnit.MILLISECONDS)

        afterAddHigh.assertNever { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun whenStrategyIsPutBackWithDelayAndTimeNotPassed_tooltipWillBePosted() {
        initQueue(createPutInQueueStrategy(1000))
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())
        queue.remove()
        queue.remove()
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)

        allTooltips.assertValueAt(5) { isSameClass(LowTooltip(), it) }
    }

    private fun initQueue(strategy: QueueStrategy) {
        queue = TooltipsQueueImpl(strategy)
        allTooltips = TestObserver()
        queue.onShow().subscribe(allTooltips)
    }

    private val childStrategy = object : QueueStrategy {
        override fun isChild(parent: Tooltip, child: Tooltip): Boolean {
            return (parent is MiddleTooltip && child is LowTooltip)
        }
        override fun putInQueueDelayMillis(hided: Tooltip) = 0L
    }

    private fun createPutInQueueStrategy(reputDelayMillis: Long?) =
            object : QueueStrategy {
                override fun isChild(parent: Tooltip, child: Tooltip) = false
                override fun putInQueueDelayMillis(hided: Tooltip) = reputDelayMillis
            }

}