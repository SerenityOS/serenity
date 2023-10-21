/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDisassembly/riscv64/Instruction.h>

// Zicsr extension.
namespace Disassembly::RISCV64 {

class CSRInstruction : public InstructionWithDestinationRegister
    , public InstructionImpl {
public:
    enum class Operation {
        ReadWrite,
        ReadSet,
        ReadClear,
    };

    virtual ~CSRInstruction() = default;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(CSRInstruction const&) const = default;
    u16 csr() const { return m_csr; }
    Operation operation() const { return m_operation; }

    CSRInstruction(Operation operation, u16 csr, Register rd)
        : InstructionWithDestinationRegister(rd)
        , m_csr(csr)
        , m_operation(operation)
    {
    }

private:
    u16 m_csr;
    Operation m_operation;
};

class CSRRegisterInstruction : public InstructionWithSourceRegister
    , public CSRInstruction {
public:
    virtual ~CSRRegisterInstruction() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual i32 immediate() const override { return 0; }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(CSRRegisterInstruction const&) const = default;

    CSRRegisterInstruction(Operation operation, u16 csr, Register rs, Register rd)
        : InstructionWithSourceRegister(rs)
        , CSRInstruction(operation, csr, rd)
    {
    }
};

class CSRImmediateInstruction : public CSRInstruction {
public:
    virtual ~CSRImmediateInstruction() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual i32 immediate() const override { return static_cast<i32>(m_immediate); }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(CSRImmediateInstruction const&) const = default;

    CSRImmediateInstruction(Operation operation, u16 csr, u8 immediate, Register rd)
        : CSRInstruction(operation, csr, rd)
        , m_immediate(immediate)
    {
    }

private:
    u8 m_immediate;
};

NonnullOwnPtr<CSRInstruction> parse_csr(RawIType raw_parts);

}
