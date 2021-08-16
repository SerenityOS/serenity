/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "opto/c2_MacroAssembler.hpp"
#include "opto/intrinsicnode.hpp"
#include "opto/subnode.hpp"
#include "runtime/stubRoutines.hpp"

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#define STOP(error) stop(error)
#else
#define BLOCK_COMMENT(str) block_comment(str)
#define STOP(error) block_comment(error); stop(error)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

typedef void (MacroAssembler::* chr_insn)(Register Rt, const Address &adr);

// Search for str1 in str2 and return index or -1
void C2_MacroAssembler::string_indexof(Register str2, Register str1,
                                       Register cnt2, Register cnt1,
                                       Register tmp1, Register tmp2,
                                       Register tmp3, Register tmp4,
                                       Register tmp5, Register tmp6,
                                       int icnt1, Register result, int ae) {
  // NOTE: tmp5, tmp6 can be zr depending on specific method version
  Label LINEARSEARCH, LINEARSTUB, LINEAR_MEDIUM, DONE, NOMATCH, MATCH;

  Register ch1 = rscratch1;
  Register ch2 = rscratch2;
  Register cnt1tmp = tmp1;
  Register cnt2tmp = tmp2;
  Register cnt1_neg = cnt1;
  Register cnt2_neg = cnt2;
  Register result_tmp = tmp4;

  bool isL = ae == StrIntrinsicNode::LL;

  bool str1_isL = ae == StrIntrinsicNode::LL || ae == StrIntrinsicNode::UL;
  bool str2_isL = ae == StrIntrinsicNode::LL || ae == StrIntrinsicNode::LU;
  int str1_chr_shift = str1_isL ? 0:1;
  int str2_chr_shift = str2_isL ? 0:1;
  int str1_chr_size = str1_isL ? 1:2;
  int str2_chr_size = str2_isL ? 1:2;
  chr_insn str1_load_1chr = str1_isL ? (chr_insn)&MacroAssembler::ldrb :
                                      (chr_insn)&MacroAssembler::ldrh;
  chr_insn str2_load_1chr = str2_isL ? (chr_insn)&MacroAssembler::ldrb :
                                      (chr_insn)&MacroAssembler::ldrh;
  chr_insn load_2chr = isL ? (chr_insn)&MacroAssembler::ldrh : (chr_insn)&MacroAssembler::ldrw;
  chr_insn load_4chr = isL ? (chr_insn)&MacroAssembler::ldrw : (chr_insn)&MacroAssembler::ldr;

  // Note, inline_string_indexOf() generates checks:
  // if (substr.count > string.count) return -1;
  // if (substr.count == 0) return 0;

  // We have two strings, a source string in str2, cnt2 and a pattern string
  // in str1, cnt1. Find the 1st occurence of pattern in source or return -1.

  // For larger pattern and source we use a simplified Boyer Moore algorithm.
  // With a small pattern and source we use linear scan.

  if (icnt1 == -1) {
    sub(result_tmp, cnt2, cnt1);
    cmp(cnt1, (u1)8);             // Use Linear Scan if cnt1 < 8 || cnt1 >= 256
    br(LT, LINEARSEARCH);
    dup(v0, T16B, cnt1); // done in separate FPU pipeline. Almost no penalty
    subs(zr, cnt1, 256);
    lsr(tmp1, cnt2, 2);
    ccmp(cnt1, tmp1, 0b0000, LT); // Source must be 4 * pattern for BM
    br(GE, LINEARSTUB);
  }

// The Boyer Moore alogorithm is based on the description here:-
//
// http://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
//
// This describes and algorithm with 2 shift rules. The 'Bad Character' rule
// and the 'Good Suffix' rule.
//
// These rules are essentially heuristics for how far we can shift the
// pattern along the search string.
//
// The implementation here uses the 'Bad Character' rule only because of the
// complexity of initialisation for the 'Good Suffix' rule.
//
// This is also known as the Boyer-Moore-Horspool algorithm:-
//
// http://en.wikipedia.org/wiki/Boyer-Moore-Horspool_algorithm
//
// This particular implementation has few java-specific optimizations.
//
// #define ASIZE 256
//
//    int bm(unsigned char *x, int m, unsigned char *y, int n) {
//       int i, j;
//       unsigned c;
//       unsigned char bc[ASIZE];
//
//       /* Preprocessing */
//       for (i = 0; i < ASIZE; ++i)
//          bc[i] = m;
//       for (i = 0; i < m - 1; ) {
//          c = x[i];
//          ++i;
//          // c < 256 for Latin1 string, so, no need for branch
//          #ifdef PATTERN_STRING_IS_LATIN1
//          bc[c] = m - i;
//          #else
//          if (c < ASIZE) bc[c] = m - i;
//          #endif
//       }
//
//       /* Searching */
//       j = 0;
//       while (j <= n - m) {
//          c = y[i+j];
//          if (x[m-1] == c)
//            for (i = m - 2; i >= 0 && x[i] == y[i + j]; --i);
//          if (i < 0) return j;
//          // c < 256 for Latin1 string, so, no need for branch
//          #ifdef SOURCE_STRING_IS_LATIN1
//          // LL case: (c< 256) always true. Remove branch
//          j += bc[y[j+m-1]];
//          #endif
//          #ifndef PATTERN_STRING_IS_UTF
//          // UU case: need if (c<ASIZE) check. Skip 1 character if not.
//          if (c < ASIZE)
//            j += bc[y[j+m-1]];
//          else
//            j += 1
//          #endif
//          #ifdef PATTERN_IS_LATIN1_AND_SOURCE_IS_UTF
//          // UL case: need if (c<ASIZE) check. Skip <pattern length> if not.
//          if (c < ASIZE)
//            j += bc[y[j+m-1]];
//          else
//            j += m
//          #endif
//       }
//    }

  if (icnt1 == -1) {
    Label BCLOOP, BCSKIP, BMLOOPSTR2, BMLOOPSTR1, BMSKIP, BMADV, BMMATCH,
        BMLOOPSTR1_LASTCMP, BMLOOPSTR1_CMP, BMLOOPSTR1_AFTER_LOAD, BM_INIT_LOOP;
    Register cnt1end = tmp2;
    Register str2end = cnt2;
    Register skipch = tmp2;

    // str1 length is >=8, so, we can read at least 1 register for cases when
    // UTF->Latin1 conversion is not needed(8 LL or 4UU) and half register for
    // UL case. We'll re-read last character in inner pre-loop code to have
    // single outer pre-loop load
    const int firstStep = isL ? 7 : 3;

    const int ASIZE = 256;
    const int STORED_BYTES = 32; // amount of bytes stored per instruction
    sub(sp, sp, ASIZE);
    mov(tmp5, ASIZE/STORED_BYTES); // loop iterations
    mov(ch1, sp);
    BIND(BM_INIT_LOOP);
      stpq(v0, v0, Address(post(ch1, STORED_BYTES)));
      subs(tmp5, tmp5, 1);
      br(GT, BM_INIT_LOOP);

      sub(cnt1tmp, cnt1, 1);
      mov(tmp5, str2);
      add(str2end, str2, result_tmp, LSL, str2_chr_shift);
      sub(ch2, cnt1, 1);
      mov(tmp3, str1);
    BIND(BCLOOP);
      (this->*str1_load_1chr)(ch1, Address(post(tmp3, str1_chr_size)));
      if (!str1_isL) {
        subs(zr, ch1, ASIZE);
        br(HS, BCSKIP);
      }
      strb(ch2, Address(sp, ch1));
    BIND(BCSKIP);
      subs(ch2, ch2, 1);
      br(GT, BCLOOP);

      add(tmp6, str1, cnt1, LSL, str1_chr_shift); // address after str1
      if (str1_isL == str2_isL) {
        // load last 8 bytes (8LL/4UU symbols)
        ldr(tmp6, Address(tmp6, -wordSize));
      } else {
        ldrw(tmp6, Address(tmp6, -wordSize/2)); // load last 4 bytes(4 symbols)
        // convert Latin1 to UTF. We'll have to wait until load completed, but
        // it's still faster than per-character loads+checks
        lsr(tmp3, tmp6, BitsPerByte * (wordSize/2 - str1_chr_size)); // str1[N-1]
        ubfx(ch1, tmp6, 8, 8); // str1[N-2]
        ubfx(ch2, tmp6, 16, 8); // str1[N-3]
        andr(tmp6, tmp6, 0xFF); // str1[N-4]
        orr(ch2, ch1, ch2, LSL, 16);
        orr(tmp6, tmp6, tmp3, LSL, 48);
        orr(tmp6, tmp6, ch2, LSL, 16);
      }
    BIND(BMLOOPSTR2);
      (this->*str2_load_1chr)(skipch, Address(str2, cnt1tmp, Address::lsl(str2_chr_shift)));
      sub(cnt1tmp, cnt1tmp, firstStep); // cnt1tmp is positive here, because cnt1 >= 8
      if (str1_isL == str2_isL) {
        // re-init tmp3. It's for free because it's executed in parallel with
        // load above. Alternative is to initialize it before loop, but it'll
        // affect performance on in-order systems with 2 or more ld/st pipelines
        lsr(tmp3, tmp6, BitsPerByte * (wordSize - str1_chr_size));
      }
      if (!isL) { // UU/UL case
        lsl(ch2, cnt1tmp, 1); // offset in bytes
      }
      cmp(tmp3, skipch);
      br(NE, BMSKIP);
      ldr(ch2, Address(str2, isL ? cnt1tmp : ch2));
      mov(ch1, tmp6);
      if (isL) {
        b(BMLOOPSTR1_AFTER_LOAD);
      } else {
        sub(cnt1tmp, cnt1tmp, 1); // no need to branch for UU/UL case. cnt1 >= 8
        b(BMLOOPSTR1_CMP);
      }
    BIND(BMLOOPSTR1);
      (this->*str1_load_1chr)(ch1, Address(str1, cnt1tmp, Address::lsl(str1_chr_shift)));
      (this->*str2_load_1chr)(ch2, Address(str2, cnt1tmp, Address::lsl(str2_chr_shift)));
    BIND(BMLOOPSTR1_AFTER_LOAD);
      subs(cnt1tmp, cnt1tmp, 1);
      br(LT, BMLOOPSTR1_LASTCMP);
    BIND(BMLOOPSTR1_CMP);
      cmp(ch1, ch2);
      br(EQ, BMLOOPSTR1);
    BIND(BMSKIP);
      if (!isL) {
        // if we've met UTF symbol while searching Latin1 pattern, then we can
        // skip cnt1 symbols
        if (str1_isL != str2_isL) {
          mov(result_tmp, cnt1);
        } else {
          mov(result_tmp, 1);
        }
        subs(zr, skipch, ASIZE);
        br(HS, BMADV);
      }
      ldrb(result_tmp, Address(sp, skipch)); // load skip distance
    BIND(BMADV);
      sub(cnt1tmp, cnt1, 1);
      add(str2, str2, result_tmp, LSL, str2_chr_shift);
      cmp(str2, str2end);
      br(LE, BMLOOPSTR2);
      add(sp, sp, ASIZE);
      b(NOMATCH);
    BIND(BMLOOPSTR1_LASTCMP);
      cmp(ch1, ch2);
      br(NE, BMSKIP);
    BIND(BMMATCH);
      sub(result, str2, tmp5);
      if (!str2_isL) lsr(result, result, 1);
      add(sp, sp, ASIZE);
      b(DONE);

    BIND(LINEARSTUB);
    cmp(cnt1, (u1)16); // small patterns still should be handled by simple algorithm
    br(LT, LINEAR_MEDIUM);
    mov(result, zr);
    RuntimeAddress stub = NULL;
    if (isL) {
      stub = RuntimeAddress(StubRoutines::aarch64::string_indexof_linear_ll());
      assert(stub.target() != NULL, "string_indexof_linear_ll stub has not been generated");
    } else if (str1_isL) {
      stub = RuntimeAddress(StubRoutines::aarch64::string_indexof_linear_ul());
       assert(stub.target() != NULL, "string_indexof_linear_ul stub has not been generated");
    } else {
      stub = RuntimeAddress(StubRoutines::aarch64::string_indexof_linear_uu());
      assert(stub.target() != NULL, "string_indexof_linear_uu stub has not been generated");
    }
    trampoline_call(stub);
    b(DONE);
  }

  BIND(LINEARSEARCH);
  {
    Label DO1, DO2, DO3;

    Register str2tmp = tmp2;
    Register first = tmp3;

    if (icnt1 == -1)
    {
        Label DOSHORT, FIRST_LOOP, STR2_NEXT, STR1_LOOP, STR1_NEXT;

        cmp(cnt1, u1(str1_isL == str2_isL ? 4 : 2));
        br(LT, DOSHORT);
      BIND(LINEAR_MEDIUM);
        (this->*str1_load_1chr)(first, Address(str1));
        lea(str1, Address(str1, cnt1, Address::lsl(str1_chr_shift)));
        sub(cnt1_neg, zr, cnt1, LSL, str1_chr_shift);
        lea(str2, Address(str2, result_tmp, Address::lsl(str2_chr_shift)));
        sub(cnt2_neg, zr, result_tmp, LSL, str2_chr_shift);

      BIND(FIRST_LOOP);
        (this->*str2_load_1chr)(ch2, Address(str2, cnt2_neg));
        cmp(first, ch2);
        br(EQ, STR1_LOOP);
      BIND(STR2_NEXT);
        adds(cnt2_neg, cnt2_neg, str2_chr_size);
        br(LE, FIRST_LOOP);
        b(NOMATCH);

      BIND(STR1_LOOP);
        adds(cnt1tmp, cnt1_neg, str1_chr_size);
        add(cnt2tmp, cnt2_neg, str2_chr_size);
        br(GE, MATCH);

      BIND(STR1_NEXT);
        (this->*str1_load_1chr)(ch1, Address(str1, cnt1tmp));
        (this->*str2_load_1chr)(ch2, Address(str2, cnt2tmp));
        cmp(ch1, ch2);
        br(NE, STR2_NEXT);
        adds(cnt1tmp, cnt1tmp, str1_chr_size);
        add(cnt2tmp, cnt2tmp, str2_chr_size);
        br(LT, STR1_NEXT);
        b(MATCH);

      BIND(DOSHORT);
      if (str1_isL == str2_isL) {
        cmp(cnt1, (u1)2);
        br(LT, DO1);
        br(GT, DO3);
      }
    }

    if (icnt1 == 4) {
      Label CH1_LOOP;

        (this->*load_4chr)(ch1, str1);
        sub(result_tmp, cnt2, 4);
        lea(str2, Address(str2, result_tmp, Address::lsl(str2_chr_shift)));
        sub(cnt2_neg, zr, result_tmp, LSL, str2_chr_shift);

      BIND(CH1_LOOP);
        (this->*load_4chr)(ch2, Address(str2, cnt2_neg));
        cmp(ch1, ch2);
        br(EQ, MATCH);
        adds(cnt2_neg, cnt2_neg, str2_chr_size);
        br(LE, CH1_LOOP);
        b(NOMATCH);
      }

    if ((icnt1 == -1 && str1_isL == str2_isL) || icnt1 == 2) {
      Label CH1_LOOP;

      BIND(DO2);
        (this->*load_2chr)(ch1, str1);
        if (icnt1 == 2) {
          sub(result_tmp, cnt2, 2);
        }
        lea(str2, Address(str2, result_tmp, Address::lsl(str2_chr_shift)));
        sub(cnt2_neg, zr, result_tmp, LSL, str2_chr_shift);
      BIND(CH1_LOOP);
        (this->*load_2chr)(ch2, Address(str2, cnt2_neg));
        cmp(ch1, ch2);
        br(EQ, MATCH);
        adds(cnt2_neg, cnt2_neg, str2_chr_size);
        br(LE, CH1_LOOP);
        b(NOMATCH);
    }

    if ((icnt1 == -1 && str1_isL == str2_isL) || icnt1 == 3) {
      Label FIRST_LOOP, STR2_NEXT, STR1_LOOP;

      BIND(DO3);
        (this->*load_2chr)(first, str1);
        (this->*str1_load_1chr)(ch1, Address(str1, 2*str1_chr_size));
        if (icnt1 == 3) {
          sub(result_tmp, cnt2, 3);
        }
        lea(str2, Address(str2, result_tmp, Address::lsl(str2_chr_shift)));
        sub(cnt2_neg, zr, result_tmp, LSL, str2_chr_shift);
      BIND(FIRST_LOOP);
        (this->*load_2chr)(ch2, Address(str2, cnt2_neg));
        cmpw(first, ch2);
        br(EQ, STR1_LOOP);
      BIND(STR2_NEXT);
        adds(cnt2_neg, cnt2_neg, str2_chr_size);
        br(LE, FIRST_LOOP);
        b(NOMATCH);

      BIND(STR1_LOOP);
        add(cnt2tmp, cnt2_neg, 2*str2_chr_size);
        (this->*str2_load_1chr)(ch2, Address(str2, cnt2tmp));
        cmp(ch1, ch2);
        br(NE, STR2_NEXT);
        b(MATCH);
    }

    if (icnt1 == -1 || icnt1 == 1) {
      Label CH1_LOOP, HAS_ZERO, DO1_SHORT, DO1_LOOP;

      BIND(DO1);
        (this->*str1_load_1chr)(ch1, str1);
        cmp(cnt2, (u1)8);
        br(LT, DO1_SHORT);

        sub(result_tmp, cnt2, 8/str2_chr_size);
        sub(cnt2_neg, zr, result_tmp, LSL, str2_chr_shift);
        mov(tmp3, str2_isL ? 0x0101010101010101 : 0x0001000100010001);
        lea(str2, Address(str2, result_tmp, Address::lsl(str2_chr_shift)));

        if (str2_isL) {
          orr(ch1, ch1, ch1, LSL, 8);
        }
        orr(ch1, ch1, ch1, LSL, 16);
        orr(ch1, ch1, ch1, LSL, 32);
      BIND(CH1_LOOP);
        ldr(ch2, Address(str2, cnt2_neg));
        eor(ch2, ch1, ch2);
        sub(tmp1, ch2, tmp3);
        orr(tmp2, ch2, str2_isL ? 0x7f7f7f7f7f7f7f7f : 0x7fff7fff7fff7fff);
        bics(tmp1, tmp1, tmp2);
        br(NE, HAS_ZERO);
        adds(cnt2_neg, cnt2_neg, 8);
        br(LT, CH1_LOOP);

        cmp(cnt2_neg, (u1)8);
        mov(cnt2_neg, 0);
        br(LT, CH1_LOOP);
        b(NOMATCH);

      BIND(HAS_ZERO);
        rev(tmp1, tmp1);
        clz(tmp1, tmp1);
        add(cnt2_neg, cnt2_neg, tmp1, LSR, 3);
        b(MATCH);

      BIND(DO1_SHORT);
        mov(result_tmp, cnt2);
        lea(str2, Address(str2, cnt2, Address::lsl(str2_chr_shift)));
        sub(cnt2_neg, zr, cnt2, LSL, str2_chr_shift);
      BIND(DO1_LOOP);
        (this->*str2_load_1chr)(ch2, Address(str2, cnt2_neg));
        cmpw(ch1, ch2);
        br(EQ, MATCH);
        adds(cnt2_neg, cnt2_neg, str2_chr_size);
        br(LT, DO1_LOOP);
    }
  }
  BIND(NOMATCH);
    mov(result, -1);
    b(DONE);
  BIND(MATCH);
    add(result, result_tmp, cnt2_neg, ASR, str2_chr_shift);
  BIND(DONE);
}

