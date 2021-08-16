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

#ifndef CPU_PPC_C2_MACROASSEMBLER_PPC_HPP
#define CPU_PPC_C2_MACROASSEMBLER_PPC_HPP

// C2_MacroAssembler contains high-level macros for C2

 public:
  // Intrinsics for CompactStrings
  // Compress char[] to byte[] by compressing 16 bytes at once.
  void string_compress_16(Register src, Register dst, Register cnt,
                          Register tmp1, Register tmp2, Register tmp3, Register tmp4, Register tmp5,
                          Label& Lfailure);

  // Compress char[] to byte[]. cnt must be positive int.
  void string_compress(Register src, Register dst, Register cnt, Register tmp, Label& Lfailure);

  // Inflate byte[] to char[] by inflating 16 bytes at once.
  void string_inflate_16(Register src, Register dst, Register cnt,
                         Register tmp1, Register tmp2, Register tmp3, Register tmp4, Register tmp5);

  // Inflate byte[] to char[]. cnt must be positive int.
  void string_inflate(Register src, Register dst, Register cnt, Register tmp);

  void string_compare(Register str1, Register str2, Register cnt1, Register cnt2,
                      Register tmp1, Register result, int ae);

  void array_equals(bool is_array_equ, Register ary1, Register ary2,
                    Register limit, Register tmp1, Register result, bool is_byte);

  void string_indexof(Register result, Register haystack, Register haycnt,
                      Register needle, ciTypeArray* needle_values, Register needlecnt, int needlecntval,
                      Register tmp1, Register tmp2, Register tmp3, Register tmp4, int ae);

  void string_indexof_char(Register result, Register haystack, Register haycnt,
                           Register needle, jchar needleChar, Register tmp1, Register tmp2, bool is_byte);

  void has_negatives(Register src, Register cnt, Register result, Register tmp1, Register tmp2);

#endif // CPU_PPC_C2_MACROASSEMBLER_PPC_HPP
