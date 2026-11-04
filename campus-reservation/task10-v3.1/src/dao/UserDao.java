package dao;

import entity.User;
import java.io.IOException;
import java.util.Optional;

public interface UserDao {
    Optional<User> findByUsername(String username);

    void insert(User user);

    boolean existsByRole(String role);
}