typedef void (MacroAssembler::* chr_insn)(Register Rt, const Address &adr);
typedef void (MacroAssembler::* uxt_insn)(Register Rd, Register Rn);

void C2_MacroAssembler::string_indexof_char(Register str1, Register cnt1,
                                            Register ch, Register result,
                                            Register tmp1, Register tmp2, Register tmp3)
{
  Label CH1_LOOP, HAS_ZERO, DO1_SHORT, DO1_LOOP, MATCH, NOMATCH, DONE;
  Register cnt1_neg = cnt1;
  Register ch1 = rscratch1;
  Register result_tmp = rscratch2;

  cbz(cnt1, NOMATCH);

  cmp(cnt1, (u1)4);
  br(LT, DO1_SHORT);

  orr(ch, ch, ch, LSL, 16);
  orr(ch, ch, ch, LSL, 32);

  sub(cnt1, cnt1, 4);
  mov(result_tmp, cnt1);
  lea(str1, Address(str1, cnt1, Address::uxtw(1)));
  sub(cnt1_neg, zr, cnt1, LSL, 1);

  mov(tmp3, 0x0001000100010001);

  BIND(CH1_LOOP);
    ldr(ch1, Address(str1, cnt1_neg));
    eor(ch1, ch, ch1);
    sub(tmp1, ch1, tmp3);
    orr(tmp2, ch1, 0x7fff7fff7fff7fff);
    bics(tmp1, tmp1, tmp2);
    br(NE, HAS_ZERO);
    adds(cnt1_neg, cnt1_neg, 8);
    br(LT, CH1_LOOP);

    cmp(cnt1_neg, (u1)8);
    mov(cnt1_neg, 0);
    br(LT, CH1_LOOP);
    b(NOMATCH);

  BIND(HAS_ZERO);
    rev(tmp1, tmp1);
    clz(tmp1, tmp1);
    add(cnt1_neg, cnt1_neg, tmp1, LSR, 3);
    b(MATCH);

  BIND(DO1_SHORT);
    mov(result_tmp, cnt1);
    lea(str1, Address(str1, cnt1, Address::uxtw(1)));
    sub(cnt1_neg, zr, cnt1, LSL, 1);
  BIND(DO1_LOOP);
    ldrh(ch1, Address(str1, cnt1_neg));
    cmpw(ch, ch1);
    br(EQ, MATCH);
    adds(cnt1_neg, cnt1_neg, 2);
    br(LT, DO1_LOOP);
  BIND(NOMATCH);
    mov(result, -1);
    b(DONE);
  BIND(MATCH);
    add(result, result_tmp, cnt1_neg, ASR, 1);
  BIND(DONE);
}

