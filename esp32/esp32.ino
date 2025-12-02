#include <WiFi.h>
#include "DHT.h"

// ====================== CONFIG WI-FI ======================
const char* ssid = "dlink-EA1C";
const char* password = "clzub24830";

// Servidor TCP
const char* host = "192.168.100.149";
const uint16_t port = 5000;

// ====================== CONFIG DHT ======================
#define DHTPIN 23
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ====================== Variáveis ======================
WiFiClient client;
unsigned long interval = 2000; // Envio a cada 2s
unsigned long lastSend = 0;
unsigned long lastRTT = 0;  // ✅ Armazena o último RTT calculado

String ipOrigem = "";

// ====================== Funções ======================
bool ensureConnection() {
  if (!client.connected()) {
    Serial.println("Reconectando ao servidor...");
    if (!client.connect(host, port)) {
      Serial.println("Erro: servidor indisponível!");
      return false;
    }
    Serial.println("Conectado ao servidor TCP!");
  }
  return true;
}

bool readSensor(float &t, float &h) {
  t = dht.readTemperature();
  h = dht.readHumidity();

  if (!isnan(t) && !isnan(h)) return true;

  // Tentativas adicionais caso falhe
  for (int i = 0; i < 3; i++) {
    delay(100);
    t = dht.readTemperature();
    h = dht.readHumidity();
    if (!isnan(t) && !isnan(h)) return true;
  }

  return false;
}

// ====================== SETUP ======================
void setup() {
  Serial.begin(115200);
  dht.begin();

  Serial.println("Conectando ao WiFi...");
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi conectado!");
  ipOrigem = WiFi.localIP().toString();
  Serial.println("IP do ESP32 = " + ipOrigem);
  Serial.println("MAC Address = " + WiFi.macAddress());
}

// ====================== LOOP ======================
void loop() {
  // Adicionar no ESP32:
  Serial.print("Gateway IP: ");
  Serial.println(WiFi.gatewayIP());
  Serial.print("DNS IP: ");
  Serial.println(WiFi.dnsIP());
  Serial.print("Subnet Mask: ");
  Serial.println(WiFi.subnetMask());
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

    // ✅ AQUI É ONDE VOCÊ DEVE COLOCAR O CÓDIGO
    // 1. Primeiro marca o tempo de envio
    unsigned long tempoEnvio = millis();
    
    // 2. Monta a mensagem com timestamp de envio (tempoEnvio) e o RTT anterior (lastRTT)
    String payload = String(t, 1) + "," +
                     String(h, 1) + "," +
                     ipOrigem + "," +
                     String(tempoEnvio) + "," +  // Timestamp de envio
                     String(lastRTT) + "\n";     // RTT da mensagem anterior

    Serial.println("Enviando -> " + payload);

    // 3. Envia a mensagem
    client.println(payload);

    // 4. Aguarda resposta do servidor para calcular RTT atual
    unsigned long timeout = millis();
    while (!client.available()) {
      if (millis() - timeout > 1000) { // timeout de 1s
        Serial.println("Sem resposta do servidor (RTT Timeout)");
        lastRTT = 0;  // Timeout significa RTT muito alto ou perda
        break;
      }
    }

    // 5. Se recebeu resposta, calcula o RTT
    if (client.available()) {
      String ack = client.readStringUntil('\n');
      lastRTT = millis() - tempoEnvio;  // Calcula RTT desta mensagem
      Serial.println("ACK do servidor: " + ack);
      Serial.println("📡 RTT = " + String(lastRTT) + " ms");
    }

    lastSend = millis();
  }

  
}
