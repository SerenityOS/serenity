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
#include "MmapRegion.h"
#include "SimpleRegion.h"
#include "SoftCPU.h"
#include <AK/LexicalPath.h>
#include <AK/LogStream.h>
#include <Kernel/API/Syscall.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

//#define DEBUG_SPAM

namespace UserspaceEmulator {

static constexpr u32 stack_location = 0x10000000;
static constexpr size_t stack_size = 64 * KB;

static Emulator* s_the;

Emulator& Emulator::the()
{
    ASSERT(s_the);
    return *s_the;
}

Emulator::Emulator(const Vector<String>& arguments, NonnullRefPtr<ELF::Loader> elf)
    : m_elf(move(elf))
    , m_cpu(*this)
{
    ASSERT(!s_the);
    s_the = this;
    setup_stack(arguments);
}

void Emulator::setup_stack(const Vector<String>& arguments)
{
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(stack_location + stack_size);

    Vector<u32> argv_entries;

    for (auto& argument : arguments) {
        m_cpu.push_string(argument.characters());
        argv_entries.append(m_cpu.esp());
    }

    m_cpu.push32(0); // char** envp = { nullptr }
    u32 envp = m_cpu.esp();

    m_cpu.push32(0); // char** argv = { argv_entries..., nullptr }
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(argv_entries[i]);
    u32 argv = m_cpu.esp();

    m_cpu.push32(0); // (alignment)

    u32 argc = argv_entries.size();
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

#ifdef DEBUG_SPAM
    dbgprintf("Syscall: %s (%x)\n", Syscall::to_string((Syscall::Function)function), function);
#endif
    switch (function) {
    case SC_mmap:
        return virt$mmap(arg1);
    case SC_gettid:
        return virt$gettid();
    case SC_getpid:
        return virt$getpid();
    case SC_pledge:
        return virt$pledge(arg1);
    case SC_unveil:
        return virt$unveil(arg1);
    case SC_getuid:
        return virt$getuid();
    case SC_getgid:
        return virt$getgid();
    case SC_close:
        return virt$close(arg1);
    case SC_fstat:
        return virt$fstat(arg1, arg2);
    case SC_write:
        return virt$write(arg1, arg2, arg3);
    case SC_read:
        return virt$read(arg1, arg2, arg3);
    case SC_mprotect:
        return virt$mprotect(arg1, arg2, arg3);
    case SC_madvise:
        return virt$madvise(arg1, arg2, arg3);
    case SC_open:
        return virt$open(arg1);
    case SC_fcntl:
        return virt$fcntl(arg1, arg2, arg3);
    case SC_getgroups:
        return virt$getgroups(arg1, arg2);
    case SC_lseek:
        return virt$lseek(arg1, arg2, arg3);
    case SC_get_process_name:
        return virt$get_process_name(arg1, arg2);
    case SC_exit:
        virt$exit((int)arg1);
        return 0;
    default:
        warn() << "Unimplemented syscall!";
        dump_backtrace();
        TODO();
    }
}

int Emulator::virt$fstat(int fd, FlatPtr statbuf)
{
    struct stat local_statbuf;
    int rc = syscall(SC_fstat, fd, &local_statbuf);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(statbuf, &local_statbuf, sizeof(local_statbuf));
    return rc;
}

int Emulator::virt$close(int fd)
{
    return syscall(SC_close, fd);
}

int Emulator::virt$get_process_name(FlatPtr buffer, int size)
{
    if (size < 9)
        return -ENAMETOOLONG;
    mmu().copy_to_vm(buffer, "EMULATED", 9);
    return 0;
}

int Emulator::virt$lseek(int fd, off_t offset, int whence)
{
    return syscall(SC_lseek, fd, offset, whence);
}

int Emulator::virt$getgroups(ssize_t count, FlatPtr groups)
{
    if (!count)
        return syscall(SC_getgroups, 0, nullptr);

    auto buffer = ByteBuffer::create_uninitialized(count * sizeof(gid_t));
    int rc = syscall(SC_getgroups, count, buffer.data());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(groups, buffer.data(), buffer.size());
    return 0;
}

u32 Emulator::virt$fcntl(int fd, int cmd, u32 arg)
{
    switch (cmd) {
    case F_DUPFD:
    case F_GETFD:
    case F_SETFD:
    case F_GETFL:
    case F_SETFL:
    case F_ISTTY:
        break;
    default:
        TODO();
    }

    return syscall(SC_fcntl, fd, cmd, arg);
}

u32 Emulator::virt$open(u32 params_addr)
{
    Syscall::SC_open_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);

    int fd = openat_with_path_length(params.dirfd, (const char*)path.data(), path.size(), params.options, params.mode);
    if (fd < 0)
        return -errno;
    return fd;
}

u32 Emulator::virt$mmap(u32 params_addr)
{
    Syscall::SC_mmap_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    ASSERT(params.addr == 0);

    // FIXME: Write a proper VM allocator
    static u32 next_address = 0x30000000;

    u32 final_address = 0;
    u32 final_size = round_up_to_power_of_two(params.size, PAGE_SIZE);

    if (params.alignment) {
        // FIXME: What if alignment is not a power of 2?
        final_address = round_up_to_power_of_two(next_address, params.alignment);
    } else {
        final_address = next_address;
    }

    next_address = final_address + final_size;

    if (params.flags & MAP_ANONYMOUS)
        mmu().add_region(MmapRegion::create_anonymous(final_address, final_size, params.prot));
    else
        mmu().add_region(MmapRegion::create_file_backed(final_address, final_size, params.prot, params.flags, params.fd, params.offset));

    return final_address;
}

u32 Emulator::virt$gettid()
{
    return gettid();
}

u32 Emulator::virt$getpid()
{
    return getpid();
}

u32 Emulator::virt$pledge(u32)
{
    return 0;
}

u32 Emulator::virt$unveil(u32)
{
    return 0;
}

u32 Emulator::virt$mprotect(FlatPtr, size_t, int)
{
    return 0;
}

u32 Emulator::virt$madvise(FlatPtr, size_t, int)
{
    return 0;
}

uid_t Emulator::virt$getuid()
{
    return getuid();
}

gid_t Emulator::virt$getgid()
{
    return getgid();
}

u32 Emulator::virt$write(int fd, FlatPtr data, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    auto buffer = mmu().copy_buffer_from_vm(data, size);
    return syscall(SC_write, fd, buffer.data(), buffer.size());
}

u32 Emulator::virt$read(int fd, FlatPtr buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    auto local_buffer = ByteBuffer::create_uninitialized(size);
    int nread = syscall(SC_read, fd, local_buffer.data(), local_buffer.size());
    if (nread < 0)
        return nread;
    mmu().copy_to_vm(buffer, local_buffer.data(), local_buffer.size());
    return nread;
}

void Emulator::virt$exit(int status)
{
    dbg() << "exit(" << status << "), shutting down!";
    m_exit_status = status;
    m_shutdown = true;
}

}
