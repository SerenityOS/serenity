/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/heap.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/mutexLocker.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/powerOfTwo.hpp"

// Implementation of Heap

CodeHeap::CodeHeap(const char* name, const int code_blob_type)
  : _code_blob_type(code_blob_type) {
  _name                         = name;
  _number_of_committed_segments = 0;
  _number_of_reserved_segments  = 0;
  _segment_size                 = 0;
  _log2_segment_size            = 0;
  _next_segment                 = 0;
  _freelist                     = NULL;
  _last_insert_point            = NULL;
  _freelist_segments            = 0;
  _freelist_length              = 0;
  _max_allocated_capacity       = 0;
  _blob_count                   = 0;
  _nmethod_count                = 0;
  _adapter_count                = 0;
  _full_count                   = 0;
  _fragmentation_count          = 0;
}

// Dummy initialization of template array.
char CodeHeap::segmap_template[] = {0};

// This template array is used to (re)initialize the segmap,
// replacing a 1..254 loop.
void CodeHeap::init_segmap_template() {
  assert(free_sentinel == 255, "Segment map logic changed!");
  for (int i = 0; i <= free_sentinel; i++) {
    segmap_template[i] = i;
  }
}

// The segmap is marked free for that part of the heap
// which has not been allocated yet (beyond _next_segment).
// The range of segments to be marked is given by [beg..end).
// "Allocated" space in this context means there exists a
// HeapBlock or a FreeBlock describing this space.
// This method takes segment map indices as range boundaries
void CodeHeap::mark_segmap_as_free(size_t beg, size_t end) {
  assert(             beg <  _number_of_committed_segments, "interval begin out of bounds");
  assert(beg < end && end <= _number_of_committed_segments, "interval end   out of bounds");
  // Don't do unpredictable things in PRODUCT build
  if (beg < end) {
    // setup _segmap pointers for faster indexing
    address p = (address)_segmap.low() + beg;
    address q = (address)_segmap.low() + end;
    // initialize interval
    memset(p, free_sentinel, q-p);
  }
}

// Don't get confused here.
// All existing blocks, no matter if they are used() or free(),
// have their segmap marked as used. This allows to find the
// block header (HeapBlock or FreeBlock) for any pointer
// within the allocated range (upper limit: _next_segment).
// This method takes segment map indices as range boundaries.
// The range of segments to be marked is given by [beg..end).
void CodeHeap::mark_segmap_as_used(size_t beg, size_t end, bool is_FreeBlock_join) {
  assert(             beg <  _number_of_committed_segments, "interval begin out of bounds");
  assert(beg < end && end <= _number_of_committed_segments, "interval end   out of bounds");
  // Don't do unpredictable things in PRODUCT build
  if (beg < end) {
    // setup _segmap pointers for faster indexing
    address p = (address)_segmap.low() + beg;
    address q = (address)_segmap.low() + end;
    // initialize interval
    // If we are joining two free blocks, the segmap range for each
    // block is consistent. To create a consistent segmap range for
    // the blocks combined, we have three choices:
    //  1 - Do a full init from beg to end. Not very efficient because
    //      the segmap range for the left block is potentially initialized
    //      over and over again.
    //  2 - Carry over the last segmap element value of the left block
    //      and initialize the segmap range of the right block starting
    //      with that value. Saves initializing the left block's segmap
    //      over and over again. Very efficient if FreeBlocks mostly
    //      are appended to the right.
    //  3 - Take full advantage of the segmap being almost correct with
    //      the two blocks combined. Lets assume the left block consists
    //      of m segments. The the segmap looks like
    //        ... (m-2) (m-1) (m) 0  1  2  3 ...
    //      By substituting the '0' by '1', we create a valid, but
    //      suboptimal, segmap range covering the two blocks combined.
    //      We introduced an extra hop for the find_block_for() iteration.
    //
    // When this method is called with is_FreeBlock_join == true, the
    // segmap index beg must select the first segment of the right block.
    // Otherwise, it has to select the first segment of the left block.
    // Variant 3 is used for all FreeBlock joins.
    if (is_FreeBlock_join && (beg > 0)) {
#ifndef PRODUCT
      FreeBlock* pBlock = (FreeBlock*)block_at(beg);
      assert(beg + pBlock->length() == end, "Internal error: (%d - %d) != %d", (unsigned int)end, (unsigned int)beg, (unsigned int)(pBlock->length()));
      assert(*p == 0, "Begin index does not select a block start segment, *p = %2.2x", *p);
#endif
      // If possible, extend the previous hop.
      if (*(p-1) < (free_sentinel-1)) {
        *p = *(p-1) + 1;
      } else {
        *p = 1;
      }
      if (_fragmentation_count++ >= fragmentation_limit) {
        defrag_segmap(true);
        _fragmentation_count = 0;
      }
    } else {
      size_t n_bulk = free_sentinel-1; // bulk processing uses template indices [1..254].
      // Use shortcut for blocks <= 255 segments.
      // Special case bulk processing: [0..254].
      if ((end - beg) <= n_bulk) {
        memcpy(p, &segmap_template[0], end - beg);
      } else {
        *p++  = 0;  // block header marker
        while (p < q) {
          if ((p+n_bulk) <= q) {
            memcpy(p, &segmap_template[1], n_bulk);
            p += n_bulk;
          } else {
            memcpy(p, &segmap_template[1], q-p);
            p = q;
          }
        }
      }
    }
  }
}

