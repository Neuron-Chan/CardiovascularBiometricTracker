import time
import sqlite3
import threading
import numpy as np
from ads1015 import ADS1015
import RPi.GPIO as GPIO

# GPIO Setup for Buzzer
BUZZER_PIN = 18
GPIO.setmode(GPIO.BCM)
GPIO.setup(BUZZER_PIN, GPIO.OUT)
GPIO.output(BUZZER_PIN, GPIO.LOW)

# Initialize ADC
ads = ADS1015()
ads.set_mode_continuous()
ads.set_data_rate(3300)

# Database Setup
conn = sqlite3.connect("ecg_data.db", check_same_thread=False)
cursor = conn.cursor()
cursor.execute("""
    CREATE TABLE IF NOT EXISTS ecg_data (
        timestamp TEXT,
        raw_voltage REAL,
        filtered_voltage REAL
    )
""")
conn.commit()

# Global Variables
BUFFER_SIZE = 1000
SAMPLE_RATE = 490  # Hz
BUZZER_THRESHOLD = 0.3  # Adjusted threshold
raw_data = []
filtered_data = []
buffer_lock = threading.Lock()

# Bandpass Filter Configuration
def bandpass_filter(data, lowcut, highcut, fs, order=2):
    from scipy.signal import butter, lfilter
    nyquist = 0.5 * fs
    low = lowcut / nyquist
    high = highcut / nyquist
    b, a = butter(order, [low, high], btype="band")
    return lfilter(b, a, data)

# Read Data from ADC
def read_adc():
    global raw_data
    while True:
        start_time = time.time()
        raw_voltage = ads.read_voltage(channel=0)
        with buffer_lock:
            raw_data.append(raw_voltage)
            if len(raw_data) > BUFFER_SIZE:
                raw_data.pop(0)
        time.sleep(max(0, (1 / SAMPLE_RATE) - (time.time() - start_time)))

# Filter Data
def filter_data():
    global raw_data, filtered_data
    while True:
        with buffer_lock:
            if len(raw_data) >= BUFFER_SIZE:
                data_to_filter = np.array(raw_data[-BUFFER_SIZE:])
                filtered = bandpass_filter(data_to_filter, 0.5, 40, SAMPLE_RATE)
                filtered_data.extend(filtered.tolist())
                if len(filtered_data) > BUFFER_SIZE:
                    filtered_data = filtered_data[-BUFFER_SIZE:]

# Monitor Buzzer
buzzer_active = False
def monitor_buzzer():
    global filtered_data, buzzer_active
    while True:
        if filtered_data:
            latest_value = filtered_data[-1]
            if latest_value < BUZZER_THRESHOLD:
                if not buzzer_active:
                    GPIO.output(BUZZER_PIN, GPIO.HIGH)
                    buzzer_active = True
            else:
                if buzzer_active:
                    GPIO.output(BUZZER_PIN, GPIO.LOW)
                    buzzer_active = False
        time.sleep(0.1)

# Save Data to Database
def save_to_db():
    global raw_data, filtered_data
    while True:
        if len(raw_data) >= BUFFER_SIZE and len(filtered_data) >= BUFFER_SIZE:
            with buffer_lock:
                raw_to_save = raw_data[-BUFFER_SIZE:]
                filtered_to_save = filtered_data[-BUFFER_SIZE:]
                timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
                data = [(timestamp, raw, filt) for raw, filt in zip(raw_to_save, filtered_to_save)]
                cursor.executemany("INSERT INTO ecg_data (timestamp, raw_voltage, filtered_voltage) VALUES (?, ?, ?)", data)
                conn.commit()
        time.sleep(10)

# Start Threads
adc_thread = threading.Thread(target=read_adc, daemon=True)
filter_thread = threading.Thread(target=filter_data, daemon=True)
buzzer_thread = threading.Thread(target=monitor_buzzer, daemon=True)
db_thread = threading.Thread(target=save_to_db, daemon=True)

adc_thread.start()
filter_thread.start()
buzzer_thread.start()
db_thread.start()

try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print("Shutting down...")
    GPIO.output(BUZZER_PIN, GPIO.LOW)
    GPIO.cleanup()
    conn.close()
