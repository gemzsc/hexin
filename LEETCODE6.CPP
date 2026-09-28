#include<cstring>
#include<iostream>
using namespace std;

//Z字形变换，将一个字符串按照Z字形变换，返回变换后的字符串
class Solution {
public:
    string convert(string s, int numRows) {
        int n=s.size();
        if(numRows==1||n<numRows) return s;
        string res;
        int t=2*numRows-2;
        for(int i=0;i<numRows;i++){
            for(int j=0;j+i<n;j+=t){
                res+=s[j+i];
                if(i!=0&&i!=numRows-1&&j+t-i<n)
                    res+=s[j+t-i];
        }
    }
    return res;
}
};