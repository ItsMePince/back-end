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

    /* ----------------- CREATE ----------------- */
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized: no login session");
        }

        System.out.println("[POST /api/expenses] user=" + owner.getUsername() + ", raw date=" + req.date);

        Expense e = new Expense();
        e.setUser(owner); // 🔗 ผูก FK

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

    // shortcut: INCOME
    @PostMapping("/incomes")
    public ResponseEntity<?> createIncome(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        req.type = "รายได้";
        return create(req, request);
    }

    // shortcut: EXPENSE
    @PostMapping("/spendings")
    public ResponseEntity<?> createExpense(
            @Valid @RequestBody CreateExpenseRequest req,
            HttpServletRequest request
    ) {
        req.type = "ค่าใช้จ่าย";
        return create(req, request);
    }

    /* ----------------- READ (FILTERED BY LOGIN USER) ----------------- */

    // ✅ ดึงทั้งหมดของ "ผู้ใช้ที่ล็อกอินเท่านั้น"
    @GetMapping
    public ResponseEntity<?> listMine(HttpServletRequest request) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized: no login session");
        }
        List<Expense> result = repo.findByUserIdOrderByDateDesc(owner.getId());
        return ResponseEntity.ok(result);
    }

    // ✅ ดึงตามช่วงวันที่ ของ "ผู้ใช้ที่ล็อกอินเท่านั้น"
    @GetMapping("/range")
    public ResponseEntity<?> listByRange(
            @RequestParam String start,
            @RequestParam String end,
            HttpServletRequest request
    ) {
        User owner = requireLoginUser(request);
        if (owner == null) {
            return ResponseEntity.status(401).body("Unauthorized: no login session");
        }

        LocalDate s = parseDateFlexible(start);
        LocalDate e = parseDateFlexible(end);

        List<Expense> result = repo.findByUserIdAndDateBetweenOrderByDateDesc(owner.getId(), s, e);
        return ResponseEntity.ok(result);
    }

    /* ----------------- HELPERS ----------------- */

    /** ดึง User จาก session ("username") ถ้าไม่มีให้คืน null */
    private User requireLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : null;
        if (username == null || username.isBlank()) return null;
        return userRepo.findByUsername(username).orElse(null);
    }

    /**
     * พาร์สวันที่แบบยืดหยุ่น:
     * - รูปแบบหลัก: yyyy-MM-dd (เช่น 2025-09-08)
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

        // 3) default
        return LocalDate.parse(s);
    }
}
