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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newgallery.data.model.Photo
import com.example.newgallery.ui.theme.TextPrimary
import com.example.newgallery.ui.viewmodel.SharedPhotoViewModel
import com.example.newgallery.ui.viewmodel.ViewModelFactory

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
    
    // State for UI visibility (top bar and bottom toolbar)
    var isUIVisible by remember { mutableStateOf(true) }
    
    // System UI control
    LaunchedEffect(isUIVisible) {
        val window = (context as android.app.Activity).window
        val windowInsetsController = window.insetsController
        
        if (isUIVisible) {
            // Show system UI
            windowInsetsController?.show(android.view.WindowInsets.Type.systemBars())
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
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
                        onDeleteClick = { /* TODO: Implement delete functionality */ }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "图片信息")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoItem("文件名", photo.displayName)
                InfoItem("路径", photo.uri.toString())
                // TODO: Add more EXIF information when available
                InfoItem("大小", "未知")
                InfoItem("拍摄时间", "未知")
                InfoItem("相机型号", "未知")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
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
            val dateTime = exifInterface.getAttribute(android.media.ExifInterface.TAG_DATETIME)
            
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