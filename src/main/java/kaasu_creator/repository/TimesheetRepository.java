package kaasu_creator.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import kaasu_creator.model.TimesheetEntry;

@Repository
public class TimesheetRepository {

    private final JdbcTemplate jdbc;

    public TimesheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @SuppressWarnings("null")
    private final RowMapper<TimesheetEntry> rowMapper = (rs, rowNum) -> new TimesheetEntry(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getObject("job_id", Long.class),
        rs.getDate("work_date"),
        rs.getBigDecimal("hours_worked"),
        rs.getString("notes"),
        rs.getTimestamp("created_at")
    );

    @SuppressWarnings("null")
    private final RowMapper<TimesheetEntry> enrichedRowMapper = (rs, rowNum) -> {
        BigDecimal hours = rs.getBigDecimal("hours_worked");
        BigDecimal wage = rs.getBigDecimal("hourly_wage");
        BigDecimal earned = BigDecimal.ZERO;
        if (hours != null && wage != null) {
            earned = hours.multiply(wage);
        }
        return new TimesheetEntry(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getObject("job_id", Long.class),
            rs.getDate("work_date"),
            hours,
            rs.getString("notes"),
            rs.getTimestamp("created_at"),
            rs.getString("job_name"),
            wage,
            earned
        );
    };

    public void save(TimesheetEntry entry) {
        String sql = "INSERT INTO timesheet_entries (user_id, job_id, work_date, hours_worked, notes) VALUES (?, ?, ?, ?, ?)";
        jdbc.update(sql,
            entry.getUserId(),
            entry.getJobId(),
            entry.getWorkDate(),
            entry.getHoursWorked(),
            entry.getNotes()
        );
    }

    @SuppressWarnings("null")
    public List<TimesheetEntry> findByUserId(Long userId) {
        String sql = "SELECT te.id, te.user_id, te.job_id, te.work_date, te.hours_worked, te.notes, te.created_at, " +
                     "j.job_name, j.hourly_wage " +
                     "FROM timesheet_entries te " +
                     "JOIN jobs j ON te.job_id = j.id " +
                     "WHERE te.user_id = ? " +
                     "ORDER BY te.work_date DESC, te.id DESC";
        return jdbc.query(sql, enrichedRowMapper, userId);
    }

    public int deleteByIdAndUserId(Long id, Long userId) {
        String sql = "DELETE FROM timesheet_entries WHERE id = ? AND user_id = ?";
        return jdbc.update(sql, id, userId);
    }

    public BigDecimal sumEarnedAmountByUserId(Long userId) {
        String sql = "SELECT COALESCE(SUM(te.hours_worked * j.hourly_wage), 0) FROM timesheet_entries te " +
                     "JOIN jobs j ON te.job_id = j.id WHERE te.user_id = ?";
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    public BigDecimal sumHoursWorkedByUserId(Long userId) {
        String sql = "SELECT COALESCE(SUM(hours_worked), 0) FROM timesheet_entries WHERE user_id = ?";
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    public List<kaasu_creator.model.TimesheetJobSummary> findJobSummariesByUserId(Long userId) {
        String sql = "SELECT j.job_name, j.hourly_wage, " +
                     "COALESCE(SUM(te.hours_worked), 0) AS total_hours, " +
                     "COALESCE(SUM(te.hours_worked * j.hourly_wage), 0) AS total_earned " +
                     "FROM timesheet_entries te " +
                     "JOIN jobs j ON te.job_id = j.id " +
                     "WHERE te.user_id = ? " +
                     "GROUP BY j.id, j.job_name, j.hourly_wage " +
                     "ORDER BY j.job_name";
        return jdbc.query(sql, (rs, rowNum) -> new kaasu_creator.model.TimesheetJobSummary(
            rs.getString("job_name"),
            rs.getBigDecimal("hourly_wage"),
            rs.getBigDecimal("total_hours"),
            rs.getBigDecimal("total_earned")
        ), userId);
    }
}
