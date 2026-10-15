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
    ListNode* reverseKGroup(ListNode* head, int k) {
        if(head==nullptr) return nullptr;
        if(k==1){
            return head;
        }
        else if(k==2){
            if(head->next==nullptr){
                return head;
            }
            else{
                ListNode *p=head->next;
                head->next = p->next;
                p->next = head;
                head->next =reverseKGroup(head->next,k);
                return p;
            }
        }
        else{
            if(head->next==nullptr){
                return head;
            }
            int count=1;
            ListNode *s=head,*p=head,*q=p->next,*r=q->next;
            while(count<k && s->next!=nullptr){
                s=s->next;
                count++;
            }
            if(count<k){
                return p;
            }
            s=s->next;
            while(r!=s){
                q->next=p;
                p=q;
                q=r;
                r=r->next;
            }
            q->next=p;
            head->next = reverseKGroup(s,k);
            return q; 
        }
        
    }
};