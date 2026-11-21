package com.example.resqhub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Helpers for report photos: shrinking before upload, and showing pictures on screen. */
object PhotoUtils {

    const val MaxPhotos = 3
    private const val UploadMaxSide = 1280
    private const val UploadQuality = 75

    /** Reads a picked picture, shrinks it (max 1280 px) and returns JPEG bytes. */
    suspend fun compressForUpload(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = decodeUri(context, uri, UploadMaxSide) ?: return@withContext null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, UploadQuality, out)
        bitmap.recycle()
        out.toByteArray()
    }

    /** Decodes a picture so that its longest side is about [maxSide] pixels, and turns it upright. */
    suspend fun decodeUri(context: Context, uri: Uri, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return@withContext null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null

            val rotation = resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            if (rotation == 0f) bitmap
            else Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                Matrix().apply { postRotate(rotation) }, true,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Pictures already downloaded from the database, so scrolling does not download them again. */
    private val storageCache = object : android.util.LruCache<String, Bitmap>(24) {}

    suspend fun loadStoragePhoto(path: String): Bitmap? {
        storageCache.get(path)?.let { return it }
        return try {
            val bytes = Supabase.downloadReportPhoto(path)
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            if (bitmap != null) storageCache.put(path, bitmap)
            bitmap
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

/** A picture from the phone's gallery (before it is sent). */
@Composable
fun UriImage(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) { bitmap = PhotoUtils.decodeUri(context, uri, 400) }

    Box(modifier = modifier.background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Attached photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

/** A picture stored in the database (what the responders see). Tap for the big view. */
@Composable
fun StorageImage(path: String, modifier: Modifier = Modifier, crop: Boolean = true, onClick: (() -> Unit)? = null) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        val loaded = PhotoUtils.loadStoragePhoto(path)
        bitmap = loaded
        failed = loaded == null
    }

    Box(
        modifier = modifier
            .background(Color(0xFFE5E7EB))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        when {
            b != null -> Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Report photo",
                contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            failed -> Text("No photo", fontSize = 10.sp, color = Color(0xFF6B7280))
            else -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

/** Full-screen view of one report photo. Tap anywhere to close. */
@Composable
fun PhotoViewerDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        StorageImage(
            path = path,
            crop = false,
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(4.dp),
        )
    }
}