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
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "memory/virtualspace.hpp"
#include "oops/compressedOops.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/powerOfTwo.hpp"

// ReservedSpace

// Dummy constructor
ReservedSpace::ReservedSpace() : _base(NULL), _size(0), _noaccess_prefix(0),
    _alignment(0), _special(false), _fd_for_heap(-1), _executable(false) {
}

ReservedSpace::ReservedSpace(size_t size) : _fd_for_heap(-1) {
  // Want to use large pages where possible. If the size is
  // not large page aligned the mapping will be a mix of
  // large and normal pages.
  size_t page_size = os::page_size_for_region_unaligned(size, 1);
  size_t alignment = os::vm_allocation_granularity();
  initialize(size, alignment, page_size, NULL, false);
}

ReservedSpace::ReservedSpace(size_t size, size_t preferred_page_size) : _fd_for_heap(-1) {
  // When a page size is given we don't want to mix large
  // and normal pages. If the size is not a multiple of the
  // page size it will be aligned up to achieve this.
  size_t alignment = os::vm_allocation_granularity();;
  if (preferred_page_size != (size_t)os::vm_page_size()) {
    alignment = MAX2(preferred_page_size, alignment);
    size = align_up(size, alignment);
  }
  initialize(size, alignment, preferred_page_size, NULL, false);
}

ReservedSpace::ReservedSpace(size_t size,
                             size_t alignment,
                             size_t page_size,
                             char* requested_address) : _fd_for_heap(-1) {
  initialize(size, alignment, page_size, requested_address, false);
}

ReservedSpace::ReservedSpace(char* base, size_t size, size_t alignment, size_t page_size,
                             bool special, bool executable) : _fd_for_heap(-1) {
  assert((size % os::vm_allocation_granularity()) == 0,
         "size not allocation aligned");
  initialize_members(base, size, alignment, page_size, special, executable);
}

// Helper method
static char* attempt_map_or_reserve_memory_at(char* base, size_t size, int fd, bool executable) {
  if (fd != -1) {
    return os::attempt_map_memory_to_file_at(base, size, fd);
  }
  return os::attempt_reserve_memory_at(base, size, executable);
}

// Helper method
static char* map_or_reserve_memory(size_t size, int fd, bool executable) {
  if (fd != -1) {
    return os::map_memory_to_file(size, fd);
  }
  return os::reserve_memory(size, executable);
}

// Helper method
static char* map_or_reserve_memory_aligned(size_t size, size_t alignment, int fd, bool executable) {
  if (fd != -1) {
    return os::map_memory_to_file_aligned(size, alignment, fd);
  }
  return os::reserve_memory_aligned(size, alignment, executable);
}

// Helper method
static void unmap_or_release_memory(char* base, size_t size, bool is_file_mapped) {
  if (is_file_mapped) {
    if (!os::unmap_memory(base, size)) {
      fatal("os::unmap_memory failed");
    }
  } else if (!os::release_memory(base, size)) {
    fatal("os::release_memory failed");
  }
}

// Helper method
static bool failed_to_reserve_as_requested(char* base, char* requested_address) {
  if (base == requested_address || requested_address == NULL) {
    return false; // did not fail
  }

  if (base != NULL) {
    // Different reserve address may be acceptable in other cases
    // but for compressed oops heap should be at requested address.
    assert(UseCompressedOops, "currently requested address used only for compressed oops");
    log_debug(gc, heap, coops)("Reserved memory not at requested address: " PTR_FORMAT " vs " PTR_FORMAT, p2i(base), p2i(requested_address));
  }
  return true;
}

static bool use_explicit_large_pages(size_t page_size) {
  return !os::can_commit_large_page_memory() &&
         page_size != (size_t) os::vm_page_size();
}

static bool large_pages_requested() {
  return UseLargePages &&
         (!FLAG_IS_DEFAULT(UseLargePages) || !FLAG_IS_DEFAULT(LargePageSizeInBytes));
}

static char* reserve_memory(char* requested_address, const size_t size,
                            const size_t alignment, int fd, bool exec) {
  char* base;
  // If the memory was requested at a particular address, use
  // os::attempt_reserve_memory_at() to avoid mapping over something
  // important.  If the reservation fails, return NULL.
  if (requested_address != 0) {
    assert(is_aligned(requested_address, alignment),
           "Requested address " PTR_FORMAT " must be aligned to " SIZE_FORMAT,
           p2i(requested_address), alignment);
    base = attempt_map_or_reserve_memory_at(requested_address, size, fd, exec);
  } else {
    // Optimistically assume that the OS returns an aligned base pointer.
    // When reserving a large address range, most OSes seem to align to at
    // least 64K.
    base = map_or_reserve_memory(size, fd, exec);
    // Check alignment constraints. This is only needed when there is
    // no requested address.
    if (!is_aligned(base, alignment)) {
      // Base not aligned, retry.
      unmap_or_release_memory(base, size, fd != -1 /*is_file_mapped*/);
      // Map using the requested alignment.
      base = map_or_reserve_memory_aligned(size, alignment, fd, exec);
    }
  }

  return base;
}

