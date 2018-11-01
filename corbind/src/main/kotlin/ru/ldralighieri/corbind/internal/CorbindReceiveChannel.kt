@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.internal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

// -----------------------------------------------------------------------------------------------

inline fun <T> corbindReceiveChannel(
        block: Channel<T>.() -> Unit
): ReceiveChannel<T> {
    val channel = Channel<T>(Channel.CONFLATED)
    channel.block()
    return channel
}

// -----------------------------------------------------------------------------------------------

fun <T> Channel<T>.safeOffer(element: T): Boolean {
    return if (!isClosedForSend) {
        offer(element)
        true
    } else { false }
}