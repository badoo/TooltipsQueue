package com.badoo.tooltips

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.badoo.tooltipsqueue.EmptyTooltip
import com.badoo.tooltipsqueue.PriorityTooltipsQueue
import com.badoo.tooltipsqueue.TooltipsQueue
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TOOLTIPS_EXTRA = "tooltips_extra"
        private const val MONEY_EXTRA = "goal_current_extra"
    }

    private val queue: TooltipsQueue = PriorityTooltipsQueue()
    private var tooltipDisposable: Disposable? = null

    private val moneyView by lazy { findViewById<TextView>(R.id.money_view) }
    private val priorityView by lazy { findViewById<TextView>(R.id.priority_view) }
    private val sendMoneyAmount by lazy { findViewById<EditText>(R.id.send_money_amount) }
    private val sendMoneyDelay by lazy { findViewById<EditText>(R.id.send_money_delay) }

    private var currentMoney = 0
    private var tooltip: SimpleTooltip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        savedInstanceState?.let {
            @Suppress("UNCHECKED_CAST")
            val queueState = it.getSerializable(TOOLTIPS_EXTRA) as List<SerializableTooltip>
            queue.add(*queueState.toTypedArray())
            currentMoney = it.getInt(MONEY_EXTRA)
        }

        val sendMoney = findViewById<Button>(R.id.send_money)
        val lowPriority = findViewById<Button>(R.id.show_low_priority)
        val highPriority = findViewById<Button>(R.id.show_high_priority)

        sendMoney.setOnClickListener {
            val amount = Integer.valueOf(sendMoneyAmount.text.toString())
            val delayMillis = TimeUnit.SECONDS.toMillis(Integer.valueOf(sendMoneyDelay.text.toString()).toLong())
            currentMoney += amount
            queue.add(MoneyTooltip(amount, delayMillis))
        }

        lowPriority.setOnClickListener {
            queue.add(LowPriorityTooltip())
        }

        highPriority.setOnClickListener {
            queue.add(HighPriorityTooltip())
        }
    }

    override fun onStart() {
        super.onStart()
        tooltipDisposable = showTooltips()
        queue.start()
    }

    override fun onStop() {
        super.onStop()
        queue.stop()
        tooltipDisposable?.dispose()
    }

    override fun onBackPressed() {
        if (!queue.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        val queueState = ArrayList<SerializableTooltip>(queue.pendingTooltips().filterIsInstance<SerializableTooltip>())
        outState.putSerializable(TOOLTIPS_EXTRA, queueState)
        outState.putInt(MONEY_EXTRA, currentMoney)
    }

    private fun showTooltips(): Disposable =
            queue.onShow().subscribe {
                when (it) {
                    is HighPriorityTooltip -> {
                        createTooltipView(priorityView, "Show high priority tooltip", Gravity.START)
                    }
                    is MoneyTooltip -> {
                        val message = "Received money - ${it.amount}\nCurrent money - $currentMoney"
                        createTooltipView(moneyView, message, Gravity.END)
                    }
                    is LowPriorityTooltip -> {
                        createTooltipView(priorityView, "Show low priority tooltip", Gravity.START)
                    }
                    EmptyTooltip -> {
                        tooltip?.dismiss()
                    }
                }
            }

    private fun createTooltipView(anchor: View, message: String, gravity: Int) {
        tooltip = SimpleTooltip.Builder(this)
                .onDismissListener {
                    queue.remove()
                }
                .anchorView(anchor)
                .text(message)
                .gravity(gravity)
                .animated(true)
                .build()
        tooltip?.show()
    }

}
