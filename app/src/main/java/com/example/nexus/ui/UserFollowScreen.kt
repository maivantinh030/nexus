package com.example.nexus.ui

import android.widget.Toast
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.play.core.integrity.i
import kotlinx.coroutines.launch
import kotlin.collections.contains

@Composable
fun UserFollowScreen(
    userId: Long,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel,
    navController: NavController,
    selectedTab: Int = 0
){
    val scope = rememberCoroutineScope()
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var following by remember { mutableStateOf<List<User>>(emptyList()) }
    var user: User? by remember { mutableStateOf(null) }

    LaunchedEffect(userId) {
        scope.launch {
            followers =  timelineViewModel.getFollowers(userId)
            following = timelineViewModel.getFollowing(userId)
            user = timelineViewModel.getUserById(userId = userId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()
    ) {
        val tabTitles = listOf("Người theo dõi","Đang theo dõi",)
        var selectedTabIndex by remember { mutableStateOf(selectedTab) }
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index ,title ->
                Tab(selected = selectedTabIndex ==index,
                    onClick = {
                            selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        when(selectedTabIndex){
            0-> FollowersTabContent(
                users = followers,
                timelineViewModel = timelineViewModel,
                activityViewModel = activityViewModel,
                navController = navController
            )
            1-> FollowingTabContent(
                users = following,
                timelineViewModel = timelineViewModel,
                activityViewModel = activityViewModel,
                navController = navController
            )
        }
    }
}

@Composable
fun FollowersTabContent(
    users: List<User>,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel,
    navController: NavController
){

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        items(users){user ->
            UserItem(
                user = user,
                timelineViewModel = timelineViewModel,
                activityViewModel = activityViewModel,
                navController = navController
            )
        }
    }
}
@Composable
fun FollowingTabContent(
    users: List<User>,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel,
    navController: NavController
){

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        items(users){user ->
            UserItem(
                user = user,
                timelineViewModel = timelineViewModel,
                activityViewModel = activityViewModel,
                navController = navController
            )
        }
    }
}
@Composable
fun UserItem(
    user: User,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel,
    navController: NavController
){
    val follows by timelineViewModel.follows.collectAsState()
    val currentUserId = 1L
    var isFollowing by remember{mutableStateOf(false)}
    if(timelineViewModel.listUserFollowing.collectAsState().value.contains(user)){
        // Nếu người dùng không phải là chính mình và đã theo dõi người dùng này
        isFollowing = true
    } else {
        isFollowing = false

    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{
                user.id?.let {id->
                    navController?.navigate("profile/$id")?: run{
                        Toast.makeText(context, "Cannot navigate to profile", Toast.LENGTH_SHORT).show()
                    }

                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            AsyncImage(
                model = user.profilePicture?: R.drawable.ic_avatar_placeholder,
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp,Color.Gray,CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(){
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user.bio.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    scope.launch {
                        if (isFollowing) {
                            timelineViewModel.unfollowUser(user.id ?: -1)
                            timelineViewModel.fetchUserFollowersAndFollowing()
                        } else {
                            timelineViewModel.followUser(user.id ?: -1)
                            timelineViewModel.fetchUserFollowersAndFollowing()
                        }
                    }
                },
                modifier = Modifier.height(36.dp)
                    .border(1.dp,Color.Gray.copy(alpha = 0.3f),RoundedCornerShape(18.dp))
            ) {
                Text(
                    if(isFollowing) "Đang theo dõi" else "Theo dõi",
                    fontSize = 12.sp,
                    color = if(isFollowing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

