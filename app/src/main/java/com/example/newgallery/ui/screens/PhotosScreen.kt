package com.example.newgallery.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent as AndroidIntent
import android.content.IntentFilter
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newgallery.ui.components.PhotoItem
import com.example.newgallery.ui.viewmodel.PhotosViewModel
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ScrollStateViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
// 网格缩放状态
var gridScale by remember { mutableFloatStateOf(1f) }
val baseColumnCount = 4
val minColumnCount = 2
val maxColumnCount = 8
val currentColumnCount = remember(gridScale) {
(baseColumnCount * gridScale).toInt().coerceIn(minColumnCount, maxColumnCount)
}
    
    // 标准Android权限处理 - API 30+ 需要 READ_MEDIA_IMAGES 和 READ_MEDIA_VIDEO
    val permissionGranted = remember { mutableStateOf(false) }
    val showPermissionRationale = remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val videoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
        val allGranted = imagesGranted && videoGranted
        
        permissionGranted.value = allGranted
        if (!allGranted) {
            // 检查是否需要显示权限说明
            showPermissionRationale.value = true
        } else {
            showPermissionRationale.value = false
        }
    }
    
    val context = LocalContext.current
    val viewModel: PhotosViewModel = viewModel(factory = ViewModelFactory(context))
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    val scrollStateViewModel: ScrollStateViewModel = viewModel(factory = ViewModelFactory(context))
    
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
    
    // 滚动位置状态
    val lazyGridState = rememberLazyGridState()
    
    // 尝试恢复滚动位置
    LaunchedEffect(Unit) {
        lazyGridState.scrollToItem(
            scrollStateViewModel.getSavedFirstVisibleItemIndex(),
            scrollStateViewModel.getSavedFirstVisibleItemScrollOffset()
        )
    }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val photosByDate by viewModel.photosByDate.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 请求权限
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        )
    }
    
    // 加载照片
    LaunchedEffect(permissionGranted.value) {
        if (permissionGranted.value) {
            viewModel.loadPhotos()
            sharedViewModel.loadAllPhotos()
        }
    }
    
    val allPhotos = photosByDate.values.flatten()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (permissionGranted.value) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Text(
                        text = error ?: "未知错误",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                photosByDate.isEmpty() -> {
                    Text(
                            text = "未找到照片或视频",
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                }

                else -> {
                    // 主内容
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 顶栏
                        TopAppBar(
                            title = { Text(text = "照片和视频") },
                            actions = {
                                IconButton(onClick = { /* TODO: 添加设置菜单 */ }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "更多选项"
                                    )
                                }
                            }
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    // 双指缩放手势处理
                                    detectTransformGestures(
                                        onGesture = { _, _, zoom, _ ->
                                            if (zoom != 1f) {
                                                // 计算缩放变化量：
                                                // iOS相册逻辑：根据zoom变化量调整列数
                                                // zoom > 1.0 时减少列数（放大）
                                                // zoom < 1.0 时增加列数（缩小）
                                                val scaleChange = if (zoom > 1f) {
                                                    // 放大：减少列数，gridScale应该减小
                                                    -(zoom - 1f) * 0.5f
                                                } else {
                                                    // 缩小：增加列数，gridScale应该增大
                                                    (1f - zoom) * 0.5f
                                                }
                                                
                                                // 更新缩放状态
                                                val newScale = (gridScale + scaleChange).coerceIn(0.5f, 2f)
                                                if (newScale != gridScale) {
                                                    gridScale = newScale
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            // 照片网格（按日期分组）
                            PhotosGridByDate(
                                photosByDate = photosByDate,
                                allPhotos = photosByDate.values.flatten(),
                                currentColumnCount = currentColumnCount,
                                lazyGridState = lazyGridState,
                                onPhotoClick = { photo, index ->
                                    // Save scroll position before navigating
                                    scrollStateViewModel.saveScrollState(lazyGridState)
                                    
                                    // 如果是视频，使用系统播放器播放
                                    if (photo.mimeType?.startsWith("video/") == true) {
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
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "需要权限来访问照片和视频",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
                if (showPermissionRationale.value) {
                    Text(
                        text = "请授予权限以查看您的照片和视频",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                            )
                        )
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "授予权限")
                }
            }
        }
    }
}



@Composable
private fun PhotosGridByDate(
    photosByDate: Map<String, List<com.example.newgallery.data.model.Photo>>,
    allPhotos: List<com.example.newgallery.data.model.Photo>,
    currentColumnCount: Int,
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(currentColumnCount),
        state = lazyGridState,
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        photosByDate.keys.forEach { dateKey ->
            val photos = photosByDate[dateKey] ?: emptyList()
            
            // 日期标题
            item(span = { GridItemSpan(maxLineSpan) }) {
                DateHeader(dateKey = dateKey)
            }
            
            // 照片项目
            items(photos) { photo ->
                PhotoItem(
                    photo = photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    onClick = {
                        val index = allPhotos.indexOf(photo)
                        onPhotoClick(photo, index)
                    }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(dateKey: String) {
    // 解析日期字符串格式：YYYY-MM-DD
    val parts = dateKey.split("-")
    val year = parts[0]
    val month = parts[1]
    val day = parts[2]
    
    // 获取周几
    val calendar = java.util.Calendar.getInstance()
    calendar.set(year.toInt(), month.toInt() - 1, day.toInt())
    val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val weekDay = weekDays[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    
    // 格式化为中文显示：MM月dd日，周几
    val displayText = "${month}月${day}日，${weekDay}"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = displayText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = year,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}