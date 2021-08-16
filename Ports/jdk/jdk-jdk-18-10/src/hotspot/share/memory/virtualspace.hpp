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

#ifndef SHARE_MEMORY_VIRTUALSPACE_HPP
#define SHARE_MEMORY_VIRTUALSPACE_HPP

#include "memory/memRegion.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

// ReservedSpace is a data structure for reserving a contiguous address range.

class ReservedSpace {
  friend class VMStructs;
 protected:
  char*  _base;
  size_t _size;
  size_t _noaccess_prefix;
  size_t _alignment;
  size_t _page_size;
  bool   _special;
  int    _fd_for_heap;
 private:
  bool   _executable;

  // ReservedSpace
  ReservedSpace(char* base, size_t size, size_t alignment,
                size_t page_size, bool special, bool executable);
 protected:
  // Helpers to clear and set members during initialization. Two members
  // require special treatment:
  //  * _fd_for_heap     - The fd is set once and should not be cleared
  //                       even if the reservation has to be retried.
  //  * _noaccess_prefix - Used for compressed heaps and updated after
  //                       the reservation is initialized. Always set to
  //                       0 during initialization.
  void clear_members();
  void initialize_members(char* base, size_t size, size_t alignment,
                          size_t page_size, bool special, bool executable);

  void initialize(size_t size, size_t alignment, size_t page_size,
                  char* requested_address, bool executable);

  void reserve(size_t size, size_t alignment, size_t page_size,
               char* requested_address, bool executable);
 public:
  // Constructor
  ReservedSpace();
  // Initialize the reserved space with the given size. Depending on the size
  // a suitable page size and alignment will be used.
  explicit ReservedSpace(size_t size);
  // Initialize the reserved space with the given size. The preferred_page_size
  // is used as the minimum page size/alignment. This may waste some space if
  // the given size is not aligned to that value, as the reservation will be
  // aligned up to the final alignment in this case.
  ReservedSpace(size_t size, size_t preferred_page_size);
  ReservedSpace(size_t size, size_t alignment, size_t page_size,
                char* requested_address = NULL);

  // Accessors
  char*  base()            const { return _base;      }
  size_t size()            const { return _size;      }
  char*  end()             const { return _base + _size; }
  size_t alignment()       const { return _alignment; }
  size_t page_size()       const { return _page_size; }
  bool   special()         const { return _special;   }
  bool   executable()      const { return _executable;   }
  size_t noaccess_prefix() const { return _noaccess_prefix;   }
  bool is_reserved()       const { return _base != NULL; }
  void release();

  // Splitting
  // This splits the space into two spaces, the first part of which will be returned.
  ReservedSpace first_part(size_t partition_size, size_t alignment);
  ReservedSpace last_part (size_t partition_size, size_t alignment);

  // These simply call the above using the default alignment.
  inline ReservedSpace first_part(size_t partition_size);
  inline ReservedSpace last_part (size_t partition_size);

  // Alignment
  static size_t page_align_size_up(size_t size);
  static size_t page_align_size_down(size_t size);
  static size_t allocation_align_size_up(size_t size);
  bool contains(const void* p) const {
    return (base() <= ((char*)p)) && (((char*)p) < (base() + size()));
  }
};

ReservedSpace
ReservedSpace::first_part(size_t partition_size)
{
  return first_part(partition_size, alignment());
}

ReservedSpace ReservedSpace::last_part(size_t partition_size)
{
  return last_part(partition_size, alignment());
}

