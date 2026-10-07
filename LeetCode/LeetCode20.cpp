#include<vector>
#include<string>
#include<stack>
using namespace std;


class Solution {
public:
    bool isValid(string s) {
        int n=s.size();
        if(n%2==1) return false;
        for(int i=0;i<n;i++){
            if(s[i]=='(') s[i]='a';
            else if(s[i]==')') s[i]='b';
            else if(s[i]=='[') s[i]='c';
            else if(s[i]==']') s[i]='d';
            else if(s[i]=='{') s[i]='e';
            else if(s[i]=='}') s[i]='f';
    }
        stack<char> stk;
        for(int i=0;i<n;i++){
            if(s[i]=='a') stk.push('a');
            else if(s[i]=='b'){
                if(stk.empty()||stk.top()!='a') return false;
                else stk.pop();
            }
            else if(s[i]=='c') stk.push('c');
            else if(s[i]=='d'){
                if(stk.empty()||stk.top()!='c') return false;
                else stk.pop();
            }
            else if(s[i]=='e') stk.push('e');
            else if(s[i]=='f'){
                if(stk.empty()||stk.top()!='e') return false;
                else stk.pop();
            }
        }
        return stk.empty();
    }
};