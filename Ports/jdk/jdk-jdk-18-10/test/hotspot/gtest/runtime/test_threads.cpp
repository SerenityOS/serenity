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
#include "memory/allocation.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "unittest.hpp"

struct Threads::Test : public AllStatic {
  class VM_TestClaimOverflow;
  class CountThreads;
  class CheckClaims;
};

class Threads::Test::CountThreads : public ThreadClosure {
  uintx _claim_token;
  uint _java_threads_count;
  uint _non_java_threads_count;
  bool _need_claim;

public:
  CountThreads(uintx claim_token, bool need_claim) :
    _claim_token(claim_token),
    _java_threads_count(0),
    _non_java_threads_count(0),
    _need_claim(need_claim)
  {}

  virtual void do_thread(Thread* t) {
    if (!_need_claim || t->claim_threads_do(true, _claim_token)) {
      if (t->is_Java_thread()) {
        ++_java_threads_count;
      } else {
        ++_non_java_threads_count;
      }
    }
  }

  uint java_threads_count() const { return _java_threads_count; }
  uint non_java_threads_count() const { return _non_java_threads_count; }
  uint count() const { return _java_threads_count + _non_java_threads_count; }
};

class Threads::Test::CheckClaims : public ThreadClosure {
  uintx _claim_token;
  uint _java_threads_claimed;
  uint _java_threads_unclaimed;
  uint _non_java_threads_claimed;
  uint _non_java_threads_unclaimed;

public:
  CheckClaims(uintx claim_token) :
    _claim_token(claim_token),
    _java_threads_claimed(0),
    _java_threads_unclaimed(0),
    _non_java_threads_claimed(0),
    _non_java_threads_unclaimed(0)
  {}

  virtual void do_thread(Thread* t) {
    uintx thread_token = t->threads_do_token();
    if (thread_token == _claim_token) {
      if (t->is_Java_thread()) {
        ++_java_threads_claimed;
      } else {
        ++_non_java_threads_claimed;
      }
    } else {
      if (t->is_Java_thread()) {
        ++_java_threads_unclaimed;
      } else {
        ++_non_java_threads_unclaimed;
      }
    }
  }

  uint java_threads_claimed() const { return _java_threads_claimed; }
  uint java_threads_unclaimed() const { return _java_threads_unclaimed; }

  uint non_java_threads_claimed() const { return _non_java_threads_claimed; }
  uint non_java_threads_unclaimed() const { return _non_java_threads_unclaimed; }

  uint claimed() const {
    return _java_threads_claimed + _non_java_threads_claimed;
  }

  uint unclaimed() const {
    return _java_threads_unclaimed + _non_java_threads_unclaimed;
  }
};

class Threads::Test::VM_TestClaimOverflow : public VM_GTestExecuteAtSafepoint {
public:
  void doit() {
    // Prevent changes to the NJT list while we're conducting our test.
    MutexLocker ml(NonJavaThreadsList_lock, Mutex::_no_safepoint_check_flag);

    _thread_claim_token = max_uintx - 1;

    ASSERT_EQ(max_uintx - 1, thread_claim_token());
    CountThreads count1(thread_claim_token(), true);
    threads_do(&count1);
    tty->print_cr("Testing claim overflow with %u threads", count1.count());
    // At least the main thread and the VM thread.
    ASSERT_LE(2u, count1.count());
    ASSERT_LE(1u, count1.java_threads_count());
    ASSERT_LE(1u, count1.non_java_threads_count());

    ASSERT_EQ(max_uintx - 1, thread_claim_token());
    CheckClaims check1(thread_claim_token());
    threads_do(&check1);
    ASSERT_EQ(count1.count(), check1.claimed());
    ASSERT_EQ(count1.java_threads_count(), check1.java_threads_claimed());
    ASSERT_EQ(0u, check1.java_threads_unclaimed());
    ASSERT_EQ(count1.non_java_threads_count(), check1.non_java_threads_claimed());
    ASSERT_EQ(0u, check1.non_java_threads_unclaimed());

    change_thread_claim_token(); // No overflow yet.
    ASSERT_EQ(max_uintx, thread_claim_token());

    CountThreads count2(thread_claim_token(), false); // Claimed by PPTD below
    possibly_parallel_threads_do(true, &count2);
    ASSERT_EQ(count1.java_threads_count(), count2.java_threads_count());
    ASSERT_EQ(1u, count2.non_java_threads_count()); // Only VM thread

    CheckClaims check2(thread_claim_token());
    threads_do(&check2);
    ASSERT_EQ(count2.java_threads_count(), check2.java_threads_claimed());
    ASSERT_EQ(0u, check2.java_threads_unclaimed());
    ASSERT_EQ(1u, check2.non_java_threads_claimed()); // Only VM thread
    ASSERT_EQ(count1.non_java_threads_count(),
              check2.non_java_threads_claimed() +
              check2.non_java_threads_unclaimed());

    change_thread_claim_token(); // Expect overflow.
    ASSERT_EQ(uintx(1), thread_claim_token());

    // Verify all threads have claim value of 0 after change overflow.
    CheckClaims check3(0);
    threads_do(&check3);
    ASSERT_EQ(count1.count(), check3.claimed());
    ASSERT_EQ(0u, check3.unclaimed());
  }
};

// Test overflow handling in Threads::change_thread_claim_token().
TEST_VM(ThreadsTest, claim_overflow) {
  Threads::Test::VM_TestClaimOverflow op;
  ThreadInVMfromNative invm(JavaThread::current());
  VMThread::execute(&op);
}

TEST_VM(ThreadsTest, fast_jni_in_vm) {
  JavaThread* current = JavaThread::current();
  JNIEnv* env = current->jni_environment();
  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXWrite, current));

  // DirectByteBuffer is an easy way to trigger GetIntField,
  // see JDK-8262896
  jlong capacity = 0x10000;
  jobject buffer = env->NewDirectByteBuffer(NULL, (jlong)capacity);
  ASSERT_NE((void*)NULL, buffer);
  ASSERT_EQ(capacity, env->GetDirectBufferCapacity(buffer));
}
