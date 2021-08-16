/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zHeuristics.hpp"
#include "gc/z/zObjectAllocator.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zPageTable.inline.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zThread.inline.hpp"
#include "gc/z/zValue.inline.hpp"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

static const ZStatCounter ZCounterUndoObjectAllocationSucceeded("Memory", "Undo Object Allocation Succeeded", ZStatUnitOpsPerSecond);
static const ZStatCounter ZCounterUndoObjectAllocationFailed("Memory", "Undo Object Allocation Failed", ZStatUnitOpsPerSecond);

ZObjectAllocator::ZObjectAllocator() :
    _use_per_cpu_shared_small_pages(ZHeuristics::use_per_cpu_shared_small_pages()),
    _used(0),
    _undone(0),
    _alloc_for_relocation(0),
    _undo_alloc_for_relocation(0),
    _shared_medium_page(NULL),
    _shared_small_page(NULL) {}

ZPage** ZObjectAllocator::shared_small_page_addr() {
  return _use_per_cpu_shared_small_pages ? _shared_small_page.addr() : _shared_small_page.addr(0);
}

ZPage* const* ZObjectAllocator::shared_small_page_addr() const {
  return _use_per_cpu_shared_small_pages ? _shared_small_page.addr() : _shared_small_page.addr(0);
}

void ZObjectAllocator::register_alloc_for_relocation(const ZPageTable* page_table, uintptr_t addr, size_t size) {
  const ZPage* const page = page_table->get(addr);
  const size_t aligned_size = align_up(size, page->object_alignment());
  Atomic::add(_alloc_for_relocation.addr(), aligned_size);
}

void ZObjectAllocator::register_undo_alloc_for_relocation(const ZPage* page, size_t size) {
  const size_t aligned_size = align_up(size, page->object_alignment());
  Atomic::add(_undo_alloc_for_relocation.addr(), aligned_size);
}

ZPage* ZObjectAllocator::alloc_page(uint8_t type, size_t size, ZAllocationFlags flags) {
  ZPage* const page = ZHeap::heap()->alloc_page(type, size, flags);
  if (page != NULL) {
    // Increment used bytes
    Atomic::add(_used.addr(), size);
  }

  return page;
}

void ZObjectAllocator::undo_alloc_page(ZPage* page) {
  // Increment undone bytes
  Atomic::add(_undone.addr(), page->size());

  ZHeap::heap()->undo_alloc_page(page);
}

uintptr_t ZObjectAllocator::alloc_object_in_shared_page(ZPage** shared_page,
                                                        uint8_t page_type,
                                                        size_t page_size,
                                                        size_t size,
                                                        ZAllocationFlags flags) {
  uintptr_t addr = 0;
  ZPage* page = Atomic::load_acquire(shared_page);

  if (page != NULL) {
    addr = page->alloc_object_atomic(size);
  }

  if (addr == 0) {
    // Allocate new page
    ZPage* const new_page = alloc_page(page_type, page_size, flags);
    if (new_page != NULL) {
      // Allocate object before installing the new page
      addr = new_page->alloc_object(size);

    retry:
      // Install new page
      ZPage* const prev_page = Atomic::cmpxchg(shared_page, page, new_page);
      if (prev_page != page) {
        if (prev_page == NULL) {
          // Previous page was retired, retry installing the new page
          page = prev_page;
          goto retry;
        }

        // Another page already installed, try allocation there first
        const uintptr_t prev_addr = prev_page->alloc_object_atomic(size);
        if (prev_addr == 0) {
          // Allocation failed, retry installing the new page
          page = prev_page;
          goto retry;
        }

        // Allocation succeeded in already installed page
        addr = prev_addr;

        // Undo new page allocation
        undo_alloc_page(new_page);
      }
    }
  }

  return addr;
}

uintptr_t ZObjectAllocator::alloc_large_object(size_t size, ZAllocationFlags flags) {
  uintptr_t addr = 0;

  // Allocate new large page
  const size_t page_size = align_up(size, ZGranuleSize);
  ZPage* const page = alloc_page(ZPageTypeLarge, page_size, flags);
  if (page != NULL) {
    // Allocate the object
    addr = page->alloc_object(size);
  }

  return addr;
}

