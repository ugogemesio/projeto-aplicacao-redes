#include <WiFi.h>
#include "DHT.h"

const char* ssid = "dlink-EA1C";
const char* password = "clzub24830";

const char* host = "192.168.100.149";
const uint16_t port = 5000;

#define DHTPIN 23
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

WiFiClient client;
unsigned long interval = 2000;
unsigned long lastSend = 0;

String ipOrigem = "";

// Histórico de RTTs para calcular jitter
#define MAX_RTT_HISTORY 10
unsigned long rttHistory[MAX_RTT_HISTORY] = {0};
int rttIndex = 0;
int rttCount = 0;
unsigned long lastRTT = 0;

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

void setup() {
  Serial.begin(115200);
  dht.begin();

  Serial.println("Conectando ao WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }

  Serial.println("\nWiFi conectado!");
  ipOrigem = WiFi.localIP().toString();
  Serial.println("IP do ESP32 = " + ipOrigem);
  Serial.println("MAC Address = " + WiFi.macAddress());
}

void loop() {
  if (!ensureConnection()) { delay(3000); return; }

  if (millis() - lastSend >= interval) {
    float t, h;
    if (!readSensor(t, h)) { Serial.println("Falha ao ler DHT11!"); return; }

    unsigned long tempoEnvio = millis();
    String payload = String(t, 1) + "," +
                     String(h, 1) + "," +
                     ipOrigem + "," +
                     String(tempoEnvio) + "," +
                     String(lastRTT) + "," +
                     String(calculateJitter()) + "\n";

    Serial.println("Enviando -> " + payload);
    client.println(payload);

    unsigned long timeout = millis();
    while (!client.available()) {
      if (millis() - timeout > 1000) { lastRTT = 0; break; }
    }

    if (client.available()) {
      String ack = client.readStringUntil('\n');
      lastRTT = millis() - tempoEnvio;

      // Atualiza histórico RTT
      rttHistory[rttIndex] = lastRTT;
      rttIndex = (rttIndex + 1) % MAX_RTT_HISTORY;
      if (rttCount < MAX_RTT_HISTORY) rttCount++;

      Serial.println("ACK do servidor: " + ack);
      Serial.println("📡 RTT = " + String(lastRTT) + " ms");
      Serial.println("📊 Jitter = " + String(calculateJitter()) + " ms");
    }

    lastSend = millis();
  }
}
