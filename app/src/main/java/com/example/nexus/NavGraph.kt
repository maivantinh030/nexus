package com.example.nexus

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexus.network.AuthManager
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.ChangePassword
import com.example.nexus.ui.CreatePostScreen
import com.example.nexus.ui.EditProfileScreen
import com.example.nexus.ui.PostDetailScreen
import com.example.nexus.ui.ProfileScreen
import com.example.nexus.ui.SearchScreen
import com.example.nexus.ui.SettingScreen
import com.example.nexus.ui.SplashScreen
import com.example.nexus.ui.TimelineScreen
import com.example.nexus.ui.UserFollowScreen
import com.example.nexus.ui.activity.ActivityScreen
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.activity.FCMManager
import com.example.nexus.ui.login.LoginScreen
import com.example.nexus.ui.login.LoginViewModel
import com.example.nexus.ui.login.SignUpScreen
import com.example.nexus.ui.search.SearchViewModel
import com.example.nexus.ui.timeline.TimelineViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(viewModel: TimelineViewModel? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activityViewModel = ActivityViewModel(
        apiService = RetrofitClient.apiService,
        fcmManager = FCMManager(context)
    )
    val loginViewModel = LoginViewModel(
        authManager = AuthManager(LocalContext.current)
    )

    val authManager = remember { AuthManager(context) }
    val timelineViewModel = viewModel ?: remember {
        if (authManager.isUserLoggedIn()) {
            TimelineViewModel(authManager = authManager, context = context)
        } else {
            null
        }
    }
    val searchViewModel = remember(timelineViewModel) {
        SearchViewModel(timelineViewModel = timelineViewModel)
    }

    val unreadCount = timelineViewModel?.unread_count?.value ?: 0
    // Các mục trong bottom navigation
    val bottomNavItems = listOf(
        BottomNavItem("timeline", "Trang chủ", Icons.Filled .Home),
        BottomNavItem("search", "Tìm kiếm", Icons.Filled.Search),
        BottomNavItem("create_post", "Đăng bài", Icons.Filled.Add),
        BottomNavItem("activity", "Hoạt động", Icons.Filled.Favorite),
        BottomNavItem("profile", "Cá nhân", Icons.Filled.Person)
    )
    Scaffold(
        bottomBar = {
            // Chỉ hiển thị bottom navigation nếu không ở màn hình login/signup
            if (currentRoute !in listOf("login", "signup","post_detail/{postId}", "profile/{userId}","splash")) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF0F8FF), // Light blue
                                    Color(0xFFF8F9FA)  // Light gray
                                )
                            )
                        )
                ){
                    NavigationBar(
                        containerColor = Color.Transparent, // Để gradient hiển thị
                        contentColor = Color.White // Màu cho nội dung
                    ) {
                        bottomNavItems.forEach { item ->

                            val hasBadge = item.route == "activity"
                            val badgeCount = unreadCount // ví dụ có 5 thông báo


                            NavigationBarItem(
                                icon = {
                                    if(hasBadge){
                                        BadgedBox(
                                            badge = {
                                                if (badgeCount > 0) {
                                                    Badge {
                                                        Text(badgeCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(item.icon, contentDescription = item.title)
                                        }
                                    }
                                    else
                                        Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        // Tránh tạo stack mới khi chuyển màn hình
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate("timeline") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    authManager = authManager
                )
            }
            composable("login") {
                LoginScreen(navController = navController,
                    viewModel = loginViewModel) // Truyền authManager vào LoginScreen
            }
            composable("signup") {
                SignUpScreen(navController = navController,
                    viewModel = loginViewModel)
            }
            composable("timeline") {
                timelineViewModel?.let { vm ->
                    TimelineScreen(
                        viewModel = vm,
                        navController = navController
                    )
                } ?: run {
                    LaunchedEffect(Unit) {
                        navController.navigate("login")
                    }
                }
            }
            composable("search") {
                timelineViewModel?.let { vm ->
                    searchViewModel?.let { searchVm ->
                        SearchScreen(
                            searchViewModel = searchVm,
                            timelineViewModel = vm,
                            navController = navController
                        )
                    }
                }
            }
            composable("create_post") {
                timelineViewModel?.let { vm ->
                    CreatePostScreen(
                        viewModel = vm,
                        navController = navController
                    )
                }
            }
            composable("activity") {
                timelineViewModel?.let { vm ->
                    ActivityScreen(
                        activityViewModel = activityViewModel,
                        timelineViewModel = vm,
                        navController = navController
                    )
                }
            }
            composable("profile") {
                timelineViewModel?.let { vm ->
                    val currentUserId = vm.currentUserId
                    if (currentUserId != null) {
                        ProfileScreen(
                            userId = currentUserId,
                            timelineViewModel = vm,
                            navController = navController
                        )
                    } else {
                        // Redirect to login
                        LaunchedEffect(Unit) {
                            navController.navigate("login")
                        }
                    }
                }
            }
            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.LongType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getLong("postId")
                if (postId != null&& timelineViewModel != null) {
                    PostDetailScreen(
                        postId = postId,
                        viewModel = timelineViewModel,
                        activityViewModel = activityViewModel,
                        navController = navController
                    )
                } else {
                    println("Post not found: postId = $postId")
                    Text(
                        text = "Post not found",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId")
                if (userId != null&& timelineViewModel != null) {
                    ProfileScreen(
                        userId = userId,
                        timelineViewModel = timelineViewModel,
                        navController = navController
                    )
                } else {
                    println("User not found: userId = $userId")
                    Text(
                        text = "User not found",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            composable(
                route = "userFollow/{userId}/{tabIndex}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.LongType },
                    navArgument("tabIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: 1L
                val tabIndex = backStackEntry.arguments?.getInt("tabIndex") ?: 0

                if( timelineViewModel != null) {
                    UserFollowScreen(
                        userId = userId,
                        timelineViewModel = timelineViewModel,
                        activityViewModel = activityViewModel,
                        navController = navController,
                        selectedTab = tabIndex
                    )
                }
            }
            composable(route="change_password") {
                if (timelineViewModel!= null){
                    ChangePassword(
                        viewModel = timelineViewModel,
                        navController = navController
                    )
                }

            }
            composable(route = "settings") {
                if(timelineViewModel!= null){
                    SettingScreen(
                        viewModel = timelineViewModel,
                        navController = navController
                    )
                }
            }
            composable(route = "editprofile"){
                if(timelineViewModel!= null){
                    EditProfileScreen(
                        viewModel = timelineViewModel,
                        navController = navController
                    )
                }

            }

        }
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)

