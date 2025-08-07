package com.example.nexus.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexus.R
import com.example.nexus.components.PostItem
import com.example.nexus.network.AuthManager
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    var isFollowing by remember{mutableStateOf(false)}


    LaunchedEffect(userId) {
        timelineViewModel.fetchPostsUser(userId)
        scope.launch {
            followers =  timelineViewModel.getFollowers(userId)
            following = timelineViewModel.getFollowing(userId)
            user = timelineViewModel.getUserById(userId = userId)
        }
    }
    LaunchedEffect(viewState.posts) {
        viewState.posts.forEach { post ->
            scope.launch {
                val isLiked = timelineViewModel.checkLike("POST", post.id ?: 0)
                post.isLiked = isLiked
                timelineViewModel.fetchLikeCount("POST", post.id ?: 0)
            }
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

        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
                )
            )){
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
                        model = RetrofitClient.MEDIA_BASE_URL + profileUser.profilePicture,
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
                                text = profileUser.fullName ?: "Unknown User",
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

                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (!profileUser.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = profileUser.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (isOwnProfile) {
                    // Own profile: Settings and Edit Profile buttons
                    Button(
                        onClick = {
                            navController?.navigate("editprofile")
                            Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA9A9A9),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Chỉnh sửa hồ sơ")
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
                            .height(40.dp) // Tăng height từ 30dp lên 40dp
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
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp) // Thêm padding
                    ) {
                        Text(
                            text = if (isFollowing) "Hủy theo dõi" else "Theo dõi",
                            fontSize = 14.sp, // Đặt font size rõ ràng
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                )
                            }
                        }
                    }
                }

            }
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


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SettingScreen(
    viewModel: TimelineViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
            )
        )){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Cài đặt",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cài đặt tài khoản",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingItem(
                title = "Chỉnh sửa hồ sơ",
                subtitle = "Cập nhật thông tin cá nhân",
                onClick = {
                    navController?.navigate("editprofile")
                    Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
                }
            )
            SettingItem(
                title = "Đổi mật khẩu",
                subtitle = "Thay đổi mật khẩu đăng nhập",
                onClick = {
                    navController.navigate("change_password")
                }
            )

            Button(
                onClick = {
                    scope.launch {
                        RetrofitClient.apiService.removeFCMToken()
                        viewModel.authManager.logout()
                        context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).edit()
                            .remove("last_sent_fcm_token")

                        Log.d("Logout", "FCM token cleared for user")
                        navController.navigate("login") {
                        }
                    }

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Đăng xuất")
            }
        }
    }
}
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
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
            )
        )){
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
                    text = "Đổi mật khẩu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Form fields with spacing
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Mật khẩu hiện tại") },
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
                label = { Text("Mật khẩu mới") },
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
                label = { Text("Xác nhận mật khẩu") },
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
                        scope.launch {
                            // Call the change password function in the ViewModel
//                            viewModel.changePassword(
//                                currentPassword = currentPassword,
//                                newPassword = newPassword
//                            )
                        }
                        Toast.makeText(context, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Vui lòng điền chính xác các thông tin", Toast.LENGTH_SHORT).show()
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
                    "Đổi mật khẩu",
                    style = MaterialTheme.typography.titleMedium
                )
            }
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


@Composable
fun EditProfileScreen(
    viewModel: TimelineViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State variables
    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<User?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Toast.makeText(context, "Ảnh đã được chọn", Toast.LENGTH_SHORT).show()
        }
    }

    // Load current user data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                user = viewModel.getUserById(viewModel.currentUserId ?: 0)
                user?.let {
                    fullName = it.fullName ?: ""
                    bio = it.bio ?: ""
                    email = it.email ?: ""
                }
            } catch (e: Exception) {
                errorMessage = "Không thể tải dữ liệu profile"
                Toast.makeText(context, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
            )
        )){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                    text = "Chỉnh sửa hồ sơ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading && user == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Avatar Section
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    AsyncImage(
                        model = selectedImageUri ?: (RetrofitClient.MEDIA_BASE_URL + user?.profilePicture) ?: R.drawable.ic_avatar_placeholder,
                        contentDescription = "Profile Avatar",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        error = painterResource(R.drawable.ic_avatar_placeholder),
                        placeholder = painterResource(R.drawable.ic_avatar_placeholder),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Profile Picture",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            }
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Input Fields
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        errorMessage = null
                    },
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = fullName.isBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.8f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = Color(0xFF7BB3D3),
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = {
                        bio = it
                        errorMessage = null
                    },
                    label = { Text("Tiểu sử") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.8f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = Color(0xFF7BB3D3),
                        unfocusedBorderColor = Color.Transparent
                    )

                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.8f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = Color(0xFF7BB3D3),
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Save Changes Button
                Button(
                    onClick = {
                        if (fullName.isBlank() || email.isBlank()) {
                            errorMessage = "Vui lòng điền đầy đủ thông tin bắt buộc"
                            return@Button
                        }

                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            errorMessage = "Email không hợp lệ"
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                val success = viewModel.updateProfile(
                                    fullName = fullName,
                                    bio = bio,
                                    email = email,
                                    profileImageUri = selectedImageUri
                                )

                                if (success) {
                                    Toast.makeText(context, "Profile đã được cập nhật", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    errorMessage = "Không thể cập nhật profile"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Lỗi: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Lưu thay đổi",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

