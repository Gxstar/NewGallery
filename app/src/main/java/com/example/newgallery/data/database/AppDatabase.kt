package com.example.newgallery.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.newgallery.data.dao.FavoriteDao
import com.example.newgallery.data.model.FavoriteEntity

/**
 * 应用数据库类 - 管理Room数据库
 */
@Database(entities = [FavoriteEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    // 获取收藏DAO
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        // 单例模式
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * 构建数据库
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gallery.db"
            )
                .fallbackToDestructiveMigration() // 数据库升级时删除旧数据
                .build()
        }
    }
}