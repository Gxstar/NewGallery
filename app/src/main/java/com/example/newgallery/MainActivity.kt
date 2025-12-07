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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.newgallery.ui.navigation.AppNavigation
import com.example.newgallery.ui.navigation.BottomNavigationBar
import com.example.newgallery.ui.theme.NewGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
    }
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