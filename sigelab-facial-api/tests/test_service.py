import pytest

from app.errors import FacialServiceError


ENROLL = [b"2:front", b"2:front", b"2:left", b"2:right", b"2:front"]


def challenge_for(service, user_id, wanted):
    while True:
        issued = service.challenges.issue(user_id)
        if issued["challenge"] == wanted:
            return issued["challengeToken"]


def test_enroll_requires_front_left_and_right(service):
    with pytest.raises(FacialServiceError) as error:
        service.enroll(2, False, [b"2:front"] * 5, False, "test")
    assert error.value.code == "CAPTURA_INCOMPLETA"
    assert "izquierda" in error.value.message


def test_enroll_rejects_mixed_people(service):
    with pytest.raises(FacialServiceError) as error:
        service.enroll(2, False, [b"2:front", b"2:front", b"5:left", b"5:right", b"2:front"], False, "test")
    assert error.value.code == "PERSONAS_DISTINTAS"


def test_enroll_then_verify_with_liveness(service):
    result = service.enroll(2, False, ENROLL, False, "test")
    assert result["success"] and result["samples"] == 5

    token = challenge_for(service, 2, "turn_left")
    verified = service.verify(2, [b"2:front", b"2:left"], token, False, "test")
    assert verified["matched"] is True
    assert verified["livenessPassed"] is True


def test_verify_wrong_person_does_not_match(service):
    service.enroll(2, False, ENROLL, False, "test")
    token = challenge_for(service, 2, "turn_right")
    verified = service.verify(2, [b"7:front", b"7:right"], token, False, "test")
    assert verified["matched"] is False


def test_liveness_fails_when_turn_is_missing_or_wrong(service):
    service.enroll(2, False, ENROLL, False, "test")
    token = challenge_for(service, 2, "turn_left")
    with pytest.raises(FacialServiceError) as error:
        service.verify(2, [b"2:front", b"2:right"], token, False, "test")
    assert error.value.code == "LIVENESS_FALLIDO"
    assert "contrario" in error.value.message


def test_challenge_cannot_be_reused(service):
    service.enroll(2, False, ENROLL, False, "test")
    token = challenge_for(service, 2, "turn_left")
    service.verify(2, [b"2:front", b"2:left"], token, False, "test")
    with pytest.raises(FacialServiceError) as error:
        service.verify(2, [b"2:front", b"2:left"], token, False, "test")
    assert error.value.code == "DESAFIO_USADO"


def test_challenge_is_bound_to_user(service):
    service.enroll(2, False, ENROLL, False, "test")
    token = challenge_for(service, 1, "turn_left")
    with pytest.raises(FacialServiceError) as error:
        service.verify(2, [b"2:front", b"2:left"], token, False, "test")
    assert error.value.code == "DESAFIO_INVALIDO"


def test_identify_finds_the_registered_user(service):
    service.enroll(2, False, ENROLL, False, "test")
    token = challenge_for(service, None, "turn_right")
    found = service.identify([b"2:front", b"2:right"], token, False, "test")
    assert found["matched"] is True and found["userId"] == 2 and found["username"] == "estudiante"

    token = challenge_for(service, None, "turn_right")
    unknown = service.identify([b"9:front", b"9:right"], token, False, "test")
    assert unknown["matched"] is False and unknown["userId"] is None


def test_same_face_cannot_be_enrolled_for_two_users(service):
    service.enroll(2, False, ENROLL, False, "test")
    with pytest.raises(FacialServiceError) as error:
        service.enroll(1, False, ENROLL, False, "test")
    assert error.value.code == "ROSTRO_YA_REGISTRADO"


def test_existing_profile_requires_replace(service):
    service.enroll(2, False, ENROLL, False, "test")
    with pytest.raises(FacialServiceError) as error:
        service.enroll(2, False, ENROLL, False, "test")
    assert error.value.code == "PERFIL_YA_EXISTE"
    assert service.enroll(2, True, ENROLL, False, "test")["success"]


def test_inactive_user_and_no_face(service):
    with pytest.raises(FacialServiceError) as error:
        service.enroll(3, False, ENROLL, False, "test")
    assert error.value.code == "USUARIO_INACTIVO"
    with pytest.raises(FacialServiceError) as error:
        service.enroll(2, False, [b"none"], False, "test")
    assert error.value.code == "CALIDAD_INSUFICIENTE"


def test_delete_requires_admin(service):
    service.enroll(2, False, ENROLL, False, "test")
    with pytest.raises(FacialServiceError):
        service.delete_profile(2, 2, "test")
    assert service.delete_profile(2, 1, "test")["deleted"] is True
    assert service.status(2)["enrolled"] is False