static char* reserve_memory_special(char* requested_address, const size_t size,
                                    const size_t alignment, const size_t page_size, bool exec) {

  log_trace(pagesize)("Attempt special mapping: size: " SIZE_FORMAT "%s, "
                      "alignment: " SIZE_FORMAT "%s",
                      byte_size_in_exact_unit(size), exact_unit_for_byte_size(size),
                      byte_size_in_exact_unit(alignment), exact_unit_for_byte_size(alignment));

  char* base = os::reserve_memory_special(size, alignment, page_size, requested_address, exec);
  if (base != NULL) {
    // Check alignment constraints.
    assert(is_aligned(base, alignment),
           "reserve_memory_special() returned an unaligned address, base: " PTR_FORMAT
           " alignment: " SIZE_FORMAT_HEX,
           p2i(base), alignment);
  } else {
    if (large_pages_requested()) {
      log_debug(gc, heap, coops)("Reserve regular memory without large pages");
    }
  }
  return base;
}

void ReservedSpace::clear_members() {
  initialize_members(NULL, 0, 0, 0, false, false);
}

void ReservedSpace::initialize_members(char* base, size_t size, size_t alignment,
                                       size_t page_size, bool special, bool executable) {
  _base = base;
  _size = size;
  _alignment = alignment;
  _page_size = page_size;
  _special = special;
  _executable = executable;
  _noaccess_prefix = 0;
}

void ReservedSpace::reserve(size_t size,
                            size_t alignment,
                            size_t page_size,
                            char* requested_address,
                            bool executable) {
  assert(is_aligned(size, alignment), "Size must be aligned to the requested alignment");

  // There are basically three different cases that we need to handle below:
  // - Mapping backed by a file
  // - Mapping backed by explicit large pages
  // - Mapping backed by normal pages or transparent huge pages
  // The first two have restrictions that requires the whole mapping to be
  // committed up front. To record this the ReservedSpace is marked 'special'.

  if (_fd_for_heap != -1) {
    // When there is a backing file directory for this space then whether
    // large pages are allocated is up to the filesystem of the backing file.
    // So UseLargePages is not taken into account for this reservation.
    char* base = reserve_memory(requested_address, size, alignment, _fd_for_heap, executable);
    if (base != NULL) {
      initialize_members(base, size, alignment, os::vm_page_size(), true, executable);
    }
    // Always return, not possible to fall back to reservation not using a file.
    return;
  } else if (use_explicit_large_pages(page_size)) {
    // System can't commit large pages i.e. use transparent huge pages and
    // the caller requested large pages. To satisfy this request we use
    // explicit large pages and these have to be committed up front to ensure
    // no reservations are lost.

    char* base = reserve_memory_special(requested_address, size, alignment, page_size, executable);
    if (base != NULL) {
      // Successful reservation using large pages.
      initialize_members(base, size, alignment, page_size, true, executable);
      return;
    }
    // Failed to reserve explicit large pages, fall back to normal reservation.
    page_size = os::vm_page_size();
  }

  // Not a 'special' reservation.
  char* base = reserve_memory(requested_address, size, alignment, -1, executable);
  if (base != NULL) {
    // Successful mapping.
    initialize_members(base, size, alignment, page_size, false, executable);
  }
}

void ReservedSpace::initialize(size_t size,
                               size_t alignment,
                               size_t page_size,
                               char* requested_address,
                               bool executable) {
  const size_t granularity = os::vm_allocation_granularity();
  assert((size & (granularity - 1)) == 0,
         "size not aligned to os::vm_allocation_granularity()");
  assert((alignment & (granularity - 1)) == 0,
         "alignment not aligned to os::vm_allocation_granularity()");
  assert(alignment == 0 || is_power_of_2((intptr_t)alignment),
         "not a power of 2");
  assert(page_size >= (size_t) os::vm_page_size(), "Invalid page size");
  assert(is_power_of_2(page_size), "Invalid page size");

  clear_members();

  if (size == 0) {
    return;
  }

  // Adjust alignment to not be 0.
  alignment = MAX2(alignment, (size_t)os::vm_page_size());

  // Reserve the memory.
  reserve(size, alignment, page_size, requested_address, executable);

  // Check that the requested address is used if given.
  if (failed_to_reserve_as_requested(_base, requested_address)) {
    // OS ignored the requested address, release the reservation.
    release();
    return;
  }
}

ReservedSpace ReservedSpace::first_part(size_t partition_size, size_t alignment) {
  assert(partition_size <= size(), "partition failed");
  ReservedSpace result(base(), partition_size, alignment, page_size(), special(), executable());
  return result;
}


ReservedSpace
ReservedSpace::last_part(size_t partition_size, size_t alignment) {
  assert(partition_size <= size(), "partition failed");
  ReservedSpace result(base() + partition_size, size() - partition_size,
                       alignment, page_size(), special(), executable());
  return result;
}


