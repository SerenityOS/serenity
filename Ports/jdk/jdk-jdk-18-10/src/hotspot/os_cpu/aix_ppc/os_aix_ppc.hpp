/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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

#ifndef OS_CPU_AIX_PPC_OS_AIX_PPC_HPP
#define OS_CPU_AIX_PPC_OS_AIX_PPC_HPP

  static void setup_fpu() {}

  // Used to register dynamic code cache area with the OS
  // Note: Currently only used in 64 bit Windows implementations
  static bool register_code_area(char *low, char *high) { return true; }

  #define PLATFORM_PRINT_NATIVE_STACK 1
  static bool platform_print_native_stack(outputStream* st, void* context,
                                          char *buf, int buf_size);

  #define HAVE_FUNCTION_DESCRIPTORS 1
  static void* resolve_function_descriptor(void* p);

#endif // OS_CPU_AIX_PPC_OS_AIX_PPC_HPP
