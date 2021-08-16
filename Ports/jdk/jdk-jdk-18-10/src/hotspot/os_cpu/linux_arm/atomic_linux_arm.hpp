/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_LINUX_ARM_ATOMIC_LINUX_ARM_HPP
#define OS_CPU_LINUX_ARM_ATOMIC_LINUX_ARM_HPP

#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"

// Implementation of class atomic

/*
 * Atomic long operations on 32-bit ARM
 * ARM v7 supports LDREXD/STREXD synchronization instructions so no problem.
 * ARM < v7 does not have explicit 64 atomic load/store capability.
 * However, gcc emits LDRD/STRD instructions on v5te and LDM/STM on v5t
 * when loading/storing 64 bits.
 * For non-MP machines (which is all we support for ARM < v7)
 * under current Linux distros these instructions appear atomic.
 * See section A3.5.3 of ARM Architecture Reference Manual for ARM v7.
 * Also, for cmpxchg64, if ARM < v7 we check for cmpxchg64 support in the
 * Linux kernel using _kuser_helper_version. See entry-armv.S in the Linux
 * kernel source or kernel_user_helpers.txt in Linux Doc.
 */

template<>
template<typename T>
inline T Atomic::PlatformLoad<8>::operator()(T const volatile* src) const {
  STATIC_ASSERT(8 == sizeof(T));
  return PrimitiveConversions::cast<T>(
    (*os::atomic_load_long_func)(reinterpret_cast<const volatile int64_t*>(src)));
}

template<>
template<typename T>
inline void Atomic::PlatformStore<8>::operator()(T volatile* dest,
                                                 T store_value) const {
  STATIC_ASSERT(8 == sizeof(T));
  (*os::atomic_store_long_func)(
    PrimitiveConversions::cast<int64_t>(store_value), reinterpret_cast<volatile int64_t*>(dest));
}

// As per atomic.hpp all read-modify-write operations have to provide two-way
// barriers semantics.
//
// For ARMv7 we add explicit barriers in the stubs.

template<size_t byte_size>
struct Atomic::PlatformAdd {
  template<typename D, typename I>
  D add_and_fetch(D volatile* dest, I add_value, atomic_memory_order order) const;

  template<typename D, typename I>
  D fetch_and_add(D volatile* dest, I add_value, atomic_memory_order order) const {
    return add_and_fetch(dest, add_value, order) - add_value;
  }
};

template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<4>::add_and_fetch(D volatile* dest, I add_value,
                                               atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(I));
  STATIC_ASSERT(4 == sizeof(D));
  return add_using_helper<int32_t>(os::atomic_add_func, dest, add_value);
}


template<>
template<typename T>
inline T Atomic::PlatformXchg<4>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));
  return xchg_using_helper<int32_t>(os::atomic_xchg_func, dest, exchange_value);
}


// The memory_order parameter is ignored - we always provide the strongest/most-conservative ordering

// No direct support for cmpxchg of bytes; emulate using int.
template<>
struct Atomic::PlatformCmpxchg<1> : Atomic::CmpxchgByteUsingInt {};


inline int32_t reorder_cmpxchg_func(int32_t exchange_value,
                                    int32_t volatile* dest,
                                    int32_t compare_value) {
  // Warning:  Arguments are swapped to avoid moving them for kernel call
  return (*os::atomic_cmpxchg_func)(compare_value, exchange_value, dest);
}

inline int64_t reorder_cmpxchg_long_func(int64_t exchange_value,
                                         int64_t volatile* dest,
                                         int64_t compare_value) {
  assert(VM_Version::supports_cx8(), "Atomic compare and exchange int64_t not supported on this architecture!");
  // Warning:  Arguments are swapped to avoid moving them for kernel call
  return (*os::atomic_cmpxchg_long_func)(compare_value, exchange_value, dest);
}


template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));
  return cmpxchg_using_helper<int32_t>(reorder_cmpxchg_func, dest, compare_value, exchange_value);
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<8>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));
  return cmpxchg_using_helper<int64_t>(reorder_cmpxchg_long_func, dest, compare_value, exchange_value);
}

#endif // OS_CPU_LINUX_ARM_ATOMIC_LINUX_ARM_HPP
