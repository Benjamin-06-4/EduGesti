package com.ugelcorongo.edugestin360.ui;

import com.ugelcorongo.edugestin360.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.ugelcorongo.edugestin360.handlers.RegistrationHandler;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

public class ChangePasswordActivity extends AppCompatActivity {
    private EditText etNew, etConfirm;
    private Button btnChange;
    private String userId;
    public static final String EXTRA_USER_ID = "extra_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        //userId = getIntent().getStringExtra("extra_user_id");
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        etNew     = findViewById(R.id.etNewPass);
        etConfirm = findViewById(R.id.etConfirmPass);
        btnChange = findViewById(R.id.btnConfirm);

        // Extraer userId de la URL
        Uri data = getIntent().getData();
        if (data != null) {
            userId = data.getQueryParameter("userId");
        }

        btnChange.setOnClickListener(v -> {
            String p1 = etNew.getText().toString();
            String p2 = etConfirm.getText().toString();
            if (!p1.equals(p2)) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p1.matches("(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}")) {
                Toast.makeText(this,
                        "Debe incluir letras y números, mínimo 6 caracteres.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            RegistrationHandler.changePassword(this, userId, p1, new RegistrationHandler.LoginCallback() {
                @Override
                public void onSuccess(String role, String docident) {
                    // Regresamos al LoginActivity con status
                    setResult(RESULT_OK);
                    finish();
                }
                @Override
                public void onFail(String message) {
                    runOnUiThread(() ->
                            Toast.makeText(ChangePasswordActivity.this,
                                    message, Toast.LENGTH_LONG).show());
                }
                @Override public void onRequirePasswordChange(String userId) { }
            });
        });
    }
}