void CodeHeap::invalidate(size_t beg, size_t end, size_t hdr_size) {
#ifndef PRODUCT
  // Fill the given range with some bad value.
  // length is expected to be in segment_size units.
  // This prevents inadvertent execution of code leftover from previous use.
  char* p = low_boundary() + segments_to_size(beg) + hdr_size;
  memset(p, badCodeHeapNewVal, segments_to_size(end-beg)-hdr_size);
#endif
}

void CodeHeap::clear(size_t beg, size_t end) {
  mark_segmap_as_free(beg, end);
  invalidate(beg, end, 0);
}

void CodeHeap::clear() {
  _next_segment = 0;
  clear(_next_segment, _number_of_committed_segments);
}


static size_t align_to_page_size(size_t size) {
  const size_t alignment = (size_t)os::vm_page_size();
  assert(is_power_of_2(alignment), "no kidding ???");
  return (size + alignment - 1) & ~(alignment - 1);
}


void CodeHeap::on_code_mapping(char* base, size_t size) {
#ifdef LINUX
  extern void linux_wrap_code(char* base, size_t size);
  linux_wrap_code(base, size);
#endif
}


bool CodeHeap::reserve(ReservedSpace rs, size_t committed_size, size_t segment_size) {
  assert(rs.size() >= committed_size, "reserved < committed");
  assert(segment_size >= sizeof(FreeBlock), "segment size is too small");
  assert(is_power_of_2(segment_size), "segment_size must be a power of 2");
  assert_locked_or_safepoint(CodeCache_lock);

  _segment_size      = segment_size;
  _log2_segment_size = exact_log2(segment_size);

  // Reserve and initialize space for _memory.
  const size_t page_size = rs.page_size();
  const size_t granularity = os::vm_allocation_granularity();
  const size_t c_size = align_up(committed_size, page_size);
  assert(c_size <= rs.size(), "alignment made committed size to large");

  os::trace_page_sizes(_name, c_size, rs.size(), page_size,
                       rs.base(), rs.size());
  if (!_memory.initialize(rs, c_size)) {
    return false;
  }

  on_code_mapping(_memory.low(), _memory.committed_size());
  _number_of_committed_segments = size_to_segments(_memory.committed_size());
  _number_of_reserved_segments  = size_to_segments(_memory.reserved_size());
  assert(_number_of_reserved_segments >= _number_of_committed_segments, "just checking");
  const size_t reserved_segments_alignment = MAX2((size_t)os::vm_page_size(), granularity);
  const size_t reserved_segments_size = align_up(_number_of_reserved_segments, reserved_segments_alignment);
  const size_t committed_segments_size = align_to_page_size(_number_of_committed_segments);

  // reserve space for _segmap
  ReservedSpace seg_rs(reserved_segments_size);
  if (!_segmap.initialize(seg_rs, committed_segments_size)) {
    return false;
  }

  MemTracker::record_virtual_memory_type((address)_segmap.low_boundary(), mtCode);

  assert(_segmap.committed_size() >= (size_t) _number_of_committed_segments, "could not commit  enough space for segment map");
  assert(_segmap.reserved_size()  >= (size_t) _number_of_reserved_segments , "could not reserve enough space for segment map");
  assert(_segmap.reserved_size()  >= _segmap.committed_size()     , "just checking");

  // initialize remaining instance variables, heap memory and segmap
  clear();
  init_segmap_template();
  return true;
}


