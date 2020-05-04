/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <LibX86/Instruction.h>
#include <stdio.h>

namespace X86 {

enum IsLockPrefixAllowed {
    LockPrefixNotAllowed = 0,
    LockPrefixAllowed
};

enum InstructionFormat {
    InvalidFormat,
    MultibyteWithSlash,
    MultibyteWithSubopcode,
    InstructionPrefix,

    __BeginFormatsWithRMByte,
    OP_RM16_reg16,
    OP_reg8_RM8,
    OP_reg16_RM16,
    OP_RM16_seg,
    OP_RM32_seg,
    OP_RM8_imm8,
    OP_RM16_imm16,
    OP_RM16_imm8,
    OP_RM32_imm8,
    OP_RM8,
    OP_RM16,
    OP_RM32,
    OP_RM8_reg8,
    OP_RM32_reg32,
    OP_reg32_RM32,
    OP_RM32_imm32,
    OP_reg16_RM16_imm8,
    OP_reg32_RM32_imm8,
    OP_reg16_RM16_imm16,
    OP_reg32_RM32_imm32,
    OP_reg16_mem16,
    OP_reg32_mem32,
    OP_seg_RM16,
    OP_seg_RM32,
    OP_RM8_1,
    OP_RM16_1,
    OP_RM32_1,
    OP_FAR_mem16,
    OP_FAR_mem32,
    OP_RM8_CL,
    OP_RM16_CL,
    OP_RM32_CL,
    OP_reg32_CR,
    OP_CR_reg32,
    OP_reg32_DR,
    OP_DR_reg32,
    OP_reg16_RM8,
    OP_reg32_RM8,
    OP_reg32_RM16,
    OP_RM16_reg16_imm8,
    OP_RM32_reg32_imm8,
    OP_RM16_reg16_CL,
    OP_RM32_reg32_CL,
    OP_mm1_mm2m64,
    OP_mm1m64_mm2,
    __EndFormatsWithRMByte,

    OP_reg32_imm32,
    OP_AL_imm8,
    OP_AX_imm16,
    OP_EAX_imm32,
    OP_CS,
    OP_DS,
    OP_ES,
    OP_SS,
    OP_FS,
    OP_GS,
    OP,
    OP_reg16,
    OP_imm16,
    OP_relimm16,
    OP_relimm32,
    OP_imm8,
    OP_imm16_imm16,
    OP_imm16_imm32,
    OP_AX_reg16,
    OP_EAX_reg32,
    OP_AL_moff8,
    OP_AX_moff16,
    OP_EAX_moff32,
    OP_moff8_AL,
    OP_moff16_AX,
    OP_moff32_EAX,
    OP_reg8_imm8,
    OP_reg16_imm16,
    OP_3,
    OP_AX_imm8,
    OP_EAX_imm8,
    OP_short_imm8,
    OP_AL_DX,
    OP_AX_DX,
    OP_EAX_DX,
    OP_DX_AL,
    OP_DX_AX,
    OP_DX_EAX,
    OP_imm8_AL,
    OP_imm8_AX,
    OP_imm8_EAX,
    OP_reg8_CL,

    OP_reg32,
    OP_imm32,
    OP_imm16_imm8,

    OP_NEAR_imm,
};

static const unsigned CurrentAddressSize = 0xB33FBABE;

struct InstructionDescriptor {
    bool opcode_has_register_index { false };
    const char* mnemonic { nullptr };
    InstructionFormat format { InvalidFormat };
    bool has_rm { false };
    unsigned imm1_bytes { 0 };
    unsigned imm2_bytes { 0 };
    InstructionDescriptor* slashes { nullptr };

    unsigned imm1_bytes_for_address_size(bool a32)
    {
        if (imm1_bytes == CurrentAddressSize)
            return a32 ? 4 : 2;
        return imm1_bytes;
    }

    unsigned imm2_bytes_for_address_size(bool a32)
    {
        if (imm2_bytes == CurrentAddressSize)
            return a32 ? 4 : 2;
        return imm2_bytes;
    }

    IsLockPrefixAllowed lock_prefix_allowed { LockPrefixNotAllowed };
};

static InstructionDescriptor s_table16[256];
static InstructionDescriptor s_table32[256];
static InstructionDescriptor s_0f_table16[256];
static InstructionDescriptor s_0f_table32[256];

static bool opcode_has_register_index(u8 op)
{
    if (op >= 0x40 && op <= 0x5F)
        return true;
    if (op >= 0x90 && op <= 0x97)
        return true;
    if (op >= 0xB0 && op <= 0xBF)
        return true;
    return false;
}

static void build(InstructionDescriptor* table, u8 op, const char* mnemonic, InstructionFormat format, IsLockPrefixAllowed lock_prefix_allowed)
{
    InstructionDescriptor& d = table[op];

    d.mnemonic = mnemonic;
    d.format = format;
    d.lock_prefix_allowed = lock_prefix_allowed;

    if ((format > __BeginFormatsWithRMByte && format < __EndFormatsWithRMByte) || format == MultibyteWithSlash)
        d.has_rm = true;
    else
        d.opcode_has_register_index = opcode_has_register_index(op);

    switch (format) {
    case OP_RM8_imm8:
    case OP_RM16_imm8:
    case OP_RM32_imm8:
    case OP_reg16_RM16_imm8:
    case OP_reg32_RM32_imm8:
    case OP_AL_imm8:
    case OP_imm8:
    case OP_reg8_imm8:
    case OP_AX_imm8:
    case OP_EAX_imm8:
    case OP_short_imm8:
    case OP_imm8_AL:
    case OP_imm8_AX:
    case OP_imm8_EAX:
    case OP_RM16_reg16_imm8:
    case OP_RM32_reg32_imm8:
        d.imm1_bytes = 1;
        break;
    case OP_reg16_RM16_imm16:
    case OP_AX_imm16:
    case OP_imm16:
    case OP_relimm16:
    case OP_reg16_imm16:
    case OP_RM16_imm16:
        d.imm1_bytes = 2;
        break;
    case OP_RM32_imm32:
    case OP_reg32_RM32_imm32:
    case OP_reg32_imm32:
    case OP_EAX_imm32:
    case OP_imm32:
    case OP_relimm32:
        d.imm1_bytes = 4;
        break;
    case OP_imm16_imm8:
        d.imm1_bytes = 2;
        d.imm2_bytes = 1;
        break;
    case OP_imm16_imm16:
        d.imm1_bytes = 2;
        d.imm2_bytes = 2;
        break;
    case OP_imm16_imm32:
        d.imm1_bytes = 2;
        d.imm2_bytes = 4;
        break;
    case OP_moff8_AL:
    case OP_moff16_AX:
    case OP_moff32_EAX:
    case OP_AL_moff8:
    case OP_AX_moff16:
    case OP_EAX_moff32:
    case OP_NEAR_imm:
        d.imm1_bytes = CurrentAddressSize;
        break;
    //default:
    case InvalidFormat:
    case MultibyteWithSlash:
    case MultibyteWithSubopcode:
    case InstructionPrefix:
    case __BeginFormatsWithRMByte:
    case OP_RM16_reg16:
    case OP_reg8_RM8:
    case OP_reg16_RM16:
    case OP_RM16_seg:
    case OP_RM32_seg:
    case OP_RM8:
    case OP_RM16:
    case OP_RM32:
    case OP_RM8_reg8:
    case OP_RM32_reg32:
    case OP_reg32_RM32:
    case OP_reg16_mem16:
    case OP_reg32_mem32:
    case OP_seg_RM16:
    case OP_seg_RM32:
    case OP_RM8_1:
    case OP_RM16_1:
    case OP_RM32_1:
    case OP_FAR_mem16:
    case OP_FAR_mem32:
    case OP_RM8_CL:
    case OP_RM16_CL:
    case OP_RM32_CL:
    case OP_reg32_CR:
    case OP_CR_reg32:
    case OP_reg16_RM8:
    case OP_reg32_RM8:
    case OP_mm1_mm2m64:
    case OP_mm1m64_mm2:
    case __EndFormatsWithRMByte:
    case OP_CS:
    case OP_DS:
    case OP_ES:
    case OP_SS:
    case OP_FS:
    case OP_GS:
    case OP:
    case OP_reg16:
    case OP_AX_reg16:
    case OP_EAX_reg32:
    case OP_3:
    case OP_AL_DX:
    case OP_AX_DX:
    case OP_EAX_DX:
    case OP_DX_AL:
    case OP_DX_AX:
    case OP_DX_EAX:
    case OP_reg8_CL:
    case OP_reg32:
    case OP_reg32_RM16:
    case OP_reg32_DR:
    case OP_DR_reg32:
    case OP_RM16_reg16_CL:
    case OP_RM32_reg32_CL:
        break;
    }
}

static void build_slash(InstructionDescriptor* table, u8 op, u8 slash, const char* mnemonic, InstructionFormat format, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    InstructionDescriptor& d = table[op];
    d.format = MultibyteWithSlash;
    d.has_rm = true;
    if (!d.slashes)
        d.slashes = new InstructionDescriptor[8];

    build(d.slashes, slash, mnemonic, format, lock_prefix_allowed);
}

static void build_0f(u8 op, const char* mnemonic, InstructionFormat format, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_0f_table16, op, mnemonic, format, lock_prefix_allowed);
    build(s_0f_table32, op, mnemonic, format, lock_prefix_allowed);
}

