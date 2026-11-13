package com.ppnnttt.posesuggestions

import android.content.Context
import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlin.math.roundToInt

/** Decode off the UI thread, respecting orientation and bounding memory for large photos. */
fun loadPhoto(context: Context, uri: Uri, maxEdge: Int = 2560): Bitmap {
    if (Build.VERSION.SDK_INT >= 28) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = minOf(1f, maxEdge.toFloat() / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(maxOf(1, (info.size.width * scale).roundToInt()), maxOf(1, (info.size.height * scale).roundToInt()))
        }
    }
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > maxEdge) inSampleSize *= 2
    }
    val bitmap = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, options) } ?: error("Invalid image")
    val orientation = runCatching { resolver.openInputStream(uri)!!.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) } }.getOrDefault(1)
    val transform = Matrix().apply {
        when (orientation) {
            2 -> postScale(-1f, 1f)
            3 -> postRotate(180f)
            4 -> postScale(1f, -1f)
            5 -> { postRotate(90f); postScale(-1f, 1f) }
            6 -> postRotate(90f)
            7 -> { postRotate(270f); postScale(-1f, 1f) }
            8 -> postRotate(270f)
        }
    }
    val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
    if (result !== bitmap) bitmap.recycle()
    return result
}
