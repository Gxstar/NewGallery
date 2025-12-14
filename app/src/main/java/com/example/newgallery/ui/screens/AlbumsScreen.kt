package com.example.newgallery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newgallery.ui.components.*
import com.example.newgallery.ui.theme.TextSecondary
import com.example.newgallery.ui.viewmodel.AlbumsViewModel
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (album: com.example.newgallery.data.model.Album) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AlbumsViewModel = viewModel(factory = ViewModelFactory(context))
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    val isLoading by viewModel.isLoading.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 使用通用权限处理
    val permissionState = rememberPermissionState { isGranted ->
        if (isGranted) {
            viewModel.loadAlbums()
            sharedViewModel.loadAllPhotos()
        }
    }
    
    // 请求权限当屏幕首次加载时
    LaunchedEffect(Unit) {
        permissionState.requestPermission()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏
        TopAppBar(
            title = { Text(text = "相册") },
            actions = {
                IconButton(onClick = { /* TODO: 添加设置菜单 */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项"
                    )
                }
            }
        )
        
        // 根据权限和状态显示不同内容
        when {
            !permissionState.isGranted -> {
                PermissionDeniedState(
                    showRationale = permissionState.showRationale,
                    onRequestPermission = { permissionState.requestPermission() }
                )
            }
            isLoading -> {
                LoadingState()
            }
            error != null -> {
                ErrorState(
                    message = error ?: "未知错误",
                    onRetry = { viewModel.loadAlbums() }
                )
            }
            albums.isEmpty() -> {
                EmptyState(message = "未找到相册")
            }
            else -> {
                AlbumsGrid(
                    albums = albums,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}

@Composable
private fun AlbumsGrid(
    albums: List<com.example.newgallery.data.model.Album>,
    onAlbumClick: (com.example.newgallery.data.model.Album) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(albums) { album ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumItem(
    album: com.example.newgallery.data.model.Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = album.name.firstOrNull()?.toString() ?: "",
                        fontSize = 48.sp
                    )
                }
            }
        }
        
        // Album name
        Text(
            text = album.name,
            modifier = Modifier.padding(8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        // Photo count
        Text(
            text = "${album.photoCount} 张照片",
            modifier = Modifier.padding(bottom = 8.dp),
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}
