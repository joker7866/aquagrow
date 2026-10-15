package com.aquagrow.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Photos live in app-private storage under files/photos. The database stores paths relative
 * to filesDir ("photos/abc.jpg") so a restored backup works on a different device.
 */
@Singleton
class PhotoStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dir: File get() = File(context.filesDir, DIR).apply { mkdirs() }

    fun resolve(relativePath: String): File = File(context.filesDir, relativePath)

    /** A fresh file for CameraX to write into, plus the relative path to store. */
    fun newCaptureFile(): Pair<File, String> {
        val name = "${UUID.randomUUID()}.jpg"
        return File(dir, name) to "$DIR/$name"
    }

    /**
     * Copies a picked image (Photo Picker content Uri) into app storage, downscaled so a
     * 50 MP photo does not eat the user's storage. Returns the relative path.
     */
    suspend fun importFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(uri) ?: error("Could not read image")
        val (file, relative) = newCaptureFile()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        relative
    }

    suspend fun delete(relativePath: String) = withContext(Dispatchers.IO) {
        // Only ever delete inside our own photos folder.
        val file = resolve(relativePath).canonicalFile
        if (file.parentFile == dir.canonicalFile) file.delete()
    }

    fun allFiles(): List<File> = dir.listFiles()?.toList().orEmpty()

    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also applies EXIF rotation.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_EDGE) {
                    val scale = MAX_EDGE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE) sample *= 2
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    }

    companion object {
        const val DIR = "photos"
        private const val MAX_EDGE = 1600
        private const val JPEG_QUALITY = 85
    }
}
