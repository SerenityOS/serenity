/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2021, Red Hat Inc. All rights reserved.
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

#ifndef OS_CPU_LINUX_AARCH64_ATOMIC_LINUX_AARCH64_HPP
#define OS_CPU_LINUX_AARCH64_ATOMIC_LINUX_AARCH64_HPP

#include "atomic_aarch64.hpp"
#include "runtime/vm_version.hpp"

// Implementation of class atomic

// Note that memory_order_conservative requires a full barrier after atomic stores.
// See https://patchwork.kernel.org/patch/3575821/

// Call one of the stubs from C++. This uses the C calling convention,
// but this asm definition is used in order only to clobber the
// registers we use. If we called the stubs via an ABI call we'd have
// to save X0 - X18 and most of the vectors.
//
// This really ought to be a template definition, but see GCC Bug
// 33661, template methods forget explicit local register asm
// vars. The problem is that register specifiers attached to local
// variables are ignored in any template function.
inline uint64_t bare_atomic_fastcall(address stub, volatile void *ptr, uint64_t arg1, uint64_t arg2 = 0) {
  register uint64_t reg0 __asm__("x0") = (uint64_t)ptr;
  register uint64_t reg1 __asm__("x1") = arg1;
  register uint64_t reg2 __asm__("x2") = arg2;
  register uint64_t reg3 __asm__("x3") = (uint64_t)stub;
  register uint64_t result __asm__("x0");
  asm volatile(// "stp x29, x30, [sp, #-16]!;"
               " blr %1;"
               // " ldp x29, x30, [sp], #16 // regs %0, %1, %2, %3, %4"
               : "=r"(result), "+r"(reg3), "+r"(reg2)
               : "r"(reg1), "0"(reg0) : "x8", "x9", "x30", "cc", "memory");
  return result;
}

template <typename F, typename D, typename T1>
inline D atomic_fastcall(F stub, volatile D *dest, T1 arg1) {
  return (D)bare_atomic_fastcall(CAST_FROM_FN_PTR(address, stub),
                                 dest, (uint64_t)arg1);
}

template <typename F, typename D, typename T1, typename T2>
inline D atomic_fastcall(F stub, volatile D *dest, T1 arg1, T2 arg2) {
  return (D)bare_atomic_fastcall(CAST_FROM_FN_PTR(address, stub),
                                 dest, (uint64_t)arg1, (uint64_t)arg2);
}

template<size_t byte_size>
struct Atomic::PlatformAdd {
  template<typename D, typename I>
  D fetch_and_add(D volatile* dest, I add_value, atomic_memory_order order) const;

  template<typename D, typename I>
  D add_and_fetch(D volatile* dest, I add_value, atomic_memory_order order) const {
    D value = fetch_and_add(dest, add_value, order) + add_value;
    return value;
  }
};

template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<4>::fetch_and_add(D volatile* dest, I add_value,
                                               atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(I));
  STATIC_ASSERT(4 == sizeof(D));
  D old_value
    = atomic_fastcall(aarch64_atomic_fetch_add_4_impl, dest, add_value);
  return old_value;
}

template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<8>::fetch_and_add(D volatile* dest, I add_value,
                                               atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(I));
  STATIC_ASSERT(8 == sizeof(D));
  D old_value
    = atomic_fastcall(aarch64_atomic_fetch_add_8_impl, dest, add_value);
  return old_value;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<4>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));
  T old_value = atomic_fastcall(aarch64_atomic_xchg_4_impl, dest, exchange_value);
  return old_value;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<8>::operator()(T volatile* dest, T exchange_value,
                                             atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));
  T old_value = atomic_fastcall(aarch64_atomic_xchg_8_impl, dest, exchange_value);
  return old_value;
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<1>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(1 == sizeof(T));
  aarch64_atomic_stub_t stub;
  switch (order) {
  case memory_order_relaxed:
    stub = aarch64_atomic_cmpxchg_1_relaxed_impl; break;
  default:
    stub = aarch64_atomic_cmpxchg_1_impl; break;
  }

  return atomic_fastcall(stub, dest, compare_value, exchange_value);
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));
  aarch64_atomic_stub_t stub;
  switch (order) {
  case memory_order_relaxed:
    stub = aarch64_atomic_cmpxchg_4_relaxed_impl; break;
  case memory_order_release:
    stub = aarch64_atomic_cmpxchg_4_release_impl; break;
  case memory_order_acq_rel:
  case memory_order_seq_cst:
    stub = aarch64_atomic_cmpxchg_4_seq_cst_impl; break;
  default:
    stub = aarch64_atomic_cmpxchg_4_impl; break;
  }

  return atomic_fastcall(stub, dest, compare_value, exchange_value);
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<8>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));
  aarch64_atomic_stub_t stub;
  switch (order) {
  case memory_order_relaxed:
    stub = aarch64_atomic_cmpxchg_8_relaxed_impl; break;
  case memory_order_release:
    stub = aarch64_atomic_cmpxchg_8_release_impl; break;
  case memory_order_acq_rel:
  case memory_order_seq_cst:
    stub = aarch64_atomic_cmpxchg_8_seq_cst_impl; break;
  default:
    stub = aarch64_atomic_cmpxchg_8_impl; break;
  }

  return atomic_fastcall(stub, dest, compare_value, exchange_value);
}

template<size_t byte_size>
struct Atomic::PlatformOrderedLoad<byte_size, X_ACQUIRE>
{
  template <typename T>
  T operator()(const volatile T* p) const { T data; __atomic_load(const_cast<T*>(p), &data, __ATOMIC_ACQUIRE); return data; }
};

template<size_t byte_size>
struct Atomic::PlatformOrderedStore<byte_size, RELEASE_X>
{
  template <typename T>
  void operator()(volatile T* p, T v) const { __atomic_store(const_cast<T*>(p), &v, __ATOMIC_RELEASE); }
};

template<size_t byte_size>
struct Atomic::PlatformOrderedStore<byte_size, RELEASE_X_FENCE>
{
  template <typename T>
  void operator()(volatile T* p, T v) const { release_store(p, v); OrderAccess::fence(); }
};

#endif // OS_CPU_LINUX_AARCH64_ATOMIC_LINUX_AARCH64_HPP
