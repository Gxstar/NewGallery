package com.example.newgallery.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * EXIF信息数据类
 */
data class ExifInfo(
    val captureTime: String = "未知",
    val resolution: String = "未知",
    val cameraModel: String = "未知",
    val lensModel: String = "未知",
    val aperture: String = "未知",
    val shutterSpeed: String = "未知",
    val iso: String = "未知",
    val focalLength: String = "未知",
    val flash: String = "未知",
    val exposureMode: String = "未知",
    val meteringMode: String = "未知",
    val whiteBalance: String = "未知",
    val gpsLocation: String = "未知",
    val fileSize: String = "未知",
    val fileFormat: String = "未知"
)

/**
 * EXIF信息提取工具类
 */
object ExifInfoUtil {
    
    /**
     * 从图片URI提取EXIF信息
     */
    fun extractExifInfo(context: Context, uri: Uri, width: Int = 0, height: Int = 0, size: Long = 0): ExifInfo {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                
                // 提取拍摄时间
                val captureTime = extractDateTime(exif)
                
                // 提取分辨率
                val resolution = if (width > 0 && height > 0) {
                    "$width × $height"
                } else {
                    extractResolution(exif)
                }
                
                // 提取相机信息
                val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "未知"
                val lensModel = exif.getAttribute("LensModel") ?: 
                               exif.getAttribute("LensModel") ?: "未知"
                
                // 提取拍摄参数
                val aperture = extractAperture(exif)
                val shutterSpeed = extractShutterSpeed(exif)
                val iso = extractISO(exif)
                val focalLength = extractFocalLength(exif)
                val flash = extractFlash(exif)
                val exposureMode = extractExposureMode(exif)
                val meteringMode = extractMeteringMode(exif)
                val whiteBalance = extractWhiteBalance(exif)
                val gpsLocation = extractGPSLocation(exif)
                
                // 提取文件信息
                val fileSize = if (size > 0) formatFileSize(size) else "未知"
                val fileFormat = extractMimeType(context, uri)
                
                ExifInfo(
                    captureTime = captureTime,
                    resolution = resolution,
                    cameraModel = cameraModel,
                    lensModel = lensModel,
                    aperture = aperture,
                    shutterSpeed = shutterSpeed,
                    iso = iso,
                    focalLength = focalLength,
                    flash = flash,
                    exposureMode = exposureMode,
                    meteringMode = meteringMode,
                    whiteBalance = whiteBalance,
                    gpsLocation = gpsLocation,
                    fileSize = fileSize,
                    fileFormat = fileFormat
                )
            } ?: ExifInfo()
        } catch (e: Exception) {
            Log.e("ExifInfoUtil", "Error extracting EXIF info: $e")
            ExifInfo()
        }
    }
    
    /**
     * 提取拍摄时间
     */
    private fun extractDateTime(exif: ExifInterface): String {
        return try {
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: 
                          exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED) ?: 
                          exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            
            if (!dateTime.isNullOrEmpty()) {
                // 解析EXIF时间格式 "yyyy:MM:dd HH:mm:ss"
                val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                val displayFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
                val date = exifFormat.parse(dateTime)
                if (date != null) {
                    return displayFormat.format(date)
                }
            }
            "未知"
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract date time: $e")
            "未知"
        }
    }
    
    /**
     * 提取分辨率
     */
    private fun extractResolution(exif: ExifInterface): String {
        return try {
            val width = exif.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION)
            val height = exif.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION)
            
            if (!width.isNullOrEmpty() && !height.isNullOrEmpty()) {
                "$width × $height"
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract resolution: $e")
            "未知"
        }
    }
    
    /**
     * 提取光圈值
     */
    private fun extractAperture(exif: ExifInterface): String {
        return try {
            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
            if (!fNumber.isNullOrEmpty()) {
                "f/$fNumber"
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract aperture: $e")
            "未知"
        }
    }
    
    /**
     * 提取快门速度
     */
    private fun extractShutterSpeed(exif: ExifInterface): String {
        return try {
            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            if (!exposureTime.isNullOrEmpty()) {
                val time = exposureTime.toDouble()
                if (time < 1) {
                    // 对于小于1秒的快门速度，显示为1/x
                    val reciprocal = (1.0 / time).toInt()
                    "1/${reciprocal}秒"
                } else {
                    "${exposureTime}秒"
                }
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract shutter speed: $e")
            "未知"
        }
    }
    
    /**
     * 提取ISO值
     */
    private fun extractISO(exif: ExifInterface): String {
        return try {
            // 尝试新的TAG_ISO_SPEED，如果不存在则尝试旧的TAG_ISO_SPEED_RATINGS
            val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED) ?: 
                     exif.getAttribute("ISOSpeedRatings") ?:
                     exif.getAttribute("ISO")
            if (!iso.isNullOrEmpty()) {
                "ISO $iso"
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract ISO: $e")
            "未知"
        }
    }
    
    /**
     * 提取焦距
     */
    private fun extractFocalLength(exif: ExifInterface): String {
        return try {
            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            if (!focalLength.isNullOrEmpty()) {
                // 尝试将分数转换为小数
                val focalLengthValue = try {
                    if (focalLength.contains("/")) {
                        val parts = focalLength.split("/")
                        if (parts.size == 2) {
                            val numerator = parts[0].toDoubleOrNull()
                            val denominator = parts[1].toDoubleOrNull()
                            if (numerator != null && denominator != null && denominator != 0.0) {
                                val result = numerator / denominator
                                // 如果是整数，显示为整数，否则显示一位小数
                                if (result == result.toInt().toDouble()) {
                                    "${result.toInt()}"
                                } else {
                                    String.format("%.1f", result)
                                }
                            } else {
                                focalLength
                            }
                        } else {
                            focalLength
                        }
                    } else {
                        val value = focalLength.toDoubleOrNull()
                        if (value != null) {
                            if (value == value.toInt().toDouble()) {
                                "${value.toInt()}"
                            } else {
                                String.format("%.1f", value)
                            }
                        } else {
                            focalLength
                        }
                    }
                } catch (e: Exception) {
                    focalLength
                }
                
                // 获取等效35mm焦距
                val focalLength35mm = exif.getAttribute("FocalLengthIn35mmFilm") ?: 
                                     exif.getAttribute("FocalLengthIn35mmFormat")
                
                val result = if (!focalLength35mm.isNullOrEmpty()) {
                    try {
                        val equivalentValue = if (focalLength35mm.contains("/")) {
                            val parts = focalLength35mm.split("/")
                            if (parts.size == 2) {
                                val numerator = parts[0].toDoubleOrNull()
                                val denominator = parts[1].toDoubleOrNull()
                                if (numerator != null && denominator != null && denominator != 0.0) {
                                    val result = numerator / denominator
                                    if (result == result.toInt().toDouble()) {
                                        "${result.toInt()}"
                                    } else {
                                        String.format("%.1f", result)
                                    }
                                } else {
                                    focalLength35mm
                                }
                            } else {
                                focalLength35mm
                            }
                        } else {
                            val value = focalLength35mm.toDoubleOrNull()
                            if (value != null) {
                                if (value == value.toInt().toDouble()) {
                                    "${value.toInt()}"
                                } else {
                                    String.format("%.1f", value)
                                }
                            } else {
                                focalLength35mm
                            }
                        }
                        "${focalLengthValue}mm (${equivalentValue}mm)"
                    } catch (e: Exception) {
                        "${focalLengthValue}mm"
                    }
                } else {
                    "${focalLengthValue}mm"
                }
                
                result
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract focal length: $e")
            "未知"
        }
    }
    
    /**
     * 提取闪光灯状态
     */
    private fun extractFlash(exif: ExifInterface): String {
        return try {
            val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
            if (!flash.isNullOrEmpty()) {
                val flashValue = flash.toInt()
                if (flashValue and 0x1 != 0) {
                    "开启"
                } else {
                    "关闭"
                }
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract flash: $e")
            "未知"
        }
    }
    
    /**
     * 提取曝光模式
     */
    private fun extractExposureMode(exif: ExifInterface): String {
        return try {
            val exposureMode = exif.getAttribute(ExifInterface.TAG_EXPOSURE_MODE)
            when (exposureMode) {
                "0" -> "自动"
                "1" -> "手动"
                "2" -> "自动包围"
                else -> "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract exposure mode: $e")
            "未知"
        }
    }
    
    /**
     * 提取测光模式
     */
    private fun extractMeteringMode(exif: ExifInterface): String {
        return try {
            val meteringMode = exif.getAttribute(ExifInterface.TAG_METERING_MODE)
            when (meteringMode) {
                "1" -> "平均"
                "2" -> "中央重点"
                "3" -> "点测光"
                "4" -> "多点"
                "5" -> "评估"
                "6" -> "局部"
                else -> "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract metering mode: $e")
            "未知"
        }
    }
    
    /**
     * 提取白平衡模式
     */
    private fun extractWhiteBalance(exif: ExifInterface): String {
        return try {
            val whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
            when (whiteBalance) {
                "0" -> "自动"
                "1" -> "手动"
                else -> "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract white balance: $e")
            "未知"
        }
    }
    
    /**
     * 提取GPS位置信息
     */
    private fun extractGPSLocation(exif: ExifInterface): String {
        return try {
            // 获取经纬度
            val latLong = exif.latLong
            if (latLong != null) {
                val latitude = latLong[0]
                val longitude = latLong[1]
                // 格式化为度分秒格式
                String.format("%.6f, %.6f", latitude, longitude)
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract GPS location: $e")
            "未知"
        }
    }
    
    /**
     * 提取MIME类型
     */
    private fun extractMimeType(context: Context, uri: Uri): String {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            mimeType ?: "未知"
        } catch (e: Exception) {
            Log.d("ExifInfoUtil", "Failed to extract MIME type: $e")
            "未知"
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return if (size < 1024) {
            "$size B"
        } else if (size < 1024 * 1024) {
            "${size / 1024}KB"
        } else if (size < 1024 * 1024 * 1024) {
            "${size / (1024 * 1024)}MB"
        } else {
            "${size / (1024 * 1024 * 1024)}GB"
        }
    }
}