package com.example.ui.browser

import android.os.Bundle
import com.example.data.model.TabItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class TabManager {

    private val initialTab = TabItem(
        id = UUID.randomUUID().toString(),
        url = "openv://home",
        title = "Página Inicial",
        isPrivate = false,
        lastAccessed = System.currentTimeMillis()
    )

    private val _tabs = MutableStateFlow<List<TabItem>>(listOf(initialTab))
    val tabs: StateFlow<List<TabItem>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Map to preserve saved state bundles for suspended tabs
    private val savedStates = mutableMapOf<String, Bundle>()

    // Maximum live webviews kept in active state simultaneously to prevent OOM on mobile
    private val maxActiveWebViews = 2

    fun getActiveTab(): TabItem? {
        return _tabs.value.find { it.id == _activeTabId.value } ?: _tabs.value.firstOrNull()
    }

    fun openNewTab(url: String = "openv://home", isPrivate: Boolean = false): String {
        val newId = UUID.randomUUID().toString()
        val newTab = TabItem(
            id = newId,
            url = url,
            title = if (url == "openv://home") "Página Inicial" else "Carregando...",
            isPrivate = isPrivate,
            lastAccessed = System.currentTimeMillis()
        )
        _tabs.update { current -> current + newTab }
        selectTab(newId)
        applySuspensionPolicy()
        return newId
    }

    fun selectTab(tabId: String) {
        val target = _tabs.value.find { it.id == tabId } ?: return
        _activeTabId.value = tabId
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) {
                    it.copy(lastAccessed = System.currentTimeMillis(), isSuspended = false)
                } else {
                    it
                }
            }
        }
        applySuspensionPolicy()
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // Keep at least one tab open
            val freshId = UUID.randomUUID().toString()
            val freshTab = TabItem(
                id = freshId,
                url = "openv://home",
                title = "Página Inicial",
                isPrivate = false
            )
            savedStates.remove(tabId)
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshId
            return
        }

        val closingIndex = currentTabs.indexOfFirst { it.id == tabId }
        val newTabs = currentTabs.filterNot { it.id == tabId }
        savedStates.remove(tabId)
        _tabs.value = newTabs

        if (_activeTabId.value == tabId) {
            val nextIndex = (closingIndex.coerceAtMost(newTabs.size - 1)).coerceAtLeast(0)
            val nextTab = newTabs[nextIndex]
            _activeTabId.value = nextTab.id
            _tabs.update { list ->
                list.map {
                    if (it.id == nextTab.id) it.copy(lastAccessed = System.currentTimeMillis(), isSuspended = false) else it
                }
            }
        }
        applySuspensionPolicy()
    }

    fun closeAllTabs(keepOnePrivate: Boolean = false) {
        savedStates.clear()
        val freshId = UUID.randomUUID().toString()
        val freshTab = TabItem(
            id = freshId,
            url = "openv://home",
            title = "Página Inicial",
            isPrivate = keepOnePrivate
        )
        _tabs.value = listOf(freshTab)
        _activeTabId.value = freshId
    }

    fun updateActiveTab(transform: (TabItem) -> TabItem) {
        val currentId = _activeTabId.value
        _tabs.update { list ->
            list.map { if (it.id == currentId) transform(it) else it }
        }
    }

    fun updateTab(tabId: String, transform: (TabItem) -> TabItem) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    fun saveTabState(tabId: String, state: Bundle) {
        savedStates[tabId] = state
    }

    fun getTabSavedState(tabId: String): Bundle? {
        return savedStates[tabId]
    }

    /**
     * Memory optimization policy:
     * Keeps the active tab and the most recently accessed tab in memory.
     * Marks older tabs as suspended to free memory and prevent leaks.
     */
    private fun applySuspensionPolicy() {
        val activeId = _activeTabId.value
        val sortedByAccess = _tabs.value
            .filter { it.id != activeId }
            .sortedByDescending { it.lastAccessed }

        val toSuspend = if (sortedByAccess.size > (maxActiveWebViews - 1)) {
            sortedByAccess.drop(maxActiveWebViews - 1).map { it.id }.toSet()
        } else {
            emptySet()
        }

        if (toSuspend.isNotEmpty()) {
            _tabs.update { list ->
                list.map {
                    if (toSuspend.contains(it.id)) it.copy(isSuspended = true) else it
                }
            }
        }
    }
}
