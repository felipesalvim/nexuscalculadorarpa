package com.example.nexuscalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText edtValorBruto, edtDependentes, edtIss;
    private Button btnProsseguir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtValorBruto = findViewById(R.id.edtValorBruto);
        edtDependentes = findViewById(R.id.edtDependentes);
        edtIss = findViewById(R.id.edtIss);
        btnProsseguir = findViewById(R.id.btnProsseguir);

        btnProsseguir.setOnClickListener(v -> {
            String strBruto = edtValorBruto.getText().toString();
            String strDep = edtDependentes.getText().toString();
            String strIss = edtIss.getText().toString();

            if (strBruto.isEmpty()) {
                Toast.makeText(this, "Informe o valor bruto contratado.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Envia os dados para a tela de Resultado
            Intent intent = new Intent(MainActivity.this, ResultadoActivity.class);
            intent.putExtra("BRUTO", Double.parseDouble(strBruto));
            intent.putExtra("DEPENDENTES", strDep.isEmpty() ? 0 : Integer.parseInt(strDep));
            intent.putExtra("ISS", strIss.isEmpty() ? 0.0 : Double.parseDouble(strIss));
            startActivity(intent);
        });
    }
}