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

#ifndef SHARE_GC_G1_HEAPREGION_HPP
#define SHARE_GC_G1_HEAPREGION_HPP

#include "gc/g1/g1BlockOffsetTable.hpp"
#include "gc/g1/g1HeapRegionTraceType.hpp"
#include "gc/g1/g1SurvRateGroup.hpp"
#include "gc/g1/heapRegionTracer.hpp"
#include "gc/g1/heapRegionType.hpp"
#include "gc/shared/ageTable.hpp"
#include "gc/shared/spaceDecorator.hpp"
#include "gc/shared/verifyOption.hpp"
#include "runtime/mutex.hpp"
#include "utilities/macros.hpp"

class G1CardSetConfiguration;
class G1CardSetMemoryManager;
class G1CollectedHeap;
class G1CMBitMap;
class G1Predictions;
class HeapRegionRemSet;
class HeapRegion;
class HeapRegionSetBase;
class nmethod;

#define HR_FORMAT "%u:(%s)[" PTR_FORMAT "," PTR_FORMAT "," PTR_FORMAT "]"
#define HR_FORMAT_PARAMS(_hr_) \
                (_hr_)->hrm_index(), \
                (_hr_)->get_short_type_str(), \
                p2i((_hr_)->bottom()), p2i((_hr_)->top()), p2i((_hr_)->end())

// sentinel value for hrm_index
#define G1_NO_HRM_INDEX ((uint) -1)

// A HeapRegion is the smallest piece of a G1CollectedHeap that
// can be collected independently.

// Each heap region is self contained. top() and end() can never
// be set beyond the end of the region. For humongous objects,
// the first region is a StartsHumongous region. If the humongous
// object is larger than a heap region, the following regions will
// be of type ContinuesHumongous. In this case the top() of the
// StartHumongous region and all ContinuesHumongous regions except
// the last will point to their own end. The last ContinuesHumongous
// region may have top() equal the end of object if there isn't
// room for filler objects to pad out to the end of the region.
class HeapRegion : public CHeapObj<mtGC> {
  friend class VMStructs;

  HeapWord* const _bottom;
  HeapWord* const _end;

  HeapWord* volatile _top;
  HeapWord* _compaction_top;

  G1BlockOffsetTablePart _bot_part;
  Mutex _par_alloc_lock;
  // When we need to retire an allocation region, while other threads
  // are also concurrently trying to allocate into it, we typically
  // allocate a dummy object at the end of the region to ensure that
  // no more allocations can take place in it. However, sometimes we
  // want to know where the end of the last "real" object we allocated
  // into the region was and this is what this keeps track.
  HeapWord* _pre_dummy_top;

public:
  HeapWord* bottom() const         { return _bottom; }
  HeapWord* end() const            { return _end;    }

  void set_compaction_top(HeapWord* compaction_top) { _compaction_top = compaction_top; }
  HeapWord* compaction_top() const { return _compaction_top; }

  void set_top(HeapWord* value) { _top = value; }
  HeapWord* top() const { return _top; }

  // See the comment above in the declaration of _pre_dummy_top for an
  // explanation of what it is.
  void set_pre_dummy_top(HeapWord* pre_dummy_top) {
    assert(is_in(pre_dummy_top) && pre_dummy_top <= top(), "pre-condition");
    _pre_dummy_top = pre_dummy_top;
  }
  HeapWord* pre_dummy_top() { return (_pre_dummy_top == NULL) ? top() : _pre_dummy_top; }
  void reset_pre_dummy_top() { _pre_dummy_top = NULL; }

  // Returns true iff the given the heap  region contains the
  // given address as part of an allocated object. This may
  // be a potentially, so we restrict its use to assertion checks only.
  bool is_in(const void* p) const {
    return is_in_reserved(p);
  }
  bool is_in(oop obj) const {
    return is_in((void*)obj);
  }
  // Returns true iff the given reserved memory of the space contains the
  // given address.
  bool is_in_reserved(const void* p) const { return _bottom <= p && p < _end; }

