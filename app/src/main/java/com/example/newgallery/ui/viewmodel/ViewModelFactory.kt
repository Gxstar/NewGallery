package com.example.newgallery.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newgallery.NewGalleryApplication
import com.example.newgallery.data.repository.PhotoRepository

/**
 * Factory for creating ViewModels
 */
class ViewModelFactory(
    private val context: Context,
    private val albumId: String = ""
) : ViewModelProvider.Factory {
    
    private val photoRepository: PhotoRepository by lazy {
        (context.applicationContext as NewGalleryApplication).photoRepository
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(PhotosViewModel::class.java) -> {
                return PhotosViewModel(photoRepository) as T
            }
            modelClass.isAssignableFrom(AlbumsViewModel::class.java) -> {
                return AlbumsViewModel(photoRepository) as T
            }
            modelClass.isAssignableFrom(SharedPhotoViewModel::class.java) -> {
                return SharedPhotoViewModel(photoRepository) as T
            }
            modelClass.isAssignableFrom(ScrollStateViewModel::class.java) -> {
                return ScrollStateViewModel() as T
            }
            modelClass.isAssignableFrom(AlbumDetailViewModel::class.java) -> {
                // AlbumDetailViewModel 需要额外的 albumId 参数
                return AlbumDetailViewModel(albumId, photoRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
