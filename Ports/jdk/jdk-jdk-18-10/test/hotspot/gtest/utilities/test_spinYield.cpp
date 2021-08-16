/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/ostream.hpp"
#include "utilities/spinYield.hpp"
#include "unittest.hpp"

// Some basic tests of SpinYield, using comparison of report output with
// expected results to verify state.  This is all very hard-wired to the
// current implementation of SpinYield, esp. the report function.

static void check_report(const SpinYield* spinner, const char* expected) {
  char buffer[100];
  stringStream s(buffer, sizeof(buffer));
  spinner->report(&s);
  ASSERT_STREQ(expected, buffer);
}

TEST(SpinYield, no_waiting) {
  SpinYield spinner;
  check_report(&spinner, "no waiting");
}

TEST(SpinYield, one_wait) {
  SpinYield spinner(100);
  spinner.wait();
  check_report(&spinner, os::is_MP() ? "spins = 1" : "yields = 1");
}

TEST(SpinYield, ten_waits) {
  SpinYield spinner(100, 100);
  for (unsigned i = 0; i < 10; ++i) {
    spinner.wait();
  }
  check_report(&spinner, os::is_MP() ? "spins = 10" : "yields = 10");
}

TEST(SpinYield, two_yields) {
  SpinYield spinner(0, 10);
  spinner.wait();
  spinner.wait();
  check_report(&spinner, "yields = 2");
}

TEST_VM(SpinYield, one_sleep) {
  SpinYield spinner(0, 0);
  spinner.wait();

  char buffer[100];
  stringStream s(buffer, sizeof(buffer));
  spinner.report(&s);

  const char* expected = "sleep = ";
  ASSERT_TRUE(strncmp(expected, buffer, strlen(expected)) == 0);
}

TEST_VM(SpinYield, one_spin_one_sleep) {
  SpinYield spinner(1, 0);
  spinner.wait();
  spinner.wait();

  char buffer[100];
  stringStream s(buffer, sizeof(buffer));
  spinner.report(&s);

  const char* expected_MP = "spins = 1, sleep = ";
  const char* expected_UP = "sleep = ";
  const char* expected = os::is_MP() ? expected_MP : expected_UP;
  ASSERT_TRUE(strncmp(expected, buffer, strlen(expected)) == 0);
}
