package com.example.nexus.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.nexus.ui.model.Comment
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostDetailScreen(
    postId: Long,
    viewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null,
    navController: NavController? = null
) {
    val currentPost by viewModel.currentPost.collectAsState()
    LaunchedEffect(postId) {
        viewModel.getPostById(postId)
    }

    currentPost?.let { post ->
        PostDetailContent(post, viewModel, activityViewModel, navController)
    } ?: run {
        Text(
            text = "Loading post...",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostDetailContent(
    post: Post,
    viewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null,
    navController: NavController? = null
) {
    val context = LocalContext.current
    var commentContent by remember { mutableStateOf("") }
    var replyContent by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<Long?>(null) }
    var replyingToPostId by remember { mutableStateOf<Long?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var triggerAnimation by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "Thread",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            PostDetailItem(
                post = post,
                onReplyClick = { parentId -> replyingToPostId = parentId },
                level = 0,
                viewModel = viewModel,
                activityViewModel = activityViewModel
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (post.replies.isNotEmpty()) {
            items(post.replies) { reply ->
                PostDetailItem(
                    post = reply,
                    parentPostId = post.id,
                    onReplyClick = { parentId -> replyingToPostId = parentId },
                    level = 1,
                    viewModel = viewModel,
                    activityViewModel = activityViewModel
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

        }

        items(post.comments) { comment ->
            CommentItem(
                postId = post.id ?: 0,
                comment = comment,
                parentCommentId = null,
                onReplyClick = { parentId -> replyingToCommentId = parentId },
                level = 0,
                viewModel = viewModel
            )
        }

        if (replyingToPostId != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyContent,
                        onValueChange = { replyContent = it },
                        label = { Text("Reply to post...") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (replyContent.isNotBlank()) {
                                viewModel.replyPost(replyingToPostId!!, replyContent)
                                Toast.makeText(context, "Reply added!", Toast.LENGTH_SHORT).show()
                                replyContent = ""
                                replyingToPostId = null
                            } else {
                                Toast.makeText(context, "Please enter a reply", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Post")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentContent,
                    onValueChange = { commentContent = it },
                    label = {
                        Text(
                            if (replyingToCommentId != null) "Replying to comment..."
                            else "Add a comment..."
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (commentContent.isNotBlank()) {
                            if (replyingToCommentId != null) {
                                viewModel.addReply(post.id ?: 0, replyingToCommentId!!, commentContent)
                            } else {
                                viewModel.addComment(post.id ?: 0, commentContent)
                            }
                            Toast.makeText(context, "Comment added!", Toast.LENGTH_SHORT).show()
                            commentContent = ""
                            replyingToCommentId = null
                        } else {
                            Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Post")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostDetailItem(
    post: Post,
    parentPostId: Long? = null,
    onReplyClick: (Long) -> Unit,
    level: Int = 0,
    viewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null
) {
    var scale by remember(post.id) { mutableStateOf(1f) }
    var triggerAnimation by remember(post.id) { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = post.user?.profile_picture ?: R.drawable.ic_avatar_placeholder,
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                error = painterResource(R.drawable.ic_avatar_placeholder),
                placeholder = painterResource(R.drawable.ic_avatar_placeholder)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.user?.username ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(post.created_at),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
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

                Text(
                    text = "${post.like_count} likes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = {
                        triggerAnimation = true
                        viewModel.toggleLike(post.id ?: 0, activityViewModel)
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
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_comment),
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        viewModel.repost(post)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val replyToId = if (level == 0) post.id else parentPostId
                        replyToId?.let { onReplyClick(it) }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_comment),
                            contentDescription = "Reply",
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
fun CommentItem(
    postId: Long,
    comment: Comment,
    parentCommentId: Long? = null,
    onReplyClick: (Long) -> Unit,
    level: Int = 0,
    viewModel: TimelineViewModel
) {
    var scale by remember(comment.id) { mutableStateOf(1f) }
    var triggerAnimation by remember(comment.id) { mutableStateOf(false) }

    LaunchedEffect(triggerAnimation) {
        if (triggerAnimation) {
            scale = 1.5f
            delay(150)
            scale = 1f
            triggerAnimation = false
        }
    }

    val likeColor by animateColorAsState(
        targetValue = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300)
    )

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 300)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = comment.user?.profile_picture ?: R.drawable.ic_avatar_placeholder,
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                error = painterResource(R.drawable.ic_avatar_placeholder),
                placeholder = painterResource(R.drawable.ic_avatar_placeholder)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user?.username ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(comment.created_at),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable {
                                val replyToId = if (level == 0) comment.id else parentCommentId
                                replyToId?.let { onReplyClick(it) }
                            }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            triggerAnimation = true
                            comment.id?.let {
                                viewModel.toggleCommentLike(
                                    postId = postId,
                                    commentId = it,
                                    isReply = level > 0
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (comment.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                            ),
                            contentDescription = "Like Comment",
                            tint = likeColor,
                            modifier = Modifier
                                .size(16.dp)
                                .scale(animatedScale)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.like_count}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            comment.replies.forEach { reply ->
                CommentItem(
                    postId = postId,
                    comment = reply,
                    parentCommentId = comment.id,
                    onReplyClick = onReplyClick,
                    level = level + 1,
                    viewModel = viewModel
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PostDetailScreenPreview() {
    val mockPost = Post(
        id = 1,
        user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
        content = "This is my first post!",
        created_at = "2025-05-13T14:55:00Z",
        updated_at = "2025-05-13T14:55:00Z",
        isLiked = false,
        like_count = 0,
        imageUri = "https://example.com/image.jpg",
        replies = listOf(
            Post(
                id = 4,
                user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
                parent_post_id = 1,
                content = "Nice post!",
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            ),
            Post(
                id = 5,
                user = User(id = 3, username = "user3", bio = "Just joined!", profile_picture = "https://example.com/avatar3.jpg"),
                parent_post_id = 1,
                content = "I agree!",
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            )
        ),
        comments = listOf(
            Comment(
                id = 1,
                user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
                content = "Great post!",
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z",
                replies = listOf(
                    Comment(
                        id = 2,
                        user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
                        content = "Thanks!",
                        created_at = "2025-05-13T14:55:00Z",
                        updated_at = "2025-05-13T14:55:00Z"
                    ),
                    Comment(
                        id = 3,
                        user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
                        content = "You're welcome!",
                        created_at = "2025-05-13T14:55:00Z",
                        updated_at = "2025-05-13T14:55:00Z"
                    )
                )
            )
        )
    )
    PostDetailContent(post = mockPost, viewModel = TimelineViewModel())
}