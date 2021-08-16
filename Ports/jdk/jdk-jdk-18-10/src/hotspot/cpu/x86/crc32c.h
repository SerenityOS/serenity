/*
* Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_CRC32C_H
#define CPU_X86_CRC32C_H

enum {
  // S. Gueron / Information Processing Letters 112 (2012) 184
  // shows than anything above 6K and below 32K is a good choice
  // 32K does not deliver any further performance gains
  // 6K=8*256 (*3 as we compute 3 blocks together)
  //
  // Thus selecting the smallest value so it could apply to the largest number
  // of buffer sizes.
  CRC32C_HIGH = 8 * 256,

  // empirical
  // based on ubench study using methodology described in
  // V. Gopal et al. / Fast CRC Computation for iSCSI Polynomial Using CRC32 Instruction April 2011 8
  //
  // arbitrary value between 27 and 256
  CRC32C_MIDDLE = 8 * 86,

  // V. Gopal et al. / Fast CRC Computation for iSCSI Polynomial Using CRC32 Instruction April 2011 9
  // shows that 240 and 1024 are equally good choices as the 216==8*27
  //
  // Selecting the smallest value which resulted in a significant performance improvement over
  // sequential version
  CRC32C_LOW = 8 * 27,

  CRC32C_NUM_ChunkSizeInBytes = 3,

  // We need to compute powers of 64N and 128N for each "chunk" size
  CRC32C_NUM_PRECOMPUTED_CONSTANTS = ( 2 * CRC32C_NUM_ChunkSizeInBytes )
};
// Notes:
// 1. Why we need to choose a "chunk" approach?
// Overhead of computing a powers and powers of for an arbitrary buffer of size N is significant
// (implementation approaches a library perf.)
// 2. Why only 3 "chunks"?
// Performance experiments results showed that a HIGH+LOW was not delivering a stable speedup
// curve.
//
// Disclaimer:
// If you ever decide to increase/decrease number of "chunks" be sure to modify
// a) constants table generation (hotspot/src/cpu/x86/vm/stubRoutines_x86.cpp)
// b) constant fetch from that table (macroAssembler_x86.cpp)
// c) unrolled for loop (macroAssembler_x86.cpp)

#endif /* !CPU_X86_CRC32C_H */
