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

#include "precompiled.hpp"
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "opto/c2_MacroAssembler.hpp"
#include "opto/intrinsicnode.hpp"
#include "runtime/stubRoutines.hpp"

#define BLOCK_COMMENT(str) block_comment(str)
#define BIND(label)        bind(label); BLOCK_COMMENT(#label ":")

//------------------------------------------------------
//   Special String Intrinsics. Implementation
//------------------------------------------------------

// Intrinsics for CompactStrings

// Compress char[] to byte[].
//   Restores: src, dst
//   Uses:     cnt
//   Kills:    tmp, Z_R0, Z_R1.
//   Early clobber: result.
// Note:
//   cnt is signed int. Do not rely on high word!
//       counts # characters, not bytes.
// The result is the number of characters copied before the first incompatible character was found.
// If precise is true, the processing stops exactly at this point. Otherwise, the result may be off
// by a few bytes. The result always indicates the number of copied characters.
// When used as a character index, the returned value points to the first incompatible character.
//
// Note: Does not behave exactly like package private StringUTF16 compress java implementation in case of failure:
// - Different number of characters may have been written to dead array (if precise is false).
// - Returns a number <cnt instead of 0. (Result gets compared with cnt.)
unsigned int C2_MacroAssembler::string_compress(Register result, Register src, Register dst, Register cnt,
                                                Register tmp,    bool precise) {
  assert_different_registers(Z_R0, Z_R1, result, src, dst, cnt, tmp);

  if (precise) {
    BLOCK_COMMENT("encode_iso_array {");
  } else {
    BLOCK_COMMENT("string_compress {");
  }
  int  block_start = offset();

  Register       Rsrc  = src;
  Register       Rdst  = dst;
  Register       Rix   = tmp;
  Register       Rcnt  = cnt;
  Register       Rmask = result;  // holds incompatibility check mask until result value is stored.
  Label          ScalarShortcut, AllDone;

  z_iilf(Rmask, 0xFF00FF00);
  z_iihf(Rmask, 0xFF00FF00);

#if 0  // Sacrifice shortcuts for code compactness
  {
    //---<  shortcuts for short strings (very frequent)   >---
    //   Strings with 4 and 8 characters were fond to occur very frequently.
    //   Therefore, we handle them right away with minimal overhead.
    Label     skipShortcut, skip4Shortcut, skip8Shortcut;
    Register  Rout = Z_R0;
    z_chi(Rcnt, 4);
    z_brne(skip4Shortcut);                 // 4 characters are very frequent
      z_lg(Z_R0, 0, Rsrc);                 // Treat exactly 4 characters specially.
      if (VM_Version::has_DistinctOpnds()) {
        Rout = Z_R0;
        z_ngrk(Rix, Z_R0, Rmask);
      } else {
        Rout = Rix;
        z_lgr(Rix, Z_R0);
        z_ngr(Z_R0, Rmask);
      }
      z_brnz(skipShortcut);
      z_stcmh(Rout, 5, 0, Rdst);
      z_stcm(Rout,  5, 2, Rdst);
      z_lgfr(result, Rcnt);
      z_bru(AllDone);
    bind(skip4Shortcut);

    z_chi(Rcnt, 8);
    z_brne(skip8Shortcut);                 // There's more to do...
      z_lmg(Z_R0, Z_R1, 0, Rsrc);          // Treat exactly 8 characters specially.
      if (VM_Version::has_DistinctOpnds()) {
        Rout = Z_R0;
        z_ogrk(Rix, Z_R0, Z_R1);
        z_ngr(Rix, Rmask);
      } else {
        Rout = Rix;
        z_lgr(Rix, Z_R0);
        z_ogr(Z_R0, Z_R1);
        z_ngr(Z_R0, Rmask);
      }
      z_brnz(skipShortcut);
      z_stcmh(Rout, 5, 0, Rdst);
      z_stcm(Rout,  5, 2, Rdst);
      z_stcmh(Z_R1, 5, 4, Rdst);
      z_stcm(Z_R1,  5, 6, Rdst);
      z_lgfr(result, Rcnt);
      z_bru(AllDone);

    bind(skip8Shortcut);
    clear_reg(Z_R0, true, false);          // #characters already processed (none). Precond for scalar loop.
    z_brl(ScalarShortcut);                 // Just a few characters

    bind(skipShortcut);
  }
#endif
  clear_reg(Z_R0);                         // make sure register is properly initialized.

  if (VM_Version::has_VectorFacility()) {
    const int  min_vcnt     = 32;          // Minimum #characters required to use vector instructions.
                                           // Otherwise just do nothing in vector mode.
                                           // Must be multiple of 2*(vector register length in chars (8 HW = 128 bits)).
    const int  log_min_vcnt = exact_log2(min_vcnt);
    Label      VectorLoop, VectorDone, VectorBreak;

    VectorRegister Vtmp1      = Z_V16;
    VectorRegister Vtmp2      = Z_V17;
    VectorRegister Vmask      = Z_V18;
    VectorRegister Vzero      = Z_V19;
    VectorRegister Vsrc_first = Z_V20;
    VectorRegister Vsrc_last  = Z_V23;

    assert((Vsrc_last->encoding() - Vsrc_first->encoding() + 1) == min_vcnt/8, "logic error");
    assert(VM_Version::has_DistinctOpnds(), "Assumption when has_VectorFacility()");
    z_srak(Rix, Rcnt, log_min_vcnt);       // # vector loop iterations
    z_brz(VectorDone);                     // not enough data for vector loop

    z_vzero(Vzero);                        // all zeroes
    z_vgmh(Vmask, 0, 7);                   // generate 0xff00 mask for all 2-byte elements
    z_sllg(Z_R0, Rix, log_min_vcnt);       // remember #chars that will be processed by vector loop

    bind(VectorLoop);
      z_vlm(Vsrc_first, Vsrc_last, 0, Rsrc);
      add2reg(Rsrc, min_vcnt*2);

      //---<  check for incompatible character  >---
      z_vo(Vtmp1, Z_V20, Z_V21);
      z_vo(Vtmp2, Z_V22, Z_V23);
      z_vo(Vtmp1, Vtmp1, Vtmp2);
      z_vn(Vtmp1, Vtmp1, Vmask);
      z_vceqhs(Vtmp1, Vtmp1, Vzero);       // high half of all chars must be zero for successful compress.
      z_bvnt(VectorBreak);                 // break vector loop if not all vector elements compare eq -> incompatible character found.
                                           // re-process data from current iteration in break handler.

      //---<  pack & store characters  >---
      z_vpkh(Vtmp1, Z_V20, Z_V21);         // pack (src1, src2) -> tmp1
      z_vpkh(Vtmp2, Z_V22, Z_V23);         // pack (src3, src4) -> tmp2
      z_vstm(Vtmp1, Vtmp2, 0, Rdst);       // store packed string
      add2reg(Rdst, min_vcnt);

      z_brct(Rix, VectorLoop);

    z_bru(VectorDone);

    bind(VectorBreak);
      add2reg(Rsrc, -min_vcnt*2);          // Fix Rsrc. Rsrc was already updated, but Rdst and Rix are not.
      z_sll(Rix, log_min_vcnt);            // # chars processed so far in VectorLoop, excl. current iteration.
      z_sr(Z_R0, Rix);                     // correct # chars processed in total.

    bind(VectorDone);
  }

  {
    const int  min_cnt     =  8;           // Minimum #characters required to use unrolled loop.
                                           // Otherwise just do nothing in unrolled loop.
                                           // Must be multiple of 8.
    const int  log_min_cnt = exact_log2(min_cnt);
    Label      UnrolledLoop, UnrolledDone, UnrolledBreak;

    if (VM_Version::has_DistinctOpnds()) {
      z_srk(Rix, Rcnt, Z_R0);              // remaining # chars to compress in unrolled loop
    } else {
      z_lr(Rix, Rcnt);
      z_sr(Rix, Z_R0);
    }
    z_sra(Rix, log_min_cnt);             // unrolled loop count
    z_brz(UnrolledDone);

    bind(UnrolledLoop);
      z_lmg(Z_R0, Z_R1, 0, Rsrc);
      if (precise) {
        z_ogr(Z_R1, Z_R0);                 // check all 8 chars for incompatibility
        z_ngr(Z_R1, Rmask);
        z_brnz(UnrolledBreak);

        z_lg(Z_R1, 8, Rsrc);               // reload destroyed register
        z_stcmh(Z_R0, 5, 0, Rdst);
        z_stcm(Z_R0,  5, 2, Rdst);
      } else {
        z_stcmh(Z_R0, 5, 0, Rdst);
        z_stcm(Z_R0,  5, 2, Rdst);

        z_ogr(Z_R0, Z_R1);
        z_ngr(Z_R0, Rmask);
        z_brnz(UnrolledBreak);
      }
      z_stcmh(Z_R1, 5, 4, Rdst);
      z_stcm(Z_R1,  5, 6, Rdst);

      add2reg(Rsrc, min_cnt*2);
      add2reg(Rdst, min_cnt);
      z_brct(Rix, UnrolledLoop);

    z_lgfr(Z_R0, Rcnt);                    // # chars processed in total after unrolled loop.
    z_nilf(Z_R0, ~(min_cnt-1));
    z_tmll(Rcnt, min_cnt-1);
    z_brnaz(ScalarShortcut);               // if all bits zero, there is nothing left to do for scalar loop.
                                           // Rix == 0 in all cases.
    z_sllg(Z_R1, Rcnt, 1);                 // # src bytes already processed. Only lower 32 bits are valid!
                                           //   Z_R1 contents must be treated as unsigned operand! For huge strings,
                                           //   (Rcnt >= 2**30), the value may spill into the sign bit by sllg.
    z_lgfr(result, Rcnt);                  // all characters processed.
    z_slgfr(Rdst, Rcnt);                   // restore ptr
    z_slgfr(Rsrc, Z_R1);                   // restore ptr, double the element count for Rsrc restore
    z_bru(AllDone);

    bind(UnrolledBreak);
    z_lgfr(Z_R0, Rcnt);                    // # chars processed in total after unrolled loop
    z_nilf(Z_R0, ~(min_cnt-1));
    z_sll(Rix, log_min_cnt);               // # chars not yet processed in UnrolledLoop (due to break), broken iteration not included.
    z_sr(Z_R0, Rix);                       // fix # chars processed OK so far.
    if (!precise) {
      z_lgfr(result, Z_R0);
      z_sllg(Z_R1, Z_R0, 1);               // # src bytes already processed. Only lower 32 bits are valid!
                                           //   Z_R1 contents must be treated as unsigned operand! For huge strings,
                                           //   (Rcnt >= 2**30), the value may spill into the sign bit by sllg.
      z_aghi(result, min_cnt/2);           // min_cnt/2 characters have already been written
                                           // but ptrs were not updated yet.
      z_slgfr(Rdst, Z_R0);                 // restore ptr
      z_slgfr(Rsrc, Z_R1);                 // restore ptr, double the element count for Rsrc restore
      z_bru(AllDone);
    }
    bind(UnrolledDone);
  }

  {
    Label     ScalarLoop, ScalarDone, ScalarBreak;

    bind(ScalarShortcut);
    z_ltgfr(result, Rcnt);
    z_brz(AllDone);

#if 0  // Sacrifice shortcuts for code compactness
    {
      //---<  Special treatment for very short strings (one or two characters)  >---
      //   For these strings, we are sure that the above code was skipped.
      //   Thus, no registers were modified, register restore is not required.
      Label     ScalarDoit, Scalar2Char;
      z_chi(Rcnt, 2);
      z_brh(ScalarDoit);
      z_llh(Z_R1,  0, Z_R0, Rsrc);
      z_bre(Scalar2Char);
      z_tmll(Z_R1, 0xff00);
      z_lghi(result, 0);                   // cnt == 1, first char invalid, no chars successfully processed
      z_brnaz(AllDone);
      z_stc(Z_R1,  0, Z_R0, Rdst);
      z_lghi(result, 1);
      z_bru(AllDone);

      bind(Scalar2Char);
      z_llh(Z_R0,  2, Z_R0, Rsrc);
      z_tmll(Z_R1, 0xff00);
      z_lghi(result, 0);                   // cnt == 2, first char invalid, no chars successfully processed
      z_brnaz(AllDone);
      z_stc(Z_R1,  0, Z_R0, Rdst);
      z_tmll(Z_R0, 0xff00);
      z_lghi(result, 1);                   // cnt == 2, second char invalid, one char successfully processed
      z_brnaz(AllDone);
      z_stc(Z_R0,  1, Z_R0, Rdst);
      z_lghi(result, 2);
      z_bru(AllDone);

      bind(ScalarDoit);
    }
#endif

    if (VM_Version::has_DistinctOpnds()) {
      z_srk(Rix, Rcnt, Z_R0);              // remaining # chars to compress in unrolled loop
    } else {
      z_lr(Rix, Rcnt);
      z_sr(Rix, Z_R0);
    }
    z_lgfr(result, Rcnt);                  // # processed characters (if all runs ok).
    z_brz(ScalarDone);                     // uses CC from Rix calculation

    bind(ScalarLoop);
      z_llh(Z_R1, 0, Z_R0, Rsrc);
      z_tmll(Z_R1, 0xff00);
      z_brnaz(ScalarBreak);
      z_stc(Z_R1, 0, Z_R0, Rdst);
      add2reg(Rsrc, 2);
      add2reg(Rdst, 1);
      z_brct(Rix, ScalarLoop);

    z_bru(ScalarDone);

    bind(ScalarBreak);
    z_sr(result, Rix);

    bind(ScalarDone);
    z_sgfr(Rdst, result);                  // restore ptr
    z_sgfr(Rsrc, result);                  // restore ptr, double the element count for Rsrc restore
    z_sgfr(Rsrc, result);
  }
  bind(AllDone);

  if (precise) {
    BLOCK_COMMENT("} encode_iso_array");
  } else {
    BLOCK_COMMENT("} string_compress");
  }
  return offset() - block_start;
}

