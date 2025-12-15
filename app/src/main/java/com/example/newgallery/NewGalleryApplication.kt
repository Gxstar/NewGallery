package com.example.newgallery

import android.app.Application
import com.example.newgallery.data.database.AppDatabase
import com.example.newgallery.data.repository.PhotoRepository

/**
 * Custom Application class for global initialization
 */
class NewGalleryApplication : Application() {
    
    // Global PhotoRepository instance
    val photoRepository: PhotoRepository by lazy {
        PhotoRepository(this)
    }
    
    // Global Database instance
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    // Global FavoriteDao instance
    val favoriteDao by lazy {
        database.favoriteDao()
    }
    
    override fun onCreate() {
        super.onCreate()
        // Global initialization if needed
    }
}