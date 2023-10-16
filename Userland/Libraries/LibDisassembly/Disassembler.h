/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <LibDisassembly/Architecture.h>
#include <LibDisassembly/Instruction.h>
#include <LibDisassembly/InstructionStream.h>
#include <LibDisassembly/x86/Instruction.h>

namespace Disassembly {

class Disassembler {
public:
    explicit Disassembler(InstructionStream& stream, Architecture arch)
        : m_stream(stream)
        , m_arch(arch)
    {
    }

    Optional<NonnullOwnPtr<Instruction>> next()
    {
        if (!m_stream.can_read())
            return {};
        switch (m_arch) {
        case Architecture::X86:
            return make<X86::Instruction>(X86::Instruction::from_stream(m_stream, X86::ProcessorMode::Long));
        default:
            dbgln("Unsupported platform");
            return {};
        }
    }

private:
    InstructionStream& m_stream;
    Architecture m_arch;
};

}
