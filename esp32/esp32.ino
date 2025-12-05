#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>
#include "DHT.h"

#define DHTPIN 23
#define DHTTYPE DHT11

Preferences prefs;
WebServer server(80);
DHT dht(DHTPIN, DHTTYPE);

// -------- CONFIGURAÇÕES DO BACKEND --------
// ATENÇÃO: Altere estes IPs para os do seu computador
const char* backendRedirect = "http://192.168.100.149:8080/dht";  // Dashboard web
const char* tcpHost = "192.168.100.149";  // Servidor TCP
const uint16_t tcpPort = 5000;

WiFiClient tcpClient;
unsigned long interval = 2000;
unsigned long lastSend = 0;

// Histórico de RTTs para calcular jitter
#define MAX_RTT_HISTORY 10
unsigned long rttHistory[MAX_RTT_HISTORY] = {0};
int rttIndex = 0;
int rttCount = 0;
unsigned long lastRTT = 0;

String ipOrigem = "";

// Flags
bool configMode = true;  // true = modo configuração (AP), false = modo normal

// ------------------------------------------------------
//  Tenta conectar a WiFi salvo
// ------------------------------------------------------
bool tryConnectSavedWiFi() {
  String ssid = prefs.getString("ssid", "");
  String pass = prefs.getString("pass", "");

  if (ssid == "") {
    Serial.println("Nenhuma rede WiFi salva.");
    return false;
  }

  Serial.print("Conectando a WiFi salva: ");
  Serial.println(ssid);
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid.c_str(), pass.c_str());

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 10000) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    ipOrigem = WiFi.localIP().toString();
    Serial.print("Conectado! IP: ");
    Serial.println(ipOrigem);
    return true;
  } else {
    Serial.println("Falha na conexão WiFi.");
    return false;
  }
}

// ------------------------------------------------------
//  Modo AP para configuração
// ------------------------------------------------------
void startConfigMode() {
  configMode = true;
  Serial.println("\n=== MODO CONFIGURAÇÃO ===");

  // Desconecta do WiFi station
  WiFi.disconnect(true);
  delay(100);
  
  // Inicia Access Point
  WiFi.mode(WIFI_AP);
  WiFi.softAP("ESP32-Config", "config123");
  
  IPAddress apIP = WiFi.softAPIP();
  Serial.println("AP 'ESP32-Config' criado!");
  Serial.println("Senha: config123");
  Serial.print("Acesse: http://");
  Serial.println(apIP);

  // Rotas do servidor web de configuração
  server.on("/", HTTP_GET, []() {
    int n = WiFi.scanNetworks();  // Scan síncrono
    String html = R"rawliteral(
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <style>
        body { font-family: Arial; padding: 20px; }
        select, input { width: 100%; padding: 10px; margin: 10px 0; }
        button { background: #4CAF50; color: white; padding: 15px; border: none; width: 100%; }
        .info { background: #e7f3fe; padding: 10px; border-left: 6px solid #2196F3; }
      </style>
    </head>
    <body>
      <h2>Configurar WiFi do ESP32</h2>
      <div class="info">
        Conecte-se à rede "ESP32-Config"<br>
        Senha: config123
      </div>
      <form action="/save" method="POST">
        <label>Rede WiFi:</label><br>
        <select name="ssid" required>
          <option value="">-- Selecione --</option>
    )rawliteral";
    
    if (n > 0) {
      for (int i = 0; i < n; i++) {
        html += "<option value='" + WiFi.SSID(i) + "'>" + WiFi.SSID(i) + " (" + WiFi.RSSI(i) + "dBm)</option>";
      }
    }
    
    html += R"rawliteral(
        </select><br>
        <label>Senha:</label><br>
        <input type="password" name="pass" placeholder="Senha da rede WiFi"><br><br>
        <button type="submit">Salvar e Conectar</button>
      </form>
      <p><a href="/">Atualizar lista</a></p>
    </body>
    </html>
    )rawliteral";
    
  server.send(200, "text/html; charset=utf-8", html);
  });

  server.on("/save", HTTP_POST, []() {
    String ssid = server.arg("ssid");
    String pass = server.arg("pass");
    
    if (ssid.length() == 0) {
      server.send(400, "text/html; charset=utf-8", "<h1>Selecione uma rede!</h1>");
      return;
    }
    
    Serial.println("Salvando WiFi: " + ssid);
    prefs.putString("ssid", ssid);
    prefs.putString("pass", pass);
    
    server.send(200, "text/html; charset=utf-8", 
      "<h1>Configurações salvas!</h1><p>Reiniciando para conectar...</p>"
      "<script>setTimeout(() => location.href='/', 3000)</script>");
    
    delay(2000);
    ESP.restart();
  });

  server.begin();
}

