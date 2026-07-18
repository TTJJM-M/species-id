package com.ttjjm.speciesid.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttjjm.speciesid.data.GuideGraph
import com.ttjjm.speciesid.data.guide.GuideRepository
import com.ttjjm.speciesid.data.guide.RecognitionRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GuideViewModel(
    private val repositoryProvider: () -> GuideRepository = { GuideGraph.repository },
) : ViewModel() {

    private val _domainFilter = MutableStateFlow<String?>(null)
    val domainFilter: StateFlow<String?> = _domainFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val records: StateFlow<List<RecognitionRecord>> =
        combine(_domainFilter, _searchQuery) { domain, query -> domain to query }
            .flatMapLatest { (domain, query) -> repositoryProvider().observe(domain, query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 各领域的全量条数,与当前筛选/搜索无关(统计卡用) */
    val domainCounts: StateFlow<Map<String, Int>> =
        repositoryProvider().observe(null, "")
            .map { list -> list.groupingBy { it.domain }.eachCount() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setDomainFilter(domain: String?) {
        _domainFilter.value = domain
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun delete(record: RecognitionRecord) {
        viewModelScope.launch {
            runCatching { repositoryProvider().delete(record) }
        }
    }
}
