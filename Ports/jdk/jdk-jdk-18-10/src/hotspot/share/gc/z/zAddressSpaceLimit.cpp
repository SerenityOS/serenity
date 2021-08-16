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
#include "gc/z/zAddressSpaceLimit.hpp"
#include "gc/z/zGlobals.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/align.hpp"

static size_t address_space_limit() {
  size_t limit = 0;

  if (os::has_allocatable_memory_limit(&limit)) {
    return limit;
  }

  // No limit
  return SIZE_MAX;
}

size_t ZAddressSpaceLimit::mark_stack() {
  // Allow mark stacks to occupy 10% of the address space
  const size_t limit = address_space_limit() / 10;
  return align_up(limit, ZMarkStackSpaceExpandSize);
}

size_t ZAddressSpaceLimit::heap_view() {
  // Allow all heap views to occupy 50% of the address space
  const size_t limit = address_space_limit() / MaxVirtMemFraction / ZHeapViews;
  return align_up(limit, ZGranuleSize);
}
