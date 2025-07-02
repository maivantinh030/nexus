@file:kotlin.OptIn(ExperimentalFoundationApi::class)

package com.example.nexus.components

import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.nexus.ui.model.Media
import com.example.nexus.ui.model.ReportReason
import kotlinx.coroutines.launch


@ExperimentalMaterial3Api
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostItem(
    post: Post,
    viewModel: TimelineViewModel,
    navController: NavController? = null
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var triggerAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var originalPost by remember { mutableStateOf<Post?>(null) }

    // Check if this is a shared post and load original post
    LaunchedEffect(post.parentPostId) {
        if (post.isSharedPost()) {
            post.parentPostId?.let { originalPostId ->
                originalPost = viewModel.getPostById(originalPostId)
            }
        }
    }

    // States for current post (share post)
    var postIsLiked by remember { mutableStateOf(post.isLiked) }
    var postLikeCount by remember { mutableStateOf(post.likeCount ?: 0L) }

    // States for original post (if this is a shared post)
    var originalIsLiked by remember { mutableStateOf(originalPost?.isLiked ?: false) }
    var originalLikeCount by remember { mutableStateOf(originalPost?.likeCount ?: 0L) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Load interactions for current post
    LaunchedEffect(Unit) {
        scope.launch {
            post.isLiked = viewModel.checkLike("POST", post.id ?: 0)
            postIsLiked = post.isLiked
            viewModel.fetchLikeCount("POST", post.id ?: 0)
            postLikeCount = post.likeCount ?: 0L
        }
    }

    // Load interactions for original post (if exists)
    LaunchedEffect(originalPost?.id) {
        originalPost?.let { original ->
            scope.launch {
                original.isLiked = viewModel.checkLike("POST", original.id ?: 0)
                originalIsLiked = original.isLiked
                viewModel.fetchLikeCount("POST", original.id ?: 0)
                originalLikeCount = original.likeCount ?: 0L
            }
        }
    }

    // Sync states when data changes
    LaunchedEffect(post.isLiked, post.likeCount) {
        postIsLiked = post.isLiked
        postLikeCount = post.likeCount ?: 0L
    }

    LaunchedEffect(originalPost?.isLiked, originalPost?.likeCount) {
        originalIsLiked = originalPost?.isLiked ?: false
        originalLikeCount = originalPost?.likeCount ?: 0L
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
        targetValue = if (postIsLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300)
    )

    val originalLikeColor by animateColorAsState(
        targetValue = if (originalIsLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
            containerColor = Color(0xFFF5F7FA)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main Post Header: Avatar + Username + Time
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
                            .clickable {
                                navController?.navigate("profile/${post.user?.id}")
                            }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.user?.fullName ?: "Unknown",
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
                IconButton(
                    onClick = { showMoreMenu = true }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Post Content (share comment)
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController?.navigate("post_detail/${post.id}") }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Original Post (if this is a shared post) - INDENTED
            if (post.isSharedPost() && originalPost != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp), // INDENT sang phải
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Original Post Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    model = (RetrofitClient.MEDIA_BASE_URL + originalPost!!.user?.profilePicture),
                                    contentDescription = "Original user avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .clickable {
                                            navController?.navigate("profile/${originalPost!!.user?.id}")
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = originalPost!!.user?.fullName ?: "Unknown",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier.clickable {
                                            navController?.navigate("profile/${originalPost!!.user?.id}")
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTimestamp(originalPost!!.createdAt),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Original Post Content
                        Text(
                            text = originalPost!!.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController?.navigate("post_detail/${originalPost!!.id}") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Original Post Media
                        originalPost!!.media?.let { mediaList ->
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
                                                contentDescription = "Original post image",
                                                modifier = Modifier
                                                    .width(240.dp) // Smaller than main post
                                                    .height(160.dp)
                                                    .clip(MaterialTheme.shapes.medium)
                                                    .clickable {
                                                        selectedMediaIndex = mediaList.indexOf(media)
                                                        showMediaViewer = true
                                                    },
                                                contentScale = ContentScale.Crop,
                                                error = painterResource(id = R.drawable.ic_avatar_placeholder),
                                                placeholder = painterResource(id = R.drawable.ic_avatar_placeholder)
                                            )
                                        }
                                        "VIDEO" -> {
                                            VideoPlayer(
                                                videoUrl = fullMediaUrl,
                                                modifier = Modifier
                                                    .width(240.dp)
                                                    .height(160.dp),
                                                onFullscreenClick = {
                                                    selectedMediaIndex = mediaList.indexOf(media)
                                                    showMediaViewer = true
                                                }
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "Unsupported media type: ${media.mediaType}",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Original Post Like count
                        Text(
                            text = "$originalLikeCount  lượt thích",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Original Post Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(onClick = {
                                originalPost!!.let { original ->
                                    if (originalIsLiked) {
                                        // Optimistic update
                                        originalIsLiked = false
                                        originalLikeCount = maxOf(0, originalLikeCount - 1)
                                        scope.launch {
                                            viewModel.unLike("POST", original.id ?: 0)
                                        }
                                    } else {
                                        // Optimistic update
                                        originalIsLiked = true
                                        originalLikeCount = originalLikeCount + 1
                                        scope.launch {
                                            viewModel.LikePost(postId = original.id ?: 0)
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (originalIsLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                                    ),
                                    contentDescription = "Like original",
                                    tint = originalLikeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = {
                                navController?.navigate("post_detail/${originalPost!!.id}")
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_comment),
                                    contentDescription = "Comment original",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = {
                                showShareDialog = true
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share),
                                    contentDescription = "Share original",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Regular post media (if not a shared post)
            if (!post.isSharedPost()) {
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
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable {
                                                selectedMediaIndex = mediaList.indexOf(media)
                                                showMediaViewer = true
                                            },
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
                                            .height(200.dp),
                                        onFullscreenClick = {
                                            selectedMediaIndex = mediaList.indexOf(media)
                                            showMediaViewer = true
                                        }
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
            }

            // Main Post Like count
            Text(
                text = "$postLikeCount lượt thích",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Main Post Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = {
                    triggerAnimation = true
                    if (postIsLiked) {
                        // Optimistic update
                        postIsLiked = false
                        postLikeCount = maxOf(0, postLikeCount - 1)
                        scope.launch {
                            viewModel.unLike("POST", post.id ?: 0)
                        }
                    } else {
                        // Optimistic update
                        postIsLiked = true
                        postLikeCount = postLikeCount + 1
                        scope.launch {
                            viewModel.LikePost(postId = post.id ?: 0)
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            id = if (postIsLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
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
                    showShareDialog = true
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

    // Share dialog
    if (showShareDialog) {
        ShareDialog(
            originalPost = if (post.isSharedPost()) originalPost!! else post,
            currentUserAvatar = viewModel.currentUser?.profilePicture,
            onDismiss = { showShareDialog = false },
            onShare = { content ->
                scope.launch {
                    val targetPostId = if (post.isSharedPost()) originalPost!!.id else post.id
                    viewModel.sharePost(
                        originalPostId = targetPostId ?: 0,
                        shareContent = content
                    )
                }
                showShareDialog = false
            }
        )
    }

    if (showMediaViewer) {
        val mediaToShow = if (post.isSharedPost()) originalPost?.media else post.media
        mediaToShow?.let { mediaList ->
            MediaViewerDialog(
                mediaList = mediaList.sortedBy { it.sortOrder },
                initialIndex = selectedMediaIndex,
                onDismiss = { showMediaViewer = false }
            )
        }
    }

    if (showMoreMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenu = false }
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Option Report
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMoreMenu = false
                            showReportDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_report),
                        contentDescription = "Report",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Báo cáo bài viết",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
    if (showReportDialog) {
        ReportDialog(
            post = post,
            onDismiss = { showReportDialog = false },
            onReport = { reason, description ->
                scope.launch {
                    val success = viewModel.reportPost(
                        postId = post.id ?: 0,
                        reason = reason,
                        description = description
                    )

                    if (success) {
                        // Hiển thị thông báo thành công
                        // Có thể dùng Snackbar hoặc Toast
                    } else {
                        // Hiển thị thông báo lỗi
                    }
                }
                showReportDialog = false
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onFullscreenClick: (() -> Unit)? = null // Thêm callback cho fullscreen
) {
    val context = LocalContext.current

    // Tạo ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
        }
    }

    // Cleanup khi component bị dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.height(200.dp)) {
        // ExoPlayer với controller
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 3000
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
        )

        // Custom fullscreen button overlay
        if (onFullscreenClick != null) {
            IconButton(
                onClick = onFullscreenClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fullscreen),
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
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

@Composable
fun MediaViewerDialog(
    mediaList: List<Media>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaList.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Media pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val media = mediaList[page]
                val fullMediaUrl = RetrofitClient.MEDIA_BASE_URL + media.mediaUrl

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (media.mediaType) {
                        "IMAGE" -> {
                            ZoomableImage(
                                imageUrl = fullMediaUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "VIDEO" -> {
                            FullScreenVideoPlayer(
                                videoUrl = fullMediaUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Page indicator
            if (mediaList.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(mediaList.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // Media counter
            Text(
                text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)

        // Chỉ cho phép di chuyển khi đã zoom
        if (newScale > 1f) {
            // Tính toán giới hạn di chuyển dựa trên mức zoom
            val maxOffsetX = (newScale - 1f) * 200f
            val maxOffsetY = (newScale - 1f) * 300f

            offsetX = (offsetX + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = (offsetY + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            // Reset offset khi zoom về 1x
            offsetX = 0f
            offsetY = 0f
        }

        scale = newScale
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = "Zoomable image",
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            // CHỈ áp dụng transformable khi đã zoom > 1f
            .then(
                if (scale > 1f) {
                    Modifier.transformable(state = transformableState)
                } else {
                    Modifier
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            // Reset về trạng thái ban đầu
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Zoom vào 2x tại vị trí tap
                            scale = 2f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                )
            },
        contentScale = ContentScale.Fit,
        error = painterResource(id = R.drawable.ic_avatar_placeholder),
        placeholder = painterResource(id = R.drawable.ic_avatar_placeholder)
    )
}

@OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
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
                    useController = true
                    controllerAutoShow = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ReportDialog(
    post: Post,
    onDismiss: () -> Unit,
    onReport: (ReportReason, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var description by remember { mutableStateOf("") }

    // Danh sách các lý do báo cáo với label tiếng Việt
    val reasons = listOf(
        ReportReason.SPAM to "Spam",
        ReportReason.HARASSMENT to "Quấy rối",
        ReportReason.HATE_SPEECH to "Ngôn từ thù địch",
        ReportReason.INAPPROPRIATE_CONTENT to "Nội dung không phù hợp",
        ReportReason.FALSE_INFORMATION to "Thông tin sai lệch",
        ReportReason.VIOLENCE to "Bạo lực",
        ReportReason.SEXUAL_CONTENT to "Nội dung tình dục",
        ReportReason.COPYRIGHT_VIOLATION to "Vi phạm bản quyền",
        ReportReason.OTHER to "Khác"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Báo cáo bài viết",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Vui lòng chọn lý do báo cáo:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp), // Giới hạn chiều cao
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(reasons) { (reason, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            selectedReason?.let { reason ->
                                onReport(reason, description)
                            }
                        },
                        enabled = selectedReason != null
                    ) {
                        Text("Báo cáo")
                    }
                }
            }
        }
    }
}