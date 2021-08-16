/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zList.inline.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zPageAllocator.hpp"
#include "gc/z/zUnmapper.hpp"
#include "jfr/jfrEvents.hpp"
#include "runtime/globals.hpp"

ZUnmapper::ZUnmapper(ZPageAllocator* page_allocator) :
    _page_allocator(page_allocator),
    _lock(),
    _queue(),
    _stop(false) {
  set_name("ZUnmapper");
  create_and_start();
}

ZPage* ZUnmapper::dequeue() {
  ZLocker<ZConditionLock> locker(&_lock);

  for (;;) {
    if (_stop) {
      return NULL;
    }

    ZPage* const page = _queue.remove_first();
    if (page != NULL) {
      return page;
    }

    _lock.wait();
  }
}

void ZUnmapper::do_unmap_and_destroy_page(ZPage* page) const {
  EventZUnmap event;
  const size_t unmapped = page->size();

  // Unmap and destroy
  _page_allocator->unmap_page(page);
  _page_allocator->destroy_page(page);

  // Send event
  event.commit(unmapped);
}

void ZUnmapper::unmap_and_destroy_page(ZPage* page) {
  // Asynchronous unmap and destroy is not supported with ZVerifyViews
  if (ZVerifyViews) {
    // Immediately unmap and destroy
    do_unmap_and_destroy_page(page);
  } else {
    // Enqueue for asynchronous unmap and destroy
    ZLocker<ZConditionLock> locker(&_lock);
    _queue.insert_last(page);
    _lock.notify_all();
  }
}

void ZUnmapper::run_service() {
  for (;;) {
    ZPage* const page = dequeue();
    if (page == NULL) {
      // Stop
      return;
    }

    do_unmap_and_destroy_page(page);
  }
}

void ZUnmapper::stop_service() {
  ZLocker<ZConditionLock> locker(&_lock);
  _stop = true;
  _lock.notify_all();
}