size_t ReservedSpace::page_align_size_up(size_t size) {
  return align_up(size, os::vm_page_size());
}


size_t ReservedSpace::page_align_size_down(size_t size) {
  return align_down(size, os::vm_page_size());
}


size_t ReservedSpace::allocation_align_size_up(size_t size) {
  return align_up(size, os::vm_allocation_granularity());
}

void ReservedSpace::release() {
  if (is_reserved()) {
    char *real_base = _base - _noaccess_prefix;
    const size_t real_size = _size + _noaccess_prefix;
    if (special()) {
      if (_fd_for_heap != -1) {
        os::unmap_memory(real_base, real_size);
      } else {
        os::release_memory_special(real_base, real_size);
      }
    } else{
      os::release_memory(real_base, real_size);
    }
    clear_members();
  }
}

static size_t noaccess_prefix_size(size_t alignment) {
  return lcm(os::vm_page_size(), alignment);
}

void ReservedHeapSpace::establish_noaccess_prefix() {
  assert(_alignment >= (size_t)os::vm_page_size(), "must be at least page size big");
  _noaccess_prefix = noaccess_prefix_size(_alignment);

  if (base() && base() + _size > (char *)OopEncodingHeapMax) {
    if (true
        WIN64_ONLY(&& !UseLargePages)
        AIX_ONLY(&& os::vm_page_size() != 64*K)) {
      // Protect memory at the base of the allocated region.
      // If special, the page was committed (only matters on windows)
      if (!os::protect_memory(_base, _noaccess_prefix, os::MEM_PROT_NONE, _special)) {
        fatal("cannot protect protection page");
      }
      log_debug(gc, heap, coops)("Protected page at the reserved heap base: "
                                 PTR_FORMAT " / " INTX_FORMAT " bytes",
                                 p2i(_base),
                                 _noaccess_prefix);
      assert(CompressedOops::use_implicit_null_checks() == true, "not initialized?");
    } else {
      CompressedOops::set_use_implicit_null_checks(false);
    }
  }

  _base += _noaccess_prefix;
  _size -= _noaccess_prefix;
  assert(((uintptr_t)_base % _alignment == 0), "must be exactly of required alignment");
}

// Tries to allocate memory of size 'size' at address requested_address with alignment 'alignment'.
// Does not check whether the reserved memory actually is at requested_address, as the memory returned
// might still fulfill the wishes of the caller.
// Assures the memory is aligned to 'alignment'.
// NOTE: If ReservedHeapSpace already points to some reserved memory this is freed, first.
void ReservedHeapSpace::try_reserve_heap(size_t size,
                                         size_t alignment,
                                         size_t page_size,
                                         char* requested_address) {
  if (_base != NULL) {
    // We tried before, but we didn't like the address delivered.
    release();
  }

  // Try to reserve the memory for the heap.
  log_trace(gc, heap, coops)("Trying to allocate at address " PTR_FORMAT
                             " heap of size " SIZE_FORMAT_HEX,
                             p2i(requested_address),
                             size);

  reserve(size, alignment, page_size, requested_address, false);

  // Check alignment constraints.
  if (is_reserved() && !is_aligned(_base, _alignment)) {
    // Base not aligned, retry.
    release();
  }
}

void ReservedHeapSpace::try_reserve_range(char *highest_start,
                                          char *lowest_start,
                                          size_t attach_point_alignment,
                                          char *aligned_heap_base_min_address,
                                          char *upper_bound,
                                          size_t size,
                                          size_t alignment,
                                          size_t page_size) {
  const size_t attach_range = highest_start - lowest_start;
  // Cap num_attempts at possible number.
  // At least one is possible even for 0 sized attach range.
  const uint64_t num_attempts_possible = (attach_range / attach_point_alignment) + 1;
  const uint64_t num_attempts_to_try   = MIN2((uint64_t)HeapSearchSteps, num_attempts_possible);

  const size_t stepsize = (attach_range == 0) ? // Only one try.
    (size_t) highest_start : align_up(attach_range / num_attempts_to_try, attach_point_alignment);

  // Try attach points from top to bottom.
  char* attach_point = highest_start;
  while (attach_point >= lowest_start  &&
         attach_point <= highest_start &&  // Avoid wrap around.
         ((_base == NULL) ||
          (_base < aligned_heap_base_min_address || _base + size > upper_bound))) {
    try_reserve_heap(size, alignment, page_size, attach_point);
    attach_point -= stepsize;
  }
}

#define SIZE_64K  ((uint64_t) UCONST64(      0x10000))
#define SIZE_256M ((uint64_t) UCONST64(   0x10000000))
#define SIZE_32G  ((uint64_t) UCONST64(  0x800000000))

