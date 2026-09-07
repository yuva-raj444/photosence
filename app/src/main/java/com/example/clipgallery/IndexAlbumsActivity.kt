package com.example.clipgallery

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.adapters.AlbumCheckboxAdapter
import com.example.clipgallery.data.AppDatabase
import com.example.clipgallery.data.IndexedAlbum
import com.example.clipgallery.data.PhotoEmbedding
import com.example.clipgallery.ml.ClipEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IndexAlbumsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var embedder: ClipEmbedder
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private var albums: List<AlbumInfo> = emptyList()
    private val selected = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index_albums)
        db = AppDatabase.get(this)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerAlbumsCheck)
        recyclerView.layoutManager = LinearLayoutManager(this)

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
                recyclerView.adapter = AlbumCheckboxAdapter(albums) { album, checked ->
                    if (checked) selected.add(album.id) else selected.remove(album.id)
                }
            }
        }

        findViewById<Button>(R.id.btnIndex).setOnClickListener { indexSelected() }
    }

    private fun indexSelected() {
        val toIndex = albums.filter { it.id in selected }
        if (toIndex.isEmpty()) {
            Toast.makeText(this, "Select at least one album", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            for (album in toIndex) {
                db.dao().insertAlbum(IndexedAlbum(album.id, album.name, true))
                val photos = MediaStoreHelper.getPhotosInAlbum(this@IndexAlbumsActivity, album.id)
                photos.forEachIndexed { idx, photo ->
                    withContext(Dispatchers.Main) {
                        statusText.text = "Indexing ${album.name}: ${idx + 1}/${photos.size}"
                    }
                    val bitmap = MediaStoreHelper.getBitmap(this@IndexAlbumsActivity, photo.uri) ?: return@forEachIndexed
                    val embedding = embedder.embedImage(bitmap)
                    db.dao().insertPhoto(PhotoEmbedding(photo.id, album.id, embedding.toByteArray()))
                    bitmap.recycle()
                }
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                statusText.text = "Done"
                Toast.makeText(this@IndexAlbumsActivity, "Indexing complete", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
