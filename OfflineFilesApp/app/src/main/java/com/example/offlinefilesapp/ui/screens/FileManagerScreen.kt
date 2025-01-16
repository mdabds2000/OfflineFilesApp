package com.example.offlinefilesapp.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.offlinefilesapp.R
import com.example.offlinefilesapp.data.models.FileEntity
import com.example.offlinefilesapp.viewmodels.FileViewModel
import java.io.File
import java.io.InputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StateFlowValueCalledInComposition", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FileManagerScreen(
    viewModel: FileViewModel, openFilePicker: () -> Unit, navController: NavController
) {
    val files by viewModel.files.observeAsState(emptyList())
    val searchQuery = remember { mutableStateOf("") }
    val filteredFiles = remember(searchQuery.value, files) {
        files.filter { it.name.contains(searchQuery.value, ignoreCase = true) }
    }
    val showDialog = remember { mutableStateOf(false) }
    val selectedFile = remember { mutableStateOf<FileEntity?>(null) }
    val selectedFiles = remember { mutableStateListOf<FileEntity>() }
    val context = LocalContext.current

    val isMultiSelectEnabled = remember { mutableStateOf(false) }

    val downloadFiles: (List<FileEntity>) -> Unit = { allFiles ->
        allFiles.forEach { file ->
            val uri = Uri.parse(file.path)
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        saveFileToDownloads(context, inputStream, file.name, uri)
                    } else {
                        Toast.makeText(context, "Error reading file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context, "Error downloading file: ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        Toast.makeText(context, "${allFiles.size} files saved to Downloads", Toast.LENGTH_SHORT)
            .show()
        isMultiSelectEnabled.value = false
        selectedFiles.clear()
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text("Offline Files App", style = MaterialTheme.typography.titleMedium)
        }, actions = {
            if (filteredFiles.size > 1) {
                IconButton(onClick = {
                    isMultiSelectEnabled.value = !isMultiSelectEnabled.value
                    selectedFiles.clear()
                }) {
                    Icon(
                        imageVector = if (isMultiSelectEnabled.value) Icons.Default.Close else Icons.Outlined.CheckCircle,
                        contentDescription = if (isMultiSelectEnabled.value) "Cancel Multi-Select" else "Enable Multi-Select"
                    )
                }
            }
            if (filteredFiles.isNotEmpty()) {
                IconButton(
                    onClick = { openFilePicker() }, modifier = Modifier.padding(end = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add File")
                }
            }
            Image(painter = painterResource(id = R.drawable.recycling_24px),
                contentDescription = "Recycle Bin",
                modifier = Modifier
                    .clickable { navController.navigate("recycle_bin") }
                    .padding(end = 10.dp))
        })
    }, bottomBar = {
        if (selectedFiles.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 30.dp)
            ) {
                Button(
                    onClick = { downloadFiles(selectedFiles) }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download Selected Files (${selectedFiles.size})")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        selectedFiles.forEach { viewModel.trashFile(it.id) }
                        selectedFiles.clear()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Selected Files (${selectedFiles.size})", color = Color.White)
                }
            }
        }
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(10.dp)
                .fillMaxSize()
        ) {
            if (filteredFiles.size > 1) {
                SearchBar(query = searchQuery.value,
                    onQueryChange = { query -> searchQuery.value = query })
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.no_data),
                            contentDescription = "No files found",
                            modifier = Modifier.size(100.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "No files found", style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { openFilePicker() },
                            modifier = Modifier
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.large)
                        ) {
                            Text(
                                text = "Add Files",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                FileList(files = filteredFiles,
                    onFileClick = { file ->
                        if (isMultiSelectEnabled.value) {
                            if (selectedFiles.contains(file)) {
                                selectedFiles.remove(file)
                            } else {
                                selectedFiles.add(file)
                            }
                        } else {
                            navController.navigate("file_preview/${file.id}")
                        }
                    },
                    onDelete = { file ->
                        selectedFile.value = file
                        showDialog.value = true
                    },
                    onDownload = { file -> downloadFiles(listOf(file)) },
                    selectedFiles = selectedFiles,
                    isMultiSelectEnabled = isMultiSelectEnabled.value
                )
            }
        }
    }

    if (showDialog.value) {
        DeleteFileDialog(onDismiss = { showDialog.value = false }, onDelete = {
            selectedFile.value?.let { viewModel.trashFile(it.id) }
            showDialog.value = false
        })
    }
}

