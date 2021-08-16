/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/ad.hpp"
#include "opto/chaitin.hpp"
#include "opto/compile.hpp"
#include "opto/matcher.hpp"
#include "opto/node.hpp"
#include "opto/regmask.hpp"
#include "utilities/population_count.hpp"
#include "utilities/powerOfTwo.hpp"

//------------------------------dump-------------------------------------------

#ifndef PRODUCT
void OptoReg::dump(int r, outputStream *st) {
  switch (r) {
  case Special: st->print("r---"); break;
  case Bad:     st->print("rBAD"); break;
  default:
    if (r < _last_Mach_Reg) st->print("%s", Matcher::regName[r]);
    else st->print("rS%d",r);
    break;
  }
}
#endif


//=============================================================================
const RegMask RegMask::Empty;

const RegMask RegMask::All(
# define BODY(I) -1,
  FORALL_BODY
# undef BODY
  0
);

//=============================================================================
bool RegMask::is_vector(uint ireg) {
  return (ireg == Op_VecA || ireg == Op_VecS || ireg == Op_VecD ||
          ireg == Op_VecX || ireg == Op_VecY || ireg == Op_VecZ );
}

int RegMask::num_registers(uint ireg) {
  switch(ireg) {
    case Op_VecZ:
      return SlotsPerVecZ;
    case Op_VecY:
      return SlotsPerVecY;
    case Op_VecX:
      return SlotsPerVecX;
    case Op_VecD:
      return SlotsPerVecD;
    case Op_RegVectMask:
      return SlotsPerRegVectMask;
    case Op_RegD:
    case Op_RegL:
#ifdef _LP64
    case Op_RegP:
#endif
      return 2;
    case Op_VecA:
      assert(Matcher::supports_scalable_vector(), "does not support scalable vector");
      return SlotsPerVecA;
    default:
      // Op_VecS and the rest ideal registers.
      assert(ireg == Op_VecS || !is_vector(ireg), "unexpected, possibly multi-slot register");
      return 1;
  }
}

int RegMask::num_registers(uint ireg, LRG &lrg) {
  int n_regs = num_registers(ireg);

  // assigned is OptoReg which is selected by register allocator
  OptoReg::Name assigned = lrg.reg();
  assert(OptoReg::is_valid(assigned), "should be valid opto register");

  if (lrg.is_scalable() && OptoReg::is_stack(assigned)) {
    n_regs = lrg.scalable_reg_slots();
  }
  return n_regs;
}

static const uintptr_t zero  = uintptr_t(0);  // 0x00..00
static const uintptr_t all   = ~uintptr_t(0);  // 0xFF..FF
static const uintptr_t fives = all/3;        // 0x5555..55

// only indices of power 2 are accessed, so index 3 is only filled in for storage.
static const uintptr_t low_bits[5] = { fives, // 0x5555..55
                                       all/0xF,        // 0x1111..11,
                                       all/0xFF,       // 0x0101..01,
                                       zero,           // 0x0000..00
                                       all/0xFFFF };   // 0x0001..01

// Clear out partial bits; leave only bit pairs
void RegMask::clear_to_pairs() {
  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    uintptr_t bits = _RM_UP[i];
    bits &= ((bits & fives) << 1U); // 1 hi-bit set for each pair
    bits |= (bits >> 1U);          // Smear 1 hi-bit into a pair
    _RM_UP[i] = bits;
  }
  assert(is_aligned_pairs(), "mask is not aligned, adjacent pairs");
}

bool RegMask::is_misaligned_pair() const {
  return Size() == 2 && !is_aligned_pairs();
}

bool RegMask::is_aligned_pairs() const {
  // Assert that the register mask contains only bit pairs.
  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    uintptr_t bits = _RM_UP[i];
    while (bits) {              // Check bits for pairing
      uintptr_t bit = uintptr_t(1) << find_lowest_bit(bits); // Extract low bit
      // Low bit is not odd means its mis-aligned.
      if ((bit & fives) == 0) return false;
      bits -= bit;              // Remove bit from mask
      // Check for aligned adjacent bit
      if ((bits & (bit << 1U)) == 0) return false;
      bits -= (bit << 1U); // Remove other halve of pair
    }
  }
  return true;
}

// Return TRUE if the mask contains a single bit
bool RegMask::is_bound1() const {
  if (is_AllStack()) return false;

  for (unsigned i = _lwm; i <= _hwm; i++) {
    uintptr_t v = _RM_UP[i];
    if (v != 0) {
      // Only one bit allowed -> v must be a power of two
      if (!is_power_of_2(v)) {
        return false;
      }

      // A single bit was found - check there are no bits in the rest of the mask
      for (i++; i <= _hwm; i++) {
        if (_RM_UP[i] != 0) {
          return false;
        }
      }
      // Done; found a single bit
      return true;
    }
  }
  // No bit found
  return false;
}

