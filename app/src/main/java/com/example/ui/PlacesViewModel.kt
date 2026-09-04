package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteEntity
import com.example.data.remote.PlacesApiService
import com.example.data.repository.PlacesRepository
import com.example.location.Coordinates
import com.example.location.LocationHelper
import com.example.model.CategoryPreset
import com.example.model.Place
import com.example.model.SearchFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    EXPLORE,
    MAP,
    FAVORITES
}

sealed interface ApiKeyTestStatus {
    object Idle : ApiKeyTestStatus
    object Testing : ApiKeyTestStatus
    data class Success(val message: String) : ApiKeyTestStatus
    data class Error(val message: String) : ApiKeyTestStatus
}

data class PlacesUiState(
    val currentTab: NavigationTab = NavigationTab.EXPLORE,
    val filter: SearchFilter = SearchFilter(),
    val topPlaces: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val isLoading: Boolean = false,
    val isLiveGoogleApi: Boolean = false,
    val statusMessage: String? = null,
    val currentLocation: Coordinates = Coordinates.DEFAULT,
    val locationPermissionGranted: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val apiKeyInput: String = "",
    val apiKeyTestStatus: ApiKeyTestStatus = ApiKeyTestStatus.Idle,
    val mapCenterLat: Double = Coordinates.DEFAULT.latitude,
    val mapCenterLng: Double = Coordinates.DEFAULT.longitude,
    val mapZoomLevel: Float = 1.0f,
    val editingNotePlace: Place? = null
)

class PlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlacesRepository(application.applicationContext)
    private val locationHelper = LocationHelper(application.applicationContext)

    val favorites: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(
        PlacesUiState(
            apiKeyInput = PlacesRepository.getDefaultApiKey(),
            filter = SearchFilter(
                apiKey = PlacesRepository.getDefaultApiKey()
            )
        )
    )
    val uiState: StateFlow<PlacesUiState> = _uiState.asStateFlow()

    init {
        checkLocationAndInitialSearch()
        observeFavoritesSync()
    }

    private fun observeFavoritesSync() {
        viewModelScope.launch {
            favorites.collect { favList ->
                val favIds = favList.map { it.placeId }.toSet()
                _uiState.update { current ->
                    val updatedPlaces = current.topPlaces.map { place ->
                        place.copy(isFavorite = favIds.contains(place.id))
                    }
                    val updatedSelected = current.selectedPlace?.let { sel ->
                        sel.copy(isFavorite = favIds.contains(sel.id))
                    }
                    current.copy(
                        topPlaces = updatedPlaces,
                        selectedPlace = updatedSelected
                    )
                }
            }
        }
    }

    fun checkLocationAndInitialSearch() {
        val hasPermission = locationHelper.hasLocationPermission()
        _uiState.update { it.copy(locationPermissionGranted = hasPermission) }

        viewModelScope.launch {
            val coords = if (hasPermission) {
                locationHelper.getCurrentLocation()
            } else {
                Coordinates.DEFAULT
            }
            _uiState.update {
                it.copy(
                    currentLocation = coords,
                    mapCenterLat = coords.latitude,
                    mapCenterLng = coords.longitude,
                    filter = it.filter.copy(
                        userLatitude = coords.latitude,
                        userLongitude = coords.longitude
                    )
                )
            }
            performSearch()
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
        if (granted) {
            viewModelScope.launch {
                val coords = locationHelper.getCurrentLocation()
                _uiState.update {
                    it.copy(
                        currentLocation = coords,
                        mapCenterLat = coords.latitude,
                        mapCenterLng = coords.longitude,
                        filter = it.filter.copy(
                            userLatitude = coords.latitude,
                            userLongitude = coords.longitude
                        )
                    )
                }
                performSearch()
            }
        }
    }

    fun setNavigationTab(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectCategoryPreset(preset: CategoryPreset) {
        _uiState.update {
            it.copy(
                filter = it.filter.copy(
                    categoryKey = preset.id,
                    categoryName = preset.title,
                    query = preset.query
                )
            )
        }
        performSearch()
    }

    fun updateCustomQuery(query: String) {
        _uiState.update {
            it.copy(
                filter = it.filter.copy(query = query)
            )
        }
    }

    fun setMinRating(rating: Double) {
        _uiState.update {
            it.copy(
                filter = it.filter.copy(minRating = rating)
            )
        }
        performSearch()
    }

    fun performSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            val favIds = favorites.value.map { it.placeId }.toSet()
            val result = repository.searchTopPlaces(_uiState.value.filter, favIds)

            val firstPlace = result.places.firstOrNull()

            _uiState.update {
                it.copy(
                    topPlaces = result.places,
                    selectedPlace = firstPlace,
                    isLoading = false,
                    isLiveGoogleApi = result.isLiveGoogleApi,
                    statusMessage = result.message,
                    mapCenterLat = firstPlace?.latitude ?: it.currentLocation.latitude,
                    mapCenterLng = firstPlace?.longitude ?: it.currentLocation.longitude
                )
            }
        }
    }

    fun selectPlace(place: Place, navigateToMap: Boolean = false) {
        _uiState.update {
            it.copy(
                selectedPlace = place,
                mapCenterLat = place.latitude,
                mapCenterLng = place.longitude,
                currentTab = if (navigateToMap) NavigationTab.MAP else it.currentTab
            )
        }
    }

    fun toggleFavorite(place: Place) {
        viewModelScope.launch {
            if (place.isFavorite) {
                repository.removeFavorite(place.id)
            } else {
                repository.saveFavorite(place)
            }
        }
    }

    fun removeFavoriteById(placeId: String) {
        viewModelScope.launch {
            repository.removeFavorite(placeId)
        }
    }

    fun openNoteDialog(place: Place) {
        _uiState.update { it.copy(editingNotePlace = place) }
    }

    fun closeNoteDialog() {
        _uiState.update { it.copy(editingNotePlace = null) }
    }

    fun savePersonalNote(placeId: String, note: String) {
        viewModelScope.launch {
            repository.updateFavoriteNote(placeId, note)
            closeNoteDialog()
        }
    }

    fun showApiKeyDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showApiKeyDialog = show,
                apiKeyTestStatus = ApiKeyTestStatus.Idle
            )
        }
    }

    fun updateApiKeyInput(input: String) {
        _uiState.update { it.copy(apiKeyInput = input) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        _uiState.update {
            it.copy(
                filter = it.filter.copy(apiKey = key),
                showApiKeyDialog = false
            )
        }
        performSearch()
    }

    fun testApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isEmpty()) {
            _uiState.update {
                it.copy(apiKeyTestStatus = ApiKeyTestStatus.Error("Por favor ingresa una clave API"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(apiKeyTestStatus = ApiKeyTestStatus.Testing) }
            try {
                val apiService = PlacesApiService.create()
                val response = apiService.searchText(
                    query = "hamburguesas",
                    location = "19.4326,-99.1332",
                    radius = 1000,
                    apiKey = key
                )
                if (response.status == "OK" || response.status == "ZERO_RESULTS") {
                    _uiState.update {
                        it.copy(
                            apiKeyTestStatus = ApiKeyTestStatus.Success("¡Clave de Google Places verificada con éxito!")
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            apiKeyTestStatus = ApiKeyTestStatus.Error(
                                response.errorMessage ?: "Respuesta de Google: ${response.status}"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        apiKeyTestStatus = ApiKeyTestStatus.Error("Error de conexión: ${e.localizedMessage ?: "No se pudo conectar"}")
                    )
                }
            }
        }
    }

    fun recenterOnUser() {
        _uiState.update {
            it.copy(
                mapCenterLat = it.currentLocation.latitude,
                mapCenterLng = it.currentLocation.longitude,
                mapZoomLevel = 1.0f
            )
        }
    }

    fun recenterOnPlaces() {
        val places = _uiState.value.topPlaces
        if (places.isNotEmpty()) {
            val avgLat = places.map { it.latitude }.average()
            val avgLng = places.map { it.longitude }.average()
            _uiState.update {
                it.copy(
                    mapCenterLat = avgLat,
                    mapCenterLng = avgLng,
                    mapZoomLevel = 1.0f
                )
            }
        }
    }

    fun zoomIn() {
        _uiState.update {
            it.copy(mapZoomLevel = (it.mapZoomLevel * 1.3f).coerceAtMost(4.0f))
        }
    }

    fun zoomOut() {
        _uiState.update {
            it.copy(mapZoomLevel = (it.mapZoomLevel / 1.3f).coerceAtLeast(0.4f))
        }
    }

    fun updateMapPan(deltaLat: Double, deltaLng: Double) {
        _uiState.update {
            it.copy(
                mapCenterLat = it.mapCenterLat + deltaLat,
                mapCenterLng = it.mapCenterLng + deltaLng
            )
        }
    }
}
