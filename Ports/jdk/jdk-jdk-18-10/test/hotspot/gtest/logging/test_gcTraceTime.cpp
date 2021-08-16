/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "logTestFixture.hpp"
#include "logTestUtils.inline.hpp"
#include "logging/log.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "unittest.hpp"

class GCTraceTimeTest : public LogTestFixture {
};

TEST_VM_F(GCTraceTimeTest, full) {
  set_log_config(TestLogFileName, "gc=debug,gc+start=debug");

  LogTarget(Debug, gc) gc_debug;
  LogTarget(Debug, gc, start) gc_start_debug;

  EXPECT_TRUE(gc_debug.is_enabled());
  EXPECT_TRUE(gc_start_debug.is_enabled());

  {
    ThreadInVMfromNative tvn(JavaThread::current());
    MutexLocker lock(Heap_lock); // Needed to read heap usage
    GCTraceTime(Debug, gc) timer("Test GC", NULL, GCCause::_allocation_failure, true);
  }

  const char* expected[] = {
    "[gc,start", "] Test GC (Allocation Failure)",
    "[gc", "] Test GC (Allocation Failure) ", "M) ", "ms",
    NULL
  };
  EXPECT_TRUE(file_contains_substrings_in_order(TestLogFileName, expected));
}

TEST_VM_F(GCTraceTimeTest, full_multitag) {
  set_log_config(TestLogFileName, "gc+ref=debug,gc+ref+start=debug");

  LogTarget(Debug, gc, ref) gc_debug;
  LogTarget(Debug, gc, ref, start) gc_start_debug;

  EXPECT_TRUE(gc_debug.is_enabled());
  EXPECT_TRUE(gc_start_debug.is_enabled());

  {
    ThreadInVMfromNative tvn(JavaThread::current());
    MutexLocker lock(Heap_lock); // Needed to read heap usage
    GCTraceTime(Debug, gc, ref) timer("Test GC", NULL, GCCause::_allocation_failure, true);
  }

  const char* expected[] = {
    "[gc,ref,start", "] Test GC (Allocation Failure)",
    "[gc,ref", "] Test GC (Allocation Failure) ", "M) ", "ms",
    NULL
  };
  EXPECT_TRUE(file_contains_substrings_in_order(TestLogFileName, expected));
}

TEST_VM_F(GCTraceTimeTest, no_heap) {
  set_log_config(TestLogFileName, "gc=debug,gc+start=debug");

  LogTarget(Debug, gc) gc_debug;
  LogTarget(Debug, gc, start) gc_start_debug;

  EXPECT_TRUE(gc_debug.is_enabled());
  EXPECT_TRUE(gc_start_debug.is_enabled());

  {
    GCTraceTime(Debug, gc) timer("Test GC", NULL, GCCause::_allocation_failure, false);
  }

  const char* expected[] = {
    // [2.975s][debug][gc,start] Test GC (Allocation Failure)
    "[gc,start", "] Test GC (Allocation Failure)",
    // [2.975s][debug][gc      ] Test GC (Allocation Failure) 0.026ms
    "[gc", "] Test GC (Allocation Failure) ", "ms",
    NULL
  };
  EXPECT_TRUE(file_contains_substrings_in_order(TestLogFileName, expected));

  const char* not_expected[] = {
      // [2.975s][debug][gc      ] Test GC 59M->59M(502M) 0.026ms
      "[gc", "] Test GC ", "M) ", "ms",
  };
  EXPECT_FALSE(file_contains_substrings_in_order(TestLogFileName, not_expected));
}

TEST_VM_F(GCTraceTimeTest, no_cause) {
  set_log_config(TestLogFileName, "gc=debug,gc+start=debug");

  LogTarget(Debug, gc) gc_debug;
  LogTarget(Debug, gc, start) gc_start_debug;

  EXPECT_TRUE(gc_debug.is_enabled());
  EXPECT_TRUE(gc_start_debug.is_enabled());

  {
    ThreadInVMfromNative tvn(JavaThread::current());
    MutexLocker lock(Heap_lock); // Needed to read heap usage
    GCTraceTime(Debug, gc) timer("Test GC", NULL, GCCause::_no_gc, true);
  }

  const char* expected[] = {
    // [2.975s][debug][gc,start] Test GC
    "[gc,start", "] Test GC",
    // [2.975s][debug][gc      ] Test GC 59M->59M(502M) 0.026ms
    "[gc", "] Test GC ", "M) ", "ms",
    NULL
  };
  EXPECT_TRUE(file_contains_substrings_in_order(TestLogFileName, expected));
}

TEST_VM_F(GCTraceTimeTest, no_heap_no_cause) {
  set_log_config(TestLogFileName, "gc=debug,gc+start=debug");

  LogTarget(Debug, gc) gc_debug;
  LogTarget(Debug, gc, start) gc_start_debug;

  EXPECT_TRUE(gc_debug.is_enabled());
  EXPECT_TRUE(gc_start_debug.is_enabled());

  {
    GCTraceTime(Debug, gc) timer("Test GC", NULL, GCCause::_no_gc, false);
  }

  const char* expected[] = {
    // [2.975s][debug][gc,start] Test GC
    "[gc,start", "] Test GC",
    // [2.975s][debug][gc      ] Test GC 0.026ms
    "[gc", "] Test GC ", "ms",
    NULL
  };
  EXPECT_TRUE(file_contains_substrings_in_order(TestLogFileName, expected));

  const char* not_expected[] = {
      // [2.975s][debug][gc      ] Test GC 59M->59M(502M) 0.026ms
      "[gc", "] Test GC ", "M) ", "ms",
  };
  EXPECT_FALSE(file_contains_substrings_in_order(TestLogFileName, not_expected));
}
