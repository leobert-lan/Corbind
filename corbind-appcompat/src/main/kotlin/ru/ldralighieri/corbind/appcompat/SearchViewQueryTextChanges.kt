package ru.ldralighieri.corbind.appcompat

import androidx.annotation.CheckResult
import androidx.appcompat.widget.SearchView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun SearchView.queryTextChanges(
        scope: CoroutineScope,
        action: suspend (CharSequence) -> Unit
) {
    val events = scope.actor<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        for (chars in channel) action(chars)
    }

    events.offer(query)
    setOnQueryTextListener(listener(events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

suspend fun SearchView.queryTextChanges(
        action: suspend (CharSequence) -> Unit
) = coroutineScope {
    val events = actor<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        for (chars in channel) action(chars)
    }

    events.offer(query)
    setOnQueryTextListener(listener(events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchView.queryTextChanges(
        scope: CoroutineScope
): ReceiveChannel<CharSequence> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(query)
    setOnQueryTextListener(listener(::offer))
    invokeOnClose { setOnQueryTextListener(null) }
}

@CheckResult
suspend fun SearchView.queryTextChanges(): ReceiveChannel<CharSequence> = coroutineScope {

    produce<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        offer(query)
        setOnQueryTextListener(listener(::offer))
        invokeOnClose { setOnQueryTextListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        emitter: (CharSequence) -> Boolean
) = object : SearchView.OnQueryTextListener {

    override fun onQueryTextChange(s: String): Boolean { return emitter(s) }
    override fun onQueryTextSubmit(query: String): Boolean { return false }
}