#include<string>
#include<vector>
#include<iostream>
using namespace std;

class Solution {
public:
    string longestCommonPrefix(vector<string>& strs) {
        if(strs.empty()) return "";
        string s=strs[0];
        for(auto &str : strs){
            while(str.substr(0,s.size())!=s){
                if(s.empty()) return "";
                s.pop_back();
            }
        }
        return s;
        
    }
};