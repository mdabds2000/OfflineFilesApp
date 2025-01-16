package com.example.offlinefilesapp.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.offlinefilesapp.data.database.AppDatabase
import com.example.offlinefilesapp.data.models.FileEntity
import com.example.offlinefilesapp.viewmodels.FileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteExpiredFilesWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val fileDao = AppDatabase.getInstance(context).fileDao()

    override fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val expirationTime = 15 * 60 * 1000 // (86400000 = 24 hours)

        CoroutineScope(Dispatchers.IO).launch {
            val expiredFiles = fileDao.getTrashedFiles().filter { file ->
                (currentTime - file.dateTrashed!!) > expirationTime
            }

            expiredFiles.forEach { file ->
                if (deleteFileFromLocalStorage(file)) {
                    fileDao.deleteFile(file.id)
                } else {
                    val handler = android.os.Handler(applicationContext.mainLooper)
                    handler.post {
                        Toast.makeText(applicationContext, "Failed to delete file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        notifyRecycleBinUpdate(applicationContext)
        return Result.success()
    }

    private fun deleteFileFromLocalStorage(file: FileEntity): Boolean {
        return try {
            val uri = android.net.Uri.parse(file.path)
            applicationContext.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun notifyRecycleBinUpdate(applicationContext: Context) {
        val intent = Intent("com.example.offlinefilesapp.ACTION_FILE_DELETED")
        applicationContext.sendBroadcast(intent)
    }
}

class DeleteExpiredFilesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
            .create(FileViewModel::class.java)
        viewModel.getTrashedFiles()
    }
}
