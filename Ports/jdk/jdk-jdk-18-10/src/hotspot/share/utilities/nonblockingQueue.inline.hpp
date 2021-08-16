/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_NONBLOCKINGQUEUE_INLINE_HPP
#define SHARE_UTILITIES_NONBLOCKINGQUEUE_INLINE_HPP

#include "utilities/nonblockingQueue.hpp"

#include "runtime/atomic.hpp"

template<typename T, T* volatile* (*next_ptr)(T&)>
T* NonblockingQueue<T, next_ptr>::next(const T& node) {
  return Atomic::load(next_ptr(const_cast<T&>(node)));
}

template<typename T, T* volatile* (*next_ptr)(T&)>
void NonblockingQueue<T, next_ptr>::set_next(T& node, T* new_next) {
  Atomic::store(next_ptr(node), new_next);
}

template<typename T, T* volatile* (*next_ptr)(T&)>
NonblockingQueue<T, next_ptr>::NonblockingQueue() : _head(NULL), _tail(NULL) {}

#ifdef ASSERT
template<typename T, T* volatile* (*next_ptr)(T&)>
NonblockingQueue<T, next_ptr>::~NonblockingQueue() {
  assert(_head == NULL, "precondition");
  assert(_tail == NULL, "precondition");
}
#endif

// The end_marker must be uniquely associated with the specific queue, in
// case queue elements can make their way through multiple queues.  A
// pointer to the queue itself (after casting) satisfies that requirement.
template<typename T, T* volatile* (*next_ptr)(T&)>
T* NonblockingQueue<T, next_ptr>::end_marker() const {
  return const_cast<T*>(reinterpret_cast<const T*>(this));
}

template<typename T, T* volatile* (*next_ptr)(T&)>
T* NonblockingQueue<T, next_ptr>::first() const {
  T* head = Atomic::load(&_head);
  return head == NULL ? end_marker() : head;
}

template<typename T, T* volatile* (*next_ptr)(T&)>
bool NonblockingQueue<T, next_ptr>::is_end(const T* entry) const {
  return entry == end_marker();
}

template<typename T, T* volatile* (*next_ptr)(T&)>
bool NonblockingQueue<T, next_ptr>::empty() const {
  return Atomic::load(&_head) == NULL;
}

template<typename T, T* volatile* (*next_ptr)(T&)>
size_t NonblockingQueue<T, next_ptr>::length() const {
  size_t result = 0;
  for (T* cur = first(); !is_end(cur); cur = next(*cur)) {
    ++result;
  }
  return result;
}

// An append operation atomically exchanges the new tail with the queue tail.
// It then sets the "next" value of the old tail to the head of the list being
// appended. If the old tail is NULL then the queue was empty, then the head
// of the list being appended is instead stored in the queue head.
//
// This means there is a period between the exchange and the old tail update
// where the queue sequence is split into two parts, the list from the queue
// head to the old tail, and the list being appended.  If there are concurrent
// push/append operations, each may introduce another such segment.  But they
// all eventually get resolved by their respective updates of their old tail's
// "next" value.  This also means that try_pop operation must handle an object
// with a NULL "next" value specially.
//
// A push operation is just a degenerate append, where the object being pushed
// is both the head and the tail of the list being appended.
template<typename T, T* volatile* (*next_ptr)(T&)>
void NonblockingQueue<T, next_ptr>::append(T& first, T& last) {
  assert(next(last) == NULL, "precondition");
  set_next(last, end_marker());
  T* old_tail = Atomic::xchg(&_tail, &last);
  bool is_old_tail_null = (old_tail == NULL);
  if (is_old_tail_null ||
      // Try to install first as old_tail's next.
      !is_end(Atomic::cmpxchg(next_ptr(*old_tail), end_marker(), &first))) {
    // Install first as the new head if either
    // (1) the list was empty, or
    // (2) a concurrent try_pop claimed old_tail, so it is no longer in the list.
    // Note that multiple concurrent push/append operations cannot modify
    // _head simultaneously, because the Atomic::xchg() above orders these
    // push/append operations so they perform Atomic::cmpxchg() on different
    // old_tail. Thus, the cmpxchg can only fail because of a concurrent try_pop.
    DEBUG_ONLY(T* old_head = Atomic::load(&_head);)
    // If old_tail is NULL, old_head could be NULL, or an unseen object
    // that is being popped.  Otherwise, old_head must be either NULL
    // or the same as old_tail.
    assert(is_old_tail_null ||
           old_head == NULL || old_head == old_tail, "invariant");
    Atomic::store(&_head, &first);
  }
}

