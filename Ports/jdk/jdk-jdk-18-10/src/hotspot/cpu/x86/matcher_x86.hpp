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

#ifndef CPU_X86_MATCHER_X86_HPP
#define CPU_X86_MATCHER_X86_HPP

  // Defined within class Matcher

  // The ecx parameter to rep stosq for the ClearArray node is in words.
  static const bool init_array_count_is_in_bytes = false;

  // Whether this platform implements the scalable vector feature
  static const bool implements_scalable_vector = false;

  static constexpr bool supports_scalable_vector() {
    return false;
  }

  // x86 supports misaligned vectors store/load.
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

  // x86 supports generic vector operands: vec and legVec.
  static const bool supports_generic_vector_operands = true;

  static constexpr bool isSimpleConstant64(jlong value) {
    // Will one (StoreL ConL) be cheaper than two (StoreI ConI)?.
    //return value == (int) value;  // Cf. storeImmL and immL32.

    // Probably always true, even if a temp register is required.
#ifdef _LP64
    return true;
#else
    return false;
#endif
  }

#ifdef _LP64
  // No additional cost for CMOVL.
  static constexpr int long_cmove_cost() { return 0; }
#else
  // Needs 2 CMOV's for longs.
  static constexpr int long_cmove_cost() { return 1; }
#endif

#ifdef _LP64
  // No CMOVF/CMOVD with SSE2
  static int float_cmove_cost() { return ConditionalMoveLimit; }
#else
  // No CMOVF/CMOVD with SSE/SSE2
  static int float_cmove_cost() { return (UseSSE>=1) ? ConditionalMoveLimit : 0; }
#endif

  static bool narrow_oop_use_complex_address() {
    NOT_LP64(ShouldNotCallThis();)
    assert(UseCompressedOops, "only for compressed oops code");
    return (LogMinObjAlignmentInBytes <= 3);
  }

  static bool narrow_klass_use_complex_address() {
    NOT_LP64(ShouldNotCallThis();)
    assert(UseCompressedClassPointers, "only for compressed klass code");
    return (LogKlassAlignmentInBytes <= 3);
  }

  // Prefer ConN+DecodeN over ConP.
  static const bool const_oop_prefer_decode() {
    NOT_LP64(ShouldNotCallThis();)
    // Prefer ConN+DecodeN over ConP.
    return true;
  }

  // Prefer ConP over ConNKlass+DecodeNKlass.
  static const bool const_klass_prefer_decode() {
    NOT_LP64(ShouldNotCallThis();)
    return false;
  }

  // Is it better to copy float constants, or load them directly from memory?
  // Intel can load a float constant from a direct address, requiring no
  // extra registers.  Most RISCs will have to materialize an address into a
  // register first, so they would do better to copy the constant from stack.
  static const bool rematerialize_float_constants = true;

  // If CPU can load and store mis-aligned doubles directly then no fixup is
  // needed.  Else we split the double into 2 integer pieces and move it
  // piece-by-piece.  Only happens when passing doubles into C code as the
  // Java calling convention forces doubles to be aligned.
  static const bool misaligned_doubles_ok = true;

  // Advertise here if the CPU requires explicit rounding operations to implement strictfp mode.
#ifdef _LP64
  static const bool strict_fp_requires_explicit_rounding = false;
#else
  static const bool strict_fp_requires_explicit_rounding = true;
#endif

  // Are floats converted to double when stored to stack during deoptimization?
  // On x64 it is stored without convertion so we can use normal access.
  // On x32 it is stored with convertion only when FPU is used for floats.
#ifdef _LP64
  static constexpr bool float_in_double() {
    return false;
  }
#else
  static bool float_in_double() {
    return (UseSSE == 0);
  }
#endif

  // Do ints take an entire long register or just half?
#ifdef _LP64
  static const bool int_in_long = true;
#else
  static const bool int_in_long = false;
#endif

  // Does the CPU supports vector variable shift instructions?
  static bool supports_vector_variable_shifts(void) {
    return (UseAVX >= 2);
  }

  // Does the CPU supports vector variable rotate instructions?
  static constexpr bool supports_vector_variable_rotates(void) {
    return true;
  }

  // Does the CPU supports vector unsigned comparison instructions?
  static const bool supports_vector_comparison_unsigned(int vlen, BasicType bt) {
    int vlen_in_bytes = vlen * type2aelembytes(bt);
    if ((UseAVX > 2) && (VM_Version::supports_avx512vl() || vlen_in_bytes == 64))
      return true;
    else {
      // instruction set supports only signed comparison
      // so need to zero extend to higher integral type and perform comparison
      // cannot cast long to higher integral type
      // and on avx1 cannot cast 128 bit integral vectors to higher size

      if ((bt != T_LONG)  &&
          ((UseAVX >= 2) || (vlen_in_bytes <= 8)))
        return true;
    }
    return false;
  }

  // Some microarchitectures have mask registers used on vectors
  static const bool has_predicated_vectors(void) {
    return VM_Version::supports_evex();
  }

  // true means we have fast l2f convers
  // false means that conversion is done by runtime call
  static constexpr bool convL2FSupported(void) {
      return true;
  }

#endif // CPU_X86_MATCHER_X86_HPP
