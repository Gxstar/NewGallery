package com.example.newgallery.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newgallery.data.repository.PhotoRepository

/**
 * Factory for creating ViewModels
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(PhotosViewModel::class.java) -> {
                return PhotosViewModel(PhotoRepository(context)) as T
            }
            modelClass.isAssignableFrom(AlbumsViewModel::class.java) -> {
                return AlbumsViewModel(PhotoRepository(context)) as T
            }
            modelClass.isAssignableFrom(SharedPhotoViewModel::class.java) -> {
                return SharedPhotoViewModel(PhotoRepository(context)) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                return SearchViewModel(PhotoRepository(context)) as T
            }
            modelClass.isAssignableFrom(ScrollStateViewModel::class.java) -> {
                return ScrollStateViewModel() as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
