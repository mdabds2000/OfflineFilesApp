package com.example.offlinefilesapp.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.offlinefilesapp.data.models.FileEntity
import com.example.offlinefilesapp.viewmodels.FileViewModel

@Composable
fun FilePreviewScreen(file: FileEntity, navController: NavController) {
    val context = LocalContext.current
    val uri = Uri.parse(file.path)

    if (isUriAccessible(context, uri)) {
        when (file.previewType) {
            "image" -> {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                )
            }

            "video" -> {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem = MediaItem.fromUri(Uri.parse(file.path))
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(factory = { context1 ->
                    PlayerView(context1).apply {
                        player = exoPlayer
                    }
                }, modifier = Modifier
                    .fillMaxSize()
                    .height(300.dp))
            }

            "audio" -> {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem = MediaItem.fromUri(Uri.parse(file.path))
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(factory = { context1 ->
                    PlayerView(context1).apply {
                        player = exoPlayer
                    }
                }, modifier = Modifier.fillMaxSize().height(100.dp))
            }

            else -> {
                OpenWithSystemDefault(uri, context, navController)
            }
        }
    } else {
        Text(text = "File is no longer accessible.")
    }
}

@Composable
fun OpenWithSystemDefault(uri: Uri, context: Context, navController: NavController) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "application/octet-stream")
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    DisposableEffect(context) {
        val activity = context as? Activity
        try {
            activity?.startActivityForResult(intent, 100)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file.", Toast.LENGTH_SHORT).show()
        }
        navController.navigate("file_manager") {
            popUpTo("file_manager") { inclusive = true }
        }
        onDispose {}
    }
}

fun isUriAccessible(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.close()
        true
    } catch (e: Exception) {
        false
    }
}