// Return TRUE if the mask contains an adjacent pair of bits and no other bits.
bool RegMask::is_bound_pair() const {
  if (is_AllStack()) return false;

  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    if (_RM_UP[i] != 0) {               // Found some bits
      unsigned int bit_index = find_lowest_bit(_RM_UP[i]);
      if (bit_index != _WordBitMask) {   // Bit pair stays in same word?
        uintptr_t bit = uintptr_t(1) << bit_index; // Extract lowest bit from mask
        if ((bit | (bit << 1U)) != _RM_UP[i]) {
          return false;            // Require adjacent bit pair and no more bits
        }
      } else {                     // Else its a split-pair case
        assert(is_power_of_2(_RM_UP[i]), "invariant");
        i++;                       // Skip iteration forward
        if (i > _hwm || _RM_UP[i] != 1) {
          return false; // Require 1 lo bit in next word
        }
      }

      // A matching pair was found - check there are no bits in the rest of the mask
      for (i++; i <= _hwm; i++) {
        if (_RM_UP[i] != 0) {
          return false;
        }
      }
      // Found a bit pair
      return true;
    }
  }
  // True for the empty mask, too
  return true;
}

// Test for a single adjacent set of ideal register's size.
bool RegMask::is_bound(uint ireg) const {
  if (is_vector(ireg)) {
    if (is_bound_set(num_registers(ireg)))
      return true;
  } else if (is_bound1() || is_bound_pair()) {
    return true;
  }
  return false;
}

// Check that whether given reg number with size is valid
// for current regmask, where reg is the highest number.
bool RegMask::is_valid_reg(OptoReg::Name reg, const int size) const {
  for (int i = 0; i < size; i++) {
    if (!Member(reg - i)) {
      return false;
    }
  }
  return true;
}

// Find the lowest-numbered register set in the mask.  Return the
// HIGHEST register number in the set, or BAD if no sets.
// Works also for size 1.
OptoReg::Name RegMask::find_first_set(LRG &lrg, const int size) const {
  if (lrg.is_scalable()) {
    // For scalable vector register, regmask is SlotsPerVecA bits aligned.
    assert(is_aligned_sets(SlotsPerVecA), "mask is not aligned, adjacent sets");
  } else {
    assert(is_aligned_sets(size), "mask is not aligned, adjacent sets");
  }
  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    if (_RM_UP[i]) {                // Found some bits
      // Convert to bit number, return hi bit in pair
      return OptoReg::Name((i<<_LogWordBits) + find_lowest_bit(_RM_UP[i]) + (size - 1));
    }
  }
  return OptoReg::Bad;
}

// Clear out partial bits; leave only aligned adjacent bit pairs
void RegMask::clear_to_sets(const unsigned int size) {
  if (size == 1) return;
  assert(2 <= size && size <= 16, "update low bits table");
  assert(is_power_of_2(size), "sanity");
  assert(valid_watermarks(), "sanity");
  uintptr_t low_bits_mask = low_bits[size >> 2U];
  for (unsigned i = _lwm; i <= _hwm; i++) {
    uintptr_t bits = _RM_UP[i];
    uintptr_t sets = (bits & low_bits_mask);
    for (unsigned j = 1U; j < size; j++) {
      sets = (bits & (sets << 1U)); // filter bits which produce whole sets
    }
    sets |= (sets >> 1U);           // Smear 1 hi-bit into a set
    if (size > 2) {
      sets |= (sets >> 2U);         // Smear 2 hi-bits into a set
      if (size > 4) {
        sets |= (sets >> 4U);       // Smear 4 hi-bits into a set
        if (size > 8) {
          sets |= (sets >> 8U);     // Smear 8 hi-bits into a set
        }
      }
    }
    _RM_UP[i] = sets;
  }
  assert(is_aligned_sets(size), "mask is not aligned, adjacent sets");
}

// Smear out partial bits to aligned adjacent bit sets
void RegMask::smear_to_sets(const unsigned int size) {
  if (size == 1) return;
  assert(2 <= size && size <= 16, "update low bits table");
  assert(is_power_of_2(size), "sanity");
  assert(valid_watermarks(), "sanity");
  uintptr_t low_bits_mask = low_bits[size >> 2U];
  for (unsigned i = _lwm; i <= _hwm; i++) {
    uintptr_t bits = _RM_UP[i];
    uintptr_t sets = 0;
    for (unsigned j = 0; j < size; j++) {
      sets |= (bits & low_bits_mask);  // collect partial bits
      bits  = bits >> 1U;
    }
    sets |= (sets << 1U);           // Smear 1 lo-bit  into a set
    if (size > 2) {
      sets |= (sets << 2U);         // Smear 2 lo-bits into a set
      if (size > 4) {
        sets |= (sets << 4U);       // Smear 4 lo-bits into a set
        if (size > 8) {
          sets |= (sets << 8U);     // Smear 8 lo-bits into a set
        }
      }
    }
    _RM_UP[i] = sets;
  }
  assert(is_aligned_sets(size), "mask is not aligned, adjacent sets");
}

