/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRBIGENDIAN_HPP
#define SHARE_JFR_UTILITIES_JFRBIGENDIAN_HPP

#include "memory/allocation.hpp"
#include "utilities/bytes.hpp"
#include "utilities/macros.hpp"

#ifndef VM_LITTLE_ENDIAN
# define bigendian_16(x) (x)
# define bigendian_32(x) (x)
# define bigendian_64(x) (x)
#else
# define bigendian_16(x) Bytes::swap_u2(x)
# define bigendian_32(x) Bytes::swap_u4(x)
# define bigendian_64(x) Bytes::swap_u8(x)
#endif

class JfrBigEndian : AllStatic {
 private:
  template <typename T>
  static T read_bytes(const address location);
  template <typename T>
  static T read_unaligned(const address location);
 public:
  static bool platform_supports_unaligned_reads(void);
  static bool is_aligned(const void* location, size_t size);
  template <typename T>
  static T read(const void* location);
};

inline bool JfrBigEndian::is_aligned(const void* location, size_t size) {
  assert(size <= sizeof(u8), "just checking");
  if (size == sizeof(u1)) {
    return true;
  }
  // check address alignment for datum access
  return (((uintptr_t)location & (size -1)) == 0);
}

template <>
inline u1 JfrBigEndian::read_bytes(const address location) {
  return (*location & 0xFF);
}

template <>
inline u2 JfrBigEndian::read_bytes(const address location) {
  return Bytes::get_Java_u2(location);
}

template <>
inline u4 JfrBigEndian::read_bytes(const address location) {
  return Bytes::get_Java_u4(location);
}

template <>
inline u8 JfrBigEndian::read_bytes(const address location) {
  return Bytes::get_Java_u8(location);
}

template <typename T>
inline T JfrBigEndian::read_unaligned(const address location) {
  assert(location != NULL, "just checking");
  switch (sizeof(T)) {
    case sizeof(u1) :
      return read_bytes<u1>(location);
    case sizeof(u2):
      return read_bytes<u2>(location);
    case sizeof(u4):
      return read_bytes<u4>(location);
    case sizeof(u8):
      return read_bytes<u8>(location);
    default:
      assert(false, "not reach");
  }
  return 0;
}

inline bool JfrBigEndian::platform_supports_unaligned_reads(void) {
#if defined(IA32) || defined(AMD64) || defined(PPC) || defined(S390)
  return true;
#elif defined(ARM) || defined(AARCH64)
  return false;
#else
  #warning "Unconfigured platform"
  return false;
#endif
}

template<typename T>
inline T JfrBigEndian::read(const void* location) {
  assert(location != NULL, "just checking");
  assert(sizeof(T) <= sizeof(u8), "no support for arbitrary sizes");
  if (sizeof(T) == sizeof(u1)) {
    return *(T*)location;
  }
  if (is_aligned(location, sizeof(T)) || platform_supports_unaligned_reads()) {
    // fastest case
    switch (sizeof(T)) {
      case sizeof(u1):
        return *(T*)location;
      case sizeof(u2):
        return bigendian_16(*(T*)(location));
      case sizeof(u4):
        return bigendian_32(*(T*)(location));
      case sizeof(u8):
        return bigendian_64(*(T*)(location));
    }
  }
  return read_unaligned<T>((const address)location);
}

#endif // SHARE_JFR_UTILITIES_JFRBIGENDIAN_HPP
