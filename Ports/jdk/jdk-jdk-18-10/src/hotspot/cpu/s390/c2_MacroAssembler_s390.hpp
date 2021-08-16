/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_S390_C2_MACROASSEMBLER_S390_HPP
#define CPU_S390_C2_MACROASSEMBLER_S390_HPP

// C2_MacroAssembler contains high-level macros for C2

 public:
  //-------------------------------------------
  // Special String Intrinsics Implementation.
  //-------------------------------------------
  // Intrinsics for CompactStrings
  //   Restores: src, dst
  //   Uses:     cnt
  //   Kills:    tmp, Z_R0, Z_R1.
  //   Early clobber: result.
  //   Boolean precise controls accuracy of result value.
  unsigned int string_compress(Register result, Register src, Register dst, Register cnt,
                               Register tmp,    bool precise);

  // Inflate byte[] to char[].
  unsigned int string_inflate_trot(Register src, Register dst, Register cnt, Register tmp);

  // Inflate byte[] to char[].
  //   Restores: src, dst
  //   Uses:     cnt
  //   Kills:    tmp, Z_R0, Z_R1.
  unsigned int string_inflate(Register src, Register dst, Register cnt, Register tmp);

  // Inflate byte[] to char[], length known at compile time.
  //   Restores: src, dst
  //   Kills:    tmp, Z_R0, Z_R1.
  // Note:
  //   len is signed int. Counts # characters, not bytes.
  unsigned int string_inflate_const(Register src, Register dst, Register tmp, int len);

  // Kills src.
  unsigned int has_negatives(Register result, Register src, Register cnt,
                             Register odd_reg, Register even_reg, Register tmp);

  unsigned int string_compare(Register str1, Register str2, Register cnt1, Register cnt2,
                              Register odd_reg, Register even_reg, Register result, int ae);

  unsigned int array_equals(bool is_array_equ, Register ary1, Register ary2, Register limit,
                            Register odd_reg, Register even_reg, Register result, bool is_byte);

  unsigned int string_indexof(Register result, Register haystack, Register haycnt,
                              Register needle, Register needlecnt, int needlecntval,
                              Register odd_reg, Register even_reg, int ae);

  unsigned int string_indexof_char(Register result, Register haystack, Register haycnt,
                                   Register needle, jchar needleChar, Register odd_reg, Register even_reg, bool is_byte);

#endif // CPU_S390_C2_MACROASSEMBLER_S390_HPP
