package kaasu_creator.model;

import java.math.BigDecimal;
import java.sql.Date;

public class Income {
    private Long id;
    private Long userId;
    private String incomeType;
    private String source;
    private String category;
    private BigDecimal amount;
    private Date incomeDate;
    private String notes;

    public Income() {}

    public Income(Long id, Long userId, String incomeType, String source, String category,
                  BigDecimal amount, Date incomeDate, String notes) {
        this.id = id;
        this.userId = userId;
        this.incomeType = incomeType;
        this.source = source;
        this.category = category;
        this.amount = amount;
        this.incomeDate = incomeDate;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getIncomeType() { return incomeType; }
    public String getSource() { return source; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public Date getIncomeDate() { return incomeDate; }
    public String getNotes() { return notes; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setIncomeType(String incomeType) { this.incomeType = incomeType; }
    public void setSource(String source) { this.source = source; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setIncomeDate(Date incomeDate) { this.incomeDate = incomeDate; }
    public void setNotes(String notes) { this.notes = notes; }
}
