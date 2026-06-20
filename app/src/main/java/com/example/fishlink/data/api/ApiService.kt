package com.example.fishlink.data.api

import com.example.fishlink.data.models.AnalysisRequest
import com.example.fishlink.data.models.AnalysisResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/scan")
    suspend fun analyzeUrl(@Body request: AnalysisRequest): AnalysisResponse
}
