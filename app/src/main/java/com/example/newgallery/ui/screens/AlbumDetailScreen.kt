package com.example.newgallery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import com.example.newgallery.ui.components.PhotoGrid

/**
 * 相册详情页面 - 显示特定相册内的照片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    albumName: String,
    navController: NavController,
    sharedPhotoViewModel: SharedPhotoViewModel
) {
    // 获取相册照片
    val photos by sharedPhotoViewModel.albumPhotos.collectAsState()
    val isLoading by sharedPhotoViewModel.isLoading.collectAsState()
    val error by sharedPhotoViewModel.error.collectAsState()
    
    // 加载相册照片
    LaunchedEffect(albumId) {
        sharedPhotoViewModel.loadAlbumPhotos(albumId)
    }
    
    val handlePhotoClick: (com.example.newgallery.data.model.Photo, Int) -> Unit = { photo, index ->
        sharedPhotoViewModel.setCurrentPhotos(photos, index)
        navController.currentBackStackEntry?.savedStateHandle?.set("photo_id", photo.id)
        navController.currentBackStackEntry?.savedStateHandle?.set("initial_index", index)
        navController.currentBackStackEntry?.savedStateHandle?.set("from_album", true)
        navController.navigate("photo_detail")
    }
    
    // 直接使用PhotoGrid组件，不再依赖PhotoListScreen
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = albumName) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        text = "${photos.size} 张照片",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    ) {
        // 使用Flow监听收藏状态
        val favoritePhotoIds by sharedPhotoViewModel.favoritePhotoIds.collectAsState(initial = emptyList())
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { sharedPhotoViewModel.loadAlbumPhotos(albumId) }) {
                            Text("重试")
                        }
                    }
                }
                photos.isEmpty() -> {
                    Text(
                        text = "此相册中没有照片",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // 照片网格
                    PhotoGrid(
                        photos = photos,
                        columnCount = 4,
                        showDateHeaders = false,
                        onPhotoClick = handlePhotoClick,
                        modifier = Modifier.fillMaxSize(),
                        enableZoom = true,
                        favoritePhotoIds = favoritePhotoIds
                    )
                }
            }
        }
    }
}