#define BLYNK_TEMPLATE_ID "TMPL32kArDUb_"
#define BLYNK_TEMPLATE_NAME "Smart Plant Pot"
#define BLYNK_AUTH_TOKEN "z2x3YEN2HYd4tNH06tjZEiyp0BVQText"

#define BLYNK_PRINT Serial

#include <ESP8266WiFi.h>
#include <BlynkSimpleEsp8266.h>
#include <FirebaseESP8266.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>


// ---------------- Wi-Fi ----------------
const char* ssid = "Tejasi's A73";
const char* password = "uwrs7483";

// ---------------- Firebase ----------------
#define DATABASE_URL "https://plantpal-f-default-rtdb.asia-southeast1.firebasedatabase.app"
#define WEB_API_KEY "AIzaSyDCpO0sk6o0a6tnUHoV4YM9BR8eO7xnmlU"

FirebaseData fbData;
FirebaseAuth auth;
FirebaseConfig config;

// ---------------- MUX & Sensors ----------------
#define S0 D1
#define S1 D2
#define S2 D5
#define S3 D6
#define MUX_SIG A0

#define UV_CHANNEL 0
#define SOIL1_CHANNEL 1
#define SOIL2_CHANNEL 2

#define DHTPIN D3
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);

#define ONE_WIRE_BUS D4
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature ds18b20(&oneWire);

BlynkTimer timer;

// ---------------- Multiplexer Functions ----------------
void setMuxChannel(byte channel) {
  digitalWrite(S0, channel & 1);
  digitalWrite(S1, (channel >> 1) & 1);
  digitalWrite(S2, (channel >> 2) & 1);
  digitalWrite(S3, (channel >> 3) & 1);
  delay(5);
}

int readAnalogMux(byte channel) {
  setMuxChannel(channel);
  return analogRead(MUX_SIG);
}

// ---------------- Main Sensor Function ----------------
void sendSensorData() {
  int soil1 = readAnalogMux(SOIL1_CHANNEL);
  int soil1Percent = map(soil1, 1023, 300, 0, 100);
  soil1Percent = constrain(soil1Percent, 0, 100);
  Blynk.virtualWrite(V4, soil1Percent);

  int soil2 = readAnalogMux(SOIL2_CHANNEL);
  int soil2Percent = map(soil2, 1023, 300, 0, 100);
  soil2Percent = constrain(soil2Percent, 0, 100);
  Blynk.virtualWrite(V5, soil2Percent);

  int uv = readAnalogMux(UV_CHANNEL);
  Blynk.virtualWrite(V3, uv);

  float airTemp = dht.readTemperature();
  float airHum = dht.readHumidity();

  if (!isnan(airTemp) && !isnan(airHum)) {
    airTemp -= 10.0;
    Blynk.virtualWrite(V1, airTemp);
    Blynk.virtualWrite(V2, airHum);
  }

  ds18b20.requestTemperatures();
  float soilTemp = ds18b20.getTempCByIndex(0);
  if (soilTemp != DEVICE_DISCONNECTED_C) {
    Blynk.virtualWrite(V0, soilTemp);
  }

  // ---- Serial Output ----
  Serial.println("\n---- Sensor Readings ----");
  Serial.printf("Soil Moisture 1: %d%%, Soil Moisture 2: %d%%\n", soil1Percent, soil2Percent);
  Serial.printf("UV: %d\n", uv);
  Serial.printf("Air Temp: %.1f°C, Humidity: %.1f%%\n", airTemp, airHum);
  Serial.printf("Soil Temp: %.1f°C\n", soilTemp);

  // ---- Send to Firebase ----
  if (Firebase.ready()) {
    FirebaseJson json;
    json.set("soilMoisture1", soil1Percent);
    json.set("soilMoisture2", soil2Percent);
    json.set("uv", uv);
    json.set("airTemperature", airTemp);
    json.set("humidity", airHum);
    json.set("soilTemperature", soilTemp);
    json.set("timestamp", millis());

    if (Firebase.setJSON(fbData, "/sensorData", json)) {
      Serial.println("Sent to Firebase successfully!");
    } else {
      Serial.printf("Firebase Error: %s\n", fbData.errorReason().c_str());
    }
  } else {
    Serial.println("⚠ Firebase not ready");
  }
}

// ---------------- Setup ----------------
void setup() {
  Serial.begin(115200);

  pinMode(S0, OUTPUT);
  pinMode(S1, OUTPUT);
  pinMode(S2, OUTPUT);
  pinMode(S3, OUTPUT);

  dht.begin();
  ds18b20.begin();

  // Wi-Fi
  Serial.print("Connecting to Wi-Fi");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" connected!");

  // Firebase Setup
  config.api_key = WEB_API_KEY;
  config.database_url = DATABASE_URL;

  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("Firebase SignUp success");
  } else {
    Serial.printf("Firebase SignUp failed: %s\n", config.signer.signupError.message.c_str());
  }

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Blynk
  Blynk.begin(BLYNK_AUTH_TOKEN, ssid, password);

  timer.setInterval(10000L, sendSensorData);
}

// ---------------- Loop ----------------
void loop() {
  Blynk.run();
  timer.run();
}