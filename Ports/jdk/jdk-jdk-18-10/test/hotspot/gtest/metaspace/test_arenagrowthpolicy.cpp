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
#include "memory/metaspace.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"

using metaspace::ArenaGrowthPolicy;
using metaspace::chunklevel_t;
using namespace metaspace::chunklevel;

static void test_arena_growth_policy(Metaspace::MetaspaceType spacetype, bool is_class) {

  const ArenaGrowthPolicy* a =
      ArenaGrowthPolicy::policy_for_space_type((Metaspace::MetaspaceType)spacetype, is_class);

  // initial level
  chunklevel_t lvl = a->get_level_at_step(0);
  ASSERT_TRUE(is_valid_level(lvl));
  if (spacetype != Metaspace::BootMetaspaceType) {
    // All types save boot loader should start with small or very small chunks
    ASSERT_GE(lvl, CHUNK_LEVEL_4K);
  }

  for (int step = 1; step < 100; step++) {
    chunklevel_t lvl2 = a->get_level_at_step(step);
    ASSERT_TRUE(is_valid_level(lvl2));
    // limit steepness: no growth allowed beyond last chunksize * 2
    ASSERT_LE(word_size_for_level(lvl2), word_size_for_level(lvl) * 2);
    lvl = lvl2;
  }
}

#define DEFINE_GROWTH_POLICY_TEST(spacetype, is_class) \
TEST_VM(metaspace, arena_growth_policy_##spacetype##_##is_class) { \
  test_arena_growth_policy(Metaspace::spacetype, is_class); \
}

DEFINE_GROWTH_POLICY_TEST(ReflectionMetaspaceType, true)
DEFINE_GROWTH_POLICY_TEST(ReflectionMetaspaceType, false)
DEFINE_GROWTH_POLICY_TEST(ClassMirrorHolderMetaspaceType, true)
DEFINE_GROWTH_POLICY_TEST(ClassMirrorHolderMetaspaceType, false)
DEFINE_GROWTH_POLICY_TEST(StandardMetaspaceType, true)
DEFINE_GROWTH_POLICY_TEST(StandardMetaspaceType, false)
DEFINE_GROWTH_POLICY_TEST(BootMetaspaceType, true)
DEFINE_GROWTH_POLICY_TEST(BootMetaspaceType, false)

