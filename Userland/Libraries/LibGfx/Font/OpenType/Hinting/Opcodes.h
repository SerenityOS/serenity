/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/StringView.h>

namespace OpenType::Hinting {

#define ENUMERATE_OPENTYPE_OPCODES                      \
    /* Pushing data onto the interpreter stack: */      \
    __ENUMERATE_OPENTYPE_OPCODES(NPUSHB, 0x40, 0x40)    \
    __ENUMERATE_OPENTYPE_OPCODES(NPUSHW, 0x41, 0x41)    \
    __ENUMERATE_OPENTYPE_OPCODES(PUSHB, 0xB0, 0xB7)     \
    __ENUMERATE_OPENTYPE_OPCODES(PUSHW, 0xB8, 0xBF)     \
    /* Managing the Storage Area */                     \
    __ENUMERATE_OPENTYPE_OPCODES(RS, 0x43, 0x43)        \
    __ENUMERATE_OPENTYPE_OPCODES(WS, 0x42, 0x42)        \
    /* Managing the Control Value Table */              \
    __ENUMERATE_OPENTYPE_OPCODES(WCVTP, 0x44, 0x44)     \
    __ENUMERATE_OPENTYPE_OPCODES(WCVTF, 0x70, 0x70)     \
    __ENUMERATE_OPENTYPE_OPCODES(RCVT, 0x45, 0x45)      \
    /* Managing the Graphics State */                   \
    __ENUMERATE_OPENTYPE_OPCODES(SVTCA, 0x00, 0x01)     \
    __ENUMERATE_OPENTYPE_OPCODES(SPVTCA, 0x02, 0x03)    \
    __ENUMERATE_OPENTYPE_OPCODES(SFVTCA, 0x04, 0x05)    \
    __ENUMERATE_OPENTYPE_OPCODES(SPVTL, 0x06, 0x07)     \
    __ENUMERATE_OPENTYPE_OPCODES(SFVTL, 0x08, 0x09)     \
    __ENUMERATE_OPENTYPE_OPCODES(SFVTPV, 0x0E, 0x0E)    \
    __ENUMERATE_OPENTYPE_OPCODES(SDPVTL, 0x86, 0x87)    \
    __ENUMERATE_OPENTYPE_OPCODES(SPVFS, 0x0A, 0x0A)     \
    __ENUMERATE_OPENTYPE_OPCODES(SFVFS, 0x0B, 0x0B)     \
    __ENUMERATE_OPENTYPE_OPCODES(GPV, 0x0C, 0x0C)       \
    __ENUMERATE_OPENTYPE_OPCODES(GFV, 0x0D, 0x0D)       \
    __ENUMERATE_OPENTYPE_OPCODES(SRP0, 0x10, 0x10)      \
    __ENUMERATE_OPENTYPE_OPCODES(SRP1, 0x11, 0x11)      \
    __ENUMERATE_OPENTYPE_OPCODES(SRP2, 0x12, 0x12)      \
    __ENUMERATE_OPENTYPE_OPCODES(SZP0, 0x13, 0x13)      \
    __ENUMERATE_OPENTYPE_OPCODES(SZP1, 0x14, 0x14)      \
    __ENUMERATE_OPENTYPE_OPCODES(SZP2, 0x15, 0x15)      \
    __ENUMERATE_OPENTYPE_OPCODES(SZPS, 0x16, 0x16)      \
    __ENUMERATE_OPENTYPE_OPCODES(RTHG, 0x19, 0x19)      \
    __ENUMERATE_OPENTYPE_OPCODES(RTG, 0x18, 0x18)       \
    __ENUMERATE_OPENTYPE_OPCODES(RTDG, 0x3D, 0x3D)      \
    __ENUMERATE_OPENTYPE_OPCODES(RDTG, 0x7D, 0x7D)      \
    __ENUMERATE_OPENTYPE_OPCODES(RUTG, 0x7C, 0x7C)      \
    __ENUMERATE_OPENTYPE_OPCODES(ROFF, 0x7A, 0x7A)      \
    __ENUMERATE_OPENTYPE_OPCODES(SROUND, 0x76, 0x76)    \
    __ENUMERATE_OPENTYPE_OPCODES(S45ROUND, 0x77, 0x77)  \
    __ENUMERATE_OPENTYPE_OPCODES(SLOOP, 0x17, 0x17)     \
    __ENUMERATE_OPENTYPE_OPCODES(SMD, 0x1A, 0x1A)       \
    __ENUMERATE_OPENTYPE_OPCODES(INSTCTRL, 0x8E, 0x8E)  \
    __ENUMERATE_OPENTYPE_OPCODES(SCANCTRL, 0x85, 0x85)  \
    __ENUMERATE_OPENTYPE_OPCODES(SCANTYPE, 0x8D, 0x8D)  \
    __ENUMERATE_OPENTYPE_OPCODES(SCVTCI, 0x1D, 0x1D)    \
    __ENUMERATE_OPENTYPE_OPCODES(SSWCI, 0x1E, 0x1E)     \
    __ENUMERATE_OPENTYPE_OPCODES(SSW, 0x1F, 0x1F)       \
    __ENUMERATE_OPENTYPE_OPCODES(FLIPON, 0x4D, 0x4D)    \
    __ENUMERATE_OPENTYPE_OPCODES(FLIPOFF, 0x4E, 0x4E)   \
    __ENUMERATE_OPENTYPE_OPCODES(SANGW, 0x7E, 0x7E)     \
    __ENUMERATE_OPENTYPE_OPCODES(SDB, 0x5E, 0x5E)       \
    __ENUMERATE_OPENTYPE_OPCODES(SDS, 0x5F, 0x5F)       \
    /* Reading and writing data */                      \
    __ENUMERATE_OPENTYPE_OPCODES(GC, 0x46, 0x47)        \
    __ENUMERATE_OPENTYPE_OPCODES(SCFS, 0x48, 0x48)      \
    __ENUMERATE_OPENTYPE_OPCODES(MD, 0x49, 0x4A)        \
    __ENUMERATE_OPENTYPE_OPCODES(MPPEM, 0x4B, 0x4B)     \
    __ENUMERATE_OPENTYPE_OPCODES(MPS, 0x4C, 0x4C)       \
    /* Managing outlines */                             \
    __ENUMERATE_OPENTYPE_OPCODES(FLIPPT, 0x80, 0x80)    \
    __ENUMERATE_OPENTYPE_OPCODES(FLIPRGON, 0x81, 0x81)  \
    __ENUMERATE_OPENTYPE_OPCODES(FLIPRGOFF, 0x82, 0x82) \
    __ENUMERATE_OPENTYPE_OPCODES(SHP, 0x32, 0x33)       \
    __ENUMERATE_OPENTYPE_OPCODES(SHC, 0x34, 0x35)       \
    __ENUMERATE_OPENTYPE_OPCODES(SHZ, 0x36, 0x37)       \
    __ENUMERATE_OPENTYPE_OPCODES(SHPIX, 0x38, 0x38)     \
    __ENUMERATE_OPENTYPE_OPCODES(MSIRP, 0x3A, 0x3B)     \
    __ENUMERATE_OPENTYPE_OPCODES(MDAP, 0x2E, 0x2F)      \
    __ENUMERATE_OPENTYPE_OPCODES(MIAP, 0x3E, 0x3F)      \
    __ENUMERATE_OPENTYPE_OPCODES(MDRP, 0xC0, 0xDF)      \
    __ENUMERATE_OPENTYPE_OPCODES(MIRP, 0xE0, 0xFF)      \
    __ENUMERATE_OPENTYPE_OPCODES(ALIGNRP, 0x3C, 0x3C)   \
    __ENUMERATE_OPENTYPE_OPCODES(ISECT, 0x0F, 0x0F)     \
    __ENUMERATE_OPENTYPE_OPCODES(ALIGNPTS, 0x27, 0x27)  \
    __ENUMERATE_OPENTYPE_OPCODES(IP, 0x39, 0x39)        \
    __ENUMERATE_OPENTYPE_OPCODES(UTP, 0x29, 0x29)       \
    __ENUMERATE_OPENTYPE_OPCODES(IUP, 0x30, 0x31)       \
    /* Managing exceptions */                           \
    __ENUMERATE_OPENTYPE_OPCODES(DELTAP1, 0x5D, 0x5D)   \
    __ENUMERATE_OPENTYPE_OPCODES(DELTAP2, 0x71, 0x71)   \
    __ENUMERATE_OPENTYPE_OPCODES(DELTAP3, 0x72, 0x72)   \
    __ENUMERATE_OPENTYPE_OPCODES(DELTAC1, 0x73, 0x73)   \
    __ENUMERATE_OPENTYPE_OPCODES(DELTAC2, 0x74, 0x74)   \
    __ENUMERATE_OPENTYPE_OPCODES(DELTAC3, 0x75, 0x75)   \
    /* Managing the stack */                            \
    __ENUMERATE_OPENTYPE_OPCODES(DUP, 0x20, 0x20)       \
    __ENUMERATE_OPENTYPE_OPCODES(POP, 0x21, 0x21)       \
    __ENUMERATE_OPENTYPE_OPCODES(CLEAR, 0x22, 0x22)     \
    __ENUMERATE_OPENTYPE_OPCODES(SWAP, 0x23, 0x23)      \
    __ENUMERATE_OPENTYPE_OPCODES(DEPTH, 0x24, 0x24)     \
    __ENUMERATE_OPENTYPE_OPCODES(CINDEX, 0x25, 0x25)    \
    __ENUMERATE_OPENTYPE_OPCODES(MINDEX, 0x26, 0x26)    \
    __ENUMERATE_OPENTYPE_OPCODES(ROLL, 0x8a, 0x8a)      \
    /* Managing the flow of control */                  \
    __ENUMERATE_OPENTYPE_OPCODES(IF, 0x58, 0x58)        \
    __ENUMERATE_OPENTYPE_OPCODES(ELSE, 0x1B, 0x1B)      \
    __ENUMERATE_OPENTYPE_OPCODES(EIF, 0x59, 0x59)       \
    __ENUMERATE_OPENTYPE_OPCODES(JROT, 0x78, 0x78)      \
    __ENUMERATE_OPENTYPE_OPCODES(JMPR, 0x1C, 0x1C)      \
    __ENUMERATE_OPENTYPE_OPCODES(JROF, 0x79, 0x79)      \
    /* Logical functions */                             \
    __ENUMERATE_OPENTYPE_OPCODES(LT, 0x50, 0x50)        \
    __ENUMERATE_OPENTYPE_OPCODES(LTEQ, 0x51, 0x51)      \
    __ENUMERATE_OPENTYPE_OPCODES(GT, 0x52, 0x52)        \
    __ENUMERATE_OPENTYPE_OPCODES(GTEQ, 0x53, 0x53)      \
    __ENUMERATE_OPENTYPE_OPCODES(EQ, 0x54, 0x54)        \
    __ENUMERATE_OPENTYPE_OPCODES(NEQ, 0x55, 0x55)       \
    __ENUMERATE_OPENTYPE_OPCODES(ODD, 0x56, 0x56)       \
    __ENUMERATE_OPENTYPE_OPCODES(EVEN, 0x57, 0x57)      \
    __ENUMERATE_OPENTYPE_OPCODES(AND, 0x5A, 0x5A)       \
    __ENUMERATE_OPENTYPE_OPCODES(OR, 0x5B, 0x5B)        \
    __ENUMERATE_OPENTYPE_OPCODES(NOT, 0x5C, 0x5C)       \
    /* Arithmetic and math instructions */              \
    __ENUMERATE_OPENTYPE_OPCODES(ADD, 0x60, 0x60)       \
    __ENUMERATE_OPENTYPE_OPCODES(SUB, 0x61, 0x61)       \
    __ENUMERATE_OPENTYPE_OPCODES(DIV, 0x62, 0x62)       \
    __ENUMERATE_OPENTYPE_OPCODES(MUL, 0x63, 0x63)       \
    __ENUMERATE_OPENTYPE_OPCODES(ABS, 0x64, 0x64)       \
    __ENUMERATE_OPENTYPE_OPCODES(NEG, 0x65, 0x65)       \
    __ENUMERATE_OPENTYPE_OPCODES(FLOOR, 0x66, 0x66)     \
    __ENUMERATE_OPENTYPE_OPCODES(CEILING, 0x67, 0x67)   \
    __ENUMERATE_OPENTYPE_OPCODES(MAX, 0x8B, 0x8B)       \
    __ENUMERATE_OPENTYPE_OPCODES(MIN, 0x8C, 0x8C)       \
    /* Compensating for the engine characteristics */   \
    __ENUMERATE_OPENTYPE_OPCODES(ROUND, 0x68, 0x6B)     \
    __ENUMERATE_OPENTYPE_OPCODES(NROUND, 0x6C, 0x6F)    \
    /* Defining and using functions and instructions */ \
    __ENUMERATE_OPENTYPE_OPCODES(FDEF, 0x2C, 0x2C)      \
    __ENUMERATE_OPENTYPE_OPCODES(ENDF, 0x2D, 0x2D)      \
    __ENUMERATE_OPENTYPE_OPCODES(CALL, 0x2B, 0x2B)      \
    __ENUMERATE_OPENTYPE_OPCODES(LOOPCALL, 0x2A, 0x2A)  \
    __ENUMERATE_OPENTYPE_OPCODES(IDEF, 0x89, 0x89)      \
    /* Debugging */                                     \
    __ENUMERATE_OPENTYPE_OPCODES(DEBUG, 0x4F, 0x4F)     \
    /* Miscellaneous instructions */                    \
    __ENUMERATE_OPENTYPE_OPCODES(GETINFO, 0x88, 0x88)   \
    __ENUMERATE_OPENTYPE_OPCODES(GETVARIATION, 0x91, 0x91)

// https://learn.microsoft.com/en-us/typography/opentype/spec/tt_instructions
enum class Opcode : u8 {
#define __ENUMERATE_OPENTYPE_OPCODES(mnemonic, range_start, range_end) \
    mnemonic = range_start,                                            \
    mnemonic##_MAX = range_end,
    ENUMERATE_OPENTYPE_OPCODES
#undef __ENUMERATE_OPENTYPE_OPCODES
};

struct Instruction {
    bool a() const;
    bool b() const;
    bool c() const;
    bool d() const;
    bool e() const;

