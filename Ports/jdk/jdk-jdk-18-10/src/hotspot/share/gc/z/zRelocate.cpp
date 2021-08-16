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
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zAbort.inline.hpp"
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zBarrier.inline.hpp"
#include "gc/z/zForwarding.inline.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zRelocate.hpp"
#include "gc/z/zRelocationSet.inline.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zTask.hpp"
#include "gc/z/zThread.inline.hpp"
#include "gc/z/zWorkers.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"

ZRelocate::ZRelocate(ZWorkers* workers) :
    _workers(workers) {}

static uintptr_t forwarding_index(ZForwarding* forwarding, uintptr_t from_addr) {
  const uintptr_t from_offset = ZAddress::offset(from_addr);
  return (from_offset - forwarding->start()) >> forwarding->object_alignment_shift();
}

static uintptr_t forwarding_find(ZForwarding* forwarding, uintptr_t from_addr, ZForwardingCursor* cursor) {
  const uintptr_t from_index = forwarding_index(forwarding, from_addr);
  const ZForwardingEntry entry = forwarding->find(from_index, cursor);
  return entry.populated() ? ZAddress::good(entry.to_offset()) : 0;
}

static uintptr_t forwarding_insert(ZForwarding* forwarding, uintptr_t from_addr, uintptr_t to_addr, ZForwardingCursor* cursor) {
  const uintptr_t from_index = forwarding_index(forwarding, from_addr);
  const uintptr_t to_offset = ZAddress::offset(to_addr);
  const uintptr_t to_offset_final = forwarding->insert(from_index, to_offset, cursor);
  return ZAddress::good(to_offset_final);
}

static uintptr_t relocate_object_inner(ZForwarding* forwarding, uintptr_t from_addr, ZForwardingCursor* cursor) {
  assert(ZHeap::heap()->is_object_live(from_addr), "Should be live");

  // Allocate object
  const size_t size = ZUtils::object_size(from_addr);
  const uintptr_t to_addr = ZHeap::heap()->alloc_object_for_relocation(size);
  if (to_addr == 0) {
    // Allocation failed
    return 0;
  }

  // Copy object
  ZUtils::object_copy_disjoint(from_addr, to_addr, size);

  // Insert forwarding
  const uintptr_t to_addr_final = forwarding_insert(forwarding, from_addr, to_addr, cursor);
  if (to_addr_final != to_addr) {
    // Already relocated, try undo allocation
    ZHeap::heap()->undo_alloc_object_for_relocation(to_addr, size);
  }

  return to_addr_final;
}

uintptr_t ZRelocate::relocate_object(ZForwarding* forwarding, uintptr_t from_addr) const {
  ZForwardingCursor cursor;

  // Lookup forwarding
  uintptr_t to_addr = forwarding_find(forwarding, from_addr, &cursor);
  if (to_addr != 0) {
    // Already relocated
    return to_addr;
  }

  // Relocate object
  if (forwarding->retain_page()) {
    to_addr = relocate_object_inner(forwarding, from_addr, &cursor);
    forwarding->release_page();

    if (to_addr != 0) {
      // Success
      return to_addr;
    }

    // Failed to relocate object. Wait for a worker thread to complete
    // relocation of this page, and then forward the object. If the GC
    // aborts the relocation phase before the page has been relocated,
    // then wait return false and we just forward the object in-place.
    if (!forwarding->wait_page_released()) {
      // Forward object in-place
      return forwarding_insert(forwarding, from_addr, from_addr, &cursor);
    }
  }

  // Forward object
  return forward_object(forwarding, from_addr);
}

uintptr_t ZRelocate::forward_object(ZForwarding* forwarding, uintptr_t from_addr) const {
  ZForwardingCursor cursor;
  const uintptr_t to_addr = forwarding_find(forwarding, from_addr, &cursor);
  assert(to_addr != 0, "Should be forwarded");
  return to_addr;
}

