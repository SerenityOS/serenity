/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_LINUX_S390_ATOMIC_LINUX_S390_HPP
#define OS_CPU_LINUX_S390_ATOMIC_LINUX_S390_HPP

#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"

// Note that the compare-and-swap instructions on System z perform
// a serialization function before the storage operand is fetched
// and again after the operation is completed.
//
// Used constraint modifiers:
// = write-only access: Value on entry to inline-assembler code irrelevant.
// + read/write access: Value on entry is used; on exit value is changed.
//   read-only  access: Value on entry is used and never changed.
// & early-clobber access: Might be modified before all read-only operands
//                         have been used.
// a address register operand (not GR0).
// d general register operand (including GR0)
// Q memory operand w/o index register.
// 0..9 operand reference (by operand position).
//      Used for operands that fill multiple roles. One example would be a
//      write-only operand receiving its initial value from a read-only operand.
//      Refer to cmpxchg(..) operand #0 and variable cmp_val for a real-life example.
//

// On System z, all store operations are atomic if the address where the data is stored into
// is an integer multiple of the data length. Furthermore, all stores are ordered:
// a store which occurs conceptually before another store becomes visible to other CPUs
// before the other store becomes visible.

//------------
// Atomic::add
//------------
// These methods force the value in memory to be augmented by the passed increment.
// Both, memory value and increment, are treated as 32bit signed binary integers.
// No overflow exceptions are recognized, and the condition code does not hold
// information about the value in memory.
//
// The value in memory is updated by using a compare-and-swap instruction. The
// instruction is retried as often as required.
//
// The return value of the method is the value that was successfully stored. At the
// time the caller receives back control, the value in memory may have changed already.

// New atomic operations only include specific-operand-serialization, not full
// memory barriers. We can use the Fast-BCR-Serialization Facility for them.
inline void z196_fast_sync() {
  __asm__ __volatile__ ("bcr 14, 0" : : : "memory");
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
inline D Atomic::PlatformAdd<4>::add_and_fetch(D volatile* dest, I inc,
                                               atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(I));
  STATIC_ASSERT(4 == sizeof(D));

  D old, upd;

  if (VM_Version::has_LoadAndALUAtomicV1()) {
    if (order == memory_order_conservative) { z196_fast_sync(); }
    __asm__ __volatile__ (
      "   LGFR     0,%[inc]                \n\t" // save increment
      "   LA       3,%[mem]                \n\t" // force data address into ARG2
//    "   LAA      %[upd],%[inc],%[mem]    \n\t" // increment and get old value
//    "   LAA      2,0,0(3)                \n\t" // actually coded instruction
      "   .byte    0xeb                    \n\t" // LAA main opcode
      "   .byte    0x20                    \n\t" // R1,R3
      "   .byte    0x30                    \n\t" // R2,disp1
      "   .byte    0x00                    \n\t" // disp2,disp3
      "   .byte    0x00                    \n\t" // disp4,disp5
      "   .byte    0xf8                    \n\t" // LAA minor opcode
      "   AR       2,0                     \n\t" // calc new value in register
      "   LR       %[upd],2                \n\t" // move to result register
      //---<  outputs  >---
      : [upd]  "=&d" (upd)    // write-only, updated counter value
      , [mem]  "+Q"  (*dest)  // read/write, memory to be updated atomically
      //---<  inputs  >---
      : [inc]  "a"   (inc)    // read-only.
      //---<  clobbered  >---
      : "cc", "r0", "r2", "r3", "memory"
    );
    if (order == memory_order_conservative) { z196_fast_sync(); }
  } else {
    __asm__ __volatile__ (
      "   LLGF     %[old],%[mem]           \n\t" // get old value
      "0: LA       %[upd],0(%[inc],%[old]) \n\t" // calc result
      "   CS       %[old],%[upd],%[mem]    \n\t" // try to xchg res with mem
      "   JNE      0b                      \n\t" // no success? -> retry
      //---<  outputs  >---
      : [old] "=&a" (old)    // write-only, old counter value
      , [upd] "=&d" (upd)    // write-only, updated counter value
      , [mem] "+Q"  (*dest)  // read/write, memory to be updated atomically
      //---<  inputs  >---
      : [inc] "a"   (inc)    // read-only.
      //---<  clobbered  >---
      : "cc", "memory"
    );
  }

  return upd;
}


