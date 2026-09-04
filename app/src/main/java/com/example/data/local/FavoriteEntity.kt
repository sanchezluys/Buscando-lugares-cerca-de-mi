package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val placeId: String,
    val name: String,
    val category: String,
    val categoryIcon: String,
    val rating: Double,
    val userRatingsTotal: Int,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val personalNote: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
