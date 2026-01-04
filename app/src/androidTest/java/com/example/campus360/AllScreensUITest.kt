package com.example.campus360

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllScreensUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        composeTestRule.waitForIdle()
        Thread.sleep(8000)
    }
    
    private fun findNodeByTextOrContentDescription(
        textOptions: List<String>,
        contentDescriptionOptions: List<String> = emptyList(),
        timeoutMillis: Long = 15000
    ): SemanticsNodeInteraction? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            for (text in textOptions) {
                try {
                    val nodes = composeTestRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                        .fetchSemanticsNodes()
                    if (nodes.isNotEmpty()) {
                        return composeTestRule.onNodeWithText(text, substring = true, useUnmergedTree = true)
                    }
                } catch (e: Exception) {
                }
            }
            
            for (contentDesc in contentDescriptionOptions) {
                try {
                    val nodes = composeTestRule.onAllNodesWithContentDescription(contentDesc, useUnmergedTree = true)
                        .fetchSemanticsNodes()
                    if (nodes.isNotEmpty()) {
                        return composeTestRule.onNodeWithContentDescription(contentDesc, useUnmergedTree = true)
                    }
                } catch (e: Exception) {
                }
            }
            
            Thread.sleep(300)
        }
        return null
    }
    
    private fun clickBottomNavItem(textOptions: List<String>, contentDescriptionOptions: List<String> = emptyList()) {
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        
        for (contentDesc in contentDescriptionOptions) {
            try {
                val nodes = composeTestRule.onAllNodesWithContentDescription(contentDesc, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    composeTestRule.onAllNodesWithContentDescription(contentDesc, useUnmergedTree = true)
                        .onLast()
                        .performClick()
                    return
                }
            } catch (e: Exception) {
            }
        }
        
        for (text in textOptions) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                if (nodes.size > 1) {
                    composeTestRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                        .onLast()
                        .performClick()
                    return
                } else if (nodes.size == 1) {
                    composeTestRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                        .onFirst()
                        .performClick()
                    return
                }
            } catch (e: Exception) {
            }
        }
        
        throw AssertionError("Could not find bottom nav item with text: $textOptions or content: $contentDescriptionOptions")
    }

    @Test
    fun testSplashScreen() {
        composeTestRule.onNodeWithText("Campus360", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun testHomeScreen() {
        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        val searchPlaceholder = findNodeByTextOrContentDescription(
            listOf("Search for room or area", "Sök efter rum eller område", "Sök efter rum"),
            listOf("Search", "Sök")
        )
        searchPlaceholder?.assertExists() ?: throw AssertionError("Search placeholder not found")

        val quickCategories = findNodeByTextOrContentDescription(
            listOf("Quick Categories", "Snabbkategorier")
        )
        quickCategories?.assertExists() ?: throw AssertionError("Quick Categories not found")

        val openMap = findNodeByTextOrContentDescription(
            listOf("Open Map", "Öppna karta")
        )
        openMap?.assertExists() ?: throw AssertionError("Open Map button not found")

        val sos = findNodeByTextOrContentDescription(
            listOf("SOS", "Nöd")
        )
        sos?.assertExists() ?: throw AssertionError("SOS button not found")
    }

    @Test
    fun testSearchScreen() {
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        clickBottomNavItem(
            listOf("Search", "Sök"),
            listOf("Search", "Sök")
        )

        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        val searchBar = findNodeByTextOrContentDescription(
            listOf("Search by name or room number", "Sök efter namn eller rummnummer", "Sök efter namn")
        )
        searchBar?.assertExists() ?: throw AssertionError("Search bar not found")

        val findRoom = findNodeByTextOrContentDescription(
            listOf("Find a Room", "Hitta ett rum")
        )
        findRoom?.assertExists() ?: throw AssertionError("Find a Room header not found")
    }

    @Test
    fun testMapScreen() {
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        clickBottomNavItem(
            listOf("Map", "Karta"),
            listOf("Map", "Karta")
        )

        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        composeTestRule.onRoot(useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun testSettingsScreen() {
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        clickBottomNavItem(
            listOf("Settings", "Inställningar"),
            listOf("Settings", "Inställningar")
        )

        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        val language = findNodeByTextOrContentDescription(
            listOf("Language", "Språk")
        )
        language?.assertExists() ?: throw AssertionError("Language section not found")

        val english = findNodeByTextOrContentDescription(
            listOf("English", "Engelska")
        )
        english?.assertExists() ?: throw AssertionError("English option not found")

        val swedish = findNodeByTextOrContentDescription(
            listOf("Swedish", "Svenska")
        )
        swedish?.assertExists() ?: throw AssertionError("Swedish option not found")
    }

    @Test
    fun testHomeScreenButtons() {
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        val openMap = findNodeByTextOrContentDescription(
            listOf("Open Map", "Öppna karta")
        )
        openMap?.assertExists()?.performClick() ?: throw AssertionError("Open Map button not found")

        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        composeTestRule.onRoot(useUnmergedTree = true)
            .assertExists()

        Thread.sleep(1000)
        clickBottomNavItem(
            listOf("Home", "Hem"),
            listOf("Home", "Hem")
        )
        
        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        val sos = findNodeByTextOrContentDescription(
            listOf("SOS", "Nöd")
        )
        sos?.assertExists() ?: throw AssertionError("SOS button not found")
    }

    @Test
    fun testLanguageSelection() {
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        clickBottomNavItem(
            listOf("Settings", "Inställningar"),
            listOf("Settings", "Inställningar")
        )
        
        composeTestRule.waitForIdle()
        Thread.sleep(3000)

        val english = findNodeByTextOrContentDescription(
            listOf("English", "Engelska")
        )
        english?.assertExists() ?: throw AssertionError("English option not found")

        val swedish = findNodeByTextOrContentDescription(
            listOf("Swedish", "Svenska")
        )
        swedish?.assertExists() ?: throw AssertionError("Swedish option not found")
    }
}
