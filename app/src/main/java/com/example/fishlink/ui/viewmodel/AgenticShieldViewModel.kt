package com.example.fishlink.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishlink.data.di.NetworkModule
import com.example.fishlink.data.models.AnalysisResponse
import com.example.fishlink.data.repository.AnalysisRepository
import kotlinx.coroutines.launch

enum class ScanState { IDLE, LOADING, SAFE, MALICIOUS, ERROR }

class AgenticShieldViewModel(
    private val repository: AnalysisRepository = NetworkModule.repository
) : ViewModel() {

    var urlToScan by mutableStateOf("")
        private set

    var currentState by mutableStateOf(ScanState.IDLE)
        private set

    var analysisResult by mutableStateOf<AnalysisResponse?>(null)
        private set

    var errorMessage by mutableStateOf("")
        private set

    fun updateUrl(url: String) {
        urlToScan = url
    }

    fun scanUrl(targetUrl: String) {
        if (targetUrl.isBlank()) return
        updateUrl(targetUrl)
        currentState = ScanState.LOADING

        viewModelScope.launch {
            repository.analyzeUrl(urlToScan)
                .onSuccess { response ->
                    analysisResult = response
                    currentState = if (response.verdict == "SAFE") {
                        ScanState.SAFE
                    } else {
                        ScanState.MALICIOUS
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Unknown error occurred"
                    currentState = ScanState.ERROR
                }
        }
    }

    fun reset() {
        updateUrl("")
        currentState = ScanState.IDLE
        analysisResult = null
        errorMessage = ""
    }
}
