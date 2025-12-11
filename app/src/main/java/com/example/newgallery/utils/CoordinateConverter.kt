package com.example.newgallery.utils

/**
 * GPS坐标转换工具类
 * 处理WGS-84坐标系到GCJ-02（火星坐标系）的转换
 * 适用于中国大陆地区的地图应用
 */
object CoordinateConverter {
    
    private const val PI = Math.PI
    private const val X_PI = (PI * 3000.0) / 180.0
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    /**
     * 判断是否在中国大陆范围外
     */
    private fun outOfChina(lat: Double, lng: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    /**
     * 转换纬度
     */
    private fun transformLat(lng: Double, lat: Double): Double {
        var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lng + 0.2 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    /**
     * 转换经度
     */
    private fun transformLng(lng: Double, lat: Double): Double {
        var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    /**
     * WGS-84坐标系转换为GCJ-02坐标系（火星坐标系）
     * 适用于中国大陆地区
     * 
     * @param wgsLat WGS-84纬度
     * @param wgsLng WGS-84经度
     * @return Pair<转换后的纬度, 转换后的经度>
     */
    fun wgs84ToGcj02(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        android.util.Log.d("CoordinateConverter", "转换前坐标: 纬度=$wgsLat, 经度=$wgsLng")
        
        if (outOfChina(wgsLat, wgsLng)) {
            android.util.Log.d("CoordinateConverter", "坐标在中国大陆外，无需转换")
            return Pair(wgsLat, wgsLng)
        }

        var dlat = transformLat(wgsLng - 105.0, wgsLat - 35.0)
        var dlng = transformLng(wgsLng - 105.0, wgsLat - 35.0)
        val radlat = wgsLat / 180.0 * PI
        var magic = Math.sin(radlat)
        magic = 1 - EE * magic * magic
        val sqrtmagic = Math.sqrt(magic)
        dlat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
        dlng = (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * PI)
        val mglat = wgsLat + dlat
        val mglng = wgsLng + dlng

        android.util.Log.d("CoordinateConverter", "转换后坐标: 纬度=$mglat, 经度=$mglng")
        return Pair(mglat, mglng)
    }

    /**
     * 判断是否需要坐标转换
     * 基于系统语言和地区设置
     */
    fun shouldConvertCoordinates(): Boolean {
        val locale = java.util.Locale.getDefault()
        val language = locale.language
        val country = locale.country
        
        android.util.Log.d("CoordinateConverter", "系统语言: $language, 地区: $country")
        
        // 简体中文用户通常需要坐标转换
        return language == "zh" || country == "CN"
    }

    /**
     * 获取转换后的坐标（如果需要的话）
     * 根据设置判断是否需要转换
     */
    fun getConvertedCoordinates(latitude: Double, longitude: Double): Pair<Double, Double> {
        return if (SettingsManager.isCoordinateConversionEnabled()) {
            wgs84ToGcj02(latitude, longitude)
        } else {
            Pair(latitude, longitude)
        }
    }
}