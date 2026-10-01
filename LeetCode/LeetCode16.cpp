#include<vector>
#include<algorithm>
#include<iostream>
using namespace std;

class Solution {
public:
    int threeSumClosest(vector<int>& nums, int target) {
        int n=nums.size();
        sort(nums.begin(), nums.end());
        int closest_sum = 0;
        int min_diff = INT_MAX;
        for (int i=0;i<n;i++)
        {
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1, r=n-1;
            while(l<r){
                int sum=nums[i]+nums[l]+nums[r];
                if (sum == target) return target;
                
                int diff = abs(sum - target);
                if(diff < min_diff){
                    min_diff = diff;
                    closest_sum = sum;
                }
                
                if(sum < target){
                    l++;
                    while(l<r && nums[l]==nums[l-1]) l++;
                } else {
                    r--;
                    while(l<r && nums[r]==nums[r+1]) r--;
                }
            }
        }
        return closest_sum;
    }
};