// Helper for heap allocation. Returns an array with addresses
// (OS-specific) which are suited for disjoint base mode. Array is
// NULL terminated.
static char** get_attach_addresses_for_disjoint_mode() {
  static uint64_t addresses[] = {
     2 * SIZE_32G,
     3 * SIZE_32G,
     4 * SIZE_32G,
     8 * SIZE_32G,
    10 * SIZE_32G,
     1 * SIZE_64K * SIZE_32G,
     2 * SIZE_64K * SIZE_32G,
     3 * SIZE_64K * SIZE_32G,
     4 * SIZE_64K * SIZE_32G,
    16 * SIZE_64K * SIZE_32G,
    32 * SIZE_64K * SIZE_32G,
    34 * SIZE_64K * SIZE_32G,
    0
  };

  // Sort out addresses smaller than HeapBaseMinAddress. This assumes
  // the array is sorted.
  uint i = 0;
  while (addresses[i] != 0 &&
         (addresses[i] < OopEncodingHeapMax || addresses[i] < HeapBaseMinAddress)) {
    i++;
  }
  uint start = i;

  // Avoid more steps than requested.
  i = 0;
  while (addresses[start+i] != 0) {
    if (i == HeapSearchSteps) {
      addresses[start+i] = 0;
      break;
    }
    i++;
  }

  return (char**) &addresses[start];
}

void ReservedHeapSpace::initialize_compressed_heap(const size_t size, size_t alignment, size_t page_size) {
  guarantee(size + noaccess_prefix_size(alignment) <= OopEncodingHeapMax,
            "can not allocate compressed oop heap for this size");
  guarantee(alignment == MAX2(alignment, (size_t)os::vm_page_size()), "alignment too small");

  const size_t granularity = os::vm_allocation_granularity();
  assert((size & (granularity - 1)) == 0,
         "size not aligned to os::vm_allocation_granularity()");
  assert((alignment & (granularity - 1)) == 0,
         "alignment not aligned to os::vm_allocation_granularity()");
  assert(alignment == 0 || is_power_of_2((intptr_t)alignment),
         "not a power of 2");

  // The necessary attach point alignment for generated wish addresses.
  // This is needed to increase the chance of attaching for mmap and shmat.
  const size_t os_attach_point_alignment =
    AIX_ONLY(SIZE_256M)  // Known shm boundary alignment.
    NOT_AIX(os::vm_allocation_granularity());
  const size_t attach_point_alignment = lcm(alignment, os_attach_point_alignment);

  char *aligned_heap_base_min_address = (char *)align_up((void *)HeapBaseMinAddress, alignment);
  size_t noaccess_prefix = ((aligned_heap_base_min_address + size) > (char*)OopEncodingHeapMax) ?
    noaccess_prefix_size(alignment) : 0;

  // Attempt to alloc at user-given address.
  if (!FLAG_IS_DEFAULT(HeapBaseMinAddress)) {
    try_reserve_heap(size + noaccess_prefix, alignment, page_size, aligned_heap_base_min_address);
    if (_base != aligned_heap_base_min_address) { // Enforce this exact address.
      release();
    }
  }

  // Keep heap at HeapBaseMinAddress.
  if (_base == NULL) {

    // Try to allocate the heap at addresses that allow efficient oop compression.
    // Different schemes are tried, in order of decreasing optimization potential.
    //
    // For this, try_reserve_heap() is called with the desired heap base addresses.
    // A call into the os layer to allocate at a given address can return memory
    // at a different address than requested.  Still, this might be memory at a useful
    // address. try_reserve_heap() always returns this allocated memory, as only here
    // the criteria for a good heap are checked.

    // Attempt to allocate so that we can run without base and scale (32-Bit unscaled compressed oops).
    // Give it several tries from top of range to bottom.
    if (aligned_heap_base_min_address + size <= (char *)UnscaledOopHeapMax) {

      // Calc address range within we try to attach (range of possible start addresses).
      char* const highest_start = align_down((char *)UnscaledOopHeapMax - size, attach_point_alignment);
      char* const lowest_start  = align_up(aligned_heap_base_min_address, attach_point_alignment);
      try_reserve_range(highest_start, lowest_start, attach_point_alignment,
                        aligned_heap_base_min_address, (char *)UnscaledOopHeapMax, size, alignment, page_size);
    }

    // zerobased: Attempt to allocate in the lower 32G.
    // But leave room for the compressed class pointers, which is allocated above
    // the heap.
    char *zerobased_max = (char *)OopEncodingHeapMax;
    const size_t class_space = align_up(CompressedClassSpaceSize, alignment);
    // For small heaps, save some space for compressed class pointer
    // space so it can be decoded with no base.
    if (UseCompressedClassPointers && !UseSharedSpaces &&
        OopEncodingHeapMax <= KlassEncodingMetaspaceMax &&
        (uint64_t)(aligned_heap_base_min_address + size + class_space) <= KlassEncodingMetaspaceMax) {
      zerobased_max = (char *)OopEncodingHeapMax - class_space;
    }

    // Give it several tries from top of range to bottom.
    if (aligned_heap_base_min_address + size <= zerobased_max &&    // Zerobased theoretical possible.
        ((_base == NULL) ||                        // No previous try succeeded.
         (_base + size > zerobased_max))) {        // Unscaled delivered an arbitrary address.

      // Calc address range within we try to attach (range of possible start addresses).
      char *const highest_start = align_down(zerobased_max - size, attach_point_alignment);
      // Need to be careful about size being guaranteed to be less
      // than UnscaledOopHeapMax due to type constraints.
      char *lowest_start = aligned_heap_base_min_address;
      uint64_t unscaled_end = UnscaledOopHeapMax - size;
      if (unscaled_end < UnscaledOopHeapMax) { // unscaled_end wrapped if size is large
        lowest_start = MAX2(lowest_start, (char*)unscaled_end);
      }
      lowest_start = align_up(lowest_start, attach_point_alignment);
      try_reserve_range(highest_start, lowest_start, attach_point_alignment,
                        aligned_heap_base_min_address, zerobased_max, size, alignment, page_size);
    }

    // Now we go for heaps with base != 0.  We need a noaccess prefix to efficiently
    // implement null checks.
    noaccess_prefix = noaccess_prefix_size(alignment);

    // Try to attach at addresses that are aligned to OopEncodingHeapMax. Disjointbase mode.
    char** addresses = get_attach_addresses_for_disjoint_mode();
    int i = 0;
    while (addresses[i] &&                                 // End of array not yet reached.
           ((_base == NULL) ||                             // No previous try succeeded.
            (_base + size >  (char *)OopEncodingHeapMax && // Not zerobased or unscaled address.
             !CompressedOops::is_disjoint_heap_base_address((address)_base)))) {  // Not disjoint address.
      char* const attach_point = addresses[i];
      assert(attach_point >= aligned_heap_base_min_address, "Flag support broken");
      try_reserve_heap(size + noaccess_prefix, alignment, page_size, attach_point);
      i++;
    }

    // Last, desperate try without any placement.
    if (_base == NULL) {
      log_trace(gc, heap, coops)("Trying to allocate at address NULL heap of size " SIZE_FORMAT_HEX, size + noaccess_prefix);
      initialize(size + noaccess_prefix, alignment, page_size, NULL, false);
    }
  }
}

