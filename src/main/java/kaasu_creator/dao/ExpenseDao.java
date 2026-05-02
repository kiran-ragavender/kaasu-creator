package kaasu_creator.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import kaasu_creator.model.Expense;

@Repository
public class ExpenseDao {

    private final JdbcTemplate jdbc;

    public ExpenseDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper converts a database row into an Expense object
    private final RowMapper<Expense> expenseRowMapper = (rs, rowNum) -> new Expense(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("title"),
        rs.getString("category"),
        rs.getBigDecimal("amount"),
        rs.getTimestamp("date") != null ? rs.getTimestamp("date").toLocalDateTime() : null
    );

    // INSERT a new expense
    public void save(Expense expense) {
        String sql = "INSERT INTO expenses (user_id, title, category, amount) VALUES (?, ?, ?, ?)";
        jdbc.update(sql, expense.getUserId(), expense.getTitle(), expense.getCategory(), expense.getAmount());
    }

    // FIND all expenses for a specific user, ordered by date (newest first)
    @SuppressWarnings("unchecked")
    public List<Expense> findByUserId(Long userId) {
        String sql = "SELECT * FROM expenses WHERE user_id = ? ORDER BY date DESC";
        return jdbc.query(sql, expenseRowMapper, userId);
    }

    // SUM all expenses for a specific user
    public BigDecimal sumByUserId(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE user_id = ?";
        return jdbc.queryForObject(sql, BigDecimal.class, userId);
    }

    // DELETE an expense only if it belongs to the given user (prevents unauthorized deletion)
    public void deleteByIdAndUserId(Long id, Long userId) {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        jdbc.update(sql, id, userId);
    }
}