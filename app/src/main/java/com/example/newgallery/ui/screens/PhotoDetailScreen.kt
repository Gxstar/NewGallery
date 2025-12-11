package com.example.newgallery.ui.screens

import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newgallery.data.model.Photo
import com.example.newgallery.ui.theme.TextPrimary
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import com.example.newgallery.utils.ExifInfoUtil
import com.example.newgallery.utils.CoordinateConverter
import com.example.newgallery.utils.SettingsManager
import com.example.newgallery.MainActivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent as AndroidIntent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.widget.Toast
import android.util.Log

@Composable
fun PhotoDetailScreen(
    photoId: Long,
    initialIndex: Int = 0,
    onBackClick: () -> Unit
) {
    var shouldExit by remember { mutableStateOf(false) }
    val exitAlpha by animateFloatAsState(
        targetValue = if (shouldExit) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        finishedListener = { if (it == 0f) onBackClick() }
    )
    val context = LocalContext.current
    val sharedViewModel: SharedPhotoViewModel = viewModel(factory = ViewModelFactory(context))
    
    // API 30+ 删除媒体文件不需要特殊权限
    
    val allPhotos by sharedViewModel.allPhotos.collectAsState()
    
    // Load photos if not already loaded
    LaunchedEffect(Unit) {
        if (allPhotos.isEmpty()) {
            sharedViewModel.loadAllPhotos()
        }
    }
    
    val photos = allPhotos
    val initialPhotoIndex = remember(photos) {
        photos.indexOfFirst { it.id == photoId }.takeIf { it != -1 } ?: initialIndex
    }
    val pagerState = rememberPagerState(
        initialPage = initialPhotoIndex,
        pageCount = { photos.size }
    )
    
    // State for favorite and EXIF dialog
    var isFavorite by remember { mutableStateOf(false) }
    var showExifDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // API 30+ 不需要权限监控逻辑
    
    // State for UI visibility (top bar and bottom toolbar)
    var isUIVisible by remember { mutableStateOf(true) }
    
    // Store current photo ID for deletion callback
    var currentPhotoId by remember { mutableLongStateOf(photoId) }
    
    // Broadcast receiver to handle photo deletion events
    val photoDeletedReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: AndroidIntent?) {
                when (intent?.action) {
                    "com.example.newgallery.PHOTO_DELETED" -> {
                        val deletedPhotoId = intent.getLongExtra("deleted_photo_id", -1)
                        if (deletedPhotoId == currentPhotoId) {
                            onBackClick()
                        }
                    }
                    "com.example.newgallery.MEDIA_CHANGED" -> {
                        // 可以在这里添加额外的处理逻辑
                    }
                }
            }
        }
    }
    
    // Register and unregister the broadcast receiver
    DisposableEffect(context) {
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.newgallery.PHOTO_DELETED")
            addAction("com.example.newgallery.MEDIA_CHANGED")
        }
        
        // API 30+ 使用RECEIVER_EXPORTED
        context.registerReceiver(photoDeletedReceiver, filter, Context.RECEIVER_EXPORTED)
        
        onDispose {
            context.unregisterReceiver(photoDeletedReceiver)
        }
    }
    
    // System UI control
    LaunchedEffect(isUIVisible) {
        val window = (context as android.app.Activity).window
        val windowInsetsController = window.insetsController
        
        if (isUIVisible) {
            // Show system UI
            windowInsetsController?.show(android.view.WindowInsets.Type.systemBars())
        } else {
            // Hide system UI (immersive mode)
            windowInsetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
    
    // Load favorite state when photo changes
    LaunchedEffect(pagerState.currentPage) {
        if (photos.isNotEmpty() && pagerState.currentPage < photos.size) {
            val currentPhoto = photos[pagerState.currentPage]
            isFavorite = isPhotoFavorite(context, currentPhoto.uri)
            // Update current photo ID when page changes
            currentPhotoId = currentPhoto.id
        }
    }
    
    // Zoom state managed at the pager level
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isUIVisible) MaterialTheme.colorScheme.background else Color.Black)
            .graphicsLayer(alpha = exitAlpha)
    ) {
        if (photos.isEmpty()) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "加载中...")
            }
        } else {
            // 顶部栏区域 (固定高度)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                AnimatedVisibility(
                    visible = isUIVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(top = 24.dp) // 增加与状态栏的距离
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 返回按钮
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    tint = TextPrimary
                                )
                            }
                            
                            // 拍摄时间信息
                            if (photos.isNotEmpty() && pagerState.currentPage < photos.size) {
                                val currentPhoto = photos[pagerState.currentPage]
                                Text(
                                    text = formatPhotoDate(context, currentPhoto),
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            
            // 图片显示区域 (占据中间大部分空间)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isUIVisible) 100.dp else 0.dp, 
                        bottom = if (isUIVisible) 80.dp else 0.dp
                    ) // 根据UI状态调整padding
                    .align(Alignment.Center)
            ) {
                if (scale > 1f) {
                    // 放大状态：显示单个图片，允许平移
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures(
                                    onGesture = { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale
                                        
                                        if (scale > 1f) {
                                            // 计算基于实际图片尺寸的平移限制
                                            val scaledWidth = size.width * scale
                                            val scaledHeight = size.height * scale
                                            val maxOffsetX = (scaledWidth - size.width) / 2f
                                            val maxOffsetY = (scaledHeight - size.height) / 2f
                                            
                                            offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    onTap = {
                                        // 单击切换全屏模式
                                        isUIVisible = !isUIVisible
                                    }
                                )
                            }
                    ) {
                        PhotoDetailItem(
                        photo = photos[pagerState.currentPage],
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        isFullScreen = !isUIVisible,
                        modifier = Modifier.fillMaxSize()
                    )
                    }
                } else {
                    // 正常状态：使用HorizontalPager切换图片
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures(
                                    onGesture = { _, _, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        
                                        if (newScale > 1f || zoom != 1f) {
                                            scale = newScale
                                            if (scale > 1f) {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = 3f
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    onTap = {
                                        // 单击切换全屏模式
                                        isUIVisible = !isUIVisible
                                    }
                                )
                            }
                    ) { page ->
                        PhotoDetailItem(
                        photo = photos[page],
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        isFullScreen = !isUIVisible,
                        modifier = Modifier.fillMaxSize()
                    )
                    }
                }
            }
            
            // 底部工具栏 (固定高度)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                AnimatedVisibility(
                    visible = isUIVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        if (photos.isNotEmpty() && pagerState.currentPage < photos.size) {
                            val currentPhoto = photos[pagerState.currentPage]
                            PhotoBottomToolBar(
                        photo = currentPhoto,
                        isFavorite = isFavorite,
                    onFavoriteClick = { 
                            android.util.Log.d("PhotoDetail", "Favorite button clicked, current state: $isFavorite")
                            val success = togglePhotoFavorite(context, currentPhoto.uri, isFavorite)
                            android.util.Log.d("PhotoDetail", "Toggle result: $success")
                            
                            if (success) {
                                // 立即更新UI状态
                                isFavorite = !isFavorite
                                android.util.Log.d("PhotoDetail", "UI updated to: $isFavorite")
                            } else {
                                // 如果数据库更新失败，仍然更新UI（用于测试）
                                android.util.Log.w("PhotoDetail", "Database update failed, updating UI anyway")
                                isFavorite = !isFavorite
                            }
                        },
                        onShareClick = { sharePhoto(context, currentPhoto.uri) },
                        onExifClick = { showExifDialog = true },
                        onDeleteClick = { showDeleteDialog = true }
                    )
                        }
                    }
                }
            }
        }
        
        // EXIF Info Dialog
        if (showExifDialog && photos.isNotEmpty() && pagerState.currentPage < photos.size) {
            ExifInfoDialog(
                photo = photos[pagerState.currentPage],
                onDismiss = { showExifDialog = false }
            )
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog && photos.isNotEmpty() && pagerState.currentPage < photos.size) {
            val currentPhoto = photos[pagerState.currentPage]
            
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(text = "删除照片")
                },
                text = {
                    Text(text = "确定要删除这张照片吗？此操作无法撤销。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // API 30+ 直接删除，不需要权限检查
                            deletePhoto(context, currentPhoto.uri, currentPhoto.id)
                            showDeleteDialog = false
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
    }
}

