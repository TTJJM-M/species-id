import asyncio
import base64
import json
import os

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, UploadFile

load_dotenv()  # reads .env from current working directory

app = FastAPI()

BAILIAN_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
BAILIAN_API_KEY = os.environ.get("BAILIAN_API_KEY", "")
BAILIAN_MODEL = "qwen3-vl-235b-a22b-thinking"
CONFIDENCE_THRESHOLD = 55
MAX_RETRIES = 2
RETRY_DELAY = 1.0


async def call_bailian(image_b64: str) -> dict:
    last_error = None
    async with httpx.AsyncClient() as client:
        for attempt in range(MAX_RETRIES + 1):
            try:
                resp = await client.post(
                    BAILIAN_URL,
                    headers={
                        "Authorization": f"Bearer {BAILIAN_API_KEY}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": BAILIAN_MODEL,
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": (
                                            "识别这张图片中的主体内容。给出类别名（中文，如植物、动物、菜品、"
                                            "建筑、车辆、日用品、电子产品等）、名称（中文常用名）、"
                                            "一段简短介绍（50字以内）、以及你的置信度（0-100）。"
                                            '严格按以下JSON格式返回：{"domain": "类别名", '
                                            '"species": "名称", "description": "简介", "confidence": 数字}。'
                                            "如果没有把握，confidence填写0。"
                                        ),
                                    },
                                    {
                                        "type": "image_url",
                                        "image_url": {
                                            "url": f"data:image/jpeg;base64,{image_b64}"
                                        },
                                    },
                                ],
                            }
                        ],
                    },
                    timeout=30,
                )
                resp.raise_for_status()
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                return json.loads(content)
            except httpx.HTTPStatusError as e:
                last_error = e
                if e.response.status_code < 500:
                    raise
                if attempt < MAX_RETRIES:
                    await asyncio.sleep(RETRY_DELAY)
            except (httpx.ConnectError, httpx.TimeoutException) as e:
                last_error = e
                if attempt < MAX_RETRIES:
                    await asyncio.sleep(RETRY_DELAY)
        raise last_error


@app.post("/recognize")
async def recognize(image: UploadFile):
    image_bytes = await image.read()
    image_b64 = base64.b64encode(image_bytes).decode("utf-8")

    result = await call_bailian(image_b64)

    confidence = result.get("confidence", 0)
    recognized = confidence >= CONFIDENCE_THRESHOLD

    if recognized:
        return {
            "recognized": True,
            "domain": result.get("domain", ""),
            "species": result.get("species", ""),
            "description": result.get("description", ""),
            "confidence": confidence,
        }
    else:
        return {
            "recognized": False,
            "message": "认不准，请换个角度拍摄",
        }
