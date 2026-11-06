package com.example.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class ExifInfo(
    val cameraMake: String = "N/D",
    val cameraModel: String = "N/D",
    val dateTime: String = "N/D",
    val software: String = "N/D",
    val exposureTime: String = "N/D",
    val fNumber: String = "N/D",
    val iso: String = "N/D",
    val focalLength: String = "N/D",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasGps: Boolean = false,
    val rawTagCount: Int = 0
)

class ExifManager(private val context: Context) {

    suspend fun extractExif(uri: Uri): Result<ExifInfo> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext Result.failure(Exception("Não foi possível abrir o arquivo."))

            val exif = ExifInterface(inputStream)
            inputStream.close()

            val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/D"
            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/D"
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "N/D"
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE) ?: "N/D"
            val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "N/D"
            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "N/D"
            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "N/D"
            val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "N/D"

            val latLong = FloatArray(2)
            val hasGps = exif.getLatLong(latLong)

            Result.success(
                ExifInfo(
                    cameraMake = make,
                    cameraModel = model,
                    dateTime = dateTime,
                    software = software,
                    exposureTime = exposure,
                    fNumber = fNumber,
                    iso = iso,
                    focalLength = focal,
                    latitude = if (hasGps) latLong[0].toDouble() else null,
                    longitude = if (hasGps) latLong[1].toDouble() else null,
                    hasGps = hasGps,
                    rawTagCount = if (make != "N/D" || dateTime != "N/D" || hasGps) 8 else 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stripMetadata(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Falha ao abrir imagem"))

            val bitmap = inputStream.use { BitmapFactory.decodeStream(it) }
                ?: return@withContext Result.failure(Exception("Não foi possível decodificar os pixels da imagem"))

            val outputDir = File(context.cacheDir, "sanitized_images")
            if (!outputDir.exists()) outputDir.mkdirs()

            val cleanFile = File(outputDir, "openv_clean_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(cleanFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            Result.success(cleanFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
