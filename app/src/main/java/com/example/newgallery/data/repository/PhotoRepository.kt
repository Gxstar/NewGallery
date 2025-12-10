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
    
    // 简化缓存机制
    private var photosCache: List<Photo>? = null
    
    /**
     * Get all photos from device, sorted by date taken (newest first)
     */
    suspend fun getAllPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        // 使用简单缓存机制
        photosCache?.let { return@withContext it }
        
        val photos = mutableListOf<Photo>()
        
        // 获取图片
        val imageProjection = arrayOf(
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
        
        val imageSortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            imageSortOrder
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
        
        // 获取视频
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        
        val videoSortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            videoSortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            
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
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
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
        
        // 按日期排序（最新的在前）
        photos.sortByDescending { it.dateTaken }
        
        // 更新缓存
        photosCache = photos
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
     * Get photos grouped by date (year-month-day)
     */
    suspend fun getPhotosGroupedByDate(): Map<String, List<Photo>> {
        val photos = getAllPhotos()
        return photos.groupBy { photo ->
            val calendar = java.util.Calendar.getInstance()
            calendar.time = photo.dateTaken
            String.format("%04d-%02d-%02d", 
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }.toSortedMap(compareByDescending { it })
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        photosCache = null
    }
    
    /**
     * 从缓存中移除特定照片
     */
    fun removePhotoFromCache(photoId: Long) {
        photosCache?.let { cache ->
            val updatedCache = cache.filter { it.id != photoId }
            photosCache = if (updatedCache.isEmpty()) null else updatedCache
        }
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
