/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_TASKQUEUE_INLINE_HPP
#define SHARE_GC_SHARED_TASKQUEUE_INLINE_HPP

#include "gc/shared/taskqueue.hpp"

#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/debug.hpp"
#include "utilities/stack.inline.hpp"

template <class T, MEMFLAGS F>
inline GenericTaskQueueSet<T, F>::GenericTaskQueueSet(uint n) : _n(n) {
  typedef T* GenericTaskQueuePtr;
  _queues = NEW_C_HEAP_ARRAY(GenericTaskQueuePtr, n, F);
  for (uint i = 0; i < n; i++) {
    _queues[i] = NULL;
  }
}

template <class T, MEMFLAGS F>
inline GenericTaskQueueSet<T, F>::~GenericTaskQueueSet() {
  FREE_C_HEAP_ARRAY(T*, _queues);
}

template<class E, MEMFLAGS F, unsigned int N>
inline void GenericTaskQueue<E, F, N>::initialize() {
  _elems = ArrayAllocator<E>::allocate(N, F);
}

template<class E, MEMFLAGS F, unsigned int N>
inline GenericTaskQueue<E, F, N>::~GenericTaskQueue() {
  ArrayAllocator<E>::free(_elems, N);
}

template<class E, MEMFLAGS F, unsigned int N> inline bool
GenericTaskQueue<E, F, N>::push(E t) {
  uint localBot = bottom_relaxed();
  assert(localBot < N, "_bottom out of range.");
  idx_t top = age_top_relaxed();
  uint dirty_n_elems = dirty_size(localBot, top);
  // A dirty_size of N-1 cannot happen in push.  Considering only push:
  // (1) dirty_n_elems is initially 0.
  // (2) push adds an element iff dirty_n_elems < max_elems(), which is N - 2.
  // (3) only push adding an element can increase dirty_n_elems.
  // => dirty_n_elems <= N - 2, by induction
  // => dirty_n_elems < N - 1, invariant
  //
  // A pop_global that is concurrent with push cannot produce a state where
  // dirty_size == N-1.  pop_global only removes an element if dirty_elems > 0,
  // so can't underflow to -1 (== N-1) with push.
  assert(dirty_n_elems <= max_elems(), "n_elems out of range.");
  if (dirty_n_elems < max_elems()) {
    _elems[localBot] = t;
    release_set_bottom(increment_index(localBot));
    TASKQUEUE_STATS_ONLY(stats.record_push());
    return true;
  }
  return false;                 // Queue is full.
}

template <class E, MEMFLAGS F, unsigned int N>
inline bool OverflowTaskQueue<E, F, N>::push(E t) {
  if (!taskqueue_t::push(t)) {
    overflow_stack()->push(t);
    TASKQUEUE_STATS_ONLY(stats.record_overflow(overflow_stack()->size()));
  }
  return true;
}

template <class E, MEMFLAGS F, unsigned int N>
inline bool OverflowTaskQueue<E, F, N>::try_push_to_taskqueue(E t) {
  return taskqueue_t::push(t);
}

// pop_local_slow() is done by the owning thread and is trying to
// get the last task in the queue.  It will compete with pop_global()
// that will be used by other threads.  The tag age is incremented
// whenever the queue goes empty which it will do here if this thread
// gets the last task or in pop_global() if the queue wraps (top == 0
// and pop_global() succeeds, see pop_global()).
template<class E, MEMFLAGS F, unsigned int N>
bool GenericTaskQueue<E, F, N>::pop_local_slow(uint localBot, Age oldAge) {
  // This queue was observed to contain exactly one element; either this
  // thread will claim it, or a competing "pop_global".  In either case,
  // the queue will be logically empty afterwards.  Create a new Age value
  // that represents the empty queue for the given value of "bottom".  (We
  // must also increment "tag" because of the case where "bottom == 1",
  // "top == 0".  A pop_global could read the queue element in that case,
  // then have the owner thread do a pop followed by another push.  Without
  // the incrementing of "tag", the pop_global's CAS could succeed,
  // allowing it to believe it has claimed the stale element.)
  Age newAge((idx_t)localBot, (idx_t)(oldAge.tag() + 1));
  // Perhaps a competing pop_global has already incremented "top", in which
  // case it wins the element.
  if (localBot == oldAge.top()) {
    // No competing pop_global has yet incremented "top"; we'll try to
    // install new_age, thus claiming the element.
    Age tempAge = cmpxchg_age(oldAge, newAge);
    if (tempAge == oldAge) {
      // We win.
      assert_not_underflow(localBot, age_top_relaxed());
      TASKQUEUE_STATS_ONLY(stats.record_pop_slow());
      return true;
    }
  }
  // We lose; a competing pop_global got the element.  But the queue is empty
  // and top is greater than bottom.  Fix this representation of the empty queue
  // to become the canonical one.
  set_age_relaxed(newAge);
  assert_not_underflow(localBot, age_top_relaxed());
  return false;
}

