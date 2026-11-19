package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenFoodFactsResponse(
    @Json(name = "code") val code: String? = null,
    @Json(name = "status") val status: Int = 0,
    @Json(name = "status_verbose") val statusVerbose: String? = null,
    @Json(name = "product") val product: OffProductDto? = null
)

@JsonClass(generateAdapter = true)
data class OffSearchResponse(
    @Json(name = "count") val count: Int = 0,
    @Json(name = "products") val products: List<OffProductDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OffProductDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_en") val productNameEn: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null,
    @Json(name = "ingredients_text_en") val ingredientsTextEn: String? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = null,
    @Json(name = "traces_tags") val tracesTags: List<String>? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "image_front_small_url") val imageFrontSmallUrl: String? = null
)

interface OpenFoodFactsService {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields")
        fields: String = "code,product_name,product_name_en,brands,ingredients_text,ingredients_text_en,allergens_tags,traces_tags,image_front_url,image_front_small_url"
    ): OpenFoodFactsResponse

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 15,
        @Query("fields")
        fields: String = "code,product_name,product_name_en,brands,ingredients_text,ingredients_text_en,allergens_tags,traces_tags,image_front_url,image_front_small_url"
    ): OffSearchResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): OpenFoodFactsService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("User-Agent", "GalSafeAndroid/1.0 (AlphaGal Barcode Scanner)")
                        .build()
                    chain.proceed(req)
                }
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OpenFoodFactsService::class.java)
        }
    }
}
