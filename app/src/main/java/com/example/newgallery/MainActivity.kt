package com.example.newgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.newgallery.ui.navigation.AppNavigation
import com.example.newgallery.ui.navigation.BottomNavigationBar
import com.example.newgallery.ui.navigation.Routes
import com.example.newgallery.ui.theme.NewGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewGalleryTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                // Custom layout for floating pill navigation
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content - no bottom padding since navigation is floating
                    AppNavigation(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Floating pill navigation at the bottom - only show on grid screens
                    if (shouldShowNavigation(currentRoute)) {
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
        }
    }
}

// Helper function to determine when to show navigation
private fun shouldShowNavigation(currentRoute: String?): Boolean {
    return currentRoute == Routes.PHOTOS || currentRoute == Routes.ALBUMS
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    NewGalleryTheme {
        val navController = rememberNavController()
        
        // Custom layout for floating pill navigation
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            AppNavigation(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    // Add bottom padding to prevent content from being hidden behind the navigation
                    .padding(bottom = 100.dp)
            )
            
            // Floating pill navigation at the bottom
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