template<typename T, T* volatile* (*next_ptr)(T&)>
bool NonblockingQueue<T, next_ptr>::try_pop(T** node_ptr) {
  // We only need memory_order_consume. Upgrade it to "load_acquire"
  // as the memory_order_consume API is not ready for use yet.
  T* result = Atomic::load_acquire(&_head);
  if (result == NULL) {
    *node_ptr = NULL;
    return true;                // Queue is empty.
  }

  T* next_node = Atomic::load_acquire(next_ptr(*result));
  if (next_node == NULL) {
    // A concurrent try_pop already claimed what was the last entry.  That
    // operation may not have cleared queue head yet, but we should still
    // treat the queue as empty until a push/append operation changes head
    // to an entry with a non-NULL next value.
    *node_ptr = NULL;
    return true;

  } else if (!is_end(next_node)) {
    // The next_node is not at the end of the queue's list.  Use the "usual"
    // lock-free pop from the head of a singly linked list to try to take it.
    if (result == Atomic::cmpxchg(&_head, result, next_node)) {
      // Former head successfully taken.
      set_next(*result, NULL);
      *node_ptr = result;
      return true;
    } else {
      // Lost race to take result from the head of the list.
      return false;
    }

  } else if (is_end(Atomic::cmpxchg(next_ptr(*result), end_marker(), (T*)NULL))) {
    // Result was the last entry and we've claimed it by setting its next
    // value to NULL.  However, this leaves the queue in disarray.  Fix up
    // the queue, possibly in conjunction with other concurrent operations.
    // Any further try_pops will consider the queue empty until a
    // push/append completes by installing a new head.

    // Attempt to change the queue tail from result to NULL.  Failure of the
    // cmpxchg indicates that a concurrent push/append updated the tail first.
    // That operation will eventually recognize the old tail (our result) is
    // no longer in the list and update head from the list being appended.
    Atomic::cmpxchg(&_tail, result, (T*)NULL);

    // Attempt to change the queue head from result to NULL.  Failure of the
    // cmpxchg indicates a concurrent push/append updated the head first.
    Atomic::cmpxchg(&_head, result, (T*)NULL);

    // The queue has been restored to order, and we can return the result.
    *node_ptr = result;
    return true;

  } else {
    // Result was the last entry in the list, but either a concurrent pop
    // claimed it first or a concurrent push/append extended the list from
    // it.  Either way, we lost the race.
    return false;
  }
}

template<typename T, T* volatile* (*next_ptr)(T&)>
T* NonblockingQueue<T, next_ptr>::pop() {
  T* result = NULL;
  // Typically try_pop() will succeed without retrying many times, thus we
  // omit SpinPause in the loop body.  SpinPause or yield may be worthwhile
  // in rare, highly contended cases, and client code could implement such
  // with try_pop().
  while (!try_pop(&result)) {}
  return result;
}

template<typename T, T* volatile* (*next_ptr)(T&)>
Pair<T*, T*> NonblockingQueue<T, next_ptr>::take_all() {
  T* tail = Atomic::load(&_tail);
  if (tail != NULL) set_next(*tail, NULL); // Clear end marker.
  Pair<T*, T*> result(Atomic::load(&_head), tail);
  Atomic::store(&_head, (T*)NULL);
  Atomic::store(&_tail, (T*)NULL);
  return result;
}

#endif // SHARE_UTILITIES_NONBLOCKINGQUEUE_INLINE_HPP
