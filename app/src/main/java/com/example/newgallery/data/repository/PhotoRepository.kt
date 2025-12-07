package com.example.newgallery.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.newgallery.data.model.Album
import com.example.newgallery.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Repository for handling photo data
 */
class PhotoRepository(private val context: Context) {
    
    // Cache for photos to avoid repeated queries
    private var photosCache: List<Photo>? = null
    private var photosCacheTimestamp: Long = 0
    
    // Cache expiration time (5 minutes)
    private val CACHE_EXPIRATION_TIME = 5 * 60 * 1000L
    
    /**
     * Get all photos from device, sorted by date taken (newest first)
     */
    suspend fun getAllPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        // Check if cache is valid
        val now = System.currentTimeMillis()
        if (photosCache != null && now - photosCacheTimestamp < CACHE_EXPIRATION_TIME) {
            return@withContext photosCache!!
        }
        
        val photos = mutableListOf<Photo>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val dateTaken = Date(cursor.getLong(dateTakenColumn))
                val dateModified = Date(cursor.getLong(dateModifiedColumn) * 1000) // Convert seconds to milliseconds
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                photos.add(
                    Photo(
                        id = id,
                        uri = uri,
                        displayName = displayName,
                        mimeType = mimeType,
                        dateTaken = dateTaken,
                        dateModified = dateModified,
                        width = width,
                        height = height,
                        size = size,
                        bucketId = bucketId,
                        bucketName = bucketName
                    )
                )
            }
        }
        
        // Update cache
        photosCache = photos
        photosCacheTimestamp = now
        
        photos
    }
    
    /**
     * Get photos grouped by year
     */
    suspend fun getPhotosGroupedByYear(): Map<Int, List<Photo>> {
        val photos = getAllPhotos()
        return photos.groupBy { it.year }
    }
    
    /**
     * Clear cache to force fresh data loading
     */
    fun clearCache() {
        photosCache = null
        photosCacheTimestamp = 0
    }
    
    /**
     * Get all albums from device
     */
    suspend fun getAllAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        
        // First, get all unique bucket IDs and names
        val bucketProjection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID
        )
        
        val bucketSortOrder = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
        
        val buckets = mutableMapOf<String, Pair<String, Long>>() // bucketId to (bucketName, coverPhotoId)
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            bucketProjection,
            null,
            null,
            bucketSortOrder
        )?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                val photoId = cursor.getLong(idColumn)
                
                // Only keep the first photo as cover
                if (!buckets.containsKey(bucketId)) {
                    buckets[bucketId] = Pair(bucketName, photoId)
                }
            }
        }
        
        // Now get photo count for each bucket and create Album objects
        buckets.forEach { (bucketId, bucketInfo) ->
            val (bucketName, coverPhotoId) = bucketInfo
            
            // Count photos in this bucket
            val countProjection = arrayOf(MediaStore.Images.Media._ID)
            val countSelection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
            val countSelectionArgs = arrayOf(bucketId)
            
            val count = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                countProjection,
                countSelection,
                countSelectionArgs,
                null
            )?.use { cursor ->
                cursor.count
            } ?: 0
            
            // Create cover URI
            val coverUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                coverPhotoId.toString()
            )
            
            albums.add(
                Album(
                    id = bucketId,
                    name = bucketName,
                    photoCount = count,
                    coverUri = coverUri
                )
            )
        }
        
        albums
    }
    
    /**
     * Get photos from a specific album
     */
    suspend fun getPhotosByAlbum(albumId: String): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(albumId)
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val dateTaken = Date(cursor.getLong(dateTakenColumn))
                val dateModified = Date(cursor.getLong(dateModifiedColumn) * 1000) // Convert seconds to milliseconds
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                photos.add(
                    Photo(
                        id = id,
                        uri = uri,
                        displayName = displayName,
                        mimeType = mimeType,
                        dateTaken = dateTaken,
                        dateModified = dateModified,
                        width = width,
                        height = height,
                        size = size,
                        bucketId = bucketId,
                        bucketName = bucketName
                    )
                )
            }
        }
        
        photos
    }
}
