/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDisassembly/riscv64/Instruction.h>

// A extension.
namespace Disassembly::RISCV64 {

class AtomicOperation : public RTypeInstruction {
public:
    virtual ~AtomicOperation() = default;
    bool operator==(AtomicOperation const&) const = default;

    AtomicOperation(DataWidth width, bool is_acquire, bool is_release, Register rs1, Register rs2, Register rd)
        : RTypeInstruction(rs1, rs2, rd)
        , m_width(width)
        , m_is_acquire(is_acquire)
        , m_is_release(is_release)
    {
    }

    DataWidth width() const { return m_width; }
    bool is_acquire() const { return m_is_acquire; }
    bool is_release() const { return m_is_release; }
    bool is_acquire_release() const { return m_is_acquire && m_is_release; }

private:
    DataWidth m_width;
    bool m_is_acquire;
    bool m_is_release;
};

class LoadReservedStoreConditional : public AtomicOperation {
public:
    enum class Operation : bool {
        LoadReserved,
        StoreConditional,
    };

    virtual ~LoadReservedStoreConditional() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(LoadReservedStoreConditional const&) const = default;

    LoadReservedStoreConditional(Operation operation, bool is_acquire, bool is_release, DataWidth width, Register rs1, Register rs2, Register rd)
        : AtomicOperation(width, is_acquire, is_release, rs1, rs2, rd)
        , m_operation(operation)
    {
    }

private:
    Operation m_operation;
};

class AtomicMemoryOperation : public AtomicOperation {
public:
    enum class Operation : u8 {
        Swap = 0b00001,
        Add = 0b00000,
        Xor = 0b00100,
        And = 0b01100,
        Or = 0b01000,
        Min = 0b10000,
        Max = 0b10100,
        MinUnsigned = 0b11000,
        MaxUnsigned = 0b11100,
    };

    virtual ~AtomicMemoryOperation() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(AtomicMemoryOperation const&) const = default;

    AtomicMemoryOperation(Operation operation, bool is_acquire, bool is_release, DataWidth width, Register rs1, Register rs2, Register rd)
        : AtomicOperation(width, is_acquire, is_release, rs1, rs2, rd)
        , m_operation(operation)
    {
    }

private:
    Operation m_operation;
};

NonnullOwnPtr<InstructionImpl> parse_amo(u32 instruction);

}
