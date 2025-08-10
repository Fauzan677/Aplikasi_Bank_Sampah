package com.gemahripah.banksampah.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.*

private const val MAXIMAL_SIZE = 1_000_000 // 1MB

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

@SuppressLint("InternalInsetResource")
fun Context.getStatusBarHeight(): Int {
    val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
}
