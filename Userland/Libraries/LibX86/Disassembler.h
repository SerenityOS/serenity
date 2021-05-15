/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibX86/Instruction.h>

namespace X86 {

class Disassembler {
public:
    explicit Disassembler(InstructionStream& stream)
        : m_stream(stream)
    {
    }

    Optional<Instruction> next()
    {
        if (!m_stream.can_read())
            return {};
        return Instruction::from_stream(m_stream, true, true);
    }

private:
    InstructionStream& m_stream;
};

}
