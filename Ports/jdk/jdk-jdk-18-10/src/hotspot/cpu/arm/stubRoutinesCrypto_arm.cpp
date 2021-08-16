/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifdef COMPILE_CRYPTO

// The Rijndael S-box and inverted S-box are embedded here for a faster access.
//
// Note about lookup tables (T1...T4 and T5..T8):
// The tables (boxes) combine ahead-of-time precalculated transposition and mixing steps as
// an alternative to a runtime calculation.
// The tables are statically generated in com/sun/crypto/provider/AESCrypt class.
// Only the first table reference is passed to AES methods below. The other 3 tables
// in ecryption and decryption are calculated in runtime by rotating the T1 result accordingly.
// It is a free operation on ARM with embedded register-shifted-register EOR capability.
// The table reference is passed in a form of a last argument on the parametes list.
// The tables lookup method proves to perform better then a runtime Galois Field caclulation,
// due to a lack of HW acceleration for the later.

unsigned char * SBox;
unsigned char * SInvBox;

void  aes_init() {

  const static unsigned char Si[256] =
    {
      0x52, 0x09, 0x6A, 0xD5, 0x30, 0x36, 0xA5, 0x38,
      0xBF, 0x40, 0xA3, 0x9E, 0x81, 0xF3, 0xD7, 0xFB,
      0x7C, 0xE3, 0x39, 0x82, 0x9B, 0x2F, 0xFF, 0x87,
      0x34, 0x8E, 0x43, 0x44, 0xC4, 0xDE, 0xE9, 0xCB,
      0x54, 0x7B, 0x94, 0x32, 0xA6, 0xC2, 0x23, 0x3D,
      0xEE, 0x4C, 0x95, 0x0B, 0x42, 0xFA, 0xC3, 0x4E,
      0x08, 0x2E, 0xA1, 0x66, 0x28, 0xD9, 0x24, 0xB2,
      0x76, 0x5B, 0xA2, 0x49, 0x6D, 0x8B, 0xD1, 0x25,
      0x72, 0xF8, 0xF6, 0x64, 0x86, 0x68, 0x98, 0x16,
      0xD4, 0xA4, 0x5C, 0xCC, 0x5D, 0x65, 0xB6, 0x92,
      0x6C, 0x70, 0x48, 0x50, 0xFD, 0xED, 0xB9, 0xDA,
      0x5E, 0x15, 0x46, 0x57, 0xA7, 0x8D, 0x9D, 0x84,
      0x90, 0xD8, 0xAB, 0x00, 0x8C, 0xBC, 0xD3, 0x0A,
      0xF7, 0xE4, 0x58, 0x05, 0xB8, 0xB3, 0x45, 0x06,
      0xD0, 0x2C, 0x1E, 0x8F, 0xCA, 0x3F, 0x0F, 0x02,
      0xC1, 0xAF, 0xBD, 0x03, 0x01, 0x13, 0x8A, 0x6B,
      0x3A, 0x91, 0x11, 0x41, 0x4F, 0x67, 0xDC, 0xEA,
      0x97, 0xF2, 0xCF, 0xCE, 0xF0, 0xB4, 0xE6, 0x73,
      0x96, 0xAC, 0x74, 0x22, 0xE7, 0xAD, 0x35, 0x85,
      0xE2, 0xF9, 0x37, 0xE8, 0x1C, 0x75, 0xDF, 0x6E,
      0x47, 0xF1, 0x1A, 0x71, 0x1D, 0x29, 0xC5, 0x89,
      0x6F, 0xB7, 0x62, 0x0E, 0xAA, 0x18, 0xBE, 0x1B,
      0xFC, 0x56, 0x3E, 0x4B, 0xC6, 0xD2, 0x79, 0x20,
      0x9A, 0xDB, 0xC0, 0xFE, 0x78, 0xCD, 0x5A, 0xF4,
      0x1F, 0xDD, 0xA8, 0x33, 0x88, 0x07, 0xC7, 0x31,
      0xB1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xEC, 0x5F,
      0x60, 0x51, 0x7F, 0xA9, 0x19, 0xB5, 0x4A, 0x0D,
      0x2D, 0xE5, 0x7A, 0x9F, 0x93, 0xC9, 0x9C, 0xEF,
      0xA0, 0xE0, 0x3B, 0x4D, 0xAE, 0x2A, 0xF5, 0xB0,
      0xC8, 0xEB, 0xBB, 0x3C, 0x83, 0x53, 0x99, 0x61,
      0x17, 0x2B, 0x04, 0x7E, 0xBA, 0x77, 0xD6, 0x26,
      0xE1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0C, 0x7D
    };

  static const unsigned char S[256]={
      0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5,
      0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76,
      0xCA, 0x82, 0xC9, 0x7D, 0xFA, 0x59, 0x47, 0xF0,
      0xAD, 0xD4, 0xA2, 0xAF, 0x9C, 0xA4, 0x72, 0xC0,
      0xB7, 0xFD, 0x93, 0x26, 0x36, 0x3F, 0xF7, 0xCC,
      0x34, 0xA5, 0xE5, 0xF1, 0x71, 0xD8, 0x31, 0x15,
      0x04, 0xC7, 0x23, 0xC3, 0x18, 0x96, 0x05, 0x9A,
      0x07, 0x12, 0x80, 0xE2, 0xEB, 0x27, 0xB2, 0x75,
      0x09, 0x83, 0x2C, 0x1A, 0x1B, 0x6E, 0x5A, 0xA0,
      0x52, 0x3B, 0xD6, 0xB3, 0x29, 0xE3, 0x2F, 0x84,
      0x53, 0xD1, 0x00, 0xED, 0x20, 0xFC, 0xB1, 0x5B,
      0x6A, 0xCB, 0xBE, 0x39, 0x4A, 0x4C, 0x58, 0xCF,
      0xD0, 0xEF, 0xAA, 0xFB, 0x43, 0x4D, 0x33, 0x85,
      0x45, 0xF9, 0x02, 0x7F, 0x50, 0x3C, 0x9F, 0xA8,
      0x51, 0xA3, 0x40, 0x8F, 0x92, 0x9D, 0x38, 0xF5,
      0xBC, 0xB6, 0xDA, 0x21, 0x10, 0xFF, 0xF3, 0xD2,
      0xCD, 0x0C, 0x13, 0xEC, 0x5F, 0x97, 0x44, 0x17,
      0xC4, 0xA7, 0x7E, 0x3D, 0x64, 0x5D, 0x19, 0x73,
      0x60, 0x81, 0x4F, 0xDC, 0x22, 0x2A, 0x90, 0x88,
      0x46, 0xEE, 0xB8, 0x14, 0xDE, 0x5E, 0x0B, 0xDB,
      0xE0, 0x32, 0x3A, 0x0A, 0x49, 0x06, 0x24, 0x5C,
      0xC2, 0xD3, 0xAC, 0x62, 0x91, 0x95, 0xE4, 0x79,
      0xE7, 0xC8, 0x37, 0x6D, 0x8D, 0xD5, 0x4E, 0xA9,
      0x6C, 0x56, 0xF4, 0xEA, 0x65, 0x7A, 0xAE, 0x08,
      0xBA, 0x78, 0x25, 0x2E, 0x1C, 0xA6, 0xB4, 0xC6,
      0xE8, 0xDD, 0x74, 0x1F, 0x4B, 0xBD, 0x8B, 0x8A,
      0x70, 0x3E, 0xB5, 0x66, 0x48, 0x03, 0xF6, 0x0E,
      0x61, 0x35, 0x57, 0xB9, 0x86, 0xC1, 0x1D, 0x9E,
      0xE1, 0xF8, 0x98, 0x11, 0x69, 0xD9, 0x8E, 0x94,
      0x9B, 0x1E, 0x87, 0xE9, 0xCE, 0x55, 0x28, 0xDF,
      0x8C, 0xA1, 0x89, 0x0D, 0xBF, 0xE6, 0x42, 0x68,
      0x41, 0x99, 0x2D, 0x0F, 0xB0, 0x54, 0xBB, 0x16
  };

  SBox = (unsigned char*)S;
  SInvBox = (unsigned char*)Si;
}

