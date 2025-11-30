#include <WiFi.h>

const char* ssid = "SSID_SEU";
const char* password = "SENHA_SUA";

const char* host = "IP_SUA_MAQUINA";
const uint16_t port = 5000;           

#include "DHT.h"
#define DHTPIN 23
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

WiFiClient client;

unsigned long interval = 1000; // padrão 5s
unsigned long lastSend = 0;

void setup() {
  Serial.begin(115200);
  dht.begin();

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi conectado!");
}

void loop() {
  if (!client.connected()) {
    Serial.println("Conectando ao servidor TCP...");
    if (client.connect(host, port)) {
      Serial.println("Conectado ao servidor!");
    } else {
      Serial.println("Falha na conexão. Tentando novamente em 5s...");
      delay(1000);
      return;
    }
  }

  if (millis() - lastSend >= interval) {
    float t = dht.readTemperature();
    float h = dht.readHumidity();

    if (!isnan(t) && !isnan(h)) {
      String data = String(t) + "," + String(h) + ",ESP32\n";
      client.print(data); // envia TCP
      Serial.println("Enviado: " + data);
    }

    lastSend = millis();
  }
}