// Inflate byte[] to char[].
unsigned int C2_MacroAssembler::string_inflate_trot(Register src, Register dst, Register cnt, Register tmp) {
  int block_start = offset();

  BLOCK_COMMENT("string_inflate {");

  Register stop_char = Z_R0;
  Register table     = Z_R1;
  Register src_addr  = tmp;

  assert_different_registers(Z_R0, Z_R1, tmp, src, dst, cnt);
  assert(dst->encoding()%2 == 0, "must be even reg");
  assert(cnt->encoding()%2 == 1, "must be odd reg");
  assert(cnt->encoding() - dst->encoding() == 1, "must be even/odd pair");

  StubRoutines::zarch::generate_load_trot_table_addr(this, table);  // kills Z_R0 (if ASSERT)
  clear_reg(stop_char);  // Stop character. Not used here, but initialized to have a defined value.
  lgr_if_needed(src_addr, src);
  z_llgfr(cnt, cnt);     // # src characters, must be a positive simm32.

  translate_ot(dst, src_addr, /* mask = */ 0x0001);

  BLOCK_COMMENT("} string_inflate");

  return offset() - block_start;
}

// Inflate byte[] to char[].
//   Restores: src, dst
//   Uses:     cnt
//   Kills:    tmp, Z_R0, Z_R1.
// Note:
//   cnt is signed int. Do not rely on high word!
//       counts # characters, not bytes.
unsigned int C2_MacroAssembler::string_inflate(Register src, Register dst, Register cnt, Register tmp) {
  assert_different_registers(Z_R0, Z_R1, src, dst, cnt, tmp);

  BLOCK_COMMENT("string_inflate {");
  int block_start = offset();

  Register   Rcnt = cnt;   // # characters (src: bytes, dst: char (2-byte)), remaining after current loop.
  Register   Rix  = tmp;   // loop index
  Register   Rsrc = src;   // addr(src array)
  Register   Rdst = dst;   // addr(dst array)
  Label      ScalarShortcut, AllDone;

#if 0  // Sacrifice shortcuts for code compactness
  {
    //---<  shortcuts for short strings (very frequent)   >---
    Label   skipShortcut, skip4Shortcut;
    z_ltr(Rcnt, Rcnt);                     // absolutely nothing to do for strings of len == 0.
    z_brz(AllDone);
    clear_reg(Z_R0);                       // make sure registers are properly initialized.
    clear_reg(Z_R1);
    z_chi(Rcnt, 4);
    z_brne(skip4Shortcut);                 // 4 characters are very frequent
      z_icm(Z_R0, 5,    0, Rsrc);          // Treat exactly 4 characters specially.
      z_icm(Z_R1, 5,    2, Rsrc);
      z_stm(Z_R0, Z_R1, 0, Rdst);
      z_bru(AllDone);
    bind(skip4Shortcut);

    z_chi(Rcnt, 8);
    z_brh(skipShortcut);                   // There's a lot to do...
    z_lgfr(Z_R0, Rcnt);                    // remaining #characters (<= 8). Precond for scalar loop.
                                           // This does not destroy the "register cleared" state of Z_R0.
    z_brl(ScalarShortcut);                 // Just a few characters
      z_icmh(Z_R0, 5, 0, Rsrc);            // Treat exactly 8 characters specially.
      z_icmh(Z_R1, 5, 4, Rsrc);
      z_icm(Z_R0,  5, 2, Rsrc);
      z_icm(Z_R1,  5, 6, Rsrc);
      z_stmg(Z_R0, Z_R1, 0, Rdst);
      z_bru(AllDone);
    bind(skipShortcut);
  }
#endif
  clear_reg(Z_R0);                         // make sure register is properly initialized.

  if (VM_Version::has_VectorFacility()) {
    const int  min_vcnt     = 32;          // Minimum #characters required to use vector instructions.
                                           // Otherwise just do nothing in vector mode.
                                           // Must be multiple of vector register length (16 bytes = 128 bits).
    const int  log_min_vcnt = exact_log2(min_vcnt);
    Label      VectorLoop, VectorDone;

    assert(VM_Version::has_DistinctOpnds(), "Assumption when has_VectorFacility()");
    z_srak(Rix, Rcnt, log_min_vcnt);       // calculate # vector loop iterations
    z_brz(VectorDone);                     // skip if none

    z_sllg(Z_R0, Rix, log_min_vcnt);       // remember #chars that will be processed by vector loop

    bind(VectorLoop);
      z_vlm(Z_V20, Z_V21, 0, Rsrc);        // get next 32 characters (single-byte)
      add2reg(Rsrc, min_vcnt);

      z_vuplhb(Z_V22, Z_V20);              // V2 <- (expand) V0(high)
      z_vupllb(Z_V23, Z_V20);              // V3 <- (expand) V0(low)
      z_vuplhb(Z_V24, Z_V21);              // V4 <- (expand) V1(high)
      z_vupllb(Z_V25, Z_V21);              // V5 <- (expand) V1(low)
      z_vstm(Z_V22, Z_V25, 0, Rdst);       // store next 32 bytes
      add2reg(Rdst, min_vcnt*2);

      z_brct(Rix, VectorLoop);

    bind(VectorDone);
  }

  const int  min_cnt     =  8;             // Minimum #characters required to use unrolled scalar loop.
                                           // Otherwise just do nothing in unrolled scalar mode.
                                           // Must be multiple of 8.
  {
    const int  log_min_cnt = exact_log2(min_cnt);
    Label      UnrolledLoop, UnrolledDone;


    if (VM_Version::has_DistinctOpnds()) {
      z_srk(Rix, Rcnt, Z_R0);              // remaining # chars to process in unrolled loop
    } else {
      z_lr(Rix, Rcnt);
      z_sr(Rix, Z_R0);
    }
    z_sra(Rix, log_min_cnt);               // unrolled loop count
    z_brz(UnrolledDone);

    clear_reg(Z_R0);
    clear_reg(Z_R1);

    bind(UnrolledLoop);
      z_icmh(Z_R0, 5, 0, Rsrc);
      z_icmh(Z_R1, 5, 4, Rsrc);
      z_icm(Z_R0,  5, 2, Rsrc);
      z_icm(Z_R1,  5, 6, Rsrc);
      add2reg(Rsrc, min_cnt);

      z_stmg(Z_R0, Z_R1, 0, Rdst);

      add2reg(Rdst, min_cnt*2);
      z_brct(Rix, UnrolledLoop);

    bind(UnrolledDone);
    z_lgfr(Z_R0, Rcnt);                    // # chars left over after unrolled loop.
    z_nilf(Z_R0, min_cnt-1);
    z_brnz(ScalarShortcut);                // if zero, there is nothing left to do for scalar loop.
                                           // Rix == 0 in all cases.
    z_sgfr(Z_R0, Rcnt);                    // negative # characters the ptrs have been advanced previously.
    z_agr(Rdst, Z_R0);                     // restore ptr, double the element count for Rdst restore.
    z_agr(Rdst, Z_R0);
    z_agr(Rsrc, Z_R0);                     // restore ptr.
    z_bru(AllDone);
  }

  {
    bind(ScalarShortcut);
    // Z_R0 must contain remaining # characters as 64-bit signed int here.
    //      register contents is preserved over scalar processing (for register fixup).

#if 0  // Sacrifice shortcuts for code compactness
    {
      Label      ScalarDefault;
      z_chi(Rcnt, 2);
      z_brh(ScalarDefault);
      z_llc(Z_R0,  0, Z_R0, Rsrc);     // 6 bytes
      z_sth(Z_R0,  0, Z_R0, Rdst);     // 4 bytes
      z_brl(AllDone);
      z_llc(Z_R0,  1, Z_R0, Rsrc);     // 6 bytes
      z_sth(Z_R0,  2, Z_R0, Rdst);     // 4 bytes
      z_bru(AllDone);
      bind(ScalarDefault);
    }
#endif

    Label   CodeTable;
    // Some comments on Rix calculation:
    //  - Rcnt is small, therefore no bits shifted out of low word (sll(g) instructions).
    //  - high word of both Rix and Rcnt may contain garbage
    //  - the final lngfr takes care of that garbage, extending the sign to high word
    z_sllg(Rix, Z_R0, 2);                // calculate 10*Rix = (4*Rix + Rix)*2
    z_ar(Rix, Z_R0);
    z_larl(Z_R1, CodeTable);
    z_sll(Rix, 1);
    z_lngfr(Rix, Rix);      // ix range: [0..7], after inversion & mult: [-(7*12)..(0*12)].
    z_bc(Assembler::bcondAlways, 0, Rix, Z_R1);

    z_llc(Z_R1,  6, Z_R0, Rsrc);  // 6 bytes
    z_sth(Z_R1, 12, Z_R0, Rdst);  // 4 bytes

    z_llc(Z_R1,  5, Z_R0, Rsrc);
    z_sth(Z_R1, 10, Z_R0, Rdst);

    z_llc(Z_R1,  4, Z_R0, Rsrc);
    z_sth(Z_R1,  8, Z_R0, Rdst);

    z_llc(Z_R1,  3, Z_R0, Rsrc);
    z_sth(Z_R1,  6, Z_R0, Rdst);

    z_llc(Z_R1,  2, Z_R0, Rsrc);
    z_sth(Z_R1,  4, Z_R0, Rdst);

    z_llc(Z_R1,  1, Z_R0, Rsrc);
    z_sth(Z_R1,  2, Z_R0, Rdst);

    z_llc(Z_R1,  0, Z_R0, Rsrc);
    z_sth(Z_R1,  0, Z_R0, Rdst);
    bind(CodeTable);

    z_chi(Rcnt, 8);                        // no fixup for small strings. Rdst, Rsrc were not modified.
    z_brl(AllDone);

    z_sgfr(Z_R0, Rcnt);                    // # characters the ptrs have been advanced previously.
    z_agr(Rdst, Z_R0);                     // restore ptr, double the element count for Rdst restore.
    z_agr(Rdst, Z_R0);
    z_agr(Rsrc, Z_R0);                     // restore ptr.
  }
  bind(AllDone);

  BLOCK_COMMENT("} string_inflate");
  return offset() - block_start;
}

