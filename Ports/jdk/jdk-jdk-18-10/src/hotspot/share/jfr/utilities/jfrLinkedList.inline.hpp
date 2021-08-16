/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRLINKEDLIST_INLINE_HPP
#define SHARE_JFR_UTILITIES_JFRLINKEDLIST_INLINE_HPP

#include "jfr/utilities/jfrLinkedList.hpp"

#include "runtime/atomic.hpp"

template <typename NodeType, typename AllocPolicy>
JfrLinkedList<NodeType, AllocPolicy>::JfrLinkedList() : _head(NULL) {}

template <typename NodeType, typename AllocPolicy>
bool JfrLinkedList<NodeType, AllocPolicy>::initialize() {
  return true;
}

template <typename NodeType, typename AllocPolicy>
inline NodeType* JfrLinkedList<NodeType, AllocPolicy>::head() const {
  return (NodeType*)Atomic::load_acquire(&_head);
}

template <typename NodeType, typename AllocPolicy>
inline bool JfrLinkedList<NodeType, AllocPolicy>::is_empty() const {
  return NULL == head();
}

template <typename NodeType, typename AllocPolicy>
inline bool JfrLinkedList<NodeType, AllocPolicy>::is_nonempty() const {
  return !is_empty();
}

template <typename NodeType, typename AllocPolicy>
inline void JfrLinkedList<NodeType, AllocPolicy>::add(NodeType* node) {
  assert(node != NULL, "invariant");
  NodePtr next;
  do {
    next = head();
    node->_next = next;
  } while (Atomic::cmpxchg(&_head, next, node) != next);
}

template <typename NodeType, typename AllocPolicy>
inline NodeType* JfrLinkedList<NodeType, AllocPolicy>::remove() {
  NodePtr node;
  NodePtr next;
  do {
    node = head();
    if (node == NULL) break;
    next = (NodePtr)node->_next;
  } while (Atomic::cmpxchg(&_head, node, next) != node);
  return node;
}

template <typename NodeType, typename AllocPolicy>
template <typename Callback>
void JfrLinkedList<NodeType, AllocPolicy>::iterate(Callback& cb) {
  NodePtr current = head();
  while (current != NULL) {
    NodePtr next = (NodePtr)current->_next;
    if (!cb.process(current)) {
      return;
    }
    current = next;
  }
}

template <typename NodeType, typename AllocPolicy>
NodeType* JfrLinkedList<NodeType, AllocPolicy>::excise(NodeType* prev, NodeType* node) {
  NodePtr next = (NodePtr)node->_next;
  if (prev == NULL) {
    prev = Atomic::cmpxchg(&_head, node, next);
    if (prev == node) {
      return NULL;
    }
  }
  assert(prev != NULL, "invariant");
  while (prev->_next != node) {
    prev = (NodePtr)prev->_next;
  }
  assert(prev->_next == node, "invariant");
  prev->_next = next;
  return prev;
}

template <typename NodeType, typename AllocPolicy>
bool JfrLinkedList<NodeType, AllocPolicy>::in_list(const NodeType* node) const {
  assert(node != NULL, "invariant");
  const NodeType* current = head();
  while (current != NULL) {
    if (current == node) {
      return true;
    }
    current = (NodeType*)current->_next;
  }
  return false;
}

#endif // SHARE_JFR_UTILITIES_JFRLINKEDLIST_INLINE_HPP
