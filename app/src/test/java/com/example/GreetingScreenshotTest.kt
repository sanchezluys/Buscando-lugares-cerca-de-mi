package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.Place
import com.example.ui.components.PlaceItemCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val samplePlace = Place(
      id = "test_1",
      name = "The Prime Burger & Craft Bar",
      category = "Hamburguesas",
      categoryIcon = "🍔",
      rating = 4.9,
      userRatingsTotal = 1420,
      address = "Av. Insurgentes Sur 1450",
      latitude = 19.4326,
      longitude = -99.1332,
      photoUrl = null,
      isOpenNow = true,
      priceLevel = 3,
      distanceMeters = 450
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        PlaceItemCard(
          place = samplePlace,
          rank = 1,
          onSelectForMap = {},
          onToggleFavorite = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
