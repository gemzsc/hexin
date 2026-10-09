#include <iostream>
#include <vector>
#include <string>
#include <algorithm>
#include <iomanip>
#include <fstream>
#include <queue>
#include <functional>
#include <limits>
#include <cmath>

using namespace std;

//进程PCB结构体
struct Job {
    string name;
    int arrivalTime;        //到达时间
    int serviceTime;        //服务时间
    int priority;           //优先级，数值越小，优先级越高
    int deadline;           //截止时间
    int period;              //周期
    int remainTime;         //剩余服务时间
    int finishTime;         //完成时间
    bool isFinished;        //是否完成

    Job() : arrivalTime(0), serviceTime(0), priority(0), deadline(-1),
            period(-1), remainTime(0), finishTime(0), isFinished(false) {}
    Job(string n, int arr, int ser, int pri, int ddl, int per)
        : name(n), arrivalTime(arr), serviceTime(ser), priority(pri),
          deadline(ddl), period(per), remainTime(ser), finishTime(0), isFinished(false) {}
};

//时间片段结构
struct Segment {
    string name;        // 空闲时为 IDLE
    int startTime;
    int endTime;
};

//性能统计结果
struct Result {
    vector<Segment> segments;
    vector<int> finishTimes;
    vector<double> turnAround;
    vector<double> wTurnAround;
    double avgTA = 0.0;
    double avgwTA = 0.0;
};

//从文件加载作业列表
bool loadJobs(const string& filename, vector<Job>& jobs) {
    jobs.clear();
    ifstream file(filename);
    if (!file.is_open()) {
        cerr << "无法打开文件：" << filename << endl;
        return false;
    }

    string name;
    int arrivalTime, serviceTime, priority, deadline, period;
    while (file >> name >> arrivalTime >> serviceTime >> priority >> deadline >> period) {
        if (arrivalTime < 0 || serviceTime <= 0) {
            cerr << "作业数据无效：" << name << endl;
            return false;
        }
        jobs.emplace_back(name, arrivalTime, serviceTime, priority, deadline, period);
    }
    if (!file.eof()) {
        cerr << "文件中存在格式错误。每行需要六列数据。" << endl;
        return false;
    }
    if (jobs.empty()) {
        cerr << "文件中没有有效作业。" << endl;
        return false;
    }
    return true;
}

void appendSegment(Result& result, const string& name, int start, int end) {
    if (end <= start) return;
    // 合并相邻且同名的区间，保证输出简洁
    if (!result.segments.empty() && result.segments.back().name == name &&
        result.segments.back().endTime == start) {
        result.segments.back().endTime = end;
    } else {
        result.segments.push_back({name, start, end});
    }
}

void finishResult(const vector<Job>& jobs, Result& result) {
    result.turnAround.assign(jobs.size(), 0.0);
    result.wTurnAround.assign(jobs.size(), 0.0);
    result.avgTA = 0.0;
    result.avgwTA = 0.0;
    if (result.finishTimes.size() != jobs.size()) result.finishTimes.resize(jobs.size(), 0);

    for (size_t i = 0; i < jobs.size(); ++i) {
        double ta = result.finishTimes[i] - jobs[i].arrivalTime;
        double wta = ta / static_cast<double>(jobs[i].serviceTime);
        result.turnAround[i] = ta;
        result.wTurnAround[i] = wta;
        result.avgTA += ta;
        result.avgwTA += wta;
    }
    result.avgTA /= jobs.size();
    result.avgwTA /= jobs.size();
}

template <typename Better>
Result runNonPreemptive(const vector<Job>& jobs, Better better) {
    Result result;
    result.finishTimes.assign(jobs.size(), 0);
    vector<bool> done(jobs.size(), false);
    size_t completed = 0;
    int currentTime = 0;

    while (completed < jobs.size()) {
        int selected = -1;
        for (size_t i = 0; i < jobs.size(); ++i) {
            if (done[i] || jobs[i].arrivalTime > currentTime) continue;
            if (selected == -1 || better(jobs[i], jobs[selected], currentTime)) {
                selected = static_cast<int>(i);
            }
        }

        if (selected == -1) {
            int nextArrival = numeric_limits<int>::max();
            for (size_t i = 0; i < jobs.size(); ++i)
                if (!done[i]) nextArrival = min(nextArrival, jobs[i].arrivalTime);
            appendSegment(result, "IDLE", currentTime, nextArrival);
            currentTime = nextArrival;
            continue;
        }

        int endTime = currentTime + jobs[selected].serviceTime;
        appendSegment(result, jobs[selected].name, currentTime, endTime);
        currentTime = endTime;
        result.finishTimes[selected] = currentTime;
        done[selected] = true;
        ++completed;
    }
    finishResult(jobs, result);
    return result;
}

