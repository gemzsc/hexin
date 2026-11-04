package dao.file;

import dao.UserDao;
import entity.User;
import exception.DataAccessException;
import utils.FileUtil;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FileUserDao implements UserDao {
    private final List<User> users;

    public FileUserDao() {
        try {
            this.users = FileUtil.loadUsers();
        } catch (IOException e) {
            throw new DataAccessException("用户数据加载失败", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(user -> user != null && username.equals(user.getUsername()))
                .findFirst();
    }

    @Override
    public void insert(User user) {
        try {
            FileUtil.saveUser(user);
            users.add(user);
        } catch (IOException e) {
            throw new DataAccessException("用户数据保存失败", e);
        }
    }

    @Override
    public boolean existsByRole(String role) {
        return users.stream()
                .anyMatch(user -> role.equals(user.getRole()));
    }
}
