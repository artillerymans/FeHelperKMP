package com.artillery.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.collect

@Composable
fun <S : Any> StateViewModel<S>.collectAsState(): State<S> =
    state.collectAsState()

@Composable
fun <S : Any, T> StateViewModel<S>.collectAsState(selector: (S) -> T): T {
    val currentSelector = rememberUpdatedState(newValue = selector)
    return produceState(
        initialValue = currentSelector.value(state.value),
        key1 = this,
    ) {
        state.collect(action = { value = currentSelector.value(it) })
    }.value
}
