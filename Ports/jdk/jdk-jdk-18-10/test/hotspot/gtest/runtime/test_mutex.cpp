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
 */

#include "precompiled.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "utilities/formatBuffer.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"

const int iterations = 10;
static Mutex* m[iterations];
static int i = 0;

static void create_mutex(Thread* thr) {
  m[i] = new Mutex(Mutex::leaf, FormatBuffer<128>("MyLock lock #%u", i), true, Mutex::_safepoint_check_never);
  i++;
}

TEST_VM(MutexName, mutex_name) {
  // Create mutexes in threads, where the names are created on the thread
  // stacks and then check that their names are correct.
  for (int i = 0; i < iterations; i++) {
    nomt_test_doer(create_mutex);
  }
  for (int i = 0; i < iterations; i++) {
    FormatBuffer<128> f("MyLock lock #%u", i);
    ASSERT_STREQ(m[i]->name(), f.buffer()) << "Wrong name!";
  }
}

#ifdef ASSERT

const int rankA = 50;

TEST_OTHER_VM(MutexRank, mutex_lock_rank_in_order) {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rankA = new Mutex(rankA, "mutex_rankA", false, Mutex::_safepoint_check_always);
  Mutex* mutex_rankA_plus_one = new Mutex(rankA + 1, "mutex_rankA_plus_one", false, Mutex::_safepoint_check_always);

  mutex_rankA_plus_one->lock();
  mutex_rankA->lock();
  mutex_rankA->unlock();
  mutex_rankA_plus_one->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, mutex_lock_rank_out_of_orderA,
                   ".* Attempting to acquire lock mutex_rankA_plus_one/51 out of order with lock mutex_rankA/50 -- possible deadlock") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rankA = new Mutex(rankA, "mutex_rankA", false, Mutex::_safepoint_check_always);
  Mutex* mutex_rankA_plus_one = new Mutex(rankA + 1, "mutex_rankA_plus_one", false, Mutex::_safepoint_check_always);

  mutex_rankA->lock();
  mutex_rankA_plus_one->lock();
  mutex_rankA_plus_one->unlock();
  mutex_rankA->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, mutex_lock_rank_out_of_orderB,
                   ".* Attempting to acquire lock mutex_rankB/50 out of order with lock mutex_rankA/50 -- possible deadlock") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rankA = new Mutex(rankA, "mutex_rankA", false, Mutex::_safepoint_check_always);
  Mutex* mutex_rankB = new Mutex(rankA, "mutex_rankB", false, Mutex::_safepoint_check_always);

  mutex_rankA->lock();
  mutex_rankB->lock();
  mutex_rankB->unlock();
  mutex_rankA->unlock();
}

TEST_OTHER_VM(MutexRank, mutex_trylock_rank_out_of_orderA) {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rankA = new Mutex(rankA, "mutex_rankA", false, Mutex::_safepoint_check_always);
  Mutex* mutex_rankA_plus_one = new Mutex(rankA + 1, "mutex_rankA_plus_one", false, Mutex::_safepoint_check_always);
  Mutex* mutex_rankA_plus_two = new Mutex(rankA + 2, "mutex_rankA_plus_two", false, Mutex::_safepoint_check_always);

  mutex_rankA_plus_one->lock();
  mutex_rankA_plus_two->try_lock_without_rank_check();
  mutex_rankA->lock();
  mutex_rankA->unlock();
  mutex_rankA_plus_two->unlock();
  mutex_rankA_plus_one->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, mutex_trylock_rank_out_of_orderB,
                   ".* Attempting to acquire lock mutex_rankA_plus_one/51 out of order with lock mutex_rankA/50 -- possible deadlock") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rankA = new Mutex(rankA, "mutex_rankA", false, Mutex::_safepoint_check_always);
  Mutex* mutex_rankA_plus_one = new Mutex(rankA + 1, "mutex_rankA_plus_one", false, Mutex::_safepoint_check_always);

  mutex_rankA->lock();
  mutex_rankA_plus_one->try_lock_without_rank_check();
  mutex_rankA_plus_one->unlock();
  mutex_rankA_plus_one->try_lock();
  mutex_rankA_plus_one->unlock();
  mutex_rankA->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, mutex_lock_access_leaf,
                   ".* Attempting to acquire lock mutex_rank_leaf/11 out of order with lock mutex_rank_access/1 "
                   "-- possible deadlock") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rank_access = new Mutex(Mutex::access, "mutex_rank_access", false, Mutex::_safepoint_check_never);
  Mutex* mutex_rank_leaf = new Mutex(Mutex::leaf, "mutex_rank_leaf", false, Mutex::_safepoint_check_never);

  mutex_rank_access->lock_without_safepoint_check();
  mutex_rank_leaf->lock_without_safepoint_check();
  mutex_rank_leaf->unlock();
  mutex_rank_access->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, mutex_lock_tty_special,
                   ".* Attempting to acquire lock mutex_rank_special/6 out of order with lock mutex_rank_tty/3 "
                   "-- possible deadlock") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Mutex* mutex_rank_tty = new Mutex(Mutex::tty, "mutex_rank_tty", false, Mutex::_safepoint_check_never);
  Mutex* mutex_rank_special = new Mutex(Mutex::special, "mutex_rank_special", false, Mutex::_safepoint_check_never);

  mutex_rank_tty->lock_without_safepoint_check();
  mutex_rank_special->lock_without_safepoint_check();
  mutex_rank_special->unlock();
  mutex_rank_tty->unlock();
}