bool CodeHeap::expand_by(size_t size) {
  assert_locked_or_safepoint(CodeCache_lock);

  // expand _memory space
  size_t dm = align_to_page_size(_memory.committed_size() + size) - _memory.committed_size();
  if (dm > 0) {
    // Use at least the available uncommitted space if 'size' is larger
    if (_memory.uncommitted_size() != 0 && dm > _memory.uncommitted_size()) {
      dm = _memory.uncommitted_size();
    }
    char* base = _memory.low() + _memory.committed_size();
    if (!_memory.expand_by(dm)) return false;
    on_code_mapping(base, dm);
    size_t i = _number_of_committed_segments;
    _number_of_committed_segments = size_to_segments(_memory.committed_size());
    assert(_number_of_reserved_segments == size_to_segments(_memory.reserved_size()), "number of reserved segments should not change");
    assert(_number_of_reserved_segments >= _number_of_committed_segments, "just checking");
    // expand _segmap space
    size_t ds = align_to_page_size(_number_of_committed_segments) - _segmap.committed_size();
    if ((ds > 0) && !_segmap.expand_by(ds)) {
      return false;
    }
    assert(_segmap.committed_size() >= (size_t) _number_of_committed_segments, "just checking");
    // initialize additional space (heap memory and segmap)
    clear(i, _number_of_committed_segments);
  }
  return true;
}


void* CodeHeap::allocate(size_t instance_size) {
  size_t number_of_segments = size_to_segments(instance_size + header_size());
  assert(segments_to_size(number_of_segments) >= sizeof(FreeBlock), "not enough room for FreeList");
  assert_locked_or_safepoint(CodeCache_lock);

  // First check if we can satisfy request from freelist
  NOT_PRODUCT(verify());
  HeapBlock* block = search_freelist(number_of_segments);
  NOT_PRODUCT(verify());

  if (block != NULL) {
    assert(!block->free(), "must not be marked free");
    guarantee((char*) block >= _memory.low_boundary() && (char*) block < _memory.high(),
              "The newly allocated block " INTPTR_FORMAT " is not within the heap "
              "starting with "  INTPTR_FORMAT " and ending with "  INTPTR_FORMAT,
              p2i(block), p2i(_memory.low_boundary()), p2i(_memory.high()));
    _max_allocated_capacity = MAX2(_max_allocated_capacity, allocated_capacity());
    _blob_count++;
    return block->allocated_space();
  }

  // Ensure minimum size for allocation to the heap.
  number_of_segments = MAX2((int)CodeCacheMinBlockLength, (int)number_of_segments);

  if (_next_segment + number_of_segments <= _number_of_committed_segments) {
    mark_segmap_as_used(_next_segment, _next_segment + number_of_segments, false);
    block = block_at(_next_segment);
    block->initialize(number_of_segments);
    _next_segment += number_of_segments;
    guarantee((char*) block >= _memory.low_boundary() && (char*) block < _memory.high(),
              "The newly allocated block " INTPTR_FORMAT " is not within the heap "
              "starting with "  INTPTR_FORMAT " and ending with " INTPTR_FORMAT,
              p2i(block), p2i(_memory.low_boundary()), p2i(_memory.high()));
    _max_allocated_capacity = MAX2(_max_allocated_capacity, allocated_capacity());
    _blob_count++;
    return block->allocated_space();
  } else {
    return NULL;
  }
}

