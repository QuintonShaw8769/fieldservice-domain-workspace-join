package dev.example.fieldservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiGateway implements DirectoryPort {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;
    private final String key;

    public InfraiGateway(@Value("${infrai.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    }

    public JsonNode getDomain(String domain) {
        return request("GET", "/v1/dns/domain/get?domain=" + URLEncoder.encode(domain, StandardCharsets.UTF_8), null);
    }

    public JsonNode listRecords(String zoneId) {
        return request("GET", "/v1/dns/record/list?zone_id=" + URLEncoder.encode(zoneId, StandardCharsets.UTF_8), null);
    }

    public JsonNode verifyDomain(String domain) {
        return post("/v1/dns/domain/verify", Map.of("domain", domain));
    }

    public JsonNode createUser(String email, String name, String workspace, String requestId) {
        return post("/v1/auth/user/create", Map.of("email", email, "name", name,
                "metadata", Map.of("workspace", workspace), "idempotency_key", requestId));
    }

    private JsonNode post(String path, Map<String, ?> body) {
        return request("POST", path, body);
    }

    private JsonNode request(String method, String path, Map<String, ?> body) {
        try {
            String payload = body == null ? "" : json.writeValueAsString(body);
            for (int attempt = 0; attempt < 4; attempt++) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json")
                        .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    long seconds = response.headers().firstValue("Retry-After")
                            .flatMap(value -> {
                                try { return java.util.Optional.of(Long.parseLong(value)); }
                                catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
                            }).orElse(1L << attempt);
                    Thread.sleep(Math.min(30, Math.max(1, seconds)) * 1000);
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new ApiError(error.path("code").asText("REQUEST_REJECTED"),
                            error.path("message").asText("Request rejected"), response.statusCode());
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Remote request failed");
                return envelope.path("data");
            }
            throw new IllegalStateException("Retry limit reached");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", e);
        }
    }

    public static class ApiError extends RuntimeException {
        public final String code;
        public final int status;
        public ApiError(String code, String message, int status) {
            super(message);
            this.code = code;
            this.status = status;
        }
    }
}
