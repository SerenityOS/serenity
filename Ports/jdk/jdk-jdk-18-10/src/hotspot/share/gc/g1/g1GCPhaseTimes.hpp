/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1GCPHASETIMES_HPP
#define SHARE_GC_G1_G1GCPHASETIMES_HPP

#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/referenceProcessorPhaseTimes.hpp"
#include "gc/shared/weakProcessorTimes.hpp"
#include "logging/logLevel.hpp"
#include "memory/allocation.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/macros.hpp"

class LineBuffer;
class G1ParScanThreadState;
class STWGCTimer;

template <class T> class WorkerDataArray;

class G1GCPhaseTimes : public CHeapObj<mtGC> {
  uint _max_gc_threads;
  jlong _gc_start_counter;
  double _gc_pause_time_ms;

 public:
  enum GCParPhases {
    GCWorkerStart,
    ExtRootScan,
    ThreadRoots,
    CLDGRoots,
    CMRefRoots,
    // For every strong OopStorage there will be one element in this enum,
    // starting with StrongOopStorageSetRoots.
    StrongOopStorageSetRoots,
    MergeER = StrongOopStorageSetRoots + EnumRange<OopStorageSet::StrongId>().size(),
    MergeRS,
    OptMergeRS,
    MergeLB,
    MergeHCC,
    ScanHR,
    OptScanHR,
    CodeRoots,
    OptCodeRoots,
    ObjCopy,
    OptObjCopy,
    Termination,
    OptTermination,
    Other,
    GCWorkerTotal,
    GCWorkerEnd,
    RedirtyCards,
    FreeCollectionSet,
    YoungFreeCSet,
    NonYoungFreeCSet,
    RebuildFreeList,
    SampleCollectionSetCandidates,
    MergePSS,
    RemoveSelfForwardingPtr,
    ClearCardTable,
    RecalculateUsed,
    ResetHotCardCache,
    PurgeCodeRoots,
#if COMPILER2_OR_JVMCI
    UpdateDerivedPointers,
#endif
    EagerlyReclaimHumongousObjects,
    RestorePreservedMarks,
    GCParPhasesSentinel
  };

  static const GCParPhases ExtRootScanSubPhasesFirst = ThreadRoots;
  static const GCParPhases ExtRootScanSubPhasesLast = GCParPhases(MergeER - 1);

  static constexpr GCParPhases strong_oopstorage_phase(OopStorageSet::StrongId id) {
    size_t index = EnumRange<OopStorageSet::StrongId>().index(id);
    return GCParPhases(StrongOopStorageSetRoots + index);
  }

  enum GCMergeRSWorkItems : uint {
    MergeRSMergedInline = 0,
    MergeRSMergedArrayOfCards,
    MergeRSMergedHowl,
    MergeRSMergedFull,
    MergeRSHowlInline,
    MergeRSHowlArrayOfCards,
    MergeRSHowlBitmap,
    MergeRSHowlFull,
    MergeRSDirtyCards,
    MergeRSContainersSentinel
  };

  static constexpr const char* GCMergeRSWorkItemsStrings[MergeRSContainersSentinel] =
    { "Merged Inline", "Merged ArrayOfCards", "Merged Howl", "Merged Full",
      "Merged Howl Inline", "Merged Howl ArrayOfCards", "Merged Howl BitMap", "Merged Howl Full",
      "Dirty Cards" };

  enum GCScanHRWorkItems {
    ScanHRScannedCards,
    ScanHRScannedBlocks,
    ScanHRClaimedChunks,
    ScanHRScannedOptRefs,
    ScanHRUsedMemory
  };

  enum GCMergeHCCWorkItems {
    MergeHCCDirtyCards,
    MergeHCCSkippedCards
  };

  enum GCMergeLBWorkItems {
    MergeLBDirtyCards,
    MergeLBSkippedCards
  };

  enum GCMergePSSWorkItems {
    MergePSSCopiedBytes,
    MergePSSLABWasteBytes,
    MergePSSLABUndoWasteBytes
  };

  enum GCEagerlyReclaimHumongousObjectsItems {
    EagerlyReclaimNumTotal,
    EagerlyReclaimNumCandidates,
    EagerlyReclaimNumReclaimed
  };

 private:
  // Markers for grouping the phases in the GCPhases enum above
  static const int GCMainParPhasesLast = GCWorkerEnd;

  WorkerDataArray<double>* _gc_par_phases[GCParPhasesSentinel];

  double _cur_collection_initial_evac_time_ms;
  double _cur_optional_evac_time_ms;
  double _cur_collection_code_root_fixup_time_ms;

  double _cur_merge_heap_roots_time_ms;
  double _cur_optional_merge_heap_roots_time_ms;