TEST_OTHER_VM(MutexRank, monitor_wait_rank_in_order) {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Monitor* monitor_rankA = new Monitor(rankA, "monitor_rankA", false, Mutex::_safepoint_check_always);
  Monitor* monitor_rankA_plus_one = new Monitor(rankA + 1, "monitor_rankA_plus_one", false, Mutex::_safepoint_check_always);

  monitor_rankA_plus_one->lock();
  monitor_rankA->lock();
  monitor_rankA->wait(1);
  monitor_rankA->unlock();
  monitor_rankA_plus_one->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, monitor_wait_rank_out_of_order,
                   ".* Attempting to wait on monitor monitor_rankA_plus_one/51 while holding lock monitor_rankA/50 "
                   "-- possible deadlock. Should wait on the least ranked monitor from all owned locks.") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Monitor* monitor_rankA = new Monitor(rankA, "monitor_rankA", false, Mutex::_safepoint_check_always);
  Monitor* monitor_rankA_plus_one = new Monitor(rankA + 1, "monitor_rankA_plus_one", false, Mutex::_safepoint_check_always);

  monitor_rankA_plus_one->lock();
  monitor_rankA->lock();
  monitor_rankA_plus_one->wait(1);
  monitor_rankA_plus_one->unlock();
  monitor_rankA->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, monitor_wait_rank_out_of_order_trylock,
                   ".* Attempting to wait on monitor monitor_rankA_plus_one/51 while holding lock monitor_rankA/50 "
                   "-- possible deadlock. Should wait on the least ranked monitor from all owned locks.") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Monitor* monitor_rankA = new Monitor(rankA, "monitor_rankA", false, Mutex::_safepoint_check_always);
  Monitor* monitor_rankA_plus_one = new Monitor(rankA + 1, "monitor_rankA_plus_one", false, Mutex::_safepoint_check_always);

  monitor_rankA->lock();
  monitor_rankA_plus_one->try_lock_without_rank_check();
  monitor_rankA_plus_one->wait();
  monitor_rankA_plus_one->unlock();
  monitor_rankA->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, monitor_wait_rank_special,
                   ".* Attempting to wait on monitor monitor_rank_special_minus_one/5 while holding lock monitor_rank_special/6 "
                   "-- possible deadlock. Should not block\\(wait\\) while holding a lock of rank special.") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Monitor* monitor_rank_special = new Monitor(Mutex::special, "monitor_rank_special", false, Mutex::_safepoint_check_never);
  Monitor* monitor_rank_special_minus_one = new Monitor(Mutex::special - 1, "monitor_rank_special_minus_one", false, Mutex::_safepoint_check_never);

  monitor_rank_special->lock_without_safepoint_check();
  monitor_rank_special_minus_one->lock_without_safepoint_check();
  monitor_rank_special_minus_one->wait_without_safepoint_check(1);
  monitor_rank_special_minus_one->unlock();
  monitor_rank_special->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, monitor_wait_access_leaf,
                   ".* Attempting to wait on monitor monitor_rank_access/1 while holding lock monitor_rank_tty/3 "
                   "-- possible deadlock. Should not block\\(wait\\) while holding a lock of rank special.") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Monitor* monitor_rank_tty = new Monitor(Mutex::tty, "monitor_rank_tty", false, Mutex::_safepoint_check_never);
  Monitor* monitor_rank_access = new Monitor(Mutex::access, "monitor_rank_access", false, Mutex::_safepoint_check_never);

  monitor_rank_tty->lock_without_safepoint_check();
  monitor_rank_access->lock_without_safepoint_check();
  monitor_rank_access->wait_without_safepoint_check(1);
  monitor_rank_access->unlock();
  monitor_rank_tty->unlock();
}

TEST_VM_ASSERT_MSG(MutexRank, monitor_wait_tty_special,
                   ".* Attempting to wait on monitor monitor_rank_tty/3 while holding lock monitor_rank_special/6 "
                   "-- possible deadlock. Should not block\\(wait\\) while holding a lock of rank special.") {
  JavaThread* THREAD = JavaThread::current();
  ThreadInVMfromNative invm(THREAD);

  Monitor* monitor_rank_special = new Monitor(Mutex::special, "monitor_rank_special", false, Mutex::_safepoint_check_never);
  Monitor* monitor_rank_tty = new Monitor(Mutex::tty, "monitor_rank_tty", false, Mutex::_safepoint_check_never);

  monitor_rank_special->lock_without_safepoint_check();
  monitor_rank_tty->lock_without_safepoint_check();
  monitor_rank_tty->wait_without_safepoint_check(1);
  monitor_rank_tty->unlock();
  monitor_rank_special->unlock();
}
#endif // ASSERT
