package com.example.clipgallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indexed_albums")
data class IndexedAlbum(
    @PrimaryKey val albumId: Long,
    val albumName: String,
    var isSearchEnabled: Boolean = true
)

@Entity(tableName = "photo_embeddings")
data class PhotoEmbedding(
    @PrimaryKey val photoId: Long,
    val albumId: Long,
    val embedding: ByteArray
)
