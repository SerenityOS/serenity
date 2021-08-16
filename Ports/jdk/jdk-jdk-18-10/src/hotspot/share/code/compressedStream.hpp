/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_COMPRESSEDSTREAM_HPP
#define SHARE_CODE_COMPRESSEDSTREAM_HPP

#include "memory/allocation.hpp"

// Simple interface for filing out and filing in basic types
// Used for writing out and reading in debugging information.

class CompressedStream : public ResourceObj {
  friend class VMStructs;
 protected:
  u_char* _buffer;
  int     _position;

  enum {
    // Constants for UNSIGNED5 coding of Pack200
    lg_H = 6, H = 1<<lg_H,    // number of high codes (64)
    L = (1<<BitsPerByte)-H,   // number of low codes (192)
    MAX_i = 4                 // bytes are numbered in (0..4), max 5 bytes
  };

  // 32-bit one-to-one sign encoding taken from Pack200
  // converts leading sign bits into leading zeroes with trailing sign bit
  static juint encode_sign(jint  value) { return (value << 1) ^ (value >> 31); }
  static jint  decode_sign(juint value) { return (value >> 1) ^ -(jint)(value & 1); }
  static juint reverse_int(juint i);   // to trim trailing float 0's
 public:
  CompressedStream(u_char* buffer, int position = 0) {
    _buffer   = buffer;
    _position = position;
  }

  u_char* buffer() const               { return _buffer; }

  // Positioning
  int position() const                 { return _position; }
  void set_position(int position)      { _position = position; }
};


class CompressedReadStream : public CompressedStream {
 private:
  inline u_char read()                 { return _buffer[_position++]; }

  // This encoding, called UNSIGNED5, is taken from J2SE Pack200.
  // It assumes that most values have lots of leading zeroes.
  // Very small values, in the range [0..191], code in one byte.
  // Any 32-bit value (including negatives) can be coded, in
  // up to five bytes.  The grammar is:
  //    low_byte  = [0..191]
  //    high_byte = [192..255]
  //    any_byte  = low_byte | high_byte
  //    coding = low_byte
  //           | high_byte low_byte
  //           | high_byte high_byte low_byte
  //           | high_byte high_byte high_byte low_byte
  //           | high_byte high_byte high_byte high_byte any_byte
  // Each high_byte contributes six bits of payload.
  // The encoding is one-to-one (except for integer overflow)
  // and easy to parse and unparse.

  jint read_int_mb(jint b0) {
    int     pos = position() - 1;
    u_char* buf = buffer() + pos;
    assert(buf[0] == b0 && b0 >= L, "correctly called");
    jint    sum = b0;
    // must collect more bytes:  b[1]...b[4]
    int lg_H_i = lg_H;
    for (int i = 0; ; ) {
      jint b_i = buf[++i]; // b_i = read(); ++i;
      sum += b_i << lg_H_i;  // sum += b[i]*(64**i)
      if (b_i < L || i == MAX_i) {
        set_position(pos+i+1);
        return sum;
      }
      lg_H_i += lg_H;
    }
  }

 public:
  CompressedReadStream(u_char* buffer, int position = 0)
  : CompressedStream(buffer, position) {}

  jboolean read_bool()                 { return (jboolean) read();      }
  jbyte    read_byte()                 { return (jbyte   ) read();      }
  jchar    read_char()                 { return (jchar   ) read_int();  }
  jshort   read_short()                { return (jshort  ) read_signed_int(); }
  jint     read_int()                  { jint   b0 = read();
                                         if (b0 < L)  return b0;
                                         else         return read_int_mb(b0);
                                       }
  jint     read_signed_int();
  jfloat   read_float();               // jfloat_cast(reverse_int(read_int()))
  jdouble  read_double();              // jdouble_cast(2*reverse_int(read_int))
  jlong    read_long();                // jlong_from(2*read_signed_int())
};


class CompressedWriteStream : public CompressedStream {
 private:
  bool full() {
    return _position >= _size;
  }
  void store(u_char b) {
    _buffer[_position++] = b;
  }
  void write(u_char b) {
    if (full()) grow();
    store(b);
  }
  void grow();

  // UNSIGNED5 coding, 1-5 byte cases
  void write_int_mb(jint value) {
    juint sum = value;
    for (int i = 0; ; ) {
      if (sum < L || i == MAX_i) {
        // remainder is either a "low code" or the 5th byte
        assert(sum == (u_char)sum, "valid byte");
        write((u_char)sum);
        break;
      }
      sum -= L;
      int b_i = L + (sum % H);  // this is a "high code"
      sum >>= lg_H;             // extracted 6 bits
      write(b_i); ++i;
    }
  }

 protected:
  int _size;

 public:
  CompressedWriteStream(int initial_size);
  CompressedWriteStream(u_char* buffer, int initial_size, int position = 0)
  : CompressedStream(buffer, position) { _size = initial_size; }

  void write_bool(jboolean value)      { write(value);      }
  void write_byte(jbyte value)         { write(value);      }
  void write_char(jchar value)         { write_int(value); }
  void write_short(jshort value)       { write_signed_int(value);  }
  void write_int(jint value)           { if ((juint)value < L && !full())
                                               store((u_char)value);
                                         else  write_int_mb(value);  }
  void write_signed_int(jint value)    { write_int(encode_sign(value)); }
  void write_float(jfloat value);      // write_int(reverse_int(jint_cast(v)))
  void write_double(jdouble value);    // write_int(reverse_int(<low,high>))
  void write_long(jlong value);        // write_signed_int(<low,high>)
};

#endif // SHARE_CODE_COMPRESSEDSTREAM_HPP