  size_t capacity()     const { return byte_size(bottom(), end()); }
  size_t used() const { return byte_size(bottom(), top()); }
  size_t free() const { return byte_size(top(), end()); }

  bool is_empty() const { return used() == 0; }

private:
  void reset_compaction_top_after_compaction();

  void reset_after_full_gc_common();

  void clear(bool mangle_space);

  HeapWord* block_start_const(const void* p) const;

  void mangle_unused_area() PRODUCT_RETURN;

  // Try to allocate at least min_word_size and up to desired_size from this region.
  // Returns NULL if not possible, otherwise sets actual_word_size to the amount of
  // space allocated.
  // This version assumes that all allocation requests to this HeapRegion are properly
  // synchronized.
  inline HeapWord* allocate_impl(size_t min_word_size, size_t desired_word_size, size_t* actual_word_size);
  // Try to allocate at least min_word_size and up to desired_size from this HeapRegion.
  // Returns NULL if not possible, otherwise sets actual_word_size to the amount of
  // space allocated.
  // This version synchronizes with other calls to par_allocate_impl().
  inline HeapWord* par_allocate_impl(size_t min_word_size, size_t desired_word_size, size_t* actual_word_size);

public:
  HeapWord* block_start(const void* p);

  void object_iterate(ObjectClosure* blk);

  // Allocation (return NULL if full).  Assumes the caller has established
  // mutually exclusive access to the HeapRegion.
  HeapWord* allocate(size_t min_word_size, size_t desired_word_size, size_t* actual_word_size);
  // Allocation (return NULL if full).  Enforces mutual exclusion internally.
  HeapWord* par_allocate(size_t min_word_size, size_t desired_word_size, size_t* actual_word_size);

  HeapWord* allocate(size_t word_size);
  HeapWord* par_allocate(size_t word_size);

  inline HeapWord* par_allocate_no_bot_updates(size_t min_word_size, size_t desired_word_size, size_t* word_size);
  inline HeapWord* allocate_no_bot_updates(size_t word_size);
  inline HeapWord* allocate_no_bot_updates(size_t min_word_size, size_t desired_word_size, size_t* actual_size);

  // Full GC support methods.

  HeapWord* initialize_threshold();
  HeapWord* cross_threshold(HeapWord* start, HeapWord* end);

  // Update heap region that has been compacted to be consistent after Full GC.
  void reset_compacted_after_full_gc();
  // Update skip-compacting heap region to be consistent after Full GC.
  void reset_skip_compacting_after_full_gc();

  // All allocated blocks are occupied by objects in a HeapRegion
  bool block_is_obj(const HeapWord* p) const;

  // Returns whether the given object is dead based on TAMS and bitmap.
  // An object is dead iff a) it was not allocated since the last mark (>TAMS), b) it
  // is not marked (bitmap).
  bool is_obj_dead(const oop obj, const G1CMBitMap* const prev_bitmap) const;

  // Returns the object size for all valid block starts
  // and the amount of unallocated words if called on top()
  size_t block_size(const HeapWord* p) const;

  // Scans through the region using the bitmap to determine what
  // objects to call size_t ApplyToMarkedClosure::apply(oop) for.
  template<typename ApplyToMarkedClosure>
  inline void apply_to_marked_objects(G1CMBitMap* bitmap, ApplyToMarkedClosure* closure);

  void reset_bot() {
    _bot_part.reset_bot();
  }

  void update_bot() {
    _bot_part.update();
  }

private:
  // The remembered set for this region.
  HeapRegionRemSet* _rem_set;

  // Cached index of this region in the heap region sequence.
  const uint _hrm_index;

  HeapRegionType _type;

  // For a humongous region, region in which it starts.
  HeapRegion* _humongous_start_region;

  static const uint InvalidCSetIndex = UINT_MAX;

  // The index in the optional regions array, if this region
  // is considered optional during a mixed collections.
  uint _index_in_opt_cset;

