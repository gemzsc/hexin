#ifndef COURSE_H
#define COURSE_H

#include <iostream>
#include <string>

// 课程类：存储单门课程的名称、成绩和学期
class Course {
private:
    std::string name;      // 课程名称
    double score;          // 成绩（0-100）
    std::string semester;  // 学期（如 "2024-2025-1"）

public:
    // 默认构造函数
    Course();

    // 二参构造函数（兼容旧数据，学期默认空）
    Course(const std::string& n, double s);

    // 三参构造函数：指定名称、成绩和学期
    Course(const std::string& n, double s, const std::string& sem);

    std::string getName() const;
    double getScore() const;
    std::string getSemester() const;

    void setName(const std::string& n);
    void setScore(double s);
    void setSemester(const std::string& sem);
};

#endif
