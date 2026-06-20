package com.example.fishlink.data.models

data class AnalysisRequest(
    val url: String
)

data class AnalysisResponse(
    val verdict: String,
    val confidence: Double,
    val source: String,
    val cached: Boolean
)
