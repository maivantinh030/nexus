package com.example.nexus.ui

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.nexus.network.RetrofitClient

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreatePostScreen(navController: NavController?= null,viewModel: TimelineViewModel) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val currentUser = viewModel?.currentUser
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Giới hạn tối đa 5 ảnh để tránh quá tải
            val selectedUris = if (uris.size > 5) uris.take(5) else uris
            imageUris = selectedUris
            Toast.makeText(
                context,
                "Đã chọn ${selectedUris.size} ảnh${if (uris.size > 5) " (tối đa 5 ảnh)" else ""}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Thanh trên cùng với nút quay lại
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
                text = "New Post",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(48.dp)) // Cân đối với IconButton
        }
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
                text = currentUser?.username ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
            )
        }
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Có gì mới?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if(imageUris.isNotEmpty()){
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ){
                items(imageUris) { uri ->
                    Box {
                        Card(
                            modifier = Modifier.size(120.dp)
                        ){
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Nút xóa được đặt ở góc trên bên phải
                        IconButton(
                            onClick = {
                                imageUris = imageUris.filterNot { it == uri }
                                Toast.makeText(context, "Đã xóa ảnh", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove Image",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { /* TODO: Xử lý chọn ảnh */
                galleryLauncher.launch("image/*")}) {
                Icon(
                    painter = painterResource(id = R.drawable.photo_library_24px),
                    contentDescription = "Image",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = if(imageUris.isEmpty()) "Attach images" else "Add more image",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if(content.isNotBlank()){
                    viewModel.addPost(
                        content = content,
                        visibility = "PUBLIC", // Hoặc lấy từ lựa chọn của người dùng
                        parentPostId = null, // Nếu không phải trả lời bài đăng nào
                        imageUris = imageUris // Truyền danh sách ảnh đã chọn
                    )

                    Toast.makeText(context,"Posted: $content",Toast.LENGTH_SHORT).show()
                    content = ""
                    imageUris = emptyList()
                }
                else{
                    Toast.makeText(context,"hay nhap noi dung",Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier
                    .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Post")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun CreatePostScreenPreview() {
//    CreatePostScreen()
//}