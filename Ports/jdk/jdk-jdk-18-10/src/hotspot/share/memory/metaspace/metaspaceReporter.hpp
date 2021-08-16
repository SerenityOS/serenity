/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_METASPACEREPORTER_HPP
#define SHARE_MEMORY_METASPACE_METASPACEREPORTER_HPP

#include "memory/allocation.hpp"

namespace metaspace {

class MetaspaceReporter : public AllStatic {
public:

  // Flags for print_report().
  enum class Option {
    // Show usage by class loader.
    ShowLoaders                 = (1 << 0),
    // Breaks report down by chunk type (small, medium, ...).
    BreakDownByChunkType        = (1 << 1),
    // Breaks report down by space type (anonymous, reflection, ...).
    BreakDownBySpaceType        = (1 << 2),
    // Print details about the underlying virtual spaces.
    ShowVSList                  = (1 << 3),
    // If show_loaders: show loaded classes for each loader.
    ShowClasses                 = (1 << 4)
  };

  // This will print out a basic metaspace usage report but
  // unlike print_report() is guaranteed not to lock or to walk the CLDG.
  static void print_basic_report(outputStream* st, size_t scale);

  // Prints a report about the current metaspace state.
  // Optional parts can be enabled via flags.
  // Function will walk the CLDG and will lock the expand lock; if that is not
  // convenient, use print_basic_report() instead.
  static void print_report(outputStream* out, size_t scale = 0, int flags = 0);

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_METASPACEREPORTER_HPP
