/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibDisassembly/x86/Instruction.h>
#include <LibDisassembly/x86/Interpreter.h>

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

namespace Disassembly::X86 {

InstructionDescriptor s_table[3][256];
InstructionDescriptor s_0f_table[3][256];
InstructionDescriptor s_sse_table_np[256];
InstructionDescriptor s_sse_table_66[256];
InstructionDescriptor s_sse_table_f3[256];
InstructionDescriptor s_sse_table_f2[256];

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

static void build_in_table(InstructionDescriptor* table, u8 op, char const* mnemonic, InstructionFormat format, InstructionHandler handler, IsLockPrefixAllowed lock_prefix_allowed)
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
    case OP_mm1_imm8:
    case OP_mm1_mm2m64_imm8:
    case OP_reg_mm1_imm8:
    case OP_mm1_r32m16_imm8:
    case OP_xmm1_imm8:
    case OP_xmm1_xmm2m32_imm8:
    case OP_xmm1_xmm2m128_imm8:
    case OP_reg_xmm1_imm8:
    case OP_xmm1_r32m16_imm8:
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
    case OP_regW_immW:
        d.imm1_bytes = CurrentOperandSize;
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
    // default:
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
    case OP_reg:
    case OP_m64:
    case OP_mm1_rm32:
    case OP_rm32_mm2:
    case OP_mm1_mm2m64:
    case OP_mm1_mm2m32:
    case OP_mm1m64_mm2:
    case OP_reg_mm1:
    case __SSE:
    case OP_xmm1_xmm2m32:
    case OP_xmm1_xmm2m64:
    case OP_xmm1_xmm2m128:
    case OP_xmm1m32_xmm2:
    case OP_xmm1m64_xmm2:
    case OP_xmm1m128_xmm2:
    case OP_reg_xmm1:
    case OP_xmm1_rm32:
    case OP_xmm1_m64:
    case OP_m64_xmm2:
    case OP_rm8_xmm2m32:
    case OP_r32_xmm2m64:
    case OP_rm32_xmm2:
    case OP_xmm1_mm2m64:
    case OP_xmm_mm:
    case OP_mm_xmm:
    case OP_mm1m64_xmm2:
    case OP_mm1_xmm2m64:
    case OP_mm1_xmm2m128:
    case OP_r32_xmm2m32:
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

static void build_slash(InstructionDescriptor* table, u8 op, u8 slash, char const* mnemonic, InstructionFormat format, InstructionHandler handler, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    InstructionDescriptor& d = table[op];
    VERIFY(d.handler == nullptr);
    d.format = MultibyteWithSlash;
    d.has_rm = true;
    if (!d.slashes)
        d.slashes = new InstructionDescriptor[8];

    build_in_table(d.slashes, slash, mnemonic, format, handler, lock_prefix_allowed);
}

static void build_slash_rm(InstructionDescriptor* table, u8 op, u8 slash, u8 rm, char const* mnemonic, InstructionFormat format, InstructionHandler handler)
{
    VERIFY((rm & 0xc0) == 0xc0);
    VERIFY(((rm >> 3) & 7) == slash);

    InstructionDescriptor& d0 = table[op];
    VERIFY(d0.format == MultibyteWithSlash);
    InstructionDescriptor& d = d0.slashes[slash];

    if (!d.slashes) {
        // Slash/RM instructions are not always dense, so make them all default to the slash instruction.
        d.slashes = new InstructionDescriptor[8];
        for (int i = 0; i < 8; ++i) {
            d.slashes[i] = d;
            d.slashes[i].slashes = nullptr;
        }
    }

    build_in_table(d.slashes, rm & 7, mnemonic, format, handler, LockPrefixNotAllowed);
}

template<auto table>
static void build_base(u8 op, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_in_table(table[to_underlying(OperandSize::Size16)], op, mnemonic, format, impl, lock_prefix_allowed);
    build_in_table(table[to_underlying(OperandSize::Size32)], op, mnemonic, format, impl, lock_prefix_allowed);
    build_in_table(table[to_underlying(OperandSize::Size64)], op, mnemonic, format, impl, lock_prefix_allowed);
}

template<auto table>
static void build_base(u8 op, char const* mnemonic, InstructionFormat format16, InstructionHandler impl16, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_in_table(table[to_underlying(OperandSize::Size16)], op, mnemonic, format16, impl16, lock_prefix_allowed);
    build_in_table(table[to_underlying(OperandSize::Size32)], op, mnemonic, format32, impl32, lock_prefix_allowed);
    build_in_table(table[to_underlying(OperandSize::Size64)], op, mnemonic, format32, impl32, lock_prefix_allowed);
}

template<auto table>
static void build_base(u8 op, char const* mnemonic16, InstructionFormat format16, InstructionHandler impl16, char const* mnemonic32, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_in_table(table[to_underlying(OperandSize::Size16)], op, mnemonic16, format16, impl16, lock_prefix_allowed);
    build_in_table(table[to_underlying(OperandSize::Size32)], op, mnemonic32, format32, impl32, lock_prefix_allowed);
    build_in_table(table[to_underlying(OperandSize::Size64)], op, mnemonic32, format32, impl32, lock_prefix_allowed);
}

template<auto table>
static void build_slash_base(u8 op, u8 slash, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(table[to_underlying(OperandSize::Size16)], op, slash, mnemonic, format, impl, lock_prefix_allowed);
    build_slash(table[to_underlying(OperandSize::Size32)], op, slash, mnemonic, format, impl, lock_prefix_allowed);
    build_slash(table[to_underlying(OperandSize::Size64)], op, slash, mnemonic, format, impl, lock_prefix_allowed);
}

template<auto table>
static void build_slash_base(u8 op, u8 slash, char const* mnemonic, InstructionFormat format16, InstructionHandler impl16, InstructionFormat format32, InstructionHandler impl32, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    build_slash(table[to_underlying(OperandSize::Size16)], op, slash, mnemonic, format16, impl16, lock_prefix_allowed);
    build_slash(table[to_underlying(OperandSize::Size32)], op, slash, mnemonic, format32, impl32, lock_prefix_allowed);
    build_slash(table[to_underlying(OperandSize::Size64)], op, slash, mnemonic, format32, impl32, lock_prefix_allowed);
}

template<typename... Args>
static void build(Args... args)
{
    build_base<s_table>(args...);
}

template<typename... Args>
static void build_0f(Args... args)
{
    build_base<s_0f_table>(args...);
}

template<typename... Args>
static void build_slash(Args... args)
{
    build_slash_base<s_table>(args...);
}

template<typename... Args>
static void build_0f_slash(Args... args)
{
    build_slash_base<s_0f_table>(args...);
}

static void build_slash_rm(u8 op, u8 slash, u8 rm, char const* mnemonic, InstructionFormat format, InstructionHandler impl)
{
    build_slash_rm(s_table[to_underlying(OperandSize::Size16)], op, slash, rm, mnemonic, format, impl);
    build_slash_rm(s_table[to_underlying(OperandSize::Size32)], op, slash, rm, mnemonic, format, impl);
    build_slash_rm(s_table[to_underlying(OperandSize::Size64)], op, slash, rm, mnemonic, format, impl);
}

static void build_slash_reg(u8 op, u8 slash, char const* mnemonic, InstructionFormat format, InstructionHandler impl)
{
    for (int i = 0; i < 8; ++i)
        build_slash_rm(op, slash, 0xc0 | (slash << 3) | i, mnemonic, format, impl);
}

static void build_sse_np(u8 op, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format == InvalidFormat) {
        build_0f(op, mnemonic, format, impl, lock_prefix_allowed);
        build_in_table(s_sse_table_np, op, mnemonic, format, impl, lock_prefix_allowed);
        return;
    }
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format != __SSE)
        build_0f(op, "__SSE_temp", __SSE, nullptr, lock_prefix_allowed);

    VERIFY(s_0f_table[to_underlying(OperandSize::Size32)][op].format == __SSE);
    build_in_table(s_sse_table_np, op, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_sse_66(u8 op, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format != __SSE)
        build_0f(op, "__SSE_temp", __SSE, nullptr, lock_prefix_allowed);
    VERIFY(s_0f_table[to_underlying(AddressSize::Size32)][op].format == __SSE);
    build_in_table(s_sse_table_66, op, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_sse_f3(u8 op, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format != __SSE)
        build_0f(op, "__SSE_temp", __SSE, nullptr, lock_prefix_allowed);
    VERIFY(s_0f_table[to_underlying(OperandSize::Size32)][op].format == __SSE);
    build_in_table(s_sse_table_f3, op, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_sse_f2(u8 op, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format != __SSE)
        build_0f(op, "__SSE_temp", __SSE, nullptr, lock_prefix_allowed);
    VERIFY(s_0f_table[to_underlying(OperandSize::Size32)][op].format == __SSE);
    VERIFY(s_sse_table_f2[op].format == InvalidFormat);

    build_in_table(s_sse_table_f2, op, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_sse_np_slash(u8 op, u8 slash, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format != __SSE)
        build_0f(op, "__SSE_temp", __SSE, nullptr, lock_prefix_allowed);

    VERIFY(s_0f_table[to_underlying(OperandSize::Size32)][op].format == __SSE);
    build_slash(s_sse_table_np, op, slash, mnemonic, format, impl, lock_prefix_allowed);
}

static void build_sse_66_slash(u8 op, u8 slash, char const* mnemonic, InstructionFormat format, InstructionHandler impl, IsLockPrefixAllowed lock_prefix_allowed = LockPrefixNotAllowed)
{
    if (s_0f_table[to_underlying(OperandSize::Size32)][op].format != __SSE)
        build_0f(op, "__SSE_temp", __SSE, nullptr, lock_prefix_allowed);
    VERIFY(s_0f_table[to_underlying(OperandSize::Size32)][op].format == __SSE);
    build_slash(s_sse_table_66, op, slash, mnemonic, format, impl, lock_prefix_allowed);
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
    // FIXME: Extraordinary prefix 0x9B + 0xD9/6: FSTENV
    build_slash_rm(0xD9, 6, 0xF0, "F2XM1", OP_FPU, &Interpreter::F2XM1);
    build_slash_rm(0xD9, 6, 0xF1, "FYL2X", OP_FPU, &Interpreter::FYL2X);
    build_slash_rm(0xD9, 6, 0xF2, "FPTAN", OP_FPU, &Interpreter::FPTAN);
    build_slash_rm(0xD9, 6, 0xF3, "FPATAN", OP_FPU, &Interpreter::FPATAN);
    build_slash_rm(0xD9, 6, 0xF4, "FXTRACT", OP_FPU, &Interpreter::FXTRACT);
    build_slash_rm(0xD9, 6, 0xF5, "FPREM1", OP_FPU, &Interpreter::FPREM1);
    build_slash_rm(0xD9, 6, 0xF6, "FDECSTP", OP_FPU, &Interpreter::FDECSTP);
    build_slash_rm(0xD9, 6, 0xF7, "FINCSTP", OP_FPU, &Interpreter::FINCSTP);
    build_slash(0xD9, 7, "FNSTCW", OP_FPU_RM16, &Interpreter::FNSTCW);
    // FIXME: Extraordinary prefix 0x9B + 0xD9/7: FSTCW
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
    // FIXME: Extraordinary prefix 0x9B + 0xDB/4: FCLEX
    build_slash_rm(0xDB, 4, 0xE3, "FNINIT", OP_FPU_reg, &Interpreter::FNINIT);
    // FIXME: Extraordinary prefix 0x9B + 0xDB/4: FINIT
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
    // FIXME: Extraordinary prefix 0x9B + 0xDD/6: FSAVE
    build_slash(0xDD, 7, "FNSTSW", OP_FPU_RM16, &Interpreter::FNSTSW);
    // FIXME: Extraordinary prefix 0x9B + 0xDD/7: FSTSW

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
    // FIXME: Extraordinary prefix 0x9B + 0xDF/e: FSTSW_AX
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

    build(0xF1, "INT1", OP, &Interpreter::INT1);

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

    build_0f_slash(0x18, 0, "PREFETCHTNTA", OP_RM8, &Interpreter::PREFETCHTNTA);
    build_0f_slash(0x18, 1, "PREFETCHT0", OP_RM8, &Interpreter::PREFETCHT0);
    build_0f_slash(0x18, 2, "PREFETCHT1", OP_RM8, &Interpreter::PREFETCHT1);
    build_0f_slash(0x18, 3, "PREFETCHT2", OP_RM8, &Interpreter::PREFETCHT2);

    build_0f_slash(0x1f, 0, "NOP", OP_RM32, &Interpreter::NOP);

    // FIXME: Technically NoPrefix (sse_np_slash?)
    build_0f_slash(0xAE, 2, "LDMXCSR", OP_RM32, &Interpreter::LDMXCSR);
    build_0f_slash(0xAE, 3, "STMXCSR", OP_RM32, &Interpreter::STMXCSR);
    // FIXME: SFENCE: NP 0F AE F8

    build_0f_slash(0xBA, 4, "BT", OP_RM16_imm8, &Interpreter::BT_RM16_imm8, OP_RM32_imm8, &Interpreter::BT_RM32_imm8, LockPrefixAllowed);
    build_0f_slash(0xBA, 5, "BTS", OP_RM16_imm8, &Interpreter::BTS_RM16_imm8, OP_RM32_imm8, &Interpreter::BTS_RM32_imm8, LockPrefixAllowed);
    build_0f_slash(0xBA, 6, "BTR", OP_RM16_imm8, &Interpreter::BTR_RM16_imm8, OP_RM32_imm8, &Interpreter::BTR_RM32_imm8, LockPrefixAllowed);
    build_0f_slash(0xBA, 7, "BTC", OP_RM16_imm8, &Interpreter::BTC_RM16_imm8, OP_RM32_imm8, &Interpreter::BTC_RM32_imm8, LockPrefixAllowed);

    build_0f(0x02, "LAR", OP_reg16_RM16, &Interpreter::LAR_reg16_RM16, OP_reg32_RM32, &Interpreter::LAR_reg32_RM32);
    build_0f(0x03, "LSL", OP_reg16_RM16, &Interpreter::LSL_reg16_RM16, OP_reg32_RM32, &Interpreter::LSL_reg32_RM32);
    build_0f(0x06, "CLTS", OP, &Interpreter::CLTS);
    build_0f(0x09, "WBINVD", OP, &Interpreter::WBINVD);
    build_0f(0x0B, "UD2", OP, &Interpreter::UD2);

    build_sse_np(0x10, "MOVUPS", OP_xmm1_xmm2m128, &Interpreter::MOVUPS_xmm1_xmm2m128);
    build_sse_66(0x10, "MOVUPD", OP_xmm1_xmm2m128, &Interpreter::MOVUPD_xmm1_xmm2m128);
    build_sse_f3(0x10, "MOVSS", OP_xmm1_xmm2m32, &Interpreter::MOVSS_xmm1_xmm2m32);
    build_sse_f2(0x10, "MOVSD", OP_xmm1_xmm2m32, &Interpreter::MOVSD_xmm1_xmm2m32);
    build_sse_np(0x11, "MOVUPS", OP_xmm1m128_xmm2, &Interpreter::MOVUPS_xmm1m128_xmm2);
    build_sse_66(0x11, "MOVUPD", OP_xmm1m128_xmm2, &Interpreter::MOVUPD_xmm1m128_xmm2);
    build_sse_f3(0x11, "MOVSS", OP_xmm1m32_xmm2, &Interpreter::MOVSS_xmm1m32_xmm2);
    build_sse_f2(0x11, "MOVSD", OP_xmm1m32_xmm2, &Interpreter::MOVSD_xmm1m32_xmm2);
    build_sse_np(0x12, "MOVLPS", OP_xmm1_xmm2m64, &Interpreter::MOVLPS_xmm1_xmm2m64); // FIXME: This mnemonic is MOVHLPS when providing xmm2
    build_sse_66(0x12, "MOVLPD", OP_xmm1_m64, &Interpreter::MOVLPD_xmm1_m64);
    build_sse_np(0x13, "MOVLPS", OP_m64_xmm2, &Interpreter::MOVLPS_m64_xmm2);
    build_sse_66(0x13, "MOVLPD", OP_m64_xmm2, &Interpreter::MOVLPD_m64_xmm2);
    build_sse_np(0x14, "UNPCKLPS", OP_xmm1_xmm2m128, &Interpreter::UNPCKLPS_xmm1_xmm2m128);
    build_sse_66(0x14, "UNPCKLPD", OP_xmm1_xmm2m128, &Interpreter::UNPCKLPD_xmm1_xmm2m128);
    build_sse_np(0x15, "UNPCKHPS", OP_xmm1_xmm2m128, &Interpreter::UNPCKHPS_xmm1_xmm2m128);
    build_sse_66(0x15, "UNPCKHPD", OP_xmm1_xmm2m128, &Interpreter::UNPCKHPD_xmm1_xmm2m128);
    build_sse_np(0x16, "MOVHPS", OP_xmm1_xmm2m64, &Interpreter::MOVHPS_xmm1_xmm2m64); // FIXME: This mnemonic is MOVLHPS when providing xmm2
    build_sse_66(0x16, "MOVHPD", OP_xmm1_xmm2m64, &Interpreter::MOVHPD_xmm1_xmm2m64); // FIXME: This mnemonic is MOVLHPS when providing xmm2
    build_sse_np(0x17, "MOVHPS", OP_m64_xmm2, &Interpreter::MOVHPS_m64_xmm2);

    build_0f(0x20, "MOV", OP_reg32_CR, &Interpreter::MOV_reg32_CR);
    build_0f(0x21, "MOV", OP_reg32_DR, &Interpreter::MOV_reg32_DR);
    build_0f(0x22, "MOV", OP_CR_reg32, &Interpreter::MOV_CR_reg32);
    build_0f(0x23, "MOV", OP_DR_reg32, &Interpreter::MOV_DR_reg32);

    build_sse_np(0x28, "MOVAPS", OP_xmm1_xmm2m128, &Interpreter::MOVAPS_xmm1_xmm2m128);
    build_sse_66(0x28, "MOVAPD", OP_xmm1_xmm2m128, &Interpreter::MOVAPD_xmm1_xmm2m128);
    build_sse_np(0x29, "MOVAPS", OP_xmm1m128_xmm2, &Interpreter::MOVAPS_xmm1m128_xmm2);
    build_sse_66(0x29, "MOVAPD", OP_xmm1m128_xmm2, &Interpreter::MOVAPD_xmm1m128_xmm2);

    build_sse_np(0x2A, "CVTPI2PS", OP_xmm1_mm2m64, &Interpreter::CVTPI2PS_xmm1_mm2m64);
    build_sse_66(0x2A, "CVTPI2PD", OP_xmm1_mm2m64, &Interpreter::CVTPI2PD_xmm1_mm2m64);
    build_sse_f3(0x2A, "CVTSI2SS", OP_xmm1_rm32, &Interpreter::CVTSI2SS_xmm1_rm32);
    build_sse_f2(0x2A, "CVTSI2SD", OP_xmm1_rm32, &Interpreter::CVTSI2SD_xmm1_rm32);
    build_sse_np(0x2B, "MOVNTPS", OP_xmm1m128_xmm2, &Interpreter::MOVNTPS_xmm1m128_xmm2);
    build_sse_np(0x2C, "CVTTPS2PI", OP_mm1_xmm2m64, &Interpreter::CVTTPS2PI_mm1_xmm2m64);
    build_sse_66(0x2C, "CVTTPD2PI", OP_mm1_xmm2m128, &Interpreter::CVTTPD2PI_mm1_xmm2m128);
    build_sse_f3(0x2C, "CVTTSS2SI", OP_r32_xmm2m32, &Interpreter::CVTTSS2SI_r32_xmm2m32);
    build_sse_f2(0x2C, "CVTTSD2SI", OP_r32_xmm2m64, &Interpreter::CVTTSS2SI_r32_xmm2m64);
    build_sse_np(0x2D, "CVTPS2PI", OP_mm1_xmm2m64, &Interpreter::CVTPS2PI_xmm1_mm2m64);
    build_sse_66(0x2D, "CVTPD2PI", OP_mm1_xmm2m128, &Interpreter::CVTPD2PI_xmm1_mm2m128);
    build_sse_f3(0x2D, "CVTSS2SI", OP_r32_xmm2m32, &Interpreter::CVTSS2SI_r32_xmm2m32);
    build_sse_f2(0x2D, "CVTSD2SI", OP_r32_xmm2m64, &Interpreter::CVTSD2SI_xmm1_rm64);
    build_sse_np(0x2E, "UCOMISS", OP_xmm1_xmm2m32, &Interpreter::UCOMISS_xmm1_xmm2m32);
    build_sse_66(0x2E, "UCOMISD", OP_xmm1_xmm2m64, &Interpreter::UCOMISD_xmm1_xmm2m64);
    build_sse_np(0x2F, "COMISS", OP_xmm1_xmm2m32, &Interpreter::COMISS_xmm1_xmm2m32);
    build_sse_66(0x2F, "COMISD", OP_xmm1_xmm2m64, &Interpreter::COMISD_xmm1_xmm2m64);

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

    build_sse_np(0x50, "MOVMSKPS", OP_reg_xmm1, &Interpreter::MOVMSKPS_reg_xmm);
    build_sse_66(0x50, "MOVMSKPD", OP_reg_xmm1, &Interpreter::MOVMSKPD_reg_xmm);
    build_sse_np(0x51, "SQRTPS", OP_xmm1_xmm2m128, &Interpreter::SQRTPS_xmm1_xmm2m128);
    build_sse_66(0x51, "SQRTPD", OP_xmm1_xmm2m128, &Interpreter::SQRTPD_xmm1_xmm2m128);
    build_sse_f3(0x51, "SQRTSS", OP_xmm1_xmm2m32, &Interpreter::SQRTSS_xmm1_xmm2m32);
    build_sse_f2(0x51, "SQRTSD", OP_xmm1_xmm2m32, &Interpreter::SQRTSD_xmm1_xmm2m32);
    build_sse_np(0x52, "RSQRTPS", OP_xmm1_xmm2m128, &Interpreter::RSQRTPS_xmm1_xmm2m128);
    build_sse_f3(0x52, "RSQRTSS", OP_xmm1_xmm2m32, &Interpreter::RSQRTSS_xmm1_xmm2m32);
    build_sse_np(0x53, "RCPPS", OP_xmm1_xmm2m128, &Interpreter::RCPPS_xmm1_xmm2m128);
    build_sse_f3(0x53, "RCPSS", OP_xmm1_xmm2m32, &Interpreter::RCPSS_xmm1_xmm2m32);
    build_sse_np(0x54, "ANDPS", OP_xmm1_xmm2m128, &Interpreter::ANDPS_xmm1_xmm2m128);
    build_sse_66(0x54, "ANDPD", OP_xmm1_xmm2m128, &Interpreter::ANDPD_xmm1_xmm2m128);
    build_sse_np(0x55, "ANDNPS", OP_xmm1_xmm2m128, &Interpreter::ANDNPS_xmm1_xmm2m128);
    build_sse_66(0x55, "ANDNPD", OP_xmm1_xmm2m128, &Interpreter::ANDNPD_xmm1_xmm2m128);
    build_sse_np(0x56, "ORPS", OP_xmm1_xmm2m128, &Interpreter::ORPS_xmm1_xmm2m128);
    build_sse_66(0x56, "ORPD", OP_xmm1_xmm2m128, &Interpreter::ORPD_xmm1_xmm2m128);
    build_sse_np(0x57, "XORPS", OP_xmm1_xmm2m128, &Interpreter::XORPS_xmm1_xmm2m128);
    build_sse_66(0x57, "XORPD", OP_xmm1_xmm2m128, &Interpreter::XORPD_xmm1_xmm2m128);

    build_sse_np(0x58, "ADDPS", OP_xmm1_xmm2m128, &Interpreter::ADDPS_xmm1_xmm2m128);
    build_sse_66(0x58, "ADDPD", OP_xmm1_xmm2m128, &Interpreter::ADDPD_xmm1_xmm2m128);
    build_sse_f3(0x58, "ADDSS", OP_xmm1_xmm2m32, &Interpreter::ADDSS_xmm1_xmm2m32);
    build_sse_f2(0x58, "ADDSD", OP_xmm1_xmm2m32, &Interpreter::ADDSD_xmm1_xmm2m32);
    build_sse_np(0x59, "MULPS", OP_xmm1_xmm2m128, &Interpreter::MULPS_xmm1_xmm2m128);
    build_sse_66(0x59, "MULPD", OP_xmm1_xmm2m128, &Interpreter::MULPD_xmm1_xmm2m128);
    build_sse_f3(0x59, "MULSS", OP_xmm1_xmm2m32, &Interpreter::MULSS_xmm1_xmm2m32);
    build_sse_f2(0x59, "MULSD", OP_xmm1_xmm2m32, &Interpreter::MULSD_xmm1_xmm2m32);
    build_sse_np(0x5A, "CVTPS2PD", OP_xmm1_xmm2m64, &Interpreter::CVTPS2PD_xmm1_xmm2m64);
    build_sse_66(0x5A, "CVTPD2PS", OP_xmm1_xmm2m128, &Interpreter::CVTPD2PS_xmm1_xmm2m128);
    build_sse_f3(0x5A, "CVTSS2SD", OP_xmm1_xmm2m32, &Interpreter::CVTSS2SD_xmm1_xmm2m32);
    build_sse_f2(0x5A, "CVTSD2SS", OP_xmm1_xmm2m64, &Interpreter::CVTSD2SS_xmm1_xmm2m64);
    build_sse_np(0x5B, "CVTDQ2PS", OP_xmm1_xmm2m128, &Interpreter::CVTDQ2PS_xmm1_xmm2m128);
    build_sse_66(0x5B, "CVTPS2DQ", OP_xmm1_xmm2m128, &Interpreter::CVTPS2DQ_xmm1_xmm2m128);
    build_sse_f3(0x5B, "CVTTPS2DQ", OP_xmm1_xmm2m128, &Interpreter::CVTTPS2DQ_xmm1_xmm2m128);

    build_sse_np(0x5C, "SUBPS", OP_xmm1_xmm2m128, &Interpreter::SUBPS_xmm1_xmm2m128);
    build_sse_66(0x5C, "SUBPD", OP_xmm1_xmm2m128, &Interpreter::SUBPD_xmm1_xmm2m128);
    build_sse_f3(0x5C, "SUBSS", OP_xmm1_xmm2m32, &Interpreter::SUBSS_xmm1_xmm2m32);
    build_sse_f2(0x5C, "SUBSD", OP_xmm1_xmm2m32, &Interpreter::SUBSD_xmm1_xmm2m32);
    build_sse_np(0x5D, "MINPS", OP_xmm1_xmm2m128, &Interpreter::MINPS_xmm1_xmm2m128);
    build_sse_66(0x5D, "MINPD", OP_xmm1_xmm2m128, &Interpreter::MINPD_xmm1_xmm2m128);
    build_sse_f3(0x5D, "MINSS", OP_xmm1_xmm2m32, &Interpreter::MINSS_xmm1_xmm2m32);
    build_sse_f2(0x5D, "MINSD", OP_xmm1_xmm2m32, &Interpreter::MINSD_xmm1_xmm2m32);
    build_sse_np(0x5E, "DIVPS", OP_xmm1_xmm2m128, &Interpreter::DIVPS_xmm1_xmm2m128);
    build_sse_66(0x5E, "DIVPD", OP_xmm1_xmm2m128, &Interpreter::DIVPD_xmm1_xmm2m128);
    build_sse_f3(0x5E, "DIVSS", OP_xmm1_xmm2m32, &Interpreter::DIVSS_xmm1_xmm2m32);
    build_sse_f2(0x5E, "DIVSD", OP_xmm1_xmm2m32, &Interpreter::DIVSD_xmm1_xmm2m32);
    build_sse_np(0x5F, "MAXPS", OP_xmm1_xmm2m128, &Interpreter::MAXPS_xmm1_xmm2m128);
    build_sse_66(0x5F, "MAXPD", OP_xmm1_xmm2m128, &Interpreter::MAXPD_xmm1_xmm2m128);
    build_sse_f3(0x5F, "MAXSS", OP_xmm1_xmm2m32, &Interpreter::MAXSS_xmm1_xmm2m32);
    build_sse_f2(0x5F, "MAXSD", OP_xmm1_xmm2m32, &Interpreter::MAXSD_xmm1_xmm2m32);

    build_0f(0x60, "PUNPCKLBW", OP_mm1_mm2m32, &Interpreter::PUNPCKLBW_mm1_mm2m32);
    build_0f(0x61, "PUNPCKLWD", OP_mm1_mm2m32, &Interpreter::PUNPCKLWD_mm1_mm2m32);
    build_0f(0x62, "PUNPCKLDQ", OP_mm1_mm2m32, &Interpreter::PUNPCKLDQ_mm1_mm2m32);
    build_0f(0x63, "PACKSSWB", OP_mm1_mm2m64, &Interpreter::PACKSSWB_mm1_mm2m64);
    build_0f(0x64, "PCMPGTB", OP_mm1_mm2m64, &Interpreter::PCMPGTB_mm1_mm2m64);
    build_0f(0x65, "PCMPGTW", OP_mm1_mm2m64, &Interpreter::PCMPGTW_mm1_mm2m64);
    build_0f(0x66, "PCMPGTD", OP_mm1_mm2m64, &Interpreter::PCMPGTD_mm1_mm2m64);
    build_0f(0x67, "PACKUSWB", OP_mm1_mm2m64, &Interpreter::PACKUSWB_mm1_mm2m64);
    build_0f(0x68, "PUNPCKHBW", OP_mm1_mm2m64, &Interpreter::PUNPCKHBW_mm1_mm2m64);
    build_0f(0x69, "PUNPCKHWD", OP_mm1_mm2m64, &Interpreter::PUNPCKHWD_mm1_mm2m64);
    build_0f(0x6A, "PUNPCKHDQ", OP_mm1_mm2m64, &Interpreter::PUNPCKHDQ_mm1_mm2m64);
    build_0f(0x6B, "PACKSSDW", OP_mm1_mm2m64, &Interpreter::PACKSSDW_mm1_mm2m64);
    build_sse_66(0x6C, "PUNPCKLQDQ", OP_xmm1_xmm2m128, &Interpreter::PUNPCKLQDQ_xmm1_xmm2m128);
    build_sse_66(0x6D, "PUNPCKHQDQ", OP_xmm1_xmm2m128, &Interpreter::PUNPCKHQDQ_xmm1_xmm2m128);
    build_0f(0x6E, "MOVD", OP_mm1_rm32, &Interpreter::MOVD_mm1_rm32); // FIXME: REX.W -> MOVQ
    build_sse_np(0x6F, "MOVQ", OP_mm1_mm2m64, &Interpreter::MOVQ_mm1_mm2m64);
    build_sse_66(0x6F, "MOVDQA", OP_xmm1_xmm2m128, &Interpreter::MOVDQA_xmm1_xmm2m128);
    build_sse_f3(0x6F, "MOVDQU", OP_xmm1_xmm2m128, &Interpreter::MOVDQU_xmm1_xmm2m128);

    build_sse_np(0x70, "PSHUFW", OP_mm1_mm2m64_imm8, &Interpreter::PSHUFW_mm1_mm2m64_imm8);
    build_sse_66(0x70, "PSHUFD", OP_xmm1_xmm2m128_imm8, &Interpreter::PSHUFD_xmm1_xmm2m128_imm8);
    build_sse_f3(0x70, "PSHUFHW", OP_xmm1_xmm2m128_imm8, &Interpreter::PSHUFHW_xmm1_xmm2m128_imm8);
    build_sse_f2(0x70, "PSHUFLW", OP_xmm1_xmm2m128_imm8, &Interpreter::PSHUFLW_xmm1_xmm2m128_imm8);
    build_0f_slash(0x71, 2, "PSRLW", OP_mm1_imm8, &Interpreter::PSRLW_mm1_imm8);
    build_0f_slash(0x71, 4, "PSRAW", OP_mm1_imm8, &Interpreter::PSRAW_mm1_imm8);
    build_0f_slash(0x71, 6, "PSLLW", OP_mm1_imm8, &Interpreter::PSLLD_mm1_imm8);

    build_0f_slash(0x72, 2, "PSRLD", OP_mm1_imm8, &Interpreter::PSRLD_mm1_imm8);
    build_0f_slash(0x72, 4, "PSRAD", OP_mm1_imm8, &Interpreter::PSRAD_mm1_imm8);
    build_0f_slash(0x72, 6, "PSLLW", OP_mm1_imm8, &Interpreter::PSLLW_mm1_imm8);

    build_sse_np_slash(0x73, 2, "PSRLQ", OP_mm1_imm8, &Interpreter::PSRLQ_mm1_imm8);
    build_sse_66_slash(0x73, 2, "PSRLQ", OP_xmm1_imm8, &Interpreter::PSRLQ_xmm1_imm8);
    build_sse_66_slash(0x73, 3, "PSRLDQ", OP_xmm1_imm8, &Interpreter::PSRLDQ_xmm1_imm8);
    build_sse_np_slash(0x73, 6, "PSLLQ", OP_mm1_imm8, &Interpreter::PSLLQ_mm1_imm8);
    build_sse_66_slash(0x73, 6, "PSLLQ", OP_xmm1_imm8, &Interpreter::PSLLQ_xmm1_imm8);
    build_sse_66_slash(0x73, 7, "PSLLDQ", OP_xmm1_imm8, &Interpreter::PSLLDQ_xmm1_imm8);

    build_0f(0x74, "PCMPEQB", OP_mm1_mm2m64, &Interpreter::PCMPEQB_mm1_mm2m64);
    build_0f(0x75, "PCMPEQW", OP_mm1_mm2m64, &Interpreter::PCMPEQW_mm1_mm2m64);
    build_0f(0x76, "PCMPEQD", OP_mm1_mm2m64, &Interpreter::PCMPEQD_mm1_mm2m64);
    build_0f(0x77, "EMMS", OP, &Interpreter::EMMS);                         // Technically NP
    build_sse_np(0x7E, "MOVD", OP_rm32_mm2, &Interpreter::MOVD_rm32_mm2);   // FIXME: REW.W -> MOVQ
    build_sse_66(0x7E, "MOVD", OP_rm32_xmm2, &Interpreter::MOVD_rm32_xmm2); // FIXME: REW.W -> MOVQ
    build_sse_f3(0x7E, "MOVQ", OP_xmm1_xmm2m128, &Interpreter::MOVQ_xmm1_xmm2m128);
    build_sse_np(0x7F, "MOVQ", OP_mm1m64_mm2, &Interpreter::MOVQ_mm1m64_mm2);
    build_sse_66(0x7F, "MOVDQA", OP_xmm1m128_xmm2, &Interpreter::MOVDQA_xmm1m128_xmm2);
    build_sse_f3(0x7F, "MOVDQU", OP_xmm1m128_xmm2, &Interpreter::MOVDQU_xmm1m128_xmm2);

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
    build_sse_np(0xC2, "CMPPS", OP_xmm1_xmm2m128_imm8, &Interpreter::CMPPS_xmm1_xmm2m128_imm8);
    build_sse_66(0xC2, "CMPPD", OP_xmm1_xmm2m128_imm8, &Interpreter::CMPPD_xmm1_xmm2m128_imm8);
    build_sse_f3(0xC2, "CMPSS", OP_xmm1_xmm2m32_imm8, &Interpreter::CMPSS_xmm1_xmm2m32_imm8);
    build_sse_f2(0xC2, "CMPSD", OP_xmm1_xmm2m32_imm8, &Interpreter::CMPSD_xmm1_xmm2m32_imm8);

    build_sse_np(0xC4, "PINSRW", OP_mm1_r32m16_imm8, &Interpreter::PINSRW_mm1_r32m16_imm8);
    build_sse_66(0xC4, "PINSRW", OP_xmm1_r32m16_imm8, &Interpreter::PINSRW_xmm1_r32m16_imm8);
    build_sse_np(0xC5, "PEXTRW", OP_reg_mm1_imm8, &Interpreter::PEXTRW_reg_mm1_imm8);
    build_sse_66(0xC5, "PEXTRW", OP_reg_xmm1_imm8, &Interpreter::PEXTRW_reg_xmm1_imm8);
    build_sse_np(0xC6, "SHUFPS", OP_xmm1_xmm2m128_imm8, &Interpreter::SHUFPS_xmm1_xmm2m128_imm8);
    build_sse_66(0xC6, "SHUFPD", OP_xmm1_xmm2m128_imm8, &Interpreter::SHUFPD_xmm1_xmm2m128_imm8);

    build_0f_slash(0xC7, 1, "CMPXCHG8B", OP_m64, &Interpreter::CMPXCHG8B_m64);
    // FIXME: NP 0f c7 /2 XRSTORS[64] mem
    // FIXME: NP 0F C7 / 4 XSAVEC mem
    // FIXME: NP 0F C7 /5 XSAVES mem
    // FIXME: VMPTRLD, VMPTRST, VMCLR, VMXON
    // This is technically NFx prefixed
    // FIXME: f3 0f c7 /7 RDPID
    build_0f_slash(0xC7, 6, "RDRAND", OP_reg, &Interpreter::RDRAND_reg);
    build_0f_slash(0xC7, 7, "RDSEED", OP_reg, &Interpreter::RDSEED_reg);

    for (u8 i = 0xc8; i <= 0xcf; ++i)
        build_0f(i, "BSWAP", OP_reg32, &Interpreter::BSWAP_reg32);

    build_0f(0xD1, "PSRLW", OP_mm1_mm2m64, &Interpreter::PSRLW_mm1_mm2m64);
    build_0f(0xD2, "PSRLD", OP_mm1_mm2m64, &Interpreter::PSRLD_mm1_mm2m64);
    build_0f(0xD3, "PSRLQ", OP_mm1_mm2m64, &Interpreter::PSRLQ_mm1_mm2m64);
    build_0f(0xD4, "PADDQ", OP_mm1_mm2m64, &Interpreter::PADDQ_mm1_mm2m64);
    build_0f(0xD5, "PMULLW", OP_mm1_mm2m64, &Interpreter::PMULLW_mm1_mm2m64);

    build_sse_66(0xD6, "MOVQ", OP_xmm1m128_xmm2, &Interpreter::MOVQ_xmm1m128_xmm2);
    build_sse_f3(0xD6, "MOVQ2DQ", OP_xmm_mm, &Interpreter::MOVQ2DQ_xmm_mm);
    build_sse_f2(0xD6, "MOVDQ2Q", OP_mm_xmm, &Interpreter::MOVDQ2Q_mm_xmm);
    build_sse_np(0xD7, "PMOVMSKB", OP_reg_mm1, &Interpreter::PMOVMSKB_reg_mm1);
    build_sse_66(0xD7, "PMOVMSKB", OP_reg_xmm1, &Interpreter::PMOVMSKB_reg_xmm1);

    build_0f(0xDB, "PAND", OP_mm1_mm2m64, &Interpreter::PAND_mm1_mm2m64);
    build_0f(0xD8, "PSUBUSB", OP_mm1_mm2m64, &Interpreter::PSUBUSB_mm1_mm2m64);
    build_0f(0xD9, "PSUBUSW", OP_mm1_mm2m64, &Interpreter::PSUBUSW_mm1_mm2m64);

    build_sse_np(0xDA, "PMINUB", OP_mm1_mm2m64, &Interpreter::PMINUB_mm1_mm2m64);
    build_sse_66(0xDA, "PMINUB", OP_xmm1_xmm2m128, &Interpreter::PMINUB_xmm1_xmm2m128);

    build_0f(0xDC, "PADDUSB", OP_mm1_mm2m64, &Interpreter::PADDUSB_mm1_mm2m64);
    build_0f(0xDD, "PADDUSW", OP_mm1_mm2m64, &Interpreter::PADDUSW_mm1_mm2m64);
    build_sse_np(0xDE, "PMAXUB", OP_mm1_mm2m64, &Interpreter::PMAXUB_mm1_mm2m64);
    build_sse_66(0xDE, "PMAXUB", OP_xmm1_xmm2m128, &Interpreter::PMAXUB_xmm1_xmm2m128);
    build_0f(0xDF, "PANDN", OP_mm1_mm2m64, &Interpreter::PANDN_mm1_mm2m64);

    build_sse_np(0xE0, "PAVGB", OP_mm1_mm2m64, &Interpreter::PAVGB_mm1_mm2m64);
    build_sse_66(0xE0, "PAVGB", OP_xmm1_xmm2m128, &Interpreter::PAVGB_xmm1_xmm2m128);
    build_sse_np(0xE3, "PAVGW", OP_mm1_mm2m64, &Interpreter::PAVGW_mm1_mm2m64);
    build_sse_66(0xE3, "PAVGW", OP_xmm1_xmm2m128, &Interpreter::PAVGW_xmm1_xmm2m128);
    build_sse_np(0xE4, "PMULHUW ", OP_mm1_mm2m64, &Interpreter::PMULHUW_mm1_mm2m64);
    build_sse_66(0xE4, "PMULHUW ", OP_xmm1_xmm2m64, &Interpreter::PMULHUW_xmm1_xmm2m64);
    build_0f(0xE5, "PMULHW", OP_mm1_mm2m64, &Interpreter::PMULHW_mm1_mm2m64);

    build_sse_66(0xE6, "CVTTPD2DQ", OP_xmm1_xmm2m128, &Interpreter::CVTTPD2DQ_xmm1_xmm2m128);
    build_sse_f2(0xE6, "CVTPD2DQ", OP_xmm1_xmm2m128, &Interpreter::CVTPD2DQ_xmm1_xmm2m128);
    build_sse_f3(0xE6, "CVTDQ2PD", OP_xmm1_xmm2m64, &Interpreter::CVTDQ2PD_xmm1_xmm2m64);
    build_sse_np(0xE7, "MOVNTQ", OP_mm1m64_mm2, &Interpreter::MOVNTQ_m64_mm1);

    build_sse_np(0xEA, "PMINSB", OP_mm1_mm2m64, &Interpreter::PMINSB_mm1_mm2m64);
    build_sse_66(0xEA, "PMINSB", OP_xmm1_xmm2m128, &Interpreter::PMINSB_xmm1_xmm2m128);
    build_0f(0xEB, "POR", OP_mm1_mm2m64, &Interpreter::POR_mm1_mm2m64);
    build_0f(0xE1, "PSRAW", OP_mm1_mm2m64, &Interpreter::PSRAW_mm1_mm2m64);
    build_0f(0xE2, "PSRAD", OP_mm1_mm2m64, &Interpreter::PSRAD_mm1_mm2m64);
    build_0f(0xE8, "PSUBSB", OP_mm1_mm2m64, &Interpreter::PSUBSB_mm1_mm2m64);
    build_0f(0xE9, "PSUBSW", OP_mm1_mm2m64, &Interpreter::PSUBSW_mm1_mm2m64);
    build_0f(0xEC, "PADDSB", OP_mm1_mm2m64, &Interpreter::PADDSB_mm1_mm2m64);
    build_0f(0xED, "PADDSW", OP_mm1_mm2m64, &Interpreter::PADDSW_mm1_mm2m64);
    build_sse_np(0xEE, "PMAXSB", OP_mm1_mm2m64, &Interpreter::PMAXSB_mm1_mm2m64);
    build_sse_66(0xEE, "PMAXSB", OP_xmm1_xmm2m128, &Interpreter::PMAXSB_xmm1_xmm2m128);
    build_0f(0xEF, "PXOR", OP_mm1_mm2m64, &Interpreter::PXOR_mm1_mm2m64);

    build_0f(0xF1, "PSLLW", OP_mm1_mm2m64, &Interpreter::PSLLW_mm1_mm2m64);
    build_0f(0xF2, "PSLLD", OP_mm1_mm2m64, &Interpreter::PSLLD_mm1_mm2m64);
    build_0f(0xF3, "PSLLQ", OP_mm1_mm2m64, &Interpreter::PSLLQ_mm1_mm2m64);
    build_sse_np(0xF4, "PMULUDQ", OP_mm1_mm2m64, &Interpreter::PMULUDQ_mm1_mm2m64);
    build_sse_66(0xF4, "PMULUDQ", OP_xmm1_xmm2m128, &Interpreter::PMULUDQ_mm1_mm2m128);
    build_0f(0xF5, "PMADDWD", OP_mm1_mm2m64, &Interpreter::PMADDWD_mm1_mm2m64);
    build_sse_np(0xF6, "PSADBW", OP_mm1_mm2m64, &Interpreter::PSADBB_mm1_mm2m64);
    build_sse_66(0xF6, "PSADBW", OP_xmm1_xmm2m128, &Interpreter::PSADBB_xmm1_xmm2m128);
    build_sse_np(0xF7, "MASKMOVQ", OP_mm1_mm2m64, &Interpreter::MASKMOVQ_mm1_mm2m64);
    build_0f(0xF8, "PSUBB", OP_mm1_mm2m64, &Interpreter::PSUBB_mm1_mm2m64);
    build_0f(0xF9, "PSUBW", OP_mm1_mm2m64, &Interpreter::PSUBW_mm1_mm2m64);
    build_0f(0xFA, "PSUBD", OP_mm1_mm2m64, &Interpreter::PSUBD_mm1_mm2m64);
    build_0f(0xFB, "PSUBQ", OP_mm1_mm2m64, &Interpreter::PSUBQ_mm1_mm2m64);
    build_0f(0xFC, "PADDB", OP_mm1_mm2m64, &Interpreter::PADDB_mm1_mm2m64);
    build_0f(0xFD, "PADDW", OP_mm1_mm2m64, &Interpreter::PADDW_mm1_mm2m64);
    build_0f(0xFE, "PADDD", OP_mm1_mm2m64, &Interpreter::PADDD_mm1_mm2m64);
    build_0f(0xFF, "UD0", OP, &Interpreter::UD0);

    // Changes between 32-bit and 64-bit. These are marked with i64/d64/f64 in the Intel manual's opcode tables
    auto* table64 = s_table[to_underlying(OperandSize::Size64)];
    table64[0x06] = {}; // PUSH ES
    table64[0x07] = {}; // POP ES
    table64[0x16] = {}; // PUSH SS
    table64[0x17] = {}; // POP SS
    table64[0x27] = {}; // DAA
    table64[0x37] = {}; // AAA
    for (u8 rex = 0x40; rex < 0x50; rex++)
        table64[rex] = {}; // INC/DEC, replaced by REX prefixes
    for (u8 pushPop = 0x50; pushPop < 0x60; pushPop++)
        table64[pushPop].long_mode_default_64 = true; // PUSH/POP general register
    for (u8 i = 0x60; i < 0x68; i++)
        table64[i] = {}; // PUSHA{D}, POPA{D}, BOUND
    // ARPL replaced by MOVSXD
    build_in_table(table64, 0x63, "MOVSXD", OP_RM32_reg32, nullptr, LockPrefixNotAllowed);
    table64[0x68].long_mode_default_64 = true; // PUSH
    table64[0x6A].long_mode_default_64 = true; // PUSH
    for (u8 jmp = 0x70; jmp < 0x80; jmp++)
        table64[jmp].long_mode_force_64 = true; // Jcc
    table64[0x9A] = {};                         // far CALL
    table64[0x9C].long_mode_default_64 = true;  // PUSHF/D/Q
    table64[0x9D].long_mode_default_64 = true;  // POPF/D/Q
    for (u8 mov = 0xB8; mov <= 0xBF; ++mov)
        build_in_table(table64, mov, "MOV", OP_regW_immW, &Interpreter::MOV_reg32_imm32, LockPrefixNotAllowed);
    table64[0xC2].long_mode_force_64 = true;   // near RET
    table64[0xC3].long_mode_force_64 = true;   // near RET
    table64[0xC4] = {};                        // LES
    table64[0xC5] = {};                        // LDS
    table64[0xC9].long_mode_default_64 = true; // LEAVE
    table64[0xCE].long_mode_default_64 = true; // INTO
    table64[0xD4] = {};                        // AAM
    table64[0xD5] = {};                        // AAD
    for (u8 i = 0; i < 4; i++) {
        table64[0xE0 | i].long_mode_force_64 = true; // LOOPN[EZ], LOOP[EZ], LOOP, JrCXZ
        table64[0xE8 | i].long_mode_force_64 = true; // near CALL, {near,far,short} JMP
    }

    auto* table64_0f = s_0f_table[to_underlying(OperandSize::Size64)];
    build_in_table(table64_0f, 0x05, "SYSCALL", OP, nullptr, LockPrefixNotAllowed);
    build_in_table(table64_0f, 0x07, "SYSRET", OP, nullptr, LockPrefixNotAllowed);
    for (u8 i = 0x80; i < 0x90; i++)
        table64_0f[i].long_mode_force_64 = true;  // Jcc
    table64_0f[0xA0].long_mode_default_64 = true; // PUSH FS
    table64_0f[0xA1].long_mode_default_64 = true; // POP FS
    table64_0f[0xA8].long_mode_default_64 = true; // PUSH GS
    table64_0f[0xA9].long_mode_default_64 = true; // POP GS
}

static StringView register_name(RegisterIndex8);
static StringView register_name(RegisterIndex16);
static StringView register_name(RegisterIndex32);
static StringView register_name(RegisterIndex64);
static StringView register_name(FpuRegisterIndex);
static StringView register_name(SegmentRegister);
static StringView register_name(MMXRegisterIndex);
static StringView register_name(XMMRegisterIndex);

StringView Instruction::reg8_name() const
{
    return register_name(static_cast<RegisterIndex8>(register_index()));
}

StringView Instruction::reg16_name() const
{
    return register_name(static_cast<RegisterIndex16>(register_index()));
}

StringView Instruction::reg32_name() const
{
    return register_name(static_cast<RegisterIndex32>(register_index()));
}

StringView Instruction::reg64_name() const
{
    return register_name(static_cast<RegisterIndex64>(register_index()));
}

ByteString MemoryOrRegisterReference::to_byte_string_o8(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg8());
    return ByteString::formatted("[{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_o16(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg16());
    return ByteString::formatted("[{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_o32(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg32());
    return ByteString::formatted("[{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_o64(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg64());
    return ByteString::formatted("[{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu_reg() const
{
    VERIFY(is_register());
    return register_name(reg_fpu());
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu_mem(Instruction const& insn) const
{
    VERIFY(!is_register());
    return ByteString::formatted("[{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu_ax16() const
{
    VERIFY(is_register());
    return register_name(reg16());
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu16(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg_fpu());
    return ByteString::formatted("word ptr [{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu32(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg_fpu());
    return ByteString::formatted("dword ptr [{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu64(Instruction const& insn) const
{
    if (is_register())
        return register_name(reg_fpu());
    return ByteString::formatted("qword ptr [{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string_fpu80(Instruction const& insn) const
{
    VERIFY(!is_register());
    return ByteString::formatted("tbyte ptr [{}]", to_byte_string(insn));
}
ByteString MemoryOrRegisterReference::to_byte_string_mm(Instruction const& insn) const
{
    if (is_register())
        return register_name(static_cast<MMXRegisterIndex>(m_register_index));
    return ByteString::formatted("[{}]", to_byte_string(insn));
}
ByteString MemoryOrRegisterReference::to_byte_string_xmm(Instruction const& insn) const
{
    if (is_register())
        return register_name(static_cast<XMMRegisterIndex>(m_register_index));
    return ByteString::formatted("[{}]", to_byte_string(insn));
}

ByteString MemoryOrRegisterReference::to_byte_string(Instruction const& insn) const
{
    switch (insn.address_size()) {
    case AddressSize::Size64:
        return to_byte_string_a64();
    case AddressSize::Size32:
        return insn.mode() == ProcessorMode::Long ? to_byte_string_a64() : to_byte_string_a32();
    case AddressSize::Size16:
        return to_byte_string_a16();
    }
    VERIFY_NOT_REACHED();
}

ByteString MemoryOrRegisterReference::to_byte_string_a16() const
{
    ByteString base;
    bool hasDisplacement = false;

    switch (rm()) {
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
        if (mod() == 0)
            base = ByteString::formatted("{:#04x}", m_displacement16);
        else
            base = "bp";
        break;
    }

    switch (mod()) {
    case 0b01:
    case 0b10:
        hasDisplacement = true;
    }

    if (!hasDisplacement)
        return base;

    return ByteString::formatted("{}{:+#x}", base, (i16)m_displacement16);
}

ByteString MemoryOrRegisterReference::sib_to_byte_string(ProcessorMode mode) const
{
    ByteString scale;
    ByteString index;
    ByteString base;
    switch (m_sib_scale) {
    case 0:;
        break;
    case 1:
        scale = "*2";
        break;
    case 2:
        scale = "*4";
        break;
    case 3:
        scale = "*8";
        break;
    }
    if (m_sib_index != 4)
        index = mode == ProcessorMode::Long ? register_name(RegisterIndex64(m_sib_index)) : register_name(RegisterIndex32(m_sib_index));
    if (m_sib_base == 5) {
        switch (m_reg) {
        case 1:
        case 2:
            base = mode == ProcessorMode::Long ? "rbp" : "ebp";
            break;
        }
    } else {
        base = mode == ProcessorMode::Long ? register_name(RegisterIndex64(m_sib_base)) : register_name(RegisterIndex32(m_sib_base));
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
    return builder.to_byte_string();
}

ByteString MemoryOrRegisterReference::to_byte_string_a64() const
{
    if (is_register())
        return register_name(static_cast<RegisterIndex64>(m_register_index));

    bool has_displacement = false;
    switch (mod()) {
    case 0b00:
        has_displacement = m_rm == 5;
        break;
    case 0b01:
    case 0b10:
        has_displacement = true;
    }
    if (m_has_sib && m_sib_base == 5)
        has_displacement = true;

    ByteString base;
    switch (m_rm) {
    case 5:
        if (mod() == 0)
            base = "rip";
        else
            base = "rbp";
        break;
    case 4:
        base = sib_to_byte_string(ProcessorMode::Long);
        break;
    default:
        base = register_name(RegisterIndex64(m_rm));
    }

    if (!has_displacement)
        return base;

    return ByteString::formatted("{}{:+#x}", base, (i32)m_displacement32);
}

ByteString MemoryOrRegisterReference::to_byte_string_a32() const
{
    if (is_register())
        return register_name(static_cast<RegisterIndex32>(m_register_index));

    bool has_displacement = false;
    switch (mod()) {
    case 0b01:
    case 0b10:
        has_displacement = true;
    }
    if (m_has_sib && m_sib_base == 5)
        has_displacement = true;

    ByteString base;
    switch (m_rm) {
    case 5:
        if (mod() == 0)
            base = ByteString::formatted("{:x}", m_displacement32);
        else
            base = "ebp";
        break;
    case 4:
        base = sib_to_byte_string(ProcessorMode::Protected);
        break;
    default:
        base = register_name(RegisterIndex32(m_rm));
    }

    if (!has_displacement)
        return base;

    return ByteString::formatted("{}{:+#x}", base, (i32)m_displacement32);
}

static ByteString relative_address(u32 origin, bool x32, i8 imm)
{
    if (x32)
        return ByteString::formatted("{:x}", origin + imm);
    u16 w = origin & 0xffff;
    return ByteString::formatted("{:x}", w + imm);
}

static ByteString relative_address(u32 origin, bool x32, i32 imm)
{
    if (x32)
        return ByteString::formatted("{:x}", origin + imm);
    u16 w = origin & 0xffff;
    i16 si = imm;
    return ByteString::formatted("{:x}", w + si);
}

ByteString Instruction::to_byte_string(u32 origin, Optional<SymbolProvider const&> symbol_provider) const
{
    StringBuilder builder;
    if (has_segment_prefix())
        builder.appendff("{}: ", register_name(segment_prefix().value()));
    if (has_address_size_override_prefix()) {
        switch (m_address_size) {
        case AddressSize::Size16:
            builder.append("a16"sv);
            break;
        case AddressSize::Size32:
            builder.append("a32"sv);
            break;
        case AddressSize::Size64:
            builder.append("a64"sv);
            break;
        }
    }
    if (has_operand_size_override_prefix()) {
        switch (m_operand_size) {
        case OperandSize::Size16:
            builder.append("o16"sv);
            break;
        case OperandSize::Size32:
            builder.append("o32"sv);
            break;
        case OperandSize::Size64:
            builder.append("o64"sv);
            break;
        }
    }
    if (has_lock_prefix())
        builder.append("lock "sv);
    // Note: SSE instructions use these to toggle between packed and single data
    if (has_rep_prefix() && !(m_descriptor->format > __SSE && m_descriptor->format < __EndFormatsWithRMByte))
        builder.append(m_rep_prefix == Prefix::REPNZ ? "repnz "sv : "repz "sv);
    to_byte_string_internal(builder, origin, symbol_provider, true);
    return builder.to_byte_string();
}

void Instruction::to_byte_string_internal(StringBuilder& builder, u32 origin, Optional<SymbolProvider const&> symbol_provider, bool x32) const
{
    if (!m_descriptor) {
        builder.appendff("db {:02x}", m_op);
        return;
    }

    ByteString mnemonic = ByteString(m_descriptor->mnemonic).to_lowercase();

    auto append_mnemonic = [&] { builder.append(mnemonic); };

    auto append_mnemonic_space = [&] { builder.appendff("{: <6} ", mnemonic); };

    auto formatted_address = [&](FlatPtr origin, bool x32, auto offset) {
        builder.append(relative_address(origin, x32, offset));
        if (symbol_provider.has_value()) {
            u32 symbol_offset = 0;
            auto symbol = symbol_provider->symbolicate(origin + offset, &symbol_offset);
            builder.append(" <"sv);
            builder.append(symbol);
            if (symbol_offset)
                builder.appendff("+{:#x}", symbol_offset);
            builder.append('>');
        }
    };

    auto append_rm8 = [&] { builder.append(m_modrm.to_byte_string_o8(*this)); };
    auto append_rm16 = [&] { builder.append(m_modrm.to_byte_string_o16(*this)); };
    auto append_rm32 = [&] {
        if (m_operand_size == OperandSize::Size64)
            builder.append(m_modrm.to_byte_string_o64(*this));
        else
            builder.append(m_modrm.to_byte_string_o32(*this));
    };
    auto append_rm64 = [&] { builder.append(m_modrm.to_byte_string_o64(*this)); };
    auto append_fpu_reg = [&] { builder.append(m_modrm.to_byte_string_fpu_reg()); };
    auto append_fpu_mem = [&] { builder.append(m_modrm.to_byte_string_fpu_mem(*this)); };
    auto append_fpu_ax16 = [&] { builder.append(m_modrm.to_byte_string_fpu_ax16()); };
    auto append_fpu_rm16 = [&] { builder.append(m_modrm.to_byte_string_fpu16(*this)); };
    auto append_fpu_rm32 = [&] { builder.append(m_modrm.to_byte_string_fpu32(*this)); };
    auto append_fpu_rm64 = [&] { builder.append(m_modrm.to_byte_string_fpu64(*this)); };
    auto append_fpu_rm80 = [&] { builder.append(m_modrm.to_byte_string_fpu80(*this)); };
    auto append_imm8 = [&] { builder.appendff("{:#02x}", imm8()); };
    auto append_imm8_2 = [&] { builder.appendff("{:#02x}", imm8_2()); };
    auto append_imm16 = [&] { builder.appendff("{:#04x}", imm16()); };
    auto append_imm16_1 = [&] { builder.appendff("{:#04x}", imm16_1()); };
    auto append_imm16_2 = [&] { builder.appendff("{:#04x}", imm16_2()); };
    auto append_imm32 = [&] { builder.appendff("{:#08x}", imm32()); };
    auto append_imm32_2 = [&] { builder.appendff("{:#08x}", imm32_2()); };
    auto append_imm64 = [&] { builder.appendff("{:#016x}", imm64()); };
    auto append_immW = [&] {
        if (m_operand_size == OperandSize::Size64)
            append_imm64();
        else
            append_imm32();
    };
    auto append_reg8 = [&] { builder.append(reg8_name()); };
    auto append_reg16 = [&] { builder.append(reg16_name()); };
    auto append_reg32 = [&] {
        if (m_operand_size == OperandSize::Size64)
            builder.append(reg64_name());
        else
            builder.append(reg32_name());
    };
    auto append_seg = [&] { builder.append(register_name(segment_register())); };
    auto append_creg = [&] { builder.appendff("cr{}", register_index()); };
    auto append_dreg = [&] { builder.appendff("dr{}", register_index()); };
    auto append_relative_addr = [&] {
        switch (m_address_size) {
        case AddressSize::Size16:
            formatted_address(origin + 4, x32, i32(imm16()));
            break;
        case AddressSize::Size32:
            formatted_address(origin + 6, x32, i32(imm32()));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };
    auto append_relative_imm8 = [&] { formatted_address(origin + 2, x32, i8(imm8())); };
    auto append_relative_imm16 = [&] { formatted_address(origin + 3, x32, i16(imm16())); };
    auto append_relative_imm32 = [&] { formatted_address(origin + 5, x32, i32(imm32())); };

    auto append_mm = [&] { builder.appendff("mm{}", register_index()); };
    auto append_mmrm32 = [&] { builder.append(m_modrm.to_byte_string_mm(*this)); };
    auto append_mmrm64 = [&] { builder.append(m_modrm.to_byte_string_mm(*this)); };
    auto append_xmm = [&] { builder.appendff("xmm{}", register_index()); };
    auto append_xmmrm32 = [&] { builder.append(m_modrm.to_byte_string_xmm(*this)); };
    auto append_xmmrm64 = [&] { builder.append(m_modrm.to_byte_string_xmm(*this)); };
    auto append_xmmrm128 = [&] { builder.append(m_modrm.to_byte_string_xmm(*this)); };

    auto append_mm_or_xmm = [&] {
        if (has_operand_size_override_prefix())
            append_xmm();
        else
            append_mm();
    };

    auto append_mm_or_xmm_or_mem = [&] {
        // FIXME: The sizes here dont fully match what is meant, but it does
        //        not really matter...
        if (has_operand_size_override_prefix())
            append_xmmrm128();
        else
            append_mmrm64();
    };

    auto append = [&](auto content) { builder.append(content); };
    auto append_moff = [&] {
        builder.append('[');
        if (m_address_size == AddressSize::Size64) {
            append_imm64();
        } else if (m_address_size == AddressSize::Size32) {
            append_imm32();
        } else if (m_address_size == AddressSize::Size16) {
            append_imm16();
        } else {
            VERIFY_NOT_REACHED();
        }
        builder.append(']');
    };

    switch (m_descriptor->format) {
    case OP_RM8_imm8:
        append_mnemonic_space();
        append_rm8();
        append(',');
        append_imm8();
        break;
    case OP_RM16_imm8:
        append_mnemonic_space();
        append_rm16();
        append(',');
        append_imm8();
        break;
    case OP_RM32_imm8:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_imm8();
        break;
    case OP_reg16_RM16_imm8:
        append_mnemonic_space();
        append_reg16();
        append(',');
        append_rm16();
        append(',');
        append_imm8();
        break;
    case OP_reg32_RM32_imm8:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_rm32();
        append(',');
        append_imm8();
        break;
    case OP_AL_imm8:
        append_mnemonic_space();
        append("al,"sv);
        append_imm8();
        break;
    case OP_imm8:
        append_mnemonic_space();
        append_imm8();
        break;
    case OP_reg8_imm8:
        append_mnemonic_space();
        append_reg8();
        append(',');
        append_imm8();
        break;
    case OP_AX_imm8:
        append_mnemonic_space();
        append("ax,"sv);
        append_imm8();
        break;
    case OP_EAX_imm8:
        append_mnemonic_space();
        append("eax,"sv);
        append_imm8();
        break;
    case OP_imm8_AL:
        append_mnemonic_space();
        append_imm8();
        append(",al"sv);
        break;
    case OP_imm8_AX:
        append_mnemonic_space();
        append_imm8();
        append(",ax"sv);
        break;
    case OP_imm8_EAX:
        append_mnemonic_space();
        append_imm8();
        append(",eax"sv);
        break;
    case OP_AX_imm16:
        append_mnemonic_space();
        append("ax,"sv);
        append_imm16();
        break;
    case OP_imm16:
        append_mnemonic_space();
        append_imm16();
        break;
    case OP_reg16_imm16:
        append_mnemonic_space();
        append_reg16();
        append(',');
        append_imm16();
        break;
    case OP_reg16_RM16_imm16:
        append_mnemonic_space();
        append_reg16();
        append(',');
        append_rm16();
        append(',');
        append_imm16();
        break;
    case OP_reg32_RM32_imm32:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_rm32();
        append(',');
        append_imm32();
        break;
    case OP_imm32:
        append_mnemonic_space();
        append_imm32();
        break;
    case OP_EAX_imm32:
        append_mnemonic_space();
        append("eax,"sv);
        append_imm32();
        break;
    case OP_CS:
        append_mnemonic_space();
        append("cs"sv);
        break;
    case OP_DS:
        append_mnemonic_space();
        append("ds"sv);
        break;
    case OP_ES:
        append_mnemonic_space();
        append("es"sv);
        break;
    case OP_SS:
        append_mnemonic_space();
        append("ss"sv);
        break;
    case OP_FS:
        append_mnemonic_space();
        append("fs"sv);
        break;
    case OP_GS:
        append_mnemonic_space();
        append("gs"sv);
        break;
    case OP:
        append_mnemonic();
        break;
    case OP_reg32:
        append_mnemonic_space();
        append_reg32();
        break;
    case OP_imm16_imm8:
        append_mnemonic_space();
        append_imm16_1();
        append(',');
        append_imm8_2();
        break;
    case OP_moff8_AL:
        append_mnemonic_space();
        append_moff();
        append(",al"sv);
        break;
    case OP_moff16_AX:
        append_mnemonic_space();
        append_moff();
        append(",ax"sv);
        break;
    case OP_moff32_EAX:
        append_mnemonic_space();
        append_moff();
        append(",eax"sv);
        break;
    case OP_AL_moff8:
        append_mnemonic_space();
        append("al,"sv);
        append_moff();
        break;
    case OP_AX_moff16:
        append_mnemonic_space();
        append("ax,"sv);
        append_moff();
        break;
    case OP_EAX_moff32:
        append_mnemonic_space();
        append("eax,"sv);
        append_moff();
        break;
    case OP_imm16_imm16:
        append_mnemonic_space();
        append_imm16_1();
        append(":"sv);
        append_imm16_2();
        break;
    case OP_imm16_imm32:
        append_mnemonic_space();
        append_imm16_1();
        append(":"sv);
        append_imm32_2();
        break;
    case OP_reg32_imm32:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_imm32();
        break;
    case OP_regW_immW:
        append_mnemonic_space();
        append_reg32();
        append(", "sv);
        append_immW();
        break;
    case OP_RM8_1:
        append_mnemonic_space();
        append_rm8();
        append(",0x01"sv);
        break;
    case OP_RM16_1:
        append_mnemonic_space();
        append_rm16();
        append(",0x01"sv);
        break;
    case OP_RM32_1:
        append_mnemonic_space();
        append_rm32();
        append(",0x01"sv);
        break;
    case OP_RM8_CL:
        append_mnemonic_space();
        append_rm8();
        append(",cl"sv);
        break;
    case OP_RM16_CL:
        append_mnemonic_space();
        append_rm16();
        append(",cl"sv);
        break;
    case OP_RM32_CL:
        append_mnemonic_space();
        append_rm32();
        append(",cl"sv);
        break;
    case OP_reg16:
        append_mnemonic_space();
        append_reg16();
        break;
    case OP_AX_reg16:
        append_mnemonic_space();
        append("ax,"sv);
        append_reg16();
        break;
    case OP_EAX_reg32:
        append_mnemonic_space();
        append("eax,"sv);
        append_reg32();
        break;
    case OP_3:
        append_mnemonic_space();
        append("0x03"sv);
        break;
    case OP_AL_DX:
        append_mnemonic_space();
        append("al,dx"sv);
        break;
    case OP_AX_DX:
        append_mnemonic_space();
        append("ax,dx"sv);
        break;
    case OP_EAX_DX:
        append_mnemonic_space();
        append("eax,dx"sv);
        break;
    case OP_DX_AL:
        append_mnemonic_space();
        append("dx,al"sv);
        break;
    case OP_DX_AX:
        append_mnemonic_space();
        append("dx,ax"sv);
        break;
    case OP_DX_EAX:
        append_mnemonic_space();
        append("dx,eax"sv);
        break;
    case OP_reg8_CL:
        append_mnemonic_space();
        append_reg8();
        append(",cl"sv);
        break;
    case OP_RM8:
        append_mnemonic_space();
        append_rm8();
        break;
    case OP_RM16:
        append_mnemonic_space();
        append_rm16();
        break;
    case OP_RM32:
        append_mnemonic_space();
        append_rm32();
        break;
    case OP_FPU:
        append_mnemonic_space();
        break;
    case OP_FPU_reg:
        append_mnemonic_space();
        append_fpu_reg();
        break;
    case OP_FPU_mem:
        append_mnemonic_space();
        append_fpu_mem();
        break;
    case OP_FPU_AX16:
        append_mnemonic_space();
        append_fpu_ax16();
        break;
    case OP_FPU_RM16:
        append_mnemonic_space();
        append_fpu_rm16();
        break;
    case OP_FPU_RM32:
        append_mnemonic_space();
        append_fpu_rm32();
        break;
    case OP_FPU_RM64:
        append_mnemonic_space();
        append_fpu_rm64();
        break;
    case OP_FPU_M80:
        append_mnemonic_space();
        append_fpu_rm80();
        break;
    case OP_RM8_reg8:
        append_mnemonic_space();
        append_rm8();
        append(',');
        append_reg8();
        break;
    case OP_RM16_reg16:
        append_mnemonic_space();
        append_rm16();
        append(',');
        append_reg16();
        break;
    case OP_RM32_reg32:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_reg32();
        break;
    case OP_reg8_RM8:
        append_mnemonic_space();
        append_reg8();
        append(',');
        append_rm8();
        break;
    case OP_reg16_RM16:
        append_mnemonic_space();
        append_reg16();
        append(',');
        append_rm16();
        break;
    case OP_reg32_RM32:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_rm32();
        break;
    case OP_reg32_RM16:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_rm16();
        break;
    case OP_reg16_RM8:
        append_mnemonic_space();
        append_reg16();
        append(',');
        append_rm8();
        break;
    case OP_reg32_RM8:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_rm8();
        break;
    case OP_RM16_imm16:
        append_mnemonic_space();
        append_rm16();
        append(',');
        append_imm16();
        break;
    case OP_RM32_imm32:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_imm32();
        break;
    case OP_RM16_seg:
        append_mnemonic_space();
        append_rm16();
        append(',');
        append_seg();
        break;
    case OP_RM32_seg:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_seg();
        break;
    case OP_seg_RM16:
        append_mnemonic_space();
        append_seg();
        append(',');
        append_rm16();
        break;
    case OP_seg_RM32:
        append_mnemonic_space();
        append_seg();
        append(',');
        append_rm32();
        break;
    case OP_reg16_mem16:
        append_mnemonic_space();
        append_reg16();
        append(',');
        append_rm16();
        break;
    case OP_reg32_mem32:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_rm32();
        break;
    case OP_FAR_mem16:
        append_mnemonic_space();
        append("far "sv);
        append_rm16();
        break;
    case OP_FAR_mem32:
        append_mnemonic_space();
        append("far "sv);
        append_rm32();
        break;
    case OP_reg32_CR:
        append_mnemonic_space();
        builder.append(register_name(static_cast<RegisterIndex32>(modrm().rm())));
        append(',');
        append_creg();
        break;
    case OP_CR_reg32:
        append_mnemonic_space();
        append_creg();
        append(',');
        builder.append(register_name(static_cast<RegisterIndex32>(modrm().rm())));
        break;
    case OP_reg32_DR:
        append_mnemonic_space();
        builder.append(register_name(static_cast<RegisterIndex32>(modrm().rm())));
        append(',');
        append_dreg();
        break;
    case OP_DR_reg32:
        append_mnemonic_space();
        append_dreg();
        append(',');
        builder.append(register_name(static_cast<RegisterIndex32>(modrm().rm())));
        break;
    case OP_short_imm8:
        append_mnemonic_space();
        append("short "sv);
        append_relative_imm8();
        break;
    case OP_relimm16:
        append_mnemonic_space();
        append_relative_imm16();
        break;
    case OP_relimm32:
        append_mnemonic_space();
        append_relative_imm32();
        break;
    case OP_NEAR_imm:
        append_mnemonic_space();
        append("near "sv);
        append_relative_addr();
        break;
    case OP_RM16_reg16_imm8:
        append_mnemonic_space();
        append_rm16();
        append(',');
        append_reg16();
        append(',');
        append_imm8();
        break;
    case OP_RM32_reg32_imm8:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_reg32();
        append(',');
        append_imm8();
        break;
    case OP_RM16_reg16_CL:
        append_mnemonic_space();
        append_rm16();
        append(',');
        append_reg16();
        append(", cl"sv);
        break;
    case OP_RM32_reg32_CL:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_reg32();
        append(",cl"sv);
        break;
    case OP_reg:
        append_mnemonic_space();
        if (m_operand_size == OperandSize::Size32)
            append_reg32();
        else
            append_reg16();
        break;
    case OP_m64:
        append_mnemonic_space();
        append_rm64();
        break;
    case OP_mm1_imm8:
        append_mnemonic_space();
        append_mm_or_xmm();
        append(',');
        append_imm8();
        break;
    case OP_mm1_mm2m32:
        append_mnemonic_space();
        append_mm_or_xmm();
        append(',');
        append_mm_or_xmm_or_mem();
        break;
    case OP_mm1_rm32:
        append_mnemonic_space();
        append_mm_or_xmm();
        append(',');
        append_rm32();
        break;
    case OP_rm32_mm2:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_mm_or_xmm();
        break;
    case OP_mm1_mm2m64:
        append_mnemonic_space();
        append_mm_or_xmm();
        append(',');
        append_mm_or_xmm_or_mem();
        break;
    case OP_mm1m64_mm2:
        append_mnemonic_space();
        append_mm_or_xmm_or_mem();
        append(',');
        append_mm_or_xmm();
        break;
    case OP_mm1_mm2m64_imm8:
        append_mnemonic_space();
        append_mm_or_xmm();
        append(',');
        append_mm_or_xmm_or_mem();
        append(',');
        append_imm8();
        break;
    case OP_reg_mm1:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_mm_or_xmm();
        break;
    case OP_reg_mm1_imm8:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_mm_or_xmm_or_mem();
        append(',');
        append_imm8();
        break;
    case OP_mm1_r32m16_imm8:
        append_mnemonic_space();
        append_mm_or_xmm();
        append_rm32(); // FIXME: r32m16
        append(',');
        append_imm8();
        break;
    case __SSE:
        break;
    case OP_xmm_mm:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_mmrm32(); // FIXME: No Memory
        break;
    case OP_mm1_xmm2m128:
    case OP_mm_xmm:
        append_mnemonic_space();
        append_mm();
        append(',');
        append_xmmrm32(); // FIXME: No Memory
        break;
    case OP_xmm1_imm8:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_imm8();
        break;
    case OP_xmm1_xmm2m32:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_xmmrm32();
        break;
    case OP_xmm1_xmm2m64:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_xmmrm64();
        break;
    case OP_xmm1_xmm2m128:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_xmmrm128();
        break;
    case OP_xmm1_xmm2m32_imm8:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_xmmrm32();
        append(',');
        append_imm8();
        break;
    case OP_xmm1_xmm2m128_imm8:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_xmmrm32();
        append(',');
        append_imm8();
        break;
    case OP_xmm1m32_xmm2:
        append_mnemonic_space();
        append_xmmrm32();
        append(',');
        append_xmm();
        break;
    case OP_xmm1m64_xmm2:
        append_mnemonic_space();
        append_xmmrm64();
        append(',');
        append_xmm();
        break;
    case OP_xmm1m128_xmm2:
        append_mnemonic_space();
        append_xmmrm128();
        append(',');
        append_xmm();
        break;
    case OP_reg_xmm1:
    case OP_r32_xmm2m64:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_xmmrm128(); // second entry in the rm byte
        break;
    case OP_rm32_xmm2:
        append_mnemonic_space();
        append_rm32();
        append(',');
        append_xmm();
        break;
    case OP_reg_xmm1_imm8:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_xmmrm128(); // second entry in the rm byte
        append(',');
        append_imm8();
        break;
    case OP_xmm1_rm32:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_rm32(); // second entry in the rm byte
        break;
    case OP_xmm1_m64:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_rm64(); // second entry in the rm byte
        break;

    case OP_m64_xmm2:
        append_mnemonic_space();
        append_rm64(); // second entry in the rm byte
        append(',');
        append_xmm();
        break;
    case OP_rm8_xmm2m32:
        append_mnemonic_space();
        append_rm8();
        append(',');
        append_xmmrm32();
        break;
    case OP_xmm1_mm2m64:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_mmrm64();
        break;
    case OP_mm1m64_xmm2:
        append_mnemonic_space();
        append_mmrm64();
        append(',');
        append_xmm();
        break;
    case OP_mm1_xmm2m64:
        append_mnemonic_space();
        append_mm();
        append(',');
        append_xmmrm64();
        break;
    case OP_r32_xmm2m32:
        append_mnemonic_space();
        append_reg32();
        append(',');
        append_xmmrm32();
        break;
    case OP_xmm1_r32m16_imm8:
        append_mnemonic_space();
        append_xmm();
        append(',');
        append_rm32(); // FIXME: r32m16
        append(',');
        append_imm8();
        break;
    case InstructionPrefix:
        append_mnemonic();
        break;
    case InvalidFormat:
    case MultibyteWithSlash:
    case __BeginFormatsWithRMByte:
    case __EndFormatsWithRMByte:
        builder.append(ByteString::formatted("(!{})", mnemonic));
        break;
    }
}

ByteString Instruction::mnemonic() const
{
    if (!m_descriptor) {
        VERIFY_NOT_REACHED();
    }
    return m_descriptor->mnemonic;
}

StringView register_name(SegmentRegister index)
{
    static constexpr StringView names[] = { "es"sv, "cs"sv, "ss"sv, "ds"sv, "fs"sv, "gs"sv, "segr6"sv, "segr7"sv };
    return names[(int)index & 7];
}

StringView register_name(RegisterIndex8 register_index)
{
    static constexpr StringView names[] = { "al"sv, "cl"sv, "dl"sv, "bl"sv, "ah"sv, "ch"sv, "dh"sv, "bh"sv, "r8b"sv, "r9b"sv, "r10b"sv, "r11b"sv, "r12b"sv, "r13b"sv, "r14b"sv, "r15b"sv };
    return names[register_index & 15];
}

StringView register_name(RegisterIndex16 register_index)
{
    static constexpr StringView names[] = { "ax"sv, "cx"sv, "dx"sv, "bx"sv, "sp"sv, "bp"sv, "si"sv, "di"sv, "r8w"sv, "r9w"sv, "r10w"sv, "r11w"sv, "r12w"sv, "r13w"sv, "r14w"sv, "r15w"sv };
    return names[register_index & 15];
}

StringView register_name(RegisterIndex32 register_index)
{
    static constexpr StringView names[] = { "eax"sv, "ecx"sv, "edx"sv, "ebx"sv, "esp"sv, "ebp"sv, "esi"sv, "edi"sv, "r8d"sv, "r9d"sv, "r10d"sv, "r11d"sv, "r12d"sv, "r13d"sv, "r14d"sv, "r15d"sv };
    return names[register_index & 15];
}

StringView register_name(RegisterIndex64 register_index)
{
    static constexpr StringView names[] = { "rax"sv, "rcx"sv, "rdx"sv, "rbx"sv, "rsp"sv, "rbp"sv, "rsi"sv, "rdi"sv, "r8"sv, "r9"sv, "r10"sv, "r11"sv, "r12"sv, "r13"sv, "r14"sv, "r15"sv };
    return names[register_index & 15];
}

StringView register_name(FpuRegisterIndex register_index)
{
    static constexpr StringView names[] = { "st0"sv, "st1"sv, "st2"sv, "st3"sv, "st4"sv, "st5"sv, "st6"sv, "st7"sv };
    return names[register_index & 7];
}

StringView register_name(MMXRegisterIndex register_index)
{
    static constexpr StringView names[] = { "mm0"sv, "mm1"sv, "mm2"sv, "mm3"sv, "mm4"sv, "mm5"sv, "mm6"sv, "mm7"sv };
    return names[register_index & 7];
}

StringView register_name(XMMRegisterIndex register_index)
{
    static constexpr StringView names[] = { "xmm0"sv, "xmm1"sv, "xmm2"sv, "xmm3"sv, "xmm4"sv, "xmm5"sv, "xmm6"sv, "xmm7"sv, "xmm8"sv, "xmm9"sv, "xmm10"sv, "xmm11"sv, "xmm12"sv, "xmm13"sv, "xmm14"sv, "xmm15"sv };
    return names[register_index & 15];
}

}
