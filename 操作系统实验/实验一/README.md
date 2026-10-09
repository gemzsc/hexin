# 实验一：进程的状态转换

本目录保存操作系统实验一的进程状态模拟程序。

## 编译与运行

在 Windows 上使用 MinGW-w64 g++：

```powershell
g++ -std=c++11 .\Experiment1.cpp -o .\Experiment1.exe
.\Experiment1.exe
```

程序提供进程创建、状态查看、手动状态转换和自动调度模拟等菜单功能。可驻留进程数上限在源码中由 `memoryLimit` 设置。
