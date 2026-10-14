#include<stdio.h>
#include<stdlib.h>
#include<time.h>
#include"raylib.h"
#include<string.h>

#define WIDTH 8      // 宽度
#define HEIGHT 8     // 高度
#define COLOR_COUNT 6      // 6种颜色
Texture2D tileTextures[COLOR_COUNT];

// 棋盘数据：-1表示空格，1-6表示不同颜色
int board[HEIGHT][WIDTH];
// 标记棋盘是否被消除
int eliminated[HEIGHT][WIDTH];

// 游戏状态
int score = 0;
int totalPairs = 0;
int remainingPairs = 0;

// 字符转数字（只支持字母 a-z）
int charToNum(char c) {
    if(c >= 'a' && c <= 'z') {
        return c - 'a';
    }
    if(c >= 'A' && c <= 'Z') {
        return c - 'A';
    }
    return -1;
}

// 数字转字符
char numToChar(int num) {
    return 'a' + num;
}

// 验证坐标是否有效
int isValidCoord(int x, int y) {
    return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
}

// 初始化游戏
void initGame() {
    // 重置棋盘
    for(int i = 0; i < HEIGHT; i++) {
        for(int j = 0; j < WIDTH; j++) {
            board[i][j] = -1;
            eliminated[i][j] = 0;
        }
    }

    // 计算需要生成的对数（必须成对，所以总数是偶数）
    int totalCells = HEIGHT * WIDTH;
    if(totalCells % 2 != 0) totalCells--;
    totalPairs = totalCells / 2;
    remainingPairs = totalPairs;

    // 生成成对的颜色
    for(int i = 0; i < totalCells; i += 2) {
        int color = (i / 2) % COLOR_COUNT + 1;
        board[i / WIDTH][i % WIDTH] = color;
        board[(i + 1) / WIDTH][(i + 1) % WIDTH] = color;
    }

    // 打乱棋盘
    for(int i = 0; i < totalCells * 3; i++) {
        int x1 = rand() % WIDTH;
        int y1 = rand() % HEIGHT;
        int x2 = rand() % WIDTH;
        int y2 = rand() % HEIGHT;
        if(board[y1][x1] != -1 && board[y2][x2] != -1) {
            int temp = board[y1][x1];
            board[y1][x1] = board[y2][x2];
            board[y2][x2] = temp;
        }
    }
}

// 显示棋盘
void showBoard() {
    system("cls");  // 清屏
    printf("\n");
    printf("    ");
    for(int x = 0; x < WIDTH; x++) {
        printf(" %c ", numToChar(x));
    }
    printf("\n");
    printf("    ");
    for(int x = 0; x < WIDTH; x++) {
        printf("---");
    }
    printf("\n");

    for(int y = 0; y < HEIGHT; y++) {
        printf(" %c ", numToChar(y));
        for(int x = 0; x < WIDTH; x++) {
            printf("|");
            if(eliminated[y][x]) {
                printf("  ");  // 已消除的位置显示空格
            } else if(board[y][x] == -1) {
                printf("  ");
            } else {
                // 用不同颜色字符显示
                char colors[] = {'R', 'G', 'B', 'Y', 'P', 'O'};
                printf("%c ", colors[board[y][x] - 1]);
            }
        }
        printf("|\n");
        printf("    ");
        for(int x = 0; x < WIDTH; x++) {
            printf("---");
        }
        printf("\n");
    }

    printf("\n剩余配对: %d 对  得分: %d\n", remainingPairs, score);
    printf("提示: 输入两个坐标，格式: 坐标1坐标2\n");
    printf("  坐标格式: 列字母+行字母 (范围: a-h)\n");
    printf("  示例1: 'abcd' 表示选择 (a,b) 和 (c,d)\n");
    printf("  示例2: 'a,b,c,d' 或 'a b c d'\n");
    printf("输入 'q' 退出游戏\n");
    printf("----------------------------------------\n");
}

