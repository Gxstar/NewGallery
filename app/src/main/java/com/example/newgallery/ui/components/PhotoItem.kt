package com.example.newgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import com.example.newgallery.data.model.Photo
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoItem(
    photo: Photo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingThumbnail by remember { mutableStateOf(false) }
    
    // 如果是视频，生成缩略图
    LaunchedEffect(photo.uri) {
        if (photo.mimeType.startsWith("video/")) {
            isLoadingThumbnail = true
            withContext(Dispatchers.IO) {
                try {
                    val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ 使用新的API，从ContentResolver获取缩略图
                        context.contentResolver.loadThumbnail(
                            photo.uri,
                            android.util.Size(512, 512),
                            null
                        )
                    } else {
                        // Android 9及以下使用旧的API
                        @Suppress("DEPRECATION")
                        ThumbnailUtils.createVideoThumbnail(
                            photo.uri.toString(),
                            MediaStore.Images.Thumbnails.MINI_KIND
                        )
                    }
                    videoThumbnail = thumbnail
                } catch (e: Exception) {
                    android.util.Log.e("PhotoItem", "生成视频缩略图失败: ${e.message}")
                    videoThumbnail = null
                } finally {
                    isLoadingThumbnail = false
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        // 点击事件处理
                        onClick()
                    }
                }
            }
    ) {
        when {
            photo.mimeType.startsWith("video/") && isLoadingThumbnail -> {
                // 显示加载指示器
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            photo.mimeType.startsWith("video/") && videoThumbnail != null -> {
                // 显示视频缩略图
                androidx.compose.foundation.Image(
                    bitmap = videoThumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // 显示图片或视频缩略图生成失败时的原图
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photo.uri)
                        .crossfade(true)
                        .apply {
                            // 如果是视频且缩略图生成失败，尝试让Coil处理视频帧提取
                            if (photo.mimeType.startsWith("video/")) {
                                // Coil会自动尝试提取视频的第一帧作为缩略图
                            }
                        }
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // 如果是视频，显示播放图标
        if (photo.mimeType.startsWith("video/")) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "视频",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}