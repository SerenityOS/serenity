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

#ifndef CPU_ARM_BYTES_ARM_HPP
#define CPU_ARM_BYTES_ARM_HPP

#include "memory/allocation.hpp"
#include "utilities/macros.hpp"

#ifndef VM_LITTLE_ENDIAN
#define VM_LITTLE_ENDIAN  1
#endif

class Bytes: AllStatic {

 public:
  static inline u2 get_Java_u2(address p) {
    return (u2(p[0]) << 8) | u2(p[1]);
  }

  static inline u4 get_Java_u4(address p) {
    return u4(p[0]) << 24 |
           u4(p[1]) << 16 |
           u4(p[2]) <<  8 |
           u4(p[3]);
  }

  static inline u8 get_Java_u8(address p) {
    return u8(p[0]) << 56 |
           u8(p[1]) << 48 |
           u8(p[2]) << 40 |
           u8(p[3]) << 32 |
           u8(p[4]) << 24 |
           u8(p[5]) << 16 |
           u8(p[6]) <<  8 |
           u8(p[7]);
  }

  static inline void put_Java_u2(address p, u2 x) {
    p[0] = x >> 8;
    p[1] = x;
  }

  static inline void put_Java_u4(address p, u4 x) {
    ((u1*)p)[0] = x >> 24;
    ((u1*)p)[1] = x >> 16;
    ((u1*)p)[2] = x >>  8;
    ((u1*)p)[3] = x;
  }

  static inline void put_Java_u8(address p, u8 x) {
    ((u1*)p)[0] = x >> 56;
    ((u1*)p)[1] = x >> 48;
    ((u1*)p)[2] = x >> 40;
    ((u1*)p)[3] = x >> 32;
    ((u1*)p)[4] = x >> 24;
    ((u1*)p)[5] = x >> 16;
    ((u1*)p)[6] = x >>  8;
    ((u1*)p)[7] = x;
  }

#ifdef VM_LITTLE_ENDIAN

  static inline u2 get_native_u2(address p) {
    return (intptr_t(p) & 1) == 0 ? *(u2*)p : u2(p[0]) | (u2(p[1]) << 8);
  }

  static inline u4 get_native_u4(address p) {
    switch (intptr_t(p) & 3) {
      case 0:  return *(u4*)p;
      case 2:  return u4(((u2*)p)[0]) |
                      u4(((u2*)p)[1]) << 16;
      default: return u4(p[0])       |
                      u4(p[1]) <<  8 |
                      u4(p[2]) << 16 |
                      u4(p[3]) << 24;
    }
  }

  static inline u8 get_native_u8(address p) {
    switch (intptr_t(p) & 7) {
      case 0:  return *(u8*)p;
      case 4:  return u8(((u4*)p)[0]) |
                      u8(((u4*)p)[1]) << 32;
      case 2:  return u8(((u2*)p)[0])       |
                      u8(((u2*)p)[1]) << 16 |
                      u8(((u2*)p)[2]) << 32 |
                      u8(((u2*)p)[3]) << 48;
      default: return u8(p[0])       |
                      u8(p[1]) <<  8 |
                      u8(p[2]) << 16 |
                      u8(p[3]) << 24 |
                      u8(p[4]) << 32 |
                      u8(p[5]) << 40 |
                      u8(p[6]) << 48 |
                      u8(p[7]) << 56;
    }
  }

  static inline void put_native_u2(address p, u2 x) {
    if ((intptr_t(p) & 1) == 0) {
      *(u2*)p = x;
    } else {
      p[0] = x;
      p[1] = x >> 8;
    }
  }

  static inline void put_native_u4(address p, u4 x) {
    switch (intptr_t(p) & 3) {
      case 0:  *(u4*)p = x;
               break;
      case 2:  ((u2*)p)[0] = x;
               ((u2*)p)[1] = x >> 16;
               break;
      default: ((u1*)p)[0] = x;
               ((u1*)p)[1] = x >>  8;
               ((u1*)p)[2] = x >> 16;
               ((u1*)p)[3] = x >> 24;
               break;
    }
  }

  static inline void put_native_u8(address p, u8 x) {
    switch (intptr_t(p) & 7) {
      case 0:  *(u8*)p = x;
               break;
      case 4:  ((u4*)p)[0] = x;
               ((u4*)p)[1] = x >> 32;
               break;
      case 2:  ((u2*)p)[0] = x;
               ((u2*)p)[1] = x >> 16;
               ((u2*)p)[2] = x >> 32;
               ((u2*)p)[3] = x >> 48;
               break;
      default: ((u1*)p)[0] = x;
               ((u1*)p)[1] = x >>  8;
               ((u1*)p)[2] = x >> 16;
               ((u1*)p)[3] = x >> 24;
               ((u1*)p)[4] = x >> 32;
               ((u1*)p)[5] = x >> 40;
               ((u1*)p)[6] = x >> 48;
               ((u1*)p)[7] = x >> 56;
    }
  }

#else

  static inline u2 get_native_u2(address p) { return get_Java_u2(p); }
  static inline u4 get_native_u4(address p) { return get_Java_u4(p); }
  static inline u8 get_native_u8(address p) { return get_Java_u8(p); }
  static inline void put_native_u2(address p, u2 x) { put_Java_u2(p, x); }
  static inline void put_native_u4(address p, u4 x) { put_Java_u4(p, x); }
  static inline void put_native_u8(address p, u8 x) { put_Java_u8(p, x); }

#endif // VM_LITTLE_ENDIAN

  // Efficient swapping of byte ordering
  static inline u2 swap_u2(u2 x);
  static inline u4 swap_u4(u4 x);
  static inline u8 swap_u8(u8 x);
};


// The following header contains the implementations of swap_u2, swap_u4, and swap_u8
#include OS_CPU_HEADER(bytes)

#endif // CPU_ARM_BYTES_ARM_HPP
