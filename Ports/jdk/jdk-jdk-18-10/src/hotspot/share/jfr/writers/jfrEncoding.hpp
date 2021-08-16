/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRENCODING_HPP
#define SHARE_JFR_WRITERS_JFRENCODING_HPP

#include "jfr/writers/jfrEncoders.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

enum JfrStringEncoding {
  NULL_STRING = 0,
  EMPTY_STRING,
  STRING_CONSTANT,
  UTF8,
  UTF16,
  LATIN1,
  NOF_STRING_ENCODINGS
};

template <typename IntegerEncoder, typename BaseEncoder>
class EncoderHost : public AllStatic {
 public:
  template <typename T>
  static u1* be_write(T value, u1* pos) {
    return be_write(&value, 1, pos);
  }

  template <typename T>
  static u1* be_write(const T* value, size_t len, u1* pos) {
    assert(value != NULL, "invariant");
    assert(pos != NULL, "invariant");
    assert(len > 0, "invariant");
    return pos + BaseEncoder::encode(value, len, pos);
  }

  template <typename T>
  static u1* write_padded(T value, u1* pos) {
    assert(pos != NULL, "invariant");
    return write_padded(&value, 1, pos);
  }

  template <typename T>
  static u1* write_padded(const T* value, size_t len, u1* pos) {
    assert(value != NULL, "invariant");
    assert(pos != NULL, "invariant");
    assert(len > 0, "invariant");
    return pos + IntegerEncoder::encode_padded(value, len, pos);
  }

  template <typename T>
  static u1* write(T value, u1* pos) {
    return write(&value, 1, pos);
  }

  template <typename T>
  static u1* write(const T* value, size_t len, u1* pos) {
    assert(value != NULL, "invariant");
    assert(pos != NULL, "invariant");
    assert(len > 0, "invariant");
    return pos + IntegerEncoder::encode(value, len, pos);
  }

  static u1* write(bool value, u1* pos) {
    return be_write((u1)value, pos);
  }

  static u1* write(float value, u1* pos) {
    return be_write(*(u4*)&(value), pos);
  }

  static u1* write(double value, u1* pos) {
    return be_write(*(u8*)&(value), pos);
  }

  static u1* write(const char* value, u1* pos) {
    u2 len = 0;
    if (value != NULL) {
      len = MIN2<u2>(max_jushort, (jushort)strlen(value));
    }
    pos = write(len, pos);
    if (len > 0) {
      pos = be_write(value, len, pos);
    }
    return pos;
  }

  static u1* write(char* value, u1* pos) {
    return write(const_cast<const char*>(value), pos);
  }
};

typedef EncoderHost<BigEndianEncoderImpl, BigEndianEncoderImpl> BigEndianEncoder;
typedef EncoderHost<Varint128EncoderImpl, BigEndianEncoderImpl> CompressedIntegerEncoder;

#endif // SHARE_JFR_WRITERS_JFRENCODING_HPP