void C2_MacroAssembler::string_indexof_char_sve(Register str1, Register cnt1,
                                                Register ch, Register result,
                                                FloatRegister ztmp1,
                                                FloatRegister ztmp2,
                                                PRegister tmp_pg,
                                                PRegister tmp_pdn, bool isL)
{
  // Note that `tmp_pdn` should *NOT* be used as governing predicate register.
  assert(tmp_pg->is_governing(),
         "this register has to be a governing predicate register");

  Label LOOP, MATCH, DONE, NOMATCH;
  Register vec_len = rscratch1;
  Register idx = rscratch2;

  SIMD_RegVariant T = (isL == true) ? B : H;

  cbz(cnt1, NOMATCH);

  // Assign the particular char throughout the vector.
  sve_dup(ztmp2, T, ch);
  if (isL) {
    sve_cntb(vec_len);
  } else {
    sve_cnth(vec_len);
  }
  mov(idx, 0);

  // Generate a predicate to control the reading of input string.
  sve_whilelt(tmp_pg, T, idx, cnt1);

  BIND(LOOP);
    // Read a vector of 8- or 16-bit data depending on the string type. Note
    // that inactive elements indicated by the predicate register won't cause
    // a data read from memory to the destination vector.
    if (isL) {
      sve_ld1b(ztmp1, T, tmp_pg, Address(str1, idx));
    } else {
      sve_ld1h(ztmp1, T, tmp_pg, Address(str1, idx, Address::lsl(1)));
    }
    add(idx, idx, vec_len);

    // Perform the comparison. An element of the destination predicate is set
    // to active if the particular char is matched.
    sve_cmpeq(tmp_pdn, T, tmp_pg, ztmp1, ztmp2);

    // Branch if the particular char is found.
    br(NE, MATCH);

    sve_whilelt(tmp_pg, T, idx, cnt1);

    // Loop back if the particular char not found.
    br(MI, LOOP);

  BIND(NOMATCH);
    mov(result, -1);
    b(DONE);

  BIND(MATCH);
    // Undo the index increment.
    sub(idx, idx, vec_len);

    // Crop the vector to find its location.
    sve_brka(tmp_pdn, tmp_pg, tmp_pdn, false /* isMerge */);
    add(result, idx, -1);
    sve_incp(result, T, tmp_pdn);
  BIND(DONE);
}

