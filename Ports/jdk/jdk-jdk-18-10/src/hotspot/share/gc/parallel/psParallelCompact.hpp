/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSPARALLELCOMPACT_HPP
#define SHARE_GC_PARALLEL_PSPARALLELCOMPACT_HPP

#include "gc/parallel/mutableSpace.hpp"
#include "gc/parallel/objectStartArray.hpp"
#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/parMarkBitMap.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/collectorCounters.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "oops/oop.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"

class ParallelScavengeHeap;
class PSAdaptiveSizePolicy;
class PSYoungGen;
class PSOldGen;
class ParCompactionManager;
class PSParallelCompact;
class MoveAndUpdateClosure;
class RefProcTaskExecutor;
class ParallelOldTracer;
class STWGCTimer;

// The SplitInfo class holds the information needed to 'split' a source region
// so that the live data can be copied to two destination *spaces*.  Normally,
// all the live data in a region is copied to a single destination space (e.g.,
// everything live in a region in eden is copied entirely into the old gen).
// However, when the heap is nearly full, all the live data in eden may not fit
// into the old gen.  Copying only some of the regions from eden to old gen
// requires finding a region that does not contain a partial object (i.e., no
// live object crosses the region boundary) somewhere near the last object that
// does fit into the old gen.  Since it's not always possible to find such a
// region, splitting is necessary for predictable behavior.
//
// A region is always split at the end of the partial object.  This avoids
// additional tests when calculating the new location of a pointer, which is a
// very hot code path.  The partial object and everything to its left will be
// copied to another space (call it dest_space_1).  The live data to the right
// of the partial object will be copied either within the space itself, or to a
// different destination space (distinct from dest_space_1).
//
// Split points are identified during the summary phase, when region
// destinations are computed:  data about the split, including the
// partial_object_size, is recorded in a SplitInfo record and the
// partial_object_size field in the summary data is set to zero.  The zeroing is
// possible (and necessary) since the partial object will move to a different
// destination space than anything to its right, thus the partial object should
// not affect the locations of any objects to its right.
//
// The recorded data is used during the compaction phase, but only rarely:  when
// the partial object on the split region will be copied across a destination
// region boundary.  This test is made once each time a region is filled, and is
// a simple address comparison, so the overhead is negligible (see
// PSParallelCompact::first_src_addr()).
//
// Notes:
//
// Only regions with partial objects are split; a region without a partial
// object does not need any extra bookkeeping.
//
// At most one region is split per space, so the amount of data required is
// constant.
//
// A region is split only when the destination space would overflow.  Once that
// happens, the destination space is abandoned and no other data (even from
// other source spaces) is targeted to that destination space.  Abandoning the
// destination space may leave a somewhat large unused area at the end, if a
// large object caused the overflow.
//
// Future work:
//
// More bookkeeping would be required to continue to use the destination space.
// The most general solution would allow data from regions in two different
// source spaces to be "joined" in a single destination region.  At the very
// least, additional code would be required in next_src_region() to detect the
// join and skip to an out-of-order source region.  If the join region was also
// the last destination region to which a split region was copied (the most
// likely case), then additional work would be needed to get fill_region() to
// stop iteration and switch to a new source region at the right point.  Basic
// idea would be to use a fake value for the top of the source space.  It is
// doable, if a bit tricky.
//
// A simpler (but less general) solution would fill the remainder of the
// destination region with a dummy object and continue filling the next
// destination region.

class SplitInfo
{
public:
  // Return true if this split info is valid (i.e., if a split has been
  // recorded).  The very first region cannot have a partial object and thus is
  // never split, so 0 is the 'invalid' value.
  bool is_valid() const { return _src_region_idx > 0; }

  // Return true if this split holds data for the specified source region.
  inline bool is_split(size_t source_region) const;

  // The index of the split region, the size of the partial object on that
  // region and the destination of the partial object.
  size_t    src_region_idx() const   { return _src_region_idx; }
  size_t    partial_obj_size() const { return _partial_obj_size; }
  HeapWord* destination() const      { return _destination; }

  // The destination count of the partial object referenced by this split
  // (either 1 or 2).  This must be added to the destination count of the
  // remainder of the source region.
  unsigned int destination_count() const { return _destination_count; }

  // If a word within the partial object will be written to the first word of a
  // destination region, this is the address of the destination region;
  // otherwise this is NULL.
  HeapWord* dest_region_addr() const     { return _dest_region_addr; }

  // If a word within the partial object will be written to the first word of a
  // destination region, this is the address of that word within the partial
  // object; otherwise this is NULL.
  HeapWord* first_src_addr() const       { return _first_src_addr; }

  // Record the data necessary to split the region src_region_idx.
  void record(size_t src_region_idx, size_t partial_obj_size,
              HeapWord* destination);

  void clear();

  DEBUG_ONLY(void verify_clear();)

private:
  size_t       _src_region_idx;
  size_t       _partial_obj_size;
  HeapWord*    _destination;
  unsigned int _destination_count;
  HeapWord*    _dest_region_addr;
  HeapWord*    _first_src_addr;
};

inline bool SplitInfo::is_split(size_t region_idx) const
{
  return _src_region_idx == region_idx && is_valid();
}

class SpaceInfo
{
 public:
  MutableSpace* space() const { return _space; }

  // Where the free space will start after the collection.  Valid only after the
  // summary phase completes.
  HeapWord* new_top() const { return _new_top; }

  // Allows new_top to be set.
  HeapWord** new_top_addr() { return &_new_top; }

  // Where the smallest allowable dense prefix ends (used only for perm gen).
  HeapWord* min_dense_prefix() const { return _min_dense_prefix; }

  // Where the dense prefix ends, or the compacted region begins.
  HeapWord* dense_prefix() const { return _dense_prefix; }

  // The start array for the (generation containing the) space, or NULL if there
  // is no start array.
  ObjectStartArray* start_array() const { return _start_array; }

  SplitInfo& split_info() { return _split_info; }

  void set_space(MutableSpace* s)           { _space = s; }
  void set_new_top(HeapWord* addr)          { _new_top = addr; }
  void set_min_dense_prefix(HeapWord* addr) { _min_dense_prefix = addr; }
  void set_dense_prefix(HeapWord* addr)     { _dense_prefix = addr; }
  void set_start_array(ObjectStartArray* s) { _start_array = s; }

