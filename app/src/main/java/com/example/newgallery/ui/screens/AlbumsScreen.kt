package com.example.newgallery.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (album: com.example.newgallery.data.model.Album) -> Unit
) {
    // Permission handling
    val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )
    
    val context = LocalContext.current
    val viewModel: AlbumsViewModel = viewModel(factory = ViewModelFactory(context))
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    val isLoading by viewModel.isLoading.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Request permission when screen is composed
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }
    
    // Load albums when permission is granted
    LaunchedEffect(permissionState.status) {
        if (permissionState.status == PermissionStatus.Granted) {
            viewModel.loadAlbums()
            sharedViewModel.loadAllPhotos()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (permissionState.status) {
            PermissionStatus.Granted -> {
                // Show albums when permission is granted
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
                                    // Navigate to album photos
                                    onAlbumClick(album)
                                }
                            )
                        }
                    }
                }
            }
            is PermissionStatus.Denied -> {
                // Show permission denied UI
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
                    if (permissionState.status.shouldShowRationale) {
                        Text(
                            text = "请授予权限以查看您的相册",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            permissionState.launchPermissionRequest()
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
