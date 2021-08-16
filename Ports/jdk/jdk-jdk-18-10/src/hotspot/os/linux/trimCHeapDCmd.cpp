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
 *
 */

#include "precompiled.hpp"
#include "logging/log.hpp"
#include "runtime/os.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "trimCHeapDCmd.hpp"

#include <malloc.h>

void TrimCLibcHeapDCmd::execute(DCmdSource source, TRAPS) {
#ifdef __GLIBC__
  stringStream ss_report(1024); // Note: before calling trim

  os::Linux::meminfo_t info1;
  os::Linux::meminfo_t info2;
  // Query memory before...
  bool have_info1 = os::Linux::query_process_memory_info(&info1);

  _output->print_cr("Attempting trim...");
  ::malloc_trim(0);
  _output->print_cr("Done.");

  // ...and after trim.
  bool have_info2 = os::Linux::query_process_memory_info(&info2);

  // Print report both to output stream as well to UL
  bool wrote_something = false;
  if (have_info1 && have_info2) {
    if (info1.vmsize != -1 && info2.vmsize != -1) {
      ss_report.print_cr("Virtual size before: " SSIZE_FORMAT "k, after: " SSIZE_FORMAT "k, (" SSIZE_FORMAT "k)",
                         info1.vmsize, info2.vmsize, (info2.vmsize - info1.vmsize));
      wrote_something = true;
    }
    if (info1.vmrss != -1 && info2.vmrss != -1) {
      ss_report.print_cr("RSS before: " SSIZE_FORMAT "k, after: " SSIZE_FORMAT "k, (" SSIZE_FORMAT "k)",
                         info1.vmrss, info2.vmrss, (info2.vmrss - info1.vmrss));
      wrote_something = true;
    }
    if (info1.vmswap != -1 && info2.vmswap != -1) {
      ss_report.print_cr("Swap before: " SSIZE_FORMAT "k, after: " SSIZE_FORMAT "k, (" SSIZE_FORMAT "k)",
                         info1.vmswap, info2.vmswap, (info2.vmswap - info1.vmswap));
      wrote_something = true;
    }
  }
  if (!wrote_something) {
    ss_report.print_raw("No details available.");
  }

  _output->print_raw(ss_report.base());
  log_info(os)("malloc_trim:\n%s", ss_report.base());
#else
  _output->print_cr("Not available.");
#endif
}
