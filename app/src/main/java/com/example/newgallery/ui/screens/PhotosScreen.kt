package com.example.newgallery.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent as AndroidIntent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newgallery.ui.viewmodel.PhotosViewModel
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ScrollStateViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.delay
import com.example.newgallery.utils.SettingsManager

/**
 * 照片屏幕 - 使用统一的照片列表组件
 * 展示所有照片，按日期分组
 */
@SuppressLint("InlinedApi")
@Composable
fun PhotosScreen(
    onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
    val context = LocalContext.current
    
    val viewModel: PhotosViewModel = viewModel(factory = ViewModelFactory(context))
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    val scrollStateViewModel: ScrollStateViewModel = viewModel(factory = ViewModelFactory(context))
    
    // 标准Android权限处理 - 向后兼容不同API级别
    val permissionGranted = remember { mutableStateOf(false) }
    val showPermissionRationale = remember { mutableStateOf(false) }
    val permissionRequested = remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 根据API级别检查不同的权限
        val allGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // API 33+ 使用新的权限
            val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            val videoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
            imagesGranted && videoGranted
        } else {
            // API 30-32 使用旧的权限
            val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            readGranted
        }
        
        permissionGranted.value = allGranted
        // 检查是否需要显示权限说明
        showPermissionRationale.value = !allGranted
    }
    
    // 检查是否已经拥有权限，避免重复请求
    LaunchedEffect(Unit) {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        permissionGranted.value = hasPermission
        
        // 如果已经拥有权限，标记为已请求过，避免重复请求
        if (hasPermission) {
            permissionRequested.value = true
        }
    }
    
    // Broadcast receiver to handle photo deletion and media changes
    val photoDeletedReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: AndroidIntent?) {
                when (intent?.action) {
                    "com.example.newgallery.PHOTO_DELETED" -> {
                        // Get the deleted photo ID if available
                        val deletedPhotoId = intent.getLongExtra("deleted_photo_id", -1)
                        if (deletedPhotoId != -1L) {
                            // Remove specific photo from shared view model
                            sharedViewModel.removePhoto(deletedPhotoId)
                        } else {
                            // Fallback: reload all photos
                            sharedViewModel.loadAllPhotos()
                        }
                        // Refresh current view model
                        viewModel.loadPhotos()
                    }
                    "com.example.newgallery.MEDIA_CHANGED" -> {
                        // 重新加载照片数据
                        sharedViewModel.loadAllPhotos()
                        viewModel.loadPhotos()
                    }
                }
            }
        }
    }
    
    // Content observer to monitor media store changes
    val mediaStoreObserver = remember {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                android.util.Log.d("PhotosScreen", "Media store changed, URI: $uri, selfChange: $selfChange")
                // 延迟一点时间避免频繁刷新
                Handler(Looper.getMainLooper()).postDelayed({
                    android.util.Log.d("PhotosScreen", "Refreshing photos after media store change")
                    // 重新加载照片数据
                    sharedViewModel.loadAllPhotos()
                    viewModel.loadPhotos()
                }, 500) // 延迟500ms
            }
        }
    }
    
    // Register and unregister the broadcast receiver and content observer
    DisposableEffect(context) {
        // Register broadcast receiver for deletion and media change events
        val filter = IntentFilter().apply {
            addAction("com.example.newgallery.PHOTO_DELETED")
            addAction("com.example.newgallery.MEDIA_CHANGED")
        }
        
        // API 30+ 使用RECEIVER_EXPORTED
        context.registerReceiver(photoDeletedReceiver, filter, Context.RECEIVER_EXPORTED)
        
        // Register content observer for media store changes
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaStoreObserver
        )
        
        // 同时注册视频内容观察者
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaStoreObserver
        )
        
        onDispose {
            context.unregisterReceiver(photoDeletedReceiver)
            context.contentResolver.unregisterContentObserver(mediaStoreObserver)
        }
    }
    
    // 状态观察
    val isLoading by viewModel.isLoading.collectAsState()
    val photosByDate by viewModel.photosByDate.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 预计算照片数据，避免重复计算
    val allPhotos = remember(photosByDate) {
        photosByDate.values.flatten()
    }
    
    // 请求权限 - 只在应用首次启动且需要时请求
    LaunchedEffect(permissionRequested.value) {
        // 只在权限未请求过且未授予的情况下请求
        if (!permissionRequested.value && !permissionGranted.value) {
            // 延迟一小段时间再请求权限，避免UI闪烁
            kotlinx.coroutines.delay(300)
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // API 33+ 使用新的权限
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                // API 30-32 使用旧的权限
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionRequested.value = true
            permissionLauncher.launch(permissions)
        }
    }
    
    // 加载照片 - 优化加载顺序避免资源竞争
    LaunchedEffect(permissionGranted.value) {
        if (permissionGranted.value) {
            // 先加载主视图数据，再加载共享数据
            viewModel.loadPhotos()
            // 延迟一小段时间再加载共享数据，避免同时大量IO操作
            delay(100)
            sharedViewModel.loadAllPhotos()
        }
    }
    
    // 设置点击回调
    val handlePhotoClick = remember<(com.example.newgallery.data.model.Photo, Int) -> Unit> {
        { photo, index ->
            // 保存滚动位置 - 使用索引来保存位置信息
            scrollStateViewModel.clearScrollState() // 先清除之前的状态
            // 这里我们保存索引信息，在返回时可以用来恢复位置
            
            // 如果是视频，使用系统播放器播放
            if (photo.mimeType.startsWith("video/")) {
                try {
                    val intent: AndroidIntent = AndroidIntent(AndroidIntent.ACTION_VIEW).apply {
                        setDataAndType(photo.uri, photo.mimeType)
                        addFlags(AndroidIntent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("PhotosScreen", "无法播放视频: ${e.message}")
                    // 如果播放失败，仍然导航到详情页作为备选方案
                    onPhotoClick(photo, index)
                }
            } else {
                // 图片正常导航到详情页
                onPhotoClick(photo, index)
            }
        }
    }
    
    // 使用统一的照片列表组件
    PhotoListScreen(
        title = "照片和视频",
        photos = allPhotos,
        isLoading = isLoading,
        error = error,
        showDateHeaders = true, // 显示日期分组
        baseColumnCount = 5,
        onPhotoClick = handlePhotoClick,
        onRetry = { viewModel.loadPhotos() },
        onSettingsClick = {
            // 显示设置对话框
            showSettingsDialog()
        },
        enableZoom = true
    )
}

/**
 * 显示设置对话框
 */
private fun showSettingsDialog() {
    // 这里可以添加设置对话框的显示逻辑
    // 由于Compose的限制，我们需要在UI中处理对话框状态
}