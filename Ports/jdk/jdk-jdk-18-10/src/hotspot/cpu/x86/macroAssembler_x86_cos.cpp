/*
* Copyright (c) 2016, Intel Corporation.
* Intel Math Library (LIBM) Source Code
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
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "macroAssembler_x86.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/globalDefinitions.hpp"

/******************************************************************************/
//                     ALGORITHM DESCRIPTION - COS()
//                     ---------------------
//
//     1. RANGE REDUCTION
//
//     We perform an initial range reduction from X to r with
//
//          X =~= N * pi/32 + r
//
//     so that |r| <= pi/64 + epsilon. We restrict inputs to those
//     where |N| <= 932560. Beyond this, the range reduction is
//     insufficiently accurate. For extremely small inputs,
//     denormalization can occur internally, impacting performance.
//     This means that the main path is actually only taken for
//     2^-252 <= |X| < 90112.
//
//     To avoid branches, we perform the range reduction to full
//     accuracy each time.
//
//          X - N * (P_1 + P_2 + P_3)
//
//     where P_1 and P_2 are 32-bit numbers (so multiplication by N
//     is exact) and P_3 is a 53-bit number. Together, these
//     approximate pi well enough for all cases in the restricted
//     range.
//
//     The main reduction sequence is:
//
//             y = 32/pi * x
//             N = integer(y)
//     (computed by adding and subtracting off SHIFTER)
//
//             m_1 = N * P_1
//             m_2 = N * P_2
//             r_1 = x - m_1
//             r = r_1 - m_2
//     (this r can be used for most of the calculation)
//
//             c_1 = r_1 - r
//             m_3 = N * P_3
//             c_2 = c_1 - m_2
//             c = c_2 - m_3
//
//     2. MAIN ALGORITHM
//
//     The algorithm uses a table lookup based on B = M * pi / 32
//     where M = N mod 64. The stored values are:
//       sigma             closest power of 2 to cos(B)
//       C_hl              53-bit cos(B) - sigma
//       S_hi + S_lo       2 * 53-bit sin(B)
//
//     The computation is organized as follows:
//
//          sin(B + r + c) = [sin(B) + sigma * r] +
//                           r * (cos(B) - sigma) +
//                           sin(B) * [cos(r + c) - 1] +
//                           cos(B) * [sin(r + c) - r]
//
//     which is approximately:
//
//          [S_hi + sigma * r] +
//          C_hl * r +
//          S_lo + S_hi * [(cos(r) - 1) - r * c] +
//          (C_hl + sigma) * [(sin(r) - r) + c]
//
//     and this is what is actually computed. We separate this sum
//     into four parts:
//
//          hi + med + pols + corr
//
//     where
//
//          hi       = S_hi + sigma r
//          med      = C_hl * r
//          pols     = S_hi * (cos(r) - 1) + (C_hl + sigma) * (sin(r) - r)
//          corr     = S_lo + c * ((C_hl + sigma) - S_hi * r)
//
//     3. POLYNOMIAL
//
//     The polynomial S_hi * (cos(r) - 1) + (C_hl + sigma) *
//     (sin(r) - r) can be rearranged freely, since it is quite
//     small, so we exploit parallelism to the fullest.
//
//          psc4       =   SC_4 * r_1
//          msc4       =   psc4 * r
//          r2         =   r * r
//          msc2       =   SC_2 * r2
//          r4         =   r2 * r2
//          psc3       =   SC_3 + msc4
//          psc1       =   SC_1 + msc2
//          msc3       =   r4 * psc3
//          sincospols =   psc1 + msc3
//          pols       =   sincospols *
//                         <S_hi * r^2 | (C_hl + sigma) * r^3>
//
//     4. CORRECTION TERM
//
//     This is where the "c" component of the range reduction is
//     taken into account; recall that just "r" is used for most of
//     the calculation.
//
//          -c   = m_3 - c_2
//          -d   = S_hi * r - (C_hl + sigma)
//          corr = -c * -d + S_lo
//
//     5. COMPENSATED SUMMATIONS
//
//     The two successive compensated summations add up the high
//     and medium parts, leaving just the low parts to add up at
//     the end.
//
//          rs        =  sigma * r
//          res_int   =  S_hi + rs
//          k_0       =  S_hi - res_int
//          k_2       =  k_0 + rs
//          med       =  C_hl * r
//          res_hi    =  res_int + med
//          k_1       =  res_int - res_hi
//          k_3       =  k_1 + med
//
//     6. FINAL SUMMATION
//
//     We now add up all the small parts:
//
//          res_lo = pols(hi) + pols(lo) + corr + k_1 + k_3
//
//     Now the overall result is just:
//
//          res_hi + res_lo
//
//     7. SMALL ARGUMENTS
//
//     Inputs with |X| < 2^-252 are treated specially as
//     1 - |x|.
//
// Special cases:
//  cos(NaN) = quiet NaN, and raise invalid exception
//  cos(INF) = NaN and raise invalid exception
//  cos(0) = 1
//
/******************************************************************************/

