package com.example.nexus.components

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostItem(post: Post,
             viewModel: TimelineViewModel,
             activityViewModel: ActivityViewModel? = null,
             navController: NavController? =null) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var triggerAnimation by remember { mutableStateOf(false) }
    // Khi triggerAnimation thay đổi, LaunchedEffect sẽ được kích hoạt
    LaunchedEffect(triggerAnimation) {
        if (triggerAnimation) {
            scale = 1.5f
            delay(150)
            scale = 1f
            triggerAnimation = false
        }
    }

    // Hiệu ứng đổi màu
    val likeColor by animateColorAsState(
        targetValue = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300)
    )

    // Hiệu ứng phóng to/thu nhỏ
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 300)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ){
            Image(
                painter = painterResource(id = R.drawable.ic_avatar_placeholder),
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable{
                        post.user?.id?.let {id->
                            navController?.navigate("profile/$id")?:run {
                                Toast.makeText(context,"Cannot navigate to profile",Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier
                .weight(1f)
                .clickable{navController?.navigate("post_detail/${post.id}")}
            ) {
                Row(verticalAlignment = Alignment.CenterVertically){

                    Text(
                        text = post.user?.username?:"Unknown",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.clickable{
                            navController?.navigate("profile/${post.user?.id}")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(post.created_at),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                post.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Post image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Số lượt thích
                Text(
                    text = "${post.like_count} likes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)){
                    IconButton(onClick = {
                        triggerAnimation = true
                        viewModel.toggleLike(post.id ?: 0,activityViewModel)
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if(post.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                            contentDescription = "Like",
                            tint = likeColor,
                            modifier = Modifier.scale(animatedScale)
                        )
                    }
                    IconButton(onClick = {navController?.navigate("post_detail/${post.id}") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_comment),
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {/* TODO: Xu li like*/
                        viewModel.repost(post)
                    })
                    {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                }
            }

        }

    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun formatTimestamp(timestamp: String?): String {
    if (timestamp == null) return ""
    return try {
        val instant = Instant.parse(timestamp)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PostItemPreview() {
    val mockPost = Post(
        id = 1,
        user = User(id = 1, username = "user1", bio = "Hello!"),
        content = "This is my first post!"
    )
    PostItem(post = mockPost, viewModel = TimelineViewModel())
}