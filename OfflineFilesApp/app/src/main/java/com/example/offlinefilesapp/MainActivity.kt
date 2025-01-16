package com.example.offlinefilesapp

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.offlinefilesapp.data.database.AppDatabase
import com.example.offlinefilesapp.data.models.FileEntity
import com.example.offlinefilesapp.data.repository.FileRepository
import com.example.offlinefilesapp.ui.screens.FileManagerScreen
import com.example.offlinefilesapp.ui.screens.FilePreviewScreen
import com.example.offlinefilesapp.ui.screens.RecycleBinScreen
import com.example.offlinefilesapp.viewmodels.FileViewModel
import com.example.offlinefilesapp.viewmodels.FileViewModelFactory
import com.example.offlinefilesapp.workers.DeleteExpiredFilesWorker
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: FileViewModel

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val uriList = mutableListOf<Uri>()

                // Check if multiple files were selected
                if (data?.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        val uri = data.clipData!!.getItemAt(i).uri
                        uriList.add(uri)
                    }
                } else {
                    // Only a single file selected
                    data?.data?.let { uriList.add(it) }
                }

                // Handle each selected URI
                for (uri in uriList) {
                    handleFileSelection(uri)
                }
            }
        }

    private val recycleBinUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.getTrashedFiles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(
                arrayOf(
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO,
                    READ_MEDIA_AUDIO,
                    READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
        } else {
            requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }

        val fileDao = AppDatabase.getInstance(applicationContext).fileDao()
        val repository = FileRepository(fileDao)
        viewModel =
            ViewModelProvider(this, FileViewModelFactory(repository))[FileViewModel::class.java]

        val workRequest = PeriodicWorkRequestBuilder<DeleteExpiredFilesWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)

        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "file_manager") {
                composable("file_manager") {
                    FileManagerScreen(viewModel = viewModel, openFilePicker = { openFilePicker() }, navController = navController)
                }
                composable("file_preview/{fileId}") { backStackEntry ->
                    val fileId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull()
                    val file = viewModel.files.value?.find { it.id == fileId }
                    file?.let { FilePreviewScreen(file = it, navController = navController) }
                }
                composable("recycle_bin") {
                    viewModel.getTrashedFiles()
                    RecycleBinScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun getFileSize(uri: Uri): Long {
        var size: Long = 0
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    private fun getFilePreviewType(fileType: String?): String {
        return when {
            fileType?.startsWith("image") == true -> "image"
            fileType?.startsWith("video") == true -> "video"
            fileType?.startsWith("audio") == true -> "audio"
            else -> "document"
        }
    }

    private fun saveFileToLocalStorage(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            val directory = File(context.filesDir, "media_files")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, fileName)
            val outputStream: OutputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save file locally", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
            null
        }
    }

    private fun handleFileSelection(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        val fileName = getFileName(uri)
        val fileType = contentResolver.getType(uri)
        val fileSize = getFileSize(uri)
        val filePreviewType = getFilePreviewType(fileType)

        val localPathURI = saveFileToLocalStorage(this, uri, fileName)
        if (localPathURI != null) {
            val fileEntity = FileEntity(
                name = fileName,
                path = localPathURI.toString(),
                type = fileType ?: "unknown",
                size = fileSize,
                dateAdded = System.currentTimeMillis(),
                previewType = filePreviewType
            )
            viewModel.addFile(fileEntity)
            Toast.makeText(this, "File added: $fileName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save file locally", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)  // Enable multiple file selection
        }
        openFileLauncher.launch(intent)
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        openFileLauncher.launch(intent)
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.example.offlinefilesapp.ACTION_FILE_DELETED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recycleBinUpdateReceiver, filter, RECEIVER_EXPORTED)
        }
        else{
            registerReceiver(recycleBinUpdateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(recycleBinUpdateReceiver)
    }
}

