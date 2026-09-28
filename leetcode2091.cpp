#include<vector>
#include<iostream>
using namespace std;

class Solution {
public:
    int minimumDeletions(vector<int>& nums) {
        int n = nums.size();
        int max_val= nums[0];
        int min_val = nums[0];
        int min_index = 0;
        int max_index = 0;
        int count=0;
        for(int i = 1; i < n; i++){
            if(nums[i] > max_val){
                max_val= nums[i];
                max_index = i;
            }
        }
        for(int i = 1; i < n; i++){
            if(nums[i]<min_val){
                min_val=nums[i];
                min_index=i;
            }
        }
        int left;
        left=min(min_index,max_index);
        int right;
        right=max(min_index,max_index);

        int op1=right+1;
        int op2=n-left;
        int op3=n-right+left+1;
        count=min(op1,min(op2,op3));
        return count;
    }
};