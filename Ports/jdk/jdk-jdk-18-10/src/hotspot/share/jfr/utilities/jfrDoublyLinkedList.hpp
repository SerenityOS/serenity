/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_JFR_UTILITIES_JFRDOUBLYLINKEDLIST_HPP
#define SHARE_JFR_UTILITIES_JFRDOUBLYLINKEDLIST_HPP

#include "memory/allocation.hpp"

template <typename T>
class JfrDoublyLinkedList {
 private:
  T* _head;
  T* _tail;
  size_t _count;

  T** list_head() { return &_head; }
  T** list_tail() { return &_tail; }

 public:
  typedef T Node;
  JfrDoublyLinkedList() : _head(NULL), _tail(NULL), _count(0) {}
  T* head() const { return _head; }
  T* tail() const { return _tail; }
  size_t count() const { return _count; }
  T* clear(bool return_tail = false);
  T* remove(T* const node);
  void prepend(T* const node);
  void append(T* const node);
  void append_list(T* const head_node, T* const tail_node, size_t count);
  bool in_list(const T* const target_node) const;
  bool locate(const T* start_node, const T* const target_node) const;
};

template <typename T>
inline void JfrDoublyLinkedList<T>::prepend(T* const node) {
  assert(node != NULL, "invariant");
  node->set_prev(NULL);
  assert(!in_list(node), "already in list error");
  T** lh = list_head();
  if (*lh != NULL) {
    (*lh)->set_prev(node);
    node->set_next(*lh);
  } else {
    T** lt = list_tail();
    assert(*lt == NULL, "invariant");
    *lt = node;
    node->set_next(NULL);
    assert(tail() == node, "invariant");
    assert(node->next() == NULL, "invariant");
  }
  *lh = node;
  ++_count;
  assert(head() == node, "head error");
  assert(in_list(node), "not in list error");
  assert(node->prev() == NULL, "invariant");
}

template <typename T>
void JfrDoublyLinkedList<T>::append(T* const node) {
  assert(node != NULL, "invariant");
  node->set_next(NULL);
  assert(!in_list(node), "already in list error");
  T** lt = list_tail();
  if (*lt != NULL) {
    // already an existing tail
    node->set_prev(*lt);
    (*lt)->set_next(node);
  } else {
    // if no tail, also update head
    assert(*lt == NULL, "invariant");
    T** lh = list_head();
    assert(*lh == NULL, "invariant");
    node->set_prev(NULL);
    *lh = node;
    assert(head() == node, "invariant");
  }
  *lt = node;
  ++_count;
  assert(tail() == node, "invariant");
  assert(in_list(node), "not in list error");
  assert(node->next() == NULL, "invariant");
}

template <typename T>
T* JfrDoublyLinkedList<T>::remove(T* const node) {
  assert(node != NULL, "invariant");
  assert(in_list(node), "invariant");
  T* const prev = (T*)node->prev();
  T* const next = (T*)node->next();
  if (prev == NULL) {
    assert(head() == node, "head error");
    if (next != NULL) {
      next->set_prev(NULL);
    } else {
      assert(next == NULL, "invariant");
      assert(tail() == node, "tail error");
      T** lt = list_tail();
      *lt = NULL;
      assert(tail() == NULL, "invariant");
    }
    T** lh = list_head();
    *lh = next;
    assert(head() == next, "invariant");
  } else {
    assert(prev != NULL, "invariant");
    if (next == NULL) {
      assert(tail() == node, "tail error");
      T** lt = list_tail();
      *lt = prev;
      assert(tail() == prev, "invariant");
    } else {
       next->set_prev(prev);
    }
    prev->set_next(next);
  }
  --_count;
  assert(!in_list(node), "still in list error");
  return node;
}

template <typename T>
T* JfrDoublyLinkedList<T>::clear(bool return_tail /* false */) {
  T* const node = return_tail ? tail() : head();
  T** l = list_head();
  *l = NULL;
  l = list_tail();
  *l = NULL;
  _count = 0;
  assert(head() == NULL, "invariant");
  assert(tail() == NULL, "invariant");
  return node;
}

template <typename T>
bool JfrDoublyLinkedList<T>::locate(const T* node, const T* const target) const {
  assert(target != NULL, "invariant");
  while (node != NULL) {
    if (node == target) {
      return true;
    }
    node = (T*)node->next();
  }
  return false;
}

template <typename T>
bool JfrDoublyLinkedList<T>::in_list(const T* const target) const {
  assert(target != NULL, "invariant");
  return locate(head(), target);
}

template <typename T>
inline void validate_count_param(T* node, size_t count_param) {
  assert(node != NULL, "invariant");
  size_t count = 0;
  while (node) {
    ++count;
    node = (T*)node->next();
  }
  assert(count_param == count, "invariant");
}

template <typename T>
void JfrDoublyLinkedList<T>::append_list(T* const head_node, T* const tail_node, size_t count) {
  assert(head_node != NULL, "invariant");
  assert(!in_list(head_node), "already in list error");
  assert(tail_node != NULL, "invariant");
  assert(!in_list(tail_node), "already in list error");
  assert(tail_node->next() == NULL, "invariant");
  // ensure passed in list nodes are connected
  assert(locate(head_node, tail_node), "invariant");
  T** lt = list_tail();
  if (*lt != NULL) {
    head_node->set_prev(*lt);
    (*lt)->set_next(head_node);
  } else {
    // no head
    assert(*lt == NULL, "invariant");
    T** lh = list_head();
    assert(*lh == NULL, "invariant");
    head_node->set_prev(NULL);
    *lh = head_node;
    assert(head() == head_node, "invariant");
  }
  *lt = tail_node;
  const T* node = head_node;
  debug_only(validate_count_param(node, count);)
    _count += count;
  assert(tail() == tail_node, "invariant");
  assert(in_list(tail_node), "not in list error");
  assert(in_list(head_node), "not in list error");
}

#endif // SHARE_JFR_UTILITIES_JFRDOUBLYLINKEDLIST_HPP
