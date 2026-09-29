#include "Student.h"
#include "utils.h"
#include <limits>

Student::Student() {}

// 从控制台逐项输入学生的基本信息（含课程要求补充的年级和宿舍字段）
void Student::inputBasicInfo() {
    std::cout << "学号：";
    std::cin >> id;
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    std::cout << "姓名：";
    std::getline(std::cin, name);
    std::cout << "性别：";
    std::cin >> gender;
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    std::cout << "年级：";
    std::cin >> grade;
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    std::cout << "学院：";
    std::getline(std::cin, college);
    std::cout << "专业：";
    std::getline(std::cin, major);
    std::cout << "班级：";
    std::getline(std::cin, className);
    std::cout << "宿舍门牌号：";
    std::cin >> dorm;
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    std::cout << "籍贯：";
    std::getline(std::cin, hometown);
    std::cout << "电话：";
    std::cin >> phone;
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    std::cout << "设置密码：";
    password = readPassword("");
}

// 输入课程数量及每门课程的名称、成绩和学期，成绩需在0~100之间
void Student::inputCourses() {
    int n;
    std::cout << "课程数量：";
    while (!(std::cin >> n) || n < 0) {
        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        std::cout << "输入无效，请输入非负整数：";
    }
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    for (int i = 0; i < n; i++) {
        std::string cname, sem;
        double score;
        std::cout << "课程名称：";
        std::getline(std::cin, cname);
        std::cout << "学期（如2024-2025-1）：";
        std::cin >> sem;
        std::cout << "成绩：";
        while (!(std::cin >> score) || score < 0 || score > 100) {
            std::cin.clear();
            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            std::cout << "成绩非法（0-100），重新输入：";
        }
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        courses.push_back(Course(cname, score, sem));
    }
}

// 格式化输出学生的课程成绩（含学期信息）
void Student::showCourses() const {
    std::cout << "-----课程成绩-----" << std::endl;
    for (const auto& c : courses) {
        std::cout << "[" << c.getSemester() << "] "
                  << c.getName() << " : " << c.getScore() << std::endl;
    }
}

// 格式化输出学生的完整信息（基本信息 + 课程成绩 + 总分 + 均分）
void Student::showInfo() const {
    std::cout << "====================" << std::endl;
    std::cout << "学号：" << id << std::endl;
    std::cout << "姓名：" << name << std::endl;
    std::cout << "性别：" << gender << std::endl;
    std::cout << "年级：" << grade << std::endl;
    std::cout << "学院：" << college << std::endl;
    std::cout << "专业：" << major << std::endl;
    std::cout << "班级：" << className << std::endl;
    std::cout << "宿舍门牌号：" << dorm << std::endl;
    std::cout << "籍贯：" << hometown << std::endl;
    std::cout << "电话：" << phone << std::endl;
    showCourses();
    std::cout << "总分：" << getTotalScore() << std::endl;
    std::cout << "平均分：" << getAverageScore() << std::endl;
}

double Student::getTotalScore() const {
    double sum = 0;
    for (const auto& c : courses) {
        sum += c.getScore();
    }
    return sum;
}

double Student::getAverageScore() const {
    if (courses.empty())
        return 0;
    return getTotalScore() / courses.size();
}

// ---- getter ----
std::string Student::getId() const { return id; }
std::string Student::getName() const { return name; }
std::string Student::getGender() const { return gender; }
std::string Student::getCollege() const { return college; }
std::string Student::getMajor() const { return major; }
std::string Student::getClassName() const { return className; }
std::string Student::getGrade() const { return grade; }
std::string Student::getHometown() const { return hometown; }
std::string Student::getPhone() const { return phone; }
std::string Student::getDorm() const { return dorm; }
std::string Student::getPassword() const { return password; }

const std::vector<Course>& Student::getCourses() const { return courses; }

size_t Student::getCourseCount() const { return courses.size(); }

Course& Student::getCourse(int index) { return courses.at(index); }
const Course& Student::getCourse(int index) const { return courses.at(index); }

void Student::setCourseScore(int index, double score) {
    courses.at(index).setScore(score);
}

// 批量设置基本信息，用于从文件反序列化（11个字段）
void Student::setBasicInfo(
    const std::string& i, const std::string& n, const std::string& g,
    const std::string& c, const std::string& m, const std::string& cn,
    const std::string& gr, const std::string& h, const std::string& p,
    const std::string& d, const std::string& pw
) {
    id = i; name = n; gender = g;
    college = c; major = m; className = cn;
    grade = gr; hometown = h; phone = p;
    dorm = d; password = pw;
}

void Student::addCourse(const std::string& cname, double score, const std::string& semester) {
    courses.push_back(Course(cname, score, semester));
}

void Student::setPassword(const std::string& p) {
    password = p;
}

// 根据字段序号修改单个字段的值
// 序号映射: 1=学号 2=姓名 3=性别 4=年级 5=学院 6=专业
//           7=班级 8=宿舍 9=籍贯 10=电话 11=密码
void Student::modifyField(int fieldIndex, const std::string& newValue) {
    switch (fieldIndex) {
    case 1:  id = newValue;       break;
    case 2:  name = newValue;     break;
    case 3:  gender = newValue;   break;
    case 4:  grade = newValue;    break;
    case 5:  college = newValue;  break;
    case 6:  major = newValue;    break;
    case 7:  className = newValue; break;
    case 8:  dorm = newValue;     break;
    case 9:  hometown = newValue; break;
    case 10: phone = newValue;    break;
    case 11: password = newValue; break;
    }
}
