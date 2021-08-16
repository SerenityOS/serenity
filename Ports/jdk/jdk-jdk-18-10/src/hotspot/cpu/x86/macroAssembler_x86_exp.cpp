/*
* Copyright (c) 2016, Intel Corporation.
* Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
//                     ALGORITHM DESCRIPTION - EXP()
//                     ---------------------
//
// Description:
//  Let K = 64 (table size).
//        x    x/log(2)     n
//       e  = 2          = 2 * T[j] * (1 + P(y))
//  where
//       x = m*log(2)/K + y,    y in [-log(2)/K..log(2)/K]
//       m = n*K + j,           m,n,j - signed integer, j in [-K/2..K/2]
//                  j/K
//       values of 2   are tabulated as T[j] = T_hi[j] ( 1 + T_lo[j]).
//
//       P(y) is a minimax polynomial approximation of exp(x)-1
//       on small interval [-log(2)/K..log(2)/K] (were calculated by Maple V).
//
//  To avoid problems with arithmetic overflow and underflow,
//            n                        n1  n2
//  value of 2  is safely computed as 2 * 2 where n1 in [-BIAS/2..BIAS/2]
//  where BIAS is a value of exponent bias.
//
// Special cases:
//  exp(NaN) = NaN
//  exp(+INF) = +INF
//  exp(-INF) = 0
//  exp(x) = 1 for subnormals
//  for finite argument, only exp(0)=1 is exact
//  For IEEE double
//    if x >  709.782712893383973096 then exp(x) overflow
//    if x < -745.133219101941108420 then exp(x) underflow
//
/******************************************************************************/

#ifdef _LP64
// The 64 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(16) juint _cv[] =
{
    0x652b82feUL, 0x40571547UL, 0x652b82feUL, 0x40571547UL, 0xfefa0000UL,
    0x3f862e42UL, 0xfefa0000UL, 0x3f862e42UL, 0xbc9e3b3aUL, 0x3d1cf79aUL,
    0xbc9e3b3aUL, 0x3d1cf79aUL, 0xfffffffeUL, 0x3fdfffffUL, 0xfffffffeUL,
    0x3fdfffffUL, 0xe3289860UL, 0x3f56c15cUL, 0x555b9e25UL, 0x3fa55555UL,
    0xc090cf0fUL, 0x3f811115UL, 0x55548ba1UL, 0x3fc55555UL
};

ATTRIBUTE_ALIGNED(16) juint _shifter[] =
{
    0x00000000UL, 0x43380000UL, 0x00000000UL, 0x43380000UL
};

ATTRIBUTE_ALIGNED(16) juint _mmask[] =
{
    0xffffffc0UL, 0x00000000UL, 0xffffffc0UL, 0x00000000UL
};

ATTRIBUTE_ALIGNED(16) juint _bias[] =
{
    0x0000ffc0UL, 0x00000000UL, 0x0000ffc0UL, 0x00000000UL
};