// Split the given block into two at the given segment.
// This is helpful when a block was allocated too large
// to trim off the unused space at the end (interpreter).
// It also helps with splitting a large free block during allocation.
// Usage state (used or free) must be set by caller since
// we don't know if the resulting blocks will be used or free.
// split_at is the segment number (relative to segment_for(b))
//          where the split happens. The segment with relative
//          number split_at is the first segment of the split-off block.
HeapBlock* CodeHeap::split_block(HeapBlock *b, size_t split_at) {
  if (b == NULL) return NULL;
  // After the split, both blocks must have a size of at least CodeCacheMinBlockLength
  assert((split_at >= CodeCacheMinBlockLength) && (split_at + CodeCacheMinBlockLength <= b->length()),
         "split position(%d) out of range [0..%d]", (int)split_at, (int)b->length());
  size_t split_segment = segment_for(b) + split_at;
  size_t b_size        = b->length();
  size_t newb_size     = b_size - split_at;

  HeapBlock* newb = block_at(split_segment);
  newb->set_length(newb_size);
  mark_segmap_as_used(segment_for(newb), segment_for(newb) + newb_size, false);
  b->set_length(split_at);
  return newb;
}

void CodeHeap::deallocate_tail(void* p, size_t used_size) {
  assert(p == find_start(p), "illegal deallocation");
  assert_locked_or_safepoint(CodeCache_lock);

  // Find start of HeapBlock
  HeapBlock* b = (((HeapBlock *)p) - 1);
  assert(b->allocated_space() == p, "sanity check");

  size_t actual_number_of_segments = b->length();
  size_t used_number_of_segments   = size_to_segments(used_size + header_size());
  size_t unused_number_of_segments = actual_number_of_segments - used_number_of_segments;
  guarantee(used_number_of_segments <= actual_number_of_segments, "Must be!");

  HeapBlock* f = split_block(b, used_number_of_segments);
  add_to_freelist(f);
  NOT_PRODUCT(verify());
}

void CodeHeap::deallocate(void* p) {
  assert(p == find_start(p), "illegal deallocation");
  assert_locked_or_safepoint(CodeCache_lock);

  // Find start of HeapBlock
  HeapBlock* b = (((HeapBlock *)p) - 1);
  assert(b->allocated_space() == p, "sanity check");
  guarantee((char*) b >= _memory.low_boundary() && (char*) b < _memory.high(),
            "The block to be deallocated " INTPTR_FORMAT " is not within the heap "
            "starting with "  INTPTR_FORMAT " and ending with " INTPTR_FORMAT,
            p2i(b), p2i(_memory.low_boundary()), p2i(_memory.high()));
  add_to_freelist(b);
  NOT_PRODUCT(verify());
}

