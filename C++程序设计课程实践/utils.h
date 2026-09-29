#ifndef UTILS_H
#define UTILS_H

#include <iostream>
#include <string>
#include <conio.h>

// 终端密码加密输入：回显 * 号，支持退格删除
inline std::string readPassword(const std::string& prompt = "密码：") {
    std::cout << prompt;
    std::string password;
    char ch;
    while (true) {
        ch = _getch();
        if (ch == '\r' || ch == '\n') {  // Enter
            std::cout << std::endl;
            break;
        }
        if (ch == '\b' || ch == 127) {   // Backspace
            if (!password.empty()) {
                password.pop_back();
                std::cout << "\b \b";
            }
            continue;
        }
        password.push_back(ch);
        std::cout << '*';
    }
    return password;
}

#endif
