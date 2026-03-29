package com.aikeyboard.core.base

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base in-memory repository.
 *
 * Feature repositories extend this to get a free StateFlow<List<T>>
 * with add / remove / update helpers — no boilerplate repeated.
 */
abstract class BaseListRepository<T>(initialItems: List<T> = emptyList()) {

    private val _items = MutableStateFlow(initialItems)

    /** Public read-only stream of items. */
    val items: StateFlow<List<T>> = _items.asStateFlow()

    protected fun setItems(items: List<T>) { _items.value = items }

    protected fun addItem(item: T, prepend: Boolean = true) {
        _items.update { list ->
            if (prepend) listOf(item) + list else list + item
        }
    }

    protected fun removeItem(predicate: (T) -> Boolean) {
        _items.update { list -> list.filterNot(predicate) }
    }
}
