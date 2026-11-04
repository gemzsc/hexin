package service;

import dao.UserDao;
import entity.Result;
import entity.User;
import exception.*;

import java.io.IOException;
import java.time.LocalDateTime;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Result<Void> register(String username, String password) throws IOException {
        validateUsername(username);
        validatePassword(password);

        if (userDao.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("用户名已存在");
        }

        User newUser = new User(username, password, "student", LocalDateTime.now());

        userDao.insert(newUser);

        return Result.success("注册成功");
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new EmptyParameterException("用户名不能为空");
        }
        if (username.contains("|")) {
            throw new BusinessException("用户名不能包含分隔符 |");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new EmptyParameterException("密码不能为空");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码长度至少为6位");
        }
        if (password.contains("|")) {
            throw new BusinessException("密码不能包含分隔符 |");
        }
    }

    public Result<User> login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new EmptyParameterException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new EmptyParameterException("密码不能为空");
        }

        User user = userDao.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("用户不存在，请先注册"));

        if (!user.getPassword().equals(password)) {
            throw new PasswordIncorrectException("密码错误");
        }
        return Result.success("登录成功", user);
    }

    public void ensureAdminExists() throws IOException {
        if (userDao.existsByRole("admin")) {
            return;
        }

        User admin = new User(
                "admin", "123456", "admin", LocalDateTime.now()
        );
        userDao.insert(admin);
    }
}
