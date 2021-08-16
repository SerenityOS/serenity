/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_FILTERQUEUE_HPP
#define SHARE_UTILITIES_FILTERQUEUE_HPP

#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"

// The FilterQueue is FIFO with the ability to skip over queued items.
// The skipping is controlled by using a filter when popping.
// It also supports lock free pushes, while popping (including contains())
// needs to be externally serialized.
template <class E>
class FilterQueue {
 private:
  class Node : public CHeapObj<mtInternal> {
   public:
    Node(const E& e): _next(NULL), _data(e) { }
    Node*    _next;
    E                   _data;
  };

  Node* _first;
  Node* load_first() {
    return Atomic::load_acquire(&_first);
  }

  static bool match_all(E d) { return true; }

 public:
  FilterQueue() : _first(NULL) { }

  bool is_empty() {
    return load_first() == NULL;
  }

  // Adds an item to the queue in a MT safe way, re-entrant.
  void push(E data);

  // Applies the match_func to the items in the queue until match_func returns
  // true and then returns true, or there is no more items and then returns
  // false. Items pushed after execution starts will not have match_func
  // applied. The method is not re-entrant and must be executed mutually
  // exclusive to other contains and pops calls.
  template <typename MATCH_FUNC>
  bool contains(MATCH_FUNC& match_func);

  // Same as peek(MATCH_FUNC& match_func) but matches everything, thus returning
  // the first inserted item.
  E peek() {
    return peek(match_all);
  }

  // Applies the match_func to each item in the queue and returns the first
  // inserted item for which match_func returns true. Returns false if there are
  // no matches or the queue is empty. Any pushed item before execution is
  // complete may or may not have match_func applied. The method is not
  // re-entrant and must be executed mutual exclusive to other contains and pops
  // calls.
  template <typename MATCH_FUNC>
  E pop(MATCH_FUNC& match_func);

  template <typename MATCH_FUNC>
  E peek(MATCH_FUNC& match_func);
};

#endif
