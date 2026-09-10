package com.example.clipgallery

import android.net.Uri
import android.os.Bundle
import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.clipgallery.data.AppDatabase
import com.example.clipgallery.ml.ClipEmbedder
import com.example.clipgallery.ui.AppleTheme
import com.example.clipgallery.ui.SearchResultsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchResultsActivity : ComponentActivity() {
    private lateinit var db: AppDatabase
    private lateinit var embedder: ClipEmbedder
    private var results by mutableStateOf<List<Uri>>(emptyList())
    private var query by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.get(this)
        results = intent.getParcelableArrayListExtra("results") ?: arrayListOf()
        query = intent.getStringExtra("query").orEmpty()
        lifecycleScope.launch(Dispatchers.IO) { embedder = ClipEmbedder(this@SearchResultsActivity) }
        setContent {
            AppleTheme {
                SearchResultsScreen(
                    results = results,
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { search(query) },
                    onCancel = { finish() },
                    onPhotoClick = { index -> startActivity(Intent(this@SearchResultsActivity, PhotoViewerActivity::class.java).apply { putParcelableArrayListExtra("photos", ArrayList(results)); putExtra("index", index) }) }
                )
            }
        }
    }

    private fun search(text: String) {
        if (text.isBlank()) return
        lifecycleScope.launch(Dispatchers.IO) {
            if (!::embedder.isInitialized) return@launch
            val queryEmbedding = embedder.embedText(text)
            val ranked = db.dao().getSearchableEmbeddings()
                .map { it to cosineSim(queryEmbedding, it.embedding.toFloatArray()) }
                .sortedByDescending { it.second }
                .take(100)
                .map { ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.first.photoId) }
            withContext(Dispatchers.Main) { results = ranked }
        }
    }
}
