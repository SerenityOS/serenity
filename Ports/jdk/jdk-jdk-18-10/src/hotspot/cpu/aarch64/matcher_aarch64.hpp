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

#ifndef CPU_AARCH64_MATCHER_AARCH64_HPP
#define CPU_AARCH64_MATCHER_AARCH64_HPP

  // Defined within class Matcher

  // false => size gets scaled to BytesPerLong, ok.
  static const bool init_array_count_is_in_bytes = false;

  // Whether this platform implements the scalable vector feature
  static const bool implements_scalable_vector = true;

  static const bool supports_scalable_vector() {
    return UseSVE > 0;
  }

  // aarch64 supports misaligned vectors store/load.
  static constexpr bool misaligned_vectors_ok() {
    return true;
  }

  // Whether code generation need accurate ConvI2L types.
  static const bool convi2l_type_required = false;

  // Does the CPU require late expand (see block.cpp for description of late expand)?
  static const bool require_postalloc_expand = false;

  // Do we need to mask the count passed to shift instructions or does
  // the cpu only look at the lower 5/6 bits anyway?
  static const bool need_masked_shift_count = false;

  // No support for generic vector operands.
  static const bool supports_generic_vector_operands = false;

  static constexpr bool isSimpleConstant64(jlong value) {
    // Will one (StoreL ConL) be cheaper than two (StoreI ConI)?.
    // Probably always true, even if a temp register is required.
    return true;
  }

  // Use conditional move (CMOVL)
  static constexpr int long_cmove_cost() {
    // long cmoves are no more expensive than int cmoves
    return 0;
  }

  static constexpr int float_cmove_cost() {
    // float cmoves are no more expensive than int cmoves
    return 0;
  }

  // This affects two different things:
  //  - how Decode nodes are matched
  //  - how ImplicitNullCheck opportunities are recognized
  // If true, the matcher will try to remove all Decodes and match them
  // (as operands) into nodes. NullChecks are not prepared to deal with
  // Decodes by final_graph_reshaping().
  // If false, final_graph_reshaping() forces the decode behind the Cmp
  // for a NullCheck. The matcher matches the Decode node into a register.
  // Implicit_null_check optimization moves the Decode along with the
  // memory operation back up before the NullCheck.
  static bool narrow_oop_use_complex_address() {
    return CompressedOops::shift() == 0;
  }

  static bool narrow_klass_use_complex_address() {
    // TODO
    // decide whether we need to set this to true
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

  // Is it better to copy float constants, or load them directly from
  // memory?  Intel can load a float constant from a direct address,
  // requiring no extra registers.  Most RISCs will have to materialize
  // an address into a register first, so they would do better to copy
  // the constant from stack.
  static const bool rematerialize_float_constants = false;

  // If CPU can load and store mis-aligned doubles directly then no
  // fixup is needed.  Else we split the double into 2 integer pieces
  // and move it piece-by-piece.  Only happens when passing doubles into
  // C code as the Java calling convention forces doubles to be aligned.
  static const bool misaligned_doubles_ok = true;

  // Advertise here if the CPU requires explicit rounding operations to implement strictfp mode.
  static const bool strict_fp_requires_explicit_rounding = false;

  // Are floats converted to double when stored to stack during
  // deoptimization?
  static constexpr bool float_in_double() { return false; }

  // Do ints take an entire long register or just half?
  // The relevant question is how the int is callee-saved:
  // the whole long is written but de-opt'ing will have to extract
  // the relevant 32 bits.
  static const bool int_in_long = true;

  // Does the CPU supports vector variable shift instructions?
  static constexpr bool supports_vector_variable_shifts(void) {
    return true;
  }

  // Does the CPU supports vector variable rotate instructions?
  static constexpr bool supports_vector_variable_rotates(void) {
    return false;
  }

  // Does the CPU supports vector unsigned comparison instructions?
  static const bool supports_vector_comparison_unsigned(int vlen, BasicType bt) {
    // Not supported on SVE yet.
    return !UseSVE;
  }

  // Some microarchitectures have mask registers used on vectors
  static const bool has_predicated_vectors(void) {
    return UseSVE > 0;
  }

  // true means we have fast l2f convers
  // false means that conversion is done by runtime call
  static constexpr bool convL2FSupported(void) {
      return true;
  }

#endif // CPU_AARCH64_MATCHER_AARCH64_HPP
