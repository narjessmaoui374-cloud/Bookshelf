package com.example.bookshelf.data

import retrofit2.HttpException

interface BooksRepository {
    suspend fun getBooks(query: String): List<Book>
    suspend fun getBookDetails(volumeId: String): BookItem?
}

class NetworkBooksRepository(
    private val bookApiService: BookApiService
) : BooksRepository {

    private val fallbackBooks = listOf(
        Book(
            id = "1",
            title = "Kotlin in Action",
            thumbnail = "https://covers.openlibrary.org/b/isbn/9781491950357-L.jpg"
        ),
        Book(
            id = "2",
            title = "Clean Code",
            thumbnail = "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg"
        ),
        Book(
            id = "3",
            title = "Effective Java",
            thumbnail = "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg"
        ),
        Book(
            id = "4",
            title = "Head First Design Patterns",
            thumbnail = "https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg"
        )
    )

    override suspend fun getBooks(query: String): List<Book> {
        return try {
            val response = bookApiService.searchBooks(query)
            val items = response.items.orEmpty().mapNotNull { item ->
                val title = item.volumeInfo.title ?: return@mapNotNull null

                var rawThumbnail = item.volumeInfo.imageLinks?.thumbnail
                    ?: item.volumeInfo.imageLinks?.smallThumbnail

                if (rawThumbnail == null) {
                    try {
                        val details = bookApiService.getBookDetails(item.id)
                        rawThumbnail = details.volumeInfo.imageLinks?.thumbnail
                            ?: details.volumeInfo.imageLinks?.smallThumbnail
                    } catch (_: Exception) {
                        // Ignorer si la récupération des détails échoue
                    }
                }

                val formattedThumbnail = rawThumbnail
                    ?.replace("http://", "https://")
                    ?.replace("&edge=curl", "")
                    ?.replace("edge=curl&", "")

                Book(
                    id = item.id,
                    title = title,
                    thumbnail = formattedThumbnail
                )
            }
            items.ifEmpty { fallbackBooks }
        } catch (_: HttpException) {
            fallbackBooks
        } catch (_: Exception) {
            fallbackBooks
        }
    }

    override suspend fun getBookDetails(volumeId: String): BookItem? {
        return try {
            bookApiService.getBookDetails(volumeId)
        } catch (_: Exception) {
            null
        }
    }
}