from fastapi import FastAPI
from app.api.endpoints import router as api_router

app = FastAPI(title="FishLink API")

app.include_router(api_router)

@app.get("/")
async def root():
    return {"message": "Welcome to FishLink API. Use /analyze to scan URLs."}