static ZPage* alloc_page(const ZForwarding* forwarding) {
  if (ZStressRelocateInPlace) {
    // Simulate failure to allocate a new page. This will
    // cause the page being relocated to be relocated in-place.
    return NULL;
  }

  ZAllocationFlags flags;
  flags.set_non_blocking();
  flags.set_worker_relocation();
  return ZHeap::heap()->alloc_page(forwarding->type(), forwarding->size(), flags);
}

static void free_page(ZPage* page) {
  ZHeap::heap()->free_page(page, true /* reclaimed */);
}

static bool should_free_target_page(ZPage* page) {
  // Free target page if it is empty. We can end up with an empty target
  // page if we allocated a new target page, and then lost the race to
  // relocate the remaining objects, leaving the target page empty when
  // relocation completed.
  return page != NULL && page->top() == page->start();
}

class ZRelocateSmallAllocator {
private:
  volatile size_t _in_place_count;

public:
  ZRelocateSmallAllocator() :
      _in_place_count(0) {}

  ZPage* alloc_target_page(ZForwarding* forwarding, ZPage* target) {
    ZPage* const page = alloc_page(forwarding);
    if (page == NULL) {
      Atomic::inc(&_in_place_count);
    }

    return page;
  }

  void share_target_page(ZPage* page) {
    // Does nothing
  }

  void free_target_page(ZPage* page) {
    if (should_free_target_page(page)) {
      free_page(page);
    }
  }

  void free_relocated_page(ZPage* page) {
    free_page(page);
  }

  uintptr_t alloc_object(ZPage* page, size_t size) const {
    return (page != NULL) ? page->alloc_object(size) : 0;
  }

  void undo_alloc_object(ZPage* page, uintptr_t addr, size_t size) const {
    page->undo_alloc_object(addr, size);
  }

  const size_t in_place_count() const {
    return _in_place_count;
  }
};

class ZRelocateMediumAllocator {
private:
  ZConditionLock      _lock;
  ZPage*              _shared;
  bool                _in_place;
  volatile size_t     _in_place_count;

public:
  ZRelocateMediumAllocator() :
      _lock(),
      _shared(NULL),
      _in_place(false),
      _in_place_count(0) {}

  ~ZRelocateMediumAllocator() {
    if (should_free_target_page(_shared)) {
      free_page(_shared);
    }
  }

  ZPage* alloc_target_page(ZForwarding* forwarding, ZPage* target) {
    ZLocker<ZConditionLock> locker(&_lock);

    // Wait for any ongoing in-place relocation to complete
    while (_in_place) {
      _lock.wait();
    }

    // Allocate a new page only if the shared page is the same as the
    // current target page. The shared page will be different from the
    // current target page if another thread shared a page, or allocated
    // a new page.
    if (_shared == target) {
      _shared = alloc_page(forwarding);
      if (_shared == NULL) {
        Atomic::inc(&_in_place_count);
        _in_place = true;
      }
    }

    return _shared;
  }

  void share_target_page(ZPage* page) {
    ZLocker<ZConditionLock> locker(&_lock);

    assert(_in_place, "Invalid state");
    assert(_shared == NULL, "Invalid state");
    assert(page != NULL, "Invalid page");

    _shared = page;
    _in_place = false;

    _lock.notify_all();
  }

  void free_target_page(ZPage* page) {
    // Does nothing
  }

  void free_relocated_page(ZPage* page) {
    free_page(page);
  }

  uintptr_t alloc_object(ZPage* page, size_t size) const {
    return (page != NULL) ? page->alloc_object_atomic(size) : 0;
  }

  void undo_alloc_object(ZPage* page, uintptr_t addr, size_t size) const {
    page->undo_alloc_object_atomic(addr, size);
  }

  const size_t in_place_count() const {
    return _in_place_count;
  }
};

template <typename Allocator>
class ZRelocateClosure : public ObjectClosure {
private:
  Allocator* const _allocator;
  ZForwarding*     _forwarding;
  ZPage*           _target;

