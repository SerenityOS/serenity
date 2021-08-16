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
 */

#include "precompiled.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "unittest.hpp"

class ThreadsListHandleTest : public ::testing::Test {
  public:
    // Accessors for the Threads class:
    //
    // Return the protected Thread::_threads_hazard_ptr field:
    static ThreadsList* get_Thread_threads_hazard_ptr(Thread* thr) {
      return thr->get_threads_hazard_ptr();
    }
    // Return the protected Thread::_threads_list_ptr field:
    static SafeThreadsListPtr* get_Thread_threads_list_ptr(Thread* thr) {
      return thr->_threads_list_ptr;
    }
    // Return the protected Thread::_nested_threads_hazard_ptr_cnt field:
    static uint get_Thread_nested_threads_hazard_ptr_cnt(Thread* thr) {
      return thr->nested_threads_hazard_ptr_cnt();
    }

    // Accessors for the ThreadsListHandle class:
    //
    // Return the private ThreadsListHandle::_list_ptr field:
    static SafeThreadsListPtr* get_TLH_list_ptr(ThreadsListHandle* tlh_p) {
      return &tlh_p->_list_ptr;
    }

    // Accessors for the ThreadsList class:
    //
    // Return the private ThreadsList::_nested_handle_cnt field:
    static intx get_TL_nested_handle_cnt(ThreadsList* tl_p) {
      return tl_p->_nested_handle_cnt;
    }

    // Accessors for the SafeThreadsListPtr class:
    //
    // Return the private SafeThreadsListPtr::_thread field:
    static Thread* get_STLP_thread(SafeThreadsListPtr* stlp_p) {
      return stlp_p->_thread;
    }
    // Return the private SafeThreadsListPtr::_has_ref_count field:
    static bool get_STLP_has_ref_count(SafeThreadsListPtr* stlp_p) {
      return stlp_p->_has_ref_count;
    }
    // Return the private SafeThreadsListPtr::_needs_release field:
    static bool get_STLP_needs_release(SafeThreadsListPtr* stlp_p) {
      return stlp_p->_needs_release;
    }
};

