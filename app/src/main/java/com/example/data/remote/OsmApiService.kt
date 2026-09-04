package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OsmPlaceDto(
    @Json(name = "place_id") val placeId: Long = 0L,
    @Json(name = "osm_id") val osmId: Long? = null,
    @Json(name = "lat") val lat: String = "0.0",
    @Json(name = "lon") val lon: String = "0.0",
    @Json(name = "name") val name: String? = null,
    @Json(name = "display_name") val displayName: String = "",
    @Json(name = "class") val placeClass: String? = null,
    @Json(name = "type") val placeType: String? = null,
    @Json(name = "importance") val importance: Double? = null,
    @Json(name = "address") val address: OsmAddressDto? = null
)

@JsonClass(generateAdapter = true)
data class OsmAddressDto(
    @Json(name = "road") val road: String? = null,
    @Json(name = "house_number") val houseNumber: String? = null,
    @Json(name = "suburb") val suburb: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "town") val town: String? = null,
    @Json(name = "village") val village: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null
)

interface OsmApiService {

    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 15,
        @Query("viewbox") viewBox: String? = null,
        @Query("bounded") bounded: Int? = null,
        @Header("User-Agent") userAgent: String = "NearbyExplorerOpenApp/1.0 (Android; OpenStreetMap)"
    ): List<OsmPlaceDto>

    companion object {
        private const val BASE_URL = "https://nominatim.openstreetmap.org/"

        fun create(): OsmApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OsmApiService::class.java)
        }
    }
}
