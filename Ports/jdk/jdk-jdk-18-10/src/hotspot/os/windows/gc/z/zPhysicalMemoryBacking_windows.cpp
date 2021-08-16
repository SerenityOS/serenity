/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zGlobals.hpp"
#include "gc/z/zGranuleMap.inline.hpp"
#include "gc/z/zLargePages.inline.hpp"
#include "gc/z/zMapper_windows.hpp"
#include "gc/z/zPhysicalMemoryBacking_windows.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"

class ZPhysicalMemoryBackingImpl : public CHeapObj<mtGC> {
public:
  virtual size_t commit(size_t offset, size_t size) = 0;
  virtual size_t uncommit(size_t offset, size_t size) = 0;
  virtual void map(uintptr_t addr, size_t size, size_t offset) const = 0;
  virtual void unmap(uintptr_t addr, size_t size) const = 0;
};

// Implements small pages (paged) support using placeholder reservation.
//
// The backing commits and uncommits physical memory, that can be
// multi-mapped into the virtual address space. To support fine-graned
// committing and uncommitting, each ZGranuleSize'd chunk is mapped to
// a separate paging file mapping.

class ZPhysicalMemoryBackingSmallPages : public ZPhysicalMemoryBackingImpl {
private:
  ZGranuleMap<HANDLE> _handles;

  HANDLE get_handle(uintptr_t offset) const {
    HANDLE const handle = _handles.get(offset);
    assert(handle != 0, "Should be set");
    return handle;
  }

  void put_handle(uintptr_t offset, HANDLE handle) {
    assert(handle != INVALID_HANDLE_VALUE, "Invalid handle");
    assert(_handles.get(offset) == 0, "Should be cleared");
    _handles.put(offset, handle);
  }

  void clear_handle(uintptr_t offset) {
    assert(_handles.get(offset) != 0, "Should be set");
    _handles.put(offset, 0);
  }

public:
  ZPhysicalMemoryBackingSmallPages(size_t max_capacity) :
      ZPhysicalMemoryBackingImpl(),
      _handles(max_capacity) {}

  size_t commit(size_t offset, size_t size) {
    for (size_t i = 0; i < size; i += ZGranuleSize) {
      HANDLE const handle = ZMapper::create_and_commit_paging_file_mapping(ZGranuleSize);
      if (handle == 0) {
        return i;
      }

      put_handle(offset + i, handle);
    }

    return size;
  }

  size_t uncommit(size_t offset, size_t size) {
    for (size_t i = 0; i < size; i += ZGranuleSize) {
      HANDLE const handle = get_handle(offset + i);
      clear_handle(offset + i);
      ZMapper::close_paging_file_mapping(handle);
    }

    return size;
  }

  void map(uintptr_t addr, size_t size, size_t offset) const {
    assert(is_aligned(offset, ZGranuleSize), "Misaligned");
    assert(is_aligned(addr, ZGranuleSize), "Misaligned");
    assert(is_aligned(size, ZGranuleSize), "Misaligned");

    for (size_t i = 0; i < size; i += ZGranuleSize) {
      HANDLE const handle = get_handle(offset + i);
      ZMapper::map_view_replace_placeholder(handle, 0 /* offset */, addr + i, ZGranuleSize);
    }
  }

  void unmap(uintptr_t addr, size_t size) const {
    assert(is_aligned(addr, ZGranuleSize), "Misaligned");
    assert(is_aligned(size, ZGranuleSize), "Misaligned");

    for (size_t i = 0; i < size; i += ZGranuleSize) {
      ZMapper::unmap_view_preserve_placeholder(addr + i, ZGranuleSize);
    }
  }
};

// Implements Large Pages (locked) support using shared AWE physical memory.
//
// Shared AWE physical memory also works with small pages, but it has
// a few drawbacks that makes it a no-go to use it at this point:
//
// 1) It seems to use 8 bytes of committed memory per *reserved* memory.
// Given our scheme to use a large address space range this turns out to
// use too much memory.
//
// 2) It requires memory locking privilages, even for small pages. This
// has always been a requirement for large pages, and would be an extra
// restriction for usage with small pages.
//
// Note: The large pages size is tied to our ZGranuleSize.

extern HANDLE ZAWESection;

class ZPhysicalMemoryBackingLargePages : public ZPhysicalMemoryBackingImpl {
private:
  ULONG_PTR* const _page_array;

  static ULONG_PTR* alloc_page_array(size_t max_capacity) {
    const size_t npages = max_capacity / ZGranuleSize;
    const size_t array_size = npages * sizeof(ULONG_PTR);

    return (ULONG_PTR*)os::malloc(array_size, mtGC);
  }

public:
  ZPhysicalMemoryBackingLargePages(size_t max_capacity) :
      ZPhysicalMemoryBackingImpl(),
      _page_array(alloc_page_array(max_capacity)) {}

