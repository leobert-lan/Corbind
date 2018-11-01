@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun View.draws(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(scope, events::offer)
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
suspend fun View.draws(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(this, events::offer)
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
@CheckResult
fun View.draws(
        scope: CoroutineScope
): ReceiveChannel<Unit> = corbindReceiveChannel {

    val listener = listener(scope, ::safeOffer)
    viewTreeObserver.addOnDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
@CheckResult
suspend fun View.draws(): ReceiveChannel<Unit> = coroutineScope {

    corbindReceiveChannel<Unit> {
        val listener = listener(this@coroutineScope, ::safeOffer)
        viewTreeObserver.addOnDrawListener(listener)
        invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnDrawListener {

    if (scope.isActive) { emitter(Unit) }
}