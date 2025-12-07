package com.example.newgallery.data.model

import android.net.Uri
import java.util.Date

/**
 * Data class representing a photo in the gallery
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateTaken: Date,
    val dateModified: Date,
    val width: Int,
    val height: Int,
    val size: Long,
    val bucketId: String,
    val bucketName: String,
    val isFavorite: Boolean = false
) {
    /**
     * Get the year of the photo for grouping
     */
    val year: Int
        get() = dateTaken.year + 1900 // Date year is 0-based
}
