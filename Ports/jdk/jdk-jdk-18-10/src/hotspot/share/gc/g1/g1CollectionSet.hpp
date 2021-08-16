/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1COLLECTIONSET_HPP
#define SHARE_GC_G1_G1COLLECTIONSET_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class G1CollectedHeap;
class G1CollectionSetCandidates;
class G1CollectorState;
class G1GCPhaseTimes;
class G1ParScanThreadStateSet;
class G1Policy;
class G1SurvivorRegions;
class HeapRegion;
class HeapRegionClaimer;
class HeapRegionClosure;

// The collection set.
//
// The set of regions that are evacuated during an evacuation pause.
//
// At the end of a collection, before freeing the collection set, this set
// contains all regions that were evacuated during this collection:
//
// - survivor regions from the last collection (if any)
// - eden regions allocated by the mutator
// - old gen regions evacuated during mixed gc
//
// This set is built incrementally at mutator time as regions are retired, and
// if this had been a mixed gc, some additional (during gc) incrementally added
// old regions from the collection set candidates built during the concurrent
// cycle.
//
// A more detailed overview of how the collection set changes over time follows:
//
// 0) at the end of GC the survivor regions are added to this collection set.
// 1) the mutator incrementally adds eden regions as they retire
//
// ----- gc starts
//
// 2) prepare (finalize) young regions of the collection set for collection
//    - relabel the survivors as eden
//    - finish up the incremental building that happened at mutator time
//
// iff this is a young-only collection:
//
// a3) evacuate the current collection set in one "initial evacuation" phase
//
// iff this is a mixed collection:
//
// b3) calculate the set of old gen regions we may be able to collect in this
//     collection from the list of collection set candidates.
//     - one part is added to the current collection set
//     - the remainder regions are labeled as optional, and NOT yet added to the
//     collection set.
// b4) evacuate the current collection set in the "initial evacuation" phase
// b5) evacuate the optional regions in the "optional evacuation" phase. This is
//     done in increments (or rounds).
//     b5-1) add a few of the optional regions to the current collection set
//     b5-2) evacuate only these newly added optional regions. For this mechanism we
//     reuse the incremental collection set building infrastructure (used also at
//     mutator time).
//     b5-3) repeat from b5-1 until the policy determines we are done
//
// all collections
//
// 6) free the collection set (contains all regions now; empties collection set
//    afterwards)
// 7) add survivors to this collection set
//
// ----- gc ends
//
// goto 1)
//
// Examples of how the collection set might look over time:
//
// Legend:
// S = survivor, E = eden, O = old.
// |xxxx| = increment (with increment markers), containing four regions
//
// |SSSS|                         ... after step 0), with four survivor regions
// |SSSSEE|                       ... at step 1), after retiring two eden regions
// |SSSSEEEE|                     ... after step 1), after retiring four eden regions
// |EEEEEEEE|                     ... after step 2)
//
// iff this is a young-only collection
//
// EEEEEEEE||                      ... after step a3), after initial evacuation phase
// ||                              ... after step 6)
// |SS|                            ... after step 7), with two survivor regions
//
// iff this is a mixed collection
//
// |EEEEEEEEOOOO|                  ... after step b3), added four regions to be
//                                     evacuated in the "initial evacuation" phase
// EEEEEEEEOOOO||                  ... after step b4), incremental part is empty
//                                     after evacuation
// EEEEEEEEOOOO|OO|                ... after step b5.1), added two regions to be
//                                     evacuated in the first round of the
//                                     "optional evacuation" phase
// EEEEEEEEOOOOOO|O|               ... after step b5.1), added one region to be
//                                     evacuated in the second round of the
//                                     "optional evacuation" phase
// EEEEEEEEOOOOOOO||               ... after step b5), the complete collection set.
// ||                              ... after step b6)
// |SSS|                           ... after step 7), with three survivor regions
//
class G1CollectionSet {
  G1CollectedHeap* _g1h;
  G1Policy* _policy;

  // All old gen collection set candidate regions for the current mixed phase.
  G1CollectionSetCandidates* _candidates;

  uint _eden_region_length;
  uint _survivor_region_length;
  uint _old_region_length;

