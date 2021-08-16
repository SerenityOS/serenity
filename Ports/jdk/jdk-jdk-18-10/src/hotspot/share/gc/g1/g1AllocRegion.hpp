/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1ALLOCREGION_HPP
#define SHARE_GC_G1_G1ALLOCREGION_HPP

#include "gc/g1/heapRegion.hpp"
#include "gc/g1/g1EvacStats.hpp"
#include "gc/g1/g1HeapRegionAttr.hpp"
#include "gc/g1/g1NUMA.hpp"

class G1CollectedHeap;

// A class that holds a region that is active in satisfying allocation
// requests, potentially issued in parallel. When the active region is
// full it will be retired and replaced with a new one. The
// implementation assumes that fast-path allocations will be lock-free
// and a lock will need to be taken when the active region needs to be
// replaced.

class G1AllocRegion : public CHeapObj<mtGC> {

private:
  // The active allocating region we are currently allocating out
  // of. The invariant is that if this object is initialized (i.e.,
  // init() has been called and release() has not) then _alloc_region
  // is either an active allocating region or the dummy region (i.e.,
  // it can never be NULL) and this object can be used to satisfy
  // allocation requests. If this object is not initialized
  // (i.e. init() has not been called or release() has been called)
  // then _alloc_region is NULL and this object should not be used to
  // satisfy allocation requests (it was done this way to force the
  // correct use of init() and release()).
  HeapRegion* volatile _alloc_region;

  // It keeps track of the distinct number of regions that are used
  // for allocation in the active interval of this object, i.e.,
  // between a call to init() and a call to release(). The count
  // mostly includes regions that are freshly allocated, as well as
  // the region that is re-used using the set() method. This count can
  // be used in any heuristics that might want to bound how many
  // distinct regions this object can used during an active interval.
  uint _count;

  // When we set up a new active region we save its used bytes in this
  // field so that, when we retire it, we can calculate how much space
  // we allocated in it.
  size_t _used_bytes_before;

  // When true, indicates that allocate calls should do BOT updates.
  const bool _bot_updates;

  // Useful for debugging and tracing.
  const char* _name;

  // A dummy region (i.e., it's been allocated specially for this
  // purpose and it is not part of the heap) that is full (i.e., top()
  // == end()). When we don't have a valid active region we make
  // _alloc_region point to this. This allows us to skip checking
  // whether the _alloc_region is NULL or not.
  static HeapRegion* _dummy_region;

  // After a region is allocated by alloc_new_region, this
  // method is used to set it as the active alloc_region
  void update_alloc_region(HeapRegion* alloc_region);

  // Allocate a new active region and use it to perform a word_size
  // allocation. The force parameter will be passed on to
  // G1CollectedHeap::allocate_new_alloc_region() and tells it to try
  // to allocate a new region even if the max has been reached.
  HeapWord* new_alloc_region_and_allocate(size_t word_size, bool force);

protected:
  // The memory node index this allocation region belongs to.
  uint _node_index;

  // Reset the alloc region to point a the dummy region.
  void reset_alloc_region();

  // Perform a non-MT-safe allocation out of the given region.
  inline HeapWord* allocate(HeapRegion* alloc_region,
                            size_t word_size);

  // Perform a MT-safe allocation out of the given region.
  inline HeapWord* par_allocate(HeapRegion* alloc_region,
                                size_t word_size);
  // Perform a MT-safe allocation out of the given region, with the given
  // minimum and desired size. Returns the actual size allocated (between
  // minimum and desired size) in actual_word_size if the allocation has been
  // successful.
  inline HeapWord* par_allocate(HeapRegion* alloc_region,
                                size_t min_word_size,
                                size_t desired_word_size,
                                size_t* actual_word_size);

  // Ensure that the region passed as a parameter has been filled up
  // so that noone else can allocate out of it any more.
  // Returns the number of bytes that have been wasted by filled up
  // the space.
  size_t fill_up_remaining_space(HeapRegion* alloc_region);

  // Retire the active allocating region. If fill_up is true then make
  // sure that the region is full before we retire it so that no one
  // else can allocate out of it.
  // Returns the number of bytes that have been filled up during retire.
  virtual size_t retire(bool fill_up);

  size_t retire_internal(HeapRegion* alloc_region, bool fill_up);

  // For convenience as subclasses use it.
  static G1CollectedHeap* _g1h;

  virtual HeapRegion* allocate_new_region(size_t word_size, bool force) = 0;
  virtual void retire_region(HeapRegion* alloc_region,
                             size_t allocated_bytes) = 0;

  G1AllocRegion(const char* name, bool bot_updates, uint node_index);

public:
  static void setup(G1CollectedHeap* g1h, HeapRegion* dummy_region);

  HeapRegion* get() const {
    HeapRegion * hr = _alloc_region;
    // Make sure that the dummy region does not escape this class.
    return (hr == _dummy_region) ? NULL : hr;
  }

  uint count() { return _count; }

  // The following two are the building blocks for the allocation method.

  // First-level allocation: Should be called without holding a
  // lock. It will try to allocate lock-free out of the active region,
  // or return NULL if it was unable to.
  inline HeapWord* attempt_allocation(size_t word_size);
  // Perform an allocation out of the current allocation region, with the given
  // minimum and desired size. Returns the actual size allocated (between
  // minimum and desired size) in actual_word_size if the allocation has been
  // successful.
  // Should be called without holding a lock. It will try to allocate lock-free
  // out of the active region, or return NULL if it was unable to.
  inline HeapWord* attempt_allocation(size_t min_word_size,
                                      size_t desired_word_size,
                                      size_t* actual_word_size);

