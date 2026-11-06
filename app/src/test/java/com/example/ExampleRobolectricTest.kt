package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SearchEngine
import com.example.ui.browser.TabManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OpenV", appName)
  }

  @Test
  fun `test tab manager initial state and new tab creation`() {
    val tabManager = TabManager()
    val initial = tabManager.getActiveTab()
    assertNotNull(initial)
    assertEquals("openv://home", initial?.url)

    val newId = tabManager.openNewTab("https://duckduckgo.com", isPrivate = true)
    assertEquals(2, tabManager.tabs.value.size)
    assertEquals(newId, tabManager.activeTabId.value)
    assertTrue(tabManager.getActiveTab()?.isPrivate == true)
  }

  @Test
  fun `test search engine query builder`() {
    val query = "osint tools"
    val url = SearchEngine.DUCKDUCKGO.buildQueryUrl(query)
    assertTrue(url.contains("duckduckgo.com"))
    assertTrue(url.contains("osint+tools") || url.contains("osint%20tools"))
  }
}
