package com.example.clipgallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.adapters.PhotoAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)
        val albumId = intent.getLongExtra("albumId", -1)
        title = intent.getStringExtra("albumName")

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerPhotos)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch(Dispatchers.IO) {
            val photos = MediaStoreHelper.getPhotosInAlbum(this@AlbumActivity, albumId)
            withContext(Dispatchers.Main) {
                recyclerView.adapter = PhotoAdapter(photos.map { it.uri })
            }
        }
    }
}