// Inflate byte[] to char[], length known at compile time.
//   Restores: src, dst
//   Kills:    tmp, Z_R0, Z_R1.
// Note:
//   len is signed int. Counts # characters, not bytes.
unsigned int C2_MacroAssembler::string_inflate_const(Register src, Register dst, Register tmp, int len) {
  assert_different_registers(Z_R0, Z_R1, src, dst, tmp);

  BLOCK_COMMENT("string_inflate_const {");
  int block_start = offset();

  Register   Rix  = tmp;   // loop index
  Register   Rsrc = src;   // addr(src array)
  Register   Rdst = dst;   // addr(dst array)
  Label      ScalarShortcut, AllDone;
  int        nprocessed = 0;
  int        src_off    = 0;  // compensate for saved (optimized away) ptr advancement.
  int        dst_off    = 0;  // compensate for saved (optimized away) ptr advancement.
  bool       restore_inputs = false;
  bool       workreg_clear  = false;

  if ((len >= 32) && VM_Version::has_VectorFacility()) {
    const int  min_vcnt     = 32;          // Minimum #characters required to use vector instructions.
                                           // Otherwise just do nothing in vector mode.
                                           // Must be multiple of vector register length (16 bytes = 128 bits).
    const int  log_min_vcnt = exact_log2(min_vcnt);
    const int  iterations   = (len - nprocessed) >> log_min_vcnt;
    nprocessed             += iterations << log_min_vcnt;
    Label      VectorLoop;

    if (iterations == 1) {
      z_vlm(Z_V20, Z_V21, 0+src_off, Rsrc);  // get next 32 characters (single-byte)
      z_vuplhb(Z_V22, Z_V20);                // V2 <- (expand) V0(high)
      z_vupllb(Z_V23, Z_V20);                // V3 <- (expand) V0(low)
      z_vuplhb(Z_V24, Z_V21);                // V4 <- (expand) V1(high)
      z_vupllb(Z_V25, Z_V21);                // V5 <- (expand) V1(low)
      z_vstm(Z_V22, Z_V25, 0+dst_off, Rdst); // store next 32 bytes

      src_off += min_vcnt;
      dst_off += min_vcnt*2;
    } else {
      restore_inputs = true;

      z_lgfi(Rix, len>>log_min_vcnt);
      bind(VectorLoop);
        z_vlm(Z_V20, Z_V21, 0, Rsrc);        // get next 32 characters (single-byte)
        add2reg(Rsrc, min_vcnt);

        z_vuplhb(Z_V22, Z_V20);              // V2 <- (expand) V0(high)
        z_vupllb(Z_V23, Z_V20);              // V3 <- (expand) V0(low)
        z_vuplhb(Z_V24, Z_V21);              // V4 <- (expand) V1(high)
        z_vupllb(Z_V25, Z_V21);              // V5 <- (expand) V1(low)
        z_vstm(Z_V22, Z_V25, 0, Rdst);       // store next 32 bytes
        add2reg(Rdst, min_vcnt*2);

        z_brct(Rix, VectorLoop);
    }
  }

  if (((len-nprocessed) >= 16) && VM_Version::has_VectorFacility()) {
    const int  min_vcnt     = 16;          // Minimum #characters required to use vector instructions.
                                           // Otherwise just do nothing in vector mode.
                                           // Must be multiple of vector register length (16 bytes = 128 bits).
    const int  log_min_vcnt = exact_log2(min_vcnt);
    const int  iterations   = (len - nprocessed) >> log_min_vcnt;
    nprocessed             += iterations << log_min_vcnt;
    assert(iterations == 1, "must be!");

    z_vl(Z_V20, 0+src_off, Z_R0, Rsrc);    // get next 16 characters (single-byte)
    z_vuplhb(Z_V22, Z_V20);                // V2 <- (expand) V0(high)
    z_vupllb(Z_V23, Z_V20);                // V3 <- (expand) V0(low)
    z_vstm(Z_V22, Z_V23, 0+dst_off, Rdst); // store next 32 bytes

    src_off += min_vcnt;
    dst_off += min_vcnt*2;
  }

  if ((len-nprocessed) > 8) {
    const int  min_cnt     =  8;           // Minimum #characters required to use unrolled scalar loop.
                                           // Otherwise just do nothing in unrolled scalar mode.
                                           // Must be multiple of 8.
    const int  log_min_cnt = exact_log2(min_cnt);
    const int  iterations  = (len - nprocessed) >> log_min_cnt;
    nprocessed     += iterations << log_min_cnt;

    //---<  avoid loop overhead/ptr increment for small # iterations  >---
    if (iterations <= 2) {
      clear_reg(Z_R0);
      clear_reg(Z_R1);
      workreg_clear = true;

      z_icmh(Z_R0, 5, 0+src_off, Rsrc);
      z_icmh(Z_R1, 5, 4+src_off, Rsrc);
      z_icm(Z_R0,  5, 2+src_off, Rsrc);
      z_icm(Z_R1,  5, 6+src_off, Rsrc);
      z_stmg(Z_R0, Z_R1, 0+dst_off, Rdst);

      src_off += min_cnt;
      dst_off += min_cnt*2;
    }

    if (iterations == 2) {
      z_icmh(Z_R0, 5, 0+src_off, Rsrc);
      z_icmh(Z_R1, 5, 4+src_off, Rsrc);
      z_icm(Z_R0,  5, 2+src_off, Rsrc);
      z_icm(Z_R1,  5, 6+src_off, Rsrc);
      z_stmg(Z_R0, Z_R1, 0+dst_off, Rdst);

      src_off += min_cnt;
      dst_off += min_cnt*2;
    }

    if (iterations > 2) {
      Label      UnrolledLoop;
      restore_inputs  = true;

      clear_reg(Z_R0);
      clear_reg(Z_R1);
      workreg_clear = true;

      z_lgfi(Rix, iterations);
      bind(UnrolledLoop);
        z_icmh(Z_R0, 5, 0, Rsrc);
        z_icmh(Z_R1, 5, 4, Rsrc);
        z_icm(Z_R0,  5, 2, Rsrc);
        z_icm(Z_R1,  5, 6, Rsrc);
        add2reg(Rsrc, min_cnt);

        z_stmg(Z_R0, Z_R1, 0, Rdst);
        add2reg(Rdst, min_cnt*2);

        z_brct(Rix, UnrolledLoop);
    }
  }

  if ((len-nprocessed) > 0) {
    switch (len-nprocessed) {
      case 8:
        if (!workreg_clear) {
          clear_reg(Z_R0);
          clear_reg(Z_R1);
        }
        z_icmh(Z_R0, 5, 0+src_off, Rsrc);
        z_icmh(Z_R1, 5, 4+src_off, Rsrc);
        z_icm(Z_R0,  5, 2+src_off, Rsrc);
        z_icm(Z_R1,  5, 6+src_off, Rsrc);
        z_stmg(Z_R0, Z_R1, 0+dst_off, Rdst);
        break;
      case 7:
        if (!workreg_clear) {
          clear_reg(Z_R0);
          clear_reg(Z_R1);
        }
        clear_reg(Rix);
        z_icm(Z_R0,  5, 0+src_off, Rsrc);
        z_icm(Z_R1,  5, 2+src_off, Rsrc);
        z_icm(Rix,   5, 4+src_off, Rsrc);
        z_stm(Z_R0,  Z_R1, 0+dst_off, Rdst);
        z_llc(Z_R0,  6+src_off, Z_R0, Rsrc);
        z_st(Rix,    8+dst_off, Z_R0, Rdst);
        z_sth(Z_R0, 12+dst_off, Z_R0, Rdst);
        break;
      case 6:
        if (!workreg_clear) {
          clear_reg(Z_R0);
          clear_reg(Z_R1);
        }
        clear_reg(Rix);
        z_icm(Z_R0, 5, 0+src_off, Rsrc);
        z_icm(Z_R1, 5, 2+src_off, Rsrc);
        z_icm(Rix,  5, 4+src_off, Rsrc);
        z_stm(Z_R0, Z_R1, 0+dst_off, Rdst);
        z_st(Rix,   8+dst_off, Z_R0, Rdst);
        break;
      case 5:
        if (!workreg_clear) {
          clear_reg(Z_R0);
          clear_reg(Z_R1);
        }
        z_icm(Z_R0, 5, 0+src_off, Rsrc);
        z_icm(Z_R1, 5, 2+src_off, Rsrc);
        z_llc(Rix,  4+src_off, Z_R0, Rsrc);
        z_stm(Z_R0, Z_R1, 0+dst_off, Rdst);
        z_sth(Rix,  8+dst_off, Z_R0, Rdst);
        break;
      case 4:
        if (!workreg_clear) {
          clear_reg(Z_R0);
          clear_reg(Z_R1);
        }
        z_icm(Z_R0, 5, 0+src_off, Rsrc);
        z_icm(Z_R1, 5, 2+src_off, Rsrc);
        z_stm(Z_R0, Z_R1, 0+dst_off, Rdst);
        break;
      case 3:
        if (!workreg_clear) {
          clear_reg(Z_R0);
        }
        z_llc(Z_R1, 2+src_off, Z_R0, Rsrc);
        z_icm(Z_R0, 5, 0+src_off, Rsrc);
        z_sth(Z_R1, 4+dst_off, Z_R0, Rdst);
        z_st(Z_R0,  0+dst_off, Rdst);
        break;
      case 2:
        z_llc(Z_R0, 0+src_off, Z_R0, Rsrc);
        z_llc(Z_R1, 1+src_off, Z_R0, Rsrc);
        z_sth(Z_R0, 0+dst_off, Z_R0, Rdst);
        z_sth(Z_R1, 2+dst_off, Z_R0, Rdst);
        break;
      case 1:
        z_llc(Z_R0, 0+src_off, Z_R0, Rsrc);
        z_sth(Z_R0, 0+dst_off, Z_R0, Rdst);
        break;
      default:
        guarantee(false, "Impossible");
        break;
    }
    src_off   +=  len-nprocessed;
    dst_off   += (len-nprocessed)*2;
    nprocessed = len;
  }

  //---< restore modified input registers  >---
  if ((nprocessed > 0) && restore_inputs) {
    z_agfi(Rsrc, -(nprocessed-src_off));
    if (nprocessed < 1000000000) { // avoid int overflow
      z_agfi(Rdst, -(nprocessed*2-dst_off));
    } else {
      z_agfi(Rdst, -(nprocessed-dst_off));
      z_agfi(Rdst, -nprocessed);
    }
  }

  BLOCK_COMMENT("} string_inflate_const");
  return offset() - block_start;
}

