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
#include "gc/z/zGlobals.hpp"
#include "gc/z/zList.inline.hpp"
#include "gc/z/zNUMA.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zPageCache.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zValue.inline.hpp"
#include "memory/allocation.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"

static const ZStatCounter ZCounterPageCacheHitL1("Memory", "Page Cache Hit L1", ZStatUnitOpsPerSecond);
static const ZStatCounter ZCounterPageCacheHitL2("Memory", "Page Cache Hit L2", ZStatUnitOpsPerSecond);
static const ZStatCounter ZCounterPageCacheHitL3("Memory", "Page Cache Hit L3", ZStatUnitOpsPerSecond);
static const ZStatCounter ZCounterPageCacheMiss("Memory", "Page Cache Miss", ZStatUnitOpsPerSecond);

class ZPageCacheFlushClosure : public StackObj {
  friend class ZPageCache;

protected:
  const size_t _requested;
  size_t       _flushed;

public:
  ZPageCacheFlushClosure(size_t requested);
  virtual bool do_page(const ZPage* page) = 0;
};

ZPageCacheFlushClosure::ZPageCacheFlushClosure(size_t requested) :
    _requested(requested),
    _flushed(0) {}

ZPageCache::ZPageCache() :
    _small(),
    _medium(),
    _large(),
    _last_commit(0) {}

ZPage* ZPageCache::alloc_small_page() {
  const uint32_t numa_id = ZNUMA::id();
  const uint32_t numa_count = ZNUMA::count();

  // Try NUMA local page cache
  ZPage* const l1_page = _small.get(numa_id).remove_first();
  if (l1_page != NULL) {
    ZStatInc(ZCounterPageCacheHitL1);
    return l1_page;
  }

  // Try NUMA remote page cache(s)
  uint32_t remote_numa_id = numa_id + 1;
  const uint32_t remote_numa_count = numa_count - 1;
  for (uint32_t i = 0; i < remote_numa_count; i++) {
    if (remote_numa_id == numa_count) {
      remote_numa_id = 0;
    }

    ZPage* const l2_page = _small.get(remote_numa_id).remove_first();
    if (l2_page != NULL) {
      ZStatInc(ZCounterPageCacheHitL2);
      return l2_page;
    }

    remote_numa_id++;
  }

  return NULL;
}

ZPage* ZPageCache::alloc_medium_page() {
  ZPage* const page = _medium.remove_first();
  if (page != NULL) {
    ZStatInc(ZCounterPageCacheHitL1);
    return page;
  }

  return NULL;
}

ZPage* ZPageCache::alloc_large_page(size_t size) {
  // Find a page with the right size
  ZListIterator<ZPage> iter(&_large);
  for (ZPage* page; iter.next(&page);) {
    if (size == page->size()) {
      // Page found
      _large.remove(page);
      ZStatInc(ZCounterPageCacheHitL1);
      return page;
    }
  }

  return NULL;
}

ZPage* ZPageCache::alloc_oversized_medium_page(size_t size) {
  if (size <= ZPageSizeMedium) {
    return _medium.remove_first();
  }

  return NULL;
}

ZPage* ZPageCache::alloc_oversized_large_page(size_t size) {
  // Find a page that is large enough
  ZListIterator<ZPage> iter(&_large);
  for (ZPage* page; iter.next(&page);) {
    if (size <= page->size()) {
      // Page found
      _large.remove(page);
      return page;
    }
  }

  return NULL;
}

ZPage* ZPageCache::alloc_oversized_page(size_t size) {
  ZPage* page = alloc_oversized_large_page(size);
  if (page == NULL) {
    page = alloc_oversized_medium_page(size);
  }

  if (page != NULL) {
    ZStatInc(ZCounterPageCacheHitL3);
  }

  return page;
}

ZPage* ZPageCache::alloc_page(uint8_t type, size_t size) {
  ZPage* page;

  // Try allocate exact page
  if (type == ZPageTypeSmall) {
    page = alloc_small_page();
  } else if (type == ZPageTypeMedium) {
    page = alloc_medium_page();
  } else {
    page = alloc_large_page(size);
  }

  if (page == NULL) {
    // Try allocate potentially oversized page
    ZPage* const oversized = alloc_oversized_page(size);
    if (oversized != NULL) {
      if (size < oversized->size()) {
        // Split oversized page
        page = oversized->split(type, size);

        // Cache remainder
        free_page(oversized);
      } else {
        // Re-type correctly sized page
        page = oversized->retype(type);
      }
    }
  }

  if (page == NULL) {
    ZStatInc(ZCounterPageCacheMiss);
  }

  return page;
}

void ZPageCache::free_page(ZPage* page) {
  const uint8_t type = page->type();
  if (type == ZPageTypeSmall) {
    _small.get(page->numa_id()).insert_first(page);
  } else if (type == ZPageTypeMedium) {
    _medium.insert_first(page);
  } else {
    _large.insert_first(page);
  }
}

