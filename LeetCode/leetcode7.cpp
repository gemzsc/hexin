class Solution {
public:
    int reverse(int x) {
        int result = 0;
        while (x != 0) {
            int y = x % 10;
            x /= 10;
            // 累加前判断是否溢出
            //写成四个if语句，减少多余判断
            if (result > INT_MAX / 10 )  return 0;
            if(result == INT_MAX / 10 && y > 7) return 0;
            if (result < INT_MIN / 10 ) return 0;
            if(result == INT_MIN / 10 && y < -8) return 0;
            result = result * 10 + y;
        }
        return result;
    }
};
