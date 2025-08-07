package com.example.nexus.ui.search

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val timelineViewModel: TimelineViewModel?
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Kết quả tìm kiếm sử dụng userCache và postCache từ TimelineViewModel
    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        timelineViewModel?.postCache ?: MutableStateFlow(emptyMap()),
        timelineViewModel?.userCache ?: MutableStateFlow(emptyMap())
    ) { query, posts, users ->
        if (query.isBlank()) {
            emptyList()
        } else {
            // Tìm kiếm users từ userCache
            val userResults = users.values
                .filter { user ->
                    user.username.contains(query, ignoreCase = true) ||
                            (user.bio?.contains(query, ignoreCase = true) == true) ||
                            (user.fullName?.contains(query, ignoreCase = true) == true)
                }.map { SearchResult.UserResult(it) }

            // Tìm kiếm posts từ postCache
            val postResults = posts.values
                .filter { post ->
                    post.content.contains(query, ignoreCase = true) ||
                            post.user?.username?.contains(query, ignoreCase = true) == true ||
                            post.user?.fullName?.contains(query, ignoreCase = true) == true
                }
                .map { SearchResult.PostResult(it) }

            // Kết hợp kết quả, ưu tiên users trước, sau đó là posts
            // Giới hạn 5 users và 5 posts để cân bằng kết quả
            val limitedUserResults = userResults.take(5)
            val limitedPostResults = postResults.take(5)

            limitedUserResults + limitedPostResults
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Thêm phương thức để lấy tổng số kết quả tìm kiếm
    @OptIn(UnstableApi::class)
    fun getSearchResultsCount(): StateFlow<SearchResultsCount> = combine(
        _searchQuery,
        timelineViewModel?.postCache ?: MutableStateFlow(emptyMap()),
        timelineViewModel?.userCache ?: MutableStateFlow(emptyMap())
    ) { query, posts, users ->
        Log.d("SearchViewModel", "=== SEARCH DEBUG ===")
        Log.d("SearchViewModel", "Query: '$query'")
        Log.d("SearchViewModel", "Posts cache size: ${posts.size}")
        Log.d("SearchViewModel", "Users cache size: ${users.size}")

        if (query.isBlank()) {
            SearchResultsCount(0, 0)
        } else {
            val userCount = users.values.count { user ->
                user.username.contains(query, ignoreCase = true) ||
                        (user.bio?.contains(query, ignoreCase = true) == true) ||
                        (user.fullName?.contains(query, ignoreCase = true) == true)
            }

            val postCount = posts.values.count { post ->
                post.content.contains(query, ignoreCase = true) ||
                        post.user?.username?.contains(query, ignoreCase = true) == true ||
                        post.user?.fullName?.contains(query, ignoreCase = true) == true
            }

            SearchResultsCount(userCount, postCount)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchResultsCount(0, 0)
    )
}

sealed class SearchResult {
    data class UserResult(val user: User) : SearchResult()
    data class PostResult(val post: Post) : SearchResult()
}

data class SearchResultsCount(
    val userCount: Int,
    val postCount: Int
)