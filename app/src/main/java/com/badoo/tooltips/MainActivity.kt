package com.badoo.tooltips

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.badoo.tooltipsqueue.EmptyTooltip
import com.badoo.tooltipsqueue.TooltipsQueue
import com.badoo.tooltipsqueue.PriorityTooltipsQueue

class MainActivity : AppCompatActivity() {

    private val queue: TooltipsQueue = PriorityTooltipsQueue()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        queue.onShow().subscribe {
            when (it) {
                EmptyTooltip -> {

                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        queue.start()
    }

    override fun onStop() {
        super.onStop()
        queue.stop()
    }
}