  double _cur_prepare_merge_heap_roots_time_ms;
  double _cur_optional_prepare_merge_heap_roots_time_ms;

  double _cur_prepare_tlab_time_ms;
  double _cur_resize_tlab_time_ms;

  double _cur_concatenate_dirty_card_logs_time_ms;

  double _cur_post_evacuate_cleanup_1_time_ms;
  double _cur_post_evacuate_cleanup_2_time_ms;

  double _cur_expand_heap_time_ms;
  double _cur_ref_proc_time_ms;

  double _cur_collection_start_sec;
  double _root_region_scan_wait_time_ms;

  double _external_accounted_time_ms;

  double _recorded_prepare_heap_roots_time_ms;

  double _recorded_clear_claimed_marks_time_ms;

  double _recorded_young_cset_choice_time_ms;
  double _recorded_non_young_cset_choice_time_ms;

  double _recorded_sample_collection_set_candidates_time_ms;

  double _recorded_preserve_cm_referents_time_ms;

  double _recorded_start_new_cset_time_ms;

  double _recorded_serial_free_cset_time_ms;

  double _recorded_total_rebuild_freelist_time_ms;

  double _recorded_serial_rebuild_freelist_time_ms;

  double _cur_region_register_time;

  double _cur_verify_before_time_ms;
  double _cur_verify_after_time_ms;

  ReferenceProcessorPhaseTimes _ref_phase_times;
  WeakProcessorTimes _weak_phase_times;

  double worker_time(GCParPhases phase, uint worker);
  void reset();

  template <class T>
  void details(T* phase, uint indent_level) const;

  void log_work_items(WorkerDataArray<double>* phase, uint indent, outputStream* out) const;
  void log_phase(WorkerDataArray<double>* phase, uint indent_level, outputStream* out, bool print_sum) const;
  void debug_serial_phase(WorkerDataArray<double>* phase, uint extra_indent = 0) const;
  void debug_phase(WorkerDataArray<double>* phase, uint extra_indent = 0) const;
  void trace_phase(WorkerDataArray<double>* phase, bool print_sum = true, uint extra_indent = 0) const;

  void info_time(const char* name, double value) const;
  void debug_time(const char* name, double value) const;
  // This will print logs for both 'gc+phases' and 'gc+phases+ref'.
  void debug_time_for_reference(const char* name, double value) const;
  void trace_time(const char* name, double value) const;
  void trace_count(const char* name, size_t value) const;

  double print_pre_evacuate_collection_set() const;
  double print_merge_heap_roots_time() const;
  double print_evacuate_initial_collection_set() const;
  double print_evacuate_optional_collection_set() const;
  double print_post_evacuate_collection_set() const;
  void print_other(double accounted_ms) const;

 public:
  G1GCPhaseTimes(STWGCTimer* gc_timer, uint max_gc_threads);
  void record_gc_pause_start();
  void record_gc_pause_end();
  void print();
  static const char* phase_name(GCParPhases phase);

  // record the time a phase took in seconds
  void record_time_secs(GCParPhases phase, uint worker_id, double secs);

  // add a number of seconds to a phase
  void add_time_secs(GCParPhases phase, uint worker_id, double secs);

  void record_or_add_time_secs(GCParPhases phase, uint worker_id, double secs);

  double get_time_secs(GCParPhases phase, uint worker_id);

  void record_thread_work_item(GCParPhases phase, uint worker_id, size_t count, uint index = 0);

  void record_or_add_thread_work_item(GCParPhases phase, uint worker_id, size_t count, uint index = 0);

  size_t get_thread_work_item(GCParPhases phase, uint worker_id, uint index = 0);

  // return the average time for a phase in milliseconds
  double average_time_ms(GCParPhases phase);

  size_t sum_thread_work_items(GCParPhases phase, uint index = 0);

  void record_prepare_tlab_time_ms(double ms) {
    _cur_prepare_tlab_time_ms = ms;
  }

  void record_resize_tlab_time_ms(double ms) {
    _cur_resize_tlab_time_ms = ms;
  }

  void record_concatenate_dirty_card_logs_time_ms(double ms) {
    _cur_concatenate_dirty_card_logs_time_ms = ms;
  }

  void record_expand_heap_time(double ms) {
    _cur_expand_heap_time_ms = ms;
  }

  void record_initial_evac_time(double ms) {
    _cur_collection_initial_evac_time_ms = ms;
  }

  void record_or_add_optional_evac_time(double ms) {
    _cur_optional_evac_time_ms += ms;
  }

  void record_or_add_code_root_fixup_time(double ms) {
    _cur_collection_code_root_fixup_time_ms += ms;
  }

