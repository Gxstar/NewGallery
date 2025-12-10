package com.example.newgallery.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newgallery.data.model.Photo
import com.example.newgallery.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for managing photo data across screens
 * This allows sharing photo data between list and detail screens
 */
class SharedPhotoViewModel(
    private val photoRepository: PhotoRepository
) : ViewModel() {
    
    // State for all photos (used for detail view navigation)
    private val _allPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val allPhotos: StateFlow<List<Photo>> = _allPhotos.asStateFlow()
    
    // State for current photo (used for detail view)
    private val _currentPhoto = MutableStateFlow<Photo?>(null)
    val currentPhoto: StateFlow<Photo?> = _currentPhoto.asStateFlow()
    
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // State for error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load all photos for navigation
     */
    fun loadAllPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Clear cache before loading to ensure fresh data
                photoRepository.clearCache()
                val photos = photoRepository.getAllPhotos()
                _allPhotos.value = photos
            } catch (e: Exception) {
                _error.value = "Failed to load photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Remove a specific photo from the current list (used after deletion)
     */
    fun removePhoto(photoId: Long) {
        val currentPhotos = _allPhotos.value.toMutableList()
        currentPhotos.removeAll { it.id == photoId }
        _allPhotos.value = currentPhotos
        
        // Also remove from repository cache
        photoRepository.removePhotoFromCache(photoId)
    }
    
    /**
     * Get photo by ID
     */
    fun getPhotoById(photoId: Long): Photo? {
        return _allPhotos.value.find { it.id == photoId }
    }
    
    /**
     * Set current photo
     */
    fun setCurrentPhoto(photo: Photo) {
        _currentPhoto.value = photo
    }
    
    /**
     * Get photo at specific index
     */
    fun getPhotoAtIndex(index: Int): Photo? {
        return if (index >= 0 && index < _allPhotos.value.size) {
            _allPhotos.value[index]
        } else {
            null
        }
    }
    
    /**
     * Get index of specific photo
     */
    fun getPhotoIndex(photo: Photo): Int {
        return _allPhotos.value.indexOf(photo)
    }
}