/**
 * The segment map is used to quickly find the the start (header) of a
 * code block (e.g. nmethod) when only a pointer to a location inside the
 * code block is known. This works as follows:
 *  - The storage reserved for the code heap is divided into 'segments'.
 *  - The size of a segment is determined by -XX:CodeCacheSegmentSize=<#bytes>.
 *  - The size must be a power of two to allow the use of shift operations
 *    to quickly convert between segment index and segment address.
 *  - Segment start addresses should be aligned to be multiples of CodeCacheSegmentSize.
 *  - It seems beneficial for CodeCacheSegmentSize to be equal to os::page_size().
 *  - Allocation in the code cache can only happen at segment start addresses.
 *  - Allocation in the code cache is in units of CodeCacheSegmentSize.
 *  - A pointer in the code cache can be mapped to a segment by calling
 *    segment_for(addr).
 *  - The segment map is a byte array where array element [i] is related
 *    to the i-th segment in the code heap.
 *  - Each time memory is allocated/deallocated from the code cache,
 *    the segment map is updated accordingly.
 *    Note: deallocation does not cause the memory to become "free", as
 *          indicated by the segment map state "free_sentinel". Deallocation
 *          just changes the block state from "used" to "free".
 *  - Elements of the segment map (byte) array are interpreted
 *    as unsigned integer.
 *  - Element values normally identify an offset backwards (in segment
 *    size units) from the associated segment towards the start of
 *    the block.
 *  - Some values have a special meaning:
 *       0 - This segment is the start of a block (HeapBlock or FreeBlock).
 *     255 - The free_sentinel value. This is a free segment, i.e. it is
 *           not yet allocated and thus does not belong to any block.
 *  - The value of the current element has to be subtracted from the
 *    current index to get closer to the start.
 *  - If the value of the then current element is zero, the block start
 *    segment is found and iteration stops. Otherwise, start over with the
 *    previous step.
 *
 *    The following example illustrates a possible state of code cache
 *    and the segment map: (seg -> segment, nm ->nmethod)
 *
 *          code cache          segmap
 *         -----------        ---------
 * seg 1   | nm 1    |   ->   | 0     |
 * seg 2   | nm 1    |   ->   | 1     |
 * ...     | nm 1    |   ->   | ..    |
 * seg m-1 | nm 1    |   ->   | m-1   |
 * seg m   | nm 2    |   ->   | 0     |
 * seg m+1 | nm 2    |   ->   | 1     |
 * ...     | nm 2    |   ->   | 2     |
 * ...     | nm 2    |   ->   | ..    |
 * ...     | nm 2    |   ->   | 0xFE  | (free_sentinel-1)
 * ...     | nm 2    |   ->   | 1     |
 * seg m+n | nm 2    |   ->   | 2     |
 * ...     | nm 2    |   ->   |       |
 *
 * How to read:
 * A value of '0' in the segmap indicates that this segment contains the
 * beginning of a CodeHeap block. Let's walk through a simple example:
 *
 * We want to find the start of the block that contains nm 1, and we are
 * given a pointer that points into segment m-2. We then read the value
 * of segmap[m-2]. The value is an offset that points to the segment
 * which contains the start of the block.
 *
 * Another example: We want to locate the start of nm 2, and we happen to
 * get a pointer that points into seg m+n. We first read seg[n+m], which
 * returns '2'. So we have to update our segment map index (ix -= segmap[n+m])
 * and start over.
 */

// Find block which contains the passed pointer,
// regardless of the block being used or free.
// NULL is returned if anything invalid is detected.
void* CodeHeap::find_block_for(void* p) const {
  // Check the pointer to be in committed range.
  if (!contains(p)) {
    return NULL;
  }

  address seg_map = (address)_segmap.low();
  size_t  seg_idx = segment_for(p);

  // This may happen in special cases. Just ignore.
  // Example: PPC ICache stub generation.
  if (is_segment_unused(seg_map[seg_idx])) {
    return NULL;
  }

  // Iterate the segment map chain to find the start of the block.
  while (seg_map[seg_idx] > 0) {
    // Don't check each segment index to refer to a used segment.
    // This method is called extremely often. Therefore, any checking
    // has a significant impact on performance. Rely on CodeHeap::verify()
    // to do the job on request.
    seg_idx -= (int)seg_map[seg_idx];
  }

  return address_for(seg_idx);
}

// Find block which contains the passed pointer.
// The block must be used, i.e. must not be a FreeBlock.
// Return a pointer that points past the block header.
void* CodeHeap::find_start(void* p) const {
  HeapBlock* h = (HeapBlock*)find_block_for(p);
  return ((h == NULL) || h->free()) ? NULL : h->allocated_space();
}

// Find block which contains the passed pointer.
// Same as find_start(p), but with additional safety net.
CodeBlob* CodeHeap::find_blob_unsafe(void* start) const {
  CodeBlob* result = (CodeBlob*)CodeHeap::find_start(start);
  return (result != NULL && result->blob_contains((address)start)) ? result : NULL;
}

size_t CodeHeap::alignment_unit() const {
  // this will be a power of two
  return _segment_size;
}


size_t CodeHeap::alignment_offset() const {
  // The lowest address in any allocated block will be
  // equal to alignment_offset (mod alignment_unit).
  return sizeof(HeapBlock) & (_segment_size - 1);
}

