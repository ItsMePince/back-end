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

/**
 * NOTE:
 * - รับวันที่จาก front-end ผ่านฟิลด์ req.date (string)
 * - ใช้ parseDateFlexible(...) เพื่อพาร์สได้ทั้ง "yyyy-MM-dd" และ "dd/MM/yyyy"
 */
@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(
    origins = {"http://localhost:3000"},   // ปรับให้ตรง origin ของ React
    allowCredentials = "true"              // ต้องเปิดถ้าใช้ session/cookie
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
        // --- ดึง username จาก session (ตั้งไว้ตอน login) ---
        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : null;
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized: no login session");
        }

        // --- หา User จาก DB แล้วผูกเป็นเจ้าของรายการ ---
        User owner = userRepo.findByUsername(username)
                .orElse(null);
        if (owner == null) {
            return ResponseEntity.status(404).body("User not found: " + username);
        }

        // DEBUG: log ค่า date ที่เข้ามา
        System.out.println("[POST /api/expenses] user=" + username + ", raw date=" + req.date);

        Expense e = new Expense();
        e.setUser(owner); // 🔗 สำคัญ: ผูก FK ไปที่ users.id

        // ไทย -> Enum
        Expense.EntryType entryType = "รายได้".equals(req.type)
                ? Expense.EntryType.INCOME
                : Expense.EntryType.EXPENSE;

        e.setType(entryType);
        e.setCategory(req.category);
        e.setAmount(BigDecimal.valueOf(req.amount));
        e.setNote(req.note);
        e.setPlace(req.place);

        // ✅ พาร์สวันที่แบบยืดหยุ่น
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

    // shortcut: สร้างเสมอเป็น INCOME
    @PostMapping("/incomes")
    public ResponseEntity<?> createIncome(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        req.type = "รายได้";
        return create(req, request);
    }

    // shortcut: สร้างเสมอเป็น EXPENSE
    @PostMapping("/spendings")
    public ResponseEntity<?> createExpense(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        req.type = "ค่าใช้จ่าย";
        return create(req, request);
    }

    @GetMapping
    public List<Expense> listAll() {
        return repo.findAll();
    }

    @GetMapping("/range")
    public List<Expense> listByRange(@RequestParam String start, @RequestParam String end) {
        LocalDate s = parseDateFlexible(start);
        LocalDate e = parseDateFlexible(end);
        return repo.findByDateBetweenOrderByDateDesc(s, e);
    }

    /**
     * พาร์สวันที่แบบยืดหยุ่น:
     * - รูปแบบหลัก: yyyy-MM-dd (เช่น 2025-09-08)  <-- มาตรฐานจาก <input type="date">
     * - รองรับ: d/M/uuuu (เช่น 8/9/2025)
     */
    private static LocalDate parseDateFlexible(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // 1) yyyy-MM-dd
        try {
            DateTimeFormatter iso = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                                                     .withResolverStyle(ResolverStyle.STRICT);
            return LocalDate.parse(s, iso);
        } catch (Exception ignore) {}

        // 2) d/M/uuuu
        try {
            DateTimeFormatter dmY = DateTimeFormatter.ofPattern("d/M/uuuu", Locale.US)
                                                     .withResolverStyle(ResolverStyle.STRICT);
            return LocalDate.parse(s, dmY);
        } catch (Exception ignore) {}

        // 3) สุดท้าย: พยายาม parse แบบ default (อาจล้มเหลว)
        return LocalDate.parse(s);
    }
}
