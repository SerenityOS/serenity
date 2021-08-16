/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZRELOCATIONSETSELECTOR_INLINE_HPP
#define SHARE_GC_Z_ZRELOCATIONSETSELECTOR_INLINE_HPP

#include "gc/z/zRelocationSetSelector.hpp"

#include "gc/z/zArray.inline.hpp"
#include "gc/z/zPage.inline.hpp"

inline size_t ZRelocationSetSelectorGroupStats::npages() const {
  return _npages;
}

inline size_t ZRelocationSetSelectorGroupStats::total() const {
  return _total;
}

inline size_t ZRelocationSetSelectorGroupStats::live() const {
  return _live;
}

inline size_t ZRelocationSetSelectorGroupStats::empty() const {
  return _empty;
}

inline size_t ZRelocationSetSelectorGroupStats::relocate() const {
  return _relocate;
}

inline const ZRelocationSetSelectorGroupStats& ZRelocationSetSelectorStats::small() const {
  return _small;
}

inline const ZRelocationSetSelectorGroupStats& ZRelocationSetSelectorStats::medium() const {
  return _medium;
}

inline const ZRelocationSetSelectorGroupStats& ZRelocationSetSelectorStats::large() const {
  return _large;
}

inline void ZRelocationSetSelectorGroup::register_live_page(ZPage* page) {
  const uint8_t type = page->type();
  const size_t size = page->size();
  const size_t live = page->live_bytes();
  const size_t garbage = size - live;

  if (garbage > _fragmentation_limit) {
    _live_pages.append(page);
  }

  _stats._npages++;
  _stats._total += size;
  _stats._live += live;
}

inline void ZRelocationSetSelectorGroup::register_empty_page(ZPage* page) {
  const size_t size = page->size();

  _stats._npages++;
  _stats._total += size;
  _stats._empty += size;
}

inline const ZArray<ZPage*>* ZRelocationSetSelectorGroup::selected() const {
  return &_live_pages;
}

inline size_t ZRelocationSetSelectorGroup::forwarding_entries() const {
  return _forwarding_entries;
}

inline const ZRelocationSetSelectorGroupStats& ZRelocationSetSelectorGroup::stats() const {
  return _stats;
}

inline void ZRelocationSetSelector::register_live_page(ZPage* page) {
  const uint8_t type = page->type();

  if (type == ZPageTypeSmall) {
    _small.register_live_page(page);
  } else if (type == ZPageTypeMedium) {
    _medium.register_live_page(page);
  } else {
    _large.register_live_page(page);
  }
}

inline void ZRelocationSetSelector::register_empty_page(ZPage* page) {
  const uint8_t type = page->type();

  if (type == ZPageTypeSmall) {
    _small.register_empty_page(page);
  } else if (type == ZPageTypeMedium) {
    _medium.register_empty_page(page);
  } else {
    _large.register_empty_page(page);
  }

  _empty_pages.append(page);
}

inline bool ZRelocationSetSelector::should_free_empty_pages(int bulk) const {
  return _empty_pages.length() >= bulk && _empty_pages.is_nonempty();
}

inline const ZArray<ZPage*>* ZRelocationSetSelector::empty_pages() const {
  return &_empty_pages;
}

inline void ZRelocationSetSelector::clear_empty_pages() {
  return _empty_pages.clear();
}

inline size_t ZRelocationSetSelector::total() const {
  return _small.stats().total() + _medium.stats().total() + _large.stats().total();
}

inline size_t ZRelocationSetSelector::empty() const {
  return _small.stats().empty() + _medium.stats().empty() + _large.stats().empty();
}

inline size_t ZRelocationSetSelector::relocate() const {
  return _small.stats().relocate() + _medium.stats().relocate() + _large.stats().relocate();
}

inline const ZArray<ZPage*>* ZRelocationSetSelector::small() const {
  return _small.selected();
}

inline const ZArray<ZPage*>* ZRelocationSetSelector::medium() const {
  return _medium.selected();
}

inline size_t ZRelocationSetSelector::forwarding_entries() const {
  return _small.forwarding_entries() + _medium.forwarding_entries();
}

#endif // SHARE_GC_Z_ZRELOCATIONSETSELECTOR_INLINE_HPP
