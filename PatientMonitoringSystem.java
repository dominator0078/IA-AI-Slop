import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Smart Patient Monitoring System - Java Backend
 * Manages patient data, vital sign monitoring, and alert generation
 */

public class PatientMonitoringSystem {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Map<String, Patient> patients;
    private List<String> systemLog;
    private ScheduledExecutorService executorService;

    public PatientMonitoringSystem() {
        this.patients = new ConcurrentHashMap<>();
        this.systemLog = Collections.synchronizedList(new ArrayList<>());
        this.executorService = Executors.newScheduledThreadPool(5);
    }

    // ==================== PATIENT MANAGEMENT ====================

    /**
     * Register a new patient in the system
     */
    public boolean registerPatient(String patientId, String name, int age, String contact) {
        if (patientId == null || patientId.trim().isEmpty()) {
            logEvent("ERROR", "Patient ID cannot be empty.");
            return false;
        }
        if (patients.containsKey(patientId)) {
            logEvent("ERROR", "Duplicate entry — Patient ID " + patientId + " already exists.");
            return false;
        }
        if (name == null || name.trim().isEmpty()) {
            logEvent("ERROR", "Patient name cannot be empty.");
            return false;
        }
        if (age < 1 || age > 150) {
            logEvent("ERROR", "Invalid age. Age must be between 1 and 150.");
            return false;
        }

        Patient patient = new Patient(patientId, name, age, contact);
        patients.put(patientId, patient);
        logEvent("SUCCESS", "Patient " + name + " (" + patientId + ") registered successfully.");
        return true;
    }

    /**
     * Retrieve patient by ID
     */
    public Patient getPatient(String patientId) {
        if (!patients.containsKey(patientId)) {
            logEvent("ERROR", "Resource unavailable — Patient " + patientId + " is not registered.");
            return null;
        }
        return patients.get(patientId);
    }

    /**
     * Delete a patient from the system
     */
    public boolean deletePatient(String patientId) {
        Patient patient = getPatient(patientId);
        if (patient == null) return false;

        if (patient.isMonitoring()) {
            stopMonitoring(patientId);
        }
        patients.remove(patientId);
        logEvent("SUCCESS", "Patient " + patient.getName() + " (" + patientId + ") deleted.");
        return true;
    }

    // ==================== MONITORING OPERATIONS ====================

    /**
     * Start monitoring for a patient
     */
    public boolean startMonitoring(String patientId) {
        Patient patient = getPatient(patientId);
        if (patient == null) return false;

        if (patient.isMonitoring()) {
            logEvent("ERROR", "Transaction violation — monitoring is already active for " + patient.getName() + ".");
            return false;
        }

        patient.setMonitoring(true);
        patient.setMonitoringStartTime(LocalDateTime.now());
        logEvent("SUCCESS", "Monitoring started for " + patient.getName() + ".");

        // Schedule automatic vital updates every 10 seconds
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(() -> {
            if (patient.isMonitoring()) {
                simulateVitalUpdate(patient);
            }
        }, 0, 10, TimeUnit.SECONDS);

        patient.setMonitoringTask(future);
        return true;
    }

    /**
     * Stop monitoring for a patient
     */
    public boolean stopMonitoring(String patientId) {
        Patient patient = getPatient(patientId);
        if (patient == null) return false;

        if (!patient.isMonitoring()) {
            logEvent("ERROR", "Transaction violation — monitoring is already inactive for " + patient.getName() + ".");
            return false;
        }

        patient.setMonitoring(false);
        if (patient.getMonitoringTask() != null) {
            patient.getMonitoringTask().cancel(true);
            patient.setMonitoringTask(null);
        }
        logEvent("SUCCESS", "Monitoring stopped for " + patient.getName() + ".");
        return true;
    }

    // ==================== VITAL SIGNS MANAGEMENT ====================