  void publish_new_top() const              { _space->set_top(_new_top); }

 private:
  MutableSpace*     _space;
  HeapWord*         _new_top;
  HeapWord*         _min_dense_prefix;
  HeapWord*         _dense_prefix;
  ObjectStartArray* _start_array;
  SplitInfo         _split_info;
};

class ParallelCompactData
{
public:
  // Sizes are in HeapWords, unless indicated otherwise.
  static const size_t Log2RegionSize;
  static const size_t RegionSize;
  static const size_t RegionSizeBytes;

  // Mask for the bits in a size_t to get an offset within a region.
  static const size_t RegionSizeOffsetMask;
  // Mask for the bits in a pointer to get an offset within a region.
  static const size_t RegionAddrOffsetMask;
  // Mask for the bits in a pointer to get the address of the start of a region.
  static const size_t RegionAddrMask;

  static const size_t Log2BlockSize;
  static const size_t BlockSize;
  static const size_t BlockSizeBytes;

  static const size_t BlockSizeOffsetMask;
  static const size_t BlockAddrOffsetMask;
  static const size_t BlockAddrMask;

  static const size_t BlocksPerRegion;
  static const size_t Log2BlocksPerRegion;

  class RegionData
  {
  public:
    // Destination address of the region.
    HeapWord* destination() const { return _destination; }

    // The first region containing data destined for this region.
    size_t source_region() const { return _source_region; }

    // Reuse _source_region to store the corresponding shadow region index
    size_t shadow_region() const { return _source_region; }

    // The object (if any) starting in this region and ending in a different
    // region that could not be updated during the main (parallel) compaction
    // phase.  This is different from _partial_obj_addr, which is an object that
    // extends onto a source region.  However, the two uses do not overlap in
    // time, so the same field is used to save space.
    HeapWord* deferred_obj_addr() const { return _partial_obj_addr; }

    // The starting address of the partial object extending onto the region.
    HeapWord* partial_obj_addr() const { return _partial_obj_addr; }

    // Size of the partial object extending onto the region (words).
    size_t partial_obj_size() const { return _partial_obj_size; }

    // Size of live data that lies within this region due to objects that start
    // in this region (words).  This does not include the partial object
    // extending onto the region (if any), or the part of an object that extends
    // onto the next region (if any).
    size_t live_obj_size() const { return _dc_and_los & los_mask; }

    // Total live data that lies within the region (words).
    size_t data_size() const { return partial_obj_size() + live_obj_size(); }

    // The destination_count is the number of other regions to which data from
    // this region will be copied.  At the end of the summary phase, the valid
    // values of destination_count are
    //
    // 0 - data from the region will be compacted completely into itself, or the
    //     region is empty.  The region can be claimed and then filled.
    // 1 - data from the region will be compacted into 1 other region; some
    //     data from the region may also be compacted into the region itself.
    // 2 - data from the region will be copied to 2 other regions.
    //
    // During compaction as regions are emptied, the destination_count is
    // decremented (atomically) and when it reaches 0, it can be claimed and
    // then filled.
    //
    // A region is claimed for processing by atomically changing the
    // destination_count to the claimed value (dc_claimed).  After a region has
    // been filled, the destination_count should be set to the completed value
    // (dc_completed).
    inline uint destination_count() const;
    inline uint destination_count_raw() const;

    // Whether the block table for this region has been filled.
    inline bool blocks_filled() const;

    // Number of times the block table was filled.
    DEBUG_ONLY(inline size_t blocks_filled_count() const;)

    // The location of the java heap data that corresponds to this region.
    inline HeapWord* data_location() const;

    // The highest address referenced by objects in this region.
    inline HeapWord* highest_ref() const;

    // Whether this region is available to be claimed, has been claimed, or has
    // been completed.
    //
    // Minor subtlety:  claimed() returns true if the region is marked
    // completed(), which is desirable since a region must be claimed before it
    // can be completed.
    bool available() const { return _dc_and_los < dc_one; }
    bool claimed()   const { return _dc_and_los >= dc_claimed; }
    bool completed() const { return _dc_and_los >= dc_completed; }

    // These are not atomic.
    void set_destination(HeapWord* addr)       { _destination = addr; }
    void set_source_region(size_t region)      { _source_region = region; }
    void set_shadow_region(size_t region)      { _source_region = region; }
    void set_deferred_obj_addr(HeapWord* addr) { _partial_obj_addr = addr; }
    void set_partial_obj_addr(HeapWord* addr)  { _partial_obj_addr = addr; }
    void set_partial_obj_size(size_t words)    {
      _partial_obj_size = (region_sz_t) words;
    }
    inline void set_blocks_filled();

    inline void set_destination_count(uint count);
    inline void set_live_obj_size(size_t words);
    inline void set_data_location(HeapWord* addr);
    inline void set_completed();
    inline bool claim_unsafe();

    // These are atomic.
    inline void add_live_obj(size_t words);
    inline void set_highest_ref(HeapWord* addr);
    inline void decrement_destination_count();
    inline bool claim();

    // Possible values of _shadow_state, and transition is as follows
    // Normal Path:
    // UnusedRegion -> mark_normal() -> NormalRegion
    // Shadow Path:
    // UnusedRegion -> mark_shadow() -> ShadowRegion ->
    // mark_filled() -> FilledShadow -> mark_copied() -> CopiedShadow
    static const int UnusedRegion = 0; // The region is not collected yet
    static const int ShadowRegion = 1; // Stolen by an idle thread, and a shadow region is created for it
    static const int FilledShadow = 2; // Its shadow region has been filled and ready to be copied back
    static const int CopiedShadow = 3; // The data of the shadow region has been copied back
    static const int NormalRegion = 4; // The region will be collected by the original parallel algorithm

    // Mark the current region as normal or shadow to enter different processing paths
    inline bool mark_normal();
    inline bool mark_shadow();
    // Mark the shadow region as filled and ready to be copied back
    inline void mark_filled();
    // Mark the shadow region as copied back to avoid double copying.
    inline bool mark_copied();
    // Special case: see the comment in PSParallelCompact::fill_and_update_shadow_region.
    // Return to the normal path here
    inline void shadow_to_normal();


