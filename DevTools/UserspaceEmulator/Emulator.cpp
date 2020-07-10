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
#include <stdio.h>
#include <unistd.h>

namespace UserspaceEmulator {

static constexpr u32 stack_location = 0x10000000;
static constexpr size_t stack_size = 64 * KB;

class SimpleRegion final : public SoftMMU::Region {
public:
    SimpleRegion(u32 base, u32 size)
        : Region(base, size)
    {
        m_data = (u8*)malloc(size);
    }

    ~SimpleRegion()
    {
        free(m_data);
    }

    virtual u8 read8(u32 offset) override
    {
        ASSERT(offset < size());
        return *reinterpret_cast<const u8*>(m_data + offset);
    }

    virtual u16 read16(u32 offset) override
    {
        ASSERT(offset + 1 < size());
        return *reinterpret_cast<const u16*>(m_data + offset);
    }

    virtual u32 read32(u32 offset) override
    {
        ASSERT(offset + 3 < size());
        return *reinterpret_cast<const u32*>(m_data + offset);
    }

    virtual void write8(u32 offset, u8 value) override
    {
        ASSERT(offset < size());
        *reinterpret_cast<u8*>(m_data + offset) = value;
    }

    virtual void write16(u32 offset, u16 value) override
    {
        ASSERT(offset + 1 < size());
        *reinterpret_cast<u16*>(m_data + offset) = value;
    }

    virtual void write32(u32 offset, u32 value) override
    {
        ASSERT(offset + 3 < size());
        *reinterpret_cast<u32*>(m_data + offset) = value;
    }

private:
    u8* m_data { nullptr };
};

Emulator::Emulator()
    : m_cpu(*this)
{
    setup_stack();
}

void Emulator::setup_stack()
{
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(stack_location + stack_size);
}

int Emulator::exec(X86::SimpleInstructionStream& stream, u32 base)
{
    size_t offset = 0;
    while (!m_shutdown) {
        auto insn = X86::Instruction::from_stream(stream, true, true);
        out() << "\033[33;1m" << insn.to_string(base + offset) << "\033[0m";

        // FIXME: Remove this hack once it's no longer needed :^)
        if (insn.mnemonic() == "RET")
            break;

        (m_cpu.*insn.handler())(insn);
        m_cpu.dump();

        offset += insn.length();
    }
    return m_exit_status;
}

u32 Emulator::virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3)
{
    (void)arg2;
    (void)arg3;

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
