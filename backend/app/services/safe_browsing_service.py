import httpx
from app.core.config import settings

class SafeBrowsingService:
    def __init__(self):
        self.api_key = settings.SAFE_BROWSING_API_KEY
        self.url = f"https://safebrowsing.googleapis.com/v4/threatMatches:find?key={self.api_key}"

    async def check_url(self, target_url: str) -> dict:
        if not self.api_key:
            return {"verdict": "ERROR", "error": "API Key not configured"}

        payload = {
            "client": {
                "clientId": "fishlink-backend",
                "clientVersion": "1.0.0"
            },
            "threatInfo": {
                "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"],
                "platformTypes": ["ANY_PLATFORM"],
                "threatEntryTypes": ["URL"],
                "threatEntries": [{"url": target_url}]
            }
        }

        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(self.url, json=payload)
                response.raise_for_status()
                data = response.json()

                # If 'matches' exists, it's malicious
                if "matches" in data and len(data["matches"]) > 0:
                    return {
                        "verdict": "MALICIOUS",
                        "confidence": 1.0,
                        "source": "Google Safe Browsing"
                    }

                return {
                    "verdict": "SAFE",
                    "confidence": 1.0,
                    "source": "Google Safe Browsing"
                }
            except Exception as e:
                return {"verdict": "ERROR", "error": str(e)}

safe_browsing_service = SafeBrowsingService()
