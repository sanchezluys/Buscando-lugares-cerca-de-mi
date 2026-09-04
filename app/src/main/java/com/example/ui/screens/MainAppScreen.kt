package com.example.ui.screens

import android.Manifest
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FavoriteEntity
import com.example.ui.NavigationTab
import com.example.ui.PlacesViewModel
import com.example.ui.components.EditNoteDialog
import com.example.ui.components.InteractiveMapCanvas
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainAppScreen(
    viewModel: PlacesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.onLocationPermissionResult(true)
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFEF7FF),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6750A4),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Nearby Explorer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color(0xFF1D1B20)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF6750A4),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (uiState.currentLocation.isRealLocation) "Ubicación GPS activa" else "Calle Mayor • Madrid",
                                    fontSize = 12.sp,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF3EDF7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setNavigationTab(NavigationTab.EXPLORE) },
                            modifier = Modifier.testTag("top_bar_search_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFEF7FF)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = Color(0xFFD0BCFF),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .testTag("bottom_navigation_bar")
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6750A4),
                    selectedTextColor = Color(0xFF6750A4),
                    indicatorColor = Color(0xFFEADDFF),
                    unselectedIconColor = Color(0xFF49454F),
                    unselectedTextColor = Color(0xFF49454F)
                )

                // Tab 1: Explore / Search
                NavigationBarItem(
                    selected = uiState.currentTab == NavigationTab.EXPLORE,
                    onClick = { viewModel.setNavigationTab(NavigationTab.EXPLORE) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == NavigationTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                            contentDescription = "Explorar"
                        )
                    },
                    label = {
                        Text(
                            text = "Explorar",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == NavigationTab.EXPLORE) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_tab_explore")
                )

                // Tab 2: Map
                NavigationBarItem(
                    selected = uiState.currentTab == NavigationTab.MAP,
                    onClick = { viewModel.setNavigationTab(NavigationTab.MAP) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == NavigationTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Mapa"
                        )
                    },
                    label = {
                        Text(
                            text = "Mapa",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == NavigationTab.MAP) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_tab_map")
                )

                // Tab 3: Favorites
                NavigationBarItem(
                    selected = uiState.currentTab == NavigationTab.FAVORITES,
                    onClick = { viewModel.setNavigationTab(NavigationTab.FAVORITES) },
                    icon = {
                        if (favorites.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFB3261E),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "${favorites.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.currentTab == NavigationTab.FAVORITES) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Favoritos"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (uiState.currentTab == NavigationTab.FAVORITES) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Favoritos"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Favoritos",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == NavigationTab.FAVORITES) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_tab_favorites")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                NavigationTab.EXPLORE -> {
                    ExploreScreen(
                        uiState = uiState,
                        onCategorySelected = { preset -> viewModel.selectCategoryPreset(preset) },
                        onCustomQueryChanged = { query -> viewModel.updateCustomQuery(query) },
                        onMinRatingChanged = { rating -> viewModel.setMinRating(rating) },
                        onSearchClicked = { viewModel.performSearch() },
                        onSelectForMap = { place -> viewModel.selectPlace(place, navigateToMap = true) },
                        onToggleFavorite = { place -> viewModel.toggleFavorite(place) },
                        onRefreshLocation = { viewModel.checkLocationAndInitialSearch() },
                        onViewAllOnMap = { viewModel.setNavigationTab(NavigationTab.MAP) }
                    )
                }

                NavigationTab.MAP -> {
                    InteractiveMapCanvas(
                        places = uiState.topPlaces,
                        selectedPlace = uiState.selectedPlace,
                        userLocation = uiState.currentLocation,
                        centerLat = uiState.mapCenterLat,
                        centerLng = uiState.mapCenterLng,
                        zoomLevel = uiState.mapZoomLevel,
                        onSelectPlace = { place -> viewModel.selectPlace(place) },
                        onToggleFavorite = { place -> viewModel.toggleFavorite(place) },
                        onRecenterUser = { viewModel.recenterOnUser() },
                        onRecenterPlaces = { viewModel.recenterOnPlaces() },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onPanMap = { deltaLat, deltaLng -> viewModel.updateMapPan(deltaLat, deltaLng) }
                    )
                }

                NavigationTab.FAVORITES -> {
                    FavoritesScreen(
                        favorites = favorites,
                        onSelectForMap = { place -> viewModel.selectPlace(place, navigateToMap = true) },
                        onRemoveFavorite = { placeId -> viewModel.removeFavoriteById(placeId) },
                        onOpenNoteDialog = { place -> viewModel.openNoteDialog(place) },
                        onExploreClicked = { viewModel.setNavigationTab(NavigationTab.EXPLORE) }
                    )
                }
            }
        }
    }

    // Personal Note Editing Dialog
    if (uiState.editingNotePlace != null) {
        EditNoteDialog(
            place = uiState.editingNotePlace!!,
            onSaveNote = { note -> viewModel.savePersonalNote(uiState.editingNotePlace!!.id, note) },
            onDismiss = { viewModel.closeNoteDialog() }
        )
    }
}
