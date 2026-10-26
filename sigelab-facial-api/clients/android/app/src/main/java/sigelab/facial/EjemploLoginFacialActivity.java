package sigelab.facial;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;

/**
 * Ejemplo: cómo usar CapturaFacialActivity desde su pantalla de login de SIGELAB móvil.
 * Copie estas partes en su propia Activity.
 */
public class EjemploLoginFacialActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> lanzadorFacial =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() != RESULT_OK || res.getData() == null) {
                    return; // el usuario canceló
                }
                FacialApiClient.Respuesta r = new FacialApiClient.Respuesta(
                        res.getData().getIntExtra(CapturaFacialActivity.RESULT_STATUS, 0),
                        res.getData().getStringExtra(CapturaFacialActivity.RESULT_JSON));
                if (r.coincide()) {
                    String usuario = r.json.optString("username");
                    int idUsuario = r.json.optInt("userId");
                    Toast.makeText(this, "Bienvenido " + usuario, Toast.LENGTH_LONG).show();
                    // abrir su menú principal con idUsuario...
                } else {
                    Toast.makeText(this, r.mensaje(), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Despertar la API apenas se abre la app (si estaba apagada tarda unos segundos).
        FacialApiClient api = new FacialApiClient(getString(R.string.facial_api_url), getString(R.string.facial_api_key));
        Executors.newSingleThreadExecutor().execute(() -> {
            try { api.salud(); } catch (Exception ignored) { }
        });
    }

    /** Después de validar usuario y contraseña contra su API, confirme con el rostro. */
    void verificarRostro(int idUsuario) {
        Intent i = new Intent(this, CapturaFacialActivity.class);
        i.putExtra(CapturaFacialActivity.EXTRA_MODO, CapturaFacialActivity.MODO_VERIFICAR);
        i.putExtra(CapturaFacialActivity.EXTRA_ID_USUARIO, idUsuario);
        lanzadorFacial.launch(i);
    }

    /** Registrar o actualizar el rostro del usuario. */
    void registrarRostro(int idUsuario) {
        Intent i = new Intent(this, CapturaFacialActivity.class);
        i.putExtra(CapturaFacialActivity.EXTRA_MODO, CapturaFacialActivity.MODO_REGISTRAR);
        i.putExtra(CapturaFacialActivity.EXTRA_ID_USUARIO, idUsuario);
        i.putExtra(CapturaFacialActivity.EXTRA_REEMPLAZAR, true);
        lanzadorFacial.launch(i);
    }

    /** Marcar ingreso al laboratorio sin escribir usuario. */
    void marcarIngreso() {
        Intent i = new Intent(this, CapturaFacialActivity.class);
        i.putExtra(CapturaFacialActivity.EXTRA_MODO, CapturaFacialActivity.MODO_IDENTIFICAR);
        lanzadorFacial.launch(i);
    }
}