#ifdef _LP64
// The 64 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(8) juint _ONE[] =
{
    0x00000000UL, 0x3ff00000UL
};
void MacroAssembler::fast_cos(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register r8, Register r9, Register r10, Register r11) {

  Label L_2TAG_PACKET_0_0_1, L_2TAG_PACKET_1_0_1, L_2TAG_PACKET_2_0_1, L_2TAG_PACKET_3_0_1;
  Label L_2TAG_PACKET_4_0_1, L_2TAG_PACKET_5_0_1, L_2TAG_PACKET_6_0_1, L_2TAG_PACKET_7_0_1;
  Label L_2TAG_PACKET_8_0_1, L_2TAG_PACKET_9_0_1, L_2TAG_PACKET_10_0_1, L_2TAG_PACKET_11_0_1;
  Label L_2TAG_PACKET_12_0_1, L_2TAG_PACKET_13_0_1, B1_2, B1_4, start;

  assert_different_registers(r8, r9, r10, r11, eax, ecx, edx);

  address ONEHALF = StubRoutines::x86::_ONEHALF_addr();
  address P_2 = StubRoutines::x86::_P_2_addr();
  address SC_4 = StubRoutines::x86::_SC_4_addr();
  address Ctable = StubRoutines::x86::_Ctable_addr();
  address SC_2 = StubRoutines::x86::_SC_2_addr();
  address SC_3 = StubRoutines::x86::_SC_3_addr();
  address SC_1 = StubRoutines::x86::_SC_1_addr();
  address PI_INV_TABLE = StubRoutines::x86::_PI_INV_TABLE_addr();
  address PI_4 = (address)StubRoutines::x86::_PI_4_addr();
  address PI32INV = (address)StubRoutines::x86::_PI32INV_addr();
  address SIGN_MASK = (address)StubRoutines::x86::_SIGN_MASK_addr();
  address P_1 = (address)StubRoutines::x86::_P_1_addr();
  address P_3 = (address)StubRoutines::x86::_P_3_addr();
  address ONE = (address)_ONE;
  address NEG_ZERO = (address)StubRoutines::x86::_NEG_ZERO_addr();

  bind(start);
  push(rbx);
  subq(rsp, 16);
  movsd(Address(rsp, 8), xmm0);

  bind(B1_2);
  movl(eax, Address(rsp, 12));
  movq(xmm1, ExternalAddress(PI32INV));    //0x6dc9c883UL, 0x40245f30UL
  andl(eax, 2147418112);
  subl(eax, 808452096);
  cmpl(eax, 281346048);
  jcc(Assembler::above, L_2TAG_PACKET_0_0_1);
  mulsd(xmm1, xmm0);
  movdqu(xmm5, ExternalAddress(ONEHALF));    //0x00000000UL, 0x3fe00000UL, 0x00000000UL, 0x3fe00000UL
  movq(xmm4, ExternalAddress(SIGN_MASK));    //0x00000000UL, 0x80000000UL
  pand(xmm4, xmm0);
  por(xmm5, xmm4);
  addpd(xmm1, xmm5);
  cvttsd2sil(edx, xmm1);
  cvtsi2sdl(xmm1, edx);
  movdqu(xmm2, ExternalAddress(P_2));    //0x1a600000UL, 0x3d90b461UL, 0x1a600000UL, 0x3d90b461UL
  movq(xmm3, ExternalAddress(P_1));    //0x54400000UL, 0x3fb921fbUL
  mulsd(xmm3, xmm1);
  unpcklpd(xmm1, xmm1);
  addq(rdx, 1865232);
  movdqu(xmm4, xmm0);
  andq(rdx, 63);
  movdqu(xmm5, ExternalAddress(SC_4));    //0xa556c734UL, 0x3ec71de3UL, 0x1a01a01aUL, 0x3efa01a0UL
  lea(rax, ExternalAddress(Ctable));
  shlq(rdx, 5);
  addq(rax, rdx);
  mulpd(xmm2, xmm1);
  subsd(xmm0, xmm3);
  mulsd(xmm1, ExternalAddress(P_3));    //0x2e037073UL, 0x3b63198aUL
  subsd(xmm4, xmm3);
  movq(xmm7, Address(rax, 8));
  unpcklpd(xmm0, xmm0);
  movdqu(xmm3, xmm4);
  subsd(xmm4, xmm2);
  mulpd(xmm5, xmm0);
  subpd(xmm0, xmm2);
  movdqu(xmm6, ExternalAddress(SC_2));    //0x11111111UL, 0x3f811111UL, 0x55555555UL, 0x3fa55555UL
  mulsd(xmm7, xmm4);
  subsd(xmm3, xmm4);
  mulpd(xmm5, xmm0);
  mulpd(xmm0, xmm0);
  subsd(xmm3, xmm2);
  movdqu(xmm2, Address(rax, 0));
  subsd(xmm1, xmm3);
  movq(xmm3, Address(rax, 24));
  addsd(xmm2, xmm3);
  subsd(xmm7, xmm2);
  mulsd(xmm2, xmm4);
  mulpd(xmm6, xmm0);
  mulsd(xmm3, xmm4);
  mulpd(xmm2, xmm0);
  mulpd(xmm0, xmm0);
  addpd(xmm5, ExternalAddress(SC_3));    //0x1a01a01aUL, 0xbf2a01a0UL, 0x16c16c17UL, 0xbf56c16cUL
  mulsd(xmm4, Address(rax, 0));
  addpd(xmm6, ExternalAddress(SC_1));    //0x55555555UL, 0xbfc55555UL, 0x00000000UL, 0xbfe00000UL
  mulpd(xmm5, xmm0);
  movdqu(xmm0, xmm3);
  addsd(xmm3, Address(rax, 8));
  mulpd(xmm1, xmm7);
  movdqu(xmm7, xmm4);
  addsd(xmm4, xmm3);
  addpd(xmm6, xmm5);
  movq(xmm5, Address(rax, 8));
  subsd(xmm5, xmm3);
  subsd(xmm3, xmm4);
  addsd(xmm1, Address(rax, 16));
  mulpd(xmm6, xmm2);
  addsd(xmm0, xmm5);
  addsd(xmm3, xmm7);
  addsd(xmm0, xmm1);
  addsd(xmm0, xmm3);
  addsd(xmm0, xmm6);
  unpckhpd(xmm6, xmm6);
  addsd(xmm0, xmm6);
  addsd(xmm0, xmm4);
  jmp(B1_4);

  bind(L_2TAG_PACKET_0_0_1);
  jcc(Assembler::greater, L_2TAG_PACKET_1_0_1);
  pextrw(eax, xmm0, 3);
  andl(eax, 32767);
  pinsrw(xmm0, eax, 3);
  movq(xmm1, ExternalAddress(ONE));    //0x00000000UL, 0x3ff00000UL
  subsd(xmm1, xmm0);
  movdqu(xmm0, xmm1);
  jmp(B1_4);

  bind(L_2TAG_PACKET_1_0_1);
  pextrw(eax, xmm0, 3);
  andl(eax, 32752);
  cmpl(eax, 32752);
  jcc(Assembler::equal, L_2TAG_PACKET_2_0_1);
  pextrw(ecx, xmm0, 3);
  andl(ecx, 32752);
  subl(ecx, 16224);
  shrl(ecx, 7);
  andl(ecx, 65532);
  lea(r11, ExternalAddress(PI_INV_TABLE));
  addq(rcx, r11);
  movdq(rax, xmm0);
  movl(r10, Address(rcx, 20));
  movl(r8, Address(rcx, 24));
  movl(edx, eax);
  shrq(rax, 21);
  orl(eax, INT_MIN);
  shrl(eax, 11);
  movl(r9, r10);
  imulq(r10, rdx);
  imulq(r9, rax);
  imulq(r8, rax);
  movl(rsi, Address(rcx, 16));
  movl(rdi, Address(rcx, 12));
  movl(r11, r10);
  shrq(r10, 32);
  addq(r9, r10);
  addq(r11, r8);
  movl(r8, r11);
  shrq(r11, 32);
  addq(r9, r11);
  movl(r10, rsi);
  imulq(rsi, rdx);
  imulq(r10, rax);
  movl(r11, rdi);
  imulq(rdi, rdx);
  movl(rbx, rsi);
  shrq(rsi, 32);
  addq(r9, rbx);
  movl(rbx, r9);
  shrq(r9, 32);
  addq(r10, rsi);
  addq(r10, r9);
  shlq(rbx, 32);
  orq(r8, rbx);
  imulq(r11, rax);
  movl(r9, Address(rcx, 8));
  movl(rsi, Address(rcx, 4));
  movl(rbx, rdi);
  shrq(rdi, 32);
  addq(r10, rbx);
  movl(rbx, r10);
  shrq(r10, 32);
  addq(r11, rdi);
  addq(r11, r10);
  movq(rdi, r9);
  imulq(r9, rdx);
  imulq(rdi, rax);
  movl(r10, r9);
  shrq(r9, 32);
  addq(r11, r10);
  movl(r10, r11);
  shrq(r11, 32);
  addq(rdi, r9);
  addq(rdi, r11);
  movq(r9, rsi);
  imulq(rsi, rdx);
  imulq(r9, rax);
  shlq(r10, 32);
  orq(r10, rbx);
  movl(eax, Address(rcx, 0));
  movl(r11, rsi);
  shrq(rsi, 32);
  addq(rdi, r11);
  movl(r11, rdi);
  shrq(rdi, 32);
  addq(r9, rsi);
  addq(r9, rdi);
  imulq(rdx, rax);
  pextrw(rbx, xmm0, 3);
  lea(rdi, ExternalAddress(PI_INV_TABLE));
  subq(rcx, rdi);
  addl(ecx, ecx);
  addl(ecx, ecx);
  addl(ecx, ecx);
  addl(ecx, 19);
  movl(rsi, 32768);
  andl(rsi, rbx);
  shrl(rbx, 4);
  andl(rbx, 2047);
  subl(rbx, 1023);
  subl(ecx, rbx);
  addq(r9, rdx);
  movl(edx, ecx);
  addl(edx, 32);
  cmpl(ecx, 1);
  jcc(Assembler::less, L_2TAG_PACKET_3_0_1);
  negl(ecx);
  addl(ecx, 29);
  shll(r9);
  movl(rdi, r9);
  andl(r9, 536870911);
  testl(r9, 268435456);
  jcc(Assembler::notEqual, L_2TAG_PACKET_4_0_1);
  shrl(r9);
  movl(rbx, 0);
  shlq(r9, 32);
  orq(r9, r11);

  bind(L_2TAG_PACKET_5_0_1);

  bind(L_2TAG_PACKET_6_0_1);
  cmpq(r9, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_7_0_1);

  bind(L_2TAG_PACKET_8_0_1);
  bsrq(r11, r9);
  movl(ecx, 29);
  subl(ecx, r11);
  jcc(Assembler::lessEqual, L_2TAG_PACKET_9_0_1);
  shlq(r9);
  movq(rax, r10);
  shlq(r10);
  addl(edx, ecx);
  negl(ecx);
  addl(ecx, 64);
  shrq(rax);
  shrq(r8);
  orq(r9, rax);
  orq(r10, r8);

  bind(L_2TAG_PACKET_10_0_1);
  cvtsi2sdq(xmm0, r9);
  shrq(r10, 1);
  cvtsi2sdq(xmm3, r10);
  xorpd(xmm4, xmm4);
  shll(edx, 4);
  negl(edx);
  addl(edx, 16368);
  orl(edx, rsi);
  xorl(edx, rbx);
  pinsrw(xmm4, edx, 3);
  movq(xmm2, ExternalAddress(PI_4));    //0x40000000UL, 0x3fe921fbUL, 0x18469899UL, 0x3e64442dUL
  movq(xmm6, ExternalAddress(8 + PI_4));    //0x3fe921fbUL, 0x18469899UL, 0x3e64442dUL
  xorpd(xmm5, xmm5);
  subl(edx, 1008);
  pinsrw(xmm5, edx, 3);
  mulsd(xmm0, xmm4);
  shll(rsi, 16);
  sarl(rsi, 31);
  mulsd(xmm3, xmm5);
  movdqu(xmm1, xmm0);
  mulsd(xmm0, xmm2);
  shrl(rdi, 29);
  addsd(xmm1, xmm3);
  mulsd(xmm3, xmm2);
  addl(rdi, rsi);
  xorl(rdi, rsi);
  mulsd(xmm6, xmm1);
  movl(eax, rdi);
  addsd(xmm6, xmm3);
  movdqu(xmm2, xmm0);
  addsd(xmm0, xmm6);
  subsd(xmm2, xmm0);
  addsd(xmm6, xmm2);

  bind(L_2TAG_PACKET_11_0_1);
  movq(xmm1, ExternalAddress(PI32INV));    //0x6dc9c883UL, 0x40245f30UL
  mulsd(xmm1, xmm0);
  movq(xmm5, ExternalAddress(ONEHALF));    //0x00000000UL, 0x3fe00000UL, 0x00000000UL, 0x3fe00000UL
  movq(xmm4, ExternalAddress(SIGN_MASK));    //0x00000000UL, 0x80000000UL
  pand(xmm4, xmm0);
  por(xmm5, xmm4);
  addpd(xmm1, xmm5);
  cvttsd2siq(rdx, xmm1);
  cvtsi2sdq(xmm1, rdx);
  movq(xmm3, ExternalAddress(P_1));    //0x54400000UL, 0x3fb921fbUL
  movdqu(xmm2, ExternalAddress(P_2));    //0x1a600000UL, 0x3d90b461UL, 0x1a600000UL, 0x3d90b461UL
  mulsd(xmm3, xmm1);
  unpcklpd(xmm1, xmm1);
  shll(eax, 3);
  addl(edx, 1865232);
  movdqu(xmm4, xmm0);
  addl(edx, eax);
  andl(edx, 63);
  movdqu(xmm5, ExternalAddress(SC_4));    //0xa556c734UL, 0x3ec71de3UL, 0x1a01a01aUL, 0x3efa01a0UL
  lea(rax, ExternalAddress(Ctable));
  shll(edx, 5);
  addq(rax, rdx);
  mulpd(xmm2, xmm1);
  subsd(xmm0, xmm3);
  mulsd(xmm1, ExternalAddress(P_3));    //0x2e037073UL, 0x3b63198aUL
  subsd(xmm4, xmm3);
  movq(xmm7, Address(rax, 8));
  unpcklpd(xmm0, xmm0);
  movdqu(xmm3, xmm4);
  subsd(xmm4, xmm2);
  mulpd(xmm5, xmm0);
  subpd(xmm0, xmm2);
  mulsd(xmm7, xmm4);
  subsd(xmm3, xmm4);
  mulpd(xmm5, xmm0);
  mulpd(xmm0, xmm0);
  subsd(xmm3, xmm2);
  movdqu(xmm2, Address(rax, 0));
  subsd(xmm1, xmm3);
  movq(xmm3, Address(rax, 24));
  addsd(xmm2, xmm3);
  subsd(xmm7, xmm2);
  subsd(xmm1, xmm6);
  movdqu(xmm6, ExternalAddress(SC_2));    //0x11111111UL, 0x3f811111UL, 0x55555555UL, 0x3fa55555UL
  mulsd(xmm2, xmm4);
  mulpd(xmm6, xmm0);
  mulsd(xmm3, xmm4);
  mulpd(xmm2, xmm0);
  mulpd(xmm0, xmm0);
  addpd(xmm5, ExternalAddress(SC_3));    //0x1a01a01aUL, 0xbf2a01a0UL, 0x16c16c17UL, 0xbf56c16cUL
  mulsd(xmm4, Address(rax, 0));
  addpd(xmm6, ExternalAddress(SC_1));    //0x55555555UL, 0xbfc55555UL, 0x00000000UL, 0xbfe00000UL
  mulpd(xmm5, xmm0);
  movdqu(xmm0, xmm3);
  addsd(xmm3, Address(rax, 8));
  mulpd(xmm1, xmm7);
  movdqu(xmm7, xmm4);
  addsd(xmm4, xmm3);
  addpd(xmm6, xmm5);
  movq(xmm5, Address(rax, 8));
  subsd(xmm5, xmm3);
  subsd(xmm3, xmm4);
  addsd(xmm1, Address(rax, 16));
  mulpd(xmm6, xmm2);
  addsd(xmm5, xmm0);
  addsd(xmm3, xmm7);
  addsd(xmm1, xmm5);
  addsd(xmm1, xmm3);
  addsd(xmm1, xmm6);
  unpckhpd(xmm6, xmm6);
  movdqu(xmm0, xmm4);
  addsd(xmm1, xmm6);
  addsd(xmm0, xmm1);
  jmp(B1_4);

  bind(L_2TAG_PACKET_7_0_1);
  addl(edx, 64);
  movq(r9, r10);
  movq(r10, r8);
  movl(r8, 0);
  cmpq(r9, 0);
  jcc(Assembler::notEqual, L_2TAG_PACKET_8_0_1);
  addl(edx, 64);
  movq(r9, r10);
  movq(r10, r8);
  cmpq(r9, 0);
  jcc(Assembler::notEqual, L_2TAG_PACKET_8_0_1);
  xorpd(xmm0, xmm0);
  xorpd(xmm6, xmm6);
  jmp(L_2TAG_PACKET_11_0_1);

  bind(L_2TAG_PACKET_9_0_1);
  jcc(Assembler::equal, L_2TAG_PACKET_10_0_1);
  negl(ecx);
  shrq(r10);
  movq(rax, r9);
  shrq(r9);
  subl(edx, ecx);
  negl(ecx);
  addl(ecx, 64);
  shlq(rax);
  orq(r10, rax);
  jmp(L_2TAG_PACKET_10_0_1);
  bind(L_2TAG_PACKET_3_0_1);
  negl(ecx);
  shlq(r9, 32);
  orq(r9, r11);
  shlq(r9);
  movq(rdi, r9);
  testl(r9, INT_MIN);
  jcc(Assembler::notEqual, L_2TAG_PACKET_12_0_1);
  shrl(r9);
  movl(rbx, 0);
  shrq(rdi, 3);
  jmp(L_2TAG_PACKET_6_0_1);

  bind(L_2TAG_PACKET_4_0_1);
  shrl(r9);
  movl(rbx, 536870912);
  shrl(rbx);
  shlq(r9, 32);
  orq(r9, r11);
  shlq(rbx, 32);
  addl(rdi, 536870912);
  movl(rcx, 0);
  movl(r11, 0);
  subq(rcx, r8);
  sbbq(r11, r10);
  sbbq(rbx, r9);
  movq(r8, rcx);
  movq(r10, r11);
  movq(r9, rbx);
  movl(rbx, 32768);
  jmp(L_2TAG_PACKET_5_0_1);

  bind(L_2TAG_PACKET_12_0_1);
  shrl(r9);
  mov64(rbx, 0x100000000);
  shrq(rbx);
  movl(rcx, 0);
  movl(r11, 0);
  subq(rcx, r8);
  sbbq(r11, r10);
  sbbq(rbx, r9);
  movq(r8, rcx);
  movq(r10, r11);
  movq(r9, rbx);
  movl(rbx, 32768);
  shrq(rdi, 3);
  addl(rdi, 536870912);
  jmp(L_2TAG_PACKET_6_0_1);

  bind(L_2TAG_PACKET_2_0_1);
  movsd(xmm0, Address(rsp, 8));
  mulsd(xmm0, ExternalAddress(NEG_ZERO));    //0x00000000UL, 0x80000000UL
  movq(Address(rsp, 0), xmm0);

  bind(L_2TAG_PACKET_13_0_1);

  bind(B1_4);
  addq(rsp, 16);
  pop(rbx);
}
#else
// The 32 bit code is at most SSE2 compliant

