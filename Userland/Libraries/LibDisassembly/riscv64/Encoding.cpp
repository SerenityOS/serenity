/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Encoding.h"
#include <AK/Assertions.h>
#include <AK/BitCast.h>
#include <AK/IntegralMath.h>

namespace Disassembly::RISCV64 {

// rd is always in bit positions [11:7].
static Register extract_rd(u32 instruction)
{
    auto register_bits = (instruction >> 7) & 0b11111;
    return static_cast<Register>(register_bits);
}

// rs1 is always in bit positions [19:15].
static Register extract_rs1(u32 instruction)
{
    auto register_bits = (instruction >> 15) & 0b11111;
    return static_cast<Register>(register_bits);
}

// rs2 is always in bit positions [24:20].
static Register extract_rs2(u32 instruction)
{
    auto register_bits = (instruction >> 20) & 0b11111;
    return static_cast<Register>(register_bits);
}

static Register extract_compressed_rs2(u16 instruction)
{
    auto register_bits = (instruction >> 2) & 0b11111;
    return static_cast<Register>(register_bits);
}

// The following register formats from C only use 3 bits and offset the register number by 8:

// Used as rd/rs1 in CA/CB.
[[maybe_unused]] static Register extract_compressed_short_rs1(u16 instruction)
{
    auto register_bits = (instruction >> 7) & 0b111;
    return static_cast<Register>(register_bits + 8);
}

// Used as rs2 in CS and CA, but as rd in CL and CIW.
[[maybe_unused]] static Register extract_compressed_short_rs2(u16 instruction)
{
    auto register_bits = (instruction >> 2) & 0b111;
    return static_cast<Register>(register_bits + 8);
}

static u8 extract_funct3(u32 instruction)
{
    return static_cast<u8>((instruction >> 12) & 0b111);
}

static u8 extract_funct7(u32 instruction)
{
    return static_cast<u8>((instruction >> 25) & 0b1111111);
}

// Only used for CB format to detect C.ANDI.
[[maybe_unused]] static u8 extract_compressed_funct2_cb(u16 instruction)
{
    return static_cast<u8>((instruction >> 10) & 0b11);
}

[[maybe_unused]] static u8 extract_compressed_funct2(u16 instruction)
{
    return static_cast<u8>((instruction >> 5) & 0b11);
}

[[maybe_unused]] static u8 extract_compressed_funct4(u16 instruction)
{
    return static_cast<u8>((instruction >> 12) & 0b1111);
}

[[maybe_unused]] static u8 extract_compressed_funct6(u16 instruction)
{
    return static_cast<u8>((instruction >> 10) & 0b111111);
}

RawUType RawUType::parse(u32 instruction)
{
    auto destination_register = extract_rd(instruction);
    i32 immediate = bit_cast<i32>(instruction & 0xfffff000);
    u8 opcode = instruction & 0b1111111;
    return RawUType {
        .imm = immediate,
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawJType RawJType::parse(u32 instruction)
{
    auto destination_register = extract_rd(instruction);

    // Figure 2.3; J-Type has a highly scrambled immediate that's hardware-friendly but not software-friendly.
    u32 sign_bit = instruction >> 31;
    u32 imm_10_1 = (instruction >> 21) & 0b11'1111'1111;
    u32 imm_11 = (instruction >> 20) & 1;
    u32 imm_19_12 = (instruction >> 12) & 0b1111'1111;
    u32 raw_immediate = (imm_10_1 << 1) | (imm_11 << 11) | (imm_19_12 << 12) | (sign_bit << 20);
    i32 immediate = AK::sign_extend(raw_immediate, 20);

    u8 opcode = instruction & 0b1111111;
    return RawJType {
        .imm = immediate,
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawIType RawIType::parse(u32 instruction)
{
    auto destination_register = extract_rd(instruction);
    auto source_register = extract_rs1(instruction);
    auto funct3 = extract_funct3(instruction);
    // Figure 2.4
    u32 raw_immediate = (instruction >> 20) & 0xfff;
    i32 immediate = AK::sign_extend(raw_immediate, 12);

    u8 opcode = instruction & 0b1111111;
    return RawIType {
        .imm = immediate,
        .rs1 = source_register,
        .funct3 = funct3,
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawSType RawSType::parse(u32 instruction)
{
    auto source_register_1 = extract_rs1(instruction);
    auto source_register_2 = extract_rs2(instruction);
    auto funct3 = extract_funct3(instruction);
    // Figure 2.3
    u32 imm_11_5 = (instruction >> 25) & 0b1111111;
    u32 imm_4_0 = (instruction >> 7) & 0b11111;
    u32 raw_immediate = imm_4_0 | (imm_11_5 << 5);
    i32 immediate = AK::sign_extend(raw_immediate, 12);

    u8 opcode = instruction & 0b1111111;
    return RawSType {
        .imm = immediate,
        .rs2 = source_register_2,
        .rs1 = source_register_1,
        .funct3 = funct3,
        .opcode = opcode,
    };
}

RawBType RawBType::parse(u32 instruction)
{
    auto source_register_1 = extract_rs1(instruction);
    auto source_register_2 = extract_rs2(instruction);
    auto funct3 = extract_funct3(instruction);
    // Figure 2.3
    u32 sign_bit = instruction >> 31;
    u32 imm_10_5 = (instruction >> 25) & 0b111111;
    u32 imm_4_1 = (instruction >> 8) & 0b1111;
    u32 imm_11 = (instruction >> 7) & 1;
    u32 raw_immediate = (imm_4_1 << 1) | (imm_10_5 << 5) | (imm_11 << 11) | (sign_bit << 12);
    i32 immediate = AK::sign_extend(raw_immediate, 13);

    u8 opcode = instruction & 0b1111111;
    return RawBType {
        .imm = immediate,
        .rs2 = source_register_2,
        .rs1 = source_register_1,
        .funct3 = funct3,
        .opcode = opcode,
    };
}

RawRType RawRType::parse(u32 instruction)
{
    auto destination_register = extract_rd(instruction);
    auto source_register_1 = extract_rs1(instruction);
    auto source_register_2 = extract_rs2(instruction);
    auto funct3 = extract_funct3(instruction);
    auto funct7 = extract_funct7(instruction);

    u8 opcode = instruction & 0b1111111;
    return RawRType {
        .funct7 = funct7,
        .rs2 = source_register_2,
        .rs1 = source_register_1,
        .funct3 = funct3,
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawR4Type RawR4Type::parse(u32 instruction)
{
    auto destination_register = extract_rd(instruction);
    auto source_register_1 = extract_rs1(instruction);
    auto source_register_2 = extract_rs2(instruction);
    auto funct3 = extract_funct3(instruction);
    auto fmt = static_cast<u8>(extract_funct7(instruction) & 0b11);
    auto source_register_3 = static_cast<Register>(extract_funct7(instruction) >> 2);

    u8 opcode = instruction & 0b1111111;
    return RawR4Type {
        .rs3 = source_register_3,
        .fmt = fmt,
        .rs2 = source_register_2,
        .rs1 = source_register_1,
        .funct3 = funct3,
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawCRType RawCRType::parse(u16 instruction)
{
    auto destination_register = extract_rd(instruction);
    auto source_register_2 = extract_compressed_rs2(instruction);
    auto funct4 = extract_compressed_funct4(instruction);

    u8 opcode = instruction & 0b11;
    return RawCRType {
        .funct4 = funct4,
        .rd_or_rs1 = destination_register,
        .rs2 = source_register_2,
        .opcode = opcode,
    };
}

RawCAType RawCAType::parse(u16 instruction)
{
    auto destination_register = extract_compressed_short_rs1(instruction);
    auto source_register_2 = extract_compressed_short_rs2(instruction);
    auto funct2 = extract_compressed_funct2(instruction);
    auto funct6 = extract_compressed_funct6(instruction);

    u8 opcode = instruction & 0b11;
    return RawCAType {
        .funct6 = funct6,
        .rd_or_rs1 = destination_register,
        .funct2 = funct2,
        .rs2 = source_register_2,
        .opcode = opcode,
    };
}

RawCIType RawCIType::parse(u16 instruction)
{
    auto destination_register = extract_rd(instruction);
    auto funct3 = extract_compressed_funct3(instruction);

    // Almost every single instruction using CI has its own immediate encoding.
    // Therefore, we need to decode the opcode to proceed.
    auto decoded_opcode = extract_compressed_opcode(instruction);
    u32 imm_5 = (instruction >> 12) & 1;
    u32 raw_immediate = imm_5 << 5;
    i32 immediate = 0;
    switch (decoded_opcode) {
    // C.LI, C.SLLI
    case CompressedOpcode::LI:
    case CompressedOpcode::SLLI: {
        u32 imm_4_0 = (instruction >> 2) & 0b11111;
        immediate = static_cast<i32>(raw_immediate | imm_4_0);
        break;
    }
    // C.ADDIW (JAL is RV32C-only), C.ADDI, C.NOP sign-extend the immediate.
    case CompressedOpcode::ADDI:
    case CompressedOpcode::JAL_ADDIW: {
        u32 imm_4_0 = (instruction >> 2) & 0b11111;
        raw_immediate |= imm_4_0;
        immediate = AK::sign_extend(raw_immediate, 6);
        break;
    }
    // C.LUI, C.ADDI16SP
    case CompressedOpcode::LUI_ADDI16SP: {
        // ADDI16SP is only valid for the stack pointer (register index 2) and uses the most scrambling shenanigans of all.
        if (destination_register == 2) {
            u32 imm_5 = (instruction >> 2) & 1;
            u32 imm_8_7 = (instruction >> 3) & 0b11;
            u32 imm_6 = (instruction >> 5) & 1;
            u32 imm_4 = (instruction >> 6) & 1;
            u32 imm_9 = (instruction >> 12) & 1;
            raw_immediate = (imm_4 << 4) | (imm_5 << 5) | (imm_6 << 6) | (imm_8_7 << 7) | (imm_9 << 9);
            immediate = AK::sign_extend(raw_immediate, 6);
        } else {
            u32 imm_16_12 = (instruction >> 2) & 0b11111;
            raw_immediate |= imm_16_12;
            immediate = static_cast<i32>(raw_immediate << 12);
        }
        break;
    }
    // C.LWSP, C.FLWSP
    case CompressedOpcode::LWSP: {
        u32 imm_7_6 = (instruction >> 2) & 0b11;
        u32 imm_4_2 = (instruction >> 4) & 0b111;
        raw_immediate |= (imm_4_2 << 2) | (imm_7_6 << 6);
        immediate = static_cast<i32>(raw_immediate);
        break;
    }
    // C.LDSP (C.FLWSP is RV32C-only), C.FLDSP (LQSP is RV128C-only)
    case CompressedOpcode::FLWSP_LDSP:
    case CompressedOpcode::FLDSP_LQSP: {
        u32 imm_8_6 = (instruction >> 2) & 0b111;
        u32 imm_4_3 = (instruction >> 5) & 0b11;
        raw_immediate |= (imm_4_3 << 3) | (imm_8_6 << 6);
        immediate = static_cast<i32>(raw_immediate);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    u8 opcode = instruction & 0b11;
    return RawCIType {
        .funct3 = funct3,
        .imm = immediate,
        .rd_or_rs1 = destination_register,
        .opcode = opcode,
    };
}

RawCSType RawCSType::parse(u16 instruction)
{
    auto source_register_1 = extract_compressed_short_rs1(instruction);
    auto source_register_2 = extract_compressed_short_rs2(instruction);
    auto funct3 = extract_compressed_funct3(instruction);

    // Almost every single instruction using CS has its own immediate encoding.
    // Therefore, we need to decode the opcode to proceed.
    auto decoded_opcode = extract_compressed_opcode(instruction);
    u32 imm_5_3 = (instruction >> 10) & 0b111;
    u32 raw_immediate;
    switch (decoded_opcode) {
    // C.SW
    case CompressedOpcode::SW: {
        u32 imm_6 = (instruction >> 5) & 1;
        u32 imm_2 = (instruction >> 6) & 1;
        raw_immediate = (imm_2 << 2) | (imm_5_3 << 3) | (imm_6 << 6);
        break;
    }
    // C.SD (FSW is RV32C-only), C.FSD (SQ is RV128C-only)
    case CompressedOpcode::FSW_SD:
    case CompressedOpcode::FSD_SQ: {
        u32 imm_7_6 = (instruction >> 5) & 0b11;
        raw_immediate = (imm_5_3 << 3) | (imm_7_6 << 6);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    u8 opcode = instruction & 0b11;
    return RawCSType {
        .funct3 = funct3,
        .imm = static_cast<i32>(raw_immediate),
        .rs1 = source_register_1,
        .rs2 = source_register_2,
        .opcode = opcode,
    };
}

RawCSSType RawCSSType::parse(u16 instruction)
{
    auto source_register_2 = extract_compressed_rs2(instruction);
    auto funct3 = extract_compressed_funct3(instruction);

    // Almost every single instruction using CSS has its own immediate encoding.
    // Therefore, we need to decode the opcode to proceed.
    auto decoded_opcode = extract_compressed_opcode(instruction);
    u32 raw_immediate;
    switch (decoded_opcode) {
    // C.SWSP
    case CompressedOpcode::SWSP: {
        u32 imm_7_6 = (instruction >> 7) & 0b11;
        u32 imm_5_2 = (instruction >> 9) & 0b1111;
        raw_immediate = (imm_5_2 << 2) | (imm_7_6 << 6);
        break;
    }
    // C.SDSP (FSWSP is RV32C-only), C.FSDSP (SQSP is RV128C-only)
    case CompressedOpcode::FSWSP_SDSP:
    case CompressedOpcode::FSDSP_SQSP: {
        u32 imm_8_6 = (instruction >> 7) & 0b111;
        u32 imm_5_3 = (instruction >> 10) & 0b111;
        raw_immediate = (imm_5_3 << 3) | (imm_8_6 << 6);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    u8 opcode = instruction & 0b11;
    return RawCSSType {
        .funct3 = funct3,
        .imm = static_cast<i32>(raw_immediate),
        .rs2 = source_register_2,
        .opcode = opcode,
    };
}

RawCLType RawCLType::parse(u16 instruction)
{
    auto source_register_1 = extract_compressed_short_rs1(instruction);
    auto destination_register = extract_compressed_short_rs2(instruction);
    auto funct3 = extract_compressed_funct3(instruction);

    // Almost every single instruction using CL has its own immediate encoding.
    // Therefore, we need to decode the opcode to proceed.
    auto decoded_opcode = extract_compressed_opcode(instruction);
    u32 raw_immediate;
    switch (decoded_opcode) {
    // C.LD (FLW is RV32C-only), C.FLD (LQ is RV128C-only)
    case CompressedOpcode::FLW_LD:
    case CompressedOpcode::FLD_LQ: {
        u32 imm_5_3 = (instruction >> 10) & 0b111;
        u32 imm_7_6 = (instruction >> 5) & 0b11;
        raw_immediate = (imm_5_3 << 3) | (imm_7_6 << 6);
        break;
    }
    // C.LW
    case CompressedOpcode::LW: {
        u32 imm_5_3 = (instruction >> 10) & 0b111;
        u32 imm_6 = (instruction >> 5) & 1;
        u32 imm_2 = (instruction >> 6) & 1;
        raw_immediate = (imm_2 << 2) | (imm_5_3 << 3) | (imm_6 << 6);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    u8 opcode = instruction & 0b11;
    return RawCLType {
        .funct3 = funct3,
        .imm = static_cast<i32>(raw_immediate),
        .rs1 = source_register_1,
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawCIWType RawCIWType::parse(u16 instruction)
{
    auto destination_register = extract_compressed_short_rs2(instruction);
    auto funct3 = extract_compressed_funct3(instruction);

    u32 imm_3 = (instruction >> 5) & 1;
    u32 imm_2 = (instruction >> 6) & 1;
    u32 imm_9_6 = (instruction >> 7) & 0b1111;
    u32 imm_5_4 = (instruction >> 11) & 0b11;
    u32 raw_immediate = (imm_2 << 2) | (imm_3 << 3) | (imm_5_4 << 4) | (imm_9_6 << 6);

    u8 opcode = instruction & 0b11;
    return RawCIWType {
        .funct3 = funct3,
        .imm = static_cast<i32>(raw_immediate),
        .rd = destination_register,
        .opcode = opcode,
    };
}

RawCBType RawCBType::parse(u16 instruction)
{
    auto destination_register = extract_compressed_short_rs1(instruction);
    auto funct3 = extract_compressed_funct3(instruction);
    auto funct2 = extract_compressed_funct2_cb(instruction);

    i32 immediate;
    if (funct3 == 0b100) {
        switch (funct2) {
        // C.SRLI, C.SRAI
        case 0b00:
        case 0b01: {
            u32 imm_5 = (instruction >> 12) & 1;
            u32 imm_4_0 = (instruction >> 2) & 0b11111;
            u32 raw_immediate = (imm_5 << 5) | imm_4_0;
            immediate = static_cast<i32>(raw_immediate);
            break;
        }
        // C.ANDI
        case 0b10: {
            u32 imm_5 = (instruction >> 12) & 1;
            u32 imm_4_0 = (instruction >> 2) & 0b11111;
            u32 raw_immediate = (imm_5 << 5) | imm_4_0;
            immediate = AK::sign_extend(raw_immediate, 6);
            break;
        }
        }
    } else {
        // C.BEQZ, C.BNEZ
        u32 imm_8 = (instruction >> 12) & 1;
        u32 imm_4_3 = (instruction >> 10) & 0b11;
        u32 imm_7_6 = (instruction >> 5) & 0b11;
        u32 imm_2_1 = (instruction >> 3) & 0b11;
        u32 imm_5 = (instruction >> 2) & 1;
        u32 raw_immediate = (imm_2_1 << 1) | (imm_4_3 << 3) | (imm_5 << 5) | (imm_7_6 << 6) | (imm_8 << 8);
        immediate = AK::sign_extend(raw_immediate, 9);
    }

    u8 opcode = instruction & 0b11;
    return RawCBType {
        .funct3 = funct3,
        .offset = immediate,
        .funct2 = funct2,
        .rs1 = destination_register,
        .opcode = opcode,
    };
}

RawCJType RawCJType::parse(u16 instruction)
{
    auto funct3 = extract_compressed_funct3(instruction);

    u32 imm_5 = (instruction >> 2) & 1;
    u32 imm_3_1 = (instruction >> 3) & 0b111;
    u32 imm_7 = (instruction >> 6) & 1;
    u32 imm_6 = (instruction >> 7) & 1;
    u32 imm_10 = (instruction >> 8) & 1;
    u32 imm_9_8 = (instruction >> 9) & 0b11;
    u32 imm_4 = (instruction >> 11) & 1;
    u32 imm_11 = (instruction >> 12) & 1;
    u32 raw_immediate = (imm_3_1 << 1) | (imm_4 << 4) | (imm_5 << 5) | (imm_6 << 6) | (imm_7 << 7) | (imm_9_8 << 8) | (imm_10 << 10) | (imm_11 << 11);
    i32 immediate = AK::sign_extend(raw_immediate, 11);

    u8 opcode = instruction & 0b11;
    return RawCJType {
        .funct3 = funct3,
        .jump_target = immediate,
        .opcode = opcode,
    };
}

}
