import { initializeApp } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-app.js";
import { getFirestore, collection, query, orderBy, limit, getDocs, onSnapshot } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-auth.js";

// ✅ Firebase Configuration
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
const auth = getAuth(app);

// ✅ Redirect to Database Page on Button Click
document.querySelector(".database-btn").addEventListener("click", () => {
    window.location.href = "database.html";
});

// ✅ Start fetching once user is authenticated
onAuthStateChanged(auth, (user) => {
    if (user) {
        const uid = user.uid;
        // Toggle between real-time or static
        fetchLatestTemperatureRealtime(uid); // Real-time
        // fetchLatestTemperature(uid); // Polling mode
    } else {
        console.error("User not signed in");
    }
});

// ✅ Polling-Based: Fetch Latest Temperature (Once or Interval)
async function fetchLatestTemperature(uid) {
    try {
        const tempRef = collection(db, `users/${uid}/tmp102_data`);
        const q = query(tempRef, orderBy("timestamp", "desc"), limit(1));
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const latestData = querySnapshot.docs[0].data();

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

// ✅ Real-Time Version
function fetchLatestTemperatureRealtime(uid) {
    const tempRef = collection(db, `users/${uid}/tmp102_data`);
    const q = query(tempRef, orderBy("timestamp", "desc"), limit(1));

    onSnapshot(q, (snapshot) => {
        snapshot.docChanges().forEach(change => {
            if (change.type === "added" || change.type === "modified") {
                const data = change.doc.data();

                document.getElementById("temperatureC").innerText = data.temperature_c.toFixed(2);
                document.getElementById("temperatureF").innerText = data.temperature_f.toFixed(2);
                document.getElementById("timestamp").innerText = data.timestamp;

                updateChart(data.temperature_c);
            }
        });
    }, (error) => {
        console.error("Real-time temp error:", error);
    });
}

// ✅ Initialize Chart.js Graph (No X-Axis Labels)
const ctx = document.getElementById("temperatureChart").getContext("2d");
const temperatureChart = new Chart(ctx, {
    type: "line",
    data: {
        labels: [],
        datasets: [{
            label: "Live Temperature Data (\u00b0C)",
            borderColor: "red",
            borderWidth: 2,
            pointRadius: 0,
            fill: false,
            tension: 0.3,
            data: []
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            x: { display: false },
            y: { 
                display: true, 
                title: { display: true, text: "Temperature (\u00b0C)" } 
            }
        }
    }
});

// ✅ Function to Update Chart
function updateChart(tempC) {
    temperatureChart.data.labels.push("");
    temperatureChart.data.datasets[0].data.push(tempC);

    if (temperatureChart.data.labels.length > 20) {
        temperatureChart.data.labels.shift();
        temperatureChart.data.datasets[0].data.shift();
    }

    temperatureChart.update();
}