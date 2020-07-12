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
#include <AK/LexicalPath.h>
#include <AK/LogStream.h>
#include <Kernel/API/Syscall.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

namespace UserspaceEmulator {

static constexpr u32 stack_location = 0x10000000;
static constexpr size_t stack_size = 64 * KB;

class SimpleRegion final : public SoftMMU::Region {
public:
    SimpleRegion(u32 base, u32 size)
        : Region(base, size)
    {
        m_data = (u8*)calloc(1, size);
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

    u8* data() { return m_data; }

private:
    u8* m_data { nullptr };
};

Emulator::Emulator(const String& executable_path, NonnullRefPtr<ELF::Loader> elf)
    : m_elf(move(elf))
    , m_cpu(*this)
    , m_executable_path(executable_path)
{
    setup_stack();
}

void Emulator::setup_stack()
{
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(stack_location + stack_size);

    m_cpu.push_string(LexicalPath(m_executable_path).basename());
    u32 argv0 = m_cpu.esp();

    m_cpu.push32(0); // char** envp = { nullptr }
    u32 envp = m_cpu.esp();

    m_cpu.push32(0); // char** argv = { argv0, nullptr }
    m_cpu.push32(argv0);
    u32 argv = m_cpu.esp();

    m_cpu.push32(0); // (alignment)

    u32 argc = 1;
    m_cpu.push32(envp);
    m_cpu.push32(argv);
    m_cpu.push32(argc);
    m_cpu.push32(0); // (alignment)
}

bool Emulator::load_elf()
{
    m_elf->image().for_each_program_header([&](const ELF::Image::ProgramHeader& program_header) {
        if (program_header.type() == PT_LOAD) {
            auto region = make<SimpleRegion>(program_header.vaddr().get(), program_header.size_in_memory());
            memcpy(region->data(), program_header.raw_data(), program_header.size_in_image());
            mmu().add_region(move(region));
            return;
        }
        if (program_header.type() == PT_TLS) {
            auto tcb_region = make<SimpleRegion>(0x20000000, program_header.size_in_memory());
            memcpy(tcb_region->data(), program_header.raw_data(), program_header.size_in_image());

            auto tls_region = make<SimpleRegion>(0, 4);
            tls_region->write32(0, tcb_region->base() + 8);

            mmu().add_region(move(tcb_region));
            mmu().set_tls_region(move(tls_region));
            return;
        }
    });

    m_cpu.set_eip(m_elf->image().entry().get());
    return true;
}

class ELFSymbolProvider final : public X86::SymbolProvider {
public:
    ELFSymbolProvider(ELF::Loader& loader)
        : m_loader(loader)
    {
    }

    virtual String symbolicate(FlatPtr address, u32* offset = nullptr) const
    {
        return m_loader.symbolicate(address, offset);
    }

private:
    ELF::Loader& m_loader;
};

int Emulator::exec()
{
    ELFSymbolProvider symbol_provider(*m_elf);

    bool trace = false;

    while (!m_shutdown) {
        u32 base_eip = 0;
        if (trace)
            base_eip = m_cpu.eip();

        auto insn = X86::Instruction::from_stream(m_cpu, true, true);

        if (trace)
            out() << (const void*)base_eip << "  \033[33;1m" << insn.to_string(base_eip, &symbol_provider) << "\033[0m";

        (m_cpu.*insn.handler())(insn);

        if (trace)
            m_cpu.dump();
    }
    return m_exit_status;
}

void Emulator::dump_backtrace()
{
    u32 offset = 0;
    String symbol = m_elf->symbolicate(m_cpu.eip(), &offset);

    printf("> %#08x  %s +%#x\n", m_cpu.eip(), symbol.characters(), offset);

    u32 frame_ptr = m_cpu.ebp();
    while (frame_ptr) {
        u32 ret_ptr = m_mmu.read32({ 0x20, frame_ptr + 4 });
        if (!ret_ptr)
            return;
        symbol = m_elf->symbolicate(ret_ptr, &offset);
        printf("> %#08x  %s +%#x\n", ret_ptr, symbol.characters(), offset);

        frame_ptr = m_mmu.read32({ 0x20, frame_ptr });
    }
}

u32 Emulator::virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3)
{
    (void)arg2;
    (void)arg3;

    printf("Syscall: %s (%x)\n", Syscall::to_string((Syscall::Function)function), function);
    switch (function) {
    case SC_gettid:
        return virt$gettid();
    case SC_pledge:
        return virt$pledge(arg1);
    case SC_unveil:
        return virt$unveil(arg1);
    case SC_getuid:
        return virt$getuid();
    case SC_exit:
        virt$exit((int)arg1);
        return 0;
    default:
        warn() << "Unimplemented syscall!";
        dump_backtrace();
        TODO();
    }
}

u32 Emulator::virt$gettid()
{
    return gettid();
}

u32 Emulator::virt$pledge(u32)
{
    return 0;
}

u32 Emulator::virt$unveil(u32)
{
    return 0;
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
