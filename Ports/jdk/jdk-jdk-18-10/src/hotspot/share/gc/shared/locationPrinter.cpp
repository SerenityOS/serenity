/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/locationPrinter.hpp"
#include "memory/universe.hpp"
#include "runtime/os.hpp"
#include "oops/klass.hpp"

bool LocationPrinter::is_valid_obj(void* obj) {
  if (!is_object_aligned(obj)) {
    return false;
  }
  if (obj < (void*)os::min_page_size()) {
    return false;
  }

  // We need at least the mark and the klass word in the committed region.
  if (!os::is_readable_range(obj, (HeapWord*)obj + oopDesc::header_size())) {
    return false;
  }
  if (!Universe::heap()->is_in(obj)) {
    return false;
  }

  Klass* k = (Klass*)oopDesc::load_klass_raw((oopDesc*)obj);
  return Klass::is_valid(k);
}
