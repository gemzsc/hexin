#include<string>
#include<stack>
using namespace std;

class Solution {
public:
    bool isValid(string s) {
        int n=s.size();
        if(n%2==1) return false; 
        stack<char> st;
        for(char c:s){
            if(c=='('||c=='['||c=='{'){
                st.push(c);
            }
            else{
                if(st.empty()) return false;
                char top=st.top();
                if((c==')'&&top!='(')||c==']'&&top!='['||c=='}'&&top!='{'){
                    return false;
            }
                st.pop();
        }
    }
    return st.empty();}
};