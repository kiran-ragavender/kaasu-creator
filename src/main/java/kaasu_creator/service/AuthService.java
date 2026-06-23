package kaasu_creator.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import kaasu_creator.dao.UserDao;
import kaasu_creator.model.User;

/**
 * AuthService handles user registration and authentication.
 *
 * Key concept: PasswordEncoder
 * We NEVER store plain-text passwords. Instead, we use BCrypt to hash them.
 * BCrypt is a one-way hashing algorithm - you can verify a password against
 * a hash, but you can't reverse the hash to get the original password.
 */
@Service
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user.
     * Hashes the password before storing it in the database.
     *
     * @throws RuntimeException if the email is already taken
     */
    public void register(String fullName, String email, String password) {
        if (userDao.emailExists(email)) {
            throw new RuntimeException("Email is already registered");
        }
        // BCrypt hash - this creates a 60-character string like:
        // $2a$10$N9qo8uLOickgx2ZMRZoMye5tY...
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(null, fullName, email, hashedPassword, null);
        userDao.save(user);
    }

}