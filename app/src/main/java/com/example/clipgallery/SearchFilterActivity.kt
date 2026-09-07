package com.example.clipgallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.adapters.AlbumToggleAdapter
import com.example.clipgallery.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFilterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_filter)
        val db = AppDatabase.get(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerToggle)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val indexed = db.dao().getIndexedAlbums()
            withContext(Dispatchers.Main) {
                recyclerView.adapter = AlbumToggleAdapter(indexed) { album, enabled ->
                    lifecycleScope.launch(Dispatchers.IO) { db.dao().setEnabled(album.albumId, enabled) }
                }
            }
        }
    }
}
