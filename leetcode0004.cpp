#include <vector>
using namespace std;

class Solution {
public:
    double findMedianSortedArrays(vector<int>& nums1, vector<int>& nums2) {
        int m = nums1.size();
        int n = nums2.size();
        int total = m + n;
        // 奇数
        if(total % 2 == 1){
            return getKth(nums1, 0, nums2, 0, (total + 1)/2);
        }else{
            // 偶数，取中间两个平均
            double a = getKth(nums1, 0, nums2, 0, total/2);
            double b = getKth(nums1, 0, nums2, 0, total/2 + 1);
            return (a + b)/2.0;
        }
    }

    // i:nums1起始下标  j:nums2起始下标，找第k小
    int getKth(vector<int>& nums1, int i, vector<int>& nums2, int j, int k){
        // 情况1：nums1耗尽，直接返回nums2往后第k‑1个
        if(i >= nums1.size()){
            return nums2[j + k - 1];
        }
        // 情况2：nums2耗尽
        if(j >= nums2.size()){
            return nums1[i + k - 1];
        }
        // k=1，直接取两个数组开头较小值
        if(k == 1){
            return min(nums1[i], nums2[j]);
        }
        int mid1 = i + k/2 - 1;
        int mid2 = j + k/2 - 1;
        int val1 = (mid1 < nums1.size()) ? nums1[mid1] : 1e9;
        int val2 = (mid2 < nums2.size()) ? nums2[mid2] : 1e9;

        if(val1 < val2){
            // nums1前k/2个不可能是第k小，舍弃
            return getKth(nums1, mid1 + 1, nums2, j, k - k/2);
        }else{
            // 舍弃nums2前k/2
            return getKth(nums1, i, nums2, mid2 + 1, k - k/2);
        }
    }
};
