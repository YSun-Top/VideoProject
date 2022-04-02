//
// Created by Void on 2020/12/16.
//

#ifndef TESTEXAMPLE_LINKED_LIST_DEFINE_H
#define TESTEXAMPLE_LINKED_LIST_DEFINE_H

#include<stdio.h>
#include<stdlib.h>
#include<memory.h>
struct BaseNode {
  struct BaseNode *next;
};

class LinkedList {
 private:
  BaseNode header{};
  int length;
 public:
  LinkedList();

  int Size();

  bool add(BaseNode *node);

  bool add(BaseNode *node, int index);

  BaseNode *get(int index);

  void set(int index, BaseNode *node);

  bool remove(BaseNode *node);

  BaseNode *removeAt(int index);

  void clear();

//    inline void release();
  void release();
};
#endif //TESTEXAMPLE_LINKED_LIST_DEFINE_H