    int shadow_state() { return _shadow_state; }

  private:
    // The type used to represent object sizes within a region.
    typedef uint region_sz_t;

    // Constants for manipulating the _dc_and_los field, which holds both the
    // destination count and live obj size.  The live obj size lives at the
    // least significant end so no masking is necessary when adding.
    static const region_sz_t dc_shift;           // Shift amount.
    static const region_sz_t dc_mask;            // Mask for destination count.
    static const region_sz_t dc_one;             // 1, shifted appropriately.
    static const region_sz_t dc_claimed;         // Region has been claimed.
    static const region_sz_t dc_completed;       // Region has been completed.
    static const region_sz_t los_mask;           // Mask for live obj size.

    HeapWord*            _destination;
    size_t               _source_region;
    HeapWord*            _partial_obj_addr;
    region_sz_t          _partial_obj_size;
    region_sz_t volatile _dc_and_los;
    bool        volatile _blocks_filled;
    int         volatile _shadow_state;

#ifdef ASSERT
    size_t               _blocks_filled_count;   // Number of block table fills.

    // These enable optimizations that are only partially implemented.  Use
    // debug builds to prevent the code fragments from breaking.
    HeapWord*            _data_location;
    HeapWord*            _highest_ref;
#endif  // #ifdef ASSERT

#ifdef ASSERT
   public:
    uint                 _pushed;   // 0 until region is pushed onto a stack
   private:
#endif
  };

  // "Blocks" allow shorter sections of the bitmap to be searched.  Each Block
  // holds an offset, which is the amount of live data in the Region to the left
  // of the first live object that starts in the Block.
  class BlockData
  {
  public:
    typedef unsigned short int blk_ofs_t;

    blk_ofs_t offset() const    { return _offset; }
    void set_offset(size_t val) { _offset = (blk_ofs_t)val; }

  private:
    blk_ofs_t _offset;
  };

public:
  ParallelCompactData();
  bool initialize(MemRegion covered_region);

  size_t region_count() const { return _region_count; }
  size_t reserved_byte_size() const { return _reserved_byte_size; }

  // Convert region indices to/from RegionData pointers.
  inline RegionData* region(size_t region_idx) const;
  inline size_t     region(const RegionData* const region_ptr) const;

  size_t block_count() const { return _block_count; }
  inline BlockData* block(size_t block_idx) const;
  inline size_t     block(const BlockData* block_ptr) const;

  void add_obj(HeapWord* addr, size_t len);
  void add_obj(oop p, size_t len) { add_obj(cast_from_oop<HeapWord*>(p), len); }

  // Fill in the regions covering [beg, end) so that no data moves; i.e., the
  // destination of region n is simply the start of region n.  Both arguments
  // beg and end must be region-aligned.
  void summarize_dense_prefix(HeapWord* beg, HeapWord* end);

  HeapWord* summarize_split_space(size_t src_region, SplitInfo& split_info,
                                  HeapWord* destination, HeapWord* target_end,
                                  HeapWord** target_next);
  bool summarize(SplitInfo& split_info,
                 HeapWord* source_beg, HeapWord* source_end,
                 HeapWord** source_next,
                 HeapWord* target_beg, HeapWord* target_end,
                 HeapWord** target_next);

  void clear();
  void clear_range(size_t beg_region, size_t end_region);
  void clear_range(HeapWord* beg, HeapWord* end) {
    clear_range(addr_to_region_idx(beg), addr_to_region_idx(end));
  }

  // Return the number of words between addr and the start of the region
  // containing addr.
  inline size_t     region_offset(const HeapWord* addr) const;

  // Convert addresses to/from a region index or region pointer.
  inline size_t     addr_to_region_idx(const HeapWord* addr) const;
  inline RegionData* addr_to_region_ptr(const HeapWord* addr) const;
  inline HeapWord*  region_to_addr(size_t region) const;
  inline HeapWord*  region_to_addr(size_t region, size_t offset) const;
  inline HeapWord*  region_to_addr(const RegionData* region) const;

  inline HeapWord*  region_align_down(HeapWord* addr) const;
  inline HeapWord*  region_align_up(HeapWord* addr) const;
  inline bool       is_region_aligned(HeapWord* addr) const;

  // Analogous to region_offset() for blocks.
  size_t     block_offset(const HeapWord* addr) const;
  size_t     addr_to_block_idx(const HeapWord* addr) const;
  size_t     addr_to_block_idx(const oop obj) const {
    return addr_to_block_idx(cast_from_oop<HeapWord*>(obj));
  }
  inline BlockData* addr_to_block_ptr(const HeapWord* addr) const;
  inline HeapWord*  block_to_addr(size_t block) const;
  inline size_t     region_to_block_idx(size_t region) const;

  inline HeapWord*  block_align_down(HeapWord* addr) const;
  inline HeapWord*  block_align_up(HeapWord* addr) const;
  inline bool       is_block_aligned(HeapWord* addr) const;

  // Return the address one past the end of the partial object.
  HeapWord* partial_obj_end(size_t region_idx) const;

  // Return the location of the object after compaction.
  HeapWord* calc_new_pointer(HeapWord* addr, ParCompactionManager* cm) const;

  HeapWord* calc_new_pointer(oop p, ParCompactionManager* cm) const {
    return calc_new_pointer(cast_from_oop<HeapWord*>(p), cm);
  }

#ifdef  ASSERT
  void verify_clear(const PSVirtualSpace* vspace);
  void verify_clear();
#endif  // #ifdef ASSERT

private:
  bool initialize_block_data();
  bool initialize_region_data(size_t region_size);
  PSVirtualSpace* create_vspace(size_t count, size_t element_size);

private:
  HeapWord*       _region_start;
#ifdef  ASSERT
  HeapWord*       _region_end;
#endif  // #ifdef ASSERT

  PSVirtualSpace* _region_vspace;
  size_t          _reserved_byte_size;
  RegionData*     _region_data;
  size_t          _region_count;

  PSVirtualSpace* _block_vspace;
  BlockData*      _block_data;
  size_t          _block_count;
};

inline uint
ParallelCompactData::RegionData::destination_count_raw() const
{
  return _dc_and_los & dc_mask;
}

