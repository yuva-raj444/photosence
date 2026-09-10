package com.example.clipgallery

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.clipgallery.data.AppDatabase
import com.example.clipgallery.ml.ClipEmbedder
import com.example.clipgallery.ui.AlbumInfoUi
import com.example.clipgallery.ui.AlbumsScreen
import com.example.clipgallery.ui.AppleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var embedder: ClipEmbedder
    private var albums: List<AlbumInfo> = emptyList()

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadAlbums() else Toast.makeText(this, "Photo permission is required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.get(this)
        setContent {
            AppleTheme {
                AlbumsScreen(
                    albums = albums.map { AlbumInfoUi(it.id, it.name, it.coverUri, it.count) },
                    onAlbumClick = { album -> startActivity(Intent(this, AlbumActivity::class.java).apply { putExtra("albumId", album.id); putExtra("albumName", album.name) }) },
                    onSearch = { query -> if (query.isNotBlank()) runSearch(query) },
                    onIndex = { startActivity(Intent(this, IndexAlbumsActivity::class.java)) },
                    onScope = { startActivity(Intent(this, SearchFilterActivity::class.java)) }
                )
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                embedder = ClipEmbedder(this@MainActivity)
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Semantic search unavailable: bundled model is not supported on this device",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                          else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAlbums()
        } else {
            requestPermission.launch(permission)
        }
    }

    private fun loadAlbums() {
        lifecycleScope.launch(Dispatchers.IO) {
            albums = MediaStoreHelper.getAlbums(this@MainActivity)
            withContext(Dispatchers.Main) { setContent { AppleTheme { AlbumsScreen(albums.map { AlbumInfoUi(it.id, it.name, it.coverUri, it.count) }, { album -> startActivity(Intent(this@MainActivity, AlbumActivity::class.java).apply { putExtra("albumId", album.id); putExtra("albumName", album.name) }) }, { query -> if (query.isNotBlank()) runSearch(query) }, { startActivity(Intent(this@MainActivity, IndexAlbumsActivity::class.java)) }, { startActivity(Intent(this@MainActivity, SearchFilterActivity::class.java)) }) } } }
        }
    }

    private fun runSearch(query: String) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            if (!::embedder.isInitialized) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Model still loading, try again", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            val queryEmb = embedder.embedText(query)
            val candidates = db.dao().getSearchableEmbeddings()
            if (candidates.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "No indexed albums enabled for search", Toast.LENGTH_LONG).show() }
                return@launch
            }
            val ranked = candidates
                .map { it to cosineSim(queryEmb, it.embedding.toFloatArray()) }
                .sortedByDescending { it.second }
                .take(100)
                .map { ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.first.photoId) }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, SearchResultsActivity::class.java)
                intent.putParcelableArrayListExtra("results", ArrayList<Uri>(ranked))
                intent.putExtra("query", query)
                startActivity(intent)
            }
        }
    }
}
