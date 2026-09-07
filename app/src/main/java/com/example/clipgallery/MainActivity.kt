package com.example.clipgallery

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.adapters.AlbumAdapter
import com.example.clipgallery.data.AppDatabase
import com.example.clipgallery.ml.ClipEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var embedder: ClipEmbedder
    private lateinit var recyclerView: RecyclerView
    private var albums: List<AlbumInfo> = emptyList()

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadAlbums() else Toast.makeText(this, "Photo permission is required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.get(this)
        recyclerView = findViewById(R.id.recyclerAlbums)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) runSearch(query)
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_index_albums -> startActivity(Intent(this, IndexAlbumsActivity::class.java))
            R.id.action_search_filter -> startActivity(Intent(this, SearchFilterActivity::class.java))
        }
        return true
    }

    private fun loadAlbums() {
        lifecycleScope.launch(Dispatchers.IO) {
            albums = MediaStoreHelper.getAlbums(this@MainActivity)
            withContext(Dispatchers.Main) {
                recyclerView.adapter = AlbumAdapter(albums) { album ->
                    val intent = Intent(this@MainActivity, AlbumActivity::class.java)
                    intent.putExtra("albumId", album.id)
                    intent.putExtra("albumName", album.name)
                    startActivity(intent)
                }
            }
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
                startActivity(intent)
            }
        }
    }
}
