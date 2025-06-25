package com.example.nexus.components

import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.nexus.network.RetrofitClient
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostItem(post: Post,
             viewModel: TimelineViewModel,
             activityViewModel: ActivityViewModel? = null,
             navController: NavController? = null) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var triggerAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isLiked by remember { mutableStateOf(post.isLiked) }


    LaunchedEffect(Unit) {
        scope.launch {
            post.isLiked = viewModel.checkLike("POST", post.id ?: 0)
            Log.d("PostItem", "Post ${post.id} liked status: ${post.isLiked}")
            viewModel.fetchLikeCount("POST",post.id?:0)
        }
    }
    // Animation effects
    LaunchedEffect(triggerAnimation) {
        if (triggerAnimation) {
            scale = 1.5f
            delay(150)
            scale = 1f
            triggerAnimation = false
        }
    }

    val likeColor by animateColorAsState(
        targetValue = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300)
    )

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor =Color(0xFFF5F7FA)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Avatar + Username + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(2.dp)
                ) {
                    AsyncImage(
                        model = (RetrofitClient.MEDIA_BASE_URL + post.user?.profilePicture),
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { /* navigation */ }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.user?.username ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.clickable {
                                navController?.navigate("profile/${post.user?.id}")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(post.createdAt),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController?.navigate("post_detail/${post.id}") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Media Section - LazyRow
            post.media?.let { mediaList ->
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(mediaList.sortedBy { it.sortOrder }) { media ->
                        val fullMediaUrl = RetrofitClient.MEDIA_BASE_URL + media.mediaUrl
                        when (media.mediaType) {
                            "IMAGE" -> {
                                AsyncImage(
                                    model = fullMediaUrl,
                                    contentDescription = "Post image",
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(200.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.ic_avatar_placeholder),
                                    placeholder = painterResource(id = R.drawable.ic_avatar_placeholder)
                                )
                            }
                            "VIDEO" -> {
                                VideoPlayer(
                                    videoUrl = fullMediaUrl,
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(200.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = "Unsupported media type: ${media.mediaType}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Like count
            Text(
                text = "${post.likeCount} ${if (post.likeCount == 1L) "like" else "likes"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = {
                    android.util.Log.d("PostItem", "Like button clicked for post ${post.id}")
                    android.util.Log.d("PostItem", "Current like status: ${post.isLiked}")
                    android.util.Log.d("PostItem", "Current user ID: ${viewModel.currentUserId}")
                    if(post.isLiked){
                        scope.launch {
                            android.util.Log.d("PostItem", "Calling unLikePost")
                            viewModel.unLike("POST",post.id?:0)
                        }
                    }
                    else{
                        scope.launch {
                            android.util.Log.d("PostItem", "Calling LikePost")
                            viewModel.LikePost(postId = post.id ?: 0)
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            id = if (post.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                        ),
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier.scale(animatedScale)
                    )
                }

                IconButton(onClick = {
                    navController?.navigate("post_detail/${post.id}")
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_comment),
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    // TODO: Handle share
                }) {
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

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    // Tạo ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()

            // Listener để theo dõi trạng thái phát
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    // Cleanup khi component bị dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .height(200.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true // Hiển thị controls
                    controllerAutoShow = false // Không tự động hiện controls
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay play button khi video không phát
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Play video",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
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

//@RequiresApi(Build.VERSION_CODES.O)
//@Preview(showBackground = true)
//@Composable
//fun PostItemPreview() {
//    val mockPost = Post(
//        id = 1,
//        user = User(id = 1, username = "user1", bio = "Hello!"),
//        content = "This is my first post!"
//    )
//    PostItem(post = mockPost, viewModel = TimelineViewModel())
//}