import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.timeline.TimelineViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

fun parseTimeStringToTimestamp(timeString: String): Long {
    return try {
        // Kiểm tra định dạng của chuỗi thời gian
        val format = if (timeString.contains(".")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        }

        // Chuyển đổi chuỗi thành đối tượng Date và lấy timestamp
        format.parse(timeString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        // Trong trường hợp lỗi, trả về thời gian hiện tại
        System.currentTimeMillis()
    }
}
/**
 * Hàm tiện ích để tính thời gian tương đối
 * Ví dụ: "2 phút trước", "3 giờ trước", "5 ngày trước"
 */
fun getRelativeTimeSpan(timeString: String): String {
    val timestamp = parseTimeStringToTimestamp(timeString)
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "vừa xong"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes phút trước"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours giờ trước"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days ngày trước"
        }
        diff < TimeUnit.DAYS.toMillis(30) -> {
            val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
            "$weeks tuần trước"
        }
        else -> {
            val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
            if (months < 12) {
                "$months tháng trước"
            } else {
                val years = months / 12
                "$years năm trước"
            }
        }
    }
}
@Composable
fun ActivityScreen(
    activityViewModel: ActivityViewModel,
    timelineViewModel: TimelineViewModel,
    navController: NavController? = null
) {
    val notifications by activityViewModel.notifications.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (notifications.isEmpty()) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        timelineViewModel = timelineViewModel,
                        activityViewModel = activityViewModel,
                        navController = navController,
                        context = context
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: ActivityViewModel.Notification,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel,
    navController: NavController?,
    context: android.content.Context
) {
    // Collect posts state
    val posts by timelineViewModel.posts.collectAsState()

    // Tìm thông tin người thực hiện hành động (actor)
    val actor = remember(posts) {
        posts
            .mapNotNull { it.user }
            .distinctBy { it.id }
            .find { it.id == notification.actor_id }
    }

    // Tìm thông tin bài đăng (nếu có)
    val post = remember(posts) {
        posts.find { it.id == notification.target_id }
    }

    // Quyết định icon và màu sắc dựa trên loại thông báo
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

    // Tính thời gian tương đối (truyền timestamp từ notification vào đây)
    val relativeTime = remember {
        // Giả sử notification.timestamp là thời gian tính bằng milliseconds
        getRelativeTimeSpan(notification.created_at)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Đánh dấu thông báo là đã đọc
                activityViewModel.markAsRead(notification.notification_id)

                // Điều hướng dựa trên loại thông báo
                when (notification.type) {
                    "LIKE_POST", "COMMENT" -> {
                        if (notification.target_id != null) {
                            navController?.navigate("post_detail/${notification.target_id}") ?: run {
                                Toast.makeText(context, "Cannot navigate to post details", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    "FOLLOW" -> {
                        if (notification.actor_id != null) {
                            navController?.navigate("profile/${notification.actor_id}") ?: run {
                                Toast.makeText(context, "Cannot navigate to profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.is_read) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.is_read)
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
            // Icon thông báo
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
                    tint = when(notification.type) {
                        "LIKE_POST", "LIKE_COMMENT" -> MaterialTheme.colorScheme.primary
                        "FOLLOW" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nội dung thông báo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Avatar người gửi thông báo và tên
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (actor != null) {
                        AsyncImage(
                            model = actor.profile_picture ?: R.drawable.ic_avatar_placeholder,
                            contentDescription = "User avatar",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_avatar_placeholder),
                            placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.ic_avatar_placeholder)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = actor?.username ?: "Someone",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Hiển thị thời gian tương đối
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $relativeTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Nội dung thông báo
                Text(
                    text = when (notification.type) {
                        "LIKE_POST" -> "liked your post"
                        "LIKE_COMMENT" -> "liked your comment"
                        "COMMENT" -> "commented on your post"
                        "REPLY_POST" -> "replied to your post"
                        "FOLLOW" -> "followed you"
                        "MENTION_POST" -> "mentioned you in a post"
                        "MENTION_COMMENT" -> "mentioned you in a comment"
                        "REPOST" -> "reposted your post"
                        else -> "sent you a notification"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Nếu có nội dung bài đăng thì hiển thị
                if (post != null && notification.type in listOf("LIKE_POST", "COMMENT", "MENTION_POST", "REPOST")) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${post.content.take(50)}${if (post.content.length > 50) "..." else ""}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Nút Follow Back (nếu là thông báo Follow)
            if (notification.type == "FOLLOW" && !notification.is_read) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val actorId = notification.actor_id
                        if (actorId != null) {
                            timelineViewModel.followUser(actorId, activityViewModel)
                            activityViewModel.markAsRead(notification.notification_id)
                            Toast.makeText(context, "Followed back", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Cannot follow back this user", Toast.LENGTH_SHORT).show()
                        }
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

