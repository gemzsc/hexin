#ifndef MANAGER_H
#define MANAGER_H

#include <vector>
#include "Student.h"

// 管理类：负责学生数据的增删改查、排序、统计、文件读写和登录验证
class Manager {
private:
    std::vector<Student> students;

    // 辅助函数：显示学生信息但不含密码（供教师查看用）
    void showStudentInfoForTeacher(const Student& s) const;

public:
    // ---- 数据管理 ----
    void addStudent();
    void showAllStudents();
    void findStudent();
    void modifyStudent();      // 新增：根据学号修改学生信息
    void deleteStudent();

    // ---- 排序（含子菜单选择字段和方向） ----
    void sortStudents();

    // ---- 统计（含子菜单选择维度） ----
    void statistics();

    // ---- 模糊查询 ----
    void fuzzyFind();

    // ---- 成绩分析 ----
    void scoreAnalysis();

    // ---- 挂科分布查询（新增） ----
    void failingDistribution();

    // ---- 文件持久化 ----
    void saveToFile() const;
    void loadFromFile();

    // ---- 身份认证 ----
    bool adminLogin() const;
    bool studentLogin() const;
    bool teacherLogin() const;  // 新增：教师登录
    void teacherMenu();         // 新增：教师操作菜单
};

#endif
