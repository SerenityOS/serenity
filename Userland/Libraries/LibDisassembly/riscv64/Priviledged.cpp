/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Priviledged.h"
#include "IM.h"
#include "Zicsr.h"

namespace Disassembly::RISCV64 {

NonnullOwnPtr<InstructionImpl> parse_system(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);
    if (raw_parts.funct3 != 0)
        return parse_csr(raw_parts);

    auto system_opcode = instruction >> 7;
    switch (system_opcode) {
    case 0b000000000000'00000'000'00000:
        return adopt_own(*new (nothrow) EnvironmentCall);
    case 0b000000000000'00000'000'00001:
        return adopt_own(*new (nothrow) EnvironmentBreak);
    case 0b0001000'00010'00000'000'00000:
        return adopt_own(*new (nothrow) SupervisorModeTrapReturn);
    case 0b0011000'00010'00000'000'00000:
        return adopt_own(*new (nothrow) MachineModeTrapReturn);
    case 0b0001000'00101'00000'000'00000:
        return adopt_own(*new (nothrow) WaitForInterrupt);
    default:
        VERIFY_NOT_REACHED();
    }
}

}