// Kills src.
unsigned int C2_MacroAssembler::has_negatives(Register result, Register src, Register cnt,
                                              Register odd_reg, Register even_reg, Register tmp) {
  int block_start = offset();
  Label Lloop1, Lloop2, Lslow, Lnotfound, Ldone;
  const Register addr = src, mask = tmp;

  BLOCK_COMMENT("has_negatives {");

  z_llgfr(Z_R1, cnt);      // Number of bytes to read. (Must be a positive simm32.)
  z_llilf(mask, 0x80808080);
  z_lhi(result, 1);        // Assume true.
  // Last possible addr for fast loop.
  z_lay(odd_reg, -16, Z_R1, src);
  z_chi(cnt, 16);
  z_brl(Lslow);

  // ind1: index, even_reg: index increment, odd_reg: index limit
  z_iihf(mask, 0x80808080);
  z_lghi(even_reg, 16);

  bind(Lloop1); // 16 bytes per iteration.
  z_lg(Z_R0, Address(addr));
  z_lg(Z_R1, Address(addr, 8));
  z_ogr(Z_R0, Z_R1);
  z_ngr(Z_R0, mask);
  z_brne(Ldone);           // If found return 1.
  z_brxlg(addr, even_reg, Lloop1);

  bind(Lslow);
  z_aghi(odd_reg, 16-1);   // Last possible addr for slow loop.
  z_lghi(even_reg, 1);
  z_cgr(addr, odd_reg);
  z_brh(Lnotfound);

  bind(Lloop2); // 1 byte per iteration.
  z_cli(Address(addr), 0x80);
  z_brnl(Ldone);           // If found return 1.
  z_brxlg(addr, even_reg, Lloop2);

  bind(Lnotfound);
  z_lhi(result, 0);

  bind(Ldone);

  BLOCK_COMMENT("} has_negatives");

  return offset() - block_start;
}

