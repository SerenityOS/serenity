/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "memory/metaspace/internalStats.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"

// Very simple test, since the VM is fired up we should see a little
// Metaspace activity already which should show up in the stats.
TEST_VM(metaspace, internstats) {

  DEBUG_ONLY(ASSERT_GT(metaspace::InternalStats::num_allocs(), (uint64_t)0);)

  ASSERT_GT(metaspace::InternalStats::num_arena_births(), (uint64_t)0);
  ASSERT_GT(metaspace::InternalStats::num_vsnodes_births(), (uint64_t)0);
  ASSERT_GT(metaspace::InternalStats::num_space_committed(), (uint64_t)0);
  ASSERT_GT(metaspace::InternalStats::num_chunks_taken_from_freelist(), (uint64_t)0);

}

