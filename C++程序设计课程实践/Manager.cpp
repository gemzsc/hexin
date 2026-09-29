#include "Manager.h"
#include "utils.h"
#include <algorithm>  // std::sort
#include <cstdio>     // std::rename
#include <fstream>    // 文件读写
#include <iostream>
#include <limits>
#include <map>        // std::map

// ============================================================
// 辅助函数
// ============================================================

// 供教师查看学生信息：与 showInfo 相同，但不显示密码
void Manager::showStudentInfoForTeacher(const Student& s) const {
    std::cout << "====================" << std::endl;
    std::cout << "学号：" << s.getId() << std::endl;
    std::cout << "姓名：" << s.getName() << std::endl;
    std::cout << "性别：" << s.getGender() << std::endl;
    std::cout << "年级：" << s.getGrade() << std::endl;
    std::cout << "学院：" << s.getCollege() << std::endl;
    std::cout << "专业：" << s.getMajor() << std::endl;
    std::cout << "班级：" << s.getClassName() << std::endl;
    std::cout << "宿舍门牌号：" << s.getDorm() << std::endl;
    std::cout << "籍贯：" << s.getHometown() << std::endl;
    std::cout << "电话：" << s.getPhone() << std::endl;
    s.showCourses();
    std::cout << "总分：" << s.getTotalScore() << std::endl;
    std::cout << "平均分：" << s.getAverageScore() << std::endl;
}

// ============================================================
// 文件持久化
// ============================================================

// 保存到 data.txt（先写临时文件，成功后再替换，防止数据丢失）
// 格式：
// id name gender college major className grade hometown phone dorm password
// N
// cname score semester ...（共N行）
// 注意：各字段中不得含空格，否则文件格式破坏
// TODO: 生产环境中密码应加盐哈希后存储，不应明文写入文件
void Manager::saveToFile() const {
    const char* tmpFile = "data.txt.tmp";
    std::ofstream fout(tmpFile);
    if (!fout) {
        std::cout << "文件打开失败" << std::endl;
        return;
    }

    for (const auto& s : students) {
        fout << s.getId() << " "
             << s.getName() << " "
             << s.getGender() << " "
             << s.getCollege() << " "
             << s.getMajor() << " "
             << s.getClassName() << " "
             << s.getGrade() << " "
             << s.getHometown() << " "
             << s.getPhone() << " "
             << s.getDorm() << " "
             << s.getPassword() << std::endl;

        if (fout.fail()) {
            std::cout << "写入失败" << std::endl;
            fout.close();
            return;
        }

        const std::vector<Course>& courses = s.getCourses();
        fout << courses.size() << std::endl;
        for (const auto& c : courses) {
            fout << c.getName() << " "
                 << c.getScore() << " "
                 << c.getSemester() << std::endl;
        }

        if (fout.fail()) {
            std::cout << "写入失败" << std::endl;
            fout.close();
            return;
        }
    }

    fout.close();
    if (fout.fail()) {
        std::cout << "文件关闭失败，数据可能不完整" << std::endl;
        return;
    }

    // 原子替换：删除旧文件，重命名临时文件
    if (std::rename(tmpFile, "data.txt") != 0) {
        std::cout << "替换文件失败" << std::endl;
        return;
    }
    std::cout << "保存成功" << std::endl;
}

// 从 data.txt 加载数据：while(fin >> id) 模式，首字段读取失败即停止
void Manager::loadFromFile() {
    std::ifstream fin("data.txt");
    if (!fin) {
        std::cout << "文件不存在或无数据" << std::endl;
        return;
    }

    students.clear();

    std::string id, name, gender, college, major, className,
                grade, hometown, phone, dorm, password;

    int lineNum = 0;
    while (fin >> id) {
        lineNum++;
        if (!(fin >> name >> gender >> college >> major
              >> className >> grade >> hometown >> phone >> dorm >> password)) {
            std::cout << "警告: 第 " << lineNum << " 条学生记录字段不完整，已跳过" << std::endl;
            fin.clear();
            fin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            continue;
        }

        Student s;
        s.setBasicInfo(id, name, gender, college, major, className,
                       grade, hometown, phone, dorm, password);

        int n;
        if (!(fin >> n) || n < 0) {
            std::cout << "警告: 学生 " << id << " 的课程数量无效，已跳过课程" << std::endl;
            fin.clear();
            fin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            students.push_back(s);
            continue;
        }

        bool coursesOk = true;
        for (int i = 0; i < n; i++) {
            std::string cname, sem;
            double score;
            if (!(fin >> cname >> score >> sem)) {
                std::cout << "警告: 学生 " << id << " 的第 " << (i+1) << " 门课程数据不完整" << std::endl;
                fin.clear();
                fin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                coursesOk = false;
                break;
            }
            s.addCourse(cname, score, sem);
        }
        if (!coursesOk) {
            // 跳过该生剩余课程行
            fin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        }

        students.push_back(s);
    }

    fin.close();
    std::cout << "读取成功，共加载 " << students.size() << " 名学生" << std::endl;
}

