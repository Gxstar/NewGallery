package com.example.newgallery.data.model

import android.net.Uri

/**
 * Data class representing an album in the gallery
 */
data class Album(
    val id: String,
    val name: String,
    val photoCount: Int,
    val coverUri: Uri?,
    val coverPhoto: Photo? = null
)