// kill: cnt1, cnt2, odd_reg, even_reg; early clobber: result
unsigned int C2_MacroAssembler::string_compare(Register str1, Register str2,
                                               Register cnt1, Register cnt2,
                                               Register odd_reg, Register even_reg, Register result, int ae) {
  int block_start = offset();

  assert_different_registers(str1, cnt1, cnt2, odd_reg, even_reg, result);
  assert_different_registers(str2, cnt1, cnt2, odd_reg, even_reg, result);

  // If strings are equal up to min length, return the length difference.
  const Register diff = result, // Pre-set result with length difference.
                 min  = cnt1,   // min number of bytes
                 tmp  = cnt2;

  // Note: Making use of the fact that compareTo(a, b) == -compareTo(b, a)
  // we interchange str1 and str2 in the UL case and negate the result.
  // Like this, str1 is always latin1 encoded, except for the UU case.
  // In addition, we need 0 (or sign which is 0) extend when using 64 bit register.
  const bool used_as_LU = (ae == StrIntrinsicNode::LU || ae == StrIntrinsicNode::UL);

  BLOCK_COMMENT("string_compare {");

  if (used_as_LU) {
    z_srl(cnt2, 1);
  }

  // See if the lengths are different, and calculate min in cnt1.
  // Save diff in case we need it for a tie-breaker.

  // diff = cnt1 - cnt2
  if (VM_Version::has_DistinctOpnds()) {
    z_srk(diff, cnt1, cnt2);
  } else {
    z_lr(diff, cnt1);
    z_sr(diff, cnt2);
  }
  if (str1 != str2) {
    if (VM_Version::has_LoadStoreConditional()) {
      z_locr(min, cnt2, Assembler::bcondHigh);
    } else {
      Label Lskip;
      z_brl(Lskip);    // min ok if cnt1 < cnt2
      z_lr(min, cnt2); // min = cnt2
      bind(Lskip);
    }
  }

  if (ae == StrIntrinsicNode::UU) {
    z_sra(diff, 1);
  }
  if (str1 != str2) {
    Label Ldone;
    if (used_as_LU) {
      // Loop which searches the first difference character by character.
      Label Lloop;
      const Register ind1 = Z_R1,
                     ind2 = min;
      int stride1 = 1, stride2 = 2; // See comment above.

      // ind1: index, even_reg: index increment, odd_reg: index limit
      z_llilf(ind1, (unsigned int)(-stride1));
      z_lhi(even_reg, stride1);
      add2reg(odd_reg, -stride1, min);
      clear_reg(ind2); // kills min

      bind(Lloop);
      z_brxh(ind1, even_reg, Ldone);
      z_llc(tmp, Address(str1, ind1));
      z_llh(Z_R0, Address(str2, ind2));
      z_ahi(ind2, stride2);
      z_sr(tmp, Z_R0);
      z_bre(Lloop);

      z_lr(result, tmp);

    } else {
      // Use clcle in fast loop (only for same encoding).
      z_lgr(Z_R0, str1);
      z_lgr(even_reg, str2);
      z_llgfr(Z_R1, min);
      z_llgfr(odd_reg, min);

      if (ae == StrIntrinsicNode::LL) {
        compare_long_ext(Z_R0, even_reg, 0);
      } else {
        compare_long_uni(Z_R0, even_reg, 0);
      }
      z_bre(Ldone);
      z_lgr(Z_R1, Z_R0);
      if (ae == StrIntrinsicNode::LL) {
        z_llc(Z_R0, Address(even_reg));
        z_llc(result, Address(Z_R1));
      } else {
        z_llh(Z_R0, Address(even_reg));
        z_llh(result, Address(Z_R1));
      }
      z_sr(result, Z_R0);
    }

    // Otherwise, return the difference between the first mismatched chars.
    bind(Ldone);
  }

  if (ae == StrIntrinsicNode::UL) {
    z_lcr(result, result); // Negate result (see note above).
  }

  BLOCK_COMMENT("} string_compare");

  return offset() - block_start;
}

