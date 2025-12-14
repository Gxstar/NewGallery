package com.example.newgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
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
import com.example.newgallery.utils.SettingsManager

class MainActivity : ComponentActivity() {
    
    // Store the photo ID being deleted for callback
    private var pendingDeletePhotoId: Long = -1
    
    // Activity Result Launcher for delete requests
    private val deleteResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "Received delete request result, resultCode: ${result.resultCode}")
        
        if (result.resultCode == android.app.Activity.RESULT_OK) {
                android.util.Log.d("MainActivity", "Delete successful for photo ID: $pendingDeletePhotoId")
                android.widget.Toast.makeText(this, "照片已删除", android.widget.Toast.LENGTH_SHORT).show()
                
                // 清除缓存以确保数据刷新
                val photoRepository = (application as? com.example.newgallery.NewGalleryApplication)?.photoRepository
                photoRepository?.clearCache()
                android.util.Log.d("MainActivity", "Photo repository cache cleared")
                
                // 发送多个广播确保所有组件都能收到更新通知
                val deleteIntent = android.content.Intent("com.example.newgallery.PHOTO_DELETED").apply {
                    putExtra("deleted_photo_id", pendingDeletePhotoId)
                }
                android.util.Log.d("MainActivity", "Sending delete broadcast: ${deleteIntent.action} with photo ID: $pendingDeletePhotoId")
                sendBroadcast(deleteIntent)
                
                // 发送通用媒体变化广播
                val mediaChangeIntent = android.content.Intent("com.example.newgallery.MEDIA_CHANGED")
                android.util.Log.d("MainActivity", "Sending media change broadcast: ${mediaChangeIntent.action}")
                sendBroadcast(mediaChangeIntent)
                
                android.util.Log.d("MainActivity", "All broadcasts sent successfully")
                
                // Reset pending photo ID
                pendingDeletePhotoId = -1
            } else {
                android.util.Log.d("MainActivity", "Delete cancelled or failed")
                android.widget.Toast.makeText(this, "删除已取消", android.widget.Toast.LENGTH_SHORT).show()
                
                // Reset pending photo ID
                pendingDeletePhotoId = -1
            }
    }
    
    // Method to launch delete request
    fun launchDeleteRequest(intentSender: android.content.IntentSender, photoId: Long) {
        // Store the photo ID for callback
        pendingDeletePhotoId = photoId
        
        val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
        deleteResultLauncher.launch(intentSenderRequest)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化设置管理器
        SettingsManager.init(this)
        
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
                        sharedPhotoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.example.newgallery.ui.viewmodel.ViewModelFactory(this@MainActivity))
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
        
        // 监听应用前后台切换，在恢复前台时刷新数据
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 应用回到前台时发送刷新广播
                val refreshIntent = android.content.Intent("com.example.newgallery.MEDIA_CHANGED")
                sendBroadcast(refreshIntent)
                android.util.Log.d("MainActivity", "App resumed, sent refresh broadcast")
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume called")
        // 发送媒体变化广播，确保数据刷新
        val refreshIntent = android.content.Intent("com.example.newgallery.MEDIA_CHANGED")
        sendBroadcast(refreshIntent)
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