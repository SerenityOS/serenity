/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/ByteString.h>
#include <AK/Endian.h>
#include <AK/EnumBits.h>
#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <LibDisassembly/Instruction.h>
#include <LibDisassembly/InstructionStream.h>
#include <LibDisassembly/riscv64/Encoding.h>
#include <LibDisassembly/riscv64/Registers.h>

// All section numbers from the RISC-V Unprivileged ISA specification V20191213
namespace Disassembly::RISCV64 {

// Various parameters to control how RISC-V instructions are displayed.
struct DisplayStyle {
    enum class RegisterNames : u8 {
        // Uses hardware names; x0 - x31, f0 - f31
        Hardware,
        // Uses standard ABI names; ra, zero, a0, s1, ft2, ...
        ABI,
        // Uses ABI names with fp instead of s0.
        ABIWithFramePointer,
    } register_names { RegisterNames::ABIWithFramePointer };

    // Use various convenience aliases that most assemblers support, such as j <offset> instead of jal x0, <offset>.
    enum class UsePseudoinstructions : bool {
        No,
        Yes,
    } use_pseudoinstructions { UsePseudoinstructions::Yes };

    // For instructions who encode a relative offset, show either the raw offset (as encoded in the instruction immediate)
    // or the target address in addition to an associated symbol, if available.
    enum class RelativeAddressStyle : u8 {
        Offset,
        Symbol,
    } relative_address_style { RelativeAddressStyle::Symbol };
};

class InstructionImpl {
public:
    virtual ~InstructionImpl() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const = 0;
    virtual i32 immediate() const = 0;
    virtual String mnemonic() const = 0;
    // Making the equality operator virtual would prevent us from defaulting (i.e. auto-generating) all normal equality operators,
    // since C++ selects the virtual equality operator for "the parent's equality check", which in fact will just call the same virtual equality operator again.
    virtual bool instruction_equals(InstructionImpl const&) const = 0;
    // Do not call this if you wish to compare two instructions.
    bool operator==(InstructionImpl const&) const { return true; }
};

class Instruction : public Disassembly::Instruction {
    AK_MAKE_DEFAULT_MOVABLE(Instruction);
    AK_MAKE_NONCOPYABLE(Instruction);

private:
    struct CompressedTag { };

public:
    virtual ~Instruction() = default;

    template<typename InstructionStreamType>
    static NonnullOwnPtr<Instruction> from_stream(InstructionStreamType&, DisplayStyle);
    static NonnullOwnPtr<Instruction> from_stream(InstructionStream&);

    virtual ByteString to_byte_string(u32, Optional<SymbolProvider const&>) const override;
    virtual size_t length() const override
    {
        return m_was_compressed ? 2 : 4;
    }
    virtual ByteString mnemonic() const override;

    static NonnullOwnPtr<Instruction> parse_compressed(u16 compressed_instruction);
    static NonnullOwnPtr<Instruction> parse_full(u32 instruction);

    Instruction(NonnullOwnPtr<InstructionImpl> data, u16 raw_instruction, CompressedTag)
        : m_data(move(data))
        , m_was_compressed(true)
        , m_raw_instruction(raw_instruction)
    {
    }

    Instruction(NonnullOwnPtr<InstructionImpl> data, u32 raw_instruction)
        : m_data(move(data))
        , m_was_compressed(false)
        , m_raw_instruction(raw_instruction)
    {
    }

    void set_display_style(DisplayStyle display_style) { m_display_style = display_style; }

    InstructionImpl const& instruction_data() const { return *m_data; }
    u32 raw_instruction() const { return m_raw_instruction; }

private:
    // FIXME: Figure out a nice way of storing this data inline (while being able to pass the InstructionImpl as a NonnullOwnPtr and not invoking tons of UB).
    NonnullOwnPtr<InstructionImpl> m_data;
    bool m_was_compressed;
    // May only contain a halfword if it was a compressed instruction.
    u32 m_raw_instruction;
    DisplayStyle m_display_style;
};

class UnknownInstruction : public InstructionImpl {
public:
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual i32 immediate() const override { return 0; }
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
};

class InstructionWithDestinationRegister {
public:
    InstructionWithDestinationRegister(Register rd)
        : m_destination_register(rd)
    {
    }
    bool operator==(InstructionWithDestinationRegister const&) const = default;