static void build(u8 op, const char* mnemonic, InstructionFormat format, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_table16, op, mnemonic, format, lock_prefix_allowed);
    build(s_table32, op, mnemonic, format, lock_prefix_allowed);
}

static void build(u8 op, const char* mnemonic, InstructionFormat format16, InstructionFormat format32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_table16, op, mnemonic, format16, lock_prefix_allowed);
    build(s_table32, op, mnemonic, format32, lock_prefix_allowed);
}

static void build_0f(u8 op, const char* mnemonic, InstructionFormat format16, InstructionFormat format32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_0f_table16, op, mnemonic, format16, lock_prefix_allowed);
    build(s_0f_table32, op, mnemonic, format32, lock_prefix_allowed);
}

static void build(u8 op, const char* mnemonic16, InstructionFormat format16, const char* mnemonic32, InstructionFormat format32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_table16, op, mnemonic16, format16, lock_prefix_allowed);
    build(s_table32, op, mnemonic32, format32, lock_prefix_allowed);
}

static void build_0f(u8 op, const char* mnemonic16, InstructionFormat format16, const char* mnemonic32, InstructionFormat format32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_0f_table16, op, mnemonic16, format16, lock_prefix_allowed);
    build(s_0f_table32, op, mnemonic32, format32, lock_prefix_allowed);
}

static void build_slash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_table16, op, slash, mnemonic, format, lock_prefix_allowed);
    build_slash(s_table32, op, slash, mnemonic, format, lock_prefix_allowed);
}

static void build_slash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format16, InstructionFormat format32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_table16, op, slash, mnemonic, format16, lock_prefix_allowed);
    build_slash(s_table32, op, slash, mnemonic, format32, lock_prefix_allowed);
}

static void build0FSlash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format16, InstructionFormat format32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_0f_table16, op, slash, mnemonic, format16, lock_prefix_allowed);
    build_slash(s_0f_table32, op, slash, mnemonic, format32, lock_prefix_allowed);
}

static void build0FSlash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_0f_table16, op, slash, mnemonic, format, lock_prefix_allowed);
    build_slash(s_0f_table32, op, slash, mnemonic, format, lock_prefix_allowed);
}

