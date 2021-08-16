/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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
#include "asm/assembler.inline.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

#ifdef PRODUCT
#define BLOCK_COMMENT(str) // nothing
#else
#define BLOCK_COMMENT(str) block_comment(str)
#endif

int AbstractAssembler::code_fill_byte() {
  return 0x00;                  // illegal instruction 0x00000000
}


// Patch instruction `inst' at offset `inst_pos' to refer to
// `dest_pos' and return the resulting instruction.  We should have
// pcs, not offsets, but since all is relative, it will work out fine.
int Assembler::patched_branch(int dest_pos, int inst, int inst_pos) {
  int m = 0; // mask for displacement field
  int v = 0; // new value for displacement field

  switch (inv_op_ppc(inst)) {
  case b_op:  m = li(-1); v = li(disp(dest_pos, inst_pos)); break;
  case bc_op: m = bd(-1); v = bd(disp(dest_pos, inst_pos)); break;
    default: ShouldNotReachHere();
  }
  return inst & ~m | v;
}

// Return the offset, relative to _code_begin, of the destination of
// the branch inst at offset pos.
int Assembler::branch_destination(int inst, int pos) {
  int r = 0;
  switch (inv_op_ppc(inst)) {
    case b_op:  r = bxx_destination_offset(inst, pos); break;
    case bc_op: r = inv_bd_field(inst, pos); break;
    default: ShouldNotReachHere();
  }
  return r;
}

// Low-level andi-one-instruction-macro.
void Assembler::andi(Register a, Register s, const long ui16) {
  if (is_power_of_2(((jlong) ui16)+1)) {
    // pow2minus1
    clrldi(a, s, 64 - log2i_exact((((jlong) ui16)+1)));
  } else if (is_power_of_2((jlong) ui16)) {
    // pow2
    rlwinm(a, s, 0, 31 - log2i_exact((jlong) ui16), 31 - log2i_exact((jlong) ui16));
  } else if (is_power_of_2((jlong)-ui16)) {
    // negpow2
    clrrdi(a, s, log2i_exact((jlong)-ui16));
  } else {
    assert(is_uimm(ui16, 16), "must be 16-bit unsigned immediate");
    andi_(a, s, ui16);
  }
}

// RegisterOrConstant version.
void Assembler::ld(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      int simm16_rest = load_const_optimized(d, roc.as_constant(), noreg, true);
      Assembler::ld(d, simm16_rest, d);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::ld(d, roc.as_constant(), s1);
    } else {
      load_const_optimized(d, roc.as_constant());
      Assembler::ldx(d, d, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::ld(d, 0, roc.as_register());
    else
      Assembler::ldx(d, roc.as_register(), s1);
  }
}

void Assembler::lwa(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      int simm16_rest = load_const_optimized(d, roc.as_constant(), noreg, true);
      Assembler::lwa(d, simm16_rest, d);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::lwa(d, roc.as_constant(), s1);
    } else {
      load_const_optimized(d, roc.as_constant());
      Assembler::lwax(d, d, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::lwa(d, 0, roc.as_register());
    else
      Assembler::lwax(d, roc.as_register(), s1);
  }
}

void Assembler::lwz(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      int simm16_rest = load_const_optimized(d, roc.as_constant(), noreg, true);
      Assembler::lwz(d, simm16_rest, d);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::lwz(d, roc.as_constant(), s1);
    } else {
      load_const_optimized(d, roc.as_constant());
      Assembler::lwzx(d, d, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::lwz(d, 0, roc.as_register());
    else
      Assembler::lwzx(d, roc.as_register(), s1);
  }
}

void Assembler::lha(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      int simm16_rest = load_const_optimized(d, roc.as_constant(), noreg, true);
      Assembler::lha(d, simm16_rest, d);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::lha(d, roc.as_constant(), s1);
    } else {
      load_const_optimized(d, roc.as_constant());
      Assembler::lhax(d, d, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::lha(d, 0, roc.as_register());
    else
      Assembler::lhax(d, roc.as_register(), s1);
  }
}

