/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_RDTSC_X86_HPP
#define CPU_X86_RDTSC_X86_HPP

#include "memory/allocation.hpp"
#include "utilities/macros.hpp"

// Interface to the x86 rdtsc() time counter, if available.
// Not guaranteed to be synchronized across hardware threads and
// therefore software threads, and can be updated asynchronously
// by software. elapsed_counter() can jump backwards
// as well as jump forward when threads query different cores/sockets.
// Very much not recommended for general use.
// INVTSC is a minimal requirement for auto-enablement.

class Rdtsc : AllStatic {
 public:
  static jlong elapsed_counter(); // provides quick time stamps
  static jlong frequency();       // tsc register
  static bool  is_supported();    // InvariantTSC
  static jlong raw();             // direct rdtsc() access
  static bool  is_elapsed_counter_enabled(); // turn off with -XX:-UseFastUnorderedTimeStamps
  static jlong epoch();
  static bool  initialize();
};

#endif // CPU_X86_RDTSC_X86_HPP
