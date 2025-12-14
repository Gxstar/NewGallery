package com.example.newgallery.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newgallery.data.model.Photo
import com.example.newgallery.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * 相册详情页面ViewModel - 管理特定相册的照片数据
 */
class AlbumDetailViewModel(
    private val albumId: String,
    private val photoRepository: PhotoRepository
) : ViewModel() {
    
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadPhotos()
    }
    
    fun loadPhotos() {
        android.util.Log.d("AlbumDetailViewModel", "开始加载相册照片，albumId: $albumId")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                
                val albumPhotos = photoRepository.getPhotosByAlbum(albumId)
                android.util.Log.d("AlbumDetailViewModel", "加载到 ${albumPhotos.size} 张照片")
                _photos.value = albumPhotos
                
            } catch (e: Exception) {
                _error.value = "加载照片失败: ${e.message}"
                android.util.Log.e("AlbumDetailViewModel", "加载相册照片失败: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}