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
 * ViewModel for managing photos screen state
 */
class PhotosViewModel(private val photoRepository: PhotoRepository) : ViewModel() {
    
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // State for photos grouped by year
    private val _photosByYear = MutableStateFlow<Map<Int, List<Photo>>>(emptyMap())
    val photosByYear: StateFlow<Map<Int, List<Photo>>> = _photosByYear.asStateFlow()
    
    // State for error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load all photos grouped by year
     */
    fun loadPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val photos = photoRepository.getPhotosGroupedByYear()
                _photosByYear.value = photos
            } catch (e: Exception) {
                _error.value = "Failed to load photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh photos data
     */
    fun refreshPhotos() {
        loadPhotos()
    }
}
