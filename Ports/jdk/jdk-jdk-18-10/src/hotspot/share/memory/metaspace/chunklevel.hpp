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

#ifndef SHARE_MEMORY_METASPACE_CHUNKLEVEL_HPP
#define SHARE_MEMORY_METASPACE_CHUNKLEVEL_HPP

#include "utilities/globalDefinitions.hpp"

// Constants for the chunk levels and some utility functions.

class outputStream;

namespace metaspace {

// Chunks are managed by a binary buddy allocator.

// Chunk sizes range from 1K to 4MB (64bit).
//

// Each chunk has a level; the level corresponds to its position in the tree
// and describes its size.
//
// The largest chunks are called root chunks, of 4MB in size, and have level 0.
// From there on it goes:
//
// size    level
// 4MB     0
// 2MB     1
// 1MB     2
// 512K    3
// 256K    4
// 128K    5
// 64K     6
// 32K     7
// 16K     8
// 8K      9
// 4K      10
// 2K      11
// 1K      12

// Metachunk level (must be signed)
typedef signed char chunklevel_t;

#define CHKLVL_FORMAT "lv%.2d"

namespace chunklevel {

static const size_t   MAX_CHUNK_BYTE_SIZE    = 4 * M;
static const int      NUM_CHUNK_LEVELS       = 13;
static const size_t   MIN_CHUNK_BYTE_SIZE    = (MAX_CHUNK_BYTE_SIZE >> ((size_t)NUM_CHUNK_LEVELS - 1));

static const size_t   MIN_CHUNK_WORD_SIZE    = MIN_CHUNK_BYTE_SIZE / sizeof(MetaWord);
static const size_t   MAX_CHUNK_WORD_SIZE    = MAX_CHUNK_BYTE_SIZE / sizeof(MetaWord);

static const chunklevel_t ROOT_CHUNK_LEVEL       = 0;

static const chunklevel_t HIGHEST_CHUNK_LEVEL    = NUM_CHUNK_LEVELS - 1;
static const chunklevel_t LOWEST_CHUNK_LEVEL     = 0;

static const chunklevel_t INVALID_CHUNK_LEVEL    = (chunklevel_t) -1;

inline bool is_valid_level(chunklevel_t level) {
  return level >= LOWEST_CHUNK_LEVEL &&
         level <= HIGHEST_CHUNK_LEVEL;
}

inline void check_valid_level(chunklevel_t lvl) {
  assert(is_valid_level(lvl), "invalid level (%d)", (int)lvl);
}

// Given a level return the chunk size, in words.
inline size_t word_size_for_level(chunklevel_t level) {
  return (MAX_CHUNK_BYTE_SIZE >> level) / BytesPerWord;
}

// Given an arbitrary word size smaller than the highest chunk size,
// return the highest chunk level able to hold this size.
// Returns INVALID_CHUNK_LEVEL if no fitting level can be found.
chunklevel_t level_fitting_word_size(size_t word_size);

// Shorthands to refer to exact sizes
static const chunklevel_t CHUNK_LEVEL_4M =     ROOT_CHUNK_LEVEL;
static const chunklevel_t CHUNK_LEVEL_2M =    (ROOT_CHUNK_LEVEL + 1);
static const chunklevel_t CHUNK_LEVEL_1M =    (ROOT_CHUNK_LEVEL + 2);
static const chunklevel_t CHUNK_LEVEL_512K =  (ROOT_CHUNK_LEVEL + 3);
static const chunklevel_t CHUNK_LEVEL_256K =  (ROOT_CHUNK_LEVEL + 4);
static const chunklevel_t CHUNK_LEVEL_128K =  (ROOT_CHUNK_LEVEL + 5);
static const chunklevel_t CHUNK_LEVEL_64K =   (ROOT_CHUNK_LEVEL + 6);
static const chunklevel_t CHUNK_LEVEL_32K =   (ROOT_CHUNK_LEVEL + 7);
static const chunklevel_t CHUNK_LEVEL_16K =   (ROOT_CHUNK_LEVEL + 8);
static const chunklevel_t CHUNK_LEVEL_8K =    (ROOT_CHUNK_LEVEL + 9);
static const chunklevel_t CHUNK_LEVEL_4K =    (ROOT_CHUNK_LEVEL + 10);
static const chunklevel_t CHUNK_LEVEL_2K =    (ROOT_CHUNK_LEVEL + 11);
static const chunklevel_t CHUNK_LEVEL_1K =    (ROOT_CHUNK_LEVEL + 12);

STATIC_ASSERT(CHUNK_LEVEL_1K == HIGHEST_CHUNK_LEVEL);
STATIC_ASSERT(CHUNK_LEVEL_4M == LOWEST_CHUNK_LEVEL);
STATIC_ASSERT(ROOT_CHUNK_LEVEL == LOWEST_CHUNK_LEVEL);

/////////////////////////////////////////////////////////
// print helpers
void print_chunk_size(outputStream* st, chunklevel_t lvl);

} // namespace chunklevel

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_CHUNKLEVEL_HPP
