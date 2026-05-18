# 📱 Nexus Calculadora RPA

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![PHP](https://img.shields.io/badge/PHP-777BB4?style=for-the-badge&logo=php&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00758F?style=for-the-badge&logo=mysql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Conclu%C3%ADdo-brightgreen?style=for-the-badge)

A **Nexus Calculadora RPA** é um ecossistema ERP completo projetado para simplificar, auditar e automatizar a emissão de Recibos de Pagamento a Autônomo (RPA). O sistema une a agilidade de um aplicativo nativo Android com o rigor de uma API em nuvem e um portal web integrado para verificação de documentos em tempo real.

---

## 🚀 Funcionalidades Principais

- **Cálculo Tributário Automatizado (2026):** Motor de cálculo nativo atualizado com as tabelas progressivas e diretrizes fiscais brasileiras para o ano-calendário de 2026.
- **Prevenção de Bitributação (INSS):** Inteligência para detectar valores de INSS já retidos anteriormente em outras fontes, limitando a retenção rigorosamente ao teto legal da Previdência Social.
- **Emissão de PDF Premium:** Geração de recibos em formato A4 diretamente pelo dispositivo móvel, com design corporativo de alta fidelidade e alinhamento contábil dos valores à direita.
- **Assinatura e Validação por QR Code:** Inclusão automática de uma Chave de Validação Digital única e um QR Code de alta resolução para checagem instantânea de autenticidade.
- **Sincronização Híbrida em Nuvem:** Envio invisível e otimizado dos pacotes de dados para o servidor central através de requisições assíncronas em segundo plano.
- **Notificação por E-mail Corporativo:** Disparo automatizado de e-mails para o profissional com um template HTML responsivo e elegante contendo o PDF oficial em anexo.
- **Interface Responsiva (Anti-Teclado):** Layout fluido estruturado em contêineres roláveis que se ajustam automaticamente ao teclado virtual, oferecendo usabilidade fluida e profissional.

---

## 🛠️ Arquitetura do Sistema

O projeto funciona como uma engrenagem integrada dividida em três pilares principais:


```

[ Aplicativo Mobile ] ──( JSON + Base64 )──> [ API PHP / PHPMailer ]
│                                             │
(Gera QR Code)                                  (Grava Dados)
│                                             ▼
└─────────> [ Portal Web de Validação ] <─── [ MySQL ]

```

1. **Client Mobile (Android Studio / Java):** Interface nativa onde ocorrem as validações de input (`.trim()`), o motor contábil, a compilação do documento em memória e a codificação do PDF em strings Base64 de alta segurança para transmissão.
2. **Backend API (`api_mobile_rpa.php`):** Script em PHP responsável por limpar máscaras de dados, gerenciar o relacionamento entre tabelas de Prestadores e Tomadores no banco de dados, reconstruir o binário do PDF e gerenciar o disparo SMTP autenticado via PHPMailer.
3. **Portal de Autenticidade (`validar.php`):** Página web segura e otimizada que recebe requisições de validação (Web e Mobile) para atestar a integridade do recibo e exibir o status do documento em tempo real.

---

## 📊 Conformidade Fiscal (Regras de Cálculo)

O motor de auditoria embutido na `ResultadoActivity.java` processa os seguintes pilares tributários:

- **INSS (Contribuição Individual):** Retenção de 11% sobre o valor bruto do serviço, com trava automática baseada na soma aritmética do teto de previdência máxima vigente (R$ 856,46).
- **IRRF (Tabela Progressiva):** Dedução legal exata de R$ 189,59 por dependente sobre a base de cálculo e enquadramento automático nas alíquotas oficiais de isenção até a faixa máxima de 27,5% (com dedução de R$ 896,00).
- **ISS (Imposto Municipal):** Retenção com alíquotas variáveis por município respeitando as margens constitucionais brasileiras (mínimo de 2% e máximo de 5%).

---

## 📦 Dependências e Tecnologias

### Mobile (Android)
- **Mínimo SDK:** API 21 (Android 5.0)
- **Target SDK:** API 34 (Android 14)
- **Geração de QR Code:** [ZXing Core (3.4.1)](https://github.com/zxing/zxing) & [ZXing Android Embedded (4.3.0)](https://github.com/journeyapps/zxing-android-embedded)
- **Componentes:** `PdfDocument`, `Canvas`, `FileProvider`, `MultiFormatWriter`, `Executors`

### Servidor (Web)
- **PHP:** Versão 7.4 ou superior
- **Banco de Dados:** MySQL / MariaDB (com suporte a conexões PDO)
- **Serviço de Correio:** [PHPMailer (v6.x)](https://github.com/PHPMailer/PHPMailer) via SMTP Seguro (Porta 465)

---

## 📂 Estrutura de Banco de Dados Relacional

A persistência de dados foi modelada para suportar a integridade referencial através das tabelas:

- **`tomadores_empresas`**: Armazena os dados cadastrais das fontes pagadoras através de chaves únicas de CNPJ.
- **`prestadores_autonomos`**: Registra o cadastro unificado de profissionais através de chaves únicas de CPF.
- **`rpa_emissões`**: Armazena o histórico consolidado de auditorias financeiras, competências mensais e a coluna `chave_mobile (VARCHAR 20)` responsável pela validação híbrida cruzada com o aplicativo.

---

## 🎨 Identidade Visual (UI/UX)

O ecossistema adota a paleta premium corporativa da **Nexus Contábil**:
- 🟢 **Verde Oficial:** `#0a4f4f` (Usado em cabeçalhos, botões principais e backgrounds corporativos).
- 🟡 **Ouro Metálico:** `#c8973a` (Usado em bordas financeiras, links, e destaques de valores).
- ⚪ **Fundo Claro:** `#f8fafc` (Garante contraste e conforto visual em conformidade com as diretrizes do Material Design).

---

## 🧑‍💻 Equipe de Desenvolvimento

O projeto foi idealizado e implementado colaborativamente pelos desenvolvedores:

- **Erivania Ferreira Dias**
- **Felipe Silva Alvim**
- **Ismael Oliveira Silva**
- **Sarah Ribeiro Marques**

*Engenharia de Software & Soluções de TI com foco em automação RPA, conformidade fiscal e performance de negócios.*

---
*Documento emitido e protegido de acordo com os padrões de segurança lógica de software. Direitos Reservados &copy; 2026.*

```
