/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1PageBasedVirtualSpace.hpp"
#include "gc/shared/pretouchTask.hpp"
#include "gc/shared/workgroup.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/bitMap.inline.hpp"

G1PageBasedVirtualSpace::G1PageBasedVirtualSpace(ReservedSpace rs, size_t used_size, size_t page_size) :
  _low_boundary(NULL), _high_boundary(NULL), _tail_size(0), _page_size(0),
  _committed(mtGC), _dirty(mtGC), _special(false), _executable(false) {
  initialize_with_page_size(rs, used_size, page_size);
}

void G1PageBasedVirtualSpace::initialize_with_page_size(ReservedSpace rs, size_t used_size, size_t page_size) {
  guarantee(rs.is_reserved(), "Given reserved space must have been reserved already.");

  vmassert(_low_boundary == NULL, "VirtualSpace already initialized");
  vmassert(page_size > 0, "Page size must be non-zero.");

  guarantee(is_aligned(rs.base(), page_size),
            "Reserved space base " PTR_FORMAT " is not aligned to requested page size " SIZE_FORMAT, p2i(rs.base()), page_size);
  guarantee(is_aligned(used_size, os::vm_page_size()),
            "Given used reserved space size needs to be OS page size aligned (%d bytes) but is " SIZE_FORMAT, os::vm_page_size(), used_size);
  guarantee(used_size <= rs.size(),
            "Used size of reserved space " SIZE_FORMAT " bytes is smaller than reservation at " SIZE_FORMAT " bytes", used_size, rs.size());
  guarantee(is_aligned(rs.size(), page_size),
            "Expected that the virtual space is size aligned, but " SIZE_FORMAT " is not aligned to page size " SIZE_FORMAT, rs.size(), page_size);

  _low_boundary  = rs.base();
  _high_boundary = _low_boundary + used_size;

  _special = rs.special();
  _executable = rs.executable();

  _page_size = page_size;

  vmassert(_committed.size() == 0, "virtual space initialized more than once");
  BitMap::idx_t size_in_pages = rs.size() / page_size;
  _committed.initialize(size_in_pages);
  if (_special) {
    _dirty.initialize(size_in_pages);
  }

  _tail_size = used_size % _page_size;
}

G1PageBasedVirtualSpace::~G1PageBasedVirtualSpace() {
  // This does not release memory it never reserved.
  // Caller must release via rs.release();
  _low_boundary           = NULL;
  _high_boundary          = NULL;
  _special                = false;
  _executable             = false;
  _page_size              = 0;
  _tail_size              = 0;
}

size_t G1PageBasedVirtualSpace::committed_size() const {
  size_t result = _committed.count_one_bits() * _page_size;
  // The last page might not be in full.
  if (is_last_page_partial() && _committed.at(_committed.size() - 1)) {
    result -= _page_size - _tail_size;
  }
  return result;
}

size_t G1PageBasedVirtualSpace::reserved_size() const {
  return pointer_delta(_high_boundary, _low_boundary, sizeof(char));
}

size_t G1PageBasedVirtualSpace::uncommitted_size()  const {
  return reserved_size() - committed_size();
}

size_t G1PageBasedVirtualSpace::addr_to_page_index(char* addr) const {
  return (addr - _low_boundary) / _page_size;
}

bool G1PageBasedVirtualSpace::is_area_committed(size_t start_page, size_t size_in_pages) const {
  size_t end_page = start_page + size_in_pages;
  return _committed.get_next_zero_offset(start_page, end_page) >= end_page;
}

bool G1PageBasedVirtualSpace::is_area_uncommitted(size_t start_page, size_t size_in_pages) const {
  size_t end_page = start_page + size_in_pages;
  return _committed.get_next_one_offset(start_page, end_page) >= end_page;
}

char* G1PageBasedVirtualSpace::page_start(size_t index) const {
  return _low_boundary + index * _page_size;
}

size_t G1PageBasedVirtualSpace::page_size() const {
  assert(_page_size > 0, "Page size is not yet initialized.");
  return _page_size;
}

bool G1PageBasedVirtualSpace::is_after_last_page(size_t index) const {
  guarantee(index <= _committed.size(),
            "Given boundary page " SIZE_FORMAT " is beyond managed page count " SIZE_FORMAT, index, _committed.size());
  return index == _committed.size();
}

void G1PageBasedVirtualSpace::commit_preferred_pages(size_t start, size_t num_pages) {
  vmassert(num_pages > 0, "No full pages to commit");
  vmassert(start + num_pages <= _committed.size(),
           "Tried to commit area from page " SIZE_FORMAT " to page " SIZE_FORMAT " "
           "that is outside of managed space of " SIZE_FORMAT " pages",
           start, start + num_pages, _committed.size());

  char* start_addr = page_start(start);
  size_t size = num_pages * _page_size;

  os::commit_memory_or_exit(start_addr, size, _page_size, _executable, "G1 virtual space");
}