// Class encapsulating behavior specific of memory space reserved for Java heap.
class ReservedHeapSpace : public ReservedSpace {
 private:
  void try_reserve_heap(size_t size, size_t alignment, size_t page_size,
                        char *requested_address);
  void try_reserve_range(char *highest_start, char *lowest_start,
                         size_t attach_point_alignment, char *aligned_HBMA,
                         char *upper_bound, size_t size, size_t alignment, size_t page_size);
  void initialize_compressed_heap(const size_t size, size_t alignment, size_t page_size);
  // Create protection page at the beginning of the space.
  void establish_noaccess_prefix();
 public:
  // Constructor. Tries to find a heap that is good for compressed oops.
  // heap_allocation_directory is the path to the backing memory for Java heap. When set, Java heap will be allocated
  // on the device which is managed by the file system where the directory resides.
  ReservedHeapSpace(size_t size, size_t forced_base_alignment, size_t page_size, const char* heap_allocation_directory = NULL);
  // Returns the base to be used for compression, i.e. so that null can be
  // encoded safely and implicit null checks can work.
  char *compressed_oop_base() const { return _base - _noaccess_prefix; }
  MemRegion region() const;
};

// Class encapsulating behavior specific memory space for Code
class ReservedCodeSpace : public ReservedSpace {
 public:
  // Constructor
  ReservedCodeSpace(size_t r_size, size_t rs_align, size_t page_size);
};

// VirtualSpace is data structure for committing a previously reserved address range in smaller chunks.

class VirtualSpace {
  friend class VMStructs;
 private:
  // Reserved area
  char* _low_boundary;
  char* _high_boundary;

  // Committed area
  char* _low;
  char* _high;

  // The entire space has been committed and pinned in memory, no
  // os::commit_memory() or os::uncommit_memory().
  bool _special;

  // Need to know if commit should be executable.
  bool   _executable;

  // MPSS Support
  // Each virtualspace region has a lower, middle, and upper region.
  // Each region has an end boundary and a high pointer which is the
  // high water mark for the last allocated byte.
  // The lower and upper unaligned to LargePageSizeInBytes uses default page.
  // size.  The middle region uses large page size.
  char* _lower_high;
  char* _middle_high;
  char* _upper_high;

  char* _lower_high_boundary;
  char* _middle_high_boundary;
  char* _upper_high_boundary;

  size_t _lower_alignment;
  size_t _middle_alignment;
  size_t _upper_alignment;

  // MPSS Accessors
  char* lower_high() const { return _lower_high; }
  char* middle_high() const { return _middle_high; }
  char* upper_high() const { return _upper_high; }

  char* lower_high_boundary() const { return _lower_high_boundary; }
  char* middle_high_boundary() const { return _middle_high_boundary; }
  char* upper_high_boundary() const { return _upper_high_boundary; }

  size_t lower_alignment() const { return _lower_alignment; }
  size_t middle_alignment() const { return _middle_alignment; }
  size_t upper_alignment() const { return _upper_alignment; }

 public:
  // Committed area
  char* low()  const { return _low; }
  char* high() const { return _high; }

  // Reserved area
  char* low_boundary()  const { return _low_boundary; }
  char* high_boundary() const { return _high_boundary; }

  bool special() const { return _special; }

 public:
  // Initialization
  VirtualSpace();
  bool initialize_with_granularity(ReservedSpace rs, size_t committed_byte_size, size_t max_commit_ganularity);
  bool initialize(ReservedSpace rs, size_t committed_byte_size);

  // Destruction
  ~VirtualSpace();

  // Reserved memory
  size_t reserved_size() const;
  // Actually committed OS memory
  size_t actual_committed_size() const;
  // Memory used/expanded in this virtual space
  size_t committed_size() const;
  // Memory left to use/expand in this virtual space
  size_t uncommitted_size() const;

  bool   contains(const void* p) const;

  // Operations
  // returns true on success, false otherwise
  bool expand_by(size_t bytes, bool pre_touch = false);
  void shrink_by(size_t bytes);
  void release();

  void check_for_contiguity() PRODUCT_RETURN;

  // Debugging
  void print_on(outputStream* out) const PRODUCT_RETURN;
  void print() const;
};

#endif // SHARE_MEMORY_VIRTUALSPACE_HPP