    Register destination_register() const { return m_destination_register; }

private:
    Register m_destination_register;
};

class InstructionWithSourceRegister {
public:
    InstructionWithSourceRegister(Register rs1)
        : m_source_register(rs1)
    {
    }
    bool operator==(InstructionWithSourceRegister const&) const = default;

    Register source_register() const { return m_source_register; }

private:
    Register m_source_register;
};

class InstructionWithTwoSourceRegisters {
public:
    InstructionWithTwoSourceRegisters(Register rs1, Register rs2)
        : m_source_register_1(rs1)
        , m_source_register_2(rs2)
    {
    }
    bool operator==(InstructionWithTwoSourceRegisters const&) const = default;

    Register source_register_1() const { return m_source_register_1; }
    Register source_register_2() const { return m_source_register_2; }

private:
    Register m_source_register_1;
    Register m_source_register_2;
};

// In terms of fields, U and J-type are identical, they just use differently scrambled immediate bits.
class UJTypeInstruction : public InstructionWithDestinationRegister
    , public InstructionImpl {
public:
    virtual ~UJTypeInstruction() = default;
    virtual i32 immediate() const override { return m_immediate; }
    bool operator==(UJTypeInstruction const&) const = default;

    UJTypeInstruction(i32 immediate, Register rd)
        : InstructionWithDestinationRegister(rd)
        , m_immediate(immediate)
    {
    }

private:
    i32 m_immediate;
};

class ITypeInstruction : public InstructionWithDestinationRegister
    , public InstructionWithSourceRegister
    , public InstructionImpl {
public:
    virtual ~ITypeInstruction() = default;
    virtual i32 immediate() const override { return m_immediate; }
    bool operator==(ITypeInstruction const&) const = default;

    ITypeInstruction(i32 immediate, Register rs1, Register rd)
        : InstructionWithDestinationRegister(rd)
        , InstructionWithSourceRegister(rs1)
        , m_immediate(immediate)
    {
    }

private:
    i32 m_immediate;
};

// In terms of fields, S and B-type are identical, they just use differently scrambled immediate bits.
class BSTypeInstruction : public InstructionImpl
    , public InstructionWithTwoSourceRegisters {
public:
    virtual ~BSTypeInstruction() = default;
    virtual i32 immediate() const override { return m_immediate; }
    bool operator==(BSTypeInstruction const&) const = default;

    BSTypeInstruction(i32 immediate, Register rs1, Register rs2)
        : InstructionWithTwoSourceRegisters(rs1, rs2)
        , m_immediate(immediate)
    {
    }

private:
    i32 m_immediate;
};

class RTypeInstruction : public InstructionImpl
    , public InstructionWithDestinationRegister
    , public InstructionWithTwoSourceRegisters {
public:
    virtual ~RTypeInstruction() = default;
    virtual i32 immediate() const override { return 0; }
    bool operator==(RTypeInstruction const&) const = default;

    RTypeInstruction(Register rs1, Register rs2, Register rd)
        : InstructionWithDestinationRegister(rd)
        , InstructionWithTwoSourceRegisters(rs1, rs2)
    {
    }
};

class InstructionWithoutArguments : public InstructionImpl {
public:
    virtual ~InstructionWithoutArguments() = default;

    virtual i32 immediate() const override { return 0; }
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
};

enum class Signedness : bool {
    Signed,
    Unsigned,
};

// Widths are represented by their corresponding powers of two byte sizes.
enum class DataWidth : u8 {
    Byte = 0,
    Halfword = 1,
    Word = 2,
    DoubleWord = 3,
    QuadWord = 4,
};

struct MemoryAccessMode {
    static MemoryAccessMode from_funct3(u8 funct3);

    bool operator==(MemoryAccessMode const&) const = default;

    DataWidth width;
    Signedness signedness;
};

template<typename InstructionStreamType>
NonnullOwnPtr<Instruction> Instruction::from_stream(InstructionStreamType& stream, DisplayStyle display_style)
{
    auto instruction = Instruction::from_stream(stream);
    instruction->set_display_style(display_style);
    return instruction;
}

}