  // Fields used by the HeapRegionSetBase class and subclasses.
  HeapRegion* _next;
  HeapRegion* _prev;
#ifdef ASSERT
  HeapRegionSetBase* _containing_set;
#endif // ASSERT

  // The start of the unmarked area. The unmarked area extends from this
  // word until the top and/or end of the region, and is the part
  // of the region for which no marking was done, i.e. objects may
  // have been allocated in this part since the last mark phase.
  // "prev" is the top at the start of the last completed marking.
  // "next" is the top at the start of the in-progress marking (if any.)
  HeapWord* _prev_top_at_mark_start;
  HeapWord* _next_top_at_mark_start;

  // We use concurrent marking to determine the amount of live data
  // in each heap region.
  size_t _prev_marked_bytes;    // Bytes known to be live via last completed marking.
  size_t _next_marked_bytes;    // Bytes known to be live via in-progress marking.

  void init_top_at_mark_start() {
    assert(_prev_marked_bytes == 0 &&
           _next_marked_bytes == 0,
           "Must be called after zero_marked_bytes.");
    _prev_top_at_mark_start = _next_top_at_mark_start = bottom();
  }

  // Data for young region survivor prediction.
  uint  _young_index_in_cset;
  G1SurvRateGroup* _surv_rate_group;
  int  _age_index;

  // Cached attributes used in the collection set policy information

  // The calculated GC efficiency of the region.
  double _gc_efficiency;

  uint _node_index;

  void report_region_type_change(G1HeapRegionTraceType::Type to);

  // Returns whether the given object address refers to a dead object, and either the
  // size of the object (if live) or the size of the block (if dead) in size.
  // May
  // - only called with obj < top()
  // - not called on humongous objects or archive regions
  inline bool is_obj_dead_with_size(const oop obj, const G1CMBitMap* const prev_bitmap, size_t* size) const;

  // Iterate over the references covered by the given MemRegion in a humongous
  // object and apply the given closure to them.
  // Humongous objects are allocated directly in the old-gen. So we need special
  // handling for concurrent processing encountering an in-progress allocation.
  // Returns the address after the last actually scanned or NULL if the area could
  // not be scanned (That should only happen when invoked concurrently with the
  // mutator).
  template <class Closure, bool is_gc_active>
  inline HeapWord* do_oops_on_memregion_in_humongous(MemRegion mr,
                                                     Closure* cl,
                                                     G1CollectedHeap* g1h);

  // Returns the block size of the given (dead, potentially having its class unloaded) object
  // starting at p extending to at most the prev TAMS using the given mark bitmap.
  inline size_t block_size_using_bitmap(const HeapWord* p, const G1CMBitMap* const prev_bitmap) const;
public:
  HeapRegion(uint hrm_index,
             G1BlockOffsetTable* bot,
             MemRegion mr,
             G1CardSetConfiguration* config);

  // If this region is a member of a HeapRegionManager, the index in that
  // sequence, otherwise -1.
  uint hrm_index() const { return _hrm_index; }

  // Initializing the HeapRegion not only resets the data structure, but also
  // resets the BOT for that heap region.
  // The default values for clear_space means that we will do the clearing if
  // there's clearing to be done ourselves. We also always mangle the space.
  void initialize(bool clear_space = false, bool mangle_space = SpaceDecorator::Mangle);

  static int    LogOfHRGrainBytes;
  static int    LogCardsPerRegion;

  static size_t GrainBytes;
  static size_t GrainWords;
  static size_t CardsPerRegion;

  static size_t align_up_to_region_byte_size(size_t sz) {
    return (sz + (size_t) GrainBytes - 1) &
                                      ~((1 << (size_t) LogOfHRGrainBytes) - 1);
  }

  // Returns whether a field is in the same region as the obj it points to.
  template <typename T>
  static bool is_in_same_region(T* p, oop obj) {
    assert(p != NULL, "p can't be NULL");
    assert(obj != NULL, "obj can't be NULL");
    return (((uintptr_t) p ^ cast_from_oop<uintptr_t>(obj)) >> LogOfHRGrainBytes) == 0;
  }

