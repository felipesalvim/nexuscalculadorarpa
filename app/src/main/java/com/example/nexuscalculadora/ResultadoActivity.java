package com.example.nexuscalculadora;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class ResultadoActivity extends AppCompatActivity {

    private String nome, cpf, telefone, email, endereco;
    private String empresa, cnpj, enderecoEmpresa, cnae, descricao;
    private double bruto, inss, irrf, iss, liquido, deducoes, baseIrrf;
    private int dependentes;
    private double alicotaIss, inssJaRetido;

    private NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private String irrfAliquotaUsada = "Isento", irrfDeducaoUsada = "R$ 0,00";

    // CHAVE CENTRALIZADA PARA O BANCO E PARA O PDF
    private String hashValidacaoMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        // 1. Gera a chave de validação UMA única vez assim que a tela abre
        // CORREÇÃO: Remove os hífens primeiro, garantindo tamanho de 12 fixo sempre!
        hashValidacaoMobile = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Auditoria em Tempo Real");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Intent intent = getIntent();

        // RESGATE TRATADO COM .TRIM() PARA ELIMINAR ESPAÇOS ACIDENTAIS
        nome     = intent.getStringExtra("NOME");     if (nome != null) nome = nome.trim();
        cpf      = intent.getStringExtra("CPF");      if (cpf != null) cpf = cpf.trim();
        telefone = intent.getStringExtra("TELEFONE"); if (telefone != null) telefone = telefone.trim();
        email    = intent.getStringExtra("EMAIL");    if (email != null) email = email.trim();
        endereco = intent.getStringExtra("ENDERECO"); if (endereco != null) endereco = endereco.trim();

        empresa         = intent.getStringExtra("EMPRESA");          if (empresa != null) empresa = empresa.trim();
        cnpj            = intent.getStringExtra("CNPJ");             if (cnpj != null) cnpj = cnpj.trim();
        enderecoEmpresa = intent.getStringExtra("ENDERECO_EMPRESA"); if (enderecoEmpresa != null) enderecoEmpresa = enderecoEmpresa.trim();
        cnae            = intent.getStringExtra("CNAE");             if (cnae != null) cnae = cnae.trim();
        descricao       = intent.getStringExtra("DESCRICAO");        if (descricao != null) descricao = descricao.trim();

        // Valores numéricos (Não sofrem com espaços em branco)
        bruto        = intent.getDoubleExtra("BRUTO", 0);
        dependentes  = intent.getIntExtra("DEP", 0);
        alicotaIss   = intent.getDoubleExtra("ISS", 0);
        inssJaRetido = intent.getDoubleExtra("INSS_RETIDO", 0);

        processarTributos();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                byte[] pdfBytes = gerarBytesDoPdf();
                String base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP);
                enviarParaBancoDeDados(base64Pdf);
            } catch (Exception e) {
                Log.e("PDF_ERROR", "Erro ao gerar bytes", e);
            }
        });

        findViewById(R.id.btnGerarPdf).setOnClickListener(v -> {
            byte[] pdfBytes = gerarBytesDoPdf();
            salvarEAbrirPdfLocal(pdfBytes);
        });

        findViewById(R.id.btnVoltar).setOnClickListener(v -> {
            Intent reset = new Intent(this, SplashActivity.class);
            reset.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(reset);
        });
    }

    private void processarTributos() {
        double tetoMaximoInss = 856.46;
        inss = bruto * 0.11;
        boolean bateuTeto = false;

        if (inss + inssJaRetido > tetoMaximoInss) {
            inss = tetoMaximoInss - inssJaRetido;
            bateuTeto = true;
        }
        if (inss < 0) inss = 0;

        iss = bruto * (alicotaIss / 100);

        deducoes = dependentes * 189.59;
        baseIrrf = bruto - inss - deducoes;

        if (baseIrrf <= 2259.20) { irrf = 0; irrfAliquotaUsada = "Isento"; irrfDeducaoUsada = "R$ 0,00"; }
        else if (baseIrrf <= 2826.65) { irrf = (baseIrrf * 0.075) - 169.44; irrfAliquotaUsada = "7,5%"; irrfDeducaoUsada = "R$ 169,44"; }
        else if (baseIrrf <= 3751.05) { irrf = (baseIrrf * 0.15) - 381.44; irrfAliquotaUsada = "15,0%"; irrfDeducaoUsada = "R$ 381,44"; }
        else if (baseIrrf <= 4664.68) { irrf = (baseIrrf * 0.225) - 662.77; irrfAliquotaUsada = "22,5%"; irrfDeducaoUsada = "R$ 662,77"; }
        else { irrf = (baseIrrf * 0.275) - 896.00; irrfAliquotaUsada = "27,5%"; irrfDeducaoUsada = "R$ 896,00"; }
        if (irrf < 0) irrf = 0;

        liquido = bruto - inss - irrf - iss;

        ((TextView) findViewById(R.id.txtResumoBruto)).setText(nf.format(bruto));
        ((TextView) findViewById(R.id.txtResumoInss)).setText(nf.format(inss));
        ((TextView) findViewById(R.id.txtResumoIrrf)).setText(nf.format(irrf));
        ((TextView) findViewById(R.id.txtResumoIss)).setText(nf.format(iss));
        ((TextView) findViewById(R.id.txtLiquido)).setText(nf.format(liquido));

        String detalheInss = "Cálculo: 11% sobre R$ " + String.format(new Locale("pt","BR"), "%.2f", bruto);
        if(inssJaRetido > 0) detalheInss += " | Retido anteriormente: " + nf.format(inssJaRetido);
        if(bateuTeto) detalheInss += " | Ajustado ao teto legal";
        ((TextView) findViewById(R.id.txtDetalheInss)).setText(detalheInss);

        String detalheIrrf = "Base: " + nf.format(baseIrrf) + " | Alíquota: " + irrfAliquotaUsada + " | Parcela Deduzida: " + irrfDeducaoUsada;
        if(dependentes > 0) detalheIrrf += "\n(Redução de " + nf.format(deducoes) + " ref. a " + dependentes + " dependentes)";
        ((TextView) findViewById(R.id.txtDetalheIrrf)).setText(detalheIrrf);

        ((TextView) findViewById(R.id.txtDetalheIss)).setText("Cálculo: Alíquota municipal informada de " + alicotaIss + "%");
    }

    private void enviarParaBancoDeDados(String pdfEmBase64) {
        try {
            JSONObject json = new JSONObject();
            json.put("nome", nome != null ? nome : "");
            json.put("cpf", cpf != null ? cpf : "");
            json.put("email", email != null ? email : "");
            json.put("empresa", empresa != null ? empresa : "");
            json.put("cnpj", cnpj != null ? cnpj : "");
            json.put("endereco_empresa", enderecoEmpresa != null ? enderecoEmpresa : "");
            json.put("bruto", bruto);
            json.put("inss", inss);
            json.put("irrf", irrf);
            json.put("iss", iss);
            json.put("liquido", liquido);
            json.put("pdf_base64", pdfEmBase64);
            // ENVIA A CHAVE PARA O PHP GRAVAR NO BANCO DE DADOS
            json.put("chave_mobile", hashValidacaoMobile);

            URL url = new URL("https://contabil.nexusinnova.com.br/api_mobile_rpa.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) { response.append(line); }
                in.close();

                String mensagemLimpa = "Sincronizado com sucesso!";
                try {
                    JSONObject jsonResposta = new JSONObject(response.toString());
                    mensagemLimpa = jsonResposta.getString("mensagem");
                } catch (Exception e) {}

                final String msgFinal = mensagemLimpa;
                runOnUiThread(() -> Toast.makeText(this, msgFinal, Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Erro " + code + " ao guardar na nuvem.", Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            Log.e("API_ERRO", "Falha: " + e.getMessage());
        }
    }

    private byte[] gerarBytesDoPdf() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.logo_nexus);
            Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, 150, 60, false);
            canvas.drawBitmap(scaledBmp, 40, 30, paint);
        } catch (Exception e) {
            paint.setColor(Color.parseColor("#0a4f4f"));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(20f);
            canvas.drawText("NEXUS CONTÁBIL", 40, 60, paint);
        }

        paint.setColor(Color.parseColor("#0a4f4f"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(18f);
        canvas.drawText("RECIBO DE PAGAMENTO A AUTÔNOMO", 220, 50, paint);

        paint.setColor(Color.parseColor("#64748b"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(11f);
        canvas.drawText("Documento Gerado Eletronicamente - Emissão Mobile", 220, 68, paint);

        paint.setColor(Color.parseColor("#0a4f4f"));
        paint.setStrokeWidth(2f);
        canvas.drawLine(40, 100, 555, 100, paint);
        paint.setStrokeWidth(0f);

        paint.setColor(Color.BLACK);
        int y = 130;

        String cnaeFormatado = (cnae != null && !cnae.isEmpty()) ? cnae : "Não informado";
        if (cnaeFormatado.length() > 55) cnaeFormatado = cnaeFormatado.substring(0, 52) + "...";

        String descFormatada = (descricao != null && !descricao.isEmpty()) ? descricao : "Serviços técnicos prestados no mês de competência.";
        if (descFormatada.length() > 85) descFormatada = descFormatada.substring(0, 82) + "...";

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(12f);
        canvas.drawText("1. TOMADOR DO SERVIÇO (FONTE PAGADORA)", 40, y, paint); y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Razão Social: " + empresa, 40, y, paint); y += 18;
        canvas.drawText("CNPJ: " + cnpj + "    |    CNAE: " + cnaeFormatado, 40, y, paint); y += 18;
        canvas.drawText("Endereço: " + (enderecoEmpresa != null && !enderecoEmpresa.isEmpty() ? enderecoEmpresa : "Não informado"), 40, y, paint); y += 35;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("2. PROFISSIONAL AUTÔNOMO", 40, y, paint); y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Nome: " + nome, 40, y, paint); y += 18;
        canvas.drawText("CPF: " + cpf + "    |    Telefone: " + (telefone != null ? telefone : ""), 40, y, paint); y += 18;
        canvas.drawText("E-mail: " + (email != null ? email : ""), 40, y, paint); y += 18;
        canvas.drawText("Endereço: " + endereco, 40, y, paint); y += 35;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("3. ESPECIFICAÇÃO DOS SERVIÇOS TÉCNICOS", 40, y, paint); y += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(descFormatada, 40, y, paint); y += 45;

        paint.setColor(Color.parseColor("#f8fafc"));
        canvas.drawRect(35, y - 20, 560, y + 190, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#c8973a"));
        paint.setStrokeWidth(1.5f);
        canvas.drawRect(35, y - 20, 560, y + 190, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("I. VALOR BRUTO DO SERVIÇO", 50, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(nf.format(bruto), 550, y, paint); y += 30;
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("(-) Desconto INSS (Previdência Social)", 50, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(nf.format(inss), 550, y, paint); y += 15;
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setColor(Color.parseColor("#64748b")); paint.setTextSize(9f);
        canvas.drawText("Base 11%. " + (inssJaRetido > 0 ? "Retenção externa anterior: " + nf.format(inssJaRetido) : "") + " | Limitado ao teto.", 50, y, paint);
        paint.setColor(Color.BLACK); paint.setTextSize(12f); y += 25;

        canvas.drawText("(-) Desconto IRRF (Imposto de Renda)", 50, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(nf.format(irrf), 550, y, paint); y += 15;
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setColor(Color.parseColor("#64748b")); paint.setTextSize(9f);
        canvas.drawText("Base: " + nf.format(baseIrrf) + " | Alíquota ref.: " + irrfAliquotaUsada + " | Dedução legal: " + irrfDeducaoUsada, 50, y, paint);
        paint.setColor(Color.BLACK); paint.setTextSize(12f); y += 25;

        canvas.drawText("(-) Desconto ISS (Imposto Municipal)", 50, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(nf.format(iss), 550, y, paint); y += 15;
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setColor(Color.parseColor("#64748b")); paint.setTextSize(9f);
        canvas.drawText("Base: Valor Bruto | Alíquota aplicada: " + alicotaIss + "%", 50, y, paint);
        paint.setColor(Color.BLACK); paint.setTextSize(12f); y += 35;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(15f);
        paint.setColor(Color.parseColor("#0a4f4f"));
        canvas.drawText("II. VALOR LÍQUIDO A RECEBER", 50, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(nf.format(liquido), 550, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        y += 120;
        paint.setColor(Color.BLACK);
        paint.setTextSize(11f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        String dataAtual = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR")).format(new Date());
        canvas.drawText("Emitido eletronicamente em " + dataAtual, 40, y, paint);

        paint.setStrokeWidth(1f);
        canvas.drawLine(320, y - 5, 540, y - 5, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Assinatura do Profissional", 430, y + 10, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        y += 50;

        // URL CORRIGIDA PARA .PHP COM A CHAVE GERADA
        String urlValidacao = "https://contabil.nexusinnova.com.br/validar.php?doc=" + hashValidacaoMobile;

        paint.setColor(Color.parseColor("#64748b"));
        paint.setTextSize(9f);
        canvas.drawText("Chave de Validação Digital: " + hashValidacaoMobile, 40, y, paint);
        canvas.drawText("Para verificar a autenticidade, aponte a câmera para o QR Code ao lado.", 40, y + 12, paint);
        canvas.drawText("Ou acesse: contabil.nexusinnova.com.br/validar", 40, y + 24, paint);

        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(urlValidacao, BarcodeFormat.QR_CODE, 80, 80);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap qrBitmap = barcodeEncoder.createBitmap(bitMatrix);
            canvas.drawBitmap(qrBitmap, 460, y - 10, paint);
        } catch (Exception e) {
            Log.e("QR_CODE_ERROR", "Falha ao gerar o QR Code", e);
        }

        document.finishPage(page);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try { document.writeTo(bos); } catch (Exception e) {}
        document.close();

        return bos.toByteArray();
    }

    private void salvarEAbrirPdfLocal(byte[] pdfBytes) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String nomeArquivoSeguro = nome != null ? nome.replaceAll("[^a-zA-Z0-9]", "_") : "Usuario";
        File file = new File(dir, "RPA_Nexus_" + nomeArquivoSeguro + "_" + System.currentTimeMillis() + ".pdf");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(pdfBytes);
            fos.close();

            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
            Intent intentView = new Intent(Intent.ACTION_VIEW);
            intentView.setDataAndType(uri, "application/pdf");
            intentView.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intentView);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar localmente.", Toast.LENGTH_LONG).show();
        }
    }
}