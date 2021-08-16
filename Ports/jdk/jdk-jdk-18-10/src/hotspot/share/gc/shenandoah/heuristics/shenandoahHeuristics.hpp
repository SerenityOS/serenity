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

#ifndef SHARE_GC_SHENANDOAH_HEURISTICS_SHENANDOAHHEURISTICS_HPP
#define SHARE_GC_SHENANDOAH_HEURISTICS_SHENANDOAHHEURISTICS_HPP

#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahSharedVariables.hpp"
#include "memory/allocation.hpp"
#include "runtime/globals_extension.hpp"

#define SHENANDOAH_ERGO_DISABLE_FLAG(name)                                  \
  do {                                                                      \
    if (FLAG_IS_DEFAULT(name) && (name)) {                                  \
      log_info(gc)("Heuristics ergonomically sets -XX:-" #name);            \
      FLAG_SET_DEFAULT(name, false);                                        \
    }                                                                       \
  } while (0)

#define SHENANDOAH_ERGO_ENABLE_FLAG(name)                                   \
  do {                                                                      \
    if (FLAG_IS_DEFAULT(name) && !(name)) {                                 \
      log_info(gc)("Heuristics ergonomically sets -XX:+" #name);            \
      FLAG_SET_DEFAULT(name, true);                                         \
    }                                                                       \
  } while (0)

#define SHENANDOAH_ERGO_OVERRIDE_DEFAULT(name, value)                       \
  do {                                                                      \
    if (FLAG_IS_DEFAULT(name)) {                                            \
      log_info(gc)("Heuristics ergonomically sets -XX:" #name "=" #value);  \
      FLAG_SET_DEFAULT(name, value);                                        \
    }                                                                       \
  } while (0)

class ShenandoahCollectionSet;
class ShenandoahHeapRegion;

class ShenandoahHeuristics : public CHeapObj<mtGC> {
  static const intx Concurrent_Adjust   = -1; // recover from penalties
  static const intx Degenerated_Penalty = 10; // how much to penalize average GC duration history on Degenerated GC
  static const intx Full_Penalty        = 20; // how much to penalize average GC duration history on Full GC

protected:
  typedef struct {
    ShenandoahHeapRegion* _region;
    size_t _garbage;
  } RegionData;

  RegionData* _region_data;

  uint _degenerated_cycles_in_a_row;
  uint _successful_cycles_in_a_row;

  double _cycle_start;
  double _last_cycle_end;

  size_t _gc_times_learned;
  intx _gc_time_penalties;
  TruncatedSeq* _gc_time_history;

  // There may be many threads that contend to set this flag
  ShenandoahSharedFlag _metaspace_oom;

  static int compare_by_garbage(RegionData a, RegionData b);

  virtual void choose_collection_set_from_regiondata(ShenandoahCollectionSet* set,
                                                     RegionData* data, size_t data_size,
                                                     size_t free) = 0;

  void adjust_penalty(intx step);

public:
  ShenandoahHeuristics();
  virtual ~ShenandoahHeuristics();

  void record_metaspace_oom()     { _metaspace_oom.set(); }
  void clear_metaspace_oom()      { _metaspace_oom.unset(); }
  bool has_metaspace_oom() const  { return _metaspace_oom.is_set(); }

  virtual void record_cycle_start();

  virtual void record_cycle_end();

  virtual bool should_start_gc();

  virtual bool should_degenerate_cycle();

  virtual void record_success_concurrent();

  virtual void record_success_degenerated();

  virtual void record_success_full();

  virtual void record_allocation_failure_gc();

  virtual void record_requested_gc();

  virtual void choose_collection_set(ShenandoahCollectionSet* collection_set);

  virtual bool can_unload_classes();
  virtual bool can_unload_classes_normal();
  virtual bool should_unload_classes();

  virtual const char* name() = 0;
  virtual bool is_diagnostic() = 0;
  virtual bool is_experimental() = 0;
  virtual void initialize();

  double time_since_last_gc() const;
};

#endif // SHARE_GC_SHENANDOAH_HEURISTICS_SHENANDOAHHEURISTICS_HPP
