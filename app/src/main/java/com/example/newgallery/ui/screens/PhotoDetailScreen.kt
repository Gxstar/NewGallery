package com.example.newgallery.ui.screens

import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newgallery.data.model.Photo
import com.example.newgallery.ui.theme.TextPrimary
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ScrollStateViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory
import com.example.newgallery.utils.ExifInfoUtil

@OptIn(ExperimentalFoundationApi::class)
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
    val scrollStateViewModel: ScrollStateViewModel = viewModel(factory = ViewModelFactory(context))
    
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
    
    // State for UI visibility (top bar and bottom toolbar)
    var isUIVisible by remember { mutableStateOf(true) }
    
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
        if (photos.isNotEmpty()) {
            val currentPhoto = photos[pagerState.currentPage]
            isFavorite = isPhotoFavorite(context, currentPhoto.uri)
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
                    // Back button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                
                // Photo info - 显示拍摄时间
                    val currentPhoto = photos[pagerState.currentPage]
                    Text(
                        text = formatPhotoDate(context, currentPhoto), // 使用EXIF或文件时间
                        color = TextPrimary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    )
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
                                    onGesture = { centroid, pan, zoom, _ ->
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
                                    onGesture = { centroid, pan, zoom, _ ->
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
                            android.util.Log.d("PhotoDetail", "Delete confirmed, attempting to delete photo")
                            deletePhoto(context, currentPhoto.uri) { success ->
                                android.util.Log.d("PhotoDetail", "Delete operation completed with success: $success")
                                if (success) {
                                    // 删除成功，返回上一页
                                    android.util.Log.d("PhotoDetail", "Delete successful, finishing activity")
                                    (context as android.app.Activity).finish()
                                } else {
                                    // 删除失败，显示错误信息
                                    android.util.Log.e("PhotoDetail", "Delete failed, showing error message")
                                    showDeleteDialog = false
                                    // 这里可以添加Toast或其他错误提示
                                    android.widget.Toast.makeText(
                                        context,
                                        "删除失败，请检查权限设置",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
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

@Composable
fun PhotoDetailItem(
    photo: Photo,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    isFullScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
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
                                    overflow = TextOverflow.Ellipsis
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
                                        ParameterRow(label = "GPS位置", value = exifInfo.gpsLocation)
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
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun sharePhoto(context: android.content.Context, uri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
}

private fun isPhotoFavorite(context: android.content.Context, uri: Uri): Boolean {
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

private fun togglePhotoFavorite(context: android.content.Context, uri: Uri, isFavorite: Boolean): Boolean {
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

private fun formatPhotoDate(context: android.content.Context, photo: com.example.newgallery.data.model.Photo): String {
    return try {
        val uri = photo.uri
        val contentResolver = context.contentResolver
        
        // 首先尝试从EXIF数据中获取拍摄时间
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val exifInterface = android.media.ExifInterface(stream)
            // 按优先级检查不同的EXIF时间标签
            val dateTime = exifInterface.getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL) ?:
                           exifInterface.getAttribute(android.media.ExifInterface.TAG_DATETIME_DIGITIZED) ?:
                           exifInterface.getAttribute(android.media.ExifInterface.TAG_DATETIME)
            
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
        
    } catch (e: Exception) {
        // 出错时显示默认格式
        "未知时间"
    }
}

/**
 * 删除照片函数，使用最新的Android MediaStore API
 */
private fun deletePhoto(context: android.content.Context, photoUri: android.net.Uri, onComplete: (Boolean) -> Unit) {
    try {
        android.util.Log.d("PhotoDetail", "Starting delete operation for URI: $photoUri")
        android.util.Log.d("PhotoDetail", "Android SDK version: ${android.os.Build.VERSION.SDK_INT}")
        
        val contentResolver = context.contentResolver
        
        // 检查URI的权限和类型
        android.util.Log.d("PhotoDetail", "URI scheme: ${photoUri.scheme}")
        android.util.Log.d("PhotoDetail", "URI authority: ${photoUri.authority}")
        
        // 尝试使用ContentResolver的delete方法，但添加更多错误处理
        try {
            // 检查我们是否有权限访问这个URI
            contentResolver.query(photoUri, null, null, null, null)?.use { cursor ->
                android.util.Log.d("PhotoDetail", "Successfully queried URI, cursor count: ${cursor.count}")
            }
            
            // 尝试删除
            val deletedRows = contentResolver.delete(photoUri, null, null)
            android.util.Log.d("PhotoDetail", "Delete result: deletedRows = $deletedRows")
            
            if (deletedRows > 0) {
                // 删除成功
                android.util.Log.d("PhotoDetail", "Delete successful")
                onComplete(true)
                return
            } else {
                android.util.Log.w("PhotoDetail", "Delete returned 0 rows affected")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("PhotoDetail", "SecurityException during delete", e)
        } catch (e: Exception) {
            android.util.Log.e("PhotoDetail", "Exception during delete", e)
        }
        
        // 如果直接删除失败，尝试其他方法
        android.util.Log.d("PhotoDetail", "Direct delete failed, trying alternative methods")
        performTraditionalDelete(contentResolver, photoUri, context, onComplete)
        
    } catch (e: Exception) {
        android.util.Log.e("PhotoDetail", "Error initiating photo deletion", e)
        onComplete(false)
    }
}

/**
 * 执行传统的删除操作
 */
private fun performTraditionalDelete(
    contentResolver: android.content.ContentResolver,
    photoUri: android.net.Uri,
    context: android.content.Context,
    onComplete: (Boolean) -> Unit
) {
    try {
        android.util.Log.d("PhotoDetail", "Attempting to delete URI: $photoUri")
        
        // 对于Android 11+，我们需要使用不同的方法
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.util.Log.d("PhotoDetail", "Using Android 11+ delete method")
            
            // 尝试使用ContentResolver的delete方法，但在Android 11+中可能需要特殊处理
            try {
                val deletedRows = contentResolver.delete(
                    photoUri,
                    null,
                    null
                )
                
                android.util.Log.d("PhotoDetail", "Android 11+ delete result: $deletedRows")
                
                if (deletedRows > 0) {
                    // 删除成功
                    android.util.Log.d("PhotoDetail", "Delete successful on Android 11+")
                    onComplete(true)
                    return
                }
            } catch (e: SecurityException) {
                android.util.Log.e("PhotoDetail", "SecurityException on Android 11+", e)
            }
            
            // 如果Android 11+的删除失败，尝试使用MediaStore API
            try {
                val selection = "${android.provider.MediaStore.MediaColumns._ID} = ?"
                val selectionArgs = arrayOf(photoUri.lastPathSegment?.split(":")?.lastOrNull() ?: "")
                
                val deletedRows = contentResolver.delete(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    selection,
                    selectionArgs
                )
                
                android.util.Log.d("PhotoDetail", "MediaStore delete result: $deletedRows")
                
                if (deletedRows > 0) {
                    android.util.Log.d("PhotoDetail", "MediaStore delete successful")
                    onComplete(true)
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoDetail", "MediaStore delete failed", e)
            }
        } else {
            // 对于较旧的Android版本，使用传统方法
            android.util.Log.d("PhotoDetail", "Using traditional delete method")
            
            val deletedRows = contentResolver.delete(
                photoUri,
                null,
                null
            )
            
            val success = deletedRows > 0
            android.util.Log.d("PhotoDetail", "Traditional delete result: $success, deleted rows: $deletedRows")
            
            if (success) {
                // 通知媒体扫描器更新
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(photoUri.toString()),
                    null
                ) { path, uri ->
                    android.util.Log.d("PhotoDetail", "Media scanner completed for: $path")
                }
                onComplete(true)
                return
            }
        }
        
        // 如果所有方法都失败，尝试直接删除文件
        android.util.Log.d("PhotoDetail", "All delete methods failed, trying direct file deletion")
        tryDeleteFileDirectly(photoUri, context, onComplete)
        
    } catch (e: SecurityException) {
        android.util.Log.e("PhotoDetail", "Permission denied when deleting photo", e)
        // 尝试使用文件系统删除
        tryDeleteFileDirectly(photoUri, context, onComplete)
    } catch (e: Exception) {
        android.util.Log.e("PhotoDetail", "Error deleting photo", e)
        onComplete(false)
    }
}

/**
 * 尝试直接删除文件
 */
private fun tryDeleteFileDirectly(
    photoUri: android.net.Uri, 
    context: android.content.Context,
    onComplete: (Boolean) -> Unit
) {
    try {
        android.util.Log.d("PhotoDetail", "Attempting direct file deletion for URI: $photoUri")
        
        // 尝试从URI获取文件路径
        val cursor: android.database.Cursor? = context.contentResolver.query(
            photoUri, 
            arrayOf(android.provider.MediaStore.MediaColumns.DATA), 
            null, 
            null, 
            null
        )
        
        val filePath: String? = cursor?.use { c ->
            if (c.moveToFirst()) {
                c.getString(c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA))
            } else null
        }
        
        android.util.Log.d("PhotoDetail", "Extracted file path: $filePath")
        
        if (filePath != null) {
            val file = java.io.File(filePath)
            android.util.Log.d("PhotoDetail", "File exists: ${file.exists()}, File path: ${file.absolutePath}")
            
            if (file.exists()) {
                val success = file.delete()
                android.util.Log.d("PhotoDetail", "Direct file delete result: $success")
                
                if (success) {
                    // 通知媒体扫描器更新
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(filePath),
                        null
                    ) { path, uri ->
                        android.util.Log.d("PhotoDetail", "Media scanner completed for: $path")
                    }
                }
                
                onComplete(success)
            } else {
                android.util.Log.w("PhotoDetail", "File does not exist at path: $filePath")
                onComplete(false)
            }
        } else {
            android.util.Log.w("PhotoDetail", "Could not extract file path from URI")
            onComplete(false)
        }
    } catch (e: Exception) {
        android.util.Log.e("PhotoDetail", "Error deleting file directly", e)
        onComplete(false)
    }
}