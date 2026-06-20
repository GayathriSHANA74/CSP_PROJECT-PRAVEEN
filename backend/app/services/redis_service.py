import json
import redis.asyncio as redis
from app.core.config import settings

class RedisService:
    def __init__(self):
        self.client = redis.from_url(settings.REDIS_URL, decode_responses=True)

    async def cache_get(self, key: str):
        data = await self.client.get(key)
        if data:
            return json.loads(data)
        return None

    async def cache_set(self, key: str, value: dict):
        await self.client.set(
            key,
            json.dumps(value),
            ex=settings.CACHE_TTL
        )

redis_service = RedisService()