unsigned int C2_MacroAssembler::array_equals(bool is_array_equ, Register ary1, Register ary2, Register limit,
                                             Register odd_reg, Register even_reg, Register result, bool is_byte) {
  int block_start = offset();

  BLOCK_COMMENT("array_equals {");

  assert_different_registers(ary1, limit, odd_reg, even_reg);
  assert_different_registers(ary2, limit, odd_reg, even_reg);

  Label Ldone, Ldone_true, Ldone_false, Lclcle, CLC_template;
  int base_offset = 0;

  if (ary1 != ary2) {
    if (is_array_equ) {
      base_offset = arrayOopDesc::base_offset_in_bytes(is_byte ? T_BYTE : T_CHAR);

      // Return true if the same array.
      compareU64_and_branch(ary1, ary2, Assembler::bcondEqual, Ldone_true);

      // Return false if one of them is NULL.
      compareU64_and_branch(ary1, (intptr_t)0, Assembler::bcondEqual, Ldone_false);
      compareU64_and_branch(ary2, (intptr_t)0, Assembler::bcondEqual, Ldone_false);

      // Load the lengths of arrays.
      z_llgf(odd_reg, Address(ary1, arrayOopDesc::length_offset_in_bytes()));

      // Return false if the two arrays are not equal length.
      z_c(odd_reg, Address(ary2, arrayOopDesc::length_offset_in_bytes()));
      z_brne(Ldone_false);

      // string len in bytes (right operand)
      if (!is_byte) {
        z_chi(odd_reg, 128);
        z_sll(odd_reg, 1); // preserves flags
        z_brh(Lclcle);
      } else {
        compareU32_and_branch(odd_reg, (intptr_t)256, Assembler::bcondHigh, Lclcle);
      }
    } else {
      z_llgfr(odd_reg, limit); // Need to zero-extend prior to using the value.
      compareU32_and_branch(limit, (intptr_t)256, Assembler::bcondHigh, Lclcle);
    }


    // Use clc instruction for up to 256 bytes.
    {
      Register str1_reg = ary1,
          str2_reg = ary2;
      if (is_array_equ) {
        str1_reg = Z_R1;
        str2_reg = even_reg;
        add2reg(str1_reg, base_offset, ary1); // string addr (left operand)
        add2reg(str2_reg, base_offset, ary2); // string addr (right operand)
      }
      z_ahi(odd_reg, -1); // Clc uses decremented limit. Also compare result to 0.
      z_brl(Ldone_true);
      // Note: We could jump to the template if equal.

      assert(VM_Version::has_ExecuteExtensions(), "unsupported hardware");
      z_exrl(odd_reg, CLC_template);
      z_bre(Ldone_true);
      // fall through

      bind(Ldone_false);
      clear_reg(result);
      z_bru(Ldone);

      bind(CLC_template);
      z_clc(0, 0, str1_reg, 0, str2_reg);
    }

    // Use clcle instruction.
    {
      bind(Lclcle);
      add2reg(even_reg, base_offset, ary2); // string addr (right operand)
      add2reg(Z_R0, base_offset, ary1);     // string addr (left operand)

      z_lgr(Z_R1, odd_reg); // string len in bytes (left operand)
      if (is_byte) {
        compare_long_ext(Z_R0, even_reg, 0);
      } else {
        compare_long_uni(Z_R0, even_reg, 0);
      }
      z_lghi(result, 0); // Preserve flags.
      z_brne(Ldone);
    }
  }
  // fall through

  bind(Ldone_true);
  z_lghi(result, 1); // All characters are equal.
  bind(Ldone);

  BLOCK_COMMENT("} array_equals");

  return offset() - block_start;
}

