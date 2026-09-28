#include <string>
#include <vector>
using namespace std;

class Solution {
    vector<vector<int>> memo;
public:
    bool isMatch(string s, string p) {
        memo.assign(s.size() + 1, vector<int>(p.size() + 1, -1));
        return dp(s, p, 0, 0);
    }
    bool dp(string& s, string& p, int i, int j) {
        if (memo[i][j] != -1) return memo[i][j];   

        if (j == p.size())                          
            return memo[i][j] = (i == s.size());    

        bool first = i < s.size() && (s[i] == p[j] || p[j] == '.');

        if (j + 1 < p.size() && p[j + 1] == '*') {
          
            return memo[i][j] = dp(s, p, i, j + 2) || (first && dp(s, p, i + 1, j));
        }
        
        return memo[i][j] = first && dp(s, p, i + 1, j + 1);
    }
};
