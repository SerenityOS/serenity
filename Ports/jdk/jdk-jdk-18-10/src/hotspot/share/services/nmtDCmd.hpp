/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_NMTDCMD_HPP
#define SHARE_SERVICES_NMTDCMD_HPP

#if INCLUDE_NMT

#include "services/diagnosticArgument.hpp"
#include "services/diagnosticFramework.hpp"
#include "services/memBaseline.hpp"
#include "services/mallocTracker.hpp"

/**
 * Native memory tracking DCmd implementation
 */
class NMTDCmd: public DCmdWithParser {
 protected:
  DCmdArgument<bool>  _summary;
  DCmdArgument<bool>  _detail;
  DCmdArgument<bool>  _baseline;
  DCmdArgument<bool>  _summary_diff;
  DCmdArgument<bool>  _detail_diff;
  DCmdArgument<bool>  _shutdown;
  DCmdArgument<bool>  _statistics;
  DCmdArgument<char*> _scale;

 public:
  NMTDCmd(outputStream* output, bool heap);
  static const char* name() { return "VM.native_memory"; }
  static const char* description() {
    return "Print native memory usage";
  }
  static const char* impact() {
    return "Medium";
  }
  static const JavaPermission permission() {
    JavaPermission p = {"java.lang.management.ManagementPermission",
                        "monitor", NULL};
    return p;
  }
  virtual void execute(DCmdSource source, TRAPS);

 private:
  void report(bool summaryOnly, size_t scale);
  void report_diff(bool summaryOnly, size_t scale);

  size_t get_scale(const char* scale) const;

  // check if NMT running at detail tracking level
  bool check_detail_tracking_level(outputStream* out);
};

#endif // INCLUDE_NMT

#endif // SHARE_SERVICES_NMTDCMD_HPP
