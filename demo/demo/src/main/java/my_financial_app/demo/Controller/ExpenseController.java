// src/main/java/my_financial_app/demo/Controller/ExpenseController.java
package my_financial_app.demo.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import my_financial_app.demo.Entity.Expense;
import my_financial_app.demo.Entity.User;
import my_financial_app.demo.Repository.ExpenseRepository;
import my_financial_app.demo.Repository.UserRepository;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(
    origins = {"http://localhost:3000","http://localhost:5173"},
    allowCredentials = "true"
)
public class ExpenseController {

    private final ExpenseRepository repo;
    private final UserRepository userRepo;

    public ExpenseController(ExpenseRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Expense e = new Expense();
        e.setUser(owner);
        Expense.EntryType entryType = "รายได้".equals(req.type)
                ? Expense.EntryType.INCOME
                : Expense.EntryType.EXPENSE;
        e.setType(entryType);
        e.setCategory(req.category);
        e.setAmount(BigDecimal.valueOf(req.amount));
        e.setNote(req.note);
        e.setPlace(req.place);
        e.setDate(parseDateFlexible(req.date));
        e.setPaymentMethod(req.paymentMethod);
        e.setIconKey(req.iconKey);
        Expense saved = repo.save(e);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/incomes")
    public ResponseEntity<?> createIncome(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        req.type = "รายได้";
        return create(req, request);
    }

    @PostMapping("/spendings")
    public ResponseEntity<?> createExpense(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        req.type = "ค่าใช้จ่าย";
        return create(req, request);
    }

    @GetMapping
    public ResponseEntity<?> listMine(HttpServletRequest request) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        List<Expense> result = repo.findByUserIdOrderByDateDesc(owner.getId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/range")
    public ResponseEntity<?> listByRange(
            @RequestParam String start,
            @RequestParam String end,
            HttpServletRequest request
    ) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        LocalDate s = parseDateFlexible(start);
        LocalDate e = parseDateFlexible(end);
        List<Expense> result = repo.findByUserIdAndDateBetweenOrderByDateDesc(owner.getId(), s, e);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOne(
            @PathVariable Long id,
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Optional<Expense> opt = repo.findByIdAndUserId(id, owner.getId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }
        Expense e = opt.get();
        Expense.EntryType entryType = "รายได้".equals(req.type)
                ? Expense.EntryType.INCOME
                : Expense.EntryType.EXPENSE;
        e.setType(entryType);
        e.setCategory(req.category);
        e.setAmount(BigDecimal.valueOf(req.amount));
        e.setNote(req.note);
        e.setPlace(req.place);
        e.setDate(parseDateFlexible(req.date));
        e.setPaymentMethod(req.paymentMethod);
        e.setIconKey(req.iconKey);
        Expense saved = repo.save(e);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Optional<Expense> opt = repo.findByIdAndUserId(id, owner.getId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private User requireLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : null;
        if (username == null || username.isBlank()) return null;
        return userRepo.findByUsername(username).orElse(null);
    }

    private static LocalDate parseDateFlexible(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        try {
            DateTimeFormatter iso = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                                                     .withResolverStyle(ResolverStyle.STRICT);
            return LocalDate.parse(s, iso);
        } catch (Exception ignore) {}
        try {
            DateTimeFormatter dmY = DateTimeFormatter.ofPattern("d/M/uuuu", Locale.US)
                                                     .withResolverStyle(ResolverStyle.STRICT);
            return LocalDate.parse(s, dmY);
        } catch (Exception ignore) {}
        return LocalDate.parse(s);
    }

    public static class CreateExpenseRequest {
        public String type;
        public String category;
        public double amount;
        public String note;
        public String place;
        public String date;
        public String paymentMethod;
        public String iconKey;
    }
}
