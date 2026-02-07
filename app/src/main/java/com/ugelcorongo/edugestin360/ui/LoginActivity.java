package com.ugelcorongo.edugestin360.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.ugelcorongo.edugestin360.handlers.ProfileHandler;
import com.ugelcorongo.edugestin360.handlers.RegistrationHandler;

import com.ugelcorongo.edugestin360.R;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUser, etPass;
    private MaterialButton      btnLogin;
    private String user;
    private static final int    REQ_CHANGE_PWD = 1234;
    public static final String EXTRA_DOCIDENTIDAD = "docidentidad";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ajuste de insets para edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
                    return insets;
                }
        );

        // Referencias
        etUser   = findViewById(R.id.etUser);
        etPass   = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            btnLogin.setEnabled(false);
            btnLogin.setText("");
            // Mostrar un spinner o ProgressBar si lo tienes
            findViewById(R.id.btnLoginProgress).setVisibility(android.view.View.VISIBLE);

            user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            RegistrationHandler.attemptLogin(
                    this,
                    user,
                    pass,
                    loginCallback
            );
        });
    }

    /** Callback centralizado */
    private final RegistrationHandler.LoginCallback loginCallback =
            new RegistrationHandler.LoginCallback() {
                @Override
                public void onSuccess(String role, String docident) {
                    runOnUiThread(() -> {
                        findViewById(R.id.btnLoginProgress).setVisibility(android.view.View.GONE);
                        // ① Sincronizamos perfil desde el servidor
                        ProfileHandler.syncUserProfile(
                                LoginActivity.this,
                                user,
                                new ProfileHandler.SyncCallback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError(String error) {}
                                }
                        );

                        Intent next;
                        switch (role) {
                            case "director":
                                next = new Intent(LoginActivity.this, DirectorActivity.class);
                                break;
                            case "especialista":
                                next = new Intent(LoginActivity.this, EspecialistaActivity.class);
                                break;
                            default:
                                next = new Intent(LoginActivity.this, DocenteActivity.class);
                        }
                        // Aquí metes el docidentidad
                        next.putExtra(EXTRA_DOCIDENTIDAD, docident);

                        startActivity(next);
                        finish();
                    });
                }

                @Override
                public void onFail(String message) {
                    runOnUiThread(() -> {
                        findViewById(R.id.btnLoginProgress).setVisibility(android.view.View.GONE);
                        btnLogin.setText("Entrar");
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onRequirePasswordChange(String userId) {
                    runOnUiThread(() -> {
                        findViewById(R.id.btnLoginProgress).setVisibility(android.view.View.GONE);
                        // Lanzar ChangePasswordActivity explicitamente
                        Intent intent = new Intent(
                                LoginActivity.this,
                                ChangePasswordActivity.class
                        );
                        intent.putExtra(ChangePasswordActivity.EXTRA_USER_ID, userId);
                        startActivityForResult(intent, REQ_CHANGE_PWD);
                    });
                }
            };

    /** Captura el resultado tras cambio de contraseña */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CHANGE_PWD && resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                    this,
                    "Contraseña actualizada exitosamente. Por favor, inicia sesión de nuevo.",
                    Toast.LENGTH_LONG
            ).show();
            btnLogin.setText("Entrar");
            btnLogin.setEnabled(true);
        }
    }
}