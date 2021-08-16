/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1CollectionSetCandidates.hpp"
#include "gc/g1/g1CollectionSetChooser.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "gc/shared/space.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/quickSort.hpp"

// Order regions according to GC efficiency. This will cause regions with a lot
// of live objects and large remembered sets to end up at the end of the array.
// Given that we might skip collecting the last few old regions, if after a few
// mixed GCs the remaining have reclaimable bytes under a certain threshold, the
// hope is that the ones we'll skip are ones with both large remembered sets and
// a lot of live objects, not the ones with just a lot of live objects if we
// ordered according to the amount of reclaimable bytes per region.
static int order_regions(HeapRegion* hr1, HeapRegion* hr2) {
  // Make sure that NULL entries are moved to the end.
  if (hr1 == NULL) {
    if (hr2 == NULL) {
      return 0;
    } else {
      return 1;
    }
  } else if (hr2 == NULL) {
    return -1;
  }

  double gc_eff1 = hr1->gc_efficiency();
  double gc_eff2 = hr2->gc_efficiency();

  if (gc_eff1 > gc_eff2) {
    return -1;
  } if (gc_eff1 < gc_eff2) {
    return 1;
  } else {
    return 0;
  }
}

// Determine collection set candidates: For all regions determine whether they
// should be a collection set candidates, calculate their efficiency, sort and
// return them as G1CollectionSetCandidates instance.
// Threads calculate the GC efficiency of the regions they get to process, and
// put them into some work area unsorted. At the end the array is sorted and
// copied into the G1CollectionSetCandidates instance; the caller will be the new
// owner of this object.
class G1BuildCandidateRegionsTask : public AbstractGangTask {

  // Work area for building the set of collection set candidates. Contains references
  // to heap regions with their GC efficiencies calculated. To reduce contention
  // on claiming array elements, worker threads claim parts of this array in chunks;
  // Array elements may be NULL as threads might not get enough regions to fill
  // up their chunks completely.
  // Final sorting will remove them.
  class G1BuildCandidateArray : public StackObj {

    uint const _max_size;
    uint const _chunk_size;

    HeapRegion** _data;

    uint volatile _cur_claim_idx;

    // Calculates the maximum array size that will be used.
    static uint required_array_size(uint num_regions, uint chunk_size, uint num_workers) {
      uint const max_waste = num_workers * chunk_size;
      // The array should be aligned with respect to chunk_size.
      uint const aligned_num_regions = ((num_regions + chunk_size - 1) / chunk_size) * chunk_size;

      return aligned_num_regions + max_waste;
    }

  public:
    G1BuildCandidateArray(uint max_num_regions, uint chunk_size, uint num_workers) :
      _max_size(required_array_size(max_num_regions, chunk_size, num_workers)),
      _chunk_size(chunk_size),
      _data(NEW_C_HEAP_ARRAY(HeapRegion*, _max_size, mtGC)),
      _cur_claim_idx(0) {
      for (uint i = 0; i < _max_size; i++) {
        _data[i] = NULL;
      }
    }

    ~G1BuildCandidateArray() {
      FREE_C_HEAP_ARRAY(HeapRegion*, _data);
    }

    // Claim a new chunk, returning its bounds [from, to[.
    void claim_chunk(uint& from, uint& to) {
      uint result = Atomic::add(&_cur_claim_idx, _chunk_size);
      assert(_max_size > result - 1,
             "Array too small, is %u should be %u with chunk size %u.",
             _max_size, result, _chunk_size);
      from = result - _chunk_size;
      to = result;
    }

    // Set element in array.
    void set(uint idx, HeapRegion* hr) {
      assert(idx < _max_size, "Index %u out of bounds %u", idx, _max_size);
      assert(_data[idx] == NULL, "Value must not have been set.");
      _data[idx] = hr;
    }

    void sort_and_copy_into(HeapRegion** dest, uint num_regions) {
      if (_cur_claim_idx == 0) {
        return;
      }
      for (uint i = _cur_claim_idx; i < _max_size; i++) {
        assert(_data[i] == NULL, "must be");
      }
      QuickSort::sort(_data, _cur_claim_idx, order_regions, true);
      for (uint i = num_regions; i < _max_size; i++) {
        assert(_data[i] == NULL, "must be");
      }
      for (uint i = 0; i < num_regions; i++) {
        dest[i] = _data[i];
      }
    }
  };

  // Per-region closure. In addition to determining whether a region should be
  // added to the candidates, and calculating those regions' gc efficiencies, also
  // gather additional statistics.
  class G1BuildCandidateRegionsClosure : public HeapRegionClosure {
    G1BuildCandidateArray* _array;

    uint _cur_chunk_idx;
    uint _cur_chunk_end;

    uint _regions_added;
    size_t _reclaimable_bytes_added;

    void add_region(HeapRegion* hr) {
      if (_cur_chunk_idx == _cur_chunk_end) {
        _array->claim_chunk(_cur_chunk_idx, _cur_chunk_end);
      }
      assert(_cur_chunk_idx < _cur_chunk_end, "Must be");

      hr->calc_gc_efficiency();
      _array->set(_cur_chunk_idx, hr);

      _cur_chunk_idx++;

      _regions_added++;
      _reclaimable_bytes_added += hr->reclaimable_bytes();
    }

    bool should_add(HeapRegion* hr) { return G1CollectionSetChooser::should_add(hr); }

  public:
    G1BuildCandidateRegionsClosure(G1BuildCandidateArray* array) :
      _array(array),
      _cur_chunk_idx(0),
      _cur_chunk_end(0),
      _regions_added(0),
      _reclaimable_bytes_added(0) { }

