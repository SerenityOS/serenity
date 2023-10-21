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
    if (raw_parts.funct3 != 0b000 && raw_parts.funct3 != 0b100)
        return parse_csr(raw_parts);

    auto system_opcode = instruction >> 7;
    switch (system_opcode) {
    case 0b000000000000'00000'000'00000:
        return make<EnvironmentCall>();
    case 0b000000000001'00000'000'00000:
        return make<EnvironmentBreak>();
    case 0b0001000'00010'00000'000'00000:
        return make<SupervisorModeTrapReturn>();
    case 0b0011000'00010'00000'000'00000:
        return make<MachineModeTrapReturn>();
    case 0b0001000'00101'00000'000'00000:
        return make<WaitForInterrupt>();
    default:
        return make<UnknownInstruction>();
    }
}

}
