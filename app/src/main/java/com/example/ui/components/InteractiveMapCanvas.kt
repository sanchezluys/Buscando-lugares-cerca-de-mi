package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.location.Coordinates
import com.example.model.Place
import com.example.ui.theme.AmberStar
import com.example.ui.util.NavigationUtil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveMapCanvas(
    places: List<Place>,
    selectedPlace: Place?,
    userLocation: Coordinates,
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Float,
    onSelectPlace: (Place) -> Unit,
    onToggleFavorite: (Place) -> Unit,
    onRecenterUser: () -> Unit,
    onRecenterPlaces: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPanMap: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // Map style mode: 0 = Standard road, 1 = Satellite terrain dark, 2 = Minimal
    var mapThemeIndex by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("interactive_map_container")
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Scaling factor: degrees to screen pixels
        // 1 degree latitude is approx 111 km. At zoomLevel 1.0, 0.01 deg is ~300 pixels
        val pixelsPerDegree = 35000f * zoomLevel

        fun latLngToScreen(lat: Double, lng: Double): Offset {
            val deltaLng = (lng - centerLng) * cos(Math.toRadians(centerLat))
            val deltaLat = lat - centerLat

            val x = containerWidth / 2f + (deltaLng * pixelsPerDegree).toFloat()
            val y = containerHeight / 2f - (deltaLat * pixelsPerDegree).toFloat()
            return Offset(x, y)
        }

        fun screenToLatLng(x: Float, y: Float): Pair<Double, Double> {
            val deltaX = x - containerWidth / 2f
            val deltaY = y - containerHeight / 2f

            val deltaLat = -deltaY / pixelsPerDegree
            val deltaLng = deltaX / (pixelsPerDegree * cos(Math.toRadians(centerLat)).toFloat())

            return Pair(centerLat + deltaLat, centerLng + deltaLng)
        }

        // Canvas with Pan & Tap gestures
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(centerLat, centerLng, zoomLevel) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val latOffset = (pan.y / pixelsPerDegree).toDouble()
                        val lngOffset = (-pan.x / (pixelsPerDegree * cos(Math.toRadians(centerLat)).toFloat())).toDouble()
                        onPanMap(latOffset, lngOffset)
                    }
                }
                .pointerInput(places, centerLat, centerLng, zoomLevel) {
                    detectTapGestures { tapOffset ->
                        // Check if tap is near any place pin (within 35dp radius)
                        val pinRadiusPx = 35.dp.toPx()
                        var tappedPlace: Place? = null
                        for (place in places) {
                            val pinPos = latLngToScreen(place.latitude, place.longitude)
                            val dist = (tapOffset - pinPos).getDistance()
                            if (dist <= pinRadiusPx) {
                                tappedPlace = place
                                break
                            }
                        }
                        if (tappedPlace != null) {
                            onSelectPlace(tappedPlace)
                        }
                    }
                }
        ) {
            // 1. Draw Map Background & stylized urban grid
            drawMapStyling(
                themeIndex = mapThemeIndex,
                center = Offset(size.width / 2f, size.height / 2f),
                zoomLevel = zoomLevel,
                textMeasurer = textMeasurer
            )

            // 2. Draw Distance Concentric Rings around User Location
            val userScreenPos = latLngToScreen(userLocation.latitude, userLocation.longitude)
            drawDistanceRings(
                userPos = userScreenPos,
                pixelsPerDegree = pixelsPerDegree,
                textMeasurer = textMeasurer
            )

            // 3. Draw Route Connection from User to Selected Place
            if (selectedPlace != null) {
                val selectedPos = latLngToScreen(selectedPlace.latitude, selectedPlace.longitude)
                drawRoutePath(
                    from = userScreenPos,
                    to = selectedPos,
                    selectedPlace = selectedPlace,
                    textMeasurer = textMeasurer
                )
            }

            // 4. Draw Place Pins (#1 to #5)
            places.forEachIndexed { index, place ->
                val pinPos = latLngToScreen(place.latitude, place.longitude)
                val isSelected = place.id == selectedPlace?.id
                val rank = index + 1

                drawPlacePin(
                    pos = pinPos,
                    rank = rank,
                    isSelected = isSelected,
                    textMeasurer = textMeasurer
                )
            }

            // 5. Draw User Location Indicator
            drawUserLocationMarker(
                pos = userScreenPos
            )
        }

        // Top Controls: Quick Place Selection Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            // Chips row for the 5 places
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(places) { index, place ->
                    val isSelected = place.id == selectedPlace?.id
                    val rank = index + 1
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .clickable { onSelectPlace(place) }
                            .testTag("map_chip_rank_$rank")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (rank == 1) "🏆 #1" else "#$rank",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else if (rank == 1) AmberStar else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = place.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Right side map action buttons (+ / - / Recenter)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zoom In
            FilledIconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_in_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Acercar mapa")
            }

            // Zoom Out
            FilledIconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_out_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Alejar mapa")
            }

            // Recenter on GPS location
            FilledIconButton(
                onClick = onRecenterUser,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_recenter_user_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = Color(0xFF6750A4)
                )
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
            }

            // Center on all 5 places
            FilledIconButton(
                onClick = onRecenterPlaces,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_recenter_places_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = AmberStar
                )
            ) {
                Icon(Icons.Default.NearMe, contentDescription = "Ver los 5 sitios")
            }

            // Toggle visual map theme
            FilledIconButton(
                onClick = { mapThemeIndex = (mapThemeIndex + 1) % 3 },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_toggle_theme_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Cambiar estilo de mapa")
            }
        }

        // Floating Place Card at bottom when a place is selected
        AnimatedVisibility(
            visible = selectedPlace != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            if (selectedPlace != null) {
                val rankIndex = places.indexOfFirst { it.id == selectedPlace.id }
                val rank = if (rankIndex != -1) rankIndex + 1 else 1

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_selected_place_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image Thumbnail with rank badge
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (selectedPlace.photoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(selectedPlace.photoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = selectedPlace.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedPlace.categoryIcon, fontSize = 32.sp)
                                }
                            }

                            // Rank badge overlay
                            Surface(
                                color = if (rank == 1) AmberStar else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "#$rank",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Info Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = selectedPlace.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AmberStar,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${selectedPlace.rating}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${selectedPlace.category}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = selectedPlace.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Action buttons: Google Maps + Favorite
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ElevatedButton(
                                    onClick = {
                                        NavigationUtil.openInGoogleMaps(
                                            context,
                                            selectedPlace.latitude,
                                            selectedPlace.longitude,
                                            selectedPlace.name
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("map_card_navigate_button")
                                ) {
                                    Icon(
                                        Icons.Default.Directions,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cómo llegar", fontSize = 12.sp)
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    IconButton(
                                        onClick = { onToggleFavorite(selectedPlace) },
                                        modifier = Modifier.testTag("map_card_favorite_button")
                                    ) {
                                        Icon(
                                            imageVector = if (selectedPlace.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Guardar",
                                            tint = if (selectedPlace.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Draw realistic styled map canvas with streets, greenery, and blocks
private fun DrawScope.drawMapStyling(
    themeIndex: Int,
    center: Offset,
    zoomLevel: Float,
    textMeasurer: TextMeasurer
) {
    val bgColor = when (themeIndex) {
        1 -> Color(0xFF192231) // Dark satellite mode
        2 -> Color(0xFFF3F4F6) // Minimal light mode
        else -> Color(0xFFEBF1F6) // Google Maps style soft canvas
    }

    val streetColor = when (themeIndex) {
        1 -> Color(0xFF283548)
        2 -> Color(0xFFE5E7EB)
        else -> Color(0xFFFFFFFF)
    }

    val mainAvenueColor = when (themeIndex) {
        1 -> Color(0xFF384B66)
        2 -> Color(0xFFD1D5DB)
        else -> Color(0xFFFFEBBA) // Google Maps classic yellow avenue
    }

    val parkColor = when (themeIndex) {
        1 -> Color(0xFF1E3A2F)
        2 -> Color(0xFFE2E8F0)
        else -> Color(0xFFD0E8D7) // Soft green park
    }

    val waterColor = when (themeIndex) {
        1 -> Color(0xFF152A3F)
        2 -> Color(0xFFDBEAFE)
        else -> Color(0xFFCCE2FC) // Google Maps blue water
    }

    // Base background
    drawRect(color = bgColor)

    // Decorative parks / greenery patches
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(size.width * 0.08f, size.height * 0.15f),
        size = Size(size.width * 0.28f, size.height * 0.22f),
        cornerRadius = CornerRadius(16f, 16f)
    )

    drawRoundRect(
        color = parkColor,
        topLeft = Offset(size.width * 0.65f, size.height * 0.55f),
        size = Size(size.width * 0.30f, size.height * 0.32f),
        cornerRadius = CornerRadius(20f, 20f)
    )

    // Water canal / lake
    val waterPath = Path().apply {
        moveTo(0f, size.height * 0.72f)
        cubicTo(
            size.width * 0.3f, size.height * 0.78f,
            size.width * 0.6f, size.height * 0.65f,
            size.width, size.height * 0.74f
        )
        lineTo(size.width, size.height * 0.79f)
        cubicTo(
            size.width * 0.6f, size.height * 0.70f,
            size.width * 0.3f, size.height * 0.83f,
            0f, size.height * 0.77f
        )
        close()
    }
    drawPath(waterPath, color = waterColor)

    // Secondary city street grid
    val spacing = 75f * zoomLevel
    var currentX = 0f
    while (currentX < size.width) {
        drawLine(
            color = streetColor,
            start = Offset(currentX, 0f),
            end = Offset(currentX, size.height),
            strokeWidth = 6f
        )
        currentX += spacing
    }

    var currentY = 0f
    while (currentY < size.height) {
        drawLine(
            color = streetColor,
            start = Offset(0f, currentY),
            end = Offset(size.width, currentY),
            strokeWidth = 6f
        )
        currentY += spacing
    }

    // Main diagonal and cross avenues
    drawLine(
        color = mainAvenueColor,
        start = Offset(0f, size.height * 0.45f),
        end = Offset(size.width, size.height * 0.45f),
        strokeWidth = 14f
    )

    drawLine(
        color = mainAvenueColor,
        start = Offset(size.width * 0.5f, 0f),
        end = Offset(size.width * 0.5f, size.height),
        strokeWidth = 14f
    )

    drawLine(
        color = mainAvenueColor,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = 10f
    )

    // Subtle Google Maps watermark label
    val labelResult = textMeasurer.measure(
        text = "Google Maps • Escala interactiva",
        style = TextStyle(
            color = if (themeIndex == 1) Color.Gray.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    )
    drawText(labelResult, topLeft = Offset(16f, size.height - 30f))
}

// Draw distance rings around user
private fun DrawScope.drawDistanceRings(
    userPos: Offset,
    pixelsPerDegree: Float,
    textMeasurer: TextMeasurer
) {
    // Approx degrees per meter: 1m = 1 / 111000 degrees
    val m500Radius = (500f / 111000f) * pixelsPerDegree
    val m1000Radius = (1000f / 111000f) * pixelsPerDegree
    val m2000Radius = (2000f / 111000f) * pixelsPerDegree

    val ringColor = Color(0xFF1D61E0).copy(alpha = 0.18f)
    val ringStroke = Stroke(
        width = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
    )

    if (m500Radius > 20f && m500Radius < size.width * 1.5f) {
        drawCircle(
            color = ringColor,
            radius = m500Radius,
            center = userPos,
            style = ringStroke
        )
        val text500 = textMeasurer.measure(
            text = "500 m",
            style = TextStyle(color = Color(0xFF1D61E0).copy(alpha = 0.6f), fontSize = 9.sp)
        )
        drawText(text500, topLeft = Offset(userPos.x + 8f, userPos.y - m500Radius - 14f))
    }

    if (m1000Radius > 40f && m1000Radius < size.width * 2f) {
        drawCircle(
            color = ringColor,
            radius = m1000Radius,
            center = userPos,
            style = ringStroke
        )
        val text1k = textMeasurer.measure(
            text = "1.0 km",
            style = TextStyle(color = Color(0xFF1D61E0).copy(alpha = 0.6f), fontSize = 9.sp)
        )
        drawText(text1k, topLeft = Offset(userPos.x + 8f, userPos.y - m1000Radius - 14f))
    }

    if (m2000Radius > 60f && m2000Radius < size.width * 3f) {
        drawCircle(
            color = ringColor,
            radius = m2000Radius,
            center = userPos,
            style = ringStroke
        )
    }
}

// Draw dashed route from user to place
private fun DrawScope.drawRoutePath(
    from: Offset,
    to: Offset,
    selectedPlace: Place,
    textMeasurer: TextMeasurer
) {
    // Draw route shadow
    drawLine(
        color = Color(0x33000000),
        start = from + Offset(2f, 2f),
        end = to + Offset(2f, 2f),
        strokeWidth = 6f
    )

    // Draw main dashed path
    drawLine(
        color = Color(0xFF1D61E0),
        start = from,
        end = to,
        strokeWidth = 4f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
    )

    // Midpoint distance pill
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
    val distStr = if (selectedPlace.distanceMeters < 1000) {
        "${selectedPlace.distanceMeters} m"
    } else {
        String.format("%.1f km", selectedPlace.distanceMeters / 1000.0)
    }

    val distText = textMeasurer.measure(
        text = distStr,
        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )

    val pillWidth = distText.size.width + 16f
    val pillHeight = distText.size.height + 8f

    drawRoundRect(
        color = Color(0xFF6750A4),
        topLeft = Offset(mid.x - pillWidth / 2f, mid.y - pillHeight / 2f),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(10f, 10f)
    )

    drawText(
        textLayoutResult = distText,
        topLeft = Offset(mid.x - distText.size.width / 2f, mid.y - distText.size.height / 2f)
    )
}

// Draw Place Pin (with teardrop/marker pin shape and ranking badge #1 to #5)
private fun DrawScope.drawPlacePin(
    pos: Offset,
    rank: Int,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val pinColor = when (rank) {
        1 -> Color(0xFFFFB800) // Gold for #1
        2 -> Color(0xFF757575) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFF6750A4) // Sleek Purple
    }

    val pinRadius = if (isSelected) 22f else 17f
    val pinHeight = if (isSelected) 54f else 42f

    // If selected, draw glowing ring
    if (isSelected) {
        drawCircle(
            color = pinColor.copy(alpha = 0.35f),
            radius = pinRadius * 1.8f,
            center = pos - Offset(0f, pinHeight * 0.55f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = pinRadius * 1.3f,
            center = pos - Offset(0f, pinHeight * 0.55f),
            style = Stroke(width = 3f)
        )
    }

    // Shadow at the base
    drawOval(
        color = Color(0x44000000),
        topLeft = Offset(pos.x - 14f, pos.y - 4f),
        size = Size(28f, 10f)
    )

    // Pin shape
    val pinCenter = Offset(pos.x, pos.y - pinHeight * 0.65f)

    val pinPath = Path().apply {
        moveTo(pos.x, pos.y) // stem tip
        cubicTo(
            pos.x - pinRadius * 0.8f, pos.y - pinHeight * 0.35f,
            pos.x - pinRadius, pinCenter.y + pinRadius * 0.5f,
            pos.x - pinRadius, pinCenter.y
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                pinCenter.x - pinRadius,
                pinCenter.y - pinRadius,
                pinCenter.x + pinRadius,
                pinCenter.y + pinRadius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        cubicTo(
            pos.x + pinRadius, pinCenter.y + pinRadius * 0.5f,
            pos.x + pinRadius * 0.8f, pos.y - pinHeight * 0.35f,
            pos.x, pos.y
        )
        close()
    }

    // Draw pin fill & stroke
    drawPath(pinPath, color = pinColor)
    drawPath(pinPath, color = Color.White, style = Stroke(width = 2.5f))

    // Inner white circle for rank text
    drawCircle(
        color = Color.White,
        radius = pinRadius * 0.62f,
        center = pinCenter
    )

    // Rank text inside pin ("#1", "#2", etc.)
    val rankText = textMeasurer.measure(
        text = "$rank",
        style = TextStyle(
            color = if (rank == 1) Color(0xFFB45309) else Color(0xFF0F172A),
            fontSize = (if (isSelected) 13 else 11).sp,
            fontWeight = FontWeight.Black
        )
    )
    drawText(
        textLayoutResult = rankText,
        topLeft = Offset(
            pinCenter.x - rankText.size.width / 2f,
            pinCenter.y - rankText.size.height / 2f
        )
    )
}

// Draw user current location indicator
private fun DrawScope.drawUserLocationMarker(pos: Offset) {
    // Outer pulsing wave
    drawCircle(
        color = Color(0x336750A4),
        radius = 28f,
        center = pos
    )

    // White rim
    drawCircle(
        color = Color.White,
        radius = 13f,
        center = pos
    )

    // Sleek Purple core
    drawCircle(
        color = Color(0xFF6750A4),
        radius = 9f,
        center = pos
    )

    // Accuracy direction pointer tip
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = pos + Offset(0f, -6f)
    )
}
