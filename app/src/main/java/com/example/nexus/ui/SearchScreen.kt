package com.example.nexus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nexus.ui.model.User
import com.example.nexus.ui.search.SearchResult
import com.example.nexus.ui.search.SearchViewModel
import com.example.nexus.ui.timeline.TimelineViewModel
import com.example.nexus.R
import com.example.nexus.ui.model.Post
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    timelineViewModel: TimelineViewModel,
    navController: NavController? = null
) {
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {searchViewModel.updateSearchQuery(it)},
            label = {Text("Search users or posts...")},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        // Danh sách kết quả
        if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
            Text(
                text = "No results found",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        else{
            LazyColumn {
                items(searchResults) { result ->
                    when (result) {
                        is SearchResult.UserResult -> UserSearchItem(
                            user = result.user,
                            onClick = { navController?.navigate("profile/${result.user.id}") }
                        )
                        is SearchResult.PostResult -> PostSearchItem(
                            post = result.post,
                            onClick = { navController?.navigate("post_detail/${result.post.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_avatar_placeholder),
            contentDescription = "User avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Text(
                text = user.bio?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
fun PostSearchItem(post: Post, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_avatar_placeholder),
            contentDescription = "User avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = post.user?.username ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Text(
                text = post.content.take(50) + if (post.content.length > 50) "..." else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    val timelineViewModel = TimelineViewModel()
    val searchViewModel = SearchViewModel(timelineViewModel)
    SearchScreen(searchViewModel, timelineViewModel)
}