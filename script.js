const patients = new Map();
let monitoringIntervals = new Map();

function log(message, type = "info") {
    const el = document.getElementById("log");
    const d = document.createElement("div");
    d.className = type;
    const timestamp = new Date().toLocaleTimeString();
    d.textContent = `[${timestamp}] ${message}`;
    el.appendChild(d);
    el.scrollTop = el.scrollHeight;
}

function fail(message) {
    log("❌ ERROR: " + message, "error");
}

function registerPatient() {
    const id = document.getElementById("pid").value.trim();
    const name = document.getElementById("pname").value.trim();
    const age = Number(document.getElementById("age").value);
    const contact = document.getElementById("contact").value.trim();

    if (!id) return fail("Patient ID cannot be empty.");
    if (patients.has(id)) return fail(`Duplicate entry — Patient ID '${id}' already exists.`);
    if (!name) return fail("Patient name cannot be empty.");
    if (!Number.isInteger(age) || age < 1 || age > 150)
        return fail("Invalid age. Age must be between 1 and 150.");

    patients.set(id, {
        id,
        name,
        age,
        contact,
        temp: 0,
        heart: 0,
        spo2: 0,
        active: false,
        registered_at: new Date().toLocaleString(),
        vitals_history: []
    });
    
    log(`✓ Patient '${name}' (${id}) registered successfully.`, "success");
    clearInputs(["pid", "pname", "age", "contact"]);
    render();
}

function getPatient(id) {
    if (!patients.has(id)) {
        fail(`Resource unavailable — Patient '${id}' is not registered.`);
        return null;
    }
    return patients.get(id);
}

function startMonitoring() {
    const id = document.getElementById("controlId").value.trim();
    const p = getPatient(id);
    if (!p) return;

    if (p.active) return fail(`Transaction violation — monitoring for '${p.name}' is already active.`);

    p.active = true;
    log(`▶ Monitoring started for ${p.name}.`, "success");
    
    // Simulate automatic vital sign updates every 10 seconds
    const interval = setInterval(() => {
        if (p.active) {
            simulateVitals(p);
        }
    }, 10000);
    
    monitoringIntervals.set(id, interval);
    render();
}

function stopMonitoring() {
    const id = document.getElementById("controlId").value.trim();
    const p = getPatient(id);
    if (!p) return;

    if (!p.active) return fail(`Transaction violation — monitoring for '${p.name}' is already inactive.`);

    p.active = false;
    if (monitoringIntervals.has(id)) {
        clearInterval(monitoringIntervals.get(id));
        monitoringIntervals.delete(id);
    }
    
    log(`⏹ Monitoring stopped for ${p.name}.`, "success");
    render();
}

function simulateVitals(patient) {
    const baseTemp = 36.8 + (Math.random() - 0.5) * 0.8;
    const baseHeart = 70 + (Math.random() - 0.5) * 20;
    const baseSpo2 = 97 + (Math.random() - 0.5) * 4;

    patient.temp = Math.round(baseTemp * 10) / 10;
    patient.heart = Math.round(baseHeart);
    patient.spo2 = Math.round(baseSpo2);

    patient.vitals_history.push({
        timestamp: new Date().toLocaleTimeString(),
        temp: patient.temp,
        heart: patient.heart,
        spo2: patient.spo2
    });

    if (patient.vitals_history.length > 20) {
        patient.vitals_history.shift();
    }

    checkAlerts(patient);
    render();
}

function updateVitals() {
    const id = document.getElementById("vpid").value.trim();
    const p = getPatient(id);
    if (!p) return;

    if (!p.active) return fail(`Transaction violation — cannot update vitals while monitoring is inactive for '${p.name}'.`);

    const temp = Number(document.getElementById("temp").value);
    const heart = Number(document.getElementById("heart").value);
    const spo2 = Number(document.getElementById("spo2").value);

    if (!Number.isFinite(temp) || temp < 30 || temp > 45) return fail("Invalid temperature value. Range: 30-45°C");
    if (!Number.isInteger(heart) || heart < 30 || heart > 220) return fail("Invalid heart-rate value. Range: 30-220 bpm");
    if (!Number.isInteger(spo2) || spo2 < 50 || spo2 > 100) return fail("Invalid SpO₂ value. Range: 50-100%");

    p.temp = temp;
    p.heart = heart;
    p.spo2 = spo2;

    p.vitals_history.push({
        timestamp: new Date().toLocaleTimeString(),
        temp: p.temp,
        heart: p.heart,
        spo2: p.spo2
    });

    if (p.vitals_history.length > 20) {
        p.vitals_history.shift();
    }

    log(`✓ Vital signs updated successfully for ${p.name}.`, "success");
    checkAlerts(p);
    clearInputs(["vpid", "temp", "heart", "spo2"]);
    render();
}

function checkAlerts(patient) {
    if (patient.heart < 60 || patient.heart > 100)
        log(`⚠ ALERT: Abnormal heart rate detected for ${patient.name} (${patient.heart} bpm).`, "error");
    if (patient.spo2 < 95)
        log(`⚠ ALERT: Low oxygen saturation detected for ${patient.name} (${patient.spo2}%).`, "error");
    if (patient.temp < 36 || patient.temp > 37.5)
        log(`⚠ ALERT: Abnormal temperature detected for ${patient.name} (${patient.temp}°C).`, "error");
}

function deletePatient() {
    const id = document.getElementById("deleteId").value.trim();
    const p = getPatient(id);
    if (!p) return;

    if (p.active) {
        stopMonitoring();
    }

    const name = p.name;
    patients.delete(id);
    log(`🗑️ Patient '${name}' (${id}) has been deleted.`, "success");
    clearInputs(["deleteId"]);
    render();
}

