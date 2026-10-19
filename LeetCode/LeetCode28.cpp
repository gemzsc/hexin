#include<iostream>
#include<string>
using namespace std;

class Solution {
public:
    int strStr(string haystack, string needle) {
        int i=0,j=0;
        int m= haystack.size(),n= needle.size();
        if(n==0) return 0;
        while(i<m && j<n){
            if(haystack[i] == needle[j]){
                i++;
                j++;
            }
            else{
                i=i-j+1;
                j=0;
            }

        }
        if(j==n) return i-j;
        else return -1;
    }
};