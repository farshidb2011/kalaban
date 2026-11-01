package com.kalaban.farshidb2011

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.kalaban.farshidb2011", appContext.packageName)
    }

    @Test
    fun home_showsDisplayPanel() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val faTitle = "پنل نمایش"
        val enTitle = "Display"
        // Default landing screen is DisplayPanel (Persian by default, English if switched)
        val hasDisplay = runCatching {
            composeRule.onNodeWithText(faTitle, substring = true).assertIsDisplayed()
            true
        }.getOrElse {
            composeRule.onNodeWithText(enTitle, substring = true).assertIsDisplayed()
            true
        }
        assertEquals(true, hasDisplay)
        assertEquals("com.kalaban.farshidb2011", appContext.packageName)
    }
}