void C2_MacroAssembler::stringL_indexof_char(Register str1, Register cnt1,
                                            Register ch, Register result,
                                            Register tmp1, Register tmp2, Register tmp3)
{
  Label CH1_LOOP, HAS_ZERO, DO1_SHORT, DO1_LOOP, MATCH, NOMATCH, DONE;
  Register cnt1_neg = cnt1;
  Register ch1 = rscratch1;
  Register result_tmp = rscratch2;

  cbz(cnt1, NOMATCH);

  cmp(cnt1, (u1)8);
  br(LT, DO1_SHORT);

  orr(ch, ch, ch, LSL, 8);
  orr(ch, ch, ch, LSL, 16);
  orr(ch, ch, ch, LSL, 32);

  sub(cnt1, cnt1, 8);
  mov(result_tmp, cnt1);
  lea(str1, Address(str1, cnt1));
  sub(cnt1_neg, zr, cnt1);

  mov(tmp3, 0x0101010101010101);

  BIND(CH1_LOOP);
    ldr(ch1, Address(str1, cnt1_neg));
    eor(ch1, ch, ch1);
    sub(tmp1, ch1, tmp3);
    orr(tmp2, ch1, 0x7f7f7f7f7f7f7f7f);
    bics(tmp1, tmp1, tmp2);
    br(NE, HAS_ZERO);
    adds(cnt1_neg, cnt1_neg, 8);
    br(LT, CH1_LOOP);

    cmp(cnt1_neg, (u1)8);
    mov(cnt1_neg, 0);
    br(LT, CH1_LOOP);
    b(NOMATCH);

  BIND(HAS_ZERO);
    rev(tmp1, tmp1);
    clz(tmp1, tmp1);
    add(cnt1_neg, cnt1_neg, tmp1, LSR, 3);
    b(MATCH);

  BIND(DO1_SHORT);
    mov(result_tmp, cnt1);
    lea(str1, Address(str1, cnt1));
    sub(cnt1_neg, zr, cnt1);
  BIND(DO1_LOOP);
    ldrb(ch1, Address(str1, cnt1_neg));
    cmp(ch, ch1);
    br(EQ, MATCH);
    adds(cnt1_neg, cnt1_neg, 1);
    br(LT, DO1_LOOP);
  BIND(NOMATCH);
    mov(result, -1);
    b(DONE);
  BIND(MATCH);
    add(result, result_tmp, cnt1_neg);
  BIND(DONE);
}

