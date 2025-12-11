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
import kotlinx.coroutines.delay
import com.example.newgallery.utils.SettingsManager
import android.content.SharedPreferences

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("InlinedApi")
@Composable
fun PhotosScreen(
onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
// 网格缩放状态 - 使用remember优化性能
var gridScale by remember { mutableFloatStateOf(1f) }
val baseColumnCount = 4
val minColumnCount = 2
val maxColumnCount = 8
val currentColumnCount = remember(gridScale) {
    (baseColumnCount * gridScale).toInt().coerceIn(minColumnCount, maxColumnCount)
}
    
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
    
    val context = LocalContext.current
    
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

    
    // 菜单和对话框状态
    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
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
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "更多选项"
                                        )
                                    }
                                    
                                    // 下拉菜单
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("设置") },
                                            onClick = {
                                                showMenu = false
                                                showSettingsDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            // 缩放手势处理 - 使用独立的手势区域，避免影响滑动性能
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
                            )
                            // 照片网格（按日期分组）
                            PhotosGridByDate(
                                photosByDate = photosByDate,
                                allPhotos = allPhotos,
                                currentColumnCount = currentColumnCount,
                                lazyGridState = lazyGridState,
                                onPhotoClick = { photo, index ->
                                    // Save scroll position before navigating
                                    scrollStateViewModel.saveScrollState(lazyGridState)
                                    
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
                        permissionLauncher.launch(permissions)
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "授予权限")
                }
            }
        }
        
        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

/**
 * Settings Dialog - 设置对话框
 */
@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var coordinateConversionEnabled by remember { 
        mutableStateOf(SettingsManager.isCoordinateConversionEnabled())
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("设置")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 坐标转换设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "GPS坐标转换",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "在中国大陆地区自动转换GPS坐标",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = coordinateConversionEnabled,
                        onCheckedChange = { enabled ->
                            coordinateConversionEnabled = enabled
                            SettingsManager.setCoordinateConversionEnabled(enabled)
                        }
                    )
                }
                
                Divider()
                
                // 说明文字
                Text(
                    text = "在中国大陆地区，GPS坐标需要转换为火星坐标系(GCJ-02)才能在地图应用中正确显示位置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}



@Composable
private fun PhotosGridByDate(
    photosByDate: Map<String, List<com.example.newgallery.data.model.Photo>>,
    allPhotos: List<com.example.newgallery.data.model.Photo>,
    currentColumnCount: Int,
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onPhotoClick: (photo: com.example.newgallery.data.model.Photo, index: Int) -> Unit
) {
    // 预计算所有照片的索引映射，避免在onClick中重复计算
    val photoIndexMap = remember(allPhotos) {
        allPhotos.mapIndexed { index, photo -> photo.id to index }.toMap()
    }
    
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
            item(
                key = "header_$dateKey",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                DateHeader(dateKey = dateKey)
            }
            
            // 照片项目 - 使用key优化重组性能
            items(
                items = photos,
                key = { it.id }
            ) { photo ->
                PhotoItem(
                    photo = photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    onClick = {
                        // 使用预计算的索引映射，避免线性搜索
                        val index = photoIndexMap[photo.id] ?: 0
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
    
    // 格式化为中文显示：YYYY年MM月DD日，周几
    val displayText = "${year}年${month}月${day}日，${weekDay}"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = displayText,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}