inline uint
ParallelCompactData::RegionData::destination_count() const
{
  return destination_count_raw() >> dc_shift;
}

inline bool
ParallelCompactData::RegionData::blocks_filled() const
{
  bool result = _blocks_filled;
  OrderAccess::acquire();
  return result;
}

#ifdef ASSERT
inline size_t
ParallelCompactData::RegionData::blocks_filled_count() const
{
  return _blocks_filled_count;
}
#endif // #ifdef ASSERT

inline void
ParallelCompactData::RegionData::set_blocks_filled()
{
  OrderAccess::release();
  _blocks_filled = true;
  // Debug builds count the number of times the table was filled.
  DEBUG_ONLY(Atomic::inc(&_blocks_filled_count));
}

inline void
ParallelCompactData::RegionData::set_destination_count(uint count)
{
  assert(count <= (dc_completed >> dc_shift), "count too large");
  const region_sz_t live_sz = (region_sz_t) live_obj_size();
  _dc_and_los = (count << dc_shift) | live_sz;
}

inline void ParallelCompactData::RegionData::set_live_obj_size(size_t words)
{
  assert(words <= los_mask, "would overflow");
  _dc_and_los = destination_count_raw() | (region_sz_t)words;
}

inline void ParallelCompactData::RegionData::decrement_destination_count()
{
  assert(_dc_and_los < dc_claimed, "already claimed");
  assert(_dc_and_los >= dc_one, "count would go negative");
  Atomic::add(&_dc_and_los, dc_mask);
}

inline HeapWord* ParallelCompactData::RegionData::data_location() const
{
  DEBUG_ONLY(return _data_location;)
  NOT_DEBUG(return NULL;)
}

inline HeapWord* ParallelCompactData::RegionData::highest_ref() const
{
  DEBUG_ONLY(return _highest_ref;)
  NOT_DEBUG(return NULL;)
}

inline void ParallelCompactData::RegionData::set_data_location(HeapWord* addr)
{
  DEBUG_ONLY(_data_location = addr;)
}

inline void ParallelCompactData::RegionData::set_completed()
{
  assert(claimed(), "must be claimed first");
  _dc_and_los = dc_completed | (region_sz_t) live_obj_size();
}

// MT-unsafe claiming of a region.  Should only be used during single threaded
// execution.
inline bool ParallelCompactData::RegionData::claim_unsafe()
{
  if (available()) {
    _dc_and_los |= dc_claimed;
    return true;
  }
  return false;
}

inline void ParallelCompactData::RegionData::add_live_obj(size_t words)
{
  assert(words <= (size_t)los_mask - live_obj_size(), "overflow");
  Atomic::add(&_dc_and_los, static_cast<region_sz_t>(words));
}

inline void ParallelCompactData::RegionData::set_highest_ref(HeapWord* addr)
{
#ifdef ASSERT
  HeapWord* tmp = _highest_ref;
  while (addr > tmp) {
    tmp = Atomic::cmpxchg(&_highest_ref, tmp, addr);
  }
#endif  // #ifdef ASSERT
}

inline bool ParallelCompactData::RegionData::claim()
{
  const region_sz_t los = static_cast<region_sz_t>(live_obj_size());
  const region_sz_t old = Atomic::cmpxchg(&_dc_and_los, los, dc_claimed | los);
  return old == los;
}

inline bool ParallelCompactData::RegionData::mark_normal() {
  return Atomic::cmpxchg(&_shadow_state, UnusedRegion, NormalRegion) == UnusedRegion;
}

inline bool ParallelCompactData::RegionData::mark_shadow() {
  if (_shadow_state != UnusedRegion) return false;
  return Atomic::cmpxchg(&_shadow_state, UnusedRegion, ShadowRegion) == UnusedRegion;
}

inline void ParallelCompactData::RegionData::mark_filled() {
  int old = Atomic::cmpxchg(&_shadow_state, ShadowRegion, FilledShadow);
  assert(old == ShadowRegion, "Fail to mark the region as filled");
}

inline bool ParallelCompactData::RegionData::mark_copied() {
  return Atomic::cmpxchg(&_shadow_state, FilledShadow, CopiedShadow) == FilledShadow;
}

void ParallelCompactData::RegionData::shadow_to_normal() {
  int old = Atomic::cmpxchg(&_shadow_state, ShadowRegion, NormalRegion);
  assert(old == ShadowRegion, "Fail to mark the region as finish");
}

inline ParallelCompactData::RegionData*
ParallelCompactData::region(size_t region_idx) const
{
  assert(region_idx <= region_count(), "bad arg");
  return _region_data + region_idx;
}

inline size_t
ParallelCompactData::region(const RegionData* const region_ptr) const
{
  assert(region_ptr >= _region_data, "bad arg");
  assert(region_ptr <= _region_data + region_count(), "bad arg");
  return pointer_delta(region_ptr, _region_data, sizeof(RegionData));
}

inline ParallelCompactData::BlockData*
ParallelCompactData::block(size_t n) const {
  assert(n < block_count(), "bad arg");
  return _block_data + n;
}

inline size_t
ParallelCompactData::region_offset(const HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  // would mistakenly return 0 for _region_end
  assert(addr < _region_end, "bad addr");
  return (size_t(addr) & RegionAddrOffsetMask) >> LogHeapWordSize;
}

inline size_t
ParallelCompactData::addr_to_region_idx(const HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr " PTR_FORMAT " _region_start " PTR_FORMAT, p2i(addr), p2i(_region_start));
  assert(addr <= _region_end, "bad addr " PTR_FORMAT " _region_end " PTR_FORMAT, p2i(addr), p2i(_region_end));
  return pointer_delta(addr, _region_start) >> Log2RegionSize;
}

inline ParallelCompactData::RegionData*
ParallelCompactData::addr_to_region_ptr(const HeapWord* addr) const
{
  return region(addr_to_region_idx(addr));
}

inline HeapWord*
ParallelCompactData::region_to_addr(size_t region) const
{
  assert(region <= _region_count, "region out of range");
  return _region_start + (region << Log2RegionSize);
}

inline HeapWord*
ParallelCompactData::region_to_addr(const RegionData* region) const
{
  return region_to_addr(pointer_delta(region, _region_data,
                                      sizeof(RegionData)));
}