  static size_t max_region_size();
  static size_t min_region_size_in_words();

  // It sets up the heap region size (GrainBytes / GrainWords), as well as
  // other related fields that are based on the heap region size
  // (LogOfHRGrainBytes / CardsPerRegion). All those fields are considered
  // constant throughout the JVM's execution, therefore they should only be set
  // up once during initialization time.
  static void setup_heap_region_size(size_t max_heap_size);

  // The number of bytes marked live in the region in the last marking phase.
  size_t marked_bytes()    { return _prev_marked_bytes; }
  size_t live_bytes() {
    return (top() - prev_top_at_mark_start()) * HeapWordSize + marked_bytes();
  }

  // The number of bytes counted in the next marking.
  size_t next_marked_bytes() { return _next_marked_bytes; }
  // The number of bytes live wrt the next marking.
  size_t next_live_bytes() {
    return
      (top() - next_top_at_mark_start()) * HeapWordSize + next_marked_bytes();
  }

  // A lower bound on the amount of garbage bytes in the region.
  size_t garbage_bytes() {
    size_t used_at_mark_start_bytes =
      (prev_top_at_mark_start() - bottom()) * HeapWordSize;
    return used_at_mark_start_bytes - marked_bytes();
  }

  // Return the amount of bytes we'll reclaim if we collect this
  // region. This includes not only the known garbage bytes in the
  // region but also any unallocated space in it, i.e., [top, end),
  // since it will also be reclaimed if we collect the region.
  size_t reclaimable_bytes() {
    size_t known_live_bytes = live_bytes();
    assert(known_live_bytes <= capacity(), "sanity");
    return capacity() - known_live_bytes;
  }

  // An upper bound on the number of live bytes in the region.
  size_t max_live_bytes() { return used() - garbage_bytes(); }

  void add_to_marked_bytes(size_t incr_bytes) {
    _next_marked_bytes = _next_marked_bytes + incr_bytes;
  }

  void zero_marked_bytes()      {
    _prev_marked_bytes = _next_marked_bytes = 0;
  }
  // Get the start of the unmarked area in this region.
  HeapWord* prev_top_at_mark_start() const { return _prev_top_at_mark_start; }
  HeapWord* next_top_at_mark_start() const { return _next_top_at_mark_start; }

  // Note the start or end of marking. This tells the heap region
  // that the collector is about to start or has finished (concurrently)
  // marking the heap.

  // Notify the region that concurrent marking is starting. Initialize
  // all fields related to the next marking info.
  inline void note_start_of_marking();

  // Notify the region that concurrent marking has finished. Copy the
  // (now finalized) next marking info fields into the prev marking
  // info fields.
  inline void note_end_of_marking();

  const char* get_type_str() const { return _type.get_str(); }
  const char* get_short_type_str() const { return _type.get_short_str(); }
  G1HeapRegionTraceType::Type get_trace_type() { return _type.get_trace_type(); }

  bool is_free() const { return _type.is_free(); }

  bool is_young()    const { return _type.is_young();    }
  bool is_eden()     const { return _type.is_eden();     }
  bool is_survivor() const { return _type.is_survivor(); }

  bool is_humongous() const { return _type.is_humongous(); }
  bool is_starts_humongous() const { return _type.is_starts_humongous(); }
  bool is_continues_humongous() const { return _type.is_continues_humongous();   }

  bool is_old() const { return _type.is_old(); }

  bool is_old_or_humongous() const { return _type.is_old_or_humongous(); }

  bool is_old_or_humongous_or_archive() const { return _type.is_old_or_humongous_or_archive(); }

  // A pinned region contains objects which are not moved by garbage collections.
  // Humongous regions and archive regions are pinned.
  bool is_pinned() const { return _type.is_pinned(); }

  // An archive region is a pinned region, also tagged as old, which
  // should not be marked during mark/sweep. This allows the address
  // space to be shared by JVM instances.
  bool is_archive()        const { return _type.is_archive(); }
  bool is_open_archive()   const { return _type.is_open_archive(); }
  bool is_closed_archive() const { return _type.is_closed_archive(); }

