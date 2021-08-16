/*
* Copyright (c) 2020, Intel Corporation.
*
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
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "compiler/compiler_globals.hpp"

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

#ifdef _LP64

#if COMPILER2_OR_JVMCI

void MacroAssembler::arraycopy_avx3_special_cases(XMMRegister xmm, KRegister mask, Register from,
                                                  Register to, Register count, int shift,
                                                  Register index, Register temp,
                                                  bool use64byteVector, Label& L_entry, Label& L_exit) {
  Label L_entry_64, L_entry_96, L_entry_128;
  Label L_entry_160, L_entry_192;

  int size_mat[][6] = {
  /* T_BYTE */ {32 , 64,  96 , 128 , 160 , 192 },
  /* T_SHORT*/ {16 , 32,  48 , 64  , 80  , 96  },
  /* T_INT  */ {8  , 16,  24 , 32  , 40  , 48  },
  /* T_LONG */ {4  ,  8,  12 , 16  , 20  , 24  }
  };

  // Case A) Special case for length less than equal to 32 bytes.
  cmpq(count, size_mat[shift][0]);
  jccb(Assembler::greater, L_entry_64);
  copy32_masked_avx(to, from, xmm, mask, count, index, temp, shift);
  jmp(L_exit);

  // Case B) Special case for length less than equal to 64 bytes.
  BIND(L_entry_64);
  cmpq(count, size_mat[shift][1]);
  jccb(Assembler::greater, L_entry_96);
  copy64_masked_avx(to, from, xmm, mask, count, index, temp, shift, 0, use64byteVector);
  jmp(L_exit);

  // Case C) Special case for length less than equal to 96 bytes.
  BIND(L_entry_96);
  cmpq(count, size_mat[shift][2]);
  jccb(Assembler::greater, L_entry_128);
  copy64_avx(to, from, index, xmm, false, shift, 0, use64byteVector);
  subq(count, 64 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, index, temp, shift, 64);
  jmp(L_exit);

  // Case D) Special case for length less than equal to 128 bytes.
  BIND(L_entry_128);
  cmpq(count, size_mat[shift][3]);
  jccb(Assembler::greater, L_entry_160);
  copy64_avx(to, from, index, xmm, false, shift, 0, use64byteVector);
  copy32_avx(to, from, index, xmm, shift, 64);
  subq(count, 96 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, index, temp, shift, 96);
  jmp(L_exit);

  // Case E) Special case for length less than equal to 160 bytes.
  BIND(L_entry_160);
  cmpq(count, size_mat[shift][4]);
  jccb(Assembler::greater, L_entry_192);
  copy64_avx(to, from, index, xmm, false, shift, 0, use64byteVector);
  copy64_avx(to, from, index, xmm, false, shift, 64, use64byteVector);
  subq(count, 128 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, index, temp, shift, 128);
  jmp(L_exit);

  // Case F) Special case for length less than equal to 192 bytes.
  BIND(L_entry_192);
  cmpq(count, size_mat[shift][5]);
  jcc(Assembler::greater, L_entry);
  copy64_avx(to, from, index, xmm, false, shift, 0, use64byteVector);
  copy64_avx(to, from, index, xmm, false, shift, 64, use64byteVector);
  copy32_avx(to, from, index, xmm, shift, 128);
  subq(count, 160 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, index, temp, shift, 160);
  jmp(L_exit);
}

