package dev.example.fieldservice;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class FieldServiceApplication {
    private final WorkspaceEnrollment enrollment;
    public FieldServiceApplication(WorkspaceEnrollment enrollment) { this.enrollment = enrollment; }
    public static void main(String[] args) { SpringApplication.run(FieldServiceApplication.class, args); }

    @PostMapping("/enroll")
    public WorkspaceEnrollment.Result enroll(@RequestBody WorkspaceEnrollment.Enrollment request) {
        return enrollment.enroll(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(InfraiGateway.ApiError.class)
    public ResponseEntity<Map<String, String>> rejected(InfraiGateway.ApiError error) {
        HttpStatus status = error.status >= 400 && error.status < 500
                ? HttpStatus.valueOf(error.status) : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", error.code, "message", error.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> unavailable(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "UPSTREAM_RESPONSE_INVALID", "message", error.getMessage()));
    }
}
