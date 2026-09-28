#include <vector>
#include <string>
using namespace std;

class Solution {
public:
    bool isMatch(string s, string p) {
        int m = s.size(), n = p.size();
        vector<vector<bool>> dp(m + 1, vector<bool>(n + 1, false));
        dp[0][0] = true;

        // 空串匹配形如 a*, a*b*, a*b*c* 的 pattern
        for (int j = 2; j <= n; ++j)
            if (p[j - 1] == '*')
                dp[0][j] = dp[0][j - 2];

        for (int i = 1; i <= m; ++i) {
            for (int j = 1; j <= n; ++j) {
                if (p[j - 1] == '*') {
                    // 0 次：忽略 p[j-2] 和 '*'
                    dp[i][j] = dp[i][j - 2];
                    // 1 次或多次：当前字符匹配，则沿用 dp[i-1][j]
                    if (p[j - 2] == '.' || p[j - 2] == s[i - 1])
                        dp[i][j] = dp[i][j] || dp[i - 1][j];
                } else {
                    // 普通字符或 '.'
                    if (p[j - 1] == '.' || p[j - 1] == s[i - 1])
                        dp[i][j] = dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }
};
