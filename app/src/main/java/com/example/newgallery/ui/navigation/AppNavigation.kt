package com.example.newgallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.newgallery.data.model.Album
import com.example.newgallery.ui.screens.AlbumDetailScreen
import com.example.newgallery.ui.screens.AlbumsScreen
import com.example.newgallery.ui.screens.PhotoDetailScreen
import com.example.newgallery.ui.screens.PhotosScreen
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel

/**
 * 导航管理 - 统一管理应用内页面导航
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    sharedPhotoViewModel: SharedPhotoViewModel
) {
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
                }
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
                }
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
                    navController = navController
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
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}