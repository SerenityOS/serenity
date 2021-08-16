/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
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

#ifndef OS_CPU_LINUX_S390_ORDERACCESS_LINUX_S390_HPP
#define OS_CPU_LINUX_S390_ORDERACCESS_LINUX_S390_HPP

// Included in orderAccess.hpp header file.

#include "runtime/vm_version.hpp"

// Implementation of class OrderAccess.

//
// machine barrier instructions:
//
//   - z_sync            two-way memory barrier, aka fence
//
// semantic barrier instructions:
// (as defined in orderAccess.hpp)
//
//   - z_release         orders Store|Store,    (maps to compiler barrier)
//                               Load|Store
//   - z_acquire         orders  Load|Store,    (maps to compiler barrier)
//                               Load|Load
//   - z_fence           orders Store|Store,    (maps to z_sync)
//                               Load|Store,
//                               Load|Load,
//                              Store|Load
//


// Only load-after-store-order is not guaranteed on z/Architecture, i.e. only 'fence'
// is needed.

// A compiler barrier, forcing the C++ compiler to invalidate all memory assumptions.
#define inlasm_compiler_barrier() __asm__ volatile ("" : : : "memory");
// "bcr 15, 0" is used as two way memory barrier.
#define inlasm_zarch_sync() __asm__ __volatile__ ("bcr 15, 0" : : : "memory");

// Release and acquire are empty on z/Architecture, but potential
// optimizations of gcc must be forbidden by OrderAccess::release and
// OrderAccess::acquire.
#define inlasm_zarch_release() inlasm_compiler_barrier()
#define inlasm_zarch_acquire() inlasm_compiler_barrier()
#define inlasm_zarch_fence()   inlasm_zarch_sync()

inline void OrderAccess::loadload()   { inlasm_compiler_barrier(); }
inline void OrderAccess::storestore() { inlasm_compiler_barrier(); }
inline void OrderAccess::loadstore()  { inlasm_compiler_barrier(); }
inline void OrderAccess::storeload()  { inlasm_zarch_sync(); }

inline void OrderAccess::acquire()    { inlasm_zarch_acquire(); }
inline void OrderAccess::release()    { inlasm_zarch_release(); }
inline void OrderAccess::fence()      { inlasm_zarch_sync(); }
inline void OrderAccess::cross_modify_fence_impl() { inlasm_zarch_sync(); }

#undef inlasm_compiler_barrier
#undef inlasm_zarch_sync
#undef inlasm_zarch_release
#undef inlasm_zarch_acquire
#undef inlasm_zarch_fence

#endif // OS_CPU_LINUX_S390_ORDERACCESS_LINUX_S390_HPP
