package com.artillery.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A small MVI state holder with an ordered, asynchronous reducer queue.
 */
open class StateViewModel<S : Any>(
    initialState: S,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : ViewModel(
    viewModelScope = CoroutineScope(context = coroutineContext + Job()),
) {
    // ponytail: unbounded FIFO keeps setState non-blocking; add backpressure if producers become untrusted.
    private val actions = Channel<(S) -> S>(capacity = Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(value = initialState)

    val state: StateFlow<S> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(block = {
            for (action in actions) {
                mutableState.value = action(mutableState.value)
            }
        })
    }

    /** Queues a reducer and returns without waiting for it to run. */
    protected fun setState(reducer: S.() -> S) {
        check(value = actions.trySend(element = { state -> reducer(state) }).isSuccess) {
            "StateViewModel is cleared"
        }
    }

    /** Queues a read after all previously queued reducers have run. */
    protected fun withState(action: (S) -> Unit) {
        check(
            value = actions.trySend(
                element = { state ->
                    action(state)
                    state
                },
            ).isSuccess,
        ) {
            "StateViewModel is cleared"
        }
    }

    open override fun onCleared() {
        actions.close()
    }
}
