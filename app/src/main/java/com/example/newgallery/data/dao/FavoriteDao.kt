package com.example.newgallery.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.newgallery.data.model.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * 收藏DAO接口 - 定义收藏相关的数据库操作
 */
@Dao
interface FavoriteDao {
    /**
     * 插入一条收藏记录
     */
    @Insert
    suspend fun insert(favorite: FavoriteEntity)

    /**
     * 删除一条收藏记录
     */
    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    /**
     * 查询所有收藏的照片ID
     */
    @Query("SELECT photoId FROM favorites")
    fun getAllFavoritePhotoIds(): Flow<List<Long>>

    /**
     * 查询指定照片ID是否被收藏
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE photoId = :photoId)")
    suspend fun isPhotoFavorite(photoId: Long): Boolean
}