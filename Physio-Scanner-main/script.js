// import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
// import { getFirestore, collection, getDocs, query, orderBy, limit } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";

// // Firebase Configuration
// const firebaseConfig = {
//     apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
//     authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
//     databaseURL: "https://ixmarket-login-register-fbase-default-rtdb.firebaseio.com",
//     projectId: "ixmarket-login-register-fbase",
//     storageBucket: "ixmarket-login-register-fbase.appspot.com",
//     messagingSenderId: "200593780997",
//     appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
// };

// // Initialize Firebase
// const app = initializeApp(firebaseConfig);
// const db = getFirestore(app);

// // Generic function to fetch latest data from Firestore
// async function fetchLatestData(collectionName, fieldName) {
//     try {
//         const colRef = collection(db, collectionName);
//         const q = query(colRef, orderBy("timestamp", "desc"), limit(10)); // Fetch last 10 values
//         const querySnapshot = await getDocs(q);

//         let data = [];
//         let timestamps = [];

//         querySnapshot.forEach(doc => {
//             data.unshift(doc.data()[fieldName]);  // Extract specific field (e.g., ecg_value, ppg_value)
//             timestamps.unshift(new Date(doc.data().timestamp).toLocaleTimeString());
//         });

//         return { data, timestamps };
//     } catch (error) {
//         console.error(`Error fetching ${collectionName} data:`, error);
//         return { data: [], timestamps: [] };
//     }
// }

// // Function to Update ECG Graph & Value
// async function updateECG() {
//     const ctx = document.getElementById('ecgChart').getContext('2d');
//     const { data, timestamps } = await fetchLatestData("ecg_data", "ecg_value");

//     if (data.length > 0) {
//         document.getElementById("ecgValue").innerText = `${data[data.length - 1].toFixed(2)} V`; // Update small number
//     }

//     new Chart(ctx, {
//         type: 'line',
//         data: {
//             labels: timestamps,
//             datasets: [{
//                 label: 'ECG Signal (V)',
//                 data: data,
//                 borderColor: 'red',
//                 borderWidth: 2,
//                 fill: false
//             }]
//         },
//         options: {
//             responsive: true,
//             maintainAspectRatio: false,
//             scales: {
//                 x: { display: false }, // Hide X labels for cleaner look
//                 y: { display: false }  // Hide Y labels for minimal UI
//             },
//             elements: { point: { radius: 0 } } // Remove dots from graph
//         }
//     });
// }

// // Function to Update PPG Graph & Value
// async function updatePPG() {
//     const ctx = document.getElementById('ppgChart').getContext('2d');
//     const { data, timestamps } = await fetchLatestData("ppg_gravity_data", "ppg_value");

//     if (data.length > 0) {
//         document.getElementById("ppgValue").innerText = `${data[data.length - 1]} bpm`; // Update small number
//     }

//     new Chart(ctx, {
//         type: 'line',
//         data: {
//             labels: timestamps,
//             datasets: [{
//                 label: 'PPG Signal',
//                 data: data,
//                 borderColor: 'blue',
//                 borderWidth: 2,
//                 fill: false
//             }]
//         },
//         options: {
//             responsive: true,
//             maintainAspectRatio: false,
//             scales: {
//                 x: { display: false }, // Hide X labels for cleaner look
//                 y: { display: false }  // Hide Y labels for minimal UI
//             },
//             elements: { point: { radius: 0 } } // Remove dots from graph
//         }
//     });
// }

// // Load Data When Page Loads
// document.addEventListener("DOMContentLoaded", async () => {
//     await updateECG();
//     await updatePPG();
// });

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import { 
    getAuth, signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, onAuthStateChanged 
} from "https://www.gstatic.com/firebasejs/10.9.0/firebase-auth.js";
import { 
    getFirestore, collection, getDocs, query, orderBy, limit 
} from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";

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

// Initialize Firebase Auth and Firestore
const auth = getAuth(app);
const db = getFirestore(app);