// 获取玩家输入
int getInput(int *x1, int *y1, int *x2, int *y2) {
    char input[100];
    printf("请输入两个坐标: ");

    if(fgets(input, sizeof(input), stdin) == NULL) {
        return -1;
    }

    // 去除换行符
    size_t len = strlen(input);
    if(len > 0 && input[len-1] == '\n') {
        input[len-1] = '\0';
    }

    // 检查退出命令
    if(strcmp(input, "q") == 0 || strcmp(input, "Q") == 0 ||
       strcmp(input, "quit") == 0 || strcmp(input, "exit") == 0) {
        return 0;
    }

    // 去除所有空格和制表符
    char cleanInput[50];
    int idx = 0;
    for(int i = 0; input[i] != '\0' && idx < 49; i++) {
        if(input[i] != ' ' && input[i] != '\t') {
            cleanInput[idx++] = input[i];
        }
    }
    cleanInput[idx] = '\0';

    // 如果去除空格后为空
    if(strlen(cleanInput) == 0) {
        printf("输入为空，请重新输入\n");
        return -1;
    }

    // 提取所有字母字符
    char chars[10];
    idx = 0;
    for(int i = 0; cleanInput[i] != '\0' && idx < 8; i++) {
        if((cleanInput[i] >= 'a' && cleanInput[i] <= 'z') ||
           (cleanInput[i] >= 'A' && cleanInput[i] <= 'Z')) {
            chars[idx++] = cleanInput[i];
        }
    }
    chars[idx] = '\0';

    // 检查是否有足够的字符
    if(strlen(chars) < 4) {
        printf("错误: 输入太短，需要4个字母（如: abcd 或 a,b,c,d）\n");
        return -1;
    }

    // 解析字符
    int positions[4];
    for(int i = 0; i < 4; i++) {
        positions[i] = charToNum(chars[i]);
        if(positions[i] == -1) {
            printf("错误: 包含无效字符: %c\n", chars[i]);
            return -1;
        }
    }

    *x1 = positions[0];  // 第一个字母 = 列坐标
    *y1 = positions[1];  // 第二个字母 = 行坐标
    *x2 = positions[2];  // 第三个字母 = 列坐标
    *y2 = positions[3];  // 第四个字母 = 行坐标

    // 验证坐标范围
    if(!isValidCoord(*x1, *y1)) {
        printf("错误: 第一个坐标 (%c,%c) 超出范围 (a-h, a-h)\n",
               numToChar(*x1), numToChar(*y1));
        return -1;
    }

    if(!isValidCoord(*x2, *y2)) {
        printf("错误: 第二个坐标 (%c,%c) 超出范围 (a-h, a-h)\n",
               numToChar(*x2), numToChar(*y2));
        return -1;
    }

    return 1;
}

// 检查两点之间是否直线连通（0拐点）
int checkLine(int x1, int y1, int x2, int y2) {
    if(x1 == x2) {
        // 垂直方向检查
        int minY = y1 < y2 ? y1 : y2;
        int maxY = y1 > y2 ? y1 : y2;
        for(int y = minY + 1; y < maxY; y++) {
            if(!eliminated[y][x1] && board[y][x1] != -1) {
                return 0;  // 有障碍物
            }
        }
        return 1;
    }
    else if(y1 == y2) {
        // 水平方向检查
        int minX = x1 < x2 ? x1 : x2;
        int maxX = x1 > x2 ? x1 : x2;
        for(int x = minX + 1; x < maxX; x++) {
            if(!eliminated[y1][x] && board[y1][x] != -1) {
                return 0;
            }
        }
        return 1;
    }
    return 0;
}

// 检查两点之间L型连通（1拐点）
int checkL(int x1, int y1, int x2, int y2) {
    // 拐点1: (x2, y1)
    if(eliminated[y1][x2] || board[y1][x2] == -1) {
        if(checkLine(x1, y1, x2, y1) && checkLine(x2, y1, x2, y2)) {
            return 1;
        }
    }
    // 拐点2: (x1, y2)
    if(eliminated[y2][x1] || board[y2][x1] == -1) {
        if(checkLine(x1, y1, x1, y2) && checkLine(x1, y2, x2, y2)) {
            return 1;
        }
    }
    return 0;
}

// 检查两点之间Z型连通（2拐点）
int checkZ(int x1, int y1, int x2, int y2) {
    // 垂直扫描
    for(int y = 0; y < HEIGHT; y++) {
        if(y != y1 && y != y2) {
            if((eliminated[y][x1] || board[y][x1] == -1) &&
               (eliminated[y][x2] || board[y][x2] == -1)) {
                if(checkLine(x1, y1, x1, y) &&
                   checkLine(x1, y, x2, y) &&
                   checkLine(x2, y, x2, y2)) {
                    return 1;
                }
            }
        }
    }
    // 水平扫描
    for(int x = 0; x < WIDTH; x++) {
        if(x != x1 && x != x2) {
            if((eliminated[y1][x] || board[y1][x] == -1) &&
               (eliminated[y2][x] || board[y2][x] == -1)) {
                if(checkLine(x1, y1, x, y1) &&
                   checkLine(x, y1, x, y2) &&
                   checkLine(x, y2, x2, y2)) {
                    return 1;
                }
            }
        }
    }
    return 0;
}

