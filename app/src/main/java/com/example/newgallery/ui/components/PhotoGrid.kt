package com.example.newgallery.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newgallery.data.model.Photo

/**
 * 通用照片网格组件 - 支持按日期分组和纯网格两种模式
 * 用于在PhotosScreen和相册详情页面中重用
 */
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    columnCount: Int = 4,
    showDateHeaders: Boolean = true,
    onPhotoClick: (photo: Photo, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    enableZoom: Boolean = true,
    onZoomChange: ((Float) -> Unit)? = null,
    favoritePhotoIds: List<Long> = emptyList()
) {
    // 支持手势缩放
    var currentColumnCount by remember { mutableStateOf(columnCount) }
    var gridScale by remember { mutableStateOf(1f) }
    
    // 当外部列数改变时更新内部状态
    LaunchedEffect(columnCount) {
        currentColumnCount = columnCount
        gridScale = 1f
    }
    
    val gridModifier = if (enableZoom) {
        modifier.pointerInput(Unit) {
            detectTransformGestures(
                onGesture = { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        // 计算缩放变化量
                        val scaleChange = if (zoom > 1f) {
                            -(zoom - 1f) * 0.5f // 放大：减少列数
                        } else {
                            (1f - zoom) * 0.5f // 缩小：增加列数
                        }
                        
                        // 更新缩放状态
                        val newScale = (gridScale + scaleChange).coerceIn(0.5f, 2f)
                        if (newScale != gridScale) {
                            gridScale = newScale
                            // 根据缩放比例计算列数
                            val scaledColumns = columnCount / gridScale
                            currentColumnCount = scaledColumns.toInt().coerceIn(2, 8)
                            onZoomChange?.invoke(gridScale)
                        }
                    }
                }
            )
        }
    } else {
        modifier
    }
    
    if (showDateHeaders && photos.isNotEmpty()) {
        // 按日期分组显示模式（类似PhotosScreen）
        PhotoGridByDate(
            photos = photos,
            columnCount = currentColumnCount,
            onPhotoClick = onPhotoClick,
            modifier = gridModifier
        )
    } else {
        // 纯网格显示模式（适合相册详情）
        PhotoGridSimple(
            photos = photos,
            columnCount = currentColumnCount,
            onPhotoClick = onPhotoClick,
            modifier = gridModifier
        )
    }
}

/**
 * 按日期分组的照片网格（PhotosScreen的现有逻辑）
 */
@Composable
private fun PhotoGridByDate(
    photos: List<Photo>,
    columnCount: Int,
    onPhotoClick: (photo: Photo, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 按日期分组照片
    val photosByDate = remember(photos) {
        photos.groupBy { photo ->
            val calendar = java.util.Calendar.getInstance()
            calendar.time = photo.dateTaken
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            String.format("%04d-%02d-%02d", year, month, day)
        }
    }
    
    // 预计算所有照片的索引映射
    val photoIndexMap = remember(photos) {
        photos.mapIndexed { index, photo -> photo.id to index }.toMap()
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier,
        contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        photosByDate.keys.sortedDescending().forEach { dateKey ->
            val photosForDate = photosByDate[dateKey] ?: emptyList()
            
            // 日期标题
            item(
                key = "header_$dateKey",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                DateHeader(dateKey = dateKey)
            }
            
            // 照片项目
            items(
                items = photosForDate,
                key = { it.id }
            ) { photo ->
                PhotoItem(
                    photo = photo,
                    isFavorite = favoritePhotoIds.contains(photo.id),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    onClick = {
                        val index = photoIndexMap[photo.id] ?: 0
                        onPhotoClick(photo, index)
                    }
                )
            }
        }
    }
}

/**
 * 简单的照片网格（适合相册详情页面）
 */
@Composable
private fun PhotoGridSimple(
    photos: List<Photo>,
    columnCount: Int,
    onPhotoClick: (photo: Photo, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier,
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(
            items = photos,
            key = { it.id }
        ) { photo ->
            PhotoItem(
                photo = photo,
                isFavorite = favoritePhotoIds.contains(photo.id),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                onClick = {
                    val index = photos.indexOf(photo)
                    onPhotoClick(photo, index)
                }
            )
        }
    }
}

/**
 * 日期标题组件
 */
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