// ------------------------------------------------------
//  Modo normal (WiFi conectado)
// ------------------------------------------------------
void startNormalMode() {
  configMode = false;
  
  // Rotas do servidor web
  server.on("/", HTTP_GET, []() {
    // Redireciona para o backend
    String html = "<!DOCTYPE html><html><head>";
    html += "<meta http-equiv='refresh' content='0; url=" + String(backendRedirect) + "'>";
    html += "<style>body {font-family: Arial; text-align: center; padding: 50px;}</style>";
    html += "</head><body>";
    html += "<h2>ESP32 Conectado</h2>";
    html += "<p>Redirecionando para o dashboard...</p>";
    html += "<p>IP Local: " + ipOrigem + "</p>";
    html += "<p><a href='/info'>Informações do Sistema</a></p>";
    html += "<p><a href='/config'>Redefinir WiFi</a></p>";
    html += "</body></html>";
    
    server.send(200, "text/html; charset=utf-8", html);
  });
  
  server.on("/info", HTTP_GET, []() {
    String html = "<!DOCTYPE html><html><head><style>";
    html += "body {font-family: Arial; padding: 20px;}";
    html += "table {border-collapse: collapse; width: 100%;}";
    html += "td, th {border: 1px solid #ddd; padding: 8px;}";
    html += "tr:nth-child(even){background-color: #f2f2f2;}";
    html += "</style></head><body>";
    html += "<h2>Informações do ESP32</h2>";
    html += "<table>";
    html += "<tr><th>Item</th><th>Valor</th></tr>";
    html += "<tr><td>IP Local</td><td>" + ipOrigem + "</td></tr>";
    html += "<tr><td>MAC Address</td><td>" + WiFi.macAddress() + "</td></tr>";
    html += "<tr><td>Rede WiFi</td><td>" + WiFi.SSID() + "</td></tr>";
    html += "<tr><td>Sinal (RSSI)</td><td>" + String(WiFi.RSSI()) + " dBm</td></tr>";
    html += "<tr><td>Servidor TCP</td><td>" + String(tcpHost) + ":" + String(tcpPort) + "</td></tr>";
    html += "<tr><td>Backend Web</td><td>" + String(backendRedirect) + "</td></tr>";
    html += "</table>";
    html += "<p><a href='/'>Voltar</a></p>";
    html += "</body></html>";
    
    server.send(200, "text/html; charset=utf-8", html);
  });
  
  server.on("/config", HTTP_GET, []() {
    prefs.clear();
    server.send(200, "text/html; charset=utf-8", 
      "<h1>Configurações apagadas!</h1><p>Reiniciando em modo configuração...</p>");
    delay(1000);
    ESP.restart();
  });
  
  server.begin();
  Serial.println("\n=== MODO NORMAL ATIVADO ===");
  Serial.println("Servidor HTTP iniciado na porta 80");
  Serial.println("Acesse: http://" + ipOrigem);
  Serial.print("Conectado a: ");
  Serial.println(WiFi.SSID());
  Serial.print("Enviando dados para TCP: ");
  Serial.print(tcpHost);
  Serial.print(":");
  Serial.println(tcpPort);
}

// ------------------------------------------------------
//  Funções TCP 
// ------------------------------------------------------
bool ensureConnection() {
  if (!tcpClient.connected()) {
    Serial.print("Conectando ao servidor TCP... ");
    if (!tcpClient.connect(tcpHost, tcpPort)) {
      Serial.println("falhou!");
      return false;
    }
    Serial.println("conectado!");
  }
  return true;
}

bool readSensor(float &t, float &h) {
  t = dht.readTemperature();
  h = dht.readHumidity();
  if (!isnan(t) && !isnan(h)) return true;

  for (int i = 0; i < 3; i++) {
    delay(100);
    t = dht.readTemperature();
    h = dht.readHumidity();
    if (!isnan(t) && !isnan(h)) return true;
  }
  return false;
}

