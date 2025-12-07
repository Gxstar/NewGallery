package com.example.newgallery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Custom pill-shaped bottom navigation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp) // 减小垂直内边距
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 4.dp, // 减小阴影高度
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp), // 减小内部边距
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationItems.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (item.route) {
                                    Routes.PHOTOS -> Icons.Default.PhotoLibrary
                                    Routes.ALBUMS -> Icons.Default.CollectionsBookmark
                                    else -> Icons.Default.PhotoLibrary
                                },
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp) // 调整图标大小
                            )
                        },
                        // 移除标签文字，只保留图标，减小导航栏高度
                        label = null,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