address generate_aescrypt_encryptBlock() {
  __ align(CodeEntryAlignment);
  StubCodeMark mark(this, "StubRoutines", "aesencryptBlock");

  address start = __ pc();

  //    Register from = R0; // source byte array
  //    Register to = R1;   // destination byte array
  //    Register key = R2;  // expanded key array
  //    Register tbox = R3; // transposition box reference

  __ push (RegisterSet(R4, R12) | LR);
  __ fpush(FloatRegisterSet(D0, 4));
  __ sub(SP, SP, 32);

  // preserve TBox references
  __ add(R3, R3, arrayOopDesc::base_offset_in_bytes(T_INT));
  __ str(R3, Address(SP, 16));

  // retrieve key length. The length is used to determine the number of subsequent rounds (10, 12 or 14)
  __ ldr(R9, Address(R2, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

  __ ldr(R5, Address(R0));
  __ ldr(R10, Address(R2, 4, post_indexed));
  __ rev(R5, R5);
  __ eor(R5, R5, R10);
  __ ldr(R6, Address(R0, 4));
  __ ldr(R10, Address(R2, 4, post_indexed));
  __ rev(R6, R6);
  __ eor(R6, R6, R10);
  __ ldr(R7, Address(R0, 8));
  __ ldr(R10, Address(R2, 4, post_indexed));
  __ rev(R7, R7);
  __ eor(R7, R7, R10);
  __ ldr(R8, Address(R0, 12));
  __ ldr(R10, Address(R2, 4, post_indexed));
  __ rev(R8, R8);
  __ eor(R8, R8, R10);

  // Store the key size; However before doing that adjust the key to compensate for the Initial and Last rounds
  __ sub(R9, R9, 8);
  __ fmsr(S7, R1);

  // load first transporistion box (T1)
  __ ldr(R0, Address(SP, 16));

  __ mov(LR, R2);

  Label round;

  __ bind(round);

  // Utilize a Transposition Box lookup along with subsequent shift and EOR with a round key.
  // instructions ordering is rearranged to minimize ReadAferWrite dependency. Not that important on A15 target
  // with register renaming but performs ~10% better on A9.
  __ mov(R12, AsmOperand(R5, lsr, 24));
  __ ubfx(R4, R6, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R7, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R8);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R10, R1, R12);

  __ mov(R12, AsmOperand(R6, lsr, 24));
  __ ubfx(R4, R7, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R8, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R5);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R11, R1, R12);

  __ mov(R12, AsmOperand(R7, lsr, 24));
  __ ubfx(R4, R8, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R5, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R6);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R3, R1, R12);
  __ str(R3, Address(SP, 0));

  __ mov(R12, AsmOperand(R8, lsr, 24));
  __ ubfx(R4, R5, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R6, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R7);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R8, R1, R12);

  // update round count
  __ subs(R9, R9, 4);

  __ mov(R5, R10);
  __ mov(R6, R11);
  __ ldr(R7, Address(SP, 0));

  __ b(round, gt);


  // last round - a special case, no MixColumn
  __ mov_slow(R10, (int)SBox);


  // output buffer pointer
  __ fmrs(R9, S7);

  __ ldr(R11, Address(LR, 4, post_indexed));
  __ ldrb(R0, Address(R10, R5, lsr, 24));
  __ ubfx(R12, R6, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R7, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R8);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);
  __ str(R0, Address(R9, 4, post_indexed));

  __ ldr(R11, Address(LR, 4, post_indexed));
  __ ldrb(R0, Address(R10, R6, lsr, 24));
  __ ubfx(R12, R7, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R8, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R5);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);

  __ str(R0, Address(R9, 4, post_indexed));
  __ ldr(R11, Address(LR, 4, post_indexed));
  __ ldrb(R0, Address(R10, R7, lsr, 24));
  __ ubfx(R12, R8, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R5, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R6);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);

  __ str(R0, Address(R9, 4, post_indexed));
  __ ldr(R11, Address(LR));
  __ ldrb(R0, Address(R10, R8, lsr, 24));
  __ ubfx(R12, R5, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R6, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R7);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);

  __ str(R0, Address(R9));

  __ add(SP, SP, 32);
  __ fpop(FloatRegisterSet(D0, 4));

  __ pop(RegisterSet(R4, R12) | PC);
  return start;
}