// **Sign-Up Function**
document.getElementById("signup-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const email = document.getElementById("signup-email").value;
    const password = document.getElementById("signup-password").value;

    createUserWithEmailAndPassword(auth, email, password)
        .then((userCredential) => {
            const user = userCredential.user;
            alert("Account created successfully!");
            localStorage.setItem("isLoggedIn", "true");
            localStorage.setItem("userEmail", user.email);
            window.location.href = "dashboard.html";
        })
        .catch(error => {
            alert(error.message);
        });
});

// **Login Function**
document.getElementById("login-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const email = document.getElementById("login-email").value;
    const password = document.getElementById("login-password").value;

    signInWithEmailAndPassword(auth, email, password)
        .then((userCredential) => {
            const user = userCredential.user;
            alert("Login Successful!");
            localStorage.setItem("isLoggedIn", "true");
            localStorage.setItem("userEmail", user.email);
            window.location.href = "dashboard.html";
        })
        .catch(error => {
            alert(error.message);
        });
});

// **Logout Function**
document.getElementById("logout-btn")?.addEventListener("click", () => {
    signOut(auth)
        .then(() => {
            alert("Logged out!");
            localStorage.removeItem("isLoggedIn");
            localStorage.removeItem("userEmail");
            window.location.href = "index.html";
        })
        .catch(error => {
            alert(error.message);
        });
});

// **Protect Dashboard**
if (window.location.pathname.includes("dashboard.html")) {
    onAuthStateChanged(auth, (user) => {
        if (!user) {
            window.location.href = "login.html";
        } else {
            document.getElementById("user-email").innerText = user.email;
        }
    });
}

// Firestore Logic for Fetching Data

// Generic function to fetch latest data from Firestore
async function fetchLatestData(collectionName, fieldName) {
    try {
        const colRef = collection(db, collectionName);
        const q = query(colRef, orderBy("timestamp", "desc"), limit(10)); // Fetch last 10 values
        const querySnapshot = await getDocs(q);

        let data = [];
        let timestamps = [];

        querySnapshot.forEach(doc => {
            data.unshift(doc.data()[fieldName]);  // Extract specific field (e.g., ecg_value, ppg_value)
            timestamps.unshift(new Date(doc.data().timestamp).toLocaleTimeString());
        });

        return { data, timestamps };
    } catch (error) {
        console.error(`Error fetching ${collectionName} data:`, error);
        return { data: [], timestamps: [] };
    }
}

// Function to Update ECG Graph & Value
async function updateECG() {
    const ctx = document.getElementById('ecgChart').getContext('2d');
    const { data, timestamps } = await fetchLatestData("ecg_data", "ecg_value");

    if (data.length > 0) {
        document.getElementById("ecgValue").innerText = `${data[data.length - 1].toFixed(2)} V`; // Update small number
    }

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: timestamps,
            datasets: [{
                label: 'ECG Signal (V)',
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
                x: { display: false }, // Hide X labels for cleaner look
                y: { display: false }  // Hide Y labels for minimal UI
            },
            elements: { point: { radius: 0 } } // Remove dots from graph
        }
    });
}

// Function to Update PPG Graph & Value
async function updatePPG() {
    const ctx = document.getElementById('ppgChart').getContext('2d');
    const { data, timestamps } = await fetchLatestData("ppg_gravity_data", "ppg_value");

    if (data.length > 0) {
        document.getElementById("ppgValue").innerText = `${data[data.length - 1]} bpm`; // Update small number
    }

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: timestamps,
            datasets: [{
                label: 'PPG Signal',
                data: data,
                borderColor: 'blue',
                borderWidth: 2,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { display: false }, // Hide X labels for cleaner look
                y: { display: false }  // Hide Y labels for minimal UI
            },
            elements: { point: { radius: 0 } } // Remove dots from graph
        }
    });
}

// Load Data When Page Loads
document.addEventListener("DOMContentLoaded", async () => {
    await updateECG();
    await updatePPG();
});