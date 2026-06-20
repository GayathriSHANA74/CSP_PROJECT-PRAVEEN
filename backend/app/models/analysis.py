from pydantic import BaseModel, HttpUrl
from typing import Optional

class AnalyzeRequest(BaseModel):
    url: str

class AnalyzeResponse(BaseModel):
    verdict: str  # SAFE, MALICIOUS, ERROR
    confidence: float
    source: str
    cached: bool
    error: Optional[str] = None
