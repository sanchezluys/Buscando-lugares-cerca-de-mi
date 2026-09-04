package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PlacesRepository
import com.example.model.Place
import com.example.model.SearchFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlacesProductionQaTest {

    private lateinit var context: Context
    private lateinit var repository: PlacesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = PlacesRepository(context)
    }

    @Test
    fun testOfflineFreePlacesSearch_ReturnsTopFivePlaces() = runBlocking {
        val filter = SearchFilter(
            query = "quiero comer hamburguesas",
            categoryKey = "hamburguesas",
            categoryName = "Hamburguesas",
            minRating = 4.0,
            userLatitude = 40.4168,
            userLongitude = -3.7038,
            apiKey = ""
        )

        val result = repository.searchTopPlaces(filter, emptySet())

        assertNotNull(result)
        assertTrue("Debe retornar resultados sin requerir API keys", result.places.isNotEmpty())
        assertTrue("Debe limitar a los 5 mejores sitios", result.places.size <= 5)
        assertTrue("Cada sitio debe cumplir la calificación mínima", result.places.all { it.rating >= 4.0 })
    }

    @Test
    fun testFavoriteToggleAndLocalPersistence() = runBlocking {
        val testPlace = Place(
            id = "qa_test_place_1",
            name = "Café Central QA",
            category = "Café",
            categoryIcon = "☕",
            rating = 4.8,
            userRatingsTotal = 320,
            address = "Gran Vía 12, Madrid",
            latitude = 40.4190,
            longitude = -3.7010,
            isOpenNow = true,
            priceLevel = 2,
            distanceMeters = 200,
            isFavorite = false
        )

        // Guardar favorito
        repository.saveFavorite(testPlace, "Nota de prueba QA")
        val favorites = repository.allFavorites.first()
        assertTrue("La lista de favoritos debe contener el lugar guardado", favorites.any { it.placeId == "qa_test_place_1" })
        assertEquals("La nota debe coincidir", "Nota de prueba QA", favorites.first { it.placeId == "qa_test_place_1" }.personalNote)

        // Eliminar favorito
        repository.removeFavorite("qa_test_place_1")
        val favoritesAfterDelete = repository.allFavorites.first()
        assertTrue("El lugar debe haberse eliminado", favoritesAfterDelete.none { it.placeId == "qa_test_place_1" })
    }
}
