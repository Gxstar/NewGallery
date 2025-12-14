package com.example.newgallery.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.util.Log
import com.example.newgallery.ui.viewmodel.*

/**
 * 相册详情页面 - 使用统一的照片列表组件
 * 显示特定相册内的照片
 */
@Composable
fun AlbumDetailScreen(
    albumId: String,
    albumName: String,
    navController: NavController,
    sharedPhotoViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val albumDetailViewModel: AlbumDetailViewModel = viewModel(factory = ViewModelFactory(context, albumId))
    
    // 获取相册照片
    val photos by albumDetailViewModel.photos.collectAsState()
    val isLoading by albumDetailViewModel.isLoading.collectAsState()
    val error by albumDetailViewModel.error.collectAsState()
    
    // 初始化日志
    LaunchedEffect(Unit) {
        Log.d("AlbumDetailScreen", "初始化AlbumDetailScreen: albumId=$albumId, albumName=$albumName")
    }
    
    // 当日志显示照片数据变化
    LaunchedEffect(photos) {
        Log.d("AlbumDetailScreen", "照片列表状态更新: size=${photos.size}, isLoading=$isLoading, error=$error")
        if (photos.isNotEmpty()) {
            Log.d("AlbumDetailScreen", "照片列表已加载完成，可以点击查看")
        }
    }
    
    val handlePhotoClick: (com.example.newgallery.data.model.Photo, Int) -> Unit = { photo, index ->
        Log.d("AlbumDetailScreen", "点击照片: ${photo.id}，索引: $index，照片列表大小: ${photos.size}")
        
        if (photos.isNotEmpty()) {
            Log.d("AlbumDetailScreen", "照片列表不为空，可以设置")
            Log.d("AlbumDetailScreen", "设置SharedPhotoViewModel前的照片列表: ${photos.map { it.id }}")
            sharedPhotoViewModel.setCurrentPhotos(photos, index)
            Log.d("AlbumDetailScreen", "导航到photo_detail: photoId=${photo.id}, index=$index, fromAlbum=true")
            navController.currentBackStackEntry?.savedStateHandle?.set("photo_id", photo.id)
            navController.currentBackStackEntry?.savedStateHandle?.set("initial_index", index)
            navController.currentBackStackEntry?.savedStateHandle?.set("from_album", true)
            navController.navigate("photo_detail")
        } else {
            Log.e("AlbumDetailScreen", "照片列表为空，无法设置")
        }
    }
    
    // 使用统一的照片列表组件
    PhotoListScreen(
        title = albumName,
        photos = photos,
        isLoading = isLoading,
        error = error,
        showDateHeaders = false, // 相册详情不显示日期分组
        baseColumnCount = 4,
        onPhotoClick = handlePhotoClick,
        onRetry = { albumDetailViewModel.loadPhotos() },
        onBackClick = { navController.navigateUp() },
        showActions = true,
        enableZoom = true
    )
}