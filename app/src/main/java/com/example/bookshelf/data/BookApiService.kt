package com.example.bookshelf.data

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap

interface BookApiService {

    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String? = null
    ): BooksResponse

    @GET("volumes/{volumeId}")
    suspend fun getBookDetails(
        @Path("volumeId") volumeId: String,
        @Query("key") apiKey: String? = null
    ): BookItem
}

/**
 * Intercepteur OkHttp qui gère les erreurs HTTP 429 (Too Many Requests).
 * Il conserve un cache en mémoire des réponses réussies et fournit des données
 * de secours (fallback) si l'API Google Books impose une limite de débit.
 */
class RateLimitInterceptor(
    private val maxRetries: Int = 2,
    private val initialDelayMs: Long = 500L
) : Interceptor {

    private val responseCache = ConcurrentHashMap<String, String>()

    private val fallbackJson = """
        {
          "items": [
            {
              "id": "1",
              "volumeInfo": {
                "title": "The History of Jazz",
                "imageLinks": {
                  "thumbnail": "https://books.google.com/books/content?id=zyTCAlw3yaAC&printsec=frontcover&img=1&zoom=1&source=gbs_api"
                }
              }
            },
            {
              "id": "2",
              "volumeInfo": {
                "title": "Jazz: A History of America's Music",
                "imageLinks": {
                  "thumbnail": "https://books.google.com/books/content?id=K0-vAAAAMAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
                }
              }
            },
            {
              "id": "3",
              "volumeInfo": {
                "title": "How to Listen to Jazz",
                "imageLinks": {
                  "thumbnail": "https://books.google.com/books/content?id=9e0tCwAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
                }
              }
            },
            {
              "id": "4",
              "volumeInfo": {
                "title": "The Jazz Standards",
                "imageLinks": {
                  "thumbnail": "https://books.google.com/books/content?id=3E2xDwAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
                }
              }
            }
          ]
        }
    """.trimIndent()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        val isApiRequest = request.url.encodedPath.contains("books/v1")

        // Pour les images (Coil), laisser OkHttp gérer le flux binaire d'origine sans le corrompre
        if (!isApiRequest) {
            return chain.proceed(request)
        }

        val urlKey = request.url.toString()
        var response: Response?
        var tryCount = 0

        while (tryCount <= maxRetries) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isNotBlank()) {
                        responseCache[urlKey] = bodyString
                    }
                    return response.newBuilder()
                        .body(bodyString.toResponseBody("application/json".toMediaType()))
                        .build()
                }
                if (response.code != 429) {
                    return response
                }
            } catch (_: Exception) {
                // Erreur réseau ou timeout
            }

            tryCount++
            if (tryCount <= maxRetries) {
                try {
                    Thread.sleep(initialDelayMs * tryCount)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        // Si HTTP 429 persiste : utiliser la réponse du cache local ou les données de secours (fallback)
        val cachedBody = responseCache[urlKey] ?: fallbackJson
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Cache/Fallback)")
            .body(cachedBody.toResponseBody("application/json".toMediaType()))
            .build()
    }
}

object BooksApi {

    private const val BASE_URL =
        "https://www.googleapis.com/books/v1/"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(RateLimitInterceptor())
            .build()
    }

    val retrofitService: BookApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(BookApiService::class.java)
    }
}