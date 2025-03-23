/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDisassembly/riscv64/Registers.h>

namespace Disassembly::RISCV64 {

struct RawRType {
    u8 funct7;
    Register rs2;
    Register rs1;
    u8 funct3;
    Register rd;
    u8 opcode;

    static RawRType parse(u32 instruction);
};
struct RawIType {
    i32 imm;
    Register rs1;
    u8 funct3;
    Register rd;
    u8 opcode;

    static RawIType parse(u32 instruction);
};
struct RawSType {
    i32 imm;
    Register rs2;
    Register rs1;
    u8 funct3;
    u8 opcode;

    static RawSType parse(u32 instruction);
};
struct RawBType {
    i32 imm;
    Register rs2;
    Register rs1;
    u8 funct3;
    u8 opcode;

    static RawBType parse(u32 instruction);
};
struct RawUType {
    i32 imm;
    Register rd;
    u8 opcode;

    static RawUType parse(u32 instruction);
};
struct RawJType {
    i32 imm;
    Register rd;
    u8 opcode;

    static RawJType parse(u32 instruction);
};
struct RawR4Type {
    Register rs3;
    u8 fmt;
    Register rs2;
    Register rs1;
    u8 funct3;
    Register rd;
    u8 opcode;

    static RawR4Type parse(u32 instruction);
};

// Table 24.1
enum class MajorOpcode : u8 {
    LOAD = 0b00000'11,
    STORE = 0b01000'11,
    MADD = 0b10000'11,
    BRANCH = 0b11000'11,
    LOAD_FP = 0b00001'11,
    STORE_FP = 0b01001'11,
    MSUB = 0b10001'11,
    JALR = 0b11001'11,
    custom_0 = 0b00010'11,
    custom_1 = 0b01010'11,
    NMSUB = 0b10010'11,
    reserved_0 = 0b11010'11,
    MISC_MEM = 0b00011'11,
    AMO = 0b01011'11,
    NMADD = 0b10011'11,
    JAL = 0b11011'11,
    OP_IMM = 0b00100'11,
    OP = 0b01100'11,
    OP_FP = 0b10100'11,
    SYSTEM = 0b11100'11,
    AUIPC = 0b00101'11,
    LUI = 0b01101'11,
    reserved_1 = 0b10101'11,
    reserved_2 = 0b11101'11,
    OP_IMM_32 = 0b00110'11,
    OP_32 = 0b01110'11,
    custom_2_rv128 = 0b10110'11,
    custom_3_rv128 = 0b11110'11,
};

// Table 16.4
// Stored in the funct3 field and lowest 2 bits of compressed instructions.
// Note that we always decode the RV64C version, but the names are as per specification and refer to all three variants.
enum class CompressedOpcode : u8 {
    ADDI4SPN = 0b000'00,
    ADDI = 0b000'01,
    SLLI = 0b000'10,
    FLD_LQ = 0b001'00,
    JAL_ADDIW = 0b001'01,
    FLDSP_LQSP = 0b001'10,
    LW = 0b010'00,
    LI = 0b010'01,
    LWSP = 0b010'10,
    FLW_LD = 0b011'00,
    LUI_ADDI16SP = 0b011'01,
    FLWSP_LDSP = 0b011'10,
    reserved = 0b100'00,
    MISC_ALU = 0b100'01,
    JALR_MV_ADD = 0b100'10,
    FSD_SQ = 0b101'00,
    J = 0b101'01,
    FSDSP_SQSP = 0b101'10,
    SW = 0b110'00,
    BEQZ = 0b110'01,
    SWSP = 0b110'10,
    FSW_SD = 0b111'00,
    BNEZ = 0b111'01,
    FSWSP_SDSP = 0b111'10,
};

// Table 11.1
enum class RoundingMode : u8 {
    RNE = 0b000,
    RTZ = 0b001,
    RDN = 0b010,
    RUP = 0b011,
    RMM = 0b100,
    Invalid1 = 0b101,
    Invalid2 = 0b110,
    DYN = 0b111,
};

constexpr CompressedOpcode extract_compressed_opcode(u16 instruction)
{
    u8 raw_opcode = (instruction & 0b11) | ((instruction >> 11) & 0b11100);
    return static_cast<CompressedOpcode>(raw_opcode);
}

// 1.5 Base Instruction-Length Encoding, Figure 1.1
constexpr bool is_compressed_instruction(u16 halfword)
{
    return (halfword & 0b11) != 0b11;
}

}
