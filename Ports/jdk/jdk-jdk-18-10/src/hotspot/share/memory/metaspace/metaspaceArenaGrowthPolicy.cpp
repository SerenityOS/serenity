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
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

// hard-coded chunk allocation sequences for various space types
//  (Note: when modifying this, don't add jumps of more than double the
//   last chunk size. There is a gtest testing this, see test_arenagrowthpolicy.cpp)

static const chunklevel_t g_sequ_standard_non_class[] = {
    chunklevel::CHUNK_LEVEL_4K,
    chunklevel::CHUNK_LEVEL_4K,
    chunklevel::CHUNK_LEVEL_4K,
    chunklevel::CHUNK_LEVEL_8K,
    chunklevel::CHUNK_LEVEL_16K
    // .. repeat last
};

static const chunklevel_t g_sequ_standard_class[] = {
    chunklevel::CHUNK_LEVEL_2K,
    chunklevel::CHUNK_LEVEL_2K,
    chunklevel::CHUNK_LEVEL_4K,
    chunklevel::CHUNK_LEVEL_8K,
    chunklevel::CHUNK_LEVEL_16K
    // .. repeat last
};

static const chunklevel_t g_sequ_anon_non_class[] = {
   chunklevel::CHUNK_LEVEL_1K,
   // .. repeat last
};

static const chunklevel_t g_sequ_anon_class[] = {
    chunklevel::CHUNK_LEVEL_1K,
    // .. repeat last
};

static const chunklevel_t g_sequ_refl_non_class[] = {
    chunklevel::CHUNK_LEVEL_2K,
    chunklevel::CHUNK_LEVEL_1K
    // .. repeat last
};

static const chunklevel_t g_sequ_refl_class[] = {
    chunklevel::CHUNK_LEVEL_1K,
    // .. repeat last
};

// Boot class loader: give it large chunks: beyond commit granule size
// (typically 64K) the costs for large chunks largely diminishes since
// they are committed on the fly.
static const chunklevel_t g_sequ_boot_non_class[] = {
    chunklevel::CHUNK_LEVEL_4M,
    chunklevel::CHUNK_LEVEL_1M
    // .. repeat last
};

static const chunklevel_t g_sequ_boot_class[] = {
    chunklevel::CHUNK_LEVEL_256K
    // .. repeat last
};

const ArenaGrowthPolicy* ArenaGrowthPolicy::policy_for_space_type(Metaspace::MetaspaceType space_type, bool is_class) {

#define DEFINE_CLASS_FOR_ARRAY(what) \
  static ArenaGrowthPolicy chunk_alloc_sequence_##what (g_sequ_##what, sizeof(g_sequ_##what)/sizeof(chunklevel_t));

  DEFINE_CLASS_FOR_ARRAY(standard_non_class)
  DEFINE_CLASS_FOR_ARRAY(standard_class)
  DEFINE_CLASS_FOR_ARRAY(anon_non_class)
  DEFINE_CLASS_FOR_ARRAY(anon_class)
  DEFINE_CLASS_FOR_ARRAY(refl_non_class)
  DEFINE_CLASS_FOR_ARRAY(refl_class)
  DEFINE_CLASS_FOR_ARRAY(boot_non_class)
  DEFINE_CLASS_FOR_ARRAY(boot_class)

  if (is_class) {
    switch(space_type) {
    case Metaspace::StandardMetaspaceType:          return &chunk_alloc_sequence_standard_class;
    case Metaspace::ReflectionMetaspaceType:        return &chunk_alloc_sequence_refl_class;
    case Metaspace::ClassMirrorHolderMetaspaceType: return &chunk_alloc_sequence_anon_class;
    case Metaspace::BootMetaspaceType:              return &chunk_alloc_sequence_boot_class;
    default: ShouldNotReachHere();
    }
  } else {
    switch(space_type) {
    case Metaspace::StandardMetaspaceType:          return &chunk_alloc_sequence_standard_non_class;
    case Metaspace::ReflectionMetaspaceType:        return &chunk_alloc_sequence_refl_non_class;
    case Metaspace::ClassMirrorHolderMetaspaceType: return &chunk_alloc_sequence_anon_non_class;
    case Metaspace::BootMetaspaceType:              return &chunk_alloc_sequence_boot_non_class;
    default: ShouldNotReachHere();
    }
  }

  return NULL;

}

} // namespace

