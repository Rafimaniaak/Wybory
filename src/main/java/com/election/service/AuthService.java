package com.election.service;

import com.election.dao.UserDAO;
import com.election.model.User;
import org.mindrot.jbcrypt.BCrypt;

// Serwis uwierzytelniania
public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    // Weryfikuje dane logowania
    public AuthResult authenticate(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            return new AuthResult(null, AuthStatus.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            return new AuthResult(null, AuthStatus.INVALID_PASSWORD);
        }
        return new AuthResult(user, AuthStatus.SUCCESS);
    }

    // Statusy uwierzytelniania
    public enum AuthStatus {
        SUCCESS, USER_NOT_FOUND, INVALID_PASSWORD
    }

    // Klasa wyników uwierzytelniania
        public record AuthResult(User user, AuthStatus status) {
    }
}