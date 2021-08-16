/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRENCODERS_HPP
#define SHARE_JFR_WRITERS_JFRENCODERS_HPP

#include "memory/allocation.hpp"
#include "utilities/bytes.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

//
// The Encoding policy prescribes a template
// method taking a first parameter of type T.
// This is the value to be encoded. The second
// parameter is a memory address - where to write
// the encoded value.
// The encoder method(s) should return the
// number of bytes encoded into that memory address.
//
// template <typename T>
// size_t encoder(T value, u1* dest);
//
// The caller ensures the destination
// address is not null and that T can be fitted
// in encoded form.
//

// Encoding policy classes

class BigEndianEncoderImpl {
 public:
  template <typename T>
  static size_t encode(T value, u1* dest);

  template <typename T>
  static size_t encode(const T* src, size_t len, u1* dest);

  template <typename T>
  static size_t encode_padded(T value, u1* dest);

  template <typename T>
  static size_t encode_padded(const T* src, size_t len, u1* dest);

};

template <typename T>
inline size_t BigEndianEncoderImpl::encode(T value, u1* dest) {
  assert(dest != NULL, "invariant");
  switch (sizeof(T)) {
    case 1: {
      ShouldNotReachHere();
       return 0;
     }
     case 2: {
       Bytes::put_Java_u2(dest, value);
       return 2;
     }
     case 4: {
       Bytes::put_Java_u4(dest, value);
       return 4;
     }
     case 8: {
       Bytes::put_Java_u8(dest, value);
       return 8;
     }
  }
  ShouldNotReachHere();
  return 0;
}

template <typename T>
inline size_t BigEndianEncoderImpl::encode(const T* src, size_t len, u1* dest) {
  assert(dest != NULL, "invariant");
  assert(len >= 1, "invariant");
  if (1 == sizeof(T)) {
    memcpy(dest, src, len);
    return len;
  }
  size_t size = encode(*src, dest);
  if (len > 1) {
    for (size_t i = 1; i < len; ++i) {
      size += encode(*(src + i), dest + size);
    }
  }
  return size;
}

template <typename T>
inline size_t BigEndianEncoderImpl::encode_padded(T value, u1* dest) {
  return encode(value, dest);
}

template <typename T>
inline size_t BigEndianEncoderImpl::encode_padded(const T* src, size_t len, u1* dest) {
  assert(dest != NULL, "invariant");
  assert(len >= 1, "invariant");
  if (1 == sizeof(T)) {
    memcpy(dest, src, len);
    return len;
  }
  size_t size = encode_padded(*src, dest);
  if (len > 1) {
    for (size_t i = 1; i < len; ++i) {
      size += encode_padded(*(src + i), dest + size);
    }
  }
  return size;
}


// The Varint128 encoder implements encoding according to
// msb(it) 128bit encoding (1 encode bit | 7 value bits),
// using least significant byte order.
//
// Example (little endian platform):
// Value: 25674
// Binary: 00000000 0000000 01100100 01001010
// Varint encoded (3 bytes):
// Value: 13289473
// Varint encoded: 11001010 11001000 00000001
//

class Varint128EncoderImpl {
 private:
  template <typename T>
  static u8 to_u8(T value);

 public:
  template <typename T>
  static size_t encode(T value, u1* dest);

  template <typename T>
  static size_t encode(const T* src, size_t len, u1* dest);

  template <typename T>
  static size_t encode_padded(T value, u1* dest);

  template <typename T>
  static size_t encode_padded(const T* src, size_t len, u1* dest);

};

template <typename T>
inline u8 Varint128EncoderImpl::to_u8(T value) {
  switch(sizeof(T)) {
    case 1:
     return static_cast<u8>(static_cast<u1>(value) & static_cast<u1>(0xff));
    case 2:
      return static_cast<u8>(static_cast<u2>(value) & static_cast<u2>(0xffff));
    case 4:
      return static_cast<u8>(static_cast<u4>(value) & static_cast<u4>(0xffffffff));
    case 8:
      return static_cast<u8>(value);
    default:
      fatal("unsupported type");
  }
  return 0;
}

static const u1 ext_bit = 0x80;
#define GREATER_THAN_OR_EQUAL_TO_128(v) (((u8)(~(ext_bit - 1)) & (v)))
#define LESS_THAN_128(v) !GREATER_THAN_OR_EQUAL_TO_128(v)

