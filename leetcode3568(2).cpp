#include<vector>
#include<unordered_map>
#include<deque>
#include<string>
using namespace std;
class Solution {
public:
    struct point {
        int d,x,y,mask;
    };
    int minMoves(vector<string>& classroom, int energy) {
        int m = classroom.size(),n = classroom[0].size();
        deque <point> dq;
        int cnt = 0;
        unordered_map <int,int> Map;
        pair <int,int> begin = {-1,-1};
        for (int i = 0;i < m;i ++) {
            for (int j = 0;j < n;j ++) {
                if (classroom[i][j] == 'S') {
                    dq.push_back(point{0,i,j,0});
                    begin = {i,j};
                } else if (classroom[i][j] == 'L') {
                    int hash = i * n + j;
                    Map[hash] = cnt ++;
                }
            }
        }
        int limit = 1 << cnt;
        int ey[m + 1][n + 1][limit + 1];
        memset(ey,-1,sizeof(ey));
        ey[begin.first][begin.second][0] = energy;
        pair<int,int> dirs[4] = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!dq.empty()) {
            auto[d,x,y,mask] = dq.front();
            if (mask == limit - 1)
                return d;
            int e = ey[x][y][mask];
            dq.pop_front();
            for (auto[ax,ay] : dirs) {
                if (x + ax < 0 || x + ax >= m || y + ay < 0 || y + ay >= n || e <= 0)
                    continue;
                int nx = x + ax,ny = y + ay,nmask = mask,ne = e - 1;
                if (classroom[nx][ny] == 'X')
                    continue;
                if (classroom[nx][ny] == 'L') {
                    int hash = nx * n + ny;
                    int idx = Map[hash];
                    if (!(nmask & (1 << idx))) {
                        nmask |= (1 << idx);
                    }
                } else if (classroom[nx][ny] == 'R') {
                    ne = energy;
                }
                if (ey[nx][ny][nmask] >= ne)
                    continue;
                ey[nx][ny][nmask] = ne;
                dq.push_back(point{d + 1,nx,ny,nmask});
            }
        }
        return -1;
    }
};