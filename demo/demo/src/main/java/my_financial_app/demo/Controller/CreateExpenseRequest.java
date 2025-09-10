package my_financial_app.demo.Controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateExpenseRequest {
    @NotBlank public String type;      // "ค่าใช้จ่าย" หรือ "รายได้" (frontend)
    @NotBlank public String category;
    @NotNull  public Double amount;
    @NotBlank public String note;
    @NotBlank public String place;
    @NotBlank public String date;      // "YYYY-MM-DD"
    public String paymentMethod;       // optional
    public String iconKey;             // optional (custom icon)
}
