/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_X86_ATOMIC_WINDOWS_X86_HPP
#define OS_CPU_WINDOWS_X86_ATOMIC_WINDOWS_X86_HPP

#include <intrin.h>
#include "runtime/os.hpp"

// Note that in MSVC, volatile memory accesses are explicitly
// guaranteed to have acquire release semantics (w.r.t. compiler
// reordering) and therefore does not even need a compiler barrier
// for normal acquire release accesses. And all generalized
// bound calls like release_store go through Atomic::load
// and Atomic::store which do volatile memory accesses.
template<> inline void ScopedFence<X_ACQUIRE>::postfix()       { }
template<> inline void ScopedFence<RELEASE_X>::prefix()        { }
template<> inline void ScopedFence<RELEASE_X_FENCE>::prefix()  { }
template<> inline void ScopedFence<RELEASE_X_FENCE>::postfix() { OrderAccess::fence(); }

template<size_t byte_size>
struct Atomic::PlatformAdd {
  template<typename D, typename I>
  D add_and_fetch(D volatile* dest, I add_value, atomic_memory_order order) const;

  template<typename D, typename I>
  D fetch_and_add(D volatile* dest, I add_value, atomic_memory_order order) const {
    return add_and_fetch(dest, add_value, order) - add_value;
  }
};

// The Interlocked* APIs only take long and will not accept __int32. That is
// acceptable on Windows, since long is a 32-bits integer type.

#define DEFINE_INTRINSIC_ADD(IntrinsicName, IntrinsicType)                \
  template<>                                                              \
  template<typename D, typename I>                                        \
  inline D Atomic::PlatformAdd<sizeof(IntrinsicType)>::add_and_fetch(D volatile* dest, \
                                                                     I add_value, \
                                                                     atomic_memory_order order) const { \
    STATIC_ASSERT(sizeof(IntrinsicType) == sizeof(D));                    \
    return PrimitiveConversions::cast<D>(                                 \
      IntrinsicName(reinterpret_cast<IntrinsicType volatile *>(dest),     \
                    PrimitiveConversions::cast<IntrinsicType>(add_value))); \
  }

DEFINE_INTRINSIC_ADD(InterlockedAdd,   long)
DEFINE_INTRINSIC_ADD(InterlockedAdd64, __int64)

#undef DEFINE_INTRINSIC_ADD

#define DEFINE_INTRINSIC_XCHG(IntrinsicName, IntrinsicType)               \
  template<>                                                              \
  template<typename T>                                                    \
  inline T Atomic::PlatformXchg<sizeof(IntrinsicType)>::operator()(T volatile* dest, \
                                                                   T exchange_value, \
                                                                   atomic_memory_order order) const { \
    STATIC_ASSERT(sizeof(IntrinsicType) == sizeof(T));                    \
    return PrimitiveConversions::cast<T>(                                 \
      IntrinsicName(reinterpret_cast<IntrinsicType volatile *>(dest),     \
                    PrimitiveConversions::cast<IntrinsicType>(exchange_value))); \
  }

DEFINE_INTRINSIC_XCHG(InterlockedExchange,   long)
DEFINE_INTRINSIC_XCHG(InterlockedExchange64, __int64)

#undef DEFINE_INTRINSIC_XCHG

// Note: the order of the parameters is different between
// Atomic::PlatformCmpxchg<*>::operator() and the
// InterlockedCompareExchange* API.

#define DEFINE_INTRINSIC_CMPXCHG(IntrinsicName, IntrinsicType)            \
  template<>                                                              \
  template<typename T>                                                    \
  inline T Atomic::PlatformCmpxchg<sizeof(IntrinsicType)>::operator()(T volatile* dest, \
                                                                      T compare_value, \
                                                                      T exchange_value, \
                                                                      atomic_memory_order order) const { \
    STATIC_ASSERT(sizeof(IntrinsicType) == sizeof(T));                    \
    return PrimitiveConversions::cast<T>(                                 \
      IntrinsicName(reinterpret_cast<IntrinsicType volatile *>(dest),     \
                    PrimitiveConversions::cast<IntrinsicType>(exchange_value), \
                    PrimitiveConversions::cast<IntrinsicType>(compare_value))); \
  }

DEFINE_INTRINSIC_CMPXCHG(_InterlockedCompareExchange8, char) // Use the intrinsic as InterlockedCompareExchange8 does not exist
DEFINE_INTRINSIC_CMPXCHG(InterlockedCompareExchange,   long)
DEFINE_INTRINSIC_CMPXCHG(InterlockedCompareExchange64, __int64)

#undef DEFINE_INTRINSIC_CMPXCHG

#ifndef AMD64

#pragma warning(disable: 4035) // Disables warnings reporting missing return statement

template<>
template<typename T>
inline T Atomic::PlatformLoad<8>::operator()(T const volatile* src) const {
  STATIC_ASSERT(8 == sizeof(T));
  volatile T dest;
  volatile T* pdest = &dest;
  __asm {
    mov eax, src
    fild     qword ptr [eax]
    mov eax, pdest
    fistp    qword ptr [eax]
  }
  return dest;
}

template<>
template<typename T>
inline void Atomic::PlatformStore<8>::operator()(T volatile* dest,
                                                 T store_value) const {
  STATIC_ASSERT(8 == sizeof(T));
  volatile T* src = &store_value;
  __asm {
    mov eax, src
    fild     qword ptr [eax]
    mov eax, dest
    fistp    qword ptr [eax]
  }
}

#pragma warning(default: 4035) // Enables warnings reporting missing return statement

template<>
struct Atomic::PlatformOrderedStore<1, RELEASE_X_FENCE>
{
  template <typename T>
  void operator()(volatile T* p, T v) const {
    __asm {
      mov edx, p;
      mov al, v;
      xchg al, byte ptr [edx];
    }
  }
};

template<>
struct Atomic::PlatformOrderedStore<2, RELEASE_X_FENCE>
{
  template <typename T>
  void operator()(volatile T* p, T v) const {
    __asm {
      mov edx, p;
      mov ax, v;
      xchg ax, word ptr [edx];
    }
  }
};

template<>
struct Atomic::PlatformOrderedStore<4, RELEASE_X_FENCE>
{
  template <typename T>
  void operator()(volatile T* p, T v) const {
    __asm {
      mov edx, p;
      mov eax, v;
      xchg eax, dword ptr [edx];
    }
  }
};
#endif // AMD64

#endif // OS_CPU_WINDOWS_X86_ATOMIC_WINDOWS_X86_HPP
