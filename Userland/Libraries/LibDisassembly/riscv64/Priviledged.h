/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Instruction.h"

// Instructions from various priviledged extensions.
namespace Disassembly::RISCV64 {

class SupervisorModeTrapReturn : public InstructionWithoutArguments {
public:
    virtual ~SupervisorModeTrapReturn() = default;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(SupervisorModeTrapReturn const&) const = default;

    SupervisorModeTrapReturn() = default;
};

class MachineModeTrapReturn : public InstructionWithoutArguments {
public:
    virtual ~MachineModeTrapReturn() = default;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(MachineModeTrapReturn const&) const = default;

    MachineModeTrapReturn() = default;
};

class WaitForInterrupt : public InstructionWithoutArguments {
public:
    virtual ~WaitForInterrupt() = default;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(WaitForInterrupt const&) const = default;

    WaitForInterrupt() = default;
};

NonnullOwnPtr<InstructionImpl> parse_system(u32 instruction);

}
