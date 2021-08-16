/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

// This test performs mocking of certain JVM functionality. This works by
// including the source file under test inside an anonymous namespace (which
// prevents linking conflicts) with the mocked symbols redefined.

// The include list should mirror the one found in the included source file -
// with the ones that should pick up the mocks removed. Those should be included
// later after the mocks have been defined.

#include "logging/log.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jfr/utilities/jfrThreadIterator.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "utilities/globalDefinitions.hpp"
#include "runtime/os.hpp"

#include "unittest.hpp"

namespace {

  class MockEventThreadCPULoad : public ::EventThreadCPULoad
  {
  public:
    float user;
    float system;

  public:
    MockEventThreadCPULoad(EventStartTime timing=TIMED) : ::EventThreadCPULoad(timing) {}

    void set_user(float new_value) {
      user = new_value;
    }
    void set_system(float new_value) {
      system = new_value;
    }
  };

  class MockOs : public ::os {
  public:
    static jlong user_cpu_time;
    static jlong system_cpu_time;

    static jlong thread_cpu_time(Thread *thread, bool user_sys_cpu_time) {
      return user_sys_cpu_time ? user_cpu_time + system_cpu_time : user_cpu_time;
    }
  };

  jlong MockOs::user_cpu_time;
  jlong MockOs::system_cpu_time;

  class MockJavaThread : public ::JavaThread {
  public:
    MockJavaThread() : ::JavaThread() {}
  };

  class MockJfrJavaThreadIterator
  {
  public:
    MockJavaThread* next() { return NULL; }
    bool has_next() const { return false; }
  };

  class MockJfrJavaThreadIteratorAdapter
  {
  public:
    MockJavaThread* next() { return NULL; }
    bool has_next() const { return false; }
  };

// Reincluding source files in the anonymous namespace unfortunately seems to
// behave strangely with precompiled headers (only when using gcc though)
#ifndef DONT_USE_PRECOMPILED_HEADER
#define DONT_USE_PRECOMPILED_HEADER
#endif

#define os MockOs
#define EventThreadCPULoad MockEventThreadCPULoad
#define JavaThread MockJavaThread
#define JfrJavaThreadIterator MockJfrJavaThreadIterator
#define JfrJavaThreadIteratorAdapter MockJfrJavaThreadIteratorAdapter

#include "jfr/periodic/jfrThreadCPULoadEvent.hpp"
#include "jfr/periodic/jfrThreadCPULoadEvent.cpp"

#undef os
#undef EventThreadCPULoad
#undef JavaThread
#define JfrJavaThreadIterator MockJfrJavaThreadIterator
#define JfrJavaThreadIteratorAdapter MockJfrJavaThreadIteratorAdapter

} // anonymous namespace

class JfrTestThreadCPULoadSingle : public ::testing::Test {
protected:
  MockJavaThread* thread;
  JfrThreadLocal* thread_data;
  MockEventThreadCPULoad event;

  void SetUp() {
    thread = new MockJavaThread();
    thread_data = thread->jfr_thread_local();
    thread_data->set_wallclock_time(0);
    thread_data->set_user_time(0);
    thread_data->set_cpu_time(0);
  }

  void TearDown() {
    delete thread;
  }

  // Fix for gcc compilation warning about unused functions
  bool TouchUnused() {
    return (&JfrThreadCPULoadEvent::send_events &&
            &JfrThreadCPULoadEvent::send_event_for_thread);
  }
};

TEST_VM_F(JfrTestThreadCPULoadSingle, SingleCpu) {
  MockOs::user_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 400 * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0.25, event.system);

  MockOs::user_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.125, event.user);
  EXPECT_FLOAT_EQ(0.125, event.system);
}

TEST_VM_F(JfrTestThreadCPULoadSingle, MultipleCpus) {
  MockOs::user_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 400 * NANOSECS_PER_MILLISEC, 2));
  EXPECT_FLOAT_EQ(0.125, event.user);
  EXPECT_FLOAT_EQ(0.125, event.system);
}

