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
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zForwarding.inline.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zUtils.inline.hpp"
#include "utilities/align.hpp"

//
// Reference count states:
//
// * If the reference count is zero, it will never change again.
//
// * If the reference count is positive, it can be both retained
//   (increased) and released (decreased).
//
// * If the reference count is negative, is can only be released
//   (increased). A negative reference count means that one or more
//   threads are waiting for one or more other threads to release
//   their references.
//
// The reference lock is used for waiting until the reference
// count has become zero (released) or negative one (claimed).
//

static const ZStatCriticalPhase ZCriticalPhaseRelocationStall("Relocation Stall");

bool ZForwarding::retain_page() {
  for (;;) {
    const int32_t ref_count = Atomic::load_acquire(&_ref_count);

    if (ref_count == 0) {
      // Released
      return false;
    }

    if (ref_count < 0) {
      // Claimed
      const bool success = wait_page_released();
      assert(success, "Should always succeed");
      return false;
    }

    if (Atomic::cmpxchg(&_ref_count, ref_count, ref_count + 1) == ref_count) {
      // Retained
      return true;
    }
  }
}

ZPage* ZForwarding::claim_page() {
  for (;;) {
    const int32_t ref_count = Atomic::load(&_ref_count);
    assert(ref_count > 0, "Invalid state");

    // Invert reference count
    if (Atomic::cmpxchg(&_ref_count, ref_count, -ref_count) != ref_count) {
      continue;
    }

    // If the previous reference count was 1, then we just changed it to -1,
    // and we have now claimed the page. Otherwise we wait until it is claimed.
    if (ref_count != 1) {
      ZLocker<ZConditionLock> locker(&_ref_lock);
      while (Atomic::load_acquire(&_ref_count) != -1) {
        _ref_lock.wait();
      }
    }

    return _page;
  }
}

void ZForwarding::release_page() {
  for (;;) {
    const int32_t ref_count = Atomic::load(&_ref_count);
    assert(ref_count != 0, "Invalid state");

    if (ref_count > 0) {
      // Decrement reference count
      if (Atomic::cmpxchg(&_ref_count, ref_count, ref_count - 1) != ref_count) {
        continue;
      }

      // If the previous reference count was 1, then we just decremented
      // it to 0 and we should signal that the page is now released.
      if (ref_count == 1) {
        // Notify released
        ZLocker<ZConditionLock> locker(&_ref_lock);
        _ref_lock.notify_all();
      }
    } else {
      // Increment reference count
      if (Atomic::cmpxchg(&_ref_count, ref_count, ref_count + 1) != ref_count) {
        continue;
      }

      // If the previous reference count was -2 or -1, then we just incremented it
      // to -1 or 0, and we should signal the that page is now claimed or released.
      if (ref_count == -2 || ref_count == -1) {
        // Notify claimed or released
        ZLocker<ZConditionLock> locker(&_ref_lock);
        _ref_lock.notify_all();
      }
    }

    return;
  }
}

bool ZForwarding::wait_page_released() const {
  if (Atomic::load_acquire(&_ref_count) != 0) {
    ZStatTimer timer(ZCriticalPhaseRelocationStall);
    ZLocker<ZConditionLock> locker(&_ref_lock);
    while (Atomic::load_acquire(&_ref_count) != 0) {
      if (_ref_abort) {
        return false;
      }

      _ref_lock.wait();
    }
  }

  return true;
}

ZPage* ZForwarding::detach_page() {
  // Wait until released
  if (Atomic::load_acquire(&_ref_count) != 0) {
    ZLocker<ZConditionLock> locker(&_ref_lock);
    while (Atomic::load_acquire(&_ref_count) != 0) {
      _ref_lock.wait();
    }
  }

  // Detach and return page
  ZPage* const page = _page;
  _page = NULL;
  return page;
}

void ZForwarding::abort_page() {
  ZLocker<ZConditionLock> locker(&_ref_lock);
  assert(Atomic::load(&_ref_count) > 0, "Invalid state");
  assert(!_ref_abort, "Invalid state");
  _ref_abort = true;
  _ref_lock.notify_all();
}

void ZForwarding::verify() const {
  guarantee(_ref_count != 0, "Invalid reference count");
  guarantee(_page != NULL, "Invalid page");

  uint32_t live_objects = 0;
  size_t live_bytes = 0;

  for (ZForwardingCursor i = 0; i < _entries.length(); i++) {
    const ZForwardingEntry entry = at(&i);
    if (!entry.populated()) {
      // Skip empty entries
      continue;
    }

    // Check from index
    guarantee(entry.from_index() < _page->object_max_count(), "Invalid from index");

    // Check for duplicates
    for (ZForwardingCursor j = i + 1; j < _entries.length(); j++) {
      const ZForwardingEntry other = at(&j);
      if (!other.populated()) {
        // Skip empty entries
        continue;
      }

      guarantee(entry.from_index() != other.from_index(), "Duplicate from");
      guarantee(entry.to_offset() != other.to_offset(), "Duplicate to");
    }

    const uintptr_t to_addr = ZAddress::good(entry.to_offset());
    const size_t size = ZUtils::object_size(to_addr);
    const size_t aligned_size = align_up(size, _page->object_alignment());
    live_bytes += aligned_size;
    live_objects++;
  }

  // Verify number of live objects and bytes
  _page->verify_live(live_objects, live_bytes);
}
