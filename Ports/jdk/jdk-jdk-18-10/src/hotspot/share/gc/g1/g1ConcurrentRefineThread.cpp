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

#include "precompiled.hpp"
#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1ConcurrentRefine.hpp"
#include "gc/g1/g1ConcurrentRefineStats.hpp"
#include "gc/g1/g1ConcurrentRefineThread.hpp"
#include "gc/g1/g1DirtyCardQueue.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "runtime/thread.hpp"

G1ConcurrentRefineThread::G1ConcurrentRefineThread(G1ConcurrentRefine* cr, uint worker_id) :
  ConcurrentGCThread(),
  _vtime_start(0.0),
  _vtime_accum(0.0),
  _refinement_stats(new G1ConcurrentRefineStats()),
  _worker_id(worker_id),
  _notifier(new Semaphore(0)),
  _should_notify(true),
  _cr(cr)
{
  // set name
  set_name("G1 Refine#%d", worker_id);
  create_and_start();
}

G1ConcurrentRefineThread::~G1ConcurrentRefineThread() {
  delete _refinement_stats;
  delete _notifier;
}

void G1ConcurrentRefineThread::wait_for_completed_buffers() {
  assert(this == Thread::current(), "precondition");
  while (Atomic::load_acquire(&_should_notify)) {
    _notifier->wait();
  }
}

void G1ConcurrentRefineThread::activate() {
  assert(this != Thread::current(), "precondition");
  // Notify iff transitioning from needing activation to not.  This helps
  // keep the semaphore count bounded and minimizes the work done by
  // activators when the thread is already active.
  if (Atomic::load_acquire(&_should_notify) &&
      Atomic::cmpxchg(&_should_notify, true, false)) {
    _notifier->signal();
  }
}

bool G1ConcurrentRefineThread::maybe_deactivate(bool more_work) {
  assert(this == Thread::current(), "precondition");

  if (more_work) {
    // Suppress unnecessary notifications.
    Atomic::release_store(&_should_notify, false);
    return false;
  } else if (Atomic::load_acquire(&_should_notify)) {
    // Deactivate if no notifications since enabled (see below).
    return true;
  } else {
    // Try for more refinement work with notifications enabled, to close
    // race; there could be a plethora of suppressed activation attempts
    // after we found no work but before we enable notifications here
    // (so there could be lots of work for this thread to do), followed
    // by a long time without activation after enabling notifications.
    // But first, clear any pending signals to prevent accumulation.
    while (_notifier->trywait()) {}
    Atomic::release_store(&_should_notify, true);
    return false;
  }
}

void G1ConcurrentRefineThread::run_service() {
  _vtime_start = os::elapsedVTime();

  while (!should_terminate()) {
    // Wait for work
    wait_for_completed_buffers();
    if (should_terminate()) {
      break;
    }

    log_debug(gc, refine)("Activated worker %d, on threshold: " SIZE_FORMAT ", current: " SIZE_FORMAT,
                          _worker_id, _cr->activation_threshold(_worker_id),
                          G1BarrierSet::dirty_card_queue_set().num_cards());

    // For logging.
    G1ConcurrentRefineStats start_stats = *_refinement_stats;
    G1ConcurrentRefineStats total_stats; // Accumulate over activation.

    {
      SuspendibleThreadSetJoiner sts_join;

      while (!should_terminate()) {
        if (sts_join.should_yield()) {
          // Accumulate changed stats before possible GC that resets stats.
          total_stats += *_refinement_stats - start_stats;
          sts_join.yield();
          // Reinitialize baseline stats after safepoint.
          start_stats = *_refinement_stats;
          continue;             // Re-check for termination after yield delay.
        }

        bool more_work = _cr->do_refinement_step(_worker_id, _refinement_stats);
        if (maybe_deactivate(more_work)) break;
      }
    }

    total_stats += *_refinement_stats - start_stats;
    log_debug(gc, refine)("Deactivated worker %d, off threshold: " SIZE_FORMAT
                          ", current: " SIZE_FORMAT
                          ", refined cards: " SIZE_FORMAT,
                          _worker_id, _cr->deactivation_threshold(_worker_id),
                          G1BarrierSet::dirty_card_queue_set().num_cards(),
                          total_stats.refined_cards());

    if (os::supports_vtime()) {
      _vtime_accum = (os::elapsedVTime() - _vtime_start);
    } else {
      _vtime_accum = 0.0;
    }
  }

  log_debug(gc, refine)("Stopping %d", _worker_id);
}

void G1ConcurrentRefineThread::stop_service() {
  activate();
}
