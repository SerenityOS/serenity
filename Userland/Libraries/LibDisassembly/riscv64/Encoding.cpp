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

static u8 extract_funct3(u32 instruction)
{
    return static_cast<u8>((instruction >> 12) & 0b111);
}

static u8 extract_funct7(u32 instruction)
{
    return static_cast<u8>((instruction >> 25) & 0b1111111);
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

}
