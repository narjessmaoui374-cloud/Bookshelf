package com.example.bookshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import coil.Coil
import coil.ImageLoader
import com.example.bookshelf.data.BooksApi
import com.example.bookshelf.data.NetworkBooksRepository
import com.example.bookshelf.ui.BookshelfScreen
import com.example.bookshelf.ui.theme.BookshelfTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(BooksApi.okHttpClient)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {

            BookshelfTheme {

                val repository = remember {
                    NetworkBooksRepository(
                        BooksApi.retrofitService
                    )
                }

                BookshelfScreen(
                    repository = repository
                )
            }
        }
    }
}