/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zLargePages.inline.hpp"
#include "gc/z/zMapper_windows.hpp"
#include "gc/z/zSyscall_windows.hpp"
#include "gc/z/zVirtualMemory.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

class ZVirtualMemoryManagerImpl : public CHeapObj<mtGC> {
public:
  virtual void initialize_before_reserve() {}
  virtual void initialize_after_reserve(ZMemoryManager* manager) {}
  virtual bool reserve(uintptr_t addr, size_t size) = 0;
  virtual void unreserve(uintptr_t addr, size_t size) = 0;
};

// Implements small pages (paged) support using placeholder reservation.
class ZVirtualMemoryManagerSmallPages : public ZVirtualMemoryManagerImpl {
private:
  class PlaceholderCallbacks : public AllStatic {
  public:
    static void split_placeholder(uintptr_t start, size_t size) {
      ZMapper::split_placeholder(ZAddress::marked0(start), size);
      ZMapper::split_placeholder(ZAddress::marked1(start), size);
      ZMapper::split_placeholder(ZAddress::remapped(start), size);
    }

    static void coalesce_placeholders(uintptr_t start, size_t size) {
      ZMapper::coalesce_placeholders(ZAddress::marked0(start), size);
      ZMapper::coalesce_placeholders(ZAddress::marked1(start), size);
      ZMapper::coalesce_placeholders(ZAddress::remapped(start), size);
    }

    static void split_into_placeholder_granules(uintptr_t start, size_t size) {
      for (uintptr_t addr = start; addr < start + size; addr += ZGranuleSize) {
        split_placeholder(addr, ZGranuleSize);
      }
    }

    static void coalesce_into_one_placeholder(uintptr_t start, size_t size) {
      assert(is_aligned(size, ZGranuleSize), "Must be granule aligned");

      if (size > ZGranuleSize) {
        coalesce_placeholders(start, size);
      }
    }

    static void create_callback(const ZMemory* area) {
      assert(is_aligned(area->size(), ZGranuleSize), "Must be granule aligned");
      coalesce_into_one_placeholder(area->start(), area->size());
    }

    static void destroy_callback(const ZMemory* area) {
      assert(is_aligned(area->size(), ZGranuleSize), "Must be granule aligned");
      // Don't try split the last granule - VirtualFree will fail
      split_into_placeholder_granules(area->start(), area->size() - ZGranuleSize);
    }

    static void shrink_from_front_callback(const ZMemory* area, size_t size) {
      assert(is_aligned(size, ZGranuleSize), "Must be granule aligned");
      split_into_placeholder_granules(area->start(), size);
    }

    static void shrink_from_back_callback(const ZMemory* area, size_t size) {
      assert(is_aligned(size, ZGranuleSize), "Must be granule aligned");
      // Don't try split the last granule - VirtualFree will fail
      split_into_placeholder_granules(area->end() - size, size - ZGranuleSize);
    }

    static void grow_from_front_callback(const ZMemory* area, size_t size) {
      assert(is_aligned(area->size(), ZGranuleSize), "Must be granule aligned");
      coalesce_into_one_placeholder(area->start() - size, area->size() + size);
    }

    static void grow_from_back_callback(const ZMemory* area, size_t size) {
      assert(is_aligned(area->size(), ZGranuleSize), "Must be granule aligned");
      coalesce_into_one_placeholder(area->start(), area->size() + size);
    }

    static void register_with(ZMemoryManager* manager) {
      // Each reserved virtual memory address area registered in _manager is
      // exactly covered by a single placeholder. Callbacks are installed so
      // that whenever a memory area changes, the corresponding placeholder
      // is adjusted.
      //
      // The create and grow callbacks are called when virtual memory is
      // returned to the memory manager. The new memory area is then covered
      // by a new single placeholder.
      //
      // The destroy and shrink callbacks are called when virtual memory is
      // allocated from the memory manager. The memory area is then is split
      // into granule-sized placeholders.
      //
      // See comment in zMapper_windows.cpp explaining why placeholders are
      // split into ZGranuleSize sized placeholders.

      ZMemoryManager::Callbacks callbacks;

      callbacks._create = &create_callback;
      callbacks._destroy = &destroy_callback;
      callbacks._shrink_from_front = &shrink_from_front_callback;
      callbacks._shrink_from_back = &shrink_from_back_callback;
      callbacks._grow_from_front = &grow_from_front_callback;
      callbacks._grow_from_back = &grow_from_back_callback;

      manager->register_callbacks(callbacks);
    }
  };

  virtual void initialize_after_reserve(ZMemoryManager* manager) {
    PlaceholderCallbacks::register_with(manager);
  }

  virtual bool reserve(uintptr_t addr, size_t size) {
    const uintptr_t res = ZMapper::reserve(addr, size);

    assert(res == addr || res == NULL, "Should not reserve other memory than requested");
    return res == addr;
  }

  virtual void unreserve(uintptr_t addr, size_t size) {
    ZMapper::unreserve(addr, size);
  }
};

// Implements Large Pages (locked) support using shared AWE physical memory.

// ZPhysicalMemory layer needs access to the section
HANDLE ZAWESection;

class ZVirtualMemoryManagerLargePages : public ZVirtualMemoryManagerImpl {
private:
  virtual void initialize_before_reserve() {
    ZAWESection = ZMapper::create_shared_awe_section();
  }

  virtual bool reserve(uintptr_t addr, size_t size) {
    const uintptr_t res = ZMapper::reserve_for_shared_awe(ZAWESection, addr, size);

    assert(res == addr || res == NULL, "Should not reserve other memory than requested");
    return res == addr;
  }

  virtual void unreserve(uintptr_t addr, size_t size) {
    ZMapper::unreserve_for_shared_awe(addr, size);
  }
};

static ZVirtualMemoryManagerImpl* _impl = NULL;

void ZVirtualMemoryManager::pd_initialize_before_reserve() {
  if (ZLargePages::is_enabled()) {
    _impl = new ZVirtualMemoryManagerLargePages();
  } else {
    _impl = new ZVirtualMemoryManagerSmallPages();
  }
  _impl->initialize_before_reserve();
}

void ZVirtualMemoryManager::pd_initialize_after_reserve() {
  _impl->initialize_after_reserve(&_manager);
}

bool ZVirtualMemoryManager::pd_reserve(uintptr_t addr, size_t size) {
  return _impl->reserve(addr, size);
}

void ZVirtualMemoryManager::pd_unreserve(uintptr_t addr, size_t size) {
  _impl->unreserve(addr, size);
}