void G1PageBasedVirtualSpace::commit_tail() {
  vmassert(_tail_size > 0, "The size of the tail area must be > 0 when reaching here");

  char* const aligned_end_address = align_down(_high_boundary, _page_size);
  os::commit_memory_or_exit(aligned_end_address, _tail_size, os::vm_page_size(), _executable, "G1 virtual space");
}

void G1PageBasedVirtualSpace::commit_internal(size_t start_page, size_t end_page) {
  guarantee(start_page < end_page,
            "Given start page " SIZE_FORMAT " is larger or equal to end page " SIZE_FORMAT, start_page, end_page);
  guarantee(end_page <= _committed.size(),
            "Given end page " SIZE_FORMAT " is beyond end of managed page amount of " SIZE_FORMAT, end_page, _committed.size());

  size_t pages = end_page - start_page;
  bool need_to_commit_tail = is_after_last_page(end_page) && is_last_page_partial();

  // If we have to commit some (partial) tail area, decrease the amount of pages to avoid
  // committing that in the full-page commit code.
  if (need_to_commit_tail) {
    pages--;
  }

  if (pages > 0) {
    commit_preferred_pages(start_page, pages);
  }

  if (need_to_commit_tail) {
    commit_tail();
  }
}

char* G1PageBasedVirtualSpace::bounded_end_addr(size_t end_page) const {
  return MIN2(_high_boundary, page_start(end_page));
}

void G1PageBasedVirtualSpace::pretouch_internal(size_t start_page, size_t end_page) {
  guarantee(start_page < end_page,
            "Given start page " SIZE_FORMAT " is larger or equal to end page " SIZE_FORMAT, start_page, end_page);

  os::pretouch_memory(page_start(start_page), bounded_end_addr(end_page), _page_size);
}

bool G1PageBasedVirtualSpace::commit(size_t start_page, size_t size_in_pages) {
  // We need to make sure to commit all pages covered by the given area.
  guarantee(is_area_uncommitted(start_page, size_in_pages),
            "Specified area is not uncommitted, start page: " SIZE_FORMAT ", page count: " SIZE_FORMAT,
            start_page, size_in_pages);

  bool zero_filled = true;
  size_t end_page = start_page + size_in_pages;

  if (_special) {
    // Check for dirty pages and update zero_filled if any found.
    if (_dirty.get_next_one_offset(start_page, end_page) < end_page) {
      zero_filled = false;
      _dirty.par_clear_range(start_page, end_page, BitMap::unknown_range);
    }
  } else {
    commit_internal(start_page, end_page);
  }
  _committed.par_set_range(start_page, end_page, BitMap::unknown_range);

  return zero_filled;
}

void G1PageBasedVirtualSpace::uncommit_internal(size_t start_page, size_t end_page) {
  guarantee(start_page < end_page,
            "Given start page " SIZE_FORMAT " is larger or equal to end page " SIZE_FORMAT, start_page, end_page);

  char* start_addr = page_start(start_page);
  os::uncommit_memory(start_addr, pointer_delta(bounded_end_addr(end_page), start_addr, sizeof(char)));
}

void G1PageBasedVirtualSpace::uncommit(size_t start_page, size_t size_in_pages) {
  guarantee(is_area_committed(start_page, size_in_pages),
            "Specified area is not committed, start page: " SIZE_FORMAT ", page count: " SIZE_FORMAT,
            start_page, size_in_pages);

  size_t end_page = start_page + size_in_pages;
  if (_special) {
    // Mark that memory is dirty. If committed again the memory might
    // need to be cleared explicitly.
    _dirty.par_set_range(start_page, end_page, BitMap::unknown_range);
  } else {
    uncommit_internal(start_page, end_page);
  }

  _committed.par_clear_range(start_page, end_page, BitMap::unknown_range);
}

void G1PageBasedVirtualSpace::pretouch(size_t start_page, size_t size_in_pages, WorkGang* pretouch_gang) {

  PretouchTask::pretouch("G1 PreTouch", page_start(start_page), bounded_end_addr(start_page + size_in_pages),
                         _page_size, pretouch_gang);
}

bool G1PageBasedVirtualSpace::contains(const void* p) const {
  return _low_boundary <= (const char*) p && (const char*) p < _high_boundary;
}

#ifndef PRODUCT
void G1PageBasedVirtualSpace::print_on(outputStream* out) {
  out->print   ("Virtual space:");
  if (_special) out->print(" (pinned in memory)");
  out->cr();
  out->print_cr(" - committed: " SIZE_FORMAT, committed_size());
  out->print_cr(" - reserved:  " SIZE_FORMAT, reserved_size());
  out->print_cr(" - preferred page size: " SIZE_FORMAT, _page_size);
  out->print_cr(" - [low_b, high_b]: [" PTR_FORMAT ", " PTR_FORMAT "]",  p2i(_low_boundary), p2i(_high_boundary));
}

void G1PageBasedVirtualSpace::print() {
  print_on(tty);
}
#endif
