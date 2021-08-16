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
 */

#ifndef GTEST_CONCURRENT_TEST_RUNNER_INLINE_HPP
#define GTEST_CONCURRENT_TEST_RUNNER_INLINE_HPP

#include "memory/allocation.hpp"
#include "runtime/semaphore.hpp"
#include "runtime/thread.inline.hpp"
#include "threadHelper.inline.hpp"

// This file contains helper classes to run unit tests concurrently in multiple threads.

// Base class for test runnable. Override runUnitTest() to specify what to run.
class TestRunnable {
public:
  virtual void runUnitTest() const = 0;
};

// This class represents a thread for a unit test.
class UnitTestThread : public JavaTestThread {
public:
  // runnableArg - what to run
  // doneArg - a semaphore to notify when the thread is done running
  // testDurationArg - how long to run (in milliseconds)
  UnitTestThread(TestRunnable* const runnableArg, Semaphore* doneArg, const long testDurationArg) :
    JavaTestThread(doneArg), runnable(runnableArg), testDuration(testDurationArg) {}

  // from JavaTestThread
  void main_run() {
    long stopTime = os::javaTimeMillis() + testDuration;
    while (os::javaTimeMillis() < stopTime) {
      runnable->runUnitTest();
    }
  }
private:
  TestRunnable* const runnable;
  const long testDuration;
};

// Helper class for running a given unit test concurrently in multiple threads.
class ConcurrentTestRunner {
public:
  // runnableArg - what to run
  // nrOfThreadsArg - how many threads to use concurrently
  // testDurationMillisArg - duration for each test run
  ConcurrentTestRunner(TestRunnable* const runnableArg, int nrOfThreadsArg, long testDurationMillisArg) :
    unitTestRunnable(runnableArg),
    nrOfThreads(nrOfThreadsArg),
    testDurationMillis(testDurationMillisArg) {}

  void run() {
    Semaphore done(0);

    UnitTestThread** t = NEW_C_HEAP_ARRAY(UnitTestThread*, nrOfThreads, mtInternal);

    for (int i = 0; i < nrOfThreads; i++) {
      t[i] = new UnitTestThread(unitTestRunnable, &done, testDurationMillis);
    }

    for (int i = 0; i < nrOfThreads; i++) {
      t[i]->doit();
    }

    for (int i = 0; i < nrOfThreads; i++) {
      done.wait();
    }

    FREE_C_HEAP_ARRAY(UnitTestThread**, t);
  }

private:
  TestRunnable* const unitTestRunnable;
  const int nrOfThreads;
  const long testDurationMillis;
};

#endif // GTEST_CONCURRENT_TEST_RUNNER_INLINE_HPP
