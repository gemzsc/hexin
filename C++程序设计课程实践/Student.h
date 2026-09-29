#ifndef STUDENT_H
#define STUDENT_H

#include <iostream>
#include <string>
#include <vector>
#include "Course.h"

// 学生类：存储学生的基本信息和课程成绩
class Student {
private:
    std::string id;           // 学号（唯一标识）
    std::string name;         // 姓名
    std::string gender;       // 性别
    std::string college;      // 学院
    std::string major;        // 专业
    std::string className;    // 班级
    std::string grade;        // 年级
    std::string hometown;     // 籍贯
    std::string phone;        // 联系电话
    std::string dorm;         // 宿舍门牌号
    std::string password;     // 登录密码
    std::vector<Course> courses;  // 课程成绩列表

public:
    Student();

    // 从控制台输入基本信息
    void inputBasicInfo();

    // 从控制台输入课程成绩
    void inputCourses();

    // 在控制台显示学生完整信息
    void showInfo() const;

    // 显示该生所有课程的成绩
    void showCourses() const;

    // 添加一门课程成绩
    void addCourse(const std::string& cname, double score, const std::string& semester = "");

    // 批量设置基本信息（用于从文件加载，共11个字段）
    void setBasicInfo(
        const std::string& id,
        const std::string& name,
        const std::string& gender,
        const std::string& college,
        const std::string& major,
        const std::string& className,
        const std::string& grade,
        const std::string& hometown,
        const std::string& phone,
        const std::string& dorm,
        const std::string& password
    );

    // 根据字段序号修改单个字段（1=学号, 2=姓名 ... 11=密码）
    void modifyField(int fieldIndex, const std::string& newValue);

    // ---- getter ----
    std::string getId() const;
    std::string getName() const;
    std::string getGender() const;
    std::string getCollege() const;
    std::string getMajor() const;
    std::string getClassName() const;
    std::string getGrade() const;
    std::string getHometown() const;
    std::string getPhone() const;
    std::string getDorm() const;
    std::string getPassword() const;

    const std::vector<Course>& getCourses() const;

    size_t getCourseCount() const;
    Course& getCourse(int index);
    const Course& getCourse(int index) const;
    void setCourseScore(int index, double score);

    double getTotalScore() const;
    double getAverageScore() const;

    // ---- setter ----
    void setPassword(const std::string& p);
};

#endif
