#include<vector>
#include<queue>
#include<iostream>
using namespace std;


 struct ListNode {
     int val;
     ListNode *next;
     ListNode() : val(0), next(nullptr) {}
     ListNode(int x) : val(x), next(nullptr) {}
     ListNode(int x, ListNode *next) : val(x), next(next) {}
};


class Solution {
public:
    ListNode* mergeKLists(vector<ListNode*>& lists) {
        if(lists.size()==0) return nullptr;
        ListNode* dummy= new ListNode(-1);
        ListNode* cur=dummy;
        priority_queue<pair<int,ListNode*>,vector<pair<int,ListNode*>>,greater<pair<int,ListNode*>>> pq;
        for(ListNode* head:lists){ //将每个链表的头节点加入优先队列
            if(head!=nullptr){
                pq.push(make_pair(head->val,head));
            }
        }
        while(!pq.empty()){
            pair<int,ListNode*> temp=pq.top();
            pq.pop();
            cur->next=temp.second;
            if(temp.second->next!=nullptr){
                pq.push(make_pair(temp.second->next->val,temp.second->next));
            }
            cur=cur->next;
        }
        return dummy->next;
    }
};