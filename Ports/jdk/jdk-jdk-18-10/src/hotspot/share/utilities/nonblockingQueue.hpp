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

#ifndef SHARE_UTILITIES_NONBLOCKINGQUEUE_HPP
#define SHARE_UTILITIES_NONBLOCKINGQUEUE_HPP

#include "memory/padded.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/pair.hpp"

// The NonblockingQueue template provides a non-blocking FIFO.
// It has inner padding of one cache line between its two internal pointers.
//
// The queue is internally represented by a linked list of elements, with
// the link to the next element provided by a member of each element.
// Access to this member is provided by the next_ptr function.
//
// The queue has a special pseudo-element that marks the end of the list.
// Each queue has its own unique special element.  A pointer to this element
// can be recognized using the is_end() function.  Such a pointer must never
// be dereferenced.  This end marker is the value of the next member of the
// last element in the queue, and possibly other elements while modifying
// the queue.
//
// A queue may temporarily appear to be empty even though elements have been
// added and not removed.  For example, after running the following program,
// the value of r may be NULL.
//
// thread1: q.push(a); r = q.pop();
// thread2: q.push(b);
//
// This can occur if the push of b started before the push of a, but didn't
// complete until after the pop.
//
// \tparam T is the class of the elements in the queue.
//
// \tparam next_ptr is a function pointer.  Applying this function to
// an object of type T must return a pointer to the list entry member
// of the object associated with the NonblockingQueue type.
template<typename T, T* volatile* (*next_ptr)(T&)>
class NonblockingQueue {
  T* volatile _head;
  // Padding of one cache line to avoid false sharing.
  DEFINE_PAD_MINUS_SIZE(1, DEFAULT_CACHE_LINE_SIZE, sizeof(T*));
  T* volatile _tail;

  NONCOPYABLE(NonblockingQueue);

  // Return the entry following node in the list used by the
  // specialized NonblockingQueue class.
  static inline T* next(const T& node);

  // Set the entry following node to new_next in the list used by the
  // specialized NonblockingQueue class. Not thread-safe, as it cannot
  // concurrently run with push or try_pop operations that modify this
  // node.
  static inline void set_next(T& node, T* new_next);

  // A unique pseudo-object pointer associated with this specific queue.
  // The resulting pointer must not be dereferenced.
  inline T* end_marker() const;

public:
  inline NonblockingQueue();
  inline ~NonblockingQueue() NOT_DEBUG(= default);

  // Return true if the queue is empty.
  // Not thread-safe.  There must be no concurrent modification while the
  // queue is being tested.
  inline bool empty() const;

  // Return the number of objects in the queue.
  // Not thread-safe. There must be no concurrent modification while the
  // length is being determined.
  inline size_t length() const;

  // Thread-safe add the object to the end of the queue.
  inline void push(T& node) { append(node, node); }

  // Thread-safe add the objects from first to last to the end of the queue.
  inline void append(T& first, T& last);

  // Thread-safe attempt to remove and return the first object in the queue.
  // Returns true if successful.  If successful then *node_ptr is the former
  // first object, or NULL if the queue was empty.  If unsuccessful, because
  // of contention with a concurrent modification, then returns false with
  // the value of *node_ptr unspecified.  Subject to ABA behavior; callers
  // must ensure usage is safe.
  inline bool try_pop(T** node_ptr);

  // Thread-safe remove and return the first object in the queue, or NULL if
  // the queue was empty.  This just iterates on try_pop() until it
  // succeeds, returning the (possibly NULL) element obtained from that.
  // Subject to ABA behavior; callers must ensure usage is safe.
  inline T* pop();

  // Take all the objects from the queue, leaving the queue empty.
  // Not thread-safe.  There must be no concurrent operations.
  // Returns a pair of <head, tail> pointers to the current queue.
  inline Pair<T*, T*> take_all();

  // Iteration support is provided by first() and is_end().  The queue must
  // not be modified while iterating over its elements.

  // Return the first object in the queue, or an end marker (a pointer p for
  // which is_end(p) is true) if the queue is empty.
  inline T* first() const;

  // Test whether entry is an end marker for this queue.
  inline bool is_end(const T* entry) const;
};

#endif // SHARE_UTILITIES_NONBLOCKINGQUEUE_HPP
