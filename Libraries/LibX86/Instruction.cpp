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
#include <LibX86/Interpreter.h>
#include <stdio.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace X86 {

InstructionDescriptor s_table16[256];
InstructionDescriptor s_table32[256];
InstructionDescriptor s_0f_table16[256];
InstructionDescriptor s_0f_table32[256];

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

static void build(InstructionDescriptor* table, u8 op, const char* mnemonic, InstructionFormat format, InstructionHandler handler, IsLockPrefixAllowed lock_prefix_allowed)
{
    InstructionDescriptor& d = table[op];

    d.handler = handler;
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
    case OP_FPU:
    case OP_FPU_reg:
    case OP_FPU_mem:
    case OP_FPU_AX16:
    case OP_FPU_RM16:
    case OP_FPU_RM32:
    case OP_FPU_RM64:
    case OP_FPU_M80:
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

static void build_slash(InstructionDescriptor* table, u8 op, u8 slash, const char* mnemonic, InstructionFormat format, InstructionHandler handler, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    InstructionDescriptor& d = table[op];
    ASSERT(d.handler == nullptr);
    d.format = MultibyteWithSlash;
    d.has_rm = true;
    if (!d.slashes)
        d.slashes = new InstructionDescriptor[8];

    build(d.slashes, slash, mnemonic, format, handler, lock_prefix_allowed);
}

static void build_slash_rm(InstructionDescriptor* table, u8 op, u8 slash, u8 rm, const char* mnemonic, InstructionFormat format, InstructionHandler handler)
{
    ASSERT((rm & 0xc0) == 0xc0);
    ASSERT(((rm >> 3) & 7) == slash);

    InstructionDescriptor& d0 = table[op];
    ASSERT(d0.format == MultibyteWithSlash);
    InstructionDescriptor& d = d0.slashes[slash];

    if (!d.slashes) {
        // Slash/RM instructions are not always dense, so make them all default to the slash instruction.
        d.slashes = new InstructionDescriptor[8];
        for (int i = 0; i < 8; ++i) {
            d.slashes[i] = d;
            d.slashes[i].slashes = nullptr;
        }
    }

    build(d.slashes, rm & 7, mnemonic, format, handler, LockPrefixNotAllowed);
}

static void build_0f(u8 op, const char* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_0f_table16, op, mnemonic, format, impl, lock_prefix_allowed);
    build(s_0f_table32, op, mnemonic, format, impl, lock_prefix_allowed);
}

static void build(u8 op, const char* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_table16, op, mnemonic, format, impl, lock_prefix_allowed);
    build(s_table32, op, mnemonic, format, impl, lock_prefix_allowed);
}

static void build(u8 op, const char* mnemonic, InstructionFormat format16, InstructionHandler impl16, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_table16, op, mnemonic, format16, impl16, lock_prefix_allowed);
    build(s_table32, op, mnemonic, format32, impl32, lock_prefix_allowed);
}

static void build_0f(u8 op, const char* mnemonic, InstructionFormat format16, InstructionHandler impl16, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_0f_table16, op, mnemonic, format16, impl16, lock_prefix_allowed);
    build(s_0f_table32, op, mnemonic, format32, impl32, lock_prefix_allowed);
}

static void build(u8 op, const char* mnemonic16, InstructionFormat format16, InstructionHandler impl16, const char* mnemonic32, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_table16, op, mnemonic16, format16, impl16, lock_prefix_allowed);
    build(s_table32, op, mnemonic32, format32, impl32, lock_prefix_allowed);
}

static void build_0f(u8 op, const char* mnemonic16, InstructionFormat format16, InstructionHandler impl16, const char* mnemonic32, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build(s_0f_table16, op, mnemonic16, format16, impl16, lock_prefix_allowed);
    build(s_0f_table32, op, mnemonic32, format32, impl32, lock_prefix_allowed);
}

static void build_slash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_table16, op, slash, mnemonic, format, impl, lock_prefix_allowed);
    build_slash(s_table32, op, slash, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_slash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format16, InstructionHandler impl16, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_table16, op, slash, mnemonic, format16, impl16, lock_prefix_allowed);
    build_slash(s_table32, op, slash, mnemonic, format32, impl32, lock_prefix_allowed);
}

static void build_0f_slash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format16, InstructionHandler impl16, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_0f_table16, op, slash, mnemonic, format16, impl16, lock_prefix_allowed);
    build_slash(s_0f_table32, op, slash, mnemonic, format32, impl32, lock_prefix_allowed);
}

