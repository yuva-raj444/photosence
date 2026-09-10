package com.example.clipgallery

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.clipgallery.data.AppDatabase
import com.example.clipgallery.data.IndexedAlbum
import com.example.clipgallery.data.PhotoEmbedding
import com.example.clipgallery.ml.ClipEmbedder
import com.example.clipgallery.ui.AppleTheme
import com.example.clipgallery.ui.IndexFoldersScreen
import com.example.clipgallery.ui.IndexFolderUi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IndexAlbumsActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var embedder: ClipEmbedder
    private var albums: List<AlbumInfo> = emptyList()
    private var selected by mutableStateOf<Set<Long>>(emptySet())
    private var folders by mutableStateOf<List<IndexFolderUi>>(emptyList())
    private var status by mutableStateOf("Select folders to index")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.get(this)
        setContent { AppleTheme { IndexFoldersScreen(folders, selected, status, onDone = { finish() }, onIndex = { indexSelected() }, onSelectionChanged = { selected = it }) } }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                embedder = ClipEmbedder(this@IndexAlbumsActivity)
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@IndexAlbumsActivity,
                        "Indexing unavailable: bundled model is not supported on this device",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            albums = MediaStoreHelper.getAlbums(this@IndexAlbumsActivity)
            withContext(Dispatchers.Main) {
                folders = albums.map { IndexFolderUi(it.id, it.name, it.count) }
                selected = db.dao().getIndexedAlbums().map { it.albumId }.toSet()
                status = "Ready"
            }
        }
    }

    private fun indexSelected() {
        val toIndex = albums.filter { it.id in selected }
        if (toIndex.isEmpty()) {
            Toast.makeText(this, "Select at least one album", Toast.LENGTH_SHORT).show()
            return
        }
        status = "Starting indexing..."
        lifecycleScope.launch(Dispatchers.IO) {
            for (album in toIndex) {
                db.dao().insertAlbum(IndexedAlbum(album.id, album.name, true))
                val photos = MediaStoreHelper.getPhotosInAlbum(this@IndexAlbumsActivity, album.id)
                photos.forEachIndexed { idx, photo ->
                    withContext(Dispatchers.Main) { status = "Indexing ${album.name}: ${idx + 1}/${photos.size}" }
                    val bitmap = MediaStoreHelper.getBitmap(this@IndexAlbumsActivity, photo.uri) ?: return@forEachIndexed
                    val embedding = embedder.embedImage(bitmap)
                    db.dao().insertPhoto(PhotoEmbedding(photo.id, album.id, embedding.toByteArray()))
                    bitmap.recycle()
                }
            }
            withContext(Dispatchers.Main) {
                status = "Done"
                Toast.makeText(this@IndexAlbumsActivity, "Indexing complete", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
