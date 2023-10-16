/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <LibDisassembly/Instruction.h>
#include <LibDisassembly/InstructionStream.h>
#include <LibDisassembly/x86/Instruction.h>

namespace Disassembly {

class Disassembler {
public:
    explicit Disassembler(InstructionStream& stream)
        : m_stream(stream)
    {
    }

    Optional<NonnullOwnPtr<Instruction>> next()
    {
        if (!m_stream.can_read())
            return {};
#if ARCH(X86_64)
        return make<X86::Instruction>(X86::Instruction::from_stream(m_stream, X86::ProcessorMode::Long));
#else
        dbgln("Unsupported platform");
        return {};
#endif
    }

private:
    InstructionStream& m_stream;
};

}
