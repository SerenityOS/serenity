/*
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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

#include <stdlib.h>
#include <stdint.h>

#include "precompiled.hpp"
#include "utilities/globalDefinitions.hpp"
#include "immediate_aarch64.hpp"

// there are at most 2^13 possible logical immediate encodings
// however, some combinations of immr and imms are invalid
static const unsigned  LI_TABLE_SIZE = (1 << 13);

static int li_table_entry_count;

// for forward lookup we just use a direct array lookup
// and assume that the cient has supplied a valid encoding
// table[encoding] = immediate
static uint64_t LITable[LI_TABLE_SIZE];

// for reverse lookup we need a sparse map so we store a table of
// immediate and encoding pairs sorted by immediate value

struct li_pair {
  uint64_t immediate;
  uint32_t encoding;
};

static struct li_pair InverseLITable[LI_TABLE_SIZE];

// comparator to sort entries in the inverse table
int compare_immediate_pair(const void *i1, const void *i2)
{
  struct li_pair *li1 = (struct li_pair *)i1;
  struct li_pair *li2 = (struct li_pair *)i2;
  if (li1->immediate < li2->immediate) {
    return -1;
  }
  if (li1->immediate > li2->immediate) {
    return 1;
  }
  return 0;
}

// helper functions used by expandLogicalImmediate

// for i = 1, ... N result<i-1> = 1 other bits are zero
static inline uint64_t ones(int N)
{
  return (N == 64 ? -1ULL : (1ULL << N) - 1);
}

/*
 * bit twiddling helpers for instruction decode
 */

// 32 bit mask with bits [hi,...,lo] set
static inline uint32_t mask32(int hi = 31, int lo = 0)
{
  int nbits = (hi + 1) - lo;
  return ((1 << nbits) - 1) << lo;
}

static inline uint64_t mask64(int hi = 63, int lo = 0)
{
  int nbits = (hi + 1) - lo;
  return ((1L << nbits) - 1) << lo;
}

// pick bits [hi,...,lo] from val
static inline uint32_t pick32(uint32_t val, int hi = 31, int lo = 0)
{
  return (val & mask32(hi, lo));
}

// pick bits [hi,...,lo] from val
static inline uint64_t pick64(uint64_t val, int hi = 31, int lo = 0)
{
  return (val & mask64(hi, lo));
}

// mask [hi,lo] and shift down to start at bit 0
static inline uint32_t pickbits32(uint32_t val, int hi = 31, int lo = 0)
{
  return (pick32(val, hi, lo) >> lo);
}

// mask [hi,lo] and shift down to start at bit 0
static inline uint64_t pickbits64(uint64_t val, int hi = 63, int lo = 0)
{
  return (pick64(val, hi, lo) >> lo);
}

// result<0> to val<N>
static inline uint64_t pickbit(uint64_t val, int N)
{
  return pickbits64(val, N, N);
}

static inline uint32_t uimm(uint32_t val, int hi, int lo)
{
  return pickbits32(val, hi, lo);
}

// SPEC bits(M*N) Replicate(bits(M) x, integer N);
// this is just an educated guess

uint64_t replicate(uint64_t bits, int nbits, int count)
{
  uint64_t result = 0;
  // nbits may be 64 in which case we want mask to be -1
  uint64_t mask = ones(nbits);
  for (int i = 0; i < count ; i++) {
    result <<= nbits;
    result |= (bits & mask);
  }
  return result;
}

// this function writes the supplied bimm reference and returns a
// boolean to indicate success (1) or fail (0) because an illegal
// encoding must be treated as an UNALLOC instruction

