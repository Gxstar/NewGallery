package com.example.newgallery.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newgallery.data.model.Photo
import com.example.newgallery.data.repository.PhotoRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for managing search screen state
 */
class SearchViewModel(private val photoRepository: PhotoRepository) : ViewModel() {
    
    // State for search query
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // State for search results
    private val _searchResults = MutableStateFlow<List<Photo>>(emptyList())
    val searchResults: StateFlow<List<Photo>> = _searchResults.asStateFlow()
    
    // State for all photos (for filtering)
    private val _allPhotos = MutableStateFlow<List<Photo>>(emptyList())
    
    // State for error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Load all photos initially for searching
        loadAllPhotos()
        
        // Set up search debounce
        setupSearchDebounce()
    }
    
    /**
     * Load all photos for searching
     */
    private fun loadAllPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val photos = photoRepository.getAllPhotos()
                _allPhotos.value = photos
            } catch (e: Exception) {
                _error.value = "Failed to load photos for search: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Set up search debounce to avoid too many searches
     */
    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        _query
            .debounce(300) // 300ms debounce
            .onEach { searchQuery ->
                if (searchQuery.isNotEmpty()) {
                    performSearch(searchQuery)
                } else {
                    _searchResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Update search query
     */
    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
    
    /**
     * Perform search with given query
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            val results = _allPhotos.value.filter { photo ->
                // Search by display name, bucket name, or other relevant fields
                photo.displayName.contains(query, ignoreCase = true) ||
                photo.bucketName.contains(query, ignoreCase = true)
            }
            _searchResults.value = results
        }
    }
    
    /**
     * Refresh search results
     */
    fun refreshSearch() {
        loadAllPhotos()
    }
}
