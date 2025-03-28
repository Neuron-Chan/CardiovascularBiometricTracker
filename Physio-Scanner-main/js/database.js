import { initializeApp } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-app.js";
import {
  getFirestore,
  collection,
  getDocs,
  query,
  where,
  orderBy,
  limit
} from "https://www.gstatic.com/firebasejs/10.9.0/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.9.0/firebase-auth.js";

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

let currentUID = null;

onAuthStateChanged(auth, (user) => {
  if (user) {
    currentUID = user.uid;
    document.getElementById("usernameDisplay").innerText = user.email || "User";
  } else {
    window.location.href = "index.html";
  }
});

function logout() {
  localStorage.removeItem("isLoggedIn");
  localStorage.removeItem("userEmail");
  window.location.href = "index.html";
}

window.logout = logout;

function formatToFirestoreTimestamp(dateString) {
  if (!dateString) return null;
  const date = new Date(dateString);
  const pad = (n, len = 2) => n.toString().padStart(len, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.000000000`;
}

async function fetchFields() {
  if (!currentUID) return;
  const collectionName = document.getElementById("collection-select").value;
  const path = `users/${currentUID}/${collectionName}`;
  const snapshot = await getDocs(collection(db, path));
  if (!snapshot.empty) {
    const fieldsDropdowns = document.querySelectorAll(".field-select");
    const firstDoc = snapshot.docs[0].data();
    fieldsDropdowns.forEach(dropdown => {
      dropdown.innerHTML = "";
      Object.keys(firstDoc).forEach(field => {
        if (field !== "arduino_timestamp") {
          const option = document.createElement("option");
          option.value = field;
          option.textContent = field;
          dropdown.appendChild(option);
        }
      });
    });
  }
}

window.addQuery = function (type) {
  const container = document.getElementById("query-conditions");
  const box = document.createElement("div");
  box.classList.add("query-box");

  if (type === "orderBy") {
    box.innerHTML = `
      <label>Order By:</label>
      <select class="field-select"></select>
      <select class="order-direction">
        <option value="asc">Ascending</option>
        <option value="desc">Descending</option>
      </select>
      <button onclick="removeQuery(this)">Remove</button>
    `;
  } else if (type === "where") {
    box.innerHTML = `
      <label>Where:</label>
      <select class="field-select"></select>
      <select class="operator-select">
        <option value="==">=</option>
        <option value=">">></option>
        <option value="<"><</option>
        <option value=">=">>=</option>
        <option value="<="><=</option>
      </select>
      <input type="text" class="value-input" placeholder="Enter value">
      <button onclick="removeQuery(this)">Remove</button>
    `;
  } else if (type === "limit") {
    box.innerHTML = `
      <label>Limit:</label>
      <input type="number" class="limit-input" min="1" placeholder="Enter number">
      <button onclick="removeQuery(this)">Remove</button>
    `;
  }

  container.appendChild(box);
  fetchFields();
};

window.removeQuery = function (button) {
  button.parentElement.remove();
};

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("collection-select").addEventListener("change", fetchFields);
  document.getElementById("query-btn").addEventListener("click", queryFirestore);
});

async function queryFirestore() {
  if (!currentUID) return;

  const collectionName = document.getElementById("collection-select").value;
  const path = `users/${currentUID}/${collectionName}`;
  const colRef = collection(db, path);
  const conditions = [];

  const from = formatToFirestoreTimestamp(document.getElementById("fromDate").value);
  const to = formatToFirestoreTimestamp(document.getElementById("toDate").value);

  if (from) conditions.push(where("timestamp", ">=", from));
  if (to) conditions.push(where("timestamp", "<=", to));
  if (from || to) conditions.push(orderBy("timestamp"));

  document.querySelectorAll("#query-conditions .query-box").forEach(box => {
    const field = box.querySelector(".field-select")?.value;
    if (box.innerHTML.includes("Order By")) {
      const dir = box.querySelector(".order-direction")?.value || "asc";
      if (field) conditions.push(orderBy(field, dir));
    } else if (box.innerHTML.includes("Where")) {
      const op = box.querySelector(".operator-select")?.value || "==";
      const val = box.querySelector(".value-input")?.value;
      if (field && val !== "") {
        const parsed = isNaN(val) ? val : Number(val);
        conditions.push(where(field, op, parsed));
      }
    } else if (box.innerHTML.includes("Limit")) {
      const num = parseInt(box.querySelector(".limit-input")?.value);
      if (!isNaN(num)) conditions.push(limit(num));
    }
  });

  try {
    const q = query(colRef, ...conditions);
    const snap = await getDocs(q);

    let output = `<h3>Results for ${collectionName}</h3>`;
    if (snap.empty) {
      output += "<p>No results found.</p>";
      document.getElementById("database-content").innerHTML = output;
      if (window.myChart) window.myChart.destroy();
      return;
    }

    let columns = [];
    if (collectionName === "ecg_data") columns = ["ecg_value", "timestamp"];
    else if (collectionName === "ppg_gravity_data") columns = ["ppg_value", "timestamp"];
    else if (collectionName === "tmp102_data") columns = ["temperature_c", "temperature_f", "timestamp"];
    else if (collectionName === "ppg_max_data") columns = ["heart_rate", "timestamp"];

    output += "<table><thead><tr>" + columns.map(c => `<th>${c.replace("_", " ").toUpperCase()}</th>`).join("") + "</tr></thead><tbody>";

    const labels = [];
    const values = [];
    let yField = columns.find(c => !["timestamp", "temperature_f", "temperature_c"].includes(c));
    if (collectionName === "tmp102_data") yField = "temperature_f";

    snap.forEach(doc => {
      const data = doc.data();
      output += "<tr>" + columns.map(c => `<td>${data[c] ?? "N/A"}</td>`).join("") + "</tr>";
      labels.push(data.timestamp);
      values.push(data[yField]);
    });

    labels.reverse();
    values.reverse();
    output += "</tbody></table>";
    document.getElementById("database-content").innerHTML = output;

    if (window.myChart) window.myChart.destroy();
    const ctx = document.getElementById("dataChart").getContext("2d");
    window.myChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: yField.replace("_", " ").toUpperCase(),
          data: values,
          borderWidth: 2,
          fill: false,
          tension: 0.3,
          borderColor: 'blue',
          pointRadius: 2
        }]
      },
      options: {
        responsive: false,
        scales: {
          x: { title: { display: true, text: "Time" } },
          y: { title: { display: true, text: yField.replace("_", " ") } }
        },
        plugins: { legend: { display: true } }
      }
    });
  } catch (err) {
    console.error("Query failed:", err);
  }
}
