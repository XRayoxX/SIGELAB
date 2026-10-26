from fastapi.testclient import TestClient

from app import main


def client(service):
    test_client = TestClient(main.app)
    test_client.__enter__()  # ejecuta el lifespan
    main.app.state.face_service = service
    return test_client


def test_health_is_public(service):
    c = client(service)
    response = c.get("/health")
    assert response.status_code == 200
    assert response.json()["modelLoaded"] is True


def test_api_key_is_required(service):
    c = client(service)
    assert c.get("/api/face/status/2").status_code == 401
    ok = c.get("/api/face/status/2", headers={"X-API-Key": "clave-de-prueba"})
    assert ok.status_code == 200 and ok.json()["enrolled"] is False


def test_multipart_enroll_and_error_format(service):
    c = client(service)
    headers = {"X-API-Key": "clave-de-prueba", "X-Client": "pytest"}
    files = [("images", (f"{i}.jpg", data, "image/jpeg")) for i, data in enumerate([b"2:front"] * 3)]
    bad = c.post("/api/face/enroll", data={"userId": "2"}, files=files, headers=headers)
    assert bad.status_code == 422
    body = bad.json()
    assert body["code"] == "CAPTURA_INCOMPLETA" and len(body["frames"]) == 3

    files = [("images", (f"{i}.jpg", d, "image/jpeg")) for i, d in enumerate([b"2:front", b"2:front", b"2:left", b"2:right", b"2:front"])]
    good = c.post("/api/face/enroll", data={"userId": "2"}, files=files, headers=headers)
    assert good.status_code == 200 and good.json()["success"] is True

    challenge = c.get("/api/face/challenge", params={"userId": 2}, headers=headers).json()
    assert challenge["challenge"] in {"turn_left", "turn_right"}
