package com.example.newgallery.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理工具类
 * 用于管理应用的各种设置选项
 */
object SettingsManager {
    
    private const val PREFS_NAME = "new_gallery_settings"
    private const val KEY_COORDINATE_CONVERSION_ENABLED = "coordinate_conversion_enabled"
    private const val KEY_COORDINATE_CONVERSION_MANUAL = "coordinate_conversion_manual"
    
    private lateinit var sharedPreferences: SharedPreferences
    
    /**
     * 初始化设置管理器
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 是否启用坐标转换功能
     * 默认根据系统语言自动判断
     */
    fun isCoordinateConversionEnabled(): Boolean {
        // 如果用户手动设置了，优先使用手动设置
        if (sharedPreferences.contains(KEY_COORDINATE_CONVERSION_MANUAL)) {
            return sharedPreferences.getBoolean(KEY_COORDINATE_CONVERSION_MANUAL, false)
        }
        // 否则根据系统语言自动判断
        return sharedPreferences.getBoolean(KEY_COORDINATE_CONVERSION_ENABLED, 
            CoordinateConverter.shouldConvertCoordinates())
    }
    
    /**
     * 设置坐标转换功能开关
     */
    fun setCoordinateConversionEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_COORDINATE_CONVERSION_MANUAL, enabled).apply()
    }
    
    /**
     * 重置坐标转换设置为自动判断
     */
    fun resetCoordinateConversionToAuto() {
        sharedPreferences.edit().remove(KEY_COORDINATE_CONVERSION_MANUAL).apply()
    }
    
    /**
     * 获取坐标转换状态的描述文本
     */
    fun getCoordinateConversionStatusText(): String {
        return when {
            sharedPreferences.contains(KEY_COORDINATE_CONVERSION_MANUAL) -> {
                if (isCoordinateConversionEnabled()) "已启用" else "已禁用"
            }
            else -> "自动"
        }
    }
}