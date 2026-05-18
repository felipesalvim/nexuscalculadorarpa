package com.example.nexuscalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class DadosPessoaisActivity extends AppCompatActivity {

    private EditText edtNome, edtCpf, edtTelefone, edtEmail, edtCep, edtEndereco, edtNumero;
    private Button btnBuscarCep, btnProximo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dados_pessoais);

        edtNome = findViewById(R.id.edtNome);
        edtCpf = findViewById(R.id.edtCpf);
        edtTelefone = findViewById(R.id.edtTelefone);
        edtEmail = findViewById(R.id.edtEmail);
        edtCep = findViewById(R.id.edtCep);
        edtEndereco = findViewById(R.id.edtEndereco);
        edtNumero = findViewById(R.id.edtNumero);
        btnBuscarCep = findViewById(R.id.btnBuscarCep);
        btnProximo = findViewById(R.id.btnProximo);

        edtCpf.addTextChangedListener(MaskUtil.insert(edtCpf, MaskUtil.FORMAT_CPF));
        edtTelefone.addTextChangedListener(MaskUtil.insert(edtTelefone, MaskUtil.FORMAT_FONE));
        edtCep.addTextChangedListener(MaskUtil.insert(edtCep, MaskUtil.FORMAT_CEP));

        btnBuscarCep.setOnClickListener(v -> buscarCepViaApi());

        btnProximo.setOnClickListener(v -> {
            if (edtNome.getText().toString().trim().isEmpty() || edtCpf.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Preencha pelo menos o Nome e o CPF.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Concatena o Endereço com o Número
            String enderecoCompleto = edtEndereco.getText().toString();
            if(!edtNumero.getText().toString().isEmpty()){
                enderecoCompleto += ", Nº " + edtNumero.getText().toString();
            }

            Intent intent = new Intent(DadosPessoaisActivity.this, DadosServicoActivity.class);
            intent.putExtra("NOME", edtNome.getText().toString());
            intent.putExtra("CPF", edtCpf.getText().toString());
            intent.putExtra("TELEFONE", edtTelefone.getText().toString());
            intent.putExtra("EMAIL", edtEmail.getText().toString());
            intent.putExtra("ENDERECO", enderecoCompleto);
            startActivity(intent);
        });
    }

    private void buscarCepViaApi() {
        String cep = edtCep.getText().toString().replaceAll("[^0-9]", "");
        if (cep.length() != 8) {
            Toast.makeText(this, "Digite um CEP válido com 8 dígitos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://viacep.com.br/ws/" + cep + "/json/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) { response.append(inputLine); }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                if (!json.has("erro")) {
                    String enderecoFull = json.getString("logradouro") + " - " +
                            json.getString("bairro") + ", " +
                            json.getString("localidade") + "/" +
                            json.getString("uf");
                    runOnUiThread(() -> edtEndereco.setText(enderecoFull));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "CEP não encontrado.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Erro de conexão ao buscar CEP.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}