// Returns the current block if available and used.
// If not, it returns the subsequent block (if available), NULL otherwise.
// Free blocks are merged, therefore there is at most one free block
// between two used ones. As a result, the subsequent block (if available) is
// guaranteed to be used.
// The returned pointer points past the block header.
void* CodeHeap::next_used(HeapBlock* b) const {
  if (b != NULL && b->free()) b = next_block(b);
  assert(b == NULL || !b->free(), "must be in use or at end of heap");
  return (b == NULL) ? NULL : b->allocated_space();
}

// Returns the first used HeapBlock
// The returned pointer points to the block header.
HeapBlock* CodeHeap::first_block() const {
  if (_next_segment > 0)
    return block_at(0);
  return NULL;
}

// The returned pointer points to the block header.
HeapBlock* CodeHeap::block_start(void* q) const {
  HeapBlock* b = (HeapBlock*)find_start(q);
  if (b == NULL) return NULL;
  return b - 1;
}

// Returns the next Heap block.
// The returned pointer points to the block header.
HeapBlock* CodeHeap::next_block(HeapBlock *b) const {
  if (b == NULL) return NULL;
  size_t i = segment_for(b) + b->length();
  if (i < _next_segment)
    return block_at(i);
  return NULL;
}


// Returns current capacity
size_t CodeHeap::capacity() const {
  return _memory.committed_size();
}

size_t CodeHeap::max_capacity() const {
  return _memory.reserved_size();
}

int CodeHeap::allocated_segments() const {
  return (int)_next_segment;
}

size_t CodeHeap::allocated_capacity() const {
  // size of used heap - size on freelist
  return segments_to_size(_next_segment - _freelist_segments);
}

// Returns size of the unallocated heap block
size_t CodeHeap::heap_unallocated_capacity() const {
  // Total number of segments - number currently used
  return segments_to_size(_number_of_reserved_segments - _next_segment);
}

// Free list management

FreeBlock* CodeHeap::following_block(FreeBlock *b) {
  return (FreeBlock*)(((address)b) + _segment_size * b->length());
}

// Inserts block b after a
void CodeHeap::insert_after(FreeBlock* a, FreeBlock* b) {
  assert(a != NULL && b != NULL, "must be real pointers");

  // Link b into the list after a
  b->set_link(a->link());
  a->set_link(b);

  // See if we can merge blocks
  merge_right(b); // Try to make b bigger
  merge_right(a); // Try to make a include b
}

// Try to merge this block with the following block
bool CodeHeap::merge_right(FreeBlock* a) {
  assert(a->free(), "must be a free block");
  if (following_block(a) == a->link()) {
    assert(a->link() != NULL && a->link()->free(), "must be free too");

    // Remember linked (following) block. invalidate should only zap header of this block.
    size_t follower = segment_for(a->link());
    // Merge block a to include the following block.
    a->set_length(a->length() + a->link()->length());
    a->set_link(a->link()->link());

    // Update the segment map and invalidate block contents.
    mark_segmap_as_used(follower, segment_for(a) + a->length(), true);
    // Block contents has already been invalidated by add_to_freelist.
    // What's left is the header of the following block which now is
    // in the middle of the merged block. Just zap one segment.
    invalidate(follower, follower + 1, 0);

    _freelist_length--;
    return true;
  }
  return false;
}


void CodeHeap::add_to_freelist(HeapBlock* a) {
  FreeBlock* b = (FreeBlock*)a;
  size_t  bseg = segment_for(b);
  _freelist_length++;

  _blob_count--;
  assert(_blob_count >= 0, "sanity");

  assert(b != _freelist, "cannot be removed twice");

  // Mark as free and update free space count
  _freelist_segments += b->length();
  b->set_free();
  invalidate(bseg, bseg + b->length(), sizeof(FreeBlock));

  // First element in list?
  if (_freelist == NULL) {
    b->set_link(NULL);
    _freelist = b;
    return;
  }

  // Since the freelist is ordered (smaller addresses -> larger addresses) and the
  // element we want to insert into the freelist has a smaller address than the first
  // element, we can simply add 'b' as the first element and we are done.
  if (b < _freelist) {
    // Insert first in list
    b->set_link(_freelist);
    _freelist = b;
    merge_right(_freelist);
    return;
  }

  // Scan for right place to put into list.
  // List is sorted by increasing addresses.
  FreeBlock* prev = _freelist;
  FreeBlock* cur  = _freelist->link();
  if ((_freelist_length > freelist_limit) && (_last_insert_point != NULL)) {
    _last_insert_point = (FreeBlock*)find_block_for(_last_insert_point);
    if ((_last_insert_point != NULL) && _last_insert_point->free() && (_last_insert_point < b)) {
      prev = _last_insert_point;
      cur  = prev->link();
    }
  }
  while(cur != NULL && cur < b) {
    assert(prev < cur, "Freelist must be ordered");
    prev = cur;
    cur  = cur->link();
  }
  assert((prev < b) && (cur == NULL || b < cur), "free-list must be ordered");
  insert_after(prev, b);
  _last_insert_point = prev;
}