unsigned long calculateJitter() {
  if (rttCount < 2) return 0;
  unsigned long totalDiff = 0;
  for (int i = 1; i < rttCount; i++) {
    totalDiff += abs((long)(rttHistory[i] - rttHistory[i - 1]));
  }
  return totalDiff / (rttCount - 1);
}

// ------------------------------------------------------
//  SETUP
// ------------------------------------------------------
void setup() {
  Serial.begin(115200);
  delay(1000);
  bool resetPrefs = false;  // Mude para false após o teste
  
  if (resetPrefs) {
    Serial.println("RESETANDO PREFERÊNCIAS...");
    Preferences prefsTemp;
    prefsTemp.begin("wifi", false);
    prefsTemp.clear();
    prefsTemp.end();
    Serial.println("Preferências apagadas!");
    delay(1000);
  }
  Serial.println("\n=== ESP32 IoT Gateway ===");
  Serial.println("Inicializando...");
  
  // Inicializa sensor
  dht.begin();
  Serial.println("Sensor DHT11 inicializado");
  
  // Inicializa armazenamento
  prefs.begin("wifi", false);
  Serial.println("Sistema de configuração pronto");
  
  // Tenta conectar ao WiFi salvo
  if (tryConnectSavedWiFi()) {
    startNormalMode();
  } else {
    startConfigMode();
  }
  
  Serial.println("\nSistema pronto!");
}

// ------------------------------------------------------
//  LOOP
// ------------------------------------------------------
void loop() {
  // Sempre processa requisições HTTP
  server.handleClient();
  
  // Se estiver em modo configuração, apenas processa HTTP
  if (configMode) return;
  
  // Verifica se WiFi ainda está conectado
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi desconectado! Tentando reconectar...");
    delay(2000);
    ESP.restart();  // Reinicia para tentar reconectar
    return;
  }
  
  // =====================================================
  // CÓDIGO TCP 
  // =====================================================
  if (!ensureConnection()) { 
    delay(3000); 
    return; 
  }

  
  if (millis() - lastSend >= interval) {
      float t, h;
      if (!readSensor(t, h)) {
          Serial.println("Falha ao ler DHT11!");
          return;
      }
  
      // ---- 1. CALCULA RTT EM MICROSSEGUNDOS
      unsigned long startPing = micros(); 
      tcpClient.println("PING");
  
      unsigned long timeout = micros();
      bool ackReceived = false;
  
      while ((micros() - timeout) < 1000000UL) { // timeout 1s em micros
          if (tcpClient.available()) {
              String ack = tcpClient.readStringUntil('\n'); 
              lastRTT = micros() - startPing; // RTT em micros
              ackReceived = true;
  
              if (lastRTT > 0) {
                  rttHistory[rttIndex] = lastRTT;
                  rttIndex = (rttIndex + 1) % MAX_RTT_HISTORY;
                  if (rttCount < MAX_RTT_HISTORY) rttCount++;
              }
              break;
          }
      }
  
      if (!ackReceived) {
          lastRTT = 0;
          Serial.println("PING timeout!");
      }
  
      // ---- 2. CALCULA JITTER EM MICROSEGUNDOS ----
      unsigned long jitter = 0;
      if (rttCount >= 2) {
          unsigned long totalDiff = 0;
          int validCount = 0;
          for (int i = 1; i < rttCount; i++) {
              if (rttHistory[i] > 0 && rttHistory[i - 1] > 0) {
                  totalDiff += abs((long)(rttHistory[i] - rttHistory[i - 1]));
                  validCount++;
              }
          }
          if (validCount > 0) jitter = totalDiff / validCount;
      }
  
      // ---- 3. ENVIA PACOTE COMPLETO COM RTT E JITTER EM MICROS ----
      unsigned long timestamp = micros();
      char payload[128];
      snprintf(payload, sizeof(payload), "%.1f,%.1f,%s,%lu,%lu,%lu",
               t, h, ipOrigem.c_str(), timestamp, lastRTT, jitter);
  
      Serial.println("Enviando dados -> " + String(payload));
      tcpClient.println(payload);
  
      lastSend = millis();
  }

}