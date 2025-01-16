package com.example.offlinefilesapp.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.offlinefilesapp.R
import com.example.offlinefilesapp.data.models.FileEntity
import com.example.offlinefilesapp.viewmodels.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(viewModel: FileViewModel, navController: NavController) {
    val trashedFiles by viewModel.trashedFiles.observeAsState(emptyList()) // Observe trashed files
    val showDialog = remember { mutableStateOf(false) }
    val selectedFile = remember { mutableStateOf<FileEntity?>(null) }

    val context = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(title = { Text("Recycle Bin") })
    }, content = { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (trashedFiles.isEmpty()) {
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
                            "No files found in Trash", style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(trashedFiles) { file ->
                        FileItem(
                            file = file,
                            onFileClick = { navController.navigate("file_preview/${file.id}") },
                            onRestore = {
                                viewModel.restoreFile(file.id)
                            },
                            onDelete = {
                                selectedFile.value = file
                                showDialog.value = true
                            }
                        )
                    }
                }
            }
        }
    })

    if (showDialog.value) {
        DeleteFileDialog(onDismiss = { showDialog.value = false }, onDelete = {
            selectedFile.value?.let { viewModel.deleteFile(it.id, context) }
            showDialog.value = false
        }, true)
    }
}

@Composable
fun FileItem(
    file: FileEntity,
    onFileClick: (FileEntity) -> Unit,
    onRestore: (FileEntity) -> Unit,
    onDelete: (FileEntity) -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onFileClick(file) },
        colors = CardDefaults.cardColors(containerColor =  Color.White),
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
                Image(painter = painterResource(id = R.drawable.delete_24px),
                    contentDescription = "Delete Permanently",
                    modifier = Modifier
                        .clickable { onDelete(file) }
                        .padding(end = 15.dp)
                )
                Image(painter = painterResource(id = R.drawable.restore_from_trash_24px),
                    contentDescription = "Restore",
                    modifier = Modifier.clickable { onRestore(file) })
        }
    }
}
