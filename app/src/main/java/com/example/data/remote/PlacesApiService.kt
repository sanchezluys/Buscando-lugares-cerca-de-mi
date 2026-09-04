package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PlacesSearchResponse(
    @Json(name = "results") val results: List<PlaceResultDto> = emptyList(),
    @Json(name = "status") val status: String = "",
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaceResultDto(
    @Json(name = "place_id") val placeId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "user_ratings_total") val userRatingsTotal: Int? = null,
    @Json(name = "vicinity") val vicinity: String? = null,
    @Json(name = "formatted_address") val formattedAddress: String? = null,
    @Json(name = "geometry") val geometry: GeometryDto? = null,
    @Json(name = "photos") val photos: List<PhotoDto>? = null,
    @Json(name = "opening_hours") val openingHours: OpeningHoursDto? = null,
    @Json(name = "price_level") val priceLevel: Int? = null,
    @Json(name = "types") val types: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeometryDto(
    @Json(name = "location") val location: LatLngDto? = null
)

@JsonClass(generateAdapter = true)
data class LatLngDto(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class PhotoDto(
    @Json(name = "photo_reference") val photoReference: String = "",
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpeningHoursDto(
    @Json(name = "open_now") val openNow: Boolean? = null
)

interface PlacesApiService {

    @GET("maps/api/place/nearbysearch/json")
    suspend fun searchNearby(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("keyword") keyword: String,
        @Query("type") type: String?,
        @Query("key") apiKey: String
    ): PlacesSearchResponse

    @GET("maps/api/place/textsearch/json")
    suspend fun searchText(
        @Query("query") query: String,
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("key") apiKey: String
    ): PlacesSearchResponse

    companion object {
        private const val BASE_URL = "https://maps.googleapis.com/"

        fun create(): PlacesApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(PlacesApiService::class.java)
        }

        fun getPhotoUrl(photoReference: String, apiKey: String, maxWidth: Int = 800): String {
            return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidth&photo_reference=$photoReference&key=$apiKey"
        }
    }
}