  void set_free();

  void set_eden();
  void set_eden_pre_gc();
  void set_survivor();

  void move_to_old();
  void set_old();

  void set_open_archive();
  void set_closed_archive();

  // For a humongous region, region in which it starts.
  HeapRegion* humongous_start_region() const {
    return _humongous_start_region;
  }

  // Makes the current region be a "starts humongous" region, i.e.,
  // the first region in a series of one or more contiguous regions
  // that will contain a single "humongous" object.
  //
  // obj_top : points to the top of the humongous object.
  // fill_size : size of the filler object at the end of the region series.
  void set_starts_humongous(HeapWord* obj_top, size_t fill_size);

  // Makes the current region be a "continues humongous'
  // region. first_hr is the "start humongous" region of the series
  // which this region will be part of.
  void set_continues_humongous(HeapRegion* first_hr);

  // Unsets the humongous-related fields on the region.
  void clear_humongous();

  void set_rem_set(HeapRegionRemSet* rem_set) { _rem_set = rem_set; }
  // If the region has a remembered set, return a pointer to it.
  HeapRegionRemSet* rem_set() const {
    return _rem_set;
  }

  inline bool in_collection_set() const;

  // Methods used by the HeapRegionSetBase class and subclasses.

  // Getter and setter for the next and prev fields used to link regions into
  // linked lists.
  void set_next(HeapRegion* next) { _next = next; }
  HeapRegion* next()              { return _next; }

  void set_prev(HeapRegion* prev) { _prev = prev; }
  HeapRegion* prev()              { return _prev; }

  void unlink_from_list();

  // Every region added to a set is tagged with a reference to that
  // set. This is used for doing consistency checking to make sure that
  // the contents of a set are as they should be and it's only
  // available in non-product builds.
#ifdef ASSERT
  void set_containing_set(HeapRegionSetBase* containing_set) {
    assert((containing_set != NULL && _containing_set == NULL) ||
            containing_set == NULL,
           "containing_set: " PTR_FORMAT " "
           "_containing_set: " PTR_FORMAT,
           p2i(containing_set), p2i(_containing_set));

    _containing_set = containing_set;
  }

  HeapRegionSetBase* containing_set() { return _containing_set; }
#else // ASSERT
  void set_containing_set(HeapRegionSetBase* containing_set) { }

  // containing_set() is only used in asserts so there's no reason
  // to provide a dummy version of it.
#endif // ASSERT


  // Reset the HeapRegion to default values and clear its remembered set.
  // If clear_space is true, clear the HeapRegion's memory.
  // Callers must ensure this is not called by multiple threads at the same time.
  void hr_clear(bool clear_space);
  // Clear the card table corresponding to this region.
  void clear_cardtable();

  // Notify the region that we are about to start processing
  // self-forwarded objects during evac failure handling.
  void note_self_forwarding_removal_start(bool during_concurrent_start,
                                          bool during_conc_mark);

  // Notify the region that we have finished processing self-forwarded
  // objects during evac failure handling.
  void note_self_forwarding_removal_end(size_t marked_bytes);

  uint index_in_opt_cset() const {
    assert(has_index_in_opt_cset(), "Opt cset index not set.");
    return _index_in_opt_cset;
  }
  bool has_index_in_opt_cset() const { return _index_in_opt_cset != InvalidCSetIndex; }
  void set_index_in_opt_cset(uint index) { _index_in_opt_cset = index; }
  void clear_index_in_opt_cset() { _index_in_opt_cset = InvalidCSetIndex; }

  void calc_gc_efficiency(void);
  double gc_efficiency() const { return _gc_efficiency;}

  uint  young_index_in_cset() const { return _young_index_in_cset; }
  void clear_young_index_in_cset() { _young_index_in_cset = 0; }
  void set_young_index_in_cset(uint index) {
    assert(index != UINT_MAX, "just checking");
    assert(index != 0, "just checking");
    assert(is_young(), "pre-condition");
    _young_index_in_cset = index;
  }

