package com.example.nexus.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nexus.R
import com.example.nexus.ui.timeline.TimelineViewModel
import coil.compose.AsyncImage
import com.example.nexus.components.VideoPlayer
import com.example.nexus.network.RetrofitClient

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreatePostScreen(navController: NavController? = null, viewModel: TimelineViewModel) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var mediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val currentUser = viewModel?.currentUser
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()

            // Xử lý multiple selection
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri ->
                        uris.add(uri)
                    }
                }
            } ?: data?.data?.let { uri ->
                // Single selection
                uris.add(uri)
            }

            if (uris.isNotEmpty()) {
                // Giới hạn tối đa 10 files
                val selectedUris = if (uris.size > 10) uris.take(10) else uris
                mediaUris = selectedUris

                val imageCount = selectedUris.count { uri ->
                    context.contentResolver.getType(uri)?.startsWith("image") == true
                }
                val videoCount = selectedUris.count { uri ->
                    context.contentResolver.getType(uri)?.startsWith("video") == true
                }

                Toast.makeText(
                    context,
                    "Đã chọn $imageCount ảnh và $videoCount video",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Function để mở media picker
    fun openMediaPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // Chấp nhận tất cả file types
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) // Chỉ ảnh và video
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Cho phép chọn nhiều
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val chooser = Intent.createChooser(intent, "Chọn ảnh hoặc video")
        mediaLauncher.launch(chooser)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
                )
            )
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header (giữ nguyên như cũ)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController?.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Bài viết mới",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // User info (giữ nguyên như cũ)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                AsyncImage(
                    model = (RetrofitClient.MEDIA_BASE_URL + currentUser?.profilePicture),
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = currentUser?.fullName?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )
            }

            // Content input (giữ nguyên như cũ)
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Có gì mới?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Color(0xFF7BB3D3),
                    unfocusedBorderColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Media preview (giữ nguyên logic hiển thị như cũ)
            if (mediaUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(mediaUris) { uri ->
                        Box {
                            Card(modifier = Modifier.size(120.dp)) {
                                val mimeType = context.contentResolver.getType(uri) ?: ""

                                when {
                                    mimeType.startsWith("image") -> {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(MaterialTheme.shapes.medium),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    mimeType.startsWith("video") -> {
                                        Box {
                                            VideoPlayer(
                                                videoUrl = uri.toString(),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(MaterialTheme.shapes.medium)
                                            )

                                            // Video overlay
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.5f),
                                                        CircleShape
                                                    )
                                                    .padding(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Remove button
                            IconButton(
                                onClick = {
                                    mediaUris = mediaUris.filterNot { it == uri }
                                    Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Media picker button - CHỈ 1 BUTTON DUY NHẤT
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(onClick = { openMediaPicker() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.photo_library_24px),
                        contentDescription = "Add Media",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (mediaUris.isEmpty()) "Chọn ảnh hoặc video" else "Thêm ảnh/video",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Media count display
            if (mediaUris.isNotEmpty()) {
                val imageCount = mediaUris.count { uri ->
                    context.contentResolver.getType(uri)?.startsWith("image") == true
                }
                val videoCount = mediaUris.count { uri ->
                    context.contentResolver.getType(uri)?.startsWith("video") == true
                }

                Text(
                    text = "Đã chọn: $imageCount ảnh, $videoCount video (${mediaUris.size} tổng cộng)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post button (giữ nguyên như cũ)
            Button(
                onClick = {
                    if (content.isNotBlank() || mediaUris.isNotEmpty()) {
                        viewModel.addPost(
                            content = content,
                            visibility = "PUBLIC",
                            parentPostId = null,
                            mediaUris = mediaUris // Gửi tất cả media URIs
                        )

                        Toast.makeText(context, "Đã đăng bài!", Toast.LENGTH_SHORT).show()
                        content = ""
                        mediaUris = emptyList()
                    } else {
                        Toast.makeText(context, "Hãy nhập nội dung hoặc chọn media", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Đăng bài")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun CreatePostScreenPreview() {
//    CreatePostScreen()
//}