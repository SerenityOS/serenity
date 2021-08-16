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
//                     ALGORITHM DESCRIPTION - LOG10()
//                     ---------------------
//
//    Let x=2^k * mx, mx in [1,2)
//
//    Get B~1/mx based on the output of rcpss instruction (B0)
//    B = int((B0*LH*2^7+0.5))/2^7
//    LH is a short approximation for log10(e)
//
//    Reduced argument: r=B*mx-LH (computed accurately in high and low parts)
//
//    Result:  k*log10(2) - log(B) + p(r)
//             p(r) is a degree 7 polynomial
//             -log(B) read from data table (high, low parts)
//             Result is formed from high and low parts
//
// Special cases:
//  log10(0) = -INF with divide-by-zero exception raised
//  log10(1) = +0
//  log10(x) = NaN with invalid exception raised if x < -0, including -INF
//  log10(+INF) = +INF
//
/******************************************************************************/

#ifdef _LP64
// The 64 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(16) juint _HIGHSIGMASK_log10[] =
{
    0xf8000000UL, 0xffffffffUL, 0x00000000UL, 0xffffe000UL
};

ATTRIBUTE_ALIGNED(16) juint _LOG10_E[] =
{
    0x00000000UL, 0x3fdbc000UL, 0xbf2e4108UL, 0x3f5a7a6cUL
};

