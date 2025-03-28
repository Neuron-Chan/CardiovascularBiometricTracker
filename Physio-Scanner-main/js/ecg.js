import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy, limit, onSnapshot } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-auth.js";

// ✅ Firebase Config
const firebaseConfig = {
    apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
    authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
    projectId: "ixmarket-login-register-fbase",
    storageBucket: "ixmarket-login-register-fbase.appspot.com",
    messagingSenderId: "200593780997",
    appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// ✅ Redirect to Database Page
document.querySelector(".database-btn").addEventListener("click", () => {
    window.location.href = "database.html";
});

// ✅ Start Fetching Once User is Authenticated
onAuthStateChanged(auth, (user) => {
    if (user) {
        console.log("User is logged in:", user.uid);
        const uid = user.uid;
        startFetchingECG(uid);
    } else {
        console.error("User not signed in");
    }
});

function startFetchingECG(uid) {
    fetchLatestECGRealtime(uid);
    setInterval(() => fetchLatestECGRealtime(uid), 210);
}

// ✅ Fetch Last 100 ECG Values (User-Specific)
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
        const ecgElement = document.getElementById("ecgValue");
        const timestampElement = document.getElementById("timestamp");

        if (ecgElement) {
            ecgElement.innerText = latestValue?.toFixed(2) ?? "--";
        }

        if (timestampElement) {
            const latestDoc = querySnapshot.docs[0];
            timestampElement.innerText = latestDoc?.data().timestamp ?? "--";
        }

    } catch (error) {
        console.error("Error fetching ECG data:", error);
    }
}

function fetchLatestECGRealtime(uid) {
    const colRef = collection(db, `users/${uid}/ecg_data`);
    const q = query(colRef, orderBy("timestamp", "desc"), limit(1)); // Latest doc only

    onSnapshot(q, (snapshot) => {
        snapshot.docChanges().forEach(change => {
            if (change.type === "added" || change.type === "modified") {
                const data = change.doc.data();

                if (data.ecg_value !== undefined && data.ecg_value !== null) {
                    updateECGChart(data.ecg_value);

                    const ecgElement = document.getElementById("ecgValue");
                    const timestampElement = document.getElementById("timestamp");

                    if (ecgElement) ecgElement.innerText = data.ecg_value.toFixed(2);
                    if (timestampElement) timestampElement.innerText = data.timestamp ?? "--";
                }
            }
        });
    }, (error) => {
        console.error("Error in real-time ECG listener:", error);
    });
}

// ✅ ECG Chart Setup
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
            pointRadius: 0,
            pointHoverRadius: 0
        }]
    },
    options: {
        responsive: true,
        animation: false,
        elements: {
            line: { tension: 0.4 }
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

// ✅ Update ECG Graph
function updateECGChart(ecg_value) {
    ecgChart.data.datasets[0].data.unshift(ecg_value);
    if (ecgChart.data.datasets[0].data.length > 100) {
        ecgChart.data.datasets[0].data.pop();
    }

    ecgChart.data.labels.unshift("");
    if (ecgChart.data.labels.length > 100) {
        ecgChart.data.labels.pop();
    }

    ecgChart.update();
}
