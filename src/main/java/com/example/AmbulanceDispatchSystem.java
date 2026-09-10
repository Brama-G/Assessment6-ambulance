import java.util.*;
import java.util.stream.Collectors;

// ============================================================
// CUSTOM EXCEPTIONS
// ============================================================

class InvalidEmergencyRequestException extends Exception {
    public InvalidEmergencyRequestException(String message) {
        super(message);
    }
}

class NoAmbulanceAvailableException extends Exception {
    public NoAmbulanceAvailableException(String message) {
        super(message);
    }
}

class AmbulanceNotAvailableException extends Exception {
    public AmbulanceNotAvailableException(String message) {
        super(message);
    }
}

// ============================================================
// ENUMS
// ============================================================

enum EmergencyType {
    CRITICAL(1, 1.0),
    HIGH(2, 1.2),
    MODERATE(3, 1.5),
    NORMAL(4, 2.0);

    private final int priority;
    private final double timeMultiplier;

    EmergencyType(int priority, double timeMultiplier) {
        this.priority = priority;
        this.timeMultiplier = timeMultiplier;
    }

    public int getPriority() { return priority; }
    public double getTimeMultiplier() { return timeMultiplier; }
}

enum AmbulanceType {
    BASIC(1, 1.0),
    ADVANCED_LIFE_SUPPORT(2, 1.3),
    ICU(3, 1.6);

    private final int capabilityLevel;
    private final double speedFactor;

    AmbulanceType(int capabilityLevel, double speedFactor) {
        this.capabilityLevel = capabilityLevel;
        this.speedFactor = speedFactor;
    }

    public int getCapabilityLevel() { return capabilityLevel; }
    public double getSpeedFactor() { return speedFactor; }
}

enum AmbulanceState {
    AVAILABLE,
    DISPATCHED,
    EN_ROUTE,
    PATIENT_PICKED_UP,
    HOSPITAL_ARRIVED
}

enum EmergencyStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    WAITING
}

// ============================================================
// DOMAIN CLASSES
// ============================================================

class Driver {
    private final String driverId;
    private final String name;
    private final String licenseNumber;
    private final String phoneNumber;

    public Driver(String driverId, String name, String licenseNumber, String phoneNumber) {
        if (driverId == null || driverId.trim().isEmpty())
            throw new IllegalArgumentException("Driver ID cannot be null or empty");
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Driver name cannot be null or empty");
        this.driverId = driverId;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.phoneNumber = phoneNumber;
    }

    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getPhoneNumber() { return phoneNumber; }

    @Override
    public String toString() {
        return "Driver{" + name + ", license=" + licenseNumber + "}";
    }
}

class Ambulance {
    private final String ambulanceId;
    private final AmbulanceType type;
    private final Driver driver;
    private AmbulanceState state;
    private double currentLatitude;
    private double currentLongitude;

    public Ambulance(String ambulanceId, AmbulanceType type, Driver driver,
                     double latitude, double longitude) {
        if (ambulanceId == null || ambulanceId.trim().isEmpty())
            throw new IllegalArgumentException("Ambulance ID cannot be null or empty");
        if (type == null)
            throw new IllegalArgumentException("Ambulance type cannot be null");
        if (driver == null)
            throw new IllegalArgumentException("Driver cannot be null");
        this.ambulanceId = ambulanceId;
        this.type = type;
        this.driver = driver;
        this.state = AmbulanceState.AVAILABLE;
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
    }

    public String getAmbulanceId() { return ambulanceId; }
    public AmbulanceType getType() { return type; }
    public Driver getDriver() { return driver; }
    public AmbulanceState getState() { return state; }
    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }

    public synchronized void setState(AmbulanceState state) {
        this.state = state;
    }

    public synchronized boolean isAvailable() {
        return state == AmbulanceState.AVAILABLE;
    }

    public void updateLocation(double lat, double lon) {
        this.currentLatitude = lat;
        this.currentLongitude = lon;
    }

    @Override
    public String toString() {
        return "Ambulance{" + ambulanceId + ", " + type + ", state=" + state + "}";
    }
}

class EmergencyRequest {
    private final String patientId;
    private final EmergencyType emergencyType;
    private final String pickupLocation;
    private final String destinationHospital;
    private final double pickupLatitude;
    private final double pickupLongitude;
    private final long requestTime;
    private EmergencyStatus status;
    private Ambulance assignedAmbulance;
    private double estimatedDistance;
    private double estimatedArrivalTime;
    private String requestId;

