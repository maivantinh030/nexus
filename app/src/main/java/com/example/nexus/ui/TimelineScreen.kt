package com.example.nexus.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nexus.R
import com.example.nexus.components.PostItem
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null,
    navController: NavController? = null
) {
    val posts by viewModel.posts.collectAsState()
    val follows by viewModel.follows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val context = LocalContext.current
    var postContent by remember { mutableStateOf("") }

    val currentUserId = 1L // Giả sử đây là ID người dùng hiện tại

    // Sử dụng remember với dependencies để tính toán lại khi posts hoặc follows thay đổi
    val filteredPosts = remember(posts, follows) {
        posts.filter { post ->
            val userId = post.user?.id
            userId == currentUserId || follows.any { it.first == currentUserId && it.second == userId }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Compose UI để tạo bài đăng mới
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController?.navigate("create_post") ?: run {
                        Toast.makeText(context, "Cannot navigate to create post", Toast.LENGTH_SHORT).show()
                    }
                },
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
        Spacer(modifier = Modifier.height(16.dp))

        // Hiển thị danh sách bài đăng hoặc thông báo tương ứng
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
            )
        } else if (filteredPosts.isEmpty()) {
            Text(
                text = "No posts yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn {
                items(filteredPosts) { post ->
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun TimelineScreenPreview() {
    val mockPosts = listOf(
        Post(
            id = 1,
            user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
            content = "This is my first post!",
            created_at = "2025-05-13T14:55:00Z",
            updated_at = "2025-05-13T14:55:00Z"
        ),
        Post(
            id = 2,
            user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
            content = "ThreadsClone is awesome!",
            created_at = "2025-05-13T14:55:00Z",
            updated_at = "2025-05-13T14:55:00Z"
        ),
        Post(
            id = 3,
            user = User(id = 3, username = "user3", bio = "Just joined!", profile_picture = "https://example.com/avatar3.jpg"),
            content = "Hello everyone!",
            created_at = "2025-05-13T14:55:00Z",
            updated_at = "2025-05-13T14:55:00Z"
        )
    )

    // Tạo TimelineViewModel giả với dữ liệu mẫu
    val viewModel = TimelineViewModel().apply {
        updatePosts(mockPosts)
    }

    // Render TimelineScreen với dữ liệu mẫu
    TimelineScreen(viewModel = viewModel)
}