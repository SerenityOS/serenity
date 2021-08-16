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

#ifndef SHARE_INTERPRETER_INVOCATIONCOUNTER_HPP
#define SHARE_INTERPRETER_INVOCATIONCOUNTER_HPP

#include "runtime/handles.hpp"
#include "utilities/exceptions.hpp"

// InvocationCounters are used to trigger actions when a limit (threshold) is reached.
//
// The counter is incremented before a method is activated and an
// action is triggered when count() > limit().

class InvocationCounter {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class ciReplay;
 private:              // bit no: |31  1|  0  |
  uint _counter;       // format: [count|carry|

  enum PrivateConstants {
    number_of_carry_bits    = 1,
    number_of_noncount_bits = number_of_carry_bits,
    count_grain             = nth_bit(number_of_carry_bits),
    carry_mask              = right_n_bits(number_of_carry_bits),
    count_mask              = ((int)(-1) ^ carry_mask)
  };

 public:
  enum PublicConstants {
    count_increment      = count_grain,          // use this value to increment the 32bit _counter word
    count_mask_value     = count_mask,           // use this value to mask the backedge counter
    count_shift          = number_of_noncount_bits,
    number_of_count_bits = BitsPerInt - number_of_noncount_bits,
    count_limit          = nth_bit(number_of_count_bits - 1)
  };

  // Manipulation
  void reset();
  void init();
  void decay();                                  // decay counter (divide by two)
  void set_carry_on_overflow();
  void set(uint count);
  void increment()                 { _counter += count_increment; }

  // Accessors
  bool carry() const               { return (_counter & carry_mask) != 0; }
  uint count() const               { return _counter >> number_of_noncount_bits; }
  uint limit() const               { return CompileThreshold; }
  uint raw_counter() const         { return _counter; }

  void print();

private:
  void set_carry()                   {  _counter |= carry_mask; }
  uint extract_carry(uint raw) const { return (raw & carry_mask); }
  uint extract_count(uint raw) const { return raw >> number_of_noncount_bits; }
  void update(uint new_count);
  void set(uint count, uint carry);

public:

  // Miscellaneous
  static ByteSize counter_offset()               { return byte_offset_of(InvocationCounter, _counter); }
};

#endif // SHARE_INTERPRETER_INVOCATIONCOUNTER_HPP
