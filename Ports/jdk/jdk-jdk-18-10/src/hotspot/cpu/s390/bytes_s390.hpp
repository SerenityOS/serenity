/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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

#ifndef CPU_S390_BYTES_S390_HPP
#define CPU_S390_BYTES_S390_HPP

#include "memory/allocation.hpp"

class Bytes: AllStatic {
 public:
  // Efficient reading and writing of unaligned unsigned data in
  // platform-specific byte ordering.

  // Use regular load and store for unaligned access.
  //
  // On z/Architecture, unaligned loads and stores are supported when using the
  // "traditional" load (LH, L/LY, LG) and store (STH, ST/STY, STG) instructions.
  // The penalty for unaligned access is just very few (two or three) ticks,
  // plus another few (two or three) ticks if the access crosses a cache line boundary.
  //
  // In short, it makes no sense on z/Architecture to piecemeal get or put unaligned data.

  static inline u2   get_native_u2(address p) { return *(u2*)p; }
  static inline u4   get_native_u4(address p) { return *(u4*)p; }
  static inline u8   get_native_u8(address p) { return *(u8*)p; }

  static inline void put_native_u2(address p, u2 x) { *(u2*)p = x; }
  static inline void put_native_u4(address p, u4 x) { *(u4*)p = x; }
  static inline void put_native_u8(address p, u8 x) { *(u8*)p = x; }

  // The following header contains the implementations of swap_u2, swap_u4, and swap_u8.
#include OS_CPU_HEADER(bytes)

  // Efficient reading and writing of unaligned unsigned data in Java byte ordering (i.e. big-endian ordering)
  static inline u2   get_Java_u2(address p) { return get_native_u2(p); }
  static inline u4   get_Java_u4(address p) { return get_native_u4(p); }
  static inline u8   get_Java_u8(address p) { return get_native_u8(p); }

  static inline void put_Java_u2(address p, u2 x) { put_native_u2(p, x); }
  static inline void put_Java_u4(address p, u4 x) { put_native_u4(p, x); }
  static inline void put_Java_u8(address p, u8 x) { put_native_u8(p, x); }
};

#endif // CPU_S390_BYTES_S390_HPP