address generate_aescrypt_decryptBlock() {
  __ align(CodeEntryAlignment);
  StubCodeMark mark(this, "StubRoutines", "aesdecryptBlock");

  address start = __ pc();

  //    Register from = R0; // source byte array
  //    Register to = R1;   // destination byte array
  //    Register key = R2;  // expanded key array
  //    Register tbox = R3; // transposition box reference

  __ push (RegisterSet(R4, R12) | LR);
  __ fpush(FloatRegisterSet(D0, 4));
  __ sub(SP, SP, 32);

  // retrieve key length
  __ ldr(R9, Address(R2, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

  // preserve TBox references
  __ add(R3, R3, arrayOopDesc::base_offset_in_bytes(T_INT));
  __ str(R3, Address(SP, 16));


  // Preserve the expanded key pointer
  __ fmsr(S8, R2);

  // The first key round is applied to the last round
  __ add(LR, R2, 16);


  __ ldr(R5, Address(R0));
  __ ldr(R10, Address(LR, 4, post_indexed));
  __ rev(R5, R5);
  __ eor(R5, R5, R10);
  __ ldr(R6, Address(R0, 4));
  __ ldr(R10, Address(LR, 4, post_indexed));
  __ rev(R6, R6);
  __ eor(R6, R6, R10);
  __ ldr(R7, Address(R0, 8));
  __ ldr(R10, Address(LR, 4, post_indexed));
  __ rev(R7, R7);
  __ eor(R7, R7, R10);
  __ ldr(R8, Address(R0, 12));
  __ ldr(R10, Address(LR, 4, post_indexed));
  __ rev(R8, R8);
  __ eor(R8, R8, R10);


  // Store the key size; However before doing that adjust the key to compensate for the Initial and Last rounds
  __ sub(R9, R9, 8);
  __ fmsr(S7, R1);

  // load transporistion box (T5)
  __ ldr(R0, Address(SP, 16));

  Label round;

  __ bind(round);
  // each sub-block is treated similary:

  // combine SubBytes|ShiftRows|MixColumn through a precalculated set of tables
  // Utilize a Transposition Box lookup along with subsequent shift and EOR with a round key.
  // instructions ordering is rearranged to minimize ReadAferWrite dependency. Not that important on A15 target
  // with register renaming but performs ~10% better on A9.
  __ mov(R12, AsmOperand(R5, lsr, 24));
  __ ubfx(R4, R8, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R7, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R6);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R10, R1, R12);

  __ mov(R12, AsmOperand(R6, lsr, 24));
  __ ubfx(R4, R5, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R8, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R7);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R11, R1, R12);

  __ mov(R12, AsmOperand(R7, lsr, 24));
  __ ubfx(R4, R6, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R5, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R8);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R3, R1, R12);
  __ str(R3, Address(SP, 0));

  __ mov(R12, AsmOperand(R8, lsr, 24));
  __ ubfx(R4, R7, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R6, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R5);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R8, R1, R12);

  // update round count
  __ subs(R9, R9, 4);

  __ mov(R5, R10);
  __ mov(R6, R11);
  __ ldr(R7, Address(SP, 0));

  __ b(round, gt);

  // last round - a special case, no MixColumn:

  // Retrieve expanded key pointer
  __ fmrs(LR, S8);

  __ mov_slow(R10, (int)SInvBox);

  // output buffer pointer
  __ fmrs(R9, S7);

  // process each sub-block in a similar manner:
  // 1. load a corresponding round key
  __ ldr(R11, Address(LR, 4, post_indexed));
  // 2. combine SubBytes and ShiftRows stages
  __ ldrb(R0, Address(R10, R5, lsr, 24));
  __ ubfx(R12, R8, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R7, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R6);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R3, R3, AsmOperand(R0, lsl, 8));
  // 3. AddRoundKey stage
  __ eor(R0, R3, R11);
  // 4. convert the result to LE representation
  __ rev(R0, R0);
  // 5. store in the output buffer
  __ str(R0, Address(R9, 4, post_indexed));

  __ ldr(R11, Address(LR, 4, post_indexed));
  __ ldrb(R0, Address(R10, R6, lsr, 24));
  __ ubfx(R12, R5, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R8, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R7);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);
  __ str(R0, Address(R9, 4, post_indexed));

  __ ldr(R11, Address(LR, 4, post_indexed));
  __ ldrb(R0, Address(R10, R7, lsr, 24));
  __ ubfx(R12, R6, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R5, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R8);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);
  __ str(R0, Address(R9, 4, post_indexed));

  __ ldr(R11, Address(LR));
  __ ldrb(R0, Address(R10, R8, lsr, 24));
  __ ubfx(R12, R7, 16, 8);
  __ ldrb(R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R6, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb (R12, R5);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ eor(R0, R0, R11);
  __ rev(R0, R0);
  __ str(R0, Address(R9));

  __ add(SP, SP, 32);
  __ fpop(FloatRegisterSet(D0, 4));
  __ pop(RegisterSet(R4, R12) | PC);

  return start;
}

