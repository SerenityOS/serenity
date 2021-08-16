/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CONCURRENTREFINETHREAD_HPP
#define SHARE_GC_G1_G1CONCURRENTREFINETHREAD_HPP

#include "gc/shared/concurrentGCThread.hpp"
#include "utilities/ticks.hpp"

// Forward Decl.
class G1ConcurrentRefine;
class G1ConcurrentRefineStats;

// One or more G1 Concurrent Refinement Threads may be active if concurrent
// refinement is in progress.
class G1ConcurrentRefineThread: public ConcurrentGCThread {
  friend class VMStructs;
  friend class G1CollectedHeap;

  double _vtime_start;  // Initial virtual time.
  double _vtime_accum;  // Accumulated virtual time.

  G1ConcurrentRefineStats* _refinement_stats;

  uint _worker_id;

  // _notifier and _should_notify form a single-reader / multi-writer
  // notification mechanism.  The owning concurrent refinement thread is the
  // single reader. The writers are (other) threads that call activate() on
  // the thread.  The i-th concurrent refinement thread is responsible for
  // activating thread i+1 if the number of buffers in the queue exceeds a
  // threshold for that i+1th thread.  The 0th (primary) thread is activated
  // by threads that add cards to the dirty card queue set when the primary
  // thread's threshold is exceeded.  activate() is also used to wake up the
  // threads during termination, so even the non-primary thread case is
  // multi-writer.
  Semaphore* _notifier;
  volatile bool _should_notify;

  // Called when no refinement work found for this thread.
  // Returns true if should deactivate.
  bool maybe_deactivate(bool more_work);

  G1ConcurrentRefine* _cr;

  void wait_for_completed_buffers();

  virtual void run_service();
  virtual void stop_service();

public:
  G1ConcurrentRefineThread(G1ConcurrentRefine* cg1r, uint worker_id);
  virtual ~G1ConcurrentRefineThread();

  // Activate this thread.
  void activate();

  G1ConcurrentRefineStats* refinement_stats() const {
    return _refinement_stats;
  }

  // Total virtual time so far.
  double vtime_accum() { return _vtime_accum; }
};

#endif // SHARE_GC_G1_G1CONCURRENTREFINETHREAD_HPP
