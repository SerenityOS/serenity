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

#ifndef SHARE_GC_G1_G1CONCURRENTMARKTHREAD_HPP
#define SHARE_GC_G1_G1CONCURRENTMARKTHREAD_HPP

#include "gc/shared/concurrentGCThread.hpp"

class G1ConcurrentMark;
class G1Policy;

// The concurrent mark thread triggers the various steps of the concurrent marking
// cycle, including various marking cleanup.
class G1ConcurrentMarkThread: public ConcurrentGCThread {
  friend class VMStructs;

  double _vtime_start;  // Initial virtual time.
  double _vtime_accum;  // Accumulated virtual time.

  G1ConcurrentMark* _cm;

  enum ServiceState : uint {
    Idle,
    FullMark,
    UndoMark
  };

  volatile ServiceState _state;

  // Wait for next cycle. Returns the command passed over.
  bool wait_for_next_cycle();

  bool mark_loop_needs_restart() const;

  // Phases and subphases for the full concurrent marking cycle in order.
  //
  // All these methods return true if the marking should be aborted. Except
  // phase_clear_cld_claimed_marks() because we must not abort before
  // scanning the root regions because of a potential deadlock otherwise.
  void phase_clear_cld_claimed_marks();
  bool phase_scan_root_regions();

  bool phase_mark_loop();
  bool subphase_mark_from_roots();
  bool subphase_preclean();
  bool subphase_delay_to_keep_mmu_before_remark();
  bool subphase_remark();

  bool phase_rebuild_remembered_sets();
  bool phase_delay_to_keep_mmu_before_cleanup();
  bool phase_cleanup();
  bool phase_clear_bitmap_for_next_mark();

  void concurrent_cycle_start();

  void concurrent_mark_cycle_do();
  void concurrent_undo_cycle_do();

  void concurrent_cycle_end(bool mark_cycle_completed);

  // Delay pauses to meet MMU.
  void delay_to_keep_mmu(bool remark);
  double mmu_delay_end(G1Policy* policy, bool remark);

  void run_service();
  void stop_service();

 public:
  // Constructor
  G1ConcurrentMarkThread(G1ConcurrentMark* cm);

  // Total virtual time so far for this thread and concurrent marking tasks.
  double vtime_accum();
  // Marking virtual time so far this thread and concurrent marking tasks.
  double vtime_mark_accum();

  G1ConcurrentMark* cm() { return _cm; }

  void set_idle();
  void start_full_mark();
  void start_undo_mark();

  bool idle() const;
  // Returns true from the moment a concurrent cycle is
  // initiated (during the concurrent start pause when started() is set)
  // to the moment when the cycle completes (just after the next
  // marking bitmap has been cleared and in_progress() is
  // cleared).
  bool in_progress() const;
};

#endif // SHARE_GC_G1_G1CONCURRENTMARKTHREAD_HPP
