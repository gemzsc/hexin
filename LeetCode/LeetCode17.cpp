#include<vector>
#include<string>
using namespace std;

class Solution {
public:
    vector<string> letterCombinations(string digits) {
        vector<string> res;
        if(digits.empty()) return res;
        //数字映射
        vector<string> map={"","","abc","def","ghi","jkl","mno","pqrs","tuv","wxyz"};
        string path;
        dfs(digits,map,0,path,res);
        return res;
    }
    void dfs(string& digits,vector<string>& map,int idx,string& path,vector<string>& res){
    if(idx==digits.size()){
        res.push_back(path);
        return;
    }
    string letters=map[digits[idx]-'0'];
    for(char ch:letters){
        path.push_back(ch);
        dfs(digits,map,idx+1,path,res);
        path.pop_back();        //撤销上次选择
    }
}

};

