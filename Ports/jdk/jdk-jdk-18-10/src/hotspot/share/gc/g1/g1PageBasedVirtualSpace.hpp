/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1PAGEBASEDVIRTUALSPACE_HPP
#define SHARE_GC_G1_G1PAGEBASEDVIRTUALSPACE_HPP

#include "memory/memRegion.hpp"
#include "memory/virtualspace.hpp"
#include "utilities/align.hpp"
#include "utilities/bitMap.hpp"

class WorkGang;

// Virtual space management helper for a virtual space with an OS page allocation
// granularity.
// (De-)Allocation requests are always OS page aligned by passing a page index
// and multiples of pages.
// For systems that only commits of memory in a given size (always greater than
// page size) the base address is required to be aligned to that page size.
// The actual size requested need not be aligned to that page size, but the size
// of the reservation passed may be rounded up to this page size. Any fragment
// (less than the page size) of the actual size at the tail of the request will
// be committed using OS small pages.
// The implementation gives an error when trying to commit or uncommit pages that
// have already been committed or uncommitted.
class G1PageBasedVirtualSpace {
  friend class VMStructs;
 private:
  // Reserved area addresses.
  char* _low_boundary;
  char* _high_boundary;

  // The size of the tail in bytes of the handled space that needs to be committed
  // using small pages.
  size_t _tail_size;

  // The preferred page size used for commit/uncommit in bytes.
  size_t _page_size;

  // Bitmap used for verification of commit/uncommit operations.
  CHeapBitMap _committed;

  // Bitmap used to keep track of which pages are dirty or not for _special
  // spaces. This is needed because for those spaces the underlying memory
  // will only be zero filled the first time it is committed. Calls to commit
  // will use this bitmap and return whether or not the memory is zero filled.
  CHeapBitMap _dirty;

  // Indicates that the entire space has been committed and pinned in memory,
  // os::commit_memory() or os::uncommit_memory() have no function.
  bool _special;

  // Indicates whether the committed space should be executable.
  bool _executable;

  // Helper function for committing memory. Commit the given memory range by using
  // _page_size pages as much as possible and the remainder with small sized pages.
  void commit_internal(size_t start_page, size_t end_page);
  // Commit num_pages pages of _page_size size starting from start. All argument
  // checking has been performed.
  void commit_preferred_pages(size_t start_page, size_t end_page);
  // Commit space at the high end of the space that needs to be committed with small
  // sized pages.
  void commit_tail();

  // Uncommit the given memory range.
  void uncommit_internal(size_t start_page, size_t end_page);

  // Pretouch the given memory range.
  void pretouch_internal(size_t start_page, size_t end_page);

  // Returns the index of the page which contains the given address.
  size_t  addr_to_page_index(char* addr) const;

  // Is the given page index the last page?
  bool is_last_page(size_t index) const { return index == (_committed.size() - 1); }
  // Is the given page index the first after last page?
  bool is_after_last_page(size_t index) const;
  // Is the last page only partially covered by this space?
  bool is_last_page_partial() const { return !is_aligned(_high_boundary, _page_size); }
  // Returns the end address of the given page bounded by the reserved space.
  char* bounded_end_addr(size_t end_page) const;

  // Returns true if the entire area is backed by committed memory.
  bool is_area_committed(size_t start_page, size_t size_in_pages) const;
  // Returns true if the entire area is not backed by committed memory.
  bool is_area_uncommitted(size_t start_page, size_t size_in_pages) const;

  void initialize_with_page_size(ReservedSpace rs, size_t used_size, size_t page_size);
 public:

  // Commit the given area of pages starting at start being size_in_pages large.
  // Returns true if the given area is zero filled upon completion.
  bool commit(size_t start_page, size_t size_in_pages);

  // Uncommit the given area of pages starting at start being size_in_pages large.
  void uncommit(size_t start_page, size_t size_in_pages);

  void pretouch(size_t start_page, size_t size_in_pages, WorkGang* pretouch_gang = NULL);

  // Initialize the given reserved space with the given base address and the size
  // actually used.
  // Prefer to commit in page_size chunks.
  G1PageBasedVirtualSpace(ReservedSpace rs, size_t used_size, size_t page_size);

  // Destruction
  ~G1PageBasedVirtualSpace();

  // Amount of reserved memory.
  size_t reserved_size() const;
  // Memory used in this virtual space.
  size_t committed_size() const;
  // Memory left to use/expand in this virtual space.
  size_t uncommitted_size() const;

  bool contains(const void* p) const;

  MemRegion reserved() {
    MemRegion x((HeapWord*)_low_boundary, reserved_size() / HeapWordSize);
    return x;
  }

  void check_for_contiguity() PRODUCT_RETURN;

  // Returns the address of the given page index.
  char*  page_start(size_t index) const;
  size_t page_size() const;

  // Debugging
  void print_on(outputStream* out) PRODUCT_RETURN;
  void print();
};

#endif // SHARE_GC_G1_G1PAGEBASEDVIRTUALSPACE_HPP