@Composable
fun FileList(
    files: List<FileEntity>,
    onFileClick: (FileEntity) -> Unit,
    onDelete: (FileEntity) -> Unit,
    onDownload: (FileEntity) -> Unit,
    selectedFiles: List<FileEntity>,
    isMultiSelectEnabled: Boolean
) {
    LazyColumn {
        items(files) { file ->
            FileMediaItem(
                file = file,
                onFileClick = onFileClick,
                onDelete = onDelete,
                onDownload = onDownload,
                isSelected = selectedFiles.contains(file),
                showActions = !isMultiSelectEnabled,
                isMultiSelectEnabled = isMultiSelectEnabled
            )
        }
    }
}

@Composable
fun FileMediaItem(
    file: FileEntity,
    onFileClick: (FileEntity) -> Unit,
    onDelete: (FileEntity) -> Unit,
    onDownload: (FileEntity) -> Unit,
    isSelected: Boolean,
    showActions: Boolean,
    isMultiSelectEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onFileClick(file) },
        colors = CardDefaults.cardColors(containerColor = if (!showActions && isSelected) Color.LightGray else Color.White),
        elevation = CardDefaults.cardElevation()
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    id = when {
                        file.type.startsWith("image") -> R.drawable.baseline_image_24
                        file.type.startsWith("video") -> R.drawable.video_file_24px
                        file.type.startsWith("audio") -> R.drawable.library_music_24px
                        else -> R.drawable.draft_24px
                    }
                ), contentDescription = file.name
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Bold)
                Text("Type: ${file.type}", style = MaterialTheme.typography.bodySmall)
//                Text("Size: ${file.size}", style = MaterialTheme.typography.bodySmall)
            }
            if (isMultiSelectEnabled) {
                IconButton(onClick = { onFileClick(file) }) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Select File"
                    )
                }
            } else if (showActions) {
                Image(painter = painterResource(id = R.drawable.delete_24px),
                    contentDescription = "Recycle Bin",
                    modifier = Modifier
                        .clickable { onDelete(file) }
                        .padding(end = 15.dp)
                )
                Image(painter = painterResource(id = R.drawable.download_24px),
                    contentDescription = "Recycle Bin",
                    modifier = Modifier.clickable { onDownload(file) })
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        placeholder = { Text("Search files...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        singleLine = true
    )
}

fun getMimeTypeFromUri(context: Context, uri: Uri): String? {
    return context.contentResolver.getType(uri)
}

fun saveFileToDownloads(context: Context, inputStream: InputStream, fileName: String, uri: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentResolver = context.contentResolver
        val mimeType = getMimeTypeFromUri(context, uri)
        val resolvedMimeType = mimeType ?: "application/octet-stream"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, resolvedMimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val contentURI =
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        contentURI?.let {
            val outputStream = contentResolver.openOutputStream(it)
            inputStream.copyTo(outputStream!!)
            inputStream.close()
            outputStream.close()
        }
    } else {
        val destinationFile =
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            extension.lowercase(
                Locale.ROOT
            )
        )

        mimeType ?: "application/octet-stream"

        val outputStream = destinationFile.outputStream()
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
    }
}

@Composable
fun DeleteFileDialog(onDismiss: () -> Unit, onDelete: () -> Unit, fromRecycleBin: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delete", fontWeight = FontWeight.Bold) },
        text = { Text(if (fromRecycleBin) "Are you sure you want to delete this file? This action cannot be undone." else "Are you sure you want to move this file to trash?") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Yes", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        },
        containerColor = Color.White
    )
}