/**
 * Search freelist for an entry on the list with the best fit.
 * @return NULL, if no one was found
 */
HeapBlock* CodeHeap::search_freelist(size_t length) {
  FreeBlock* found_block  = NULL;
  FreeBlock* found_prev   = NULL;
  size_t     found_length = _next_segment; // max it out to begin with

  HeapBlock* res  = NULL;
  FreeBlock* prev = NULL;
  FreeBlock* cur  = _freelist;

  length = length < CodeCacheMinBlockLength ? CodeCacheMinBlockLength : length;

  // Search for best-fitting block
  while(cur != NULL) {
    size_t cur_length = cur->length();
    if (cur_length == length) {
      // We have a perfect fit
      found_block  = cur;
      found_prev   = prev;
      found_length = cur_length;
      break;
    } else if ((cur_length > length) && (cur_length < found_length)) {
      // This is a new, closer fit. Remember block, its previous element, and its length
      found_block  = cur;
      found_prev   = prev;
      found_length = cur_length;
    }
    // Next element in list
    prev = cur;
    cur  = cur->link();
  }

  if (found_block == NULL) {
    // None found
    return NULL;
  }

  // Exact (or at least good enough) fit. Remove from list.
  // Don't leave anything on the freelist smaller than CodeCacheMinBlockLength.
  if (found_length - length < CodeCacheMinBlockLength) {
    _freelist_length--;
    length = found_length;
    if (found_prev == NULL) {
      assert(_freelist == found_block, "sanity check");
      _freelist = _freelist->link();
    } else {
      assert((found_prev->link() == found_block), "sanity check");
      // Unmap element
      found_prev->set_link(found_block->link());
    }
    res = (HeapBlock*)found_block;
    // sizeof(HeapBlock) < sizeof(FreeBlock).
    // Invalidate the additional space that FreeBlock occupies.
    // The rest of the block should already be invalidated.
    // This is necessary due to a dubious assert in nmethod.cpp(PcDescCache::reset_to()).
    // Can't use invalidate() here because it works on segment_size units (too coarse).
    DEBUG_ONLY(memset((void*)res->allocated_space(), badCodeHeapNewVal, sizeof(FreeBlock) - sizeof(HeapBlock)));
  } else {
    // Truncate the free block and return the truncated part
    // as new HeapBlock. The remaining free block does not
    // need to be updated, except for it's length. Truncating
    // the segment map does not invalidate the leading part.
    res = split_block(found_block, found_length - length);
  }

  res->set_used();
  _freelist_segments -= length;
  return res;
}

int CodeHeap::defrag_segmap(bool do_defrag) {
  int extra_hops_used = 0;
  int extra_hops_free = 0;
  int blocks_used     = 0;
  int blocks_free     = 0;
  for(HeapBlock* h = first_block(); h != NULL; h = next_block(h)) {
    size_t beg = segment_for(h);
    size_t end = segment_for(h) + h->length();
    int extra_hops = segmap_hops(beg, end);
    if (h->free()) {
      extra_hops_free += extra_hops;
      blocks_free++;
    } else {
      extra_hops_used += extra_hops;
      blocks_used++;
    }
    if (do_defrag && (extra_hops > 0)) {
      mark_segmap_as_used(beg, end, false);
    }
  }
  return extra_hops_used + extra_hops_free;
}

