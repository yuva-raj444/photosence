package com.example.clipgallery.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clipgallery.R
import com.example.clipgallery.data.IndexedAlbum

class AlbumToggleAdapter(
    private val albums: List<IndexedAlbum>,
    private val onToggle: (IndexedAlbum, Boolean) -> Unit
) : RecyclerView.Adapter<AlbumToggleAdapter.VH>() {

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val label: TextView = v.findViewById(R.id.txtLabel)
        val switch: Switch = v.findViewById(R.id.switchEnabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_album_toggle, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = albums[position]
        holder.label.text = album.albumName
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = album.isSearchEnabled
        holder.switch.setOnCheckedChangeListener { _, checked ->
            album.isSearchEnabled = checked
            onToggle(album, checked)
        }
    }

    override fun getItemCount() = albums.size
}
