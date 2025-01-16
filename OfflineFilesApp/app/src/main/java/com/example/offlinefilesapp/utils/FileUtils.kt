package com.example.offlinefilesapp.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream


fun saveFileToDownloads(context: Context, sourceFile: File, fileName: String) {
    // For Android 10 (API Level 29) and above, use the MediaStore API to insert files into Downloads folder.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // File name
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream") // MIME type, adjust as necessary
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Save to Downloads folder
        }

        // Insert the file into the MediaStore
        val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            val inputStream: InputStream = FileInputStream(sourceFile)

            inputStream.copyTo(outputStream!!)

            // Close streams
            inputStream.close()
            outputStream.close()
        }
    } else {
        // For Android below API level 29 (Scoped Storage is not enforced)
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        sourceFile.copyTo(destinationFile, overwrite = true)
    }
}

// Function to save multiple files to the Downloads folder
fun saveMultipleFilesToDownloads(context: Context, files: List<File>) {
    for (file in files) {
        saveFileToDownloads(context, file, file.name)
    }
}