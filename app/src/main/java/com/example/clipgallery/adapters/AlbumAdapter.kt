package com.example.clipgallery.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.clipgallery.AlbumInfo
import com.example.clipgallery.R

class AlbumAdapter(
    private val albums: List<AlbumInfo>,
    private val onClick: (AlbumInfo) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.VH>() {

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgCover)
        val name: TextView = v.findViewById(R.id.txtName)
        val count: TextView = v.findViewById(R.id.txtCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = albums[position]
        holder.name.text = album.name
        holder.count.text = "${album.count} photos"
        Glide.with(holder.img).load(album.coverUri).centerCrop().into(holder.img)
        holder.itemView.setOnClickListener { onClick(album) }
    }

    override fun getItemCount() = albums.size
}
