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
#include "utilities/globalDefinitions.hpp"

/******************************************************************************/
//                     ALGORITHM DESCRIPTION - LOG()
//                     ---------------------
//
//    x=2^k * mx, mx in [1,2)
//
//    Get B~1/mx based on the output of rcpss instruction (B0)
//    B = int((B0*2^7+0.5))/2^7
//
//    Reduced argument: r=B*mx-1.0 (computed accurately in high and low parts)
//
//    Result:  k*log(2) - log(B) + p(r) if |x-1| >= small value (2^-6)  and
//             p(r) is a degree 7 polynomial
//             -log(B) read from data table (high, low parts)
//             Result is formed from high and low parts
//
// Special cases:
//  log(NaN) = quiet NaN, and raise invalid exception
//  log(+INF) = that INF
//  log(0) = -INF with divide-by-zero exception raised
//  log(1) = +0
//  log(x) = NaN with invalid exception raised if x < -0, including -INF
//
/******************************************************************************/

#ifdef _LP64
// The 64 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(16) juint _L_tbl[] =
{
    0xfefa3800UL, 0x3fe62e42UL, 0x93c76730UL, 0x3d2ef357UL, 0xaa241800UL,
    0x3fe5ee82UL, 0x0cda46beUL, 0x3d220238UL, 0x5c364800UL, 0x3fe5af40UL,
    0xac10c9fbUL, 0x3d2dfa63UL, 0x26bb8c00UL, 0x3fe5707aUL, 0xff3303ddUL,
    0x3d09980bUL, 0x26867800UL, 0x3fe5322eUL, 0x5d257531UL, 0x3d05ccc4UL,
    0x835a5000UL, 0x3fe4f45aUL, 0x6d93b8fbUL, 0xbd2e6c51UL, 0x6f970c00UL,
    0x3fe4b6fdUL, 0xed4c541cUL, 0x3cef7115UL, 0x27e8a400UL, 0x3fe47a15UL,
    0xf94d60aaUL, 0xbd22cb6aUL, 0xf2f92400UL, 0x3fe43d9fUL, 0x481051f7UL,
    0xbcfd984fUL, 0x2125cc00UL, 0x3fe4019cUL, 0x30f0c74cUL, 0xbd26ce79UL,
    0x0c36c000UL, 0x3fe3c608UL, 0x7cfe13c2UL, 0xbd02b736UL, 0x17197800UL,
    0x3fe38ae2UL, 0xbb5569a4UL, 0xbd218b7aUL, 0xad9d8c00UL, 0x3fe35028UL,
    0x9527e6acUL, 0x3d10b83fUL, 0x44340800UL, 0x3fe315daUL, 0xc5a0ed9cUL,
    0xbd274e93UL, 0x57b0e000UL, 0x3fe2dbf5UL, 0x07b9dc11UL, 0xbd17a6e5UL,
    0x6d0ec000UL, 0x3fe2a278UL, 0xe797882dUL, 0x3d206d2bUL, 0x1134dc00UL,
    0x3fe26962UL, 0x05226250UL, 0xbd0b61f1UL, 0xd8bebc00UL, 0x3fe230b0UL,
    0x6e48667bUL, 0x3d12fc06UL, 0x5fc61800UL, 0x3fe1f863UL, 0xc9fe81d3UL,
    0xbd2a7242UL, 0x49ae6000UL, 0x3fe1c078UL, 0xed70e667UL, 0x3cccacdeUL,
    0x40f23c00UL, 0x3fe188eeUL, 0xf8ab4650UL, 0x3d14cc4eUL, 0xf6f29800UL,
    0x3fe151c3UL, 0xa293ae49UL, 0xbd2edd97UL, 0x23c75c00UL, 0x3fe11af8UL,
    0xbb9ddcb2UL, 0xbd258647UL, 0x8611cc00UL, 0x3fe0e489UL, 0x07801742UL,
    0x3d1c2998UL, 0xe2d05400UL, 0x3fe0ae76UL, 0x887e7e27UL, 0x3d1f486bUL,
    0x0533c400UL, 0x3fe078bfUL, 0x41edf5fdUL, 0x3d268122UL, 0xbe760400UL,
    0x3fe04360UL, 0xe79539e0UL, 0xbd04c45fUL, 0xe5b20800UL, 0x3fe00e5aUL,
    0xb1727b1cUL, 0xbd053ba3UL, 0xaf7a4800UL, 0x3fdfb358UL, 0x3c164935UL,
    0x3d0085faUL, 0xee031800UL, 0x3fdf4aa7UL, 0x6f014a8bUL, 0x3d12cde5UL,
    0x56b41000UL, 0x3fdee2a1UL, 0x5a470251UL, 0x3d2f27f4UL, 0xc3ddb000UL,
    0x3fde7b42UL, 0x5372bd08UL, 0xbd246550UL, 0x1a272800UL, 0x3fde148aUL,
    0x07322938UL, 0xbd1326b2UL, 0x484c9800UL, 0x3fddae75UL, 0x60dc616aUL,
    0xbd1ea42dUL, 0x46def800UL, 0x3fdd4902UL, 0xe9a767a8UL, 0x3d235bafUL,
    0x18064800UL, 0x3fdce42fUL, 0x3ec7a6b0UL, 0xbd0797c3UL, 0xc7455800UL,
    0x3fdc7ff9UL, 0xc15249aeUL, 0xbd29b6ddUL, 0x693fa000UL, 0x3fdc1c60UL,
    0x7fe8e180UL, 0x3d2cec80UL, 0x1b80e000UL, 0x3fdbb961UL, 0xf40a666dUL,
    0x3d27d85bUL, 0x04462800UL, 0x3fdb56faUL, 0x2d841995UL, 0x3d109525UL,
    0x5248d000UL, 0x3fdaf529UL, 0x52774458UL, 0xbd217cc5UL, 0x3c8ad800UL,
    0x3fda93edUL, 0xbea77a5dUL, 0x3d1e36f2UL, 0x0224f800UL, 0x3fda3344UL,
    0x7f9d79f5UL, 0x3d23c645UL, 0xea15f000UL, 0x3fd9d32bUL, 0x10d0c0b0UL,
    0xbd26279eUL, 0x43135800UL, 0x3fd973a3UL, 0xa502d9f0UL, 0xbd152313UL,
    0x635bf800UL, 0x3fd914a8UL, 0x2ee6307dUL, 0xbd1766b5UL, 0xa88b3000UL,
    0x3fd8b639UL, 0xe5e70470UL, 0xbd205ae1UL, 0x776dc800UL, 0x3fd85855UL,
    0x3333778aUL, 0x3d2fd56fUL, 0x3bd81800UL, 0x3fd7fafaUL, 0xc812566aUL,
    0xbd272090UL, 0x687cf800UL, 0x3fd79e26UL, 0x2efd1778UL, 0x3d29ec7dUL,
    0x76c67800UL, 0x3fd741d8UL, 0x49dc60b3UL, 0x3d2d8b09UL, 0xe6af1800UL,
    0x3fd6e60eUL, 0x7c222d87UL, 0x3d172165UL, 0x3e9c6800UL, 0x3fd68ac8UL,
    0x2756eba0UL, 0x3d20a0d3UL, 0x0b3ab000UL, 0x3fd63003UL, 0xe731ae00UL,
    0xbd2db623UL, 0xdf596000UL, 0x3fd5d5bdUL, 0x08a465dcUL, 0xbd0a0b2aUL,
    0x53c8d000UL, 0x3fd57bf7UL, 0xee5d40efUL, 0x3d1fadedUL, 0x0738a000UL,
    0x3fd522aeUL, 0x8164c759UL, 0x3d2ebe70UL, 0x9e173000UL, 0x3fd4c9e0UL,
    0x1b0ad8a4UL, 0xbd2e2089UL, 0xc271c800UL, 0x3fd4718dUL, 0x0967d675UL,
    0xbd2f27ceUL, 0x23d5e800UL, 0x3fd419b4UL, 0xec90e09dUL, 0x3d08e436UL,
    0x77333000UL, 0x3fd3c252UL, 0xb606bd5cUL, 0x3d183b54UL, 0x76be1000UL,
    0x3fd36b67UL, 0xb0f177c8UL, 0x3d116ecdUL, 0xe1d36000UL, 0x3fd314f1UL,
    0xd3213cb8UL, 0xbd28e27aUL, 0x7cdc9000UL, 0x3fd2bef0UL, 0x4a5004f4UL,
    0x3d2a9cfaUL, 0x1134d800UL, 0x3fd26962UL, 0xdf5bb3b6UL, 0x3d2c93c1UL,
    0x6d0eb800UL, 0x3fd21445UL, 0xba46baeaUL, 0x3d0a87deUL, 0x635a6800UL,
    0x3fd1bf99UL, 0x5147bdb7UL, 0x3d2ca6edUL, 0xcbacf800UL, 0x3fd16b5cUL,
    0xf7a51681UL, 0x3d2b9acdUL, 0x8227e800UL, 0x3fd1178eUL, 0x63a5f01cUL,
    0xbd2c210eUL, 0x67616000UL, 0x3fd0c42dUL, 0x163ceae9UL, 0x3d27188bUL,
    0x604d5800UL, 0x3fd07138UL, 0x16ed4e91UL, 0x3cf89cdbUL, 0x5626c800UL,
    0x3fd01eaeUL, 0x1485e94aUL, 0xbd16f08cUL, 0x6cb3b000UL, 0x3fcf991cUL,
    0xca0cdf30UL, 0x3d1bcbecUL, 0xe4dd0000UL, 0x3fcef5adUL, 0x65bb8e11UL,
    0xbcca2115UL, 0xffe71000UL, 0x3fce530eUL, 0x6041f430UL, 0x3cc21227UL,
    0xb0d49000UL, 0x3fcdb13dUL, 0xf715b035UL, 0xbd2aff2aUL, 0xf2656000UL,
    0x3fcd1037UL, 0x75b6f6e4UL, 0xbd084a7eUL, 0xc6f01000UL, 0x3fcc6ffbUL,
    0xc5962bd2UL, 0xbcf1ec72UL, 0x383be000UL, 0x3fcbd087UL, 0x595412b6UL,
    0xbd2d4bc4UL, 0x575bd000UL, 0x3fcb31d8UL, 0x4eace1aaUL, 0xbd0c358dUL,
    0x3c8ae000UL, 0x3fca93edUL, 0x50562169UL, 0xbd287243UL, 0x07089000UL,
    0x3fc9f6c4UL, 0x6865817aUL, 0x3d29904dUL, 0xdcf70000UL, 0x3fc95a5aUL,
    0x58a0ff6fUL, 0x3d07f228UL, 0xeb390000UL, 0x3fc8beafUL, 0xaae92cd1UL,
    0xbd073d54UL, 0x6551a000UL, 0x3fc823c1UL, 0x9a631e83UL, 0x3d1e0ddbUL,
    0x85445000UL, 0x3fc7898dUL, 0x70914305UL, 0xbd1c6610UL, 0x8b757000UL,
    0x3fc6f012UL, 0xe59c21e1UL, 0xbd25118dUL, 0xbe8c1000UL, 0x3fc6574eUL,
    0x2c3c2e78UL, 0x3d19cf8bUL, 0x6b544000UL, 0x3fc5bf40UL, 0xeb68981cUL,
    0xbd127023UL, 0xe4a1b000UL, 0x3fc527e5UL, 0xe5697dc7UL, 0x3d2633e8UL,
    0x8333b000UL, 0x3fc4913dUL, 0x54fdb678UL, 0x3d258379UL, 0xa5993000UL,
    0x3fc3fb45UL, 0x7e6a354dUL, 0xbd2cd1d8UL, 0xb0159000UL, 0x3fc365fcUL,
    0x234b7289UL, 0x3cc62fa8UL, 0x0c868000UL, 0x3fc2d161UL, 0xcb81b4a1UL,
    0x3d039d6cUL, 0x2a49c000UL, 0x3fc23d71UL, 0x8fd3df5cUL, 0x3d100d23UL,
    0x7e23f000UL, 0x3fc1aa2bUL, 0x44389934UL, 0x3d2ca78eUL, 0x8227e000UL,
    0x3fc1178eUL, 0xce2d07f2UL, 0x3d21ef78UL, 0xb59e4000UL, 0x3fc08598UL,
    0x7009902cUL, 0xbd27e5ddUL, 0x39dbe000UL, 0x3fbfe891UL, 0x4fa10afdUL,
    0xbd2534d6UL, 0x830a2000UL, 0x3fbec739UL, 0xafe645e0UL, 0xbd2dc068UL,
    0x63844000UL, 0x3fbda727UL, 0x1fa71733UL, 0x3d1a8940UL, 0x01bc4000UL,
    0x3fbc8858UL, 0xc65aacd3UL, 0x3d2646d1UL, 0x8dad6000UL, 0x3fbb6ac8UL,
    0x2bf768e5UL, 0xbd139080UL, 0x40b1c000UL, 0x3fba4e76UL, 0xb94407c8UL,
    0xbd0e42b6UL, 0x5d594000UL, 0x3fb9335eUL, 0x3abd47daUL, 0x3d23115cUL,
    0x2f40e000UL, 0x3fb8197eUL, 0xf96ffdf7UL, 0x3d0f80dcUL, 0x0aeac000UL,
    0x3fb700d3UL, 0xa99ded32UL, 0x3cec1e8dUL, 0x4d97a000UL, 0x3fb5e95aUL,
    0x3c5d1d1eUL, 0xbd2c6906UL, 0x5d208000UL, 0x3fb4d311UL, 0x82f4e1efUL,
    0xbcf53a25UL, 0xa7d1e000UL, 0x3fb3bdf5UL, 0xa5db4ed7UL, 0x3d2cc85eUL,
    0xa4472000UL, 0x3fb2aa04UL, 0xae9c697dUL, 0xbd20b6e8UL, 0xd1466000UL,
    0x3fb1973bUL, 0x560d9e9bUL, 0xbd25325dUL, 0xb59e4000UL, 0x3fb08598UL,
    0x7009902cUL, 0xbd17e5ddUL, 0xc006c000UL, 0x3faeea31UL, 0x4fc93b7bUL,
    0xbd0e113eUL, 0xcdddc000UL, 0x3faccb73UL, 0x47d82807UL, 0xbd1a68f2UL,
    0xd0fb0000UL, 0x3faaaef2UL, 0x353bb42eUL, 0x3d20fc1aUL, 0x149fc000UL,
    0x3fa894aaUL, 0xd05a267dUL, 0xbd197995UL, 0xf2d4c000UL, 0x3fa67c94UL,
    0xec19afa2UL, 0xbd029efbUL, 0xd42e0000UL, 0x3fa466aeUL, 0x75bdfd28UL,
    0xbd2c1673UL, 0x2f8d0000UL, 0x3fa252f3UL, 0xe021b67bUL, 0x3d283e9aUL,
    0x89e74000UL, 0x3fa0415dUL, 0x5cf1d753UL, 0x3d0111c0UL, 0xec148000UL,
    0x3f9c63d2UL, 0x3f9eb2f3UL, 0x3d2578c6UL, 0x28c90000UL, 0x3f984925UL,
    0x325a0c34UL, 0xbd2aa0baUL, 0x25980000UL, 0x3f9432a9UL, 0x928637feUL,
    0x3d098139UL, 0x58938000UL, 0x3f902056UL, 0x06e2f7d2UL, 0xbd23dc5bUL,
    0xa3890000UL, 0x3f882448UL, 0xda74f640UL, 0xbd275577UL, 0x75890000UL,
    0x3f801015UL, 0x999d2be8UL, 0xbd10c76bUL, 0x59580000UL, 0x3f700805UL,
    0xcb31c67bUL, 0x3d2166afUL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x80000000UL
};

