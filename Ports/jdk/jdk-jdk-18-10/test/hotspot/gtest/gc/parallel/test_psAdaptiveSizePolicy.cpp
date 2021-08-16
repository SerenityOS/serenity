/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/psAdaptiveSizePolicy.hpp"
#include "utilities/macros.hpp"
#include "unittest.hpp"

#if INCLUDE_PARALLELGC

  TEST_VM(gc, oldFreeSpaceCalculation) {

    struct TestCase {
        size_t live;
        uintx ratio;
        size_t expectedResult;
    };

    TestCase test_cases[] = {
                                {100, 20, 25},
                                {100, 50, 100},
                                {100, 60, 150},
                                {100, 75, 300},
                                {400, 20, 100},
                                {400, 50, 400},
                                {400, 60, 600},
                                {400, 75, 1200},
                            };

    size_t array_len = sizeof(test_cases) / sizeof(TestCase);
    for (size_t i = 0; i < array_len; ++i) {
      ASSERT_EQ(PSAdaptiveSizePolicy::calculate_free_based_on_live(
          test_cases[i].live, test_cases[i].ratio),
          test_cases[i].expectedResult)
          << " Calculation of free memory failed"
          << " - Test case " << i << ": live = " << test_cases[i].live
          << "; ratio = " << test_cases[i].ratio;
    }
  }
#endif
