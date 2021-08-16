/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2008, 2009, 2010 Red Hat, Inc.
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

#ifndef CPU_ZERO_ENTRY_ZERO_HPP
#define CPU_ZERO_ENTRY_ZERO_HPP

#include "interpreter/zero/zeroInterpreter.hpp"

class ZeroEntry {
 public:
  ZeroEntry() {
    ShouldNotCallThis();
  }

 private:
  address _entry_point;

 public:
  address entry_point() const {
    return _entry_point;
  }
  void set_entry_point(address entry_point) {
    _entry_point = entry_point;
  }

 private:
  typedef int (*NormalEntryFunc)(Method* method,
                                 intptr_t  base_pc,
                                 TRAPS);
  typedef int (*OSREntryFunc)(Method* method,
                              address   osr_buf,
                              intptr_t  base_pc,
                              TRAPS);

 public:
  void invoke(Method* method, TRAPS) const {
    maybe_deoptimize(
      ((NormalEntryFunc) entry_point())(method, (intptr_t) this, THREAD),
      THREAD);
  }
  void invoke_osr(Method* method, address osr_buf, TRAPS) const {
    maybe_deoptimize(
      ((OSREntryFunc) entry_point())(method, osr_buf, (intptr_t) this, THREAD),
      THREAD);
  }

 private:
  static void maybe_deoptimize(int deoptimized_frames, TRAPS) {
    if (deoptimized_frames)
      ZeroInterpreter::main_loop(deoptimized_frames - 1, THREAD);
  }

 public:
  static ByteSize entry_point_offset() {
    return byte_offset_of(ZeroEntry, _entry_point);
  }
};

#endif // CPU_ZERO_ENTRY_ZERO_HPP
