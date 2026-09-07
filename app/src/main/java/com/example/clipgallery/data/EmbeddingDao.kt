package com.example.clipgallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: IndexedAlbum)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEmbedding)

    @Query("SELECT * FROM indexed_albums")
    suspend fun getIndexedAlbums(): List<IndexedAlbum>

    @Query("UPDATE indexed_albums SET isSearchEnabled = :enabled WHERE albumId = :albumId")
    suspend fun setEnabled(albumId: Long, enabled: Boolean)

    @Query("SELECT * FROM photo_embeddings WHERE albumId IN (SELECT albumId FROM indexed_albums WHERE isSearchEnabled = 1)")
    suspend fun getSearchableEmbeddings(): List<PhotoEmbedding>
}