// ============================================================
// 增删改查
// ============================================================

void Manager::addStudent() {
    Student s;
    s.inputBasicInfo();

    // 检查学号是否重复
    for (const auto& existing : students) {
        if (existing.getId() == s.getId()) {
            std::cout << "学号 " << s.getId() << " 已存在，添加失败" << std::endl;
            return;
        }
    }

    s.inputCourses();
    students.push_back(s);
    std::cout << "添加成功！" << std::endl;
}

void Manager::showAllStudents() {
    if (students.empty()) {
        std::cout << "暂无学生信息" << std::endl;
        return;
    }
    for (const auto& s : students) {
        s.showInfo();
        std::cout << std::endl;
    }
}

void Manager::findStudent() {
    std::string id;
    std::cout << "输入学号：";
    std::cin >> id;
    for (const auto& s : students) {
        if (s.getId() == id) {
            s.showInfo();
            return;
        }
    }
    std::cout << "未找到" << std::endl;
}

// 修改学生信息：先根据学号查找，再通过字段序号修改指定字段
void Manager::modifyStudent() {
    std::string id;
    std::cout << "输入要修改的学生学号：";
    std::cin >> id;

    for (auto& s : students) {
        if (s.getId() == id) {
            s.showInfo();
            std::cout << std::endl;

            while (true) {
                std::cout << "----- 选择要修改的字段 -----" << std::endl;
                std::cout << "1. 学号     2. 姓名" << std::endl;
                std::cout << "3. 性别     4. 年级" << std::endl;
                std::cout << "5. 学院     6. 专业" << std::endl;
                std::cout << "7. 班级     8. 宿舍门牌号" << std::endl;
                std::cout << "9. 籍贯     10. 电话" << std::endl;
                std::cout << "11. 密码    0. 返回" << std::endl;

                int field;
                if (!(std::cin >> field)) {
                    std::cin.clear();
                    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                    std::cout << "输入无效" << std::endl;
                    continue;
                }
                if (field == 0) break;
                if (field < 1 || field > 11) {
                    std::cout << "序号无效" << std::endl;
                    continue;
                }

                std::string newValue;
                if (field == 11) {
                    newValue = readPassword("输入新密码：");
                } else {
                    std::cout << "输入新值：";
                    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                    std::getline(std::cin, newValue);
                }
                s.modifyField(field, newValue);
                std::cout << "修改成功！" << std::endl;
                s.showInfo();
                std::cout << std::endl;
            }
            return;
        }
    }
    std::cout << "未找到该学生" << std::endl;
}

void Manager::deleteStudent() {
    std::string id;
    std::cout << "输入要删除的学号：";
    std::cin >> id;
    for (auto it = students.begin(); it != students.end(); ++it) {
        if (it->getId() == id) {
            students.erase(it);
            std::cout << "删除成功" << std::endl;
            return;
        }
    }
    std::cout << "未找到" << std::endl;
}

// ============================================================
// 多字段排序（含子菜单）
// ============================================================

void Manager::sortStudents() {
    if (students.empty()) {
        std::cout << "暂无学生数据" << std::endl;
        return;
    }

    std::cout << "----- 选择排序依据 -----" << std::endl;
    std::cout << "1. 按学号" << std::endl;
    std::cout << "2. 按总分" << std::endl;
    std::cout << "3. 按平均分" << std::endl;
    std::cout << "0. 返回" << std::endl;

    int sortBy;
    if (!(std::cin >> sortBy)) {
        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        std::cout << "输入无效" << std::endl;
        return;
    }
    if (sortBy == 0) return;
    if (sortBy < 1 || sortBy > 3) {
        std::cout << "无效选择" << std::endl;
        return;
    }

    std::cout << "选择方向：1. 升序  2. 降序" << std::endl;
    int dir;
    if (!(std::cin >> dir)) {
        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        std::cout << "输入无效" << std::endl;
        return;
    }
    bool ascending = (dir == 1);

    // 根据选择构造 Lambda 比较函数
    switch (sortBy) {
    case 1: // 按学号
        std::sort(students.begin(), students.end(),
            [ascending](const Student& a, const Student& b) {
                return ascending ? (a.getId() < b.getId())
                                 : (a.getId() > b.getId());
            });
        break;
    case 2: // 按总分
        std::sort(students.begin(), students.end(),
            [ascending](const Student& a, const Student& b) {
                return ascending ? (a.getTotalScore() < b.getTotalScore())
                                 : (a.getTotalScore() > b.getTotalScore());
            });
        break;
    case 3: // 按平均分
        std::sort(students.begin(), students.end(),
            [ascending](const Student& a, const Student& b) {
                return ascending ? (a.getAverageScore() < b.getAverageScore())
                                 : (a.getAverageScore() > b.getAverageScore());
            });
        break;
    }

    std::cout << "排序完成" << std::endl;
}