inline HeapWord*
ParallelCompactData::region_to_addr(size_t region, size_t offset) const
{
  assert(region <= _region_count, "region out of range");
  assert(offset < RegionSize, "offset too big");  // This may be too strict.
  return region_to_addr(region) + offset;
}

inline HeapWord*
ParallelCompactData::region_align_down(HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  assert(addr < _region_end + RegionSize, "bad addr");
  return (HeapWord*)(size_t(addr) & RegionAddrMask);
}

inline HeapWord*
ParallelCompactData::region_align_up(HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  assert(addr <= _region_end, "bad addr");
  return region_align_down(addr + RegionSizeOffsetMask);
}

inline bool
ParallelCompactData::is_region_aligned(HeapWord* addr) const
{
  return (size_t(addr) & RegionAddrOffsetMask) == 0;
}

inline size_t
ParallelCompactData::block_offset(const HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  assert(addr <= _region_end, "bad addr");
  return (size_t(addr) & BlockAddrOffsetMask) >> LogHeapWordSize;
}

inline size_t
ParallelCompactData::addr_to_block_idx(const HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  assert(addr <= _region_end, "bad addr");
  return pointer_delta(addr, _region_start) >> Log2BlockSize;
}

inline ParallelCompactData::BlockData*
ParallelCompactData::addr_to_block_ptr(const HeapWord* addr) const
{
  return block(addr_to_block_idx(addr));
}

inline HeapWord*
ParallelCompactData::block_to_addr(size_t block) const
{
  assert(block < _block_count, "block out of range");
  return _region_start + (block << Log2BlockSize);
}

inline size_t
ParallelCompactData::region_to_block_idx(size_t region) const
{
  return region << Log2BlocksPerRegion;
}

inline HeapWord*
ParallelCompactData::block_align_down(HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  assert(addr < _region_end + RegionSize, "bad addr");
  return (HeapWord*)(size_t(addr) & BlockAddrMask);
}

inline HeapWord*
ParallelCompactData::block_align_up(HeapWord* addr) const
{
  assert(addr >= _region_start, "bad addr");
  assert(addr <= _region_end, "bad addr");
  return block_align_down(addr + BlockSizeOffsetMask);
}

inline bool
ParallelCompactData::is_block_aligned(HeapWord* addr) const
{
  return block_offset(addr) == 0;
}

// Abstract closure for use with ParMarkBitMap::iterate(), which will invoke the
// do_addr() method.
//
// The closure is initialized with the number of heap words to process
// (words_remaining()), and becomes 'full' when it reaches 0.  The do_addr()
// methods in subclasses should update the total as words are processed.  Since
// only one subclass actually uses this mechanism to terminate iteration, the
// default initial value is > 0.  The implementation is here and not in the
// single subclass that uses it to avoid making is_full() virtual, and thus
// adding a virtual call per live object.

class ParMarkBitMapClosure: public StackObj {
 public:
  typedef ParMarkBitMap::idx_t idx_t;
  typedef ParMarkBitMap::IterationStatus IterationStatus;

 public:
  inline ParMarkBitMapClosure(ParMarkBitMap* mbm, ParCompactionManager* cm,
                              size_t words = max_uintx);

  inline ParCompactionManager* compaction_manager() const;
  inline ParMarkBitMap*        bitmap() const;
  inline size_t                words_remaining() const;
  inline bool                  is_full() const;
  inline HeapWord*             source() const;

  inline void                  set_source(HeapWord* addr);

  virtual IterationStatus do_addr(HeapWord* addr, size_t words) = 0;

 protected:
  inline void decrement_words_remaining(size_t words);

 private:
  ParMarkBitMap* const        _bitmap;
  ParCompactionManager* const _compaction_manager;
  DEBUG_ONLY(const size_t     _initial_words_remaining;) // Useful in debugger.
  size_t                      _words_remaining; // Words left to copy.

 protected:
  HeapWord*                   _source;          // Next addr that would be read.
};

inline
ParMarkBitMapClosure::ParMarkBitMapClosure(ParMarkBitMap* bitmap,
                                           ParCompactionManager* cm,
                                           size_t words):
  _bitmap(bitmap), _compaction_manager(cm)
#ifdef  ASSERT
  , _initial_words_remaining(words)
#endif
{
  _words_remaining = words;
  _source = NULL;
}

inline ParCompactionManager* ParMarkBitMapClosure::compaction_manager() const {
  return _compaction_manager;
}

inline ParMarkBitMap* ParMarkBitMapClosure::bitmap() const {
  return _bitmap;
}

inline size_t ParMarkBitMapClosure::words_remaining() const {
  return _words_remaining;
}

inline bool ParMarkBitMapClosure::is_full() const {
  return words_remaining() == 0;
}

inline HeapWord* ParMarkBitMapClosure::source() const {
  return _source;
}

inline void ParMarkBitMapClosure::set_source(HeapWord* addr) {
  _source = addr;
}

inline void ParMarkBitMapClosure::decrement_words_remaining(size_t words) {
  assert(_words_remaining >= words, "processed too many words");
  _words_remaining -= words;
}

