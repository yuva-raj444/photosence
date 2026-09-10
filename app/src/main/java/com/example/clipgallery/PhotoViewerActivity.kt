package com.example.clipgallery

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.clipgallery.ui.AppleTheme
import com.example.clipgallery.ui.PhotoViewerScreen

class PhotoViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photos = intent.getParcelableArrayListExtra<Uri>("photos") ?: arrayListOf()
        val index = intent.getIntExtra("index", 0).coerceIn(0, (photos.size - 1).coerceAtLeast(0))
        setContent { AppleTheme { PhotoViewerScreen(photos, index) { finish() } } }
    }
}