// ============================================================
// 多维度统计（含子菜单）
// ============================================================

void Manager::statistics() {
    if (students.empty()) {
        std::cout << "暂无学生数据" << std::endl;
        return;
    }

    std::cout << "----- 选择统计维度 -----" << std::endl;
    std::cout << "1. 按学院" << std::endl;
    std::cout << "2. 按专业" << std::endl;
    std::cout << "3. 按班级" << std::endl;
    std::cout << "4. 按籍贯" << std::endl;
    std::cout << "0. 返回" << std::endl;

    int dim;
    if (!(std::cin >> dim)) {
        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        std::cout << "输入无效" << std::endl;
        return;
    }
    if (dim == 0) return;
    if (dim < 1 || dim > 4) {
        std::cout << "无效选择" << std::endl;
        return;
    }

    // 统一使用 map 统计模式
    std::map<std::string, int> mp;
    for (const auto& s : students) {
        switch (dim) {
        case 1: mp[s.getCollege()]++;   break;
        case 2: mp[s.getMajor()]++;     break;
        case 3: mp[s.getClassName()]++; break;
        case 4: mp[s.getHometown()]++;  break;
        }
    }

    const char* titles[] = {"", "学院", "专业", "班级", "籍贯"};
    std::cout << "----- 按" << titles[dim] << "统计人数 -----" << std::endl;
    for (const auto& p : mp) {
        std::cout << p.first << " : " << p.second << "人" << std::endl;
    }
}

// ============================================================
// 模糊查询
// ============================================================

void Manager::fuzzyFind() {
    std::string keyword;
    std::cout << "输入姓名关键字：";
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    std::getline(std::cin, keyword);

    bool found = false;
    for (const auto& s : students) {
        if (s.getName().find(keyword) != std::string::npos) {
            s.showInfo();
            found = true;
        }
    }
    if (!found) {
        std::cout << "未找到" << std::endl;
    }
}

// ============================================================
// 成绩分析
// ============================================================

void Manager::scoreAnalysis() {
    if (students.empty()) {
        std::cout << "暂无学生数据" << std::endl;
        return;
    }

    int excellent = 0, fail = 0;
    int total = static_cast<int>(students.size());

    for (const auto& s : students) {
        double avg = s.getAverageScore();
        if (avg >= 90) excellent++;
        if (avg < 60)  fail++;
    }

    std::cout << "优秀率（均分>=90）：" << (double)excellent / total * 100 << "%" << std::endl;
    std::cout << "挂科率（均分<60）："  << (double)fail / total * 100 << "%" << std::endl;
}

// ============================================================
// 挂科分布查询（新增）
// ============================================================

// 按学院分组，统计各学院下不及格课程的分布情况
void Manager::failingDistribution() {
    if (students.empty()) {
        std::cout << "暂无学生数据" << std::endl;
        return;
    }

    // 数据结构: map<学院, map<课程名, 挂科人数>>
    std::map<std::string, std::map<std::string, int>> dist;
    bool hasFailing = false;

    for (const auto& s : students) {
        for (const auto& c : s.getCourses()) {
            if (c.getScore() < 60) {
                dist[s.getCollege()][c.getName()]++;
                hasFailing = true;
            }
        }
    }

    if (!hasFailing) {
        std::cout << "无挂科记录" << std::endl;
        return;
    }

    std::cout << "----- 各学院挂科分布 -----" << std::endl;
    for (const auto& collegeEntry : dist) {
        std::cout << "【" << collegeEntry.first << "】" << std::endl;
        for (const auto& courseEntry : collegeEntry.second) {
            std::cout << "  " << courseEntry.first
                      << " : " << courseEntry.second << "人挂科" << std::endl;
        }
    }
}

