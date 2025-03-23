import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy, limit } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";

// ✅ Firebase Configuration
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

// ✅ Redirect to Database Page on Button Click
document.querySelector(".database-btn").addEventListener("click", () => {
    window.location.href = "database.html";
});

/* ✅ Function to Fetch PPG Data (for Graph & Value) */
async function fetchLatestPPG() {
    try {
        const colRef = collection(db, "ppg_gravity_data");
        const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const latestData = querySnapshot.docs[0].data();

            document.getElementById("ppgValue").innerText =
                latestData.ppg_value !== undefined && latestData.ppg_value !== null
                    ? latestData.ppg_value
                    : "--";

            // ✅ Update timestamp
            document.getElementById("timestamp").innerText =
                latestData.timestamp ?? "--";

            updatePPGChart(latestData.ppg_value);
        }
    } catch (error) {
        console.error("Error fetching PPG data:", error);
    }
}


/* ✅ Function to Fetch the Latest Non-Zero Heart Rate Data */
async function fetchLatestHeartRate() {
    try {
        const colRef = collection(db, "ppg_max_data"); // Ensure correct collection
        const q = query(colRef, orderBy("timestamp", "desc"), limit(10)); // Fetch latest 10 entries
        const querySnapshot = await getDocs(q);

        let latestValidHeartRate = "--"; // Default placeholder
        let latestValidTimestamp = "--";

        for (const doc of querySnapshot.docs) { // Loop through latest entries
            const data = doc.data();
            if (data.heart_rate && data.heart_rate > 0) { // ✅ Ensure non-zero value
                latestValidHeartRate = data.heart_rate;
                latestValidTimestamp = data.timestamp;
                break; // Stop at the first latest valid non-zero value
            }
        }

        // ✅ Update Heart Rate Display (Only Latest Non-Zero)
        document.getElementById("heartRateValue").innerText = latestValidHeartRate;
            latestData.latestValidHeartRate !== undefined && latestData.latestValidHeartRate !== null
            ? latestData.latestValidHeartRate
            : "--";

        document.getElementById("timestamp").innerText = latestValidTimestamp;

    } catch (error) {
        console.error("Error fetching Heart Rate data:", error);
    }
}

/* ✅ PPG Graph Initialization */
const ctx = document.getElementById("ppgChart").getContext("2d");
const ppgChart = new Chart(ctx, {
    type: "line",
    data: {
        labels: Array(100).fill(""), // Smoother graph with more points
        datasets: [{
            label: "Live PPG Data",
            borderColor: "blue",
            backgroundColor: "rgba(0, 0, 255, 0.1)", 
            data: Array(100).fill(300), // Placeholder data
            borderWidth: 1.5,
            cubicInterpolationMode: "monotone",
            fill: false,
            pointRadius: 0,
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
                min: 200,
                max: 1000,
                grid: { color: "rgba(0, 0, 0, 0.1)" },
                title: {
                    display: true,
                    text: "PPG Signal Value",
                    color: "#333"
                }
            }
        },
        plugins: {
            legend: { display: true }
        }
    }
});

/* ✅ Function to Update the PPG Graph */
function updatePPGChart(ppg_value) {
    ppgChart.data.datasets[0].data.unshift(ppg_value); 

    // Keep only the last 100 points for a smoother graph
    if (ppgChart.data.datasets[0].data.length > 100) {
        ppgChart.data.datasets[0].data.pop();
    }

    // Shift labels accordingly
    ppgChart.data.labels.unshift("");
    if (ppgChart.data.labels.length > 100) {
        ppgChart.data.labels.pop();
    }

    // Update the graph
    ppgChart.update();
}

/* ✅ Fetch PPG & Heart Rate Separately (Without Interfering) */
setInterval(fetchLatestPPG, 1000);  // Fetch PPG every 0.21s
setInterval(fetchLatestHeartRate, 1000);  // Fetch Heart Rate every 1s