// kill: haycnt, needlecnt, odd_reg, even_reg; early clobber: result
unsigned int C2_MacroAssembler::string_indexof(Register result, Register haystack, Register haycnt,
                                               Register needle, Register needlecnt, int needlecntval,
                                               Register odd_reg, Register even_reg, int ae) {
  int block_start = offset();

  // Ensure 0<needlecnt<=haycnt in ideal graph as prerequisite!
  assert(ae != StrIntrinsicNode::LU, "Invalid encoding");
  const int h_csize = (ae == StrIntrinsicNode::LL) ? 1 : 2;
  const int n_csize = (ae == StrIntrinsicNode::UU) ? 2 : 1;
  Label L_needle1, L_Found, L_NotFound;

  BLOCK_COMMENT("string_indexof {");

  if (needle == haystack) {
    z_lhi(result, 0);
  } else {

  // Load first character of needle (R0 used by search_string instructions).
  if (n_csize == 2) { z_llgh(Z_R0, Address(needle)); } else { z_llgc(Z_R0, Address(needle)); }

  // Compute last haystack addr to use if no match gets found.
  if (needlecnt != noreg) { // variable needlecnt
    z_ahi(needlecnt, -1); // Remaining characters after first one.
    z_sr(haycnt, needlecnt); // Compute index succeeding last element to compare.
    if (n_csize == 2) { z_sll(needlecnt, 1); } // In bytes.
  } else { // constant needlecnt
    assert((needlecntval & 0x7fff) == needlecntval, "must be positive simm16 immediate");
    // Compute index succeeding last element to compare.
    if (needlecntval != 1) { z_ahi(haycnt, 1 - needlecntval); }
  }

  z_llgfr(haycnt, haycnt); // Clear high half.
  z_lgr(result, haystack); // Final result will be computed from needle start pointer.
  if (h_csize == 2) { z_sll(haycnt, 1); } // Scale to number of bytes.
  z_agr(haycnt, haystack); // Point to address succeeding last element (haystack+scale*(haycnt-needlecnt+1)).

  if (h_csize != n_csize) {
    assert(ae == StrIntrinsicNode::UL, "Invalid encoding");

    if (needlecnt != noreg || needlecntval != 1) {
      if (needlecnt != noreg) {
        compare32_and_branch(needlecnt, (intptr_t)0, Assembler::bcondEqual, L_needle1);
      }

      // Main Loop: UL version (now we have at least 2 characters).
      Label L_OuterLoop, L_InnerLoop, L_Skip;
      bind(L_OuterLoop); // Search for 1st 2 characters.
      z_lgr(Z_R1, haycnt);
      MacroAssembler::search_string_uni(Z_R1, result);
      z_brc(Assembler::bcondNotFound, L_NotFound);
      z_lgr(result, Z_R1);

      z_lghi(Z_R1, n_csize);
      z_lghi(even_reg, h_csize);
      bind(L_InnerLoop);
      z_llgc(odd_reg, Address(needle, Z_R1));
      z_ch(odd_reg, Address(result, even_reg));
      z_brne(L_Skip);
      if (needlecnt != noreg) { z_cr(Z_R1, needlecnt); } else { z_chi(Z_R1, needlecntval - 1); }
      z_brnl(L_Found);
      z_aghi(Z_R1, n_csize);
      z_aghi(even_reg, h_csize);
      z_bru(L_InnerLoop);

      bind(L_Skip);
      z_aghi(result, h_csize); // This is the new address we want to use for comparing.
      z_bru(L_OuterLoop);
    }

  } else {
    const intptr_t needle_bytes = (n_csize == 2) ? ((needlecntval - 1) << 1) : (needlecntval - 1);
    Label L_clcle;

    if (needlecnt != noreg || (needlecntval != 1 && needle_bytes <= 256)) {
      if (needlecnt != noreg) {
        compare32_and_branch(needlecnt, 256, Assembler::bcondHigh, L_clcle);
        z_ahi(needlecnt, -1); // remaining bytes -1 (for CLC)
        z_brl(L_needle1);
      }

      // Main Loop: clc version (now we have at least 2 characters).
      Label L_OuterLoop, CLC_template;
      bind(L_OuterLoop); // Search for 1st 2 characters.
      z_lgr(Z_R1, haycnt);
      if (h_csize == 1) {
        MacroAssembler::search_string(Z_R1, result);
      } else {
        MacroAssembler::search_string_uni(Z_R1, result);
      }
      z_brc(Assembler::bcondNotFound, L_NotFound);
      z_lgr(result, Z_R1);

      if (needlecnt != noreg) {
        assert(VM_Version::has_ExecuteExtensions(), "unsupported hardware");
        z_exrl(needlecnt, CLC_template);
      } else {
        z_clc(h_csize, needle_bytes -1, Z_R1, n_csize, needle);
      }
      z_bre(L_Found);
      z_aghi(result, h_csize); // This is the new address we want to use for comparing.
      z_bru(L_OuterLoop);

      if (needlecnt != noreg) {
        bind(CLC_template);
        z_clc(h_csize, 0, Z_R1, n_csize, needle);
      }
    }

    if (needlecnt != noreg || needle_bytes > 256) {
      bind(L_clcle);

      // Main Loop: clcle version (now we have at least 256 bytes).
      Label L_OuterLoop, CLC_template;
      bind(L_OuterLoop); // Search for 1st 2 characters.
      z_lgr(Z_R1, haycnt);
      if (h_csize == 1) {
        MacroAssembler::search_string(Z_R1, result);
      } else {
        MacroAssembler::search_string_uni(Z_R1, result);
      }
      z_brc(Assembler::bcondNotFound, L_NotFound);

      add2reg(Z_R0, n_csize, needle);
      add2reg(even_reg, h_csize, Z_R1);
      z_lgr(result, Z_R1);
      if (needlecnt != noreg) {
        z_llgfr(Z_R1, needlecnt); // needle len in bytes (left operand)
        z_llgfr(odd_reg, needlecnt);
      } else {
        load_const_optimized(Z_R1, needle_bytes);
        if (Immediate::is_simm16(needle_bytes)) { z_lghi(odd_reg, needle_bytes); } else { z_lgr(odd_reg, Z_R1); }
      }
      if (h_csize == 1) {
        compare_long_ext(Z_R0, even_reg, 0);
      } else {
        compare_long_uni(Z_R0, even_reg, 0);
      }
      z_bre(L_Found);

      if (n_csize == 2) { z_llgh(Z_R0, Address(needle)); } else { z_llgc(Z_R0, Address(needle)); } // Reload.
      z_aghi(result, h_csize); // This is the new address we want to use for comparing.
      z_bru(L_OuterLoop);
    }
  }

  if (needlecnt != noreg || needlecntval == 1) {
    bind(L_needle1);

    // Single needle character version.
    if (h_csize == 1) {
      MacroAssembler::search_string(haycnt, result);
    } else {
      MacroAssembler::search_string_uni(haycnt, result);
    }
    z_lgr(result, haycnt);
    z_brc(Assembler::bcondFound, L_Found);
  }

  bind(L_NotFound);
  add2reg(result, -1, haystack); // Return -1.

  bind(L_Found); // Return index (or -1 in fallthrough case).
  z_sgr(result, haystack);
  if (h_csize == 2) { z_srag(result, result, exact_log2(sizeof(jchar))); }
  }
  BLOCK_COMMENT("} string_indexof");

  return offset() - block_start;
}

