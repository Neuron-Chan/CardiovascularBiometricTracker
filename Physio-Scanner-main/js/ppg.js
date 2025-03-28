import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy, limit, onSnapshot } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-auth.js";

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
const auth = getAuth(app);

// ✅ Redirect to Database Page on Button Click
document.querySelector(".database-btn").addEventListener("click", () => {
    window.location.href = "database.html";
});

// ✅ Start fetching once user is authenticated
onAuthStateChanged(auth, (user) => {
    if (user) {
        console.log("User is logged in: ", user.uid);
        const uid = user.uid;
        startFetchingUserData(uid);
    } else {
        console.error("User not signed in");
    }
});

function startFetchingUserData(uid) {
    fetchLatestPPG(uid);
    fetchLatestHeartRate(uid);

    setInterval(() => fetchLatestPPG(uid), 210);
    setInterval(() => fetchLatestHeartRate(uid), 1000);
}

function fetchLatestPPG(uid) {
    const colRef = collection(db, `users/${uid}/ppg_gravity_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(1)); // Listen to latest ppg_value

    onSnapshot(q, (snapshot) => {
        snapshot.docChanges().forEach(change => {
            if (change.type === "added" || change.type === "modified") {
                const data = change.doc.data();

                if (data.ppg_value !== undefined && data.ppg_value !== null) {
                    updatePPGChart(data.ppg_value);
                    document.getElementById("ppgValue").innerText = data.ppg_value;
                    document.getElementById("timestamp").innerText = data.timestamp ?? "--";
                }
            }
        });
    }, (error) => {
        console.error("Error in real-time PPG listener:", error);
    });
}

/* ✅ Function to Fetch the Last 100 PPG Values and Update Graph */
/*async function fetchLatestPPG(uid) {
    try {
        const colRef = collection(db, `users/${uid}/ppg_gravity_data`);
        const q = query(colRef, orderBy("timestamp", "desc"), limit(100));
        const querySnapshot = await getDocs(q);

        const ppgValues = [];

        querySnapshot.forEach(doc => {
            const data = doc.data();
            if (data.ppg_value !== undefined && data.ppg_value !== null) {
                ppgValues.push(data.ppg_value);
            }
        });

        ppgValues.reverse();

        ppgChart.data.datasets[0].data = ppgValues;
        ppgChart.data.labels = ppgValues.map(() => "");
        ppgChart.update();

        const latestValue = ppgValues[ppgValues.length - 1];
        document.getElementById("ppgValue").innerText = latestValue ?? "--";

    } catch (error) {
        console.error("Error fetching PPG data:", error);
    }
} 
*/

/* ✅ Function to Fetch the Latest Non-Zero Heart Rate Data */
async function fetchLatestHeartRate(uid) {
    try {
        const colRef = collection(db, `users/${uid}/ppg_max_data`);
        const q = query(colRef, orderBy("timestamp", "desc"), limit(1));
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const latestData = querySnapshot.docs[0].data();

            const heartRate = latestData.heart_rate;
            const timestamp = latestData.timestamp;

            document.getElementById("heartRateValue").innerText =
                typeof heartRate === "number" ? heartRate : "--";

            document.getElementById("timestamp").innerText = timestamp ?? "--";
        }

    } catch (error) {
        console.error("Error fetching Heart Rate data:", error);
    }
}

/* ✅ PPG Graph Initialization */
const ctx = document.getElementById("ppgChart").getContext("2d");
const ppgChart = new Chart(ctx, {
    type: "line",
    data: {
        labels: Array(100).fill(""),
        datasets: [{
            label: "Live PPG Data",
            borderColor: "blue",
            backgroundColor: "rgba(0, 0, 255, 0.1)",
            data: Array(100).fill(300),
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
                min: 0,
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

    if (ppgChart.data.datasets[0].data.length > 100) {
        ppgChart.data.datasets[0].data.pop();
    }

    ppgChart.data.labels.unshift("");
    if (ppgChart.data.labels.length > 100) {
        ppgChart.data.labels.pop();
    }

    ppgChart.update();
}