ATTRIBUTE_ALIGNED(16) juint _Tbl_addr[] =
{
    0x00000000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL, 0x0e03754dUL,
    0x3cad7bbfUL, 0x3e778060UL, 0x00002c9aUL, 0x3567f613UL, 0x3c8cd252UL,
    0xd3158574UL, 0x000059b0UL, 0x61e6c861UL, 0x3c60f74eUL, 0x18759bc8UL,
    0x00008745UL, 0x5d837b6cUL, 0x3c979aa6UL, 0x6cf9890fUL, 0x0000b558UL,
    0x702f9cd1UL, 0x3c3ebe3dUL, 0x32d3d1a2UL, 0x0000e3ecUL, 0x1e63bcd8UL,
    0x3ca3516eUL, 0xd0125b50UL, 0x00011301UL, 0x26f0387bUL, 0x3ca4c554UL,
    0xaea92ddfUL, 0x0001429aUL, 0x62523fb6UL, 0x3ca95153UL, 0x3c7d517aUL,
    0x000172b8UL, 0x3f1353bfUL, 0x3c8b898cUL, 0xeb6fcb75UL, 0x0001a35bUL,
    0x3e3a2f5fUL, 0x3c9aecf7UL, 0x3168b9aaUL, 0x0001d487UL, 0x44a6c38dUL,
    0x3c8a6f41UL, 0x88628cd6UL, 0x0002063bUL, 0xe3a8a894UL, 0x3c968efdUL,
    0x6e756238UL, 0x0002387aUL, 0x981fe7f2UL, 0x3c80472bUL, 0x65e27cddUL,
    0x00026b45UL, 0x6d09ab31UL, 0x3c82f7e1UL, 0xf51fdee1UL, 0x00029e9dUL,
    0x720c0ab3UL, 0x3c8b3782UL, 0xa6e4030bUL, 0x0002d285UL, 0x4db0abb6UL,
    0x3c834d75UL, 0x0a31b715UL, 0x000306feUL, 0x5dd3f84aUL, 0x3c8fdd39UL,
    0xb26416ffUL, 0x00033c08UL, 0xcc187d29UL, 0x3ca12f8cUL, 0x373aa9caUL,
    0x000371a7UL, 0x738b5e8bUL, 0x3ca7d229UL, 0x34e59ff6UL, 0x0003a7dbUL,
    0xa72a4c6dUL, 0x3c859f48UL, 0x4c123422UL, 0x0003dea6UL, 0x259d9205UL,
    0x3ca8b846UL, 0x21f72e29UL, 0x0004160aUL, 0x60c2ac12UL, 0x3c4363edUL,
    0x6061892dUL, 0x00044e08UL, 0xdaa10379UL, 0x3c6ecce1UL, 0xb5c13cd0UL,
    0x000486a2UL, 0xbb7aafb0UL, 0x3c7690ceUL, 0xd5362a27UL, 0x0004bfdaUL,
    0x9b282a09UL, 0x3ca083ccUL, 0x769d2ca6UL, 0x0004f9b2UL, 0xc1aae707UL,
    0x3ca509b0UL, 0x569d4f81UL, 0x0005342bUL, 0x18fdd78eUL, 0x3c933505UL,
    0x36b527daUL, 0x00056f47UL, 0xe21c5409UL, 0x3c9063e1UL, 0xdd485429UL,
    0x0005ab07UL, 0x2b64c035UL, 0x3c9432e6UL, 0x15ad2148UL, 0x0005e76fUL,
    0x99f08c0aUL, 0x3ca01284UL, 0xb03a5584UL, 0x0006247eUL, 0x0073dc06UL,
    0x3c99f087UL, 0x82552224UL, 0x00066238UL, 0x0da05571UL, 0x3c998d4dUL,
    0x667f3bccUL, 0x0006a09eUL, 0x86ce4786UL, 0x3ca52bb9UL, 0x3c651a2eUL,
    0x0006dfb2UL, 0x206f0dabUL, 0x3ca32092UL, 0xe8ec5f73UL, 0x00071f75UL,
    0x8e17a7a6UL, 0x3ca06122UL, 0x564267c8UL, 0x00075febUL, 0x461e9f86UL,
    0x3ca244acUL, 0x73eb0186UL, 0x0007a114UL, 0xabd66c55UL, 0x3c65ebe1UL,
    0x36cf4e62UL, 0x0007e2f3UL, 0xbbff67d0UL, 0x3c96fe9fUL, 0x994cce12UL,
    0x00082589UL, 0x14c801dfUL, 0x3c951f14UL, 0x9b4492ecUL, 0x000868d9UL,
    0xc1f0eab4UL, 0x3c8db72fUL, 0x422aa0dbUL, 0x0008ace5UL, 0x59f35f44UL,
    0x3c7bf683UL, 0x99157736UL, 0x0008f1aeUL, 0x9c06283cUL, 0x3ca360baUL,
    0xb0cdc5e4UL, 0x00093737UL, 0x20f962aaUL, 0x3c95e8d1UL, 0x9fde4e4fUL,
    0x00097d82UL, 0x2b91ce27UL, 0x3c71affcUL, 0x82a3f090UL, 0x0009c491UL,
    0x589a2ebdUL, 0x3c9b6d34UL, 0x7b5de564UL, 0x000a0c66UL, 0x9ab89880UL,
    0x3c95277cUL, 0xb23e255cUL, 0x000a5503UL, 0x6e735ab3UL, 0x3c846984UL,
    0x5579fdbfUL, 0x000a9e6bUL, 0x92cb3387UL, 0x3c8c1a77UL, 0x995ad3adUL,
    0x000ae89fUL, 0xdc2d1d96UL, 0x3ca22466UL, 0xb84f15faUL, 0x000b33a2UL,
    0xb19505aeUL, 0x3ca1112eUL, 0xf2fb5e46UL, 0x000b7f76UL, 0x0a5fddcdUL,
    0x3c74ffd7UL, 0x904bc1d2UL, 0x000bcc1eUL, 0x30af0cb3UL, 0x3c736eaeUL,
    0xdd85529cUL, 0x000c199bUL, 0xd10959acUL, 0x3c84e08fUL, 0x2e57d14bUL,
    0x000c67f1UL, 0x6c921968UL, 0x3c676b2cUL, 0xdcef9069UL, 0x000cb720UL,
    0x36df99b3UL, 0x3c937009UL, 0x4a07897bUL, 0x000d072dUL, 0xa63d07a7UL,
    0x3c74a385UL, 0xdcfba487UL, 0x000d5818UL, 0xd5c192acUL, 0x3c8e5a50UL,
    0x03db3285UL, 0x000da9e6UL, 0x1c4a9792UL, 0x3c98bb73UL, 0x337b9b5eUL,
    0x000dfc97UL, 0x603a88d3UL, 0x3c74b604UL, 0xe78b3ff6UL, 0x000e502eUL,
    0x92094926UL, 0x3c916f27UL, 0xa2a490d9UL, 0x000ea4afUL, 0x41aa2008UL,
    0x3c8ec3bcUL, 0xee615a27UL, 0x000efa1bUL, 0x31d185eeUL, 0x3c8a64a9UL,
    0x5b6e4540UL, 0x000f5076UL, 0x4d91cd9dUL, 0x3c77893bUL, 0x819e90d8UL,
    0x000fa7c1UL
};

