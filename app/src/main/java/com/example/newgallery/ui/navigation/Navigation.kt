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
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.newgallery.ui.screens.AlbumsScreen
import com.example.newgallery.ui.screens.PhotoDetailScreen
import com.example.newgallery.ui.screens.PhotosScreen

// Navigation routes
object Routes {
    const val PHOTOS = "photos"
    const val ALBUMS = "albums"
    const val PHOTO_DETAIL = "photo_detail"
}

// Navigation items data class
data class NavigationItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

// List of navigation items
val navigationItems = listOf(
    NavigationItem(
        route = Routes.PHOTOS,
        label = "照片",
        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "照片") }
    ),
    NavigationItem(
        route = Routes.ALBUMS,
        label = "相册",
        icon = { Icon(Icons.Default.CollectionsBookmark, contentDescription = "相册") }
    )
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.PHOTOS,
        modifier = modifier
    ) {
        composable(Routes.PHOTOS) {
            PhotosScreen(
                onPhotoClick = { photo, index ->
                    // Navigate to photo detail with the photo ID and index
                    navController.navigate("${Routes.PHOTO_DETAIL}/${photo.id}/$index")
                }
            )
        }
        composable(Routes.ALBUMS) {
            AlbumsScreen(
                onAlbumClick = { album ->
                    // TODO: Implement proper navigation to album photos
                    // For now, just show a placeholder
                    println("Album clicked: ${album.name}")
                }
            )
        }
        // Photo detail route with photo ID and index arguments
        composable(
            route = "${Routes.PHOTO_DETAIL}/{photoId}/{index}",
            arguments = listOf(
                navArgument("photoId") { type = NavType.LongType },
                navArgument("index") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            
            // For now, we'll create a simple photo detail screen
            // In a real app, you'd want to get the photo from a shared ViewModel or repository
            PhotoDetailScreen(
                photoId = photoId,
                initialIndex = index,
                onBackClick = { 
                    // Clear scroll state when going back
                    navController.popBackStack() 
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // iOS-style 精致悬浮导航栏
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 80.dp, vertical = 24.dp) // 增加水平边距，让导航栏更窄
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp), // 减少圆角，更精致
            shadowElevation = 8.dp, // 减少阴影强度
            tonalElevation = 4.dp, // 减少色调高度
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // 增加透明度
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // 进一步减少高度
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp), // 减少内边距
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationItems.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    // 导航项容器 - 图标+文字设计
                    Column(
                        modifier = Modifier
                            .clickable {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp), // 调整内边距
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 图标容器
                        Box(
                            modifier = Modifier.size(28.dp), // 减小图标容器尺寸
                            contentAlignment = Alignment.Center
                        ) {
                            // 选中状态的背景效果
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp), // 适配小尺寸
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(24.dp)
                                ) {}
                            }
                            
                            // 图标
                            Icon(
                                imageVector = when (item.route) {
                                    Routes.PHOTOS -> Icons.Default.PhotoLibrary
                                    Routes.ALBUMS -> Icons.Default.CollectionsBookmark
                                    else -> Icons.Default.PhotoLibrary
                                },
                                contentDescription = item.label,
                                modifier = Modifier.size(18.dp), // 减小图标尺寸
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                        
                        // 文字标签
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall, // 使用小字体
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.padding(top = 1.dp) // 减少图标和文字间距
                        )
                    }
                }
            }
        }
    }
}
