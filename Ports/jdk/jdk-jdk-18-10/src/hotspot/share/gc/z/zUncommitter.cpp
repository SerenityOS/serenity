/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zUncommitter.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"

static const ZStatCounter ZCounterUncommit("Memory", "Uncommit", ZStatUnitBytesPerSecond);

ZUncommitter::ZUncommitter(ZPageAllocator* page_allocator) :
    _page_allocator(page_allocator),
    _lock(),
    _stop(false) {
  set_name("ZUncommitter");
  create_and_start();
}

bool ZUncommitter::wait(uint64_t timeout) const {
  ZLocker<ZConditionLock> locker(&_lock);
  while (!ZUncommit && !_stop) {
    _lock.wait();
  }

  if (!_stop && timeout > 0) {
    log_debug(gc, heap)("Uncommit Timeout: " UINT64_FORMAT "s", timeout);
    _lock.wait(timeout * MILLIUNITS);
  }

  return !_stop;
}

bool ZUncommitter::should_continue() const {
  ZLocker<ZConditionLock> locker(&_lock);
  return !_stop;
}

void ZUncommitter::run_service() {
  uint64_t timeout = 0;

  while (wait(timeout)) {
    EventZUncommit event;
    size_t uncommitted = 0;

    while (should_continue()) {
      // Uncommit chunk
      const size_t flushed = _page_allocator->uncommit(&timeout);
      if (flushed == 0) {
        // Done
        break;
      }

      uncommitted += flushed;
    }

    if (uncommitted > 0) {
      // Update statistics
      ZStatInc(ZCounterUncommit, uncommitted);
      log_info(gc, heap)("Uncommitted: " SIZE_FORMAT "M(%.0f%%)",
                         uncommitted / M, percent_of(uncommitted, ZHeap::heap()->max_capacity()));

      // Send event
      event.commit(uncommitted);
    }
  }
}

void ZUncommitter::stop_service() {
  ZLocker<ZConditionLock> locker(&_lock);
  _stop = true;
  _lock.notify_all();
}
