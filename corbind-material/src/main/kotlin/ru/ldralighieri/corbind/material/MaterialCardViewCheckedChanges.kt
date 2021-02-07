/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on [MaterialCardView] check change.
 *
 * *Warning:* Perform only when the [MaterialCardView] is in checkable state.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun MaterialCardView.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (checked in channel) action(checked)
    }

    checkCheckableState(this)
    events.offer(isChecked)
    val listener = listener(scope, events::offer)
    setOnCheckedChangeListener(listener)
    events.invokeOnClose { setOnCheckedChangeListener(listener) }
}

/**
 * Perform an action on [MaterialCardView] check change, inside new [CoroutineScope].
 *
 * *Warning:* Perform only when the [MaterialCardView] is in checkable state.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun MaterialCardView.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel which emits on [MaterialCardView] check change.
 *
 * *Warning:* Emits only when the [MaterialCardView] is in checkable state.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialCardView.checkedChanges(scope)
 *          .consumeEach { /* handle check change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun MaterialCardView.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    checkCheckableState(this@checkedChanges)
    offerCatching(isChecked)
    val listener = listener(scope, ::offerCatching)
    setOnCheckedChangeListener(listener)
    invokeOnClose { setOnCheckedChangeListener(listener) }
}

/**
 * Create a flow which emits on [MaterialCardView] check change.
 *
 * *Warning:* Emits only when the [MaterialCardView] is in checkable state.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * materialCardView.checkedChanges()
 *      .onEach { /* handle check change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * materialCardView.checkedChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle check change */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun MaterialCardView.checkedChanges(): InitialValueFlow<Boolean> = channelFlow<Boolean> {
    checkCheckableState(this@checkedChanges)
    setOnCheckedChangeListener(listener(this, ::offerCatching))
    awaitClose { setOnCheckedChangeListener(null) }
}.asInitialValueFlow(isChecked)

private fun checkCheckableState(card: MaterialCardView) {
    check(card.isCheckable) { "The MaterialCardView is not checkable." }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean
) = MaterialCardView.OnCheckedChangeListener { _, isChecked ->
    if (scope.isActive) { emitter(isChecked) }
}