// Compare strings.
void C2_MacroAssembler::string_compare(Register str1, Register str2,
    Register cnt1, Register cnt2, Register result, Register tmp1, Register tmp2,
    FloatRegister vtmp1, FloatRegister vtmp2, FloatRegister vtmp3, int ae) {
  Label DONE, SHORT_LOOP, SHORT_STRING, SHORT_LAST, TAIL, STUB,
      DIFF, NEXT_WORD, SHORT_LOOP_TAIL, SHORT_LAST2, SHORT_LAST_INIT,
      SHORT_LOOP_START, TAIL_CHECK;

  bool isLL = ae == StrIntrinsicNode::LL;
  bool isLU = ae == StrIntrinsicNode::LU;
  bool isUL = ae == StrIntrinsicNode::UL;

  // The stub threshold for LL strings is: 72 (64 + 8) chars
  // UU: 36 chars, or 72 bytes (valid for the 64-byte large loop with prefetch)
  // LU/UL: 24 chars, or 48 bytes (valid for the 16-character loop at least)
  const u1 stub_threshold = isLL ? 72 : ((isLU || isUL) ? 24 : 36);

  bool str1_isL = isLL || isLU;
  bool str2_isL = isLL || isUL;

  int str1_chr_shift = str1_isL ? 0 : 1;
  int str2_chr_shift = str2_isL ? 0 : 1;
  int str1_chr_size = str1_isL ? 1 : 2;
  int str2_chr_size = str2_isL ? 1 : 2;
  int minCharsInWord = isLL ? wordSize : wordSize/2;

  FloatRegister vtmpZ = vtmp1, vtmp = vtmp2;
  chr_insn str1_load_chr = str1_isL ? (chr_insn)&MacroAssembler::ldrb :
                                      (chr_insn)&MacroAssembler::ldrh;
  chr_insn str2_load_chr = str2_isL ? (chr_insn)&MacroAssembler::ldrb :
                                      (chr_insn)&MacroAssembler::ldrh;
  uxt_insn ext_chr = isLL ? (uxt_insn)&MacroAssembler::uxtbw :
                            (uxt_insn)&MacroAssembler::uxthw;

  BLOCK_COMMENT("string_compare {");

  // Bizzarely, the counts are passed in bytes, regardless of whether they
  // are L or U strings, however the result is always in characters.
  if (!str1_isL) asrw(cnt1, cnt1, 1);
  if (!str2_isL) asrw(cnt2, cnt2, 1);

  // Compute the minimum of the string lengths and save the difference.
  subsw(result, cnt1, cnt2);
  cselw(cnt2, cnt1, cnt2, Assembler::LE); // min

  // A very short string
  cmpw(cnt2, minCharsInWord);
  br(Assembler::LE, SHORT_STRING);

  // Compare longwords
  // load first parts of strings and finish initialization while loading
  {
    if (str1_isL == str2_isL) { // LL or UU
      ldr(tmp1, Address(str1));
      cmp(str1, str2);
      br(Assembler::EQ, DONE);
      ldr(tmp2, Address(str2));
      cmp(cnt2, stub_threshold);
      br(GE, STUB);
      subsw(cnt2, cnt2, minCharsInWord);
      br(EQ, TAIL_CHECK);
      lea(str2, Address(str2, cnt2, Address::uxtw(str2_chr_shift)));
      lea(str1, Address(str1, cnt2, Address::uxtw(str1_chr_shift)));
      sub(cnt2, zr, cnt2, LSL, str2_chr_shift);
    } else if (isLU) {
      ldrs(vtmp, Address(str1));
      ldr(tmp2, Address(str2));
      cmp(cnt2, stub_threshold);
      br(GE, STUB);
      subw(cnt2, cnt2, 4);
      eor(vtmpZ, T16B, vtmpZ, vtmpZ);
      lea(str1, Address(str1, cnt2, Address::uxtw(str1_chr_shift)));
      lea(str2, Address(str2, cnt2, Address::uxtw(str2_chr_shift)));
      zip1(vtmp, T8B, vtmp, vtmpZ);
      sub(cnt1, zr, cnt2, LSL, str1_chr_shift);
      sub(cnt2, zr, cnt2, LSL, str2_chr_shift);
      add(cnt1, cnt1, 4);
      fmovd(tmp1, vtmp);
    } else { // UL case
      ldr(tmp1, Address(str1));
      ldrs(vtmp, Address(str2));
      cmp(cnt2, stub_threshold);
      br(GE, STUB);
      subw(cnt2, cnt2, 4);
      lea(str1, Address(str1, cnt2, Address::uxtw(str1_chr_shift)));
      eor(vtmpZ, T16B, vtmpZ, vtmpZ);
      lea(str2, Address(str2, cnt2, Address::uxtw(str2_chr_shift)));
      sub(cnt1, zr, cnt2, LSL, str1_chr_shift);
      zip1(vtmp, T8B, vtmp, vtmpZ);
      sub(cnt2, zr, cnt2, LSL, str2_chr_shift);
      add(cnt1, cnt1, 8);
      fmovd(tmp2, vtmp);
    }
    adds(cnt2, cnt2, isUL ? 4 : 8);
    br(GE, TAIL);
    eor(rscratch2, tmp1, tmp2);
    cbnz(rscratch2, DIFF);
    // main loop
    bind(NEXT_WORD);
    if (str1_isL == str2_isL) {
      ldr(tmp1, Address(str1, cnt2));
      ldr(tmp2, Address(str2, cnt2));
      adds(cnt2, cnt2, 8);
    } else if (isLU) {
      ldrs(vtmp, Address(str1, cnt1));
      ldr(tmp2, Address(str2, cnt2));
      add(cnt1, cnt1, 4);
      zip1(vtmp, T8B, vtmp, vtmpZ);
      fmovd(tmp1, vtmp);
      adds(cnt2, cnt2, 8);
    } else { // UL
      ldrs(vtmp, Address(str2, cnt2));
      ldr(tmp1, Address(str1, cnt1));
      zip1(vtmp, T8B, vtmp, vtmpZ);
      add(cnt1, cnt1, 8);
      fmovd(tmp2, vtmp);
      adds(cnt2, cnt2, 4);
    }
    br(GE, TAIL);

    eor(rscratch2, tmp1, tmp2);
    cbz(rscratch2, NEXT_WORD);
    b(DIFF);
    bind(TAIL);
    eor(rscratch2, tmp1, tmp2);
    cbnz(rscratch2, DIFF);
    // Last longword.  In the case where length == 4 we compare the
    // same longword twice, but that's still faster than another
    // conditional branch.
    if (str1_isL == str2_isL) {
      ldr(tmp1, Address(str1));
      ldr(tmp2, Address(str2));
    } else if (isLU) {
      ldrs(vtmp, Address(str1));
      ldr(tmp2, Address(str2));
      zip1(vtmp, T8B, vtmp, vtmpZ);
      fmovd(tmp1, vtmp);
    } else { // UL
      ldrs(vtmp, Address(str2));
      ldr(tmp1, Address(str1));
      zip1(vtmp, T8B, vtmp, vtmpZ);
      fmovd(tmp2, vtmp);
    }
    bind(TAIL_CHECK);
    eor(rscratch2, tmp1, tmp2);
    cbz(rscratch2, DONE);

    // Find the first different characters in the longwords and
    // compute their difference.
    bind(DIFF);
    rev(rscratch2, rscratch2);
    clz(rscratch2, rscratch2);
    andr(rscratch2, rscratch2, isLL ? -8 : -16);
    lsrv(tmp1, tmp1, rscratch2);
    (this->*ext_chr)(tmp1, tmp1);
    lsrv(tmp2, tmp2, rscratch2);
    (this->*ext_chr)(tmp2, tmp2);
    subw(result, tmp1, tmp2);
    b(DONE);
  }

  bind(STUB);
    RuntimeAddress stub = NULL;
    switch(ae) {
      case StrIntrinsicNode::LL:
        stub = RuntimeAddress(StubRoutines::aarch64::compare_long_string_LL());
        break;
      case StrIntrinsicNode::UU:
        stub = RuntimeAddress(StubRoutines::aarch64::compare_long_string_UU());
        break;
      case StrIntrinsicNode::LU:
        stub = RuntimeAddress(StubRoutines::aarch64::compare_long_string_LU());
        break;
      case StrIntrinsicNode::UL:
        stub = RuntimeAddress(StubRoutines::aarch64::compare_long_string_UL());
        break;
      default:
        ShouldNotReachHere();
     }
    assert(stub.target() != NULL, "compare_long_string stub has not been generated");
    trampoline_call(stub);
    b(DONE);

  bind(SHORT_STRING);
  // Is the minimum length zero?
  cbz(cnt2, DONE);
  // arrange code to do most branches while loading and loading next characters
  // while comparing previous
  (this->*str1_load_chr)(tmp1, Address(post(str1, str1_chr_size)));
  subs(cnt2, cnt2, 1);
  br(EQ, SHORT_LAST_INIT);
  (this->*str2_load_chr)(cnt1, Address(post(str2, str2_chr_size)));
  b(SHORT_LOOP_START);
  bind(SHORT_LOOP);
  subs(cnt2, cnt2, 1);
  br(EQ, SHORT_LAST);
  bind(SHORT_LOOP_START);
  (this->*str1_load_chr)(tmp2, Address(post(str1, str1_chr_size)));
  (this->*str2_load_chr)(rscratch1, Address(post(str2, str2_chr_size)));
  cmp(tmp1, cnt1);
  br(NE, SHORT_LOOP_TAIL);
  subs(cnt2, cnt2, 1);
  br(EQ, SHORT_LAST2);
  (this->*str1_load_chr)(tmp1, Address(post(str1, str1_chr_size)));
  (this->*str2_load_chr)(cnt1, Address(post(str2, str2_chr_size)));
  cmp(tmp2, rscratch1);
  br(EQ, SHORT_LOOP);
  sub(result, tmp2, rscratch1);
  b(DONE);
  bind(SHORT_LOOP_TAIL);
  sub(result, tmp1, cnt1);
  b(DONE);
  bind(SHORT_LAST2);
  cmp(tmp2, rscratch1);
  br(EQ, DONE);
  sub(result, tmp2, rscratch1);

  b(DONE);
  bind(SHORT_LAST_INIT);
  (this->*str2_load_chr)(cnt1, Address(post(str2, str2_chr_size)));
  bind(SHORT_LAST);
  cmp(tmp1, cnt1);
  br(EQ, DONE);
  sub(result, tmp1, cnt1);

  bind(DONE);

  BLOCK_COMMENT("} string_compare");
}