bool ZPageCache::flush_list_inner(ZPageCacheFlushClosure* cl, ZList<ZPage>* from, ZList<ZPage>* to) {
  ZPage* const page = from->last();
  if (page == NULL || !cl->do_page(page)) {
    // Don't flush page
    return false;
  }

  // Flush page
  from->remove(page);
  to->insert_last(page);
  return true;
}

void ZPageCache::flush_list(ZPageCacheFlushClosure* cl, ZList<ZPage>* from, ZList<ZPage>* to) {
  while (flush_list_inner(cl, from, to));
}

void ZPageCache::flush_per_numa_lists(ZPageCacheFlushClosure* cl, ZPerNUMA<ZList<ZPage> >* from, ZList<ZPage>* to) {
  const uint32_t numa_count = ZNUMA::count();
  uint32_t numa_done = 0;
  uint32_t numa_next = 0;

  // Flush lists round-robin
  while (numa_done < numa_count) {
    ZList<ZPage>* numa_list = from->addr(numa_next);
    if (++numa_next == numa_count) {
      numa_next = 0;
    }

    if (flush_list_inner(cl, numa_list, to)) {
      // Not done
      numa_done = 0;
    } else {
      // Done
      numa_done++;
    }
  }
}

void ZPageCache::flush(ZPageCacheFlushClosure* cl, ZList<ZPage>* to) {
  // Prefer flushing large, then medium and last small pages
  flush_list(cl, &_large, to);
  flush_list(cl, &_medium, to);
  flush_per_numa_lists(cl, &_small, to);

  if (cl->_flushed > cl->_requested) {
    // Overflushed, re-insert part of last page into the cache
    const size_t overflushed = cl->_flushed - cl->_requested;
    ZPage* const reinsert = to->last()->split(overflushed);
    free_page(reinsert);
    cl->_flushed -= overflushed;
  }
}

class ZPageCacheFlushForAllocationClosure : public ZPageCacheFlushClosure {
public:
  ZPageCacheFlushForAllocationClosure(size_t requested) :
      ZPageCacheFlushClosure(requested) {}

  virtual bool do_page(const ZPage* page) {
    if (_flushed < _requested) {
      // Flush page
      _flushed += page->size();
      return true;
    }

    // Don't flush page
    return false;
  }
};

void ZPageCache::flush_for_allocation(size_t requested, ZList<ZPage>* to) {
  ZPageCacheFlushForAllocationClosure cl(requested);
  flush(&cl, to);
}

class ZPageCacheFlushForUncommitClosure : public ZPageCacheFlushClosure {
private:
  const uint64_t _now;
  uint64_t*      _timeout;

public:
  ZPageCacheFlushForUncommitClosure(size_t requested, uint64_t now, uint64_t* timeout) :
      ZPageCacheFlushClosure(requested),
      _now(now),
      _timeout(timeout) {
    // Set initial timeout
    *_timeout = ZUncommitDelay;
  }

  virtual bool do_page(const ZPage* page) {
    const uint64_t expires = page->last_used() + ZUncommitDelay;
    if (expires > _now) {
      // Don't flush page, record shortest non-expired timeout
      *_timeout = MIN2(*_timeout, expires - _now);
      return false;
    }

    if (_flushed >= _requested) {
      // Don't flush page, requested amount flushed
      return false;
    }

    // Flush page
    _flushed += page->size();
    return true;
  }
};

size_t ZPageCache::flush_for_uncommit(size_t requested, ZList<ZPage>* to, uint64_t* timeout) {
  const uint64_t now = os::elapsedTime();
  const uint64_t expires = _last_commit + ZUncommitDelay;
  if (expires > now) {
    // Delay uncommit, set next timeout
    *timeout = expires - now;
    return 0;
  }

  if (requested == 0) {
    // Nothing to flush, set next timeout
    *timeout = ZUncommitDelay;
    return 0;
  }

  ZPageCacheFlushForUncommitClosure cl(requested, now, timeout);
  flush(&cl, to);

  return cl._flushed;
}

void ZPageCache::set_last_commit() {
  _last_commit = ceil(os::elapsedTime());
}

void ZPageCache::pages_do(ZPageClosure* cl) const {
  // Small
  ZPerNUMAConstIterator<ZList<ZPage> > iter_numa(&_small);
  for (const ZList<ZPage>* list; iter_numa.next(&list);) {
    ZListIterator<ZPage> iter_small(list);
    for (ZPage* page; iter_small.next(&page);) {
      cl->do_page(page);
    }
  }

  // Medium
  ZListIterator<ZPage> iter_medium(&_medium);
  for (ZPage* page; iter_medium.next(&page);) {
    cl->do_page(page);
  }

  // Large
  ZListIterator<ZPage> iter_large(&_large);
  for (ZPage* page; iter_large.next(&page);) {
    cl->do_page(page);
  }
}