// construct a 32 bit immediate value for a logical immediate operation
int expandLogicalImmediate(uint32_t immN, uint32_t immr,
                            uint32_t imms, uint64_t &bimm)
{
  int len;                 // ought to be <= 6
  uint32_t levels;         // 6 bits
  uint32_t tmask_and;      // 6 bits
  uint32_t wmask_and;      // 6 bits
  uint32_t tmask_or;       // 6 bits
  uint32_t wmask_or;       // 6 bits
  uint64_t imm64;          // 64 bits
  uint64_t tmask, wmask;   // 64 bits
  uint32_t S, R, diff;     // 6 bits?

  if (immN == 1) {
    len = 6; // looks like 7 given the spec above but this cannot be!
  } else {
    len = 0;
    uint32_t val = (~imms & 0x3f);
    for (int i = 5; i > 0; i--) {
      if (val & (1 << i)) {
        len = i;
        break;
      }
    }
    if (len < 1) {
      return 0;
    }
    // for valid inputs leading 1s in immr must be less than leading
    // zeros in imms
    int len2 = 0;                   // ought to be < len
    uint32_t val2 = (~immr & 0x3f);
    for (int i = 5; i > 0; i--) {
      if (!(val2 & (1 << i))) {
        len2 = i;
        break;
      }
    }
    if (len2 >= len) {
      return 0;
    }
  }

  levels = (1 << len) - 1;

  if ((imms & levels) == levels) {
    return 0;
  }

  S = imms & levels;
  R = immr & levels;

 // 6 bit arithmetic!
  diff = S - R;
  tmask_and = (diff | ~levels) & 0x3f;
  tmask_or = (diff & levels) & 0x3f;
  tmask = 0xffffffffffffffffULL;

  for (int i = 0; i < 6; i++) {
    int nbits = 1 << i;
    uint64_t and_bit = pickbit(tmask_and, i);
    uint64_t or_bit = pickbit(tmask_or, i);
    uint64_t and_bits_sub = replicate(and_bit, 1, nbits);
    uint64_t or_bits_sub = replicate(or_bit, 1, nbits);
    uint64_t and_bits_top = (and_bits_sub << nbits) | ones(nbits);
    uint64_t or_bits_top = (0 << nbits) | or_bits_sub;

    tmask = ((tmask
              & (replicate(and_bits_top, 2 * nbits, 32 / nbits)))
             | replicate(or_bits_top, 2 * nbits, 32 / nbits));
  }

  wmask_and = (immr | ~levels) & 0x3f;
  wmask_or = (immr & levels) & 0x3f;

  wmask = 0;

  for (int i = 0; i < 6; i++) {
    int nbits = 1 << i;
    uint64_t and_bit = pickbit(wmask_and, i);
    uint64_t or_bit = pickbit(wmask_or, i);
    uint64_t and_bits_sub = replicate(and_bit, 1, nbits);
    uint64_t or_bits_sub = replicate(or_bit, 1, nbits);
    uint64_t and_bits_top = (ones(nbits) << nbits) | and_bits_sub;
    uint64_t or_bits_top = (or_bits_sub << nbits) | 0;

    wmask = ((wmask
              & (replicate(and_bits_top, 2 * nbits, 32 / nbits)))
             | replicate(or_bits_top, 2 * nbits, 32 / nbits));
  }

  if (diff & (1U << 6)) {
    imm64 = tmask & wmask;
  } else {
    imm64 = tmask | wmask;
  }


  bimm = imm64;
  return 1;
}

// constructor to initialise the lookup tables

static void initLITables();
// Use an empty struct with a construtor as MSVC doesn't support `__attribute__ ((constructor))`
// See https://stackoverflow.com/questions/1113409/attribute-constructor-equivalent-in-vc
static struct initLITables_t { initLITables_t(void) { initLITables(); } } _initLITables;
static void initLITables()
{
  li_table_entry_count = 0;
  for (unsigned index = 0; index < LI_TABLE_SIZE; index++) {
    uint32_t N = uimm(index, 12, 12);
    uint32_t immr = uimm(index, 11, 6);
    uint32_t imms = uimm(index, 5, 0);
    if (expandLogicalImmediate(N, immr, imms, LITable[index])) {
      InverseLITable[li_table_entry_count].immediate = LITable[index];
      InverseLITable[li_table_entry_count].encoding = index;
      li_table_entry_count++;
    }
  }
  // now sort the inverse table
  qsort(InverseLITable, li_table_entry_count,
        sizeof(InverseLITable[0]), compare_immediate_pair);
}

// public APIs provided for logical immediate lookup and reverse lookup

uint64_t logical_immediate_for_encoding(uint32_t encoding)
{
  return LITable[encoding];
}

uint32_t encoding_for_logical_immediate(uint64_t immediate)
{
  struct li_pair pair;
  struct li_pair *result;

  pair.immediate = immediate;

  result = (struct li_pair *)
    bsearch(&pair, InverseLITable, li_table_entry_count,
            sizeof(InverseLITable[0]), compare_immediate_pair);

  if (result) {
    return result->encoding;
  }

  return 0xffffffff;
}

// floating point immediates are encoded in 8 bits
// fpimm[7] = sign bit
// fpimm[6:4] = signed exponent
// fpimm[3:0] = fraction (assuming leading 1)
// i.e. F = s * 1.f * 2^(e - b)

uint64_t fp_immediate_for_encoding(uint32_t imm8, int is_dp)
{
  union {
    float fpval;
    double dpval;
    uint64_t val;
  };

  uint32_t s, e, f;
  s = (imm8 >> 7 ) & 0x1;
  e = (imm8 >> 4) & 0x7;
  f = imm8 & 0xf;
  // the fp value is s * n/16 * 2r where n is 16+e
  fpval = (16.0 + f) / 16.0;
  // n.b. exponent is signed
  if (e < 4) {
    int epos = e;
    for (int i = 0; i <= epos; i++) {
      fpval *= 2.0;
    }
  } else {
    int eneg = 7 - e;
    for (int i = 0; i < eneg; i++) {
      fpval /= 2.0;
    }
  }

  if (s) {
    fpval = -fpval;
  }
  if (is_dp) {
    dpval = (double)fpval;
  }
  return val;
}

uint32_t encoding_for_fp_immediate(float immediate)
{
  // given a float which is of the form
  //
  //     s * n/16 * 2r
  //
  // where n is 16+f and imm1:s, imm4:f, simm3:r
  // return the imm8 result [s:r:f]
  //

  union {
    float fpval;
    uint32_t val;
  };
  fpval = immediate;
  uint32_t s, r, f, res;
  // sign bit is 31
  s = (val >> 31) & 0x1;
  // exponent is bits 30-23 but we only want the bottom 3 bits
  // strictly we ought to check that the bits bits 30-25 are
  // either all 1s or all 0s
  r = (val >> 23) & 0x7;
  // fraction is bits 22-0
  f = (val >> 19) & 0xf;
  res = (s << 7) | (r << 4) | f;
  return res;
}

