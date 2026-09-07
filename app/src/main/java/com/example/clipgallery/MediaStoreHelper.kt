package com.example.clipgallery

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore

data class AlbumInfo(val id: Long, val name: String, val coverUri: Uri, val count: Int)
data class PhotoInfo(val id: Long, val uri: Uri)

object MediaStoreHelper {

    fun getAlbums(context: Context): List<AlbumInfo> {
        val ids = LinkedHashMap<Long, MutableList<Long>>()
        val names = HashMap<Long, String>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val photoId = cursor.getLong(idCol)
                val bucketId = cursor.getLong(bucketIdCol)
                names[bucketId] = cursor.getString(bucketNameCol) ?: "Unknown"
                ids.getOrPut(bucketId) { mutableListOf() }.add(photoId)
            }
        }
        return ids.map { (bucketId, photoIds) ->
            val cover = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoIds.first())
            AlbumInfo(bucketId, names[bucketId] ?: "Unknown", cover, photoIds.size)
        }
    }

    fun getPhotosInAlbum(context: Context, albumId: Long): List<PhotoInfo> {
        val result = mutableListOf<PhotoInfo>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
            "${MediaStore.Images.Media.BUCKET_ID} = ?", arrayOf(albumId.toString()),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result.add(PhotoInfo(id, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)))
            }
        }
        return result
    }

    fun getBitmap(context: Context, uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight, 224)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        null
    }

    private fun calculateSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) {
            sample *= 2
        }
        return sample
    }
}
