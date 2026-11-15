package com.example.bookshelf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bookshelf.data.Book
import com.example.bookshelf.data.BooksRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    repository: BooksRepository
) {
    var books by remember {
        mutableStateOf<List<Book>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var retryTrigger by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(repository, retryTrigger) {
        isLoading = true
        errorMessage = null
        try {
            books = repository.getBooks("jazz history")
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
                ?: "An error occurred."
        } finally {
            isLoading = false
        }
    }

    val brownColor = Color(0xFF5D4037)

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Bookshelf",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = brownColor
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = brownColor)
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { retryTrigger++ }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                books.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No books found.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { retryTrigger++ }
                            ) {
                                Text("Refresh")
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = books,
                            key = { book -> book.id }
                        ) { book ->
                            BookItemCard(book = book)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookItemCard(
    book: Book,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val imageRequest = remember(book.thumbnail) {
        val imageUrl = book.thumbnail
            ?.replace("http://", "https://")
            ?.replace("&edge=curl", "")
            ?.replace("edge=curl&", "")

        ImageRequest.Builder(context)
            .data(imageUrl)
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onClick() }
    ) {
        if (!book.thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            BookCoverPlaceholder(title = book.title)
        }
    }
}

@Composable
fun BookCoverPlaceholder(title: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFD7CCC8),
        shape = MaterialTheme.shapes.small
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF3E2723),
                textAlign = TextAlign.Center,
                maxLines = 4
            )
        }
    }
}