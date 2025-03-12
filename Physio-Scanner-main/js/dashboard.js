// ✅ Initialize Firebase Without Import Statements
const firebaseConfig = {
    apiKey: "AIzaSyCdlgneYba6TU5SyyFT3ZEz8DzO1_-hdAA",
    authDomain: "ixmarket-login-register-fbase.firebaseapp.com",
    projectId: "ixmarket-login-register-fbase",
    storageBucket: "ixmarket-login-register-fbase.appspot.com",
    messagingSenderId: "200593780997",
    appId: "1:200593780997:web:40f4c31ce0ff3c8414c76c"
};

// // ✅ Use firebase.app() instead of import
// firebase.initializeApp(firebaseConfig);
// const db = firebase.firestore();

// /* ✅ Function to Fetch Latest Temperature Value */
// async function fetchLatestTemperature() {
//     try {
//         const colRef = db.collection("tmp102_data");
//         const querySnapshot = await colRef.orderBy("timestamp", "desc").limit(1).get();

//         console.log("Temperature Query Snapshot:", querySnapshot.docs.map(doc => doc.data())); // Debugging Log

//         if (!querySnapshot.empty) {
//             const latestData = querySnapshot.docs[0].data();
//             document.getElementById("dashboardTemperature").innerText = latestData.temperature || "--";
//         }
//     } catch (error) {
//         console.error("Error fetching Temperature data:", error);
//     }
// }

// /* ✅ Function to Fetch the Latest Non-Zero Heart Rate Value */
// async function fetchLatestHeartRate() {
//     try {
//         const colRef = db.collection("ppg_max_data");
//         const querySnapshot = await colRef.orderBy("timestamp", "desc").limit(10).get();

//         console.log("Heart Rate Query Snapshot:", querySnapshot.docs.map(doc => doc.data())); // Debugging Log

//         let latestValidHeartRate = "--";
//         for (const doc of querySnapshot.docs) {
//             const data = doc.data();
//             if (data.heart_rate && data.heart_rate > 0) { // ✅ Ensure non-zero value
//                 latestValidHeartRate = data.heart_rate;
//                 break;
//             }
//         }

//         document.getElementById("dashboardHeartRate").innerText = latestValidHeartRate;

//     } catch (error) {
//         console.error("Error fetching Heart Rate data:", error);
//     }
// }

// /* ✅ Fetch Data Every 1 Second */
// setInterval(fetchLatestTemperature, 1000);
// setInterval(fetchLatestHeartRate, 1000);
