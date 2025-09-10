package my_financial_app.demo.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    origins = {"http://localhost:3000"},
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
        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : null;
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized: no login session");
        }

        User owner = userRepo.findByUsername(username).orElse(null);
        if (owner == null) {
            return ResponseEntity.status(404).body("User not found: " + username);
        }

        System.out.println("[POST /api/expenses] user=" + username + ", raw date=" + req.date);

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

        LocalDate parsed = parseDateFlexible(req.date);
        e.setDate(parsed);

        e.setPaymentMethod(req.paymentMethod);
        e.setIconKey(req.iconKey);

        Expense saved = repo.save(e);
        System.out.println("[POST /api/expenses] saved id=" + saved.getId() +
                           ", userId=" + owner.getId() +
                           ", date=" + saved.getDate());

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

    /* ==================== แก้ส่วน GET ให้เป็นของผู้ใช้ปัจจุบันเท่านั้น ==================== */

    @GetMapping
    public ResponseEntity<?> listMine(HttpServletRequest request) {
        String username = getUsernameFromSession(request);
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized: no login session");
        }
        // ต้องมีเมธอดใน Repository: findByUserUsernameOrderByDateDesc
        List<Expense> myList = repo.findByUserUsernameOrderByDateDesc(username);
        return ResponseEntity.ok(myList);
    }


    /* ==================== helpers ==================== */

    private static String getUsernameFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (String) session.getAttribute("username") : null;
    }

    /**
     * พาร์สวันที่แบบยืดหยุ่น:
     * - รูปแบบหลัก: yyyy-MM-dd
     * - รองรับ: d/M/uuuu
     */
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

        return LocalDate.parse(s); // fallback
    }
}