ReservedHeapSpace::ReservedHeapSpace(size_t size, size_t alignment, size_t page_size, const char* heap_allocation_directory) : ReservedSpace() {

  if (size == 0) {
    return;
  }

  if (heap_allocation_directory != NULL) {
    _fd_for_heap = os::create_file_for_heap(heap_allocation_directory);
    if (_fd_for_heap == -1) {
      vm_exit_during_initialization(
        err_msg("Could not create file for Heap at location %s", heap_allocation_directory));
    }
    // When there is a backing file directory for this space then whether
    // large pages are allocated is up to the filesystem of the backing file.
    // If requested, let the user know that explicit large pages can't be used.
    if (use_explicit_large_pages(page_size) && large_pages_requested()) {
      log_debug(gc, heap)("Cannot allocate explicit large pages for Java Heap when AllocateHeapAt option is set.");
    }
  }

  // Heap size should be aligned to alignment, too.
  guarantee(is_aligned(size, alignment), "set by caller");

  if (UseCompressedOops) {
    initialize_compressed_heap(size, alignment, page_size);
    if (_size > size) {
      // We allocated heap with noaccess prefix.
      // It can happen we get a zerobased/unscaled heap with noaccess prefix,
      // if we had to try at arbitrary address.
      establish_noaccess_prefix();
    }
  } else {
    initialize(size, alignment, page_size, NULL, false);
  }

  assert(markWord::encode_pointer_as_mark(_base).decode_pointer() == _base,
         "area must be distinguishable from marks for mark-sweep");
  assert(markWord::encode_pointer_as_mark(&_base[size]).decode_pointer() == &_base[size],
         "area must be distinguishable from marks for mark-sweep");

  if (base() != NULL) {
    MemTracker::record_virtual_memory_type((address)base(), mtJavaHeap);
  }

  if (_fd_for_heap != -1) {
    os::close(_fd_for_heap);
  }
}

MemRegion ReservedHeapSpace::region() const {
  return MemRegion((HeapWord*)base(), (HeapWord*)end());
}

// Reserve space for code segment.  Same as Java heap only we mark this as
// executable.
ReservedCodeSpace::ReservedCodeSpace(size_t r_size,
                                     size_t rs_align,
                                     size_t rs_page_size) : ReservedSpace() {
  initialize(r_size, rs_align, rs_page_size, /*requested address*/ NULL, /*executable*/ true);
  MemTracker::record_virtual_memory_type((address)base(), mtCode);
}

// VirtualSpace

VirtualSpace::VirtualSpace() {
  _low_boundary           = NULL;
  _high_boundary          = NULL;
  _low                    = NULL;
  _high                   = NULL;
  _lower_high             = NULL;
  _middle_high            = NULL;
  _upper_high             = NULL;
  _lower_high_boundary    = NULL;
  _middle_high_boundary   = NULL;
  _upper_high_boundary    = NULL;
  _lower_alignment        = 0;
  _middle_alignment       = 0;
  _upper_alignment        = 0;
  _special                = false;
  _executable             = false;
}