template<class E, MEMFLAGS F, unsigned int N> inline bool
GenericTaskQueue<E, F, N>::pop_local(E& t, uint threshold) {
  uint localBot = bottom_relaxed();
  // This value cannot be N-1.  That can only occur as a result of
  // the assignment to bottom in this method.  If it does, this method
  // resets the size to 0 before the next call (which is sequential,
  // since this is pop_local.)
  uint dirty_n_elems = dirty_size(localBot, age_top_relaxed());
  assert_not_underflow(dirty_n_elems);
  if (dirty_n_elems <= threshold) return false;
  localBot = decrement_index(localBot);
  set_bottom_relaxed(localBot);
  // This is necessary to prevent any read below from being reordered
  // before the store just above.
  OrderAccess::fence();
  t = _elems[localBot];
  // This is a second read of "age"; the "size()" above is the first.
  // If there's still at least one element in the queue, based on the
  // "_bottom" and "age" we've read, then there can be no interference with
  // a "pop_global" operation, and we're done.
  idx_t tp = age_top_relaxed();
  if (clean_size(localBot, tp) > 0) {
    assert_not_underflow(localBot, tp);
    TASKQUEUE_STATS_ONLY(stats.record_pop());
    return true;
  } else {
    // Otherwise, the queue contained exactly one element; we take the slow
    // path.

    // The barrier is required to prevent reordering the two reads of age:
    // one is the age() below, and the other is age_top() above the if-stmt.
    // The algorithm may fail if age() reads an older value than age_top().
    OrderAccess::loadload();
    return pop_local_slow(localBot, age_relaxed());
  }
}

template <class E, MEMFLAGS F, unsigned int N>
bool OverflowTaskQueue<E, F, N>::pop_overflow(E& t)
{
  if (overflow_empty()) return false;
  t = overflow_stack()->pop();
  return true;
}

// A pop_global operation may read an element that is being concurrently
// written by a push operation.  The pop_global operation will not use
// such an element, returning failure instead.  But the concurrent read
// and write places requirements on the element type.
//
// Strictly, such concurrent reads and writes are undefined behavior.
// We ignore that. Instead we require that whatever value tearing may
// occur as a result is benign. A trivially copyable type (C++14 3.9/9)
// satisfies the requirement. But we might use classes such as oop that
// are not trivially copyable (in some build configurations).  Such
// classes need to be carefully examined with this requirement in mind.
//
// The sequence where such a read/write collision can arise is as follows.
// Assume there is one value in the queue, so bottom == top+1.
// (1) Thief is doing a pop_global.  It has read age and bottom, and its
// captured (localBottom - oldAge.top) == 1.
// (2) Owner does a pop_local and wins the race for that element.  It
// decrements bottom and increments the age tag.
// (3) Owner starts a push, writing elems[bottom].  At the same time, Thief
// reads elems[oldAge.top].  The owner's bottom == the thief's oldAge.top.
// (4) Thief will discard the read value, because its cmpxchg of age will fail.
template<class E, MEMFLAGS F, unsigned int N>
bool GenericTaskQueue<E, F, N>::pop_global(E& t) {
  Age oldAge = age_relaxed();

  // Architectures with non-multi-copy-atomic memory model require a
  // full fence here to guarantee that bottom is not older than age,
  // which is crucial for the correctness of the algorithm.
  //
  // We need a full fence here for this case:
  //
  // Thread1: set bottom (push)
  // Thread2: read age, read bottom, set age (pop_global)
  // Thread3: read age, read bottom (pop_global)
  //
  // The requirement is that Thread3 must never read an older bottom
  // value than Thread2 after Thread3 has seen the age value from
  // Thread2.
  OrderAccess::loadload_for_IRIW();

  uint localBot = bottom_acquire();
  uint n_elems = clean_size(localBot, oldAge.top());
  if (n_elems == 0) {
    return false;
  }

  t = _elems[oldAge.top()];
  // Increment top; if it wraps, also increment tag, to distinguish it
  // from any recent _age for the same top() index.
  idx_t new_top = increment_index(oldAge.top());
  idx_t new_tag = oldAge.tag() + ((new_top == 0) ? 1 : 0);
  Age newAge(new_top, new_tag);
  Age resAge = cmpxchg_age(oldAge, newAge);

  // Note that using "bottom" here might fail, since a pop_local might
  // have decremented it.
  assert_not_underflow(localBot, newAge.top());
  return resAge == oldAge;
}

