package com.vatsalp.transitlens.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

internal object ImageUtils {

    /** Memory-map a .tflite model from assets (requires noCompress "tflite"). */
    fun loadModelFile(context: Context, assetPath: String): MappedByteBuffer {
        val fd = context.assets.openFd(assetPath)
        FileInputStream(fd.fileDescriptor).use { input ->
            return input.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Resize [bitmap] to [size]x[size] and pack RGB into a direct float32 buffer.
     * @param normalize01 true -> divide by 255 (YOLO detector); false -> raw 0..255
     *   (MobileNetV3 rescales internally via include_preprocessing).
     */
    fun toFloatBuffer(bitmap: Bitmap, size: Int, normalize01: Boolean): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val buffer = ByteBuffer.allocateDirect(size * size * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        for (pixel in pixels) {
            val r = pixel shr 16 and 0xFF
            val g = pixel shr 8 and 0xFF
            val b = pixel and 0xFF
            if (normalize01) {
                buffer.putFloat(r / 255f)
                buffer.putFloat(g / 255f)
                buffer.putFloat(b / 255f)
            } else {
                buffer.putFloat(r.toFloat())
                buffer.putFloat(g.toFloat())
                buffer.putFloat(b.toFloat())
            }
        }
        buffer.rewind()
        return buffer
    }
}
