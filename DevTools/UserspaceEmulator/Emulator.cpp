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
#include "SharedBufferRegion.h"
#include "SimpleRegion.h"
#include "SoftCPU.h"
#include <AK/LexicalPath.h>
#include <AK/LogStream.h>
#include <Kernel/API/Syscall.h>
#include <LibX86/ELFSymbolProvider.h>
#include <fcntl.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <termios.h>
#include <unistd.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

//#define DEBUG_SPAM

namespace UserspaceEmulator {

static constexpr u32 stack_location = 0x10000000;
static constexpr size_t stack_size = 64 * KiB;

static Emulator* s_the;

Emulator& Emulator::the()
{
    ASSERT(s_the);
    return *s_the;
}

Emulator::Emulator(const Vector<String>& arguments, const Vector<String>& environment, NonnullRefPtr<ELF::Loader> elf)
    : m_elf(move(elf))
    , m_cpu(*this)
{
    m_malloc_tracer = make<MallocTracer>();
    ASSERT(!s_the);
    s_the = this;
    setup_stack(arguments, environment);
    register_signal_handlers();
    setup_signal_trampoline();
}

void Emulator::setup_stack(const Vector<String>& arguments, const Vector<String>& environment)
{
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    stack_region->set_stack(true);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(shadow_wrap_as_initialized<u32>(stack_location + stack_size));

    Vector<u32> argv_entries;

    for (auto& argument : arguments) {
        m_cpu.push_string(argument.characters());
        argv_entries.append(m_cpu.esp().value());
    }

    Vector<u32> env_entries;

    for (auto& variable : environment) {
        m_cpu.push_string(variable.characters());
        env_entries.append(m_cpu.esp().value());
    }

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // char** envp = { envv_entries..., nullptr }
    for (ssize_t i = env_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(shadow_wrap_as_initialized(env_entries[i]));
    u32 envp = m_cpu.esp().value();

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // char** argv = { argv_entries..., nullptr }
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        m_cpu.push32(shadow_wrap_as_initialized(argv_entries[i]));
    u32 argv = m_cpu.esp().value();

    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // (alignment)

    u32 argc = argv_entries.size();
    m_cpu.push32(shadow_wrap_as_initialized(envp));
    m_cpu.push32(shadow_wrap_as_initialized(argv));
    m_cpu.push32(shadow_wrap_as_initialized(argc));
    m_cpu.push32(shadow_wrap_as_initialized<u32>(0)); // (alignment)
}

bool Emulator::load_elf()
{
    m_elf->image().for_each_program_header([&](const ELF::Image::ProgramHeader& program_header) {
        if (program_header.type() == PT_LOAD) {
            auto region = make<SimpleRegion>(program_header.vaddr().get(), program_header.size_in_memory());
            if (program_header.is_executable() && !program_header.is_writable())
                region->set_text(true);
            memcpy(region->data(), program_header.raw_data(), program_header.size_in_image());
            memset(region->shadow_data(), 0x01, program_header.size_in_memory());
            mmu().add_region(move(region));
            return;
        }
        if (program_header.type() == PT_TLS) {
            auto tcb_region = make<SimpleRegion>(0x20000000, program_header.size_in_memory());
            memcpy(tcb_region->data(), program_header.raw_data(), program_header.size_in_image());
            memset(tcb_region->shadow_data(), 0x01, program_header.size_in_memory());

            auto tls_region = make<SimpleRegion>(0, 4);
            tls_region->write32(0, shadow_wrap_as_initialized(tcb_region->base() + program_header.size_in_memory()));
            memset(tls_region->shadow_data(), 0x01, 4);

            mmu().add_region(move(tcb_region));
            mmu().set_tls_region(move(tls_region));
            return;
        }
    });

    m_cpu.set_eip(m_elf->image().entry().get());

    auto malloc_symbol = m_elf->find_demangled_function("malloc");
    auto free_symbol = m_elf->find_demangled_function("free");

    m_malloc_symbol_start = malloc_symbol.value().value();
    m_malloc_symbol_end = m_malloc_symbol_start + malloc_symbol.value().size();
    m_free_symbol_start = free_symbol.value().value();
    m_free_symbol_end = m_free_symbol_start + free_symbol.value().size();

    m_debug_info = make<Debug::DebugInfo>(m_elf);
    return true;
}

int Emulator::exec()
{
    X86::ELFSymbolProvider symbol_provider(*m_elf);

    bool trace = false;

    while (!m_shutdown) {
        m_cpu.save_base_eip();

        auto insn = X86::Instruction::from_stream(m_cpu, true, true);

        if (trace)
            out() << (const void*)m_cpu.base_eip() << "  \033[33;1m" << insn.to_string(m_cpu.base_eip(), &symbol_provider) << "\033[0m";

        (m_cpu.*insn.handler())(insn);

        if (trace)
            m_cpu.dump();

        if (m_pending_signals)
            dispatch_one_pending_signal();
    }

    if (auto* tracer = malloc_tracer())
        tracer->dump_leak_report();

    return m_exit_status;
}

bool Emulator::is_in_malloc_or_free() const
{
    return (m_cpu.base_eip() >= m_malloc_symbol_start && m_cpu.base_eip() < m_malloc_symbol_end) || (m_cpu.base_eip() >= m_free_symbol_start && m_cpu.base_eip() < m_free_symbol_end);
}

Vector<FlatPtr> Emulator::raw_backtrace()
{
    Vector<FlatPtr> backtrace;
    backtrace.append(m_cpu.base_eip());

    // FIXME: Maybe do something if the backtrace has uninitialized data in the frame chain.

    u32 frame_ptr = m_cpu.ebp().value();
    while (frame_ptr) {
        u32 ret_ptr = m_mmu.read32({ 0x20, frame_ptr + 4 }).value();
        if (!ret_ptr)
            break;
        backtrace.append(ret_ptr);
        frame_ptr = m_mmu.read32({ 0x20, frame_ptr }).value();
    }
    return backtrace;
}

void Emulator::dump_backtrace(const Vector<FlatPtr>& backtrace)
{
    for (auto& address : backtrace) {
        u32 offset = 0;
        String symbol = m_elf->symbolicate(address, &offset);
        auto source_position = m_debug_info->get_source_position(address);
        new_warn("=={}==    {:p}  {}", getpid(), address, symbol);
        if (source_position.has_value())
            warnln(" (\033[34;1m{}\033[0m:{})", LexicalPath(source_position.value().file_path).basename(), source_position.value().line_number);
        else
            warnln(" +{:x}", offset);
    }
}

void Emulator::dump_backtrace()
{
    dump_backtrace(raw_backtrace());
}

u32 Emulator::virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3)
{
#ifdef DEBUG_SPAM
    dbgln("Syscall: {} ({:x})", Syscall::to_string((Syscall::Function)function), function);
#endif
    switch (function) {
    case SC_chdir:
        return virt$chdir(arg1, arg2);
    case SC_dup2:
        return virt$dup2(arg1, arg2);
    case SC_access:
        return virt$access(arg1, arg2, arg3);
    case SC_waitid:
        return virt$waitid(arg1);
    case SC_getcwd:
        return virt$getcwd(arg1, arg2);
    case SC_ttyname:
        return virt$ttyname(arg1, arg2, arg3);
    case SC_getpgrp:
        return virt$getpgrp();
    case SC_getpgid:
        return virt$getpgid(arg1);
    case SC_setpgid:
        return virt$setpgid(arg1, arg2);
    case SC_execve:
        return virt$execve(arg1);
    case SC_sigaction:
        return virt$sigaction(arg1, arg2, arg3);
    case SC_sigreturn:
        return virt$sigreturn();
    case SC_stat:
        return virt$stat(arg1);
    case SC_realpath:
        return virt$realpath(arg1);
    case SC_gethostname:
        return virt$gethostname(arg1, arg2);
    case SC_ioctl:
        return virt$ioctl(arg1, arg2, arg3);
    case SC_get_dir_entries:
        return virt$get_dir_entries(arg1, arg2, arg3);
    case SC_shbuf_create:
        return virt$shbuf_create(arg1, arg2);
    case SC_shbuf_allow_pid:
        return virt$shbuf_allow_pid(arg1, arg2);
    case SC_shbuf_allow_all:
        return virt$shbuf_allow_all(arg1);
    case SC_shbuf_get:
        return virt$shbuf_get(arg1, arg2);
    case SC_shbuf_release:
        return virt$shbuf_release(arg1);
    case SC_shbuf_seal:
        return virt$shbuf_seal(arg1);
    case SC_shbuf_set_volatile:
        return virt$shbuf_set_volatile(arg1, arg2);
    case SC_mmap:
        return virt$mmap(arg1);
    case SC_munmap:
        return virt$munmap(arg1, arg2);
    case SC_gettid:
        return virt$gettid();
    case SC_getpid:
        return virt$getpid();
    case SC_getsid:
        return virt$getsid(arg1);
    case SC_pledge:
        return virt$pledge(arg1);
    case SC_unveil:
        return virt$unveil(arg1);
    case SC_getuid:
        return virt$getuid();
    case SC_getgid:
        return virt$getgid();
    case SC_setuid:
        return virt$setuid(arg1);
    case SC_setgid:
        return virt$setgid(arg2);
    case SC_close:
        return virt$close(arg1);
    case SC_fstat:
        return virt$fstat(arg1, arg2);
    case SC_mkdir:
        return virt$mkdir(arg1, arg2, arg3);
    case SC_unlink:
        return virt$unlink(arg1, arg2);
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
    case SC_pipe:
        return virt$pipe(arg1, arg2);
    case SC_fcntl:
        return virt$fcntl(arg1, arg2, arg3);
    case SC_getgroups:
        return virt$getgroups(arg1, arg2);
    case SC_lseek:
        return virt$lseek(arg1, arg2, arg3);
    case SC_socket:
        return virt$socket(arg1, arg2, arg3);
    case SC_getsockopt:
        return virt$getsockopt(arg1);
    case SC_get_process_name:
        return virt$get_process_name(arg1, arg2);
    case SC_dbgputstr:
        return virt$dbgputstr(arg1, arg2);
    case SC_dbgputch:
        return virt$dbgputch(arg1);
    case SC_fchmod:
        return virt$fchmod(arg1, arg2);
    case SC_accept:
        return virt$accept(arg1, arg2, arg3);
    case SC_setsockopt:
        return virt$setsockopt(arg1);
    case SC_bind:
        return virt$bind(arg1, arg2, arg3);
    case SC_connect:
        return virt$connect(arg1, arg2, arg3);
    case SC_listen:
        return virt$listen(arg1, arg2);
    case SC_select:
        return virt$select(arg1);
    case SC_recvmsg:
        return virt$recvmsg(arg1, arg2, arg3);
    case SC_sendmsg:
        return virt$sendmsg(arg1, arg2, arg3);
    case SC_kill:
        return virt$kill(arg1, arg2);
    case SC_set_mmap_name:
        return virt$set_mmap_name(arg1);
    case SC_set_process_icon:
        return virt$set_process_icon(arg1);
    case SC_exit:
        virt$exit((int)arg1);
        return 0;
    case SC_gettimeofday:
        return virt$gettimeofday(arg1);
    case SC_clock_gettime:
        return virt$clock_gettime(arg1, arg2);
    case SC_getrandom:
        return virt$getrandom(arg1, arg2, arg3);
    case SC_fork:
        return virt$fork();
    default:
        warnln("\n=={}==  \033[31;1mUnimplemented syscall: {}\033[0m, {:p}", getpid(), Syscall::to_string((Syscall::Function)function), function);
        dump_backtrace();
        TODO();
    }
}

int Emulator::virt$shbuf_create(int size, FlatPtr buffer)
{
    u8* host_data = nullptr;
    int shbuf_id = syscall(SC_shbuf_create, size, &host_data);
    if (shbuf_id < 0)
        return shbuf_id;
    FlatPtr address = allocate_vm(size, PAGE_SIZE);
    auto region = SharedBufferRegion::create_with_shbuf_id(address, size, shbuf_id, host_data);
    m_mmu.add_region(move(region));
    m_mmu.copy_to_vm(buffer, &address, sizeof(address));
    return shbuf_id;
}

FlatPtr Emulator::virt$shbuf_get(int shbuf_id, FlatPtr size_ptr)
{
    size_t host_size = 0;
    void* host_data = (void*)syscall(SC_shbuf_get, shbuf_id, &host_size);
    if (host_data == (void*)-1)
        return (FlatPtr)host_data;
    FlatPtr address = allocate_vm(host_size, PAGE_SIZE);
    auto region = SharedBufferRegion::create_with_shbuf_id(address, host_size, shbuf_id, (u8*)host_data);
    m_mmu.add_region(move(region));
    m_mmu.copy_to_vm(size_ptr, &host_size, sizeof(host_size));
    return address;
}

int Emulator::virt$shbuf_allow_pid(int shbuf_id, pid_t peer_pid)
{
    auto* region = m_mmu.shbuf_region(shbuf_id);
    ASSERT(region);
    return region->allow_pid(peer_pid);
}

int Emulator::virt$shbuf_allow_all(int shbuf_id)
{
    auto* region = m_mmu.shbuf_region(shbuf_id);
    ASSERT(region);
    return region->allow_all();
}

int Emulator::virt$shbuf_release(int shbuf_id)
{
    auto* region = m_mmu.shbuf_region(shbuf_id);
    ASSERT(region);
    auto rc = region->release();
    m_mmu.remove_region(*region);
    return rc;
}

int Emulator::virt$shbuf_seal(int shbuf_id)
{
    auto* region = m_mmu.shbuf_region(shbuf_id);
    ASSERT(region);
    return region->seal();
}

int Emulator::virt$shbuf_set_volatile(int shbuf_id, bool is_volatile)
{
    auto* region = m_mmu.shbuf_region(shbuf_id);
    ASSERT(region);
    return region->set_volatile(is_volatile);
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

int Emulator::virt$mkdir(FlatPtr path, size_t path_length, mode_t mode)
{
    auto buffer = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_mkdir, buffer.data(), buffer.size(), mode);
}

int Emulator::virt$unlink(FlatPtr path, size_t path_length)
{
    auto buffer = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_unlink, buffer.data(), buffer.size());
}