bool VirtualSpace::initialize(ReservedSpace rs, size_t committed_size) {
  const size_t max_commit_granularity = os::page_size_for_region_unaligned(rs.size(), 1);
  return initialize_with_granularity(rs, committed_size, max_commit_granularity);
}

bool VirtualSpace::initialize_with_granularity(ReservedSpace rs, size_t committed_size, size_t max_commit_granularity) {
  if(!rs.is_reserved()) return false;  // allocation failed.
  assert(_low_boundary == NULL, "VirtualSpace already initialized");
  assert(max_commit_granularity > 0, "Granularity must be non-zero.");

  _low_boundary  = rs.base();
  _high_boundary = low_boundary() + rs.size();

  _low = low_boundary();
  _high = low();

  _special = rs.special();
  _executable = rs.executable();

  // When a VirtualSpace begins life at a large size, make all future expansion
  // and shrinking occur aligned to a granularity of large pages.  This avoids
  // fragmentation of physical addresses that inhibits the use of large pages
  // by the OS virtual memory system.  Empirically,  we see that with a 4MB
  // page size, the only spaces that get handled this way are codecache and
  // the heap itself, both of which provide a substantial performance
  // boost in many benchmarks when covered by large pages.
  //
  // No attempt is made to force large page alignment at the very top and
  // bottom of the space if they are not aligned so already.
  _lower_alignment  = os::vm_page_size();
  _middle_alignment = max_commit_granularity;
  _upper_alignment  = os::vm_page_size();

  // End of each region
  _lower_high_boundary = align_up(low_boundary(), middle_alignment());
  _middle_high_boundary = align_down(high_boundary(), middle_alignment());
  _upper_high_boundary = high_boundary();

  // High address of each region
  _lower_high = low_boundary();
  _middle_high = lower_high_boundary();
  _upper_high = middle_high_boundary();

  // commit to initial size
  if (committed_size > 0) {
    if (!expand_by(committed_size)) {
      return false;
    }
  }
  return true;
}


VirtualSpace::~VirtualSpace() {
  release();
}


void VirtualSpace::release() {
  // This does not release memory it reserved.
  // Caller must release via rs.release();
  _low_boundary           = NULL;
  _high_boundary          = NULL;
  _low                    = NULL;
  _high                   = NULL;
  _lower_high             = NULL;
  _middle_high            = NULL;
  _upper_high             = NULL;
  _lower_high_boundary    = NULL;
  _middle_high_boundary   = NULL;
  _upper_high_boundary    = NULL;
  _lower_alignment        = 0;
  _middle_alignment       = 0;
  _upper_alignment        = 0;
  _special                = false;
  _executable             = false;
}


size_t VirtualSpace::committed_size() const {
  return pointer_delta(high(), low(), sizeof(char));
}


size_t VirtualSpace::reserved_size() const {
  return pointer_delta(high_boundary(), low_boundary(), sizeof(char));
}


size_t VirtualSpace::uncommitted_size()  const {
  return reserved_size() - committed_size();
}

size_t VirtualSpace::actual_committed_size() const {
  // Special VirtualSpaces commit all reserved space up front.
  if (special()) {
    return reserved_size();
  }

  size_t committed_low    = pointer_delta(_lower_high,  _low_boundary,         sizeof(char));
  size_t committed_middle = pointer_delta(_middle_high, _lower_high_boundary,  sizeof(char));
  size_t committed_high   = pointer_delta(_upper_high,  _middle_high_boundary, sizeof(char));

#ifdef ASSERT
  size_t lower  = pointer_delta(_lower_high_boundary,  _low_boundary,         sizeof(char));
  size_t middle = pointer_delta(_middle_high_boundary, _lower_high_boundary,  sizeof(char));
  size_t upper  = pointer_delta(_upper_high_boundary,  _middle_high_boundary, sizeof(char));

  if (committed_high > 0) {
    assert(committed_low == lower, "Must be");
    assert(committed_middle == middle, "Must be");
  }

  if (committed_middle > 0) {
    assert(committed_low == lower, "Must be");
  }
  if (committed_middle < middle) {
    assert(committed_high == 0, "Must be");
  }

  if (committed_low < lower) {
    assert(committed_high == 0, "Must be");
    assert(committed_middle == 0, "Must be");
  }
#endif

  return committed_low + committed_middle + committed_high;
}


bool VirtualSpace::contains(const void* p) const {
  return low() <= (const char*) p && (const char*) p < high();
}

static void pretouch_expanded_memory(void* start, void* end) {
  assert(is_aligned(start, os::vm_page_size()), "Unexpected alignment");
  assert(is_aligned(end,   os::vm_page_size()), "Unexpected alignment");

  os::pretouch_memory(start, end);
}