// early clobber: result
unsigned int C2_MacroAssembler::string_indexof_char(Register result, Register haystack, Register haycnt,
                                                    Register needle, jchar needleChar, Register odd_reg, Register even_reg, bool is_byte) {
  int block_start = offset();

  BLOCK_COMMENT("string_indexof_char {");

  if (needle == haystack) {
    z_lhi(result, 0);
  } else {

  Label Ldone;

  z_llgfr(odd_reg, haycnt);  // Preset loop ctr/searchrange end.
  if (needle == noreg) {
    load_const_optimized(Z_R0, (unsigned long)needleChar);
  } else {
    if (is_byte) {
      z_llgcr(Z_R0, needle); // First (and only) needle char.
    } else {
      z_llghr(Z_R0, needle); // First (and only) needle char.
    }
  }

  if (!is_byte) {
    z_agr(odd_reg, odd_reg); // Calc #bytes to be processed with SRSTU.
  }

  z_lgr(even_reg, haystack); // haystack addr
  z_agr(odd_reg, haystack);  // First char after range end.
  z_lghi(result, -1);

  if (is_byte) {
    MacroAssembler::search_string(odd_reg, even_reg);
  } else {
    MacroAssembler::search_string_uni(odd_reg, even_reg);
  }
  z_brc(Assembler::bcondNotFound, Ldone);
  if (is_byte) {
    if (VM_Version::has_DistinctOpnds()) {
      z_sgrk(result, odd_reg, haystack);
    } else {
      z_sgr(odd_reg, haystack);
      z_lgr(result, odd_reg);
    }
  } else {
    z_slgr(odd_reg, haystack);
    z_srlg(result, odd_reg, exact_log2(sizeof(jchar)));
  }

  bind(Ldone);
  }
  BLOCK_COMMENT("} string_indexof_char");

  return offset() - block_start;
}