//FCFS调度算法
Result runFCFS(const vector<Job>& jobs) {
    return runNonPreemptive(jobs, [](const Job& a, const Job& b, int) {
        return a.arrivalTime < b.arrivalTime;
    });
}

//SJF调度算法
Result runSJF(const vector<Job>& jobs) {
    return runNonPreemptive(jobs, [](const Job& a, const Job& b, int) {
        if (a.serviceTime != b.serviceTime) return a.serviceTime < b.serviceTime;
        return a.arrivalTime < b.arrivalTime;
    });
}

//优先级调度算法，非抢占式
Result runPSA(const vector<Job>& jobs) {
    return runNonPreemptive(jobs, [](const Job& a, const Job& b, int) {
        if (a.priority != b.priority) return a.priority < b.priority;
        return a.arrivalTime < b.arrivalTime;
    });
}

//高响应比优先调度算法
Result runHRRN(const vector<Job>& jobs) {
    return runNonPreemptive(jobs, [](const Job& a, const Job& b, int now) {
        double waitA = now - a.arrivalTime;
        double waitB = now - b.arrivalTime;
        double ratioA = (waitA + a.serviceTime) / static_cast<double>(a.serviceTime);
        double ratioB = (waitB + b.serviceTime) / static_cast<double>(b.serviceTime);
        if (fabs(ratioA - ratioB) > 1e-9) return ratioA > ratioB;
        return a.arrivalTime < b.arrivalTime;
    });
}

//时间片轮转调度算法
Result runRR(const vector<Job>& jobs, int quantum) {
    Result result;
    result.finishTimes.assign(jobs.size(), 0);
    if (quantum <= 0) return result;

    queue<int> ready;
    vector<int> remaining(jobs.size());
    vector<bool> enqueued(jobs.size(), false);
    for (size_t i = 0; i < jobs.size(); ++i) remaining[i] = jobs[i].serviceTime;

    int currentTime = 0;
    size_t completed = 0;
    while (completed < jobs.size()) {
        for (size_t i = 0; i < jobs.size(); ++i) {
            if (!enqueued[i] && jobs[i].arrivalTime <= currentTime) {
                ready.push(static_cast<int>(i));
                enqueued[i] = true;
            }
        }
        if (ready.empty()) {
            int nextArrival = numeric_limits<int>::max();
            for (size_t i = 0; i < jobs.size(); ++i)
                if (!enqueued[i]) nextArrival = min(nextArrival, jobs[i].arrivalTime);
            appendSegment(result, "IDLE", currentTime, nextArrival);
            currentTime = nextArrival;
            continue;
        }

        int i = ready.front();
        ready.pop();
        int runTime = min(quantum, remaining[i]);
        int endTime = currentTime + runTime;
        appendSegment(result, jobs[i].name, currentTime, endTime);
        currentTime = endTime;
        remaining[i] -= runTime;

        // 先將本時間片內到達的作業加入隊列，再把未完成的當前作業放到隊尾
        for (size_t j = 0; j < jobs.size(); ++j) {
            if (!enqueued[j] && jobs[j].arrivalTime <= currentTime) {
                ready.push(static_cast<int>(j));
                enqueued[j] = true;
            }
        }
        if (remaining[i] > 0) {
            ready.push(i);
        } else {
            result.finishTimes[i] = currentTime;
            ++completed;
        }
    }
    finishResult(jobs, result);
    return result;
}

template <typename Better>
Result runPreemptive(const vector<Job>& jobs, Better better, const string& label, bool showTrace) {
    Result result;
    result.finishTimes.assign(jobs.size(), 0);
    vector<int> remaining(jobs.size());
    vector<bool> done(jobs.size(), false);
    for (size_t i = 0; i < jobs.size(); ++i) remaining[i] = jobs[i].serviceTime;

    size_t completed = 0;
    int currentTime = 0;
    while (completed < jobs.size()) {
        vector<int> ready;
        for (size_t i = 0; i < jobs.size(); ++i)
            if (!done[i] && jobs[i].arrivalTime <= currentTime) ready.push_back(static_cast<int>(i));

        if (ready.empty()) {
            int nextArrival = numeric_limits<int>::max();
            for (size_t i = 0; i < jobs.size(); ++i)
                if (!done[i]) nextArrival = min(nextArrival, jobs[i].arrivalTime);
            if (showTrace) cout << "t=" << currentTime << ": 就绪队列为空，CPU 空闲到 t=" << nextArrival << endl;
            appendSegment(result, "IDLE", currentTime, nextArrival);
            currentTime = nextArrival;
            continue;
        }

        int selected = ready[0];
        for (size_t k = 1; k < ready.size(); ++k)
            if (better(ready[k], selected, jobs, remaining, currentTime)) selected = ready[k];

        if (showTrace) {
            cout << "t=" << currentTime << " 就绪队列: ";
            for (int i : ready) {
                cout << jobs[i].name << "(剩余=" << remaining[i];
                if (jobs[i].deadline >= 0) cout << ",截止=" << jobs[i].deadline;
                else cout << ",无截止时间";
                if (label == "LLF" && jobs[i].deadline >= 0)
                    cout << ",松弛度=" << jobs[i].deadline - currentTime - remaining[i];
                cout << ") ";
            }
            cout << "=> 选择 " << jobs[selected].name << endl;
        }

        appendSegment(result, jobs[selected].name, currentTime, currentTime + 1);
        --remaining[selected];
        ++currentTime;
        if (remaining[selected] == 0) {
            done[selected] = true;
            result.finishTimes[selected] = currentTime;
            ++completed;
        }
    }
    finishResult(jobs, result);
    return result;
}

