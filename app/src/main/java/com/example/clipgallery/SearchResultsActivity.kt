package com.example.clipgallery

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.adapters.PhotoAdapter

class SearchResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)
        title = "Search Results"

        val results: ArrayList<Uri> = intent.getParcelableArrayListExtra("results") ?: arrayListOf()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerPhotos)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = PhotoAdapter(results)
    }
}