// ============================================================
// 身份认证
// ============================================================

// TODO: 生产环境应将凭证存储于配置文件并对密码做哈希处理，不应硬编码于源码中
bool Manager::adminLogin() const {
    std::string account;
    std::cout << "教务秘书账号：";
    std::cin >> account;
    std::string password = readPassword("教务秘书密码：");

    if (account == "admin" && password == "123456") {
        std::cout << "登录成功" << std::endl;
        return true;
    }
    std::cout << "登录失败" << std::endl;
    return false;
}

bool Manager::studentLogin() const {
    std::string id;
    std::cout << "学号：";
    std::cin >> id;
    std::string password = readPassword("密码：");

    for (const auto& s : students) {
        if (s.getId() == id && s.getPassword() == password) {
            std::cout << "登录成功" << std::endl;
            s.showInfo();
            return true;
        }
    }
    std::cout << "账号或密码错误" << std::endl;
    return false;
}

// 教师登录：固定账号 teacher / 123456
bool Manager::teacherLogin() const {
    std::string account;
    std::cout << "教师账号：";
    std::cin >> account;
    std::string password = readPassword("教师密码：");

    if (account == "teacher" && password == "123456") {
        std::cout << "登录成功" << std::endl;
        return true;
    }
    std::cout << "登录失败" << std::endl;
    return false;
}

// 教师操作菜单
void Manager::teacherMenu() {
    while (true) {
        std::cout << "----- 教师菜单 -----" << std::endl;
        std::cout << "1. 查看所有学生" << std::endl;
        std::cout << "2. 登记/修改学生成绩" << std::endl;
        std::cout << "0. 返回" << std::endl;

        int op;
        if (!(std::cin >> op)) {
            std::cin.clear();
            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            std::cout << "输入无效" << std::endl;
            continue;
        }
        if (op == 0) break;

        switch (op) {
        case 1:
            // 教师可查看所有学生基本信息（不含密码）
            if (students.empty()) {
                std::cout << "暂无学生信息" << std::endl;
            } else {
                for (const auto& s : students) {
                    showStudentInfoForTeacher(s);
                    std::cout << std::endl;
                }
            }
            break;

        case 2: {
            // 登记/修改成绩：根据学号找到学生，操作其课程成绩
            std::string id;
            std::cout << "输入学生学号：";
            std::cin >> id;

            bool found = false;
            for (auto& s : students) {
                if (s.getId() == id) {
                    found = true;
                    std::cout << "当前课程成绩：" << std::endl;
                    s.showCourses();

                    std::cout << std::endl << "1. 添加新课程" << std::endl;
                    std::cout << "2. 修改已有课程成绩" << std::endl;
                    std::cout << "0. 返回" << std::endl;

                    int act;
                    if (!(std::cin >> act)) {
                        std::cin.clear();
                        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                        std::cout << "输入无效" << std::endl;
                        break;
                    }
                    if (act == 0) break;

                    if (act == 1) {
                        std::string cname, sem;
                        double score;
                        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                        std::cout << "课程名称：";
                        std::getline(std::cin, cname);
                        std::cout << "学期：";
                        std::cin >> sem;
                        std::cout << "成绩：";
                        while (!(std::cin >> score) || score < 0 || score > 100) {
                            std::cin.clear();
                            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                            std::cout << "成绩非法（0-100），重新输入：";
                        }
                        s.addCourse(cname, score, sem);
                        std::cout << "添加成功！" << std::endl;
                    }
                    else if (act == 2) {
                        if (s.getCourseCount() == 0) {
                            std::cout << "该生暂无课程成绩" << std::endl;
                            break;
                        }
                        std::cout << "选择要修改的课程序号（1-" << s.getCourseCount() << "）：";
                        int idx;
                        if (!(std::cin >> idx) || idx < 1 || idx > static_cast<int>(s.getCourseCount())) {
                            std::cin.clear();
                            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                            std::cout << "序号无效" << std::endl;
                        } else {
                            double newScore;
                            std::cout << "输入新成绩：";
                            while (!(std::cin >> newScore) || newScore < 0 || newScore > 100) {
                                std::cin.clear();
                                std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
                                std::cout << "成绩非法，重新输入：";
                            }
                            s.setCourseScore(idx - 1, newScore);
                            std::cout << "修改成功！" << std::endl;
                        }
                    }
                    break;
                }
            }
            if (!found) {
                std::cout << "未找到该学生" << std::endl;
            }
            break;
        }

        default:
            std::cout << "无效选择" << std::endl;
        }
    }
}