    u8 flag_bits() const { return m_flag_bits; }
    Opcode opcode() const { return m_opcode; }
    ReadonlyBytes values() const { return m_values; }

    Instruction(Opcode opcode, ReadonlyBytes values = {});

private:
    Opcode m_opcode;
    ReadonlyBytes m_values;
    u8 m_flag_bits;
};

StringView opcode_mnemonic(Opcode);

struct InstructionHandler;

struct InstructionStream {
    InstructionStream(InstructionHandler& handler, ReadonlyBytes bytes)
        : m_handler { handler }
        , m_bytes(bytes)
    {
    }

    bool at_end() const;
    void process_next_instruction();
    void jump_to_next(Opcode);

    size_t current_position() const { return m_byte_index; }
    size_t length() const { return m_bytes.size(); }

    struct Context {
        Context(Instruction instruction, InstructionStream& stream)
            : m_instruction(instruction)
            , m_stream(stream)
        {
        }
        Instruction instruction() { return m_instruction; }
        InstructionStream& stream() { return m_stream; }

    private:
        Instruction m_instruction;
        InstructionStream& m_stream;
    };

private:
    u8 next_byte();
    ReadonlyBytes take_n_bytes(size_t n);

    InstructionHandler& m_handler;
    ReadonlyBytes m_bytes;
    size_t m_byte_index { 0 };
};

struct InstructionHandler {
    using Context = InstructionStream::Context;

    virtual void default_handler(Context) = 0;
    virtual void before_operation(InstructionStream&, Opcode) { }
    virtual void after_operation(InstructionStream&, Opcode) { }

#define __ENUMERATE_OPENTYPE_OPCODES(mnemonic, _, __) \
    virtual void handle_##mnemonic(Context context)   \
    {                                                 \
        default_handler(context);                     \
    }
    ENUMERATE_OPENTYPE_OPCODES
#undef __ENUMERATE_OPENTYPE_OPCODES

    virtual ~InstructionHandler() = default;
};

}