void Assembler::lhz(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      int simm16_rest = load_const_optimized(d, roc.as_constant(), noreg, true);
      Assembler::lhz(d, simm16_rest, d);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::lhz(d, roc.as_constant(), s1);
    } else {
      load_const_optimized(d, roc.as_constant());
      Assembler::lhzx(d, d, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::lhz(d, 0, roc.as_register());
    else
      Assembler::lhzx(d, roc.as_register(), s1);
  }
}

void Assembler::lbz(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      int simm16_rest = load_const_optimized(d, roc.as_constant(), noreg, true);
      Assembler::lbz(d, simm16_rest, d);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::lbz(d, roc.as_constant(), s1);
    } else {
      load_const_optimized(d, roc.as_constant());
      Assembler::lbzx(d, d, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::lbz(d, 0, roc.as_register());
    else
      Assembler::lbzx(d, roc.as_register(), s1);
  }
}

void Assembler::std(Register d, RegisterOrConstant roc, Register s1, Register tmp) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      int simm16_rest = load_const_optimized(tmp, roc.as_constant(), noreg, true);
      Assembler::std(d, simm16_rest, tmp);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::std(d, roc.as_constant(), s1);
    } else {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      load_const_optimized(tmp, roc.as_constant());
      Assembler::stdx(d, tmp, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::std(d, 0, roc.as_register());
    else
      Assembler::stdx(d, roc.as_register(), s1);
  }
}

void Assembler::stw(Register d, RegisterOrConstant roc, Register s1, Register tmp) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      int simm16_rest = load_const_optimized(tmp, roc.as_constant(), noreg, true);
      Assembler::stw(d, simm16_rest, tmp);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::stw(d, roc.as_constant(), s1);
    } else {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      load_const_optimized(tmp, roc.as_constant());
      Assembler::stwx(d, tmp, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::stw(d, 0, roc.as_register());
    else
      Assembler::stwx(d, roc.as_register(), s1);
  }
}

void Assembler::sth(Register d, RegisterOrConstant roc, Register s1, Register tmp) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      int simm16_rest = load_const_optimized(tmp, roc.as_constant(), noreg, true);
      Assembler::sth(d, simm16_rest, tmp);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::sth(d, roc.as_constant(), s1);
    } else {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      load_const_optimized(tmp, roc.as_constant());
      Assembler::sthx(d, tmp, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::sth(d, 0, roc.as_register());
    else
      Assembler::sthx(d, roc.as_register(), s1);
  }
}

void Assembler::stb(Register d, RegisterOrConstant roc, Register s1, Register tmp) {
  if (roc.is_constant()) {
    if (s1 == noreg) {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      int simm16_rest = load_const_optimized(tmp, roc.as_constant(), noreg, true);
      Assembler::stb(d, simm16_rest, tmp);
    } else if (is_simm(roc.as_constant(), 16)) {
      Assembler::stb(d, roc.as_constant(), s1);
    } else {
      guarantee(tmp != noreg, "Need tmp reg to encode large constants");
      load_const_optimized(tmp, roc.as_constant());
      Assembler::stbx(d, tmp, s1);
    }
  } else {
    if (s1 == noreg)
      Assembler::stb(d, 0, roc.as_register());
    else
      Assembler::stbx(d, roc.as_register(), s1);
  }
}

void Assembler::add(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    intptr_t c = roc.as_constant();
    assert(is_simm(c, 16), "too big");
    addi(d, s1, (int)c);
  }
  else add(d, roc.as_register(), s1);
}

void Assembler::subf(Register d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    intptr_t c = roc.as_constant();
    assert(is_simm(-c, 16), "too big");
    addi(d, s1, (int)-c);
  }
  else subf(d, roc.as_register(), s1);
}

void Assembler::cmpd(ConditionRegister d, RegisterOrConstant roc, Register s1) {
  if (roc.is_constant()) {
    intptr_t c = roc.as_constant();
    assert(is_simm(c, 16), "too big");
    cmpdi(d, s1, (int)c);
  }
  else cmpd(d, roc.as_register(), s1);
}

