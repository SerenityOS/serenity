/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_SUSPENDIBLETHREADSET_HPP
#define SHARE_GC_SHARED_SUSPENDIBLETHREADSET_HPP

#include "memory/allocation.hpp"

// A SuspendibleThreadSet is a set of threads that can be suspended.
// A thread can join and later leave the set, and periodically yield.
// If some thread (not in the set) requests, via synchronize(), that
// the threads be suspended, then the requesting thread is blocked
// until all the threads in the set have yielded or left the set. Threads
// may not enter the set when an attempted suspension is in progress. The
// suspending thread later calls desynchronize(), allowing the suspended
// threads to continue.
class SuspendibleThreadSet : public AllStatic {
  friend class SuspendibleThreadSetJoiner;
  friend class SuspendibleThreadSetLeaver;

private:
  static uint   _nthreads;
  static uint   _nthreads_stopped;
  static bool   _suspend_all;
  static double _suspend_all_start;

  static bool is_synchronized();

  // Add the current thread to the set. May block if a suspension is in progress.
  static void join();

  // Removes the current thread from the set.
  static void leave();

public:
  // Returns true if an suspension is in progress.
  static bool should_yield() { return _suspend_all; }

  // Suspends the current thread if a suspension is in progress.
  static void yield();

  // Returns when all threads in the set are suspended.
  static void synchronize();

  // Resumes all suspended threads in the set.
  static void desynchronize();
};

class SuspendibleThreadSetJoiner : public StackObj {
private:
  bool _active;

public:
  SuspendibleThreadSetJoiner(bool active = true) : _active(active) {
    if (_active) {
      SuspendibleThreadSet::join();
    }
  }

  ~SuspendibleThreadSetJoiner() {
    if (_active) {
      SuspendibleThreadSet::leave();
    }
  }

  bool should_yield() {
    if (_active) {
      return SuspendibleThreadSet::should_yield();
    } else {
      return false;
    }
  }

  void yield() {
    assert(_active, "Thread has not joined the suspendible thread set");
    SuspendibleThreadSet::yield();
  }
};

class SuspendibleThreadSetLeaver : public StackObj {
private:
  bool _active;

public:
  SuspendibleThreadSetLeaver(bool active = true) : _active(active) {
    if (_active) {
      SuspendibleThreadSet::leave();
    }
  }

  ~SuspendibleThreadSetLeaver() {
    if (_active) {
      SuspendibleThreadSet::join();
    }
  }
};

#endif // SHARE_GC_SHARED_SUSPENDIBLETHREADSET_HPP
