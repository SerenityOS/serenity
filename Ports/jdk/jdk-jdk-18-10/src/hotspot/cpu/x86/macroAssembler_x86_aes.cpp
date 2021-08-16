/*
* Copyright (c) 2019, Intel Corporation.
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
#include "runtime/stubRoutines.hpp"
#include "macroAssembler_x86.hpp"

#ifdef _LP64

void MacroAssembler::roundEnc(XMMRegister key, int rnum) {
    for (int xmm_reg_no = 0; xmm_reg_no <=rnum; xmm_reg_no++) {
      vaesenc(as_XMMRegister(xmm_reg_no), as_XMMRegister(xmm_reg_no), key, Assembler::AVX_512bit);
    }
}

void MacroAssembler::lastroundEnc(XMMRegister key, int rnum) {
    for (int xmm_reg_no = 0; xmm_reg_no <=rnum; xmm_reg_no++) {
      vaesenclast(as_XMMRegister(xmm_reg_no), as_XMMRegister(xmm_reg_no), key, Assembler::AVX_512bit);
    }
}

void MacroAssembler::roundDec(XMMRegister key, int rnum) {
    for (int xmm_reg_no = 0; xmm_reg_no <=rnum; xmm_reg_no++) {
      vaesdec(as_XMMRegister(xmm_reg_no), as_XMMRegister(xmm_reg_no), key, Assembler::AVX_512bit);
    }
}

void MacroAssembler::lastroundDec(XMMRegister key, int rnum) {
    for (int xmm_reg_no = 0; xmm_reg_no <=rnum; xmm_reg_no++) {
      vaesdeclast(as_XMMRegister(xmm_reg_no), as_XMMRegister(xmm_reg_no), key, Assembler::AVX_512bit);
    }
}

// Load key and shuffle operation
void MacroAssembler::ev_load_key(XMMRegister xmmdst, Register key, int offset, XMMRegister xmm_shuf_mask=NULL) {
    movdqu(xmmdst, Address(key, offset));
    if (xmm_shuf_mask != NULL) {
        pshufb(xmmdst, xmm_shuf_mask);
    } else {
       pshufb(xmmdst, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    }
   evshufi64x2(xmmdst, xmmdst, xmmdst, 0x0, Assembler::AVX_512bit);
}

// AES-ECB Encrypt Operation
void MacroAssembler::aesecb_encrypt(Register src_addr, Register dest_addr, Register key, Register len) {

    const Register pos = rax;
    const Register rounds = r12;

    Label NO_PARTS, LOOP, Loop_start, LOOP2, AES192, END_LOOP, AES256, REMAINDER, LAST2, END, KEY_192, KEY_256, EXIT;
    push(r13);
    push(r12);

    // For EVEX with VL and BW, provide a standard mask, VL = 128 will guide the merge
    // context for the registers used, where all instructions below are using 128-bit mode
    // On EVEX without VL and BW, these instructions will all be AVX.
    if (VM_Version::supports_avx512vlbw()) {
       movl(rax, 0xffff);
       kmovql(k1, rax);
    }
    push(len); // Save
    push(rbx);

    vzeroupper();

    xorptr(pos, pos);

    // Calculate number of rounds based on key length(128, 192, 256):44 for 10-rounds, 52 for 12-rounds, 60 for 14-rounds
    movl(rounds, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    // Load Key shuf mask
    const XMMRegister xmm_key_shuf_mask = xmm31;  // used temporarily to swap key bytes up front
    movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));

    // Load and shuffle key based on number of rounds
    ev_load_key(xmm8, key, 0 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm9, key, 1 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm10, key, 2 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm23, key, 3 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm12, key, 4 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm13, key, 5 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm14, key, 6 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm15, key, 7 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm16, key, 8 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm17, key, 9 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm24, key, 10 * 16, xmm_key_shuf_mask);
    cmpl(rounds, 52);
    jcc(Assembler::greaterEqual, KEY_192);
    jmp(Loop_start);

    bind(KEY_192);
    ev_load_key(xmm19, key, 11 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm20, key, 12 * 16, xmm_key_shuf_mask);
    cmpl(rounds, 60);
    jcc(Assembler::equal, KEY_256);
    jmp(Loop_start);

    bind(KEY_256);
    ev_load_key(xmm21, key, 13 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm22, key, 14 * 16, xmm_key_shuf_mask);

    bind(Loop_start);
    movq(rbx, len);
    // Divide length by 16 to convert it to number of blocks
    shrq(len, 4);
    shlq(rbx, 60);
    jcc(Assembler::equal, NO_PARTS);
    addq(len, 1);
    // Check if number of blocks is greater than or equal to 32
    // If true, 512 bytes are processed at a time (code marked by label LOOP)
    // If not, 16 bytes are processed (code marked by REMAINDER label)
    bind(NO_PARTS);
    movq(rbx, len);
    shrq(len, 5);
    jcc(Assembler::equal, REMAINDER);
    movl(r13, len);
    // Compute number of blocks that will be processed 512 bytes at a time
    // Subtract this from the total number of blocks which will then be processed by REMAINDER loop
    shlq(r13, 5);
    subq(rbx, r13);
    //Begin processing 512 bytes
    bind(LOOP);
    // Move 64 bytes of PT data into a zmm register, as a result 512 bytes of PT loaded in zmm0-7
    evmovdquq(xmm0, Address(src_addr, pos, Address::times_1, 0 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm1, Address(src_addr, pos, Address::times_1, 1 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm2, Address(src_addr, pos, Address::times_1, 2 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm3, Address(src_addr, pos, Address::times_1, 3 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm4, Address(src_addr, pos, Address::times_1, 4 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm5, Address(src_addr, pos, Address::times_1, 5 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm6, Address(src_addr, pos, Address::times_1, 6 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm7, Address(src_addr, pos, Address::times_1, 7 * 64), Assembler::AVX_512bit);
    // Xor with the first round key
    evpxorq(xmm0, xmm0, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm2, xmm2, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm3, xmm3, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm4, xmm4, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm5, xmm5, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm6, xmm6, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm7, xmm7, xmm8, Assembler::AVX_512bit);
    // 9 Aes encode round operations
    roundEnc(xmm9,  7);
    roundEnc(xmm10, 7);
    roundEnc(xmm23, 7);
    roundEnc(xmm12, 7);
    roundEnc(xmm13, 7);
    roundEnc(xmm14, 7);
    roundEnc(xmm15, 7);
    roundEnc(xmm16, 7);
    roundEnc(xmm17, 7);
    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192);
    // Aesenclast round operation for keysize = 128
    lastroundEnc(xmm24, 7);
    jmp(END_LOOP);
    //Additional 2 rounds of Aesenc operation for keysize = 192
    bind(AES192);
    roundEnc(xmm24, 7);
    roundEnc(xmm19, 7);
    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256);
    // Aesenclast round for keysize = 192
    lastroundEnc(xmm20, 7);
    jmp(END_LOOP);
    // 2 rounds of Aesenc operation and Aesenclast for keysize = 256
    bind(AES256);
    roundEnc(xmm20, 7);
    roundEnc(xmm21, 7);
    lastroundEnc(xmm22, 7);

    bind(END_LOOP);
    // Move 512 bytes of CT to destination
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0 * 64), xmm0, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 1 * 64), xmm1, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 2 * 64), xmm2, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 3 * 64), xmm3, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 4 * 64), xmm4, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 5 * 64), xmm5, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 6 * 64), xmm6, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 7 * 64), xmm7, Assembler::AVX_512bit);

    addq(pos, 512);
    decq(len);
    jcc(Assembler::notEqual, LOOP);

    bind(REMAINDER);
    vzeroupper();
    cmpq(rbx, 0);
    jcc(Assembler::equal, END);
    // Process 16 bytes at a time
    bind(LOOP2);
    movdqu(xmm1, Address(src_addr, pos, Address::times_1, 0));
    vpxor(xmm1, xmm1, xmm8, Assembler::AVX_128bit);
    // xmm2 contains shuffled key for Aesenclast operation.
    vmovdqu(xmm2, xmm24);

    vaesenc(xmm1, xmm1, xmm9, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm10, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm23, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm12, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm13, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm14, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm15, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm16, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm17, Assembler::AVX_128bit);

    cmpl(rounds, 52);
    jcc(Assembler::below, LAST2);
    vmovdqu(xmm2, xmm20);
    vaesenc(xmm1, xmm1, xmm24, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm19, Assembler::AVX_128bit);
    cmpl(rounds, 60);
    jcc(Assembler::below, LAST2);
    vmovdqu(xmm2, xmm22);
    vaesenc(xmm1, xmm1, xmm20, Assembler::AVX_128bit);
    vaesenc(xmm1, xmm1, xmm21, Assembler::AVX_128bit);

    bind(LAST2);
    // Aesenclast round
    vaesenclast(xmm1, xmm1, xmm2, Assembler::AVX_128bit);
    // Write 16 bytes of CT to destination
    movdqu(Address(dest_addr, pos, Address::times_1, 0), xmm1);
    addq(pos, 16);
    decq(rbx);
    jcc(Assembler::notEqual, LOOP2);

    bind(END);
    // Zero out the round keys
    evpxorq(xmm8, xmm8, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm9, xmm9, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm10, xmm10, xmm10, Assembler::AVX_512bit);
    evpxorq(xmm23, xmm23, xmm23, Assembler::AVX_512bit);
    evpxorq(xmm12, xmm12, xmm12, Assembler::AVX_512bit);
    evpxorq(xmm13, xmm13, xmm13, Assembler::AVX_512bit);
    evpxorq(xmm14, xmm14, xmm14, Assembler::AVX_512bit);
    evpxorq(xmm15, xmm15, xmm15, Assembler::AVX_512bit);
    evpxorq(xmm16, xmm16, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm17, xmm17, xmm17, Assembler::AVX_512bit);
    evpxorq(xmm24, xmm24, xmm24, Assembler::AVX_512bit);
    cmpl(rounds, 44);
    jcc(Assembler::belowEqual, EXIT);
    evpxorq(xmm19, xmm19, xmm19, Assembler::AVX_512bit);
    evpxorq(xmm20, xmm20, xmm20, Assembler::AVX_512bit);
    cmpl(rounds, 52);
    jcc(Assembler::belowEqual, EXIT);
    evpxorq(xmm21, xmm21, xmm21, Assembler::AVX_512bit);
    evpxorq(xmm22, xmm22, xmm22, Assembler::AVX_512bit);
    bind(EXIT);
    pop(rbx);
    pop(rax); // return length
    pop(r12);
    pop(r13);
}

// AES-ECB Decrypt Operation
void MacroAssembler::aesecb_decrypt(Register src_addr, Register dest_addr, Register key, Register len)  {

    Label NO_PARTS, LOOP, Loop_start, LOOP2, AES192, END_LOOP, AES256, REMAINDER, LAST2, END, KEY_192, KEY_256, EXIT;
    const Register pos = rax;
    const Register rounds = r12;
    push(r13);
    push(r12);

    // For EVEX with VL and BW, provide a standard mask, VL = 128 will guide the merge
    // context for the registers used, where all instructions below are using 128-bit mode
    // On EVEX without VL and BW, these instructions will all be AVX.
    if (VM_Version::supports_avx512vlbw()) {
       movl(rax, 0xffff);
       kmovql(k1, rax);
    }

    push(len); // Save
    push(rbx);

    vzeroupper();

    xorptr(pos, pos);
    // Calculate number of rounds i.e. based on key length(128, 192, 256):44 for 10-rounds, 52 for 12-rounds, 60 for 14-rounds
    movl(rounds, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    // Load Key shuf mask
    const XMMRegister xmm_key_shuf_mask = xmm31;  // used temporarily to swap key bytes up front
    movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));

    // Load and shuffle round keys. The java expanded key ordering is rotated one position in decryption.
    // So the first round key is loaded from 1*16 here and last round key is loaded from 0*16
    ev_load_key(xmm9,  key, 1 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm10, key, 2 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm11, key, 3 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm12, key, 4 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm13, key, 5 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm14, key, 6 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm15, key, 7 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm16, key, 8 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm17, key, 9 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm18, key, 10 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm27, key, 0 * 16, xmm_key_shuf_mask);
    cmpl(rounds, 52);
    jcc(Assembler::greaterEqual, KEY_192);
    jmp(Loop_start);

    bind(KEY_192);
    ev_load_key(xmm19, key, 11 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm20, key, 12 * 16, xmm_key_shuf_mask);
    cmpl(rounds, 60);
    jcc(Assembler::equal, KEY_256);
    jmp(Loop_start);

    bind(KEY_256);
    ev_load_key(xmm21, key, 13 * 16, xmm_key_shuf_mask);
    ev_load_key(xmm22, key, 14 * 16, xmm_key_shuf_mask);
    bind(Loop_start);
    movq(rbx, len);
    // Convert input length to number of blocks
    shrq(len, 4);
    shlq(rbx, 60);
    jcc(Assembler::equal, NO_PARTS);
    addq(len, 1);
    // Check if number of blocks is greater than/ equal to 32
    // If true, blocks then 512 bytes are processed at a time (code marked by label LOOP)
    // If not, 16 bytes are processed (code marked by label REMAINDER)
    bind(NO_PARTS);
    movq(rbx, len);
    shrq(len, 5);
    jcc(Assembler::equal, REMAINDER);
    movl(r13, len);
    // Compute number of blocks that will be processed as 512 bytes at a time
    // Subtract this from the total number of blocks, which will then be processed by REMAINDER loop.
    shlq(r13, 5);
    subq(rbx, r13);

    bind(LOOP);
    // Move 64 bytes of CT data into a zmm register, as a result 512 bytes of CT loaded in zmm0-7
    evmovdquq(xmm0, Address(src_addr, pos, Address::times_1, 0 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm1, Address(src_addr, pos, Address::times_1, 1 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm2, Address(src_addr, pos, Address::times_1, 2 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm3, Address(src_addr, pos, Address::times_1, 3 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm4, Address(src_addr, pos, Address::times_1, 4 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm5, Address(src_addr, pos, Address::times_1, 5 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm6, Address(src_addr, pos, Address::times_1, 6 * 64), Assembler::AVX_512bit);
    evmovdquq(xmm7, Address(src_addr, pos, Address::times_1, 7 * 64), Assembler::AVX_512bit);
    // Xor with the first round key
    evpxorq(xmm0, xmm0, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm2, xmm2, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm3, xmm3, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm4, xmm4, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm5, xmm5, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm6, xmm6, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm7, xmm7, xmm9, Assembler::AVX_512bit);
    // 9 rounds of Aesdec
    roundDec(xmm10, 7);
    roundDec(xmm11, 7);
    roundDec(xmm12, 7);
    roundDec(xmm13, 7);
    roundDec(xmm14, 7);
    roundDec(xmm15, 7);
    roundDec(xmm16, 7);
    roundDec(xmm17, 7);
    roundDec(xmm18, 7);
    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192);
    // Aesdeclast round for keysize = 128
    lastroundDec(xmm27, 7);
    jmp(END_LOOP);

    bind(AES192);
    // 2 Additional rounds for keysize = 192
    roundDec(xmm19, 7);
    roundDec(xmm20, 7);
    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256);
    // Aesdeclast round for keysize = 192
    lastroundDec(xmm27, 7);
    jmp(END_LOOP);
    bind(AES256);
    // 2 Additional rounds and Aesdeclast for keysize = 256
    roundDec(xmm21, 7);
    roundDec(xmm22, 7);
    lastroundDec(xmm27, 7);

    bind(END_LOOP);
    // Write 512 bytes of PT to the destination
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0 * 64), xmm0, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 1 * 64), xmm1, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 2 * 64), xmm2, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 3 * 64), xmm3, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 4 * 64), xmm4, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 5 * 64), xmm5, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 6 * 64), xmm6, Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 7 * 64), xmm7, Assembler::AVX_512bit);

    addq(pos, 512);
    decq(len);
    jcc(Assembler::notEqual, LOOP);

    bind(REMAINDER);
    vzeroupper();
    cmpq(rbx, 0);
    jcc(Assembler::equal, END);
    // Process 16 bytes at a time
    bind(LOOP2);
    movdqu(xmm1, Address(src_addr, pos, Address::times_1, 0));
    vpxor(xmm1, xmm1, xmm9, Assembler::AVX_128bit);
    // xmm2 contains shuffled key for Aesdeclast operation.
    vmovdqu(xmm2, xmm27);

    vaesdec(xmm1, xmm1, xmm10, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm11, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm12, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm13, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm14, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm15, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm16, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm17, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm18, Assembler::AVX_128bit);

    cmpl(rounds, 52);
    jcc(Assembler::below, LAST2);
    vaesdec(xmm1, xmm1, xmm19, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm20, Assembler::AVX_128bit);
    cmpl(rounds, 60);
    jcc(Assembler::below, LAST2);
    vaesdec(xmm1, xmm1, xmm21, Assembler::AVX_128bit);
    vaesdec(xmm1, xmm1, xmm22, Assembler::AVX_128bit);

    bind(LAST2);
    // Aesdeclast round
    vaesdeclast(xmm1, xmm1, xmm2, Assembler::AVX_128bit);
    // Write 16 bytes of PT to destination
    movdqu(Address(dest_addr, pos, Address::times_1, 0), xmm1);
    addq(pos, 16);
    decq(rbx);
    jcc(Assembler::notEqual, LOOP2);

    bind(END);
    // Zero out the round keys
    evpxorq(xmm8, xmm8, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm9, xmm9, xmm9, Assembler::AVX_512bit);
    evpxorq(xmm10, xmm10, xmm10, Assembler::AVX_512bit);
    evpxorq(xmm11, xmm11, xmm11, Assembler::AVX_512bit);
    evpxorq(xmm12, xmm12, xmm12, Assembler::AVX_512bit);
    evpxorq(xmm13, xmm13, xmm13, Assembler::AVX_512bit);
    evpxorq(xmm14, xmm14, xmm14, Assembler::AVX_512bit);
    evpxorq(xmm15, xmm15, xmm15, Assembler::AVX_512bit);
    evpxorq(xmm16, xmm16, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm17, xmm17, xmm17, Assembler::AVX_512bit);
    evpxorq(xmm18, xmm18, xmm18, Assembler::AVX_512bit);
    evpxorq(xmm27, xmm27, xmm27, Assembler::AVX_512bit);
    cmpl(rounds, 44);
    jcc(Assembler::belowEqual, EXIT);
    evpxorq(xmm19, xmm19, xmm19, Assembler::AVX_512bit);
    evpxorq(xmm20, xmm20, xmm20, Assembler::AVX_512bit);
    cmpl(rounds, 52);
    jcc(Assembler::belowEqual, EXIT);
    evpxorq(xmm21, xmm21, xmm21, Assembler::AVX_512bit);
    evpxorq(xmm22, xmm22, xmm22, Assembler::AVX_512bit);
    bind(EXIT);
    pop(rbx);
    pop(rax); // return length
    pop(r12);
    pop(r13);
}

// Multiply 128 x 128 bits, using 4 pclmulqdq operations
void MacroAssembler::schoolbookAAD(int i, Register htbl, XMMRegister data,
    XMMRegister tmp0, XMMRegister tmp1, XMMRegister tmp2, XMMRegister tmp3) {
    movdqu(xmm15, Address(htbl, i * 16));
    vpclmulhqlqdq(tmp3, data, xmm15); // 0x01
    vpxor(tmp2, tmp2, tmp3, Assembler::AVX_128bit);
    vpclmulldq(tmp3, data, xmm15); // 0x00
    vpxor(tmp0, tmp0, tmp3, Assembler::AVX_128bit);
    vpclmulhdq(tmp3, data, xmm15); // 0x11
    vpxor(tmp1, tmp1, tmp3, Assembler::AVX_128bit);
    vpclmullqhqdq(tmp3, data, xmm15); // 0x10
    vpxor(tmp2, tmp2, tmp3, Assembler::AVX_128bit);
}

// Multiply two 128 bit numbers resulting in a 256 bit value
// Result of the multiplication followed by reduction stored in state
void MacroAssembler::gfmul(XMMRegister tmp0, XMMRegister state) {
    const XMMRegister tmp1 = xmm4;
    const XMMRegister tmp2 = xmm5;
    const XMMRegister tmp3 = xmm6;
    const XMMRegister tmp4 = xmm7;

    vpclmulldq(tmp1, state, tmp0); //0x00  (a0 * b0)
    vpclmulhdq(tmp4, state, tmp0);//0x11 (a1 * b1)
    vpclmullqhqdq(tmp2, state, tmp0);//0x10 (a1 * b0)
    vpclmulhqlqdq(tmp3, state, tmp0); //0x01 (a0 * b1)

    vpxor(tmp2, tmp2, tmp3, Assembler::AVX_128bit); // (a0 * b1) + (a1 * b0)

    vpslldq(tmp3, tmp2, 8, Assembler::AVX_128bit);
    vpsrldq(tmp2, tmp2, 8, Assembler::AVX_128bit);
    vpxor(tmp1, tmp1, tmp3, Assembler::AVX_128bit); // tmp1 and tmp4 hold the result
    vpxor(tmp4, tmp4, tmp2, Assembler::AVX_128bit); // of carryless multiplication
    // Follows the reduction technique mentioned in
    // Shift-XOR reduction described in Gueron-Kounavis May 2010
    // First phase of reduction
    //
    vpslld(xmm8, tmp1, 31, Assembler::AVX_128bit); // packed right shift shifting << 31
    vpslld(xmm9, tmp1, 30, Assembler::AVX_128bit); // packed right shift shifting << 30
    vpslld(xmm10, tmp1, 25, Assembler::AVX_128bit);// packed right shift shifting << 25
    // xor the shifted versions
    vpxor(xmm8, xmm8, xmm9, Assembler::AVX_128bit);
    vpxor(xmm8, xmm8, xmm10, Assembler::AVX_128bit);
    vpslldq(xmm9, xmm8, 12, Assembler::AVX_128bit);
    vpsrldq(xmm8, xmm8, 4, Assembler::AVX_128bit);
    vpxor(tmp1, tmp1, xmm9, Assembler::AVX_128bit);// first phase of the reduction complete
    //
    // Second phase of the reduction
    //
    vpsrld(xmm9, tmp1, 1, Assembler::AVX_128bit);// packed left shifting >> 1
    vpsrld(xmm10, tmp1, 2, Assembler::AVX_128bit);// packed left shifting >> 2
    vpsrld(xmm11, tmp1, 7, Assembler::AVX_128bit);// packed left shifting >> 7
    vpxor(xmm9, xmm9, xmm10, Assembler::AVX_128bit);// xor the shifted versions
    vpxor(xmm9, xmm9, xmm11, Assembler::AVX_128bit);
    vpxor(xmm9, xmm9, xmm8, Assembler::AVX_128bit);
    vpxor(tmp1, tmp1, xmm9, Assembler::AVX_128bit);
    vpxor(state, tmp4, tmp1, Assembler::AVX_128bit);// the result is in state
    ret(0);
}

// This method takes the subkey after expansion as input and generates 1 * 16 power of subkey H.
// The power of H is used in reduction process for one block ghash
void MacroAssembler::generateHtbl_one_block(Register htbl) {
    const XMMRegister t = xmm13;

    // load the original subkey hash
    movdqu(t, Address(htbl, 0));
    // shuffle using long swap mask
    movdqu(xmm10, ExternalAddress(StubRoutines::x86::ghash_long_swap_mask_addr()));
    vpshufb(t, t, xmm10, Assembler::AVX_128bit);

    // Compute H' = GFMUL(H, 2)
    vpsrld(xmm3, t, 7, Assembler::AVX_128bit);
    movdqu(xmm4, ExternalAddress(StubRoutines::x86::ghash_shufflemask_addr()));
    vpshufb(xmm3, xmm3, xmm4, Assembler::AVX_128bit);
    movl(rax, 0xff00);
    movdl(xmm4, rax);
    vpshufb(xmm4, xmm4, xmm3, Assembler::AVX_128bit);
    movdqu(xmm5, ExternalAddress(StubRoutines::x86::ghash_polynomial_addr()));
    vpand(xmm5, xmm5, xmm4, Assembler::AVX_128bit);
    vpsrld(xmm3, t, 31, Assembler::AVX_128bit);
    vpslld(xmm4, t, 1, Assembler::AVX_128bit);
    vpslldq(xmm3, xmm3, 4, Assembler::AVX_128bit);
    vpxor(t, xmm4, xmm3, Assembler::AVX_128bit);// t holds p(x) <<1 or H * 2

    //Adding p(x)<<1 to xmm5 which holds the reduction polynomial
    vpxor(t, t, xmm5, Assembler::AVX_128bit);
    movdqu(Address(htbl, 1 * 16), t); // H * 2

    ret(0);
}

// This method takes the subkey after expansion as input and generates the remaining powers of subkey H.
// The power of H is used in reduction process for eight block ghash
void MacroAssembler::generateHtbl_eight_blocks(Register htbl) {
    const XMMRegister t = xmm13;
    const XMMRegister tmp0 = xmm1;
    Label GFMUL;

    movdqu(t, Address(htbl, 1 * 16));
    movdqu(tmp0, t);

    // tmp0 and t hold H. Now we compute powers of H by using GFMUL(H, H)
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 2 * 16), t); //H ^ 2 * 2
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 3 * 16), t); //H ^ 3 * 2
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 4 * 16), t); //H ^ 4 * 2
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 5 * 16), t); //H ^ 5 * 2
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 6 * 16), t); //H ^ 6 * 2
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 7 * 16), t); //H ^ 7 * 2
    call(GFMUL, relocInfo::none);
    movdqu(Address(htbl, 8 * 16), t); //H ^ 8 * 2
    ret(0);

    bind(GFMUL);
    gfmul(tmp0, t);
}

// Multiblock and single block GHASH computation using Shift XOR reduction technique
void MacroAssembler::avx_ghash(Register input_state, Register htbl,
    Register input_data, Register blocks) {

    // temporary variables to hold input data and input state
    const XMMRegister data = xmm1;
    const XMMRegister state = xmm0;
    // temporary variables to hold intermediate results
    const XMMRegister tmp0 = xmm3;
    const XMMRegister tmp1 = xmm4;
    const XMMRegister tmp2 = xmm5;
    const XMMRegister tmp3 = xmm6;
    // temporary variables to hold byte and long swap masks
    const XMMRegister bswap_mask = xmm2;
    const XMMRegister lswap_mask = xmm14;

    Label GENERATE_HTBL_1_BLK, GENERATE_HTBL_8_BLKS, BEGIN_PROCESS, GFMUL, BLOCK8_REDUCTION,
          ONE_BLK_INIT, PROCESS_1_BLOCK, PROCESS_8_BLOCKS, SAVE_STATE, EXIT_GHASH;

    testptr(blocks, blocks);
    jcc(Assembler::zero, EXIT_GHASH);

    // Check if Hashtable (1*16) has been already generated
    // For anything less than 8 blocks, we generate only the first power of H.
    movdqu(tmp2, Address(htbl, 1 * 16));
    ptest(tmp2, tmp2);
    jcc(Assembler::notZero, BEGIN_PROCESS);
    call(GENERATE_HTBL_1_BLK, relocInfo::none);

    // Shuffle the input state
    bind(BEGIN_PROCESS);
    movdqu(lswap_mask, ExternalAddress(StubRoutines::x86::ghash_long_swap_mask_addr()));
    movdqu(state, Address(input_state, 0));
    vpshufb(state, state, lswap_mask, Assembler::AVX_128bit);

    cmpl(blocks, 8);
    jcc(Assembler::below, ONE_BLK_INIT);
    // If we have 8 blocks or more data, then generate remaining powers of H
    movdqu(tmp2, Address(htbl, 8 * 16));
    ptest(tmp2, tmp2);
    jcc(Assembler::notZero, PROCESS_8_BLOCKS);
    call(GENERATE_HTBL_8_BLKS, relocInfo::none);

    //Do 8 multiplies followed by a reduction processing 8 blocks of data at a time
    //Each block = 16 bytes.
    bind(PROCESS_8_BLOCKS);
    subl(blocks, 8);
    movdqu(bswap_mask, ExternalAddress(StubRoutines::x86::ghash_byte_swap_mask_addr()));
    movdqu(data, Address(input_data, 16 * 7));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    //Loading 1*16 as calculated powers of H required starts at that location.
    movdqu(xmm15, Address(htbl, 1 * 16));
    //Perform carryless multiplication of (H*2, data block #7)
    vpclmulhqlqdq(tmp2, data, xmm15);//a0 * b1
    vpclmulldq(tmp0, data, xmm15);//a0 * b0
    vpclmulhdq(tmp1, data, xmm15);//a1 * b1
    vpclmullqhqdq(tmp3, data, xmm15);//a1* b0
    vpxor(tmp2, tmp2, tmp3, Assembler::AVX_128bit);// (a0 * b1) + (a1 * b0)

    movdqu(data, Address(input_data, 16 * 6));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^2 * 2, data block #6)
    schoolbookAAD(2, htbl, data, tmp0, tmp1, tmp2, tmp3);

    movdqu(data, Address(input_data, 16 * 5));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^3 * 2, data block #5)
    schoolbookAAD(3, htbl, data, tmp0, tmp1, tmp2, tmp3);
    movdqu(data, Address(input_data, 16 * 4));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^4 * 2, data block #4)
    schoolbookAAD(4, htbl, data, tmp0, tmp1, tmp2, tmp3);
    movdqu(data, Address(input_data, 16 * 3));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^5 * 2, data block #3)
    schoolbookAAD(5, htbl, data, tmp0, tmp1, tmp2, tmp3);
    movdqu(data, Address(input_data, 16 * 2));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^6 * 2, data block #2)
    schoolbookAAD(6, htbl, data, tmp0, tmp1, tmp2, tmp3);
    movdqu(data, Address(input_data, 16 * 1));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^7 * 2, data block #1)
    schoolbookAAD(7, htbl, data, tmp0, tmp1, tmp2, tmp3);
    movdqu(data, Address(input_data, 16 * 0));
    // xor data block#0 with input state before perfoming carry-less multiplication
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    vpxor(data, data, state, Assembler::AVX_128bit);
    // Perform carryless multiplication of (H^8 * 2, data block #0)
    schoolbookAAD(8, htbl, data, tmp0, tmp1, tmp2, tmp3);
    vpslldq(tmp3, tmp2, 8, Assembler::AVX_128bit);
    vpsrldq(tmp2, tmp2, 8, Assembler::AVX_128bit);
    vpxor(tmp0, tmp0, tmp3, Assembler::AVX_128bit);// tmp0, tmp1 contains aggregated results of
    vpxor(tmp1, tmp1, tmp2, Assembler::AVX_128bit);// the multiplication operation

    // we have the 2 128-bit partially accumulated multiplication results in tmp0:tmp1
    // with higher 128-bit in tmp1 and lower 128-bit in corresponding tmp0
    // Follows the reduction technique mentioned in
    // Shift-XOR reduction described in Gueron-Kounavis May 2010
    bind(BLOCK8_REDUCTION);
    // First Phase of the reduction
    vpslld(xmm8, tmp0, 31, Assembler::AVX_128bit); // packed right shifting << 31
    vpslld(xmm9, tmp0, 30, Assembler::AVX_128bit); // packed right shifting << 30
    vpslld(xmm10, tmp0, 25, Assembler::AVX_128bit); // packed right shifting << 25
    // xor the shifted versions
    vpxor(xmm8, xmm8, xmm10, Assembler::AVX_128bit);
    vpxor(xmm8, xmm8, xmm9, Assembler::AVX_128bit);

    vpslldq(xmm9, xmm8, 12, Assembler::AVX_128bit);
    vpsrldq(xmm8, xmm8, 4, Assembler::AVX_128bit);

    vpxor(tmp0, tmp0, xmm9, Assembler::AVX_128bit); // first phase of reduction is complete
    // second phase of the reduction
    vpsrld(xmm9, tmp0, 1, Assembler::AVX_128bit); // packed left shifting >> 1
    vpsrld(xmm10, tmp0, 2, Assembler::AVX_128bit); // packed left shifting >> 2
    vpsrld(tmp2, tmp0, 7, Assembler::AVX_128bit); // packed left shifting >> 7
    // xor the shifted versions
    vpxor(xmm9, xmm9, xmm10, Assembler::AVX_128bit);
    vpxor(xmm9, xmm9, tmp2, Assembler::AVX_128bit);
    vpxor(xmm9, xmm9, xmm8, Assembler::AVX_128bit);
    vpxor(tmp0, xmm9, tmp0, Assembler::AVX_128bit);
    // Final result is in state
    vpxor(state, tmp0, tmp1, Assembler::AVX_128bit);

    lea(input_data, Address(input_data, 16 * 8));
    cmpl(blocks, 8);
    jcc(Assembler::below, ONE_BLK_INIT);
    jmp(PROCESS_8_BLOCKS);

    // Since this is one block operation we will only use H * 2 i.e. the first power of H
    bind(ONE_BLK_INIT);
    movdqu(tmp0, Address(htbl, 1 * 16));
    movdqu(bswap_mask, ExternalAddress(StubRoutines::x86::ghash_byte_swap_mask_addr()));

    //Do one (128 bit x 128 bit) carry-less multiplication at a time followed by a reduction.
    bind(PROCESS_1_BLOCK);
    cmpl(blocks, 0);
    jcc(Assembler::equal, SAVE_STATE);
    subl(blocks, 1);
    movdqu(data, Address(input_data, 0));
    vpshufb(data, data, bswap_mask, Assembler::AVX_128bit);
    vpxor(state, state, data, Assembler::AVX_128bit);
    // gfmul(H*2, state)
    call(GFMUL, relocInfo::none);
    addptr(input_data, 16);
    jmp(PROCESS_1_BLOCK);

    bind(SAVE_STATE);
    vpshufb(state, state, lswap_mask, Assembler::AVX_128bit);
    movdqu(Address(input_state, 0), state);
    jmp(EXIT_GHASH);

    bind(GFMUL);
    gfmul(tmp0, state);

    bind(GENERATE_HTBL_1_BLK);
    generateHtbl_one_block(htbl);

    bind(GENERATE_HTBL_8_BLKS);
    generateHtbl_eight_blocks(htbl);

    bind(EXIT_GHASH);
    // zero out xmm registers used for Htbl storage
    vpxor(xmm0, xmm0, xmm0, Assembler::AVX_128bit);
    vpxor(xmm1, xmm1, xmm1, Assembler::AVX_128bit);
    vpxor(xmm3, xmm3, xmm3, Assembler::AVX_128bit);
    vpxor(xmm15, xmm15, xmm15, Assembler::AVX_128bit);
}

// AES Counter Mode using VAES instructions
void MacroAssembler::aesctr_encrypt(Register src_addr, Register dest_addr, Register key, Register counter,
    Register len_reg, Register used, Register used_addr, Register saved_encCounter_start) {

    const Register rounds = 0;
    const Register pos = r12;

    Label PRELOOP_START, EXIT_PRELOOP, REMAINDER, REMAINDER_16, LOOP, END, EXIT, END_LOOP,
    AES192, AES256, AES192_REMAINDER16, REMAINDER16_END_LOOP, AES256_REMAINDER16,
    REMAINDER_8, REMAINDER_4, AES192_REMAINDER8, REMAINDER_LOOP, AES256_REMINDER,
    AES192_REMAINDER, END_REMAINDER_LOOP, AES256_REMAINDER8, REMAINDER8_END_LOOP,
    AES192_REMAINDER4, AES256_REMAINDER4, AES256_REMAINDER, END_REMAINDER4, EXTRACT_TAILBYTES,
    EXTRACT_TAIL_4BYTES, EXTRACT_TAIL_2BYTES, EXTRACT_TAIL_1BYTE, STORE_CTR;

    cmpl(len_reg, 0);
    jcc(Assembler::belowEqual, EXIT);

    movl(pos, 0);
    // if the number of used encrypted counter bytes < 16,
    // XOR PT with saved encrypted counter to obtain CT
    bind(PRELOOP_START);
    cmpl(used, 16);
    jcc(Assembler::aboveEqual, EXIT_PRELOOP);
    movb(rbx, Address(saved_encCounter_start, used));
    xorb(rbx, Address(src_addr, pos));
    movb(Address(dest_addr, pos), rbx);
    addptr(pos, 1);
    addptr(used, 1);
    decrement(len_reg);
    jmp(PRELOOP_START);

    bind(EXIT_PRELOOP);
    movl(Address(used_addr, 0), used);

    // Calculate number of rounds i.e. 10, 12, 14,  based on key length(128, 192, 256).
    movl(rounds, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    vpxor(xmm0, xmm0, xmm0, Assembler::AVX_128bit);
    // Move initial counter value in xmm0
    movdqu(xmm0, Address(counter, 0));
    // broadcast counter value to zmm8
    evshufi64x2(xmm8, xmm0, xmm0, 0, Assembler::AVX_512bit);

    // load lbswap mask
    evmovdquq(xmm16, ExternalAddress(StubRoutines::x86::counter_mask_addr()), Assembler::AVX_512bit, r15);

    //shuffle counter using lbswap_mask
    vpshufb(xmm8, xmm8, xmm16, Assembler::AVX_512bit);

    // pre-increment and propagate counter values to zmm9-zmm15 registers.
    // Linc0 increments the zmm8 by 1 (initial value being 0), Linc4 increments the counters zmm9-zmm15 by 4
    // The counter is incremented after each block i.e. 16 bytes is processed;
    // each zmm register has 4 counter values as its MSB
    // the counters are incremented in parallel
    vpaddd(xmm8, xmm8, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 64), Assembler::AVX_512bit, r15);//linc0
    vpaddd(xmm9, xmm8, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//linc4(rip)
    vpaddd(xmm10, xmm9, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)
    vpaddd(xmm11, xmm10, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)
    vpaddd(xmm12, xmm11, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)
    vpaddd(xmm13, xmm12, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)
    vpaddd(xmm14, xmm13, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)
    vpaddd(xmm15, xmm14, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)

    // load linc32 mask in zmm register.linc32 increments counter by 32
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 256), Assembler::AVX_512bit, r15);//Linc32

    // xmm31 contains the key shuffle mask.
    movdqu(xmm31, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    // Load key function loads 128 bit key and shuffles it. Then we broadcast the shuffled key to convert it into a 512 bit value.
    // For broadcasting the values to ZMM, vshufi64 is used instead of evbroadcasti64x2 as the source in this case is ZMM register
    // that holds shuffled key value.
    ev_load_key(xmm20, key, 0, xmm31);
    ev_load_key(xmm21, key, 1 * 16, xmm31);
    ev_load_key(xmm22, key, 2 * 16, xmm31);
    ev_load_key(xmm23, key, 3 * 16, xmm31);
    ev_load_key(xmm24, key, 4 * 16, xmm31);
    ev_load_key(xmm25, key, 5 * 16, xmm31);
    ev_load_key(xmm26, key, 6 * 16, xmm31);
    ev_load_key(xmm27, key, 7 * 16, xmm31);
    ev_load_key(xmm28, key, 8 * 16, xmm31);
    ev_load_key(xmm29, key, 9 * 16, xmm31);
    ev_load_key(xmm30, key, 10 * 16, xmm31);

    // Process 32 blocks or 512 bytes of data
    bind(LOOP);
    cmpl(len_reg, 512);
    jcc(Assembler::less, REMAINDER);
    subq(len_reg, 512);
    //Shuffle counter and Exor it with roundkey1. Result is stored in zmm0-7
    vpshufb(xmm0, xmm8, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm0, xmm0, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm1, xmm9, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm2, xmm10, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm2, xmm2, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm3, xmm11, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm3, xmm3, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm4, xmm12, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm4, xmm4, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm5, xmm13, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm5, xmm5, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm6, xmm14, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm6, xmm6, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm7, xmm15, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm7, xmm7, xmm20, Assembler::AVX_512bit);
    // Perform AES encode operations and put results in zmm0-zmm7.
    // This is followed by incrementing counter values in zmm8-zmm15.
    // Since we will be processing 32 blocks at a time, the counter is incremented by 32.
    roundEnc(xmm21, 7);
    vpaddq(xmm8, xmm8, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm22, 7);
    vpaddq(xmm9, xmm9, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm23, 7);
    vpaddq(xmm10, xmm10, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm24, 7);
    vpaddq(xmm11, xmm11, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm25, 7);
    vpaddq(xmm12, xmm12, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm26, 7);
    vpaddq(xmm13, xmm13, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm27, 7);
    vpaddq(xmm14, xmm14, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm28, 7);
    vpaddq(xmm15, xmm15, xmm19, Assembler::AVX_512bit);
    roundEnc(xmm29, 7);

    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192);
    lastroundEnc(xmm30, 7);
    jmp(END_LOOP);

    bind(AES192);
    roundEnc(xmm30, 7);
    ev_load_key(xmm18, key, 11 * 16, xmm31);
    roundEnc(xmm18, 7);
    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256);
    ev_load_key(xmm18, key, 12 * 16, xmm31);
    lastroundEnc(xmm18, 7);
    jmp(END_LOOP);

    bind(AES256);
    ev_load_key(xmm18, key, 12 * 16, xmm31);
    roundEnc(xmm18, 7);
    ev_load_key(xmm18, key, 13 * 16, xmm31);
    roundEnc(xmm18, 7);
    ev_load_key(xmm18, key, 14 * 16, xmm31);
    lastroundEnc(xmm18, 7);

    // After AES encode rounds, the encrypted block cipher lies in zmm0-zmm7
    // xor encrypted block cipher and input plaintext and store resultant ciphertext
    bind(END_LOOP);
    evpxorq(xmm0, xmm0, Address(src_addr, pos, Address::times_1, 0 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0), xmm0, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, Address(src_addr, pos, Address::times_1, 1 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 64), xmm1, Assembler::AVX_512bit);
    evpxorq(xmm2, xmm2, Address(src_addr, pos, Address::times_1, 2 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 2 * 64), xmm2, Assembler::AVX_512bit);
    evpxorq(xmm3, xmm3, Address(src_addr, pos, Address::times_1, 3 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 3 * 64), xmm3, Assembler::AVX_512bit);
    evpxorq(xmm4, xmm4, Address(src_addr, pos, Address::times_1, 4 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 4 * 64), xmm4, Assembler::AVX_512bit);
    evpxorq(xmm5, xmm5, Address(src_addr, pos, Address::times_1, 5 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 5 * 64), xmm5, Assembler::AVX_512bit);
    evpxorq(xmm6, xmm6, Address(src_addr, pos, Address::times_1, 6 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 6 * 64), xmm6, Assembler::AVX_512bit);
    evpxorq(xmm7, xmm7, Address(src_addr, pos, Address::times_1, 7 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 7 * 64), xmm7, Assembler::AVX_512bit);
    addq(pos, 512);
    jmp(LOOP);

    // Encode 256, 128, 64 or 16 bytes at a time if length is less than 512 bytes
    bind(REMAINDER);
    cmpl(len_reg, 0);
    jcc(Assembler::equal, END);
    cmpl(len_reg, 256);
    jcc(Assembler::aboveEqual, REMAINDER_16);
    cmpl(len_reg, 128);
    jcc(Assembler::aboveEqual, REMAINDER_8);
    cmpl(len_reg, 64);
    jcc(Assembler::aboveEqual, REMAINDER_4);
    // At this point, we will process 16 bytes of data at a time.
    // So load xmm19 with counter increment value as 1
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 80), Assembler::AVX_128bit, r15);
    jmp(REMAINDER_LOOP);

    // Each ZMM register can be used to encode 64 bytes of data, so we have 4 ZMM registers to encode 256 bytes of data
    bind(REMAINDER_16);
    subq(len_reg, 256);
    // As we process 16 blocks at a time, load mask for incrementing the counter value by 16
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 320), Assembler::AVX_512bit, r15);//Linc16(rip)
    // shuffle counter and XOR counter with roundkey1
    vpshufb(xmm0, xmm8, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm0, xmm0, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm1, xmm9, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm2, xmm10, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm2, xmm2, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm3, xmm11, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm3, xmm3, xmm20, Assembler::AVX_512bit);
    // Increment counter values by 16
    vpaddq(xmm8, xmm8, xmm19, Assembler::AVX_512bit);
    vpaddq(xmm9, xmm9, xmm19, Assembler::AVX_512bit);
    // AES encode rounds
    roundEnc(xmm21, 3);
    roundEnc(xmm22, 3);
    roundEnc(xmm23, 3);
    roundEnc(xmm24, 3);
    roundEnc(xmm25, 3);
    roundEnc(xmm26, 3);
    roundEnc(xmm27, 3);
    roundEnc(xmm28, 3);
    roundEnc(xmm29, 3);

    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192_REMAINDER16);
    lastroundEnc(xmm30, 3);
    jmp(REMAINDER16_END_LOOP);

    bind(AES192_REMAINDER16);
    roundEnc(xmm30, 3);
    ev_load_key(xmm18, key, 11 * 16, xmm31);
    roundEnc(xmm18, 3);
    ev_load_key(xmm5, key, 12 * 16, xmm31);

    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256_REMAINDER16);
    lastroundEnc(xmm5, 3);
    jmp(REMAINDER16_END_LOOP);
    bind(AES256_REMAINDER16);
    roundEnc(xmm5, 3);
    ev_load_key(xmm6, key, 13 * 16, xmm31);
    roundEnc(xmm6, 3);
    ev_load_key(xmm7, key, 14 * 16, xmm31);
    lastroundEnc(xmm7, 3);

    // After AES encode rounds, the encrypted block cipher lies in zmm0-zmm3
    // xor 256 bytes of PT with the encrypted counters to produce CT.
    bind(REMAINDER16_END_LOOP);
    evpxorq(xmm0, xmm0, Address(src_addr, pos, Address::times_1, 0), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0), xmm0, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, Address(src_addr, pos, Address::times_1, 1 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 1 * 64), xmm1, Assembler::AVX_512bit);
    evpxorq(xmm2, xmm2, Address(src_addr, pos, Address::times_1, 2 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 2 * 64), xmm2, Assembler::AVX_512bit);
    evpxorq(xmm3, xmm3, Address(src_addr, pos, Address::times_1, 3 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 3 * 64), xmm3, Assembler::AVX_512bit);
    addq(pos, 256);

    cmpl(len_reg, 128);
    jcc(Assembler::aboveEqual, REMAINDER_8);

    cmpl(len_reg, 64);
    jcc(Assembler::aboveEqual, REMAINDER_4);
    //load mask for incrementing the counter value by 1
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 80), Assembler::AVX_128bit, r15);//Linc0 + 16(rip)
    jmp(REMAINDER_LOOP);

    // Each ZMM register can be used to encode 64 bytes of data, so we have 2 ZMM registers to encode 128 bytes of data
    bind(REMAINDER_8);
    subq(len_reg, 128);
    // As we process 8 blocks at a time, load mask for incrementing the counter value by 8
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 192), Assembler::AVX_512bit, r15);//Linc8(rip)
    // shuffle counters and xor with roundkey1
    vpshufb(xmm0, xmm8, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm0, xmm0, xmm20, Assembler::AVX_512bit);
    vpshufb(xmm1, xmm9, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, xmm20, Assembler::AVX_512bit);
    // increment counter by 8
    vpaddq(xmm8, xmm8, xmm19, Assembler::AVX_512bit);
    // AES encode
    roundEnc(xmm21, 1);
    roundEnc(xmm22, 1);
    roundEnc(xmm23, 1);
    roundEnc(xmm24, 1);
    roundEnc(xmm25, 1);
    roundEnc(xmm26, 1);
    roundEnc(xmm27, 1);
    roundEnc(xmm28, 1);
    roundEnc(xmm29, 1);

    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192_REMAINDER8);
    lastroundEnc(xmm30, 1);
    jmp(REMAINDER8_END_LOOP);

    bind(AES192_REMAINDER8);
    roundEnc(xmm30, 1);
    ev_load_key(xmm18, key, 11 * 16, xmm31);
    roundEnc(xmm18, 1);
    ev_load_key(xmm5, key, 12 * 16, xmm31);
    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256_REMAINDER8);
    lastroundEnc(xmm5, 1);
    jmp(REMAINDER8_END_LOOP);

    bind(AES256_REMAINDER8);
    roundEnc(xmm5, 1);
    ev_load_key(xmm6, key, 13 * 16, xmm31);
    roundEnc(xmm6, 1);
    ev_load_key(xmm7, key, 14 * 16, xmm31);
    lastroundEnc(xmm7, 1);

    bind(REMAINDER8_END_LOOP);
    // After AES encode rounds, the encrypted block cipher lies in zmm0-zmm1
    // XOR PT with the encrypted counter and store as CT
    evpxorq(xmm0, xmm0, Address(src_addr, pos, Address::times_1, 0 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0 * 64), xmm0, Assembler::AVX_512bit);
    evpxorq(xmm1, xmm1, Address(src_addr, pos, Address::times_1, 1 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 1 * 64), xmm1, Assembler::AVX_512bit);
    addq(pos, 128);

    cmpl(len_reg, 64);
    jcc(Assembler::aboveEqual, REMAINDER_4);
    // load mask for incrementing the counter value by 1
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 80), Assembler::AVX_128bit, r15);//Linc0 + 16(rip)
    jmp(REMAINDER_LOOP);

    // Each ZMM register can be used to encode 64 bytes of data, so we have 1 ZMM register used in this block of code
    bind(REMAINDER_4);
    subq(len_reg, 64);
    // As we process 4 blocks at a time, load mask for incrementing the counter value by 4
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 128), Assembler::AVX_512bit, r15);//Linc4(rip)
    // XOR counter with first roundkey
    vpshufb(xmm0, xmm8, xmm16, Assembler::AVX_512bit);
    evpxorq(xmm0, xmm0, xmm20, Assembler::AVX_512bit);
    // Increment counter
    vpaddq(xmm8, xmm8, xmm19, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm21, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm22, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm23, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm24, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm25, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm26, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm27, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm28, Assembler::AVX_512bit);
    vaesenc(xmm0, xmm0, xmm29, Assembler::AVX_512bit);
    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192_REMAINDER4);
    vaesenclast(xmm0, xmm0, xmm30, Assembler::AVX_512bit);
    jmp(END_REMAINDER4);

    bind(AES192_REMAINDER4);
    vaesenc(xmm0, xmm0, xmm30, Assembler::AVX_512bit);
    ev_load_key(xmm18, key, 11 * 16, xmm31);
    vaesenc(xmm0, xmm0, xmm18, Assembler::AVX_512bit);
    ev_load_key(xmm5, key, 12 * 16, xmm31);

    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256_REMAINDER4);
    vaesenclast(xmm0, xmm0, xmm5, Assembler::AVX_512bit);
    jmp(END_REMAINDER4);

    bind(AES256_REMAINDER4);
    vaesenc(xmm0, xmm0, xmm5, Assembler::AVX_512bit);
    ev_load_key(xmm6, key, 13 * 16, xmm31);
    vaesenc(xmm0, xmm0, xmm6, Assembler::AVX_512bit);
    ev_load_key(xmm7, key, 14 * 16, xmm31);
    vaesenclast(xmm0, xmm0, xmm7, Assembler::AVX_512bit);
    // After AES encode rounds, the encrypted block cipher lies in zmm0.
    // XOR encrypted block cipher with PT and store 64 bytes of ciphertext
    bind(END_REMAINDER4);
    evpxorq(xmm0, xmm0, Address(src_addr, pos, Address::times_1, 0 * 64), Assembler::AVX_512bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0), xmm0, Assembler::AVX_512bit);
    addq(pos, 64);
    // load mask for incrementing the counter value by 1
    evmovdquq(xmm19, ExternalAddress(StubRoutines::x86::counter_mask_addr() + 80), Assembler::AVX_128bit, r15);//Linc0 + 16(rip)

    // For a single block, the AES rounds start here.
    bind(REMAINDER_LOOP);
    cmpl(len_reg, 0);
    jcc(Assembler::belowEqual, END);
    // XOR counter with first roundkey
    vpshufb(xmm0, xmm8, xmm16, Assembler::AVX_128bit);
    evpxorq(xmm0, xmm0, xmm20, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm21, Assembler::AVX_128bit);
    // Increment counter by 1
    vpaddq(xmm8, xmm8, xmm19, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm22, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm23, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm24, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm25, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm26, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm27, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm28, Assembler::AVX_128bit);
    vaesenc(xmm0, xmm0, xmm29, Assembler::AVX_128bit);

    cmpl(rounds, 52);
    jcc(Assembler::aboveEqual, AES192_REMAINDER);
    vaesenclast(xmm0, xmm0, xmm30, Assembler::AVX_128bit);
    jmp(END_REMAINDER_LOOP);

    bind(AES192_REMAINDER);
    vaesenc(xmm0, xmm0, xmm30, Assembler::AVX_128bit);
    ev_load_key(xmm18, key, 11 * 16, xmm31);
    vaesenc(xmm0, xmm0, xmm18, Assembler::AVX_128bit);
    ev_load_key(xmm5, key, 12 * 16, xmm31);
    cmpl(rounds, 60);
    jcc(Assembler::aboveEqual, AES256_REMAINDER);
    vaesenclast(xmm0, xmm0, xmm5, Assembler::AVX_128bit);
    jmp(END_REMAINDER_LOOP);

    bind(AES256_REMAINDER);
    vaesenc(xmm0, xmm0, xmm5, Assembler::AVX_128bit);
    ev_load_key(xmm6, key, 13 * 16, xmm31);
    vaesenc(xmm0, xmm0, xmm6, Assembler::AVX_128bit);
    ev_load_key(xmm7, key, 14 * 16, xmm31);
    vaesenclast(xmm0, xmm0, xmm7, Assembler::AVX_128bit);

    bind(END_REMAINDER_LOOP);
    // If the length register is less than the blockSize i.e. 16
    // then we store only those bytes of the CT to the destination
    // corresponding to the length register value
    // extracting the exact number of bytes is handled by EXTRACT_TAILBYTES
    cmpl(len_reg, 16);
    jcc(Assembler::less, EXTRACT_TAILBYTES);
    subl(len_reg, 16);
    // After AES encode rounds, the encrypted block cipher lies in xmm0.
    // If the length register is equal to 16 bytes, store CT in dest after XOR operation.
    evpxorq(xmm0, xmm0, Address(src_addr, pos, Address::times_1, 0), Assembler::AVX_128bit);
    evmovdquq(Address(dest_addr, pos, Address::times_1, 0), xmm0, Assembler::AVX_128bit);
    addl(pos, 16);

    jmp(REMAINDER_LOOP);

    bind(EXTRACT_TAILBYTES);
    // Save encrypted counter value in xmm0 for next invocation, before XOR operation
    movdqu(Address(saved_encCounter_start, 0), xmm0);
    // XOR encryted block cipher in xmm0 with PT to produce CT
    evpxorq(xmm0, xmm0, Address(src_addr, pos, Address::times_1, 0), Assembler::AVX_128bit);
    // extract upto 15 bytes of CT from xmm0 as specified by length register
    testptr(len_reg, 8);
    jcc(Assembler::zero, EXTRACT_TAIL_4BYTES);
    pextrq(Address(dest_addr, pos), xmm0, 0);
    psrldq(xmm0, 8);
    addl(pos, 8);
    bind(EXTRACT_TAIL_4BYTES);
    testptr(len_reg, 4);
    jcc(Assembler::zero, EXTRACT_TAIL_2BYTES);
    pextrd(Address(dest_addr, pos), xmm0, 0);
    psrldq(xmm0, 4);
    addq(pos, 4);
    bind(EXTRACT_TAIL_2BYTES);
    testptr(len_reg, 2);
    jcc(Assembler::zero, EXTRACT_TAIL_1BYTE);
    pextrw(Address(dest_addr, pos), xmm0, 0);
    psrldq(xmm0, 2);
    addl(pos, 2);
    bind(EXTRACT_TAIL_1BYTE);
    testptr(len_reg, 1);
    jcc(Assembler::zero, END);
    pextrb(Address(dest_addr, pos), xmm0, 0);
    addl(pos, 1);

    bind(END);
    // If there are no tail bytes, store counter value and exit
    cmpl(len_reg, 0);
    jcc(Assembler::equal, STORE_CTR);
    movl(Address(used_addr, 0), len_reg);

    bind(STORE_CTR);
    //shuffle updated counter and store it
    vpshufb(xmm8, xmm8, xmm16, Assembler::AVX_128bit);
    movdqu(Address(counter, 0), xmm8);
    // Zero out counter and key registers
    evpxorq(xmm8, xmm8, xmm8, Assembler::AVX_512bit);
    evpxorq(xmm20, xmm20, xmm20, Assembler::AVX_512bit);
    evpxorq(xmm21, xmm21, xmm21, Assembler::AVX_512bit);
    evpxorq(xmm22, xmm22, xmm22, Assembler::AVX_512bit);
    evpxorq(xmm23, xmm23, xmm23, Assembler::AVX_512bit);
    evpxorq(xmm24, xmm24, xmm24, Assembler::AVX_512bit);
    evpxorq(xmm25, xmm25, xmm25, Assembler::AVX_512bit);
    evpxorq(xmm26, xmm26, xmm26, Assembler::AVX_512bit);
    evpxorq(xmm27, xmm27, xmm27, Assembler::AVX_512bit);
    evpxorq(xmm28, xmm28, xmm28, Assembler::AVX_512bit);
    evpxorq(xmm29, xmm29, xmm29, Assembler::AVX_512bit);
    evpxorq(xmm30, xmm30, xmm30, Assembler::AVX_512bit);
    cmpl(rounds, 44);
    jcc(Assembler::belowEqual, EXIT);
    evpxorq(xmm18, xmm18, xmm18, Assembler::AVX_512bit);
    evpxorq(xmm5, xmm5, xmm5, Assembler::AVX_512bit);
    cmpl(rounds, 52);
    jcc(Assembler::belowEqual, EXIT);
    evpxorq(xmm6, xmm6, xmm6, Assembler::AVX_512bit);
    evpxorq(xmm7, xmm7, xmm7, Assembler::AVX_512bit);
    bind(EXIT);
}

#endif // _LP64
