#include<cstring>
#include<iostream>
using namespace std;

//寻找最长回文串
class Solution {
public:
    string longestPalindrome(string s) {
        int len=s.length();
        if(len==0) return "";
        int start=0,maxlen=1;
        for(int i=0;i<len;i++){
            int l=i-1,r=i+1;
            while(l>=0&&r<len&&s[l]==s[r]){
                if(r-l+1>maxlen){
                    start=l;
                    maxlen=r-l+1;
                }
                l--;
                r++;
            }
        }
        for(int i=0;i<len;i++){
            int l=i,r=i+1;
            while(l>=0&&r<len&&s[l]==s[r]){
                if(r-l+1>maxlen){
                    start=l;
                    maxlen=r-l+1;
                }
                l--;
                r++;
            }
        }
        return s.substr(start,maxlen);
    }
};
