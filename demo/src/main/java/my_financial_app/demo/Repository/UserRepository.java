package my_financial_app.demo.Repository;

import my_financial_app.demo.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Lookups ---
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // หาได้ทั้ง username หรือ email (ส่งค่าเดียวกันซ้ำสองพารามิเตอร์ได้)
    Optional<User> findByUsernameOrEmail(String username, String email);

    // --- Uniqueness checks ---
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // --- Stats ---
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin IS NOT NULL AND u.lastLogin >= CURRENT_DATE")
    long countActiveUsersToday();
}
