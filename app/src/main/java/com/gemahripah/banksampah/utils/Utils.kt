package com.gemahripah.banksampah.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.*

private const val MAXIMAL_SIZE = 1_000_000 // 1MB

fun uriToFile(uri: Uri, context: Context): File {
    val contentResolver = context.contentResolver
    val file = File.createTempFile("temp_image", ".jpg", context.cacheDir)

    val inputStream = contentResolver.openInputStream(uri) ?: return file
    val outputStream = FileOutputStream(file)

    val buffer = ByteArray(1024)
    var length: Int
    while (inputStream.read(buffer).also { length = it } > 0) {
        outputStream.write(buffer, 0, length)
    }

    outputStream.flush()
    outputStream.close()
    inputStream.close()

    return file
}

fun Bitmap.getRotatedBitmap(file: File): Bitmap {
    val exif = ExifInterface(file)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    val rotationAngle = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    return if (rotationAngle != 0f) {
        val matrix = Matrix()
        matrix.postRotate(rotationAngle)
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    } else {
        this
    }
}

fun File.reduceFileImage(): File {
    val bitmap = BitmapFactory.decodeFile(this.path)?.getRotatedBitmap(this)
    var compressQuality = 100
    var streamLength: Int

    do {
        val bmpStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
        val bmpPicByteArray = bmpStream.toByteArray()
        streamLength = bmpPicByteArray.size
        compressQuality -= 5
    } while (streamLength > MAXIMAL_SIZE && compressQuality > 5)

    bitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(this))
    return this
}