package com.example.newgallery.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.newgallery.data.model.Album
import com.example.newgallery.ui.screens.AlbumDetailScreen
import com.example.newgallery.ui.screens.AlbumsScreen
import com.example.newgallery.ui.screens.PhotoDetailScreen
import com.example.newgallery.ui.screens.PhotosScreen
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel

/**
 * 导航管理 - 统一管理应用内页面导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    sharedPhotoViewModel: SharedPhotoViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "photos"
        ) {
            // 照片页面
            composable("photos") {
                PhotosScreen(
                    onPhotoClick = { photo, index ->
                        // 设置当前照片列表和索引
                        sharedPhotoViewModel.setCurrentPhotos(sharedPhotoViewModel.allPhotos.value, index)
                        navController.currentBackStackEntry?.savedStateHandle?.set("photo_id", photo.id)
                        navController.currentBackStackEntry?.savedStateHandle?.set("initial_index", index)
                        navController.currentBackStackEntry?.savedStateHandle?.set("from_album", false)
                        navController.navigate("photo_detail")
                    },
                    sharedPhotoViewModel = sharedPhotoViewModel
                )
            }
            
            // 相册页面
            composable("albums") {
                AlbumsScreen(
                    onAlbumClick = { album ->
                        // 导航到相册详情，传递相册ID而非整个对象
                        navController.currentBackStackEntry?.savedStateHandle?.set("album_id", album.id)
                        navController.currentBackStackEntry?.savedStateHandle?.set("album_name", album.name)
                        navController.navigate("album_detail")
                    },
                    sharedPhotoViewModel = sharedPhotoViewModel
                )
            }
            
            // 相册详情页面
            composable("album_detail") {
                // 从上一个返回栈中获取相册ID和名称（相册列表页面设置的参数）
                val albumId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("album_id")
                val albumName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("album_name")
                
                if (albumId != null && albumName != null) {
                    AlbumDetailScreen(
                        albumId = albumId,
                        albumName = albumName,
                        navController = navController,
                        sharedPhotoViewModel = sharedPhotoViewModel
                    )
                }
            }
            
            // 照片详情页面
            composable("photo_detail") {
                // 从返回栈中获取照片ID和索引
                val photoId = navController.previousBackStackEntry?.savedStateHandle?.get<Long>("photo_id") ?: -1L
                val initialIndex = navController.previousBackStackEntry?.savedStateHandle?.get<Int>("initial_index") ?: 0
                val fromAlbum = navController.previousBackStackEntry?.savedStateHandle?.get<Boolean>("from_album") ?: false
                
                android.util.Log.d("AppNavigation", "导航到photo_detail: photoId=$photoId, initialIndex=$initialIndex, fromAlbum=$fromAlbum")
                
                PhotoDetailScreen(
                    photoId = photoId,
                    initialIndex = initialIndex,
                    fromAlbum = fromAlbum,
                    onBackClick = { navController.navigateUp() },
                    sharedViewModel = sharedPhotoViewModel
                )
            }
        }
        
        // 底部导航栏
        if (currentRoute in listOf("photos", "albums")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 自定义悬浮胶囊状导航栏
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 60.dp, vertical = 20.dp) // 距离底部和两侧的距离
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp), // 胶囊形状
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 照片导航项
                Box(
                    modifier = Modifier
                        .clickable {
                            navController.navigate("photos") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "照片",
                            tint = if (currentDestination?.hierarchy?.any { it.route == "photos" } == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "照片",
                            fontSize = 12.sp,
                            color = if (currentDestination?.hierarchy?.any { it.route == "photos" } == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                // 相册导航项
                Box(
                    modifier = Modifier
                        .clickable {
                            navController.navigate("albums") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = "相册",
                            tint = if (currentDestination?.hierarchy?.any { it.route == "albums" } == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "相册",
                            fontSize = 12.sp,
                            color = if (currentDestination?.hierarchy?.any { it.route == "albums" } == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}