inline int randomParkAndMiller(int *seed0) {
  const int a =      16807;
  const int m = 2147483647;
  const int q =     127773;  /* m div a */
  const int r =       2836;  /* m mod a */
  STATIC_ASSERT(sizeof(int) == 4);
  int seed = *seed0;
  int hi   = seed / q;
  int lo   = seed % q;
  int test = a * lo - r * hi;
  if (test > 0) {
    seed = test;
  } else {
    seed = test + m;
  }
  *seed0 = seed;
  return seed;
}

template<class E, MEMFLAGS F, unsigned int N>
int GenericTaskQueue<E, F, N>::next_random_queue_id() {
  return randomParkAndMiller(&_seed);
}

template<class T, MEMFLAGS F> bool
GenericTaskQueueSet<T, F>::steal_best_of_2(uint queue_num, E& t) {
  if (_n > 2) {
    T* const local_queue = _queues[queue_num];
    uint k1 = queue_num;

    if (local_queue->is_last_stolen_queue_id_valid()) {
      k1 = local_queue->last_stolen_queue_id();
      assert(k1 != queue_num, "Should not be the same");
    } else {
      while (k1 == queue_num) {
        k1 = local_queue->next_random_queue_id() % _n;
      }
    }

    uint k2 = queue_num;
    while (k2 == queue_num || k2 == k1) {
      k2 = local_queue->next_random_queue_id() % _n;
    }
    // Sample both and try the larger.
    uint sz1 = _queues[k1]->size();
    uint sz2 = _queues[k2]->size();

    uint sel_k = 0;
    bool suc = false;

    if (sz2 > sz1) {
      sel_k = k2;
      suc = _queues[k2]->pop_global(t);
    } else if (sz1 > 0) {
      sel_k = k1;
      suc = _queues[k1]->pop_global(t);
    }

    if (suc) {
      local_queue->set_last_stolen_queue_id(sel_k);
    } else {
      local_queue->invalidate_last_stolen_queue_id();
    }

    return suc;
  } else if (_n == 2) {
    // Just try the other one.
    uint k = (queue_num + 1) % 2;
    return _queues[k]->pop_global(t);
  } else {
    assert(_n == 1, "can't be zero.");
    return false;
  }
}

template<class T, MEMFLAGS F> bool
GenericTaskQueueSet<T, F>::steal(uint queue_num, E& t) {
  for (uint i = 0; i < 2 * _n; i++) {
    TASKQUEUE_STATS_ONLY(queue(queue_num)->stats.record_steal_attempt());
    if (steal_best_of_2(queue_num, t)) {
      TASKQUEUE_STATS_ONLY(queue(queue_num)->stats.record_steal());
      return true;
    }
  }
  return false;
}

template<class E, MEMFLAGS F, unsigned int N>
template<class Fn>
inline void GenericTaskQueue<E, F, N>::iterate(Fn fn) {
  uint iters = size();
  uint index = bottom_relaxed();
  for (uint i = 0; i < iters; ++i) {
    index = decrement_index(index);
    fn(_elems[index]);
  }
}


#endif // SHARE_GC_SHARED_TASKQUEUE_INLINE_HPP
