package com.ttjjm.speciesid.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageCompressor {

    fun compress(context: android.content.Context, uri: Uri): ByteArray {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open image")

        inputStream.use { stream ->
            // First pass: decode bounds only to avoid OOM on large images
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, bounds)

            // Reopen — the stream was consumed by bounds decoding
            context.contentResolver.openInputStream(uri)!!.use { realStream ->
                val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 1920)
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                var bitmap = BitmapFactory.decodeStream(realStream, null, decodeOptions)
                    ?: throw IllegalStateException("Cannot decode image")

                // Fine-tune to exact long edge target
                bitmap = scaleToLongEdge(bitmap, 1920)

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                bitmap.recycle()
                return out.toByteArray()
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, target: Int): Int {
        val longEdge = maxOf(width, height)
        var sample = 1
        while (longEdge / sample > target * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleToLongEdge(bitmap: Bitmap, target: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= target) return bitmap
        val scale = target.toFloat() / longEdge
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}