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
#include "unittest.hpp"

#ifdef ASSERT

// Test mismatched safepoint check flag on lock declaration vs. lock acquisition.
TEST_VM_ASSERT_MSG(SafepointLockAssertTest, always_check,
    ".*This lock should always have a safepoint check for Java threads: SFPT_Test_lock") {
  MutexLocker ml(new Mutex(Mutex::leaf, "SFPT_Test_lock", true, Mutex::_safepoint_check_always),
                 Mutex::_no_safepoint_check_flag);
}

TEST_VM_ASSERT_MSG(SafepointLockAssertTest, never_check,
    ".*This lock should never have a safepoint check for Java threads: SFPT_Test_lock") {
  MutexLocker ml(new Mutex(Mutex::leaf, "SFPT_Test_lock", true, Mutex::_safepoint_check_never),
                 Mutex::_safepoint_check_flag);
}

TEST_VM_ASSERT_MSG(SafepointLockAssertTest, special_locks,
    ".*Special locks or below should never safepoint") {
  MutexLocker ml(new Mutex(Mutex::special, "SpecialTest_lock", /*vm_block*/true, Mutex::_safepoint_check_always),
                 Mutex::_safepoint_check_flag);
}

TEST_VM_ASSERT_MSG(SafepointLockAssertTest, possible_safepoint_lock,
    ".* Possible safepoint reached by thread that does not allow it") {
  JavaThread* thread = JavaThread::current();
  ThreadInVMfromNative in_native(thread);
  MutexLocker ml(new Mutex(Mutex::special, "SpecialTest_lock", /*vm_block*/true, Mutex::_safepoint_check_never),
                   Mutex::_no_safepoint_check_flag);
  thread->print_thread_state_on(tty);
  // If the lock above succeeds, try to safepoint to test the NSV implied with this special lock.
  ThreadBlockInVM tbivm(thread);
  thread->print_thread_state_on(tty);
}

#endif // ASSERT
