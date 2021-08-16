/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/objectMonitor.hpp"
#include "runtime/vm_version.hpp"
#include "unittest.hpp"

TEST_VM(ObjectMonitor, sanity) {
 uint cache_line_size = VM_Version::L1_data_cache_line_size();

 if (cache_line_size != 0) {
   // We were able to determine the L1 data cache line size so
   // do some cache line specific sanity checks
   EXPECT_EQ((size_t) 0, sizeof (PaddedEnd<ObjectMonitor>) % cache_line_size)
        << "PaddedEnd<ObjectMonitor> size is not a "
        << "multiple of a cache line which permits false sharing. "
        << "sizeof(PaddedEnd<ObjectMonitor>) = "
        << sizeof (PaddedEnd<ObjectMonitor>)
        << "; cache_line_size = " << cache_line_size;

   EXPECT_GE((size_t) ObjectMonitor::owner_offset_in_bytes(), cache_line_size)
        << "the _header and _owner fields are closer "
        << "than a cache line which permits false sharing.";
  }
}