TEST_VM_F(JfrTestThreadCPULoadSingle, BelowThreshold) {
  MockOs::user_cpu_time = 100;
  MockOs::system_cpu_time = 100;
  EXPECT_FALSE(JfrThreadCPULoadEvent::update_event(event, thread, 400 * NANOSECS_PER_MILLISEC, 2));
}

TEST_VM_F(JfrTestThreadCPULoadSingle, UserAboveMaximum) {

  // First call will not report above 100%
  MockOs::user_cpu_time = 200 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 200 * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.5, event.user);
  EXPECT_FLOAT_EQ(0.5, event.system);

  // Second call will see an extra 100 millisecs user time from the remainder
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (200 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0, event.system);

  // Third call: make sure there are no leftovers
  MockOs::user_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (200 + 400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.125, event.user);
  EXPECT_FLOAT_EQ(0.125, event.system);
}

TEST_VM_F(JfrTestThreadCPULoadSingle, UserAboveMaximumNonZeroBase) {

  // Setup a non zero base
  // Previously there was a bug when cur_user_time would be reset to zero and test that uses zero base would fail to detect it
  MockOs::user_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 400 * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0.25, event.system);

  // First call will not report above 100%
  MockOs::user_cpu_time += 200 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 200) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.5, event.user);
  EXPECT_FLOAT_EQ(0.5, event.system);

  // Second call will see an extra 100 millisecs user time from the remainder
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 200 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0, event.system);

  // Third call: make sure there are no leftovers
  MockOs::user_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 200 + 400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.125, event.user);
  EXPECT_FLOAT_EQ(0.125, event.system);
}

TEST_VM_F(JfrTestThreadCPULoadSingle, SystemAboveMaximum) {

  // First call will not report above 100%
  MockOs::user_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 300 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 200 * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0, event.user);
  EXPECT_FLOAT_EQ(1, event.system);

  // Second call will see an extra 100 millisecs user and system time from the remainder
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (200 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0.25, event.system);

  // Third call: make sure there are no leftovers
  MockOs::user_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (200 + 400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.125, event.user);
  EXPECT_FLOAT_EQ(0.125, event.system);
}

TEST_VM_F(JfrTestThreadCPULoadSingle, SystemAboveMaximumNonZeroBase) {

  // Setup a non zero base
  // Previously there was a bug when cur_user_time would be reset to zero and test that uses zero base would fail to detect it
  MockOs::user_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 400 * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0.25, event.system);

  // First call will not report above 100%
  MockOs::user_cpu_time += 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 300 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 200) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0, event.user);
  EXPECT_FLOAT_EQ(1, event.system);

  // Second call will see an extra 100 millisecs user and system time from the remainder
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 200 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0.25, event.system);

  // Third call: make sure there are no leftovers
  MockOs::user_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 50 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 200 + 400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.125, event.user);
  EXPECT_FLOAT_EQ(0.125, event.system);
}

TEST_VM_F(JfrTestThreadCPULoadSingle, SystemTimeDecreasing) {

  // As seen in an actual run - caused by different resolution for total and user time
  // Total time    User time    (Calculated system time)
  //       200          100         100
  //       210          200          10
  //       400          300         100

  MockOs::user_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time = 100 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, 400 * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0.25, event.system);

  MockOs::user_cpu_time += 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time -= 90 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0, event.system);

  MockOs::user_cpu_time += 100 * NANOSECS_PER_MILLISEC;
  MockOs::system_cpu_time += 90 * NANOSECS_PER_MILLISEC;
  EXPECT_TRUE(JfrThreadCPULoadEvent::update_event(event, thread, (400 + 400 + 400) * NANOSECS_PER_MILLISEC, 1));
  EXPECT_FLOAT_EQ(0.25, event.user);
  EXPECT_FLOAT_EQ(0, event.system);
}
