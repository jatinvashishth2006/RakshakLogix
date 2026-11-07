from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import List
import numpy as np
from sklearn.linear_model import LinearRegression

app = FastAPI(title="RakshakLogix Forecast Service", version="1.0.0")

class ForecastInput(BaseModel):
    item_name: str
    current_stock: int = Field(ge=0)
    history: List[float] = []
    horizon_days: int = Field(default=30, ge=1, le=365)
    minimum_stock: int = Field(default=0, ge=0)

@app.get("/health")
def health():
    return {"status": "ok", "service": "forecast"}

@app.post("/forecast")
def forecast(data: ForecastInput):
    values = np.asarray([max(0.0, float(v)) for v in data.history], dtype=float)
    if len(values) >= 3:
        x = np.arange(len(values)).reshape(-1, 1)
        model = LinearRegression().fit(x, values)
        daily = max(0.0, float(model.predict([[len(values) + 1]])[0]))
        name = "linear-regression"
        metrics = {"training_points": len(values), "mae": float(np.mean(np.abs(model.predict(x) - values)))}
    elif len(values):
        daily = float(np.mean(values))
        name = "moving-average-fallback"
        metrics = {"training_points": len(values), "note": "Insufficient history for model validation"}
    else:
        daily = 0.0
        name = "no-history-fallback"
        metrics = {"training_points": 0, "note": "No consumption history available"}
    demand = round(daily * data.horizon_days, 2)
    remaining = round(data.current_stock - demand, 2)
    if remaining < 0: risk = "Critical Shortage"
    elif remaining < data.minimum_stock: risk = "Potential Shortage"
    elif remaining < data.minimum_stock * 1.5: risk = "Low Buffer"
    else: risk = "Normal"
    explanation = "Forecast demand exceeds current stock." if remaining < 0 else ("Projected stock falls below the configured minimum." if remaining < data.minimum_stock else "Projected stock remains above the configured minimum.")
    return {"item_name": data.item_name, "current_stock": data.current_stock, "historical_consumption": values.tolist(), "predicted_demand": demand, "expected_remaining_stock": remaining, "forecast_horizon_days": data.horizon_days, "risk_level": risk, "explanation": explanation, "model": name, "metrics": metrics}
