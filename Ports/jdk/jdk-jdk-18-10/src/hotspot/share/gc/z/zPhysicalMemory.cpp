/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zArray.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zLargePages.inline.hpp"
#include "gc/z/zNUMA.inline.hpp"
#include "gc/z/zPhysicalMemory.inline.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/init.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"

ZPhysicalMemory::ZPhysicalMemory() :
    _segments() {}

ZPhysicalMemory::ZPhysicalMemory(const ZPhysicalMemorySegment& segment) :
    _segments() {
  add_segment(segment);
}

ZPhysicalMemory::ZPhysicalMemory(const ZPhysicalMemory& pmem) :
    _segments() {
  add_segments(pmem);
}

const ZPhysicalMemory& ZPhysicalMemory::operator=(const ZPhysicalMemory& pmem) {
  // Free segments
  _segments.clear_and_deallocate();

  // Copy segments
  add_segments(pmem);

  return *this;
}

size_t ZPhysicalMemory::size() const {
  size_t size = 0;

  for (int i = 0; i < _segments.length(); i++) {
    size += _segments.at(i).size();
  }

  return size;
}

void ZPhysicalMemory::insert_segment(int index, uintptr_t start, size_t size, bool committed) {
  _segments.insert_before(index, ZPhysicalMemorySegment(start, size, committed));
}

void ZPhysicalMemory::replace_segment(int index, uintptr_t start, size_t size, bool committed) {
  _segments.at_put(index, ZPhysicalMemorySegment(start, size, committed));
}

void ZPhysicalMemory::remove_segment(int index) {
  _segments.remove_at(index);
}

void ZPhysicalMemory::add_segments(const ZPhysicalMemory& pmem) {
  for (int i = 0; i < pmem.nsegments(); i++) {
    add_segment(pmem.segment(i));
  }
}

void ZPhysicalMemory::remove_segments() {
  _segments.clear_and_deallocate();
}

static bool is_mergable(const ZPhysicalMemorySegment& before, const ZPhysicalMemorySegment& after) {
  return before.end() == after.start() && before.is_committed() == after.is_committed();
}

void ZPhysicalMemory::add_segment(const ZPhysicalMemorySegment& segment) {
  // Insert segments in address order, merge segments when possible
  for (int i = _segments.length(); i > 0; i--) {
    const int current = i - 1;

    if (_segments.at(current).end() <= segment.start()) {
      if (is_mergable(_segments.at(current), segment)) {
        if (current + 1 < _segments.length() && is_mergable(segment, _segments.at(current + 1))) {
          // Merge with end of current segment and start of next segment
          const size_t start = _segments.at(current).start();
          const size_t size = _segments.at(current).size() + segment.size() + _segments.at(current + 1).size();
          replace_segment(current, start, size, segment.is_committed());
          remove_segment(current + 1);
          return;
        }

        // Merge with end of current segment
        const size_t start = _segments.at(current).start();
        const size_t size = _segments.at(current).size() + segment.size();
        replace_segment(current, start, size, segment.is_committed());
        return;
      } else if (current + 1 < _segments.length() && is_mergable(segment, _segments.at(current + 1))) {
        // Merge with start of next segment
        const size_t start = segment.start();
        const size_t size = segment.size() + _segments.at(current + 1).size();
        replace_segment(current + 1, start, size, segment.is_committed());
        return;
      }

      // Insert after current segment
      insert_segment(current + 1, segment.start(), segment.size(), segment.is_committed());
      return;
    }
  }

  if (_segments.length() > 0 && is_mergable(segment, _segments.at(0))) {
    // Merge with start of first segment
    const size_t start = segment.start();
    const size_t size = segment.size() + _segments.at(0).size();
    replace_segment(0, start, size, segment.is_committed());
    return;
  }

  // Insert before first segment
  insert_segment(0, segment.start(), segment.size(), segment.is_committed());
}

bool ZPhysicalMemory::commit_segment(int index, size_t size) {
  assert(size <= _segments.at(index).size(), "Invalid size");
  assert(!_segments.at(index).is_committed(), "Invalid state");

  if (size == _segments.at(index).size()) {
    // Completely committed
    _segments.at(index).set_committed(true);
    return true;
  }

  if (size > 0) {
    // Partially committed, split segment
    insert_segment(index + 1, _segments.at(index).start() + size, _segments.at(index).size() - size, false /* committed */);
    replace_segment(index, _segments.at(index).start(), size, true /* committed */);
  }

  return false;
}

