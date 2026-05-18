package com.example.nexuscalculadora; // Confira se o pacote está correto

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Aguarda 2 segundos e chama a TELA DE DADOS PESSOAIS
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, DadosPessoaisActivity.class));
            finish();
        }, 2000);
    }
}