// 检查两个格子能否消除
int canEliminate(int x1, int y1, int x2, int y2) {
    // 如果是同一个点
    if(x1 == x2 && y1 == y2) return 0;

    // 检查是否已被消除
    if(eliminated[y1][x1] || eliminated[y2][x2]) return 0;

    // 检查颜色是否相同
    if(board[y1][x1] != board[y2][x2]) return 0;

    // 检查是否为空
    if(board[y1][x1] == -1 || board[y2][x2] == -1) return 0;

    // 检查路径
    if(checkLine(x1, y1, x2, y2)) return 1;
    if(checkL(x1, y1, x2, y2)) return 1;
    if(checkZ(x1, y1, x2, y2)) return 1;

    return 0;
}

// 执行消除
void doEliminate(int x1, int y1, int x2, int y2) {
    eliminated[y1][x1] = 1;
    eliminated[y2][x2] = 1;
    score += 10;
    remainingPairs--;
}

// 检查游戏是否胜利
int checkWin() {
    return remainingPairs == 0;
}

// 检查是否还有可消除的对
int hasRemainingMoves() {
    for(int y1 = 0; y1 < HEIGHT; y1++) {
        for(int x1 = 0; x1 < WIDTH; x1++) {
            if(eliminated[y1][x1] || board[y1][x1] == -1) continue;
            for(int y2 = 0; y2 < HEIGHT; y2++) {
                for(int x2 = 0; x2 < WIDTH; x2++) {
                    if((x1 == x2 && y1 == y2) || eliminated[y2][x2] || board[y2][x2] == -1) continue;
                    if(board[y1][x1] == board[y2][x2]) {
                        if(canEliminate(x1, y1, x2, y2)) {
                            return 1;
                        }
                    }
                }
            }
        }
    }
    return 0;
}

// 重新洗牌
void shuffleBoard() {
    // 收集所有未被消除的棋子
    int pieces[WIDTH * HEIGHT][2];
    int count = 0;
    for(int y = 0; y < HEIGHT; y++) {
        for(int x = 0; x < WIDTH; x++) {
            if(!eliminated[y][x] && board[y][x] != -1) {
                pieces[count][0] = x;
                pieces[count][1] = y;
                count++;
            }
        }
    }

    // 如果是奇数，去掉最后一个
    if(count % 2 != 0) count--;

    // 随机交换
    for(int i = 0; i < count * 2; i++) {
        int idx1 = rand() % count;
        int idx2 = rand() % count;
        if(idx1 != idx2) {
            int tempX = pieces[idx1][0];
            int tempY = pieces[idx1][1];
            pieces[idx1][0] = pieces[idx2][0];
            pieces[idx1][1] = pieces[idx2][1];
            pieces[idx2][0] = tempX;
            pieces[idx2][1] = tempY;
        }
    }

    // 重新分配颜色
    for(int i = 0; i < count; i += 2) {
        int color = (i / 2) % COLOR_COUNT + 1;
        board[pieces[i][1]][pieces[i][0]] = color;
        board[pieces[i+1][1]][pieces[i+1][0]] = color;
    }
}

//棋盘绘制函数
const int boardLeft=40;
const int boardTop=100;
const int cellSize=66;
const int cellGap=8;
const int cellStep=cellSize+cellGap;

void drawBoard(int selectedX, int selectedY){
    Color tileColors[COLOR_COUNT]={
        RED,GREEN,BLUE,GOLD,PURPLE,ORANGE
    };

    for(int y=0;y<HEIGHT;y++){
        for(int x=0; x < WIDTH; x++){
            int px = boardLeft + cellStep * x;
            int py = boardTop + cellStep * y;

            if(eliminated[y][x]||board[y][x]==-1){
                continue;
            }

           int textureIndex = board[y][x] - 1;

            if (textureIndex < 0 || textureIndex >= COLOR_COUNT) {
                continue;
            }

            if (tileTextures[textureIndex].id != 0) {
                DrawTexturePro(
                    tileTextures[textureIndex],
                    {
                        0, 0,
                        (float)tileTextures[textureIndex].width,
                        (float)tileTextures[textureIndex].height
                    },
                    {
                        (float)px, (float)py,
                        (float)cellSize, (float)cellSize
                    },
                    { 0, 0 },
                    0,
                    WHITE
                );
            } else {
                // 图片加载失败时显示彩色方块和问号，方便判断棋盘仍在绘制
                DrawRectangle(px, py, cellSize, cellSize,
                            tileColors[textureIndex]);
                DrawText("?", px + 24, py + 18, 30, WHITE);
            }

            if(x ==selectedX && y == selectedY){
                DrawRectangleLinesEx(
                    {(float)px,(float)py,(float)cellSize,(float)cellSize},
                    4,
                    WHITE
                );
            }
        }
    }
}



