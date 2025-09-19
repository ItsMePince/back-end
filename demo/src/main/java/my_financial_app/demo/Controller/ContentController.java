package my_financial_app.demo.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import my_financial_app.demo.Entity.User;
import my_financial_app.demo.Repository.UserRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ContentController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/profile/{id}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
     
            profile.put("email", user.getEmail());
            profile.put("username", user.getUsername());
            profile.put("role", user.getRole().toString());
            profile.put("memberSince", user.getCreatedAt());
            profile.put("lastLogin", user.getLastLogin());
            
            return ResponseEntity.ok(profile);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // ข้อมูลจริงจาก database
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countActiveUsersToday();
            
            // ข้อมูล mock สำหรับ demo (ในการใช้งานจริงควรมี entity อื่นๆ)
            Random random = new Random();
            long totalOrders = 500 + random.nextInt(500);
            double revenue = 50000 + (random.nextDouble() * 100000);
            
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("totalOrders", totalOrders);
            stats.put("revenue", Math.round(revenue * 100.0) / 100.0);
            stats.put("userGrowth", calculateUserGrowth());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            // Fallback เมื่อมีปัญหา
            Map<String, Object> fallbackStats = new HashMap<>();
            fallbackStats.put("totalUsers", 0);
            fallbackStats.put("activeUsers", 0);
            fallbackStats.put("totalOrders", 0);
            fallbackStats.put("revenue", 0.0);
            fallbackStats.put("error", "Unable to fetch stats");
            
            return ResponseEntity.ok(fallbackStats);
        }
    }

    @GetMapping("/users/list")
    public ResponseEntity<Map<String, Object>> getUsersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            // ในตัวอย่างง่ายๆ เอาทั้งหมดก่อน (ไม่ใช้ pagination ยัง)
            var users = userRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
            
                userInfo.put("email", user.getEmail());
                userInfo.put("role", user.getRole().toString());
                userInfo.put("createdAt", user.getCreatedAt());
                userInfo.put("lastLogin", user.getLastLogin());
                return userInfo;
            }).toList());
            
            response.put("total", users.size());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unable to fetch users: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/public/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // ตรวจสอบการเชื่อมต่อ database
            long userCount = userRepository.count();
            
            response.put("status", "OK");
            response.put("message", "API is running");
            response.put("database", "Connected");
            response.put("userCount", userCount);
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            response.put("status", "WARNING");
            response.put("message", "API is running but database may have issues");
            response.put("database", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    private double calculateUserGrowth() {
        // Mock calculation สำหรับการเจริญเติบโตของผู้ใช้
        // ในการใช้งานจริงควรเปรียบเทียบกับข้อมูลช่วงเวลาก่อนหน้า
        Random random = new Random();
        return Math.round((5.0 + (random.nextDouble() * 15.0)) * 100.0) / 100.0;
    }
}