//优先级抢占式调度算法
Result runPreemptivePriority(const vector<Job>& jobs) {
    return runPreemptive(jobs,
        [](int a, int b, const vector<Job>& j, const vector<int>&, int) {
            if (j[a].priority != j[b].priority) return j[a].priority < j[b].priority;
            return j[a].arrivalTime < j[b].arrivalTime;
        }, "优先级抢占", false);
}

//最早截止时间优先调度算法（抢占式）
Result runEDF(const vector<Job>& jobs) {
    return runPreemptive(jobs,
        [](int a, int b, const vector<Job>& j, const vector<int>&, int) {
            int da = j[a].deadline < 0 ? numeric_limits<int>::max() : j[a].deadline;
            int db = j[b].deadline < 0 ? numeric_limits<int>::max() : j[b].deadline;
            if (da != db) return da < db;
            return j[a].arrivalTime < j[b].arrivalTime;
        }, "EDF", true);
}

//最低松弛度优先调度算法（抢占式）
Result runLLF(const vector<Job>& jobs) {
    return runPreemptive(jobs,
        [](int a, int b, const vector<Job>& j, const vector<int>& rem, int now) {
            long long la = j[a].deadline < 0 ? numeric_limits<long long>::max() :
                           static_cast<long long>(j[a].deadline) - now - rem[a];
            long long lb = j[b].deadline < 0 ? numeric_limits<long long>::max() :
                           static_cast<long long>(j[b].deadline) - now - rem[b];
            if (la != lb) return la < lb;
            return j[a].arrivalTime < j[b].arrivalTime;
        }, "LLF", true);
}

//输出调度序列与性能指标
void printResult(const string& title, const vector<Job>& jobs, const Result& result) {
    cout << "\n========== " << title << " ==========\n";
    cout << "调度时间线：";
    for (const Segment& s : result.segments)
        cout << " [" << s.startTime << "," << s.endTime << ") " << s.name;
    cout << endl;
    cout << left << setw(10) << "作业" << setw(12) << "完成时间"
         << setw(12) << "周转时间" << setw(16) << "带权周转时间" << endl;
    for (size_t i = 0; i < jobs.size(); ++i) {
        cout << left << setw(10) << jobs[i].name << setw(12) << result.finishTimes[i]
             << setw(12) << fixed << setprecision(2) << result.turnAround[i]
             << setw(16) << result.wTurnAround[i] << endl;
    }
    cout << "平均周转时间：" << fixed << setprecision(2) << result.avgTA
         << "，平均带权周转时间：" << result.avgwTA << endl;

    bool hasRealtimeJob = false;
    bool schedulable = true;
    for (size_t i = 0; i < jobs.size(); ++i) {
        if (jobs[i].deadline < 0) continue;
        hasRealtimeJob = true;
        bool met = result.finishTimes[i] <= jobs[i].deadline;
        if (!met) schedulable = false;
        cout << jobs[i].name << (met ? " 按时完成" : " 错过截止时间")
             << "（截止=" << jobs[i].deadline << "，完成=" << result.finishTimes[i] << "）" << endl;
    }
    if (hasRealtimeJob) cout << "该任务集按此单次作业模拟" << (schedulable ? "可调度。" : "不可调度。") << endl;
}

int main() {
    vector<Job> jobs;
    if (!loadJobs("processes.txt", jobs)) return 1;

    cout << "成功读取 " << jobs.size() << " 个作业。"
         << "实时算法将 deadline 视为绝对截止时刻；period 当前只读取，不重复释放作业。\n";

    printResult("FCFS", jobs, runFCFS(jobs));
    printResult("SJF（非抢占）", jobs, runSJF(jobs));
    printResult("PSA（非抢占）", jobs, runPSA(jobs));
    printResult("HRRN", jobs, runHRRN(jobs));
    printResult("优先级抢占式", jobs, runPreemptivePriority(jobs));

    for (int q : {1, 2, 4})
        printResult("RR，时间片 q=" + to_string(q), jobs, runRR(jobs, q));

    printResult("EDF（逐时刻决策）", jobs, runEDF(jobs));
    printResult("LLF（逐时刻松弛度决策）", jobs, runLLF(jobs));
    return 0;
}



