/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_S390_MATCHER_S390_HPP
#define CPU_S390_MATCHER_S390_HPP

  // Defined within class Matcher


  // Should correspond to setting above
  static const bool init_array_count_is_in_bytes = false;

  // Whether this platform implements the scalable vector feature
  static const bool implements_scalable_vector = false;

  static constexpr const bool supports_scalable_vector() {
    return false;
  }

  // z/Architecture does support misaligned store/load at minimal extra cost.
  static constexpr bool misaligned_vectors_ok() {
    return true;
  }

  // Whether code generation need accurate ConvI2L types.
  static const bool convi2l_type_required = true;

  // Do the processor's shift instructions only use the low 5/6 bits
  // of the count for 32/64 bit ints? If not we need to do the masking
  // ourselves.
  static const bool need_masked_shift_count = false;

  // Does the CPU require late expand (see block.cpp for description of late expand)?
  static const bool require_postalloc_expand = false;

  // No support for generic vector operands.
  static const bool supports_generic_vector_operands = false;

  static constexpr bool isSimpleConstant64(jlong value) {
    // Probably always true, even if a temp register is required.
    return true;
  }

  // Suppress CMOVL. Conditional move available on z/Architecture only from z196 onwards. Not exploited yet.
  static const int long_cmove_cost() { return ConditionalMoveLimit; }

  // Suppress CMOVF. Conditional move available on z/Architecture only from z196 onwards. Not exploited yet.
  static const int float_cmove_cost() { return ConditionalMoveLimit; }

  // Set this as clone_shift_expressions.
  static bool narrow_oop_use_complex_address() {
    if (CompressedOops::base() == NULL && CompressedOops::shift() == 0) return true;
    return false;
  }

  static bool narrow_klass_use_complex_address() {
    NOT_LP64(ShouldNotCallThis());
    assert(UseCompressedClassPointers, "only for compressed klass code");
    // TODO HS25: z port if (MatchDecodeNodes) return true;
    return false;
  }

  static bool const_oop_prefer_decode() {
    // Prefer ConN+DecodeN over ConP in simple compressed oops mode.
    return CompressedOops::base() == NULL;
  }

  static bool const_klass_prefer_decode() {
    // Prefer ConNKlass+DecodeNKlass over ConP in simple compressed klass mode.
    return CompressedKlassPointers::base() == NULL;
  }

  // Is it better to copy float constants, or load them directly from memory?
  // Most RISCs will have to materialize an address into a
  // register first, so they would do better to copy the constant from stack.
  static const bool rematerialize_float_constants = false;

  // If CPU can load and store mis-aligned doubles directly then no fixup is
  // needed. Else we split the double into 2 integer pieces and move it
  // piece-by-piece. Only happens when passing doubles into C code as the
  // Java calling convention forces doubles to be aligned.
  static const bool misaligned_doubles_ok = true;

  // Advertise here if the CPU requires explicit rounding operations to implement strictfp mode.
  static const bool strict_fp_requires_explicit_rounding = false;

  // Do floats take an entire double register or just half?
  //
  // A float in resides in a zarch double register. When storing it by
  // z_std, it cannot be restored in C-code by reloading it as a double
  // and casting it into a float afterwards.
  static constexpr bool float_in_double() { return false; }

  // Do ints take an entire long register or just half?
  // The relevant question is how the int is callee-saved:
  // the whole long is written but de-opt'ing will have to extract
  // the relevant 32 bits.
  static const bool int_in_long = true;

  // Does the CPU supports vector variable shift instructions?
  static constexpr bool supports_vector_variable_shifts(void) {
    return false;
  }

  // Does the CPU supports vector variable rotate instructions?
  static constexpr bool supports_vector_variable_rotates(void) {
    return false;
  }

  // Does the CPU supports vector unsigned comparison instructions?
  static constexpr bool supports_vector_comparison_unsigned(int vlen, BasicType bt) {
    return false;
  }

  // Some microarchitectures have mask registers used on vectors
  static constexpr bool has_predicated_vectors(void) {
    return false;
  }

  // true means we have fast l2f convers
  // false means that conversion is done by runtime call
  static constexpr bool convL2FSupported(void) {
      return true;
  }

#endif // CPU_S390_MATCHER_S390_HPP
