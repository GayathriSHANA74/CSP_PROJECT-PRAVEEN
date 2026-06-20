from fastapi import APIRouter, HTTPException
from app.models.analysis import AnalyzeRequest, AnalyzeResponse
from app.services.decision_service import decision_service

router = APIRouter()

@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_url(request: AnalyzeRequest):
    try:
        return await decision_service.analyze(request.url)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/health")
async def health_check():
    return {"status": "healthy"}
