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

public class DadosServicoActivity extends AppCompatActivity {

    private EditText edtEmpresa, edtCnpj, edtEnderecoEmpresa, edtCnae, edtDescricao, edtBruto, edtDependentes, edtIss, edtInssRetido;
    private Button btnBuscarCnpj;
    private String nome, cpf, telefone, email, endereco;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dados_servico);

        edtEmpresa = findViewById(R.id.edtEmpresa);
        edtCnpj = findViewById(R.id.edtCnpj);
        edtEnderecoEmpresa = findViewById(R.id.edtEnderecoEmpresa);
        edtCnae = findViewById(R.id.edtCnae);
        edtDescricao = findViewById(R.id.edtDescricao);
        edtBruto = findViewById(R.id.edtBruto);
        edtDependentes = findViewById(R.id.edtDependentes);
        edtIss = findViewById(R.id.edtIss);
        edtInssRetido = findViewById(R.id.edtInssRetido);
        btnBuscarCnpj = findViewById(R.id.btnBuscarCnpj);

        edtCnpj.addTextChangedListener(MaskUtil.insert(edtCnpj, MaskUtil.FORMAT_CNPJ));
        btnBuscarCnpj.setOnClickListener(v -> buscarCnpjViaApi());

        Intent recebido = getIntent();
        nome = recebido.getStringExtra("NOME");
        cpf = recebido.getStringExtra("CPF");
        telefone = recebido.getStringExtra("TELEFONE");
        email = recebido.getStringExtra("EMAIL");
        endereco = recebido.getStringExtra("ENDERECO");

        findViewById(R.id.btnCalcular).setOnClickListener(v -> {
            if (edtBruto.getText().toString().trim().isEmpty() || edtEmpresa.getText().toString().isEmpty()) {
                Toast.makeText(this, "Preencha Empresa e Valor Bruto.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(DadosServicoActivity.this, ResultadoActivity.class);
            intent.putExtras(recebido);
            intent.putExtra("EMPRESA", edtEmpresa.getText().toString());
            intent.putExtra("CNPJ", edtCnpj.getText().toString());
            intent.putExtra("ENDERECO_EMPRESA", edtEnderecoEmpresa.getText().toString());
            intent.putExtra("CNAE", edtCnae.getText().toString());
            intent.putExtra("DESCRICAO", edtDescricao.getText().toString());

            intent.putExtra("BRUTO", Double.parseDouble(edtBruto.getText().toString()));
            intent.putExtra("DEP", Integer.parseInt(edtDependentes.getText().toString().isEmpty() ? "0" : edtDependentes.getText().toString()));
            intent.putExtra("ISS", Double.parseDouble(edtIss.getText().toString().isEmpty() ? "0" : edtIss.getText().toString()));
            intent.putExtra("INSS_RETIDO", Double.parseDouble(edtInssRetido.getText().toString().isEmpty() ? "0" : edtInssRetido.getText().toString()));

            startActivity(intent);
        });
    }

    private void buscarCnpjViaApi() {
        String cnpj = edtCnpj.getText().toString().replaceAll("[^0-9]", "");
        if (cnpj.length() != 14) {
            Toast.makeText(this, "CNPJ inválido (14 dígitos necessários).", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://brasilapi.com.br/api/cnpj/v1/" + cnpj);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) { response.append(inputLine); }
                    in.close();

                    JSONObject json = new JSONObject(response.toString());
                    String razaoSocial = json.getString("razao_social");
                    String cnaeDesc = json.optString("cnae_fiscal_descricao", "Atividade não informada");
                    String logradouro = json.optString("logradouro", "");
                    String numero = json.optString("numero", "S/N");
                    String bairro = json.optString("bairro", "");
                    String municipio = json.optString("municipio", "");
                    String uf = json.optString("uf", "");

                    String enderecoMontado = logradouro + ", " + numero + " - " + bairro + ", " + municipio + "/" + uf;

                    runOnUiThread(() -> {
                        edtEmpresa.setText(razaoSocial);
                        edtCnae.setText(cnaeDesc);
                        edtEnderecoEmpresa.setText(enderecoMontado);
                        Toast.makeText(this, "Dados da Receita carregados!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "CNPJ não encontrado.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Erro de conexão API.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}