template <typename T>
inline size_t Varint128EncoderImpl::encode(T value, u1* dest) {
  assert(dest != NULL, "invariant");

  const u8 v = to_u8(value);

  if (LESS_THAN_128(v)) {
    *dest = static_cast<u1>(v); // set bit 0-6, no extension
    return 1;
  }
  *dest = static_cast<u1>(v | ext_bit); // set bit 0-6, with extension
  if (LESS_THAN_128(v >> 7)) {
    *(dest + 1) = static_cast<u1>(v >> 7); // set bit 7-13, no extension
    return 2;
  }
  *(dest + 1) = static_cast<u1>((v >> 7) | ext_bit); // set bit 7-13, with extension
  if (LESS_THAN_128(v >> 14)) {
    *(dest + 2) = static_cast<u1>(v >> 14); // set bit 14-20, no extension
    return 3;
  }
  *(dest + 2) = static_cast<u1>((v >> 14) | ext_bit); // set bit 14-20, with extension
  if (LESS_THAN_128(v >> 21)) {
    *(dest + 3) = static_cast<u1>(v >> 21); // set bit 21-27, no extension
    return 4;
  }
  *(dest + 3) = static_cast<u1>((v >> 21) | ext_bit); // set bit 21-27, with extension
  if (LESS_THAN_128(v >> 28)) {
    *(dest + 4) = static_cast<u1>(v >> 28); // set bit 28-34, no extension
    return 5;
  }
  *(dest + 4) = static_cast<u1>((v >> 28) | ext_bit); // set bit 28-34, with extension
  if (LESS_THAN_128(v >> 35)) {
    *(dest + 5) = static_cast<u1>(v >> 35); // set bit 35-41, no extension
    return 6;
  }
  *(dest + 5) = static_cast<u1>((v >> 35) | ext_bit); // set bit 35-41, with extension
  if (LESS_THAN_128(v >> 42)) {
    *(dest + 6) = static_cast<u1>(v >> 42); // set bit 42-48, no extension
    return 7;
  }
  *(dest + 6) = static_cast<u1>((v >> 42) | ext_bit); // set bit 42-48, with extension
  if (LESS_THAN_128(v >> 49)) {
    *(dest + 7) = static_cast<u1>(v >> 49); // set bit 49-55, no extension
    return 8;
  }
  *(dest + 7) = static_cast<u1>((v >> 49) | ext_bit); // set bit 49-55, with extension
  // no need to extend since only 64 bits allowed.
  *(dest + 8) = static_cast<u1>(v >> 56);  // set bit 56-63
  return 9;
}

template <typename T>
inline size_t Varint128EncoderImpl::encode(const T* src, size_t len, u1* dest) {
  assert(dest != NULL, "invariant");
  assert(len >= 1, "invariant");
  size_t size = encode(*src, dest);
  if (len > 1) {
    for (size_t i = 1; i < len; ++i) {
      size += encode(*(src + i), dest + size);
    }
  }
  return size;
}

template <typename T>
inline size_t Varint128EncoderImpl::encode_padded(T value, u1* dest) {
  assert(dest != NULL, "invariant");
  const u8 v = to_u8(value);
  switch (sizeof(T)) {
    case 1:
      dest[0] = static_cast<u1>(v);
      return 1;
    case 2:
      dest[0] = static_cast<u1>(v | 0x80);
      dest[1] = static_cast<u1>(v >> 7);
      return 2;
    case 4:
      dest[0] = static_cast<u1>(v | 0x80);
      dest[1] = static_cast<u1>(v >> 7 | 0x80);
      dest[2] = static_cast<u1>(v >> 14 | 0x80);
      dest[3] = static_cast<u1>(v >> 21);
      return 4;
    case 8:
      dest[0] = static_cast<u1>(v | 0x80);
      dest[1] = static_cast<u1>(v >> 7 | 0x80);
      dest[2] = static_cast<u1>(v >> 14 | 0x80);
      dest[3] = static_cast<u1>(v >> 21 | 0x80);
      dest[4] = static_cast<u1>(v >> 28 | 0x80);
      dest[5] = static_cast<u1>(v >> 35 | 0x80);
      dest[6] = static_cast<u1>(v >> 42 | 0x80);
      dest[7] = static_cast<u1>(v >> 49);
      return 8;
    default:
      ShouldNotReachHere();
    }
  return 0;
}


template <typename T>
inline size_t Varint128EncoderImpl::encode_padded(const T* src, size_t len, u1* dest) {
  assert(dest != NULL, "invariant");
  assert(len >= 1, "invariant");
  size_t size = encode_padded(*src, dest);
  if (len > 1) {
    for (size_t i = 1; i < len; ++i) {
      size += encode_padded(*(src + i), dest + size);
    }
  }
  return size;
}

#endif // SHARE_JFR_WRITERS_JFRENCODERS_HPP
