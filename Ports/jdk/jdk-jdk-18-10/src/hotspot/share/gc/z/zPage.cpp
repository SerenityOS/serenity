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
#include "gc/z/zList.inline.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zPhysicalMemory.inline.hpp"
#include "gc/z/zVirtualMemory.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

ZPage::ZPage(const ZVirtualMemory& vmem, const ZPhysicalMemory& pmem) :
    ZPage(type_from_size(vmem.size()), vmem, pmem) {}

ZPage::ZPage(uint8_t type, const ZVirtualMemory& vmem, const ZPhysicalMemory& pmem) :
    _type(type),
    _numa_id((uint8_t)-1),
    _seqnum(0),
    _virtual(vmem),
    _top(start()),
    _livemap(object_max_count()),
    _last_used(0),
    _physical(pmem),
    _node() {
  assert_initialized();
}

ZPage::~ZPage() {}

void ZPage::assert_initialized() const {
  assert(!_virtual.is_null(), "Should not be null");
  assert(!_physical.is_null(), "Should not be null");
  assert(_virtual.size() == _physical.size(), "Virtual/Physical size mismatch");
  assert((_type == ZPageTypeSmall && size() == ZPageSizeSmall) ||
         (_type == ZPageTypeMedium && size() == ZPageSizeMedium) ||
         (_type == ZPageTypeLarge && is_aligned(size(), ZGranuleSize)),
         "Page type/size mismatch");
}

void ZPage::reset() {
  _seqnum = ZGlobalSeqNum;
  _top = start();
  _livemap.reset();
  _last_used = 0;
}

void ZPage::reset_for_in_place_relocation() {
  _seqnum = ZGlobalSeqNum;
  _top = start();
}

ZPage* ZPage::retype(uint8_t type) {
  assert(_type != type, "Invalid retype");
  _type = type;
  _livemap.resize(object_max_count());
  return this;
}

ZPage* ZPage::split(size_t size) {
  return split(type_from_size(size), size);
}

ZPage* ZPage::split(uint8_t type, size_t size) {
  assert(_virtual.size() > size, "Invalid split");

  // Resize this page, keep _numa_id, _seqnum, and _last_used
  const ZVirtualMemory vmem = _virtual.split(size);
  const ZPhysicalMemory pmem = _physical.split(size);
  _type = type_from_size(_virtual.size());
  _top = start();
  _livemap.resize(object_max_count());

  // Create new page, inherit _seqnum and _last_used
  ZPage* const page = new ZPage(type, vmem, pmem);
  page->_seqnum = _seqnum;
  page->_last_used = _last_used;
  return page;
}

ZPage* ZPage::split_committed() {
  // Split any committed part of this page into a separate page,
  // leaving this page with only uncommitted physical memory.
  const ZPhysicalMemory pmem = _physical.split_committed();
  if (pmem.is_null()) {
    // Nothing committed
    return NULL;
  }

  assert(!_physical.is_null(), "Should not be null");

  // Resize this page
  const ZVirtualMemory vmem = _virtual.split(pmem.size());
  _type = type_from_size(_virtual.size());
  _top = start();
  _livemap.resize(object_max_count());

  // Create new page
  return new ZPage(vmem, pmem);
}

void ZPage::print_on(outputStream* out) const {
  out->print_cr(" %-6s  " PTR_FORMAT " " PTR_FORMAT " " PTR_FORMAT " %s%s",
                type_to_string(), start(), top(), end(),
                is_allocating()  ? " Allocating"  : "",
                is_relocatable() ? " Relocatable" : "");
}

void ZPage::print() const {
  print_on(tty);
}

void ZPage::verify_live(uint32_t live_objects, size_t live_bytes) const {
  guarantee(live_objects == _livemap.live_objects(), "Invalid number of live objects");
  guarantee(live_bytes == _livemap.live_bytes(), "Invalid number of live bytes");
}
