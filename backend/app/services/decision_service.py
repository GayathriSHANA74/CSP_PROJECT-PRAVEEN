from app.services.redis_service import redis_service
from app.services.safe_browsing_service import safe_browsing_service
from app.utils.normalizer import normalize_url
from app.models.analysis import AnalyzeResponse

class DecisionService:
    async def analyze(self, raw_url: str) -> AnalyzeResponse:
        normalized_url = normalize_url(raw_url)

        # 1. Check Redis Cache
        cached_result = await redis_service.cache_get(normalized_url)
        if cached_result:
            return AnalyzeResponse(
                verdict=cached_result["verdict"],
                confidence=cached_result.get("confidence", 1.0),
                source=cached_result.get("source", "Cache"),
                cached=True
            )

        # 2. Call Google Safe Browsing
        result = await safe_browsing_service.check_url(normalized_url)

        # 3. Store in Redis if not an error
        if result["verdict"] != "ERROR":
            await redis_service.cache_set(normalized_url, result)

        return AnalyzeResponse(
            verdict=result["verdict"],
            confidence=result.get("confidence", 0.0),
            source=result.get("source", "N/A"),
            cached=False,
            error=result.get("error")
        )

decision_service = DecisionService()
