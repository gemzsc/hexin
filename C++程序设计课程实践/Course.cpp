#include "Course.h"

Course::Course() : name(""), score(0), semester("") {}

Course::Course(const std::string& n, double s) : name(n), score(s), semester("") {}

Course::Course(const std::string& n, double s, const std::string& sem)
    : name(n), score(s), semester(sem) {}

std::string Course::getName() const { return name; }
double Course::getScore() const { return score; }
std::string Course::getSemester() const { return semester; }

void Course::setName(const std::string& n) { name = n; }
void Course::setScore(double s) { score = s; }
void Course::setSemester(const std::string& sem) { semester = sem; }
