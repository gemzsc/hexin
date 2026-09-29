# 学习与课程项目

本仓库用于保存大学期间的编程练习和课程实践项目。

## 内容

- [`LeetCode/`](./LeetCode/)：LeetCode 题解与编程练习
- [`计网/`](./计网/)：计算机网络课程实践项目——局域网数据包捕获程序
- [`C++程序设计课程实践/`](./C++程序设计课程实践/)：C++程序设计课程实践项目——高校学生管理系统


## 目录与分支

日常学习代码统一维护在默认分支 `master`，按文件夹分类，不需要切换分支查看不同课程。

```text
hexin/
├── .gitignore
├── README.md
├── LeetCode/                 # C++ 刷题代码
├── C++程序设计课程实践/       # 高校学生管理系统
└── 计网/                     # 数据包捕获程序
```

`LeetCode`、`计网`、`C++程序设计课程实践` 分支保留作为历史参考，后续更新提交到 `master` 的对应文件夹。开发需要隔离的功能或试验时，可以新建临时分支，完成后再合并。

## 使用

克隆仓库：

```bash
git clone https://github.com/gemzsc/hexin.git
cd hexin
```

各项目的运行方式见对应文件夹的 README。LeetCode 题解主要用于在线判题，不作为一个完整程序统一编译。

## 添加题解

确认当前分支为 `master`，在 `LeetCode/` 中创建或修改题解文件，然后暂存、提交并推送。例如：

```bash
git add LeetCode/LeetCode12.cpp
git commit -m "更新 LeetCode 12 题解"
git push origin master
```
