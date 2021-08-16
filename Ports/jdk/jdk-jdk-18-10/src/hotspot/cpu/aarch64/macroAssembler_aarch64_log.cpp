/* Copyright (c) 2018, Cavium. All rights reserved. (By BELLSOFT)
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
#include "macroAssembler_aarch64.hpp"

// Algorithm idea is taken from x86 hotspot intrinsic and adapted for AARCH64.
//
// For mathematical background please refer to the following literature:
//
// Tang, Ping-Tak Peter.
// Table-driven implementation of the logarithm function
// in IEEE floating-point arithmetic.
// ACM Transactions on Mathematical Software (TOMS) 16, no. 4, 1990: 378-400.

/******************************************************************************/
//                     ALGORITHM DESCRIPTION - LOG()
//                     ---------------------
//
//    x=2^k * mx, mx in [1,2)
//
//    Get B~1/mx based on the output of frecpe instruction (B0)
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
// 1. log(NaN) = quiet NaN
// 2. log(+INF) = that INF
// 3. log(0) = -INF
// 4. log(1) = +0
// 5. log(x) = NaN if x < -0, including -INF
//
/******************************************************************************/

// Table with p(r) polynomial coefficients
// and table representation of logarithm values (hi and low parts)
ATTRIBUTE_ALIGNED(64) juint _L_tbl[] =
{
    // coefficients of p(r) polynomial:
    // _coeff[]
    0x00000000UL, 0xbfd00000UL, // C1_0 = -0.25
    0x92492492UL, 0x3fc24924UL, // C1_1 = 0.14285714285714285
    0x55555555UL, 0x3fd55555UL, // C2_0 = 0.3333333333333333
    0x3d6fb175UL, 0xbfc5555eUL, // C2_1 = -0.16666772842235003
    0x00000000UL, 0xbfe00000UL, // C3_0 = -0.5
    0x9999999aUL, 0x3fc99999UL, // C3_1 = 0.2
    // _log2[]
    0xfefa3800UL, 0x3fa62e42UL, // C4_0 = 0.043321698784993146
    0x93c76730UL, 0x3ceef357UL, // C4_1 = 3.436201886692732e-15
    // _L_tbl[] with logarithm values (hi and low parts)
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

// BEGIN dlog PSEUDO CODE:
//  double dlog(double X) {
//    // p(r) polynomial coefficients initialized from _L_tbl table
//    double C1_0 = _L_tbl[0];
//    double C1_1 = _L_tbl[1];
//    double C2_0 = _L_tbl[2];
//    double C2_1 = _L_tbl[3];
//    double C3_0 = _L_tbl[4];
//    double C3_1 = _L_tbl[5];
//    double C4_0 = _L_tbl[6];
//    double C4_1 = _L_tbl[7];
//    // NOTE: operations with coefficients above are mostly vectorized in assembly
//    // Check corner cases first
//    if (X == 1.0d || AS_LONG_BITS(X) + 0x0010000000000000 <= 0x0010000000000000) {
//      // NOTE: AS_LONG_BITS(X) + 0x0010000000000000 <= 0x0010000000000000 means
//      //    that X < 0 or X >= 0x7FF0000000000000 (0x7FF* is NaN or INF)
//      if (X < 0 || X is NaN) return NaN;
//      if (X == 1.0d) return 0.0d;
//      if (X == 0.0d) return -INFINITY;
//      if (X is INFINITY) return INFINITY;
//    }
//    // double representation is 2^exponent * mantissa
//    // split X into two multipliers: 2^exponent and 1.0 * mantissa
//    // pseudo function: zeroExponent(X) return value of X with exponent == 0
//    float vtmp5 = 1/(float)(zeroExponent(X)); // reciprocal estimate
//    // pseudo function: HI16(X) returns high 16 bits of double value
//    int hiWord = HI16(X);
//    double vtmp1 = (double) 0x77F0 << 48 | mantissa(X);
//    hiWord -= 16;
//    if (AS_LONG_BITS(hiWord) > 0x8000) {
//      // SMALL_VALUE branch
//      vtmp0 = vtmp1 = vtmp0 * AS_DOUBLE_BITS(0x47F0000000000000);
//      hiWord = HI16(vtmp1);
//      vtmp0 = AS_DOUBLE_BITS(AS_LONG_BITS(vtmp0) |= 0x3FF0000000000000);
//      vtmp5 = (double) (1/(float)vtmp0);
//      vtmp1 <<= 12;
//      vtmp1 >>= 12;
//    }
//    // MAIN branch
//    double vtmp3 = AS_LONG_BITS(vtmp1) & 0xffffe00000000000; // hi part
//    int intB0 = AS_INT_BITS(vtmp5) + 0x8000;
//    double vtmp0 = AS_DOUBLE_BITS(0xffffe00000000000 & (intB0<<29));
//    int index = (intB0 >> 16) && 0xFF;
//    double hiTableValue = _L_tbl[8+index]; // vtmp2[0]
//    double lowTableValue = _L_tbl[16+index]; // vtmp2[1]
//    vtmp5 = AS_DOUBLE_BITS(hiWord & 0x7FF0 - 0x3FE0); // 0x3FE = 1023 << 4
//    vtmp1 -= vtmp3; // low part
//    vtmp3 = vtmp3*vtmp0 - 1.0;
//    hiTableValue += C4_0 * vtmp5;
//    lowTableValue += C4_1 * vtmp5;
//    double r = vtmp1 * vtmp0 + vtmp3; // r = B*mx-1.0, computed in hi and low parts
//    vtmp0 = hiTableValue + r;
//    hiTableValue -= vtmp0;
//    double r2 = r*r;
//    double r3 = r2*r;
//    double p7 = C3_0*r2 + C2_0*r3 + C1_0*r2*r2 + C3_1*r3*r2 + C2_1*r3*r3
//              + C1_1*r3*r2*r2; // degree 7 polynomial
//    return p7 + (vtmp0 + ((r + hiTableValue) + lowTableValue));
//  }
//
// END dlog PSEUDO CODE


// Generate log(X). X passed in register v0. Return log(X) into v0.
// Generator parameters: 10 temporary FPU registers and  temporary general
// purpose registers
void MacroAssembler::fast_log(FloatRegister vtmp0, FloatRegister vtmp1,
                              FloatRegister vtmp2, FloatRegister vtmp3,
                              FloatRegister vtmp4, FloatRegister vtmp5,
                              FloatRegister C1, FloatRegister C2,
                              FloatRegister C3, FloatRegister C4,
                              Register tmp1, Register tmp2, Register tmp3,
                              Register tmp4, Register tmp5) {
  Label DONE, CHECK_CORNER_CASES, SMALL_VALUE, MAIN,
      CHECKED_CORNER_CASES, RETURN_MINF_OR_NAN;
  const int64_t INF_OR_NAN_PREFIX = 0x7FF0;
  const int64_t MINF_OR_MNAN_PREFIX = 0xFFF0;
  const int64_t ONE_PREFIX = 0x3FF0;
    movz(tmp2, ONE_PREFIX, 48);
    movz(tmp4, 0x0010, 48);
    fmovd(rscratch1, v0); // rscratch1 = AS_LONG_BITS(X)
    lea(rscratch2, ExternalAddress((address)_L_tbl));
    movz(tmp5, 0x7F);
    add(tmp1, rscratch1, tmp4);
    cmp(tmp2, rscratch1);
    lsr(tmp3, rscratch1, 29);
    ccmp(tmp1, tmp4, 0b1101 /* LE */, NE);
    bfm(tmp3, tmp5, 41, 8);
    fmovs(vtmp5, tmp3);
    // Load coefficients from table. All coefficients are organized to be
    // in specific order, because load below will load it in vectors to be used
    // later in vector instructions. Load will be performed in parallel while
    // branches are taken. C1 will contain vector of {C1_0, C1_1}, C2 =
    // {C2_0, C2_1}, C3 = {C3_0, C3_1}, C4 = {C4_0, C4_1}
    ld1(C1, C2, C3, C4, T2D, post(rscratch2, 64));
    br(LE, CHECK_CORNER_CASES);
  bind(CHECKED_CORNER_CASES);
    // all corner cases are handled
    frecpe(vtmp5, vtmp5, S);                   // vtmp5 ~= 1/vtmp5
    lsr(tmp2, rscratch1, 48);
    movz(tmp4, 0x77f0, 48);
    fmovd(vtmp4, 1.0);
    movz(tmp1, INF_OR_NAN_PREFIX, 48);
    bfm(tmp4, rscratch1, 0, 51);               // tmp4 = 0x77F0 << 48 | mantissa(X)
    // vtmp1 = AS_DOUBLE_BITS(0x77F0 << 48 | mantissa(X)) == mx
    fmovd(vtmp1, tmp4);
    subw(tmp2, tmp2, 16);
    subs(zr, tmp2, 0x8000);
    br(GE, SMALL_VALUE);
  bind(MAIN);
    fmovs(tmp3, vtmp5);                        // int intB0 = AS_INT_BITS(B);
    mov(tmp5, 0x3FE0);
    mov(rscratch1, 0xffffe00000000000);
    andr(tmp2, tmp2, tmp1, LSR, 48);           // hiWord & 0x7FF0
    sub(tmp2, tmp2, tmp5);                     // tmp2 = hiWord & 0x7FF0 - 0x3FE0
    scvtfwd(vtmp5, tmp2);                      // vtmp5 = (double)tmp2;
    addw(tmp3, tmp3, 0x8000);                  // tmp3 = B
    andr(tmp4, tmp4, rscratch1);               // tmp4 == hi_part(mx)
    andr(rscratch1, rscratch1, tmp3, LSL, 29); // rscratch1 = hi_part(B)
    ubfm(tmp3, tmp3, 16, 23);                  // int index = (intB0 >> 16) && 0xFF
    ldrq(vtmp2, Address(rscratch2, tmp3, Address::lsl(4))); // vtmp2 = _L_tbl[index]
    // AS_LONG_BITS(vtmp1) & 0xffffe00000000000 // hi_part(mx)
    fmovd(vtmp3, tmp4);
    fmovd(vtmp0, rscratch1);                   // vtmp0 = hi_part(B)
    fsubd(vtmp1, vtmp1, vtmp3);                // vtmp1 -= vtmp3; // low_part(mx)
    fnmsub(vtmp3, vtmp3, vtmp0, vtmp4);        // vtmp3 = vtmp3*vtmp0 - vtmp4
    fmlavs(vtmp2, T2D, C4, vtmp5, 0);          // vtmp2 += {C4} * vtmp5
    // vtmp1 = r = vtmp1 * vtmp0 + vtmp3 == low_part(mx) * hi_part(B) + (hi_part(mx)*hi_part(B) - 1.0)
    fmaddd(vtmp1, vtmp1, vtmp0, vtmp3);
    ins(vtmp5, D, vtmp2, 0, 1);                // vtmp5 = vtmp2[1];
    faddd(vtmp0, vtmp2, vtmp1);                // vtmp0 = vtmp2 + vtmp1
    fmlavs(C3, T2D, C2, vtmp1, 0);             // {C3} += {C2}*vtmp1
    fsubd(vtmp2, vtmp2, vtmp0);                // vtmp2 -= vtmp0
    fmuld(vtmp3, vtmp1, vtmp1);                // vtmp3 = vtmp1*vtmp1
    faddd(C4, vtmp1, vtmp2);                   // C4[0] = vtmp1 + vtmp2
    fmlavs(C3, T2D, C1, vtmp3, 0);             // {C3} += {C1}*vtmp3
    faddd(C4, C4, vtmp5);                      // C4 += vtmp5
    fmuld(vtmp4, vtmp3, vtmp1);                // vtmp4 = vtmp3*vtmp1
    faddd(vtmp0, vtmp0, C4);                   // vtmp0 += C4
    fmlavs(C3, T2D, vtmp4, C3, 1);             // {C3} += {vtmp4}*C3[1]
    fmaddd(vtmp0, C3, vtmp3, vtmp0);           // vtmp0 = C3 * vtmp3 + vtmp0
    ret(lr);

  block_comment("if (AS_LONG_BITS(hiWord) > 0x8000)"); {
    bind(SMALL_VALUE);
      movz(tmp2, 0x47F0, 48);
      fmovd(vtmp1, tmp2);
      fmuld(vtmp0, vtmp1, v0);
      fmovd(vtmp1, vtmp0);
      umov(tmp2, vtmp1, S, 3);
      orr(vtmp0, T16B, vtmp0, vtmp4);
      ushr(vtmp5, T2D, vtmp0, 27);
      ushr(vtmp5, T4S, vtmp5, 2);
      frecpe(vtmp5, vtmp5, S);
      shl(vtmp1, T2D, vtmp1, 12);
      ushr(vtmp1, T2D, vtmp1, 12);
      b(MAIN);
  }

  block_comment("Corner cases"); {
    bind(RETURN_MINF_OR_NAN);
      movz(tmp1, MINF_OR_MNAN_PREFIX, 48);
      orr(rscratch1, rscratch1, tmp1);
      fmovd(v0, rscratch1);
      ret(lr);
    bind(CHECK_CORNER_CASES);
      movz(tmp1, INF_OR_NAN_PREFIX, 48);
      cmp(rscratch1, zr);
      br(LE, RETURN_MINF_OR_NAN);
      cmp(rscratch1, tmp1);
      br(GE, DONE);
      cmp(rscratch1, tmp2);
      br(NE, CHECKED_CORNER_CASES);
      fmovd(v0, 0.0);
  }
  bind(DONE);
    ret(lr);
}
