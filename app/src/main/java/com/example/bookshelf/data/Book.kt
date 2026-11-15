package com.example.bookshelf.data

data class BooksResponse(
    val items: List<BookItem>?
)

data class BookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

typealias BookDetails = BookItem

data class VolumeInfo(
    val title: String?,
    val imageLinks: ImageLinks?
)

data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?
)

data class Book(
    val id: String,
    val title: String,
    val thumbnail: String?
)