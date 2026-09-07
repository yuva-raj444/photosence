package com.example.clipgallery.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.clipgallery.R

class PhotoAdapter(private val photos: List<Uri>) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.img).load(photos[position]).centerCrop().into(holder.img)
    }

    override fun getItemCount() = photos.size
}
