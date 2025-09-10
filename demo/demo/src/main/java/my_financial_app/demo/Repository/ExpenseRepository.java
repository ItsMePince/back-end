package my_financial_app.demo.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import my_financial_app.demo.Entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // ใน Repository
List<Expense> findByUserUsernameOrderByDateDesc(String username);
    List<Expense> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);
}
