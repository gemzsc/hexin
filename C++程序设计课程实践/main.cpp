// 高校学生管理系统 —— 主程序入口
// 三级角色：教务秘书（增删改查/排序/统计/成绩分析）、
// 教师（查看学生/登记成绩）、学生（查看个人信息）
#include <iostream>
#include <limits>
#define NOMINMAX
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include "Manager.h"

int main() {
    // 设置控制台编码为UTF-8，确保中文输入输出和文件读写一致
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);

    Manager manager;

    // 启动时自动从文件加载已有数据
    manager.loadFromFile();

    // 主菜单循环
    while (true) {
        std::cout << "====================" << std::endl;
        std::cout << "高校学生管理系统" << std::endl;
        std::cout << "====================" << std::endl;
        std::cout << "1. 教务秘书登录" << std::endl;
        std::cout << "2. 学生登录" << std::endl;
        std::cout << "3. 教师登录" << std::endl;
        std::cout << "0. 退出" << std::endl;

        int choice;
        if (!(std::cin >> choice)) {
            std::cin.clear();
            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            std::cout << "输入无效，请重新选择" << std::endl;
            continue;
        }

        // 教务秘书入口
        if (choice == 1) {
            if (manager.adminLogin()) {
                while (true) {
                    int op;
                    std::cout << "----- 教务秘书菜单 -----" << std::endl;
                    std::cout << "1. 添加学生" << std::endl;
                    std::cout << "2. 显示全部" << std::endl;
                    std::cout << "3. 查询" << std::endl;
                    std::cout << "4. 修改" << std::endl;
                    std::cout << "5. 删除" << std::endl;
                    std::cout << "6. 排序" << std::endl;
                    std::cout << "7. 统计" << std::endl;
                    std::cout << "8. 模糊查询" << std::endl;
                    std::cout << "9. 成绩分析" << std::endl;
                    std::cout << "10. 挂科分布" << std::endl;
                    std::cout << "0. 返回" << std::endl;

                    if (!(std::cin >> op)) {
                        std::cin.clear();
                        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                        std::cout << "输入无效" << std::endl;
                        continue;
                    }

                    if (op == 0) break;

                    switch (op) {
                    case 1:  manager.addStudent();         break;
                    case 2:  manager.showAllStudents();    break;
                    case 3:  manager.findStudent();        break;
                    case 4:  manager.modifyStudent();      break;
                    case 5:  manager.deleteStudent();      break;
                    case 6:  manager.sortStudents();       break;
                    case 7:  manager.statistics();         break;
                    case 8:  manager.fuzzyFind();          break;
                    case 9:  manager.scoreAnalysis();      break;
                    case 10: manager.failingDistribution(); break;
                    default: std::cout << "输入错误" << std::endl;
                    }
                }
            }
        }
        // 学生入口
        else if (choice == 2) {
            manager.studentLogin();
        }
        // 教师入口
        else if (choice == 3) {
            if (manager.teacherLogin()) {
                manager.teacherMenu();
            }
        }
        // 退出：保存数据到文件
        else if (choice == 0) {
            manager.saveToFile();
            return 0;
        }
        else {
            std::cout << "输入错误" << std::endl;
        }
    }
}