function clearAllPatients() {
    if (patients.size === 0) return fail("No patients to clear.");
    
    const count = patients.size;
    monitoringIntervals.forEach(interval => clearInterval(interval));
    monitoringIntervals.clear();
    patients.clear();
    
    log(`🔄 All ${count} patient(s) have been cleared from the system.`, "success");
    render();
}

function exportData() {
    if (patients.size === 0) return fail("No patient data to export.");

    const data = Array.from(patients.values()).map(p => ({
        ID: p.id,
        Name: p.name,
        Age: p.age,
        Contact: p.contact,
        'Temperature (°C)': p.temp,
        'Heart Rate (bpm)': p.heart,
        'SpO₂ (%)': p.spo2,
        'Monitoring Active': p.active ? 'Yes' : 'No',
        'Registered At': p.registered_at
    }));

    const csv = [
        Object.keys(data[0]).join(","),
        ...data.map(row => Object.values(row).map(v => `"${v}"`).join(","))
    ].join("\n");

    const blob = new Blob([csv], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `patient_data_${new Date().getTime()}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);

    log(`📥 Exported data for ${patients.size} patient(s).`, "success");
}

function openPatientModal(id) {
    const p = getPatient(id);
    if (!p) return;

    const modal = document.getElementById("modal");
    const title = document.getElementById("modalTitle");
    const body = document.getElementById("modalBody");

    title.textContent = `Patient: ${p.name}`;
    
    let historyHTML = "";
    if (p.vitals_history.length > 0) {
        historyHTML = "<h4>Recent Vital History:</h4><table style='width:100%;border-collapse:collapse;'>";
        p.vitals_history.slice(-5).forEach(h => {
            historyHTML += `<tr style='border-bottom:1px solid #e0e0e0;'>
                <td style='padding:8px;'>${h.timestamp}</td>
                <td style='padding:8px;'>${h.temp}°C</td>
                <td style='padding:8px;'>${h.heart} bpm</td>
                <td style='padding:8px;'>${h.spo2}%</td>
            </tr>`;
        });
        historyHTML += "</table>";
    }

    body.innerHTML = `
        <p><strong>ID:</strong> ${p.id}</p>
        <p><strong>Name:</strong> ${p.name}</p>
        <p><strong>Age:</strong> ${p.age} years</p>
        <p><strong>Contact:</strong> ${p.contact || 'N/A'}</p>
        <p><strong>Registered:</strong> ${p.registered_at}</p>
        <p><strong>Current Vitals:</strong></p>
        <ul>
            <li>Temperature: ${p.temp || 'N/A'}°C</li>
            <li>Heart Rate: ${p.heart || 'N/A'} bpm</li>
            <li>SpO₂: ${p.spo2 || 'N/A'}%</li>
        </ul>
        <p><strong>Monitoring Status:</strong> ${p.active ? '🟢 Active' : '🔴 Inactive'}</p>
        ${historyHTML}
    `;
    
    modal.style.display = "block";
}

function closeModal() {
    const modal = document.getElementById("modal");
    modal.style.display = "none";
}

function clearInputs(ids) {
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = "";
    });
}

function render() {
    const box = document.getElementById("patients");
    
    if (patients.size === 0) {
        box.innerHTML = '<div class="empty"><strong>No patients registered yet.</strong><br>Register a patient to begin monitoring.</div>';
        return;
    }

    box.innerHTML = "";
    patients.forEach(p => {
        const alerts = [];
        if (p.heart && (p.heart < 60 || p.heart > 100)) alerts.push("Abnormal heart rate");
        if (p.spo2 && p.spo2 < 95) alerts.push("Low SpO₂");
        if (p.temp && (p.temp < 36 || p.temp > 37.5)) alerts.push("Abnormal temperature");

        const div = document.createElement("div");
        div.className = "patient";
        div.onclick = () => openPatientModal(p.id);
        div.innerHTML = `
            <h3>${escapeHtml(p.name)}</h3>
            <div class="id">${escapeHtml(p.id)} • Age ${p.age}</div>
            <span class="badge ${p.active ? 'active' : 'inactive'}">
                ${p.active ? '🟢 MONITORING ACTIVE' : '🔴 MONITORING INACTIVE'}
            </span>
            <div class="vitals">
                <div class="vital">
                    🌡️<br>
                    <b>${p.temp ? p.temp + "°C" : "—"}</b>
                    <div class="vital-label">Temp</div>
                </div>
                <div class="vital">
                    ❤️<br>
                    <b>${p.heart ? p.heart + "" : "—"}</b>
                    <div class="vital-label">BPM</div>
                </div>
                <div class="vital">
                    🫁<br>
                    <b>${p.spo2 ? p.spo2 + "%" : "—"}</b>
                    <div class="vital-label">SpO₂</div>
                </div>
            </div>
            <div class="alert ${alerts.length ? 'warn' : 'ok'}">
                ${alerts.length ? '⚠️ ' + alerts.join(" • ") : '✓ No abnormal readings detected'}
            </div>
        `;
        box.appendChild(div);
    });
}

function escapeHtml(s) {
    return s.replace(/[&<>"']/g, c => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    }[c]));
}

// Close modal when clicking outside
window.onclick = function (event) {
    const modal = document.getElementById("modal");
    if (event.target == modal) {
        modal.style.display = "none";
    }
}

// Initialize
render();
log("💚 Patient monitoring system ready.");