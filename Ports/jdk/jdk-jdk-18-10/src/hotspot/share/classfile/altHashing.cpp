/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * halfsiphash code adapted from reference implementation
 * (https://github.com/veorq/SipHash/blob/master/halfsiphash.c)
 * which is distributed with the following copyright:
 *
 * SipHash reference C implementation
 *
 * Copyright (c) 2016 Jean-Philippe Aumasson <jeanphilippe.aumasson@gmail.com>
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain
 * worldwide. This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "precompiled.hpp"
#include "classfile/altHashing.hpp"
#include "classfile/vmClasses.hpp"
#include "oops/klass.inline.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/os.hpp"

// Get the hash code of the classes mirror if it exists, otherwise just
// return a random number, which is one of the possible hash code used for
// objects.  We don't want to call the synchronizer hash code to install
// this value because it may safepoint.
static intptr_t object_hash(Klass* k) {
  intptr_t hc = k->java_mirror()->mark().hash();
  return hc != markWord::no_hash ? hc : os::random();
}

// Seed value used for each alternative hash calculated.
uint64_t AltHashing::compute_seed() {
  uint64_t nanos = os::javaTimeNanos();
  uint64_t now = os::javaTimeMillis();
  uint32_t SEED_MATERIAL[8] = {
            (uint32_t) object_hash(vmClasses::String_klass()),
            (uint32_t) object_hash(vmClasses::System_klass()),
            (uint32_t) os::random(),  // current thread isn't a java thread
            (uint32_t) (((uint64_t)nanos) >> 32),
            (uint32_t) nanos,
            (uint32_t) (((uint64_t)now) >> 32),
            (uint32_t) now,
            (uint32_t) (os::javaTimeNanos() >> 2)
  };

  return halfsiphash_64(SEED_MATERIAL, 8);
}

// utility function copied from java/lang/Integer
static uint32_t Integer_rotateLeft(uint32_t i, int distance) {
  return (i << distance) | (i >> (32 - distance));
}

static void halfsiphash_rounds(uint32_t v[4], int rounds) {
  while (rounds-- > 0) {
    v[0] += v[1];
    v[1] = Integer_rotateLeft(v[1], 5);
    v[1] ^= v[0];
    v[0] = Integer_rotateLeft(v[0], 16);
    v[2] += v[3];
    v[3] = Integer_rotateLeft(v[3], 8);
    v[3] ^= v[2];
    v[0] += v[3];
    v[3] = Integer_rotateLeft(v[3], 7);
    v[3] ^= v[0];
    v[2] += v[1];
    v[1] = Integer_rotateLeft(v[1], 13);
    v[1] ^= v[2];
    v[2] = Integer_rotateLeft(v[2], 16);
  }
}

static void halfsiphash_adddata(uint32_t v[4], uint32_t newdata, int rounds) {
  v[3] ^= newdata;
  halfsiphash_rounds(v, rounds);
  v[0] ^= newdata;
}

static void halfsiphash_init32(uint32_t v[4], uint64_t seed) {
  v[0] = seed & 0xffffffff;
  v[1] = seed >> 32;
  v[2] = 0x6c796765 ^ v[0];
  v[3] = 0x74656462 ^ v[1];
}

static void halfsiphash_init64(uint32_t v[4], uint64_t seed) {
  halfsiphash_init32(v, seed);
  v[1] ^= 0xee;
}

uint32_t halfsiphash_finish32(uint32_t v[4], int rounds) {
  v[2] ^= 0xff;
  halfsiphash_rounds(v, rounds);
  return (v[1] ^ v[3]);
}

static uint64_t halfsiphash_finish64(uint32_t v[4], int rounds) {
  uint64_t rv;
  v[2] ^= 0xee;
  halfsiphash_rounds(v, rounds);
  rv = v[1] ^ v[3];
  v[1] ^= 0xdd;
  halfsiphash_rounds(v, rounds);
  rv |= (uint64_t)(v[1] ^ v[3]) << 32;
  return rv;
}

// HalfSipHash-2-4 (32-bit output) for Symbols
uint32_t AltHashing::halfsiphash_32(uint64_t seed, const uint8_t* data, int len) {
  uint32_t v[4];
  uint32_t newdata;
  int off = 0;
  int count = len;

  halfsiphash_init32(v, seed);

  // body
  while (count >= 4) {

    // Avoid sign extension with 0x0ff
    newdata = (data[off] & 0x0FF)
        | (data[off + 1] & 0x0FF) << 8
        | (data[off + 2] & 0x0FF) << 16
        | data[off + 3] << 24;

    count -= 4;
    off += 4;

    halfsiphash_adddata(v, newdata, 2);
  }

  // tail
  newdata = ((uint32_t)len) << 24; // (Byte.SIZE / Byte.SIZE);

  if (count > 0) {
    switch (count) {
      case 3:
        newdata |= (data[off + 2] & 0x0ff) << 16;
      // fall through
      case 2:
        newdata |= (data[off + 1] & 0x0ff) << 8;
      // fall through
      case 1:
        newdata |= (data[off] & 0x0ff);
      // fall through
    }
  }

  halfsiphash_adddata(v, newdata, 2);

  // finalization
  return halfsiphash_finish32(v, 4);
}

// HalfSipHash-2-4 (32-bit output) for Strings
uint32_t AltHashing::halfsiphash_32(uint64_t seed, const uint16_t* data, int len) {
  uint32_t v[4];
  uint32_t newdata;
  int off = 0;
  int count = len;

  halfsiphash_init32(v, seed);

  // body
  while (count >= 2) {
    uint16_t d1 = data[off++] & 0x0FFFF;
    uint16_t d2 = data[off++];
    newdata = (d1 | d2 << 16);

    count -= 2;

    halfsiphash_adddata(v, newdata, 2);
  }

  // tail
  newdata = ((uint32_t)len * 2) << 24; // (Character.SIZE / Byte.SIZE);
  if (count > 0) {
    newdata |= (uint32_t)data[off];
  }
  halfsiphash_adddata(v, newdata, 2);

  // finalization
  return halfsiphash_finish32(v, 4);
}

// HalfSipHash-2-4 (64-bit output) for integers (used to create seed)
uint64_t AltHashing::halfsiphash_64(uint64_t seed, const uint32_t* data, int len) {
  uint32_t v[4];

  int off = 0;
  int end = len;

  halfsiphash_init64(v, seed);

  // body
  while (off < end) {
    halfsiphash_adddata(v, (uint32_t)data[off++], 2);
  }

  // tail (always empty, as body is always 32-bit chunks)

  // finalization
  halfsiphash_adddata(v, ((uint32_t)len * 4) << 24, 2); // (Integer.SIZE / Byte.SIZE);
  return halfsiphash_finish64(v, 4);
}

// HalfSipHash-2-4 (64-bit output) for integers (used to create seed)
uint64_t AltHashing::halfsiphash_64(const uint32_t* data, int len) {
  return halfsiphash_64((uint64_t)0, data, len);
}
