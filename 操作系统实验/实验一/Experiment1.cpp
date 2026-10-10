#include <iostream>
#include <vector>
#include <string>
#include <cstdlib>
#include <ctime>
#include <sstream>
#include <iomanip>
#include <map>
#include <windows.h>

using namespace std;

//1.状态定义
enum ProcessState{
    New=0,      //新建
    Ready,      //就绪
    Running,    //运行
    Blocked,    //阻塞
    Suspend_Ready,    //挂起就绪
    Suspend_Blocked,    //挂起阻塞
    Terminated    //终止
};

static const char* State_Names[]={
    "新建 New",
    "就绪 Ready",
    "运行 Running",
    "阻塞 Blocked",
    "挂起就绪 Suspend_Ready",
    "挂起阻塞 Suspend_Blocked",
    "终止 Terminated"
};

static const int State_Count=7;

//2.进程控制块 PCB
struct PCB{
    int pid;            //进程ID
    ProcessState state;    //进程状态
    int priority;        //进程优先级
    int cpuTime;        //已占用CPU时间
    int needTime;        //所需总运行时间
    string name;        //进程名
    bool suspended;    //是否挂起

    PCB(int id,string nm,int pri,int need)
        :pid(id),state(New),priority(pri),cpuTime(0),
        needTime(need),name(nm),suspended(false){}
};

//3.全局模拟环境
vector<PCB> processes;          // 所有进程
int nextPid = 1;                // 下一个进程号
int memoryLimit = 4;            // 内存中可容纳的“活跃”进程上限（用于模拟内存紧张）
int memoryUsed = 0;             // 当前内存中活跃进程数

// 帮助函数：获取某状态对应的字符串
string stateStr(ProcessState s) {
    return string(State_Names[s]);
}

// 找到指定 pid 的进程下标，找不到返回 -1
int findProcess(int pid) {
    for (size_t i = 0; i < processes.size(); i++) {
        if (processes[i].pid == pid) return (int)i;
    }
    return -1;
}

// 判断一个状态是否属于“内存中的活跃状态”（需要占用内存）
bool isActiveState(ProcessState s) {
    return s == Ready || s == Running || s == Blocked;
}

// 重新计算内存中活跃进程数
void recomputeMemory() {
    memoryUsed = 0;
    for (size_t i = 0; i < processes.size(); i++) {
        if (isActiveState(processes[i].state)) memoryUsed++;
    }
}

//4.功能函数

//查找某种状态的进程
int findProcessByState(ProcessState state) {
    for(size_t i=0;i<processes.size();++i){
        if(processes[i].state==state)
            return static_cast<int>(i);
    }
    return -1;
}

//内存有空位时，将等待的新建进程接纳到就绪态
void admitNewProcess() {
    while(memoryUsed<memoryLimit){
        int index=findProcessByState(New);
        if(index==-1) break;

        processes[index].state=Ready;
        ++memoryUsed;
        cout<<"进程 "<<processes[index].pid
            <<" 已从新建态接纳到就绪态。\n";
    }
}

//显示进程列表
void showProcesses() {
    cout << "\nPID\t名称\t状态\t\t优先级\tCPU时间/所需时间\n";
    for (const PCB& p : processes) {
        cout << p.pid << '\t' << p.name << '\t'
             << stateStr(p.state) << '\t'
             << p.priority << '\t'
             << p.cpuTime << '/' << p.needTime << '\n';
    }
    cout << "内存使用：" << memoryUsed << '/' << memoryLimit << "\n";
}

