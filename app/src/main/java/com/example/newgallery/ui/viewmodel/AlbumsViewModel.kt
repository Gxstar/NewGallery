package com.example.newgallery.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newgallery.data.model.Album
import com.example.newgallery.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing albums screen state
 */
class AlbumsViewModel(private val photoRepository: PhotoRepository) : ViewModel() {
    
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // State for albums list
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    
    // State for error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load all albums
     */
    fun loadAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val albums = photoRepository.getAllAlbums()
                _albums.value = albums
            } catch (e: Exception) {
                _error.value = "Failed to load albums: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh albums data
     */
    fun refreshAlbums() {
        loadAlbums()
    }
}
