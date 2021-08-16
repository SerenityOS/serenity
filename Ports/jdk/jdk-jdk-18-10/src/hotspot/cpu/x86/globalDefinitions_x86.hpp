/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_GLOBALDEFINITIONS_X86_HPP
#define CPU_X86_GLOBALDEFINITIONS_X86_HPP

const int StackAlignmentInBytes  = 16;

// Indicates whether the C calling conventions require that
// 32-bit integer argument values are extended to 64 bits.
const bool CCallingConventionRequiresIntsAsLongs = false;

#define SUPPORTS_NATIVE_CX8

#define CPU_MULTI_COPY_ATOMIC

// The expected size in bytes of a cache line, used to pad data structures.
#if COMPILER1_AND_COMPILER2
  #ifdef _LP64
    // tiered, 64-bit, large machine
    #define DEFAULT_CACHE_LINE_SIZE 128
    #define OM_CACHE_LINE_SIZE 64
  #else
    // tiered, 32-bit, medium machine
    #define DEFAULT_CACHE_LINE_SIZE 64
  #endif
#elif defined(COMPILER1)
  // pure C1, 32-bit, small machine
  // i486 was the last Intel chip with 16-byte cache line size
  #define DEFAULT_CACHE_LINE_SIZE 32
#elif defined(COMPILER2)
  #ifdef _LP64
    // pure C2, 64-bit, large machine
    #define DEFAULT_CACHE_LINE_SIZE 128
    #define OM_CACHE_LINE_SIZE 64
  #else
    // pure C2, 32-bit, medium machine
    #define DEFAULT_CACHE_LINE_SIZE 64
  #endif
#endif

#if defined(COMPILER2)
// Include Restricted Transactional Memory lock eliding optimization
#define INCLUDE_RTM_OPT 1
#endif

#if defined(LINUX) || defined(__APPLE__)
#define SUPPORT_RESERVED_STACK_AREA
#endif

#if INCLUDE_JVMCI
#define COMPRESSED_CLASS_POINTERS_DEPENDS_ON_COMPRESSED_OOPS EnableJVMCI
#else
#define COMPRESSED_CLASS_POINTERS_DEPENDS_ON_COMPRESSED_OOPS false
#endif

#endif // CPU_X86_GLOBALDEFINITIONS_X86_HPP