  inline HeapWord* attempt_allocation_locked(size_t word_size);
  // Second-level allocation: Should be called while holding a
  // lock. We require that the caller takes the appropriate lock
  // before calling this so that it is easier to make it conform
  // to the locking protocol. The min and desired word size allow
  // specifying a minimum and maximum size of the allocation. The
  // actual size of allocation is returned in actual_word_size.
  inline HeapWord* attempt_allocation_locked(size_t min_word_size,
                                             size_t desired_word_size,
                                             size_t* actual_word_size);

  // Perform an allocation out of a new allocation region, retiring the current one.
  inline HeapWord* attempt_allocation_using_new_region(size_t min_word_size,
                                                       size_t desired_word_size,
                                                       size_t* actual_word_size);

  // Should be called to allocate a new region even if the max of this
  // type of regions has been reached. Should only be called if other
  // allocation attempts have failed and we are not holding a valid
  // active region.
  inline HeapWord* attempt_allocation_force(size_t word_size);

  // Should be called before we start using this object.
  virtual void init();

  // This can be used to set the active region to a specific
  // region. (Use Example: we try to retain the last old GC alloc
  // region that we've used during a GC and we can use set() to
  // re-instate it at the beginning of the next GC.)
  void set(HeapRegion* alloc_region);

  // Should be called when we want to release the active region which
  // is returned after it's been retired.
  virtual HeapRegion* release();

  void trace(const char* str,
             size_t min_word_size = 0,
             size_t desired_word_size = 0,
             size_t actual_word_size = 0,
             HeapWord* result = NULL) PRODUCT_RETURN;
};

class MutatorAllocRegion : public G1AllocRegion {
private:
  // Keeps track of the total waste generated during the current
  // mutator phase.
  size_t _wasted_bytes;

  // Retained allocation region. Used to lower the waste generated
  // during mutation by having two active regions if the free space
  // in a region about to be retired still could fit a TLAB.
  HeapRegion* volatile _retained_alloc_region;

  // Decide if the region should be retained, based on the free size
  // in it and the free size in the currently retained region, if any.
  bool should_retain(HeapRegion* region);
protected:
  virtual HeapRegion* allocate_new_region(size_t word_size, bool force);
  virtual void retire_region(HeapRegion* alloc_region, size_t allocated_bytes);
  virtual size_t retire(bool fill_up);
public:
  MutatorAllocRegion(uint node_index)
    : G1AllocRegion("Mutator Alloc Region", false /* bot_updates */, node_index),
      _wasted_bytes(0),
      _retained_alloc_region(NULL) { }

  // Returns the combined used memory in the current alloc region and
  // the retained alloc region.
  size_t used_in_alloc_regions();

  // Perform an allocation out of the retained allocation region, with the given
  // minimum and desired size. Returns the actual size allocated (between
  // minimum and desired size) in actual_word_size if the allocation has been
  // successful.
  // Should be called without holding a lock. It will try to allocate lock-free
  // out of the retained region, or return NULL if it was unable to.
  inline HeapWord* attempt_retained_allocation(size_t min_word_size,
                                               size_t desired_word_size,
                                               size_t* actual_word_size);

  // This specialization of release() makes sure that the retained alloc
  // region is retired and set to NULL.
  virtual HeapRegion* release();

  virtual void init();
};

// Common base class for allocation regions used during GC.
class G1GCAllocRegion : public G1AllocRegion {
protected:
  G1EvacStats* _stats;
  G1HeapRegionAttr::region_type_t _purpose;

  virtual HeapRegion* allocate_new_region(size_t word_size, bool force);
  virtual void retire_region(HeapRegion* alloc_region, size_t allocated_bytes);

  virtual size_t retire(bool fill_up);

  G1GCAllocRegion(const char* name, bool bot_updates, G1EvacStats* stats,
                  G1HeapRegionAttr::region_type_t purpose, uint node_index = G1NUMA::AnyNodeIndex)
  : G1AllocRegion(name, bot_updates, node_index), _stats(stats), _purpose(purpose) {
    assert(stats != NULL, "Must pass non-NULL PLAB statistics");
  }
};

class SurvivorGCAllocRegion : public G1GCAllocRegion {
public:
  SurvivorGCAllocRegion(G1EvacStats* stats, uint node_index)
  : G1GCAllocRegion("Survivor GC Alloc Region", false /* bot_updates */, stats, G1HeapRegionAttr::Young, node_index) { }
};

class OldGCAllocRegion : public G1GCAllocRegion {
public:
  OldGCAllocRegion(G1EvacStats* stats)
  : G1GCAllocRegion("Old GC Alloc Region", true /* bot_updates */, stats, G1HeapRegionAttr::Old) { }

  // This specialization of release() makes sure that the last card that has
  // been allocated into has been completely filled by a dummy object.  This
  // avoids races when remembered set scanning wants to update the BOT of the
  // last card in the retained old gc alloc region, and allocation threads
  // allocating into that card at the same time.
  virtual HeapRegion* release();
};

#endif // SHARE_GC_G1_G1ALLOCREGION_HPP