// The Parallel collector is a stop-the-world garbage collector that
// does parts of the collection using parallel threads.  The collection includes
// the tenured generation and the young generation.
//
// There are four phases of the collection.
//
//      - marking phase
//      - summary phase
//      - compacting phase
//      - clean up phase
//
// Roughly speaking these phases correspond, respectively, to
//      - mark all the live objects
//      - calculate the destination of each object at the end of the collection
//      - move the objects to their destination
//      - update some references and reinitialize some variables
//
// These three phases are invoked in PSParallelCompact::invoke_no_policy().  The
// marking phase is implemented in PSParallelCompact::marking_phase() and does a
// complete marking of the heap.  The summary phase is implemented in
// PSParallelCompact::summary_phase().  The move and update phase is implemented
// in PSParallelCompact::compact().
//
// A space that is being collected is divided into regions and with each region
// is associated an object of type ParallelCompactData.  Each region is of a
// fixed size and typically will contain more than 1 object and may have parts
// of objects at the front and back of the region.
//
// region            -----+---------------------+----------
// objects covered   [ AAA  )[ BBB )[ CCC   )[ DDD     )
//
// The marking phase does a complete marking of all live objects in the heap.
// The marking also compiles the size of the data for all live objects covered
// by the region.  This size includes the part of any live object spanning onto
// the region (part of AAA if it is live) from the front, all live objects
// contained in the region (BBB and/or CCC if they are live), and the part of
// any live objects covered by the region that extends off the region (part of
// DDD if it is live).  The marking phase uses multiple GC threads and marking
// is done in a bit array of type ParMarkBitMap.  The marking of the bit map is
// done atomically as is the accumulation of the size of the live objects
// covered by a region.
//
// The summary phase calculates the total live data to the left of each region
// XXX.  Based on that total and the bottom of the space, it can calculate the
// starting location of the live data in XXX.  The summary phase calculates for
// each region XXX quantities such as
//
//      - the amount of live data at the beginning of a region from an object
//        entering the region.
//      - the location of the first live data on the region
//      - a count of the number of regions receiving live data from XXX.
//
// See ParallelCompactData for precise details.  The summary phase also
// calculates the dense prefix for the compaction.  The dense prefix is a
// portion at the beginning of the space that is not moved.  The objects in the
// dense prefix do need to have their object references updated.  See method
// summarize_dense_prefix().
//
// The summary phase is done using 1 GC thread.
//
// The compaction phase moves objects to their new location and updates all
// references in the object.
//
// A current exception is that objects that cross a region boundary are moved
// but do not have their references updated.  References are not updated because
// it cannot easily be determined if the klass pointer KKK for the object AAA
// has been updated.  KKK likely resides in a region to the left of the region
// containing AAA.  These AAA's have there references updated at the end in a
// clean up phase.  See the method PSParallelCompact::update_deferred_objects().
// An alternate strategy is being investigated for this deferral of updating.
//
// Compaction is done on a region basis.  A region that is ready to be filled is
// put on a ready list and GC threads take region off the list and fill them.  A
// region is ready to be filled if it empty of live objects.  Such a region may
// have been initially empty (only contained dead objects) or may have had all
// its live objects copied out already.  A region that compacts into itself is
// also ready for filling.  The ready list is initially filled with empty
// regions and regions compacting into themselves.  There is always at least 1
// region that can be put on the ready list.  The regions are atomically added
// and removed from the ready list.

class TaskQueue;

class PSParallelCompact : AllStatic {
 public:
  // Convenient access to type names.
  typedef ParMarkBitMap::idx_t idx_t;
  typedef ParallelCompactData::RegionData RegionData;
  typedef ParallelCompactData::BlockData BlockData;

  typedef enum {
    old_space_id, eden_space_id,
    from_space_id, to_space_id, last_space_id
  } SpaceId;

  struct UpdateDensePrefixTask : public CHeapObj<mtGC> {
    SpaceId _space_id;
    size_t _region_index_start;
    size_t _region_index_end;

    UpdateDensePrefixTask() :
        _space_id(SpaceId(0)),
        _region_index_start(0),
        _region_index_end(0) {}

    UpdateDensePrefixTask(SpaceId space_id,
                          size_t region_index_start,
                          size_t region_index_end) :
        _space_id(space_id),
        _region_index_start(region_index_start),
        _region_index_end(region_index_end) {}
  };

 public:
  // Inline closure decls
  //
  class IsAliveClosure: public BoolObjectClosure {
   public:
    virtual bool do_object_b(oop p);
  };

  friend class RefProcTaskProxy;
  friend class PSParallelCompactTest;

 private:
  static STWGCTimer           _gc_timer;
  static ParallelOldTracer    _gc_tracer;
  static elapsedTimer         _accumulated_time;
  static unsigned int         _total_invocations;
  static unsigned int         _maximum_compaction_gc_num;
  static CollectorCounters*   _counters;
  static ParMarkBitMap        _mark_bitmap;
  static ParallelCompactData  _summary_data;
  static IsAliveClosure       _is_alive_closure;
  static SpaceInfo            _space_info[last_space_id];

  // Reference processing (used in ...follow_contents)
  static SpanSubjectToDiscoveryClosure  _span_based_discoverer;
  static ReferenceProcessor*  _ref_processor;

  // Values computed at initialization and used by dead_wood_limiter().
  static double _dwl_mean;
  static double _dwl_std_dev;
  static double _dwl_first_term;
  static double _dwl_adjustment;
#ifdef  ASSERT
  static bool   _dwl_initialized;
#endif  // #ifdef ASSERT

 public:
  static ParallelOldTracer* gc_tracer() { return &_gc_tracer; }

 private:

  static void initialize_space_info();

  // Clear the marking bitmap and summary data that cover the specified space.
  static void clear_data_covering_space(SpaceId id);

  static void pre_compact();
  static void post_compact();

  // Mark live objects
  static void marking_phase(ParCompactionManager* cm,
                            bool maximum_heap_compaction,
                            ParallelOldTracer *gc_tracer);

  // Compute the dense prefix for the designated space.  This is an experimental
  // implementation currently not used in production.
  static HeapWord* compute_dense_prefix_via_density(const SpaceId id,
                                                    bool maximum_compaction);

  // Methods used to compute the dense prefix.

  // Compute the value of the normal distribution at x = density.  The mean and
  // standard deviation are values saved by initialize_dead_wood_limiter().
  static inline double normal_distribution(double density);

  // Initialize the static vars used by dead_wood_limiter().
  static void initialize_dead_wood_limiter();

  // Return the percentage of space that can be treated as "dead wood" (i.e.,
  // not reclaimed).
  static double dead_wood_limiter(double density, size_t min_percent);

  // Find the first (left-most) region in the range [beg, end) that has at least
  // dead_words of dead space to the left.  The argument beg must be the first
  // region in the space that is not completely live.
  static RegionData* dead_wood_limit_region(const RegionData* beg,
                                            const RegionData* end,
                                            size_t dead_words);

  // Return a pointer to the first region in the range [beg, end) that is not
  // completely full.
  static RegionData* first_dead_space_region(const RegionData* beg,
                                             const RegionData* end);