  bool relocate_object(uintptr_t from_addr) const {
    ZForwardingCursor cursor;

    // Lookup forwarding
    if (forwarding_find(_forwarding, from_addr, &cursor) != 0) {
      // Already relocated
      return true;
    }

    // Allocate object
    const size_t size = ZUtils::object_size(from_addr);
    const uintptr_t to_addr = _allocator->alloc_object(_target, size);
    if (to_addr == 0) {
      // Allocation failed
      return false;
    }

    // Copy object. Use conjoint copying if we are relocating
    // in-place and the new object overlapps with the old object.
    if (_forwarding->in_place() && to_addr + size > from_addr) {
      ZUtils::object_copy_conjoint(from_addr, to_addr, size);
    } else {
      ZUtils::object_copy_disjoint(from_addr, to_addr, size);
    }

    // Insert forwarding
    if (forwarding_insert(_forwarding, from_addr, to_addr, &cursor) != to_addr) {
      // Already relocated, undo allocation
      _allocator->undo_alloc_object(_target, to_addr, size);
    }

    return true;
  }

  virtual void do_object(oop obj) {
    const uintptr_t addr = ZOop::to_address(obj);
    assert(ZHeap::heap()->is_object_live(addr), "Should be live");

    while (!relocate_object(addr)) {
      // Allocate a new target page, or if that fails, use the page being
      // relocated as the new target, which will cause it to be relocated
      // in-place.
      _target = _allocator->alloc_target_page(_forwarding, _target);
      if (_target != NULL) {
        continue;
      }

      // Claim the page being relocated to block other threads from accessing
      // it, or its forwarding table, until it has been released (relocation
      // completed).
      _target = _forwarding->claim_page();
      _target->reset_for_in_place_relocation();
      _forwarding->set_in_place();
    }
  }

public:
  ZRelocateClosure(Allocator* allocator) :
      _allocator(allocator),
      _forwarding(NULL),
      _target(NULL) {}

  ~ZRelocateClosure() {
    _allocator->free_target_page(_target);
  }

  void do_forwarding(ZForwarding* forwarding) {
    _forwarding = forwarding;

    // Check if we should abort
    if (ZAbort::should_abort()) {
      _forwarding->abort_page();
      return;
    }

    // Relocate objects
    _forwarding->object_iterate(this);

    // Verify
    if (ZVerifyForwarding) {
      _forwarding->verify();
    }

    // Release relocated page
    _forwarding->release_page();

    if (_forwarding->in_place()) {
      // The relocated page has been relocated in-place and should not
      // be freed. Keep it as target page until it is full, and offer to
      // share it with other worker threads.
      _allocator->share_target_page(_target);
    } else {
      // Detach and free relocated page
      ZPage* const page = _forwarding->detach_page();
      _allocator->free_relocated_page(page);
    }
  }
};

class ZRelocateTask : public ZTask {
private:
  ZRelocationSetParallelIterator _iter;
  ZRelocateSmallAllocator        _small_allocator;
  ZRelocateMediumAllocator       _medium_allocator;

  static bool is_small(ZForwarding* forwarding) {
    return forwarding->type() == ZPageTypeSmall;
  }

public:
  ZRelocateTask(ZRelocationSet* relocation_set) :
      ZTask("ZRelocateTask"),
      _iter(relocation_set),
      _small_allocator(),
      _medium_allocator() {}

  ~ZRelocateTask() {
    ZStatRelocation::set_at_relocate_end(_small_allocator.in_place_count(),
                                         _medium_allocator.in_place_count());
  }

  virtual void work() {
    ZRelocateClosure<ZRelocateSmallAllocator> small(&_small_allocator);
    ZRelocateClosure<ZRelocateMediumAllocator> medium(&_medium_allocator);

    for (ZForwarding* forwarding; _iter.next(&forwarding);) {
      if (is_small(forwarding)) {
        small.do_forwarding(forwarding);
      } else {
        medium.do_forwarding(forwarding);
      }
    }
  }
};

void ZRelocate::relocate(ZRelocationSet* relocation_set) {
  ZRelocateTask task(relocation_set);
  _workers->run(&task);
}
