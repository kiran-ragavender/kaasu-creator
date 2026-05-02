package kaasu_creator.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Expense {
    private Long id;
    private Long userId;
    private String title;
    private String category;
    private BigDecimal amount;
    private LocalDateTime date;

    public Expense() {}

    public Expense(Long id, Long userId, String title, String category, BigDecimal amount, LocalDateTime date) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getDate() { return date; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDateTime date) { this.date = date; }
}