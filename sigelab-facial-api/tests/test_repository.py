from contextlib import contextmanager

from app.repository import FacialRepository


class FakeCursor:
    def __init__(self, row):
        self.row, self.sql = row, None

    def execute(self, sql, *params):
        self.sql, self.params = sql, params
        return self

    def fetchone(self):
        return self.row


class FakeDb:
    def __init__(self, row):
        self.cur = FakeCursor(row)

    @contextmanager
    def cursor(self):
        yield self.cur


def test_sigelab_usuario_table_mapping(settings):
    # Fila real de dbo.Usuario: IdUsuario, NombreUsuario, IdRol, Estado
    db = FakeDb((14, "fletran", 1, "ACTIVO"))
    user = FacialRepository(db, settings).get_user(14)
    assert "SELECT [IdUsuario], [NombreUsuario], [IdRol], [Estado] FROM dbo.[Usuario]" in db.cur.sql
    assert user == {"id_usuario": 14, "usuario": "fletran", "rol": "1", "activo": True}


def test_inactive_estado(settings):
    user = FacialRepository(FakeDb((5, "jmorales", 1, "INACTIVO")), settings).get_user(5)
    assert user["activo"] is False
