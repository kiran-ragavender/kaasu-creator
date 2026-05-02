package kaasu_creator.dao;

import java.sql.PreparedStatement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import kaasu_creator.model.Goal;

@Repository
public class GoalDao {

    private final JdbcTemplate jdbc;

    public GoalDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper converts a database row into a Goal object
    private final RowMapper<Goal> goalRowMapper = (rs, rowNum) -> new Goal(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("name"),
        rs.getBigDecimal("target_amount"),
        rs.getBigDecimal("current_amount"),
        rs.getDate("deadline").toLocalDate(),
        rs.getTimestamp("created_at")
    );

    // INSERT a new goal and return the generated ID
    public Long save(Goal goal) {
        String sql = "INSERT INTO goals (user_id, name, target_amount, current_amount, deadline) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, goal.getUserId());
            ps.setString(2, goal.getName());
            ps.setBigDecimal(3, goal.getTargetAmount());
            ps.setBigDecimal(4, goal.getCurrentAmount());
            ps.setObject(5, goal.getDeadline());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    // FIND all goals for a specific user
    @SuppressWarnings("unchecked")
    public List<Goal> findByUserId(Long userId) {
        String sql = "SELECT * FROM goals WHERE user_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, goalRowMapper, userId);
    }

    // FIND a single goal by ID
    @SuppressWarnings("unchecked")
    public Goal findById(Long id) {
        String sql = "SELECT * FROM goals WHERE id = ?";
        return jdbc.query(sql, goalRowMapper, id).stream().findFirst().orElse(null);
    }

    // DELETE a goal by ID
    public void deleteById(Long goalId) {
        jdbc.update("DELETE FROM goals WHERE id = ?", goalId);
    }

    // UPDATE the current_amount of a goal (when user adds savings)
    public void updateCurrentAmount(Long goalId, java.math.BigDecimal newAmount) {
        String sql = "UPDATE goals SET current_amount = ? WHERE id = ?";
        jdbc.update(sql, newAmount, goalId);
    }

    // UPDATE the current_amount incrementally
    public void addToCurrentAmount(Long goalId, java.math.BigDecimal amountToAdd) {
        String sql = "UPDATE goals SET current_amount = current_amount + ? WHERE id = ?";
        jdbc.update(sql, amountToAdd, goalId);
    }
}