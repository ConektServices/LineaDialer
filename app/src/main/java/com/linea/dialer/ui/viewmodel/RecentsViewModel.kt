package com.linea.dialer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linea.dialer.data.model.CallType
import com.linea.dialer.data.model.RealCallLog
import com.linea.dialer.data.repository.CallLogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LogFilter(val label: String) {
    ALL("All"), INCOMING("Incoming"), OUTGOING("Outgoing"), MISSED("Missed")
}

/** Why the log failed to load — drives the UI to show the right action. */
enum class LogError { PERMISSION_DENIED, UNKNOWN }

data class RecentsUiState(
    val logs: List<RealCallLog> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val errorType: LogError? = null,
)

class RecentsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CallLogRepository.getInstance(app)

    private val _allLogs  = MutableStateFlow<List<RealCallLog>>(emptyList())
    private val _search   = MutableStateFlow("")
    private val _filter   = MutableStateFlow(LogFilter.ALL)
    private val _loading  = MutableStateFlow(true)
    private val _error    = MutableStateFlow<String?>(null)
    private val _errType  = MutableStateFlow<LogError?>(null)

    val search: StateFlow<String>    = _search.asStateFlow()
    val filter: StateFlow<LogFilter> = _filter.asStateFlow()

    val uiState: StateFlow<RecentsUiState> = combine(
        _allLogs, _search, _filter, _loading, _error, _errType
    ) { arr ->
        val logs     = arr[0] as List<RealCallLog>
        val query    = arr[1] as String
        val mode     = arr[2] as LogFilter
        val loading  = arr[3] as Boolean
        val err      = arr[4] as String?
        val errType  = arr[5] as LogError?

        val filtered = logs
            .filter { log ->
                when (mode) {
                    LogFilter.ALL      -> true
                    LogFilter.INCOMING -> log.type == CallType.INCOMING
                    LogFilter.OUTGOING -> log.type == CallType.OUTGOING
                    LogFilter.MISSED   -> log.type == CallType.MISSED
                }
            }
            .filter { log ->
                query.isBlank() ||
                log.displayName.contains(query, ignoreCase = true) ||
                log.number.contains(query)
            }

        RecentsUiState(
            logs      = filtered,
            isLoading = loading,
            error     = err,
            errorType = errType,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecentsUiState())

    init { loadLogs() }

    fun loadLogs() {
        viewModelScope.launch {
            _loading.value = true
            _error.value   = null
            _errType.value = null

            runCatching { repo.loadRecent(150) }
                .onSuccess {
                    _allLogs.value  = it
                    _loading.value  = false
                }
                .onFailure { e ->
                    _loading.value = false
                    when (e) {
                        is SecurityException -> {
                            _error.value   = "Call log permission required"
                            _errType.value = LogError.PERMISSION_DENIED
                        }
                        else -> {
                            _error.value   = e.message ?: "Failed to load call log"
                            _errType.value = LogError.UNKNOWN
                        }
                    }
                }
        }
    }

    fun onSearch(query: String)    { _search.value = query }
    fun onFilter(f: LogFilter)     { _filter.value = f }

    suspend fun logsForContact(numbers: List<String>): List<RealCallLog> =
        repo.loadForContact(numbers)
}