// Load a 64 bit constant. Patchable.
void Assembler::load_const(Register d, long x, Register tmp) {
  // 64-bit value: x = xa xb xc xd
  int xa = (x >> 48) & 0xffff;
  int xb = (x >> 32) & 0xffff;
  int xc = (x >> 16) & 0xffff;
  int xd = (x >>  0) & 0xffff;
  if (tmp == noreg) {
    Assembler::lis( d, (int)(short)xa);
    Assembler::ori( d, d, (unsigned int)xb);
    Assembler::sldi(d, d, 32);
    Assembler::oris(d, d, (unsigned int)xc);
    Assembler::ori( d, d, (unsigned int)xd);
  } else {
    // exploit instruction level parallelism if we have a tmp register
    assert_different_registers(d, tmp);
    Assembler::lis(tmp, (int)(short)xa);
    Assembler::lis(d, (int)(short)xc);
    Assembler::ori(tmp, tmp, (unsigned int)xb);
    Assembler::ori(d, d, (unsigned int)xd);
    Assembler::insrdi(d, tmp, 32, 0);
  }
}

// Load a 64 bit constant, optimized, not identifyable.
// Tmp can be used to increase ILP. Set return_simm16_rest=true to get a
// 16 bit immediate offset.
int Assembler::load_const_optimized(Register d, long x, Register tmp, bool return_simm16_rest) {
  // Avoid accidentally trying to use R0 for indexed addressing.
  assert_different_registers(d, tmp);

  short xa, xb, xc, xd; // Four 16-bit chunks of const.
  long rem = x;         // Remaining part of const.

  xd = rem & 0xFFFF;    // Lowest 16-bit chunk.
  rem = (rem >> 16) + ((unsigned short)xd >> 15); // Compensation for sign extend.

  if (rem == 0) { // opt 1: simm16
    li(d, xd);
    return 0;
  }

  int retval = 0;
  if (return_simm16_rest) {
    retval = xd;
    x = rem << 16;
    xd = 0;
  }

  if (d == R0) { // Can't use addi.
    if (is_simm(x, 32)) { // opt 2: simm32
      lis(d, x >> 16);
      if (xd) ori(d, d, (unsigned short)xd);
    } else {
      // 64-bit value: x = xa xb xc xd
      xa = (x >> 48) & 0xffff;
      xb = (x >> 32) & 0xffff;
      xc = (x >> 16) & 0xffff;
      bool xa_loaded = (xb & 0x8000) ? (xa != -1) : (xa != 0);
      if (tmp == noreg || (xc == 0 && xd == 0)) {
        if (xa_loaded) {
          lis(d, xa);
          if (xb) { ori(d, d, (unsigned short)xb); }
        } else {
          li(d, xb);
        }
        sldi(d, d, 32);
        if (xc) { oris(d, d, (unsigned short)xc); }
        if (xd) { ori( d, d, (unsigned short)xd); }
      } else {
        // Exploit instruction level parallelism if we have a tmp register.
        bool xc_loaded = (xd & 0x8000) ? (xc != -1) : (xc != 0);
        if (xa_loaded) {
          lis(tmp, xa);
        }
        if (xc_loaded) {
          lis(d, xc);
        }
        if (xa_loaded) {
          if (xb) { ori(tmp, tmp, (unsigned short)xb); }
        } else {
          li(tmp, xb);
        }
        if (xc_loaded) {
          if (xd) { ori(d, d, (unsigned short)xd); }
        } else {
          li(d, xd);
        }
        insrdi(d, tmp, 32, 0);
      }
    }
    return retval;
  }

  xc = rem & 0xFFFF; // Next 16-bit chunk.
  rem = (rem >> 16) + ((unsigned short)xc >> 15); // Compensation for sign extend.

  if (rem == 0) { // opt 2: simm32
    lis(d, xc);
  } else { // High 32 bits needed.

    if (tmp != noreg  && (int)x != 0) { // opt 3: We have a temp reg.
      // No carry propagation between xc and higher chunks here (use logical instructions).
      xa = (x >> 48) & 0xffff;
      xb = (x >> 32) & 0xffff; // No sign compensation, we use lis+ori or li to allow usage of R0.
      bool xa_loaded = (xb & 0x8000) ? (xa != -1) : (xa != 0);
      bool return_xd = false;

      if (xa_loaded) { lis(tmp, xa); }
      if (xc) { lis(d, xc); }
      if (xa_loaded) {
        if (xb) { ori(tmp, tmp, (unsigned short)xb); } // No addi, we support tmp == R0.
      } else {
        li(tmp, xb);
      }
      if (xc) {
        if (xd) { addi(d, d, xd); }
      } else {
        li(d, xd);
      }
      insrdi(d, tmp, 32, 0);
      return retval;
    }

    xb = rem & 0xFFFF; // Next 16-bit chunk.
    rem = (rem >> 16) + ((unsigned short)xb >> 15); // Compensation for sign extend.

    xa = rem & 0xFFFF; // Highest 16-bit chunk.

    // opt 4: avoid adding 0
    if (xa) { // Highest 16-bit needed?
      lis(d, xa);
      if (xb) { addi(d, d, xb); }
    } else {
      li(d, xb);
    }
    sldi(d, d, 32);
    if (xc) { addis(d, d, xc); }
  }

  if (xd) { addi(d, d, xd); }
  return retval;
}