ATTRIBUTE_ALIGNED(16) juint _L_tbl_log10[] =
{
    0x509f7800UL, 0x3fd34413UL, 0x1f12b358UL, 0x3d1fef31UL, 0x80333400UL,
    0x3fd32418UL, 0xc671d9d0UL, 0xbcf542bfUL, 0x51195000UL, 0x3fd30442UL,
    0x78a4b0c3UL, 0x3d18216aUL, 0x6fc79400UL, 0x3fd2e490UL, 0x80fa389dUL,
    0xbc902869UL, 0x89d04000UL, 0x3fd2c502UL, 0x75c2f564UL, 0x3d040754UL,
    0x4ddd1c00UL, 0x3fd2a598UL, 0xd219b2c3UL, 0xbcfa1d84UL, 0x6baa7c00UL,
    0x3fd28651UL, 0xfd9abec1UL, 0x3d1be6d3UL, 0x94028800UL, 0x3fd2672dUL,
    0xe289a455UL, 0xbd1ede5eUL, 0x78b86400UL, 0x3fd2482cUL, 0x6734d179UL,
    0x3d1fe79bUL, 0xcca3c800UL, 0x3fd2294dUL, 0x981a40b8UL, 0xbced34eaUL,
    0x439c5000UL, 0x3fd20a91UL, 0xcc392737UL, 0xbd1a9cc3UL, 0x92752c00UL,
    0x3fd1ebf6UL, 0x03c9afe7UL, 0x3d1e98f8UL, 0x6ef8dc00UL, 0x3fd1cd7dUL,
    0x71dae7f4UL, 0x3d08a86cUL, 0x8fe4dc00UL, 0x3fd1af25UL, 0xee9185a1UL,
    0xbcff3412UL, 0xace59400UL, 0x3fd190eeUL, 0xc2cab353UL, 0x3cf17ed9UL,
    0x7e925000UL, 0x3fd172d8UL, 0x6952c1b2UL, 0x3cf1521cUL, 0xbe694400UL,
    0x3fd154e2UL, 0xcacb79caUL, 0xbd0bdc78UL, 0x26cbac00UL, 0x3fd1370dUL,
    0xf71f4de1UL, 0xbd01f8beUL, 0x72fa0800UL, 0x3fd11957UL, 0x55bf910bUL,
    0x3c946e2bUL, 0x5f106000UL, 0x3fd0fbc1UL, 0x39e639c1UL, 0x3d14a84bUL,
    0xa802a800UL, 0x3fd0de4aUL, 0xd3f31d5dUL, 0xbd178385UL, 0x0b992000UL,
    0x3fd0c0f3UL, 0x3843106fUL, 0xbd1f602fUL, 0x486ce800UL, 0x3fd0a3baUL,
    0x8819497cUL, 0x3cef987aUL, 0x1de49400UL, 0x3fd086a0UL, 0x1caa0467UL,
    0x3d0faec7UL, 0x4c30cc00UL, 0x3fd069a4UL, 0xa4424372UL, 0xbd1618fcUL,
    0x94490000UL, 0x3fd04cc6UL, 0x946517d2UL, 0xbd18384bUL, 0xb7e84000UL,
    0x3fd03006UL, 0xe0109c37UL, 0xbd19a6acUL, 0x798a0c00UL, 0x3fd01364UL,
    0x5121e864UL, 0xbd164cf7UL, 0x38ce8000UL, 0x3fcfedbfUL, 0x46214d1aUL,
    0xbcbbc402UL, 0xc8e62000UL, 0x3fcfb4efUL, 0xdab93203UL, 0x3d1e0176UL,
    0x2cb02800UL, 0x3fcf7c5aUL, 0x2a2ea8e4UL, 0xbcfec86aUL, 0xeeeaa000UL,
    0x3fcf43fdUL, 0xc18e49a4UL, 0x3cf110a8UL, 0x9bb6e800UL, 0x3fcf0bdaUL,
    0x923cc9c0UL, 0xbd15ce99UL, 0xc093f000UL, 0x3fced3efUL, 0x4d4b51e9UL,
    0x3d1a04c7UL, 0xec58f800UL, 0x3fce9c3cUL, 0x163cad59UL, 0x3cac8260UL,
    0x9a907000UL, 0x3fce2d7dUL, 0x3fa93646UL, 0x3ce4a1c0UL, 0x37311000UL,
    0x3fcdbf99UL, 0x32abd1fdUL, 0x3d07ea9dUL, 0x6744b800UL, 0x3fcd528cUL,
    0x4dcbdfd4UL, 0xbd1b08e2UL, 0xe36de800UL, 0x3fcce653UL, 0x0b7b7f7fUL,
    0xbd1b8f03UL, 0x77506800UL, 0x3fcc7aecUL, 0xa821c9fbUL, 0x3d13c163UL,
    0x00ff8800UL, 0x3fcc1053UL, 0x536bca76UL, 0xbd074ee5UL, 0x70719800UL,
    0x3fcba684UL, 0xd7da9b6bUL, 0xbd1fbf16UL, 0xc6f8d800UL, 0x3fcb3d7dUL,
    0xe2220bb3UL, 0x3d1a295dUL, 0x16c15800UL, 0x3fcad53cUL, 0xe724911eUL,
    0xbcf55822UL, 0x82533800UL, 0x3fca6dbcUL, 0x6d982371UL, 0x3cac567cUL,
    0x3c19e800UL, 0x3fca06fcUL, 0x84d17d80UL, 0x3d1da204UL, 0x85ef8000UL,
    0x3fc9a0f8UL, 0x54466a6aUL, 0xbd002204UL, 0xb0ac2000UL, 0x3fc93baeUL,
    0xd601fd65UL, 0x3d18840cUL, 0x1bb9b000UL, 0x3fc8d71cUL, 0x7bf58766UL,
    0xbd14f897UL, 0x34aae800UL, 0x3fc8733eUL, 0x3af6ac24UL, 0xbd0f5c45UL,
    0x76d68000UL, 0x3fc81012UL, 0x4303e1a1UL, 0xbd1f9a80UL, 0x6af57800UL,
    0x3fc7ad96UL, 0x43fbcb46UL, 0x3cf4c33eUL, 0xa6c51000UL, 0x3fc74bc7UL,
    0x70f0eac5UL, 0xbd192e3bUL, 0xccab9800UL, 0x3fc6eaa3UL, 0xc0093dfeUL,
    0xbd0faf15UL, 0x8b60b800UL, 0x3fc68a28UL, 0xde78d5fdUL, 0xbc9ea4eeUL,
    0x9d987000UL, 0x3fc62a53UL, 0x962bea6eUL, 0xbd194084UL, 0xc9b0e800UL,
    0x3fc5cb22UL, 0x888dd999UL, 0x3d1fe201UL, 0xe1634800UL, 0x3fc56c93UL,
    0x16ada7adUL, 0x3d1b1188UL, 0xc176c000UL, 0x3fc50ea4UL, 0x4159b5b5UL,
    0xbcf09c08UL, 0x51766000UL, 0x3fc4b153UL, 0x84393d23UL, 0xbcf6a89cUL,
    0x83695000UL, 0x3fc4549dUL, 0x9f0b8bbbUL, 0x3d1c4b8cUL, 0x538d5800UL,
    0x3fc3f881UL, 0xf49df747UL, 0x3cf89b99UL, 0xc8138000UL, 0x3fc39cfcUL,
    0xd503b834UL, 0xbd13b99fUL, 0xf0df0800UL, 0x3fc3420dUL, 0xf011b386UL,
    0xbd05d8beUL, 0xe7466800UL, 0x3fc2e7b2UL, 0xf39c7bc2UL, 0xbd1bb94eUL,
    0xcdd62800UL, 0x3fc28de9UL, 0x05e6d69bUL, 0xbd10ed05UL, 0xd015d800UL,
    0x3fc234b0UL, 0xe29b6c9dUL, 0xbd1ff967UL, 0x224ea800UL, 0x3fc1dc06UL,
    0x727711fcUL, 0xbcffb30dUL, 0x01540000UL, 0x3fc183e8UL, 0x39786c5aUL,
    0x3cc23f57UL, 0xb24d9800UL, 0x3fc12c54UL, 0xc905a342UL, 0x3d003a1dUL,
    0x82835800UL, 0x3fc0d54aUL, 0x9b9920c0UL, 0x3d03b25aUL, 0xc72ac000UL,
    0x3fc07ec7UL, 0x46f26a24UL, 0x3cf0fa41UL, 0xdd35d800UL, 0x3fc028caUL,
    0x41d9d6dcUL, 0x3d034a65UL, 0x52474000UL, 0x3fbfa6a4UL, 0x44f66449UL,
    0x3d19cad3UL, 0x2da3d000UL, 0x3fbefcb8UL, 0x67832999UL, 0x3d18400fUL,
    0x32a10000UL, 0x3fbe53ceUL, 0x9c0e3b1aUL, 0xbcff62fdUL, 0x556b7000UL,
    0x3fbdabe3UL, 0x02976913UL, 0xbcf8243bUL, 0x97e88000UL, 0x3fbd04f4UL,
    0xec793797UL, 0x3d1c0578UL, 0x09647000UL, 0x3fbc5effUL, 0x05fc0565UL,
    0xbd1d799eUL, 0xc6426000UL, 0x3fbbb9ffUL, 0x4625f5edUL, 0x3d1f5723UL,
    0xf7afd000UL, 0x3fbb15f3UL, 0xdd5aae61UL, 0xbd1a7e1eUL, 0xd358b000UL,
    0x3fba72d8UL, 0x3314e4d3UL, 0x3d17bc91UL, 0x9b1f5000UL, 0x3fb9d0abUL,
    0x9a4d514bUL, 0x3cf18c9bUL, 0x9cd4e000UL, 0x3fb92f69UL, 0x7e4496abUL,
    0x3cf1f96dUL, 0x31f4f000UL, 0x3fb88f10UL, 0xf56479e7UL, 0x3d165818UL,
    0xbf628000UL, 0x3fb7ef9cUL, 0x26bf486dUL, 0xbd1113a6UL, 0xb526b000UL,
    0x3fb7510cUL, 0x1a1c3384UL, 0x3ca9898dUL, 0x8e31e000UL, 0x3fb6b35dUL,
    0xb3875361UL, 0xbd0661acUL, 0xd01de000UL, 0x3fb6168cUL, 0x2a7cacfaUL,
    0xbd1bdf10UL, 0x0af23000UL, 0x3fb57a98UL, 0xff868816UL, 0x3cf046d0UL,
    0xd8ea0000UL, 0x3fb4df7cUL, 0x1515fbe7UL, 0xbd1fd529UL, 0xde3b2000UL,
    0x3fb44538UL, 0x6e59a132UL, 0x3d1faeeeUL, 0xc8df9000UL, 0x3fb3abc9UL,
    0xf1322361UL, 0xbd198807UL, 0x505f1000UL, 0x3fb3132dUL, 0x0888e6abUL,
    0x3d1e5380UL, 0x359bd000UL, 0x3fb27b61UL, 0xdfbcbb22UL, 0xbcfe2724UL,
    0x429ee000UL, 0x3fb1e463UL, 0x6eb4c58cUL, 0xbcfe4dd6UL, 0x4a673000UL,
    0x3fb14e31UL, 0x4ce1ac9bUL, 0x3d1ba691UL, 0x28b96000UL, 0x3fb0b8c9UL,
    0x8c7813b8UL, 0xbd0b3872UL, 0xc1f08000UL, 0x3fb02428UL, 0xc2bc8c2cUL,
    0x3cb5ea6bUL, 0x05a1a000UL, 0x3faf209cUL, 0x72e8f18eUL, 0xbce8df84UL,
    0xc0b5e000UL, 0x3fadfa6dUL, 0x9fdef436UL, 0x3d087364UL, 0xaf416000UL,
    0x3facd5c2UL, 0x1068c3a9UL, 0x3d0827e7UL, 0xdb356000UL, 0x3fabb296UL,
    0x120a34d3UL, 0x3d101a9fUL, 0x5dfea000UL, 0x3faa90e6UL, 0xdaded264UL,
    0xbd14c392UL, 0x6034c000UL, 0x3fa970adUL, 0x1c9d06a9UL, 0xbd1b705eUL,
    0x194c6000UL, 0x3fa851e8UL, 0x83996ad9UL, 0xbd0117bcUL, 0xcf4ac000UL,
    0x3fa73492UL, 0xb1a94a62UL, 0xbca5ea42UL, 0xd67b4000UL, 0x3fa618a9UL,
    0x75aed8caUL, 0xbd07119bUL, 0x9126c000UL, 0x3fa4fe29UL, 0x5291d533UL,
    0x3d12658fUL, 0x6f4d4000UL, 0x3fa3e50eUL, 0xcd2c5cd9UL, 0x3d1d5c70UL,
    0xee608000UL, 0x3fa2cd54UL, 0xd1008489UL, 0x3d1a4802UL, 0x9900e000UL,
    0x3fa1b6f9UL, 0x54fb5598UL, 0xbd16593fUL, 0x06bb6000UL, 0x3fa0a1f9UL,
    0x64ef57b4UL, 0xbd17636bUL, 0xb7940000UL, 0x3f9f1c9fUL, 0xee6a4737UL,
    0x3cb5d479UL, 0x91aa0000UL, 0x3f9cf7f5UL, 0x3a16373cUL, 0x3d087114UL,
    0x156b8000UL, 0x3f9ad5edUL, 0x836c554aUL, 0x3c6900b0UL, 0xd4764000UL,
    0x3f98b67fUL, 0xed12f17bUL, 0xbcffc974UL, 0x77dec000UL, 0x3f9699a7UL,
    0x232ce7eaUL, 0x3d1e35bbUL, 0xbfbf4000UL, 0x3f947f5dUL, 0xd84ffa6eUL,
    0x3d0e0a49UL, 0x82c7c000UL, 0x3f92679cUL, 0x8d170e90UL, 0xbd14d9f2UL,
    0xadd20000UL, 0x3f90525dUL, 0x86d9f88eUL, 0x3cdeb986UL, 0x86f10000UL,
    0x3f8c7f36UL, 0xb9e0a517UL, 0x3ce29faaUL, 0xb75c8000UL, 0x3f885e9eUL,
    0x542568cbUL, 0xbd1f7bdbUL, 0x46b30000UL, 0x3f8442e8UL, 0xb954e7d9UL,
    0x3d1e5287UL, 0xb7e60000UL, 0x3f802c07UL, 0x22da0b17UL, 0xbd19fb27UL,
    0x6c8b0000UL, 0x3f7833e3UL, 0x821271efUL, 0xbd190f96UL, 0x29910000UL,
    0x3f701936UL, 0xbc3491a5UL, 0xbd1bcf45UL, 0x354a0000UL, 0x3f600fe3UL,
    0xc0ff520aUL, 0xbd19d71cUL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL
};