    public EmergencyRequest(String patientId, EmergencyType emergencyType,
                            String pickupLocation, String destinationHospital,
                            double pickupLatitude, double pickupLongitude) {
        if (patientId == null || patientId.trim().isEmpty())
            throw new IllegalArgumentException("Patient ID cannot be null or empty");
        if (emergencyType == null)
            throw new IllegalArgumentException("Emergency type cannot be null");
        if (pickupLocation == null || pickupLocation.trim().isEmpty())
            throw new IllegalArgumentException("Pickup location cannot be null or empty");
        if (destinationHospital == null || destinationHospital.trim().isEmpty())
            throw new IllegalArgumentException("Destination hospital cannot be null or empty");
        this.patientId = patientId;
        this.emergencyType = emergencyType;
        this.pickupLocation = pickupLocation;
        this.destinationHospital = destinationHospital;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.requestTime = System.currentTimeMillis();
        this.status = EmergencyStatus.PENDING;
    }

    public String getPatientId() { return patientId; }
    public EmergencyType getEmergencyType() { return emergencyType; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDestinationHospital() { return destinationHospital; }
    public double getPickupLatitude() { return pickupLatitude; }
    public double getPickupLongitude() { return pickupLongitude; }
    public long getRequestTime() { return requestTime; }
    public EmergencyStatus getStatus() { return status; }
    public Ambulance getAssignedAmbulance() { return assignedAmbulance; }
    public double getEstimatedDistance() { return estimatedDistance; }
    public double getEstimatedArrivalTime() { return estimatedArrivalTime; }
    public String getRequestId() { return requestId; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setStatus(EmergencyStatus status) { this.status = status; }

    public void assignAmbulance(Ambulance ambulance, double distance, double eta) {
        this.assignedAmbulance = ambulance;
        this.estimatedDistance = distance;
        this.estimatedArrivalTime = eta;
        this.status = EmergencyStatus.ASSIGNED;
    }

    @Override
    public String toString() {
        return "EmergencyRequest{" + patientId + ", " + emergencyType +
                ", status=" + status + "}";
    }
}

// ============================================================
// MAIN DISPATCH SYSTEM
// ============================================================

class AmbulanceDispatchSystem {

    private static final double AVERAGE_SPEED_KM_PER_HOUR = 60.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final Map<String, Ambulance> ambulances;
    private final Map<String, EmergencyRequest> requests;
    private final PriorityQueue<EmergencyRequest> waitingQueue;
    private final List<EmergencyRequest> emergencyHistory;
    private int requestCounter;

    public AmbulanceDispatchSystem() {
        this.ambulances = new HashMap<>();
        this.requests = new LinkedHashMap<>();
        this.waitingQueue = new PriorityQueue<>(Comparator
                .comparingInt((EmergencyRequest r) -> r.getEmergencyType().getPriority())
                .thenComparingLong(EmergencyRequest::getRequestTime));
        this.emergencyHistory = new ArrayList<>();
        this.requestCounter = 0;
    }

    // ---------------- Ambulance Management ----------------

    public void registerAmbulance(Ambulance ambulance) {
        if (ambulance == null)
            throw new IllegalArgumentException("Ambulance cannot be null");
        if (ambulances.containsKey(ambulance.getAmbulanceId()))
            throw new IllegalArgumentException(
                    "Ambulance already registered: " + ambulance.getAmbulanceId());
        ambulances.put(ambulance.getAmbulanceId(), ambulance);
    }

    public Ambulance getAmbulance(String ambulanceId) {
        return ambulances.get(ambulanceId);
    }

    public int getTotalAmbulances() {
        return ambulances.size();
    }

    public List<Ambulance> getAvailableAmbulances() {
        return ambulances.values().stream()
                .filter(Ambulance::isAvailable)
                .collect(Collectors.toList());
    }

    // ---------------- Distance Calculation ----------------

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public double calculateEstimatedArrivalTime(double distanceKm, EmergencyType emergencyType,
                                                AmbulanceType ambulanceType) {
        double baseTime = (distanceKm / AVERAGE_SPEED_KM_PER_HOUR) * 60.0; // minutes
        return baseTime * emergencyType.getTimeMultiplier() / ambulanceType.getSpeedFactor();
    }

    // ---------------- Emergency Request Processing ----------------

    public synchronized EmergencyRequest submitEmergencyRequest(EmergencyRequest request)
            throws InvalidEmergencyRequestException, NoAmbulanceAvailableException {

        if (request == null)
            throw new InvalidEmergencyRequestException("Emergency request cannot be null");

        if (request.getPatientId() == null || request.getPatientId().trim().isEmpty())
            throw new InvalidEmergencyRequestException("Patient ID is required");

        if (request.getEmergencyType() == null)
            throw new InvalidEmergencyRequestException("Emergency type is required");

        if (request.getPickupLocation() == null || request.getPickupLocation().trim().isEmpty())
            throw new InvalidEmergencyRequestException("Pickup location is required");

        requestCounter++;
        String requestId = "REQ-" + String.format("%04d", requestCounter);
        request.setRequestId(requestId);
        requests.put(requestId, request);

        Ambulance bestAmbulance = findBestAmbulance(request);

        if (bestAmbulance == null) {
            // No ambulance available -> queue it
            request.setStatus(EmergencyStatus.WAITING);
            waitingQueue.offer(request);
            throw new NoAmbulanceAvailableException(
                    "No ambulance available. Request " + requestId + " queued.");
        }

        assignAmbulanceToRequest(bestAmbulance, request);
        return request;
    }

    private Ambulance findBestAmbulance(EmergencyRequest request) {
        List<Ambulance> available = getAvailableAmbulances();
        if (available.isEmpty()) return null;

        // Required minimum capability based on emergency type
        int requiredCapability = requiredCapabilityLevel(request.getEmergencyType());

        Ambulance best = null;
        double bestScore = Double.MAX_VALUE;

        for (Ambulance a : available) {
            if (a.getType().getCapabilityLevel() < requiredCapability) continue;

            double distance = calculateDistance(
                    a.getCurrentLatitude(), a.getCurrentLongitude(),
                    request.getPickupLatitude(), request.getPickupLongitude());

            double eta = calculateEstimatedArrivalTime(
                    distance, request.getEmergencyType(), a.getType());

            // Lower score = better. Add small bonus for higher capability.
            double score = eta - (a.getType().getCapabilityLevel() * 0.1);

            if (score < bestScore) {
                bestScore = score;
                best = a;
            }
        }

        return best;
    }

    private int requiredCapabilityLevel(EmergencyType type) {
        switch (type) {
            case CRITICAL: return AmbulanceType.ICU.getCapabilityLevel();
            case HIGH:     return AmbulanceType.ADVANCED_LIFE_SUPPORT.getCapabilityLevel();
            default:       return AmbulanceType.BASIC.getCapabilityLevel();
        }
    }

    private void assignAmbulanceToRequest(Ambulance ambulance, EmergencyRequest request) {
        double distance = calculateDistance(
                ambulance.getCurrentLatitude(), ambulance.getCurrentLongitude(),
                request.getPickupLatitude(), request.getPickupLongitude());
        double eta = calculateEstimatedArrivalTime(
                distance, request.getEmergencyType(), ambulance.getType());

        ambulance.setState(AmbulanceState.DISPATCHED);
        request.assignAmbulance(ambulance, distance, eta);
    }

    // ---------------- State Transitions ----------------

    public synchronized void updateAmbulanceState(String ambulanceId, AmbulanceState newState)
            throws AmbulanceNotAvailableException {

        Ambulance ambulance = ambulances.get(ambulanceId);
        if (ambulance == null)
            throw new AmbulanceNotAvailableException("Ambulance not found: " + ambulanceId);

        AmbulanceState current = ambulance.getState();

        // Validate legal transitions
        boolean valid = false;
        switch (current) {
            case DISPATCHED:
                valid = (newState == AmbulanceState.EN_ROUTE);
                break;
            case EN_ROUTE:
                valid = (newState == AmbulanceState.PATIENT_PICKED_UP);
                break;
            case PATIENT_PICKED_UP:
                valid = (newState == AmbulanceState.HOSPITAL_ARRIVED);
                break;
            case HOSPITAL_ARRIVED:
                valid = (newState == AmbulanceState.AVAILABLE);
                break;
            default:
                valid = false;
        }

        if (!valid)
            throw new AmbulanceNotAvailableException(
                    "Invalid state transition: " + current + " -> " + newState);

        ambulance.setState(newState);

        if (newState == AmbulanceState.AVAILABLE) {
            completeRequestForAmbulance(ambulanceId);
            dispatchFromWaitingQueue();
        }
    }

    private void completeRequestForAmbulance(String ambulanceId) {
        for (EmergencyRequest req : requests.values()) {
            if (req.getAssignedAmbulance() != null
                    && req.getAssignedAmbulance().getAmbulanceId().equals(ambulanceId)
                    && req.getStatus() != EmergencyStatus.COMPLETED) {
                req.setStatus(EmergencyStatus.COMPLETED);
                emergencyHistory.add(req);
                break;
            }
        }
    }

    // ---------------- Waiting Queue ----------------

    public synchronized void dispatchFromWaitingQueue() {
        if (waitingQueue.isEmpty()) return;

        Iterator<EmergencyRequest> it = waitingQueue.iterator();
        List<EmergencyRequest> stillWaiting = new ArrayList<>();

        while (it.hasNext()) {
            EmergencyRequest req = it.next();
            Ambulance best = findBestAmbulance(req);
            if (best != null) {
                assignAmbulanceToRequest(best, req);
            } else {
                stillWaiting.add(req);
            }
        }

        waitingQueue.clear();
        waitingQueue.addAll(stillWaiting);
    }

    public int getWaitingQueueSize() {
        return waitingQueue.size();
    }

    public synchronized EmergencyRequest peekWaitingQueue() {
        return waitingQueue.peek();
    }

    // ---------------- History & Lookups ----------------

    public List<EmergencyRequest> getEmergencyHistory() {
        return Collections.unmodifiableList(emergencyHistory);
    }

    public EmergencyRequest getRequest(String requestId) {
        return requests.get(requestId);
    }

    public int getTotalRequests() {
        return requests.size();
    }

    // Cancel an emergency request
    public synchronized boolean cancelRequest(String requestId) {
        EmergencyRequest req = requests.get(requestId);
        if (req == null) return false;
        if (req.getStatus() == EmergencyStatus.COMPLETED) return false;

        Ambulance amb = req.getAssignedAmbulance();
        if (amb != null && amb.getState() != AmbulanceState.AVAILABLE) {
            amb.setState(AmbulanceState.AVAILABLE);
        }
        req.setStatus(EmergencyStatus.CANCELLED);
        waitingQueue.remove(req);
        return true;
    }
}

// ============================================================
// DEMO / MAIN
// ============================================================

public class AmbulanceDispatchSystem_Main {
    public static void main(String[] args) {
        AmbulanceDispatchSystem system = new AmbulanceDispatchSystem();

        // Register drivers & ambulances
        Driver d1 = new Driver("D1", "Alice", "LIC-001", "555-0001");
        Driver d2 = new Driver("D2", "Bob",   "LIC-002", "555-0002");
        Driver d3 = new Driver("D3", "Carol", "LIC-003", "555-0003");

        system.registerAmbulance(new Ambulance("AMB-1", AmbulanceType.BASIC, d1, 12.97, 77.59));
        system.registerAmbulance(new Ambulance("AMB-2", AmbulanceType.ADVANCED_LIFE_SUPPORT, d2, 12.98, 77.60));
        system.registerAmbulance(new Ambulance("AMB-3", AmbulanceType.ICU, d3, 12.96, 77.58));

        System.out.println("Total ambulances: " + system.getTotalAmbulances());
        System.out.println("Available: " + system.getAvailableAmbulances().size());

        try {
            EmergencyRequest req = new EmergencyRequest(
                    "PAT-101", EmergencyType.CRITICAL,
                    "MG Road", "City Hospital",
                    12.9716, 77.5946);

            EmergencyRequest dispatched = system.submitEmergencyRequest(req);
            System.out.println("Dispatched: " + dispatched);
            System.out.println("Ambulance: " + dispatched.getAssignedAmbulance());
            System.out.println("ETA (min): " + String.format("%.2f", dispatched.getEstimatedArrivalTime()));

            // Move through states
            system.updateAmbulanceState("AMB-3", AmbulanceState.EN_ROUTE);
            system.updateAmbulanceState("AMB-3", AmbulanceState.PATIENT_PICKED_UP);
            system.updateAmbulanceState("AMB-3", AmbulanceState.HOSPITAL_ARRIVED);
            system.updateAmbulanceState("AMB-3", AmbulanceState.AVAILABLE);

            System.out.println("History: " + system.getEmergencyHistory().size());

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
