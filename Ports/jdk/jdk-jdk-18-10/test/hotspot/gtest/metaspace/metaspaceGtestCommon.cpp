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
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestRangeHelpers.hpp"
#include "runtime/os.hpp"

void zap_range(MetaWord* p, size_t word_size) {
  for (MetaWord* pzap = p; pzap < p + word_size; pzap += os::vm_page_size() / BytesPerWord) {
    *pzap = (MetaWord)NOT_LP64(0xFEFEFEFE) LP64_ONLY(0xFEFEFEFEEFEFEFEFULL);
  }
}

// Writes a unique pattern to p
void mark_address(MetaWord* p, uintx pattern) {
  MetaWord x = (MetaWord)((uintx) p ^ pattern);
  *p = x;
}

// checks pattern at address
void check_marked_address(const MetaWord* p, uintx pattern) {
  MetaWord x = (MetaWord)((uintx) p ^ pattern);
  EXPECT_EQ(*p, x);
}

// "fill_range_with_pattern" fills a range of heap words with pointers to itself.
//
// The idea is to fill a memory range with a pattern which is both marked clearly to the caller
// and cannot be moved without becoming invalid.
//
// The filled range can be checked with check_range_for_pattern. One also can only check
// a sub range of the original range.
void fill_range_with_pattern(MetaWord* p, size_t word_size, uintx pattern) {
  assert(word_size > 0 && p != NULL, "sanity");
  for (MetaWord* p2 = p; p2 < p + word_size; p2++) {
    mark_address(p2, pattern);
  }
}

void check_range_for_pattern(const MetaWord* p, size_t word_size, uintx pattern) {
  assert(p != NULL, "sanity");
  const MetaWord* p2 = p;
  while (p2 < p + word_size) {
    check_marked_address(p2, pattern);
    p2++;
  }
}

// Similar to fill_range_with_pattern, but only marks start and end. This is optimized for cases
// where fill_range_with_pattern just is too slow.
// Use check_marked_range to check the range. In contrast to check_range_for_pattern, only the original
// range can be checked.
void mark_range(MetaWord* p, size_t word_size, uintx pattern) {
  assert(word_size > 0 && p != NULL, "sanity");
  mark_address(p, pattern);
  mark_address(p + word_size - 1, pattern);
}

void check_marked_range(const MetaWord* p, size_t word_size, uintx pattern) {
  assert(word_size > 0 && p != NULL, "sanity");
  check_marked_address(p, pattern);
  check_marked_address(p + word_size - 1, pattern);
}

void mark_range(MetaWord* p, size_t word_size) {
  assert(word_size > 0 && p != NULL, "sanity");
  uintx pattern = (uintx)p2i(p);
  mark_range(p, word_size, pattern);
}

void check_marked_range(const MetaWord* p, size_t word_size) {
  uintx pattern = (uintx)p2i(p);
  check_marked_range(p, word_size, pattern);
}

