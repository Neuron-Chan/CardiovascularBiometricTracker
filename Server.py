#!/usr/bin/env python3
import serial
import sqlite3
from datetime import datetime
import threading
from flask import Flask, jsonify
from flask_socketio import SocketIO

DB_FILE = 'sensor_data.db'

# Sampling counters
ecg_count = 0
ppg_count = 0
temp_count = 0

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*")

def init_db():
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute("PRAGMA journal_mode=WAL")
    # ECG table
    c.execute('''
        CREATE TABLE IF NOT EXISTS ecg_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            rpi_timestamp TEXT NOT NULL,
            arduino_timestamp INTEGER NOT NULL,
            ecg_value REAL NOT NULL
        )
    ''')
    # PPG table
    c.execute('''
        CREATE TABLE IF NOT EXISTS ppg_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            rpi_timestamp TEXT NOT NULL,
            arduino_timestamp INTEGER NOT NULL,
            heart_rate INTEGER,
            confidence INTEGER,
            oxygen INTEGER,
            status INTEGER
        )
    ''')
    # Temperature table
    c.execute('''
        CREATE TABLE IF NOT EXISTS temperature_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            rpi_timestamp TEXT NOT NULL,
            arduino_timestamp INTEGER NOT NULL,
            temperature_c REAL NOT NULL,
            temperature_f REAL NOT NULL
        )
    ''')
    conn.commit()
    conn.close()

def process_line(line, conn):
    global ecg_count, ppg_count, temp_count
    line = line.strip()
    rpi_timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    c = conn.cursor()

    if line.startswith("ECG,"):
        parts = line.split(",")
        if len(parts) >= 3:
            try:
                arduino_ts = int(parts[1])
                try:
                    raw_val = int(parts[2])
                except Exception:
                    return
                voltage = raw_val * 5.0 / 1023.0
                c.execute(
                    "INSERT INTO ecg_data (rpi_timestamp, arduino_timestamp, ecg_value) VALUES (?, ?, ?)",
                    (rpi_timestamp, arduino_ts, voltage)
                )
                ecg_count += 1
                socketio.emit("ecg_data", {"timestamp": rpi_timestamp, "voltage": voltage})
                if ecg_count % 10 == 0:
                    conn.commit()
            except Exception as e:
                print("Error processing ECG data:", e)
    elif line.startswith("PPG,"):
        parts = line.split(",")
        if len(parts) >= 6:
            try:
                arduino_ts = int(parts[1])
                heart_rate = int(parts[2])
                confidence = int(parts[3])
                oxygen = int(parts[4])
                status = int(parts[5])
                c.execute(
                    "INSERT INTO ppg_data (rpi_timestamp, arduino_timestamp, heart_rate, confidence, oxygen, status)>
                    (rpi_timestamp, arduino_ts, heart_rate, confidence, oxygen, status)
                )
                ppg_count += 1
                socketio.emit("ppg_data", {"timestamp": rpi_timestamp, "ppg": heart_rate})
                if ppg_count % 5 == 0:
                    conn.commit()
            except Exception as e:
                print("Error processing PPG data:", e)
    elif line.startswith("TEMP,"):
        parts = line.split(",")
        if len(parts) >= 3:
            try:
                arduino_ts = int(parts[1])
                temperature_c = float(parts[2])
                temperature_f = temperature_c * 9/5 + 32
                c.execute(
                    "INSERT INTO temperature_data (rpi_timestamp, arduino_timestamp, temperature_c, temperature_f) V>
                    (rpi_timestamp, arduino_ts, temperature_c, temperature_f)
                )
                temp_count += 1
                socketio.emit("temp_data", {"timestamp": rpi_timestamp, "temperature_c": temperature_c, "temperature>
                conn.commit()
            except Exception as e:
                print("Error processing TEMP data:", e)

def print_sampling_rates(start_time):
    global ecg_count, ppg_count, temp_count
    elapsed_time = (datetime.now() - start_time).total_seconds()
    if elapsed_time >= 1:
        print(f"Effective sampling rate (last 1 sec): ECG: {ecg_count} Hz, PPG: {ppg_count} Hz, TEMP: {temp_count} H>
        ecg_count = 0
        ppg_count = 0
        temp_count = 0
        return datetime.now()
    return start_time

def read_serial():
    conn = sqlite3.connect(DB_FILE, check_same_thread=False)
    try:
        ser = serial.Serial('/dev/ttyACM0', 115200, timeout=0.01)
        print("Listening on serial port /dev/ttyACM0...")
    except Exception as e:
        print("Error opening serial port:", e)
        return
    start_time = datetime.now()
    while True:
        try:
            line = ser.readline().decode('utf-8')
            if line:
                process_line(line, conn)
            start_time = print_sampling_rates(start_time)
        except Exception as e:
            print("Error reading serial data:", e)

@app.route('/api/ecg_data', methods=['GET'])
def get_ecg_data():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    c = conn.cursor()
    c.execute("SELECT rpi_timestamp, ecg_value FROM ecg_data ORDER BY id ASC")
    rows = c.fetchall()
    data = []
    for row in rows:
        data.append({"timestamp": row["rpi_timestamp"], "voltage": row["ecg_value"]})
    conn.close()
    return jsonify(data)

@app.route('/api/ppg_data', methods=['GET'])
def get_ppg_data():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    c = conn.cursor()
    # For simplicity, we return only the heart rate for PPG data.
    c.execute("SELECT rpi_timestamp, heart_rate FROM ppg_data ORDER BY id ASC")
    rows = c.fetchall()
    data = []
    for row in rows:
        data.append({"timestamp": row["rpi_timestamp"], "ppg": row["heart_rate"]})
    conn.close()
    return jsonify(data)

if __name__ == "__main__":
    init_db()
    serial_thread = threading.Thread(target=read_serial)
    serial_thread.daemon = True
    serial_thread.start()
    socketio.run(app, host="0.0.0.0", port=5000)


