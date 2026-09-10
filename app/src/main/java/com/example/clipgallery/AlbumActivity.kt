package com.example.clipgallery

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.clipgallery.ui.AppleTheme
import com.example.clipgallery.ui.GalleryGridScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val albumId = intent.getLongExtra("albumId", -1)
        val albumName = intent.getStringExtra("albumName").orEmpty()
        setContent { AppleTheme { GalleryGridScreen(albumName, emptyList()) {} } }

        lifecycleScope.launch(Dispatchers.IO) {
            val photos = MediaStoreHelper.getPhotosInAlbum(this@AlbumActivity, albumId)
            withContext(Dispatchers.Main) {
                val uris = photos.map { it.uri }
                setContent { AppleTheme { GalleryGridScreen("", uris) { index -> startActivity(Intent(this@AlbumActivity, PhotoViewerActivity::class.java).apply { putParcelableArrayListExtra("photos", ArrayList(uris)); putExtra("index", index) }) } } }
            }
        }
    }
}
