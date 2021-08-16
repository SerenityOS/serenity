/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/metaspace.hpp"
#include "memory/metaspaceUtils.hpp"
#include "unittest.hpp"

TEST_VM(MetaspaceUtils, reserved) {
  size_t reserved = MetaspaceUtils::reserved_bytes();
  EXPECT_GT(reserved, 0UL);

  size_t reserved_metadata = MetaspaceUtils::reserved_bytes(Metaspace::NonClassType);
  EXPECT_GT(reserved_metadata, 0UL);
  EXPECT_LE(reserved_metadata, reserved);
}

TEST_VM(MetaspaceUtils, reserved_compressed_class_pointers) {
  if (!UseCompressedClassPointers) {
    return;
  }
  size_t reserved = MetaspaceUtils::reserved_bytes();
  EXPECT_GT(reserved, 0UL);

  size_t reserved_class = MetaspaceUtils::reserved_bytes(Metaspace::ClassType);
  EXPECT_GT(reserved_class, 0UL);
  EXPECT_LE(reserved_class, reserved);
}

TEST_VM(MetaspaceUtils, committed) {
  size_t committed = MetaspaceUtils::committed_bytes();
  EXPECT_GT(committed, 0UL);

  size_t reserved  = MetaspaceUtils::reserved_bytes();
  EXPECT_LE(committed, reserved);

  size_t committed_metadata = MetaspaceUtils::committed_bytes(Metaspace::NonClassType);
  EXPECT_GT(committed_metadata, 0UL);
  EXPECT_LE(committed_metadata, committed);
}

TEST_VM(MetaspaceUtils, committed_compressed_class_pointers) {
  if (!UseCompressedClassPointers) {
    return;
  }
  size_t committed = MetaspaceUtils::committed_bytes();
  EXPECT_GT(committed, 0UL);

  size_t committed_class = MetaspaceUtils::committed_bytes(Metaspace::ClassType);
  EXPECT_GT(committed_class, 0UL);
  EXPECT_LE(committed_class, committed);
}

TEST_VM(MetaspaceUtils, non_compressed_class_pointers) {
  if (UseCompressedClassPointers) {
    return;
  }

  size_t committed_class = MetaspaceUtils::committed_bytes(Metaspace::ClassType);
  EXPECT_EQ(committed_class, 0UL);

  size_t used_class = MetaspaceUtils::used_bytes(Metaspace::ClassType);
  EXPECT_EQ(used_class, 0UL);

  size_t reserved_class = MetaspaceUtils::reserved_bytes(Metaspace::ClassType);
  EXPECT_EQ(reserved_class, 0UL);
}

static void check_metaspace_stats_are_consistent(const MetaspaceStats& stats) {
  EXPECT_LT(stats.committed(), stats.reserved());
  EXPECT_LT(stats.used(), stats.committed());
}

static void check_metaspace_stats_are_not_null(const MetaspaceStats& stats) {
  EXPECT_GT(stats.reserved(), 0UL);
  EXPECT_GT(stats.committed(), 0UL);
  EXPECT_GT(stats.used(), 0UL);
}

TEST_VM(MetaspaceUtils, get_statistics) {
  MetaspaceCombinedStats combined_stats = MetaspaceUtils::get_combined_statistics();
  check_metaspace_stats_are_not_null(combined_stats);
  check_metaspace_stats_are_consistent(combined_stats);
  check_metaspace_stats_are_not_null(combined_stats.non_class_space_stats());
  check_metaspace_stats_are_consistent(combined_stats.non_class_space_stats());

  if (UseCompressedClassPointers) {
    check_metaspace_stats_are_not_null(combined_stats.class_space_stats());
    check_metaspace_stats_are_consistent(combined_stats.class_space_stats());
  } else {
    // if we don't have a class space, combined stats should equal non-class stats
    EXPECT_EQ(combined_stats.non_class_space_stats().reserved(), combined_stats.reserved());
    EXPECT_EQ(combined_stats.non_class_space_stats().committed(), combined_stats.committed());
    EXPECT_EQ(combined_stats.non_class_space_stats().used(), combined_stats.used());
  }
}
