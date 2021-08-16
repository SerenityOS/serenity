/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zMarkStack.inline.hpp"
#include "gc/z/zMarkStackAllocator.hpp"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "utilities/debug.hpp"

uintptr_t ZMarkStackSpaceStart;

ZMarkStackSpace::ZMarkStackSpace() :
    _expand_lock(),
    _start(0),
    _top(0),
    _end(0) {
  assert(ZMarkStackSpaceLimit >= ZMarkStackSpaceExpandSize, "ZMarkStackSpaceLimit too small");

  // Reserve address space
  const size_t size = ZMarkStackSpaceLimit;
  const uintptr_t addr = (uintptr_t)os::reserve_memory(size, !ExecMem, mtGC);
  if (addr == 0) {
    log_error_pd(gc, marking)("Failed to reserve address space for mark stacks");
    return;
  }

  // Successfully initialized
  _start = _top = _end = addr;

  // Register mark stack space start
  ZMarkStackSpaceStart = _start;

  // Prime space
  _end += expand_space();
}

bool ZMarkStackSpace::is_initialized() const {
  return _start != 0;
}

size_t ZMarkStackSpace::size() const {
  return _end - _start;
}

size_t ZMarkStackSpace::used() const {
  return _top - _start;
}

size_t ZMarkStackSpace::expand_space() {
  const size_t expand_size = ZMarkStackSpaceExpandSize;
  const size_t old_size = size();
  const size_t new_size = old_size + expand_size;

  if (new_size > ZMarkStackSpaceLimit) {
    // Expansion limit reached. This is a fatal error since we
    // currently can't recover from running out of mark stack space.
    fatal("Mark stack space exhausted. Use -XX:ZMarkStackSpaceLimit=<size> to increase the "
          "maximum number of bytes allocated for mark stacks. Current limit is " SIZE_FORMAT "M.",
          ZMarkStackSpaceLimit / M);
  }

  log_debug(gc, marking)("Expanding mark stack space: " SIZE_FORMAT "M->" SIZE_FORMAT "M",
                         old_size / M, new_size / M);

  // Expand
  os::commit_memory_or_exit((char*)_end, expand_size, false /* executable */, "Mark stack space");

  return expand_size;
}

size_t ZMarkStackSpace::shrink_space() {
  // Shrink to what is currently used
  const size_t old_size = size();
  const size_t new_size = align_up(used(), ZMarkStackSpaceExpandSize);
  const size_t shrink_size = old_size - new_size;

  if (shrink_size > 0) {
    // Shrink
    log_debug(gc, marking)("Shrinking mark stack space: " SIZE_FORMAT "M->" SIZE_FORMAT "M",
                           old_size / M, new_size / M);

    const uintptr_t shrink_start = _end - shrink_size;
    os::uncommit_memory((char*)shrink_start, shrink_size, false /* executable */);
  }

  return shrink_size;
}

uintptr_t ZMarkStackSpace::alloc_space(size_t size) {
  uintptr_t top = Atomic::load(&_top);

  for (;;) {
    const uintptr_t end = Atomic::load(&_end);
    const uintptr_t new_top = top + size;
    if (new_top > end) {
      // Not enough space left
      return 0;
    }

    const uintptr_t prev_top = Atomic::cmpxchg(&_top, top, new_top);
    if (prev_top == top) {
      // Success
      return top;
    }

    // Retry
    top = prev_top;
  }
}

uintptr_t ZMarkStackSpace::expand_and_alloc_space(size_t size) {
  ZLocker<ZLock> locker(&_expand_lock);

  // Retry allocation before expanding
  uintptr_t addr = alloc_space(size);
  if (addr != 0) {
    return addr;
  }

  // Expand
  const size_t expand_size = expand_space();

  // Increment top before end to make sure another
  // thread can't steal out newly expanded space.
  addr = Atomic::fetch_and_add(&_top, size);
  Atomic::add(&_end, expand_size);

  return addr;
}

uintptr_t ZMarkStackSpace::alloc(size_t size) {
  assert(size <= ZMarkStackSpaceExpandSize, "Invalid size");

  const uintptr_t addr = alloc_space(size);
  if (addr != 0) {
    return addr;
  }

  return expand_and_alloc_space(size);
}

void ZMarkStackSpace::free() {
  _end -= shrink_space();
  _top = _start;
}

ZMarkStackAllocator::ZMarkStackAllocator() :
    _freelist(),
    _space() {}

bool ZMarkStackAllocator::is_initialized() const {
  return _space.is_initialized();
}

size_t ZMarkStackAllocator::size() const {
  return _space.size();
}

ZMarkStackMagazine* ZMarkStackAllocator::create_magazine_from_space(uintptr_t addr, size_t size) {
  assert(is_aligned(size, ZMarkStackSize), "Invalid size");

  // Use first stack as magazine
  ZMarkStackMagazine* const magazine = new ((void*)addr) ZMarkStackMagazine();
  for (size_t i = ZMarkStackSize; i < size; i += ZMarkStackSize) {
    ZMarkStack* const stack = new ((void*)(addr + i)) ZMarkStack();
    const bool success = magazine->push(stack);
    assert(success, "Magazine should never get full");
  }

  return magazine;
}

ZMarkStackMagazine* ZMarkStackAllocator::alloc_magazine() {
  // Try allocating from the free list first
  ZMarkStackMagazine* const magazine = _freelist.pop();
  if (magazine != NULL) {
    return magazine;
  }

  // Allocate new magazine
  const uintptr_t addr = _space.alloc(ZMarkStackMagazineSize);
  if (addr == 0) {
    return NULL;
  }

  return create_magazine_from_space(addr, ZMarkStackMagazineSize);
}

void ZMarkStackAllocator::free_magazine(ZMarkStackMagazine* magazine) {
  _freelist.push(magazine);
}

void ZMarkStackAllocator::free() {
  _freelist.clear();
  _space.free();
}
