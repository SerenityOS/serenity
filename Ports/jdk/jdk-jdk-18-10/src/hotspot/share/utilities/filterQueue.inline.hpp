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

#ifndef SHARE_UTILITIES_FILTERQUEUE_INLINE_HPP
#define SHARE_UTILITIES_FILTERQUEUE_INLINE_HPP

#include "utilities/filterQueue.hpp"

#include "utilities/spinYield.hpp"

template <class E>
void FilterQueue<E>::push(E data) {
  Node* head;
  Node* insnode = new Node(data);
  SpinYield yield(SpinYield::default_spin_limit * 10); // Very unlikely with multiple failed CAS.
  while (true){
    head = load_first();
    insnode->_next = head;
    if (Atomic::cmpxchg(&_first, head, insnode) == head) {
      break;
    }
    yield.wait();
  }
}

// MT-Unsafe, external serialization needed.
template <class E>
template <typename MATCH_FUNC>
bool FilterQueue<E>::contains(MATCH_FUNC& match_func) {
  Node* cur = load_first();
  if (cur == NULL) {
    return false;
  }
  do {
    if (match_func(cur->_data)) {
      return true;
    }
    cur = cur->_next;
  } while (cur != NULL);
  return false;
}

// MT-Unsafe, external serialization needed.
template <class E>
template <typename MATCH_FUNC>
E FilterQueue<E>::pop(MATCH_FUNC& match_func) {
  Node*  first       = load_first();
  Node*  cur         = first;
  Node*  prev        = NULL;
  Node*  match       = NULL;
  Node*  match_prev  = NULL;

  if (cur == NULL) {
    return (E)NULL;
  }
  SpinYield yield(SpinYield::default_spin_limit * 10); // Very unlikely with multiple failed CAS.
  do {
    do {
      if (match_func(cur->_data)) {
        match = cur;
        match_prev = prev;
      }
      prev = cur;
      cur = cur->_next;
    } while (cur != NULL);

    if (match == NULL) {
      return (E)NULL;
    }

    if (match_prev == NULL) {
      // Working on first
      if (Atomic::cmpxchg(&_first, match, match->_next) == match) {
        E ret = match->_data;
        delete match;
        return ret;
      }
      yield.wait();
      // Failed, we need to restart to know the Node prior to the match.
      first       = load_first();
      cur         = first;
      prev        = NULL;
      match       = NULL;
      match_prev  = NULL;
    } else {
      match_prev->_next = match->_next;
      E ret = match->_data;
      delete match;
      return ret;
    }
  } while (true);
}

// MT-Unsafe, external serialization needed.
template <class E>
template <typename MATCH_FUNC>
E FilterQueue<E>::peek(MATCH_FUNC& match_func) {
  Node*  first       = load_first();
  Node*  cur         = first;
  Node*  match       = NULL;

  if (cur == NULL) {
    return (E)NULL;
  }
  do {
    if (match_func(cur->_data)) {
      match = cur;
    }
    cur = cur->_next;
  } while (cur != NULL);

  if (match == NULL) {
    return (E)NULL;
  }

  return (E)match->_data;
}

#endif // SHARE_UTILITIES_FILTERQUEUE_INLINE_HPP
