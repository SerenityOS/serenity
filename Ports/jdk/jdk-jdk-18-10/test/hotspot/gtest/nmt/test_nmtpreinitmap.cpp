/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "jvm_io.h"
#include "memory/allocation.hpp"
#include "runtime/os.hpp"
#include "services/nmtPreInit.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "unittest.hpp"

#if INCLUDE_NMT

// This tests the NMTPreInitAllocationTable hash table used to store C-heap allocations before NMT initialization ran.

static size_t small_random_nonzero_size() {
  // We keep the sizes random but not too random; the more regular the sizes, the
  // more regular the malloc return pointers and the better we see how our hash
  // function copes in the NMT preinit lu table.
  switch (os::random() % 4) {
  case 0: return 0x10;
  case 1: return 0x42;
  case 2: return 0x20;
  case 3: return 0x80;
  }
  return 1;
}

//#define VERBOSE

static void print_and_check_table(NMTPreInitAllocationTable& table, int expected_num_entries) {
  char tmp[256];
  stringStream ss(tmp, sizeof(tmp));
  char expected[256];
  jio_snprintf(expected, sizeof(expected), "entries: %d", expected_num_entries);
  table.print_state(&ss);
  EXPECT_EQ(::strncmp(tmp, expected, strlen(expected)), 0);
#ifdef VERBOSE
  tty->print_raw(tmp);
  tty->cr();
#endif
  DEBUG_ONLY(table.verify();)
}

TEST_VM(NMTPreInit, stress_test_map) {
  NMTPreInitAllocationTable table;
  const int num_allocs = 32 * K; // about factor 100 more than normally expected
  NMTPreInitAllocation** allocations = NEW_C_HEAP_ARRAY(NMTPreInitAllocation*, num_allocs, mtTest);

  // Fill table with allocations
  for (int i = 0; i < num_allocs; i++) {
    NMTPreInitAllocation* a = NMTPreInitAllocation::do_alloc(small_random_nonzero_size());
    table.add(a);
    allocations[i] = a;
  }

  print_and_check_table(table, num_allocs);

  // look them all up
  for (int i = 0; i < num_allocs; i++) {
    const NMTPreInitAllocation* a = table.find(allocations[i]->payload());
    ASSERT_EQ(a, allocations[i]);
  }

  // Randomly realloc
  for (int j = 0; j < num_allocs/2; j++) {
    int pos = os::random() % num_allocs;
    NMTPreInitAllocation* a1 = allocations[pos];
    NMTPreInitAllocation* a2 = table.find_and_remove(a1->payload());
    ASSERT_EQ(a1, a2);
    NMTPreInitAllocation* a3 = NMTPreInitAllocation::do_reallocate(a2, small_random_nonzero_size());
    table.add(a3);
    allocations[pos] = a3;
  }

  print_and_check_table(table, num_allocs);

  // look them all up
  for (int i = 0; i < num_allocs; i++) {
    const NMTPreInitAllocation* a = table.find(allocations[i]->payload());
    ASSERT_EQ(a, allocations[i]);
  }

  // free all
  for (int i = 0; i < num_allocs; i++) {
    NMTPreInitAllocation* a = table.find_and_remove(allocations[i]->payload());
    ASSERT_EQ(a, allocations[i]);
    NMTPreInitAllocation::do_free(a);
    allocations[i] = NULL;
  }

  print_and_check_table(table, 0);

  FREE_C_HEAP_ARRAY(NMTPreInitAllocation*, allocations);
}

#ifdef ASSERT
// Test that we will assert if the lookup table is seriously over-booked.
TEST_VM_ASSERT_MSG(NMTPreInit, assert_on_lu_table_overflow, ".*NMT preinit lookup table degenerated.*") {
  NMTPreInitAllocationTable table;
  const int num_allocs = 400 * 1000; // anything above ~250K entries should trigger the assert (note: normal number of entries is ~500)
  for (int i = 0; i < num_allocs; i++) {
    NMTPreInitAllocation* a = NMTPreInitAllocation::do_alloc(1);
    table.add(a);
  }
#ifdef VERBOSE
  table.print_state(tty);
  tty->cr();
#endif
  table.verify();
}
#endif // ASSERT

#endif // INCLUDE_NMT