  // The actual collection set as a set of region indices.
  // All entries in _collection_set_regions below _collection_set_cur_length are
  // assumed to be part of the collection set.
  // We assume that at any time there is at most only one writer and (one or more)
  // concurrent readers. This means we are good with using storestore and loadload
  // barriers on the writer and reader respectively only.
  uint* _collection_set_regions;
  volatile size_t _collection_set_cur_length;
  size_t _collection_set_max_length;

  // When doing mixed collections we can add old regions to the collection set, which
  // will be collected only if there is enough time. We call these optional regions.
  // This member records the current number of regions that are of that type that
  // correspond to the first x entries in the collection set candidates.
  uint _num_optional_regions;

  // The number of bytes in the collection set before the pause. Set from
  // the incrementally built collection set at the start of an evacuation
  // pause, and updated as more regions are added to the collection set.
  size_t _bytes_used_before;

  // The number of cards in the remembered set in the collection set. Set from
  // the incrementally built collection set at the start of an evacuation
  // pause, and updated as more regions are added to the collection set.
  size_t _recorded_rs_length;

  enum CSetBuildType {
    Active,             // We are actively building the collection set
    Inactive            // We are not actively building the collection set
  };

  CSetBuildType _inc_build_state;
  size_t _inc_part_start;

  // Information about eden regions in the incremental collection set.
  struct IncCollectionSetRegionStat {
    // The predicted non-copy time that was added to the total incremental value
    // for the collection set.
    double _non_copy_time_ms;
    // The remembered set length that was added to the total incremental value
    // for the collection set.
    size_t _rs_length;

#ifdef ASSERT
    // Resets members to "uninitialized" values.
    void reset() { _rs_length = ~(size_t)0; _non_copy_time_ms = -1.0; }
#endif
  };

  IncCollectionSetRegionStat* _inc_collection_set_stats;
  // The associated information that is maintained while the incremental
  // collection set is being built with *young* regions. Used to populate
  // the recorded info for the evacuation pause.

  // The number of bytes in the incrementally built collection set.
  // Used to set _collection_set_bytes_used_before at the start of
  // an evacuation pause.
  size_t _inc_bytes_used_before;

  // The RSet lengths recorded for regions in the CSet. It is updated
  // by the thread that adds a new region to the CSet. We assume that
  // only one thread can be allocating a new CSet region (currently,
  // it does so after taking the Heap_lock) hence no need to
  // synchronize updates to this field.
  size_t _inc_recorded_rs_length;

  // A concurrent refinement thread periodically samples the young
  // region RSets and needs to update _inc_recorded_rs_length as
  // the RSets grow. Instead of having to synchronize updates to that
  // field we accumulate them in this field and add it to
  // _inc_recorded_rs_length_diff at the start of a GC.
  size_t _inc_recorded_rs_length_diff;

  // The predicted elapsed time it will take to collect the regions in
  // the CSet. This is updated by the thread that adds a new region to
  // the CSet. See the comment for _inc_recorded_rs_length about
  // MT-safety assumptions.
  double _inc_predicted_non_copy_time_ms;

  // See the comment for _inc_recorded_rs_length_diff.
  double _inc_predicted_non_copy_time_ms_diff;

  void set_recorded_rs_length(size_t rs_length);

  G1CollectorState* collector_state() const;
  G1GCPhaseTimes* phase_times();

  void verify_young_cset_indices() const NOT_DEBUG_RETURN;

  double predict_region_non_copy_time_ms(HeapRegion* hr) const;

  // Update the incremental collection set information when adding a region.
  void add_young_region_common(HeapRegion* hr);

  // Add old region "hr" to the collection set.
  void add_old_region(HeapRegion* hr);
  void free_optional_regions();

  // Add old region "hr" to optional collection set.
  void add_optional_region(HeapRegion* hr);

  void move_candidates_to_collection_set(uint num_regions);

  // Finalize the young part of the initial collection set. Relabel survivor regions
  // as Eden and calculate a prediction on how long the evacuation of all young regions
  // will take.
  double finalize_young_part(double target_pause_time_ms, G1SurvivorRegions* survivors);
  // Perform any final calculations on the incremental collection set fields before we
  // can use them.
  void finalize_incremental_building();

