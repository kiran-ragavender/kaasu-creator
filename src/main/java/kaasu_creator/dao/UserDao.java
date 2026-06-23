package kaasu_creator.dao;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import kaasu_creator.model.User;

@Repository
public class UserDao {

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper converts a database row into a User object
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
        rs.getLong("id"),
        rs.getString("full_name"),
        rs.getString("email"),
        rs.getString("password"),
        rs.getTimestamp("created_at")
    );

    // INSERT a new user into the database
    public void save(User user) {
        String sql = "INSERT INTO users (full_name, email, password) VALUES (?, ?, ?)";
        jdbc.update(sql, user.getFullName(), user.getEmail(), user.getPassword());
    }

    // FIND a user by their email address
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return jdbc.query(sql, userRowMapper, email)
                   .stream()
                   .findFirst();
    }

    // CHECK if an email already exists in the database
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    // DELETE a user by email
    public void deleteByEmail(String email) {
        String sql = "DELETE FROM users WHERE email = ?";
        jdbc.update(sql, email);
    }

    // DELETE a user by ID
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbc.update(sql, id);
    }
}