void build_opcode_tables_if_needed()
{
    static bool has_built_tables = false;
    if (has_built_tables)
        return;

    build(0x00, "ADD", OP_RM8_reg8, LockPrefixAllowed);
    build(0x01, "ADD", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x02, "ADD", OP_reg8_RM8, LockPrefixAllowed);
    build(0x03, "ADD", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x04, "ADD", OP_AL_imm8);
    build(0x05, "ADD", OP_AX_imm16, OP_EAX_imm32);
    build(0x06, "PUSH", OP_ES);
    build(0x07, "POP", OP_ES);
    build(0x08, "OR", OP_RM8_reg8, LockPrefixAllowed);
    build(0x09, "OR", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x0A, "OR", OP_reg8_RM8, LockPrefixAllowed);
    build(0x0B, "OR", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x0C, "OR", OP_AL_imm8);
    build(0x0D, "OR", OP_AX_imm16, OP_EAX_imm32);
    build(0x0E, "PUSH", OP_CS);

    build(0x10, "ADC", OP_RM8_reg8, LockPrefixAllowed);
    build(0x11, "ADC", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x12, "ADC", OP_reg8_RM8, LockPrefixAllowed);
    build(0x13, "ADC", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x14, "ADC", OP_AL_imm8);
    build(0x15, "ADC", OP_AX_imm16, OP_EAX_imm32);
    build(0x16, "PUSH", OP_SS);
    build(0x17, "POP", OP_SS);
    build(0x18, "SBB", OP_RM8_reg8, LockPrefixAllowed);
    build(0x19, "SBB", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x1A, "SBB", OP_reg8_RM8, LockPrefixAllowed);
    build(0x1B, "SBB", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x1C, "SBB", OP_AL_imm8);
    build(0x1D, "SBB", OP_AX_imm16, OP_EAX_imm32);
    build(0x1E, "PUSH", OP_DS);
    build(0x1F, "POP", OP_DS);

    build(0x20, "AND", OP_RM8_reg8, LockPrefixAllowed);
    build(0x21, "AND", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x22, "AND", OP_reg8_RM8, LockPrefixAllowed);
    build(0x23, "AND", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x24, "AND", OP_AL_imm8);
    build(0x25, "AND", OP_AX_imm16, OP_EAX_imm32);
    build(0x27, "DAA", OP);
    build(0x28, "SUB", OP_RM8_reg8, LockPrefixAllowed);
    build(0x29, "SUB", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x2A, "SUB", OP_reg8_RM8, LockPrefixAllowed);
    build(0x2B, "SUB", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x2C, "SUB", OP_AL_imm8);
    build(0x2D, "SUB", OP_AX_imm16, OP_EAX_imm32);
    build(0x2F, "DAS", OP);

    build(0x30, "XOR", OP_RM8_reg8, LockPrefixAllowed);
    build(0x31, "XOR", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x32, "XOR", OP_reg8_RM8, LockPrefixAllowed);
    build(0x33, "XOR", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x34, "XOR", OP_AL_imm8);
    build(0x35, "XOR", OP_AX_imm16, OP_EAX_imm32);
    build(0x37, "AAA", OP);
    build(0x38, "CMP", OP_RM8_reg8, LockPrefixAllowed);
    build(0x39, "CMP", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build(0x3A, "CMP", OP_reg8_RM8, LockPrefixAllowed);
    build(0x3B, "CMP", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x3C, "CMP", OP_AL_imm8);
    build(0x3D, "CMP", OP_AX_imm16, OP_EAX_imm32);
    build(0x3F, "AAS", OP);

    for (u8 i = 0; i <= 7; ++i)
        build(0x40 + i, "INC", OP_reg16, OP_reg32);

    for (u8 i = 0; i <= 7; ++i)
        build(0x48 + i, "DEC", OP_reg16, OP_reg32);

    for (u8 i = 0; i <= 7; ++i)
        build(0x50 + i, "PUSH", OP_reg16, OP_reg32);

    for (u8 i = 0; i <= 7; ++i)
        build(0x58 + i, "POP", OP_reg16, OP_reg32);

    build(0x60, "PUSHAW", OP, "PUSHAD", OP);
    build(0x61, "POPAW", OP, "POPAD", OP);
    build(0x62, "BOUND", OP_reg16_RM16, "BOUND", OP_reg32_RM32);
    build(0x63, "ARPL", OP_RM16_reg16);

    build(0x68, "PUSH", OP_imm16, OP_imm32);
    build(0x69, "IMUL", OP_reg16_RM16_imm16, OP_reg32_RM32_imm32);
    build(0x6A, "PUSH", OP_imm8);
    build(0x6B, "IMUL", OP_reg16_RM16_imm8, OP_reg32_RM32_imm8);
    build(0x6C, "INSB", OP);
    build(0x6D, "INSW", OP, "INSD", OP);
    build(0x6E, "OUTSB", OP);
    build(0x6F, "OUTSW", OP, "OUTSD", OP);

    build(0x70, "JO", OP_short_imm8);
    build(0x71, "JNO", OP_short_imm8);
    build(0x72, "JC", OP_short_imm8);
    build(0x73, "JNC", OP_short_imm8);
    build(0x74, "JZ", OP_short_imm8);
    build(0x75, "JNZ", OP_short_imm8);
    build(0x76, "JNA", OP_short_imm8);
    build(0x77, "JA", OP_short_imm8);
    build(0x78, "JS", OP_short_imm8);
    build(0x79, "JNS", OP_short_imm8);
    build(0x7A, "JP", OP_short_imm8);
    build(0x7B, "JNP", OP_short_imm8);
    build(0x7C, "JL", OP_short_imm8);
    build(0x7D, "JNL", OP_short_imm8);
    build(0x7E, "JNG", OP_short_imm8);
    build(0x7F, "JG", OP_short_imm8);

    build(0x84, "TEST", OP_RM8_reg8);
    build(0x85, "TEST", OP_RM16_reg16, OP_RM32_reg32);
    build(0x86, "XCHG", OP_reg8_RM8, LockPrefixAllowed);
    build(0x87, "XCHG", OP_reg16_RM16, OP_reg32_RM32, LockPrefixAllowed);
    build(0x88, "MOV", OP_RM8_reg8);
    build(0x89, "MOV", OP_RM16_reg16, OP_RM32_reg32);
    build(0x8A, "MOV", OP_reg8_RM8);
    build(0x8B, "MOV", OP_reg16_RM16, OP_reg32_RM32);
    build(0x8C, "MOV", OP_RM16_seg);
    build(0x8D, "LEA", OP_reg16_mem16, OP_reg32_mem32);
    build(0x8E, "MOV", OP_seg_RM16, OP_seg_RM32);

    build(0x90, "NOP", OP);

    for (u8 i = 0; i <= 6; ++i)
        build(0x91 + i, "XCHG", OP_AX_reg16, OP_EAX_reg32);

    build(0x98, "CBW", OP, "CWDE", OP);
    build(0x99, "CWD", OP, "CDQ", OP);
    build(0x9A, "CALL", OP_imm16_imm16, OP_imm16_imm32);
    build(0x9B, "WAIT", OP);
    build(0x9C, "PUSHFW", OP, "PUSHFD", OP);
    build(0x9D, "POPFW", OP, "POPFD", OP);
    build(0x9E, "SAHF", OP);
    build(0x9F, "LAHF", OP);

    build(0xA0, "MOV", OP_AL_moff8);
    build(0xA1, "MOV", OP_AX_moff16, OP_EAX_moff32);
    build(0xA2, "MOV", OP_moff8_AL);
    build(0xA3, "MOV", OP_moff16_AX, OP_moff32_EAX);
    build(0xA4, "MOVSB", OP);
    build(0xA5, "MOVSW", OP, "MOVSD", OP);
    build(0xA6, "CMPSB", OP);
    build(0xA7, "CMPSW", OP, "CMPSD", OP);
    build(0xA8, "TEST", OP_AL_imm8);
    build(0xA9, "TEST", OP_AX_imm16, OP_EAX_imm32);
    build(0xAA, "STOSB", OP);
    build(0xAB, "STOSW", OP, "STOSD", OP);
    build(0xAC, "LODSB", OP);
    build(0xAD, "LODSW", OP, "LODSD", OP);
    build(0xAE, "SCASB", OP);
    build(0xAF, "SCASW", OP, "SCASD", OP);

    for (u8 i = 0xb0; i <= 0xb7; ++i)
        build(i, "MOV", OP_reg8_imm8);

    for (u8 i = 0xb8; i <= 0xbf; ++i)
        build(i, "MOV", OP_reg16_imm16, OP_reg32_imm32);

    build(0xC2, "RET", OP_imm16);
    build(0xC3, "RET", OP);
    build(0xC4, "LES", OP_reg16_mem16, OP_reg32_mem32);
    build(0xC5, "LDS", OP_reg16_mem16, OP_reg32_mem32);
    build(0xC6, "MOV", OP_RM8_imm8);
    build(0xC7, "MOV", OP_RM16_imm16, OP_RM32_imm32);
    build(0xC8, "ENTER", OP_imm16_imm8);
    build(0xC9, "LEAVE", OP, OP);
    build(0xCA, "RETF", OP_imm16);
    build(0xCB, "RETF", OP);
    build(0xCC, "INT3", OP_3);
    build(0xCD, "INT", OP_imm8);
    build(0xCE, "INTO", OP);
    build(0xCF, "IRET", OP);

    build(0xD4, "AAM", OP_imm8);
    build(0xD5, "AAD", OP_imm8);
    build(0xD6, "SALC", OP);
    build(0xD7, "XLAT", OP);

    // FIXME: D8-DF == FPU
    for (u8 i = 0; i <= 7; ++i)
        build(0xD8 + i, "FPU?", OP_RM8);

    build(0xE0, "LOOPNZ", OP_imm8);
    build(0xE1, "LOOPZ", OP_imm8);
    build(0xE2, "LOOP", OP_imm8);
    build(0xE3, "JCXZ", OP_imm8);
    build(0xE4, "IN", OP_AL_imm8);
    build(0xE5, "IN", OP_AX_imm8, OP_EAX_imm8);
    build(0xE6, "OUT", OP_imm8_AL);
    build(0xE7, "OUT", OP_imm8_AX, OP_imm8_EAX);
    build(0xE8, "CALL", OP_relimm16, OP_relimm32);
    build(0xE9, "JMP", OP_relimm16, OP_relimm32);
    build(0xEA, "JMP", OP_imm16_imm16, OP_imm16_imm32);
    build(0xEB, "JMP", OP_short_imm8);
    build(0xEC, "IN", OP_AL_DX);
    build(0xED, "IN", OP_AX_DX, OP_EAX_DX);
    build(0xEE, "OUT", OP_DX_AL);
    build(0xEF, "OUT", OP_DX_AX, OP_DX_EAX);

    build(0xF1, "VKILL", OP);

    build(0xF4, "HLT", OP);
    build(0xF5, "CMC", OP);

    build(0xF8, "CLC", OP);
    build(0xF9, "STC", OP);
    build(0xFA, "CLI", OP);
    build(0xFB, "STI", OP);
    build(0xFC, "CLD", OP);
    build(0xFD, "STD", OP);

    build_slash(0x80, 0, "ADD", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 1, "OR", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 2, "ADC", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 3, "SBB", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 4, "AND", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 5, "SUB", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 6, "XOR", OP_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 7, "CMP", OP_RM8_imm8);

    build_slash(0x81, 0, "ADD", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 1, "OR", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 2, "ADC", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 3, "SBB", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 4, "AND", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 5, "SUB", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 6, "XOR", OP_RM16_imm16, OP_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 7, "CMP", OP_RM16_imm16, OP_RM32_imm32);

    build_slash(0x83, 0, "ADD", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 1, "OR", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 2, "ADC", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 3, "SBB", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 4, "AND", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 5, "SUB", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 6, "XOR", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 7, "CMP", OP_RM16_imm8, OP_RM32_imm8);

    build_slash(0x8F, 0, "POP", OP_RM16, OP_RM32);

    build_slash(0xC0, 0, "ROL", OP_RM8_imm8);
    build_slash(0xC0, 1, "ROR", OP_RM8_imm8);
    build_slash(0xC0, 2, "RCL", OP_RM8_imm8);
    build_slash(0xC0, 3, "RCR", OP_RM8_imm8);
    build_slash(0xC0, 4, "SHL", OP_RM8_imm8);
    build_slash(0xC0, 5, "SHR", OP_RM8_imm8);
    build_slash(0xC0, 6, "SHL", OP_RM8_imm8); // Undocumented
    build_slash(0xC0, 7, "SAR", OP_RM8_imm8);

    build_slash(0xC1, 0, "ROL", OP_RM16_imm8, OP_RM32_imm8);
    build_slash(0xC1, 1, "ROR", OP_RM16_imm8, OP_RM32_imm8);
    build_slash(0xC1, 2, "RCL", OP_RM16_imm8, OP_RM32_imm8);
    build_slash(0xC1, 3, "RCR", OP_RM16_imm8, OP_RM32_imm8);
    build_slash(0xC1, 4, "SHL", OP_RM16_imm8, OP_RM32_imm8);
    build_slash(0xC1, 5, "SHR", OP_RM16_imm8, OP_RM32_imm8);
    build_slash(0xC1, 6, "SHL", OP_RM16_imm8, OP_RM32_imm8); // Undocumented
    build_slash(0xC1, 7, "SAR", OP_RM16_imm8, OP_RM32_imm8);

    build_slash(0xD0, 0, "ROL", OP_RM8_1);
    build_slash(0xD0, 1, "ROR", OP_RM8_1);
    build_slash(0xD0, 2, "RCL", OP_RM8_1);
    build_slash(0xD0, 3, "RCR", OP_RM8_1);
    build_slash(0xD0, 4, "SHL", OP_RM8_1);
    build_slash(0xD0, 5, "SHR", OP_RM8_1);
    build_slash(0xD0, 6, "SHL", OP_RM8_1); // Undocumented
    build_slash(0xD0, 7, "SAR", OP_RM8_1);

    build_slash(0xD1, 0, "ROL", OP_RM16_1, OP_RM32_1);
    build_slash(0xD1, 1, "ROR", OP_RM16_1, OP_RM32_1);
    build_slash(0xD1, 2, "RCL", OP_RM16_1, OP_RM32_1);
    build_slash(0xD1, 3, "RCR", OP_RM16_1, OP_RM32_1);
    build_slash(0xD1, 4, "SHL", OP_RM16_1, OP_RM32_1);
    build_slash(0xD1, 5, "SHR", OP_RM16_1, OP_RM32_1);
    build_slash(0xD1, 6, "SHL", OP_RM16_1, OP_RM32_1); // Undocumented
    build_slash(0xD1, 7, "SAR", OP_RM16_1, OP_RM32_1);

    build_slash(0xD2, 0, "ROL", OP_RM8_CL);
    build_slash(0xD2, 1, "ROR", OP_RM8_CL);
    build_slash(0xD2, 2, "RCL", OP_RM8_CL);
    build_slash(0xD2, 3, "RCR", OP_RM8_CL);
    build_slash(0xD2, 4, "SHL", OP_RM8_CL);
    build_slash(0xD2, 5, "SHR", OP_RM8_CL);
    build_slash(0xD2, 6, "SHL", OP_RM8_CL); // Undocumented
    build_slash(0xD2, 7, "SAR", OP_RM8_CL);

    build_slash(0xD3, 0, "ROL", OP_RM16_CL, OP_RM32_CL);
    build_slash(0xD3, 1, "ROR", OP_RM16_CL, OP_RM32_CL);
    build_slash(0xD3, 2, "RCL", OP_RM16_CL, OP_RM32_CL);
    build_slash(0xD3, 3, "RCR", OP_RM16_CL, OP_RM32_CL);
    build_slash(0xD3, 4, "SHL", OP_RM16_CL, OP_RM32_CL);
    build_slash(0xD3, 5, "SHR", OP_RM16_CL, OP_RM32_CL);
    build_slash(0xD3, 6, "SHL", OP_RM16_CL, OP_RM32_CL); // Undocumented
    build_slash(0xD3, 7, "SAR", OP_RM16_CL, OP_RM32_CL);

    build_slash(0xF6, 0, "TEST", OP_RM8_imm8);
    build_slash(0xF6, 1, "TEST", OP_RM8_imm8); // Undocumented
    build_slash(0xF6, 2, "NOT", OP_RM8, LockPrefixAllowed);
    build_slash(0xF6, 3, "NEG", OP_RM8, LockPrefixAllowed);
    build_slash(0xF6, 4, "MUL", OP_RM8);
    build_slash(0xF6, 5, "IMUL", OP_RM8);
    build_slash(0xF6, 6, "DIV", OP_RM8);
    build_slash(0xF6, 7, "IDIV", OP_RM8);

    build_slash(0xF7, 0, "TEST", OP_RM16_imm16, OP_RM32_imm32);
    build_slash(0xF7, 1, "TEST", OP_RM16_imm16, OP_RM32_imm32); // Undocumented
    build_slash(0xF7, 2, "NOT", OP_RM16, OP_RM32, LockPrefixAllowed);
    build_slash(0xF7, 3, "NEG", OP_RM16, OP_RM32, LockPrefixAllowed);
    build_slash(0xF7, 4, "MUL", OP_RM16, OP_RM32);
    build_slash(0xF7, 5, "IMUL", OP_RM16, OP_RM32);
    build_slash(0xF7, 6, "DIV", OP_RM16, OP_RM32);
    build_slash(0xF7, 7, "IDIV", OP_RM16, OP_RM32);

    build_slash(0xFE, 0, "INC", OP_RM8, LockPrefixAllowed);
    build_slash(0xFE, 1, "DEC", OP_RM8, LockPrefixAllowed);

    build_slash(0xFF, 0, "INC", OP_RM16, OP_RM32, LockPrefixAllowed);
    build_slash(0xFF, 1, "DEC", OP_RM16, OP_RM32, LockPrefixAllowed);
    build_slash(0xFF, 2, "CALL", OP_RM16, OP_RM32);
    build_slash(0xFF, 3, "CALL", OP_FAR_mem16, OP_FAR_mem32);
    build_slash(0xFF, 4, "JMP", OP_RM16, OP_RM32);
    build_slash(0xFF, 5, "JMP", OP_FAR_mem16, OP_FAR_mem32);
    build_slash(0xFF, 6, "PUSH", OP_RM16, OP_RM32);

    // Instructions starting with 0x0F are multi-byte opcodes.
    build0FSlash(0x00, 0, "SLDT", OP_RM16);
    build0FSlash(0x00, 1, "STR", OP_RM16);
    build0FSlash(0x00, 2, "LLDT", OP_RM16);
    build0FSlash(0x00, 3, "LTR", OP_RM16);
    build0FSlash(0x00, 4, "VERR", OP_RM16);
    build0FSlash(0x00, 5, "VERW", OP_RM16);

    build0FSlash(0x01, 0, "SGDT", OP_RM16);
    build0FSlash(0x01, 1, "SIDT", OP_RM16);
    build0FSlash(0x01, 2, "LGDT", OP_RM16);
    build0FSlash(0x01, 3, "LIDT", OP_RM16);
    build0FSlash(0x01, 4, "SMSW", OP_RM16);
    build0FSlash(0x01, 6, "LMSW", OP_RM16);
    build0FSlash(0x01, 7, "INVLPG", OP_RM32);

    build0FSlash(0xBA, 4, "BT", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build0FSlash(0xBA, 5, "BTS", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build0FSlash(0xBA, 6, "BTR", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);
    build0FSlash(0xBA, 7, "BTC", OP_RM16_imm8, OP_RM32_imm8, LockPrefixAllowed);

    build0FSlash(0xC7, 6, "RDRAND", OP_RM16, OP_RM32);

    build_0f(0x02, "LAR", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x03, "LSL", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x06, "CLTS", OP);
    build_0f(0x09, "WBINVD", OP);
    build_0f(0x0B, "UD2", OP);

    build_0f(0x20, "MOV", OP_reg32_CR);
    build_0f(0x21, "MOV", OP_reg32_DR);
    build_0f(0x22, "MOV", OP_CR_reg32);
    build_0f(0x23, "MOV", OP_DR_reg32);

    build_0f(0x31, "RDTSC", OP);

    build_0f(0x40, "CMOVO", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x41, "CMOVNO", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x42, "CMOVC", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x43, "CMOVNC", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x44, "CMOVZ", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x45, "CMOVNZ", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x46, "CMOVNA", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x47, "CMOVA", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x48, "CMOVS", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x49, "CMOVNS", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x4A, "CMOVP", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x4B, "CMOVNP", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x4C, "CMOVL", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x4D, "CMOVNL", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x4E, "CMOVNG", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0x4F, "CMOVG", OP_reg16_RM16, OP_reg32_RM32);

    build_0f(0x6F, "MOVQ", OP_mm1_mm2m64);
    build_0f(0x77, "EMMS", OP);
    build_0f(0x7F, "MOVQ", OP_mm1m64_mm2);

    build_0f(0x80, "JO", OP_NEAR_imm);
    build_0f(0x81, "JNO", OP_NEAR_imm);
    build_0f(0x82, "JC", OP_NEAR_imm);
    build_0f(0x83, "JNC", OP_NEAR_imm);
    build_0f(0x84, "JZ", OP_NEAR_imm);
    build_0f(0x85, "JNZ", OP_NEAR_imm);
    build_0f(0x86, "JNA", OP_NEAR_imm);
    build_0f(0x87, "JA", OP_NEAR_imm);
    build_0f(0x88, "JS", OP_NEAR_imm);
    build_0f(0x89, "JNS", OP_NEAR_imm);
    build_0f(0x8A, "JP", OP_NEAR_imm);
    build_0f(0x8B, "JNP", OP_NEAR_imm);
    build_0f(0x8C, "JL", OP_NEAR_imm);
    build_0f(0x8D, "JNL", OP_NEAR_imm);
    build_0f(0x8E, "JNG", OP_NEAR_imm);
    build_0f(0x8F, "JG", OP_NEAR_imm);

    build_0f(0x90, "SETO", OP_RM8);
    build_0f(0x91, "SETNO", OP_RM8);
    build_0f(0x92, "SETC", OP_RM8);
    build_0f(0x93, "SETNC", OP_RM8);
    build_0f(0x94, "SETZ", OP_RM8);
    build_0f(0x95, "SETNZ", OP_RM8);
    build_0f(0x96, "SETNA", OP_RM8);
    build_0f(0x97, "SETA", OP_RM8);
    build_0f(0x98, "SETS", OP_RM8);
    build_0f(0x99, "SETNS", OP_RM8);
    build_0f(0x9A, "SETP", OP_RM8);
    build_0f(0x9B, "SETNP", OP_RM8);
    build_0f(0x9C, "SETL", OP_RM8);
    build_0f(0x9D, "SETNL", OP_RM8);
    build_0f(0x9E, "SETNG", OP_RM8);
    build_0f(0x9F, "SETG", OP_RM8);

    build_0f(0xA0, "PUSH", OP_FS);
    build_0f(0xA1, "POP", OP_FS);
    build_0f(0xA2, "CPUID", OP);
    build_0f(0xA3, "BT", OP_RM16_reg16, OP_RM32_reg32);
    build_0f(0xA4, "SHLD", OP_RM16_reg16_imm8, OP_RM32_reg32_imm8);
    build_0f(0xA5, "SHLD", OP_RM16_reg16_CL, OP_RM32_reg32_CL);
    build_0f(0xA8, "PUSH", OP_GS);
    build_0f(0xA9, "POP", OP_GS);
    build_0f(0xAB, "BTS", OP_RM16_reg16, OP_RM32_reg32);
    build_0f(0xAC, "SHRD", OP_RM16_reg16_imm8, OP_RM32_reg32_imm8);
    build_0f(0xAD, "SHRD", OP_RM16_reg16_CL, OP_RM32_reg32_CL);
    build_0f(0xAF, "IMUL", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0xB0, "CMPXCHG", OP_RM8_reg8, LockPrefixAllowed);
    build_0f(0xB1, "CMPXCHG", OP_RM16_reg16, OP_RM32_reg32, LockPrefixAllowed);
    build_0f(0xB2, "LSS", OP_reg16_mem16, OP_reg32_mem32);
    build_0f(0xB3, "BTR", OP_RM16_reg16, OP_RM32_reg32);
    build_0f(0xB4, "LFS", OP_reg16_mem16, OP_reg32_mem32);
    build_0f(0xB5, "LGS", OP_reg16_mem16, OP_reg32_mem32);
    build_0f(0xB6, "MOVZX", OP_reg16_RM8, OP_reg32_RM8);
    build_0f(0xB7, "0xB7", OP, "MOVZX", OP_reg32_RM16);
    build_0f(0xB9, "UD1", OP);
    build_0f(0xBB, "BTC", OP_RM16_reg16, OP_RM32_reg32);
    build_0f(0xBC, "BSF", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0xBD, "BSR", OP_reg16_RM16, OP_reg32_RM32);
    build_0f(0xBE, "MOVSX", OP_reg16_RM8, OP_reg32_RM8);
    build_0f(0xBF, "0xBF", OP, "MOVSX", OP_reg32_RM16);

    for (u8 i = 0xc8; i <= 0xcf; ++i)
        build_0f(i, "BSWAP", OP_reg32);

    build_0f(0xFC, "PADDB", OP_mm1_mm2m64);
    build_0f(0xFD, "PADDW", OP_mm1_mm2m64);
    build_0f(0xFE, "PADDD", OP_mm1_mm2m64);
    build_0f(0xFF, "UD0", OP);

    has_built_tables = true;
}

Instruction Instruction::from_stream(InstructionStream& stream, bool o32, bool a32)
{
    build_opcode_tables_if_needed();
    return Instruction(stream, o32, a32);
}

unsigned Instruction::length() const
{
    unsigned len = 1;
    if (m_has_sub_op)
        ++len;
    if (m_has_rm) {
        ++len;
        if (m_modrm.m_has_sib)
            ++len;
        len += m_modrm.m_displacement_bytes;
    }
    len += m_imm1_bytes;
    len += m_imm2_bytes;
    len += m_prefix_bytes;
    return len;
}

static SegmentRegister to_segment_prefix(u8 op)
{
    switch (op) {
    case 0x26:
        return SegmentRegister::ES;
    case 0x2e:
        return SegmentRegister::CS;
    case 0x36:
        return SegmentRegister::SS;
    case 0x3e:
        return SegmentRegister::DS;
    case 0x64:
        return SegmentRegister::FS;
    case 0x65:
        return SegmentRegister::GS;
    default:
        return SegmentRegister::None;
    }
}

Instruction::Instruction(InstructionStream& stream, bool o32, bool a32)
    : m_a32(a32)
    , m_o32(o32)
{
    for (;; ++m_prefix_bytes) {
        u8 opbyte = stream.read8();
        if (opbyte == Prefix::OperandSizeOverride) {
            m_o32 = !o32;
            m_has_operand_size_override_prefix = true;
            continue;
        }
        if (opbyte == Prefix::AddressSizeOverride) {
            m_a32 = !a32;
            m_has_address_size_override_prefix = true;
            continue;
        }
        if (opbyte == Prefix::REPZ || opbyte == Prefix::REPNZ) {
            m_rep_prefix = opbyte;
            continue;
        }
        if (opbyte == Prefix::LOCK) {
            m_has_lock_prefix = true;
            continue;
        }
        auto segment_prefix = to_segment_prefix(opbyte);
        if (segment_prefix != SegmentRegister::None) {
            m_segment_prefix = segment_prefix;
            continue;
        }
        m_op = opbyte;
        break;
    }

    if (m_op == 0x0F) {
        m_has_sub_op = true;
        m_sub_op = stream.read8();
        m_descriptor = m_o32 ? &s_0f_table32[m_sub_op] : &s_0f_table16[m_sub_op];
    } else {
        m_descriptor = m_o32 ? &s_table32[m_op] : &s_table16[m_op];
    }

    m_has_rm = m_descriptor->has_rm;
    if (m_has_rm) {
        // Consume ModR/M (may include SIB and displacement.)
        m_modrm.decode(stream, m_a32);
        m_register_index = (m_modrm.m_rm >> 3) & 7;
    } else {
        if (m_has_sub_op)
            m_register_index = m_sub_op & 7;
        else
            m_register_index = m_op & 7;
    }

    bool hasSlash = m_descriptor->format == MultibyteWithSlash;

    if (hasSlash) {
        m_descriptor = &m_descriptor->slashes[slash()];
    }

    if (!m_descriptor->mnemonic) {
        if (m_has_sub_op) {
            if (hasSlash)
                fprintf(stderr, "Instruction %02X %02X /%u not understood\n", m_op, m_sub_op, slash());
            else
                fprintf(stderr, "Instruction %02X %02X not understood\n", m_op, m_sub_op);
        } else {
            if (hasSlash)
                fprintf(stderr, "Instruction %02X /%u not understood\n", m_op, slash());
            else
                fprintf(stderr, "Instruction %02X not understood\n", m_op);
        }
        m_descriptor = nullptr;
        return;
    }

    m_imm1_bytes = m_descriptor->imm1_bytes_for_address_size(m_a32);
    m_imm2_bytes = m_descriptor->imm2_bytes_for_address_size(m_a32);

    // Consume immediates if present.
    if (m_imm2_bytes)
        m_imm2 = stream.read(m_imm2_bytes);
    if (m_imm1_bytes)
        m_imm1 = stream.read(m_imm1_bytes);

#ifdef DISALLOW_INVALID_LOCK_PREFIX
    if (m_has_lock_prefix && !m_descriptor->lock_prefix_allowed) {
        fprintf(stderr, "Instruction not allowed with LOCK prefix, this will raise #UD\n");
        m_descriptor = nullptr;
    }
#endif
}

u32 InstructionStream::read(unsigned count)
{
    switch (count) {
    case 1:
        return read8();
    case 2:
        return read16();
    case 4:
        return read32();
    }
    ASSERT_NOT_REACHED();
    return 0;
}

static const char* register_name(RegisterIndex8);
static const char* register_name(RegisterIndex16);
static const char* register_name(RegisterIndex32);
static const char* register_name(SegmentRegister);
static const char* register_name(MMXRegisterIndex);

const char* Instruction::reg8_name() const
{
    return register_name(static_cast<RegisterIndex8>(register_index()));
}

const char* Instruction::reg16_name() const
{
    return register_name(static_cast<RegisterIndex16>(register_index()));
}

const char* Instruction::reg32_name() const
{
    return register_name(static_cast<RegisterIndex32>(register_index()));
}

String MemoryOrRegisterReference::to_string_o8() const
{
    if (is_register())
        return register_name(static_cast<RegisterIndex8>(m_register_index));
    return String::format("[%s]", to_string().characters());
}

String MemoryOrRegisterReference::to_string_o16() const
{
    if (is_register())
        return register_name(static_cast<RegisterIndex16>(m_register_index));
    return String::format("[%s]", to_string().characters());
}

String MemoryOrRegisterReference::to_string_o32() const
{
    if (is_register())
        return register_name(static_cast<RegisterIndex32>(m_register_index));
    return String::format("[%s]", to_string().characters());
}

String MemoryOrRegisterReference::to_string_mm() const
{
    if (is_register())
        return register_name(static_cast<MMXRegisterIndex>(m_register_index));
    return String::format("[%s]", to_string().characters());
}

String MemoryOrRegisterReference::to_string() const
{
    if (m_a32)
        return to_string_a32();
    return to_string_a16();
}

String MemoryOrRegisterReference::to_string_a16() const
{
    String base;
    bool hasDisplacement = false;

    switch (m_rm & 7) {
    case 0:
        base = "bx+si";
        break;
    case 1:
        base = "bx+di";
        break;
    case 2:
        base = "bp+si";
        break;
    case 3:
        base = "bp+di";
        break;
    case 4:
        base = "si";
        break;
    case 5:
        base = "di";
        break;
    case 7:
        base = "bx";
        break;
    case 6:
        if ((m_rm & 0xc0) == 0)
            base = String::format("%#04x", m_displacement16);
        else
            base = "bp";
        break;
    }

    switch (m_rm & 0xc0) {
    case 0x40:
    case 0x80:
        hasDisplacement = true;
    }

    if (!hasDisplacement)
        return base;

    String disp;
    if ((i16)m_displacement16 < 0)
        disp = String::format("-%#x", -(i16)m_displacement16);
    else
        String::format("+%#x", m_displacement16);
    return String::format("%s%s", base.characters(), disp.characters());
}

static String sib_to_string(u8 rm, u8 sib)
{
    String scale;
    String index;
    String base;
    switch (sib & 0xC0) {
    case 0x00:;
        break;
    case 0x40:
        scale = "*2";
        break;
    case 0x80:
        scale = "*4";
        break;
    case 0xC0:
        scale = "*8";
        break;
    }
    switch ((sib >> 3) & 0x07) {
    case 0:
        index = "eax";
        break;
    case 1:
        index = "ecx";
        break;
    case 2:
        index = "edx";
        break;
    case 3:
        index = "ebx";
        break;
    case 4:
        break;
    case 5:
        index = "ebp";
        break;
    case 6:
        index = "esi";
        break;
    case 7:
        index = "edi";
        break;
    }
    switch (sib & 0x07) {
    case 0:
        base = "eax";
        break;
    case 1:
        base = "ecx";
        break;
    case 2:
        base = "edx";
        break;
    case 3:
        base = "ebx";
        break;
    case 4:
        base = "esp";
        break;
    case 6:
        base = "esi";
        break;
    case 7:
        base = "edi";
        break;
    default: // 5
        switch ((rm >> 6) & 3) {
        case 1:
        case 2:
            base = "ebp";
            break;
        }
        break;
    }
    StringBuilder builder;
    if (base.is_empty()) {
        builder.append(index);
        builder.append(scale);
    } else {
        builder.append(base);
        if (!base.is_empty() && !index.is_empty())
            builder.append('+');
        builder.append(index);
        builder.append(scale);
    }
    return builder.to_string();
}

String MemoryOrRegisterReference::to_string_a32() const
{
    if (is_register())
        return register_name(static_cast<RegisterIndex32>(m_register_index));

    bool has_displacement = false;
    switch (m_rm & 0xc0) {
    case 0x40:
    case 0x80:
        has_displacement = true;
    }
    if (m_has_sib && (m_sib & 7) == 5)
        has_displacement = true;

    String base;
    switch (m_rm & 7) {
    case 0:
        base = "eax";
        break;
    case 1:
        base = "ecx";
        break;
    case 2:
        base = "edx";
        break;
    case 3:
        base = "ebx";
        break;
    case 6:
        base = "esi";
        break;
    case 7:
        base = "edi";
        break;
    case 5:
        if ((m_rm & 0xc0) == 0)
            base = String::format("%#08x", m_displacement32);
        else
            base = "ebp";
        break;
    case 4:
        base = sib_to_string(m_rm, m_sib);
        break;
    }

    if (!has_displacement)
        return base;

    String disp;
    if ((i32)m_displacement32 < 0)
        disp = String::format("-%#x", -(i32)m_displacement32);
    else
        disp = String::format("+%#x", m_displacement32);
    return String::format("%s%s", base.characters(), disp.characters());
}

static String relative_address(u32 origin, bool x32, i8 imm)
{
    if (x32)
        return String::format("%#08x", origin + imm);
    u16 w = origin & 0xffff;
    return String::format("%#04x", w + imm);
}

static String relative_address(u32 origin, bool x32, i32 imm)
{
    if (x32)
        return String::format("%#08x", origin + imm);
    u16 w = origin & 0xffff;
    i16 si = imm;
    return String::format("%#04x", w + si);
}

String Instruction::to_string(u32 origin, const SymbolProvider* symbol_provider, bool x32) const
{
    String segment_prefix;
    String asize_prefix;
    String osize_prefix;
    String rep_prefix;
    String lock_prefix;
    if (has_segment_prefix()) {
        segment_prefix = String::format("%s: ", register_name(m_segment_prefix));
    }
    if (has_address_size_override_prefix()) {
        asize_prefix = m_a32 ? "a32 " : "a16 ";
    }
    if (has_operand_size_override_prefix()) {
        osize_prefix = m_o32 ? "o32 " : "o16 ";
    }
    if (has_lock_prefix()) {
        lock_prefix = "lock ";
    }
    if (has_rep_prefix()) {
        rep_prefix = m_rep_prefix == Prefix::REPNZ ? "repnz " : "repz ";
    }
    StringBuilder builder;
    builder.append(segment_prefix);
    builder.append(asize_prefix);
    builder.append(osize_prefix);
    builder.append(lock_prefix);
    builder.append(rep_prefix);
    builder.append(to_string_internal(origin, symbol_provider, x32));
    return builder.to_string();
}

String Instruction::to_string_internal(u32 origin, const SymbolProvider* symbol_provider, bool x32) const
{
    if (!m_descriptor)
        return String::format("db %#02x", m_op);

    StringBuilder builder;

    String mnemonic = String(m_descriptor->mnemonic).to_lowercase();

    builder.append(mnemonic);
    builder.append(' ');

    auto formatted_address = [&](FlatPtr origin, bool x32, auto offset) {
        StringBuilder builder;
        builder.append(relative_address(origin, x32, offset));
        if (symbol_provider) {
            u32 symbol_offset = 0;
            auto symbol = symbol_provider->symbolicate(origin + offset, &symbol_offset);
            builder.append(" <");
            builder.append(symbol);
            if (symbol_offset)
                builder.appendf("+%u", symbol_offset);
            builder.append('>');
        }
        return builder.to_string();
    };

    auto append_rm8 = [&] { builder.append(m_modrm.to_string_o8()); };
    auto append_rm16 = [&] { builder.append(m_modrm.to_string_o16()); };
    auto append_rm32 = [&] { builder.append(m_modrm.to_string_o32()); };
    auto append_imm8 = [&] { builder.appendf("%#02x", imm8()); };
    auto append_imm8_2 = [&] { builder.appendf("%#02x", imm8_2()); };
    auto append_imm16 = [&] { builder.appendf("%#04x", imm16()); };
    auto append_imm16_1 = [&] { builder.appendf("%#04x", imm16_1()); };
    auto append_imm16_2 = [&] { builder.appendf("%#04x", imm16_2()); };
    auto append_imm32 = [&] { builder.appendf("%#08x", imm32()); };
    auto append_imm32_2 = [&] { builder.appendf("%#08x", imm32_2()); };
    auto append_reg8 = [&] { builder.append(reg8_name()); };
    auto append_reg16 = [&] { builder.append(reg16_name()); };
    auto append_reg32 = [&] { builder.append(reg32_name()); };
    auto append_seg = [&] { builder.append(register_name(segment_register())); };
    auto append_creg = [&] { builder.appendf("cr%u", register_index()); };
    auto append_dreg = [&] { builder.appendf("dr%u", register_index()); };
    auto append_relative_addr = [&] { builder.append(formatted_address(origin + (m_a32 ? 6 : 4), x32, i32(m_a32 ? imm32() : imm16()))); };
    auto append_relative_imm8 = [&] { builder.append(formatted_address(origin + 2, x32, i8(imm8()))); };
    auto append_relative_imm16 = [&] { builder.append(formatted_address(origin + 3, x32, i16(imm16()))); };
    auto append_relative_imm32 = [&] { builder.append(formatted_address(origin + 5, x32, i32(imm32()))); };

    auto append_mm = [&] { builder.appendf("mm%u", register_index()); };
    auto append_mmrm64 = [&] { builder.append(m_modrm.to_string_mm()); };

    auto append = [&](auto& content) { builder.append(content); };
    auto append_moff = [&] {
        builder.append('[');
        if (m_a32) {
            append_imm32();
        } else {
            append_imm16();
        }
        builder.append(']');
    };

    switch (m_descriptor->format) {
    case OP_RM8_imm8:
        append_rm8();
        append(", ");
        append_imm8();
        break;
    case OP_RM16_imm8:
        append_rm16();
        append(", ");
        append_imm8();
        break;
    case OP_RM32_imm8:
        append_rm32();
        append(", ");
        append_imm8();
        break;
    case OP_reg16_RM16_imm8:
        append_reg16();
        append(", ");
        append_rm16();
        append(", ");
        append_imm8();
        break;
    case OP_reg32_RM32_imm8:
        append_reg32();
        append(", ");
        append_rm32();
        append(", ");
        append_imm8();
        break;
    case OP_AL_imm8:
        append("al, ");
        append_imm8();
        break;
    case OP_imm8:
        append_imm8();
        break;
    case OP_reg8_imm8:
        append_reg8();
        append(", ");
        append_imm8();
        break;
    case OP_AX_imm8:
        append("ax, ");
        append_imm8();
        break;
    case OP_EAX_imm8:
        append("eax, ");
        append_imm8();
        break;
    case OP_imm8_AL:
        append_imm8();
        append(", al");
        break;
    case OP_imm8_AX:
        append_imm8();
        append(", ax");
        break;
    case OP_imm8_EAX:
        append_imm8();
        append(", eax");
        break;
    case OP_AX_imm16:
        append("ax, ");
        append_imm16();
        break;
    case OP_imm16:
        append_imm16();
        break;
    case OP_reg16_imm16:
        append_reg16();
        append(", ");
        append_imm16();
        break;
    case OP_reg16_RM16_imm16:
        append_reg16();
        append(", ");
        append_rm16();
        append(", ");
        append_imm16();
        break;
    case OP_reg32_RM32_imm32:
        append_reg32();
        append(", ");
        append_rm32();
        append(", ");
        append_imm32();
        break;
    case OP_imm32:
        append_imm32();
        break;
    case OP_EAX_imm32:
        append("eax, ");
        append_imm32();
        break;
    case OP_CS:
        append("cs");
        break;
    case OP_DS:
        append("ds");
        break;
    case OP_ES:
        append("es");
        break;
    case OP_SS:
        append("ss");
        break;
    case OP_FS:
        append("fs");
        break;
    case OP_GS:
        append("gs");
        break;
    case OP:
        break;
    case OP_reg32:
        append_reg32();
        break;
    case OP_imm16_imm8:
        append_imm16_1();
        append(", ");
        append_imm8_2();
        break;
    case OP_moff8_AL:
        append_moff();
        append(", al");
        break;
    case OP_moff16_AX:
        append_moff();
        append(", ax");
        break;
    case OP_moff32_EAX:
        append_moff();
        append(", eax");
        break;
    case OP_AL_moff8:
        append("al, ");
        append_moff();
        break;
    case OP_AX_moff16:
        append("ax, ");
        append_moff();
        break;
    case OP_EAX_moff32:
        append("eax, ");
        append_moff();
        break;
    case OP_imm16_imm16:
        append_imm16_1();
        append(":");
        append_imm16_2();
        break;
    case OP_imm16_imm32:
        append_imm16_1();
        append(":");
        append_imm32_2();
        break;
    case OP_reg32_imm32:
        append_reg32();
        append(", ");
        append_imm32();
        break;
    case OP_RM8_1:
        append_rm8();
        append(", 0x01");
        break;
    case OP_RM16_1:
        append_rm16();
        append(", 0x01");
        break;
    case OP_RM32_1:
        append_rm32();
        append(", 0x01");
        break;
    case OP_RM8_CL:
        append_rm8();
        append(", cl");
        break;
    case OP_RM16_CL:
        append_rm16();
        append(", cl");
        break;
    case OP_RM32_CL:
        append_rm32();
        append(", cl");
        break;
    case OP_reg16:
        append_reg16();
        break;
    case OP_AX_reg16:
        append("ax, ");
        append_reg16();
        break;
    case OP_EAX_reg32:
        append("eax, ");
        append_reg32();
        break;
    case OP_3:
        append("0x03");
        break;
    case OP_AL_DX:
        append("al, dx");
        break;
    case OP_AX_DX:
        append("ax, dx");
        break;
    case OP_EAX_DX:
        append("eax, dx");
        break;
    case OP_DX_AL:
        append("dx, al");
        break;
    case OP_DX_AX:
        append("dx, ax");
        break;
    case OP_DX_EAX:
        append("dx, eax");
        break;
    case OP_reg8_CL:
        append_reg8();
        append(", cl");
        break;
    case OP_RM8:
        append_rm8();
        break;
    case OP_RM16:
        append_rm16();
        break;
    case OP_RM32:
        append_rm32();
        break;
    case OP_RM8_reg8:
        append_rm8();
        append(", ");
        append_reg8();
        break;
    case OP_RM16_reg16:
        append_rm16();
        append(", ");
        append_reg16();
        break;
    case OP_RM32_reg32:
        append_rm32();
        append(", ");
        append_reg32();
        break;
    case OP_reg8_RM8:
        append_reg8();
        append(", ");
        append_rm8();
        break;
    case OP_reg16_RM16:
        append_reg16();
        append(", ");
        append_rm16();
        break;
    case OP_reg32_RM32:
        append_reg32();
        append(", ");
        append_rm32();
        break;
    case OP_reg32_RM16:
        append_reg32();
        append(", ");
        append_rm16();
        break;
    case OP_reg16_RM8:
        append_reg16();
        append(", ");
        append_rm8();
        break;
    case OP_reg32_RM8:
        append_reg32();
        append(", ");
        append_rm8();
        break;
    case OP_RM16_imm16:
        append_rm16();
        append(", ");
        append_imm16();
        break;
    case OP_RM32_imm32:
        append_rm32();
        append(", ");
        append_imm32();
        break;
    case OP_RM16_seg:
        append_rm16();
        append(", ");
        append_seg();
        break;
    case OP_RM32_seg:
        append_rm32();
        append(", ");
        append_seg();
        break;
    case OP_seg_RM16:
        append_seg();
        append(", ");
        append_rm16();
        break;
    case OP_seg_RM32:
        append_seg();
        append(", ");
        append_rm32();
        break;
    case OP_reg16_mem16:
        append_reg16();
        append(", ");
        append_rm16();
        break;
    case OP_reg32_mem32:
        append_reg32();
        append(", ");
        append_rm32();
        break;
    case OP_FAR_mem16:
        append("far ");
        append_rm16();
        break;
    case OP_FAR_mem32:
        append("far ");
        append_rm32();
        break;
    case OP_reg32_CR:
        builder.append(register_name(static_cast<RegisterIndex32>(rm() & 7)));
        append(", ");
        append_creg();
        break;
    case OP_CR_reg32:
        append_creg();
        append(", ");
        builder.append(register_name(static_cast<RegisterIndex32>(rm() & 7)));
        break;
    case OP_reg32_DR:
        builder.append(register_name(static_cast<RegisterIndex32>(rm() & 7)));
        append(", ");
        append_dreg();
        break;
    case OP_DR_reg32:
        append_dreg();
        append(", ");
        builder.append(register_name(static_cast<RegisterIndex32>(rm() & 7)));
        break;
    case OP_short_imm8:
        append("short ");
        append_relative_imm8();
        break;
    case OP_relimm16:
        append_relative_imm16();
        break;
    case OP_relimm32:
        append_relative_imm32();
        break;
    case OP_NEAR_imm:
        append("near ");
        append_relative_addr();
        break;
    case OP_RM16_reg16_imm8:
        append_rm16();
        append(", ");
        append_reg16();
        append(", ");
        append_imm8();
        break;
    case OP_RM32_reg32_imm8:
        append_rm32();
        append(", ");
        append_reg32();
        append(", ");
        append_imm8();
        break;
    case OP_RM16_reg16_CL:
        append_rm16();
        append(", ");
        append_reg16();
        append(", cl");
        break;
    case OP_RM32_reg32_CL:
        append_rm32();
        append(", ");
        append_reg32();
        append(", cl");
        break;
    case OP_mm1_mm2m64:
        append_mm();
        append(", ");
        append_mmrm64();
        break;
    case OP_mm1m64_mm2:
        append_mm();
        append(", ");
        append_mmrm64();
        break;
    case InstructionPrefix:
        return mnemonic;
    case InvalidFormat:
    case MultibyteWithSlash:
    case MultibyteWithSubopcode:
    case __BeginFormatsWithRMByte:
    case __EndFormatsWithRMByte:
        return String::format("(!%s)", mnemonic.characters());
    }
    return builder.to_string();
}

String Instruction::mnemonic() const
{
    if (!m_descriptor) {
        ASSERT_NOT_REACHED();
    }
    return m_descriptor->mnemonic;
}

const char* register_name(SegmentRegister index)
{
    static constexpr const char* names[] = { "es", "cs", "ss", "ds", "fs", "gs", "segr6", "segr7" };
    return names[(int)index & 7];
}

const char* register_name(RegisterIndex8 register_index)
{
    static constexpr const char* names[] = { "al", "cl", "dl", "bl", "ah", "ch", "dh", "bh" };
    return names[register_index & 7];
}

const char* register_name(RegisterIndex16 register_index)
{
    static constexpr const char* names[] = { "ax", "cx", "dx", "bx", "sp", "bp", "si", "di" };
    return names[register_index & 7];
}

const char* register_name(RegisterIndex32 register_index)
{
    static constexpr const char* names[] = { "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi" };
    return names[register_index & 7];
}

const char* register_name(MMXRegisterIndex register_index)
{
    static constexpr const char* names[] = { "mm0", "mm1", "mm2", "mm3", "mm4", "mm5", "mm6", "mm7" };
    return names[register_index & 7];
}

void MemoryOrRegisterReference::decode(InstructionStream& stream, bool a32)
{
    m_a32 = a32;
    m_rm = stream.read8();

    if (m_a32) {
        decode32(stream);
        switch (m_displacement_bytes) {
        case 0:
            break;
        case 1:
            m_displacement32 = sign_extended_to<u32>(stream.read8());
            break;
        case 4:
            m_displacement32 = stream.read32();
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
    } else {
        decode16(stream);
        switch (m_displacement_bytes) {
        case 0:
            break;
        case 1:
            m_displacement16 = sign_extended_to<u16>(stream.read8());
            break;
        case 2:
            m_displacement16 = stream.read16();
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
    }
}

void MemoryOrRegisterReference::decode16(InstructionStream&)
{
    ASSERT(!m_a32);

    switch (m_rm & 0xc0) {
    case 0:
        if ((m_rm & 0x07) == 6)
            m_displacement_bytes = 2;
        else
            ASSERT(m_displacement_bytes == 0);
        break;
    case 0x40:
        m_displacement_bytes = 1;
        break;
    case 0x80:
        m_displacement_bytes = 2;
        break;
    case 0xc0:
        m_register_index = m_rm & 7;
        break;
    }
}

void MemoryOrRegisterReference::decode32(InstructionStream& stream)
{
    ASSERT(m_a32);

    switch (m_rm & 0xc0) {
    case 0:
        if ((m_rm & 0x07) == 5)
            m_displacement_bytes = 4;
        break;
    case 0x40:
        m_displacement_bytes = 1;
        break;
    case 0x80:
        m_displacement_bytes = 4;
        break;
    case 0xc0:
        m_register_index = m_rm & 7;
        return;
    }

    m_has_sib = (m_rm & 0x07) == 4;
    if (m_has_sib) {
        m_sib = stream.read8();
        if ((m_sib & 0x07) == 5) {
            switch ((m_rm >> 6) & 0x03) {
            case 0:
                ASSERT(!m_displacement_bytes || m_displacement_bytes == 4);
                m_displacement_bytes = 4;
                break;
            case 1:
                ASSERT(!m_displacement_bytes || m_displacement_bytes == 1);
                m_displacement_bytes = 1;
                break;
            case 2:
                ASSERT(!m_displacement_bytes || m_displacement_bytes == 4);
                m_displacement_bytes = 4;
                break;
            default:
                ASSERT_NOT_REACHED();
                break;
            }
        }
    }
}

}
