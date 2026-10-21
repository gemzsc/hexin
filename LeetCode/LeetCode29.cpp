#include<iostream>
using namespace std;

class Solution {
public:
    int divide(int dividend, int divisor) {
        if(dividend ==INT_MIN && divisor == -1) return INT_MAX;

        long long a = dividend < 0 ? -static_cast<long long>(dividend) : dividend;
        long long b = divisor < 0 ? -static_cast<long long>(divisor) : divisor;
        
        long long ans=0;

        while(a>=b){
            long value = b;
            long multiple = 1;
            
            while(value<=a-value){
                value+=value;
                multiple+=multiple;
            }
            a -= value;
            ans+=multiple;
        }
        bool sign = (dividend<0)!=(divisor<0);
        return static_cast<int>(sign ? -ans : ans);
    }
};