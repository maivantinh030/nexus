package com.example.nexus

import ActivityScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexus.ui.ChangePassword
import com.example.nexus.ui.CreatePostScreen
import com.example.nexus.ui.PostDetailScreen
import com.example.nexus.ui.ProfileScreen
import com.example.nexus.ui.SearchScreen
import com.example.nexus.ui.TimelineScreen
import com.example.nexus.ui.UserFollowScreen
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.login.LoginScreen
import com.example.nexus.ui.login.SignUpScreen
import com.example.nexus.ui.search.SearchViewModel
import com.example.nexus.ui.timeline.TimelineViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(viewModel: TimelineViewModel = TimelineViewModel()){
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val searchViewModel = SearchViewModel(viewModel)
    val activityViewModel = ActivityViewModel(viewModel)

    // Các mục trong bottom navigation
    val bottomNavItems = listOf(
        BottomNavItem("timeline", "Home", Icons.Filled.Home),
        BottomNavItem("search", "Search", Icons.Filled.Search),
        BottomNavItem("create_post", "Create", Icons.Filled.Add),
        BottomNavItem("activity", "Activity", Icons.Filled.Favorite),
        BottomNavItem("profile", "Profile", Icons.Filled.Person)
    )
    Scaffold(
        bottomBar = {
            // Chỉ hiển thị bottom navigation nếu không ở màn hình login/signup
            if (currentRoute !in listOf("login", "signup","post_detail/{postId}", "profile/{userId}")) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController = navController)
            }
            composable("signup") {
                SignUpScreen(navController = navController)
            }
            composable("timeline") {
                TimelineScreen( viewModel = viewModel,
                    activityViewModel = activityViewModel,
                    navController = navController)
            }
            composable("search") {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    timelineViewModel = viewModel,
                    navController = navController
                )
            }
            composable("create_post") {
                CreatePostScreen(
                    viewModel = viewModel,navController = navController
                )
            }
            composable("activity") {
                ActivityScreen(
                    activityViewModel = activityViewModel,
                    timelineViewModel = viewModel,
                    navController = navController)
            }
            composable("profile") {
                ProfileScreen(
                    userId = 1, // Giả lập userId cho nút Profile
                    timelineViewModel = viewModel,
                    navController = navController
                )
            }
            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.LongType }) // Đổi từ Long sang Int
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getLong("postId")
                if (postId != null) {
                    PostDetailScreen(
                        postId = postId,
                        viewModel = viewModel,
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
                if (userId != null) {
                    ProfileScreen(
                        userId = userId,
                        timelineViewModel = viewModel,
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

                UserFollowScreen(
                    userId = userId,
                    timelineViewModel = viewModel,
                    activityViewModel = activityViewModel,
                    navController = navController,
                    selectedTab = tabIndex
                )
            }
            composable(route="change_password") {
                ChangePassword(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)