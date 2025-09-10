package my_financial_app.demo.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import my_financial_app.demo.Entity.User;
import my_financial_app.demo.Repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
// รองรับ fetch(..., { credentials: 'include' })
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // --------- LOGIN ----------
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            // ใช้ช่องเดียวเป็นตัวระบุ จะกรอก username หรือ email ก็ได้
            String identifier = safeTrim(request.getUsername());
            String password   = safeTrim(request.getPassword());

            if (isBlank(identifier) || isBlank(password)) {
                response.put("success", false);
                response.put("message", "กรุณากรอกชื่อผู้ใช้/อีเมล และรหัสผ่าน");
                return ResponseEntity.badRequest().body(response);
            }

            // หา user ได้ทั้งสองทาง: username หรือ email
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // เดโม: เทียบ plain text (โปรดเปลี่ยนเป็น BCrypt ในโปรดักชัน)
                if (password.equals(user.getPassword())) {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);

                    // ✅ สร้าง/รียูส session แล้วเก็บ username ไว้
                    HttpSession session = httpRequest.getSession(true);
                    session.setAttribute("username", user.getUsername());

                    response.put("success", true);
                    response.put("message", "Login successful");
                    response.put("user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole() != null ? user.getRole().toString() : "USER"
                    ));
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("message", "รหัสผ่านไม่ถูกต้อง");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            response.put("success", false);
            response.put("message", "ไม่พบบัญชีผู้ใช้");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // --------- REGISTER ----------
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = safeTrim(request.getUsername());
            String email    = safeTrim(request.getEmail());
            String password = safeTrim(request.getPassword());

            // validation พื้นฐาน
            if (isBlank(username) || isBlank(email) || isBlank(password)) {
                response.put("success", false);
                response.put("message", "กรุณากรอกข้อมูลให้ครบถ้วน");
                return ResponseEntity.badRequest().body(response);
            }
            if (password.length() < 6) {
                response.put("success", false);
                response.put("message", "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร");
                return ResponseEntity.badRequest().body(response);
            }

            // unique checks
            if (userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }
            if (userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // สร้างผู้ใช้ใหม่ (ไม่มี name)
            User newUser = new User(username, password, email);
            User savedUser = userRepository.save(newUser);

            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("user", Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail(),
                    "role", savedUser.getRole() != null ? savedUser.getRole().toString() : "USER"
            ));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ให้ frontend เรียก /signup ได้ด้วย (ใช้ logic เดียวกับ /register)
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody RegisterRequest request) {
        return register(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) session.invalidate(); // ✅ ลบ session
        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole() != null ? user.getRole().toString() : "USER");
            response.put("createdAt", user.getCreatedAt());
            response.put("lastLogin", user.getLastLogin());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // --------- utils ----------
    private static String safeTrim(String s) { return s == null ? null : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}

// ------- DTOs --------
class LoginRequest {
    private String username; // ใช้เป็นตัวระบุ: ใส่ได้ทั้ง username หรือ email
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class RegisterRequest {
    private String username;
    private String password;
    private String email;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