TEST_VM(ThreadsListHandle, sanity) {
  bool saved_flag_val = EnableThreadSMRStatistics;
  EnableThreadSMRStatistics = true;  // enable Thread::_nested_threads_hazard_ptr_cnt

  Thread* thr = Thread::current();

  // Test case: no ThreadsListHandle
  //

  // Verify the current thread refers to no ThreadsListHandle:
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
      << "thr->_threads_hazard_ptr must be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), (SafeThreadsListPtr*) NULL)
      << "thr->_threads_list_ptr must be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
      << "thr->_nested_threads_hazard_ptr_cnt must be 0";

  // Test case: single ThreadsListHandle, no recursion
  //
  {
    ThreadsListHandle tlh1;
    SafeThreadsListPtr* list_ptr1 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh1);

    // Verify the current thread refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh1.list())
        << "thr->_threads_hazard_ptr must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), false)
        << "list_ptr1->_has_ref_count must be false";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)0)
        << "list_ptr1->list()->_nested_handle_cnt must be 0";
  } // destroy tlh1

  // Test case: after first ThreadsListHandle (tlh1) has been destroyed
  //

  // Verify the current thread refers to no ThreadsListHandle:
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
      << "thr->_threads_hazard_ptr must match be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), (SafeThreadsListPtr*) NULL)
      << "thr->_threads_list_ptr must be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
      << "thr->_nested_threads_hazard_ptr_cnt must be 0";

  // Test case: first ThreadsListHandle to prepare for nesting
  //
  {
    ThreadsListHandle tlh1;
    SafeThreadsListPtr* list_ptr1 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh1);

    // Verify the current thread refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh1.list())
        << "thr->_threads_hazard_ptr must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), false)
        << "list_ptr1->_has_ref_count must be false";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)0)
        << "list_ptr1->list()->_nested_handle_cnt must be 0";

    // Test case: first nested ThreadsListHandle
    //
    {
      ThreadsListHandle tlh2;
      SafeThreadsListPtr* list_ptr2 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh2);

      // Verify the current thread refers to tlh2:
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh2.list())
          << "thr->_threads_hazard_ptr must match tlh2.list()";
      EXPECT_EQ(tlh1.list(), tlh2.list())
          << "tlh1.list() must match tlh2.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr2)
          << "thr->_threads_list_ptr must match list_ptr2";
      EXPECT_NE(list_ptr1, list_ptr2)
          << "list_ptr1 must not match list_ptr2";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)1)
          << "thr->_nested_threads_hazard_ptr_cnt must be 1";

      // Verify tlh2 has the right field values:
      EXPECT_EQ(list_ptr2->previous(), list_ptr1)
          << "list_ptr2->previous() must be list_ptr1";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr2), thr)
          << "list_ptr2->_thread must match current thread";
      EXPECT_EQ(list_ptr2->list(), tlh2.list())
          << "list_ptr2->list() must match tlh2.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr2), false)
          << "list_ptr2->_has_ref_count must be false";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr2), true)
          << "list_ptr2->_needs_release must be true";

      // Verify tlh1 has the right field values:
      EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
          << "list_ptr1->previous() must be NULL";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
          << "list_ptr1->_thread must match current thread";
      EXPECT_EQ(list_ptr1->list(), tlh1.list())
          << "list_ptr1->list() must match tlh1.list()";
      // When tlh2 was created, tlh1's _has_ref_count was set to true and
      // tlh1's list->_nested_handle_cnt was incremented.
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
          << "list_ptr1->_has_ref_count must be true";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
          << "list_ptr1->_needs_release must be true";

      // Verify tlh1 ThreadsList has the right field values:
      EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
          << "list_ptr1->list()->_nested_handle_cnt must be 1";
    } // destroy tlh2

    // Test case: after first nested ThreadsListHandle (tlh2) has been destroyed

    // Verify the current thread's hazard ptr is NULL:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
        << "thr->_threads_hazard_ptr must be NULL";
    // Verify the current thread's threads list ptr refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    // When tlh2 was created, tlh1's _has_ref_count was set to true and
    // tlh1's list->_nested_handle_cnt was incremented.
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
        << "list_ptr1->_has_ref_count must be true";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

      // Verify tlh1 ThreadsList has the right field values:
      EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
          << "list_ptr1->list()->_nested_handle_cnt must be 1";
  } // destroy tlh1

  // Test case: after first ThreadsListHandle to prepare for nesting has been destroyed
  //

  // Verify the current thread refers to no ThreadsListHandle:
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
      << "thr->_threads_hazard_ptr must match be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), (SafeThreadsListPtr*) NULL)
      << "thr->_threads_list_ptr must be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
      << "thr->_nested_threads_hazard_ptr_cnt must be 0";

  // Test case: first ThreadsListHandle to prepare for double nesting
  //
  {
    ThreadsListHandle tlh1;
    SafeThreadsListPtr* list_ptr1 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh1);

    // Verify the current thread refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh1.list())
        << "thr->_threads_hazard_ptr must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), false)
        << "list_ptr1->_has_ref_count must be false";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)0)
        << "list_ptr1->list()->_nested_handle_cnt must be 0";

    // Test case: first nested ThreadsListHandle
    //
    {
      ThreadsListHandle tlh2;
      SafeThreadsListPtr* list_ptr2 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh2);

      // Verify the current thread refers to tlh2:
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh2.list())
          << "thr->_threads_hazard_ptr must match tlh2.list()";
      EXPECT_EQ(tlh1.list(), tlh2.list())
          << "tlh1.list() must match tlh2.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr2)
          << "thr->_threads_list_ptr must match list_ptr2";
      EXPECT_NE(list_ptr1, list_ptr2)
          << "list_ptr1 must not match list_ptr2";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)1)
          << "thr->_nested_threads_hazard_ptr_cnt must be 1";

      // Verify tlh2 has the right field values:
      EXPECT_EQ(list_ptr2->previous(), list_ptr1)
          << "list_ptr2->previous() must be list_ptr1";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr2), thr)
          << "list_ptr2->_thread must match current thread";
      EXPECT_EQ(list_ptr2->list(), tlh2.list())
          << "list_ptr2->list() must match tlh2.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr2), false)
          << "list_ptr2->_has_ref_count must be false";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr2), true)
          << "list_ptr2->_needs_release must be true";

      // Verify tlh1 has the right field values:
      EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
          << "list_ptr1->previous() must be NULL";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
          << "list_ptr1->_thread must match current thread";
      EXPECT_EQ(list_ptr1->list(), tlh1.list())
          << "list_ptr1->list() must match tlh1.list()";
      // When tlh2 was created, tlh1's _has_ref_count was set to true and
      // tlh1's list->_nested_handle_cnt was incremented.
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
          << "list_ptr1->_has_ref_count must be true";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
          << "list_ptr1->_needs_release must be true";

      // Verify tlh1 ThreadsList has the right field values:
      EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
          << "list_ptr1->list()->_nested_handle_cnt must be 1";

      // Test case: double nested ThreadsListHandle
      //
      {
        ThreadsListHandle tlh3;
        SafeThreadsListPtr* list_ptr3 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh3);

        // Verify the current thread refers to tlh3:
        EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh3.list())
            << "thr->_threads_hazard_ptr must match tlh3.list()";
        EXPECT_EQ(tlh1.list(), tlh3.list())
            << "tlh1.list() must match tlh3.list()";
        EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr3)
            << "thr->_threads_list_ptr must match list_ptr3";
        EXPECT_NE(list_ptr1, list_ptr3)
            << "list_ptr1 must not match list_ptr3";
        EXPECT_NE(list_ptr2, list_ptr3)
            << "list_ptr. must not match list_ptr3";
        EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)2)
            << "thr->_nested_threads_hazard_ptr_cnt must be 2";

        // Verify tlh3 has the right field values:
        EXPECT_EQ(list_ptr3->previous(), list_ptr2)
            << "list_ptr3->previous() must be list_ptr2";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr3), thr)
            << "list_ptr3->_thread must match current thread";
        EXPECT_EQ(list_ptr3->list(), tlh3.list())
            << "list_ptr3->list() must match tlh3.list()";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr3), false)
            << "list_ptr3->_has_ref_count must be false";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr3), true)
            << "list_ptr3->_needs_release must be true";

        // Verify tlh2 has the right field values:
        EXPECT_EQ(list_ptr2->previous(), list_ptr1)
            << "list_ptr2->previous() must be list_ptr1";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr2), thr)
            << "list_ptr2->_thread must match current thread";
        EXPECT_EQ(list_ptr2->list(), tlh2.list())
            << "list_ptr2->list() must match tlh2.list()";
        // When tlh3 was created, tlh2's _has_ref_count was set to true and
        // tlh2's list->_nested_handle_cnt was incremented.
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr2), true)
            << "list_ptr2->_has_ref_count must be true";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr2), true)
            << "list_ptr2->_needs_release must be true";

        // Verify tlh1 has the right field values:
        EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
            << "list_ptr1->previous() must be NULL";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
            << "list_ptr1->_thread must match current thread";
        EXPECT_EQ(list_ptr1->list(), tlh1.list())
            << "list_ptr1->list() must match tlh1.list()";
        // When tlh2 was created, tlh1's _has_ref_count was set to true and
        // tlh1's list->_nested_handle_cnt was incremented.
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
            << "list_ptr1->_has_ref_count must be true";
        EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
            << "list_ptr1->_needs_release must be true";

        // Verify tlh1 ThreadsList has the right field values:
        EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)2)
            << "list_ptr1->list()->_nested_handle_cnt must be 2";
      } // destroy tlh3

      // Test case: after double nested ThreadsListHandle (tlh3) has been destroyed

      // Verify the current thread's hazard ptr is NULL:
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
          << "thr->_threads_hazard_ptr must be NULL";
      // Verify the current thread's threads list ptr refers to tlh2:
      EXPECT_EQ(tlh1.list(), tlh2.list())
          << "tlh1.list() must match tlh2.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr2)
          << "thr->_threads_list_ptr must match list_ptr2";
      EXPECT_NE(list_ptr1, list_ptr2)
          << "list_ptr1 must not match list_ptr2";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)1)
          << "thr->_nested_threads_hazard_ptr_cnt must be 1";

      // Verify tlh2 has the right field values:
      EXPECT_EQ(list_ptr2->previous(), list_ptr1)
          << "list_ptr2->previous() must be list_ptr1";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr2), thr)
          << "list_ptr2->_thread must match current thread";
      EXPECT_EQ(list_ptr2->list(), tlh2.list())
          << "list_ptr2->list() must match tlh2.list()";
      // When tlh3 was created, tlh2's _has_ref_count was set to true and
      // tlh2's list->_nested_handle_cnt was incremented.
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr2), true)
          << "list_ptr2->_has_ref_count must be true";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr2), true)
          << "list_ptr2->_needs_release must be true";

      // Verify tlh1 has the right field values:
      EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
          << "list_ptr1->previous() must be NULL";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
          << "list_ptr1->_thread must match current thread";
      EXPECT_EQ(list_ptr1->list(), tlh1.list())
          << "list_ptr1->list() must match tlh1.list()";
      // When tlh2 was created, tlh1's _has_ref_count was set to true and
      // tlh1's list->_nested_handle_cnt was incremented.
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
          << "list_ptr1->_has_ref_count must be true";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
          << "list_ptr1->_needs_release must be true";

      // Verify tlh1 ThreadsList has the right field values:
      EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)2)
          << "list_ptr1->list()->_nested_handle_cnt must be 2";
    } // destroy tlh2

    // Test case: after first nested ThreadsListHandle (tlh2) has been destroyed

    // Verify the current thread's hazard ptr is NULL:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
        << "thr->_threads_hazard_ptr must be NULL";
    // Verify the current thread's threads list ptr refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    // When tlh2 was created, tlh1's _has_ref_count was set to true and
    // tlh1's list->_nested_handle_cnt was incremented.
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
        << "list_ptr1->_has_ref_count must be true";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
        << "list_ptr1->list()->_nested_handle_cnt must be 1";
  } // destroy tlh1

  // Test case: after first ThreadsListHandle to prepare for double nesting has been destroyed
  //

  // Verify the current thread refers to no ThreadsListHandle:
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
      << "thr->_threads_hazard_ptr must match be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), (SafeThreadsListPtr*) NULL)
      << "thr->_threads_list_ptr must be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
      << "thr->_nested_threads_hazard_ptr_cnt must be 0";

  // Test case: first ThreadsListHandle to prepare for back-to-back nesting
  //
  {
    ThreadsListHandle tlh1;
    SafeThreadsListPtr* list_ptr1 = ThreadsListHandleTest::get_TLH_list_ptr(&tlh1);

    // Verify the current thread refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh1.list())
        << "thr->_threads_hazard_ptr must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), false)
        << "list_ptr1->_has_ref_count must be false";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)0)
        << "list_ptr1->list()->_nested_handle_cnt must be 0";

    // Test case: first back-to-back nested ThreadsListHandle
    //
    {
      ThreadsListHandle tlh2a;
      SafeThreadsListPtr* list_ptr2a = ThreadsListHandleTest::get_TLH_list_ptr(&tlh2a);

      // Verify the current thread refers to tlh2a:
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh2a.list())
          << "thr->_threads_hazard_ptr must match tlh2a.list()";
      EXPECT_EQ(tlh1.list(), tlh2a.list())
          << "tlh1.list() must match tlh2a.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr2a)
          << "thr->_threads_list_ptr must match list_ptr2a";
      EXPECT_NE(list_ptr1, list_ptr2a)
          << "list_ptr1 must not match list_ptr2a";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)1)
          << "thr->_nested_threads_hazard_ptr_cnt must be 1";

      // Verify tlh2a has the right field values:
      EXPECT_EQ(list_ptr2a->previous(), list_ptr1)
          << "list_ptr2a->previous() must be list_ptr1";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr2a), thr)
          << "list_ptr2a->_thread must match current thread";
      EXPECT_EQ(list_ptr2a->list(), tlh2a.list())
          << "list_ptr2a->list() must match tlh2a.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr2a), false)
          << "list_ptr2a->_has_ref_count must be false";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr2a), true)
          << "list_ptr2a->_needs_release must be true";

      // Verify tlh1 has the right field values:
      EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
          << "list_ptr1->previous() must be NULL";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
          << "list_ptr1->_thread must match current thread";
      EXPECT_EQ(list_ptr1->list(), tlh1.list())
          << "list_ptr1->list() must match tlh1.list()";
      // When tlh2a was created, tlh1's _has_ref_count was set to true and
      // tlh1's list->_nested_handle_cnt was incremented.
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
          << "list_ptr1->_has_ref_count must be true";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
          << "list_ptr1->_needs_release must be true";

      // Verify tlh1 ThreadsList has the right field values:
      EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
          << "list_ptr1->list()->_nested_handle_cnt must be 1";
    } // destroy tlh2a

    // Test case: after first back-to-back nested ThreadsListHandle (tlh2a) has been destroyed

    // Verify the current thread's hazard ptr is NULL:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
        << "thr->_threads_hazard_ptr must be NULL";
    // Verify the current thread's threads list ptr refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    // When tlh2a was created, tlh1's _has_ref_count was set to true and
    // tlh1's list->_nested_handle_cnt was incremented.
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
        << "list_ptr1->_has_ref_count must be true";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
        << "list_ptr1->list()->_nested_handle_cnt must be 1";

    // Test case: second back-to-back nested ThreadsListHandle
    //
    {
      ThreadsListHandle tlh2b;
      SafeThreadsListPtr* list_ptr2b = ThreadsListHandleTest::get_TLH_list_ptr(&tlh2b);

      // Verify the current thread refers to tlh2b:
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), tlh2b.list())
          << "thr->_threads_hazard_ptr must match tlh2b.list()";
      EXPECT_EQ(tlh1.list(), tlh2b.list())
          << "tlh1.list() must match tlh2b.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr2b)
          << "thr->_threads_list_ptr must match list_ptr2b";
      EXPECT_NE(list_ptr1, list_ptr2b)
          << "list_ptr1 must not match list_ptr2b";
      EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)1)
          << "thr->_nested_threads_hazard_ptr_cnt must be 1";

      // Verify tlh2b has the right field values:
      EXPECT_EQ(list_ptr2b->previous(), list_ptr1)
          << "list_ptr2b->previous() must be list_ptr1";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr2b), thr)
          << "list_ptr2b->_thread must match current thread";
      EXPECT_EQ(list_ptr2b->list(), tlh2b.list())
          << "list_ptr2b->list() must match tlh2b.list()";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr2b), false)
          << "list_ptr2b->_has_ref_count must be false";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr2b), true)
          << "list_ptr2b->_needs_release must be true";

      // Verify tlh1 has the right field values:
      EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
          << "list_ptr1->previous() must be NULL";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
          << "list_ptr1->_thread must match current thread";
      EXPECT_EQ(list_ptr1->list(), tlh1.list())
          << "list_ptr1->list() must match tlh1.list()";
      // When tlh2a was created, tlh1's _has_ref_count was set to true and
      // tlh1's list->_nested_handle_cnt was incremented.
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
          << "list_ptr1->_has_ref_count must be true";
      EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
          << "list_ptr1->_needs_release must be true";

      // Verify tlh1 ThreadsList has the right field values:
      EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
          << "list_ptr1->list()->_nested_handle_cnt must be 1";
    } // destroy tlh2b

    // Test case: after second back-to-back nested ThreadsListHandle (tlh2b) has been destroyed

    // Verify the current thread's hazard ptr is NULL:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
        << "thr->_threads_hazard_ptr must be NULL";
    // Verify the current thread's threads list ptr refers to tlh1:
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), list_ptr1)
        << "thr->_threads_list_ptr must match list_ptr1";
    EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
        << "thr->_nested_threads_hazard_ptr_cnt must be 0";

    // Verify tlh1 has the right field values:
    EXPECT_EQ(list_ptr1->previous(), (SafeThreadsListPtr*)NULL)
        << "list_ptr1->previous() must be NULL";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_thread(list_ptr1), thr)
        << "list_ptr1->_thread must match current thread";
    EXPECT_EQ(list_ptr1->list(), tlh1.list())
        << "list_ptr1->list() must match tlh1.list()";
    // When tlh2a was created, tlh1's _has_ref_count was set to true and
    // tlh1's list->_nested_handle_cnt was incremented.
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_has_ref_count(list_ptr1), true)
        << "list_ptr1->_has_ref_count must be true";
    EXPECT_EQ(ThreadsListHandleTest::get_STLP_needs_release(list_ptr1), true)
        << "list_ptr1->_needs_release must be true";

    // Verify tlh1 ThreadsList has the right field values:
    EXPECT_EQ(ThreadsListHandleTest::get_TL_nested_handle_cnt(list_ptr1->list()), (intx)1)
        << "list_ptr1->list()->_nested_handle_cnt must be 1";
  } // destroy tlh1

  // Test case: after first ThreadsListHandle to prepare for back-to-back nesting has been destroyed
  //

  // Verify the current thread refers to no ThreadsListHandle:
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_hazard_ptr(thr), (ThreadsList*)NULL)
      << "thr->_threads_hazard_ptr must match be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_threads_list_ptr(thr), (SafeThreadsListPtr*) NULL)
      << "thr->_threads_list_ptr must be NULL";
  EXPECT_EQ(ThreadsListHandleTest::get_Thread_nested_threads_hazard_ptr_cnt(thr), (uint)0)
      << "thr->_nested_threads_hazard_ptr_cnt must be 0";

  EnableThreadSMRStatistics = saved_flag_val;
}
