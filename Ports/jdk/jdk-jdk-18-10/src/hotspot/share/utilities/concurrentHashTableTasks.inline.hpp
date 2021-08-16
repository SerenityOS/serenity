/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_CONCURRENTHASHTABLETASKS_INLINE_HPP
#define SHARE_UTILITIES_CONCURRENTHASHTABLETASKS_INLINE_HPP

// No concurrentHashTableTasks.hpp

#include "runtime/atomic.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/concurrentHashTable.inline.hpp"

// This inline file contains BulkDeleteTask and GrowTasks which are both bucket
// operations, which they are serialized with each other.

// Base class for pause and/or parallel bulk operations.
template <typename CONFIG, MEMFLAGS F>
class ConcurrentHashTable<CONFIG, F>::BucketsOperation {
 protected:
  ConcurrentHashTable<CONFIG, F>* _cht;

  // Default size of _task_size_log2
  static const size_t DEFAULT_TASK_SIZE_LOG2 = 12;

  // The table is split into ranges, every increment is one range.
  volatile size_t _next_to_claim;
  size_t _task_size_log2; // Number of buckets.
  size_t _stop_task;      // Last task
  size_t _size_log2;      // Table size.
  bool   _is_mt;

  BucketsOperation(ConcurrentHashTable<CONFIG, F>* cht, bool is_mt = false)
    : _cht(cht), _next_to_claim(0), _task_size_log2(DEFAULT_TASK_SIZE_LOG2),
    _stop_task(0), _size_log2(0), _is_mt(is_mt) {}

  // Returns true if you succeeded to claim the range start -> (stop-1).
  bool claim(size_t* start, size_t* stop) {
    size_t claimed = Atomic::fetch_and_add(&_next_to_claim, 1u);
    if (claimed >= _stop_task) {
      return false;
    }
    *start = claimed * (((size_t)1) << _task_size_log2);
    *stop  = ((*start) + (((size_t)1) << _task_size_log2));
    return true;
  }

  // Calculate starting values.
  void setup(Thread* thread) {
    thread_owns_resize_lock(thread);
    _size_log2 = _cht->_table->_log2_size;
    _task_size_log2 = MIN2(_task_size_log2, _size_log2);
    size_t tmp = _size_log2 > _task_size_log2 ?
                 _size_log2 - _task_size_log2 : 0;
    _stop_task = (((size_t)1) << tmp);
  }

  // Returns false if all ranges are claimed.
  bool have_more_work() {
    return Atomic::load_acquire(&_next_to_claim) >= _stop_task;
  }

  void thread_owns_resize_lock(Thread* thread) {
    assert(BucketsOperation::_cht->_resize_lock_owner == thread,
           "Should be locked by me");
    assert(BucketsOperation::_cht->_resize_lock->owned_by_self(),
           "Operations lock not held");
  }
  void thread_owns_only_state_lock(Thread* thread) {
    assert(BucketsOperation::_cht->_resize_lock_owner == thread,
           "Should be locked by me");
    assert(!BucketsOperation::_cht->_resize_lock->owned_by_self(),
           "Operations lock held");
  }
  void thread_do_not_own_resize_lock(Thread* thread) {
    assert(!BucketsOperation::_cht->_resize_lock->owned_by_self(),
           "Operations lock held");
    assert(BucketsOperation::_cht->_resize_lock_owner != thread,
           "Should not be locked by me");
  }

public:
  // Pauses for safepoint
  void pause(Thread* thread) {
    // This leaves internal state locked.
    this->thread_owns_resize_lock(thread);
    BucketsOperation::_cht->_resize_lock->unlock();
    this->thread_owns_only_state_lock(thread);
  }

  // Continues after safepoint.
  void cont(Thread* thread) {
    this->thread_owns_only_state_lock(thread);
    // If someone slips in here directly after safepoint.
    while (!BucketsOperation::_cht->_resize_lock->try_lock())
      { /* for ever */ };
    this->thread_owns_resize_lock(thread);
  }
};

// For doing pausable/parallel bulk delete.
template <typename CONFIG, MEMFLAGS F>
class ConcurrentHashTable<CONFIG, F>::BulkDeleteTask :
  public BucketsOperation
{
 public:
  BulkDeleteTask(ConcurrentHashTable<CONFIG, F>* cht, bool is_mt = false)
    : BucketsOperation(cht, is_mt) {
  }
  // Before start prepare must be called.
  bool prepare(Thread* thread) {
    bool lock = BucketsOperation::_cht->try_resize_lock(thread);
    if (!lock) {
      return false;
    }
    this->setup(thread);
    return true;
  }

  // Does one range destroying all matching EVALUATE_FUNC and
  // DELETE_FUNC is called be destruction. Returns true if there is more work.
  template <typename EVALUATE_FUNC, typename DELETE_FUNC>
  bool do_task(Thread* thread, EVALUATE_FUNC& eval_f, DELETE_FUNC& del_f) {
    size_t start, stop;
    assert(BucketsOperation::_cht->_resize_lock_owner != NULL,
           "Should be locked");
    if (!this->claim(&start, &stop)) {
      return false;
    }
    BucketsOperation::_cht->do_bulk_delete_locked_for(thread, start, stop,
                                                      eval_f, del_f,
                                                      BucketsOperation::_is_mt);
    assert(BucketsOperation::_cht->_resize_lock_owner != NULL,
           "Should be locked");
    return true;
  }

  // Must be called after ranges are done.
  void done(Thread* thread) {
    this->thread_owns_resize_lock(thread);
    BucketsOperation::_cht->unlock_resize_lock(thread);
    this->thread_do_not_own_resize_lock(thread);
  }
};

template <typename CONFIG, MEMFLAGS F>
class ConcurrentHashTable<CONFIG, F>::GrowTask :
  public BucketsOperation
{
 public:
  GrowTask(ConcurrentHashTable<CONFIG, F>* cht) : BucketsOperation(cht) {
  }
  // Before start prepare must be called.
  bool prepare(Thread* thread) {
    if (!BucketsOperation::_cht->internal_grow_prolog(
          thread, BucketsOperation::_cht->_log2_size_limit)) {
      return false;
    }
    this->setup(thread);
    return true;
  }

  // Re-sizes a portion of the table. Returns true if there is more work.
  bool do_task(Thread* thread) {
    size_t start, stop;
    assert(BucketsOperation::_cht->_resize_lock_owner != NULL,
           "Should be locked");
    if (!this->claim(&start, &stop)) {
      return false;
    }
    BucketsOperation::_cht->internal_grow_range(thread, start, stop);
    assert(BucketsOperation::_cht->_resize_lock_owner != NULL,
           "Should be locked");
    return true;
  }

  // Must be called after do_task returns false.
  void done(Thread* thread) {
    this->thread_owns_resize_lock(thread);
    BucketsOperation::_cht->internal_grow_epilog(thread);
    this->thread_do_not_own_resize_lock(thread);
  }
};

#endif // SHARE_UTILITIES_CONCURRENTHASHTABLETASKS_INLINE_HPP