    /**
     * Update vital signs for a patient
     */
    public boolean updateVitals(String patientId, double temperature, int heartRate, int spO2) {
        Patient patient = getPatient(patientId);
        if (patient == null) return false;

        if (!patient.isMonitoring()) {
            logEvent("ERROR", "Transaction violation — cannot update vitals while monitoring is inactive.");
            return false;
        }

        // Validate temperature
        if (temperature < 30 || temperature > 45) {
            logEvent("ERROR", "Invalid temperature value. Range: 30-45°C");
            return false;
        }

        // Validate heart rate
        if (heartRate < 30 || heartRate > 220) {
            logEvent("ERROR", "Invalid heart-rate value. Range: 30-220 bpm");
            return false;
        }

        // Validate SpO2
        if (spO2 < 50 || spO2 > 100) {
            logEvent("ERROR", "Invalid SpO₂ value. Range: 50-100%");
            return false;
        }

        // Record vitals
        VitalSigns vitals = new VitalSigns(temperature, heartRate, spO2);
        patient.addVitalRecord(vitals);
        logEvent("SUCCESS", "Vital signs updated for " + patient.getName() + ".");

        // Check for alerts
        checkAndGenerateAlerts(patient);
        return true;
    }

    /**
     * Simulate realistic vital sign updates
     */
    private void simulateVitalUpdate(Patient patient) {
        double temp = 36.8 + (Math.random() - 0.5) * 0.8;
        int heart = 70 + (int) ((Math.random() - 0.5) * 20);
        int spo2 = 97 + (int) ((Math.random() - 0.5) * 4);

        updateVitals(patient.getPatientId(),
                Math.round(temp * 10.0) / 10.0,
                heart,
                spo2);
    }

    // ==================== ALERT GENERATION ====================

    /**
     * Check vital signs and generate alerts if needed
     */
    private void checkAndGenerateAlerts(Patient patient) {
        VitalSigns latest = patient.getLatestVitals();
        if (latest == null) return;

        List<String> alerts = new ArrayList<>();

        // Check heart rate
        if (latest.getHeartRate() < 60 || latest.getHeartRate() > 100) {
            String alertMsg = "ALERT: Abnormal heart rate detected for " + patient.getName() +
                    " (" + latest.getHeartRate() + " bpm).";
            logEvent("ALERT", alertMsg);
            alerts.add(alertMsg);
        }

        // Check SpO2
        if (latest.getSpO2() < 95) {
            String alertMsg = "ALERT: Low oxygen saturation detected for " + patient.getName() +
                    " (" + latest.getSpO2() + "%).";
            logEvent("ALERT", alertMsg);
            alerts.add(alertMsg);
        }

        // Check temperature
        if (latest.getTemperature() < 36 || latest.getTemperature() > 37.5) {
            String alertMsg = "ALERT: Abnormal temperature detected for " + patient.getName() +
                    " (" + latest.getTemperature() + "°C).";
            logEvent("ALERT", alertMsg);
            alerts.add(alertMsg);
        }

        if (!alerts.isEmpty()) {
            patient.addAlerts(alerts);
        }
    }

    // ==================== DATA EXPORT ====================

    /**
     * Export patient data to CSV file
     */
    public boolean exportPatientDataToCSV(String filename) {
        if (patients.isEmpty()) {
            logEvent("ERROR", "No patient data to export.");
            return false;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write header
            writer.println("ID,Name,Age,Contact,Temperature,HeartRate,SpO2,Monitoring,RegisteredAt");

            // Write patient data
            for (Patient patient : patients.values()) {
                VitalSigns vitals = patient.getLatestVitals();
                writer.printf("%s,%s,%d,%s,%.1f,%d,%d,%s,%s%n",
                        patient.getPatientId(),
                        patient.getName(),
                        patient.getAge(),
                        patient.getContact(),
                        vitals != null ? vitals.getTemperature() : 0,
                        vitals != null ? vitals.getHeartRate() : 0,
                        vitals != null ? vitals.getSpO2() : 0,
                        patient.isMonitoring() ? "Yes" : "No",
                        patient.getRegisteredAt());
            }

            logEvent("SUCCESS", "Exported data for " + patients.size() + " patient(s) to " + filename);
            return true;
        } catch (IOException e) {
            logEvent("ERROR", "Failed to export data: " + e.getMessage());
            return false;
        }
    }

    // ==================== SYSTEM LOGGING ====================

    /**
     * Log system events
     */
    private void logEvent(String type, String message) {
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String logEntry = "[" + timestamp + "] [" + type + "] " + message;
        systemLog.add(logEntry);
        System.out.println(logEntry);
    }

    /**
     * Get system logs
     */
    public List<String> getSystemLogs() {
        return new ArrayList<>(systemLog);
    }