ATTRIBUTE_ALIGNED(16) juint _ALLONES[] =
{
    0xffffffffUL, 0xffffffffUL, 0xffffffffUL, 0xffffffffUL
};

ATTRIBUTE_ALIGNED(16) juint _ebias[] =
{
    0x00000000UL, 0x3ff00000UL, 0x00000000UL, 0x3ff00000UL
};

ATTRIBUTE_ALIGNED(4) juint _XMAX[] =
{
    0xffffffffUL, 0x7fefffffUL
};

ATTRIBUTE_ALIGNED(4) juint _XMIN[] =
{
    0x00000000UL, 0x00100000UL
};

ATTRIBUTE_ALIGNED(4) juint _INF[] =
{
    0x00000000UL, 0x7ff00000UL
};

ATTRIBUTE_ALIGNED(4) juint _ZERO[] =
{
    0x00000000UL, 0x00000000UL
};

ATTRIBUTE_ALIGNED(4) juint _ONE_val[] =
{
    0x00000000UL, 0x3ff00000UL
};


// Registers:
// input: xmm0
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, tmp - r11

// Code generated by Intel C compiler for LIBM library

void MacroAssembler::fast_exp(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register tmp) {
  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label L_2TAG_PACKET_4_0_2, L_2TAG_PACKET_5_0_2, L_2TAG_PACKET_6_0_2, L_2TAG_PACKET_7_0_2;
  Label L_2TAG_PACKET_8_0_2, L_2TAG_PACKET_9_0_2, L_2TAG_PACKET_10_0_2, L_2TAG_PACKET_11_0_2;
  Label L_2TAG_PACKET_12_0_2, B1_3, B1_5, start;

  assert_different_registers(tmp, eax, ecx, edx);
  address cv = (address)_cv;
  address Shifter = (address)_shifter;
  address mmask = (address)_mmask;
  address bias = (address)_bias;
  address Tbl_addr = (address)_Tbl_addr;
  address ALLONES = (address)_ALLONES;
  address ebias = (address)_ebias;
  address XMAX = (address)_XMAX;
  address XMIN = (address)_XMIN;
  address INF = (address)_INF;
  address ZERO = (address)_ZERO;
  address ONE_val = (address)_ONE_val;

  bind(start);
  subq(rsp, 24);
  movsd(Address(rsp, 8), xmm0);
  unpcklpd(xmm0, xmm0);
  movdqu(xmm1, ExternalAddress(cv));       // 0x652b82feUL, 0x40571547UL, 0x652b82feUL, 0x40571547UL
  movdqu(xmm6, ExternalAddress(Shifter));  // 0x00000000UL, 0x43380000UL, 0x00000000UL, 0x43380000UL
  movdqu(xmm2, ExternalAddress(16 + cv));    // 0xfefa0000UL, 0x3f862e42UL, 0xfefa0000UL, 0x3f862e42UL
  movdqu(xmm3, ExternalAddress(32 + cv));    // 0xbc9e3b3aUL, 0x3d1cf79aUL, 0xbc9e3b3aUL, 0x3d1cf79aUL
  pextrw(eax, xmm0, 3);
  andl(eax, 32767);
  movl(edx, 16527);
  subl(edx, eax);
  subl(eax, 15504);
  orl(edx, eax);
  cmpl(edx, INT_MIN);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_0_0_2);
  mulpd(xmm1, xmm0);
  addpd(xmm1, xmm6);
  movapd(xmm7, xmm1);
  subpd(xmm1, xmm6);
  mulpd(xmm2, xmm1);
  movdqu(xmm4, ExternalAddress(64 + cv));    // 0xe3289860UL, 0x3f56c15cUL, 0x555b9e25UL, 0x3fa55555UL
  mulpd(xmm3, xmm1);
  movdqu(xmm5, ExternalAddress(80 + cv));    // 0xc090cf0fUL, 0x3f811115UL, 0x55548ba1UL, 0x3fc55555UL
  subpd(xmm0, xmm2);
  movdl(eax, xmm7);
  movl(ecx, eax);
  andl(ecx, 63);
  shll(ecx, 4);
  sarl(eax, 6);
  movl(edx, eax);
  movdqu(xmm6, ExternalAddress(mmask));    // 0xffffffc0UL, 0x00000000UL, 0xffffffc0UL, 0x00000000UL
  pand(xmm7, xmm6);
  movdqu(xmm6, ExternalAddress(bias));     // 0x0000ffc0UL, 0x00000000UL, 0x0000ffc0UL, 0x00000000UL
  paddq(xmm7, xmm6);
  psllq(xmm7, 46);
  subpd(xmm0, xmm3);
  lea(tmp, ExternalAddress(Tbl_addr));
  movdqu(xmm2, Address(ecx, tmp));
  mulpd(xmm4, xmm0);
  movapd(xmm6, xmm0);
  movapd(xmm1, xmm0);
  mulpd(xmm6, xmm6);
  mulpd(xmm0, xmm6);
  addpd(xmm5, xmm4);
  mulsd(xmm0, xmm6);
  mulpd(xmm6, ExternalAddress(48 + cv));     // 0xfffffffeUL, 0x3fdfffffUL, 0xfffffffeUL, 0x3fdfffffUL
  addsd(xmm1, xmm2);
  unpckhpd(xmm2, xmm2);
  mulpd(xmm0, xmm5);
  addsd(xmm1, xmm0);
  por(xmm2, xmm7);
  unpckhpd(xmm0, xmm0);
  addsd(xmm0, xmm1);
  addsd(xmm0, xmm6);
  addl(edx, 894);
  cmpl(edx, 1916);
  jcc(Assembler::above, L_2TAG_PACKET_1_0_2);
  mulsd(xmm0, xmm2);
  addsd(xmm0, xmm2);
  jmp(B1_5);

  bind(L_2TAG_PACKET_1_0_2);
  xorpd(xmm3, xmm3);
  movdqu(xmm4, ExternalAddress(ALLONES));  // 0xffffffffUL, 0xffffffffUL, 0xffffffffUL, 0xffffffffUL
  movl(edx, -1022);
  subl(edx, eax);
  movdl(xmm5, edx);
  psllq(xmm4, xmm5);
  movl(ecx, eax);
  sarl(eax, 1);
  pinsrw(xmm3, eax, 3);
  movdqu(xmm6, ExternalAddress(ebias));    // 0x00000000UL, 0x3ff00000UL, 0x00000000UL, 0x3ff00000UL
  psllq(xmm3, 4);
  psubd(xmm2, xmm3);
  mulsd(xmm0, xmm2);
  cmpl(edx, 52);
  jcc(Assembler::greater, L_2TAG_PACKET_2_0_2);
  pand(xmm4, xmm2);
  paddd(xmm3, xmm6);
  subsd(xmm2, xmm4);
  addsd(xmm0, xmm2);
  cmpl(ecx, 1023);
  jcc(Assembler::greaterEqual, L_2TAG_PACKET_3_0_2);
  pextrw(ecx, xmm0, 3);
  andl(ecx, 32768);
  orl(edx, ecx);
  cmpl(edx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_4_0_2);
  movapd(xmm6, xmm0);
  addsd(xmm0, xmm4);
  mulsd(xmm0, xmm3);
  pextrw(ecx, xmm0, 3);
  andl(ecx, 32752);
  cmpl(ecx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_5_0_2);
  jmp(B1_5);

  bind(L_2TAG_PACKET_5_0_2);
  mulsd(xmm6, xmm3);
  mulsd(xmm4, xmm3);
  movdqu(xmm0, xmm6);
  pxor(xmm6, xmm4);
  psrad(xmm6, 31);
  pshufd(xmm6, xmm6, 85);
  psllq(xmm0, 1);
  psrlq(xmm0, 1);
  pxor(xmm0, xmm6);
  psrlq(xmm6, 63);
  paddq(xmm0, xmm6);
  paddq(xmm0, xmm4);
  movl(Address(rsp, 0), 15);
  jmp(L_2TAG_PACKET_6_0_2);

  bind(L_2TAG_PACKET_4_0_2);
  addsd(xmm0, xmm4);
  mulsd(xmm0, xmm3);
  jmp(B1_5);

  bind(L_2TAG_PACKET_3_0_2);
  addsd(xmm0, xmm4);
  mulsd(xmm0, xmm3);
  pextrw(ecx, xmm0, 3);
  andl(ecx, 32752);
  cmpl(ecx, 32752);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_7_0_2);
  jmp(B1_5);

  bind(L_2TAG_PACKET_2_0_2);
  paddd(xmm3, xmm6);
  addpd(xmm0, xmm2);
  mulsd(xmm0, xmm3);
  movl(Address(rsp, 0), 15);
  jmp(L_2TAG_PACKET_6_0_2);

  bind(L_2TAG_PACKET_8_0_2);
  cmpl(eax, 2146435072);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_9_0_2);
  movl(eax, Address(rsp, 12));
  cmpl(eax, INT_MIN);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_10_0_2);
  movsd(xmm0, ExternalAddress(XMAX));      // 0xffffffffUL, 0x7fefffffUL
  mulsd(xmm0, xmm0);

  bind(L_2TAG_PACKET_7_0_2);
  movl(Address(rsp, 0), 14);
  jmp(L_2TAG_PACKET_6_0_2);

  bind(L_2TAG_PACKET_10_0_2);
  movsd(xmm0, ExternalAddress(XMIN));      // 0x00000000UL, 0x00100000UL
  mulsd(xmm0, xmm0);
  movl(Address(rsp, 0), 15);
  jmp(L_2TAG_PACKET_6_0_2);

  bind(L_2TAG_PACKET_9_0_2);
  movl(edx, Address(rsp, 8));
  cmpl(eax, 2146435072);
  jcc(Assembler::above, L_2TAG_PACKET_11_0_2);
  cmpl(edx, 0);
  jcc(Assembler::notEqual, L_2TAG_PACKET_11_0_2);
  movl(eax, Address(rsp, 12));
  cmpl(eax, 2146435072);
  jcc(Assembler::notEqual, L_2TAG_PACKET_12_0_2);
  movsd(xmm0, ExternalAddress(INF));       // 0x00000000UL, 0x7ff00000UL
  jmp(B1_5);

  bind(L_2TAG_PACKET_12_0_2);
  movsd(xmm0, ExternalAddress(ZERO));      // 0x00000000UL, 0x00000000UL
  jmp(B1_5);

  bind(L_2TAG_PACKET_11_0_2);
  movsd(xmm0, Address(rsp, 8));
  addsd(xmm0, xmm0);
  jmp(B1_5);

  bind(L_2TAG_PACKET_0_0_2);
  movl(eax, Address(rsp, 12));
  andl(eax, 2147483647);
  cmpl(eax, 1083179008);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_8_0_2);
  movsd(Address(rsp, 8), xmm0);
  addsd(xmm0, ExternalAddress(ONE_val));   // 0x00000000UL, 0x3ff00000UL
  jmp(B1_5);

  bind(L_2TAG_PACKET_6_0_2);
  movq(Address(rsp, 16), xmm0);

  bind(B1_3);
  movq(xmm0, Address(rsp, 16));

  bind(B1_5);
  addq(rsp, 24);
}
#else
// The 32 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(16) juint _static_const_table[] =
{
    0x00000000UL, 0xfff00000UL, 0x00000000UL, 0xfff00000UL, 0xffffffc0UL,
    0x00000000UL, 0xffffffc0UL, 0x00000000UL, 0x0000ffc0UL, 0x00000000UL,
    0x0000ffc0UL, 0x00000000UL, 0x00000000UL, 0x43380000UL, 0x00000000UL,
    0x43380000UL, 0x652b82feUL, 0x40571547UL, 0x652b82feUL, 0x40571547UL,
    0xfefa0000UL, 0x3f862e42UL, 0xfefa0000UL, 0x3f862e42UL, 0xbc9e3b3aUL,
    0x3d1cf79aUL, 0xbc9e3b3aUL, 0x3d1cf79aUL, 0xfffffffeUL, 0x3fdfffffUL,
    0xfffffffeUL, 0x3fdfffffUL, 0xe3289860UL, 0x3f56c15cUL, 0x555b9e25UL,
    0x3fa55555UL, 0xc090cf0fUL, 0x3f811115UL, 0x55548ba1UL, 0x3fc55555UL,
    0x00000000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL, 0x0e03754dUL,
    0x3cad7bbfUL, 0x3e778060UL, 0x00002c9aUL, 0x3567f613UL, 0x3c8cd252UL,
    0xd3158574UL, 0x000059b0UL, 0x61e6c861UL, 0x3c60f74eUL, 0x18759bc8UL,
    0x00008745UL, 0x5d837b6cUL, 0x3c979aa6UL, 0x6cf9890fUL, 0x0000b558UL,
    0x702f9cd1UL, 0x3c3ebe3dUL, 0x32d3d1a2UL, 0x0000e3ecUL, 0x1e63bcd8UL,
    0x3ca3516eUL, 0xd0125b50UL, 0x00011301UL, 0x26f0387bUL, 0x3ca4c554UL,
    0xaea92ddfUL, 0x0001429aUL, 0x62523fb6UL, 0x3ca95153UL, 0x3c7d517aUL,
    0x000172b8UL, 0x3f1353bfUL, 0x3c8b898cUL, 0xeb6fcb75UL, 0x0001a35bUL,
    0x3e3a2f5fUL, 0x3c9aecf7UL, 0x3168b9aaUL, 0x0001d487UL, 0x44a6c38dUL,
    0x3c8a6f41UL, 0x88628cd6UL, 0x0002063bUL, 0xe3a8a894UL, 0x3c968efdUL,
    0x6e756238UL, 0x0002387aUL, 0x981fe7f2UL, 0x3c80472bUL, 0x65e27cddUL,
    0x00026b45UL, 0x6d09ab31UL, 0x3c82f7e1UL, 0xf51fdee1UL, 0x00029e9dUL,
    0x720c0ab3UL, 0x3c8b3782UL, 0xa6e4030bUL, 0x0002d285UL, 0x4db0abb6UL,
    0x3c834d75UL, 0x0a31b715UL, 0x000306feUL, 0x5dd3f84aUL, 0x3c8fdd39UL,
    0xb26416ffUL, 0x00033c08UL, 0xcc187d29UL, 0x3ca12f8cUL, 0x373aa9caUL,
    0x000371a7UL, 0x738b5e8bUL, 0x3ca7d229UL, 0x34e59ff6UL, 0x0003a7dbUL,
    0xa72a4c6dUL, 0x3c859f48UL, 0x4c123422UL, 0x0003dea6UL, 0x259d9205UL,
    0x3ca8b846UL, 0x21f72e29UL, 0x0004160aUL, 0x60c2ac12UL, 0x3c4363edUL,
    0x6061892dUL, 0x00044e08UL, 0xdaa10379UL, 0x3c6ecce1UL, 0xb5c13cd0UL,
    0x000486a2UL, 0xbb7aafb0UL, 0x3c7690ceUL, 0xd5362a27UL, 0x0004bfdaUL,
    0x9b282a09UL, 0x3ca083ccUL, 0x769d2ca6UL, 0x0004f9b2UL, 0xc1aae707UL,
    0x3ca509b0UL, 0x569d4f81UL, 0x0005342bUL, 0x18fdd78eUL, 0x3c933505UL,
    0x36b527daUL, 0x00056f47UL, 0xe21c5409UL, 0x3c9063e1UL, 0xdd485429UL,
    0x0005ab07UL, 0x2b64c035UL, 0x3c9432e6UL, 0x15ad2148UL, 0x0005e76fUL,
    0x99f08c0aUL, 0x3ca01284UL, 0xb03a5584UL, 0x0006247eUL, 0x0073dc06UL,
    0x3c99f087UL, 0x82552224UL, 0x00066238UL, 0x0da05571UL, 0x3c998d4dUL,
    0x667f3bccUL, 0x0006a09eUL, 0x86ce4786UL, 0x3ca52bb9UL, 0x3c651a2eUL,
    0x0006dfb2UL, 0x206f0dabUL, 0x3ca32092UL, 0xe8ec5f73UL, 0x00071f75UL,
    0x8e17a7a6UL, 0x3ca06122UL, 0x564267c8UL, 0x00075febUL, 0x461e9f86UL,
    0x3ca244acUL, 0x73eb0186UL, 0x0007a114UL, 0xabd66c55UL, 0x3c65ebe1UL,
    0x36cf4e62UL, 0x0007e2f3UL, 0xbbff67d0UL, 0x3c96fe9fUL, 0x994cce12UL,
    0x00082589UL, 0x14c801dfUL, 0x3c951f14UL, 0x9b4492ecUL, 0x000868d9UL,
    0xc1f0eab4UL, 0x3c8db72fUL, 0x422aa0dbUL, 0x0008ace5UL, 0x59f35f44UL,
    0x3c7bf683UL, 0x99157736UL, 0x0008f1aeUL, 0x9c06283cUL, 0x3ca360baUL,
    0xb0cdc5e4UL, 0x00093737UL, 0x20f962aaUL, 0x3c95e8d1UL, 0x9fde4e4fUL,
    0x00097d82UL, 0x2b91ce27UL, 0x3c71affcUL, 0x82a3f090UL, 0x0009c491UL,
    0x589a2ebdUL, 0x3c9b6d34UL, 0x7b5de564UL, 0x000a0c66UL, 0x9ab89880UL,
    0x3c95277cUL, 0xb23e255cUL, 0x000a5503UL, 0x6e735ab3UL, 0x3c846984UL,
    0x5579fdbfUL, 0x000a9e6bUL, 0x92cb3387UL, 0x3c8c1a77UL, 0x995ad3adUL,
    0x000ae89fUL, 0xdc2d1d96UL, 0x3ca22466UL, 0xb84f15faUL, 0x000b33a2UL,
    0xb19505aeUL, 0x3ca1112eUL, 0xf2fb5e46UL, 0x000b7f76UL, 0x0a5fddcdUL,
    0x3c74ffd7UL, 0x904bc1d2UL, 0x000bcc1eUL, 0x30af0cb3UL, 0x3c736eaeUL,
    0xdd85529cUL, 0x000c199bUL, 0xd10959acUL, 0x3c84e08fUL, 0x2e57d14bUL,
    0x000c67f1UL, 0x6c921968UL, 0x3c676b2cUL, 0xdcef9069UL, 0x000cb720UL,
    0x36df99b3UL, 0x3c937009UL, 0x4a07897bUL, 0x000d072dUL, 0xa63d07a7UL,
    0x3c74a385UL, 0xdcfba487UL, 0x000d5818UL, 0xd5c192acUL, 0x3c8e5a50UL,
    0x03db3285UL, 0x000da9e6UL, 0x1c4a9792UL, 0x3c98bb73UL, 0x337b9b5eUL,
    0x000dfc97UL, 0x603a88d3UL, 0x3c74b604UL, 0xe78b3ff6UL, 0x000e502eUL,
    0x92094926UL, 0x3c916f27UL, 0xa2a490d9UL, 0x000ea4afUL, 0x41aa2008UL,
    0x3c8ec3bcUL, 0xee615a27UL, 0x000efa1bUL, 0x31d185eeUL, 0x3c8a64a9UL,
    0x5b6e4540UL, 0x000f5076UL, 0x4d91cd9dUL, 0x3c77893bUL, 0x819e90d8UL,
    0x000fa7c1UL, 0x00000000UL, 0x3ff00000UL, 0x00000000UL, 0x7ff00000UL,
    0x00000000UL, 0x00000000UL, 0xffffffffUL, 0x7fefffffUL, 0x00000000UL,
    0x00100000UL
};

