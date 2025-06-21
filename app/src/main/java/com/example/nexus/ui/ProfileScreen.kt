package com.example.nexus.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.components.PostItem
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(
    userId: Long,
    timelineViewModel: TimelineViewModel,
    activityViewModel: ActivityViewModel? = null,
    navController: NavController? = null
) {
    val scope = rememberCoroutineScope()
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var following by remember { mutableStateOf<List<User>>(emptyList()) }
    var user: User? by remember { mutableStateOf(null) }
    val viewState by timelineViewModel.postsState
    val follows by timelineViewModel.follows.collectAsState()
    val context = LocalContext.current
    var isFollowing by remember{mutableStateOf(false)}
    timelineViewModel.fetchPostsUser(userId)

    LaunchedEffect(userId) {
        scope.launch {
            followers =  timelineViewModel.getFollowers(userId)
            following = timelineViewModel.getFollowing(userId)
            user = timelineViewModel.getUserById(userId = userId)
        }
    }
    // Tìm người dùng dựa trên userId (sử dụng posts đã thu thập)
    val currentUserId = timelineViewModel.currentUserId
    val isOwnProfile = userId == currentUserId
    // Kiểm tra trạng thái theo dõi

    if(!isOwnProfile && timelineViewModel.listUserFollowing.collectAsState().value.contains(user)){
        // Nếu người dùng không phải là chính mình và đã theo dõi người dùng này
        isFollowing = true
    } else {
        isFollowing = false

    }
    user?.let { profileUser ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = profileUser.profilePicture ?: R.drawable.ic_avatar_placeholder,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    error = painterResource(R.drawable.ic_avatar_placeholder),
                    placeholder = painterResource(R.drawable.ic_avatar_placeholder)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(
                            text = profileUser.username ?: "Unknown User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isOwnProfile) {
                            IconButton(onClick = {
                                navController?.navigate("settings")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(){
                        TextButton(onClick = {
                            navController?.navigate("userFollow/$userId/0")?: run{
                                Toast.makeText(context, "Cannot navigate to followers", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Column (){
                                Text(
                                    text = followers.size.toString()
                                )
                                Text(
                                    text = "người theo dõi"
                                )
                            }
                        }
                        TextButton(onClick = {
                            navController?.navigate("userFollow/$userId/1")?: run{
                                Toast.makeText(context, "Cannot navigate to followers", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Column (){
                                Text(
                                    text = following.size.toString()
                                )
                                Text(
                                    text = "đang theo dõi"
                                )
                            }
                        }
                    }
                }



            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isOwnProfile) {
                // Own profile: Settings and Edit Profile buttons
                    Button(
                        onClick = {
                            Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit Profile")
                    }
            } else {
                // Other user's profile: Follow/Unfollow button
                Button(
                    onClick = {
                        scope.launch {
                            if (isFollowing) {
                                timelineViewModel.unfollowUser(userId)
                                timelineViewModel.fetchUserFollowersAndFollowing()
                            } else {
                                timelineViewModel.followUser(userId)
                                timelineViewModel.fetchUserFollowersAndFollowing()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp) // Ensure consistent height
                        .then(
                            if (isFollowing) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color.Black,
                                    shape = RoundedCornerShape(30.dp)
                                )
                            } else {
                                Modifier
                            }
                        ),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else Color.Black,
                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                    )

                ) {
                    Text(if (isFollowing) "Unfollow" else "Follow")
                }
            }
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
                        items(viewState.posts) { post ->
                            PostItem(
                                post = post,
                                viewModel = timelineViewModel,
                                navController = navController,
                                activityViewModel = activityViewModel
                            )
                        }
                    }
                }
            }

            // Hiển thị danh sách bài đăng của người dùng
//            val userPosts = remember(posts) {
//                posts.filter { it.user?.id == userId }
//            }
//
//            if (userPosts.isEmpty()) {
//                Text(
//                    text = "No posts yet",
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .wrapContentSize(Alignment.Center),
//                    style = MaterialTheme.typography.bodyLarge
//                )
//            } else {
//                LazyColumn {
//                    items(userPosts) { post ->
//                        PostItem(
//                            post = post,
//                            viewModel = timelineViewModel,
//                            navController = navController
//                        )
//                    }
//                }
//            }
        }
    } ?: run {
        Text(
            text = "User not found",
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun SettingScreen(
    viewModel: TimelineViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Account Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        SettingItem(
            title = "Edit Profile",
            subtitle = "Update your profile information",
            onClick = {
                Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
            }
        )
        SettingItem(
            title = "Change Password",
            subtitle = "Update your password",
            onClick = {
                navController.navigate("change_password")
            }
        )

        Button(
            onClick = {

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text("Log Out")
        }
    }
}
//@Preview(showBackground = true)
//@Composable
//fun SettingScreenPreview() {
//    MaterialTheme {
//        SettingScreen(
//            viewModel = TimelineViewModel(),
//            navController = NavController(LocalContext.current)
//        )
//    }
//}
@Composable
fun SettingItem(
    title: String,
    subtitle:String,
    onClick:() -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
@Composable
fun ChangePassword(
    viewModel: TimelineViewModel,
    navController: NavController
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Form fields with spacing
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                passwordErrorMessage = validatePasswordMatch(it, confirmPassword)
            },
            label = { Text("New Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                passwordErrorMessage = validatePasswordMatch(newPassword, it)
            },
            label = { Text("Confirm New Password") },
            modifier = Modifier.fillMaxWidth(),
            isError = passwordErrorMessage != null,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            singleLine = true
        )

        // Error message
        passwordErrorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Black button at bottom
        Button(
            onClick = {
                if (isFormValid(currentPassword, newPassword, confirmPassword)) {
                    // Handle password change logic
                    Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Please complete all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                "Change Password",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Helper functions
private fun validatePasswordMatch(password: String, confirmPassword: String): String? {
    return if (confirmPassword.isNotEmpty() && password != confirmPassword) {
        "Passwords do not match"
    } else {
        null
    }
}

private fun isFormValid(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
    return currentPassword.isNotEmpty() &&
            newPassword.isNotEmpty() &&
            confirmPassword.isNotEmpty() &&
            newPassword == confirmPassword
}

//@Preview(showBackground = true)
//@Composable
//fun ChangePasswordPreview() {
//    MaterialTheme {
//        ChangePassword(
//            viewModel = TimelineViewModel(),
//            navController = NavController(LocalContext.current)
//        )
//    }
//}
