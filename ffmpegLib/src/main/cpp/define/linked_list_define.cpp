//
// Created by Void on 2020/12/16.
//

#include "linked_list_define.h"

LinkedList::LinkedList() {
    length = 0;
    header.next = NULL;
}

int LinkedList::Size() {
    return length;
}

bool LinkedList::add(BaseNode *node) {
    return add(node, Size());
}

bool LinkedList::add(BaseNode *node, int index) {
    if (index < 0 || node == NULL) {
        fprintf(stderr, "Wrong argument.\n");
        return false;
    }
    BaseNode *cur = &header;

    int i = 0;
    while (i < index && cur->next != NULL) {
        cur = cur->next;
        i++;
    }
    node->next = cur->next;
    cur->next = node;
    length++;
    return true;
}

BaseNode *LinkedList::get(int index) {
    if (index < 0) {
        fprintf(stderr, "Wrong argument.\n");
        return NULL;
    }
    BaseNode *cur = &header;
    int i = 0;
    while (i < index && cur->next != NULL) {
        cur = cur->next;
        i++;
    }
    return cur->next;
}

void LinkedList::set(int index, BaseNode *node) {
    if (index < 0 || index >= Size()) {
        printf("非法操作");
        return;
    }
    BaseNode *cur = &header;
    int i = 0;
    while (i < index && cur->next != NULL) {
        cur = cur->next;
        i++;
    }
    BaseNode *r = cur->next->next;
    cur->next = node;
    node->next = r;
}

BaseNode *LinkedList::removeAt(int index) {
    if (index < 0 || index >= Size()) {
        fprintf(stderr, "非法移除，\n");
        return NULL;
    }
    BaseNode *cur = &header;
    int i = 0;
    while (i < index && cur->next != NULL) {
        cur = cur->next;
        i++;
    }
    BaseNode *r = cur->next;
    cur->next = cur->next->next;
    length--;
    return r;
}

bool LinkedList::remove(BaseNode *node) {
    if (node == NULL)
        return false;
    BaseNode *cur = (&header);
    while (cur->next != NULL) {
        if (cur->next == node) {
            cur->next = cur->next->next;
            length--;
            return true;
        }
        cur = cur->next;
    }
    return false;
}

void LinkedList::clear() {
    length = 0;
    header.next = NULL;
}

void LinkedList::release() {
    free(this);
}