//registers,
// input: (rbp + 8)
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, rbx (tmp)

// Code generated by Intel C compiler for LIBM library

void MacroAssembler::fast_exp(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register tmp) {
  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label L_2TAG_PACKET_4_0_2, L_2TAG_PACKET_5_0_2, L_2TAG_PACKET_6_0_2, L_2TAG_PACKET_7_0_2;
  Label L_2TAG_PACKET_8_0_2, L_2TAG_PACKET_9_0_2, L_2TAG_PACKET_10_0_2, L_2TAG_PACKET_11_0_2;
  Label L_2TAG_PACKET_12_0_2, start;

  assert_different_registers(tmp, eax, ecx, edx);
  address static_const_table = (address)_static_const_table;

  bind(start);
  subl(rsp, 120);
  movl(Address(rsp, 64), tmp);
  lea(tmp, ExternalAddress(static_const_table));
  movsd(xmm0, Address(rsp, 128));
  unpcklpd(xmm0, xmm0);
  movdqu(xmm1, Address(tmp, 64));          // 0x652b82feUL, 0x40571547UL, 0x652b82feUL, 0x40571547UL
  movdqu(xmm6, Address(tmp, 48));          // 0x00000000UL, 0x43380000UL, 0x00000000UL, 0x43380000UL
  movdqu(xmm2, Address(tmp, 80));          // 0xfefa0000UL, 0x3f862e42UL, 0xfefa0000UL, 0x3f862e42UL
  movdqu(xmm3, Address(tmp, 96));          // 0xbc9e3b3aUL, 0x3d1cf79aUL, 0xbc9e3b3aUL, 0x3d1cf79aUL
  pextrw(eax, xmm0, 3);
  andl(eax, 32767);
  movl(edx, 16527);
  subl(edx, eax);
  subl(eax, 15504);
  orl(edx, eax);
  cmpl(edx, INT_MIN);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_0_0_2);
  mulpd(xmm1, xmm0);
  addpd(xmm1, xmm6);
  movapd(xmm7, xmm1);
  subpd(xmm1, xmm6);
  mulpd(xmm2, xmm1);
  movdqu(xmm4, Address(tmp, 128));         // 0xe3289860UL, 0x3f56c15cUL, 0x555b9e25UL, 0x3fa55555UL
  mulpd(xmm3, xmm1);
  movdqu(xmm5, Address(tmp, 144));         // 0xc090cf0fUL, 0x3f811115UL, 0x55548ba1UL, 0x3fc55555UL
  subpd(xmm0, xmm2);
  movdl(eax, xmm7);
  movl(ecx, eax);
  andl(ecx, 63);
  shll(ecx, 4);
  sarl(eax, 6);
  movl(edx, eax);
  movdqu(xmm6, Address(tmp, 16));          // 0xffffffc0UL, 0x00000000UL, 0xffffffc0UL, 0x00000000UL
  pand(xmm7, xmm6);
  movdqu(xmm6, Address(tmp, 32));          // 0x0000ffc0UL, 0x00000000UL, 0x0000ffc0UL, 0x00000000UL
  paddq(xmm7, xmm6);
  psllq(xmm7, 46);
  subpd(xmm0, xmm3);
  movdqu(xmm2, Address(tmp, ecx, Address::times_1, 160));
  mulpd(xmm4, xmm0);
  movapd(xmm6, xmm0);
  movapd(xmm1, xmm0);
  mulpd(xmm6, xmm6);
  mulpd(xmm0, xmm6);
  addpd(xmm5, xmm4);
  mulsd(xmm0, xmm6);
  mulpd(xmm6, Address(tmp, 112));          // 0xfffffffeUL, 0x3fdfffffUL, 0xfffffffeUL, 0x3fdfffffUL
  addsd(xmm1, xmm2);
  unpckhpd(xmm2, xmm2);
  mulpd(xmm0, xmm5);
  addsd(xmm1, xmm0);
  por(xmm2, xmm7);
  unpckhpd(xmm0, xmm0);
  addsd(xmm0, xmm1);
  addsd(xmm0, xmm6);
  addl(edx, 894);
  cmpl(edx, 1916);
  jcc(Assembler::above, L_2TAG_PACKET_1_0_2);
  mulsd(xmm0, xmm2);
  addsd(xmm0, xmm2);
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_1_0_2);
  fnstcw(Address(rsp, 24));
  movzwl(edx, Address(rsp, 24));
  orl(edx, 768);
  movw(Address(rsp, 28), edx);
  fldcw(Address(rsp, 28));
  movl(edx, eax);
  sarl(eax, 1);
  subl(edx, eax);
  movdqu(xmm6, Address(tmp, 0));           // 0x00000000UL, 0xfff00000UL, 0x00000000UL, 0xfff00000UL
  pandn(xmm6, xmm2);
  addl(eax, 1023);
  movdl(xmm3, eax);
  psllq(xmm3, 52);
  por(xmm6, xmm3);
  addl(edx, 1023);
  movdl(xmm4, edx);
  psllq(xmm4, 52);
  movsd(Address(rsp, 8), xmm0);
  fld_d(Address(rsp, 8));
  movsd(Address(rsp, 16), xmm6);
  fld_d(Address(rsp, 16));
  fmula(1);
  faddp(1);
  movsd(Address(rsp, 8), xmm4);
  fld_d(Address(rsp, 8));
  fmulp(1);
  fstp_d(Address(rsp, 8));
  movsd(xmm0, Address(rsp, 8));
  fldcw(Address(rsp, 24));
  pextrw(ecx, xmm0, 3);
  andl(ecx, 32752);
  cmpl(ecx, 32752);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_3_0_2);
  cmpl(ecx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_4_0_2);
  jmp(L_2TAG_PACKET_2_0_2);
  cmpl(ecx, INT_MIN);
  jcc(Assembler::below, L_2TAG_PACKET_3_0_2);
  cmpl(ecx, -1064950997);
  jcc(Assembler::below, L_2TAG_PACKET_2_0_2);
  jcc(Assembler::above, L_2TAG_PACKET_4_0_2);
  movl(edx, Address(rsp, 128));
  cmpl(edx, -17155601);
  jcc(Assembler::below, L_2TAG_PACKET_2_0_2);
  jmp(L_2TAG_PACKET_4_0_2);

  bind(L_2TAG_PACKET_3_0_2);
  movl(edx, 14);
  jmp(L_2TAG_PACKET_5_0_2);

  bind(L_2TAG_PACKET_4_0_2);
  movl(edx, 15);

  bind(L_2TAG_PACKET_5_0_2);
  movsd(Address(rsp, 0), xmm0);
  movsd(xmm0, Address(rsp, 128));
  fld_d(Address(rsp, 0));
  jmp(L_2TAG_PACKET_6_0_2);

  bind(L_2TAG_PACKET_7_0_2);
  cmpl(eax, 2146435072);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_8_0_2);
  movl(eax, Address(rsp, 132));
  cmpl(eax, INT_MIN);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_9_0_2);
  movsd(xmm0, Address(tmp, 1208));         // 0xffffffffUL, 0x7fefffffUL
  mulsd(xmm0, xmm0);
  movl(edx, 14);
  jmp(L_2TAG_PACKET_5_0_2);

  bind(L_2TAG_PACKET_9_0_2);
  movsd(xmm0, Address(tmp, 1216));
  mulsd(xmm0, xmm0);
  movl(edx, 15);
  jmp(L_2TAG_PACKET_5_0_2);

  bind(L_2TAG_PACKET_8_0_2);
  movl(edx, Address(rsp, 128));
  cmpl(eax, 2146435072);
  jcc(Assembler::above, L_2TAG_PACKET_10_0_2);
  cmpl(edx, 0);
  jcc(Assembler::notEqual, L_2TAG_PACKET_10_0_2);
  movl(eax, Address(rsp, 132));
  cmpl(eax, 2146435072);
  jcc(Assembler::notEqual, L_2TAG_PACKET_11_0_2);
  movsd(xmm0, Address(tmp, 1192));         // 0x00000000UL, 0x7ff00000UL
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_11_0_2);
  movsd(xmm0, Address(tmp, 1200));         // 0x00000000UL, 0x00000000UL
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_10_0_2);
  movsd(xmm0, Address(rsp, 128));
  addsd(xmm0, xmm0);
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_0_0_2);
  movl(eax, Address(rsp, 132));
  andl(eax, 2147483647);
  cmpl(eax, 1083179008);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_7_0_2);
  movsd(xmm0, Address(rsp, 128));
  addsd(xmm0, Address(tmp, 1184));         // 0x00000000UL, 0x3ff00000UL
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_2_0_2);
  movsd(Address(rsp, 48), xmm0);
  fld_d(Address(rsp, 48));

  bind(L_2TAG_PACKET_6_0_2);
  movl(tmp, Address(rsp, 64));
}
#endif
