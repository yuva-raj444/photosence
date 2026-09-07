package com.example.clipgallery.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.AlbumInfo
import com.example.clipgallery.R

class AlbumCheckboxAdapter(
    private val albums: List<AlbumInfo>,
    private val onToggle: (AlbumInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AlbumCheckboxAdapter.VH>() {

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val checkbox: CheckBox = v.findViewById(R.id.checkbox)
        val label: TextView = v.findViewById(R.id.txtLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_album_checkbox, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = albums[position]
        holder.label.text = "${album.name} (${album.count})"
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = false
        holder.checkbox.setOnCheckedChangeListener { _, checked -> onToggle(album, checked) }
    }

    override fun getItemCount() = albums.size
}
