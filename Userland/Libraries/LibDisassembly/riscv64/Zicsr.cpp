/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Zicsr.h"

namespace Disassembly::RISCV64 {

NonnullOwnPtr<CSRInstruction> parse_csr(RawIType raw_parts)
{
    auto is_immediate = (raw_parts.funct3 & 0b100) > 0;
    auto operation = CSRInstruction::Operation::ReadWrite;

    switch (raw_parts.funct3 & 0b11) {
    case 0b01:
        operation = CSRInstruction::Operation::ReadWrite;
        break;
    case 0b10:
        operation = CSRInstruction::Operation::ReadSet;
        break;
    case 0b11:
        operation = CSRInstruction::Operation::ReadClear;
        break;
    case 0b00:
        VERIFY_NOT_REACHED();
    }

    if (is_immediate)
        return adopt_own(*new (nothrow) CSRImmediateInstruction(operation, static_cast<u16>(raw_parts.imm & 0xfff), raw_parts.rs1.value(), raw_parts.rd));
    else
        return adopt_own(*new (nothrow) CSRRegisterInstruction(operation, static_cast<u16>(raw_parts.imm & 0xfff), raw_parts.rs1, raw_parts.rd));
}

}
