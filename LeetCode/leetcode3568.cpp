#include <vector>
#include <iostream>
#include <queue>
#include <tuple>
#include <utility>
#include <algorithm>
#include <cstring>
using namespace std;

using PII = pair<int, int>;
// 方向数组，抽成全局常量
const int dx[] = {-1, 1, 0, 0};
const int dy[] = {0, 0, -1, 1};
const int NO_RECORD = -1;

void parseMap(const vector<string>& classroom,
              int& sx, int& sy,
              vector<PII>& litters,
              vector<vector<int>>& litterId)
{
    sx = -1;
    sy = -1;
    litters.clear();
    int m = classroom.size();
    int n = classroom[0].size();
    litterId.assign(m, vector<int>(n, -1));

    for(int i = 0; i < m; i++)
    {
        for(int j = 0; j < n; j++)
        {
            char ch = classroom[i][j];
            if(ch == 'S')
            {
                sx = i;
                sy = j;
            }
            else if(ch == 'L')
            {
                litterId[i][j] = litters.size();
                litters.emplace_back(i, j);
            }
        }
    }
}


bool shouldEnqueue(vector<vector<vector<int>>>& best,
                   int nx, int ny, int new_mask, int new_rem)
{
    if(new_rem > best[nx][ny][new_mask])
    {
        best[nx][ny][new_mask] = new_rem;
        return true;
    }
    return false;
}


int bfsSearch(const vector<string>& classroom,
              int maxEnergy,
              int sx, int sy,
              const vector<vector<int>>& litterId,
              int k)
{
    int m = classroom.size();
    int n = classroom[0].size();
    int fullMask = (1 << k) - 1;

    // best[x][y][mask] 保存到达该状态最大剩余能量
    vector<vector<vector<int>>> best(m, vector<vector<int>>(n, vector<int>(1 << k, NO_RECORD)));
    // 队列元素：x,y,mask,剩余能量,步数
    queue<tuple<int, int, int, int, int>> q;

    best[sx][sy][0] = maxEnergy;
    q.emplace(sx, sy, 0, maxEnergy, 0);

    while(!q.empty())
    {
        auto [x, y, mask, rem, step] = q.front();
        q.pop();

        // 全部垃圾收集完成，直接返回步数
        if(mask == fullMask)
        {
            return step;
        }
        // 剪枝：当前状态不如历史最优，跳过
        if(rem < best[x][y][mask])
        {
            continue;
        }

        for(int d = 0; d < 4; d++)
        {
            int nx = x + dx[d];
            int ny = y + dy[d];
            // 越界
            if(nx < 0 || nx >= m || ny < 0 || ny >= n)
                continue;
            // 障碍物
            if(classroom[nx][ny] == 'X')
                continue;
            // 移动一步消耗1能量，移动前必须还有能量
            if(rem - 1 <0)
                continue;

            int newRem = rem - 1;
            int newMask = mask;

            //踩到补给R，能量补满上限
            if(classroom[nx][ny] == 'R')
            {
                newRem = maxEnergy;
            }
            //踩到垃圾L，更新mask
            if(litterId[nx][ny] != -1)
            {
                newMask = mask | (1 << litterId[nx][ny]);
            }

            if(shouldEnqueue(best, nx, ny, newMask, newRem))
            {
                q.emplace(nx, ny, newMask, newRem, step + 1);
            }
        }
    }
    return -1;
}

class Solution {
public:
    int minMoves(vector<string>& classroom, int energy) {
        int sx, sy;
        vector<PII> litters;
        vector<vector<int>> litterId;

        // 模块1：解析地图
        parseMap(classroom, sx, sy, litters, litterId);
        int k = litters.size();
        if(k == 0)
        {
            return 0;
        }
        // 模块2：BFS搜索求解
        return bfsSearch(classroom, energy, sx, sy, litterId, k);
    }
};

// 测试入口
int main()
{
    vector<string> testMap = {
        "S.L",
        ".R#",
        "L.."
    };
    Solution sol;
    cout << sol.minMoves(testMap, 5) << endl;
    return 0;
}
