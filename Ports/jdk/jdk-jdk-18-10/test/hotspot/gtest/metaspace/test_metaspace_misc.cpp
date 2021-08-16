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
#include "classfile/classLoaderData.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "utilities/powerOfTwo.hpp"
// #define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"

using metaspace::chunklevel_t;
using namespace metaspace::chunklevel;
using metaspace::Settings;

TEST_VM(metaspace, misc_sizes)   {

  // Test test common sizes (seems primitive but breaks surprisingly often during development
  //  because of word vs byte confusion)
  // Adjust this test if numbers change.
  ASSERT_TRUE(Settings::commit_granule_bytes() == 16 * K ||
              Settings::commit_granule_bytes() == 64 * K);
  ASSERT_EQ(Settings::commit_granule_bytes(), Metaspace::commit_alignment());
  ASSERT_TRUE(is_aligned(Settings::virtual_space_node_default_word_size(),
              metaspace::chunklevel::MAX_CHUNK_WORD_SIZE));
  ASSERT_EQ(Settings::virtual_space_node_default_word_size(),
            metaspace::chunklevel::MAX_CHUNK_WORD_SIZE * 2);
  ASSERT_EQ(Settings::virtual_space_node_reserve_alignment_words(),
            Metaspace::reserve_alignment_words());

}

TEST_VM(metaspace, misc_max_alloc_size)   {

  // Make sure we can allocate what we promise to allocate
  const size_t sz = Metaspace::max_allocation_word_size();
  ClassLoaderData* cld = ClassLoaderData::the_null_class_loader_data();
  MetaWord* p = cld->metaspace_non_null()->allocate(sz, Metaspace::NonClassType);
  ASSERT_NOT_NULL(p);
  cld->metaspace_non_null()->deallocate(p, sz, false);

}

TEST_VM(metaspace, chunklevel_utils)   {

  // These tests seem to be really basic, but it is amazing what one can
  // break accidentally...
  LOG(SIZE_FORMAT, MAX_CHUNK_BYTE_SIZE);
  LOG(SIZE_FORMAT, MIN_CHUNK_BYTE_SIZE);
  LOG(SIZE_FORMAT, MIN_CHUNK_WORD_SIZE);
  LOG(SIZE_FORMAT, MAX_CHUNK_WORD_SIZE);
  LOG(SIZE_FORMAT, MAX_CHUNK_BYTE_SIZE);
  LOG("%u", (unsigned)ROOT_CHUNK_LEVEL);
  LOG("%u", (unsigned)HIGHEST_CHUNK_LEVEL);
  LOG("%u", (unsigned)LOWEST_CHUNK_LEVEL);

  static const chunklevel_t INVALID_CHUNK_LEVEL    = (chunklevel_t) -1;

  EXPECT_TRUE(is_power_of_2(MAX_CHUNK_WORD_SIZE));
  EXPECT_TRUE(is_power_of_2(MIN_CHUNK_WORD_SIZE));

  EXPECT_TRUE(is_valid_level(LOWEST_CHUNK_LEVEL));
  EXPECT_TRUE(is_valid_level(HIGHEST_CHUNK_LEVEL));
  EXPECT_FALSE(is_valid_level(HIGHEST_CHUNK_LEVEL + 1));
  EXPECT_FALSE(is_valid_level(LOWEST_CHUNK_LEVEL - 1));

  EXPECT_EQ(word_size_for_level(ROOT_CHUNK_LEVEL), MAX_CHUNK_WORD_SIZE);
  EXPECT_EQ(word_size_for_level(HIGHEST_CHUNK_LEVEL), MIN_CHUNK_WORD_SIZE);

  EXPECT_EQ(word_size_for_level(CHUNK_LEVEL_4K), (4 * K) / BytesPerWord);
  EXPECT_EQ(word_size_for_level(CHUNK_LEVEL_64K), (64 * K) / BytesPerWord);

  EXPECT_EQ(level_fitting_word_size(0), HIGHEST_CHUNK_LEVEL);
  EXPECT_EQ(level_fitting_word_size(1), HIGHEST_CHUNK_LEVEL);
  EXPECT_EQ(level_fitting_word_size(MIN_CHUNK_WORD_SIZE), HIGHEST_CHUNK_LEVEL);
  EXPECT_EQ(level_fitting_word_size(MIN_CHUNK_WORD_SIZE + 1), HIGHEST_CHUNK_LEVEL - 1);

  EXPECT_EQ(level_fitting_word_size(MAX_CHUNK_WORD_SIZE), ROOT_CHUNK_LEVEL);
  EXPECT_EQ(level_fitting_word_size(MAX_CHUNK_WORD_SIZE - 1), ROOT_CHUNK_LEVEL);
  EXPECT_EQ(level_fitting_word_size((MAX_CHUNK_WORD_SIZE / 2) + 1), ROOT_CHUNK_LEVEL);
  EXPECT_EQ(level_fitting_word_size(MAX_CHUNK_WORD_SIZE / 2), ROOT_CHUNK_LEVEL + 1);

  EXPECT_EQ(level_fitting_word_size(8 * K), LP64_ONLY(CHUNK_LEVEL_64K) NOT_LP64(CHUNK_LEVEL_32K));
  EXPECT_EQ(level_fitting_word_size(8 * K + 13), LP64_ONLY(CHUNK_LEVEL_64K) NOT_LP64(CHUNK_LEVEL_32K) - 1);
  EXPECT_EQ(level_fitting_word_size(8 * K - 13), LP64_ONLY(CHUNK_LEVEL_64K) NOT_LP64(CHUNK_LEVEL_32K));

}

