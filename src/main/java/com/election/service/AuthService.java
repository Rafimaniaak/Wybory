package com.election.service;

import com.election.dao.UserDAO;
import com.election.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private UserDAO userDAO = new UserDAO();

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

    public enum AuthStatus {
        SUCCESS, USER_NOT_FOUND, INVALID_PASSWORD
    }

    public static class AuthResult {
        private final User user;
        private final AuthStatus status;

        public AuthResult(User user, AuthStatus status) {
            this.user = user;
            this.status = status;
        }

        public User getUser() {
            return user;
        }

        public AuthStatus getStatus() {
            return status;
        }
    }
}