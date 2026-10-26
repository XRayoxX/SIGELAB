import math

import numpy as np

from app.engine import classify_pose
from app.quality import estimate_pose

# Ojos, nariz y comisuras en una cara frontal de 200 px.
FRONT = np.array([[70, 80], [130, 80], [100, 118], [75, 150], [125, 150]], dtype=float)


def rotate(points, degrees, center=(100, 110)):
    a = math.radians(degrees)
    r = np.array([[math.cos(a), -math.sin(a)], [math.sin(a), math.cos(a)]])
    return (points - center) @ r.T + center


def test_frontal_face_has_no_yaw():
    pose = estimate_pose(FRONT, (200, 200, 3))
    assert abs(pose.yaw) < 1 and abs(pose.pitch) < 5


def test_head_tilt_does_not_change_yaw():
    pose = estimate_pose(rotate(FRONT, 25), (200, 200, 3))
    assert abs(pose.yaw) < 1


def test_nose_toward_image_right_means_person_turned_left(settings):
    turned = FRONT.copy()
    turned[2, 0] += 15  # la nariz se desplaza a la derecha de la imagen
    pose = estimate_pose(turned, (200, 200, 3))
    assert pose.yaw > 15
    assert classify_pose(pose.yaw, settings) == "left"
    mirrored = turned.copy()
    mirrored[:, 0] = 200 - mirrored[:, 0]
    mirrored[[0, 1]] = mirrored[[1, 0]]
    mirrored[[3, 4]] = mirrored[[4, 3]]
    assert classify_pose(estimate_pose(mirrored, (200, 200, 3)).yaw, settings) == "right"
