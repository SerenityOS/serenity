/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Instruction.h"

// I, M, and Zifencei extensions.
namespace Disassembly::RISCV64 {

// LUI
class LoadUpperImmediate : public UJTypeInstruction {
public:
    virtual ~LoadUpperImmediate() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(LoadUpperImmediate const&) const = default;

    LoadUpperImmediate(i32 immediate, Register rd)
        : UJTypeInstruction(immediate, rd)
    {
    }
};

// JAL
class JumpAndLink : public UJTypeInstruction {
public:
    virtual ~JumpAndLink() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(JumpAndLink const&) const = default;

    JumpAndLink(i32 immediate, Register rd)
        : UJTypeInstruction(immediate, rd)
    {
    }
};

// JALR
class JumpAndLinkRegister : public ITypeInstruction {
public:
    virtual ~JumpAndLinkRegister() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(JumpAndLinkRegister const&) const = default;

    JumpAndLinkRegister(i32 offset, Register base, Register rd)
        : ITypeInstruction(offset, base, rd)
    {
    }
};

// AUIPC
class AddUpperImmediateToProgramCounter : public UJTypeInstruction {
public:
    virtual ~AddUpperImmediateToProgramCounter() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(AddUpperImmediateToProgramCounter const&) const = default;

    AddUpperImmediateToProgramCounter(i32 immediate, Register rd)
        : UJTypeInstruction(immediate, rd)
    {
    }
};

class ArithmeticImmediateInstruction : public ITypeInstruction {
public:
    enum class Operation {
        Add,
        SetLessThan,
        SetLessThanUnsigned,
        Xor,
        Or,
        And,
        ShiftLeftLogical,
        ShiftRightLogical,
        ShiftRightArithmetic,
        AddWord,
        ShiftLeftLogicalWord,
        ShiftRightLogicalWord,
        ShiftRightArithmeticWord,
    };

    virtual ~ArithmeticImmediateInstruction() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(ArithmeticImmediateInstruction const&) const = default;

    ArithmeticImmediateInstruction(Operation operation, i32 immediate, Register rs1, Register rd)
        : ITypeInstruction(immediate, rs1, rd)
        , m_operation(operation)
    {
    }

private:
    Operation m_operation;
};

class ArithmeticInstruction : public RTypeInstruction {
public:
    enum class Operation {
        // RV32I
        Add,
        Subtract,
        SetLessThan,
        SetLessThanUnsigned,
        Xor,
        Or,
        And,
        ShiftLeftLogical,
        ShiftRightLogical,
        ShiftRightArithmetic,
        // RV64I
        AddWord,
        SubtractWord,
        ShiftLeftLogicalWord,
        ShiftRightLogicalWord,
        ShiftRightArithmeticWord,
        // RV32M
        Multiply,
        MultiplyHigh,
        MultiplyHighSignedUnsigned,
        MultiplyHighUnsigned,
        Divide,
        DivideUnsigned,
        Remainder,
        RemainderUnsigned,
        // RV64M
        MultiplyWord,
        DivideWord,
        DivideUnsignedWord,
        RemainderWord,
        RemainderUnsignedWord,
    };

    virtual ~ArithmeticInstruction() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(ArithmeticInstruction const&) const = default;

    ArithmeticInstruction(Operation operation, Register rs1, Register rs2, Register rd)
        : RTypeInstruction(rs1, rs2, rd)
        , m_operation(operation)
    {
    }

private:
    Operation m_operation;
};

class MemoryLoad : public ITypeInstruction {
public:
    virtual ~MemoryLoad() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(MemoryLoad const&) const = default;

    MemoryLoad(i32 offset, Register base, MemoryAccessMode width, Register rd)
        : ITypeInstruction(offset, base, rd)
        , m_width(width)
    {
    }

private:
    MemoryAccessMode m_width;
};

class MemoryStore : public BSTypeInstruction {
public:
    virtual ~MemoryStore() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(MemoryStore const&) const = default;

    MemoryStore(i32 offset, Register source, Register base, MemoryAccessMode width)
        : BSTypeInstruction(offset, base, source)
        , m_width(width)
    {
    }

private:
    MemoryAccessMode m_width;
};

class Branch : public BSTypeInstruction {
public:
    enum class Condition : u8 {
        Equals = 0b000,
        NotEquals = 0b001,
        LessThan = 0b100,
        GreaterEquals = 0b101,
        LessThanUnsigned = 0b110,
        GreaterEqualsUnsigned = 0b111,
    };

    virtual ~Branch() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(Branch const&) const = default;

    Branch(Condition condition, i32 offset, Register rs1, Register rs2)
        : BSTypeInstruction(offset, rs1, rs2)
        , m_condition(condition)
    {
    }

private:
    Condition m_condition;
};

class Fence : public InstructionImpl {
public:
    enum class AccessType : u8 {
        Input = 1 << 3,
        Output = 1 << 2,
        Read = 1 << 1,
        Write = 1 << 0,
    };

    enum class Mode : u8 {
        Normal = 0,
        // Used by fence.tso for implementing Total Store Ordering (x86's memory consistency model)
        // Chapter 2.7: "This leaves non-AMO store operations in the FENCE.TSO’s predecessor set unordered with non-AMO loads in its successor set."
        NoStoreToLoadOrdering = 0b1000,
    };

    virtual ~Fence() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual i32 immediate() const override { return 0; }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(Fence const&) const = default;

    Fence(AccessType predecessor, AccessType successor, Mode mode)
        : m_predecessor(predecessor)
        , m_successor(successor)
        , m_mode(mode)
    {
    }

private:
    AccessType m_predecessor;
    AccessType m_successor;
    Mode m_mode;
};
AK_ENUM_BITWISE_OPERATORS(Fence::AccessType);

class EnvironmentCall : public InstructionWithoutArguments {
public:
    virtual ~EnvironmentCall() = default;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(EnvironmentCall const&) const = default;

    EnvironmentCall() = default;
};

class EnvironmentBreak : public InstructionWithoutArguments {
public:
    virtual ~EnvironmentBreak() = default;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(EnvironmentBreak const&) const = default;

    EnvironmentBreak() = default;
};

class InstructionFetchFence : public InstructionWithoutArguments {
public:
    virtual ~InstructionFetchFence() = default;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(InstructionFetchFence const&) const = default;

    InstructionFetchFence() = default;
};

NonnullOwnPtr<LoadUpperImmediate> parse_lui(u32 instruction);
NonnullOwnPtr<JumpAndLink> parse_jal(u32 instruction);
NonnullOwnPtr<JumpAndLinkRegister> parse_jalr(u32 instruction);
NonnullOwnPtr<AddUpperImmediateToProgramCounter> parse_auipc(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_op_imm(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_op_imm_32(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_op_32(u32 instruction);
NonnullOwnPtr<ArithmeticInstruction> parse_op(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_branch(u32 instruction);
NonnullOwnPtr<MemoryLoad> parse_load(u32 instruction);
NonnullOwnPtr<MemoryStore> parse_store(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_misc_mem(u32 instruction);

}