// Assert that the register mask contains only bit sets.
bool RegMask::is_aligned_sets(const unsigned int size) const {
  if (size == 1) return true;
  assert(2 <= size && size <= 16, "update low bits table");
  assert(is_power_of_2(size), "sanity");
  uintptr_t low_bits_mask = low_bits[size >> 2U];
  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    uintptr_t bits = _RM_UP[i];
    while (bits) {              // Check bits for pairing
      uintptr_t bit = uintptr_t(1) << find_lowest_bit(bits);
      // Low bit is not odd means its mis-aligned.
      if ((bit & low_bits_mask) == 0) {
        return false;
      }
      // Do extra work since (bit << size) may overflow.
      uintptr_t hi_bit = bit << (size-1); // high bit
      uintptr_t set = hi_bit + ((hi_bit-1) & ~(bit-1));
      // Check for aligned adjacent bits in this set
      if ((bits & set) != set) {
        return false;
      }
      bits -= set;  // Remove this set
    }
  }
  return true;
}

// Return TRUE if the mask contains one adjacent set of bits and no other bits.
// Works also for size 1.
bool RegMask::is_bound_set(const unsigned int size) const {
  if (is_AllStack()) return false;
  assert(1 <= size && size <= 16, "update low bits table");
  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    if (_RM_UP[i] != 0) {       // Found some bits
      unsigned bit_index = find_lowest_bit(_RM_UP[i]);
      uintptr_t bit = uintptr_t(1) << bit_index;
      if (bit_index + size <= BitsPerWord) { // Bit set stays in same word?
        uintptr_t hi_bit = bit << (size - 1);
        uintptr_t set = hi_bit + ((hi_bit-1) & ~(bit-1));
        if (set != _RM_UP[i]) {
          return false;         // Require adjacent bit set and no more bits
        }
      } else {                  // Else its a split-set case
        // All bits from bit to highest bit in the word must be set
        if ((all & ~(bit-1)) != _RM_UP[i]) {
          return false;
        }
        i++;                    // Skip iteration forward and check high part
        // The lower bits should be 1 since it is split case.
        uintptr_t set = (bit >> (BitsPerWord - size)) - 1;
        if (i > _hwm || _RM_UP[i] != set) {
          return false; // Require expected low bits in next word
        }
      }

      // A matching set found - check there are no bits in the rest of the mask
      for (i++; i <= _hwm; i++) {
        if (_RM_UP[i] != 0) {
          return false;
        }
      }
      // Done - found a bit set
      return true;
    }
  }
  // True for the empty mask, too
  return true;
}

// UP means register only, Register plus stack, or stack only is DOWN
bool RegMask::is_UP() const {
  // Quick common case check for DOWN (any stack slot is legal)
  if (is_AllStack())
    return false;
  // Slower check for any stack bits set (also DOWN)
  if (overlap(Matcher::STACK_ONLY_mask))
    return false;
  // Not DOWN, so must be UP
  return true;
}

// Compute size of register mask in bits
uint RegMask::Size() const {
  uint sum = 0;
  assert(valid_watermarks(), "sanity");
  for (unsigned i = _lwm; i <= _hwm; i++) {
    sum += population_count(_RM_UP[i]);
  }
  return sum;
}

#ifndef PRODUCT
void RegMask::dump(outputStream *st) const {
  st->print("[");

  RegMaskIterator rmi(*this);
  if (rmi.has_next()) {
    OptoReg::Name start = rmi.next();

    OptoReg::dump(start, st);   // Print register
    OptoReg::Name last = start;

    // Now I have printed an initial register.
    // Print adjacent registers as "rX-rZ" instead of "rX,rY,rZ".
    // Begin looping over the remaining registers.
    while (rmi.has_next()) {
      OptoReg::Name reg = rmi.next(); // Get a register

      if (last + 1 == reg) {      // See if they are adjacent
        // Adjacent registers just collect into long runs, no printing.
        last = reg;
      } else {                  // Ending some kind of run
        if (start == last) {    // 1-register run; no special printing
        } else if (start+1 == last) {
          st->print(",");       // 2-register run; print as "rX,rY"
          OptoReg::dump(last, st);
        } else {                // Multi-register run; print as "rX-rZ"
          st->print("-");
          OptoReg::dump(last, st);
        }
        st->print(",");         // Seperate start of new run
        start = last = reg;     // Start a new register run
        OptoReg::dump(start, st); // Print register
      } // End of if ending a register run or not
    } // End of while regmask not empty

    if (start == last) {        // 1-register run; no special printing
    } else if (start+1 == last) {
      st->print(",");           // 2-register run; print as "rX,rY"
      OptoReg::dump(last, st);
    } else {                    // Multi-register run; print as "rX-rZ"
      st->print("-");
      OptoReg::dump(last, st);
    }
    if (is_AllStack()) st->print("...");
  }
  st->print("]");
}
#endif
