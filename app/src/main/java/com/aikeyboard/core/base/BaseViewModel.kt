package com.aikeyboard.core.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base ViewModel that every feature ViewModel extends.
 *
 * Provides a single [state] StateFlow and a protected [setState]
 * helper so every child has a consistent, boilerplate-free pattern.
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /** Public read-only state exposed to the UI. */
    val state: StateFlow<S> = _state.asStateFlow()

    /** Update state safely from any function inside the ViewModel. */
    protected fun setState(reducer: S.() -> S) = _state.update(reducer)
}