ATTRIBUTE_ALIGNED(16) juint _log2_log10[] =
{
    0x509f7800UL, 0x3f934413UL, 0x1f12b358UL, 0x3cdfef31UL
};

ATTRIBUTE_ALIGNED(16) juint _coeff_log10[] =
{
    0xc1a5f12eUL, 0x40358874UL, 0x64d4ef0dUL, 0xc0089309UL, 0x385593b1UL,
    0xc025c917UL, 0xdc963467UL, 0x3ffc6a02UL, 0x7f9d3aa1UL, 0x4016ab9fUL,
    0xdc77b115UL, 0xbff27af2UL
};

// Registers:
// input: xmm0
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, tmp - r11

// Code generated by Intel C compiler for LIBM library

void MacroAssembler::fast_log10(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register r11) {
  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label L_2TAG_PACKET_4_0_2, L_2TAG_PACKET_5_0_2, L_2TAG_PACKET_6_0_2, L_2TAG_PACKET_7_0_2;
  Label L_2TAG_PACKET_8_0_2, L_2TAG_PACKET_9_0_2, B1_2, B1_3, B1_5, start;

  assert_different_registers(r11, eax, ecx, edx);

  address HIGHSIGMASK = (address)_HIGHSIGMASK_log10;
  address LOG10_E = (address)_LOG10_E;
  address L_tbl = (address)_L_tbl_log10;
  address log2 = (address)_log2_log10;
  address coeff = (address)_coeff_log10;

  bind(start);
  subq(rsp, 24);
  movsd(Address(rsp, 0), xmm0);

  bind(B1_2);
  xorpd(xmm2, xmm2);
  movl(eax, 16368);
  pinsrw(xmm2, eax, 3);
  movl(ecx, 1054736384);
  movdl(xmm7, ecx);
  xorpd(xmm3, xmm3);
  movl(edx, 30704);
  pinsrw(xmm3, edx, 3);
  movdqu(xmm1, xmm0);
  movl(edx, 32768);
  movdl(xmm4, edx);
  movdqu(xmm5, ExternalAddress(HIGHSIGMASK));    //0xf8000000UL, 0xffffffffUL, 0x00000000UL, 0xffffe000UL
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  movl(ecx, 16352);
  psrlq(xmm0, 27);
  movdqu(xmm2, ExternalAddress(LOG10_E));    //0x00000000UL, 0x3fdbc000UL, 0xbf2e4108UL, 0x3f5a7a6cUL
  psrld(xmm0, 2);
  rcpps(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 78);
  psrlq(xmm1, 12);
  subl(eax, 16);
  cmpl(eax, 32736);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_0_0_2);

  bind(L_2TAG_PACKET_1_0_2);
  mulss(xmm0, xmm7);
  por(xmm1, xmm3);
  lea(r11, ExternalAddress(L_tbl));
  andpd(xmm5, xmm1);
  paddd(xmm0, xmm4);
  subsd(xmm1, xmm5);
  movdl(edx, xmm0);
  psllq(xmm0, 29);
  andpd(xmm0, xmm6);
  andl(eax, 32752);
  subl(eax, ecx);
  cvtsi2sdl(xmm7, eax);
  mulpd(xmm5, xmm0);
  mulsd(xmm1, xmm0);
  movq(xmm6, ExternalAddress(log2));    //0x509f7800UL, 0x3f934413UL, 0x1f12b358UL, 0x3cdfef31UL
  movdqu(xmm3, ExternalAddress(coeff));    //0xc1a5f12eUL, 0x40358874UL, 0x64d4ef0dUL, 0xc0089309UL
  subsd(xmm5, xmm2);
  andl(edx, 16711680);
  shrl(edx, 12);
  movdqu(xmm0, Address(r11, rdx, Address::times_1, -1504));
  movdqu(xmm4, ExternalAddress(16 + coeff));    //0x385593b1UL, 0xc025c917UL, 0xdc963467UL, 0x3ffc6a02UL
  addsd(xmm1, xmm5);
  movdqu(xmm2, ExternalAddress(32 + coeff));    //0x7f9d3aa1UL, 0x4016ab9fUL, 0xdc77b115UL, 0xbff27af2UL
  mulsd(xmm6, xmm7);
  pshufd(xmm5, xmm1, 68);
  mulsd(xmm7, ExternalAddress(8 + log2));    //0x1f12b358UL, 0x3cdfef31UL
  mulsd(xmm3, xmm1);
  addsd(xmm0, xmm6);
  mulpd(xmm4, xmm5);
  movq(xmm6, ExternalAddress(8 + LOG10_E));    //0xbf2e4108UL, 0x3f5a7a6cUL
  mulpd(xmm5, xmm5);
  addpd(xmm4, xmm2);
  mulpd(xmm3, xmm5);
  pshufd(xmm2, xmm0, 228);
  addsd(xmm0, xmm1);
  mulsd(xmm4, xmm1);
  subsd(xmm2, xmm0);
  mulsd(xmm6, xmm1);
  addsd(xmm1, xmm2);
  pshufd(xmm2, xmm0, 238);
  mulsd(xmm5, xmm5);
  addsd(xmm7, xmm2);
  addsd(xmm1, xmm6);
  addpd(xmm4, xmm3);
  addsd(xmm1, xmm7);
  mulpd(xmm4, xmm5);
  addsd(xmm1, xmm4);
  pshufd(xmm5, xmm4, 238);
  addsd(xmm1, xmm5);
  addsd(xmm0, xmm1);
  jmp(B1_5);

  bind(L_2TAG_PACKET_0_0_2);
  movq(xmm0, Address(rsp, 0));
  movq(xmm1, Address(rsp, 0));
  addl(eax, 16);
  cmpl(eax, 32768);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_2_0_2);
  cmpl(eax, 16);
  jcc(Assembler::below, L_2TAG_PACKET_3_0_2);

  bind(L_2TAG_PACKET_4_0_2);
  addsd(xmm0, xmm0);
  jmp(B1_5);

  bind(L_2TAG_PACKET_5_0_2);
  jcc(Assembler::above, L_2TAG_PACKET_4_0_2);
  cmpl(edx, 0);
  jcc(Assembler::above, L_2TAG_PACKET_4_0_2);
  jmp(L_2TAG_PACKET_6_0_2);

  bind(L_2TAG_PACKET_3_0_2);
  xorpd(xmm1, xmm1);
  addsd(xmm1, xmm0);
  movdl(edx, xmm1);
  psrlq(xmm1, 32);
  movdl(ecx, xmm1);
  orl(edx, ecx);
  cmpl(edx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_7_0_2);
  xorpd(xmm1, xmm1);
  movl(eax, 18416);
  pinsrw(xmm1, eax, 3);
  mulsd(xmm0, xmm1);
  xorpd(xmm2, xmm2);
  movl(eax, 16368);
  pinsrw(xmm2, eax, 3);
  movdqu(xmm1, xmm0);
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  movl(ecx, 18416);
  psrlq(xmm0, 27);
  movdqu(xmm2, ExternalAddress(LOG10_E));    //0x00000000UL, 0x3fdbc000UL, 0xbf2e4108UL, 0x3f5a7a6cUL
  psrld(xmm0, 2);
  rcpps(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 78);
  psrlq(xmm1, 12);
  jmp(L_2TAG_PACKET_1_0_2);

  bind(L_2TAG_PACKET_2_0_2);
  movdl(edx, xmm1);
  psrlq(xmm1, 32);
  movdl(ecx, xmm1);
  addl(ecx, ecx);
  cmpl(ecx, -2097152);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_5_0_2);
  orl(edx, ecx);
  cmpl(edx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_7_0_2);

  bind(L_2TAG_PACKET_6_0_2);
  xorpd(xmm1, xmm1);
  xorpd(xmm0, xmm0);
  movl(eax, 32752);
  pinsrw(xmm1, eax, 3);
  mulsd(xmm0, xmm1);
  movl(Address(rsp, 16), 9);
  jmp(L_2TAG_PACKET_8_0_2);

  bind(L_2TAG_PACKET_7_0_2);
  xorpd(xmm1, xmm1);
  xorpd(xmm0, xmm0);
  movl(eax, 49136);
  pinsrw(xmm0, eax, 3);
  divsd(xmm0, xmm1);
  movl(Address(rsp, 16), 8);

  bind(L_2TAG_PACKET_8_0_2);
  movq(Address(rsp, 8), xmm0);

  bind(B1_3);
  movq(xmm0, Address(rsp, 8));

  bind(L_2TAG_PACKET_9_0_2);

  bind(B1_5);
  addq(rsp, 24);

}
#else
// The 32 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(16) juint _static_const_table_log10[] =
{
    0x509f7800UL, 0x3fd34413UL, 0x1f12b358UL, 0x3d1fef31UL, 0x80333400UL,
    0x3fd32418UL, 0xc671d9d0UL, 0xbcf542bfUL, 0x51195000UL, 0x3fd30442UL,
    0x78a4b0c3UL, 0x3d18216aUL, 0x6fc79400UL, 0x3fd2e490UL, 0x80fa389dUL,
    0xbc902869UL, 0x89d04000UL, 0x3fd2c502UL, 0x75c2f564UL, 0x3d040754UL,
    0x4ddd1c00UL, 0x3fd2a598UL, 0xd219b2c3UL, 0xbcfa1d84UL, 0x6baa7c00UL,
    0x3fd28651UL, 0xfd9abec1UL, 0x3d1be6d3UL, 0x94028800UL, 0x3fd2672dUL,
    0xe289a455UL, 0xbd1ede5eUL, 0x78b86400UL, 0x3fd2482cUL, 0x6734d179UL,
    0x3d1fe79bUL, 0xcca3c800UL, 0x3fd2294dUL, 0x981a40b8UL, 0xbced34eaUL,
    0x439c5000UL, 0x3fd20a91UL, 0xcc392737UL, 0xbd1a9cc3UL, 0x92752c00UL,
    0x3fd1ebf6UL, 0x03c9afe7UL, 0x3d1e98f8UL, 0x6ef8dc00UL, 0x3fd1cd7dUL,
    0x71dae7f4UL, 0x3d08a86cUL, 0x8fe4dc00UL, 0x3fd1af25UL, 0xee9185a1UL,
    0xbcff3412UL, 0xace59400UL, 0x3fd190eeUL, 0xc2cab353UL, 0x3cf17ed9UL,
    0x7e925000UL, 0x3fd172d8UL, 0x6952c1b2UL, 0x3cf1521cUL, 0xbe694400UL,
    0x3fd154e2UL, 0xcacb79caUL, 0xbd0bdc78UL, 0x26cbac00UL, 0x3fd1370dUL,
    0xf71f4de1UL, 0xbd01f8beUL, 0x72fa0800UL, 0x3fd11957UL, 0x55bf910bUL,
    0x3c946e2bUL, 0x5f106000UL, 0x3fd0fbc1UL, 0x39e639c1UL, 0x3d14a84bUL,
    0xa802a800UL, 0x3fd0de4aUL, 0xd3f31d5dUL, 0xbd178385UL, 0x0b992000UL,
    0x3fd0c0f3UL, 0x3843106fUL, 0xbd1f602fUL, 0x486ce800UL, 0x3fd0a3baUL,
    0x8819497cUL, 0x3cef987aUL, 0x1de49400UL, 0x3fd086a0UL, 0x1caa0467UL,
    0x3d0faec7UL, 0x4c30cc00UL, 0x3fd069a4UL, 0xa4424372UL, 0xbd1618fcUL,
    0x94490000UL, 0x3fd04cc6UL, 0x946517d2UL, 0xbd18384bUL, 0xb7e84000UL,
    0x3fd03006UL, 0xe0109c37UL, 0xbd19a6acUL, 0x798a0c00UL, 0x3fd01364UL,
    0x5121e864UL, 0xbd164cf7UL, 0x38ce8000UL, 0x3fcfedbfUL, 0x46214d1aUL,
    0xbcbbc402UL, 0xc8e62000UL, 0x3fcfb4efUL, 0xdab93203UL, 0x3d1e0176UL,
    0x2cb02800UL, 0x3fcf7c5aUL, 0x2a2ea8e4UL, 0xbcfec86aUL, 0xeeeaa000UL,
    0x3fcf43fdUL, 0xc18e49a4UL, 0x3cf110a8UL, 0x9bb6e800UL, 0x3fcf0bdaUL,
    0x923cc9c0UL, 0xbd15ce99UL, 0xc093f000UL, 0x3fced3efUL, 0x4d4b51e9UL,
    0x3d1a04c7UL, 0xec58f800UL, 0x3fce9c3cUL, 0x163cad59UL, 0x3cac8260UL,
    0x9a907000UL, 0x3fce2d7dUL, 0x3fa93646UL, 0x3ce4a1c0UL, 0x37311000UL,
    0x3fcdbf99UL, 0x32abd1fdUL, 0x3d07ea9dUL, 0x6744b800UL, 0x3fcd528cUL,
    0x4dcbdfd4UL, 0xbd1b08e2UL, 0xe36de800UL, 0x3fcce653UL, 0x0b7b7f7fUL,
    0xbd1b8f03UL, 0x77506800UL, 0x3fcc7aecUL, 0xa821c9fbUL, 0x3d13c163UL,
    0x00ff8800UL, 0x3fcc1053UL, 0x536bca76UL, 0xbd074ee5UL, 0x70719800UL,
    0x3fcba684UL, 0xd7da9b6bUL, 0xbd1fbf16UL, 0xc6f8d800UL, 0x3fcb3d7dUL,
    0xe2220bb3UL, 0x3d1a295dUL, 0x16c15800UL, 0x3fcad53cUL, 0xe724911eUL,
    0xbcf55822UL, 0x82533800UL, 0x3fca6dbcUL, 0x6d982371UL, 0x3cac567cUL,
    0x3c19e800UL, 0x3fca06fcUL, 0x84d17d80UL, 0x3d1da204UL, 0x85ef8000UL,
    0x3fc9a0f8UL, 0x54466a6aUL, 0xbd002204UL, 0xb0ac2000UL, 0x3fc93baeUL,
    0xd601fd65UL, 0x3d18840cUL, 0x1bb9b000UL, 0x3fc8d71cUL, 0x7bf58766UL,
    0xbd14f897UL, 0x34aae800UL, 0x3fc8733eUL, 0x3af6ac24UL, 0xbd0f5c45UL,
    0x76d68000UL, 0x3fc81012UL, 0x4303e1a1UL, 0xbd1f9a80UL, 0x6af57800UL,
    0x3fc7ad96UL, 0x43fbcb46UL, 0x3cf4c33eUL, 0xa6c51000UL, 0x3fc74bc7UL,
    0x70f0eac5UL, 0xbd192e3bUL, 0xccab9800UL, 0x3fc6eaa3UL, 0xc0093dfeUL,
    0xbd0faf15UL, 0x8b60b800UL, 0x3fc68a28UL, 0xde78d5fdUL, 0xbc9ea4eeUL,
    0x9d987000UL, 0x3fc62a53UL, 0x962bea6eUL, 0xbd194084UL, 0xc9b0e800UL,
    0x3fc5cb22UL, 0x888dd999UL, 0x3d1fe201UL, 0xe1634800UL, 0x3fc56c93UL,
    0x16ada7adUL, 0x3d1b1188UL, 0xc176c000UL, 0x3fc50ea4UL, 0x4159b5b5UL,
    0xbcf09c08UL, 0x51766000UL, 0x3fc4b153UL, 0x84393d23UL, 0xbcf6a89cUL,
    0x83695000UL, 0x3fc4549dUL, 0x9f0b8bbbUL, 0x3d1c4b8cUL, 0x538d5800UL,
    0x3fc3f881UL, 0xf49df747UL, 0x3cf89b99UL, 0xc8138000UL, 0x3fc39cfcUL,
    0xd503b834UL, 0xbd13b99fUL, 0xf0df0800UL, 0x3fc3420dUL, 0xf011b386UL,
    0xbd05d8beUL, 0xe7466800UL, 0x3fc2e7b2UL, 0xf39c7bc2UL, 0xbd1bb94eUL,
    0xcdd62800UL, 0x3fc28de9UL, 0x05e6d69bUL, 0xbd10ed05UL, 0xd015d800UL,
    0x3fc234b0UL, 0xe29b6c9dUL, 0xbd1ff967UL, 0x224ea800UL, 0x3fc1dc06UL,
    0x727711fcUL, 0xbcffb30dUL, 0x01540000UL, 0x3fc183e8UL, 0x39786c5aUL,
    0x3cc23f57UL, 0xb24d9800UL, 0x3fc12c54UL, 0xc905a342UL, 0x3d003a1dUL,
    0x82835800UL, 0x3fc0d54aUL, 0x9b9920c0UL, 0x3d03b25aUL, 0xc72ac000UL,
    0x3fc07ec7UL, 0x46f26a24UL, 0x3cf0fa41UL, 0xdd35d800UL, 0x3fc028caUL,
    0x41d9d6dcUL, 0x3d034a65UL, 0x52474000UL, 0x3fbfa6a4UL, 0x44f66449UL,
    0x3d19cad3UL, 0x2da3d000UL, 0x3fbefcb8UL, 0x67832999UL, 0x3d18400fUL,
    0x32a10000UL, 0x3fbe53ceUL, 0x9c0e3b1aUL, 0xbcff62fdUL, 0x556b7000UL,
    0x3fbdabe3UL, 0x02976913UL, 0xbcf8243bUL, 0x97e88000UL, 0x3fbd04f4UL,
    0xec793797UL, 0x3d1c0578UL, 0x09647000UL, 0x3fbc5effUL, 0x05fc0565UL,
    0xbd1d799eUL, 0xc6426000UL, 0x3fbbb9ffUL, 0x4625f5edUL, 0x3d1f5723UL,
    0xf7afd000UL, 0x3fbb15f3UL, 0xdd5aae61UL, 0xbd1a7e1eUL, 0xd358b000UL,
    0x3fba72d8UL, 0x3314e4d3UL, 0x3d17bc91UL, 0x9b1f5000UL, 0x3fb9d0abUL,
    0x9a4d514bUL, 0x3cf18c9bUL, 0x9cd4e000UL, 0x3fb92f69UL, 0x7e4496abUL,
    0x3cf1f96dUL, 0x31f4f000UL, 0x3fb88f10UL, 0xf56479e7UL, 0x3d165818UL,
    0xbf628000UL, 0x3fb7ef9cUL, 0x26bf486dUL, 0xbd1113a6UL, 0xb526b000UL,
    0x3fb7510cUL, 0x1a1c3384UL, 0x3ca9898dUL, 0x8e31e000UL, 0x3fb6b35dUL,
    0xb3875361UL, 0xbd0661acUL, 0xd01de000UL, 0x3fb6168cUL, 0x2a7cacfaUL,
    0xbd1bdf10UL, 0x0af23000UL, 0x3fb57a98UL, 0xff868816UL, 0x3cf046d0UL,
    0xd8ea0000UL, 0x3fb4df7cUL, 0x1515fbe7UL, 0xbd1fd529UL, 0xde3b2000UL,
    0x3fb44538UL, 0x6e59a132UL, 0x3d1faeeeUL, 0xc8df9000UL, 0x3fb3abc9UL,
    0xf1322361UL, 0xbd198807UL, 0x505f1000UL, 0x3fb3132dUL, 0x0888e6abUL,
    0x3d1e5380UL, 0x359bd000UL, 0x3fb27b61UL, 0xdfbcbb22UL, 0xbcfe2724UL,
    0x429ee000UL, 0x3fb1e463UL, 0x6eb4c58cUL, 0xbcfe4dd6UL, 0x4a673000UL,
    0x3fb14e31UL, 0x4ce1ac9bUL, 0x3d1ba691UL, 0x28b96000UL, 0x3fb0b8c9UL,
    0x8c7813b8UL, 0xbd0b3872UL, 0xc1f08000UL, 0x3fb02428UL, 0xc2bc8c2cUL,
    0x3cb5ea6bUL, 0x05a1a000UL, 0x3faf209cUL, 0x72e8f18eUL, 0xbce8df84UL,
    0xc0b5e000UL, 0x3fadfa6dUL, 0x9fdef436UL, 0x3d087364UL, 0xaf416000UL,
    0x3facd5c2UL, 0x1068c3a9UL, 0x3d0827e7UL, 0xdb356000UL, 0x3fabb296UL,
    0x120a34d3UL, 0x3d101a9fUL, 0x5dfea000UL, 0x3faa90e6UL, 0xdaded264UL,
    0xbd14c392UL, 0x6034c000UL, 0x3fa970adUL, 0x1c9d06a9UL, 0xbd1b705eUL,
    0x194c6000UL, 0x3fa851e8UL, 0x83996ad9UL, 0xbd0117bcUL, 0xcf4ac000UL,
    0x3fa73492UL, 0xb1a94a62UL, 0xbca5ea42UL, 0xd67b4000UL, 0x3fa618a9UL,
    0x75aed8caUL, 0xbd07119bUL, 0x9126c000UL, 0x3fa4fe29UL, 0x5291d533UL,
    0x3d12658fUL, 0x6f4d4000UL, 0x3fa3e50eUL, 0xcd2c5cd9UL, 0x3d1d5c70UL,
    0xee608000UL, 0x3fa2cd54UL, 0xd1008489UL, 0x3d1a4802UL, 0x9900e000UL,
    0x3fa1b6f9UL, 0x54fb5598UL, 0xbd16593fUL, 0x06bb6000UL, 0x3fa0a1f9UL,
    0x64ef57b4UL, 0xbd17636bUL, 0xb7940000UL, 0x3f9f1c9fUL, 0xee6a4737UL,
    0x3cb5d479UL, 0x91aa0000UL, 0x3f9cf7f5UL, 0x3a16373cUL, 0x3d087114UL,
    0x156b8000UL, 0x3f9ad5edUL, 0x836c554aUL, 0x3c6900b0UL, 0xd4764000UL,
    0x3f98b67fUL, 0xed12f17bUL, 0xbcffc974UL, 0x77dec000UL, 0x3f9699a7UL,
    0x232ce7eaUL, 0x3d1e35bbUL, 0xbfbf4000UL, 0x3f947f5dUL, 0xd84ffa6eUL,
    0x3d0e0a49UL, 0x82c7c000UL, 0x3f92679cUL, 0x8d170e90UL, 0xbd14d9f2UL,
    0xadd20000UL, 0x3f90525dUL, 0x86d9f88eUL, 0x3cdeb986UL, 0x86f10000UL,
    0x3f8c7f36UL, 0xb9e0a517UL, 0x3ce29faaUL, 0xb75c8000UL, 0x3f885e9eUL,
    0x542568cbUL, 0xbd1f7bdbUL, 0x46b30000UL, 0x3f8442e8UL, 0xb954e7d9UL,
    0x3d1e5287UL, 0xb7e60000UL, 0x3f802c07UL, 0x22da0b17UL, 0xbd19fb27UL,
    0x6c8b0000UL, 0x3f7833e3UL, 0x821271efUL, 0xbd190f96UL, 0x29910000UL,
    0x3f701936UL, 0xbc3491a5UL, 0xbd1bcf45UL, 0x354a0000UL, 0x3f600fe3UL,
    0xc0ff520aUL, 0xbd19d71cUL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0x509f7800UL, 0x3f934413UL, 0x1f12b358UL, 0x3cdfef31UL,
    0xc1a5f12eUL, 0x40358874UL, 0x64d4ef0dUL, 0xc0089309UL, 0x385593b1UL,
    0xc025c917UL, 0xdc963467UL, 0x3ffc6a02UL, 0x7f9d3aa1UL, 0x4016ab9fUL,
    0xdc77b115UL, 0xbff27af2UL, 0xf8000000UL, 0xffffffffUL, 0x00000000UL,
    0xffffe000UL, 0x00000000UL, 0x3fdbc000UL, 0xbf2e4108UL, 0x3f5a7a6cUL
};
//registers,
// input: xmm0
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, rbx (tmp)

