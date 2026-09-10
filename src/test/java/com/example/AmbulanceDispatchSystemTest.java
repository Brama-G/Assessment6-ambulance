package com.example;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class AmbulanceDispatchSystemTest {

    private AmbulanceDispatchSystem system;
    private Driver driver1, driver2, driver3;

    @BeforeEach
    public void setUp() {
        system = new AmbulanceDispatchSystem();

        driver1 = new Driver("D1", "Alice", "LIC-001", "555-0001");
        driver2 = new Driver("D2", "Bob",   "LIC-002", "555-0002");
        driver3 = new Driver("D3", "Carol", "LIC-003", "555-0003");

        system.registerAmbulance(new Ambulance(
                "AMB-1", AmbulanceType.BASIC, driver1, 12.97, 77.59));
        system.registerAmbulance(new Ambulance(
                "AMB-2", AmbulanceType.ADVANCED_LIFE_SUPPORT, driver2, 12.98, 77.60));
        system.registerAmbulance(new Ambulance(
                "AMB-3", AmbulanceType.ICU, driver3, 12.96, 77.58));
    }

    // =========================================================
    // 1. AMBULANCE REGISTRATION
    // =========================================================

    @Test
    @DisplayName("Register ambulance - positive")
    void testRegisterAmbulance() {
        Ambulance newAmb = new Ambulance("AMB-4", AmbulanceType.BASIC,
                new Driver("D4", "Dave", "LIC-004", "555-0004"), 12.9, 77.5);
        system.registerAmbulance(newAmb);
        assertNotNull(system.getAmbulance("AMB-4"));
        assertEquals(4, system.getTotalAmbulances());
    }

    @Test
    @DisplayName("Register duplicate ambulance - negative")
    void testRegisterDuplicateAmbulance() {
        Ambulance dup = new Ambulance("AMB-1", AmbulanceType.BASIC,
                new Driver("D4", "Dave", "LIC-004", "555-0004"), 12.9, 77.5);
        assertThrows(IllegalArgumentException.class,
                () -> system.registerAmbulance(dup));
    }

    @Test
    @DisplayName("Register null ambulance - negative")
    void testRegisterNullAmbulance() {
        assertThrows(IllegalArgumentException.class,
                () -> system.registerAmbulance(null));
    }

    // =========================================================
    // 2. EMERGENCY REQUEST SUBMISSION
    // =========================================================

    @Test
    @DisplayName("Submit CRITICAL emergency - positive")
    void testSubmitCriticalEmergency() throws Exception {
        EmergencyRequest req = new EmergencyRequest(
                "PAT-101", EmergencyType.CRITICAL, "MG Road", "City Hospital",
                12.9716, 77.5946);

        EmergencyRequest result = system.submitEmergencyRequest(req);

        assertNotNull(result);
        assertNotNull(result.getAssignedAmbulance());
        assertTrue(result.getEstimatedArrivalTime() > 0);
        assertEquals(EmergencyStatus.ASSIGNED, result.getStatus());
    }

    @Test
    @DisplayName("Critical emergency gets ICU ambulance - boundary")
    void testCriticalGetsICU() throws Exception {
        EmergencyRequest req = new EmergencyRequest(
                "PAT-101", EmergencyType.CRITICAL, "MG Road", "City Hospital",
                12.9716, 77.5946);
        EmergencyRequest result = system.submitEmergencyRequest(req);

        assertEquals(AmbulanceType.ICU,
                result.getAssignedAmbulance().getType());
    }

    @Test
    @DisplayName("Normal emergency can get BASIC ambulance - boundary")
    void testNormalCanGetBasic() throws Exception {
        // First, make ADVANCED and ICU unavailable
        system.getAmbulance("AMB-2").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-3").setState(AmbulanceState.DISPATCHED);

        EmergencyRequest req = new EmergencyRequest(
                "PAT-202", EmergencyType.NORMAL, "MG Road", "City Hospital",
                12.9716, 77.5946);
        EmergencyRequest result = system.submitEmergencyRequest(req);

        assertEquals(AmbulanceType.BASIC,
                result.getAssignedAmbulance().getType());
    }

    @Test
    @DisplayName("Submit null request - negative")
    void testSubmitNullRequest() {
        assertThrows(InvalidEmergencyRequestException.class,
                () -> system.submitEmergencyRequest(null));
    }

    @Test
    @DisplayName("Empty patient ID - negative")
    void testEmptyPatientId() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmergencyRequest("", EmergencyType.CRITICAL,
                        "Location", "Hospital", 12.97, 77.59));
    }

    @Test
    @DisplayName("Null emergency type - negative")
    void testNullEmergencyType() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmergencyRequest("PAT-100", null,
                        "Location", "Hospital", 12.97, 77.59));
    }

    @Test
    @DisplayName("Empty pickup location - negative")
    void testEmptyPickupLocation() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmergencyRequest("PAT-100", EmergencyType.NORMAL,
                        "  ", "Hospital", 12.97, 77.59));
    }

    @Test
    @DisplayName("Empty destination hospital - negative")
    void testEmptyDestinationHospital() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmergencyRequest("PAT-100", EmergencyType.NORMAL,
                        "Location", "", 12.97, 77.59));
    }

    // =========================================================
    // 3. AMBULANCE ALLOCATION
    // =========================================================

    @Test
    @DisplayName("Best ambulance selected based on proximity - positive")
    void testBestAmbulanceByProximity() throws Exception {
        EmergencyRequest req = new EmergencyRequest(
                "PAT-103", EmergencyType.NORMAL, "Pickup", "Hospital",
                12.9700, 77.5900);
        EmergencyRequest result = system.submitEmergencyRequest(req);

        assertEquals(AmbulanceState.DISPATCHED,
                result.getAssignedAmbulance().getState());
    }

    @Test
    @DisplayName("Ambulance cannot be assigned to two emergencies - negative")
    void testAmbulanceCannotBeDoubleAssigned() throws Exception {
        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-1", EmergencyType.CRITICAL, "L1", "H1", 12.9716, 77.5946));
        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-2", EmergencyType.HIGH, "L2", "H2", 12.9800, 77.6000));
        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-3", EmergencyType.NORMAL, "L3", "H3", 12.9900, 77.6100));

        assertEquals(0, system.getAvailableAmbulances().size());

        assertThrows(NoAmbulanceAvailableException.class, () ->
                system.submitEmergencyRequest(new EmergencyRequest(
                        "PAT-4", EmergencyType.CRITICAL, "L4", "H4",
                        12.9700, 77.5900)));
    }

    @Test
    @DisplayName("No ambulance available - negative")
    void testNoAmbulanceAvailable() {
        system.getAmbulance("AMB-1").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-2").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-3").setState(AmbulanceState.DISPATCHED);

        assertThrows(NoAmbulanceAvailableException.class, () ->
                system.submitEmergencyRequest(new EmergencyRequest(
                        "PAT-X", EmergencyType.NORMAL, "L", "H",
                        12.97, 77.59)));
    }

    // =========================================================
    // 4. WAITING QUEUE  (FIXED: declare throws Exception)
    // =========================================================

    @Test
    @DisplayName("Request is queued when no ambulance available - boundary")
    void testRequestQueued() throws Exception {
        system.getAmbulance("AMB-1").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-2").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-3").setState(AmbulanceState.DISPATCHED);

        assertThrows(NoAmbulanceAvailableException.class, () ->
                system.submitEmergencyRequest(new EmergencyRequest(
                        "PAT-Q1", EmergencyType.NORMAL, "L", "H",
                        12.97, 77.59)));

        assertEquals(1, system.getWaitingQueueSize());
        EmergencyRequest queued = system.peekWaitingQueue();
        assertNotNull(queued);
        assertEquals(EmergencyStatus.WAITING, queued.getStatus());
    }

    // FIXED: declare throws Exception since submitEmergencyRequest throws both
    @Test
    @DisplayName("Queued request auto-dispatched when ambulance becomes available - positive")
    void testQueuedRequestAutoDispatch() throws Exception {
        system.getAmbulance("AMB-1").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-2").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-3").setState(AmbulanceState.DISPATCHED);

        assertThrows(NoAmbulanceAvailableException.class, () ->
                system.submitEmergencyRequest(new EmergencyRequest(
                        "PAT-Q2", EmergencyType.NORMAL, "L", "H",
                        12.97, 77.59)));

        assertEquals(1, system.getWaitingQueueSize());

        // Free AMB-1 directly
        system.getAmbulance("AMB-1").setState(AmbulanceState.AVAILABLE);
        system.dispatchFromWaitingQueue();

        assertEquals(0, system.getWaitingQueueSize());
    }

    // =========================================================
    // 5. PRIORITY ORDERING  (FIXED: declare throws Exception)
    // =========================================================

    // FIXED: declared throws Exception to cover InvalidEmergencyRequestException
    @Test
    @DisplayName("Critical is processed before normal when queued - boundary")
    void testPriorityOrdering() throws Exception {
        system.getAmbulance("AMB-1").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-2").setState(AmbulanceState.DISPATCHED);
        system.getAmbulance("AMB-3").setState(AmbulanceState.DISPATCHED);

        assertThrows(NoAmbulanceAvailableException.class, () ->
                system.submitEmergencyRequest(new EmergencyRequest(
                        "PAT-N", EmergencyType.NORMAL, "L", "H", 12.97, 77.59)));

        assertThrows(NoAmbulanceAvailableException.class, () ->
                system.submitEmergencyRequest(new EmergencyRequest(
                        "PAT-C", EmergencyType.CRITICAL, "L", "H", 12.97, 77.59)));

        assertEquals(2, system.getWaitingQueueSize());
        EmergencyRequest head = system.peekWaitingQueue();
        assertEquals(EmergencyType.CRITICAL, head.getEmergencyType());
    }

    // =========================================================
    // 6. STATE TRANSITIONS
    // =========================================================

    @Test
    @DisplayName("Valid state transition chain - positive")
    void testValidStateTransitions() throws Exception {
        EmergencyRequest req = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-1", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));
        String ambId = req.getAssignedAmbulance().getAmbulanceId();

        system.updateAmbulanceState(ambId, AmbulanceState.EN_ROUTE);
        assertEquals(AmbulanceState.EN_ROUTE,
                system.getAmbulance(ambId).getState());

        system.updateAmbulanceState(ambId, AmbulanceState.PATIENT_PICKED_UP);
        assertEquals(AmbulanceState.PATIENT_PICKED_UP,
                system.getAmbulance(ambId).getState());

        system.updateAmbulanceState(ambId, AmbulanceState.HOSPITAL_ARRIVED);
        assertEquals(AmbulanceState.HOSPITAL_ARRIVED,
                system.getAmbulance(ambId).getState());

        system.updateAmbulanceState(ambId, AmbulanceState.AVAILABLE);
        assertEquals(AmbulanceState.AVAILABLE,
                system.getAmbulance(ambId).getState());
    }

    @Test
    @DisplayName("Invalid state transition throws exception - negative")
    void testInvalidStateTransition() throws Exception {
        EmergencyRequest req = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-1", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));
        String ambId = req.getAssignedAmbulance().getAmbulanceId();

        assertThrows(AmbulanceNotAvailableException.class, () ->
                system.updateAmbulanceState(ambId, AmbulanceState.HOSPITAL_ARRIVED));
    }

    @Test
    @DisplayName("Update non-existent ambulance - negative")
    void testUpdateNonExistentAmbulance() {
        assertThrows(AmbulanceNotAvailableException.class, () ->
                system.updateAmbulanceState("AMB-XX", AmbulanceState.EN_ROUTE));
    }

    // =========================================================
    // 7. DISTANCE & ETA CALCULATION
    // =========================================================

    @Test
    @DisplayName("Distance calculation - positive")
    void testDistanceCalculation() {
        double d0 = system.calculateDistance(12.97, 77.59, 12.97, 77.59);
        assertEquals(0.0, d0, 0.001);

        double d1 = system.calculateDistance(12.9716, 77.5946, 13.0827, 80.2707);
        assertTrue(d1 > 250 && d1 < 320,
                "Distance should be roughly 250-320 km, got " + d1);
    }

    @Test
    @DisplayName("ETA calculation - positive")
    void testEtaCalculation() {
        double eta = system.calculateEstimatedArrivalTime(
                60.0, EmergencyType.NORMAL, AmbulanceType.BASIC);
        assertEquals(120.0, eta, 0.01);

        double etaIcu = system.calculateEstimatedArrivalTime(
                60.0, EmergencyType.NORMAL, AmbulanceType.ICU);
        assertTrue(etaIcu < eta, "ICU ambulance should have lower ETA");
    }

    @Test
    @DisplayName("ETA zero distance - boundary")
    void testEtaZeroDistance() {
        double eta = system.calculateEstimatedArrivalTime(
                0.0, EmergencyType.CRITICAL, AmbulanceType.ICU);
        assertEquals(0.0, eta, 0.001);
    }

    // =========================================================
    // 8. EMERGENCY HISTORY
    // =========================================================

    @Test
    @DisplayName("History records completed emergency - positive")
    void testHistoryRecording() throws Exception {
        EmergencyRequest req = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-H1", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));
        String ambId = req.getAssignedAmbulance().getAmbulanceId();

        system.updateAmbulanceState(ambId, AmbulanceState.EN_ROUTE);
        system.updateAmbulanceState(ambId, AmbulanceState.PATIENT_PICKED_UP);
        system.updateAmbulanceState(ambId, AmbulanceState.HOSPITAL_ARRIVED);
        system.updateAmbulanceState(ambId, AmbulanceState.AVAILABLE);

        List<EmergencyRequest> history = system.getEmergencyHistory();
        assertEquals(1, history.size());
        assertEquals("PAT-H1", history.get(0).getPatientId());
        assertEquals(EmergencyStatus.COMPLETED, history.get(0).getStatus());
    }

    @Test
    @DisplayName("History empty initially - boundary")
    void testHistoryEmptyInitially() {
        assertEquals(0, system.getEmergencyHistory().size());
    }

    // =========================================================
    // 9. REQUEST CANCELLATION
    // =========================================================

    @Test
    @DisplayName("Cancel active request - positive")
    void testCancelRequest() throws Exception {
        EmergencyRequest req = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-C1", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));
        String ambId = req.getAssignedAmbulance().getAmbulanceId();

        assertTrue(system.cancelRequest(req.getRequestId()));
        assertEquals(EmergencyStatus.CANCELLED, req.getStatus());
        assertEquals(AmbulanceState.AVAILABLE,
                system.getAmbulance(ambId).getState());
    }

    @Test
    @DisplayName("Cancel non-existent request - negative")
    void testCancelNonExistent() {
        assertFalse(system.cancelRequest("REQ-XXXX"));
    }

    @Test
    @DisplayName("Cancel completed request - negative")
    void testCancelCompletedRequest() throws Exception {
        EmergencyRequest req = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-C2", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));
        String ambId = req.getAssignedAmbulance().getAmbulanceId();

        system.updateAmbulanceState(ambId, AmbulanceState.EN_ROUTE);
        system.updateAmbulanceState(ambId, AmbulanceState.PATIENT_PICKED_UP);
        system.updateAmbulanceState(ambId, AmbulanceState.HOSPITAL_ARRIVED);
        system.updateAmbulanceState(ambId, AmbulanceState.AVAILABLE);

        assertFalse(system.cancelRequest(req.getRequestId()));
    }

    // =========================================================
    // 10. DRIVER / AMBULANCE VALIDATION
    // =========================================================

    @Test
    @DisplayName("Driver null id - negative")
    void testDriverNullId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Driver(null, "Name", "LIC", "555"));
    }

    @Test
    @DisplayName("Driver empty name - negative")
    void testDriverEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Driver("D1", "", "LIC", "555"));
    }

    @Test
    @DisplayName("Ambulance null id - negative")
    void testAmbulanceNullId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Ambulance(null, AmbulanceType.BASIC, driver1, 0, 0));
    }

    @Test
    @DisplayName("Ambulance null driver - negative")
    void testAmbulanceNullDriver() {
        assertThrows(IllegalArgumentException.class, () ->
                new Ambulance("AMB-X", AmbulanceType.BASIC, null, 0, 0));
    }

    @Test
    @DisplayName("Ambulance null type - negative")
    void testAmbulanceNullType() {
        assertThrows(IllegalArgumentException.class, () ->
                new Ambulance("AMB-X", null, driver1, 0, 0));
    }

    // =========================================================
    // 11. BOUNDARY - EXACTLY MATCHING RESOURCES
    // =========================================================

    @Test
    @DisplayName("Exactly three requests fill three ambulances - boundary")
    void testExactResourceMatch() throws Exception {
        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-B1", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));
        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-B2", EmergencyType.HIGH, "L", "H", 12.9800, 77.6000));
        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-B3", EmergencyType.NORMAL, "L", "H", 12.9900, 77.6100));

        assertEquals(0, system.getAvailableAmbulances().size());
        assertEquals(3, system.getTotalRequests());
    }

    @Test
    @DisplayName("Available count after assigning ambulances - positive")
    void testAvailableCountAfterAssign() throws Exception {
        assertEquals(3, system.getAvailableAmbulances().size());

        system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-A1", EmergencyType.CRITICAL, "L", "H", 12.9716, 77.5946));

        assertEquals(2, system.getAvailableAmbulances().size());
    }

    // =========================================================
    // 12. MULTIPLE EMERGENCY REQUESTS
    // =========================================================

    @Test
    @DisplayName("Multiple requests processed in sequence - positive")
    void testMultipleRequests() throws Exception {
        EmergencyRequest r1 = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-M1", EmergencyType.CRITICAL, "L1", "H1", 12.9716, 77.5946));
        EmergencyRequest r2 = system.submitEmergencyRequest(new EmergencyRequest(
                "PAT-M2", EmergencyType.HIGH, "L2", "H2", 12.9800, 77.6000));

        assertNotEquals(r1.getAssignedAmbulance().getAmbulanceId(),
                r2.getAssignedAmbulance().getAmbulanceId());

        assertEquals(2, system.getTotalRequests());
    }

    // =========================================================
    // 13. Ambulance state direct manipulation check
    // =========================================================

    @Test
    @DisplayName("New ambulance is AVAILABLE by default - positive")
    void testAmbulanceDefaultState() {
        Ambulance a = new Ambulance("AMB-NEW", AmbulanceType.BASIC,
                driver1, 0, 0);
        assertEquals(AmbulanceState.AVAILABLE, a.getState());
        assertTrue(a.isAvailable());
    }

    @Test
    @DisplayName("Location update - positive")
    void testAmbulanceLocationUpdate() {
        Ambulance a = system.getAmbulance("AMB-1");
        a.updateLocation(13.0, 78.0);
        assertEquals(13.0, a.getCurrentLatitude(), 0.001);
        assertEquals(78.0, a.getCurrentLongitude(), 0.001);
    }
}
