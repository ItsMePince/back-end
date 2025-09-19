package my_financial_app.demo.Config;

import my_financial_app.demo.Entity.User;
import my_financial_app.demo.Entity.Role;
import my_financial_app.demo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            initializeUsers();
            System.out.println("✅ Sample data initialized successfully!");
        } else {
            System.out.println("📋 Database already has data, skipping initialization");
        }
    }

    private void initializeUsers() {
        User admin = new User("admin", "admin", "admin@example.com");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User user = new User("user", "password", "user@example.com");
        user.setRole(Role.USER);
        userRepository.save(user);

        User user2 = new User("jane", "password123", "jane@example.com");
        user2.setRole(Role.USER);
        userRepository.save(user2);

        User user3 = new User("bob", "mypassword", "bob@example.com");
        user3.setRole(Role.USER);
        userRepository.save(user3);

        User user4 = new User("alice", "alicepass", "alice@example.com");
        user4.setRole(Role.USER);
        userRepository.save(user4);

        System.out.println("Created sample users:");
        System.out.println("- Admin: username=admin, password=admin");
        System.out.println("- User: username=user, password=password");
        System.out.println("- User: username=jane, password=password123");
        System.out.println("- User: username=bob, password=mypassword");
        System.out.println("- User: username=alice, password=alicepass");
    }
}
