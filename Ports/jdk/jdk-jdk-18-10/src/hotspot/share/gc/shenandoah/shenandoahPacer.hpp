/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHPACER_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHPACER_HPP

#include "gc/shenandoah/shenandoahNumberSeq.hpp"
#include "gc/shenandoah/shenandoahPadding.hpp"
#include "memory/allocation.hpp"

class ShenandoahHeap;

#define PACING_PROGRESS_UNINIT (-1)
#define PACING_PROGRESS_ZERO   ( 0)

/**
 * ShenandoahPacer provides allocation pacing mechanism.
 *
 * Currently it implements simple tax-and-spend pacing policy: GC threads provide
 * credit, allocating thread spend the credit, or stall when credit is not available.
 */
class ShenandoahPacer : public CHeapObj<mtGC> {
private:
  ShenandoahHeap* _heap;
  double _last_time;
  TruncatedSeq* _progress_history;
  Monitor* _wait_monitor;
  ShenandoahSharedFlag _need_notify_waiters;

  // Set once per phase
  volatile intptr_t _epoch;
  volatile double _tax_rate;

  // Heavily updated, protect from accidental false sharing
  shenandoah_padding(0);
  volatile intptr_t _budget;
  shenandoah_padding(1);

  // Heavily updated, protect from accidental false sharing
  shenandoah_padding(2);
  volatile intptr_t _progress;
  shenandoah_padding(3);

public:
  ShenandoahPacer(ShenandoahHeap* heap) :
          _heap(heap),
          _last_time(os::elapsedTime()),
          _progress_history(new TruncatedSeq(5)),
          _wait_monitor(new Monitor(Mutex::leaf, "_wait_monitor", true, Monitor::_safepoint_check_always)),
          _epoch(0),
          _tax_rate(1),
          _budget(0),
          _progress(PACING_PROGRESS_UNINIT) {}

  void setup_for_idle();
  void setup_for_mark();
  void setup_for_evac();
  void setup_for_updaterefs();

  void setup_for_reset();

  inline void report_mark(size_t words);
  inline void report_evac(size_t words);
  inline void report_updaterefs(size_t words);

  inline void report_alloc(size_t words);

  bool claim_for_alloc(size_t words, bool force);
  void pace_for_alloc(size_t words);
  void unpace_for_alloc(intptr_t epoch, size_t words);

  void notify_waiters();

  intptr_t epoch();

  void flush_stats_to_cycle();
  void print_cycle_on(outputStream* out);

private:
  inline void report_internal(size_t words);
  inline void report_progress_internal(size_t words);

  inline void add_budget(size_t words);
  void restart_with(size_t non_taxable_bytes, double tax_rate);

  size_t update_and_get_progress_history();

  void wait(size_t time_ms);
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHPACER_HPP
