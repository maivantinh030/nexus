package com.example.nexus.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val timelineViewModel: TimelineViewModel
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Danh sách người dùng giả
    private val mockUsers = listOf(
        User(id = 1, username = "user1", bio = "Hello!"),
        User(id = 2, username = "user2", bio = "Loving this app!"),
        User(id = 3, username = "user3", bio = "Just joined!")
    )

    // Kết quả tìm kiếm
    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        timelineViewModel.posts
    ) { query, posts ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val userResults = mockUsers
                .filter { it.username.contains(query, ignoreCase = true) }
                .map { SearchResult.UserResult(it) }

            val postResults = posts
                .filter { it.content.contains(query, ignoreCase = true) }
                .map { SearchResult.PostResult(it) }

            (userResults + postResults).take(10) // Giới hạn 10 kết quả
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

sealed class SearchResult {
    data class UserResult(val user: User) : SearchResult()
    data class PostResult(val post: Post) : SearchResult()
}