from pathlib import Path

html = r'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Smart Patient Monitoring System</title>
<style>
*{box-sizing:border-box}
body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#f4f7fb;color:#172033}
header{background:linear-gradient(135deg,#16213e,#274c77);color:white;padding:28px 6%;display:flex;justify-content:space-between;align-items:center}
header h1{margin:0;font-size:28px} header p{margin:6px 0 0;opacity:.8}
.status{background:#dff7e8;color:#167342;padding:9px 14px;border-radius:20px;font-weight:bold}
.container{width:88%;max-width:1200px;margin:28px auto}
.grid{display:grid;grid-template-columns:350px 1fr;gap:22px}
.card{background:white;border-radius:16px;padding:22px;box-shadow:0 8px 25px rgba(20,40,80,.08)}
.card h2{margin-top:0;font-size:19px}
label{display:block;margin:14px 0 6px;font-weight:bold;font-size:14px}
input{width:100%;padding:11px;border:1px solid #ccd5e1;border-radius:9px;font-size:14px}
button{border:0;border-radius:9px;padding:11px 15px;font-weight:bold;cursor:pointer;margin-top:12px}
.primary{background:#274c77;color:white}.secondary{background:#e8eef5;color:#24364d}
.danger{background:#fee2e2;color:#a32121}
.patient-list{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:15px}
.patient{border:1px solid #e0e6ee;border-radius:13px;padding:17px}
.patient h3{margin:0 0 5px}.id{color:#68778c;font-size:13px}
.badge{display:inline-block;padding:5px 9px;border-radius:15px;font-size:12px;font-weight:bold;margin-top:8px}
.active{background:#dff7e8;color:#167342}.inactive{background:#edf0f4;color:#667085}
.vitals{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:13px}
.vital{background:#f6f8fb;border-radius:9px;padding:10px;text-align:center}
.vital b{display:block;font-size:18px}
.alert{margin-top:12px;padding:10px;border-radius:9px;font-size:13px}
.alert.ok{background:#e9f8ef;color:#17663c}.alert.warn{background:#fff4dc;color:#8a5b00}
.log{margin-top:22px;background:#101827;color:#d7e1ee;border-radius:13px;padding:15px;font-family:Consolas,monospace;font-size:13px;max-height:190px;overflow:auto}
.log div{margin:5px 0}.success{color:#70e0a2}.error{color:#ff9292}.info{color:#8fc5ff}
.empty{text-align:center;padding:45px;color:#7a8798}
@media(max-width:800px){.grid{grid-template-columns:1fr}.vitals{grid-template-columns:1fr 1fr 1fr}}
</style>
</head>
<body>
<header>
  <div>
    <h1>Smart Patient Monitoring System</h1>
    <p>Patient registration • Live monitoring • Vital alerts • Error handling</p>
  </div>
  <div class="status">● SYSTEM ONLINE</div>
</header>

<div class="container">
<div class="grid">
<section class="card">
<h2>Register Patient</h2>
<label>Patient ID</label><input id="pid" placeholder="e.g. P101">
<label>Patient Name</label><input id="pname" placeholder="e.g. Arun Kumar">
<label>Age</label><input id="age" type="number" placeholder="e.g. 21">
<button class="primary" onclick="registerPatient()">Register Patient</button>

<h2 style="margin-top:28px">Monitoring Controls</h2>
<label>Patient ID</label><input id="controlId" placeholder="e.g. P101">
<button class="primary" onclick="startMonitoring()">Start Monitoring</button>
<button class="secondary" onclick="stopMonitoring()">Stop Monitoring</button>

<h2 style="margin-top:28px">Update Vital Signs</h2>
<label>Patient ID</label><input id="vpid" placeholder="e.g. P101">
<label>Temperature (°C)</label><input id="temp" type="number" step="0.1" placeholder="36.8">
<label>Heart Rate (bpm)</label><input id="heart" type="number" placeholder="75">
<label>SpO₂ (%)</label><input id="spo2" type="number" placeholder="98">
<button class="primary" onclick="updateVitals()">Update Vitals</button>
</section>

<section>
<div class="card">
<h2>Patient Dashboard</h2>
<div id="patients" class="patient-list"></div>
</div>
<div class="log" id="log">
<div class="info">[System] Ready. Waiting for transactions...</div>
</div>
</section>
</div>
</div>

<script>
const patients = new Map();

function log(message,type="info"){
  const el=document.getElementById("log");
  const d=document.createElement("div");
  d.className=type;
  d.textContent="[System] "+message;
  el.appendChild(d); el.scrollTop=el.scrollHeight;
}

function fail(message){
  log("ERROR: "+message,"error");
}

function registerPatient(){
  const id=document.getElementById("pid").value.trim();
  const name=document.getElementById("pname").value.trim();
  const age=Number(document.getElementById("age").value);

  if(!id) return fail("Patient ID cannot be empty.");
  if(patients.has(id)) return fail("Duplicate entry — Patient ID "+id+" already exists.");
  if(!name) return fail("Patient name cannot be empty.");
  if(!Number.isInteger(age) || age<1 || age>120)
    return fail("Invalid age. Age must be between 1 and 120.");

  patients.set(id,{id,name,age,temp:0,heart:0,spo2:0,active:false});
  log("Patient "+name+" ("+id+") registered successfully.","success");
  clear(["pid","pname","age"]); render();
}

function getPatient(id){
  if(!patients.has(id)) { fail("Resource unavailable — Patient "+id+" is not registered."); return null; }
  return patients.get(id);
}

function startMonitoring(){
  const id=document.getElementById("controlId").value.trim();
  const p=getPatient(id); if(!p)return;
  if(p.active) return fail("Transaction violation — monitoring is already active.");
  p.active=true; log("Monitoring started for "+p.name+".","success"); render();
}

function stopMonitoring(){
  const id=document.getElementById("controlId").value.trim();
  const p=getPatient(id); if(!p)return;
  if(!p.active) return fail("Transaction violation — monitoring is already inactive.");
  p.active=false; log("Monitoring stopped for "+p.name+".","success"); render();
}

function updateVitals(){
  const id=document.getElementById("vpid").value.trim();
  const p=getPatient(id); if(!p)return;
  if(!p.active) return fail("Transaction violation — cannot update vitals while monitoring is inactive.");

  const temp=Number(document.getElementById("temp").value);
  const heart=Number(document.getElementById("heart").value);
  const spo2=Number(document.getElementById("spo2").value);

  if(!Number.isFinite(temp)||temp<30||temp>45) return fail("Invalid temperature value.");
  if(!Number.isInteger(heart)||heart<30||heart>220) return fail("Invalid heart-rate value.");
  if(!Number.isInteger(spo2)||spo2<50||spo2>100) return fail("Invalid SpO₂ value.");

  p.temp=temp;p.heart=heart;p.spo2=spo2;
  log("Vital signs updated successfully for "+p.name+".","success");

  if(heart<60||heart>100) log("ALERT: Abnormal heart rate detected.","error");
  if(spo2<95) log("ALERT: Low oxygen saturation detected.","error");
  if(temp<36||temp>37.5) log("ALERT: Abnormal temperature detected.","error");

  clear(["vpid","temp","heart","spo2"]); render();
}

function clear(ids){ids.forEach(id=>document.getElementById(id).value="")}

function render(){
  const box=document.getElementById("patients");
  if(!patients.size){box.innerHTML='<div class="empty">No patients registered yet.<br>Register a patient to begin.</div>';return}
  box.innerHTML="";
  patients.forEach(p=>{
    let alerts=[];
    if(p.heart && (p.heart<60||p.heart>100)) alerts.push("Abnormal heart rate");
    if(p.spo2 && p.spo2<95) alerts.push("Low SpO₂");
    if(p.temp && (p.temp<36||p.temp>37.5)) alerts.push("Abnormal temperature");
    const div=document.createElement("div"); div.className="patient";
    div.innerHTML=`
      <h3>${escapeHtml(p.name)}</h3>
      <div class="id">${escapeHtml(p.id)} • Age ${p.age}</div>
      <span class="badge ${p.active?'active':'inactive'}">${p.active?'MONITORING ACTIVE':'MONITORING INACTIVE'}</span>
      <div class="vitals">
        <div class="vital">🌡️<b>${p.temp?p.temp+"°C":"—"}</b>Temp</div>
        <div class="vital">❤️<b>${p.heart?p.heart+"":"—"}</b>BPM</div>
        <div class="vital">🫁<b>${p.spo2?p.spo2+"%":"—"}</b>SpO₂</div>
      </div>
      <div class="alert ${alerts.length?'warn':'ok'}">
        ${alerts.length?'⚠ '+alerts.join(" • "):'✓ No abnormal readings detected'}
      </div>`;
    box.appendChild(div);
  });
}

function escapeHtml(s){
  return s.replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c]));
}

render();
</script>
</body>
</html>
'''

path = Path("/mnt/data/SmartPatientMonitoringSystem.html")
path.write_text(html, encoding="utf-8")
print(f"Created: {path}")