// We emit only one addition to s to optimize latency.
int Assembler::add_const_optimized(Register d, Register s, long x, Register tmp, bool return_simm16_rest) {
  assert(s != R0 && s != tmp, "unsupported");
  long rem = x;

  // Case 1: Can use mr or addi.
  short xd = rem & 0xFFFF; // Lowest 16-bit chunk.
  rem = (rem >> 16) + ((unsigned short)xd >> 15);
  if (rem == 0) {
    if (xd == 0) {
      if (d != s) { mr(d, s); }
      return 0;
    }
    if (return_simm16_rest && (d == s)) {
      return xd;
    }
    addi(d, s, xd);
    return 0;
  }

  // Case 2: Can use addis.
  if (xd == 0) {
    short xc = rem & 0xFFFF; // 2nd 16-bit chunk.
    rem = (rem >> 16) + ((unsigned short)xc >> 15);
    if (rem == 0) {
      addis(d, s, xc);
      return 0;
    }
  }

  // Other cases: load & add.
  Register tmp1 = tmp,
           tmp2 = noreg;
  if ((d != tmp) && (d != s)) {
    // Can use d.
    tmp1 = d;
    tmp2 = tmp;
  }
  int simm16_rest = load_const_optimized(tmp1, x, tmp2, return_simm16_rest);
  add(d, tmp1, s);
  return simm16_rest;
}

