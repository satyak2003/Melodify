package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.data.storage.SearchHistoryStorage
import com.melodify.shared.domain.model.SearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

sealed class SearchUiState {
    data object Empty : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: SearchResult) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@OptIn(FlowPreview::class)
class SearchViewModel(private val musicRepository: MusicRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private val _searchResults = MutableStateFlow<SearchUiState>(SearchUiState.Empty)
    val searchResults: StateFlow<SearchUiState> = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    init {
        loadHistory()
        viewModelScope.launch {
            _query.debounce(300).filter { it.length >= 2 }.collect { query ->
                search(query)
            }
        }
    }

    private fun loadHistory() {
        _searchHistory.value = SearchHistoryStorage.loadHistory()
    }
    
    fun updateQuery(query: String) {
        _query.value = query
        if (query.length < 2) {
            _searchResults.value = SearchUiState.Empty
        }
    }
    
    fun clearSearch() {
        _query.value = ""
        _searchResults.value = SearchUiState.Empty
    }

    fun removeHistoryItem(queryItem: String) {
        SearchHistoryStorage.removeQuery(queryItem)
        loadHistory()
    }

    fun clearHistory() {
        SearchHistoryStorage.clearHistory()
        loadHistory()
    }
    
    private suspend fun search(query: String) {
        _searchResults.value = SearchUiState.Loading
        musicRepository.search(query)
            .onSuccess {
                SearchHistoryStorage.saveQuery(query)
                loadHistory()
                _searchResults.value = SearchUiState.Success(it)
            }
            .onFailure { _searchResults.value = SearchUiState.Error(it.message ?: "Search failed") }
    }
}

