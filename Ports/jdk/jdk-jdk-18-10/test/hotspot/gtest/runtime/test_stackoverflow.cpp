/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/os.hpp"
#include "runtime/globals.hpp"
#include "runtime/stackOverflow.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "unittest.hpp"


TEST_VM(StackOverflow, basics) {
  StackOverflow so;

  // Make up a stack range. No need to allocate anything. Size has to be large enough
  //  to fit sum of all zones into them.
  address base = (address) 0x40000000;
  const size_t size = os::vm_page_size() * 100;
  address end = base - size;
  so.initialize(base, end);

  // Walking down the "stack" check for consistency of the three "in_stack_xxx" functions
  enum { normal_stack, reserved_or_yellow_zone, red_zone } where = normal_stack;
  for (address p = base - 1; p >= end; p -= os::vm_page_size()) {
    // tty->print_cr(PTR_FORMAT " %d %d %d", p2i(p),
    //     (int)so.in_stack_reserved_zone(p),
    //     (int)so.in_stack_yellow_reserved_zone(p),
    //     (int)so.in_stack_red_zone(p));
    switch (where) {
    case normal_stack:
      ASSERT_FALSE(so.in_stack_red_zone(p));
      if (so.in_stack_yellow_reserved_zone(p)) {
        if (StackReservedPages > 0) {
          ASSERT_TRUE(so.in_stack_reserved_zone(p));
        } else {
          ASSERT_FALSE(so.in_stack_reserved_zone(p));
        }
        where = reserved_or_yellow_zone;
      } else {
        ASSERT_FALSE(so.in_stack_reserved_zone((p)));
      }
      break;
    case reserved_or_yellow_zone:
      if (so.in_stack_red_zone(p)) {
        ASSERT_FALSE(so.in_stack_yellow_reserved_zone(p));
        where = red_zone;
      } else {
        ASSERT_TRUE(so.in_stack_yellow_reserved_zone(p));
      }
      break;
    case red_zone:
      ASSERT_TRUE(so.in_stack_red_zone(p));
      ASSERT_FALSE(so.in_stack_yellow_reserved_zone(p));
      ASSERT_FALSE(so.in_stack_reserved_zone((p)));
      break;
    }
  }
  ASSERT_EQ(where, red_zone);

  // Check bases.
  ASSERT_FALSE(so.in_stack_red_zone(so.stack_red_zone_base()));
  ASSERT_TRUE(so.in_stack_red_zone(so.stack_red_zone_base() - 1));
  ASSERT_TRUE(so.in_stack_yellow_reserved_zone(so.stack_red_zone_base()));
  ASSERT_FALSE(so.in_stack_reserved_zone(so.stack_reserved_zone_base()));
  if (so.stack_reserved_zone_size() > 0) {
    ASSERT_TRUE(so.in_stack_reserved_zone(so.stack_reserved_zone_base() - 1));
  }
}
