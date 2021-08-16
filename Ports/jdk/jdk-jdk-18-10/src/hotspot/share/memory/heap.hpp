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

#ifndef SHARE_MEMORY_HEAP_HPP
#define SHARE_MEMORY_HEAP_HPP

#include "code/codeBlob.hpp"
#include "memory/allocation.hpp"
#include "memory/virtualspace.hpp"
#include "utilities/macros.hpp"

// Blocks

class HeapBlock {
  friend class VMStructs;

 public:
  struct Header {
    size_t  _length;                             // the length in segments
    bool    _used;                               // Used bit
  };

 protected:
  union {
    Header _header;
    int64_t _padding[ (sizeof(Header) + sizeof(int64_t)-1) / sizeof(int64_t) ];
                        // pad to 0 mod 8
  };

 public:
  // Initialization
  void initialize(size_t length)                 { _header._length = length; set_used(); }
  // Merging/splitting
  void set_length(size_t length)                 { _header._length = length; }

  // Accessors
  void* allocated_space() const                  { return (void*)(this + 1); }
  size_t length() const                          { return _header._length; }

  // Used/free
  void set_used()                                { _header._used = true; }
  void set_free()                                { _header._used = false; }
  bool free()                                    { return !_header._used; }
};

class FreeBlock: public HeapBlock {
  friend class VMStructs;
 protected:
  FreeBlock* _link;

 public:
  // Initialization
  void initialize(size_t length)             { HeapBlock::initialize(length); _link= NULL; }

  // Accessors
  FreeBlock* link() const                    { return _link; }
  void set_link(FreeBlock* link)             { _link = link; }
};

class CodeHeap : public CHeapObj<mtCode> {
  friend class VMStructs;
 protected:
  VirtualSpace _memory;                          // the memory holding the blocks
  VirtualSpace _segmap;                          // the memory holding the segment map

  size_t       _number_of_committed_segments;
  size_t       _number_of_reserved_segments;
  size_t       _segment_size;
  int          _log2_segment_size;

  size_t       _next_segment;

  FreeBlock*   _freelist;
  FreeBlock*   _last_insert_point;               // last insert point in add_to_freelist
  size_t       _freelist_segments;               // No. of segments in freelist
  int          _freelist_length;
  size_t       _max_allocated_capacity;          // Peak capacity that was allocated during lifetime of the heap

  const char*  _name;                            // Name of the CodeHeap
  const int    _code_blob_type;                  // CodeBlobType it contains
  int          _blob_count;                      // Number of CodeBlobs
  int          _nmethod_count;                   // Number of nmethods
  int          _adapter_count;                   // Number of adapters
  int          _full_count;                      // Number of times the code heap was full
  int          _fragmentation_count;             // #FreeBlock joins without fully initializing segment map elements.

  enum { free_sentinel = 0xFF };
  static const int fragmentation_limit = 10000;  // defragment after that many potential fragmentations.
  static const int freelist_limit = 100;         // improve insert point search if list is longer than this limit.
  static char  segmap_template[free_sentinel+1];

  // Helper functions
  size_t   size_to_segments(size_t size) const { return (size + _segment_size - 1) >> _log2_segment_size; }
  size_t   segments_to_size(size_t number_of_segments) const { return number_of_segments << _log2_segment_size; }

  size_t   segment_for(void* p) const            { return ((char*)p - _memory.low()) >> _log2_segment_size; }
  bool     is_segment_unused(int val) const      { return val == free_sentinel; }
  void*    address_for(size_t i) const           { return (void*)(_memory.low() + segments_to_size(i)); }
  void*    find_block_for(void* p) const;
  HeapBlock* block_at(size_t i) const            { return (HeapBlock*)address_for(i); }

  // These methods take segment map indices as range boundaries
  void mark_segmap_as_free(size_t beg, size_t end);
  void mark_segmap_as_used(size_t beg, size_t end, bool is_FreeBlock_join);
  void invalidate(size_t beg, size_t end, size_t header_bytes);
  void clear(size_t beg, size_t end);
  void clear();                                 // clears all heap contents
  static void init_segmap_template();

  // Freelist management helpers
  FreeBlock* following_block(FreeBlock* b);
  void insert_after(FreeBlock* a, FreeBlock* b);
  bool merge_right (FreeBlock* a);