  // Select the old regions of the initial collection set and determine how many optional
  // regions we might be able to evacuate in this pause.
  void finalize_old_part(double time_remaining_ms);

  // Iterate the part of the collection set given by the offset and length applying the given
  // HeapRegionClosure. The worker_id will determine where in the part to start the iteration
  // to allow for more efficient parallel iteration.
  void iterate_part_from(HeapRegionClosure* cl,
                         HeapRegionClaimer* hr_claimer,
                         size_t offset,
                         size_t length,
                         uint worker_id,
                         uint total_workers) const;
public:
  G1CollectionSet(G1CollectedHeap* g1h, G1Policy* policy);
  ~G1CollectionSet();

  // Initializes the collection set giving the maximum possible length of the collection set.
  void initialize(uint max_region_length);

  void clear_candidates();
  bool has_candidates();

  void set_candidates(G1CollectionSetCandidates* candidates) {
    assert(_candidates == NULL, "Trying to replace collection set candidates.");
    _candidates = candidates;
  }
  G1CollectionSetCandidates* candidates() { return _candidates; }

  void init_region_lengths(uint eden_cset_region_length,
                           uint survivor_cset_region_length);

  uint region_length() const       { return young_region_length() +
                                            old_region_length(); }
  uint young_region_length() const { return eden_region_length() +
                                            survivor_region_length(); }

  uint eden_region_length() const     { return _eden_region_length;     }
  uint survivor_region_length() const { return _survivor_region_length; }
  uint old_region_length() const      { return _old_region_length;      }
  uint optional_region_length() const { return _num_optional_regions; }

  // Reset the contents of the collection set.
  void clear();

  // Incremental collection set support

  // Initialize incremental collection set info.
  void start_incremental_building();
  // Start a new collection set increment.
  void update_incremental_marker() { _inc_build_state = Active; _inc_part_start = _collection_set_cur_length; }
  // Stop adding regions to the current collection set increment.
  void stop_incremental_building() { _inc_build_state = Inactive; }

  // Iterate over the current collection set increment applying the given HeapRegionClosure
  // from a starting position determined by the given worker id.
  void iterate_incremental_part_from(HeapRegionClosure* cl, HeapRegionClaimer* hr_claimer, uint worker_id, uint total_workers) const;

  // Returns the length of the current increment in number of regions.
  size_t increment_length() const { return _collection_set_cur_length - _inc_part_start; }
  // Returns the length of the whole current collection set in number of regions
  size_t cur_length() const { return _collection_set_cur_length; }

  // Iterate over the entire collection set (all increments calculated so far), applying
  // the given HeapRegionClosure on all of them.
  void iterate(HeapRegionClosure* cl) const;
  void par_iterate(HeapRegionClosure* cl,
                   HeapRegionClaimer* hr_claimer,
                   uint worker_id,
                   uint total_workers) const;

  void iterate_optional(HeapRegionClosure* cl) const;

  size_t recorded_rs_length() { return _recorded_rs_length; }

  size_t bytes_used_before() const {
    return _bytes_used_before;
  }

  void reset_bytes_used_before() {
    _bytes_used_before = 0;
  }

  // Finalize the initial collection set consisting of all young regions potentially a
  // few old gen regions.
  void finalize_initial_collection_set(double target_pause_time_ms, G1SurvivorRegions* survivor);
  // Finalize the next collection set from the set of available optional old gen regions.
  bool finalize_optional_for_evacuation(double remaining_pause_time);
  // Abandon (clean up) optional collection set regions that were not evacuated in this
  // pause.
  void abandon_optional_collection_set(G1ParScanThreadStateSet* pss);

  // Update information about hr in the aggregated information for
  // the incrementally built collection set.
  void update_young_region_prediction(HeapRegion* hr, size_t new_rs_length);

  // Add eden region to the collection set.
  void add_eden_region(HeapRegion* hr);

  // Add survivor region to the collection set.
  void add_survivor_regions(HeapRegion* hr);

#ifndef PRODUCT
  bool verify_young_ages();

  void print(outputStream* st);
#endif // !PRODUCT
};

#endif // SHARE_GC_G1_G1COLLECTIONSET_HPP