/**
 * 检查GPS坐标是否有效（不为0）
 */
private fun isValidGpsCoordinate(latitude: Double?, longitude: Double?): Boolean {
    return latitude != null && longitude != null && 
           latitude != 0.0 && longitude != 0.0 &&
           latitude >= -90.0 && latitude <= 90.0 &&
           longitude >= -180.0 && longitude <= 180.0
}

/**
 * 打开地图应用显示指定位置
 */
private fun openMapWithLocation(context: Context, latitude: Double, longitude: Double) {
    android.util.Log.d("PhotoDetail", "原始GPS坐标: 纬度=$latitude, 经度=$longitude")
    
    // 根据系统语言和地区判断是否需要坐标转换
    val (convertedLat, convertedLng) = CoordinateConverter.getConvertedCoordinates(latitude, longitude)
    android.util.Log.d("PhotoDetail", "转换后坐标: 纬度=$convertedLat, 经度=$convertedLng")
    
    try {
        // 首先尝试使用geo URI方案，这通常会打开默认的地图应用
        val uri = "geo:$convertedLat,$convertedLng?q=$convertedLat,$convertedLng(照片位置)"
        android.util.Log.d("PhotoDetail", "使用geo URI: $uri")
        
        val mapIntent = AndroidIntent().apply {
            action = AndroidIntent.ACTION_VIEW
            data = android.net.Uri.parse(uri)
            addFlags(AndroidIntent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // 对于API 30+，直接使用try-catch来处理无法找到应用的情况
        try {
            android.util.Log.d("PhotoDetail", "尝试启动地图应用...")
            context.startActivity(mapIntent)
            android.util.Log.d("PhotoDetail", "地图应用启动成功")
            return // 如果成功打开，直接返回
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.w("PhotoDetail", "未找到地图应用，尝试使用浏览器", e)
        } catch (e: Exception) {
            android.util.Log.e("PhotoDetail", "打开地图应用失败", e)
        }
        
        // 备选方案1：使用Google Maps网页版
        try {
            val webUri = "https://www.google.com/maps?q=$convertedLat,$convertedLng"
            android.util.Log.d("PhotoDetail", "尝试使用Google Maps网页版: $webUri")
            
            val webIntent = AndroidIntent().apply {
                action = AndroidIntent.ACTION_VIEW
                data = android.net.Uri.parse(webUri)
                addFlags(AndroidIntent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            android.util.Log.d("PhotoDetail", "Google Maps网页版启动成功")
            return // 如果成功打开，直接返回
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.e("PhotoDetail", "浏览器也打不开", e)
        } catch (e: Exception) {
            android.util.Log.e("PhotoDetail", "打开网页版地图失败", e)
        }
        
        // 备选方案2：使用更通用的地图服务
        try {
            val fallbackUri = "https://maps.google.com/maps?q=$convertedLat,$convertedLng"
            android.util.Log.d("PhotoDetail", "尝试使用通用地图服务: $fallbackUri")
            
            val fallbackIntent = AndroidIntent().apply {
                action = AndroidIntent.ACTION_VIEW
                data = android.net.Uri.parse(fallbackUri)
                addFlags(AndroidIntent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            android.util.Log.d("PhotoDetail", "通用地图服务启动成功")
        } catch (e: Exception) {
            android.util.Log.e("PhotoDetail", "所有地图打开方式都失败了", e)
            android.widget.Toast.makeText(context, "无法打开地图应用，请检查是否安装了浏览器或地图应用", android.widget.Toast.LENGTH_SHORT).show()
        }
        
    } catch (e: Exception) {
        android.util.Log.e("PhotoDetail", "打开地图失败", e)
        android.widget.Toast.makeText(context, "打开地图失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PhotoDetailItem(
    modifier: Modifier = Modifier,
    photo: Photo,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    isFullScreen: Boolean = false

) {
    val context = LocalContext.current
    var videoThumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoadingThumbnail by remember { mutableStateOf(false) }
    
    // 如果是视频，生成缩略图
    LaunchedEffect(photo.uri) {
        if (photo.mimeType.startsWith("video/")) {
            isLoadingThumbnail = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // API 30+ 直接使用ContentResolver.loadThumbnail
                    val thumbnail = context.contentResolver.loadThumbnail(
                        photo.uri,
                        android.util.Size(512, 512),
                        null
                    )
                    videoThumbnail = thumbnail
                } catch (e: Exception) {
                    android.util.Log.e("PhotoDetailItem", "生成视频缩略图失败: ${e.message}")
                    videoThumbnail = null
                } finally {
                    isLoadingThumbnail = false
                }
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (photo.mimeType.startsWith("video/")) {
            // 视频文件：显示缩略图和播放按钮
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 显示视频缩略图
                if (videoThumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = videoThumbnail!!.asImageBitmap(),
                        contentDescription = photo.displayName,
                        contentScale = if (isFullScreen) ContentScale.Fit else ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                                transformOrigin = TransformOrigin.Center
                            )
                    )
                } else if (isLoadingThumbnail) {
                    // 加载中显示进度条
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    // 缩略图加载失败，显示占位符
                    Text(
                        text = "视频",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                
                // 播放按钮
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            // 调用系统播放器播放视频
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(photo.uri, photo.mimeType)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("PhotoDetailItem", "无法播放视频: ${e.message}")
                                // 可以在这里添加Toast提示用户
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放视频",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        } else {
            // 图片文件：使用原来的逻辑
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .size(coil.size.Size.ORIGINAL) // 加载原始尺寸图片
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .crossfade(false) // 禁用淡入动画，提高清晰度
                    .build(),
                contentDescription = photo.displayName,
                contentScale = if (isFullScreen) ContentScale.Fit else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                        transformOrigin = TransformOrigin.Center
                    )
            )
        }
    }
}

@Composable
fun PhotoBottomToolBar(
    photo: Photo,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onExifClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onShareClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "分享",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Favorite button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onFavoriteClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isFavorite) "已收藏" else "收藏",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // EXIF info button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onExifClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "图片信息",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "信息",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Delete button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onDeleteClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ExifInfoDialog(
    photo: Photo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exifInfo = remember(photo.id) {
        ExifInfoUtil.extractExifInfo(context, photo.uri, photo.width, photo.height, photo.size)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "图片信息",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 内容区域
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 文件信息 - 简洁展示
                    item {
                        InfoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = photo.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                if (exifInfo.fileSize != "未知") {
                                    Text(
                                        text = exifInfo.fileSize,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // 拍摄时间和设备信息
                    if (exifInfo.captureTime != "未知" || exifInfo.cameraModel != "未知") {
                        item {
                            InfoCard {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (exifInfo.captureTime != "未知") {
                                        Text(
                                            text = exifInfo.captureTime,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (exifInfo.cameraModel != "未知") {
                                        Text(
                                            text = exifInfo.cameraModel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (exifInfo.lensModel != "未知") {
                                        Text(
                                            text = exifInfo.lensModel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 曝光三要素 - 组合在一行
                    if (exifInfo.aperture != "未知" || exifInfo.shutterSpeed != "未知" || exifInfo.iso != "未知") {
                        item {
                            InfoCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (exifInfo.aperture != "未知") {
                                        ExposureParameter(value = exifInfo.aperture)
                                    }
                                    if (exifInfo.shutterSpeed != "未知") {
                                        ExposureParameter(value = exifInfo.shutterSpeed)
                                    }
                                    if (exifInfo.iso != "未知") {
                                        ExposureParameter(value = exifInfo.iso)
                                    }
                                }
                            }
                        }
                    }
                    
                    // 其他拍摄参数
                    if (exifInfo.focalLength != "未知" || exifInfo.resolution != "未知") {
                        item {
                            InfoCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (exifInfo.focalLength != "未知") {
                                        SecondaryParameter(value = exifInfo.focalLength)
                                    }
                                    if (exifInfo.resolution != "未知") {
                                        SecondaryParameter(value = exifInfo.resolution)
                                    }
                                }
                            }
                        }
                    }
                    
                    // 高级设置
                    if (exifInfo.flash != "未知" || exifInfo.exposureMode != "未知" || 
                        exifInfo.meteringMode != "未知" || exifInfo.whiteBalance != "未知" || 
                        exifInfo.gpsLocation != "未知") {
                        item {
                            InfoCard {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (exifInfo.flash != "未知") {
                                        ParameterRow(label = "闪光灯", value = exifInfo.flash)
                                    }
                                    if (exifInfo.exposureMode != "未知") {
                                        ParameterRow(label = "曝光模式", value = exifInfo.exposureMode)
                                    }
                                    if (exifInfo.meteringMode != "未知") {
                                        ParameterRow(label = "测光模式", value = exifInfo.meteringMode)
                                    }
                                    if (exifInfo.whiteBalance != "未知") {
                                        ParameterRow(label = "白平衡", value = exifInfo.whiteBalance)
                                    }
                                    if (exifInfo.gpsLocation != "未知") {
                                        val isValidGps = isValidGpsCoordinate(exifInfo.gpsLatitude, exifInfo.gpsLongitude)
                                        android.util.Log.d("PhotoDetail", "GPS位置点击检查: gpsLatitude=${exifInfo.gpsLatitude}, gpsLongitude=${exifInfo.gpsLongitude}, isValid=$isValidGps")
                                        
                                        ParameterRow(
                                            label = "GPS位置", 
                                            value = exifInfo.gpsLocation,
                                            onClick = if (isValidGps) {
                                                { 
                                                    android.util.Log.d("PhotoDetail", "GPS位置被点击，坐标: ${exifInfo.gpsLatitude}, ${exifInfo.gpsLongitude}")
                                                    openMapWithLocation(context, exifInfo.gpsLatitude!!, exifInfo.gpsLongitude!!) 
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ExposureParameter(
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SecondaryParameter(
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ParameterRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val hasClickAction = onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasClickAction) {
                    Modifier
                        .clickable(onClick = onClick!!)
                        .padding(vertical = 4.dp)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onClick != null && label == "GPS位置") {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "位置图标",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasClickAction) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (hasClickAction) {
                TextDecoration.Underline
            } else {
                TextDecoration.None
            }
        )
    }
}



private fun sharePhoto(context: Context, uri: Uri) {
    val shareIntent = AndroidIntent().apply {
        action = AndroidIntent.ACTION_SEND
        type = "image/*"
        putExtra(AndroidIntent.EXTRA_STREAM, uri)
        addFlags(AndroidIntent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(AndroidIntent.createChooser(shareIntent, "分享图片"))
}

private fun isPhotoFavorite(context: Context, uri: Uri): Boolean {
    return try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            uri,
            arrayOf(android.provider.MediaStore.Images.ImageColumns.IS_FAVORITE),
            null,
            null,
            null
        )
        
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val favoriteIndex = c.getColumnIndex(android.provider.MediaStore.Images.ImageColumns.IS_FAVORITE)
                if (favoriteIndex >= 0) {
                    val favoriteValue = c.getInt(favoriteIndex)
                    android.util.Log.d("PhotoDetail", "Favorite value: $favoriteValue")
                    return favoriteValue == 1
                } else {
                    android.util.Log.d("PhotoDetail", "IS_FAVORITE column not found")
                }
            }
        }
        false
    } catch (e: Exception) {
        android.util.Log.e("PhotoDetail", "Error checking favorite status", e)
        false
    }
}

private fun togglePhotoFavorite(context: Context, uri: Uri, isFavorite: Boolean): Boolean {
    return try {
        val contentResolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.ImageColumns.IS_FAVORITE, if (isFavorite) 0 else 1)
        }
        
        val rowsUpdated = contentResolver.update(uri, values, null, null)
        android.util.Log.d("PhotoDetail", "Updated favorite status: $rowsUpdated rows affected")
        
        // 验证更新是否成功
        val newFavoriteStatus = isPhotoFavorite(context, uri)
        android.util.Log.d("PhotoDetail", "New favorite status: $newFavoriteStatus")
        
        rowsUpdated > 0
    } catch (e: Exception) {
        android.util.Log.e("PhotoDetail", "Error toggling favorite status", e)
        false
    }
}

private fun formatPhotoDate(context: Context, photo: Photo): String {
    return try {
        val uri = photo.uri
        val contentResolver = context.contentResolver
        
        // 首先尝试从EXIF数据中获取拍摄时间
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val exifInterface = ExifInterface(stream)
            // 按优先级检查不同的EXIF时间标签
            val dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?:
                           exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED) ?:
                           exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
            
            if (!dateTime.isNullOrEmpty()) {
                // 解析EXIF时间格式 "yyyy:MM:dd HH:mm:ss"
                val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                val displayFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
                val date = exifFormat.parse(dateTime)
                if (date != null) {
                    return displayFormat.format(date)
                }
            }
        }
        
        // 如果EXIF中没有拍摄时间，尝试获取文件修改时间
        val cursor = contentResolver.query(
            uri,
            arrayOf(android.provider.MediaStore.MediaColumns.DATE_MODIFIED),
            null,
            null,
            null
        )
        
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val modifiedTime = c.getLong(0) * 1000 // 转换为毫秒
                val displayFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
                return displayFormat.format(Date(modifiedTime))
            }
        }
        
        // 最后的备选方案：显示"未知时间"
        "未知时间"
        
    } catch (_: Exception) {
        // 出错时显示默认格式
        "未知时间"
    }
}

/**
 * 使用MediaStore.createDeleteRequest删除照片 (Android 11+标准方法)
 */
private fun deletePhoto(context: Context, photoUri: Uri, photoId: Long) {
    val mainActivity = context as? MainActivity
    if (mainActivity != null) {
        try {
            // 创建删除请求
            val contentResolver = context.contentResolver
            val request = android.provider.MediaStore.createDeleteRequest(contentResolver, listOf(photoUri))
            val intentSender = request.intentSender
            mainActivity.launchDeleteRequest(intentSender, photoId)
        } catch (e: Exception) {
            android.util.Log.e("PhotoDetail", "创建删除请求失败", e)
            android.widget.Toast.makeText(context, "删除失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    } else {
        android.widget.Toast.makeText(context, "删除失败: 无法获取Activity上下文", android.widget.Toast.LENGTH_SHORT).show()
    }
}