bool ZPhysicalMemory::uncommit_segment(int index, size_t size) {
  assert(size <= _segments.at(index).size(), "Invalid size");
  assert(_segments.at(index).is_committed(), "Invalid state");

  if (size == _segments.at(index).size()) {
    // Completely uncommitted
    _segments.at(index).set_committed(false);
    return true;
  }

  if (size > 0) {
    // Partially uncommitted, split segment
    insert_segment(index + 1, _segments.at(index).start() + size, _segments.at(index).size() - size, true /* committed */);
    replace_segment(index, _segments.at(index).start(), size, false /* committed */);
  }

  return false;
}

ZPhysicalMemory ZPhysicalMemory::split(size_t size) {
  ZPhysicalMemory pmem;
  int nsegments = 0;

  for (int i = 0; i < _segments.length(); i++) {
    const ZPhysicalMemorySegment& segment = _segments.at(i);
    if (pmem.size() < size) {
      if (pmem.size() + segment.size() <= size) {
        // Transfer segment
        pmem.add_segment(segment);
      } else {
        // Split segment
        const size_t split_size = size - pmem.size();
        pmem.add_segment(ZPhysicalMemorySegment(segment.start(), split_size, segment.is_committed()));
        _segments.at_put(nsegments++, ZPhysicalMemorySegment(segment.start() + split_size, segment.size() - split_size, segment.is_committed()));
      }
    } else {
      // Keep segment
      _segments.at_put(nsegments++, segment);
    }
  }

  _segments.trunc_to(nsegments);

  return pmem;
}

ZPhysicalMemory ZPhysicalMemory::split_committed() {
  ZPhysicalMemory pmem;
  int nsegments = 0;

  for (int i = 0; i < _segments.length(); i++) {
    const ZPhysicalMemorySegment& segment = _segments.at(i);
    if (segment.is_committed()) {
      // Transfer segment
      pmem.add_segment(segment);
    } else {
      // Keep segment
      _segments.at_put(nsegments++, segment);
    }
  }

  _segments.trunc_to(nsegments);

  return pmem;
}

ZPhysicalMemoryManager::ZPhysicalMemoryManager(size_t max_capacity) :
    _backing(max_capacity) {
  // Make the whole range free
  _manager.free(0, max_capacity);
}

bool ZPhysicalMemoryManager::is_initialized() const {
  return _backing.is_initialized();
}

void ZPhysicalMemoryManager::warn_commit_limits(size_t max_capacity) const {
  _backing.warn_commit_limits(max_capacity);
}

void ZPhysicalMemoryManager::try_enable_uncommit(size_t min_capacity, size_t max_capacity) {
  assert(!is_init_completed(), "Invalid state");

  // If uncommit is not explicitly disabled, max capacity is greater than
  // min capacity, and uncommit is supported by the platform, then uncommit
  // will be enabled.
  if (!ZUncommit) {
    log_info_p(gc, init)("Uncommit: Disabled");
    return;
  }

  if (max_capacity == min_capacity) {
    log_info_p(gc, init)("Uncommit: Implicitly Disabled (-Xms equals -Xmx)");
    FLAG_SET_ERGO(ZUncommit, false);
    return;
  }

  // Test if uncommit is supported by the operating system by committing
  // and then uncommitting a granule.
  ZPhysicalMemory pmem(ZPhysicalMemorySegment(0, ZGranuleSize, false /* committed */));
  if (!commit(pmem) || !uncommit(pmem)) {
    log_info_p(gc, init)("Uncommit: Implicitly Disabled (Not supported by operating system)");
    FLAG_SET_ERGO(ZUncommit, false);
    return;
  }

  log_info_p(gc, init)("Uncommit: Enabled");
  log_info_p(gc, init)("Uncommit Delay: " UINTX_FORMAT "s", ZUncommitDelay);
}

void ZPhysicalMemoryManager::nmt_commit(uintptr_t offset, size_t size) const {
  // From an NMT point of view we treat the first heap view (marked0) as committed
  const uintptr_t addr = ZAddress::marked0(offset);
  MemTracker::record_virtual_memory_commit((void*)addr, size, CALLER_PC);
}

void ZPhysicalMemoryManager::nmt_uncommit(uintptr_t offset, size_t size) const {
  if (MemTracker::tracking_level() > NMT_minimal) {
    const uintptr_t addr = ZAddress::marked0(offset);
    Tracker tracker(Tracker::uncommit);
    tracker.record((address)addr, size);
  }
}

