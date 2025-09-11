package my_financial_app.demo.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import my_financial_app.demo.Entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // เดิม
    List<Expense> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    // ✅ ใหม่: กรองตาม userId ทั้งหมด
    List<Expense> findByUserIdOrderByDateDesc(Long userId);

    // ✅ ใหม่: กรองตาม userId + ช่วงวันที่
    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);
}
