/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_BSD_X86_OS_BSD_X86_HPP
#define OS_CPU_BSD_X86_OS_BSD_X86_HPP

// Core region alignment is 16K to be able to run binaries built on MacOS x64
// on MacOS aarch64.
#if defined(__APPLE__) && defined(COMPATIBLE_CDS_ALIGNMENT)
#define CDS_CORE_REGION_ALIGNMENT (16*K)
#endif

  static void setup_fpu();
  static bool supports_sse();
  static juint cpu_microcode_revision();

  static jlong rdtsc();

  // Used to register dynamic code cache area with the OS
  // Note: Currently only used in 64 bit Windows implementations
  static bool register_code_area(char *low, char *high) { return true; }

#endif // OS_CPU_BSD_X86_OS_BSD_X86_HPP
