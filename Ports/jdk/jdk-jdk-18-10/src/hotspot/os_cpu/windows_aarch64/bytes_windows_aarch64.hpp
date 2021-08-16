/*
 * Copyright (c) 2020, Microsoft Corporation. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_AARCH64_BYTES_WINDOWS_AARCH64_HPP
#define OS_CPU_WINDOWS_AARCH64_BYTES_WINDOWS_AARCH64_HPP

#include <stdlib.h>

// Efficient swapping of data bytes from Java byte
// ordering to native byte ordering and vice versa.
inline u2   Bytes::swap_u2(u2 x) {
  return _byteswap_ushort(x);
}

inline u4   Bytes::swap_u4(u4 x) {
  return _byteswap_ulong(x);
}

inline u8 Bytes::swap_u8(u8 x) {
  return _byteswap_uint64(x);
}

#pragma warning(default: 4035) // Enable warning 4035: no return value

#endif // OS_CPU_WINDOWS_AARCH64_BYTES_WINDOWS_AARCH64_HPP
