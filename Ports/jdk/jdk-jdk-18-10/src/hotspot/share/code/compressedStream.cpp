/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "code/compressedStream.hpp"
#include "utilities/ostream.hpp"

// 32-bit self-inverse encoding of float bits
// converts trailing zeroes (common in floats) to leading zeroes
inline juint CompressedStream::reverse_int(juint i) {
  // Hacker's Delight, Figure 7-1
  i = (i & 0x55555555) << 1 | ((i >> 1) & 0x55555555);
  i = (i & 0x33333333) << 2 | ((i >> 2) & 0x33333333);
  i = (i & 0x0f0f0f0f) << 4 | ((i >> 4) & 0x0f0f0f0f);
  i = (i << 24) | ((i & 0xff00) << 8) | ((i >> 8) & 0xff00) | (i >> 24);
  return i;
}

jint CompressedReadStream::read_signed_int() {
  return decode_sign(read_int());
}

// Compressing floats is simple, because the only common pattern
// is trailing zeroes.  (Compare leading sign bits on ints.)
// Since floats are left-justified, as opposed to right-justified
// ints, we can bit-reverse them in order to take advantage of int
// compression.

jfloat CompressedReadStream::read_float() {
  int rf = read_int();
  int f  = reverse_int(rf);
  return jfloat_cast(f);
}

jdouble CompressedReadStream::read_double() {
  jint rh = read_int();
  jint rl = read_int();
  jint h  = reverse_int(rh);
  jint l  = reverse_int(rl);
  return jdouble_cast(jlong_from(h, l));
}

jlong CompressedReadStream::read_long() {
  jint low  = read_signed_int();
  jint high = read_signed_int();
  return jlong_from(high, low);
}

CompressedWriteStream::CompressedWriteStream(int initial_size) : CompressedStream(NULL, 0) {
  _buffer   = NEW_RESOURCE_ARRAY(u_char, initial_size);
  _size     = initial_size;
  _position = 0;
}

void CompressedWriteStream::grow() {
  u_char* _new_buffer = NEW_RESOURCE_ARRAY(u_char, _size * 2);
  memcpy(_new_buffer, _buffer, _position);
  _buffer = _new_buffer;
  _size   = _size * 2;
}

void CompressedWriteStream::write_float(jfloat value) {
  juint f = jint_cast(value);
  juint rf = reverse_int(f);
  assert(f == reverse_int(rf), "can re-read same bits");
  write_int(rf);
}

void CompressedWriteStream::write_double(jdouble value) {
  juint h  = high(jlong_cast(value));
  juint l  = low( jlong_cast(value));
  juint rh = reverse_int(h);
  juint rl = reverse_int(l);
  assert(h == reverse_int(rh), "can re-read same bits");
  assert(l == reverse_int(rl), "can re-read same bits");
  write_int(rh);
  write_int(rl);
}

void CompressedWriteStream::write_long(jlong value) {
  write_signed_int(low(value));
  write_signed_int(high(value));
}
