package dev.example.fieldservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceEnrollmentTest {
    @Test
    void unverifiedCompanyCannotJoinOrCreateUser() throws Exception {
        FakeDirectory gateway = new FakeDirectory(false);
        WorkspaceEnrollment service = new WorkspaceEnrollment(gateway);
        var request = new WorkspaceEnrollment.Enrollment("school.example", "ada@school.example", "Ada", "join-42",
                new WorkspaceEnrollment.WorkOrder("WO-42", "dispatched", List.of("photo-42"), "Call customer Friday"));

        assertThrows(IllegalArgumentException.class, () -> service.enroll(request));
        assertEquals("zone-42", gateway.listedZone);
        assertFalse(gateway.created);
    }

    @Test
    void verifiedCompanyPassesWorkOrderToTechnician() throws Exception {
        FakeDirectory gateway = new FakeDirectory(true);
        var request = new WorkspaceEnrollment.Enrollment("school.example", "ada@school.example", "Ada", "join-42",
                new WorkspaceEnrollment.WorkOrder("WO-42", "dispatched", List.of("photo-42"), "Call customer Friday"));

        var result = new WorkspaceEnrollment(gateway).enroll(request);
        assertEquals("dispatched", result.dispatchStatus());
        assertEquals(List.of("photo-42"), result.photoReferences());
        assertTrue(gateway.created);
        assertEquals("join-42", gateway.requestId);
    }

    @Test
    void readsZoneIdentifierFromLiveVendorPayloadShape() {
        FakeDirectory gateway = new FakeDirectory(true, true);
        var request = new WorkspaceEnrollment.Enrollment("school.example", "ada@school.example", "Ada", "join-42",
                new WorkspaceEnrollment.WorkOrder("WO-42", "dispatched", List.of("photo-42"), "Call customer Friday"));

        new WorkspaceEnrollment(gateway).enroll(request);

        assertEquals("zone-42", gateway.listedZone);
    }

    @Test
    void invalidUpstreamResponseIsStructuredBadGateway() {
        var response = new FieldServiceApplication(null)
                .unavailable(new IllegalStateException("Registered zone has no identifier"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(Map.of("error", "UPSTREAM_RESPONSE_INVALID",
                "message", "Registered zone has no identifier"), response.getBody());
    }

    private static class FakeDirectory implements DirectoryPort {
        final boolean verified;
        boolean created;
        String listedZone;
        String requestId;
        FakeDirectory(boolean verified) { this.verified = verified; }
        FakeDirectory(boolean verified, boolean vendorPayload) {
            this.verified = verified;
            this.vendorPayload = vendorPayload;
        }
        boolean vendorPayload;
        public com.fasterxml.jackson.databind.JsonNode getDomain(String domain) {
            if (vendorPayload) {
                var mapper = new ObjectMapper();
                var response = mapper.createObjectNode();
                response.putObject("raw_vendor_payload").putArray("result")
                        .addObject().put("id", "zone-42");
                return response;
            }
            return new ObjectMapper().createObjectNode().put("zone_id", "zone-42");
        }
        public com.fasterxml.jackson.databind.JsonNode listRecords(String zoneId) {
            listedZone = zoneId;
            return new ObjectMapper().createArrayNode();
        }
        public com.fasterxml.jackson.databind.JsonNode verifyDomain(String domain) {
            return new ObjectMapper().createObjectNode().put("verified", verified);
        }
        public com.fasterxml.jackson.databind.JsonNode createUser(String email, String name, String workspace, String id) {
            created = true;
            requestId = id;
            return new ObjectMapper().createObjectNode();
        }
    }
}
