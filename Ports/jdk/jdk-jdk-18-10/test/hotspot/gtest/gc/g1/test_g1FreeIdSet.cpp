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

#include "precompiled.hpp"
#include "gc/g1/g1FreeIdSet.hpp"
#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/semaphore.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"

struct G1FreeIdSet::TestSupport : AllStatic {
  static uint next(const G1FreeIdSet& set, uint index) {
    assert(index < set._size, "precondition");
    return set._next[index];
  }

  static uint start(const G1FreeIdSet& set) { return set._start; }
  static uint size(const G1FreeIdSet& set) { return set._size; }
  static uintx mask(const G1FreeIdSet& set) { return set._head_index_mask; }
  static uintx head(const G1FreeIdSet& set) { return Atomic::load(&set._head); }

  static uint head_index(const G1FreeIdSet& set, uintx head) {
    return set.head_index(head);
  }
};

typedef G1FreeIdSet::TestSupport TestSupport;

TEST_VM(G1FreeIdSetTest, initial_state) {
  const uint start = 5;
  const uint size = 4;
  G1FreeIdSet set(start, size);

  ASSERT_EQ(start, TestSupport::start(set));
  ASSERT_EQ(size, TestSupport::size(set));
  ASSERT_EQ(7u, TestSupport::mask(set));
  ASSERT_EQ(0u, TestSupport::head(set));
  for (uint i = 0; i < size; ++i) {
    ASSERT_EQ(i + 1, TestSupport::next(set, i));
  }
}

TEST_VM(G1FreeIdSetTest, non_blocking_ops) {
  const uint start = 5;
  const uint size = 3;
  G1FreeIdSet set(start, size);

  ASSERT_EQ(5u, set.claim_par_id());
  ASSERT_EQ(1u, TestSupport::head_index(set, TestSupport::head(set)));
  ASSERT_EQ(6u, set.claim_par_id());
  ASSERT_EQ(2u, TestSupport::head_index(set, TestSupport::head(set)));
  ASSERT_EQ(7u, set.claim_par_id());
  ASSERT_EQ(3u, TestSupport::head_index(set, TestSupport::head(set)));

  set.release_par_id(5u);
  set.release_par_id(6u);
  ASSERT_EQ(6u, set.claim_par_id());
  ASSERT_EQ(5u, set.claim_par_id());
}

class TestG1FreeIdSetThread : public JavaTestThread {
  G1FreeIdSet* _set;
  volatile size_t* _total_allocations;
  volatile bool* _continue_running;
  size_t _allocations;
  uint _thread_number;

public:
  TestG1FreeIdSetThread(uint thread_number,
                        Semaphore* post,
                        G1FreeIdSet* set,
                        volatile size_t* total_allocations,
                        volatile bool* continue_running) :
    JavaTestThread(post),
    _set(set),
    _total_allocations(total_allocations),
    _continue_running(continue_running),
    _allocations(0),
    _thread_number(thread_number)
  {}

  virtual void main_run() {
    while (Atomic::load_acquire(_continue_running)) {
      uint id = _set->claim_par_id();
      _set->release_par_id(id);
      ++_allocations;
      ThreadBlockInVM tbiv(this); // Safepoint check.
    }
    tty->print_cr("%u allocations: " SIZE_FORMAT, _thread_number, _allocations);
    Atomic::add(_total_allocations, _allocations);
  }
};

TEST_VM(G1FreeIdSetTest, stress) {
  const uint start = 5;
  const uint size = 3;
  const uint nthreads = size + 1;
  const uint milliseconds_to_run = 1000;

  Semaphore post;
  volatile size_t total_allocations = 0;
  volatile bool continue_running = true;

  G1FreeIdSet set(start, size);

  TestG1FreeIdSetThread* threads[nthreads] = {};
  for (uint i = 0; i < nthreads; ++i) {
    threads[i] = new TestG1FreeIdSetThread(i,
                                           &post,
                                           &set,
                                           &total_allocations,
                                           &continue_running);
    threads[i]->doit();
  }

  JavaThread* this_thread = JavaThread::current();
  tty->print_cr("Stressing G1FreeIdSet for %u ms", milliseconds_to_run);
  {
    ThreadInVMfromNative invm(this_thread);
    this_thread->sleep(milliseconds_to_run);
  }
  Atomic::release_store(&continue_running, false);
  for (uint i = 0; i < nthreads; ++i) {
    ThreadInVMfromNative invm(this_thread);
    post.wait_with_safepoint_check(this_thread);
  }
  tty->print_cr("total allocations: " SIZE_FORMAT, total_allocations);
  tty->print_cr("final free list: ");
  uint ids[size] = {};
  for (uint i = 0; i < size; ++i) {
    uint id = set.claim_par_id();
    uint index = id - TestSupport::start(set);
    ASSERT_LT(index, TestSupport::size(set));
    tty->print_cr("  %u: %u", i, index);
  }
  ASSERT_EQ(size, TestSupport::head_index(set, TestSupport::head(set)));
}