// 创建进程：先进入新建态，再尝试接纳
void createProcess() {
    string name;
    int priority, needTime;

    cout << "请输入进程名：";
    cin >> name;
    cout << "请输入优先级：";
    cin >> priority;
    cout << "请输入所需运行时间：";
    cin >> needTime;

    if (needTime <= 0) {
        cout << "运行时间必须大于 0。\n";
        return;
    }

    processes.emplace_back(nextPid++, name, priority, needTime);
    cout << "进程创建成功，PID = " << processes.back().pid << "。\n";
    admitNewProcess();
}
bool changeState(int pid, ProcessState target) {
    int index = findProcess(pid);
    if (index == -1) {
        cout << "没有找到该 PID。\n";
        return false;
    }

    PCB& p = processes[index];
    ProcessState old = p.state;

    bool allowed =
        (old == Ready && target == Running) ||
        (old == Running && target == Ready) ||
        (old == Running && target == Blocked) ||
        (old == Blocked && target == Ready) ||
        (old == Running && target == Terminated) ||
        (old == Ready && target == Suspend_Ready) ||
        (old == Blocked && target == Suspend_Blocked) ||
        (old == Suspend_Ready && target == Ready) ||
        (old == Suspend_Blocked && target == Blocked);


        // 单核模拟：系统中已有运行进程时，不允许另一个进程进入运行态
        if (target == Running) {
            for (const PCB& other : processes) {
                if (other.state == Running && other.pid != pid) {
                    cout << "已有进程正在运行，请先将它切换到就绪、阻塞或终止态。\n";
                    return false;
                }
            }
        }
        if (!allowed) {
        cout << "不允许从“" << stateStr(old)
             << "”转换到“" << stateStr(target) << "”。\n";
        return false;
    }

    // 挂起会释放内存；恢复前检查内存是否有空间
    bool wasActive = isActiveState(old);
    bool willBeActive = isActiveState(target);

    if (!wasActive && willBeActive && memoryUsed >= memoryLimit) {
        cout << "内存不足，无法恢复该进程。\n";
        return false;
    }

    p.state = target;
    memoryUsed += static_cast<int>(willBeActive) - static_cast<int>(wasActive);

    cout << "进程 " << pid << "："
         << stateStr(old) << " -> " << stateStr(target) << "\n";

    // 结束或挂起释放出的空间可用于接纳新建进程
    if (target == Terminated ||
        target == Suspend_Ready ||
        target == Suspend_Blocked) {
        admitNewProcess();
    }

    return true;
}
// 菜单
void menu() {
    cout << "\n====== 进程状态模拟器 ======\n"
         << "1. 创建进程\n"
         << "2. 显示进程\n"
         << "3. 手动状态转换\n"
         << "4. 自动运行一步\n"
         << "5. 自动运行直到结束\n"
         << "0. 退出\n"
         << "请选择：";
}

// 手动选择目标状态并转换
void manualTransition() {
    int pid;
    cout << "请输入进程 PID：";
    if (!(cin >> pid)) {
        cin.clear();
        cin.ignore(10000, '\n');
        cout << "PID 输入无效。\n";
        return;
    }

    cout << "可选目标状态：\n";
    for (int i = 0; i < State_Count; ++i) {
        cout << i << ". " << State_Names[i] << '\n';
    }

    int stateNumber;
    cout << "请输入目标状态编号：";
    if (!(cin >> stateNumber) ||
        stateNumber < 0 || stateNumber >= State_Count) {
        cin.clear();
        cin.ignore(10000, '\n');
        cout << "状态编号无效。\n";
        return;
    }

    changeState(pid, static_cast<ProcessState>(stateNumber));
}

// 自动模拟一步：运行一个就绪进程一个时间单位
void runOneStep() {
    // 若当前没有运行进程，选择第一个就绪进程运行
    int runningIndex = findProcessByState(Running);

    if (runningIndex == -1) {
        int readyIndex = findProcessByState(Ready);
        if (readyIndex == -1) {
            cout << "当前没有可运行的就绪进程。\n";
            return;
        }

        changeState(processes[readyIndex].pid, Running);
        runningIndex = findProcessByState(Running);
    }

    if (runningIndex == -1) return;

    PCB& p = processes[runningIndex];
    ++p.cpuTime;

    cout << "进程 " << p.pid << "（" << p.name
         << "）运行一个时间单位，CPU 时间："
         << p.cpuTime << "/" << p.needTime << "\n";

    if (p.cpuTime >= p.needTime) {
        changeState(p.pid, Terminated);
    } else {
        // 时间片结束，进程回到就绪态
        changeState(p.pid, Ready);
    }

    // 有空闲内存时接纳等待中的新建进程
    admitNewProcess();
}

// 自动运行，直到没有可运行进程或用户停止
void runUntilStop() {
    while (true) {
        bool hasRunnable = false;

        for (const PCB& p : processes) {
            if (p.state == Ready || p.state == Running) {
                hasRunnable = true;
                break;
            }
        }

        if (!hasRunnable) {
            cout << "没有就绪或运行中的进程，自动运行结束。\n";
            break;
        }

        runOneStep();
        showProcesses();

        cout << "按回车继续，输入 q 后回车停止：";
        string input;
        cin >> input;
        if (input == "q" || input == "Q") break;
    }
}

// 主函数
int main() {
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);
    int choice;

    while (true) {
        menu();

        if (!(cin >> choice)) {
            cin.clear();
            cin.ignore(10000, '\n');
            cout << "请输入有效的菜单编号。\n";
            continue;
        }

        switch (choice) {
        case 1:
            createProcess();
            break;

        case 2:
            showProcesses();
            break;

        case 3:
            manualTransition();
            break;

        case 4:
            runOneStep();
            break;

        case 5:
            runUntilStop();
            break;

        case 0:
            cout << "程序退出。\n";
            return 0;

        default:
            cout << "菜单选项无效，请重新输入。\n";
        }
    }
}