package com.example.newgallery.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newgallery.ui.theme.TextSecondary
import com.example.newgallery.ui.viewmodel.AlbumsViewModel
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (album: com.example.newgallery.data.model.Album) -> Unit
) {
    // 标准Android权限处理 - API 30+ 只需要 READ_MEDIA_IMAGES
    val permissionGranted = remember { mutableStateOf(false) }
    val showPermissionRationale = remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted.value = isGranted
        if (!isGranted) {
            // 检查是否需要显示权限说明
            showPermissionRationale.value = true
        } else {
            showPermissionRationale.value = false
        }
    }
    
    val context = LocalContext.current
    val viewModel: AlbumsViewModel = viewModel(factory = ViewModelFactory(context))
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    val isLoading by viewModel.isLoading.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 请求权限当屏幕首次加载时
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }
    
    // 当权限被授予时加载相册
    LaunchedEffect(permissionGranted.value) {
        if (permissionGranted.value) {
            viewModel.loadAlbums()
            sharedViewModel.loadAllPhotos()
        }
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
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (permissionGranted.value) {
                // 权限已授予时显示相册
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (error != null) {
                    Text(
                        text = error ?: "未知错误",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                } else if (albums.isEmpty()) {
                    Text(
                        text = "未找到相册",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                } else {
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
                                onClick = { 
                                    // 导航到相册照片
                                    onAlbumClick(album)
                                }
                            )
                        }
                    }
                }
            } else {
                // 权限未授予时显示权限请求UI
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "需要权限来访问相册",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    if (showPermissionRationale.value) {
                        Text(
                            text = "请授予权限以查看您的相册",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = "授予权限")
                    }
                }
            }
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
