/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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

#ifndef OS_CPU_AIX_PPC_ATOMIC_AIX_PPC_HPP
#define OS_CPU_AIX_PPC_ATOMIC_AIX_PPC_HPP

#ifndef PPC64
#error "Atomic currently only implemented for PPC64"
#endif

#include "orderAccess_aix_ppc.hpp"
#include "utilities/debug.hpp"

// Implementation of class atomic

//
// machine barrier instructions:
//
// - sync            two-way memory barrier, aka fence
// - lwsync          orders  Store|Store,
//                            Load|Store,
//                            Load|Load,
//                   but not Store|Load
// - eieio           orders memory accesses for device memory (only)
// - isync           invalidates speculatively executed instructions
//                   From the POWER ISA 2.06 documentation:
//                    "[...] an isync instruction prevents the execution of
//                   instructions following the isync until instructions
//                   preceding the isync have completed, [...]"
//                   From IBM's AIX assembler reference:
//                    "The isync [...] instructions causes the processor to
//                   refetch any instructions that might have been fetched
//                   prior to the isync instruction. The instruction isync
//                   causes the processor to wait for all previous instructions
//                   to complete. Then any instructions already fetched are
//                   discarded and instruction processing continues in the
//                   environment established by the previous instructions."
//
// semantic barrier instructions:
// (as defined in orderAccess.hpp)
//
// - release         orders Store|Store,       (maps to lwsync)
//                           Load|Store
// - acquire         orders  Load|Store,       (maps to lwsync)
//                           Load|Load
// - fence           orders Store|Store,       (maps to sync)
//                           Load|Store,
//                           Load|Load,
//                          Store|Load
//

inline void pre_membar(atomic_memory_order order) {
  switch (order) {
    case memory_order_relaxed:
    case memory_order_acquire: break;
    case memory_order_release:
    case memory_order_acq_rel: __asm__ __volatile__ ("lwsync" : : : "memory"); break;
    default /*conservative*/ : __asm__ __volatile__ ("sync"   : : : "memory"); break;
  }
}

inline void post_membar(atomic_memory_order order) {
  switch (order) {
    case memory_order_relaxed:
    case memory_order_release: break;
    case memory_order_acquire:
    case memory_order_acq_rel: __asm__ __volatile__ ("isync"  : : : "memory"); break;
    default /*conservative*/ : __asm__ __volatile__ ("sync"   : : : "memory"); break;
  }
}


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

  D result;

  pre_membar(order);

  __asm__ __volatile__ (
    "1: lwarx   %0,  0, %2    \n"
    "   add     %0, %0, %1    \n"
    "   stwcx.  %0,  0, %2    \n"
    "   bne-    1b            \n"
    : /*%0*/"=&r" (result)
    : /*%1*/"r" (add_value), /*%2*/"r" (dest)
    : "cc", "memory" );

  post_membar(order);

  return result;
}


template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<8>::add_and_fetch(D volatile* dest, I add_value,
                                               atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(I));
  STATIC_ASSERT(8 == sizeof(D));

  D result;

  pre_membar(order);

  __asm__ __volatile__ (
    "1: ldarx   %0,  0, %2    \n"
    "   add     %0, %0, %1    \n"
    "   stdcx.  %0,  0, %2    \n"
    "   bne-    1b            \n"
    : /*%0*/"=&r" (result)
    : /*%1*/"r" (add_value), /*%2*/"r" (dest)
    : "cc", "memory" );

  post_membar(order);

  return result;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<4>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order order) const {
  // Note that xchg doesn't necessarily do an acquire
  // (see synchronizer.cpp).

  T old_value;
  const uint64_t zero = 0;

  pre_membar(order);

  __asm__ __volatile__ (
    /* atomic loop */
    "1:                                                 \n"
    "   lwarx   %[old_value], %[dest], %[zero]          \n"
    "   stwcx.  %[exchange_value], %[dest], %[zero]     \n"
    "   bne-    1b                                      \n"
    /* exit */
    "2:                                                 \n"
    /* out */
    : [old_value]       "=&r"   (old_value),
                        "=m"    (*dest)
    /* in */
    : [dest]            "b"     (dest),
      [zero]            "r"     (zero),
      [exchange_value]  "r"     (exchange_value),
                        "m"     (*dest)
    /* clobber */
    : "cc",
      "memory"
    );

  post_membar(order);

  return old_value;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<8>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));
  // Note that xchg doesn't necessarily do an acquire
  // (see synchronizer.cpp).

  T old_value;
  const uint64_t zero = 0;

  pre_membar(order);

  __asm__ __volatile__ (
    /* atomic loop */
    "1:                                                 \n"
    "   ldarx   %[old_value], %[dest], %[zero]          \n"
    "   stdcx.  %[exchange_value], %[dest], %[zero]     \n"
    "   bne-    1b                                      \n"
    /* exit */
    "2:                                                 \n"
    /* out */
    : [old_value]       "=&r"   (old_value),
                        "=m"    (*dest)
    /* in */
    : [dest]            "b"     (dest),
      [zero]            "r"     (zero),
      [exchange_value]  "r"     (exchange_value),
                        "m"     (*dest)
    /* clobber */
    : "cc",
      "memory"
    );

  post_membar(order);

  return old_value;
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<1>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(1 == sizeof(T));

  // Note that cmpxchg guarantees a two-way memory barrier across
  // the cmpxchg, so it's really a a 'fence_cmpxchg_fence' if not
  // specified otherwise (see atomic.hpp).

  // Using 32 bit internally.
  volatile int *dest_base = (volatile int*)((uintptr_t)dest & ~3);

