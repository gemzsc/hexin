#include<vector>
#include<string>
#include<stack>
#include<iostream>
using namespace std;

class Solution {
public:
    vector<string> generateParenthesis(int n) {
        vector<string> res;
        string path;
        dfs(res,path,0,0,n);
        return res;
    }
    void dfs(vector<string>& res,string& path,int left,int right,int n){
        if(left==n&&right==n){
            res.push_back(path);
            return;
        }
        if(left<n){
            path.push_back('(');
            dfs(res,path,left+1,right,n);
            path.pop_back();
        }
        if(right<left){
            path.push_back(')');
            dfs(res,path,left,right+1,n);
            path.pop_back();
        }
    }
};

int main(){
    Solution s;
    vector<string> res=s.generateParenthesis(3);
    for(auto i:res){
        cout<<i<<endl;
    }
    return 0;
}