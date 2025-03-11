  GNU nano 7.2                                                                                      CombinedSensors.ino
#include <Wire.h>
#include <SparkFun_Bio_Sensor_Hub_Library.h>
#include <SparkFun_TMP117.h>  // TMP117 temperature sensor

// ===========================
// ECG Sensor Settings
// ===========================
const int ecgPin = A0;
const int loPlus = 10;
const int loMinus = 11;

// ===========================
// PPG Sensor Settings
// ===========================
#define RESET_PIN 4
#define MFIO_PIN  5

SparkFun_Bio_Sensor_Hub bioHub(RESET_PIN, MFIO_PIN);
bioData body;

// ===========================
// Temperature Sensor Settings
// ===========================
TMP117 tempSensor;

// ===========================
// Buzzer Settings
// ===========================
const int buzzerPin = 7;           // Digital pin for the buzzer

// Temperature thresholds (in °C)
const float TEMP_LOW_THRESHOLD  = 36.0;
const float TEMP_HIGH_THRESHOLD = 37.8;

// Buzzer trigger interval (once every 30 seconds)
const unsigned long BUZZER_INTERVAL = 30000; // 30,000 milliseconds
unsigned long previousBuzzerMillis = 0;

// ===========================
// Timing Variables
// ===========================
unsigned long previousEcgMillis = 0;
unsigned long previousPpgMillis = 0;
unsigned long previousTempMillis = 0;
const unsigned long ecgInterval = 10;    // 10ms = 100 Hz
const unsigned long ppgInterval = 40;      // 40ms = 25 Hz
const unsigned long tempInterval = 1000;   // 1000ms = 1 Hz

// Function to trigger a brief beep
void beepBuzzer() {
  digitalWrite(buzzerPin, HIGH);
  delay(500); // Buzzer sounds for 500ms
  digitalWrite(buzzerPin, LOW);
}

void setup() {
  Serial.begin(115200);
  Wire.begin();

  pinMode(ecgPin, INPUT);
  pinMode(loPlus, INPUT);
pinMode(loMinus, INPUT);

  // Initialize the buzzer pin
  pinMode(buzzerPin, OUTPUT);
  digitalWrite(buzzerPin, LOW);  // Ensure buzzer is off initially

  // Initialize PPG sensor
  if (bioHub.begin() != 0) {
    Serial.println("ERROR: PPG sensor initialization failed.");
    while (1);
  }
  bioHub.configBpm(MODE_ONE);

  // Initialize Temperature Sensor
  if (!tempSensor.begin()) {
    Serial.println("ERROR: TMP117 sensor not found!");
    while (1);
  }

  Serial.println("Sensors initialized successfully.");
  delay(1000); // Allow sensors to settle
}

void loop() {
  unsigned long currentMillis = millis();

  // ----- ECG Sampling (100 Hz) -----
  if (currentMillis - previousEcgMillis >= ecgInterval) {
    previousEcgMillis = currentMillis;
    int ecgSignal = analogRead(ecgPin);

    Serial.print("ECG,");
    Serial.print(currentMillis);
    Serial.print(",");
    if (ecgSignal > 0) {
      Serial.println(ecgSignal);
    } else {
      Serial.println("N/A");
    }
  }

  // ----- PPG Sampling (25 Hz) -----
  if (currentMillis - previousPpgMillis >= ppgInterval) {
    previousPpgMillis = currentMillis;
    body = bioHub.readBpm();

    Serial.print("PPG,");
    Serial.print(currentMillis);
    Serial.print(",");
    Serial.print(body.heartRate);
    Serial.print(",");
    Serial.print(body.confidence);
    Serial.print(",");
    Serial.print(body.oxygen);
    Serial.print(",");
    Serial.println(body.status);
  }

  // ----- Temperature Sampling (1 Hz) -----
  if (currentMillis - previousTempMillis >= tempInterval) {
    previousTempMillis = currentMillis;
float temperature = tempSensor.readTempC();

    Serial.print("TEMP,");
    Serial.print(currentMillis);
    Serial.print(",");
    Serial.println(temperature, 2); // Print with 2 decimal places

    // Check if temperature is out of the normal body range
    if (temperature < TEMP_LOW_THRESHOLD || temperature > TEMP_HIGH_THRESHOLD) {
      // Check if 30 seconds have passed since the last buzzer trigger
      if (currentMillis - previousBuzzerMillis >= BUZZER_INTERVAL) {
        previousBuzzerMillis = currentMillis;
        beepBuzzer();  // Trigger the buzzer for 500ms
      }
    }
  }
}