int Emulator::virt$dbgputstr(FlatPtr characters, int length)
{
    auto buffer = mmu().copy_buffer_from_vm(characters, length);
    dbgputstr((const char*)buffer.data(), buffer.size());
    return 0;
}

int Emulator::virt$fchmod(int fd, mode_t mode)
{
    return syscall(SC_fchmod, fd, mode);
}

int Emulator::virt$setsockopt(FlatPtr params_addr)
{
    Syscall::SC_setsockopt_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    if (params.option == SO_RCVTIMEO || params.option == SO_TIMESTAMP) {
        auto host_value_buffer = ByteBuffer::create_zeroed(params.value_size);
        mmu().copy_from_vm(host_value_buffer.data(), (FlatPtr)params.value, params.value_size);
        int rc = setsockopt(params.sockfd, params.level, params.option, host_value_buffer.data(), host_value_buffer.size());
        if (rc < 0)
            return -errno;
        return rc;
    }

    TODO();
}

int Emulator::virt$accept(int sockfd, FlatPtr address, FlatPtr address_length)
{
    socklen_t host_address_length = 0;
    mmu().copy_from_vm(&host_address_length, address_length, sizeof(host_address_length));
    auto host_buffer = ByteBuffer::create_zeroed(host_address_length);
    int rc = syscall(SC_accept, sockfd, host_buffer.data(), &host_address_length);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(address, host_buffer.data(), min((socklen_t)host_buffer.size(), host_address_length));
    mmu().copy_to_vm(address_length, &host_address_length, sizeof(host_address_length));
    return rc;
}