  // Toplevel freelist management
  void add_to_freelist(HeapBlock* b);
  HeapBlock* search_freelist(size_t length);

  // Iteration helpers
  void*      next_used(HeapBlock* b) const;
  HeapBlock* block_start(void* p) const;

  // to perform additional actions on creation of executable code
  void on_code_mapping(char* base, size_t size);

 public:
  CodeHeap(const char* name, const int code_blob_type);

  // Heap extents
  bool  reserve(ReservedSpace rs, size_t committed_size, size_t segment_size);
  bool  expand_by(size_t size);                  // expands committed memory by size

  // Memory allocation
  void* allocate (size_t size); // Allocate 'size' bytes in the code cache or return NULL
  void  deallocate(void* p);    // Deallocate memory
  // Free the tail of segments allocated by the last call to 'allocate()' which exceed 'used_size'.
  // ATTENTION: this is only safe to use if there was no other call to 'allocate()' after
  //            'p' was allocated. Only intended for freeing memory which would be otherwise
  //            wasted after the interpreter generation because we don't know the interpreter size
  //            beforehand and we also can't easily relocate the interpreter to a new location.
  void  deallocate_tail(void* p, size_t used_size);

  // Boundaries of committed space.
  char* low()  const                             { return _memory.low(); }
  char* high() const                             { return _memory.high(); }
  // Boundaries of reserved space.
  char* low_boundary() const                     { return _memory.low_boundary(); }
  char* high_boundary() const                    { return _memory.high_boundary(); }

  // Containment means "contained in committed space".
  bool contains(const void* p) const             { return low() <= p && p < high(); }
  bool contains_blob(const CodeBlob* blob) const {
    return contains((void*)blob);
  }

  virtual void* find_start(void* p)     const;   // returns the block containing p or NULL
  virtual CodeBlob* find_blob_unsafe(void* start) const;
  size_t alignment_unit()       const;           // alignment of any block
  size_t alignment_offset()     const;           // offset of first byte of any block, within the enclosing alignment unit
  static size_t header_size()         { return sizeof(HeapBlock); } // returns the header size for each heap block

  size_t segment_size()         const { return _segment_size; }  // for CodeHeapState
  HeapBlock* first_block() const;                                // for CodeHeapState
  HeapBlock* next_block(HeapBlock* b) const;                     // for CodeHeapState
  HeapBlock* split_block(HeapBlock* b, size_t split_seg);        // split one block into two

  FreeBlock* freelist()         const { return _freelist; }      // for CodeHeapState

  size_t allocated_in_freelist() const           { return _freelist_segments * CodeCacheSegmentSize; }
  int    freelist_length()       const           { return _freelist_length; } // number of elements in the freelist

  // returns the first block or NULL
  virtual void* first() const                    { return next_used(first_block()); }
  // returns the next block given a block p or NULL
  virtual void* next(void* p) const              { return next_used(next_block(block_start(p))); }

  // Statistics
  size_t capacity() const;
  size_t max_capacity() const;
  int    allocated_segments() const;
  size_t allocated_capacity() const;
  size_t max_allocated_capacity() const          { return _max_allocated_capacity; }
  size_t unallocated_capacity() const            { return max_capacity() - allocated_capacity(); }

  // Returns true if the CodeHeap contains CodeBlobs of the given type
  bool accepts(int code_blob_type) const         { return (_code_blob_type == CodeBlobType::All) ||
                                                          (_code_blob_type == code_blob_type); }
  int code_blob_type() const                     { return _code_blob_type; }

  // Debugging / Profiling
  const char* name() const                       { return _name; }
  int         blob_count()                       { return _blob_count; }
  int         nmethod_count()                    { return _nmethod_count; }
  void    set_nmethod_count(int count)           {        _nmethod_count = count; }
  int         adapter_count()                    { return _adapter_count; }
  void    set_adapter_count(int count)           {        _adapter_count = count; }
  int         full_count()                       { return _full_count; }
  void        report_full()                      {        _full_count++; }

private:
  size_t heap_unallocated_capacity() const;
  int defrag_segmap(bool do_defrag);
  int segmap_hops(size_t beg, size_t end);

public:
  // Debugging
  void verify() PRODUCT_RETURN;
  void print()  PRODUCT_RETURN;
};

#endif // SHARE_MEMORY_HEAP_HPP