    /**
     * Get patient statistics
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_patients", patients.size());
        stats.put("monitoring_active", patients.values().stream().filter(Patient::isMonitoring).count());
        stats.put("total_alerts", patients.values().stream()
                .mapToInt(p -> p.getAlerts().size()).sum());
        stats.put("timestamp", LocalDateTime.now().format(dateFormatter));
        return stats;
    }

    /**
     * Shutdown the system
     */
    public void shutdown() {
        // Stop all monitoring
        patients.keySet().forEach(this::stopMonitoring);
        executorService.shutdown();
        logEvent("INFO", "System shutdown complete.");
    }

    // ==================== INNER CLASSES ====================

    /**
     * Patient class - represents a patient in the system
     */
    public static class Patient {
        private String patientId;
        private String name;
        private int age;
        private String contact;
        private boolean monitoring;
        private LocalDateTime monitoringStartTime;
        private LocalDateTime registeredAt;
        private List<VitalSigns> vitalHistory;
        private List<String> alerts;
        private ScheduledFuture<?> monitoringTask;

        public Patient(String patientId, String name, int age, String contact) {
            this.patientId = patientId;
            this.name = name;
            this.age = age;
            this.contact = contact;
            this.monitoring = false;
            this.registeredAt = LocalDateTime.now();
            this.vitalHistory = Collections.synchronizedList(new ArrayList<>());
            this.alerts = Collections.synchronizedList(new ArrayList<>());
        }

        // Getters and Setters
        public String getPatientId() { return patientId; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getContact() { return contact; }
        public boolean isMonitoring() { return monitoring; }
        public void setMonitoring(boolean monitoring) { this.monitoring = monitoring; }
        public LocalDateTime getMonitoringStartTime() { return monitoringStartTime; }
        public void setMonitoringStartTime(LocalDateTime time) { this.monitoringStartTime = time; }
        public String getRegisteredAt() { return registeredAt.format(dateFormatter); }
        public ScheduledFuture<?> getMonitoringTask() { return monitoringTask; }
        public void setMonitoringTask(ScheduledFuture<?> task) { this.monitoringTask = task; }

        public void addVitalRecord(VitalSigns vitals) {
            vitalHistory.add(vitals);
            if (vitalHistory.size() > 100) {
                vitalHistory.remove(0);
            }
        }

        public VitalSigns getLatestVitals() {
            return vitalHistory.isEmpty() ? null : vitalHistory.get(vitalHistory.size() - 1);
        }

        public List<VitalSigns> getVitalHistory() {
            return new ArrayList<>(vitalHistory);
        }

        public void addAlerts(List<String> newAlerts) {
            alerts.addAll(newAlerts);
        }

        public List<String> getAlerts() {
            return new ArrayList<>(alerts);
        }
    }

    /**
     * VitalSigns class - represents a single vital sign measurement
     */
    public static class VitalSigns {
        private double temperature;
        private int heartRate;
        private int spO2;
        private LocalDateTime timestamp;

        public VitalSigns(double temperature, int heartRate, int spO2) {
            this.temperature = temperature;
            this.heartRate = heartRate;
            this.spO2 = spO2;
            this.timestamp = LocalDateTime.now();
        }

        public double getTemperature() { return temperature; }
        public int getHeartRate() { return heartRate; }
        public int getSpO2() { return spO2; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("VitalSigns[Temp: %.1f°C, HR: %d bpm, SpO2: %d%%, Time: %s]",
                    temperature, heartRate, spO2, timestamp.format(dateFormatter));
        }
    }

    // ==================== MAIN METHOD ====================

    public static void main(String[] args) {
        PatientMonitoringSystem system = new PatientMonitoringSystem();

        try {
            // Demo: Register patients
            system.registerPatient("P001", "John Doe", 45, "+1-555-0123");
            system.registerPatient("P002", "Jane Smith", 32, "+1-555-0124");
            system.registerPatient("P003", "Bob Johnson", 68, "+1-555-0125");

            // Start monitoring
            system.startMonitoring("P001");
            system.startMonitoring("P002");

            // Simulate vital updates
            Thread.sleep(5000);
            system.updateVitals("P001", 36.8, 72, 98);
            system.updateVitals("P002", 37.2, 85, 96);

            // Display statistics
            System.out.println("\n=== System Statistics ===");
            system.getSystemStatistics().forEach((k, v) -> System.out.println(k + ": " + v));

            // Export data
            system.exportPatientDataToCSV("patient_data_export.csv");

            // Cleanup
            Thread.sleep(2000);
            system.shutdown();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
