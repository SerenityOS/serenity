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

#ifndef OS_CPU_BSD_X86_BYTES_BSD_X86_HPP
#define OS_CPU_BSD_X86_BYTES_BSD_X86_HPP

#ifdef __APPLE__
#include <libkern/OSByteOrder.h>
#endif

#if defined(AMD64)
#  if defined(__APPLE__)
#    define bswap_16(x) OSSwapInt16(x)
#    define bswap_32(x) OSSwapInt32(x)
#    define bswap_64(x) OSSwapInt64(x)
#  elif defined(__OpenBSD__)
#    define bswap_16(x) swap16(x)
#    define bswap_32(x) swap32(x)
#    define bswap_64(x) swap64(x)
#  elif defined(__NetBSD__)
#    define bswap_16(x) bswap16(x)
#    define bswap_32(x) bswap32(x)
#    define bswap_64(x) bswap64(x)
#  else
#    define bswap_16(x) __bswap16(x)
#    define bswap_32(x) __bswap32(x)
#    define bswap_64(x) __bswap64(x)
#  endif
#endif

// Efficient swapping of data bytes from Java byte
// ordering to native byte ordering and vice versa.
inline u2   Bytes::swap_u2(u2 x) {
#ifdef AMD64
  return bswap_16(x);
#else
  u2 ret;
  __asm__ __volatile__ (
    "movw %0, %%ax;"
    "xchg %%al, %%ah;"
    "movw %%ax, %0"
    :"=r" (ret)      // output : register 0 => ret
    :"0"  (x)        // input  : x => register 0
    :"ax", "0"       // clobbered registers
  );
  return ret;
#endif // AMD64
}

inline u4   Bytes::swap_u4(u4 x) {
#ifdef AMD64
  return bswap_32(x);
#else
  u4 ret;
  __asm__ __volatile__ (
    "bswap %0"
    :"=r" (ret)      // output : register 0 => ret
    :"0"  (x)        // input  : x => register 0
    :"0"             // clobbered register
  );
  return ret;
#endif // AMD64
}

#ifdef AMD64
inline u8 Bytes::swap_u8(u8 x) {
  return bswap_64(x);
}
#else
// Helper function for swap_u8
inline u8   Bytes::swap_u8_base(u4 x, u4 y) {
  return (((u8)swap_u4(x))<<32) | swap_u4(y);
}

inline u8 Bytes::swap_u8(u8 x) {
  return swap_u8_base(*(u4*)&x, *(((u4*)&x)+1));
}
#endif // !AMD64

#endif // OS_CPU_BSD_X86_BYTES_BSD_X86_HPP