void ZPhysicalMemoryManager::alloc(ZPhysicalMemory& pmem, size_t size) {
  assert(is_aligned(size, ZGranuleSize), "Invalid size");

  // Allocate segments
  while (size > 0) {
    size_t allocated = 0;
    const uintptr_t start = _manager.alloc_from_front_at_most(size, &allocated);
    assert(start != UINTPTR_MAX, "Allocation should never fail");
    pmem.add_segment(ZPhysicalMemorySegment(start, allocated, false /* committed */));
    size -= allocated;
  }
}

void ZPhysicalMemoryManager::free(const ZPhysicalMemory& pmem) {
  // Free segments
  for (int i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    _manager.free(segment.start(), segment.size());
  }
}

bool ZPhysicalMemoryManager::commit(ZPhysicalMemory& pmem) {
  // Commit segments
  for (int i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    if (segment.is_committed()) {
      // Segment already committed
      continue;
    }

    // Commit segment
    const size_t committed = _backing.commit(segment.start(), segment.size());
    if (!pmem.commit_segment(i, committed)) {
      // Failed or partially failed
      return false;
    }
  }

  // Success
  return true;
}

bool ZPhysicalMemoryManager::uncommit(ZPhysicalMemory& pmem) {
  // Commit segments
  for (int i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    if (!segment.is_committed()) {
      // Segment already uncommitted
      continue;
    }

    // Uncommit segment
    const size_t uncommitted = _backing.uncommit(segment.start(), segment.size());
    if (!pmem.uncommit_segment(i, uncommitted)) {
      // Failed or partially failed
      return false;
    }
  }

  // Success
  return true;
}

void ZPhysicalMemoryManager::pretouch_view(uintptr_t addr, size_t size) const {
  const size_t page_size = ZLargePages::is_explicit() ? ZGranuleSize : os::vm_page_size();
  os::pretouch_memory((void*)addr, (void*)(addr + size), page_size);
}

void ZPhysicalMemoryManager::map_view(uintptr_t addr, const ZPhysicalMemory& pmem) const {
  size_t size = 0;

  // Map segments
  for (int i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    _backing.map(addr + size, segment.size(), segment.start());
    size += segment.size();
  }

  // Setup NUMA interleaving for large pages
  if (ZNUMA::is_enabled() && ZLargePages::is_explicit()) {
    // To get granule-level NUMA interleaving when using large pages,
    // we simply let the kernel interleave the memory for us at page
    // fault time.
    os::numa_make_global((char*)addr, size);
  }
}

void ZPhysicalMemoryManager::unmap_view(uintptr_t addr, size_t size) const {
  _backing.unmap(addr, size);
}

void ZPhysicalMemoryManager::pretouch(uintptr_t offset, size_t size) const {
  if (ZVerifyViews) {
    // Pre-touch good view
    pretouch_view(ZAddress::good(offset), size);
  } else {
    // Pre-touch all views
    pretouch_view(ZAddress::marked0(offset), size);
    pretouch_view(ZAddress::marked1(offset), size);
    pretouch_view(ZAddress::remapped(offset), size);
  }
}

void ZPhysicalMemoryManager::map(uintptr_t offset, const ZPhysicalMemory& pmem) const {
  const size_t size = pmem.size();

  if (ZVerifyViews) {
    // Map good view
    map_view(ZAddress::good(offset), pmem);
  } else {
    // Map all views
    map_view(ZAddress::marked0(offset), pmem);
    map_view(ZAddress::marked1(offset), pmem);
    map_view(ZAddress::remapped(offset), pmem);
  }

  nmt_commit(offset, size);
}

void ZPhysicalMemoryManager::unmap(uintptr_t offset, size_t size) const {
  nmt_uncommit(offset, size);

  if (ZVerifyViews) {
    // Unmap good view
    unmap_view(ZAddress::good(offset), size);
  } else {
    // Unmap all views
    unmap_view(ZAddress::marked0(offset), size);
    unmap_view(ZAddress::marked1(offset), size);
    unmap_view(ZAddress::remapped(offset), size);
  }
}

void ZPhysicalMemoryManager::debug_map(uintptr_t offset, const ZPhysicalMemory& pmem) const {
  // Map good view
  assert(ZVerifyViews, "Should be enabled");
  map_view(ZAddress::good(offset), pmem);
}

void ZPhysicalMemoryManager::debug_unmap(uintptr_t offset, size_t size) const {
  // Unmap good view
  assert(ZVerifyViews, "Should be enabled");
  unmap_view(ZAddress::good(offset), size);
}
