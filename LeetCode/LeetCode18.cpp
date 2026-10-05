#include<vector>
#include<algorithm>
using namespace std;

class Solution {
public:
    vector<vector<int>> fourSum(vector<int>& nums, int target) {
        int n=nums.size();
        vector<vector<int>> ans;
        if(n<4) return ans;
        sort(nums.begin(),nums.end());
        for(int i=0;i<n-3;i++){
            if(i>0&&nums[i]==nums[i-1]) continue;
            if(static_cast<long long>(nums[i])+nums[i+1]+nums[i+2]+nums[i+3]>target) break;
            if(static_cast<long long>(nums[i])+nums[n-1]+nums[n-2]+nums[n-3]<target) continue;
            for(int j=i+1;j<n-2;j++){
                if(j>i+1&&nums[j]==nums[j-1]) continue;
                if(static_cast<long long>(nums[i])+nums[j]+nums[j+1]+nums[j+2]>target) break;
                if(static_cast<long long>(nums[i])+nums[j]+nums[n-1]+nums[n-2]<target) continue;
                int left=j+1;
                int right=n-1;
                while(left<right){
                    long long sum=static_cast<long long>(nums[i])+nums[j]+nums[left]+nums[right];
                    if(sum==target){
                        ans.push_back({nums[i],nums[j],nums[left],nums[right]});
                        while(left<right&&nums[left]==nums[left+1]) left++;
                        while(left<right&&nums[right]==nums[right-1]) right--;
                        left++;
                        right--;
                        while(left<right&&nums[left]==nums[left-1]) left++;
                        while(left<right&&nums[right]==nums[right+1]) right--;
                    }
                    else if(sum<target) left++;
                    else right--;
                }

            }
        }
        return ans;
    }
};