import io
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def make_test_image() -> io.BytesIO:
    """Create a minimal JPEG image for testing."""
    from PIL import Image

    img = Image.new("RGB", (100, 100), color="red")
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf


def test_recognize_success(httpx_mock):
    """POST /recognize with a valid image returns recognized result."""
    # Mock the 百炼 API call — simulate a successful recognition
    httpx_mock.add_response(
        url="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        json={
            "choices": [
                {
                    "message": {
                        "content": '{"domain": "动物", "species": "柯基", "description": "柯基犬，短腿长身，性格活泼。", "confidence": 88}'
                    }
                }
            ]
        },
    )

    img = make_test_image()
    response = client.post(
        "/recognize",
        files={"image": ("test.jpg", img, "image/jpeg")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["recognized"] is True
    assert body["domain"] == "动物"
    assert body["species"] == "柯基"
    assert body["confidence"] == 88


def test_recognize_low_confidence(httpx_mock):
    """POST /recognize with low confidence returns unrecognized."""
    httpx_mock.add_response(
        url="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        json={
            "choices": [
                {
                    "message": {
                        "content": '{"domain": "动物", "species": "", "description": "", "confidence": 20}'
                    }
                }
            ]
        },
    )

    img = make_test_image()
    response = client.post(
        "/recognize",
        files={"image": ("test.jpg", img, "image/jpeg")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["recognized"] is False
    assert "message" in body


def test_retry_on_transient_failure(httpx_mock):
    """First call fails, second succeeds — endpoint retries and returns result."""
    httpx_mock.add_response(
        url="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        status_code=503,
    )
    httpx_mock.add_response(
        url="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        json={
            "choices": [
                {
                    "message": {
                        "content": '{"domain": "植物", "species": "银杏", "description": "银杏，古老裸子植物，扇形叶。", "confidence": 90}'
                    }
                }
            ]
        },
    )

    img = make_test_image()
    response = client.post(
        "/recognize",
        files={"image": ("test.jpg", img, "image/jpeg")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["recognized"] is True
    assert body["species"] == "银杏"