#ifndef PRODUCT
// Test of ppc assembler.
void Assembler::test_asm() {
  // PPC 1, section 3.3.8, Fixed-Point Arithmetic Instructions
  addi(   R0,  R1,  10);
  addis(  R5,  R2,  11);
  addic_( R3,  R31, 42);
  subfic( R21, R12, 2112);
  add(    R3,  R2,  R1);
  add_(   R11, R22, R30);
  subf(   R7,  R6,  R5);
  subf_(  R8,  R9,  R4);
  addc(   R11, R12, R13);
  addc_(  R14, R14, R14);
  subfc(  R15, R16, R17);
  subfc_( R18, R20, R19);
  adde(   R20, R22, R24);
  adde_(  R29, R27, R26);
  subfe(  R28, R1,  R0);
  subfe_( R21, R11, R29);
  neg(    R21, R22);
  neg_(   R13, R23);
  mulli(  R0,  R11, -31);
  mulld(  R1,  R18, R21);
  mulld_( R2,  R17, R22);
  mullw(  R3,  R16, R23);
  mullw_( R4,  R15, R24);
  divd(   R5,  R14, R25);
  divd_(  R6,  R13, R26);
  divw(   R7,  R12, R27);
  divw_(  R8,  R11, R28);

  li(     R3, -4711);

  // PPC 1, section 3.3.9, Fixed-Point Compare Instructions
  cmpi(   CCR7,  0, R27, 4711);
  cmp(    CCR0, 1, R14, R11);
  cmpli(  CCR5,  1, R17, 45);
  cmpl(   CCR3, 0, R9,  R10);

  cmpwi(  CCR7,  R27, 4711);
  cmpw(   CCR0, R14, R11);
  cmplwi( CCR5,  R17, 45);
  cmplw(  CCR3, R9,  R10);

  cmpdi(  CCR7,  R27, 4711);
  cmpd(   CCR0, R14, R11);
  cmpldi( CCR5,  R17, 45);
  cmpld(  CCR3, R9,  R10);

  // PPC 1, section 3.3.11, Fixed-Point Logical Instructions
  andi_(  R4,  R5,  0xff);
  andis_( R12, R13, 0x7b51);
  ori(    R1,  R4,  13);
  oris(   R3,  R5,  177);
  xori(   R7,  R6,  51);
  xoris(  R29, R0,  1);
  andr(   R17, R21, R16);
  and_(   R3,  R5,  R15);
  orr(    R2,  R1,  R9);
  or_(    R17, R15, R11);
  xorr(   R19, R18, R10);
  xor_(   R31, R21, R11);
  nand(   R5,  R7,  R3);
  nand_(  R3,  R1,  R0);
  nor(    R2,  R3,  R5);
  nor_(   R3,  R6,  R8);
  andc(   R25, R12, R11);
  andc_(  R24, R22, R21);
  orc(    R20, R10, R12);
  orc_(   R22, R2,  R13);

  nop();

  // PPC 1, section 3.3.12, Fixed-Point Rotate and Shift Instructions
  sld(    R5,  R6,  R8);
  sld_(   R3,  R5,  R9);
  slw(    R2,  R1,  R10);
  slw_(   R6,  R26, R16);
  srd(    R16, R24, R8);
  srd_(   R21, R14, R7);
  srw(    R22, R25, R29);
  srw_(   R5,  R18, R17);
  srad(   R7,  R11, R0);
  srad_(  R9,  R13, R1);
  sraw(   R7,  R15, R2);
  sraw_(  R4,  R17, R3);
  sldi(   R3,  R18, 63);
  sldi_(  R2,  R20, 30);
  slwi(   R1,  R21, 30);
  slwi_(  R7,  R23, 8);
  srdi(   R0,  R19, 2);
  srdi_(  R12, R24, 5);
  srwi(   R13, R27, 6);
  srwi_(  R14, R29, 7);
  sradi(  R15, R30, 9);
  sradi_( R16, R31, 19);
  srawi(  R17, R31, 15);
  srawi_( R18, R31, 12);

  clrrdi( R3, R30, 5);
  clrldi( R9, R10, 11);

  rldicr( R19, R20, 13, 15);
  rldicr_(R20, R20, 16, 14);
  rldicl( R21, R21, 30, 33);
  rldicl_(R22, R1,  20, 25);
  rlwinm( R23, R2,  25, 10, 11);
  rlwinm_(R24, R3,  12, 13, 14);

  // PPC 1, section 3.3.2 Fixed-Point Load Instructions
  lwzx(   R3,  R5, R7);
  lwz(    R11,  0, R1);
  lwzu(   R31, -4, R11);

  lwax(   R3,  R5, R7);
  lwa(    R31, -4, R11);
  lhzx(   R3,  R5, R7);
  lhz(    R31, -4, R11);
  lhzu(   R31, -4, R11);


  lhax(   R3,  R5, R7);
  lha(    R31, -4, R11);
  lhau(   R11,  0, R1);

  lbzx(   R3,  R5, R7);
  lbz(    R31, -4, R11);
  lbzu(   R11,  0, R1);

  ld(     R31, -4, R11);
  ldx(    R3,  R5, R7);
  ldu(    R31, -4, R11);

  //  PPC 1, section 3.3.3 Fixed-Point Store Instructions
  stwx(   R3,  R5, R7);
  stw(    R31, -4, R11);
  stwu(   R11,  0, R1);

  sthx(   R3,  R5, R7 );
  sth(    R31, -4, R11);
  sthu(   R31, -4, R11);

  stbx(   R3,  R5, R7);
  stb(    R31, -4, R11);
  stbu(   R31, -4, R11);

  std(    R31, -4, R11);
  stdx(   R3,  R5, R7);
  stdu(   R31, -4, R11);

 // PPC 1, section 3.3.13 Move To/From System Register Instructions
  mtlr(   R3);
  mflr(   R3);
  mtctr(  R3);
  mfctr(  R3);
  mtcrf(  0xff, R15);
  mtcr(   R15);
  mtcrf(  0x03, R15);
  mtcr(   R15);
  mfcr(   R15);

 // PPC 1, section 2.4.1 Branch Instructions
  Label lbl1, lbl2, lbl3;
  bind(lbl1);

  b(pc());
  b(pc() - 8);
  b(lbl1);
  b(lbl2);
  b(lbl3);

  bl(pc() - 8);
  bl(lbl1);
  bl(lbl2);

  bcl(4, 10, pc() - 8);
  bcl(4, 10, lbl1);
  bcl(4, 10, lbl2);

  bclr( 4, 6, 0);
  bclrl(4, 6, 0);

  bind(lbl2);

  bcctr( 4, 6, 0);
  bcctrl(4, 6, 0);

  blt(CCR0, lbl2);
  bgt(CCR1, lbl2);
  beq(CCR2, lbl2);
  bso(CCR3, lbl2);
  bge(CCR4, lbl2);
  ble(CCR5, lbl2);
  bne(CCR6, lbl2);
  bns(CCR7, lbl2);

  bltl(CCR0, lbl2);
  bgtl(CCR1, lbl2);
  beql(CCR2, lbl2);
  bsol(CCR3, lbl2);
  bgel(CCR4, lbl2);
  blel(CCR5, lbl2);
  bnel(CCR6, lbl2);
  bnsl(CCR7, lbl2);
  blr();

  sync();
  icbi( R1, R2);
  dcbst(R2, R3);

  // FLOATING POINT instructions ppc.
  // PPC 1, section 4.6.2 Floating-Point Load Instructions
  lfs( F1, -11, R3);
  lfsu(F2, 123, R4);
  lfsx(F3, R5,  R6);
  lfd( F4, 456, R7);
  lfdu(F5, 789, R8);
  lfdx(F6, R10, R11);

  // PPC 1, section 4.6.3 Floating-Point Store Instructions
  stfs(  F7,  876, R12);
  stfsu( F8,  543, R13);
  stfsx( F9,  R14, R15);
  stfd(  F10, 210, R16);
  stfdu( F11, 111, R17);
  stfdx( F12, R18, R19);

  // PPC 1, section 4.6.4 Floating-Point Move Instructions
  fmr(   F13, F14);
  fmr_(  F14, F15);
  fneg(  F16, F17);
  fneg_( F18, F19);
  fabs(  F20, F21);
  fabs_( F22, F23);
  fnabs( F24, F25);
  fnabs_(F26, F27);

  // PPC 1, section 4.6.5.1 Floating-Point Elementary Arithmetic
  // Instructions
  fadd(  F28, F29, F30);
  fadd_( F31, F0,  F1);
  fadds( F2,  F3,  F4);
  fadds_(F5,  F6,  F7);
  fsub(  F8,  F9,  F10);
  fsub_( F11, F12, F13);
  fsubs( F14, F15, F16);
  fsubs_(F17, F18, F19);
  fmul(  F20, F21, F22);
  fmul_( F23, F24, F25);
  fmuls( F26, F27, F28);
  fmuls_(F29, F30, F31);
  fdiv(  F0,  F1,  F2);
  fdiv_( F3,  F4,  F5);
  fdivs( F6,  F7,  F8);
  fdivs_(F9,  F10, F11);

  // PPC 1, section 4.6.6 Floating-Point Rounding and Conversion
  // Instructions
  frsp(  F12, F13);
  fctid( F14, F15);
  fctidz(F16, F17);
  fctiw( F18, F19);
  fctiwz(F20, F21);
  fcfid( F22, F23);

  // PPC 1, section 4.6.7 Floating-Point Compare Instructions
  fcmpu( CCR7, F24, F25);

  tty->print_cr("\ntest_asm disassembly (0x%lx 0x%lx):", p2i(code()->insts_begin()), p2i(code()->insts_end()));
  code()->decode();
}

#endif // !PRODUCT
