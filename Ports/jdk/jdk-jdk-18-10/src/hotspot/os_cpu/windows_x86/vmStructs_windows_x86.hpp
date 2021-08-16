/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_X86_VMSTRUCTS_WINDOWS_X86_HPP
#define OS_CPU_WINDOWS_X86_VMSTRUCTS_WINDOWS_X86_HPP

// These are the OS and CPU-specific fields, types and integer
// constants required by the Serviceability Agent. This file is
// referenced by vmStructs.cpp.

#define VM_STRUCTS_OS_CPU(nonstatic_field, static_field, unchecked_nonstatic_field, volatile_nonstatic_field, nonproduct_nonstatic_field, c2_nonstatic_field, unchecked_c1_static_field, unchecked_c2_static_field) \
                                                                                                                                     \
  /******************************/                                                                                                   \
  /* Threads (NOTE: incomplete) */                                                                                                   \
  /******************************/                                                                                                   \
                                                                                                                                     \
  nonstatic_field(OSThread,                    _thread_id,                                    OSThread::thread_id_t)                 \
  unchecked_nonstatic_field(OSThread,          _thread_handle,                                sizeof(HANDLE)) /* NOTE: no type */

#define VM_TYPES_OS_CPU(declare_type, declare_toplevel_type, declare_oop_type, declare_integer_type, declare_unsigned_integer_type, declare_c1_toplevel_type, declare_c2_type, declare_c2_toplevel_type) \
                                                                          \
  declare_unsigned_integer_type(OSThread::thread_id_t)

#define VM_INT_CONSTANTS_OS_CPU(declare_constant, declare_preprocessor_constant, declare_c1_constant, declare_c2_constant, declare_c2_preprocessor_constant)

#define VM_LONG_CONSTANTS_OS_CPU(declare_constant, declare_preprocessor_constant, declare_c1_constant, declare_c2_constant, declare_c2_preprocessor_constant)

#endif // OS_CPU_WINDOWS_X86_VMSTRUCTS_WINDOWS_X86_HPP
