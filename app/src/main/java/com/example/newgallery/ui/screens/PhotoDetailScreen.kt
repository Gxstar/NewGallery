package com.example.newgallery.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import com.example.newgallery.ui.theme.TextPrimary
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newgallery.data.model.Photo
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
    val initialPhotoIndex = photos.indexOfFirst { it.id == photoId }.takeIf { it != -1 } ?: initialIndex
    val pagerState = rememberPagerState(
        initialPage = initialPhotoIndex,
        pageCount = { photos.size }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
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
            // Photo pager
            HorizontalPager(state = pagerState) { page ->
                PhotoDetailItem(
                    photo = photos[page],
                    onScaleBelowNormal = { shouldExit = true }
                )
            }
        }
        
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary
            )
        }
        
        // Photo info
        if (photos.isNotEmpty() && pagerState.currentPage < photos.size) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                val currentPhoto = photos[pagerState.currentPage]
                Text(
                    text = currentPhoto.displayName,
                    color = TextPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoDetailItem(
    photo: Photo,
    onScaleBelowNormal: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // 透明度动画 - 使用更短的动画时间提高响应速度
    val imageAlpha by animateFloatAsState(
        targetValue = if (scale < 1f) scale else 1f,
        animationSpec = tween(durationMillis = 30), // 极短动画时间，几乎实时响应
        finishedListener = { 
            // 当动画完成且scale小于0.7时触发退出
            if (scale < 0.7f) {
                onScaleBelowNormal()
            }
        }
    )
    
    // 双击缩放功能
    val handleDoubleTap = {
        if (scale <= 1.5f) {
            // 放大到2倍
            scale = 2f
        } else {
            // 缩小到正常大小
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }
    

    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 检测双指缩放和拖动手势 - 优化跟手性
                    detectTransformGestures(
                        onGesture = { centroid, pan, zoom, _ ->
                            // 直接计算新的缩放值，减少中间环节
                            val newScale = (scale * zoom).coerceIn(0.3f, 5f)
                            
                            when {
                                newScale > 1f -> {
                                    // 放大状态，允许平移
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    val maxOffset = 1000f * (newScale - 1f)
                                    offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                                    offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                                    scale = newScale
                                    true
                                }
                                zoom < 1f && scale <= 1f -> {
                                    // 双指缩小且图片是正常大小，准备退出
                                    offsetX = 0f
                                    offsetY = 0f
                                    scale = newScale
                                    true
                                }
                                else -> {
                                    // 正常状态，不消费手势
                                    false
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { handleDoubleTap() }
                    )
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    transformOrigin = TransformOrigin.Center,
                    alpha = imageAlpha
                )
        )
    }
}