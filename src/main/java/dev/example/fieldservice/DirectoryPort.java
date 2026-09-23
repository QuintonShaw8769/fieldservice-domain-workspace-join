package dev.example.fieldservice;

import com.fasterxml.jackson.databind.JsonNode;

public interface DirectoryPort {
    JsonNode getDomain(String domain);
    JsonNode listRecords(String zoneId);
    JsonNode verifyDomain(String domain);
    JsonNode createUser(String email, String name, String workspace, String requestId);
}