ATTRIBUTE_ALIGNED(16) juint _log2[] =
{
    0xfefa3800UL, 0x3fa62e42UL, 0x93c76730UL, 0x3ceef357UL
};

ATTRIBUTE_ALIGNED(16) juint _coeff[] =
{
    0x92492492UL, 0x3fc24924UL, 0x00000000UL, 0xbfd00000UL, 0x3d6fb175UL,
    0xbfc5555eUL, 0x55555555UL, 0x3fd55555UL, 0x9999999aUL, 0x3fc99999UL,
    0x00000000UL, 0xbfe00000UL
};

//registers,
// input: xmm0
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, r8, r11

void MacroAssembler::fast_log(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register tmp1, Register tmp2) {
  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label L_2TAG_PACKET_4_0_2, L_2TAG_PACKET_5_0_2, L_2TAG_PACKET_6_0_2, L_2TAG_PACKET_7_0_2;
  Label L_2TAG_PACKET_8_0_2;
  Label B1_3, B1_5, start;

  assert_different_registers(tmp1, tmp2, eax, ecx, edx);
  address L_tbl = (address)_L_tbl;
  address log2 = (address)_log2;
  address coeff = (address)_coeff;

  bind(start);
  subq(rsp, 24);
  movsd(Address(rsp, 0), xmm0);
  mov64(rax, 0x3ff0000000000000);
  movdq(xmm2, rax);
  mov64(rdx, 0x77f0000000000000);
  movdq(xmm3, rdx);
  movl(ecx, 32768);
  movdl(xmm4, rcx);
  mov64(tmp1, 0xffffe00000000000);
  movdq(xmm5, tmp1);
  movdqu(xmm1, xmm0);
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  movl(ecx, 16352);
  psrlq(xmm0, 27);
  lea(tmp2, ExternalAddress(L_tbl));
  psrld(xmm0, 2);
  rcpps(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 228);
  psrlq(xmm1, 12);
  subl(eax, 16);
  cmpl(eax, 32736);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_0_0_2);

  bind(L_2TAG_PACKET_1_0_2);
  paddd(xmm0, xmm4);
  por(xmm1, xmm3);
  movdl(edx, xmm0);
  psllq(xmm0, 29);
  pand(xmm5, xmm1);
  pand(xmm0, xmm6);
  subsd(xmm1, xmm5);
  mulpd(xmm5, xmm0);
  andl(eax, 32752);
  subl(eax, ecx);
  cvtsi2sdl(xmm7, eax);
  mulsd(xmm1, xmm0);
  movq(xmm6, ExternalAddress(log2));       // 0xfefa3800UL, 0x3fa62e42UL
  movdqu(xmm3, ExternalAddress(coeff));    // 0x92492492UL, 0x3fc24924UL, 0x00000000UL, 0xbfd00000UL
  subsd(xmm5, xmm2);
  andl(edx, 16711680);
  shrl(edx, 12);
  movdqu(xmm0, Address(tmp2, edx));
  movdqu(xmm4, ExternalAddress(16 + coeff)); // 0x3d6fb175UL, 0xbfc5555eUL, 0x55555555UL, 0x3fd55555UL
  addsd(xmm1, xmm5);
  movdqu(xmm2, ExternalAddress(32 + coeff)); // 0x9999999aUL, 0x3fc99999UL, 0x00000000UL, 0xbfe00000UL
  mulsd(xmm6, xmm7);
  if (VM_Version::supports_sse3()) {
    movddup(xmm5, xmm1);
  }
  else {
    movdqu(xmm5, xmm1);
    movlhps(xmm5, xmm5);
  }
  mulsd(xmm7, ExternalAddress(8 + log2));    // 0x93c76730UL, 0x3ceef357UL
  mulsd(xmm3, xmm1);
  addsd(xmm0, xmm6);
  mulpd(xmm4, xmm5);
  mulpd(xmm5, xmm5);
  if (VM_Version::supports_sse3()) {
    movddup(xmm6, xmm0);
  }
  else {
    movdqu(xmm6, xmm0);
    movlhps(xmm6, xmm6);
  }
  addsd(xmm0, xmm1);
  addpd(xmm4, xmm2);
  mulpd(xmm3, xmm5);
  subsd(xmm6, xmm0);
  mulsd(xmm4, xmm1);
  pshufd(xmm2, xmm0, 238);
  addsd(xmm1, xmm6);
  mulsd(xmm5, xmm5);
  addsd(xmm7, xmm2);
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
  movdqu(xmm1, xmm0);
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  psrlq(xmm0, 27);
  movl(ecx, 18416);
  psrld(xmm0, 2);
  rcpps(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 228);
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
  movl(Address(rsp, 16), 3);
  jmp(L_2TAG_PACKET_8_0_2);
  bind(L_2TAG_PACKET_7_0_2);
  xorpd(xmm1, xmm1);
  xorpd(xmm0, xmm0);
  movl(eax, 49136);
  pinsrw(xmm0, eax, 3);
  divsd(xmm0, xmm1);
  movl(Address(rsp, 16), 2);

  bind(L_2TAG_PACKET_8_0_2);
  movq(Address(rsp, 8), xmm0);

  bind(B1_3);
  movq(xmm0, Address(rsp, 8));

  bind(B1_5);
  addq(rsp, 24);
}
#else
// The 32 bit code is at most SSE2 compliant
ATTRIBUTE_ALIGNED(16) juint _static_const_table_log[] =
{
    0xfefa3800UL, 0x3fe62e42UL, 0x93c76730UL, 0x3d2ef357UL, 0xaa241800UL,
    0x3fe5ee82UL, 0x0cda46beUL, 0x3d220238UL, 0x5c364800UL, 0x3fe5af40UL,
    0xac10c9fbUL, 0x3d2dfa63UL, 0x26bb8c00UL, 0x3fe5707aUL, 0xff3303ddUL,
    0x3d09980bUL, 0x26867800UL, 0x3fe5322eUL, 0x5d257531UL, 0x3d05ccc4UL,
    0x835a5000UL, 0x3fe4f45aUL, 0x6d93b8fbUL, 0xbd2e6c51UL, 0x6f970c00UL,
    0x3fe4b6fdUL, 0xed4c541cUL, 0x3cef7115UL, 0x27e8a400UL, 0x3fe47a15UL,
    0xf94d60aaUL, 0xbd22cb6aUL, 0xf2f92400UL, 0x3fe43d9fUL, 0x481051f7UL,
    0xbcfd984fUL, 0x2125cc00UL, 0x3fe4019cUL, 0x30f0c74cUL, 0xbd26ce79UL,
    0x0c36c000UL, 0x3fe3c608UL, 0x7cfe13c2UL, 0xbd02b736UL, 0x17197800UL,
    0x3fe38ae2UL, 0xbb5569a4UL, 0xbd218b7aUL, 0xad9d8c00UL, 0x3fe35028UL,
    0x9527e6acUL, 0x3d10b83fUL, 0x44340800UL, 0x3fe315daUL, 0xc5a0ed9cUL,
    0xbd274e93UL, 0x57b0e000UL, 0x3fe2dbf5UL, 0x07b9dc11UL, 0xbd17a6e5UL,
    0x6d0ec000UL, 0x3fe2a278UL, 0xe797882dUL, 0x3d206d2bUL, 0x1134dc00UL,
    0x3fe26962UL, 0x05226250UL, 0xbd0b61f1UL, 0xd8bebc00UL, 0x3fe230b0UL,
    0x6e48667bUL, 0x3d12fc06UL, 0x5fc61800UL, 0x3fe1f863UL, 0xc9fe81d3UL,
    0xbd2a7242UL, 0x49ae6000UL, 0x3fe1c078UL, 0xed70e667UL, 0x3cccacdeUL,
    0x40f23c00UL, 0x3fe188eeUL, 0xf8ab4650UL, 0x3d14cc4eUL, 0xf6f29800UL,
    0x3fe151c3UL, 0xa293ae49UL, 0xbd2edd97UL, 0x23c75c00UL, 0x3fe11af8UL,
    0xbb9ddcb2UL, 0xbd258647UL, 0x8611cc00UL, 0x3fe0e489UL, 0x07801742UL,
    0x3d1c2998UL, 0xe2d05400UL, 0x3fe0ae76UL, 0x887e7e27UL, 0x3d1f486bUL,
    0x0533c400UL, 0x3fe078bfUL, 0x41edf5fdUL, 0x3d268122UL, 0xbe760400UL,
    0x3fe04360UL, 0xe79539e0UL, 0xbd04c45fUL, 0xe5b20800UL, 0x3fe00e5aUL,
    0xb1727b1cUL, 0xbd053ba3UL, 0xaf7a4800UL, 0x3fdfb358UL, 0x3c164935UL,
    0x3d0085faUL, 0xee031800UL, 0x3fdf4aa7UL, 0x6f014a8bUL, 0x3d12cde5UL,
    0x56b41000UL, 0x3fdee2a1UL, 0x5a470251UL, 0x3d2f27f4UL, 0xc3ddb000UL,
    0x3fde7b42UL, 0x5372bd08UL, 0xbd246550UL, 0x1a272800UL, 0x3fde148aUL,
    0x07322938UL, 0xbd1326b2UL, 0x484c9800UL, 0x3fddae75UL, 0x60dc616aUL,
    0xbd1ea42dUL, 0x46def800UL, 0x3fdd4902UL, 0xe9a767a8UL, 0x3d235bafUL,
    0x18064800UL, 0x3fdce42fUL, 0x3ec7a6b0UL, 0xbd0797c3UL, 0xc7455800UL,
    0x3fdc7ff9UL, 0xc15249aeUL, 0xbd29b6ddUL, 0x693fa000UL, 0x3fdc1c60UL,
    0x7fe8e180UL, 0x3d2cec80UL, 0x1b80e000UL, 0x3fdbb961UL, 0xf40a666dUL,
    0x3d27d85bUL, 0x04462800UL, 0x3fdb56faUL, 0x2d841995UL, 0x3d109525UL,
    0x5248d000UL, 0x3fdaf529UL, 0x52774458UL, 0xbd217cc5UL, 0x3c8ad800UL,
    0x3fda93edUL, 0xbea77a5dUL, 0x3d1e36f2UL, 0x0224f800UL, 0x3fda3344UL,
    0x7f9d79f5UL, 0x3d23c645UL, 0xea15f000UL, 0x3fd9d32bUL, 0x10d0c0b0UL,
    0xbd26279eUL, 0x43135800UL, 0x3fd973a3UL, 0xa502d9f0UL, 0xbd152313UL,
    0x635bf800UL, 0x3fd914a8UL, 0x2ee6307dUL, 0xbd1766b5UL, 0xa88b3000UL,
    0x3fd8b639UL, 0xe5e70470UL, 0xbd205ae1UL, 0x776dc800UL, 0x3fd85855UL,
    0x3333778aUL, 0x3d2fd56fUL, 0x3bd81800UL, 0x3fd7fafaUL, 0xc812566aUL,
    0xbd272090UL, 0x687cf800UL, 0x3fd79e26UL, 0x2efd1778UL, 0x3d29ec7dUL,
    0x76c67800UL, 0x3fd741d8UL, 0x49dc60b3UL, 0x3d2d8b09UL, 0xe6af1800UL,
    0x3fd6e60eUL, 0x7c222d87UL, 0x3d172165UL, 0x3e9c6800UL, 0x3fd68ac8UL,
    0x2756eba0UL, 0x3d20a0d3UL, 0x0b3ab000UL, 0x3fd63003UL, 0xe731ae00UL,
    0xbd2db623UL, 0xdf596000UL, 0x3fd5d5bdUL, 0x08a465dcUL, 0xbd0a0b2aUL,
    0x53c8d000UL, 0x3fd57bf7UL, 0xee5d40efUL, 0x3d1fadedUL, 0x0738a000UL,
    0x3fd522aeUL, 0x8164c759UL, 0x3d2ebe70UL, 0x9e173000UL, 0x3fd4c9e0UL,
    0x1b0ad8a4UL, 0xbd2e2089UL, 0xc271c800UL, 0x3fd4718dUL, 0x0967d675UL,
    0xbd2f27ceUL, 0x23d5e800UL, 0x3fd419b4UL, 0xec90e09dUL, 0x3d08e436UL,
    0x77333000UL, 0x3fd3c252UL, 0xb606bd5cUL, 0x3d183b54UL, 0x76be1000UL,
    0x3fd36b67UL, 0xb0f177c8UL, 0x3d116ecdUL, 0xe1d36000UL, 0x3fd314f1UL,
    0xd3213cb8UL, 0xbd28e27aUL, 0x7cdc9000UL, 0x3fd2bef0UL, 0x4a5004f4UL,
    0x3d2a9cfaUL, 0x1134d800UL, 0x3fd26962UL, 0xdf5bb3b6UL, 0x3d2c93c1UL,
    0x6d0eb800UL, 0x3fd21445UL, 0xba46baeaUL, 0x3d0a87deUL, 0x635a6800UL,
    0x3fd1bf99UL, 0x5147bdb7UL, 0x3d2ca6edUL, 0xcbacf800UL, 0x3fd16b5cUL,
    0xf7a51681UL, 0x3d2b9acdUL, 0x8227e800UL, 0x3fd1178eUL, 0x63a5f01cUL,
    0xbd2c210eUL, 0x67616000UL, 0x3fd0c42dUL, 0x163ceae9UL, 0x3d27188bUL,
    0x604d5800UL, 0x3fd07138UL, 0x16ed4e91UL, 0x3cf89cdbUL, 0x5626c800UL,
    0x3fd01eaeUL, 0x1485e94aUL, 0xbd16f08cUL, 0x6cb3b000UL, 0x3fcf991cUL,
    0xca0cdf30UL, 0x3d1bcbecUL, 0xe4dd0000UL, 0x3fcef5adUL, 0x65bb8e11UL,
    0xbcca2115UL, 0xffe71000UL, 0x3fce530eUL, 0x6041f430UL, 0x3cc21227UL,
    0xb0d49000UL, 0x3fcdb13dUL, 0xf715b035UL, 0xbd2aff2aUL, 0xf2656000UL,
    0x3fcd1037UL, 0x75b6f6e4UL, 0xbd084a7eUL, 0xc6f01000UL, 0x3fcc6ffbUL,
    0xc5962bd2UL, 0xbcf1ec72UL, 0x383be000UL, 0x3fcbd087UL, 0x595412b6UL,
    0xbd2d4bc4UL, 0x575bd000UL, 0x3fcb31d8UL, 0x4eace1aaUL, 0xbd0c358dUL,
    0x3c8ae000UL, 0x3fca93edUL, 0x50562169UL, 0xbd287243UL, 0x07089000UL,
    0x3fc9f6c4UL, 0x6865817aUL, 0x3d29904dUL, 0xdcf70000UL, 0x3fc95a5aUL,
    0x58a0ff6fUL, 0x3d07f228UL, 0xeb390000UL, 0x3fc8beafUL, 0xaae92cd1UL,
    0xbd073d54UL, 0x6551a000UL, 0x3fc823c1UL, 0x9a631e83UL, 0x3d1e0ddbUL,
    0x85445000UL, 0x3fc7898dUL, 0x70914305UL, 0xbd1c6610UL, 0x8b757000UL,
    0x3fc6f012UL, 0xe59c21e1UL, 0xbd25118dUL, 0xbe8c1000UL, 0x3fc6574eUL,
    0x2c3c2e78UL, 0x3d19cf8bUL, 0x6b544000UL, 0x3fc5bf40UL, 0xeb68981cUL,
    0xbd127023UL, 0xe4a1b000UL, 0x3fc527e5UL, 0xe5697dc7UL, 0x3d2633e8UL,
    0x8333b000UL, 0x3fc4913dUL, 0x54fdb678UL, 0x3d258379UL, 0xa5993000UL,
    0x3fc3fb45UL, 0x7e6a354dUL, 0xbd2cd1d8UL, 0xb0159000UL, 0x3fc365fcUL,
    0x234b7289UL, 0x3cc62fa8UL, 0x0c868000UL, 0x3fc2d161UL, 0xcb81b4a1UL,
    0x3d039d6cUL, 0x2a49c000UL, 0x3fc23d71UL, 0x8fd3df5cUL, 0x3d100d23UL,
    0x7e23f000UL, 0x3fc1aa2bUL, 0x44389934UL, 0x3d2ca78eUL, 0x8227e000UL,
    0x3fc1178eUL, 0xce2d07f2UL, 0x3d21ef78UL, 0xb59e4000UL, 0x3fc08598UL,
    0x7009902cUL, 0xbd27e5ddUL, 0x39dbe000UL, 0x3fbfe891UL, 0x4fa10afdUL,
    0xbd2534d6UL, 0x830a2000UL, 0x3fbec739UL, 0xafe645e0UL, 0xbd2dc068UL,
    0x63844000UL, 0x3fbda727UL, 0x1fa71733UL, 0x3d1a8940UL, 0x01bc4000UL,
    0x3fbc8858UL, 0xc65aacd3UL, 0x3d2646d1UL, 0x8dad6000UL, 0x3fbb6ac8UL,
    0x2bf768e5UL, 0xbd139080UL, 0x40b1c000UL, 0x3fba4e76UL, 0xb94407c8UL,
    0xbd0e42b6UL, 0x5d594000UL, 0x3fb9335eUL, 0x3abd47daUL, 0x3d23115cUL,
    0x2f40e000UL, 0x3fb8197eUL, 0xf96ffdf7UL, 0x3d0f80dcUL, 0x0aeac000UL,
    0x3fb700d3UL, 0xa99ded32UL, 0x3cec1e8dUL, 0x4d97a000UL, 0x3fb5e95aUL,
    0x3c5d1d1eUL, 0xbd2c6906UL, 0x5d208000UL, 0x3fb4d311UL, 0x82f4e1efUL,
    0xbcf53a25UL, 0xa7d1e000UL, 0x3fb3bdf5UL, 0xa5db4ed7UL, 0x3d2cc85eUL,
    0xa4472000UL, 0x3fb2aa04UL, 0xae9c697dUL, 0xbd20b6e8UL, 0xd1466000UL,
    0x3fb1973bUL, 0x560d9e9bUL, 0xbd25325dUL, 0xb59e4000UL, 0x3fb08598UL,
    0x7009902cUL, 0xbd17e5ddUL, 0xc006c000UL, 0x3faeea31UL, 0x4fc93b7bUL,
    0xbd0e113eUL, 0xcdddc000UL, 0x3faccb73UL, 0x47d82807UL, 0xbd1a68f2UL,
    0xd0fb0000UL, 0x3faaaef2UL, 0x353bb42eUL, 0x3d20fc1aUL, 0x149fc000UL,
    0x3fa894aaUL, 0xd05a267dUL, 0xbd197995UL, 0xf2d4c000UL, 0x3fa67c94UL,
    0xec19afa2UL, 0xbd029efbUL, 0xd42e0000UL, 0x3fa466aeUL, 0x75bdfd28UL,
    0xbd2c1673UL, 0x2f8d0000UL, 0x3fa252f3UL, 0xe021b67bUL, 0x3d283e9aUL,
    0x89e74000UL, 0x3fa0415dUL, 0x5cf1d753UL, 0x3d0111c0UL, 0xec148000UL,
    0x3f9c63d2UL, 0x3f9eb2f3UL, 0x3d2578c6UL, 0x28c90000UL, 0x3f984925UL,
    0x325a0c34UL, 0xbd2aa0baUL, 0x25980000UL, 0x3f9432a9UL, 0x928637feUL,
    0x3d098139UL, 0x58938000UL, 0x3f902056UL, 0x06e2f7d2UL, 0xbd23dc5bUL,
    0xa3890000UL, 0x3f882448UL, 0xda74f640UL, 0xbd275577UL, 0x75890000UL,
    0x3f801015UL, 0x999d2be8UL, 0xbd10c76bUL, 0x59580000UL, 0x3f700805UL,
    0xcb31c67bUL, 0x3d2166afUL, 0x00000000UL, 0x00000000UL, 0x00000000UL,
    0x80000000UL, 0xfefa3800UL, 0x3fa62e42UL, 0x93c76730UL, 0x3ceef357UL,
    0x92492492UL, 0x3fc24924UL, 0x00000000UL, 0xbfd00000UL, 0x3d6fb175UL,
    0xbfc5555eUL, 0x55555555UL, 0x3fd55555UL, 0x9999999aUL, 0x3fc99999UL,
    0x00000000UL, 0xbfe00000UL, 0x00000000UL, 0xffffe000UL, 0x00000000UL,
    0xffffe000UL
};
//registers,
// input: xmm0
// scratch: xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
//          rax, rdx, rcx, rbx (tmp)

