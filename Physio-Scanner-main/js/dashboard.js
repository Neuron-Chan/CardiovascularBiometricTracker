import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { getFirestore, collection, query, orderBy, limit, getDocs } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-auth.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
  authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
  projectId: "ixmarket-login-register-fbase",
  storageBucket: "ixmarket-login-register-fbase.appspot.com",
  messagingSenderId: "200593780997",
  appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
};

// Init Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Logout
document.getElementById("logoutBtn").addEventListener("click", () => {
  localStorage.removeItem("isLoggedIn");
  localStorage.removeItem("userEmail");
  window.location.href = "index.html";
});

// Get user and start
onAuthStateChanged(auth, (user) => {
  if (user) {
    document.getElementById("usernameDisplay").innerText = user.email || "User";
    const uid = user.uid;
    startFetching(uid);
  } else {
    window.location.href = "index.html";
  }
});

function startFetching(uid) {
  createECGChart();
  createPPGChart();

  setInterval(() => fetchLatestECG(uid), 210);
  setInterval(() => fetchLatestPPG(uid), 210);
  setInterval(() => fetchLatestTemperature(uid), 1000);
  setInterval(() => fetchLatestHeartRate(uid), 1000);
  setInterval(() => fetchLatestSpO2(uid), 1000);
  setInterval(() => fetchLatestConfidence(uid), 1000);
}

// ECG Fetch
async function fetchLatestECG(uid) {
  try {
    const colRef = collection(db, `users/${uid}/ecg_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(100));
    const querySnapshot = await getDocs(q);

    const ecgValues = [];
    querySnapshot.forEach(doc => {
      const data = doc.data();
      if (data.ecg_value !== undefined && data.ecg_value !== null) {
        ecgValues.push(data.ecg_value);
      }
    });

    ecgValues.reverse();
    ecgChart.data.datasets[0].data = ecgValues;
    ecgChart.data.labels = ecgValues.map(() => "");
    ecgChart.update();

    const latestValue = ecgValues[ecgValues.length - 1];
    document.getElementById("dashboardECGValue").innerText = latestValue !== undefined ? `${latestValue.toFixed(2)} V` : "--";

  } catch (error) {
    console.error("ECG fetch error:", error);
  }
}

// PPG Fetch
async function fetchLatestPPG(uid) {
  try {
    const colRef = collection(db, `users/${uid}/ppg_gravity_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(100));
    const querySnapshot = await getDocs(q);

    const ppgValues = [];
    querySnapshot.forEach(doc => {
      const data = doc.data();
      if (data.value !== undefined && data.value !== null) {
        ppgValues.push(data.value);
      }
    });

    ppgValues.reverse();
    ppgChart.data.datasets[0].data = ppgValues;
    ppgChart.data.labels = ppgValues.map(() => "");
    ppgChart.update();

    const latestValue = ppgValues[ppgValues.length - 1];
    document.getElementById("ppgValue").innerText = latestValue ?? "--";
  } catch (error) {
    console.error("PPG fetch error:", error);
  }
}

// Temperature Fetch
async function fetchLatestTemperature(uid) {
  try {
    const colRef = collection(db, `users/${uid}/tmp102_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
    const querySnapshot = await getDocs(q);

    if (!querySnapshot.empty) {
      const data = querySnapshot.docs[0].data();
      document.getElementById("dashboardTemperature").innerText = data.temperature_c.toFixed(2) + "°C";
    }
  } catch (error) {
    console.error("Temperature fetch error:", error);
  }
}

// Heart Rate Fetch
async function fetchLatestHeartRate(uid) {
  try {
    const colRef = collection(db, `users/${uid}/ppg_max_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
    const querySnapshot = await getDocs(q);

    if (!querySnapshot.empty) {
      const data = querySnapshot.docs[0].data();
      const hr = typeof data.heart_rate === "number" ? data.heart_rate : "--";
      document.getElementById("dashboardHeartRate").innerText = hr !== "--" ? `${hr} BPM` : "--";
    }
  } catch (error) {
    console.error("Heart rate fetch error:", error);
  }
}

// SpO2 Fetch
async function fetchLatestSpO2(uid) {
  try {
    const colRef = collection(db, `users/${uid}/ppg_max_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
    const querySnapshot = await getDocs(q);

    if (!querySnapshot.empty) {
      const data = querySnapshot.docs[0].data();
      const spo2 = typeof data.oxygen === "number" ? data.oxygen : "--";
      document.getElementById("dashboardSpO2").innerText = `${spo2} %`;
    }
  } catch (error) {
    console.error("SpO2 fetch error:", error);
  }
}

// Confidence Fetch
async function fetchLatestConfidence(uid) {
  try {
    const colRef = collection(db, `users/${uid}/ppg_max_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
    const querySnapshot = await getDocs(q);

    if (!querySnapshot.empty) {
      const data = querySnapshot.docs[0].data();
      const confidence = typeof data.confidence === "number" ? data.confidence : "--";
      document.getElementById("dashboardConfidence").innerText = `${confidence} %`;
    }
  } catch (error) {
    console.error("Confidence fetch error:", error);
  }
}


// Chart Initialization
let ecgChart, ppgChart;

function createECGChart() {
  const ctx = document.getElementById("ecgDashboardChart").getContext("2d");
  ecgChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: Array(100).fill(""),
      datasets: [{
        label: "ECG",
        borderColor: "red",
        backgroundColor: "rgba(255,0,0,0.1)",
        data: Array(100).fill(0),
        borderWidth: 1.5,
        fill: false,
        pointRadius: 0
      }]
    },
    options: {
      responsive: true,
      animation: false,
      maintainAspectRatio: false,
      elements: { line: { tension: 0.4 } },
      scales: {
        x: { display: true },
        y: { min: 0, max: 4.5 }
      }
    }
  });
}

function createPPGChart() {
  const ctx = document.getElementById("ppgDashboardChart").getContext("2d");
  ppgChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: Array(100).fill(""),
      datasets: [{
        label: "PPG",
        borderColor: "blue",
        backgroundColor: "rgba(0,0,255,0.1)",
        data: Array(100).fill(300),
        borderWidth: 1.5,
        fill: false,
        pointRadius: 0
      }]
    },
    options: {
      responsive: true,
      animation: false,
      maintainAspectRatio: false,
      elements: { line: { tension: 0.4 } },
      scales: {
        x: { display: true },
        y: { min: 0, max: 1000 }
      }
    }
  });
}