static void build_0f_slash(u8 op, u8 slash, const char* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(s_0f_table16, op, slash, mnemonic, format, impl, lock_prefix_allowed);
    build_slash(s_0f_table32, op, slash, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_slash_rm(u8 op, u8 slash, u8 rm, const char* mnemonic, InstructionFormat format, InstructionHandler impl)
{
    build_slash_rm(s_table16, op, slash, rm, mnemonic, format, impl);
    build_slash_rm(s_table32, op, slash, rm, mnemonic, format, impl);
}

static void build_slash_reg(u8 op, u8 slash, const char* mnemonic, InstructionFormat format, InstructionHandler impl)
{
    for (int i = 0; i < 8; ++i)
        build_slash_rm(op, slash, 0xc0 | (slash << 3) | i, mnemonic, format, impl);
}

[[gnu::constructor]] static void build_opcode_tables()
{
    build(0x00, "ADD", OP_RM8_reg8, &Interpreter::ADD_RM8_reg8, LockPrefixAllowed);
    build(0x01, "ADD", OP_RM16_reg16, &Interpreter::ADD_RM16_reg16, OP_RM32_reg32, &Interpreter::ADD_RM32_reg32, LockPrefixAllowed);
    build(0x02, "ADD", OP_reg8_RM8, &Interpreter::ADD_reg8_RM8, LockPrefixAllowed);
    build(0x03, "ADD", OP_reg16_RM16, &Interpreter::ADD_reg16_RM16, OP_reg32_RM32, &Interpreter::ADD_reg32_RM32, LockPrefixAllowed);
    build(0x04, "ADD", OP_AL_imm8, &Interpreter::ADD_AL_imm8);
    build(0x05, "ADD", OP_AX_imm16, &Interpreter::ADD_AX_imm16, OP_EAX_imm32, &Interpreter::ADD_EAX_imm32);
    build(0x06, "PUSH", OP_ES, &Interpreter::PUSH_ES);
    build(0x07, "POP", OP_ES, &Interpreter::POP_ES);
    build(0x08, "OR", OP_RM8_reg8, &Interpreter::OR_RM8_reg8, LockPrefixAllowed);
    build(0x09, "OR", OP_RM16_reg16, &Interpreter::OR_RM16_reg16, OP_RM32_reg32, &Interpreter::OR_RM32_reg32, LockPrefixAllowed);
    build(0x0A, "OR", OP_reg8_RM8, &Interpreter::OR_reg8_RM8, LockPrefixAllowed);
    build(0x0B, "OR", OP_reg16_RM16, &Interpreter::OR_reg16_RM16, OP_reg32_RM32, &Interpreter::OR_reg32_RM32, LockPrefixAllowed);
    build(0x0C, "OR", OP_AL_imm8, &Interpreter::OR_AL_imm8);
    build(0x0D, "OR", OP_AX_imm16, &Interpreter::OR_AX_imm16, OP_EAX_imm32, &Interpreter::OR_EAX_imm32);
    build(0x0E, "PUSH", OP_CS, &Interpreter::PUSH_CS);

    build(0x10, "ADC", OP_RM8_reg8, &Interpreter::ADC_RM8_reg8, LockPrefixAllowed);
    build(0x11, "ADC", OP_RM16_reg16, &Interpreter::ADC_RM16_reg16, OP_RM32_reg32, &Interpreter::ADC_RM32_reg32, LockPrefixAllowed);
    build(0x12, "ADC", OP_reg8_RM8, &Interpreter::ADC_reg8_RM8, LockPrefixAllowed);
    build(0x13, "ADC", OP_reg16_RM16, &Interpreter::ADC_reg16_RM16, OP_reg32_RM32, &Interpreter::ADC_reg32_RM32, LockPrefixAllowed);
    build(0x14, "ADC", OP_AL_imm8, &Interpreter::ADC_AL_imm8);
    build(0x15, "ADC", OP_AX_imm16, &Interpreter::ADC_AX_imm16, OP_EAX_imm32, &Interpreter::ADC_EAX_imm32);
    build(0x16, "PUSH", OP_SS, &Interpreter::PUSH_SS);
    build(0x17, "POP", OP_SS, &Interpreter::POP_SS);
    build(0x18, "SBB", OP_RM8_reg8, &Interpreter::SBB_RM8_reg8, LockPrefixAllowed);
    build(0x19, "SBB", OP_RM16_reg16, &Interpreter::SBB_RM16_reg16, OP_RM32_reg32, &Interpreter::SBB_RM32_reg32, LockPrefixAllowed);
    build(0x1A, "SBB", OP_reg8_RM8, &Interpreter::SBB_reg8_RM8, LockPrefixAllowed);
    build(0x1B, "SBB", OP_reg16_RM16, &Interpreter::SBB_reg16_RM16, OP_reg32_RM32, &Interpreter::SBB_reg32_RM32, LockPrefixAllowed);
    build(0x1C, "SBB", OP_AL_imm8, &Interpreter::SBB_AL_imm8);
    build(0x1D, "SBB", OP_AX_imm16, &Interpreter::SBB_AX_imm16, OP_EAX_imm32, &Interpreter::SBB_EAX_imm32);
    build(0x1E, "PUSH", OP_DS, &Interpreter::PUSH_DS);
    build(0x1F, "POP", OP_DS, &Interpreter::POP_DS);

    build(0x20, "AND", OP_RM8_reg8, &Interpreter::AND_RM8_reg8, LockPrefixAllowed);
    build(0x21, "AND", OP_RM16_reg16, &Interpreter::AND_RM16_reg16, OP_RM32_reg32, &Interpreter::AND_RM32_reg32, LockPrefixAllowed);
    build(0x22, "AND", OP_reg8_RM8, &Interpreter::AND_reg8_RM8, LockPrefixAllowed);
    build(0x23, "AND", OP_reg16_RM16, &Interpreter::AND_reg16_RM16, OP_reg32_RM32, &Interpreter::AND_reg32_RM32, LockPrefixAllowed);
    build(0x24, "AND", OP_AL_imm8, &Interpreter::AND_AL_imm8);
    build(0x25, "AND", OP_AX_imm16, &Interpreter::AND_AX_imm16, OP_EAX_imm32, &Interpreter::AND_EAX_imm32);
    build(0x27, "DAA", OP, &Interpreter::DAA);
    build(0x28, "SUB", OP_RM8_reg8, &Interpreter::SUB_RM8_reg8, LockPrefixAllowed);
    build(0x29, "SUB", OP_RM16_reg16, &Interpreter::SUB_RM16_reg16, OP_RM32_reg32, &Interpreter::SUB_RM32_reg32, LockPrefixAllowed);
    build(0x2A, "SUB", OP_reg8_RM8, &Interpreter::SUB_reg8_RM8, LockPrefixAllowed);
    build(0x2B, "SUB", OP_reg16_RM16, &Interpreter::SUB_reg16_RM16, OP_reg32_RM32, &Interpreter::SUB_reg32_RM32, LockPrefixAllowed);
    build(0x2C, "SUB", OP_AL_imm8, &Interpreter::SUB_AL_imm8);
    build(0x2D, "SUB", OP_AX_imm16, &Interpreter::SUB_AX_imm16, OP_EAX_imm32, &Interpreter::SUB_EAX_imm32);
    build(0x2F, "DAS", OP, &Interpreter::DAS);

    build(0x30, "XOR", OP_RM8_reg8, &Interpreter::XOR_RM8_reg8, LockPrefixAllowed);
    build(0x31, "XOR", OP_RM16_reg16, &Interpreter::XOR_RM16_reg16, OP_RM32_reg32, &Interpreter::XOR_RM32_reg32, LockPrefixAllowed);
    build(0x32, "XOR", OP_reg8_RM8, &Interpreter::XOR_reg8_RM8, LockPrefixAllowed);
    build(0x33, "XOR", OP_reg16_RM16, &Interpreter::XOR_reg16_RM16, OP_reg32_RM32, &Interpreter::XOR_reg32_RM32, LockPrefixAllowed);
    build(0x34, "XOR", OP_AL_imm8, &Interpreter::XOR_AL_imm8);
    build(0x35, "XOR", OP_AX_imm16, &Interpreter::XOR_AX_imm16, OP_EAX_imm32, &Interpreter::XOR_EAX_imm32);
    build(0x37, "AAA", OP, &Interpreter::AAA);
    build(0x38, "CMP", OP_RM8_reg8, &Interpreter::CMP_RM8_reg8, LockPrefixAllowed);
    build(0x39, "CMP", OP_RM16_reg16, &Interpreter::CMP_RM16_reg16, OP_RM32_reg32, &Interpreter::CMP_RM32_reg32, LockPrefixAllowed);
    build(0x3A, "CMP", OP_reg8_RM8, &Interpreter::CMP_reg8_RM8, LockPrefixAllowed);
    build(0x3B, "CMP", OP_reg16_RM16, &Interpreter::CMP_reg16_RM16, OP_reg32_RM32, &Interpreter::CMP_reg32_RM32, LockPrefixAllowed);
    build(0x3C, "CMP", OP_AL_imm8, &Interpreter::CMP_AL_imm8);
    build(0x3D, "CMP", OP_AX_imm16, &Interpreter::CMP_AX_imm16, OP_EAX_imm32, &Interpreter::CMP_EAX_imm32);
    build(0x3F, "AAS", OP, &Interpreter::AAS);

    for (u8 i = 0; i <= 7; ++i)
        build(0x40 + i, "INC", OP_reg16, &Interpreter::INC_reg16, OP_reg32, &Interpreter::INC_reg32);

    for (u8 i = 0; i <= 7; ++i)
        build(0x48 + i, "DEC", OP_reg16, &Interpreter::DEC_reg16, OP_reg32, &Interpreter::DEC_reg32);

    for (u8 i = 0; i <= 7; ++i)
        build(0x50 + i, "PUSH", OP_reg16, &Interpreter::PUSH_reg16, OP_reg32, &Interpreter::PUSH_reg32);

    for (u8 i = 0; i <= 7; ++i)
        build(0x58 + i, "POP", OP_reg16, &Interpreter::POP_reg16, OP_reg32, &Interpreter::POP_reg32);

    build(0x60, "PUSHAW", OP, &Interpreter::PUSHA, "PUSHAD", OP, &Interpreter::PUSHAD);
    build(0x61, "POPAW", OP, &Interpreter::POPA, "POPAD", OP, &Interpreter::POPAD);
    build(0x62, "BOUND", OP_reg16_RM16, &Interpreter::BOUND, "BOUND", OP_reg32_RM32, &Interpreter::BOUND);
    build(0x63, "ARPL", OP_RM16_reg16, &Interpreter::ARPL);

    build(0x68, "PUSH", OP_imm16, &Interpreter::PUSH_imm16, OP_imm32, &Interpreter::PUSH_imm32);
    build(0x69, "IMUL", OP_reg16_RM16_imm16, &Interpreter::IMUL_reg16_RM16_imm16, OP_reg32_RM32_imm32, &Interpreter::IMUL_reg32_RM32_imm32);
    build(0x6A, "PUSH", OP_imm8, &Interpreter::PUSH_imm8);
    build(0x6B, "IMUL", OP_reg16_RM16_imm8, &Interpreter::IMUL_reg16_RM16_imm8, OP_reg32_RM32_imm8, &Interpreter::IMUL_reg32_RM32_imm8);
    build(0x6C, "INSB", OP, &Interpreter::INSB);
    build(0x6D, "INSW", OP, &Interpreter::INSW, "INSD", OP, &Interpreter::INSD);
    build(0x6E, "OUTSB", OP, &Interpreter::OUTSB);
    build(0x6F, "OUTSW", OP, &Interpreter::OUTSW, "OUTSD", OP, &Interpreter::OUTSD);

    build(0x70, "JO", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x71, "JNO", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x72, "JC", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x73, "JNC", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x74, "JZ", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x75, "JNZ", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x76, "JNA", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x77, "JA", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x78, "JS", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x79, "JNS", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x7A, "JP", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x7B, "JNP", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x7C, "JL", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x7D, "JNL", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x7E, "JNG", OP_short_imm8, &Interpreter::Jcc_imm8);
    build(0x7F, "JG", OP_short_imm8, &Interpreter::Jcc_imm8);

    build(0x84, "TEST", OP_RM8_reg8, &Interpreter::TEST_RM8_reg8);
    build(0x85, "TEST", OP_RM16_reg16, &Interpreter::TEST_RM16_reg16, OP_RM32_reg32, &Interpreter::TEST_RM32_reg32);
    build(0x86, "XCHG", OP_reg8_RM8, &Interpreter::XCHG_reg8_RM8, LockPrefixAllowed);
    build(0x87, "XCHG", OP_reg16_RM16, &Interpreter::XCHG_reg16_RM16, OP_reg32_RM32, &Interpreter::XCHG_reg32_RM32, LockPrefixAllowed);
    build(0x88, "MOV", OP_RM8_reg8, &Interpreter::MOV_RM8_reg8);
    build(0x89, "MOV", OP_RM16_reg16, &Interpreter::MOV_RM16_reg16, OP_RM32_reg32, &Interpreter::MOV_RM32_reg32);
    build(0x8A, "MOV", OP_reg8_RM8, &Interpreter::MOV_reg8_RM8);
    build(0x8B, "MOV", OP_reg16_RM16, &Interpreter::MOV_reg16_RM16, OP_reg32_RM32, &Interpreter::MOV_reg32_RM32);
    build(0x8C, "MOV", OP_RM16_seg, &Interpreter::MOV_RM16_seg);
    build(0x8D, "LEA", OP_reg16_mem16, &Interpreter::LEA_reg16_mem16, OP_reg32_mem32, &Interpreter::LEA_reg32_mem32);
    build(0x8E, "MOV", OP_seg_RM16, &Interpreter::MOV_seg_RM16, OP_seg_RM32, &Interpreter::MOV_seg_RM32);

    build(0x90, "NOP", OP, &Interpreter::NOP);

    for (u8 i = 0; i <= 6; ++i)
        build(0x91 + i, "XCHG", OP_AX_reg16, &Interpreter::XCHG_AX_reg16, OP_EAX_reg32, &Interpreter::XCHG_EAX_reg32);

    build(0x98, "CBW", OP, &Interpreter::CBW, "CWDE", OP, &Interpreter::CWDE);
    build(0x99, "CWD", OP, &Interpreter::CWD, "CDQ", OP, &Interpreter::CDQ);
    build(0x9A, "CALL", OP_imm16_imm16, &Interpreter::CALL_imm16_imm16, OP_imm16_imm32, &Interpreter::CALL_imm16_imm32);
    build(0x9B, "WAIT", OP, &Interpreter::WAIT);
    build(0x9C, "PUSHFW", OP, &Interpreter::PUSHF, "PUSHFD", OP, &Interpreter::PUSHFD);
    build(0x9D, "POPFW", OP, &Interpreter::POPF, "POPFD", OP, &Interpreter::POPFD);
    build(0x9E, "SAHF", OP, &Interpreter::SAHF);
    build(0x9F, "LAHF", OP, &Interpreter::LAHF);

    build(0xA0, "MOV", OP_AL_moff8, &Interpreter::MOV_AL_moff8);
    build(0xA1, "MOV", OP_AX_moff16, &Interpreter::MOV_AX_moff16, OP_EAX_moff32, &Interpreter::MOV_EAX_moff32);
    build(0xA2, "MOV", OP_moff8_AL, &Interpreter::MOV_moff8_AL);
    build(0xA3, "MOV", OP_moff16_AX, &Interpreter::MOV_moff16_AX, OP_moff32_EAX, &Interpreter::MOV_moff32_EAX);
    build(0xA4, "MOVSB", OP, &Interpreter::MOVSB);
    build(0xA5, "MOVSW", OP, &Interpreter::MOVSW, "MOVSD", OP, &Interpreter::MOVSD);
    build(0xA6, "CMPSB", OP, &Interpreter::CMPSB);
    build(0xA7, "CMPSW", OP, &Interpreter::CMPSW, "CMPSD", OP, &Interpreter::CMPSD);
    build(0xA8, "TEST", OP_AL_imm8, &Interpreter::TEST_AL_imm8);
    build(0xA9, "TEST", OP_AX_imm16, &Interpreter::TEST_AX_imm16, OP_EAX_imm32, &Interpreter::TEST_EAX_imm32);
    build(0xAA, "STOSB", OP, &Interpreter::STOSB);
    build(0xAB, "STOSW", OP, &Interpreter::STOSW, "STOSD", OP, &Interpreter::STOSD);
    build(0xAC, "LODSB", OP, &Interpreter::LODSB);
    build(0xAD, "LODSW", OP, &Interpreter::LODSW, "LODSD", OP, &Interpreter::LODSD);
    build(0xAE, "SCASB", OP, &Interpreter::SCASB);
    build(0xAF, "SCASW", OP, &Interpreter::SCASW, "SCASD", OP, &Interpreter::SCASD);

    for (u8 i = 0xb0; i <= 0xb7; ++i)
        build(i, "MOV", OP_reg8_imm8, &Interpreter::MOV_reg8_imm8);

    for (u8 i = 0xb8; i <= 0xbf; ++i)
        build(i, "MOV", OP_reg16_imm16, &Interpreter::MOV_reg16_imm16, OP_reg32_imm32, &Interpreter::MOV_reg32_imm32);

    build(0xC2, "RET", OP_imm16, &Interpreter::RET_imm16);
    build(0xC3, "RET", OP, &Interpreter::RET);
    build(0xC4, "LES", OP_reg16_mem16, &Interpreter::LES_reg16_mem16, OP_reg32_mem32, &Interpreter::LES_reg32_mem32);
    build(0xC5, "LDS", OP_reg16_mem16, &Interpreter::LDS_reg16_mem16, OP_reg32_mem32, &Interpreter::LDS_reg32_mem32);
    build(0xC6, "MOV", OP_RM8_imm8, &Interpreter::MOV_RM8_imm8);
    build(0xC7, "MOV", OP_RM16_imm16, &Interpreter::MOV_RM16_imm16, OP_RM32_imm32, &Interpreter::MOV_RM32_imm32);
    build(0xC8, "ENTER", OP_imm16_imm8, &Interpreter::ENTER16, OP_imm16_imm8, &Interpreter::ENTER32);
    build(0xC9, "LEAVE", OP, &Interpreter::LEAVE16, OP, &Interpreter::LEAVE32);
    build(0xCA, "RETF", OP_imm16, &Interpreter::RETF_imm16);
    build(0xCB, "RETF", OP, &Interpreter::RETF);
    build(0xCC, "INT3", OP_3, &Interpreter::INT3);
    build(0xCD, "INT", OP_imm8, &Interpreter::INT_imm8);
    build(0xCE, "INTO", OP, &Interpreter::INTO);
    build(0xCF, "IRET", OP, &Interpreter::IRET);

    build(0xD4, "AAM", OP_imm8, &Interpreter::AAM);
    build(0xD5, "AAD", OP_imm8, &Interpreter::AAD);
    build(0xD6, "SALC", OP, &Interpreter::SALC);
    build(0xD7, "XLAT", OP, &Interpreter::XLAT);

    // D8-DF == FPU
    build_slash(0xD8, 0, "FADD", OP_FPU_RM32, &Interpreter::FADD_RM32);
    build_slash(0xD8, 1, "FMUL", OP_FPU_RM32, &Interpreter::FMUL_RM32);
    build_slash(0xD8, 2, "FCOM", OP_FPU_RM32, &Interpreter::FCOM_RM32);
    // FIXME: D8/2 D1 (...but isn't this what D8/2 does naturally, with D1 just being normal R/M?)
    build_slash(0xD8, 3, "FCOMP", OP_FPU_RM32, &Interpreter::FCOMP_RM32);
    // FIXME: D8/3 D9 (...but isn't this what D8/3 does naturally, with D9 just being normal R/M?)
    build_slash(0xD8, 4, "FSUB", OP_FPU_RM32, &Interpreter::FSUB_RM32);
    build_slash(0xD8, 5, "FSUBR", OP_FPU_RM32, &Interpreter::FSUBR_RM32);
    build_slash(0xD8, 6, "FDIV", OP_FPU_RM32, &Interpreter::FDIV_RM32);
    build_slash(0xD8, 7, "FDIVR", OP_FPU_RM32, &Interpreter::FDIVR_RM32);

    build_slash(0xD9, 0, "FLD", OP_FPU_RM32, &Interpreter::FLD_RM32);
    build_slash(0xD9, 1, "FXCH", OP_FPU_reg, &Interpreter::FXCH);
    // FIXME: D9/1 C9 (...but isn't this what D9/1 does naturally, with C9 just being normal R/M?)
    build_slash(0xD9, 2, "FST", OP_FPU_RM32, &Interpreter::FST_RM32);
    build_slash_rm(0xD9, 2, 0xD0, "FNOP", OP_FPU, &Interpreter::FNOP);
    build_slash(0xD9, 3, "FSTP", OP_FPU_RM32, &Interpreter::FSTP_RM32);
    build_slash(0xD9, 4, "FLDENV", OP_FPU_RM32, &Interpreter::FLDENV);
    build_slash_rm(0xD9, 4, 0xE0, "FCHS", OP_FPU, &Interpreter::FCHS);
    build_slash_rm(0xD9, 4, 0xE1, "FABS", OP_FPU, &Interpreter::FABS);
    build_slash_rm(0xD9, 4, 0xE2, "FTST", OP_FPU, &Interpreter::FTST);
    build_slash_rm(0xD9, 4, 0xE3, "FXAM", OP_FPU, &Interpreter::FXAM);
    build_slash(0xD9, 5, "FLDCW", OP_FPU_RM16, &Interpreter::FLDCW);
    build_slash_rm(0xD9, 5, 0xE8, "FLD1", OP_FPU, &Interpreter::FLD1);
    build_slash_rm(0xD9, 5, 0xE9, "FLDL2T", OP_FPU, &Interpreter::FLDL2T);
    build_slash_rm(0xD9, 5, 0xEA, "FLDL2E", OP_FPU, &Interpreter::FLDL2E);
    build_slash_rm(0xD9, 5, 0xEB, "FLDPI", OP_FPU, &Interpreter::FLDPI);
    build_slash_rm(0xD9, 5, 0xEC, "FLDLG2", OP_FPU, &Interpreter::FLDLG2);
    build_slash_rm(0xD9, 5, 0xED, "FLDLN2", OP_FPU, &Interpreter::FLDLN2);
    build_slash_rm(0xD9, 5, 0xEE, "FLDZ", OP_FPU, &Interpreter::FLDZ);
    build_slash(0xD9, 6, "FNSTENV", OP_FPU_RM32, &Interpreter::FNSTENV);
    // FIXME: Extraodinary prefix 0x9B + 0xD9/6: FSTENV
    build_slash_rm(0xD9, 6, 0xF0, "F2XM1", OP_FPU, &Interpreter::F2XM1);
    build_slash_rm(0xD9, 6, 0xF1, "FYL2X", OP_FPU, &Interpreter::FYL2X);
    build_slash_rm(0xD9, 6, 0xF2, "FPTAN", OP_FPU, &Interpreter::FPTAN);
    build_slash_rm(0xD9, 6, 0xF3, "FPATAN", OP_FPU, &Interpreter::FPATAN);
    build_slash_rm(0xD9, 6, 0xF4, "FXTRACT", OP_FPU, &Interpreter::FXTRACT);
    build_slash_rm(0xD9, 6, 0xF5, "FPREM1", OP_FPU, &Interpreter::FPREM1);
    build_slash_rm(0xD9, 6, 0xF6, "FDECSTP", OP_FPU, &Interpreter::FDECSTP);
    build_slash_rm(0xD9, 6, 0xF7, "FINCSTP", OP_FPU, &Interpreter::FINCSTP);
    build_slash(0xD9, 7, "FNSTCW", OP_FPU_RM16, &Interpreter::FNSTCW);
    // FIXME: Extraodinary prefix 0x9B + 0xD9/7: FSTCW
    build_slash_rm(0xD9, 7, 0xF8, "FPREM", OP_FPU, &Interpreter::FPREM);
    build_slash_rm(0xD9, 7, 0xF9, "FYL2XP1", OP_FPU, &Interpreter::FYL2XP1);
    build_slash_rm(0xD9, 7, 0xFA, "FSQRT", OP_FPU, &Interpreter::FSQRT);
    build_slash_rm(0xD9, 7, 0xFB, "FSINCOS", OP_FPU, &Interpreter::FSINCOS);
    build_slash_rm(0xD9, 7, 0xFC, "FRNDINT", OP_FPU, &Interpreter::FRNDINT);
    build_slash_rm(0xD9, 7, 0xFD, "FSCALE", OP_FPU, &Interpreter::FSCALE);
    build_slash_rm(0xD9, 7, 0xFE, "FSIN", OP_FPU, &Interpreter::FSIN);
    build_slash_rm(0xD9, 7, 0xFF, "FCOS", OP_FPU, &Interpreter::FCOS);

    build_slash(0xDA, 0, "FIADD", OP_FPU_RM32, &Interpreter::FIADD_RM32);
    build_slash_reg(0xDA, 0, "FCMOVB", OP_FPU_reg, &Interpreter::FCMOVB);
    build_slash(0xDA, 1, "FIMUL", OP_FPU_RM32, &Interpreter::FIMUL_RM32);
    build_slash_reg(0xDA, 1, "FCMOVE", OP_FPU_reg, &Interpreter::FCMOVE);
    build_slash(0xDA, 2, "FICOM", OP_FPU_RM32, &Interpreter::FICOM_RM32);
    build_slash_reg(0xDA, 2, "FCMOVBE", OP_FPU_reg, &Interpreter::FCMOVBE);
    build_slash(0xDA, 3, "FICOMP", OP_FPU_RM32, &Interpreter::FICOMP_RM32);
    build_slash_reg(0xDA, 3, "FCMOVU", OP_FPU_reg, &Interpreter::FCMOVU);
    build_slash(0xDA, 4, "FISUB", OP_FPU_RM32, &Interpreter::FISUB_RM32);
    build_slash(0xDA, 5, "FISUBR", OP_FPU_RM32, &Interpreter::FISUBR_RM32);
    build_slash_rm(0xDA, 5, 0xE9, "FUCOMPP", OP_FPU, &Interpreter::FUCOMPP);
    build_slash(0xDA, 6, "FIDIV", OP_FPU_RM32, &Interpreter::FIDIV_RM32);
    build_slash(0xDA, 7, "FIDIVR", OP_FPU_RM32, &Interpreter::FIDIVR_RM32);

    build_slash(0xDB, 0, "FILD", OP_FPU_RM32, &Interpreter::FILD_RM32);
    build_slash_reg(0xDB, 0, "FCMOVNB", OP_FPU_reg, &Interpreter::FCMOVNB);
    build_slash(0xDB, 1, "FISTTP", OP_FPU_RM32, &Interpreter::FISTTP_RM32);
    build_slash_reg(0xDB, 1, "FCMOVNE", OP_FPU_reg, &Interpreter::FCMOVNE);
    build_slash(0xDB, 2, "FIST", OP_FPU_RM32, &Interpreter::FIST_RM32);
    build_slash_reg(0xDB, 2, "FCMOVNBE", OP_FPU_reg, &Interpreter::FCMOVNBE);
    build_slash(0xDB, 3, "FISTP", OP_FPU_RM32, &Interpreter::FISTP_RM32);
    build_slash_reg(0xDB, 3, "FCMOVNU", OP_FPU_reg, &Interpreter::FCMOVNU);
    build_slash(0xDB, 4, "FUNASSIGNED", OP_FPU, &Interpreter::ESCAPE);
    build_slash_rm(0xDB, 4, 0xE0, "FNENI", OP_FPU_reg, &Interpreter::FNENI);
    build_slash_rm(0xDB, 4, 0xE1, "FNDISI", OP_FPU_reg, &Interpreter::FNDISI);
    build_slash_rm(0xDB, 4, 0xE2, "FNCLEX", OP_FPU_reg, &Interpreter::FNCLEX);
    // FIXME: Extraodinary prefix 0x9B + 0xDB/4: FCLEX
    build_slash_rm(0xDB, 4, 0xE3, "FNINIT", OP_FPU_reg, &Interpreter::FNINIT);
    // FIXME: Extraodinary prefix 0x9B + 0xDB/4: FINIT
    build_slash_rm(0xDB, 4, 0xE4, "FNSETPM", OP_FPU_reg, &Interpreter::FNSETPM);
    build_slash(0xDB, 5, "FLD", OP_FPU_M80, &Interpreter::FLD_RM80);
    build_slash_reg(0xDB, 5, "FUCOMI", OP_FPU_reg, &Interpreter::FUCOMI);
    build_slash(0xDB, 6, "FCOMI", OP_FPU_reg, &Interpreter::FCOMI);
    build_slash(0xDB, 7, "FSTP", OP_FPU_M80, &Interpreter::FSTP_RM80);

    build_slash(0xDC, 0, "FADD", OP_FPU_RM64, &Interpreter::FADD_RM64);
    build_slash(0xDC, 1, "FMUL", OP_FPU_RM64, &Interpreter::FMUL_RM64);
    build_slash(0xDC, 2, "FCOM", OP_FPU_RM64, &Interpreter::FCOM_RM64);
    build_slash(0xDC, 3, "FCOMP", OP_FPU_RM64, &Interpreter::FCOMP_RM64);
    build_slash(0xDC, 4, "FSUB", OP_FPU_RM64, &Interpreter::FSUB_RM64);
    build_slash(0xDC, 5, "FSUBR", OP_FPU_RM64, &Interpreter::FSUBR_RM64);
    build_slash(0xDC, 6, "FDIV", OP_FPU_RM64, &Interpreter::FDIV_RM64);
    build_slash(0xDC, 7, "FDIVR", OP_FPU_RM64, &Interpreter::FDIVR_RM64);

    build_slash(0xDD, 0, "FLD", OP_FPU_RM64, &Interpreter::FLD_RM64);
    build_slash_reg(0xDD, 0, "FFREE", OP_FPU_reg, &Interpreter::FFREE);
    build_slash(0xDD, 1, "FISTTP", OP_FPU_RM64, &Interpreter::FISTTP_RM64);
    build_slash_reg(0xDD, 1, "FXCH4", OP_FPU_reg, &Interpreter::FXCH);
    build_slash(0xDD, 2, "FST", OP_FPU_RM64, &Interpreter::FST_RM64);
    build_slash(0xDD, 3, "FSTP", OP_FPU_RM64, &Interpreter::FSTP_RM64);
    build_slash(0xDD, 4, "FRSTOR", OP_FPU_mem, &Interpreter::FRSTOR);
    build_slash_reg(0xDD, 4, "FUCOM", OP_FPU_reg, &Interpreter::FUCOM);
    // FIXME: DD/4 E1 (...but isn't this what DD/4 does naturally, with E1 just being normal R/M?)
    build_slash(0xDD, 5, "FUCOMP", OP_FPU_reg, &Interpreter::FUCOMP);
    // FIXME: DD/5 E9 (...but isn't this what DD/5 does naturally, with E9 just being normal R/M?)
    build_slash(0xDD, 6, "FNSAVE", OP_FPU_mem, &Interpreter::FNSAVE);
    // FIXME: Extraodinary prefix 0x9B + 0xDD/6: FSAVE
    build_slash(0xDD, 7, "FNSTSW", OP_FPU_RM16, &Interpreter::FNSTSW);
    // FIXME: Extraodinary prefix 0x9B + 0xDD/7: FSTSW

    build_slash(0xDE, 0, "FIADD", OP_FPU_RM16, &Interpreter::FIADD_RM16);
    build_slash_reg(0xDE, 0, "FADDP", OP_FPU_reg, &Interpreter::FADDP);
    // FIXME: DE/0 C1 (...but isn't this what DE/0 does naturally, with C1 just being normal R/M?)
    build_slash(0xDE, 1, "FIMUL", OP_FPU_RM16, &Interpreter::FIMUL_RM16);
    build_slash_reg(0xDE, 1, "FMULP", OP_FPU_reg, &Interpreter::FMULP);
    // FIXME: DE/1 C9 (...but isn't this what DE/1 does naturally, with C9 just being normal R/M?)
    build_slash(0xDE, 2, "FICOM", OP_FPU_RM16, &Interpreter::FICOM_RM16);
    build_slash_reg(0xDE, 2, "FCOMP5", OP_FPU_reg, &Interpreter::FCOMP_RM32);
    build_slash(0xDE, 3, "FICOMP", OP_FPU_RM16, &Interpreter::FICOMP_RM16);
    build_slash_reg(0xDE, 3, "FCOMPP", OP_FPU_reg, &Interpreter::FCOMPP);
    build_slash(0xDE, 4, "FISUB", OP_FPU_RM16, &Interpreter::FISUB_RM16);
    build_slash_reg(0xDE, 4, "FSUBRP", OP_FPU_reg, &Interpreter::FSUBRP);
    // FIXME: DE/4 E1 (...but isn't this what DE/4 does naturally, with E1 just being normal R/M?)
    build_slash(0xDE, 5, "FISUBR", OP_FPU_RM16, &Interpreter::FISUBR_RM16);
    build_slash_reg(0xDE, 5, "FSUBP", OP_FPU_reg, &Interpreter::FSUBP);
    // FIXME: DE/5 E9 (...but isn't this what DE/5 does naturally, with E9 just being normal R/M?)
    build_slash(0xDE, 6, "FIDIV", OP_FPU_RM16, &Interpreter::FIDIV_RM16);
    build_slash_reg(0xDE, 6, "FDIVRP", OP_FPU_reg, &Interpreter::FDIVRP);
    // FIXME: DE/6 F1 (...but isn't this what DE/6 does naturally, with F1 just being normal R/M?)
    build_slash(0xDE, 7, "FIDIVR", OP_FPU_RM16, &Interpreter::FIDIVR_RM16);
    build_slash_reg(0xDE, 7, "FDIVP", OP_FPU_reg, &Interpreter::FDIVP);
    // FIXME: DE/7 F9 (...but isn't this what DE/7 does naturally, with F9 just being normal R/M?)

    build_slash(0xDF, 0, "FILD", OP_FPU_RM32, &Interpreter::FILD_RM16);
    build_slash_reg(0xDF, 0, "FFREEP", OP_FPU_reg, &Interpreter::FFREEP);
    build_slash(0xDF, 1, "FISTTP", OP_FPU_RM32, &Interpreter::FISTTP_RM16);
    build_slash_reg(0xDF, 1, "FXCH7", OP_FPU_reg, &Interpreter::FXCH);
    build_slash(0xDF, 2, "FIST", OP_FPU_RM32, &Interpreter::FIST_RM16);
    build_slash_reg(0xDF, 2, "FSTP8", OP_FPU_reg, &Interpreter::FSTP_RM32);
    build_slash(0xDF, 3, "FISTP", OP_FPU_RM32, &Interpreter::FISTP_RM16);
    build_slash_reg(0xDF, 3, "FSTP9", OP_FPU_reg, &Interpreter::FSTP_RM32);
    build_slash(0xDF, 4, "FBLD", OP_FPU_M80, &Interpreter::FBLD_M80);
    build_slash_reg(0xDF, 4, "FNSTSW", OP_FPU_AX16, &Interpreter::FNSTSW_AX);
    // FIXME: Extraodinary prefix 0x9B + 0xDF/e: FSTSW_AX
    build_slash(0xDF, 5, "FILD", OP_FPU_RM64, &Interpreter::FILD_RM64);
    build_slash_reg(0xDF, 5, "FUCOMIP", OP_FPU_reg, &Interpreter::FUCOMIP);
    build_slash(0xDF, 6, "FBSTP", OP_FPU_M80, &Interpreter::FBSTP_M80);
    build_slash_reg(0xDF, 6, "FCOMIP", OP_FPU_reg, &Interpreter::FCOMIP);
    build_slash(0xDF, 7, "FISTP", OP_FPU_RM64, &Interpreter::FISTP_RM64);

    build(0xE0, "LOOPNZ", OP_imm8, &Interpreter::LOOPNZ_imm8);
    build(0xE1, "LOOPZ", OP_imm8, &Interpreter::LOOPZ_imm8);
    build(0xE2, "LOOP", OP_imm8, &Interpreter::LOOP_imm8);
    build(0xE3, "JCXZ", OP_imm8, &Interpreter::JCXZ_imm8);
    build(0xE4, "IN", OP_AL_imm8, &Interpreter::IN_AL_imm8);
    build(0xE5, "IN", OP_AX_imm8, &Interpreter::IN_AX_imm8, OP_EAX_imm8, &Interpreter::IN_EAX_imm8);
    build(0xE6, "OUT", OP_imm8_AL, &Interpreter::OUT_imm8_AL);
    build(0xE7, "OUT", OP_imm8_AX, &Interpreter::OUT_imm8_AX, OP_imm8_EAX, &Interpreter::OUT_imm8_EAX);
    build(0xE8, "CALL", OP_relimm16, &Interpreter::CALL_imm16, OP_relimm32, &Interpreter::CALL_imm32);
    build(0xE9, "JMP", OP_relimm16, &Interpreter::JMP_imm16, OP_relimm32, &Interpreter::JMP_imm32);
    build(0xEA, "JMP", OP_imm16_imm16, &Interpreter::JMP_imm16_imm16, OP_imm16_imm32, &Interpreter::JMP_imm16_imm32);
    build(0xEB, "JMP", OP_short_imm8, &Interpreter::JMP_short_imm8);
    build(0xEC, "IN", OP_AL_DX, &Interpreter::IN_AL_DX);
    build(0xED, "IN", OP_AX_DX, &Interpreter::IN_AX_DX, OP_EAX_DX, &Interpreter::IN_EAX_DX);
    build(0xEE, "OUT", OP_DX_AL, &Interpreter::OUT_DX_AL);
    build(0xEF, "OUT", OP_DX_AX, &Interpreter::OUT_DX_AX, OP_DX_EAX, &Interpreter::OUT_DX_EAX);

    build(0xF4, "HLT", OP, &Interpreter::HLT);
    build(0xF5, "CMC", OP, &Interpreter::CMC);

    build(0xF8, "CLC", OP, &Interpreter::CLC);
    build(0xF9, "STC", OP, &Interpreter::STC);
    build(0xFA, "CLI", OP, &Interpreter::CLI);
    build(0xFB, "STI", OP, &Interpreter::STI);
    build(0xFC, "CLD", OP, &Interpreter::CLD);
    build(0xFD, "STD", OP, &Interpreter::STD);

    build_slash(0x80, 0, "ADD", OP_RM8_imm8, &Interpreter::ADD_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 1, "OR", OP_RM8_imm8, &Interpreter::OR_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 2, "ADC", OP_RM8_imm8, &Interpreter::ADC_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 3, "SBB", OP_RM8_imm8, &Interpreter::SBB_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 4, "AND", OP_RM8_imm8, &Interpreter::AND_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 5, "SUB", OP_RM8_imm8, &Interpreter::SUB_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 6, "XOR", OP_RM8_imm8, &Interpreter::XOR_RM8_imm8, LockPrefixAllowed);
    build_slash(0x80, 7, "CMP", OP_RM8_imm8, &Interpreter::CMP_RM8_imm8);

    build_slash(0x81, 0, "ADD", OP_RM16_imm16, &Interpreter::ADD_RM16_imm16, OP_RM32_imm32, &Interpreter::ADD_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 1, "OR", OP_RM16_imm16, &Interpreter::OR_RM16_imm16, OP_RM32_imm32, &Interpreter::OR_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 2, "ADC", OP_RM16_imm16, &Interpreter::ADC_RM16_imm16, OP_RM32_imm32, &Interpreter::ADC_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 3, "SBB", OP_RM16_imm16, &Interpreter::SBB_RM16_imm16, OP_RM32_imm32, &Interpreter::SBB_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 4, "AND", OP_RM16_imm16, &Interpreter::AND_RM16_imm16, OP_RM32_imm32, &Interpreter::AND_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 5, "SUB", OP_RM16_imm16, &Interpreter::SUB_RM16_imm16, OP_RM32_imm32, &Interpreter::SUB_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 6, "XOR", OP_RM16_imm16, &Interpreter::XOR_RM16_imm16, OP_RM32_imm32, &Interpreter::XOR_RM32_imm32, LockPrefixAllowed);
    build_slash(0x81, 7, "CMP", OP_RM16_imm16, &Interpreter::CMP_RM16_imm16, OP_RM32_imm32, &Interpreter::CMP_RM32_imm32);

    build_slash(0x83, 0, "ADD", OP_RM16_imm8, &Interpreter::ADD_RM16_imm8, OP_RM32_imm8, &Interpreter::ADD_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 1, "OR", OP_RM16_imm8, &Interpreter::OR_RM16_imm8, OP_RM32_imm8, &Interpreter::OR_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 2, "ADC", OP_RM16_imm8, &Interpreter::ADC_RM16_imm8, OP_RM32_imm8, &Interpreter::ADC_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 3, "SBB", OP_RM16_imm8, &Interpreter::SBB_RM16_imm8, OP_RM32_imm8, &Interpreter::SBB_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 4, "AND", OP_RM16_imm8, &Interpreter::AND_RM16_imm8, OP_RM32_imm8, &Interpreter::AND_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 5, "SUB", OP_RM16_imm8, &Interpreter::SUB_RM16_imm8, OP_RM32_imm8, &Interpreter::SUB_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 6, "XOR", OP_RM16_imm8, &Interpreter::XOR_RM16_imm8, OP_RM32_imm8, &Interpreter::XOR_RM32_imm8, LockPrefixAllowed);
    build_slash(0x83, 7, "CMP", OP_RM16_imm8, &Interpreter::CMP_RM16_imm8, OP_RM32_imm8, &Interpreter::CMP_RM32_imm8);

    build_slash(0x8F, 0, "POP", OP_RM16, &Interpreter::POP_RM16, OP_RM32, &Interpreter::POP_RM32);

    build_slash(0xC0, 0, "ROL", OP_RM8_imm8, &Interpreter::ROL_RM8_imm8);
    build_slash(0xC0, 1, "ROR", OP_RM8_imm8, &Interpreter::ROR_RM8_imm8);
    build_slash(0xC0, 2, "RCL", OP_RM8_imm8, &Interpreter::RCL_RM8_imm8);
    build_slash(0xC0, 3, "RCR", OP_RM8_imm8, &Interpreter::RCR_RM8_imm8);
    build_slash(0xC0, 4, "SHL", OP_RM8_imm8, &Interpreter::SHL_RM8_imm8);
    build_slash(0xC0, 5, "SHR", OP_RM8_imm8, &Interpreter::SHR_RM8_imm8);
    build_slash(0xC0, 6, "SHL", OP_RM8_imm8, &Interpreter::SHL_RM8_imm8); // Undocumented
    build_slash(0xC0, 7, "SAR", OP_RM8_imm8, &Interpreter::SAR_RM8_imm8);

    build_slash(0xC1, 0, "ROL", OP_RM16_imm8, &Interpreter::ROL_RM16_imm8, OP_RM32_imm8, &Interpreter::ROL_RM32_imm8);
    build_slash(0xC1, 1, "ROR", OP_RM16_imm8, &Interpreter::ROR_RM16_imm8, OP_RM32_imm8, &Interpreter::ROR_RM32_imm8);
    build_slash(0xC1, 2, "RCL", OP_RM16_imm8, &Interpreter::RCL_RM16_imm8, OP_RM32_imm8, &Interpreter::RCL_RM32_imm8);
    build_slash(0xC1, 3, "RCR", OP_RM16_imm8, &Interpreter::RCR_RM16_imm8, OP_RM32_imm8, &Interpreter::RCR_RM32_imm8);
    build_slash(0xC1, 4, "SHL", OP_RM16_imm8, &Interpreter::SHL_RM16_imm8, OP_RM32_imm8, &Interpreter::SHL_RM32_imm8);
    build_slash(0xC1, 5, "SHR", OP_RM16_imm8, &Interpreter::SHR_RM16_imm8, OP_RM32_imm8, &Interpreter::SHR_RM32_imm8);
    build_slash(0xC1, 6, "SHL", OP_RM16_imm8, &Interpreter::SHL_RM16_imm8, OP_RM32_imm8, &Interpreter::SHL_RM32_imm8); // Undocumented
    build_slash(0xC1, 7, "SAR", OP_RM16_imm8, &Interpreter::SAR_RM16_imm8, OP_RM32_imm8, &Interpreter::SAR_RM32_imm8);

    build_slash(0xD0, 0, "ROL", OP_RM8_1, &Interpreter::ROL_RM8_1);
    build_slash(0xD0, 1, "ROR", OP_RM8_1, &Interpreter::ROR_RM8_1);
    build_slash(0xD0, 2, "RCL", OP_RM8_1, &Interpreter::RCL_RM8_1);
    build_slash(0xD0, 3, "RCR", OP_RM8_1, &Interpreter::RCR_RM8_1);
    build_slash(0xD0, 4, "SHL", OP_RM8_1, &Interpreter::SHL_RM8_1);
    build_slash(0xD0, 5, "SHR", OP_RM8_1, &Interpreter::SHR_RM8_1);
    build_slash(0xD0, 6, "SHL", OP_RM8_1, &Interpreter::SHL_RM8_1); // Undocumented
    build_slash(0xD0, 7, "SAR", OP_RM8_1, &Interpreter::SAR_RM8_1);

    build_slash(0xD1, 0, "ROL", OP_RM16_1, &Interpreter::ROL_RM16_1, OP_RM32_1, &Interpreter::ROL_RM32_1);
    build_slash(0xD1, 1, "ROR", OP_RM16_1, &Interpreter::ROR_RM16_1, OP_RM32_1, &Interpreter::ROR_RM32_1);
    build_slash(0xD1, 2, "RCL", OP_RM16_1, &Interpreter::RCL_RM16_1, OP_RM32_1, &Interpreter::RCL_RM32_1);
    build_slash(0xD1, 3, "RCR", OP_RM16_1, &Interpreter::RCR_RM16_1, OP_RM32_1, &Interpreter::RCR_RM32_1);
    build_slash(0xD1, 4, "SHL", OP_RM16_1, &Interpreter::SHL_RM16_1, OP_RM32_1, &Interpreter::SHL_RM32_1);
    build_slash(0xD1, 5, "SHR", OP_RM16_1, &Interpreter::SHR_RM16_1, OP_RM32_1, &Interpreter::SHR_RM32_1);
    build_slash(0xD1, 6, "SHL", OP_RM16_1, &Interpreter::SHL_RM16_1, OP_RM32_1, &Interpreter::SHL_RM32_1); // Undocumented
    build_slash(0xD1, 7, "SAR", OP_RM16_1, &Interpreter::SAR_RM16_1, OP_RM32_1, &Interpreter::SAR_RM32_1);

    build_slash(0xD2, 0, "ROL", OP_RM8_CL, &Interpreter::ROL_RM8_CL);
    build_slash(0xD2, 1, "ROR", OP_RM8_CL, &Interpreter::ROR_RM8_CL);
    build_slash(0xD2, 2, "RCL", OP_RM8_CL, &Interpreter::RCL_RM8_CL);
    build_slash(0xD2, 3, "RCR", OP_RM8_CL, &Interpreter::RCR_RM8_CL);
    build_slash(0xD2, 4, "SHL", OP_RM8_CL, &Interpreter::SHL_RM8_CL);
    build_slash(0xD2, 5, "SHR", OP_RM8_CL, &Interpreter::SHR_RM8_CL);
    build_slash(0xD2, 6, "SHL", OP_RM8_CL, &Interpreter::SHL_RM8_CL); // Undocumented
    build_slash(0xD2, 7, "SAR", OP_RM8_CL, &Interpreter::SAR_RM8_CL);

    build_slash(0xD3, 0, "ROL", OP_RM16_CL, &Interpreter::ROL_RM16_CL, OP_RM32_CL, &Interpreter::ROL_RM32_CL);
    build_slash(0xD3, 1, "ROR", OP_RM16_CL, &Interpreter::ROR_RM16_CL, OP_RM32_CL, &Interpreter::ROR_RM32_CL);
    build_slash(0xD3, 2, "RCL", OP_RM16_CL, &Interpreter::RCL_RM16_CL, OP_RM32_CL, &Interpreter::RCL_RM32_CL);
    build_slash(0xD3, 3, "RCR", OP_RM16_CL, &Interpreter::RCR_RM16_CL, OP_RM32_CL, &Interpreter::RCR_RM32_CL);
    build_slash(0xD3, 4, "SHL", OP_RM16_CL, &Interpreter::SHL_RM16_CL, OP_RM32_CL, &Interpreter::SHL_RM32_CL);
    build_slash(0xD3, 5, "SHR", OP_RM16_CL, &Interpreter::SHR_RM16_CL, OP_RM32_CL, &Interpreter::SHR_RM32_CL);
    build_slash(0xD3, 6, "SHL", OP_RM16_CL, &Interpreter::SHL_RM16_CL, OP_RM32_CL, &Interpreter::SHL_RM32_CL); // Undocumented
    build_slash(0xD3, 7, "SAR", OP_RM16_CL, &Interpreter::SAR_RM16_CL, OP_RM32_CL, &Interpreter::SAR_RM32_CL);

    build_slash(0xF6, 0, "TEST", OP_RM8_imm8, &Interpreter::TEST_RM8_imm8);
    build_slash(0xF6, 1, "TEST", OP_RM8_imm8, &Interpreter::TEST_RM8_imm8); // Undocumented
    build_slash(0xF6, 2, "NOT", OP_RM8, &Interpreter::NOT_RM8, LockPrefixAllowed);
    build_slash(0xF6, 3, "NEG", OP_RM8, &Interpreter::NEG_RM8, LockPrefixAllowed);
    build_slash(0xF6, 4, "MUL", OP_RM8, &Interpreter::MUL_RM8);
    build_slash(0xF6, 5, "IMUL", OP_RM8, &Interpreter::IMUL_RM8);
    build_slash(0xF6, 6, "DIV", OP_RM8, &Interpreter::DIV_RM8);
    build_slash(0xF6, 7, "IDIV", OP_RM8, &Interpreter::IDIV_RM8);

    build_slash(0xF7, 0, "TEST", OP_RM16_imm16, &Interpreter::TEST_RM16_imm16, OP_RM32_imm32, &Interpreter::TEST_RM32_imm32);
    build_slash(0xF7, 1, "TEST", OP_RM16_imm16, &Interpreter::TEST_RM16_imm16, OP_RM32_imm32, &Interpreter::TEST_RM32_imm32); // Undocumented
    build_slash(0xF7, 2, "NOT", OP_RM16, &Interpreter::NOT_RM16, OP_RM32, &Interpreter::NOT_RM32, LockPrefixAllowed);
    build_slash(0xF7, 3, "NEG", OP_RM16, &Interpreter::NEG_RM16, OP_RM32, &Interpreter::NEG_RM32, LockPrefixAllowed);
    build_slash(0xF7, 4, "MUL", OP_RM16, &Interpreter::MUL_RM16, OP_RM32, &Interpreter::MUL_RM32);
    build_slash(0xF7, 5, "IMUL", OP_RM16, &Interpreter::IMUL_RM16, OP_RM32, &Interpreter::IMUL_RM32);
    build_slash(0xF7, 6, "DIV", OP_RM16, &Interpreter::DIV_RM16, OP_RM32, &Interpreter::DIV_RM32);
    build_slash(0xF7, 7, "IDIV", OP_RM16, &Interpreter::IDIV_RM16, OP_RM32, &Interpreter::IDIV_RM32);

    build_slash(0xFE, 0, "INC", OP_RM8, &Interpreter::INC_RM8, LockPrefixAllowed);
    build_slash(0xFE, 1, "DEC", OP_RM8, &Interpreter::DEC_RM8, LockPrefixAllowed);

    build_slash(0xFF, 0, "INC", OP_RM16, &Interpreter::INC_RM16, OP_RM32, &Interpreter::INC_RM32, LockPrefixAllowed);
    build_slash(0xFF, 1, "DEC", OP_RM16, &Interpreter::DEC_RM16, OP_RM32, &Interpreter::DEC_RM32, LockPrefixAllowed);
    build_slash(0xFF, 2, "CALL", OP_RM16, &Interpreter::CALL_RM16, OP_RM32, &Interpreter::CALL_RM32);
    build_slash(0xFF, 3, "CALL", OP_FAR_mem16, &Interpreter::CALL_FAR_mem16, OP_FAR_mem32, &Interpreter::CALL_FAR_mem32);
    build_slash(0xFF, 4, "JMP", OP_RM16, &Interpreter::JMP_RM16, OP_RM32, &Interpreter::JMP_RM32);
    build_slash(0xFF, 5, "JMP", OP_FAR_mem16, &Interpreter::JMP_FAR_mem16, OP_FAR_mem32, &Interpreter::JMP_FAR_mem32);
    build_slash(0xFF, 6, "PUSH", OP_RM16, &Interpreter::PUSH_RM16, OP_RM32, &Interpreter::PUSH_RM32);

    // Instructions starting with 0x0F are multi-byte opcodes.
    build_0f_slash(0x00, 0, "SLDT", OP_RM16, &Interpreter::SLDT_RM16);
    build_0f_slash(0x00, 1, "STR", OP_RM16, &Interpreter::STR_RM16);
    build_0f_slash(0x00, 2, "LLDT", OP_RM16, &Interpreter::LLDT_RM16);
    build_0f_slash(0x00, 3, "LTR", OP_RM16, &Interpreter::LTR_RM16);
    build_0f_slash(0x00, 4, "VERR", OP_RM16, &Interpreter::VERR_RM16);
    build_0f_slash(0x00, 5, "VERW", OP_RM16, &Interpreter::VERW_RM16);

    build_0f_slash(0x01, 0, "SGDT", OP_RM16, &Interpreter::SGDT);
    build_0f_slash(0x01, 1, "SIDT", OP_RM16, &Interpreter::SIDT);
    build_0f_slash(0x01, 2, "LGDT", OP_RM16, &Interpreter::LGDT);
    build_0f_slash(0x01, 3, "LIDT", OP_RM16, &Interpreter::LIDT);
    build_0f_slash(0x01, 4, "SMSW", OP_RM16, &Interpreter::SMSW_RM16);
    build_0f_slash(0x01, 6, "LMSW", OP_RM16, &Interpreter::LMSW_RM16);
    build_0f_slash(0x01, 7, "INVLPG", OP_RM32, &Interpreter::INVLPG);

    build_0f_slash(0xBA, 4, "BT", OP_RM16_imm8, &Interpreter::BT_RM16_imm8, OP_RM32_imm8, &Interpreter::BT_RM32_imm8, LockPrefixAllowed);
    build_0f_slash(0xBA, 5, "BTS", OP_RM16_imm8, &Interpreter::BTS_RM16_imm8, OP_RM32_imm8, &Interpreter::BTS_RM32_imm8, LockPrefixAllowed);
    build_0f_slash(0xBA, 6, "BTR", OP_RM16_imm8, &Interpreter::BTR_RM16_imm8, OP_RM32_imm8, &Interpreter::BTR_RM32_imm8, LockPrefixAllowed);
    build_0f_slash(0xBA, 7, "BTC", OP_RM16_imm8, &Interpreter::BTC_RM16_imm8, OP_RM32_imm8, &Interpreter::BTC_RM32_imm8, LockPrefixAllowed);

    build_0f(0x02, "LAR", OP_reg16_RM16, &Interpreter::LAR_reg16_RM16, OP_reg32_RM32, &Interpreter::LAR_reg32_RM32);
    build_0f(0x03, "LSL", OP_reg16_RM16, &Interpreter::LSL_reg16_RM16, OP_reg32_RM32, &Interpreter::LSL_reg32_RM32);
    build_0f(0x06, "CLTS", OP, &Interpreter::CLTS);
    build_0f(0x09, "WBINVD", OP, &Interpreter::WBINVD);
    build_0f(0x0B, "UD2", OP, &Interpreter::UD2);

    build_0f(0x20, "MOV", OP_reg32_CR, &Interpreter::MOV_reg32_CR);
    build_0f(0x21, "MOV", OP_reg32_DR, &Interpreter::MOV_reg32_DR);
    build_0f(0x22, "MOV", OP_CR_reg32, &Interpreter::MOV_CR_reg32);
    build_0f(0x23, "MOV", OP_DR_reg32, &Interpreter::MOV_DR_reg32);

    build_0f(0x31, "RDTSC", OP, &Interpreter::RDTSC);

    build_0f(0x40, "CMOVO", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x41, "CMOVNO", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x42, "CMOVC", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x43, "CMOVNC", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x44, "CMOVZ", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x45, "CMOVNZ", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x46, "CMOVNA", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x47, "CMOVA", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x48, "CMOVS", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x49, "CMOVNS", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x4A, "CMOVP", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x4B, "CMOVNP", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x4C, "CMOVL", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x4D, "CMOVNL", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x4E, "CMOVNG", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);
    build_0f(0x4F, "CMOVG", OP_reg16_RM16, &Interpreter::CMOVcc_reg16_RM16, OP_reg32_RM32, &Interpreter::CMOVcc_reg32_RM32);

    build_0f(0x6F, "MOVQ", OP_mm1_mm2m64, &Interpreter::MOVQ_mm1_mm2m64);
    build_0f(0x77, "EMMS", OP, &Interpreter::EMMS);
    build_0f(0x7F, "MOVQ", OP_mm1m64_mm2, &Interpreter::MOVQ_mm1_m64_mm2);

    build_0f(0x80, "JO", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x81, "JNO", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x82, "JC", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x83, "JNC", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x84, "JZ", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x85, "JNZ", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x86, "JNA", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x87, "JA", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x88, "JS", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x89, "JNS", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x8A, "JP", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x8B, "JNP", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x8C, "JL", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x8D, "JNL", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x8E, "JNG", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);
    build_0f(0x8F, "JG", OP_NEAR_imm, &Interpreter::Jcc_NEAR_imm);

    build_0f(0x90, "SETO", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x91, "SETNO", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x92, "SETC", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x93, "SETNC", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x94, "SETZ", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x95, "SETNZ", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x96, "SETNA", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x97, "SETA", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x98, "SETS", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x99, "SETNS", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x9A, "SETP", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x9B, "SETNP", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x9C, "SETL", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x9D, "SETNL", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x9E, "SETNG", OP_RM8, &Interpreter::SETcc_RM8);
    build_0f(0x9F, "SETG", OP_RM8, &Interpreter::SETcc_RM8);

    build_0f(0xA0, "PUSH", OP_FS, &Interpreter::PUSH_FS);
    build_0f(0xA1, "POP", OP_FS, &Interpreter::POP_FS);
    build_0f(0xA2, "CPUID", OP, &Interpreter::CPUID);
    build_0f(0xA3, "BT", OP_RM16_reg16, &Interpreter::BT_RM16_reg16, OP_RM32_reg32, &Interpreter::BT_RM32_reg32);
    build_0f(0xA4, "SHLD", OP_RM16_reg16_imm8, &Interpreter::SHLD_RM16_reg16_imm8, OP_RM32_reg32_imm8, &Interpreter::SHLD_RM32_reg32_imm8);
    build_0f(0xA5, "SHLD", OP_RM16_reg16_CL, &Interpreter::SHLD_RM16_reg16_CL, OP_RM32_reg32_CL, &Interpreter::SHLD_RM32_reg32_CL);
    build_0f(0xA8, "PUSH", OP_GS, &Interpreter::PUSH_GS);
    build_0f(0xA9, "POP", OP_GS, &Interpreter::POP_GS);
    build_0f(0xAB, "BTS", OP_RM16_reg16, &Interpreter::BTS_RM16_reg16, OP_RM32_reg32, &Interpreter::BTS_RM32_reg32);
    build_0f(0xAC, "SHRD", OP_RM16_reg16_imm8, &Interpreter::SHRD_RM16_reg16_imm8, OP_RM32_reg32_imm8, &Interpreter::SHRD_RM32_reg32_imm8);
    build_0f(0xAD, "SHRD", OP_RM16_reg16_CL, &Interpreter::SHRD_RM16_reg16_CL, OP_RM32_reg32_CL, &Interpreter::SHRD_RM32_reg32_CL);
    build_0f(0xAF, "IMUL", OP_reg16_RM16, &Interpreter::IMUL_reg16_RM16, OP_reg32_RM32, &Interpreter::IMUL_reg32_RM32);
    build_0f(0xB0, "CMPXCHG", OP_RM8_reg8, &Interpreter::CMPXCHG_RM8_reg8, LockPrefixAllowed);
    build_0f(0xB1, "CMPXCHG", OP_RM16_reg16, &Interpreter::CMPXCHG_RM16_reg16, OP_RM32_reg32, &Interpreter::CMPXCHG_RM32_reg32, LockPrefixAllowed);
    build_0f(0xB2, "LSS", OP_reg16_mem16, &Interpreter::LSS_reg16_mem16, OP_reg32_mem32, &Interpreter::LSS_reg32_mem32);
    build_0f(0xB3, "BTR", OP_RM16_reg16, &Interpreter::BTR_RM16_reg16, OP_RM32_reg32, &Interpreter::BTR_RM32_reg32);
    build_0f(0xB4, "LFS", OP_reg16_mem16, &Interpreter::LFS_reg16_mem16, OP_reg32_mem32, &Interpreter::LFS_reg32_mem32);
    build_0f(0xB5, "LGS", OP_reg16_mem16, &Interpreter::LGS_reg16_mem16, OP_reg32_mem32, &Interpreter::LGS_reg32_mem32);
    build_0f(0xB6, "MOVZX", OP_reg16_RM8, &Interpreter::MOVZX_reg16_RM8, OP_reg32_RM8, &Interpreter::MOVZX_reg32_RM8);
    build_0f(0xB7, "0xB7", OP, nullptr, "MOVZX", OP_reg32_RM16, &Interpreter::MOVZX_reg32_RM16);
    build_0f(0xB9, "UD1", OP, &Interpreter::UD1);
    build_0f(0xBB, "BTC", OP_RM16_reg16, &Interpreter::BTC_RM16_reg16, OP_RM32_reg32, &Interpreter::BTC_RM32_reg32);
    build_0f(0xBC, "BSF", OP_reg16_RM16, &Interpreter::BSF_reg16_RM16, OP_reg32_RM32, &Interpreter::BSF_reg32_RM32);
    build_0f(0xBD, "BSR", OP_reg16_RM16, &Interpreter::BSR_reg16_RM16, OP_reg32_RM32, &Interpreter::BSR_reg32_RM32);
    build_0f(0xBE, "MOVSX", OP_reg16_RM8, &Interpreter::MOVSX_reg16_RM8, OP_reg32_RM8, &Interpreter::MOVSX_reg32_RM8);
    build_0f(0xBF, "0xBF", OP, nullptr, "MOVSX", OP_reg32_RM16, &Interpreter::MOVSX_reg32_RM16);
    build_0f(0xC0, "XADD", OP_RM8_reg8, &Interpreter::XADD_RM8_reg8, LockPrefixAllowed);
    build_0f(0xC1, "XADD", OP_RM16_reg16, &Interpreter::XADD_RM16_reg16, OP_RM32_reg32, &Interpreter::XADD_RM32_reg32, LockPrefixAllowed);

    for (u8 i = 0xc8; i <= 0xcf; ++i)
        build_0f(i, "BSWAP", OP_reg32, &Interpreter::BSWAP_reg32);

    build_0f(0xFC, "PADDB", OP_mm1_mm2m64, &Interpreter::PADDB_mm1_mm2m64);
    build_0f(0xFD, "PADDW", OP_mm1_mm2m64, &Interpreter::PADDW_mm1_mm2m64);
    build_0f(0xFE, "PADDD", OP_mm1_mm2m64, &Interpreter::PADDD_mm1_mm2m64);
    build_0f(0xFF, "UD0", OP, &Interpreter::UD0);
}

static const char* register_name(RegisterIndex8);
static const char* register_name(RegisterIndex16);
static const char* register_name(RegisterIndex32);
static const char* register_name(FpuRegisterIndex);
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

String MemoryOrRegisterReference::to_string_o8(const Instruction& insn) const
{
    if (is_register())
        return register_name(reg8());
    return String::format("[%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_o16(const Instruction& insn) const
{
    if (is_register())
        return register_name(reg16());
    return String::format("[%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_o32(const Instruction& insn) const
{
    if (is_register())
        return register_name(reg32());
    return String::format("[%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_fpu_reg() const
{
    ASSERT(is_register());
    return register_name(reg_fpu());
}

String MemoryOrRegisterReference::to_string_fpu_mem(const Instruction& insn) const
{
    ASSERT(!is_register());
    return String::format("[%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_fpu_ax16() const
{
    ASSERT(is_register());
    return register_name(reg16());
}

String MemoryOrRegisterReference::to_string_fpu16(const Instruction& insn) const
{
    if (is_register())
        return register_name(reg_fpu());
    return String::format("word ptr [%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_fpu32(const Instruction& insn) const
{
    if (is_register())
        return register_name(reg_fpu());
    return String::format("dword ptr [%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_fpu64(const Instruction& insn) const
{
    if (is_register())
        return register_name(reg_fpu());
    return String::format("qword ptr [%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string_fpu80(const Instruction& insn) const
{
    ASSERT(!is_register());
    return String::format("tbyte ptr [%s]", to_string(insn).characters());
}
String MemoryOrRegisterReference::to_string_mm(const Instruction& insn) const
{
    if (is_register())
        return register_name(static_cast<MMXRegisterIndex>(m_register_index));
    return String::format("[%s]", to_string(insn).characters());
}

String MemoryOrRegisterReference::to_string(const Instruction& insn) const
{
    if (insn.a32())
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
    StringBuilder builder;
    if (m_segment_prefix.has_value())
        builder.appendf("%s: ", register_name(m_segment_prefix.value()));
    if (has_address_size_override_prefix())
        builder.append(m_a32 ? "a32 " : "a16 ");
    if (has_operand_size_override_prefix())
        builder.append(m_o32 ? "o32 " : "o16 ");
    if (has_lock_prefix())
        builder.append("lock ");
    if (has_rep_prefix())
        builder.append(m_rep_prefix == Prefix::REPNZ ? "repnz " : "repz ");
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

    auto append_rm8 = [&] { builder.append(m_modrm.to_string_o8(*this)); };
    auto append_rm16 = [&] { builder.append(m_modrm.to_string_o16(*this)); };
    auto append_rm32 = [&] { builder.append(m_modrm.to_string_o32(*this)); };
    auto append_fpu_reg = [&] { builder.append(m_modrm.to_string_fpu_reg()); };
    auto append_fpu_mem = [&] { builder.append(m_modrm.to_string_fpu_mem(*this)); };
    auto append_fpu_ax16 = [&] { builder.append(m_modrm.to_string_fpu_ax16()); };
    auto append_fpu_rm16 = [&] { builder.append(m_modrm.to_string_fpu16(*this)); };
    auto append_fpu_rm32 = [&] { builder.append(m_modrm.to_string_fpu32(*this)); };
    auto append_fpu_rm64 = [&] { builder.append(m_modrm.to_string_fpu64(*this)); };
    auto append_fpu_rm80 = [&] { builder.append(m_modrm.to_string_fpu80(*this)); };
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
    auto append_mmrm64 = [&] { builder.append(m_modrm.to_string_mm(*this)); };

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
    case OP_FPU:
        break;
    case OP_FPU_reg:
        append_fpu_reg();
        break;
    case OP_FPU_mem:
        append_fpu_mem();
        break;
    case OP_FPU_AX16:
        append_fpu_ax16();
        break;
    case OP_FPU_RM16:
        append_fpu_rm16();
        break;
    case OP_FPU_RM32:
        append_fpu_rm32();
        break;
    case OP_FPU_RM64:
        append_fpu_rm64();
        break;
    case OP_FPU_M80:
        append_fpu_rm80();
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

const char* register_name(FpuRegisterIndex register_index)
{
    static constexpr const char* names[] = { "st0", "st1", "st2", "st3", "st4", "st5", "st6", "st7" };
    return names[register_index & 7];
}

const char* register_name(MMXRegisterIndex register_index)
{
    static constexpr const char* names[] = { "mm0", "mm1", "mm2", "mm3", "mm4", "mm5", "mm6", "mm7" };
    return names[register_index & 7];
}

}
