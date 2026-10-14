# 基础彩色版

基于 C++ 和 raylib 的纯色方块连连看。点击两个相同颜色且路径符合规则的方块即可消除。

## 依赖

- 64 位 MinGW-w64 `g++`
- raylib 6.0 Windows MinGW-w64 发行包

## 编译和运行

在 PowerShell 中进入本目录，并把 `$raylib` 改成 raylib 的实际解压目录：

```powershell
$raylib = "C:\raylib\raylib-6.0_win64_mingw-w64"
g++ .\Linkup.cpp -o .\Linkup.exe `
  -I "$raylib\include" `
  -L "$raylib\lib" `
  -lraylib -lopengl32 -lgdi32 -lwinmm -lshcore `
  '-Wl,--defsym,stat64i32=_stat64'
.\Linkup.exe
```

最后的链接器参数用于兼容部分 MinGW-w64 环境与预编译 raylib 包之间的 `stat64i32` 符号差异。若使用自行构建的 raylib，可以按其构建说明调整链接参数。
