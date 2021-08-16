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

#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

// A fixture base class for tests that need to change the log configuration,
// or use a log file. After each test, the fixture will automatically restore
// the log configuration and remove the test file (if used).
// Provides TestLogFileName which is unique for each test, and is automatically
// deleted after the test completes.
class LogTestFixture : public testing::Test {
 private:
  char _filename[2 * K];
  size_t _n_snapshots;
  char** _configuration_snapshot;

 protected:
  const char* TestLogFileName;

  LogTestFixture();
  ~LogTestFixture();

  static bool set_log_config(const char* output,
                             const char* what,
                             const char* decorators = "",
                             const char* options = "",
                             bool allow_failure = false);

  void snapshot_config();
  void restore_config();
  void clear_snapshot();
};