address generate_cipherBlockChaining_encryptAESCrypt() {
  // R0 - plain
  // R1 - cipher
  // R2 - expanded key
  // R3 - Initialization Vector (IV)
  // [sp+0] - cipher len
  // [sp+4] Transposition Box reference

  __ align(CodeEntryAlignment);
  StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_encryptAESCrypt");

  address start = __ pc();

  __ push(RegisterSet(R4, R12) | LR);
  // load cipher length (which is first element on the original calling stack)
  __ ldr(R4, Address(SP, 40));

  __ sub(SP, SP, 32);

  // preserve some arguments
  __ mov(R5, R1);
  __ mov(R6, R2);

  // load IV
  __ ldmia(R3, RegisterSet(R9, R12), writeback);

  // preserve original source buffer on stack
  __ str(R0, Address(SP, 16));

  Label loop;
  __ bind(loop);
  __ ldmia(R0, RegisterSet(R0, R1) | RegisterSet(R7, R8));

  __ eor(R0, R0, R9);
  __ eor(R1, R1, R10);
  __ eor(R7, R7, R11);
  __ eor(R8, R8, R12);
  __ stmia(SP, RegisterSet(R0, R1) | RegisterSet(R7, R8));

  __ mov(R0, SP);
  __ mov(R1, R5);
  __ mov(R2, R6);
  __ ldr(R3, Address(SP, 40+32+4));

  // near call is sufficient since the target is also in the stubs
  __ bl(StubRoutines::_aescrypt_encryptBlock);

  __ subs(R4, R4, 16);
  __ ldr(R0, Address(SP, 16), gt);
  __ ldmia(R5, RegisterSet(R9, R12), writeback);
  __ add(R0, R0, 16, gt);
  __ str(R0, Address(SP, 16), gt);
  __ b(loop, gt);

  __ add(SP, SP, 32);
  __ pop(RegisterSet(R4, R12) | LR);
  // return cipher len (copied from the original argument)
  __ ldr(R0, Address(SP));
  __ bx(LR);

  return start;
}


