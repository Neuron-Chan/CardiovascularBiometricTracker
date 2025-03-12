import { initializeApp } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-app.js";
import { getFirestore, collection, query, orderBy, limit, getDocs } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-firestore.js";

// ✅ Firebase Configuration (Original)
const firebaseConfig = {
    apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
    authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
    projectId: "ixmarket-login-register-fbase",
    storageBucket: "ixmarket-login-register-fbase.appspot.com",
    messagingSenderId: "200593780997",
    appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
};

// ✅ Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// ✅ Redirect to Database Page on Button Click
document.querySelector(".database-btn").addEventListener("click", () => {
    window.location.href = "database.html";
});

// ✅ Function to Fetch Latest Temperature Data
async function fetchLatestTemperature() {
    try {
        const tempRef = collection(db, "tmp102_data");
        const q = query(tempRef, orderBy("timestamp", "desc"), limit(1));
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const latestData = querySnapshot.docs[0].data();
            console.log("Temperature Data Retrieved:", latestData);

            document.getElementById("temperatureC").innerText = latestData.temperature_c.toFixed(2);
            document.getElementById("temperatureF").innerText = latestData.temperature_f.toFixed(2);
            document.getElementById("timestamp").innerText = latestData.timestamp;

            updateChart(latestData.temperature_c);
        } else {
            console.warn("No temperature data found.");
        }
    } catch (error) {
        console.error("Error fetching temperature data:", error);
    }
}

// ✅ Initialize Chart.js Graph (No X-Axis Labels)
const ctx = document.getElementById("temperatureChart").getContext("2d");
const temperatureChart = new Chart(ctx, {
    type: "line",
    data: {
        labels: [], // ✅ Keep labels empty to hide x-axis timestamps
        datasets: [{
            label: "Live Temperature Data (°C)",
            borderColor: "red",
            borderWidth: 2,
            pointRadius: 0, // ✅ Remove dots/circles
            fill: false,
            tension: 0.3, // ✅ Makes the graph smooth
            data: []
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            x: { 
                display: false // ✅ Hide the x-axis labels
            },
            y: { 
                display: true, 
                title: { display: true, text: "Temperature (°C)" } 
            }
        }
    }
});

// ✅ Function to Update Chart
function updateChart(tempC) {
    temperatureChart.data.labels.push(""); // ✅ Keep x-axis empty
    temperatureChart.data.datasets[0].data.push(tempC);

    if (temperatureChart.data.labels.length > 20) {
        temperatureChart.data.labels.shift();
        temperatureChart.data.datasets[0].data.shift();
    }

    temperatureChart.update();
}

// ✅ Fetch Data Every 1 Second
setInterval(fetchLatestTemperature, 1000);