static bool commit_expanded(char* start, size_t size, size_t alignment, bool pre_touch, bool executable) {
  if (os::commit_memory(start, size, alignment, executable)) {
    if (pre_touch || AlwaysPreTouch) {
      pretouch_expanded_memory(start, start + size);
    }
    return true;
  }

  debug_only(warning(
      "INFO: os::commit_memory(" PTR_FORMAT ", " PTR_FORMAT
      " size=" SIZE_FORMAT ", executable=%d) failed",
      p2i(start), p2i(start + size), size, executable);)

  return false;
}

/*
   First we need to determine if a particular virtual space is using large
   pages.  This is done at the initialize function and only virtual spaces
   that are larger than LargePageSizeInBytes use large pages.  Once we
   have determined this, all expand_by and shrink_by calls must grow and
   shrink by large page size chunks.  If a particular request
   is within the current large page, the call to commit and uncommit memory
   can be ignored.  In the case that the low and high boundaries of this
   space is not large page aligned, the pages leading to the first large
   page address and the pages after the last large page address must be
   allocated with default pages.
*/
bool VirtualSpace::expand_by(size_t bytes, bool pre_touch) {
  if (uncommitted_size() < bytes) {
    return false;
  }

  if (special()) {
    // don't commit memory if the entire space is pinned in memory
    _high += bytes;
    return true;
  }

  char* previous_high = high();
  char* unaligned_new_high = high() + bytes;
  assert(unaligned_new_high <= high_boundary(), "cannot expand by more than upper boundary");

  // Calculate where the new high for each of the regions should be.  If
  // the low_boundary() and high_boundary() are LargePageSizeInBytes aligned
  // then the unaligned lower and upper new highs would be the
  // lower_high() and upper_high() respectively.
  char* unaligned_lower_new_high =  MIN2(unaligned_new_high, lower_high_boundary());
  char* unaligned_middle_new_high = MIN2(unaligned_new_high, middle_high_boundary());
  char* unaligned_upper_new_high =  MIN2(unaligned_new_high, upper_high_boundary());

  // Align the new highs based on the regions alignment.  lower and upper
  // alignment will always be default page size.  middle alignment will be
  // LargePageSizeInBytes if the actual size of the virtual space is in
  // fact larger than LargePageSizeInBytes.
  char* aligned_lower_new_high =  align_up(unaligned_lower_new_high, lower_alignment());
  char* aligned_middle_new_high = align_up(unaligned_middle_new_high, middle_alignment());
  char* aligned_upper_new_high =  align_up(unaligned_upper_new_high, upper_alignment());

  // Determine which regions need to grow in this expand_by call.
  // If you are growing in the lower region, high() must be in that
  // region so calculate the size based on high().  For the middle and
  // upper regions, determine the starting point of growth based on the
  // location of high().  By getting the MAX of the region's low address
  // (or the previous region's high address) and high(), we can tell if it
  // is an intra or inter region growth.
  size_t lower_needs = 0;
  if (aligned_lower_new_high > lower_high()) {
    lower_needs = pointer_delta(aligned_lower_new_high, lower_high(), sizeof(char));
  }
  size_t middle_needs = 0;
  if (aligned_middle_new_high > middle_high()) {
    middle_needs = pointer_delta(aligned_middle_new_high, middle_high(), sizeof(char));
  }
  size_t upper_needs = 0;
  if (aligned_upper_new_high > upper_high()) {
    upper_needs = pointer_delta(aligned_upper_new_high, upper_high(), sizeof(char));
  }

  // Check contiguity.
  assert(low_boundary() <= lower_high() && lower_high() <= lower_high_boundary(),
         "high address must be contained within the region");
  assert(lower_high_boundary() <= middle_high() && middle_high() <= middle_high_boundary(),
         "high address must be contained within the region");
  assert(middle_high_boundary() <= upper_high() && upper_high() <= upper_high_boundary(),
         "high address must be contained within the region");

  // Commit regions
  if (lower_needs > 0) {
    assert(lower_high() + lower_needs <= lower_high_boundary(), "must not expand beyond region");
    if (!commit_expanded(lower_high(), lower_needs, _lower_alignment, pre_touch, _executable)) {
      return false;
    }
    _lower_high += lower_needs;
  }

  if (middle_needs > 0) {
    assert(middle_high() + middle_needs <= middle_high_boundary(), "must not expand beyond region");
    if (!commit_expanded(middle_high(), middle_needs, _middle_alignment, pre_touch, _executable)) {
      return false;
    }
    _middle_high += middle_needs;
  }

  if (upper_needs > 0) {
    assert(upper_high() + upper_needs <= upper_high_boundary(), "must not expand beyond region");
    if (!commit_expanded(upper_high(), upper_needs, _upper_alignment, pre_touch, _executable)) {
      return false;
    }
    _upper_high += upper_needs;
  }

  _high += bytes;
  return true;
}