ATTRIBUTE_ALIGNED(16) juint _static_const_table_cos[] =
{
    0x00000000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x00000000UL, 0x3ff00000UL, 0x176d6d31UL, 0xbf73b92eUL,
    0xbc29b42cUL, 0x3fb917a6UL, 0xe0000000UL, 0xbc3e2718UL, 0x00000000UL,
    0x3ff00000UL, 0x011469fbUL, 0xbf93ad06UL, 0x3c69a60bUL, 0x3fc8f8b8UL,
    0xc0000000UL, 0xbc626d19UL, 0x00000000UL, 0x3ff00000UL, 0x939d225aUL,
    0xbfa60beaUL, 0x2ed59f06UL, 0x3fd29406UL, 0xa0000000UL, 0xbc75d28dUL,
    0x00000000UL, 0x3ff00000UL, 0x866b95cfUL, 0xbfb37ca1UL, 0xa6aea963UL,
    0x3fd87de2UL, 0xe0000000UL, 0xbc672cedUL, 0x00000000UL, 0x3ff00000UL,
    0x73fa1279UL, 0xbfbe3a68UL, 0x3806f63bUL, 0x3fde2b5dUL, 0x20000000UL,
    0x3c5e0d89UL, 0x00000000UL, 0x3ff00000UL, 0x5bc57974UL, 0xbfc59267UL,
    0x39ae68c8UL, 0x3fe1c73bUL, 0x20000000UL, 0x3c8b25ddUL, 0x00000000UL,
    0x3ff00000UL, 0x53aba2fdUL, 0xbfcd0dfeUL, 0x25091dd6UL, 0x3fe44cf3UL,
    0x20000000UL, 0x3c68076aUL, 0x00000000UL, 0x3ff00000UL, 0x99fcef32UL,
    0x3fca8279UL, 0x667f3bcdUL, 0x3fe6a09eUL, 0x20000000UL, 0xbc8bdd34UL,
    0x00000000UL, 0x3fe00000UL, 0x94247758UL, 0x3fc133ccUL, 0x6b151741UL,
    0x3fe8bc80UL, 0x20000000UL, 0xbc82c5e1UL, 0x00000000UL, 0x3fe00000UL,
    0x9ae68c87UL, 0x3fac73b3UL, 0x290ea1a3UL, 0x3fea9b66UL, 0xe0000000UL,
    0x3c39f630UL, 0x00000000UL, 0x3fe00000UL, 0x7f909c4eUL, 0xbf9d4a2cUL,
    0xf180bdb1UL, 0x3fec38b2UL, 0x80000000UL, 0xbc76e0b1UL, 0x00000000UL,
    0x3fe00000UL, 0x65455a75UL, 0xbfbe0875UL, 0xcf328d46UL, 0x3fed906bUL,
    0x20000000UL, 0x3c7457e6UL, 0x00000000UL, 0x3fe00000UL, 0x76acf82dUL,
    0x3fa4a031UL, 0x56c62ddaUL, 0x3fee9f41UL, 0xe0000000UL, 0x3c8760b1UL,
    0x00000000UL, 0x3fd00000UL, 0x0e5967d5UL, 0xbfac1d1fUL, 0xcff75cb0UL,
    0x3fef6297UL, 0x20000000UL, 0x3c756217UL, 0x00000000UL, 0x3fd00000UL,
    0x0f592f50UL, 0xbf9ba165UL, 0xa3d12526UL, 0x3fefd88dUL, 0x40000000UL,
    0xbc887df6UL, 0x00000000UL, 0x3fc00000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x3ff00000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x0f592f50UL, 0x3f9ba165UL, 0xa3d12526UL, 0x3fefd88dUL,
    0x40000000UL, 0xbc887df6UL, 0x00000000UL, 0xbfc00000UL, 0x0e5967d5UL,
    0x3fac1d1fUL, 0xcff75cb0UL, 0x3fef6297UL, 0x20000000UL, 0x3c756217UL,
    0x00000000UL, 0xbfd00000UL, 0x76acf82dUL, 0xbfa4a031UL, 0x56c62ddaUL,
    0x3fee9f41UL, 0xe0000000UL, 0x3c8760b1UL, 0x00000000UL, 0xbfd00000UL,
    0x65455a75UL, 0x3fbe0875UL, 0xcf328d46UL, 0x3fed906bUL, 0x20000000UL,
    0x3c7457e6UL, 0x00000000UL, 0xbfe00000UL, 0x7f909c4eUL, 0x3f9d4a2cUL,
    0xf180bdb1UL, 0x3fec38b2UL, 0x80000000UL, 0xbc76e0b1UL, 0x00000000UL,
    0xbfe00000UL, 0x9ae68c87UL, 0xbfac73b3UL, 0x290ea1a3UL, 0x3fea9b66UL,
    0xe0000000UL, 0x3c39f630UL, 0x00000000UL, 0xbfe00000UL, 0x94247758UL,
    0xbfc133ccUL, 0x6b151741UL, 0x3fe8bc80UL, 0x20000000UL, 0xbc82c5e1UL,
    0x00000000UL, 0xbfe00000UL, 0x99fcef32UL, 0xbfca8279UL, 0x667f3bcdUL,
    0x3fe6a09eUL, 0x20000000UL, 0xbc8bdd34UL, 0x00000000UL, 0xbfe00000UL,
    0x53aba2fdUL, 0x3fcd0dfeUL, 0x25091dd6UL, 0x3fe44cf3UL, 0x20000000UL,
    0x3c68076aUL, 0x00000000UL, 0xbff00000UL, 0x5bc57974UL, 0x3fc59267UL,
    0x39ae68c8UL, 0x3fe1c73bUL, 0x20000000UL, 0x3c8b25ddUL, 0x00000000UL,
    0xbff00000UL, 0x73fa1279UL, 0x3fbe3a68UL, 0x3806f63bUL, 0x3fde2b5dUL,
    0x20000000UL, 0x3c5e0d89UL, 0x00000000UL, 0xbff00000UL, 0x866b95cfUL,
    0x3fb37ca1UL, 0xa6aea963UL, 0x3fd87de2UL, 0xe0000000UL, 0xbc672cedUL,
    0x00000000UL, 0xbff00000UL, 0x939d225aUL, 0x3fa60beaUL, 0x2ed59f06UL,
    0x3fd29406UL, 0xa0000000UL, 0xbc75d28dUL, 0x00000000UL, 0xbff00000UL,
    0x011469fbUL, 0x3f93ad06UL, 0x3c69a60bUL, 0x3fc8f8b8UL, 0xc0000000UL,
    0xbc626d19UL, 0x00000000UL, 0xbff00000UL, 0x176d6d31UL, 0x3f73b92eUL,
    0xbc29b42cUL, 0x3fb917a6UL, 0xe0000000UL, 0xbc3e2718UL, 0x00000000UL,
    0xbff00000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x00000000UL, 0x00000000UL, 0xbff00000UL, 0x176d6d31UL,
    0x3f73b92eUL, 0xbc29b42cUL, 0xbfb917a6UL, 0xe0000000UL, 0x3c3e2718UL,
    0x00000000UL, 0xbff00000UL, 0x011469fbUL, 0x3f93ad06UL, 0x3c69a60bUL,
    0xbfc8f8b8UL, 0xc0000000UL, 0x3c626d19UL, 0x00000000UL, 0xbff00000UL,
    0x939d225aUL, 0x3fa60beaUL, 0x2ed59f06UL, 0xbfd29406UL, 0xa0000000UL,
    0x3c75d28dUL, 0x00000000UL, 0xbff00000UL, 0x866b95cfUL, 0x3fb37ca1UL,
    0xa6aea963UL, 0xbfd87de2UL, 0xe0000000UL, 0x3c672cedUL, 0x00000000UL,
    0xbff00000UL, 0x73fa1279UL, 0x3fbe3a68UL, 0x3806f63bUL, 0xbfde2b5dUL,
    0x20000000UL, 0xbc5e0d89UL, 0x00000000UL, 0xbff00000UL, 0x5bc57974UL,
    0x3fc59267UL, 0x39ae68c8UL, 0xbfe1c73bUL, 0x20000000UL, 0xbc8b25ddUL,
    0x00000000UL, 0xbff00000UL, 0x53aba2fdUL, 0x3fcd0dfeUL, 0x25091dd6UL,
    0xbfe44cf3UL, 0x20000000UL, 0xbc68076aUL, 0x00000000UL, 0xbff00000UL,
    0x99fcef32UL, 0xbfca8279UL, 0x667f3bcdUL, 0xbfe6a09eUL, 0x20000000UL,
    0x3c8bdd34UL, 0x00000000UL, 0xbfe00000UL, 0x94247758UL, 0xbfc133ccUL,
    0x6b151741UL, 0xbfe8bc80UL, 0x20000000UL, 0x3c82c5e1UL, 0x00000000UL,
    0xbfe00000UL, 0x9ae68c87UL, 0xbfac73b3UL, 0x290ea1a3UL, 0xbfea9b66UL,
    0xe0000000UL, 0xbc39f630UL, 0x00000000UL, 0xbfe00000UL, 0x7f909c4eUL,
    0x3f9d4a2cUL, 0xf180bdb1UL, 0xbfec38b2UL, 0x80000000UL, 0x3c76e0b1UL,
    0x00000000UL, 0xbfe00000UL, 0x65455a75UL, 0x3fbe0875UL, 0xcf328d46UL,
    0xbfed906bUL, 0x20000000UL, 0xbc7457e6UL, 0x00000000UL, 0xbfe00000UL,
    0x76acf82dUL, 0xbfa4a031UL, 0x56c62ddaUL, 0xbfee9f41UL, 0xe0000000UL,
    0xbc8760b1UL, 0x00000000UL, 0xbfd00000UL, 0x0e5967d5UL, 0x3fac1d1fUL,
    0xcff75cb0UL, 0xbfef6297UL, 0x20000000UL, 0xbc756217UL, 0x00000000UL,
    0xbfd00000UL, 0x0f592f50UL, 0x3f9ba165UL, 0xa3d12526UL, 0xbfefd88dUL,
    0x40000000UL, 0x3c887df6UL, 0x00000000UL, 0xbfc00000UL, 0x00000000UL,
    0x00000000UL, 0x00000000UL, 0xbff00000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x00000000UL, 0x0f592f50UL, 0xbf9ba165UL, 0xa3d12526UL,
    0xbfefd88dUL, 0x40000000UL, 0x3c887df6UL, 0x00000000UL, 0x3fc00000UL,
    0x0e5967d5UL, 0xbfac1d1fUL, 0xcff75cb0UL, 0xbfef6297UL, 0x20000000UL,
    0xbc756217UL, 0x00000000UL, 0x3fd00000UL, 0x76acf82dUL, 0x3fa4a031UL,
    0x56c62ddaUL, 0xbfee9f41UL, 0xe0000000UL, 0xbc8760b1UL, 0x00000000UL,
    0x3fd00000UL, 0x65455a75UL, 0xbfbe0875UL, 0xcf328d46UL, 0xbfed906bUL,
    0x20000000UL, 0xbc7457e6UL, 0x00000000UL, 0x3fe00000UL, 0x7f909c4eUL,
    0xbf9d4a2cUL, 0xf180bdb1UL, 0xbfec38b2UL, 0x80000000UL, 0x3c76e0b1UL,
    0x00000000UL, 0x3fe00000UL, 0x9ae68c87UL, 0x3fac73b3UL, 0x290ea1a3UL,
    0xbfea9b66UL, 0xe0000000UL, 0xbc39f630UL, 0x00000000UL, 0x3fe00000UL,
    0x94247758UL, 0x3fc133ccUL, 0x6b151741UL, 0xbfe8bc80UL, 0x20000000UL,
    0x3c82c5e1UL, 0x00000000UL, 0x3fe00000UL, 0x99fcef32UL, 0x3fca8279UL,
    0x667f3bcdUL, 0xbfe6a09eUL, 0x20000000UL, 0x3c8bdd34UL, 0x00000000UL,
    0x3fe00000UL, 0x53aba2fdUL, 0xbfcd0dfeUL, 0x25091dd6UL, 0xbfe44cf3UL,
    0x20000000UL, 0xbc68076aUL, 0x00000000UL, 0x3ff00000UL, 0x5bc57974UL,
    0xbfc59267UL, 0x39ae68c8UL, 0xbfe1c73bUL, 0x20000000UL, 0xbc8b25ddUL,
    0x00000000UL, 0x3ff00000UL, 0x73fa1279UL, 0xbfbe3a68UL, 0x3806f63bUL,
    0xbfde2b5dUL, 0x20000000UL, 0xbc5e0d89UL, 0x00000000UL, 0x3ff00000UL,
    0x866b95cfUL, 0xbfb37ca1UL, 0xa6aea963UL, 0xbfd87de2UL, 0xe0000000UL,
    0x3c672cedUL, 0x00000000UL, 0x3ff00000UL, 0x939d225aUL, 0xbfa60beaUL,
    0x2ed59f06UL, 0xbfd29406UL, 0xa0000000UL, 0x3c75d28dUL, 0x00000000UL,
    0x3ff00000UL, 0x011469fbUL, 0xbf93ad06UL, 0x3c69a60bUL, 0xbfc8f8b8UL,
    0xc0000000UL, 0x3c626d19UL, 0x00000000UL, 0x3ff00000UL, 0x176d6d31UL,
    0xbf73b92eUL, 0xbc29b42cUL, 0xbfb917a6UL, 0xe0000000UL, 0x3c3e2718UL,
    0x00000000UL, 0x3ff00000UL, 0x55555555UL, 0xbfc55555UL, 0x00000000UL,
    0xbfe00000UL, 0x11111111UL, 0x3f811111UL, 0x55555555UL, 0x3fa55555UL,
    0x1a01a01aUL, 0xbf2a01a0UL, 0x16c16c17UL, 0xbf56c16cUL, 0xa556c734UL,
    0x3ec71de3UL, 0x1a01a01aUL, 0x3efa01a0UL, 0x1a600000UL, 0x3d90b461UL,
    0x1a600000UL, 0x3d90b461UL, 0x54400000UL, 0x3fb921fbUL, 0x00000000UL,
    0x00000000UL, 0x2e037073UL, 0x3b63198aUL, 0x00000000UL, 0x00000000UL,
    0x6dc9c883UL, 0x40245f30UL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x43380000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL, 0x3ff00000UL,
    0x00000000UL, 0x00000000UL, 0x00000000UL, 0x80000000UL, 0x00000000UL,
    0x00000000UL, 0x00000000UL, 0x80000000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x3fe00000UL, 0x00000000UL, 0x3fe00000UL
};
//registers,
// input: (rbp + 8)
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, rbx (tmp)

