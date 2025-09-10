package my_financial_app.demo.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000") // ระบุ origin ตรงนี้
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowCredentials(true); // เพิ่มอันนี้ด้วย
    }
}
