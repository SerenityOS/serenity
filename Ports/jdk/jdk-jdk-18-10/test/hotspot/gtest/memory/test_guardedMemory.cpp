/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/guardedMemory.hpp"
#include "runtime/os.hpp"
#include "unittest.hpp"

#define GEN_PURPOSE_TAG ((void *) ((uintptr_t)0xf000f000))

static void guarded_memory_test_check(void* p, size_t sz, void* tag) {
  ASSERT_TRUE(p != NULL) << "NULL pointer given to check";
  u_char* c = (u_char*) p;
  GuardedMemory guarded(c);
  EXPECT_EQ(guarded.get_tag(), tag) << "Tag is not the same as supplied";
  EXPECT_EQ(guarded.get_user_ptr(), c) << "User pointer is not the same as supplied";
  EXPECT_EQ(guarded.get_user_size(), sz) << "User size is not the same as supplied";
  EXPECT_TRUE(guarded.verify_guards()) << "Guard broken";
}

class GuardedMemoryTest {
 public:
  static size_t get_guard_header_size() {
    return sizeof (GuardedMemory::GuardHeader);
  }
  static size_t get_guard_size() {
    return sizeof (GuardedMemory::Guard);
  }
};

// Test GuardedMemory size
TEST(GuardedMemory, size) {
  size_t total_sz = GuardedMemory::get_total_size(1);
  ASSERT_GT(total_sz, (size_t) 1) << "Unexpected size";
  ASSERT_GE(total_sz, GuardedMemoryTest::get_guard_header_size() + 1
          + GuardedMemoryTest::get_guard_size()) << "Unexpected size";
}

// Test the basic characteristics
TEST(GuardedMemory, basic) {
  u_char* basep =
          (u_char*) os::malloc(GuardedMemory::get_total_size(1), mtInternal);
  GuardedMemory guarded(basep, 1, GEN_PURPOSE_TAG);

  EXPECT_EQ(badResourceValue, *basep)
          << "Expected guard in the form of badResourceValue";

  u_char* userp = guarded.get_user_ptr();
  EXPECT_EQ(uninitBlockPad, *userp)
          << "Expected uninitialized data in the form of uninitBlockPad";
  guarded_memory_test_check(userp, 1, GEN_PURPOSE_TAG);

  void* freep = guarded.release_for_freeing();
  EXPECT_EQ((u_char*) freep, basep) << "Expected the same pointer guard was ";
  EXPECT_EQ(freeBlockPad, *userp) << "Expected user data to be free block padded";
  EXPECT_FALSE(guarded.verify_guards());
  os::free(freep);
}

// Test a number of odd sizes
TEST(GuardedMemory, odd_sizes) {
  u_char* basep =
          (u_char*) os::malloc(GuardedMemory::get_total_size(1), mtInternal);
  GuardedMemory guarded(basep, 1, GEN_PURPOSE_TAG);

  size_t sz = 0;
  do {
    void* p = os::malloc(GuardedMemory::get_total_size(sz), mtInternal);
    void* up = guarded.wrap_with_guards(p, sz, (void*) 1);
    memset(up, 0, sz);
    guarded_memory_test_check(up, sz, (void*) 1);
    if (HasFatalFailure()) {
      return;
    }

    os::free(guarded.release_for_freeing());
    sz = (sz << 4) + 1;
  } while (sz < (256 * 1024));
}

// Test buffer overrun into head...
TEST(GuardedMemory, buffer_overrun_head) {
  u_char* basep =
          (u_char*) os::malloc(GuardedMemory::get_total_size(1), mtInternal);
  GuardedMemory guarded(basep, 1, GEN_PURPOSE_TAG);

  guarded.wrap_with_guards(basep, 1);
  *basep = 0;
  EXPECT_FALSE(guarded.verify_guards());
  os::free(basep);
}

// Test buffer overrun into tail with a number of odd sizes
TEST(GuardedMemory, buffer_overrun_tail) {
  u_char* basep =
          (u_char*) os::malloc(GuardedMemory::get_total_size(1), mtInternal);
  GuardedMemory guarded(basep, 1, GEN_PURPOSE_TAG);

  size_t sz = 1;
  do {
    void* p = os::malloc(GuardedMemory::get_total_size(sz), mtInternal);
    void* up = guarded.wrap_with_guards(p, sz, (void*) 1);
    memset(up, 0, sz + 1); // Buffer-overwrite (within guard)
    EXPECT_FALSE(guarded.verify_guards()) << "Guard was not broken as expected";
    os::free(guarded.release_for_freeing());
    sz = (sz << 4) + 1;
  } while (sz < (256 * 1024));
}

// Test wrap_copy/wrap_free
TEST(GuardedMemory, wrap) {
  EXPECT_TRUE(GuardedMemory::free_copy(NULL)) << "Expected free NULL to be OK";

  const char* str = "Check my bounds out";
  size_t str_sz = strlen(str) + 1;
  char* str_copy = (char*) GuardedMemory::wrap_copy(str, str_sz);
  guarded_memory_test_check(str_copy, str_sz, NULL);
  if (HasFatalFailure()) {
    return;
  }
  EXPECT_EQ(0, strcmp(str, str_copy)) << "Not identical copy";
  EXPECT_TRUE(GuardedMemory::free_copy(str_copy)) << "Free copy failed to verify";

  void* no_data = NULL;
  void* no_data_copy = GuardedMemory::wrap_copy(no_data, 0);
  EXPECT_TRUE(GuardedMemory::free_copy(no_data_copy))
          << "Expected valid guards even for no data copy";
}
