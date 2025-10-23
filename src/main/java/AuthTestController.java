import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gateway.security.GoogleTokenService;

@RestController
public class AuthTestController {

    @Autowired
    private GoogleTokenService googleTokenService;

    @GetMapping("/auth/test")
    public ResponseEntity<?> testAuth() {
        if (googleTokenService.isInitialized()) {
            String token = googleTokenService.getAccessToken();
            return ResponseEntity.ok(Map.of(
                "status", "Service Account Working",
                "token_preview", token != null ? token.substring(0, 50) + "..." : "null",
                "initialized", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "Service Account Not Available",
                "initialized", false
            ));
        }
    }
}