int Emulator::virt$bind(int sockfd, FlatPtr address, socklen_t address_length)
{
    auto buffer = mmu().copy_buffer_from_vm(address, address_length);
    return syscall(SC_bind, sockfd, buffer.data(), buffer.size());
}

int Emulator::virt$connect(int sockfd, FlatPtr address, socklen_t address_size)
{
    auto buffer = mmu().copy_buffer_from_vm(address, address_size);
    return syscall(SC_connect, sockfd, buffer.data(), buffer.size());
}

int Emulator::virt$dbgputch(char ch)
{
    dbgputch(ch);
    return 0;
}

int Emulator::virt$listen(int fd, int backlog)
{
    return syscall(SC_listen, fd, backlog);
}

int Emulator::virt$kill(pid_t pid, int signal)
{
    return syscall(SC_kill, pid, signal);
}

int Emulator::virt$set_process_icon(int shbuf_id)
{
    return syscall(SC_set_process_icon, shbuf_id);
}

int Emulator::virt$gettimeofday(FlatPtr timeval)
{
    struct timeval host_timeval;
    int rc = syscall(SC_gettimeofday, &host_timeval);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(timeval, &host_timeval, sizeof(host_timeval));
    return rc;
}

int Emulator::virt$clock_gettime(int clockid, FlatPtr timespec)
{
    struct timespec host_timespec;
    int rc = syscall(SC_clock_gettime, clockid, &host_timespec);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(timespec, &host_timespec, sizeof(host_timespec));
    return rc;
}