void MacroAssembler::fast_log10(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register tmp) {

  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label L_2TAG_PACKET_4_0_2, L_2TAG_PACKET_5_0_2, L_2TAG_PACKET_6_0_2, L_2TAG_PACKET_7_0_2;
  Label L_2TAG_PACKET_8_0_2, L_2TAG_PACKET_9_0_2, L_2TAG_PACKET_10_0_2, start;

  assert_different_registers(tmp, eax, ecx, edx);

  address static_const_table_log10 = (address)_static_const_table_log10;

  bind(start);
  subl(rsp, 104);
  movl(Address(rsp, 40), tmp);
  lea(tmp, ExternalAddress(static_const_table_log10));
  xorpd(xmm2, xmm2);
  movl(eax, 16368);
  pinsrw(xmm2, eax, 3);
  movl(ecx, 1054736384);
  movdl(xmm7, ecx);
  xorpd(xmm3, xmm3);
  movl(edx, 30704);
  pinsrw(xmm3, edx, 3);
  movsd(xmm0, Address(rsp, 112));
  movdqu(xmm1, xmm0);
  movl(edx, 32768);
  movdl(xmm4, edx);
  movdqu(xmm5, Address(tmp, 2128));    //0x3ffc6a02UL, 0x7f9d3aa1UL, 0x4016ab9fUL, 0xdc77b115UL
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  movl(ecx, 16352);
  psllq(xmm0, 5);
  movsd(xmm2, Address(tmp, 2144));    //0xbff27af2UL, 0xf8000000UL, 0xffffffffUL, 0x00000000UL
  psrlq(xmm0, 34);
  rcpss(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 78);
  psrlq(xmm1, 12);
  subl(eax, 16);
  cmpl(eax, 32736);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_0_0_2);

  bind(L_2TAG_PACKET_1_0_2);
  mulss(xmm0, xmm7);
  por(xmm1, xmm3);
  andpd(xmm5, xmm1);
  paddd(xmm0, xmm4);
  subsd(xmm1, xmm5);
  movdl(edx, xmm0);
  psllq(xmm0, 29);
  andpd(xmm0, xmm6);
  andl(eax, 32752);
  subl(eax, ecx);
  cvtsi2sdl(xmm7, eax);
  mulpd(xmm5, xmm0);
  mulsd(xmm1, xmm0);
  movsd(xmm6, Address(tmp, 2064));    //0xbd19d71cUL, 0x00000000UL, 0x00000000UL, 0x00000000UL
  movdqu(xmm3, Address(tmp, 2080));    //0x00000000UL, 0x509f7800UL, 0x3f934413UL, 0x1f12b358UL
  subsd(xmm5, xmm2);
  andl(edx, 16711680);
  shrl(edx, 12);
  movdqu(xmm0, Address(tmp, edx, Address::times_1, -1504));
  movdqu(xmm4, Address(tmp, 2096));    //0x3cdfef31UL, 0xc1a5f12eUL, 0x40358874UL, 0x64d4ef0dUL
  addsd(xmm1, xmm5);
  movdqu(xmm2, Address(tmp, 2112));    //0xc0089309UL, 0x385593b1UL, 0xc025c917UL, 0xdc963467UL
  mulsd(xmm6, xmm7);
  pshufd(xmm5, xmm1, 68);
  mulsd(xmm7, Address(tmp, 2072));    //0x00000000UL, 0x00000000UL, 0x00000000UL, 0x509f7800UL
  mulsd(xmm3, xmm1);
  addsd(xmm0, xmm6);
  mulpd(xmm4, xmm5);
  movsd(xmm6, Address(tmp, 2152));    //0xffffffffUL, 0x00000000UL, 0xffffe000UL, 0x00000000UL
  mulpd(xmm5, xmm5);
  addpd(xmm4, xmm2);
  mulpd(xmm3, xmm5);
  pshufd(xmm2, xmm0, 228);
  addsd(xmm0, xmm1);
  mulsd(xmm4, xmm1);
  subsd(xmm2, xmm0);
  mulsd(xmm6, xmm1);
  addsd(xmm1, xmm2);
  pshufd(xmm2, xmm0, 238);
  mulsd(xmm5, xmm5);
  addsd(xmm7, xmm2);
  addsd(xmm1, xmm6);
  addpd(xmm4, xmm3);
  addsd(xmm1, xmm7);
  mulpd(xmm4, xmm5);
  addsd(xmm1, xmm4);
  pshufd(xmm5, xmm4, 238);
  addsd(xmm1, xmm5);
  addsd(xmm0, xmm1);
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_0_0_2);
  movsd(xmm0, Address(rsp, 112));    //0xbcfa1d84UL, 0x6baa7c00UL, 0x3fd28651UL, 0xfd9abec1UL
  movdqu(xmm1, xmm0);
  addl(eax, 16);
  cmpl(eax, 32768);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_3_0_2);
  cmpl(eax, 16);
  jcc(Assembler::below, L_2TAG_PACKET_4_0_2);

  bind(L_2TAG_PACKET_5_0_2);
  addsd(xmm0, xmm0);
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_6_0_2);
  jcc(Assembler::above, L_2TAG_PACKET_5_0_2);
  cmpl(edx, 0);
  jcc(Assembler::above, L_2TAG_PACKET_5_0_2);
  jmp(L_2TAG_PACKET_7_0_2);

  bind(L_2TAG_PACKET_3_0_2);
  movdl(edx, xmm1);
  psrlq(xmm1, 32);
  movdl(ecx, xmm1);
  addl(ecx, ecx);
  cmpl(ecx, -2097152);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_6_0_2);
  orl(edx, ecx);
  cmpl(edx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_8_0_2);

  bind(L_2TAG_PACKET_7_0_2);
  xorpd(xmm1, xmm1);
  xorpd(xmm0, xmm0);
  movl(eax, 32752);
  pinsrw(xmm1, eax, 3);
  movl(edx, 9);
  mulsd(xmm0, xmm1);

  bind(L_2TAG_PACKET_9_0_2);
  movsd(Address(rsp, 0), xmm0);
  movsd(xmm0, Address(rsp, 112));    //0xbcfa1d84UL, 0x6baa7c00UL, 0x3fd28651UL, 0xfd9abec1UL
  fld_d(Address(rsp, 0));
  jmp(L_2TAG_PACKET_10_0_2);

  bind(L_2TAG_PACKET_8_0_2);
  xorpd(xmm1, xmm1);
  xorpd(xmm0, xmm0);
  movl(eax, 49136);
  pinsrw(xmm0, eax, 3);
  divsd(xmm0, xmm1);
  movl(edx, 8);
  jmp(L_2TAG_PACKET_9_0_2);

  bind(L_2TAG_PACKET_4_0_2);
  movdl(edx, xmm1);
  psrlq(xmm1, 32);
  movdl(ecx, xmm1);
  orl(edx, ecx);
  cmpl(edx, 0);
  jcc(Assembler::equal, L_2TAG_PACKET_8_0_2);
  xorpd(xmm1, xmm1);
  movl(eax, 18416);
  pinsrw(xmm1, eax, 3);
  mulsd(xmm0, xmm1);
  xorpd(xmm2, xmm2);
  movl(eax, 16368);
  pinsrw(xmm2, eax, 3);
  movdqu(xmm1, xmm0);
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  movl(ecx, 18416);
  psllq(xmm0, 5);
  movsd(xmm2, Address(tmp, 2144));    //0xbff27af2UL, 0xf8000000UL, 0xffffffffUL, 0x00000000UL
  psrlq(xmm0, 34);
  rcpss(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 78);
  psrlq(xmm1, 12);
  jmp(L_2TAG_PACKET_1_0_2);

  bind(L_2TAG_PACKET_2_0_2);
  movsd(Address(rsp, 24), xmm0);
  fld_d(Address(rsp, 24));

  bind(L_2TAG_PACKET_10_0_2);
  movl(tmp, Address(rsp, 40));

}
#endif
