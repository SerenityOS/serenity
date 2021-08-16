/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 SAP SE. All rights reserved.
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

#ifndef TESTUTILS_HPP
#define TESTUTILS_HPP

#include "memory/allStatic.hpp"
#include "utilities/globalDefinitions.hpp"
#include "unittest.hpp"

class GtestUtils : public AllStatic {
public:

  // Fill range with a byte mark.
  // Tolerates p == NULL or s == 0.
  static void mark_range_with(void* p, size_t s, uint8_t mark);

  // Given a memory range, check that the whole range is filled with the expected byte.
  // If not, hex dump around first non-matching address and return false.
  // If p == NULL or size == 0, returns true.
  static bool check_range(const void* p, size_t s, uint8_t expected);

  // Convenience method with a predefined byte mark.
  static void mark_range(void* p, size_t s)           { mark_range_with(p, s, 32); }
  static bool check_range(const void* p, size_t s)    { return check_range(p, s, 32); }

};

#define ASSERT_RANGE_IS_MARKED_WITH(p, size, mark)  ASSERT_TRUE(GtestUtils::check_range(p, size, mark))
#define ASSERT_RANGE_IS_MARKED(p, size)             ASSERT_TRUE(GtestUtils::check_range(p, size))

// Convenience asserts
#define ASSERT_NOT_NULL(p)  ASSERT_NE(p, (char*)NULL)
#define ASSERT_NULL(p)      ASSERT_EQ(p, (char*)NULL)

#define ASSERT_ALIGN(p, n) ASSERT_TRUE(is_aligned(p, n))

#endif // TESTUTILS_HPP
