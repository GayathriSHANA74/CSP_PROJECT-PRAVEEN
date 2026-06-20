package com.example.fishlink.data.models

sealed class AnalysisError(message: String) : Exception(message) {
    object NetworkError : AnalysisError("No internet connection")
    object ServerError : AnalysisError("Internal server error")
    object TimeoutError : AnalysisError("Request timed out")
    data class UnknownError(val msg: String) : AnalysisError(msg)
}
