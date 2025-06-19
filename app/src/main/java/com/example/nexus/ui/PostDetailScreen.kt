package com.example.nexus.ui

import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.components.VideoPlayer
import com.example.nexus.components.formatTimestamp
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.Comment
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var currentPost by remember { mutableStateOf<Post?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var shouldRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(postId,shouldRefresh) {
        try {
            currentPost = viewModel.getPostById(postId)
            if (currentPost != null) {
                viewModel.getCommentsForPost(postId)
            } else {
                error = "Không thể tải bài đăng"
            }
        } catch (e: Exception) {
            error = e.message ?: "Lỗi không xác định"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        navController?.currentBackStackEntryFlow?.collect { backStackEntry ->
            if (backStackEntry.destination.route == "post_detail/$postId") {
                shouldRefresh = !shouldRefresh // Chuyển đổi trạng thái để kích hoạt làm mới
            }
        }
    }

    when {
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }
        error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Lỗi: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    isLoading = true
                    error = null
                    scope.launch {
                        currentPost = viewModel.getPostById(postId)
                        if (currentPost != null) {
                            viewModel.getCommentsForPost(postId)
                        }
                    }
                }) {
                    Text("Thử lại")
                }
            }
        }
        currentPost != null -> {
            PostDetailContent(
                post = currentPost!!,
                viewModel = viewModel,
                activityViewModel = activityViewModel,
                navController = navController
            )
        }
        else -> {
            Text(
                text = "Không tìm thấy bài đăng",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostDetailContent(
    post: Post,
    viewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null,
    navController: NavController? = null
) {
    val viewState by viewModel.commentState
    val context = LocalContext.current
    var commentContent by remember { mutableStateOf("") }
    var replyContent by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<Long?>(null) }
    var replyingToPostId by remember { mutableStateOf<Long?>(null) }
    var replyingToUsername by remember { mutableStateOf<String?>(null) } // Track username being replied to
    var scale by remember { mutableStateOf(1f) }
    var triggerAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Fetch comments when component loads
    LaunchedEffect(post.id) {
        post.id?.let { postId ->
            viewModel.getCommentsForPost(postId)
        }
        post.isLiked = viewModel.checkLike("POST", post.id ?: 0)
        viewModel.fetchLikeCount("POST",post.id?:0)

    }

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
        modifier = Modifier.fillMaxSize()
    ){
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
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
                        text = "Nexus",
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

            items(viewState.comments) { comment ->
                CommentItem(
                    postId = post.id ?: 0,
                    comment = comment,
                    parentCommentId = null,
                    onReplyClick = { commentId, username ->
                        replyingToCommentId = commentId
                        replyingToUsername = username
                    },
                    level = 0,
                    viewModel = viewModel
                )
            }

            // Reply to post section
            if (replyingToPostId != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Reply indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replying to post",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                replyingToPostId = null
                                replyContent = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel reply",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

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
                                    scope.launch {
                                        try {
                                            viewModel.replyPost(replyingToPostId!!, replyContent)
                                            Toast.makeText(context, "Reply added!", Toast.LENGTH_SHORT).show()
                                            replyContent = ""
                                            replyingToPostId = null
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to add reply: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
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

        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Show reply indicator if replying to comment
            if (replyingToCommentId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to ${replyingToUsername ?: "comment"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            replyingToCommentId = null
                            replyingToUsername = null
                            commentContent = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel reply",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentContent,
                    onValueChange = { commentContent = it },
                    label = {
                        Text(
                            if (replyingToCommentId != null) "Replying to ${replyingToUsername ?: "comment"}..."
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
                            scope.launch {
                                try {
                                    if (replyingToCommentId != null) {
                                        viewModel.addComment(post.id ?: 0, commentContent, replyingToCommentId)
                                    } else {
                                        viewModel.addComment(post.id ?: 0, commentContent)
                                    }
                                    Log.d("PostDetailScreen", "Adding comment to post ${post.id} replying to comment $replyingToCommentId")
                                    Toast.makeText(context, "Comment added!", Toast.LENGTH_SHORT).show()
                                    commentContent = ""
                                    replyingToCommentId = null
                                    replyingToUsername = null

                                    // Refresh comments after adding
                                    post.id?.let { postId ->
                                        viewModel.getCommentsForPost(postId)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to add comment: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
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
    var isLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    val scope = rememberCoroutineScope()

    // Đồng bộ state local với post state
    LaunchedEffect(post.isLiked) {
        isLiked = post.isLiked
    }

    LaunchedEffect(triggerAnimation) {
        if (triggerAnimation) {
            scale = 1.5f
            delay(150)
            scale = 1f
            triggerAnimation = false
            // Fetch lại like count sau khi animation xong
            viewModel.fetchLikeCount("POST",post.id ?: 0)
        }
    }

    val likeColor by animateColorAsState(
        targetValue = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
                model = RetrofitClient.MEDIA_BASE_URL + post.user?.profilePicture,
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
                        text = formatTimestamp(post.createdAt),
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

                post.media?.let { mediaList ->
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mediaList) { media ->
                            val fullMediaUrl = RetrofitClient.MEDIA_BASE_URL + media.mediaUrl
                            when (media.mediaType) {
                                "IMAGE" -> {
                                    AsyncImage(
                                        model = fullMediaUrl,
                                        contentDescription = "Post image",
                                        modifier = Modifier
                                            .height(200.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "${post.likeCount} likes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                if (isLiked) {
                                    // Unlike: cập nhật UI trước, sau đó gọi API
                                    isLiked = false
                                    post.isLiked = false
                                    post.likeCount = (post.likeCount?.minus(1))?.coerceAtLeast(0)
                                    viewModel.unLike("POST", post.id ?: 0)
                                } else {
                                    // Like: cập nhật UI trước, sau đó gọi API
                                    isLiked = true
                                    post.isLiked = true
                                    post.likeCount = post.likeCount?.plus(1)
                                    triggerAnimation = true
                                    viewModel.LikePost(postId = post.id ?: 0)
                                }
                            } catch (e: Exception) {
                                // Nếu API call thất bại, revert lại UI state
                                isLiked = !isLiked
                                post.isLiked = !post.isLiked
                                // Revert likeCount
                                if (isLiked) {
                                    post.likeCount = (post.likeCount?.minus(1))?.coerceAtLeast(0)
                                } else {
                                    post.likeCount = post.likeCount?.plus(1)
                                }
                            }
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
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

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CommentItem(
    postId: Long,
    comment: Comment,
    parentCommentId: Long? = null,
    onReplyClick: (Long, String) -> Unit, // Modified to include username
    level: Int = 0,
    viewModel: TimelineViewModel
) {
    var scale by remember(comment.id) { mutableStateOf(1f) }
    var triggerAnimation by remember(comment.id) { mutableStateOf(false) }
    var showReplies by remember { mutableStateOf(false) }
    var isLiked by remember(comment.id) { mutableStateOf(comment.isLiked) }
    val scope = rememberCoroutineScope()
    var countLike by remember { mutableStateOf(comment.likeCount ?: 0) }
    var actor by remember { mutableStateOf<User?>(null) }
    LaunchedEffect(comment.id) {
        scope.launch {
            try {
                // Fetch like status
                val likeStatus = viewModel.checkLike("COMMENT", comment.id ?: 0)
                isLiked = likeStatus
                comment.isLiked = likeStatus
                // Fetch like count
                countLike =viewModel.fetchLikeCount("COMMENT", comment.id ?: 0)
                // Fetch user info
                val userInfo = viewModel.getUserById(comment.userId)
                actor = userInfo

                Log.d("CommentItem", "Comment ${comment.id} - Initial like status: $likeStatus")
            } catch (e: Exception) {
                Log.e("CommentItem", "Error fetching comment data: ${e.message}")
            }
        }
    }

    LaunchedEffect(isLiked){
        isLiked = comment.isLiked
        Log.d("CommentItem", "Comment ${comment.id} - Updated like status: $isLiked")
    }
    LaunchedEffect(triggerAnimation) {
        if (triggerAnimation) {
            scale = 1.5f
            delay(150)
            scale = 1f
            triggerAnimation = false
        }
    }

    val likeColor by animateColorAsState(
        targetValue = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
                model = RetrofitClient.MEDIA_BASE_URL + actor?.profilePicture,
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_avatar_placeholder),
                placeholder = painterResource(R.drawable.ic_avatar_placeholder)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.username,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(comment.createdAt),
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
                                replyToId?.let {
                                    onReplyClick(it, comment.username) // Pass username
                                }
                            }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    if (isLiked) {
                                        isLiked = false
                                        comment.isLiked = false
                                        viewModel.unLike("COMMENT", comment.id ?: 0)
                                        countLike--
                                    } else {
                                        isLiked = true
                                        comment.isLiked = true
                                        triggerAnimation = true
                                        viewModel.likeComment(comment.id)
                                        countLike++
                                    }
                                } catch (e: Exception) {
                                    // Nếu API call thất bại, revert lại UI state
                                    isLiked = !isLiked
                                    comment.isLiked = !comment.isLiked
                                    // Revert likeCount
                                    if (isLiked) {
                                        comment.likeCount = (comment.likeCount - 1).coerceAtLeast(0)
                                    } else {
                                        comment.likeCount += 1
                                    }
                                }
                            }
                        }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                                ),
                                contentDescription = "Like",
                                tint = likeColor,
                                modifier = Modifier.scale(animatedScale)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${countLike}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Render replies recursively
        val replies = comment.replies ?: emptyList()
        if (replies.isNotEmpty()) {
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