// Code generated by Intel C compiler for LIBM library

void MacroAssembler::fast_cos(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register tmp) {
  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label start;

  assert_different_registers(tmp, eax, ecx, edx);

  address static_const_table_cos = (address)_static_const_table_cos;

  bind(start);
  subl(rsp, 120);
  movl(Address(rsp, 56), tmp);
  lea(tmp, ExternalAddress(static_const_table_cos));
  movsd(xmm0, Address(rsp, 128));
  pextrw(eax, xmm0, 3);
  andl(eax, 32767);
  subl(eax, 12336);
  cmpl(eax, 4293);
  jcc(Assembler::above, L_2TAG_PACKET_0_0_2);
  movsd(xmm1, Address(tmp, 2160));
  mulsd(xmm1, xmm0);
  movdqu(xmm5, Address(tmp, 2240));
  movsd(xmm4, Address(tmp, 2224));
  pand(xmm4, xmm0);
  por(xmm5, xmm4);
  movsd(xmm3, Address(tmp, 2128));
  movdqu(xmm2, Address(tmp, 2112));
  addpd(xmm1, xmm5);
  cvttsd2sil(edx, xmm1);
  cvtsi2sdl(xmm1, edx);
  mulsd(xmm3, xmm1);
  unpcklpd(xmm1, xmm1);
  addl(edx, 1865232);
  movdqu(xmm4, xmm0);
  andl(edx, 63);
  movdqu(xmm5, Address(tmp, 2096));
  lea(eax, Address(tmp, 0));
  shll(edx, 5);
  addl(eax, edx);
  mulpd(xmm2, xmm1);
  subsd(xmm0, xmm3);
  mulsd(xmm1, Address(tmp, 2144));
  subsd(xmm4, xmm3);
  movsd(xmm7, Address(eax, 8));
  unpcklpd(xmm0, xmm0);
  movapd(xmm3, xmm4);
  subsd(xmm4, xmm2);
  mulpd(xmm5, xmm0);
  subpd(xmm0, xmm2);
  movdqu(xmm6, Address(tmp, 2064));
  mulsd(xmm7, xmm4);
  subsd(xmm3, xmm4);
  mulpd(xmm5, xmm0);
  mulpd(xmm0, xmm0);
  subsd(xmm3, xmm2);
  movdqu(xmm2, Address(eax, 0));
  subsd(xmm1, xmm3);
  movsd(xmm3, Address(eax, 24));
  addsd(xmm2, xmm3);
  subsd(xmm7, xmm2);
  mulsd(xmm2, xmm4);
  mulpd(xmm6, xmm0);
  mulsd(xmm3, xmm4);
  mulpd(xmm2, xmm0);
  mulpd(xmm0, xmm0);
  addpd(xmm5, Address(tmp, 2080));
  mulsd(xmm4, Address(eax, 0));
  addpd(xmm6, Address(tmp, 2048));
  mulpd(xmm5, xmm0);
  movapd(xmm0, xmm3);
  addsd(xmm3, Address(eax, 8));
  mulpd(xmm1, xmm7);
  movapd(xmm7, xmm4);
  addsd(xmm4, xmm3);
  addpd(xmm6, xmm5);
  movsd(xmm5, Address(eax, 8));
  subsd(xmm5, xmm3);
  subsd(xmm3, xmm4);
  addsd(xmm1, Address(eax, 16));
  mulpd(xmm6, xmm2);
  addsd(xmm5, xmm0);
  addsd(xmm3, xmm7);
  addsd(xmm1, xmm5);
  addsd(xmm1, xmm3);
  addsd(xmm1, xmm6);
  unpckhpd(xmm6, xmm6);
  addsd(xmm1, xmm6);
  addsd(xmm4, xmm1);
  movsd(Address(rsp, 0), xmm4);
  fld_d(Address(rsp, 0));
  jmp(L_2TAG_PACKET_1_0_2);

  bind(L_2TAG_PACKET_0_0_2);
  jcc(Assembler::greater, L_2TAG_PACKET_2_0_2);
  pextrw(eax, xmm0, 3);
  andl(eax, 32767);
  pinsrw(xmm0, eax, 3);
  movsd(xmm1, Address(tmp, 2192));
  subsd(xmm1, xmm0);
  movsd(Address(rsp, 0), xmm1);
  fld_d(Address(rsp, 0));
  jmp(L_2TAG_PACKET_1_0_2);

  bind(L_2TAG_PACKET_2_0_2);
  movl(eax, Address(rsp, 132));
  andl(eax, 2146435072);
  cmpl(eax, 2146435072);
  jcc(Assembler::equal, L_2TAG_PACKET_3_0_2);
  subl(rsp, 32);
  movsd(Address(rsp, 0), xmm0);
  lea(eax, Address(rsp, 40));
  movl(Address(rsp, 8), eax);
  movl(eax, 1);
  movl(Address(rsp, 12), eax);
  call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dlibm_sin_cos_huge())));
  addl(rsp, 32);
  fld_d(Address(rsp, 8));
  jmp(L_2TAG_PACKET_1_0_2);

  bind(L_2TAG_PACKET_3_0_2);
  fld_d(Address(rsp, 128));
  fmul_d(Address(tmp, 2208));

  bind(L_2TAG_PACKET_1_0_2);
  movl(tmp, Address(rsp, 56));
}
#endif
