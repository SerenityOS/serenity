/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Emulator.h"
#include "SoftCPU.h"
#include <AK/LogStream.h>
#include <Kernel/API/Syscall.h>
#include <unistd.h>
#include <stdio.h>

namespace UserspaceEmulator {

Emulator::Emulator()
    : m_cpu(*this)
{
}

int Emulator::exec(X86::SimpleInstructionStream& stream, u32 base)
{
    size_t offset = 0;
    while (!m_shutdown) {
        auto insn = X86::Instruction::from_stream(stream, true, true);
        out() << "instruction: " << insn.to_string(base + offset);

        (m_cpu.*insn.handler())(insn);
        m_cpu.dump();

        offset += insn.length();

        if (insn.mnemonic() == "RET")
            break;
    }
    return m_exit_status;
}

u32 Emulator::virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3)
{
    (void) arg2;
    (void) arg3;

    printf("Syscall: %s (%x)\n", Syscall::to_string((Syscall::Function)function), function);
    switch (function) {
    case SC_getuid:
        return virt$getuid();
    case SC_exit:
        virt$exit((int)arg1);
        return 0;
    default:
        warn() << "Unimplemented syscall!";
        TODO();
    }
}

uid_t Emulator::virt$getuid()
{
    return getuid();
}

void Emulator::virt$exit(int status)
{
    out() << "exit(" << status << "), shutting down!";
    m_exit_status = status;
    m_shutdown = true;
}

}
