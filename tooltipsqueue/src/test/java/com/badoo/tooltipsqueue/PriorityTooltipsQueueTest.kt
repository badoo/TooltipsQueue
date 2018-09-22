package com.badoo.tooltipsqueue

import io.reactivex.observers.TestObserver
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PriorityTooltipsQueueTest {

    private lateinit var queue: TooltipsQueue
    private lateinit var allTooltips: TestObserver<Tooltip>

    @Before
    fun setup() {
        queue = PriorityTooltipsQueue()
        allTooltips = TestObserver()
        queue.onShow().subscribe(allTooltips)
    }

    @Test
    fun whenNoTooltipsAdded_onlyOnePosted() {
        queue.start()

        allTooltips.assertValueCount(1)
    }

    @Test
    fun whenNoTooltipsAdded_onlyEmptyPosted() {
        queue.start()

        allTooltips.assertValueAt(0) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun whenLowTooltipAdded_lowTooltipAfterEmptyPosted() {
        queue.add(LowTooltip())
        queue.start()

        allTooltips.assertValueAt(1) { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun whenRemoveNotCalled_onlyOneAfterEmptyPosted() {
        queue.add(LowTooltip(), MiddleTooltip())
        queue.start()

        allTooltips.assertValueCount(2)
    }

    @Test
    fun whenRemoveCalled_emptyTooltipPosted() {
        queue.add(LowTooltip())
        queue.start()
        queue.remove()

        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun whenRemoveCalledAndQueueHaveTooltips_afterEmptyWillBePostedOneMore() {
        queue.add(LowTooltip(), MiddleTooltip())
        queue.start()
        queue.remove()

        allTooltips.assertValueCount(4)
    }

    @Test
    fun whenMiddleAddedLater_itWillBePostedBeforeLow() {
        queue.add(LowTooltip(), MiddleTooltip())
        queue.start()
        queue.remove()

        allTooltips.assertValueAt(1) { isSameClass(it, MiddleTooltip()) }
        allTooltips.assertValueAt(3) { isSameClass(it, LowTooltip()) }
    }

    @Test
    fun whenMiddleAddedEarlier_itWillBePostedBeforeLow() {
        queue.add(MiddleTooltip(), LowTooltip())
        queue.start()
        queue.remove()

        allTooltips.assertValueAt(1) { isSameClass(it, MiddleTooltip()) }
        allTooltips.assertValueAt(3) { isSameClass(it, LowTooltip()) }
    }

    @Test
    fun whenAddedSameTooltip_onlyOneWillBePosted() {
        queue.add(LowTooltip(), LowTooltip())
        queue.start()

        allTooltips.assertValueCount(2)
    }

    @Test
    fun whenAddedSameTooltipDuringShowing_itWillNotBePosted() {
        queue.add(LowTooltip())
        queue.start()
        queue.add(LowTooltip())

        allTooltips.assertValueCount(2)
    }

    @Test
    fun whenAddedSameTooltipAfterHide_itWillBePosted() {
        queue.add(LowTooltip())
        queue.start()
        queue.remove()
        queue.add(LowTooltip())

        allTooltips.assertValueCount(4)
    }

    @Test
    fun whenDuringShowMiddleAddedLow_orderIsNotChanged() {
        queue.add(MiddleTooltip())
        queue.start()
        queue.add(LowTooltip())

        allTooltips.assertValueCount(2)
        allTooltips.assertValueAt(1) { isSameClass(MiddleTooltip(), it) }
    }

    @Test
    fun whenDuringShowLowAddedMiddle_emptyWillBePosted() {
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())

        allTooltips.assertValueCount(3)
        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun whenDuringShowLowAddedMiddleAndEmptyProcessed_middleWillBeShown() {
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())
        queue.remove()

        allTooltips.assertValueCount(4)
        allTooltips.assertValueAt(3) { isSameClass(MiddleTooltip(), it) }
    }

    @Test
    fun whenDuringShowLowAddedMiddleAndEmptyProcessed_lowWillBeShownOneMoreTime() {
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip())
        queue.remove()
        queue.remove()

        allTooltips.assertValueCount(6)
        allTooltips.assertValueAt(5) { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun afterStop_lowTooltipsNotShown() {
        queue.start()
        queue.stop()
        queue.add(LowTooltip())

        allTooltips.values().none { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun afterStop_emptyWillBePosted() {
        queue.start()
        queue.add(LowTooltip())
        queue.stop()

        allTooltips.assertValueCount(3)
        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun whenRemovingFromMiddle_willNotBeShown() {
        queue.add(MiddleTooltip(), LowTooltip())
        queue.remove(LowTooltip::class.java)
        queue.start()
        queue.remove()

        allTooltips.assertNever { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun whenOnBackPressedWithEmptyTooltip_falseWillReturn() {
        queue.start()

        assertEquals(false, queue.onBackPressedHandled())
        assertEquals(1, allTooltips.valueCount())
    }

    @Test
    fun whenOnBackPressedWithShowing_trueWillReturn() {
        queue.start()
        queue.add(LowTooltip())

        assertEquals(true, queue.onBackPressedHandled())
    }

    @Test
    fun whenOnBackPressed_emptyWillBePosted() {
        queue.start()
        queue.add(LowTooltip())
        queue.onBackPressedHandled()

        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun whenRemovingEmpty_exceptionWillBeThrown() {
        queue.add(LowTooltip())
        queue.start()
        queue.remove(EmptyTooltip::class.java)
    }

    @Test
    fun whenTimeNotPassed_tooltipWillNotBePosted() {
        queue.add(LowTooltip(delayMillis = 1000))
        queue.start()
        Robolectric.getForegroundThreadScheduler().advanceBy(999, TimeUnit.MILLISECONDS)

        allTooltips.assertNever { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun afterPassingDelayedTime_tooltipWillBePosted() {
        queue.add(LowTooltip(delayMillis = 1000))
        queue.start()
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)

        allTooltips.assertValueAt(1) { isSameClass(LowTooltip(), it) }
    }

    @Test
    fun afterRemovingDelayedTooltipBeforeShowing_itWillNotBePosted() {
        queue.start()
        queue.add(LowTooltip(delayMillis = 1000))
        queue.remove(LowTooltip::class.java)
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)

        allTooltips.assertValueCount(1)
    }

    @Test
    fun afterRemovingDelayedTooltipAfterShow_emptyWillBePosted() {
        queue.add(LowTooltip(delayMillis = 1000))
        queue.start()
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)
        queue.remove()

        allTooltips.assertValueCount(3)
        allTooltips.assertValueAt(0) { isSameClass(EmptyTooltip, it) }
        allTooltips.assertValueAt(1) { isSameClass(LowTooltip(), it) }
        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun afterCallingStop_emptyWillBePosted() {
        queue.add(LowTooltip())
        queue.start()
        queue.stop()

        allTooltips.assertValueCount(3)
        allTooltips.assertValueAt(0) { isSameClass(EmptyTooltip, it) }
        allTooltips.assertValueAt(1) { isSameClass(LowTooltip(), it) }
        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun queueState_returnPendingTooltips() {
        queue.add(LowTooltip(), MiddleTooltip())

        assertArrayEquals(queue.pendingTooltips().toTypedArray(), arrayOf(MiddleTooltip(), LowTooltip()))
    }

    @Test
    fun queueStateAfterStart_returnTooltipsExceptFirst() {
        queue.add(LowTooltip(), MiddleTooltip())
        queue.start()

        assertArrayEquals(queue.pendingTooltips().toTypedArray(), arrayOf(LowTooltip()))
    }

    @Test
    fun queueStateAfterProcessingAll_returnEmpty() {
        queue.add(LowTooltip(), MiddleTooltip())
        queue.start()
        queue.remove()

        assertArrayEquals(queue.pendingTooltips().toTypedArray(), arrayOf())
    }

    @Test
    fun afterClearQueue_itIsEmpty() {
        queue.add(LowTooltip(), MiddleTooltip())
        queue.clear()

        assertArrayEquals(queue.pendingTooltips().toTypedArray(), arrayOf())
    }

    @Test
    fun multipleRemove_dontProduceNewEvents() {
        queue.start()
        queue.remove()
        queue.remove()

        allTooltips.assertValueCount(1)
        allTooltips.assertValueAt(0) { isSameClass(EmptyTooltip, it) }
    }

    @Test
    fun whenAddTooltipInTheMiddleOfRemovingLowPriority_HighestPriorityShownFirst() {
        queue.add(LowTooltip())
        queue.start()
        queue.add(MiddleTooltip(), HighTooltip())
        queue.remove()
        queue.remove()

        allTooltips.assertValueCount(6)
        allTooltips.assertValueAt(0) { isSameClass(EmptyTooltip, it) }
        allTooltips.assertValueAt(1) { isSameClass(LowTooltip(), it) }
        allTooltips.assertValueAt(2) { isSameClass(EmptyTooltip, it) }
        allTooltips.assertValueAt(3) { isSameClass(HighTooltip(), it) }
        allTooltips.assertValueAt(4) { isSameClass(EmptyTooltip, it) }
        allTooltips.assertValueAt(5) { isSameClass(MiddleTooltip(), it) }
    }

}