template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<8>::add_and_fetch(D volatile* dest, I inc,
                                               atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(I));
  STATIC_ASSERT(8 == sizeof(D));

  D old, upd;

  if (VM_Version::has_LoadAndALUAtomicV1()) {
    if (order == memory_order_conservative) { z196_fast_sync(); }
    __asm__ __volatile__ (
      "   LGR      0,%[inc]                \n\t" // save increment
      "   LA       3,%[mem]                \n\t" // force data address into ARG2
//    "   LAAG     %[upd],%[inc],%[mem]    \n\t" // increment and get old value
//    "   LAAG     2,0,0(3)                \n\t" // actually coded instruction
      "   .byte    0xeb                    \n\t" // LAA main opcode
      "   .byte    0x20                    \n\t" // R1,R3
      "   .byte    0x30                    \n\t" // R2,disp1
      "   .byte    0x00                    \n\t" // disp2,disp3
      "   .byte    0x00                    \n\t" // disp4,disp5
      "   .byte    0xe8                    \n\t" // LAA minor opcode
      "   AGR      2,0                     \n\t" // calc new value in register
      "   LGR      %[upd],2                \n\t" // move to result register
      //---<  outputs  >---
      : [upd]  "=&d" (upd)    // write-only, updated counter value
      , [mem]  "+Q"  (*dest)  // read/write, memory to be updated atomically
      //---<  inputs  >---
      : [inc]  "a"   (inc)    // read-only.
      //---<  clobbered  >---
      : "cc", "r0", "r2", "r3", "memory"
    );
    if (order == memory_order_conservative) { z196_fast_sync(); }
  } else {
    __asm__ __volatile__ (
      "   LG       %[old],%[mem]           \n\t" // get old value
      "0: LA       %[upd],0(%[inc],%[old]) \n\t" // calc result
      "   CSG      %[old],%[upd],%[mem]    \n\t" // try to xchg res with mem
      "   JNE      0b                      \n\t" // no success? -> retry
      //---<  outputs  >---
      : [old] "=&a" (old)    // write-only, old counter value
      , [upd] "=&d" (upd)    // write-only, updated counter value
      , [mem] "+Q"  (*dest)  // read/write, memory to be updated atomically
      //---<  inputs  >---
      : [inc] "a"   (inc)    // read-only.
      //---<  clobbered  >---
      : "cc", "memory"
    );
  }

  return upd;
}


//-------------
// Atomic::xchg
//-------------
// These methods force the value in memory to be replaced by the new value passed
// in as argument.
//
// The value in memory is replaced by using a compare-and-swap instruction. The
// instruction is retried as often as required. This makes sure that the new
// value can be seen, at least for a very short period of time, by other CPUs.
//
// If we would use a normal "load(old value) store(new value)" sequence,
// the new value could be lost unnoticed, due to a store(new value) from
// another thread.
//
// The return value is the (unchanged) value from memory as it was when the
// replacement succeeded.
template<>
template<typename T>
inline T Atomic::PlatformXchg<4>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order unused) const {
  STATIC_ASSERT(4 == sizeof(T));
  T old;

  __asm__ __volatile__ (
    "   LLGF     %[old],%[mem]           \n\t" // get old value
    "0: CS       %[old],%[upd],%[mem]    \n\t" // try to xchg upd with mem
    "   JNE      0b                      \n\t" // no success? -> retry
    //---<  outputs  >---
    : [old] "=&d" (old)      // write-only, prev value irrelevant
    , [mem] "+Q"  (*dest)    // read/write, memory to be updated atomically
    //---<  inputs  >---
    : [upd] "d"   (exchange_value) // read-only, value to be written to memory
    //---<  clobbered  >---
    : "cc", "memory"
  );

  return old;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<8>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order unused) const {
  STATIC_ASSERT(8 == sizeof(T));
  T old;

  __asm__ __volatile__ (
    "   LG       %[old],%[mem]           \n\t" // get old value
    "0: CSG      %[old],%[upd],%[mem]    \n\t" // try to xchg upd with mem
    "   JNE      0b                      \n\t" // no success? -> retry
    //---<  outputs  >---
    : [old] "=&d" (old)      // write-only, init from memory
    , [mem] "+Q"  (*dest)    // read/write, memory to be updated atomically
    //---<  inputs  >---
    : [upd] "d"   (exchange_value) // read-only, value to be written to memory
    //---<  clobbered  >---
    : "cc", "memory"
  );

  return old;
}