void C2_MacroAssembler::neon_compare(FloatRegister dst, BasicType bt, FloatRegister src1,
                                     FloatRegister src2, int cond, bool isQ) {
  SIMD_Arrangement size = esize2arrangement(type2aelembytes(bt), isQ);
  if (bt == T_FLOAT || bt == T_DOUBLE) {
    switch (cond) {
      case BoolTest::eq: fcmeq(dst, size, src1, src2); break;
      case BoolTest::ne: {
        fcmeq(dst, size, src1, src2);
        notr(dst, T16B, dst);
        break;
      }
      case BoolTest::ge: fcmge(dst, size, src1, src2); break;
      case BoolTest::gt: fcmgt(dst, size, src1, src2); break;
      case BoolTest::le: fcmge(dst, size, src2, src1); break;
      case BoolTest::lt: fcmgt(dst, size, src2, src1); break;
      default:
        assert(false, "unsupported");
        ShouldNotReachHere();
    }
  } else {
    switch (cond) {
      case BoolTest::eq: cmeq(dst, size, src1, src2); break;
      case BoolTest::ne: {
        cmeq(dst, size, src1, src2);
        notr(dst, T16B, dst);
        break;
      }
      case BoolTest::ge: cmge(dst, size, src1, src2); break;
      case BoolTest::gt: cmgt(dst, size, src1, src2); break;
      case BoolTest::le: cmge(dst, size, src2, src1); break;
      case BoolTest::lt: cmgt(dst, size, src2, src1); break;
      case BoolTest::uge: cmhs(dst, size, src1, src2); break;
      case BoolTest::ugt: cmhi(dst, size, src1, src2); break;
      case BoolTest::ult: cmhi(dst, size, src2, src1); break;
      case BoolTest::ule: cmhs(dst, size, src2, src1); break;
      default:
        assert(false, "unsupported");
        ShouldNotReachHere();
    }
  }
}