int Emulator::virt$set_mmap_name(FlatPtr)
{
    // FIXME: Implement.
    return 0;
}

int Emulator::virt$get_process_name(FlatPtr buffer, int size)
{
    if (size < 0)
        return -EINVAL;
    auto host_buffer = ByteBuffer::create_zeroed((size_t)size);
    int rc = syscall(SC_get_process_name, host_buffer.data(), host_buffer.size());
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$lseek(int fd, off_t offset, int whence)
{
    return syscall(SC_lseek, fd, offset, whence);
}

int Emulator::virt$socket(int domain, int type, int protocol)
{
    return syscall(SC_socket, domain, type, protocol);
}

int Emulator::virt$recvmsg(int sockfd, FlatPtr msg_addr, int flags)
{
    msghdr mmu_msg;
    mmu().copy_from_vm(&mmu_msg, msg_addr, sizeof(mmu_msg));

    Vector<iovec, 1> mmu_iovs;
    mmu_iovs.resize(mmu_msg.msg_iovlen);
    mmu().copy_from_vm(mmu_iovs.data(), (FlatPtr)mmu_msg.msg_iov, mmu_msg.msg_iovlen * sizeof(iovec));
    Vector<ByteBuffer, 1> buffers;
    Vector<iovec, 1> iovs;
    for (const auto& iov : mmu_iovs) {
        buffers.append(ByteBuffer::create_uninitialized(iov.iov_len));
        iovs.append({ buffers.last().data(), buffers.last().size() });
    }

    ByteBuffer control_buffer;
    if (mmu_msg.msg_control)
        control_buffer = ByteBuffer::create_uninitialized(mmu_msg.msg_controllen);

    sockaddr_storage addr;
    msghdr msg = { &addr, sizeof(addr), iovs.data(), (int)iovs.size(), mmu_msg.msg_control ? control_buffer.data() : nullptr, mmu_msg.msg_controllen, mmu_msg.msg_flags };
    int rc = recvmsg(sockfd, &msg, flags);
    if (rc < 0)
        return -errno;

    for (size_t i = 0; i < buffers.size(); ++i)
        mmu().copy_to_vm((FlatPtr)mmu_iovs[i].iov_base, buffers[i].data(), mmu_iovs[i].iov_len);

    if (mmu_msg.msg_name)
        mmu().copy_to_vm((FlatPtr)mmu_msg.msg_name, &addr, min(sizeof(addr), (size_t)mmu_msg.msg_namelen));
    if (mmu_msg.msg_control)
        mmu().copy_to_vm((FlatPtr)mmu_msg.msg_control, control_buffer.data(), min(mmu_msg.msg_controllen, msg.msg_controllen));
    mmu_msg.msg_namelen = msg.msg_namelen;
    mmu_msg.msg_controllen = msg.msg_controllen;
    mmu_msg.msg_flags = msg.msg_flags;
    mmu().copy_to_vm(msg_addr, &mmu_msg, sizeof(mmu_msg));
    return rc;
}

int Emulator::virt$sendmsg(int sockfd, FlatPtr msg_addr, int flags)
{
    msghdr mmu_msg;
    mmu().copy_from_vm(&mmu_msg, msg_addr, sizeof(mmu_msg));

    Vector<iovec, 1> iovs;
    iovs.resize(mmu_msg.msg_iovlen);
    mmu().copy_from_vm(iovs.data(), (FlatPtr)mmu_msg.msg_iov, mmu_msg.msg_iovlen * sizeof(iovec));
    Vector<ByteBuffer, 1> buffers;
    for (auto& iov : iovs) {
        buffers.append(mmu().copy_buffer_from_vm((FlatPtr)iov.iov_base, iov.iov_len));
        iov = { buffers.last().data(), buffers.last().size() };
    }

    ByteBuffer control_buffer;
    if (mmu_msg.msg_control)
        control_buffer = ByteBuffer::create_uninitialized(mmu_msg.msg_controllen);

    sockaddr_storage address;
    socklen_t address_length = 0;
    if (mmu_msg.msg_name) {
        address_length = min(sizeof(address), (size_t)mmu_msg.msg_namelen);
        mmu().copy_from_vm(&address, (FlatPtr)mmu_msg.msg_name, address_length);
    }

    msghdr msg = { mmu_msg.msg_name ? &address : nullptr, address_length, iovs.data(), (int)iovs.size(), mmu_msg.msg_control ? control_buffer.data() : nullptr, mmu_msg.msg_controllen, mmu_msg.msg_flags };
    return sendmsg(sockfd, &msg, flags);
}

int Emulator::virt$select(FlatPtr params_addr)
{
    Syscall::SC_select_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    fd_set readfds {};
    fd_set writefds {};
    fd_set exceptfds {};
    struct timespec timeout;
    u32 sigmask;

    if (params.readfds)
        mmu().copy_from_vm(&readfds, (FlatPtr)params.readfds, sizeof(readfds));
    if (params.writefds)
        mmu().copy_from_vm(&writefds, (FlatPtr)params.writefds, sizeof(writefds));
    if (params.exceptfds)
        mmu().copy_from_vm(&exceptfds, (FlatPtr)params.exceptfds, sizeof(exceptfds));
    if (params.timeout)
        mmu().copy_from_vm(&timeout, (FlatPtr)params.timeout, sizeof(timeout));
    if (params.sigmask)
        mmu().copy_from_vm(&sigmask, (FlatPtr)params.sigmask, sizeof(sigmask));

    int rc = pselect(params.nfds, &readfds, &writefds, &exceptfds, params.timeout ? &timeout : nullptr, params.sigmask ? &sigmask : nullptr);
    if (rc < 0)
        return -errno;

    if (params.readfds)
        mmu().copy_to_vm((FlatPtr)params.readfds, &readfds, sizeof(readfds));
    if (params.writefds)
        mmu().copy_to_vm((FlatPtr)params.writefds, &writefds, sizeof(writefds));
    if (params.exceptfds)
        mmu().copy_to_vm((FlatPtr)params.exceptfds, &exceptfds, sizeof(exceptfds));
    if (params.timeout)
        mmu().copy_to_vm((FlatPtr)params.timeout, &timeout, sizeof(timeout));

    return rc;
}

int Emulator::virt$getsockopt(FlatPtr params_addr)
{
    Syscall::SC_getsockopt_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    if (params.option == SO_PEERCRED) {
        struct ucred creds = {};
        socklen_t creds_size = sizeof(creds);
        int rc = getsockopt(params.sockfd, params.level, SO_PEERCRED, &creds, &creds_size);
        if (rc < 0)
            return -errno;
        // FIXME: Check params.value_size
        mmu().copy_to_vm((FlatPtr)params.value, &creds, sizeof(creds));
        return rc;
    }

    TODO();
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

int Emulator::virt$pipe(FlatPtr vm_pipefd, int flags)
{
    int pipefd[2];
    int rc = syscall(SC_pipe, pipefd, flags);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(vm_pipefd, pipefd, sizeof(pipefd));
    return rc;
}

u32 Emulator::virt$munmap(FlatPtr address, u32 size)
{
    auto* region = mmu().find_region({ 0x20, address });
    ASSERT(region);
    if (region->size() != round_up_to_power_of_two(size, PAGE_SIZE))
        TODO();
    mmu().remove_region(*region);
    return 0;
}

FlatPtr Emulator::allocate_vm(size_t size, size_t alignment)
{
    // FIXME: Write a proper VM allocator
    static FlatPtr next_address = 0x30000000;

    FlatPtr final_address;

    if (alignment) {
        // FIXME: What if alignment is not a power of 2?
        final_address = round_up_to_power_of_two(next_address, alignment);
    } else {
        final_address = next_address;
    }

    next_address = final_address + size;
    return final_address;
}

u32 Emulator::virt$mmap(u32 params_addr)
{
    Syscall::SC_mmap_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    ASSERT(params.addr == 0);

    u32 final_size = round_up_to_power_of_two(params.size, PAGE_SIZE);
    u32 final_address = allocate_vm(final_size, params.alignment);

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

int Emulator::virt$setuid(uid_t uid)
{
    return syscall(SC_setuid, uid);
}

int Emulator::virt$setgid(gid_t gid)
{
    return syscall(SC_setgid, gid);
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
    if (nread < 0) {
        if (nread == -EPERM) {
            dump_backtrace();
            TODO();
        }
        return nread;
    }
    mmu().copy_to_vm(buffer, local_buffer.data(), local_buffer.size());
    return nread;
}

void Emulator::virt$exit(int status)
{
    warnln("\n=={}==  \033[33;1mSyscall: exit({})\033[0m, shutting down!", getpid(), status);
    m_exit_status = status;
    m_shutdown = true;
}

ssize_t Emulator::virt$getrandom(FlatPtr buffer, size_t buffer_size, unsigned int flags)
{
    auto host_buffer = ByteBuffer::create_uninitialized(buffer_size);
    int rc = syscall(SC_getrandom, host_buffer.data(), host_buffer.size(), flags);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$get_dir_entries(int fd, FlatPtr buffer, ssize_t size)
{
    auto host_buffer = ByteBuffer::create_uninitialized(size);
    int rc = syscall(SC_get_dir_entries, fd, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$ioctl(int fd, unsigned request, FlatPtr arg)
{
    (void)fd;
    (void)arg;
    if (request == TIOCGWINSZ) {
        struct winsize ws;
        int rc = syscall(SC_ioctl, fd, TIOCGWINSZ, &ws);
        if (rc < 0)
            return rc;
        mmu().copy_to_vm(arg, &ws, sizeof(winsize));
        return 0;
    }
    if (request == TIOCSPGRP) {
        return syscall(SC_ioctl, fd, request, arg);
    }
    if (request == TCGETS) {
        struct termios termios;
        int rc = syscall(SC_ioctl, fd, request, &termios);
        if (rc < 0)
            return rc;
        mmu().copy_to_vm(arg, &termios, sizeof(termios));
        return rc;
    }
    if (request == TCSETS) {
        struct termios termios;
        mmu().copy_from_vm(&termios, arg, sizeof(termios));
        return syscall(SC_ioctl, fd, request, &termios);
    }
    dbgln("Unsupported ioctl: {}", request);
    dump_backtrace();
    TODO();
}

int Emulator::virt$fork()
{
    int rc = fork();
    if (rc < 0)
        return -errno;
    return rc;
}

int Emulator::virt$execve(FlatPtr params_addr)
{
    Syscall::SC_execve_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = String::copy(mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length));
    Vector<String> arguments;
    Vector<String> environment;

    auto copy_string_list = [this](auto& output_vector, auto& string_list) {
        for (size_t i = 0; i < string_list.length; ++i) {
            Syscall::StringArgument string;
            mmu().copy_from_vm(&string, (FlatPtr)&string_list.strings[i], sizeof(string));
            output_vector.append(String::copy(mmu().copy_buffer_from_vm((FlatPtr)string.characters, string.length)));
        }
    };

    copy_string_list(arguments, params.arguments);
    copy_string_list(environment, params.environment);

    warnln("\n=={}==  \033[33;1mSyscall:\033[0m execve", getpid());
    warnln("=={}==  @ {}", getpid(), path);
    for (auto& argument : arguments)
        warnln("=={}==    - {}", getpid(), argument);

    Vector<char*> argv;
    Vector<char*> envp;

    argv.append(const_cast<char*>("/bin/UserspaceEmulator"));
    argv.append(const_cast<char*>(path.characters()));

    auto create_string_vector = [](auto& output_vector, auto& input_vector) {
        for (auto& string : input_vector)
            output_vector.append(const_cast<char*>(string.characters()));
        output_vector.append(nullptr);
    };

    create_string_vector(argv, arguments);
    create_string_vector(envp, environment);

    // Yoink duplicated program name.
    argv.remove(2);

    return execve(argv[0], (char* const*)argv.data(), (char* const*)envp.data());
}

int Emulator::virt$stat(FlatPtr params_addr)
{
    Syscall::SC_stat_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = String::copy(mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length));
    struct stat host_statbuf;
    int rc;
    if (params.follow_symlinks)
        rc = stat(path.characters(), &host_statbuf);
    else
        rc = lstat(path.characters(), &host_statbuf);
    if (rc < 0)
        return -errno;
    mmu().copy_to_vm((FlatPtr)params.statbuf, &host_statbuf, sizeof(host_statbuf));
    return rc;
}

int Emulator::virt$realpath(FlatPtr params_addr)
{
    Syscall::SC_realpath_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = String::copy(mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length));
    char host_buffer[PATH_MAX] = {};

    Syscall::SC_realpath_params host_params;
    host_params.path = { path.characters(), path.length() };
    host_params.buffer = { host_buffer, sizeof(host_buffer) };
    int rc = syscall(SC_realpath, &host_params);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm((FlatPtr)params.buffer.data, host_buffer, min(params.buffer.size, sizeof(host_buffer)));
    return rc;
}

int Emulator::virt$gethostname(FlatPtr buffer, ssize_t buffer_size)
{
    if (buffer_size < 0)
        return -EINVAL;
    auto host_buffer = ByteBuffer::create_zeroed(buffer_size);
    int rc = syscall(SC_gethostname, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

static void emulator_signal_handler(int signum)
{
    Emulator::the().did_receive_signal(signum);
}

void Emulator::register_signal_handlers()
{
    for (int signum = 0; signum < NSIG; ++signum)
        signal(signum, emulator_signal_handler);
}

int Emulator::virt$sigaction(int signum, FlatPtr act, FlatPtr oldact)
{
    if (signum == SIGKILL) {
        dbg() << "Attempted to sigaction() with SIGKILL";
        return -EINVAL;
    }

    if (signum <= 0 || signum >= NSIG)
        return -EINVAL;

    struct sigaction host_act;
    mmu().copy_from_vm(&host_act, act, sizeof(host_act));

    auto& handler = m_signal_handler[signum];
    handler.handler = (FlatPtr)host_act.sa_handler;
    handler.mask = host_act.sa_mask;
    handler.flags = host_act.sa_flags;

    if (oldact) {
        struct sigaction host_oldact;
        auto& old_handler = m_signal_handler[signum];
        host_oldact.sa_handler = (void (*)(int))(old_handler.handler);
        host_oldact.sa_mask = old_handler.mask;
        host_oldact.sa_flags = old_handler.flags;
        mmu().copy_to_vm(oldact, &host_oldact, sizeof(host_oldact));
    }
    return 0;
}

int Emulator::virt$sigreturn()
{
    u32 stack_ptr = m_cpu.esp().value();
    auto local_pop = [&]() -> ValueWithShadow<u32> {
        auto value = m_cpu.read_memory32({ m_cpu.ss(), stack_ptr });
        stack_ptr += sizeof(u32);
        return value;
    };

    auto smuggled_eax = local_pop();

    stack_ptr += 4 * sizeof(u32);

    m_signal_mask = local_pop().value();

    m_cpu.set_edi(local_pop());
    m_cpu.set_esi(local_pop());
    m_cpu.set_ebp(local_pop());
    m_cpu.set_esp(local_pop());
    m_cpu.set_ebx(local_pop());
    m_cpu.set_edx(local_pop());
    m_cpu.set_ecx(local_pop());
    m_cpu.set_eax(local_pop());

    m_cpu.set_eip(local_pop().value());
    m_cpu.set_eflags(local_pop());

    // FIXME: We're losing shadow bits here.
    return smuggled_eax.value();
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

static DefaultSignalAction default_signal_action(int signal)
{
    ASSERT(signal && signal < NSIG);

    switch (signal) {
    case SIGHUP:
    case SIGINT:
    case SIGKILL:
    case SIGPIPE:
    case SIGALRM:
    case SIGUSR1:
    case SIGUSR2:
    case SIGVTALRM:
    case SIGSTKFLT:
    case SIGIO:
    case SIGPROF:
    case SIGTERM:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
    case SIGINFO:
        return DefaultSignalAction::Ignore;
    case SIGQUIT:
    case SIGILL:
    case SIGTRAP:
    case SIGABRT:
    case SIGBUS:
    case SIGFPE:
    case SIGSEGV:
    case SIGXCPU:
    case SIGXFSZ:
    case SIGSYS:
        return DefaultSignalAction::DumpCore;
    case SIGCONT:
        return DefaultSignalAction::Continue;
    case SIGSTOP:
    case SIGTSTP:
    case SIGTTIN:
    case SIGTTOU:
        return DefaultSignalAction::Stop;
    }
    ASSERT_NOT_REACHED();
}

void Emulator::dispatch_one_pending_signal()
{
    int signum = -1;
    for (signum = 1; signum < NSIG; ++signum) {
        int mask = 1 << signum;
        if (m_pending_signals & mask)
            break;
    }
    ASSERT(signum != -1);
    m_pending_signals &= ~(1 << signum);

    auto& handler = m_signal_handler[signum];

    if (handler.handler == 0) {
        // SIG_DFL
        auto action = default_signal_action(signum);
        if (action == DefaultSignalAction::Ignore)
            return;
        warnln("\n=={}== Got signal {} ({}), no handler registered", getpid(), signum, strsignal(signum));
        m_shutdown = true;
        return;
    }

    if (handler.handler == 1) {
        // SIG_IGN
        return;
    }

    warnln("\n=={}== Got signal {} ({}), handler at {:p}", getpid(), signum, strsignal(signum), handler.handler);

    auto old_esp = m_cpu.esp();

    u32 stack_alignment = (m_cpu.esp().value() - 56) % 16;
    m_cpu.set_esp(shadow_wrap_as_initialized(m_cpu.esp().value() - stack_alignment));

    m_cpu.push32(shadow_wrap_as_initialized(m_cpu.eflags()));
    m_cpu.push32(shadow_wrap_as_initialized(m_cpu.eip()));
    m_cpu.push32(m_cpu.eax());
    m_cpu.push32(m_cpu.ecx());
    m_cpu.push32(m_cpu.edx());
    m_cpu.push32(m_cpu.ebx());
    m_cpu.push32(old_esp);
    m_cpu.push32(m_cpu.ebp());
    m_cpu.push32(m_cpu.esi());
    m_cpu.push32(m_cpu.edi());

    // FIXME: Push old signal mask here.
    m_cpu.push32(shadow_wrap_as_initialized(0u));

    m_cpu.push32(shadow_wrap_as_initialized((u32)signum));
    m_cpu.push32(shadow_wrap_as_initialized(handler.handler));
    m_cpu.push32(shadow_wrap_as_initialized(0u));

    ASSERT((m_cpu.esp().value() % 16) == 0);

    m_cpu.set_eip(m_signal_trampoline);
}

// Make sure the compiler doesn't "optimize away" this function:
extern void signal_trampoline_dummy(void);
void signal_trampoline_dummy(void)
{
    // The trampoline preserves the current eax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // necessary to preserve it here.
    asm(
        ".intel_syntax noprefix\n"
        "asm_signal_trampoline:\n"
        "push ebp\n"
        "mov ebp, esp\n"
        "push eax\n"          // we have to store eax 'cause it might be the return value from a syscall
        "sub esp, 4\n"        // align the stack to 16 bytes
        "mov eax, [ebp+12]\n" // push the signal code
        "push eax\n"
        "call [ebp+8]\n" // call the signal handler
        "add esp, 8\n"
        "mov eax, %P0\n"
        "int 0x82\n" // sigreturn syscall
        "asm_signal_trampoline_end:\n"
        ".att_syntax" ::"i"(Syscall::SC_sigreturn));
}

extern "C" void asm_signal_trampoline(void);
extern "C" void asm_signal_trampoline_end(void);

void Emulator::setup_signal_trampoline()
{
    auto trampoline_region = make<SimpleRegion>(0xb0000000, 4096);

    u8* trampoline = (u8*)asm_signal_trampoline;
    u8* trampoline_end = (u8*)asm_signal_trampoline_end;
    size_t trampoline_size = trampoline_end - trampoline;

    u8* code_ptr = trampoline_region->data();
    memcpy(code_ptr, trampoline, trampoline_size);

    m_signal_trampoline = trampoline_region->base();
    mmu().add_region(move(trampoline_region));
}

int Emulator::virt$getpgrp()
{
    return syscall(SC_getpgrp);
}

int Emulator::virt$getpgid(pid_t pid)
{
    return syscall(SC_getpgid, pid);
}

int Emulator::virt$setpgid(pid_t pid, pid_t pgid)
{
    return syscall(SC_setpgid, pid, pgid);
}

int Emulator::virt$ttyname(int fd, FlatPtr buffer, size_t buffer_size)
{
    auto host_buffer = ByteBuffer::create_zeroed(buffer_size);
    int rc = syscall(SC_ttyname, fd, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$getcwd(FlatPtr buffer, size_t buffer_size)
{
    auto host_buffer = ByteBuffer::create_zeroed(buffer_size);
    int rc = syscall(SC_getcwd, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$getsid(pid_t pid)
{
    return syscall(SC_getsid, pid);
}

int Emulator::virt$access(FlatPtr path, size_t path_length, int type)
{
    auto host_path = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_access, host_path.data(), host_path.size(), type);
}

int Emulator::virt$waitid(FlatPtr params_addr)
{
    Syscall::SC_waitid_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    Syscall::SC_waitid_params host_params = params;
    siginfo info;
    host_params.infop = &info;

    int rc = syscall(SC_waitid, &host_params);
    if (rc < 0)
        return rc;

    if (info.si_addr) {
        // FIXME: Translate this somehow.
        TODO();
    }

    if (params.infop)
        mmu().copy_to_vm((FlatPtr)params.infop, &info, sizeof(info));

    return rc;
}

int Emulator::virt$chdir(FlatPtr path, size_t path_length)
{
    auto host_path = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_chdir, host_path.data(), host_path.size());
}

int Emulator::virt$dup2(int old_fd, int new_fd)
{
    return syscall(SC_dup2, old_fd, new_fd);
}

}