void MacroAssembler::fast_log(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register eax, Register ecx, Register edx, Register tmp) {
  Label L_2TAG_PACKET_0_0_2, L_2TAG_PACKET_1_0_2, L_2TAG_PACKET_2_0_2, L_2TAG_PACKET_3_0_2;
  Label L_2TAG_PACKET_4_0_2, L_2TAG_PACKET_5_0_2, L_2TAG_PACKET_6_0_2, L_2TAG_PACKET_7_0_2;
  Label L_2TAG_PACKET_8_0_2, L_2TAG_PACKET_9_0_2;
  Label L_2TAG_PACKET_10_0_2, start;

  assert_different_registers(tmp, eax, ecx, edx);
  address static_const_table = (address)_static_const_table_log;

  bind(start);
  subl(rsp, 104);
  movl(Address(rsp, 40), tmp);
  lea(tmp, ExternalAddress(static_const_table));
  xorpd(xmm2, xmm2);
  movl(eax, 16368);
  pinsrw(xmm2, eax, 3);
  xorpd(xmm3, xmm3);
  movl(edx, 30704);
  pinsrw(xmm3, edx, 3);
  movsd(xmm0, Address(rsp, 112));
  movapd(xmm1, xmm0);
  movl(ecx, 32768);
  movdl(xmm4, ecx);
  movsd(xmm5, Address(tmp, 2128));         // 0x00000000UL, 0xffffe000UL
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  psllq(xmm0, 5);
  movl(ecx, 16352);
  psrlq(xmm0, 34);
  rcpss(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 228);
  psrlq(xmm1, 12);
  subl(eax, 16);
  cmpl(eax, 32736);
  jcc(Assembler::aboveEqual, L_2TAG_PACKET_0_0_2);

  bind(L_2TAG_PACKET_1_0_2);
  paddd(xmm0, xmm4);
  por(xmm1, xmm3);
  movdl(edx, xmm0);
  psllq(xmm0, 29);
  pand(xmm5, xmm1);
  pand(xmm0, xmm6);
  subsd(xmm1, xmm5);
  mulpd(xmm5, xmm0);
  andl(eax, 32752);
  subl(eax, ecx);
  cvtsi2sdl(xmm7, eax);
  mulsd(xmm1, xmm0);
  movsd(xmm6, Address(tmp, 2064));         // 0xfefa3800UL, 0x3fa62e42UL
  movdqu(xmm3, Address(tmp, 2080));        // 0x92492492UL, 0x3fc24924UL, 0x00000000UL, 0xbfd00000UL
  subsd(xmm5, xmm2);
  andl(edx, 16711680);
  shrl(edx, 12);
  movdqu(xmm0, Address(tmp, edx));
  movdqu(xmm4, Address(tmp, 2096));        // 0x3d6fb175UL, 0xbfc5555eUL, 0x55555555UL, 0x3fd55555UL
  addsd(xmm1, xmm5);
  movdqu(xmm2, Address(tmp, 2112));        // 0x9999999aUL, 0x3fc99999UL, 0x00000000UL, 0xbfe00000UL
  mulsd(xmm6, xmm7);
  pshufd(xmm5, xmm1, 68);
  mulsd(xmm7, Address(tmp, 2072));         // 0x93c76730UL, 0x3ceef357UL, 0x92492492UL, 0x3fc24924UL
  mulsd(xmm3, xmm1);
  addsd(xmm0, xmm6);
  mulpd(xmm4, xmm5);
  mulpd(xmm5, xmm5);
  pshufd(xmm6, xmm0, 228);
  addsd(xmm0, xmm1);
  addpd(xmm4, xmm2);
  mulpd(xmm3, xmm5);
  subsd(xmm6, xmm0);
  mulsd(xmm4, xmm1);
  pshufd(xmm2, xmm0, 238);
  addsd(xmm1, xmm6);
  mulsd(xmm5, xmm5);
  addsd(xmm7, xmm2);
  addpd(xmm4, xmm3);
  addsd(xmm1, xmm7);
  mulpd(xmm4, xmm5);
  addsd(xmm1, xmm4);
  pshufd(xmm5, xmm4, 238);
  addsd(xmm1, xmm5);
  addsd(xmm0, xmm1);
  jmp(L_2TAG_PACKET_2_0_2);

  bind(L_2TAG_PACKET_0_0_2);
  movsd(xmm0, Address(rsp, 112));
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
  movl(edx, 3);
  mulsd(xmm0, xmm1);

  bind(L_2TAG_PACKET_9_0_2);
  movsd(Address(rsp, 0), xmm0);
  movsd(xmm0, Address(rsp, 112));
  fld_d(Address(rsp, 0));
  jmp(L_2TAG_PACKET_10_0_2);

  bind(L_2TAG_PACKET_8_0_2);
  xorpd(xmm1, xmm1);
  xorpd(xmm0, xmm0);
  movl(eax, 49136);
  pinsrw(xmm0, eax, 3);
  divsd(xmm0, xmm1);
  movl(edx, 2);
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
  movapd(xmm1, xmm0);
  pextrw(eax, xmm0, 3);
  por(xmm0, xmm2);
  psllq(xmm0, 5);
  movl(ecx, 18416);
  psrlq(xmm0, 34);
  rcpss(xmm0, xmm0);
  psllq(xmm1, 12);
  pshufd(xmm6, xmm5, 228);
  psrlq(xmm1, 12);
  jmp(L_2TAG_PACKET_1_0_2);

  bind(L_2TAG_PACKET_2_0_2);
  movsd(Address(rsp, 24), xmm0);
  fld_d(Address(rsp, 24));

  bind(L_2TAG_PACKET_10_0_2);
  movl(tmp, Address(rsp, 40));
}
#endif
