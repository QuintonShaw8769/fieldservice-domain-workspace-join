package dev.example.fieldservice;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEnrollment {
    private final DirectoryPort infrai;

    public WorkspaceEnrollment(DirectoryPort infrai) { this.infrai = infrai; }

    public record WorkOrder(String number, String dispatchStatus, List<String> photoReferences,
                            String technicianFollowUp) {}
    public record Enrollment(String workspace, String email, String name, String requestId,
                             WorkOrder workOrder) {}
    public record Result(String workspace, String technician, String dispatchStatus,
                         List<String> photoReferences, String technicianFollowUp) {}

    public Result enroll(Enrollment input) {
        if (input.workspace() == null || input.email() == null || input.name() == null
                || input.requestId() == null || input.workOrder() == null
                || input.workOrder().number() == null || input.workOrder().dispatchStatus() == null
                || input.workOrder().photoReferences() == null || input.workOrder().technicianFollowUp() == null)
            throw new IllegalArgumentException("Complete enrollment and work order fields are required");
        String domain = input.workspace().toLowerCase(Locale.ROOT);
        if (domain.isBlank() || !input.email().toLowerCase(Locale.ROOT).endsWith("@" + domain)
                || input.requestId().isBlank())
            throw new IllegalArgumentException("Email domain must match the workspace; requestId is required");

        JsonNode zone = infrai.getDomain(domain);
        String zoneId = zone.path("zone_id").asText();
        if (zoneId.isBlank()) {
            zoneId = zone.path("raw_vendor_payload").path("result").path(0).path("id").asText();
        }
        if (zoneId.isBlank()) throw new IllegalStateException("Registered zone has no identifier");
        infrai.listRecords(zoneId);
        JsonNode proof = infrai.verifyDomain(domain);
        if (!proof.path("verified").asBoolean(false))
            throw new IllegalArgumentException("Domain ownership must be verified before joining");

        infrai.createUser(input.email(), input.name(), domain, input.requestId());
        WorkOrder order = input.workOrder();
        return new Result(domain, input.email(), order.dispatchStatus(),
                List.copyOf(order.photoReferences()), order.technicianFollowUp());
    }
}