  void record_merge_heap_roots_time(double ms) {
    _cur_merge_heap_roots_time_ms += ms;
  }

  void record_or_add_optional_merge_heap_roots_time(double ms) {
    _cur_optional_merge_heap_roots_time_ms += ms;
  }

  void record_prepare_merge_heap_roots_time(double ms) {
    _cur_prepare_merge_heap_roots_time_ms += ms;
  }

  void record_or_add_optional_prepare_merge_heap_roots_time(double ms) {
    _cur_optional_prepare_merge_heap_roots_time_ms += ms;
  }

  void record_ref_proc_time(double ms) {
    _cur_ref_proc_time_ms = ms;
  }

  void record_root_region_scan_wait_time(double time_ms) {
    _root_region_scan_wait_time_ms = time_ms;
  }

  void record_serial_free_cset_time_ms(double time_ms) {
    _recorded_serial_free_cset_time_ms = time_ms;
  }

  void record_total_rebuild_freelist_time_ms(double time_ms) {
    _recorded_total_rebuild_freelist_time_ms = time_ms;
  }

  void record_serial_rebuild_freelist_time_ms(double time_ms) {
    _recorded_serial_rebuild_freelist_time_ms = time_ms;
  }

  void record_register_regions(double time_ms) {
    _cur_region_register_time = time_ms;
  }

  void record_post_evacuate_cleanup_task_1_time(double time_ms) {
    _cur_post_evacuate_cleanup_1_time_ms = time_ms;
  }

  void record_post_evacuate_cleanup_task_2_time(double time_ms) {
    _cur_post_evacuate_cleanup_2_time_ms = time_ms;
  }

  void record_young_cset_choice_time_ms(double time_ms) {
    _recorded_young_cset_choice_time_ms = time_ms;
  }

  void record_non_young_cset_choice_time_ms(double time_ms) {
    _recorded_non_young_cset_choice_time_ms = time_ms;
  }

  void record_sample_collection_set_candidates_time_ms(double time_ms) {
    _recorded_sample_collection_set_candidates_time_ms = time_ms;
  }

  void record_preserve_cm_referents_time_ms(double time_ms) {
    _recorded_preserve_cm_referents_time_ms = time_ms;
  }

  void record_start_new_cset_time_ms(double time_ms) {
    _recorded_start_new_cset_time_ms = time_ms;
  }

  void record_cur_collection_start_sec(double time_ms) {
    _cur_collection_start_sec = time_ms;
  }

  void record_verify_before_time_ms(double time_ms) {
    _cur_verify_before_time_ms = time_ms;
  }

  void record_verify_after_time_ms(double time_ms) {
    _cur_verify_after_time_ms = time_ms;
  }

  void inc_external_accounted_time_ms(double time_ms) {
    _external_accounted_time_ms += time_ms;
  }

  void record_prepare_heap_roots_time_ms(double recorded_prepare_heap_roots_time_ms) {
    _recorded_prepare_heap_roots_time_ms = recorded_prepare_heap_roots_time_ms;
  }

  void record_clear_claimed_marks_time_ms(double recorded_clear_claimed_marks_time_ms) {
    _recorded_clear_claimed_marks_time_ms = recorded_clear_claimed_marks_time_ms;
  }

  double cur_collection_start_sec() {
    return _cur_collection_start_sec;
  }

  double cur_collection_par_time_ms() {
    return _cur_collection_initial_evac_time_ms + _cur_optional_evac_time_ms;
  }

  double cur_expand_heap_time_ms() {
    return _cur_expand_heap_time_ms;
  }

  double root_region_scan_wait_time_ms() {
    return _root_region_scan_wait_time_ms;
  }

  double young_cset_choice_time_ms() {
    return _recorded_young_cset_choice_time_ms;
  }

  double total_rebuild_freelist_time_ms() {
    return _recorded_total_rebuild_freelist_time_ms;
  }

  double non_young_cset_choice_time_ms() {
    return _recorded_non_young_cset_choice_time_ms;
  }

  ReferenceProcessorPhaseTimes* ref_phase_times() { return &_ref_phase_times; }

  WeakProcessorTimes* weak_phase_times() { return &_weak_phase_times; }
};

class G1EvacPhaseWithTrimTimeTracker : public StackObj {
  G1ParScanThreadState* _pss;
  Ticks _start;

  Tickspan& _total_time;
  Tickspan& _trim_time;

  bool _stopped;
public:
  G1EvacPhaseWithTrimTimeTracker(G1ParScanThreadState* pss, Tickspan& total_time, Tickspan& trim_time);
  ~G1EvacPhaseWithTrimTimeTracker();

  void stop();
};

#endif // SHARE_GC_G1_G1GCPHASETIMES_HPP