void MacroAssembler::arraycopy_avx3_special_cases_conjoint(XMMRegister xmm, KRegister mask, Register from,
                                                           Register to, Register start_index, Register end_index,
                                                           Register count, int shift, Register temp,
                                                           bool use64byteVector, Label& L_entry, Label& L_exit) {
  Label L_entry_64, L_entry_96, L_entry_128;
  Label L_entry_160, L_entry_192;
  bool avx3 = MaxVectorSize > 32 && AVX3Threshold == 0;

  int size_mat[][6] = {
  /* T_BYTE */ {32 , 64,  96 , 128 , 160 , 192 },
  /* T_SHORT*/ {16 , 32,  48 , 64  , 80  , 96  },
  /* T_INT  */ {8  , 16,  24 , 32  , 40  , 48  },
  /* T_LONG */ {4  ,  8,  12 , 16  , 20  , 24  }
  };

  // Case A) Special case for length less than equal to 32 bytes.
  cmpq(count, size_mat[shift][0]);
  jccb(Assembler::greater, L_entry_64);
  copy32_masked_avx(to, from, xmm, mask, count, start_index, temp, shift);
  jmp(L_exit);

  // Case B) Special case for length less than equal to 64 bytes.
  BIND(L_entry_64);
  cmpq(count, size_mat[shift][1]);
  jccb(Assembler::greater, L_entry_96);
  if (avx3) {
     copy64_masked_avx(to, from, xmm, mask, count, start_index, temp, shift, 0, true);
  } else {
     copy32_avx(to, from, end_index, xmm, shift, -32);
     subq(count, 32 >> shift);
     copy32_masked_avx(to, from, xmm, mask, count, start_index, temp, shift);
  }
  jmp(L_exit);

  // Case C) Special case for length less than equal to 96 bytes.
  BIND(L_entry_96);
  cmpq(count, size_mat[shift][2]);
  jccb(Assembler::greater, L_entry_128);
  copy64_avx(to, from, end_index, xmm, true, shift, -64, use64byteVector);
  subq(count, 64 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, start_index, temp, shift);
  jmp(L_exit);

  // Case D) Special case for length less than equal to 128 bytes.
  BIND(L_entry_128);
  cmpq(count, size_mat[shift][3]);
  jccb(Assembler::greater, L_entry_160);
  copy64_avx(to, from, end_index, xmm, true, shift, -64, use64byteVector);
  copy32_avx(to, from, end_index, xmm, shift, -96);
  subq(count, 96 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, start_index, temp, shift);
  jmp(L_exit);

  // Case E) Special case for length less than equal to 160 bytes.
  BIND(L_entry_160);
  cmpq(count, size_mat[shift][4]);
  jccb(Assembler::greater, L_entry_192);
  copy64_avx(to, from, end_index, xmm, true, shift, -64, use64byteVector);
  copy64_avx(to, from, end_index, xmm, true, shift, -128, use64byteVector);
  subq(count, 128 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, start_index, temp, shift);
  jmp(L_exit);

  // Case F) Special case for length less than equal to 192 bytes.
  BIND(L_entry_192);
  cmpq(count, size_mat[shift][5]);
  jcc(Assembler::greater, L_entry);
  copy64_avx(to, from, end_index, xmm, true, shift, -64, use64byteVector);
  copy64_avx(to, from, end_index, xmm, true, shift, -128, use64byteVector);
  copy32_avx(to, from, end_index, xmm, shift, -160);
  subq(count, 160 >> shift);
  copy32_masked_avx(to, from, xmm, mask, count, start_index, temp, shift);
  jmp(L_exit);
}

void MacroAssembler::copy64_masked_avx(Register dst, Register src, XMMRegister xmm,
                                       KRegister mask, Register length, Register index,
                                       Register temp, int shift, int offset,
                                       bool use64byteVector) {
  BasicType type[] = { T_BYTE,  T_SHORT,  T_INT,   T_LONG};
  assert(MaxVectorSize >= 32, "vector length should be >= 32");
  if (!use64byteVector) {
    copy32_avx(dst, src, index, xmm, shift, offset);
    subptr(length, 32 >> shift);
    copy32_masked_avx(dst, src, xmm, mask, length, index, temp, shift, offset+32);
  } else {
    Address::ScaleFactor scale = (Address::ScaleFactor)(shift);
    assert(MaxVectorSize == 64, "vector length != 64");
    mov64(temp, -1L);
    bzhiq(temp, temp, length);
    kmovql(mask, temp);
    evmovdqu(type[shift], mask, xmm, Address(src, index, scale, offset), Assembler::AVX_512bit);
    evmovdqu(type[shift], mask, Address(dst, index, scale, offset), xmm, Assembler::AVX_512bit);
  }
}


void MacroAssembler::copy32_masked_avx(Register dst, Register src, XMMRegister xmm,
                                       KRegister mask, Register length, Register index,
                                       Register temp, int shift, int offset) {
  assert(MaxVectorSize >= 32, "vector length should be >= 32");
  BasicType type[] = { T_BYTE,  T_SHORT,  T_INT,   T_LONG};
  Address::ScaleFactor scale = (Address::ScaleFactor)(shift);
  mov64(temp, -1L);
  bzhiq(temp, temp, length);
  kmovql(mask, temp);
  evmovdqu(type[shift], mask, xmm, Address(src, index, scale, offset), Assembler::AVX_256bit);
  evmovdqu(type[shift], mask, Address(dst, index, scale, offset), xmm, Assembler::AVX_256bit);
}


void MacroAssembler::copy32_avx(Register dst, Register src, Register index, XMMRegister xmm,
                                int shift, int offset) {
  assert(MaxVectorSize >= 32, "vector length should be >= 32");
  Address::ScaleFactor scale = (Address::ScaleFactor)(shift);
  vmovdqu(xmm, Address(src, index, scale, offset));
  vmovdqu(Address(dst, index, scale, offset), xmm);
}


void MacroAssembler::copy64_avx(Register dst, Register src, Register index, XMMRegister xmm,
                                bool conjoint, int shift, int offset, bool use64byteVector) {
  assert(MaxVectorSize == 64 || MaxVectorSize == 32, "vector length mismatch");
  if (!use64byteVector) {
    if (conjoint) {
      copy32_avx(dst, src, index, xmm, shift, offset+32);
      copy32_avx(dst, src, index, xmm, shift, offset);
    } else {
      copy32_avx(dst, src, index, xmm, shift, offset);
      copy32_avx(dst, src, index, xmm, shift, offset+32);
    }
  } else {
    Address::ScaleFactor scale = (Address::ScaleFactor)(shift);
    evmovdquq(xmm, Address(src, index, scale, offset), Assembler::AVX_512bit);
    evmovdquq(Address(dst, index, scale, offset), xmm, Assembler::AVX_512bit);
  }
}

#endif // COMPILER2_OR_JVMCI

#endif