uintptr_t ZObjectAllocator::alloc_medium_object(size_t size, ZAllocationFlags flags) {
  return alloc_object_in_shared_page(_shared_medium_page.addr(), ZPageTypeMedium, ZPageSizeMedium, size, flags);
}

uintptr_t ZObjectAllocator::alloc_small_object(size_t size, ZAllocationFlags flags) {
  return alloc_object_in_shared_page(shared_small_page_addr(), ZPageTypeSmall, ZPageSizeSmall, size, flags);
}

uintptr_t ZObjectAllocator::alloc_object(size_t size, ZAllocationFlags flags) {
  if (size <= ZObjectSizeLimitSmall) {
    // Small
    return alloc_small_object(size, flags);
  } else if (size <= ZObjectSizeLimitMedium) {
    // Medium
    return alloc_medium_object(size, flags);
  } else {
    // Large
    return alloc_large_object(size, flags);
  }
}

uintptr_t ZObjectAllocator::alloc_object(size_t size) {
  ZAllocationFlags flags;
  return alloc_object(size, flags);
}

uintptr_t ZObjectAllocator::alloc_object_for_relocation(const ZPageTable* page_table, size_t size) {
  ZAllocationFlags flags;
  flags.set_non_blocking();

  const uintptr_t addr = alloc_object(size, flags);
  if (addr != 0) {
    register_alloc_for_relocation(page_table, addr, size);
  }

  return addr;
}

void ZObjectAllocator::undo_alloc_object_for_relocation(ZPage* page, uintptr_t addr, size_t size) {
  const uint8_t type = page->type();

  if (type == ZPageTypeLarge) {
    register_undo_alloc_for_relocation(page, size);
    undo_alloc_page(page);
    ZStatInc(ZCounterUndoObjectAllocationSucceeded);
  } else {
    if (page->undo_alloc_object_atomic(addr, size)) {
      register_undo_alloc_for_relocation(page, size);
      ZStatInc(ZCounterUndoObjectAllocationSucceeded);
    } else {
      ZStatInc(ZCounterUndoObjectAllocationFailed);
    }
  }
}

size_t ZObjectAllocator::used() const {
  size_t total_used = 0;
  size_t total_undone = 0;

  ZPerCPUConstIterator<size_t> iter_used(&_used);
  for (const size_t* cpu_used; iter_used.next(&cpu_used);) {
    total_used += *cpu_used;
  }

  ZPerCPUConstIterator<size_t> iter_undone(&_undone);
  for (const size_t* cpu_undone; iter_undone.next(&cpu_undone);) {
    total_undone += *cpu_undone;
  }

  return total_used - total_undone;
}

size_t ZObjectAllocator::remaining() const {
  assert(ZThread::is_java(), "Should be a Java thread");

  const ZPage* const page = Atomic::load_acquire(shared_small_page_addr());
  if (page != NULL) {
    return page->remaining();
  }

  return 0;
}

size_t ZObjectAllocator::relocated() const {
  size_t total_alloc = 0;
  size_t total_undo_alloc = 0;

  ZPerCPUConstIterator<size_t> iter_alloc(&_alloc_for_relocation);
  for (const size_t* alloc; iter_alloc.next(&alloc);) {
    total_alloc += Atomic::load(alloc);
  }

  ZPerCPUConstIterator<size_t> iter_undo_alloc(&_undo_alloc_for_relocation);
  for (const size_t* undo_alloc; iter_undo_alloc.next(&undo_alloc);) {
    total_undo_alloc += Atomic::load(undo_alloc);
  }

  assert(total_alloc >= total_undo_alloc, "Mismatch");

  return total_alloc - total_undo_alloc;
}

void ZObjectAllocator::retire_pages() {
  assert(SafepointSynchronize::is_at_safepoint(), "Should be at safepoint");

  // Reset used and undone bytes
  _used.set_all(0);
  _undone.set_all(0);

  // Reset relocated bytes
  _alloc_for_relocation.set_all(0);
  _undo_alloc_for_relocation.set_all(0);

  // Reset allocation pages
  _shared_medium_page.set(NULL);
  _shared_small_page.set_all(NULL);
}
