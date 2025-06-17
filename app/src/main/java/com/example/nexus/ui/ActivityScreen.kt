
package com.example.nexus.ui.activity

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.ui.model.Notification
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ActivityScreen(
    activityViewModel: ActivityViewModel,
    timelineViewModel: TimelineViewModel,
    navController: NavController? = null
) {
    val notificationState by activityViewModel.notificationState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Hiển thị lỗi qua Snackbar
    LaunchedEffect(notificationState.error) {
        notificationState.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = timelineViewModel.getAccessToken()
            if (token != null) {
                activityViewModel.fetchNotifications(page = 0) // Lấy thông báo
//                activityViewModel.fetchUnreadNotifications(page = 0) // Lấy thông báo chưa đọc
            } else {
                snackbarHostState.showSnackbar("Vui lòng đăng nhập để xem thông báo")
            }
        }
    }
    // Kích hoạt loadMoreNotifications khi cuộn đến gần cuối
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty() && totalItems > 0) {
                    val lastVisibleItem = visibleItems.last().index
                    if (lastVisibleItem >= totalItems - 3 && !notificationState.loadingMore && !notificationState.last) {
                        scope.launch {
                            val userId = timelineViewModel.currentUserId
                            if (userId != null) {
                                activityViewModel.loadMoreNotifications() // Cập nhật để truyền userId
                            }
                        }
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            notificationState.loading && notificationState.notifications.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )
            }
            notificationState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${notificationState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
//                    Button(onClick = { activityViewModel.fetchNotifications(page = 0) }) {
//                        Text("Try Again")
//                    }
                }
            }
            else -> {
                LazyColumn(state = listState) {
                    items(notificationState.notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            timelineViewModel = timelineViewModel,
                            activityViewModel = activityViewModel,
                            navController = navController,
                            context = context
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (notificationState.loadingMore) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .wrapContentSize(Alignment.Center)
                            )
                        }
                    }
                    if (notificationState.last && notificationState.notifications.isNotEmpty()) {
                        item {
                            Text(
                                text = "Đã hết thông báo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    if (notificationState.notifications.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .wrapContentSize(Alignment.Center)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "No notifications",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No notifications yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationCard(
    notification: Notification,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel,
    navController: NavController?,
    context: android.content.Context
) {
    val scope = rememberCoroutineScope()

    val actorName = notification.actorFullName.ifEmpty { notification.actorUsername }
    val actorProfilePicture = notification.actorProfilePicture

    val (icon, backgroundColor) = when (notification.type) {
        "LIKE_POST", "LIKE_COMMENT" -> Pair(
            Icons.Default.Favorite,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        "COMMENT", "REPLY_POST", "MENTION_POST", "MENTION_COMMENT" -> Pair(
            Icons.Default.Comment,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        )
        "FOLLOW" -> Pair(
            Icons.Default.Person,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
        "REPOST" -> Pair(
            Icons.Default.Refresh,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        )
        else -> Pair(
            Icons.Default.Refresh,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    }

    val relativeTime = remember(notification.createdAt) {
        try {
            val instant = Instant.parse(notification.createdAt)
            val now = Instant.now()
            val diff = now.toEpochMilli() - instant.toEpochMilli()
            when {
                diff < 60_000 -> "vừa xong"
                diff < 3_600_000 -> "${diff / 60_000} phút trước"
                diff < 86_400_000 -> "${diff / 3_600_000} giờ trước"
                diff < 604_800_000 -> "${diff / 86_400_000} ngày trước"
                diff < 2_592_000_000 -> "${diff / 604_800_000} tuần trước"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                }
            }
        } catch (e: Exception) {
            "Unknown time"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                activityViewModel.markAsRead(notification.id) // Đổi từ notificationId thành id
                when (notification.type) {
                    "LIKE_POST", "COMMENT" -> {
                        if (notification.targetId != null) {
                            navController?.navigate("post_detail/${notification.targetId}") ?: run {
                                Toast.makeText(context, "Cannot navigate to post details", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    "FOLLOW" -> {
                        navController?.navigate("profile/${notification.actorId}") ?: run {
                            Toast.makeText(context, "Cannot navigate to profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Notification Type",
                    tint = when (notification.type) {
                        "LIKE_POST", "LIKE_COMMENT" -> MaterialTheme.colorScheme.primary
                        "FOLLOW" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = actorProfilePicture ?: R.drawable.ic_avatar_placeholder,
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = actorName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $relativeTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sử dụng message từ backend thay vì hardcode
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (notification.type == "FOLLOW" && !notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        timelineViewModel.followUser(notification.actorId)
                        activityViewModel.markAsRead(notification.id) // Đổi từ notificationId thành id
                        Toast.makeText(context, "Followed back", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Follow back", fontSize = 12.sp)
                }
            }
        }
    }
}