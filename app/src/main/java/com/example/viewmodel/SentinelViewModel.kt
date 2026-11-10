package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ApkPreflightReport
import com.example.core.model.AppSnapshot
import com.example.core.model.Finding
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import com.example.core.model.ScanRun
import com.example.core.model.TrustException
import com.example.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab {
    OVERVIEW,
    APPS,
    INSPECT,
    TRUST,
    HISTORY
}

sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Scanning(val stepName: String, val progressFraction: Float) : ScanUiState
    data class Completed(val scanRun: ScanRun) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface PreflightUiState {
    object Idle : PreflightUiState
    object Loading : PreflightUiState
    data class Success(val report: ApkPreflightReport) : PreflightUiState
    data class Error(val message: String) : PreflightUiState
}

class SentinelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecurityRepository(application.applicationContext)

    private val _selectedTab = MutableStateFlow(NavigationTab.OVERVIEW)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    private val _preflightUiState = MutableStateFlow<PreflightUiState>(PreflightUiState.Idle)
    val preflightUiState: StateFlow<PreflightUiState> = _preflightUiState.asStateFlow()

    private val _severityFilter = MutableStateFlow<FindingSeverity?>(null)
    val severityFilter: StateFlow<FindingSeverity?> = _severityFilter.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    val latestScanRun: StateFlow<ScanRun?> = repository.latestScanRun
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trustExceptions: StateFlow<List<TrustException>> = repository.trustExceptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFindings: StateFlow<List<Finding>> = repository.allFindings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFindings: StateFlow<List<Finding>> = combine(allFindings, _severityFilter) { list, filter ->
        if (filter == null) list else list.filter { it.severity == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AppSnapshot>> = repository.recentApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredApps: StateFlow<List<AppSnapshot>> = combine(allApps, _appSearchQuery) { apps, query ->
        if (query.isBlank()) apps else apps.filter {
            it.displayName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial quick audit if no prior scan exists
        viewModelScope.launch {
            // Check if DB is empty
            if (latestScanRun.value == null) {
                runAudit(isQuick = true)
            }
        }
    }

    fun selectTab(tab: NavigationTab) {
        _selectedTab.value = tab
    }

    fun setSeverityFilter(severity: FindingSeverity?) {
        _severityFilter.value = severity
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun runAudit(isQuick: Boolean) {
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Scanning("Starting Sentinel audit...", 0.05f)
            try {
                val result = repository.runAudit(isQuick = isQuick) { step, fraction ->
                    _scanUiState.value = ScanUiState.Scanning(step, fraction)
                }
                _scanUiState.value = ScanUiState.Completed(result)
            } catch (e: Exception) {
                _scanUiState.value = ScanUiState.Error(e.message ?: "Audit encountered an unexpected error.")
            }
        }
    }

    fun inspectApk(uri: Uri) {
        viewModelScope.launch {
            _preflightUiState.value = PreflightUiState.Loading
            val result = repository.preflightApk(uri)
            result.fold(
                onSuccess = { _preflightUiState.value = PreflightUiState.Success(it) },
                onFailure = { _preflightUiState.value = PreflightUiState.Error(it.message ?: "Failed to inspect APK.") }
            )
        }
    }

    fun clearPreflight() {
        _preflightUiState.value = PreflightUiState.Idle
    }

    fun addTrustException(packageName: String, ruleId: String, reason: String) {
        viewModelScope.launch {
            repository.addTrustException(
                TrustException(
                    packageName = packageName,
                    ruleId = ruleId,
                    reason = reason.ifBlank { "User accepted risk annotation for developer/AI tool." }
                )
            )
        }
    }

    fun toggleTrustException(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleTrustException(id, enabled)
        }
    }

    fun deleteTrustException(id: String) {
        viewModelScope.launch {
            repository.deleteTrustException(id)
        }
    }

    fun dismissFinding(findingId: String) {
        viewModelScope.launch {
            repository.updateFindingStatus(findingId, FindingStatus.DISMISSED)
        }
    }

    fun exportReport(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val text = repository.generateSanitizedReport()
            onReady(text)
        }
    }
}
