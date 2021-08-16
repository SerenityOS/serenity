/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zGranuleMap.inline.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zPageTable.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/debug.hpp"

ZPageTable::ZPageTable() :
    _map(ZAddressOffsetMax) {}

void ZPageTable::insert(ZPage* page) {
  const uintptr_t offset = page->start();
  const size_t size = page->size();

  // Make sure a newly created page is
  // visible before updating the page table.
  OrderAccess::storestore();

  assert(_map.get(offset) == NULL, "Invalid entry");
  _map.put(offset, size, page);
}

void ZPageTable::remove(ZPage* page) {
  const uintptr_t offset = page->start();
  const size_t size = page->size();

  assert(_map.get(offset) == page, "Invalid entry");
  _map.put(offset, size, NULL);
}
