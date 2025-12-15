package com.example.newgallery.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newgallery.data.model.Album
import com.example.newgallery.data.model.FavoriteEntity
import com.example.newgallery.data.model.Photo
import com.example.newgallery.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for managing photo data across all screens
 * This combines functionality from PhotosViewModel, AlbumsViewModel, and ScrollStateViewModel
 */
class SharedPhotoViewModel(
    private val photoRepository: PhotoRepository,
    private val favoriteDao: com.example.newgallery.data.dao.FavoriteDao
) : ViewModel() {
    
    // State for all photos (used for detail view navigation)
    private val _allPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val allPhotos: StateFlow<List<Photo>> = _allPhotos.asStateFlow()
    
    // State for photos grouped by date (used for PhotosScreen)
    private val _photosByDate = MutableStateFlow<Map<String, List<Photo>>>(emptyMap())
    val photosByDate: StateFlow<Map<String, List<Photo>>> = _photosByDate.asStateFlow()
    
    // State for current photo (used for detail view)
    private val _currentPhoto = MutableStateFlow<Photo?>(null)
    val currentPhoto: StateFlow<Photo?> = _currentPhoto.asStateFlow()
    
    // State for albums list (used for AlbumsScreen)
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    
    // State for album photos (used for AlbumDetailScreen)
    private val _albumPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val albumPhotos: StateFlow<List<Photo>> = _albumPhotos.asStateFlow()
    
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // State for error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Flow for all favorite photo IDs
    private val _favoritePhotoIds = favoriteDao.getAllFavoritePhotoIds()
    val favoritePhotoIds: Flow<List<Long>> = _favoritePhotoIds
    
    // Scroll state management
    private var firstVisibleItemIndex: Int = 0
    private var firstVisibleItemScrollOffset: Int = 0
    
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
     * Load all photos grouped by date
     */
    fun loadPhotosByDate() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val photosByDate = photoRepository.getPhotosGroupedByDate()
                _photosByDate.value = photosByDate
            } catch (e: Exception) {
                _error.value = "Failed to load photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
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
     * Load photos for a specific album
     */
    fun loadAlbumPhotos(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                
                val albumPhotos = photoRepository.getPhotosByAlbum(albumId)
                _albumPhotos.value = albumPhotos
                
            } catch (e: Exception) {
                _error.value = "Failed to load album photos: ${e.message}"
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
     * Set current photos list and optionally set a specific photo as current
     */
    fun setCurrentPhotos(photos: List<Photo>, currentIndex: Int = 0) {
        android.util.Log.d("SharedPhotoViewModel", "设置照片列表: ${photos.size}张照片，当前索引: $currentIndex")
        if (currentIndex >= 0 && currentIndex < photos.size) {
            android.util.Log.d("SharedPhotoViewModel", "当前照片: ${photos[currentIndex].id} - ${photos[currentIndex].displayName}")
        }
        _allPhotos.value = photos.toList() // 创建新列表确保触发状态更新
        if (currentIndex >= 0 && currentIndex < photos.size) {
            _currentPhoto.value = photos[currentIndex]
        }
        android.util.Log.d("SharedPhotoViewModel", "allPhotos已更新，新大小: ${_allPhotos.value.size}")
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
    
    /**
     * Save the current scroll state
     */
    fun saveScrollState(index: Int, offset: Int) {
        firstVisibleItemIndex = index
        firstVisibleItemScrollOffset = offset
    }
    
    /**
     * Get saved scroll state
     */
    fun getSavedScrollState(): Pair<Int, Int> {
        return Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    }
    
    /**
     * Clear the saved scroll state
     */
    fun clearScrollState() {
        firstVisibleItemIndex = 0
        firstVisibleItemScrollOffset = 0
    }
    
    /**
     * 切换照片的收藏状态
     */
    suspend fun togglePhotoFavorite(photoId: Long) {
        // 检查照片是否已经被收藏
        val isFavorite = favoriteDao.isPhotoFavorite(photoId)
        
        if (isFavorite) {
            // 已收藏，取消收藏
            favoriteDao.delete(FavoriteEntity(photoId))
        } else {
            // 未收藏，添加收藏
            favoriteDao.insert(FavoriteEntity(photoId))
        }
    }
    
    /**
     * 检查照片是否被收藏
     */
    suspend fun isPhotoFavorite(photoId: Long): Boolean {
        return favoriteDao.isPhotoFavorite(photoId)
    }
    
    /**
     * 获取带收藏状态的照片列表
     * 用于将MediaStore的照片列表与Room的收藏ID列表合并
     */
    fun getPhotosWithFavoriteStatus(photos: List<Photo>): Flow<List<Pair<Photo, Boolean>>> {
        return favoritePhotoIds.map { favoriteIds ->
            photos.map { photo ->
                Pair(photo, favoriteIds.contains(photo.id))
            }
        }
    }
    
    /**
     * 获取带收藏状态的相册照片列表
     */
    fun getAlbumPhotosWithFavoriteStatus(): Flow<List<Pair<Photo, Boolean>>> {
        return albumPhotos.combine(favoritePhotoIds) { photos, favoriteIds ->
            photos.map { photo ->
                Pair(photo, favoriteIds.contains(photo.id))
            }
        }
    }
}