#include<cstring>
#include<iostream>
using namespace std;

class Solution {
public:
    int lengthOfLongestSubstring(string s) {
        int len = s.length();
        int maxLen = 0;
        int start = 0;
        int end = 0;
        int hash[256] = {0};
        while(end < len){
            if(hash[s[end]] == 0){
                hash[s[end]] = 1;
                end++;
                maxLen = max(maxLen, end - start);
            }
            else{
                hash[s[start]] = 0;
                start++;
            }
     }
     return maxLen;
    }
};
