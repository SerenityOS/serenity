/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_LOCKFREESTACK_HPP
#define SHARE_UTILITIES_LOCKFREESTACK_HPP

#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// The LockFreeStack class template provides a lock-free LIFO. The objects
// in the sequence are intrusively linked via a member in the objects.  As
// a result, there is no allocation involved in adding objects to the stack
// or removing them from the stack.
//
// To be used in a LockFreeStack of objects of type T, an object of
// type T must have a list entry member of type T* volatile, with an
// non-member accessor function returning a pointer to that member.  A
// LockFreeStack is associated with the class of its elements and an
// entry member from that class.
//
// An object can be in multiple stacks at the same time, so long as
// each stack uses a different entry member. That is, the class of the
// object must have multiple LockFreeStack entry members, one for each
// stack in which the object may simultaneously be an element.
//
// LockFreeStacks support polymorphic elements.  Because the objects
// in a stack are externally managed, rather than being embedded
// values in the stack, the actual type of such objects may be more
// specific than the stack's element type.
//
// \tparam T is the class of the elements in the stack.
//
// \tparam next_ptr is a function pointer.  Applying this function to
// an object of type T must return a pointer to the list entry member
// of the object associated with the LockFreeStack type.
template<typename T, T* volatile* (*next_ptr)(T&)>
class LockFreeStack {
  T* volatile _top;

  void prepend_impl(T* first, T* last) {
    T* cur = top();
    T* old;
    do {
      old = cur;
      set_next(*last, cur);
      cur = Atomic::cmpxchg(&_top, cur, first);
    } while (old != cur);
  }

  NONCOPYABLE(LockFreeStack);

public:
  LockFreeStack() : _top(NULL) {}
  ~LockFreeStack() { assert(empty(), "stack not empty"); }

  // Atomically removes the top object from this stack and returns a
  // pointer to that object, or NULL if this stack is empty. Acts as a
  // full memory barrier. Subject to ABA behavior; callers must ensure
  // usage is safe.
  T* pop() {
    T* result = top();
    T* old;
    do {
      old = result;
      T* new_top = NULL;
      if (result != NULL) {
        new_top = next(*result);
      }
      // CAS even on empty pop, for consistent membar bahavior.
      result = Atomic::cmpxchg(&_top, result, new_top);
    } while (result != old);
    if (result != NULL) {
      set_next(*result, NULL);
    }
    return result;
  }

  // Atomically exchange the list of elements with NULL, returning the old
  // list of elements.  Acts as a full memory barrier.
  // postcondition: empty()
  T* pop_all() {
    return Atomic::xchg(&_top, (T*)NULL);
  }

  // Atomically adds value to the top of this stack.  Acts as a full
  // memory barrier.
  void push(T& value) {
    assert(next(value) == NULL, "precondition");
    prepend_impl(&value, &value);
  }

  // Atomically adds the list of objects (designated by first and
  // last) before the objects already in this stack, in the same order
  // as in the list. Acts as a full memory barrier.
  // precondition: next(last) == NULL.
  // postcondition: top() == &first, next(last) == old top().
  void prepend(T& first, T& last) {
    assert(next(last) == NULL, "precondition");
#ifdef ASSERT
    for (T* p = &first; p != &last; p = next(*p)) {
      assert(p != NULL, "invalid prepend list");
    }
#endif
    prepend_impl(&first, &last);
  }

  // Atomically adds the list of objects headed by first before the
  // objects already in this stack, in the same order as in the list.
  // Acts as a full memory barrier.
  // postcondition: top() == &first.
  void prepend(T& first) {
    T* last = &first;
    while (true) {
      T* step_to = next(*last);
      if (step_to == NULL) break;
      last = step_to;
    }
    prepend_impl(&first, last);
  }

  // Return true if the stack is empty.
  bool empty() const { return top() == NULL; }

  // Return the most recently pushed element, or NULL if the stack is empty.
  // The returned element is not removed from the stack.
  T* top() const { return Atomic::load(&_top); }

  // Return the number of objects in the stack.  There must be no concurrent
  // pops while the length is being determined.
  size_t length() const {
    size_t result = 0;
    for (const T* current = top(); current != NULL; current = next(*current)) {
      ++result;
    }
    return result;
  }

  // Return the entry following value in the list used by the
  // specialized LockFreeStack class.
  static T* next(const T& value) {
    return Atomic::load(next_ptr(const_cast<T&>(value)));
  }

  // Set the entry following value to new_next in the list used by the
  // specialized LockFreeStack class.  Not thread-safe; in particular,
  // if value is in an instance of this specialization of LockFreeStack,
  // there must be no concurrent push or pop operations on that stack.
  static void set_next(T& value, T* new_next) {
    Atomic::store(next_ptr(value), new_next);
  }
};

#endif // SHARE_UTILITIES_LOCKFREESTACK_HPP
