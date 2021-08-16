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

#ifndef OS_LINUX_TRIMCHEAPDCMD_HPP
#define OS_LINUX_TRIMCHEAPDCMD_HPP

#include "services/diagnosticCommand.hpp"

class outputStream;

class TrimCLibcHeapDCmd : public DCmd {
public:
  TrimCLibcHeapDCmd(outputStream* output, bool heap) : DCmd(output, heap) {}
  static const char* name() {
    return "System.trim_native_heap";
  }
  static const char* description() {
    return "Attempts to free up memory by trimming the C-heap.";
  }
  static const char* impact() {
    return "Low";
  }
  static const JavaPermission permission() {
    JavaPermission p = { "java.lang.management.ManagementPermission", "control", NULL };
    return p;
  }
  virtual void execute(DCmdSource source, TRAPS);
};

#endif // OS_LINUX_TRIMCHEAPDCMD_HPP