//----------------
// Atomic::cmpxchg
//----------------
// These methods compare the value in memory with a given compare value.
// If both values compare equal, the value in memory is replaced with
// the exchange value.
//
// The value in memory is compared and replaced by using a compare-and-swap
// instruction. The instruction is NOT retried (one shot only).
//
// The return value is the (unchanged) value from memory as it was when the
// compare-and-swap instruction completed. A successful exchange operation
// is indicated by (return value == compare_value). If unsuccessful, a new
// exchange value can be calculated based on the return value which is the
// latest contents of the memory location.
//
// Inspecting the return value is the only way for the caller to determine
// if the compare-and-swap instruction was successful:
// - If return value and compare value compare equal, the compare-and-swap
//   instruction was successful and the value in memory was replaced by the
//   exchange value.
// - If return value and compare value compare unequal, the compare-and-swap
//   instruction was not successful. The value in memory was left unchanged.
//
// The s390 processors always fence before and after the csg instructions.
// Thus we ignore the memory ordering argument. The docu says: "A serialization
// function is performed before the operand is fetched and again after the
// operation is completed."

// No direct support for cmpxchg of bytes; emulate using int.
template<>
struct Atomic::PlatformCmpxchg<1> : Atomic::CmpxchgByteUsingInt {};

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T cmp_val,
                                                T xchg_val,
                                                atomic_memory_order unused) const {
  STATIC_ASSERT(4 == sizeof(T));
  T old;

  __asm__ __volatile__ (
    "   CS       %[old],%[upd],%[mem]    \n\t" // Try to xchg upd with mem.
    // outputs
    : [old] "=&d" (old)      // Write-only, prev value irrelevant.
    , [mem] "+Q"  (*dest)    // Read/write, memory to be updated atomically.
    // inputs
    : [upd] "d"   (xchg_val)
    ,       "0"   (cmp_val)  // Read-only, initial value for [old] (operand #0).
    // clobbered
    : "cc", "memory"
  );

  return old;
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<8>::operator()(T volatile* dest,
                                                T cmp_val,
                                                T xchg_val,
                                                atomic_memory_order unused) const {
  STATIC_ASSERT(8 == sizeof(T));
  T old;

  __asm__ __volatile__ (
    "   CSG      %[old],%[upd],%[mem]    \n\t" // Try to xchg upd with mem.
    // outputs
    : [old] "=&d" (old)      // Write-only, prev value irrelevant.
    , [mem] "+Q"  (*dest)    // Read/write, memory to be updated atomically.
    // inputs
    : [upd] "d"   (xchg_val)
    ,       "0"   (cmp_val)  // Read-only, initial value for [old] (operand #0).
    // clobbered
    : "cc", "memory"
  );

  return old;
}

template<size_t byte_size>
struct Atomic::PlatformOrderedLoad<byte_size, X_ACQUIRE>
{
  template <typename T>
  T operator()(const volatile T* p) const { T t = *p; OrderAccess::acquire(); return t; }
};

#endif // OS_CPU_LINUX_S390_ATOMIC_LINUX_S390_HPP
