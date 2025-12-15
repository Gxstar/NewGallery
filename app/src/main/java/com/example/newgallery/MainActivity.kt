package com.example.newgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.newgallery.ui.navigation.AppNavigation
import com.example.newgallery.ui.theme.NewGalleryTheme
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NewGalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPhotoViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    AppNavigation(
        navController = navController,
        sharedPhotoViewModel = sharedPhotoViewModel
    )
}