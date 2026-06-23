package kaasu_creator.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import kaasu_creator.model.Job;

@Repository
public class JobRepository {

    private final JdbcTemplate jdbc;

    public JobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Job> rowMapper = (rs, rowNum) -> new Job(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("job_name"),
            rs.getBigDecimal("hourly_wage"),
            rs.getString("notes"),
            rs.getTimestamp("created_at")
    );

    public void save(Job job) {
        String sql = "INSERT INTO jobs (user_id, job_name, hourly_wage, notes) VALUES (?, ?, ?, ?)";
        jdbc.update(sql,
                job.getUserId(),
                job.getJobName(),
                job.getHourlyWage(),
                job.getNotes()
        );
    }

    public void update(Job job) {
        String sql = "UPDATE jobs SET job_name = ?, hourly_wage = ?, notes = ? WHERE id = ? AND user_id = ?";
        jdbc.update(sql,
                job.getJobName(),
                job.getHourlyWage(),
                job.getNotes(),
                job.getId(),
                job.getUserId()
        );
    }

    public void deleteById(Long id, Long userId) {
        String sql = "DELETE FROM jobs WHERE id = ? AND user_id = ?";
        jdbc.update(sql, id, userId);
    }

    @SuppressWarnings("null")
    public List<Job> findByUserId(Long userId) {
        String sql = "SELECT * FROM jobs WHERE user_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, rowMapper, userId);
    }

    /**
     * Returns jobs with total_hours and total_earned aggregated from timesheet_entries.
     * Uses LEFT JOIN so jobs with zero entries are still returned.
     */
    @SuppressWarnings("null")
    public List<Job> findWithSummaryByUserId(Long userId) {
        String sql =
            "SELECT j.id, j.user_id, j.job_name, j.hourly_wage, j.notes, j.created_at, " +
            "  COALESCE(SUM(te.hours_worked), 0)                     AS total_hours, " +
            "  COALESCE(SUM(te.hours_worked * j.hourly_wage), 0)     AS total_earned " +
            "FROM jobs j " +
            "LEFT JOIN timesheet_entries te ON te.job_id = j.id " +
            "WHERE j.user_id = ? " +
            "GROUP BY j.id, j.user_id, j.job_name, j.hourly_wage, j.notes, j.created_at " +
            "ORDER BY j.created_at DESC";
        return jdbc.query(sql, (rs, rowNum) -> {
            Job job = new Job(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("job_name"),
                rs.getBigDecimal("hourly_wage"),
                rs.getString("notes"),
                rs.getTimestamp("created_at")
            );
            job.setTotalHours(rs.getBigDecimal("total_hours"));
            job.setTotalEarned(rs.getBigDecimal("total_earned"));
            return job;
        }, userId);
    }

    @SuppressWarnings("null")
    public Optional<Job> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT * FROM jobs WHERE id = ? AND user_id = ?";
        return jdbc.query(sql, rowMapper, id, userId).stream().findFirst();
    }
}
