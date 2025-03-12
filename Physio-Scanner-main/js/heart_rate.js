import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy, limit } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";

// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
    authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
    databaseURL: "https://ixmarket-login-register-fbase-default-rtdb.firebaseio.com",
    projectId: "ixmarket-login-register-fbase",
    storageBucket: "ixmarket-login-register-fbase.appspot.com",
    messagingSenderId: "200593780997",
    appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// Function to fetch the latest heart rate data
async function fetchHeartRate() {
    try {
        const colRef = collection(db, "ppg_max_data");
        const q = query(colRef, orderBy("timestamp", "desc"), limit(1)); // Fetch the latest entry
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const latestData = querySnapshot.docs[0].data();
            document.getElementById("heartRateValue").innerText = latestData.heart_rate || "--";
            document.getElementById("timestamp").innerText = latestData.timestamp || "--";
        }
    } catch (error) {
        console.error("Error fetching heart rate data:", error);
    }
}

// Function to update Heart Rate Graph
async function updateHeartRateChart() {
    const ctx = document.getElementById('heartRateChart').getContext('2d');
    const colRef = collection(db, "ppg_max_data");
    const q = query(colRef, orderBy("timestamp", "desc"), limit(10)); // Fetch last 10 values
    const querySnapshot = await getDocs(q);

    let data = [];
    let timestamps = [];

    querySnapshot.forEach(doc => {
        data.unshift(doc.data().heart_rate);
        timestamps.unshift(new Date(doc.data().timestamp).toLocaleTimeString());
    });

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: timestamps,
            datasets: [{
                label: 'Heart Rate (BPM)',
                data: data,
                borderColor: 'red',
                borderWidth: 2,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { display: false },
                y: { display: false }
            },
            elements: { point: { radius: 0 } }
        }
    });
}

// Load data when the page loads
document.addEventListener("DOMContentLoaded", async () => {
    await fetchHeartRate();
    await updateHeartRateChart();
});