  size_t commit(size_t offset, size_t size) {
    const size_t index = offset >> ZGranuleSizeShift;
    const size_t npages = size >> ZGranuleSizeShift;

    size_t npages_res = npages;
    const bool res = AllocateUserPhysicalPages(ZAWESection, &npages_res, &_page_array[index]);
    if (!res) {
      fatal("Failed to allocate physical memory " SIZE_FORMAT "M @ " PTR_FORMAT " (%d)",
            size / M, offset, GetLastError());
    } else {
      log_debug(gc)("Allocated physical memory: " SIZE_FORMAT "M @ " PTR_FORMAT, size / M, offset);
    }

    // AllocateUserPhysicalPages might not be able to allocate the requested amount of memory.
    // The allocated number of pages are written in npages_res.
    return npages_res << ZGranuleSizeShift;
  }

  size_t uncommit(size_t offset, size_t size) {
    const size_t index = offset >> ZGranuleSizeShift;
    const size_t npages = size >> ZGranuleSizeShift;

    size_t npages_res = npages;
    const bool res = FreeUserPhysicalPages(ZAWESection, &npages_res, &_page_array[index]);
    if (!res) {
      fatal("Failed to uncommit physical memory " SIZE_FORMAT "M @ " PTR_FORMAT " (%d)",
            size, offset, GetLastError());
    }

    return npages_res << ZGranuleSizeShift;
  }

  void map(uintptr_t addr, size_t size, size_t offset) const {
    const size_t npages = size >> ZGranuleSizeShift;
    const size_t index = offset >> ZGranuleSizeShift;

    const bool res = MapUserPhysicalPages((char*)addr, npages, &_page_array[index]);
    if (!res) {
      fatal("Failed to map view " PTR_FORMAT " " SIZE_FORMAT "M @ " PTR_FORMAT " (%d)",
            addr, size / M, offset, GetLastError());
    }
  }

  void unmap(uintptr_t addr, size_t size) const {
    const size_t npages = size >> ZGranuleSizeShift;

    const bool res = MapUserPhysicalPages((char*)addr, npages, NULL);
    if (!res) {
      fatal("Failed to unmap view " PTR_FORMAT " " SIZE_FORMAT "M (%d)",
            addr, size / M, GetLastError());
    }
  }
};

static ZPhysicalMemoryBackingImpl* select_impl(size_t max_capacity) {
  if (ZLargePages::is_enabled()) {
    return new ZPhysicalMemoryBackingLargePages(max_capacity);
  }

  return new ZPhysicalMemoryBackingSmallPages(max_capacity);
}

ZPhysicalMemoryBacking::ZPhysicalMemoryBacking(size_t max_capacity) :
    _impl(select_impl(max_capacity)) {}

bool ZPhysicalMemoryBacking::is_initialized() const {
  return true;
}

void ZPhysicalMemoryBacking::warn_commit_limits(size_t max_capacity) const {
  // Does nothing
}

size_t ZPhysicalMemoryBacking::commit(size_t offset, size_t length) {
  log_trace(gc, heap)("Committing memory: " SIZE_FORMAT "M-" SIZE_FORMAT "M (" SIZE_FORMAT "M)",
                      offset / M, (offset + length) / M, length / M);

  return _impl->commit(offset, length);
}

size_t ZPhysicalMemoryBacking::uncommit(size_t offset, size_t length) {
  log_trace(gc, heap)("Uncommitting memory: " SIZE_FORMAT "M-" SIZE_FORMAT "M (" SIZE_FORMAT "M)",
                      offset / M, (offset + length) / M, length / M);

  return _impl->uncommit(offset, length);
}

void ZPhysicalMemoryBacking::map(uintptr_t addr, size_t size, size_t offset) const {
  assert(is_aligned(offset, ZGranuleSize), "Misaligned: " PTR_FORMAT, offset);
  assert(is_aligned(addr, ZGranuleSize), "Misaligned: " PTR_FORMAT, addr);
  assert(is_aligned(size, ZGranuleSize), "Misaligned: " PTR_FORMAT, size);

  _impl->map(addr, size, offset);
}

void ZPhysicalMemoryBacking::unmap(uintptr_t addr, size_t size) const {
  assert(is_aligned(addr, ZGranuleSize), "Misaligned");
  assert(is_aligned(size, ZGranuleSize), "Misaligned");

  _impl->unmap(addr, size);
}