  // Return a value indicating the benefit or 'yield' if the compacted region
  // were to start (or equivalently if the dense prefix were to end) at the
  // candidate region.  Higher values are better.
  //
  // The value is based on the amount of space reclaimed vs. the costs of (a)
  // updating references in the dense prefix plus (b) copying objects and
  // updating references in the compacted region.
  static inline double reclaimed_ratio(const RegionData* const candidate,
                                       HeapWord* const bottom,
                                       HeapWord* const top,
                                       HeapWord* const new_top);

  // Compute the dense prefix for the designated space.
  static HeapWord* compute_dense_prefix(const SpaceId id,
                                        bool maximum_compaction);

  // Return true if dead space crosses onto the specified Region; bit must be
  // the bit index corresponding to the first word of the Region.
  static inline bool dead_space_crosses_boundary(const RegionData* region,
                                                 idx_t bit);

  // Summary phase utility routine to fill dead space (if any) at the dense
  // prefix boundary.  Should only be called if the the dense prefix is
  // non-empty.
  static void fill_dense_prefix_end(SpaceId id);

  static void summarize_spaces_quick();
  static void summarize_space(SpaceId id, bool maximum_compaction);
  static void summary_phase(ParCompactionManager* cm, bool maximum_compaction);

  // Adjust addresses in roots.  Does not adjust addresses in heap.
  static void adjust_roots();

  DEBUG_ONLY(static void write_block_fill_histogram();)

  // Move objects to new locations.
  static void compact_perm(ParCompactionManager* cm);
  static void compact();

  // Add available regions to the stack and draining tasks to the task queue.
  static void prepare_region_draining_tasks(uint parallel_gc_threads);

  // Add dense prefix update tasks to the task queue.
  static void enqueue_dense_prefix_tasks(TaskQueue& task_queue,
                                         uint parallel_gc_threads);

#ifndef PRODUCT
  // Print generic summary data
  static void print_generic_summary_data(ParallelCompactData& summary_data,
                                         HeapWord* const beg_addr,
                                         HeapWord* const end_addr);
#endif  // #ifndef PRODUCT

 public:

  PSParallelCompact();

  static void invoke(bool maximum_heap_compaction);
  static bool invoke_no_policy(bool maximum_heap_compaction);

  static void post_initialize();
  // Perform initialization for PSParallelCompact that requires
  // allocations.  This should be called during the VM initialization
  // at a pointer where it would be appropriate to return a JNI_ENOMEM
  // in the event of a failure.
  static bool initialize();

  // Closure accessors
  static BoolObjectClosure* is_alive_closure()     { return &_is_alive_closure; }

  // Public accessors
  static elapsedTimer* accumulated_time() { return &_accumulated_time; }
  static unsigned int total_invocations() { return _total_invocations; }
  static CollectorCounters* counters()    { return _counters; }

  // Marking support
  static inline bool mark_obj(oop obj);
  static inline bool is_marked(oop obj);

  template <class T> static inline void adjust_pointer(T* p, ParCompactionManager* cm);

  // Compaction support.
  // Return true if p is in the range [beg_addr, end_addr).
  static inline bool is_in(HeapWord* p, HeapWord* beg_addr, HeapWord* end_addr);
  static inline bool is_in(oop* p, HeapWord* beg_addr, HeapWord* end_addr);

  // Convenience wrappers for per-space data kept in _space_info.
  static inline MutableSpace*     space(SpaceId space_id);
  static inline HeapWord*         new_top(SpaceId space_id);
  static inline HeapWord*         dense_prefix(SpaceId space_id);
  static inline ObjectStartArray* start_array(SpaceId space_id);

  // Process the end of the given region range in the dense prefix.
  // This includes saving any object not updated.
  static void dense_prefix_regions_epilogue(ParCompactionManager* cm,
                                            size_t region_start_index,
                                            size_t region_end_index,
                                            idx_t exiting_object_offset,
                                            idx_t region_offset_start,
                                            idx_t region_offset_end);

  // Update a region in the dense prefix.  For each live object
  // in the region, update it's interior references.  For each
  // dead object, fill it with deadwood. Dead space at the end
  // of a region range will be filled to the start of the next
  // live object regardless of the region_index_end.  None of the
  // objects in the dense prefix move and dead space is dead
  // (holds only dead objects that don't need any processing), so
  // dead space can be filled in any order.
  static void update_and_deadwood_in_dense_prefix(ParCompactionManager* cm,
                                                  SpaceId space_id,
                                                  size_t region_index_start,
                                                  size_t region_index_end);

  // Return the address of the count + 1st live word in the range [beg, end).
  static HeapWord* skip_live_words(HeapWord* beg, HeapWord* end, size_t count);

  // Return the address of the word to be copied to dest_addr, which must be
  // aligned to a region boundary.
  static HeapWord* first_src_addr(HeapWord* const dest_addr,
                                  SpaceId src_space_id,
                                  size_t src_region_idx);

  // Determine the next source region, set closure.source() to the start of the
  // new region return the region index.  Parameter end_addr is the address one
  // beyond the end of source range just processed.  If necessary, switch to a
  // new source space and set src_space_id (in-out parameter) and src_space_top
  // (out parameter) accordingly.
  static size_t next_src_region(MoveAndUpdateClosure& closure,
                                SpaceId& src_space_id,
                                HeapWord*& src_space_top,
                                HeapWord* end_addr);

  // Decrement the destination count for each non-empty source region in the
  // range [beg_region, region(region_align_up(end_addr))).  If the destination
  // count for a region goes to 0 and it needs to be filled, enqueue it.
  static void decrement_destination_counts(ParCompactionManager* cm,
                                           SpaceId src_space_id,
                                           size_t beg_region,
                                           HeapWord* end_addr);

  static void fill_region(ParCompactionManager* cm, MoveAndUpdateClosure& closure, size_t region);
  static void fill_and_update_region(ParCompactionManager* cm, size_t region);

  static bool steal_unavailable_region(ParCompactionManager* cm, size_t& region_idx);
  static void fill_and_update_shadow_region(ParCompactionManager* cm, size_t region);
  // Copy the content of a shadow region back to its corresponding heap region
  static void copy_back(HeapWord* shadow_addr, HeapWord* region_addr);
  // Collect empty regions as shadow regions and initialize the
  // _next_shadow_region filed for each compact manager
  static void initialize_shadow_regions(uint parallel_gc_threads);

  // Fill in the block table for the specified region.
  static void fill_blocks(size_t region_idx);

