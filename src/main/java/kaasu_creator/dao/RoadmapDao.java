package kaasu_creator.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import kaasu_creator.model.Roadmap;

@Repository
public class RoadmapDao {

    private final JdbcTemplate jdbc;

    public RoadmapDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper converts a database row into a Roadmap object
    private final RowMapper<Roadmap> roadmapRowMapper = (rs, rowNum) -> new Roadmap(
        rs.getLong("id"),
        rs.getLong("goal_id"),
        rs.getInt("week_number"),
        rs.getBigDecimal("target_amount"),
        rs.getString("status")
    );

    // SAVE multiple roadmap entries at once (used when generating a full roadmap)
    public void saveAll(List<Roadmap> roadmaps) {
        String sql = "INSERT INTO roadmaps (goal_id, week_number, target_amount, status) VALUES (?, ?, ?, ?)";
        for (Roadmap r : roadmaps) {
            jdbc.update(sql, r.getGoalId(), r.getWeekNumber(), r.getTargetAmount(), r.getStatus());
        }
    }

    // FIND all roadmap entries for a specific goal
    @SuppressWarnings("unchecked")
    public List<Roadmap> findByGoalId(Long goalId) {
        String sql = "SELECT * FROM roadmaps WHERE goal_id = ? ORDER BY week_number ASC";
        return jdbc.query(sql, roadmapRowMapper, goalId);
    }
}