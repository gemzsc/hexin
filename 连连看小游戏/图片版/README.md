# 图片版

基于 C++ 和 raylib 的图片方块连连看。程序从运行目录旁的 `assets` 文件夹读取六张 PNG 图片。

## 替换图片

将 `assets/tile1.png` 至 `assets/tile6.png` 替换为自己的 PNG 图片，保留文件名。建议使用一致的长宽比和接近的尺寸。若要改变图片数量，还需同步修改 `COLOR_COUNT` 和相关配对逻辑。

## 编译和运行

需要 64 位 MinGW-w64 `g++` 和 raylib 6.0 Windows MinGW-w64 发行包。在 PowerShell 中进入本目录，并把 `$raylib` 改成 raylib 的实际解压目录：

```powershell
$raylib = "C:\raylib\raylib-6.0_win64_mingw-w64"
g++ .\Linkup.cpp -o .\Linkup.exe `
  -I "$raylib\include" `
  -L "$raylib\lib" `
  -lraylib -lopengl32 -lgdi32 -lwinmm -lshcore `
  '-Wl,--defsym,stat64i32=_stat64'
.\Linkup.exe
```

`assets` 文件夹必须与运行的 `Linkup.exe` 位于同一项目目录。最后的链接器参数用于兼容部分 MinGW-w64 环境与预编译 raylib 包之间的 `stat64i32` 符号差异。若使用自行构建的 raylib，可以按其构建说明调整链接参数。