  // Update the deferred objects in the space.
  static void update_deferred_objects(ParCompactionManager* cm, SpaceId id);

  static ParMarkBitMap* mark_bitmap() { return &_mark_bitmap; }
  static ParallelCompactData& summary_data() { return _summary_data; }

  // Reference Processing
  static ReferenceProcessor* const ref_processor() { return _ref_processor; }

  static STWGCTimer* gc_timer() { return &_gc_timer; }

  // Return the SpaceId for the given address.
  static SpaceId space_id(HeapWord* addr);

  static void print_on_error(outputStream* st);

#ifndef PRODUCT
  // Debugging support.
  static const char* space_names[last_space_id];
  static void print_region_ranges();
  static void print_dense_prefix_stats(const char* const algorithm,
                                       const SpaceId id,
                                       const bool maximum_compaction,
                                       HeapWord* const addr);
  static void summary_phase_msg(SpaceId dst_space_id,
                                HeapWord* dst_beg, HeapWord* dst_end,
                                SpaceId src_space_id,
                                HeapWord* src_beg, HeapWord* src_end);
#endif  // #ifndef PRODUCT

#ifdef  ASSERT
  // Sanity check the new location of a word in the heap.
  static inline void check_new_location(HeapWord* old_addr, HeapWord* new_addr);
  // Verify that all the regions have been emptied.
  static void verify_complete(SpaceId space_id);
#endif  // #ifdef ASSERT
};

class MoveAndUpdateClosure: public ParMarkBitMapClosure {
  static inline size_t calculate_words_remaining(size_t region);
 public:
  inline MoveAndUpdateClosure(ParMarkBitMap* bitmap, ParCompactionManager* cm,
                              size_t region);

  // Accessors.
  HeapWord* destination() const         { return _destination; }
  HeapWord* copy_destination() const    { return _destination + _offset; }

  // If the object will fit (size <= words_remaining()), copy it to the current
  // destination, update the interior oops and the start array and return either
  // full (if the closure is full) or incomplete.  If the object will not fit,
  // return would_overflow.
  IterationStatus do_addr(HeapWord* addr, size_t size);

  // Copy enough words to fill this closure, starting at source().  Interior
  // oops and the start array are not updated.  Return full.
  IterationStatus copy_until_full();

  // Copy enough words to fill this closure or to the end of an object,
  // whichever is smaller, starting at source().  Interior oops and the start
  // array are not updated.
  void copy_partial_obj();

  virtual void complete_region(ParCompactionManager* cm, HeapWord* dest_addr,
                               PSParallelCompact::RegionData* region_ptr);

protected:
  // Update variables to indicate that word_count words were processed.
  inline void update_state(size_t word_count);

 protected:
  HeapWord*               _destination;         // Next addr to be written.
  ObjectStartArray* const _start_array;
  size_t                  _offset;
};

inline size_t MoveAndUpdateClosure::calculate_words_remaining(size_t region) {
  HeapWord* dest_addr = PSParallelCompact::summary_data().region_to_addr(region);
  PSParallelCompact::SpaceId dest_space_id = PSParallelCompact::space_id(dest_addr);
  HeapWord* new_top = PSParallelCompact::new_top(dest_space_id);
  assert(dest_addr < new_top, "sanity");

  return MIN2(pointer_delta(new_top, dest_addr), ParallelCompactData::RegionSize);
}

inline
MoveAndUpdateClosure::MoveAndUpdateClosure(ParMarkBitMap* bitmap,
                                           ParCompactionManager* cm,
                                           size_t region_idx) :
  ParMarkBitMapClosure(bitmap, cm, calculate_words_remaining(region_idx)),
  _destination(PSParallelCompact::summary_data().region_to_addr(region_idx)),
  _start_array(PSParallelCompact::start_array(PSParallelCompact::space_id(_destination))),
  _offset(0) { }


inline void MoveAndUpdateClosure::update_state(size_t words)
{
  decrement_words_remaining(words);
  _source += words;
  _destination += words;
}

class MoveAndUpdateShadowClosure: public MoveAndUpdateClosure {
  inline size_t calculate_shadow_offset(size_t region_idx, size_t shadow_idx);
public:
  inline MoveAndUpdateShadowClosure(ParMarkBitMap* bitmap, ParCompactionManager* cm,
                       size_t region, size_t shadow);

  virtual void complete_region(ParCompactionManager* cm, HeapWord* dest_addr,
                               PSParallelCompact::RegionData* region_ptr);

private:
  size_t _shadow;
};

inline size_t MoveAndUpdateShadowClosure::calculate_shadow_offset(size_t region_idx, size_t shadow_idx) {
  ParallelCompactData& sd = PSParallelCompact::summary_data();
  HeapWord* dest_addr = sd.region_to_addr(region_idx);
  HeapWord* shadow_addr = sd.region_to_addr(shadow_idx);
  return pointer_delta(shadow_addr, dest_addr);
}

inline
MoveAndUpdateShadowClosure::MoveAndUpdateShadowClosure(ParMarkBitMap *bitmap,
                                                       ParCompactionManager *cm,
                                                       size_t region,
                                                       size_t shadow) :
  MoveAndUpdateClosure(bitmap, cm, region),
  _shadow(shadow) {
  _offset = calculate_shadow_offset(region, shadow);
}

class UpdateOnlyClosure: public ParMarkBitMapClosure {
 private:
  const PSParallelCompact::SpaceId _space_id;
  ObjectStartArray* const          _start_array;

 public:
  UpdateOnlyClosure(ParMarkBitMap* mbm,
                    ParCompactionManager* cm,
                    PSParallelCompact::SpaceId space_id);

  // Update the object.
  virtual IterationStatus do_addr(HeapWord* addr, size_t words);

  inline void do_addr(HeapWord* addr);
};

class FillClosure: public ParMarkBitMapClosure {
 public:
  FillClosure(ParCompactionManager* cm, PSParallelCompact::SpaceId space_id);

  virtual IterationStatus do_addr(HeapWord* addr, size_t size);

 private:
  ObjectStartArray* const _start_array;
};

void steal_marking_work(TaskTerminator& terminator, uint worker_id);

#endif // SHARE_GC_PARALLEL_PSPARALLELCOMPACT_HPP
