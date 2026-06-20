package com.example.fishlink.data.repository

import com.example.fishlink.data.api.ApiService
import com.example.fishlink.data.models.AnalysisError
import com.example.fishlink.data.models.AnalysisRequest
import com.example.fishlink.data.models.AnalysisResponse
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

interface AnalysisRepository {
    suspend fun analyzeUrl(url: String): Result<AnalysisResponse>
}

class AnalysisRepositoryImpl(private val apiService: ApiService) : AnalysisRepository {
    override suspend fun analyzeUrl(url: String): Result<AnalysisResponse> {
        return try {
            val response = apiService.analyzeUrl(AnalysisRequest(url))
            Result.success(response)
        } catch (e: Exception) {
            val mappedError = when (e) {
                is SocketTimeoutException -> AnalysisError.TimeoutError
                is IOException -> AnalysisError.NetworkError
                is HttpException -> {
                    if (e.code() >= 500) AnalysisError.ServerError
                    else AnalysisError.UnknownError("HTTP ${e.code()}: ${e.message()}")
                }
                else -> AnalysisError.UnknownError(e.message ?: "Unknown error")
            }
            Result.failure(mappedError)
        }
    }
}
