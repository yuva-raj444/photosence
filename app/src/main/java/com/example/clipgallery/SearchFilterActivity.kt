package com.example.clipgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.clipgallery.adapters.AlbumToggleAdapter
import com.example.clipgallery.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.clipgallery.ui.AppleTheme
import com.example.clipgallery.ui.SearchSourceUi
import com.example.clipgallery.ui.SearchScopeScreen
import androidx.compose.runtime.mutableStateOf

class SearchFilterActivity : ComponentActivity() {
    private val sources = mutableStateOf<List<SearchSourceUi>>(emptyList())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        setContent { AppleTheme { SearchScopeScreen(sources.value, { id, enabled -> sources.value = sources.value.map { if (it.id == id) it.copy(enabled = enabled) else it }; lifecycleScope.launch(Dispatchers.IO) { db.dao().setEnabled(id, enabled) } }, { finish() }) } }

        lifecycleScope.launch(Dispatchers.IO) {
            val indexed = db.dao().getIndexedAlbums().associateBy { it.albumId }
            val albums = MediaStoreHelper.getAlbums(this@SearchFilterActivity)
            withContext(Dispatchers.Main) {
                sources.value = albums.map { album ->
                    val indexedAlbum = indexed[album.id]
                    SearchSourceUi(album.id, album.name, "${album.count} photos", indexedAlbum?.isSearchEnabled ?: false)
                }
            }
        }
    }
}