// The CBC decryption could benefit from parallel processing as the blocks could be
// decrypted separatly from each other.
// NEON is utilized (if available) to perform parallel execution on 8 blocks at a time.
// Since Transposition Box (tbox) is used the parallel execution will only apply to an
// Initial Round and the last round. It's not practical to use NEON for a table lookup
// larger than 128 bytes. It also appears to be faster performing  tbox lookup
// sequentially then execute Galois Field calculation in parallel.

address generate_cipherBlockChaining_decryptAESCrypt() {
  __ align(CodeEntryAlignment);
  StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_decryptAESCrypt");

  address start = __ pc();

  Label single_block_done, single_block, cbc_done;
  // R0 - cipher
  // R1 - plain
  // R2 - expanded key
  // R3 - Initialization Vector (iv)
  // [sp+0] - cipher len
  // [sp+4] - Transpotition Box reference

  __ push(RegisterSet(R4, R12) | LR);

  // load cipher len: must be modulo 16
  __ ldr(R4, Address(SP, 40));

  if (VM_Version::has_simd()) {
    __ andrs(R4, R4, 0x7f);
  }

  // preserve registers based arguments
  __ mov(R7, R2);
  __ mov(R8, R3);

  if (VM_Version::has_simd()) {
    __ b(single_block_done, eq);
  }

  __ bind(single_block);
  // preserve args
  __ mov(R5, R0);
  __ mov(R6, R1);

  // reload arguments
  __ mov(R2, R7);
  __ ldr(R3, Address(SP, 40+4));

  // near call is sufficient as the method is part of the StubGenerator
  __ bl((address)StubRoutines::_aescrypt_decryptBlock);

  // check remainig cipher size (for individual block processing)
  __ subs(R4, R4, 16);
  if (VM_Version::has_simd()) {
    __ tst(R4, 0x7f);
  }

  // load IV (changes based on a CBC schedule)
  __ ldmia(R8, RegisterSet(R9, R12));

  // load plaintext from the previous block processing
  __ ldmia(R6, RegisterSet(R0, R3));

  // perform IV addition and save the plaintext for good now
  __ eor(R0, R0, R9);
  __ eor(R1, R1, R10);
  __ eor(R2, R2, R11);
  __ eor(R3, R3, R12);
  __ stmia(R6, RegisterSet(R0, R3));

  // adjust pointers for next block processing
  __ mov(R8, R5);
  __ add(R0, R5, 16);
  __ add(R1, R6, 16);
  __ b(single_block, ne);

  __ bind(single_block_done);
  if (!VM_Version::has_simd()) {
    __ b(cbc_done);
  } else {
  // done with single blocks.
  // check if any 8 block chunks are available for parallel processing
  __ ldr(R4, Address(SP, 40));
  __ bics(R4, R4, 0x7f);
  __ b(cbc_done, eq);

  Label decrypt_8_blocks;
  int quad = 1;
  // Process 8 blocks in parallel
  __ fpush(FloatRegisterSet(D8, 8));
  __ sub(SP, SP, 40);

  // record output buffer end address (used as a block counter)
  Address output_buffer_end(SP, 16);
  __ add(R5, R1, R4);
  __ str(R5, output_buffer_end);

  // preserve key pointer
  Address rounds_key(SP, 28);
  __ str(R7, rounds_key);
  // in decryption the first 16 bytes of expanded key are used in the last round
  __ add(LR, R7, 16);


  // Record the end of the key which is used to indicate a last round
  __ ldr(R3, Address(R7, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));
  __ add(R9, R7, AsmOperand(R3, lsl, 2));

  // preserve IV
  Address iv(SP, 36);
  __ str(R8, iv);

  __ bind(decrypt_8_blocks);
  __ mov(R5, R1);

  // preserve original source pointer
  Address original_src(SP, 32);
  __ str(R0, original_src);

  // Apply ShiftRow for 8 block at once:
  // use output buffer for a temp storage to preload it into cache

  __ vld1(D18, LR, MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vld1(D0, Address(R0, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D0, D0, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D0, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D2, Address(R0, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D2, D2, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D2, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D4, Address(R0, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D4, D4, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D4, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D6, Address(R0, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D6, D6, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D6, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D8, Address(R0, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D8, D8, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D8, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D10, Address(R0, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D10, D10, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D10, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D12, Address(R0, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D12, D12, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D12, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D14, Address(R0, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D14, D14, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ veor(D20, D14, D18, quad);
  __ vst1(D20, Address(R5, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);


  // Local frame map:
  // sp+20 - ouput buffer pointer
  // sp+28 - key pointer
  // sp+32 - original source
  // sp+36 - block counter


  // preserve output buffer pointer
  Address block_current_output_buffer(SP, 20);
  __ str(R1, block_current_output_buffer);

  // individual rounds in block processing are executed sequentially .
  Label block_start;

  // record end of the output buffer
  __ add(R0, R1, 128);
  __ str(R0, Address(SP, 12));

  __ bind(block_start);

  // load transporistion box reference (T5)
  // location of the reference (6th incoming argument, second slot on the stack):
  // 10 scalar registers on stack
  //  8 double-precision FP registers
  // 40 bytes frame size for local storage
  //  4 bytes offset to the original arguments list
  __ ldr(R0, Address(SP, 40+64+40+4));
  __ add(R0, R0, arrayOopDesc::base_offset_in_bytes(T_INT));

  // load rounds key and compensate for the first and last rounds
  __ ldr(LR, rounds_key);
  __ add(LR, LR, 32);

  // load block data out buffer
  __ ldr(R2, block_current_output_buffer);
  __ ldmia(R2, RegisterSet(R5, R8));

  Label round;
  __ bind(round);

  // Utilize a Transposition Box lookup along with subsequent shift and EOR with a round key.
  // instructions ordering is rearranged to minimize ReadAferWrite dependency. Not that important on A15 target
  // with register renaming but performs ~10% better on A9.
  __ mov(R12, AsmOperand(R5, lsr, 24));
  __ ubfx(R4, R8, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R7, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R6);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R10, R1, R12);

  __ mov(R12, AsmOperand(R6, lsr, 24));
  __ ubfx(R4, R5, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R8, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R7);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R11, R1, R12);

  __ mov(R12, AsmOperand(R7, lsr, 24));
  __ ubfx(R4, R6, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R5, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R8);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R3, R1, R12);
  __ str(R3, Address(SP, 0));

  __ mov(R12, AsmOperand(R8, lsr, 24));
  __ ubfx(R4, R7, 16, 8);
  __ ldr (R1, Address(R0, R12, lsl, 2));
  __ ldr(R2, Address(R0, R4, lsl, 2));
  __ ubfx(R3, R6, 8, 8);
  __ eor(R1, R1, AsmOperand(R2, ror, 8));
  __ uxtb(R4, R5);
  __ ldr(R3, Address(R0, R3, lsl, 2));
  __ ldr(R4, Address(R0, R4, lsl, 2));
  __ ldr(R12, Address(LR, 4, post_indexed));
  __ eor(R1, R1, AsmOperand(R3, ror, 16));
  __ eor(R12, R12, AsmOperand(R4, ror, 24));
  __ eor(R8, R1, R12);

  // see if we reached the key array end
  __ cmp(R9, LR);

  //  load processed data
  __ mov(R5, R10);
  __ mov(R6, R11);
  __ ldr(R7, Address(SP, 0));

  __ b(round, gt);


  // last round is special
  // this round could be implemented through vtbl instruction in NEON. However vtbl is limited to a 32-byte wide table (4 vectors),
  // thus it requires 8 lookup rounds to cover 256-byte wide Si table. On the other hand scalar lookup is independent of the
  // lookup table size and thus proves to be faster.
  __ ldr(LR, block_current_output_buffer);

  // cipher counter
  __ ldr(R11, Address(SP, 12));

  __ mov_slow(R10, (int)SInvBox);
  __ ldrb(R0, Address(R10, R5, lsr, 24));
  __ ubfx(R12, R8, 16, 8);
  __ ldrb (R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R7, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb(R12, R6);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ str(R0, Address(LR, 4, post_indexed));

  __ ldrb(R0, Address(R10, R6, lsr, 24));
  __ ubfx(R12, R5, 16, 8);
  __ ldrb (R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R8, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb(R12, R7);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ str(R0, Address(LR, 4, post_indexed));


  __ ldrb(R0, Address(R10, R7, lsr, 24));
  __ ubfx(R12, R6, 16, 8);
  __ ldrb (R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R5, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb(R12, R8);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ str(R0, Address(LR, 4, post_indexed));


  __ ldrb(R0, Address(R10, R8, lsr, 24));
  __ ubfx(R12, R7, 16, 8);
  __ ldrb (R1, Address(R10, R12));
  __ orr(R0, R1, AsmOperand(R0, lsl, 8));
  __ ubfx(R12, R6, 8, 8);
  __ ldrb(R2, Address(R10, R12));
  __ orr(R0, R2, AsmOperand(R0, lsl, 8));
  __ uxtb(R12, R5);
  __ ldrb(R3, Address(R10, R12));
  __ orr(R0, R3, AsmOperand(R0, lsl, 8));
  __ str(R0, Address(LR, 4, post_indexed));


  // preserve current scratch buffer pointer
  __ cmp(R11, LR);
  __ str(LR, block_current_output_buffer);

  // go to the next block processing
  __ b(block_start, ne);



  // Perform last round AddRoundKey state on all 8 blocks

  // load key pointer (remember that [sp+24]  points to a byte #32 at the key array)
  // last round is processed with the key[0 ..3]
  __ ldr(LR, rounds_key);

  // retireve original output buffer pointer
  __ ldr(R1, block_current_output_buffer);
  __ sub(R1, R1, 128);
  __ mov(R5, R1);


  // retrieve original cipher (source) pointer
  __ ldr(R0, original_src);

  // retrieve IV (second argument on stack)
  __ ldr(R6, iv);

  __ vld1(D20, R6, MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ vrev(D20, D20, quad, 32, MacroAssembler::VELEM_SIZE_8);

  // perform last AddRoundKey and IV addition
  __ vld1(D18, Address(LR, 0, post_indexed), MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D20, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);


  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D0, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D2, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D4, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D6, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D8, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D10, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);

  __ vld1(D22, Address(R1, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);
  __ veor(D22, D22, D18, quad);
  __ veor(D22, D22, D12, quad);
  __ vrev(D22, D22, quad, 32, MacroAssembler::VELEM_SIZE_8);
  __ vst1(D22, Address(R5, 0, post_indexed),  MacroAssembler::VELEM_SIZE_8, MacroAssembler::VLD1_TYPE_2_REGS);


  // check if we're done
  __ ldr(R4, output_buffer_end);
  __ cmp(R4, R1);
  __ add(R0, R0, 128-16);
  __ str(R0, iv);
  __ add(R0, R0, 16);

  __ b(decrypt_8_blocks, ne);

  __ add(SP, SP, 40);
  __ fpop(FloatRegisterSet(D8, 8));
  }

  __ bind(cbc_done);
  __ pop(RegisterSet(R4, R12) | LR);
  __ ldr(R0, Address(SP));
  __ bx(LR);

  return start;
}
#endif // USE_CRYPTO
