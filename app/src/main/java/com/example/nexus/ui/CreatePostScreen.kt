package com.example.nexus.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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

@Composable
fun CreatePostScreen(navController: NavController?= null,viewModel: TimelineViewModel?=null) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = uri.toString()
            Toast.makeText(context, "Selected image: $uri", Toast.LENGTH_SHORT).show()
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
            Image(
                painter = painterResource(id = R.drawable.ic_avatar_placeholder),
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "user1", // Giả lập username
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
        imageUri?.let { uri->
            AsyncImage(
                model = uri,
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
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
                text = "Attach image",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if(content.isNotBlank()){
                    viewModel?.addPost(content)
                    Toast.makeText(context,"Posted: $content",Toast.LENGTH_SHORT).show()
                    content = ""
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

@Preview(showBackground = true)
@Composable
fun CreatePostScreenPreview() {
    CreatePostScreen()
}