// A page is uncommitted if the contents of the entire page is deemed unusable.
// Continue to decrement the high() pointer until it reaches a page boundary
// in which case that particular page can now be uncommitted.
void VirtualSpace::shrink_by(size_t size) {
  if (committed_size() < size)
    fatal("Cannot shrink virtual space to negative size");

  if (special()) {
    // don't uncommit if the entire space is pinned in memory
    _high -= size;
    return;
  }

  char* unaligned_new_high = high() - size;
  assert(unaligned_new_high >= low_boundary(), "cannot shrink past lower boundary");

  // Calculate new unaligned address
  char* unaligned_upper_new_high =
    MAX2(unaligned_new_high, middle_high_boundary());
  char* unaligned_middle_new_high =
    MAX2(unaligned_new_high, lower_high_boundary());
  char* unaligned_lower_new_high =
    MAX2(unaligned_new_high, low_boundary());

  // Align address to region's alignment
  char* aligned_upper_new_high =  align_up(unaligned_upper_new_high, upper_alignment());
  char* aligned_middle_new_high = align_up(unaligned_middle_new_high, middle_alignment());
  char* aligned_lower_new_high =  align_up(unaligned_lower_new_high, lower_alignment());

  // Determine which regions need to shrink
  size_t upper_needs = 0;
  if (aligned_upper_new_high < upper_high()) {
    upper_needs =
      pointer_delta(upper_high(), aligned_upper_new_high, sizeof(char));
  }
  size_t middle_needs = 0;
  if (aligned_middle_new_high < middle_high()) {
    middle_needs =
      pointer_delta(middle_high(), aligned_middle_new_high, sizeof(char));
  }
  size_t lower_needs = 0;
  if (aligned_lower_new_high < lower_high()) {
    lower_needs =
      pointer_delta(lower_high(), aligned_lower_new_high, sizeof(char));
  }

  // Check contiguity.
  assert(middle_high_boundary() <= upper_high() &&
         upper_high() <= upper_high_boundary(),
         "high address must be contained within the region");
  assert(lower_high_boundary() <= middle_high() &&
         middle_high() <= middle_high_boundary(),
         "high address must be contained within the region");
  assert(low_boundary() <= lower_high() &&
         lower_high() <= lower_high_boundary(),
         "high address must be contained within the region");

  // Uncommit
  if (upper_needs > 0) {
    assert(middle_high_boundary() <= aligned_upper_new_high &&
           aligned_upper_new_high + upper_needs <= upper_high_boundary(),
           "must not shrink beyond region");
    if (!os::uncommit_memory(aligned_upper_new_high, upper_needs, _executable)) {
      debug_only(warning("os::uncommit_memory failed"));
      return;
    } else {
      _upper_high -= upper_needs;
    }
  }
  if (middle_needs > 0) {
    assert(lower_high_boundary() <= aligned_middle_new_high &&
           aligned_middle_new_high + middle_needs <= middle_high_boundary(),
           "must not shrink beyond region");
    if (!os::uncommit_memory(aligned_middle_new_high, middle_needs, _executable)) {
      debug_only(warning("os::uncommit_memory failed"));
      return;
    } else {
      _middle_high -= middle_needs;
    }
  }
  if (lower_needs > 0) {
    assert(low_boundary() <= aligned_lower_new_high &&
           aligned_lower_new_high + lower_needs <= lower_high_boundary(),
           "must not shrink beyond region");
    if (!os::uncommit_memory(aligned_lower_new_high, lower_needs, _executable)) {
      debug_only(warning("os::uncommit_memory failed"));
      return;
    } else {
      _lower_high -= lower_needs;
    }
  }

  _high -= size;
}

#ifndef PRODUCT
void VirtualSpace::check_for_contiguity() {
  // Check contiguity.
  assert(low_boundary() <= lower_high() &&
         lower_high() <= lower_high_boundary(),
         "high address must be contained within the region");
  assert(lower_high_boundary() <= middle_high() &&
         middle_high() <= middle_high_boundary(),
         "high address must be contained within the region");
  assert(middle_high_boundary() <= upper_high() &&
         upper_high() <= upper_high_boundary(),
         "high address must be contained within the region");
  assert(low() >= low_boundary(), "low");
  assert(low_boundary() <= lower_high_boundary(), "lower high boundary");
  assert(upper_high_boundary() <= high_boundary(), "upper high boundary");
  assert(high() <= upper_high(), "upper high");
}

void VirtualSpace::print_on(outputStream* out) const {
  out->print   ("Virtual space:");
  if (special()) out->print(" (pinned in memory)");
  out->cr();
  out->print_cr(" - committed: " SIZE_FORMAT, committed_size());
  out->print_cr(" - reserved:  " SIZE_FORMAT, reserved_size());
  out->print_cr(" - [low, high]:     [" INTPTR_FORMAT ", " INTPTR_FORMAT "]",  p2i(low()), p2i(high()));
  out->print_cr(" - [low_b, high_b]: [" INTPTR_FORMAT ", " INTPTR_FORMAT "]",  p2i(low_boundary()), p2i(high_boundary()));
}

void VirtualSpace::print() const {
  print_on(tty);
}

#endif
