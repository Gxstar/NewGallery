package com.example.newgallery.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 收藏实体类 - 存储在本地Room数据库中
 * 用于记录用户收藏的照片ID
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val photoId: Long
)