// 主游戏循环
int main() {
    srand((unsigned int)time(NULL));
    initGame();
    score = 0;

    InitWindow(980, 760, "Link Up");

    TraceLog(LOG_INFO, "CUSTOM IMAGE LOADER STARTED: %s",
            GetApplicationDirectory());

    const char* appDir = GetApplicationDirectory();
    TraceLog(LOG_INFO, "图片目录：%s", appDir);

    for (int i = 0; i < COLOR_COUNT; i++) {
        char path[512];
        snprintf(path, sizeof(path), "%sassets/tile%d.png", appDir, i + 1);

        tileTextures[i] = LoadTexture(path);

        TraceLog(LOG_INFO, "tile%d: path=%s, id=%u, size=%dx%d",
                i + 1, path,
                tileTextures[i].id,
                tileTextures[i].width,
                tileTextures[i].height);
    }

    SetTargetFPS(60);

    int selectedX = -1;
    int selectedY = -1;
    const char* message = "Click two matching tiles";
    bool gameOver = false;

    while (!WindowShouldClose()) {
        if (!gameOver && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
            Vector2 mouse = GetMousePosition();

            int relX = (int)mouse.x - boardLeft;
            int relY = (int)mouse.y - boardTop;

            if (relX >= 0 && relY >= 0) {
                int x = relX / cellStep;
                int y = relY / cellStep;
                int offsetX = relX % cellStep;
                int offsetY = relY % cellStep;

                // 忽略格子间的空隙和棋盘以外的点击
                if (x < WIDTH && y < HEIGHT &&
                    offsetX < cellSize && offsetY < cellSize &&
                    !eliminated[y][x] && board[y][x] != -1) {

                    if (selectedX == -1) {
                        selectedX = x;
                        selectedY = y;
                    } else if (selectedX == x && selectedY == y) {
                        selectedX = -1;
                        selectedY = -1;
                    } else if (canEliminate(selectedX, selectedY, x, y)) {
                        doEliminate(selectedX, selectedY, x, y);
                        selectedX = -1;
                        selectedY = -1;
                        message = "Match!";
                    } else {
                        // 本次点击成为新的第一个选择
                        selectedX = x;
                        selectedY = y;
                        message = "No match or path blocked";
                    }
                }
            }
        }

        if (!gameOver && checkWin()) {
            gameOver = true;
            message = "You win! Press R to restart";
        }

        if (!gameOver && !hasRemainingMoves()) {
            shuffleBoard();
            message = "Board shuffled";
        }

        if (gameOver && IsKeyPressed(KEY_R)) {
            initGame();
            score = 0;
            selectedX = -1;
            selectedY = -1;
            gameOver = false;
            message = "Click two matching tiles";
        }

        BeginDrawing();
        ClearBackground(Color{ 27, 35, 52, 255 });

        DrawText("LINK UP", 680, 120, 36, RAYWHITE);
        DrawText(TextFormat("Pairs: %d", remainingPairs), 680, 200, 24, RAYWHITE);
        DrawText(TextFormat("Score: %d", score), 680, 240, 24, RAYWHITE);
        DrawText(message, 680, 320, 18, LIGHTGRAY);
        DrawText("Click two matching tiles", 680, 390, 18, LIGHTGRAY);
        DrawText("R: Restart after winning", 680, 425, 18, LIGHTGRAY);
        DrawText("ESC: Quit", 680, 460, 18, LIGHTGRAY);

        drawBoard(selectedX, selectedY);
        EndDrawing();
    }

    for(int i=0; i< COLOR_COUNT; i++){
        UnloadTexture(tileTextures[i]);
    }

    CloseWindow();
    return 0;
}