// Count the hops required to get from the last segment of a
// heap block to the block header segment. For the optimal case,
//   #hops = ((#segments-1)+(free_sentinel-2))/(free_sentinel-1)
// The range of segments to be checked is given by [beg..end).
// Return the number of extra hops required. There may be extra hops
// due to the is_FreeBlock_join optimization in mark_segmap_as_used().
int CodeHeap::segmap_hops(size_t beg, size_t end) {
  if (beg < end) {
    // setup _segmap pointers for faster indexing
    address p = (address)_segmap.low() + beg;
    int hops_expected
      = checked_cast<int>(((end-beg-1)+(free_sentinel-2))/(free_sentinel-1));
    int nhops = 0;
    size_t ix = end-beg-1;
    while (p[ix] > 0) {
      ix -= p[ix];
      nhops++;
    }
    return (nhops > hops_expected) ? nhops - hops_expected : 0;
  }
  return 0;
}

//----------------------------------------------------------------------------
// Non-product code

#ifndef PRODUCT

void CodeHeap::print() {
  tty->print_cr("The Heap");
}

void CodeHeap::verify() {
  if (VerifyCodeCache) {
    assert_locked_or_safepoint(CodeCache_lock);
    size_t len = 0;
    int count = 0;
    for(FreeBlock* b = _freelist; b != NULL; b = b->link()) {
      len += b->length();
      count++;
      // Check if we have merged all free blocks
      assert(merge_right(b) == false, "Missed merging opportunity");
    }
    // Verify that freelist contains the right amount of free space
    assert(len == _freelist_segments, "wrong freelist");

    for(HeapBlock* h = first_block(); h != NULL; h = next_block(h)) {
      if (h->free()) count--;
    }
    // Verify that the freelist contains the same number of blocks
    // than free blocks found on the full list.
    assert(count == 0, "missing free blocks");

    //---<  all free block memory must have been invalidated  >---
    for(FreeBlock* b = _freelist; b != NULL; b = b->link()) {
      for (char* c = (char*)b + sizeof(FreeBlock); c < (char*)b + segments_to_size(b->length()); c++) {
        assert(*c == (char)badCodeHeapNewVal, "FreeBlock@" PTR_FORMAT "(" PTR_FORMAT ") not invalidated @byte %d", p2i(b), b->length(), (int)(c - (char*)b));
      }
    }

    address seg_map = (address)_segmap.low();
    size_t  nseg       = 0;
    int     extra_hops = 0;
    count = 0;
    for(HeapBlock* b = first_block(); b != NULL; b = next_block(b)) {
      size_t seg1 = segment_for(b);
      size_t segn = seg1 + b->length();
      extra_hops += segmap_hops(seg1, segn);
      count++;
      for (size_t i = seg1; i < segn; i++) {
        nseg++;
        //---<  Verify segment map marking  >---
        // All allocated segments, no matter if in a free or used block,
        // must be marked "in use".
        assert(!is_segment_unused(seg_map[i]), "CodeHeap: unused segment. seg_map[%d]([%d..%d]) = %d, %s block",    (int)i, (int)seg1, (int)segn, seg_map[i], b->free()? "free":"used");
        assert((unsigned char)seg_map[i] < free_sentinel, "CodeHeap: seg_map[%d]([%d..%d]) = %d (out of range)",    (int)i, (int)seg1, (int)segn, seg_map[i]);
      }
    }
    assert(nseg == _next_segment, "CodeHeap: segment count mismatch. found %d, expected %d.", (int)nseg, (int)_next_segment);
    assert(extra_hops <= _fragmentation_count, "CodeHeap: extra hops wrong. fragmentation: %d, extra hops: %d.", _fragmentation_count, extra_hops);
    if (extra_hops >= (16 + 2 * count)) {
      warning("CodeHeap: many extra hops due to optimization. blocks: %d, extra hops: %d.", count, extra_hops);
    }

    // Verify that the number of free blocks is not out of hand.
    static int free_block_threshold = 10000;
    if (count > free_block_threshold) {
      warning("CodeHeap: # of free blocks > %d", free_block_threshold);
      // Double the warning limit
      free_block_threshold *= 2;
    }
  }
}

#endif