  int age_in_surv_rate_group() const;
  bool has_valid_age_in_surv_rate() const;

  bool has_surv_rate_group() const;

  double surv_rate_prediction(G1Predictions const& predictor) const;

  void install_surv_rate_group(G1SurvRateGroup* surv_rate_group);
  void uninstall_surv_rate_group();

  void record_surv_words_in_group(size_t words_survived);

  // Determine if an object has been allocated since the last
  // mark performed by the collector. This returns true iff the object
  // is within the unmarked area of the region.
  bool obj_allocated_since_prev_marking(oop obj) const {
    return cast_from_oop<HeapWord*>(obj) >= prev_top_at_mark_start();
  }
  bool obj_allocated_since_next_marking(oop obj) const {
    return cast_from_oop<HeapWord*>(obj) >= next_top_at_mark_start();
  }

  // Update the region state after a failed evacuation.
  void handle_evacuation_failure();

  // Iterate over the objects overlapping the given memory region, applying cl
  // to all references in the region.  This is a helper for
  // G1RemSet::refine_card*, and is tightly coupled with them.
  // mr must not be empty. Must be trimmed to the allocated/parseable space in this region.
  // This region must be old or humongous.
  // Returns the next unscanned address if the designated objects were successfully
  // processed, NULL if an unparseable part of the heap was encountered (That should
  // only happen when invoked concurrently with the mutator).
  template <bool is_gc_active, class Closure>
  inline HeapWord* oops_on_memregion_seq_iterate_careful(MemRegion mr, Closure* cl);

  // Routines for managing a list of code roots (attached to the
  // this region's RSet) that point into this heap region.
  void add_strong_code_root(nmethod* nm);
  void add_strong_code_root_locked(nmethod* nm);
  void remove_strong_code_root(nmethod* nm);

  // Applies blk->do_code_blob() to each of the entries in
  // the strong code roots list for this region
  void strong_code_roots_do(CodeBlobClosure* blk) const;

  uint node_index() const { return _node_index; }
  void set_node_index(uint node_index) { _node_index = node_index; }

  // Verify that the entries on the strong code root list for this
  // region are live and include at least one pointer into this region.
  void verify_strong_code_roots(VerifyOption vo, bool* failures) const;

  void print() const;
  void print_on(outputStream* st) const;

  // vo == UsePrevMarking -> use "prev" marking information,
  // vo == UseNextMarking -> use "next" marking information
  // vo == UseFullMarking -> use "next" marking bitmap but no TAMS
  //
  // NOTE: Only the "prev" marking information is guaranteed to be
  // consistent most of the time, so most calls to this should use
  // vo == UsePrevMarking.
  // Currently, there is only one case where this is called with
  // vo == UseNextMarking, which is to verify the "next" marking
  // information at the end of remark.
  // Currently there is only one place where this is called with
  // vo == UseFullMarking, which is to verify the marking during a
  // full GC.
  void verify(VerifyOption vo, bool *failures) const;

  // Verify using the "prev" marking information
  void verify() const;

  void verify_rem_set(VerifyOption vo, bool *failures) const;
  void verify_rem_set() const;
};

// HeapRegionClosure is used for iterating over regions.
// Terminates the iteration when the "do_heap_region" method returns "true".
class HeapRegionClosure : public StackObj {
  friend class HeapRegionManager;
  friend class G1CollectionSet;
  friend class G1CollectionSetCandidates;

  bool _is_complete;
  void set_incomplete() { _is_complete = false; }

public:
  HeapRegionClosure(): _is_complete(true) {}

  // Typically called on each region until it returns true.
  virtual bool do_heap_region(HeapRegion* r) = 0;

  // True after iteration if the closure was applied to all heap regions
  // and returned "false" in all cases.
  bool is_complete() { return _is_complete; }
};

#endif // SHARE_GC_G1_HEAPREGION_HPP
