/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_X86_ORDERACCESS_WINDOWS_X86_HPP
#define OS_CPU_WINDOWS_X86_ORDERACCESS_WINDOWS_X86_HPP

// Included in orderAccess.hpp header file.

#include <intrin.h>

// Compiler version last used for testing: Microsoft Visual Studio 2010
// Please update this information when this file changes

// Implementation of class OrderAccess.

// A compiler barrier, forcing the C++ compiler to invalidate all memory assumptions
inline void compiler_barrier() {
  _ReadWriteBarrier();
}

inline void OrderAccess::loadload()   { compiler_barrier(); }
inline void OrderAccess::storestore() { compiler_barrier(); }
inline void OrderAccess::loadstore()  { compiler_barrier(); }
inline void OrderAccess::storeload()  { fence(); }

inline void OrderAccess::acquire()    { compiler_barrier(); }
inline void OrderAccess::release()    { compiler_barrier(); }

inline void OrderAccess::fence() {
#ifdef AMD64
  StubRoutines_fence();
#else
  __asm {
    lock add dword ptr [esp], 0;
  }
#endif // AMD64
  compiler_barrier();
}

inline void OrderAccess::cross_modify_fence_impl()
#if _MSC_VER >= 1928
{
//_serialize() intrinsic is supported starting from VS2019-16.7.2
  if (VM_Version::supports_serialize()) {
    _serialize();
  } else {
    int regs[4];
    __cpuid(regs, 0);
  }
}
#else
{
  int regs[4];
  __cpuid(regs, 0);
}
#endif

#endif // OS_CPU_WINDOWS_X86_ORDERACCESS_WINDOWS_X86_HPP