#ifdef VM_LITTLE_ENDIAN
  const unsigned int shift_amount        = ((uintptr_t)dest & 3) * 8;
#else
  const unsigned int shift_amount        = ((~(uintptr_t)dest) & 3) * 8;
#endif
  const unsigned int masked_compare_val  = ((unsigned int)(unsigned char)compare_value),
                     masked_exchange_val = ((unsigned int)(unsigned char)exchange_value),
                     xor_value           = (masked_compare_val ^ masked_exchange_val) << shift_amount;

  unsigned int old_value, value32;

  pre_membar(order);

  __asm__ __volatile__ (
    /* simple guard */
    "   lbz     %[old_value], 0(%[dest])                  \n"
    "   cmpw    %[masked_compare_val], %[old_value]       \n"
    "   bne-    2f                                        \n"
    /* atomic loop */
    "1:                                                   \n"
    "   lwarx   %[value32], 0, %[dest_base]               \n"
    /* extract byte and compare */
    "   srd     %[old_value], %[value32], %[shift_amount] \n"
    "   clrldi  %[old_value], %[old_value], 56            \n"
    "   cmpw    %[masked_compare_val], %[old_value]       \n"
    "   bne-    2f                                        \n"
    /* replace byte and try to store */
    "   xor     %[value32], %[xor_value], %[value32]      \n"
    "   stwcx.  %[value32], 0, %[dest_base]               \n"
    "   bne-    1b                                        \n"
    /* exit */
    "2:                                                   \n"
    /* out */
    : [old_value]           "=&r"   (old_value),
      [value32]             "=&r"   (value32),
                            "=m"    (*dest),
                            "=m"    (*dest_base)
    /* in */
    : [dest]                "b"     (dest),
      [dest_base]           "b"     (dest_base),
      [shift_amount]        "r"     (shift_amount),
      [masked_compare_val]  "r"     (masked_compare_val),
      [xor_value]           "r"     (xor_value),
                            "m"     (*dest),
                            "m"     (*dest_base)
    /* clobber */
    : "cc",
      "memory"
    );

  post_membar(order);

  return PrimitiveConversions::cast<T>((unsigned char)old_value);
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));

  // Note that cmpxchg guarantees a two-way memory barrier across
  // the cmpxchg, so it's really a a 'fence_cmpxchg_fence' if not
  // specified otherwise (see atomic.hpp).

  T old_value;
  const uint64_t zero = 0;

  pre_membar(order);

  __asm__ __volatile__ (
    /* simple guard */
    "   lwz     %[old_value], 0(%[dest])                \n"
    "   cmpw    %[compare_value], %[old_value]          \n"
    "   bne-    2f                                      \n"
    /* atomic loop */
    "1:                                                 \n"
    "   lwarx   %[old_value], %[dest], %[zero]          \n"
    "   cmpw    %[compare_value], %[old_value]          \n"
    "   bne-    2f                                      \n"
    "   stwcx.  %[exchange_value], %[dest], %[zero]     \n"
    "   bne-    1b                                      \n"
    /* exit */
    "2:                                                 \n"
    /* out */
    : [old_value]       "=&r"   (old_value),
                        "=m"    (*dest)
    /* in */
    : [dest]            "b"     (dest),
      [zero]            "r"     (zero),
      [compare_value]   "r"     (compare_value),
      [exchange_value]  "r"     (exchange_value),
                        "m"     (*dest)
    /* clobber */
    : "cc",
      "memory"
    );

  post_membar(order);

  return old_value;
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<8>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));

  // Note that cmpxchg guarantees a two-way memory barrier across
  // the cmpxchg, so it's really a a 'fence_cmpxchg_fence' if not
  // specified otherwise (see atomic.hpp).

  T old_value;
  const uint64_t zero = 0;

  pre_membar(order);

  __asm__ __volatile__ (
    /* simple guard */
    "   ld      %[old_value], 0(%[dest])                \n"
    "   cmpd    %[compare_value], %[old_value]          \n"
    "   bne-    2f                                      \n"
    /* atomic loop */
    "1:                                                 \n"
    "   ldarx   %[old_value], %[dest], %[zero]          \n"
    "   cmpd    %[compare_value], %[old_value]          \n"
    "   bne-    2f                                      \n"
    "   stdcx.  %[exchange_value], %[dest], %[zero]     \n"
    "   bne-    1b                                      \n"
    /* exit */
    "2:                                                 \n"
    /* out */
    : [old_value]       "=&r"   (old_value),
                        "=m"    (*dest)
    /* in */
    : [dest]            "b"     (dest),
      [zero]            "r"     (zero),
      [compare_value]   "r"     (compare_value),
      [exchange_value]  "r"     (exchange_value),
                        "m"     (*dest)
    /* clobber */
    : "cc",
      "memory"
    );

  post_membar(order);

  return old_value;
}

template<size_t byte_size>
struct Atomic::PlatformOrderedLoad<byte_size, X_ACQUIRE> {
  template <typename T>
  T operator()(const volatile T* p) const {
    T t = Atomic::load(p);
    // Use twi-isync for load_acquire (faster than lwsync).
    __asm__ __volatile__ ("twi 0,%0,0\n isync\n" : : "r" (t) : "memory");
    return t;
  }
};

#endif // OS_CPU_AIX_PPC_ATOMIC_AIX_PPC_HPP
