package com.example.model

data class Place(
    val id: String,
    val name: String,
    val category: String,
    val categoryIcon: String = "📍",
    val rating: Double,
    val userRatingsTotal: Int,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String? = null,
    val isOpenNow: Boolean? = true,
    val priceLevel: Int? = 2,
    val distanceMeters: Int = 0,
    val isFavorite: Boolean = false,
    val personalNote: String? = null,
    val phoneNumber: String? = null,
    val googleMapsUrl: String = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
)

data class SearchFilter(
    val categoryKey: String = "hamburguesas",
    val categoryName: String = "Hamburguesas",
    val query: String = "quiero comer hamburguesas",
    val minRating: Double = 4.0,
    val radiusMeters: Int = 5000,
    val userLatitude: Double = 19.4326,
    val userLongitude: Double = -99.1332,
    val apiKey: String = ""
)

data class CategoryPreset(
    val id: String,
    val title: String,
    val query: String,
    val icon: String,
    val googleType: String
)
