# TooltipsQueue

[![Build Status](https://travis-ci.org/badoo/TooltipsQueue.svg?branch=master)](https://travis-ci.org/badoo/TooltipsQueue)

TooltipsQueue is a simple library for display items/tooltips one by one on the screen. You will not find any UI here, so it's up to you how do you want to display item on the screen(you can use any tooltip library)

This library perfectly fits your needs when you have some order of tooltips and you want to show it one by one and you don't want to show multiple tooltips at the same time. And it is easily extandable to add tooltips dynamically in runtime. Also you can easily split logic related to showing this items to the different presenters. With such approach you can write new code without touching old one and be sure, that everything works.

## Features
 - Always show only one tooltip on the screen
 - Stops queue in background and hides current tooltip
 - Doesn't have any UI, so it's up to you how to show tooltips
 - Split logic between multiple presenters
 - Dynamically adding tooltips in runtime
 - You can easily manage process/activity death
 - You can show any complex animations, and it will not break rule of showing one tooltip on the screen.
 - You can manage onBackPressed to hide currently showing tooltip
<!---
## Download
Available through jitpack.
(Add the maven repo to your root build.gradle)

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependencies:
```gradle
implementation 'com.github.badoo.tooltipsqueue:tooltipsqueue:0.1.0'
```
--->
## How to use
1. You should call start/stop method on queue during activity lifecycle events, so queue will be able to stop showing tooltips in background.
```kotlin
override fun onStart() {
    super.onStart()
    queue.start()
}
override fun onStop() {
    super.onStop()
    queue.stop()
}
```
2. You should add to queue tooltips(you can do this in started/stopped state it doesn't really matter):
```kotlin
queue.add(LowPriorityTooltip())
queue.add(HighPriorityTooltip())
```
3. You need to subscribe to showing tooltips and show, when you receive specific type from queue:
```kotlin
queue.onShow().subscribe {
    when (it) {
        is HighPriorityTooltip -> showHighPriorityTooltip()
        is LowPriorityTooltip -> showLowPriorityTooltip()
        EmptyTooltip -> tooltip?.dismiss()
}
```
We will talk about `EmptyTooltip` a little bit later.

4. After dismissing tooltip you should always notify queue that you processed with showing current one and now ready to process next items. So it mostly will look like:
```kotlin
.onDismissListener {
    queue.remove()
}
```
Or something similar. Don't forget to do this, otherwise queue will not post any new tooltips.

Basically that's all you need to know to work with `TooltipsQueue` in simplest way. Your tooltips will be shown one by one, queue will be stopped during going in background, and you can have any fancy animations that you want. But let's discuss what else you can do with the library.

## Tooltip interface
```kotlin
interface Tooltip {

    val priority: Int
    val delayMillis: Long

}
```
You will see more high priority tooltip earlier than low priority one. So it is main rule and for example if you are showing now low priority tooltip and adding to queue highPriority one, than current one will be hidden and placed back in the queue, and highPriority will be shown immediately. Because of that you must hide currently shown tooltips in case of receiving `EmptyTooltip`.

`dellayMillis` field is responsible for having gap between starting processing tooltip and actual showing. Imagine, that you want to show tooltip, only if user was staying on this screen at least 3 minutes. Important thing, that queue have only one timer for tooltip, that is on top. It means, that if for example delay is 3 minutes, but in the middle of waiting you received and show more high priority one -> timer will than start again and will wait 3 minutes. The same for example if user will hide current activity.

`EmptyTooltip` - it is special kind of `Tooltip` interface and his main responsibility -> notify you, to hide currently shown tooltip if you have such. It can happen when user hides activity to background, or more high priority posted during showing of low priority.

## TooltipsQueue contract
Let's discuss other queue methods, that can be useful.
 
```kotlin
fun add(vararg tooltips: Tooltip)
```
You can add one, or multiple tooltips, and you can do this when queue is paused or running.

```kotlin
fun remove(tooltip: Class<out Tooltip>? = null)
```
You should always notify queue, when you hide tooltip from the screen. 
If you don't pass any parameter here by default we think that you removing tooltip from the head of the queue. 
But you can also remove tooltips from any part of the queue. 
For example you wanted to show tooltip about specific behaviour of your UI, but user was able to find it before tooltip has been shown.
In such case you should pass what kind you want to remove from the queue.
Mostly all time you will use this method without parameters.

```kotlin
fun onBackPressed(): Boolean
```
You can use this method to emulate hiding tooltips when user press back button.
So if you call it, and currently we have tooltip on the screen -> `EmptyTooltip` will be posted, and `true` will be returned.
Be careful, because this method has side effects with `EmptyTooltip` posting.

```kotlin
fun clear()
```
Removing all pending tooltips from the queue. Can be used when you need completely reinitialize your queue.

```kotlin
fun pendingTooltips(): List<Tooltip>
``` 
Returns all pending tooltips that were not shown. 
You can use to save your tooltips inside bundle to handle activity death. 
For example if your tooltips satisfy also `Serializable` contract you can easily save and restore state:
```kotlin
interface SerializableTooltip: Tooltip, Serializable

override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
    val queueState = ArrayList<SerializableTooltip>(queue.pendingTooltips().filterIsInstance<SerializableTooltip>())
    outState.putSerializable("TOOLTIPS_KEY", queueState)
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let {
        val queueState = it.getSerializable(TOOLTIPS_EXTRA) as List<SerializableTooltip>
        queue.add(*queueState.toTypedArray())
    }
}
```

## Additional configurations
`PriorityTooltipsQueue` has additional strategy parameter. It can be used to get more customizations.
```kotlin
fun isChild(parent: Tooltip, child: Tooltip): Boolean
```
Tooltips can have (parent => child) relationships. If you add parent to the queue, than all his children
should be removed. For example you have specific order of tooltips to guide user to make an order.
User saw 2 from 3 of this tooltips. And during delay he made order by himself, without waiting for 3rd one.
You are showing to him congratulations tooltips and now it doesn't really make sense to show 3rd
introductory tooltip for him. So if you setup him as child it will be automatically removed from queue.
`DefaultStrategy` always return false for this method.

```kotlin
fun putInQueueDelayMillis(hided: Tooltip): Long?
```
If you are adding highPriority tooltip when lowPriority is showing than lowPriority will be hidden and
based on this function will be put in queue with specific delay.
You can skip adding it back to queue -> just return null here. 
You can setup different delay for different kind of tooltips.
`DefaultStrategy` return 0 delay. So by default tooltip will be shown immediately after high priority will be hidden. 