package com.example.nexus.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.components.PostItem
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.timeline.TimelineViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null,
    navController: NavController? = null
) {
    val viewState by viewModel.postsState

    val follows by viewModel.follows.collectAsState()
    val context = LocalContext.current
    var postContent by remember { mutableStateOf("") }
    val postMap by viewModel.postCache.collectAsState()
    val posts = postMap.values.toList()

    val currentUserId = 1L // Giả sử đây là ID người dùng hiện tại

//    // Sử dụng remember với dependencies để tính toán lại khi posts hoặc follows thay đổi
//    val filteredPosts = remember(posts, follows) {
//        posts.filter { post ->
//            val userId = post.user?.id
//            userId == currentUserId || follows.any { it.first == currentUserId && it.second == userId }
//        }
//    }

    Box(
        modifier = Modifier.fillMaxSize()
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
            // Compose UI để tạo bài đăng mới
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = (RetrofitClient.MEDIA_BASE_URL + viewModel.currentUser?.profilePicture),
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "What's new?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            navController?.navigate("create_post") ?: run {
                                Toast.makeText(context, "Cannot navigate to create post", Toast.LENGTH_SHORT).show()
                            }
                        }
                )
            }
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            when{
                viewState.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.Center)
                    )
                }
                viewState.error != null -> {
                    Text(
                        text = "Error: ${viewState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    // Hiển thị danh sách bài đăng
                    LazyColumn {
                        items(posts) { post ->
                            PostItem(
                                post = post,
                                viewModel = viewModel,
                                navController = navController,
                                activityViewModel = activityViewModel
                            )
                        }
                    }
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimelineScreenPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Preview UI tạo bài đăng
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                    text = "What's new?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview placeholder cho loading state
            Text(
                text = "Timeline posts will appear here...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
