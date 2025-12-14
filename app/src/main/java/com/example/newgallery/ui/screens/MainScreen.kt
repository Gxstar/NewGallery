package com.example.newgallery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.newgallery.ui.navigation.AppNavigation
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.newgallery.ui.viewmodel.ViewModelFactory

/**
 * 主界面 - 包含底部导航栏，在照片和相册页面间切换
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPhotoViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "照片") },
                    label = { Text("照片") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate("photos") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "相册") },
                    label = { Text("相册") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("albums") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppNavigation(
                navController = navController,
                sharedPhotoViewModel = sharedPhotoViewModel
            )
        }
    }
}