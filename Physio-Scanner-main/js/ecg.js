import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy, limit } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";

// ✅ Your Original Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
    authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
    projectId: "ixmarket-login-register-fbase",
    storageBucket: "ixmarket-login-register-fbase.appspot.com",
    messagingSenderId: "200593780997",
    appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// ✅ Function to Fetch Latest ECG Data from Firestore
async function fetchLatestECG() {
    try {
        const colRef = collection(db, "ecg_data"); // Ensure collection name matches
        const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const latestData = querySnapshot.docs[0].data();
            console.log("ECG Data Retrieved:", latestData);

            // ✅ Get HTML elements safely
            const ecgElement = document.getElementById("ecgValue");
            const timestampElement = document.getElementById("timestamp");

            if (ecgElement) {
                ecgElement.innerText = latestData.ecg_value.toFixed(2); // Show ECG with 2 decimals
            } else {
                console.warn("Element with ID 'ecgValue' not found.");
            }

            if (timestampElement) {
                timestampElement.innerText = latestData.timestamp;
            } else {
                console.warn("Element with ID 'timestamp' not found.");
            }

            // ✅ Update ECG Graph
            updateECGChart(latestData.ecg_value);
        } else {
            console.warn("No ECG data found in Firestore.");
        }
    } catch (error) {
        console.error("Error fetching ECG data:", error);
    }
}

// ✅ ECG Graph Initialization (Smooth Display)
const ctx = document.getElementById("ecgChart").getContext("2d");
const ecgChart = new Chart(ctx, {
    type: "line",
    data: {
        labels: Array(100).fill(""),
        datasets: [{
            label: "Live ECG Data",
            borderColor: "red",
            backgroundColor: "rgba(255, 0, 0, 0.1)",
            data: Array(100).fill(0),
            borderWidth: 1.5,
            cubicInterpolationMode: "monotone",
            fill: false,
            pointRadius: 0, // ✅ Removes Circles
            pointHoverRadius: 0
        }]
    },
    options: {
        responsive: true,
        animation: false,
        elements: {
            line: {
                tension: 0.4
            }
        },
        scales: {
            x: {
                display: true,
                grid: { display: false },
                title: {
                    display: true,
                    text: "Time",
                    color: "#333"
                }
            },
            y: {
                min: 0, // Adjusted for ECG range
                max: 4.5,
                grid: { color: "rgba(0, 0, 0, 0.1)" },
                title: {
                    display: true,
                    text: "ECG Voltage (V)",
                    color: "#333"
                }
            }
        },
        plugins: {
            legend: { display: true }
        }
    }
});

// ✅ Function to Update the ECG Graph
function updateECGChart(ecg_value) {
    ecgChart.data.datasets[0].data.unshift(ecg_value); // Add new data to the left

    if (ecgChart.data.datasets[0].data.length > 100) {
        ecgChart.data.datasets[0].data.pop(); // Keep only last 100 points
    }

    ecgChart.data.labels.unshift("");
    if (ecgChart.data.labels.length > 100) {
        ecgChart.data.labels.pop();
    }

    ecgChart.update();
}

// ✅ Fetch ECG Data Every 0.21 Seconds
setInterval(fetchLatestECG, 210);
