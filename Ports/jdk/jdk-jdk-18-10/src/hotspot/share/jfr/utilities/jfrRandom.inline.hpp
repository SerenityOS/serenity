/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRRANDOM_INLINE_HPP
#define SHARE_JFR_UTILITIES_JFRRANDOM_INLINE_HPP

#include "jfr/utilities/jfrRandom.hpp"

inline JfrPRNG::JfrPRNG(const void* seed) : _rnd(reinterpret_cast<uint64_t>(seed)) {
  assert(seed != NULL, "invariant");
}

// Returns the next prng value.
// pRNG is: aX+b mod c with a = 0x5DEECE66D, b =  0xB, c = 1<<48
// This is the lrand64 generator.
inline uint64_t next(uint64_t rnd) {
  static const uint64_t PrngMult = 0x5DEECE66DLL;
  static const uint64_t PrngAdd = 0xB;
  static const uint64_t PrngModPower = 48;
  static const uint64_t PrngModMask = (static_cast<uint64_t>(1) << PrngModPower) - 1;
  return (PrngMult * rnd + PrngAdd) & PrngModMask;
}

inline double JfrPRNG::next_uniform() const {
  _rnd = next(_rnd);
  // Take the top 26 bits as the random number
  // (This plus a 1<<58 sampling bound gives a max possible step of
  // 5194297183973780480 bytes.  In this case,
  // for sample_parameter = 1<<19, max possible step is
  // 9448372 bytes (24 bits).
  static const uint64_t PrngModPower = 48;  // Number of bits in prng
  // The uint32_t cast is to prevent a (hard-to-reproduce) NAN
  // under piii debug for some binaries.
  // the n_rand value is between 0 and 2**26-1 so it needs to be normalized by dividing by 2**26 (67108864)
  return (static_cast<uint32_t>(_rnd >> (PrngModPower - 26)) / static_cast<double>(67108864));
}

#endif // SHARE_JFR_UTILITIES_JFRRANDOM_INLINE_HPP