    bool do_heap_region(HeapRegion* r) {
      // We will skip any region that's currently used as an old GC
      // alloc region (we should not consider those for collection
      // before we fill them up).
      if (should_add(r) && !G1CollectedHeap::heap()->is_old_gc_alloc_region(r)) {
        add_region(r);
      } else if (r->is_old()) {
        // Keep remembered sets for humongous regions, otherwise clean out remembered
        // sets for old regions.
        r->rem_set()->clear(true /* only_cardset */);
      } else {
        assert(r->is_archive() || !r->is_old() || !r->rem_set()->is_tracked(),
               "Missed to clear unused remembered set of region %u (%s) that is %s",
               r->hrm_index(), r->get_type_str(), r->rem_set()->get_state_str());
      }
      return false;
    }

    uint regions_added() const { return _regions_added; }
    size_t reclaimable_bytes_added() const { return _reclaimable_bytes_added; }
  };

  G1CollectedHeap* _g1h;
  HeapRegionClaimer _hrclaimer;

  uint volatile _num_regions_added;
  size_t volatile _reclaimable_bytes_added;

  G1BuildCandidateArray _result;

  void update_totals(uint num_regions, size_t reclaimable_bytes) {
    if (num_regions > 0) {
      assert(reclaimable_bytes > 0, "invariant");
      Atomic::add(&_num_regions_added, num_regions);
      Atomic::add(&_reclaimable_bytes_added, reclaimable_bytes);
    } else {
      assert(reclaimable_bytes == 0, "invariant");
    }
  }

public:
  G1BuildCandidateRegionsTask(uint max_num_regions, uint chunk_size, uint num_workers) :
    AbstractGangTask("G1 Build Candidate Regions"),
    _g1h(G1CollectedHeap::heap()),
    _hrclaimer(num_workers),
    _num_regions_added(0),
    _reclaimable_bytes_added(0),
    _result(max_num_regions, chunk_size, num_workers) { }

  void work(uint worker_id) {
    G1BuildCandidateRegionsClosure cl(&_result);
    _g1h->heap_region_par_iterate_from_worker_offset(&cl, &_hrclaimer, worker_id);
    update_totals(cl.regions_added(), cl.reclaimable_bytes_added());
  }

  G1CollectionSetCandidates* get_sorted_candidates() {
    HeapRegion** regions = NEW_C_HEAP_ARRAY(HeapRegion*, _num_regions_added, mtGC);
    _result.sort_and_copy_into(regions, _num_regions_added);
    return new G1CollectionSetCandidates(regions,
                                         _num_regions_added,
                                         _reclaimable_bytes_added);
  }
};

uint G1CollectionSetChooser::calculate_work_chunk_size(uint num_workers, uint num_regions) {
  assert(num_workers > 0, "Active gc workers should be greater than 0");
  return MAX2(num_regions / num_workers, 1U);
}

bool G1CollectionSetChooser::should_add(HeapRegion* hr) {
  return !hr->is_young() &&
         !hr->is_pinned() &&
         region_occupancy_low_enough_for_evac(hr->live_bytes()) &&
         hr->rem_set()->is_complete();
}

// Closure implementing early pruning (removal) of regions meeting the
// G1HeapWastePercent criteria. That is, either until _max_pruned regions were
// removed (for forward progress in evacuation) or the waste accumulated by the
// removed regions is above max_wasted.
class G1PruneRegionClosure : public HeapRegionClosure {
  uint _num_pruned;
  size_t _cur_wasted;

  uint const _max_pruned;
  size_t const _max_wasted;

public:
  G1PruneRegionClosure(uint max_pruned, size_t max_wasted) :
    _num_pruned(0), _cur_wasted(0), _max_pruned(max_pruned), _max_wasted(max_wasted) { }

  virtual bool do_heap_region(HeapRegion* r) {
    size_t const reclaimable = r->reclaimable_bytes();
    if (_num_pruned > _max_pruned ||
        _cur_wasted + reclaimable > _max_wasted) {
      return true;
    }
    r->rem_set()->clear(true /* cardset_only */);
    _cur_wasted += reclaimable;
    _num_pruned++;
    return false;
  }

  uint num_pruned() const { return _num_pruned; }
  size_t wasted() const { return _cur_wasted; }
};

void G1CollectionSetChooser::prune(G1CollectionSetCandidates* candidates) {
  G1Policy* p = G1CollectedHeap::heap()->policy();

  uint min_old_cset_length = p->calc_min_old_cset_length(candidates);
  uint num_candidates = candidates->num_regions();

  if (min_old_cset_length < num_candidates) {
    size_t allowed_waste = p->allowed_waste_in_collection_set();

    G1PruneRegionClosure prune_cl(num_candidates - min_old_cset_length,
                                  allowed_waste);
    candidates->iterate_backwards(&prune_cl);

    log_debug(gc, ergo, cset)("Pruned %u regions out of %u, leaving " SIZE_FORMAT " bytes waste (allowed " SIZE_FORMAT ")",
                              prune_cl.num_pruned(),
                              candidates->num_regions(),
                              prune_cl.wasted(),
                              allowed_waste);

    candidates->remove_from_end(prune_cl.num_pruned(), prune_cl.wasted());
  }
}

G1CollectionSetCandidates* G1CollectionSetChooser::build(WorkGang* workers, uint max_num_regions) {
  uint num_workers = workers->active_workers();
  uint chunk_size = calculate_work_chunk_size(num_workers, max_num_regions);

  G1BuildCandidateRegionsTask cl(max_num_regions, chunk_size, num_workers);
  workers->run_task(&cl, num_workers);

  G1CollectionSetCandidates* result = cl.get_sorted_candidates();
  prune(result);
  result->verify();
  return result;
}
