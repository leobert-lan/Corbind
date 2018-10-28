package ru.ldralighieri.corbind.view

import android.view.MotionEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------


fun View.touches(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) {

    val events = scope.actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}

suspend fun View.touches(
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {

    val events = actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.touches(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnTouchListener(listener(this, handled, ::offer))
    invokeOnClose { setOnTouchListener(null) }
}

@CheckResult
suspend fun View.touches(
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = coroutineScope {

    produce<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnTouchListener(listener(this, handled, ::offer))
        invokeOnClose { setOnTouchListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnTouchListener { _, motionEvent ->

    if (scope.isActive) {
        if (handled(motionEvent)) {
            emitter(motionEvent)
            return@OnTouchListener true
        }
    }
    return@OnTouchListener false
}