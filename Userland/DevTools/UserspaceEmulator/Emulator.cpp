/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/Random.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <LibPthread/pthread.h>
#include <LibX86/ELFSymbolProvider.h>
#include <fcntl.h>
#include <net/if.h>
#include <net/route.h>
#include <sched.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <syscall.h>
#include <termios.h>
#include <unistd.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace UserspaceEmulator {

static constexpr u32 stack_location = 0x10000000;
static constexpr size_t stack_size = 64 * KiB;

static Emulator* s_the;

Emulator& Emulator::the()
{
    VERIFY(s_the);
    return *s_the;
}

Emulator::Emulator(const String& executable_path, const Vector<String>& arguments, const Vector<String>& environment)
    : m_executable_path(executable_path)
    , m_arguments(arguments)
    , m_environment(environment)
    , m_mmu(*this)
    , m_cpu(*this)
{
    m_malloc_tracer = make<MallocTracer>(*this);

    static constexpr FlatPtr userspace_range_base = 0x00800000;
    static constexpr FlatPtr userspace_range_ceiling = 0xbe000000;
#ifdef UE_ASLR
    static constexpr FlatPtr page_mask = 0xfffff000u;
    size_t random_offset = (get_random<u8>() % 32 * MiB) & page_mask;
    FlatPtr base = userspace_range_base + random_offset;
#else
    FlatPtr base = userspace_range_base;
#endif

    m_range_allocator.initialize_with_range(VirtualAddress(base), userspace_range_ceiling - base);

    VERIFY(!s_the);
    s_the = this;
    // setup_stack(arguments, environment);
    register_signal_handlers();
    setup_signal_trampoline();
}

Vector<ELF::AuxiliaryValue> Emulator::generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, String executable_path, int executable_fd) const
{
    // FIXME: This is not fully compatible with the auxiliary vector the kernel generates, this is just the bare
    //        minimum to get the loader going.
    Vector<ELF::AuxiliaryValue> auxv;
    // PHDR/EXECFD
    // PH*
    auxv.append({ ELF::AuxiliaryValue::PageSize, PAGE_SIZE });
    auxv.append({ ELF::AuxiliaryValue::BaseAddress, (void*)load_base });

    auxv.append({ ELF::AuxiliaryValue::Entry, (void*)entry_eip });

    // FIXME: Don't hard code this? We might support other platforms later.. (e.g. x86_64)
    auxv.append({ ELF::AuxiliaryValue::Platform, "i386" });

    auxv.append({ ELF::AuxiliaryValue::ExecFilename, executable_path });

    auxv.append({ ELF::AuxiliaryValue::ExecFileDescriptor, executable_fd });

    auxv.append({ ELF::AuxiliaryValue::Null, 0L });
    return auxv;
}

void Emulator::setup_stack(Vector<ELF::AuxiliaryValue> aux_vector)
{
    auto stack_region = make<SimpleRegion>(stack_location, stack_size);
    stack_region->set_stack(true);
    m_mmu.add_region(move(stack_region));
    m_cpu.set_esp(shadow_wrap_as_initialized<u32>(stack_location + stack_size));

    Vector<u32> argv_entries;

    for (auto& argument : m_arguments) {
        m_cpu.push_string(argument.characters());
        argv_entries.append(m_cpu.esp().value());
    }

    Vector<u32> env_entries;

    for (auto& variable : m_environment) {
        m_cpu.push_string(variable.characters());
        env_entries.append(m_cpu.esp().value());
    }

    for (auto& auxv : aux_vector) {
        if (!auxv.optional_string.is_empty()) {
            m_cpu.push_string(auxv.optional_string.characters());
            auxv.auxv.a_un.a_ptr = (void*)m_cpu.esp().value();
        }
    }

    for (ssize_t i = aux_vector.size() - 1; i >= 0; --i) {
        auto& value = aux_vector[i].auxv;
        m_cpu.push_buffer((const u8*)&value, sizeof(value));
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
    auto file_or_error = MappedFile::map(m_executable_path);
    if (file_or_error.is_error()) {
        reportln("Unable to map {}: {}", m_executable_path, file_or_error.error());
        return false;
    }

    auto elf_image_data = file_or_error.value()->bytes();
    ELF::Image executable_elf(elf_image_data);

    if (!executable_elf.is_dynamic()) {
        // FIXME: Support static objects
        VERIFY_NOT_REACHED();
    }

    String interpreter_path;
    if (!ELF::validate_program_headers(*(const Elf32_Ehdr*)elf_image_data.data(), elf_image_data.size(), (const u8*)elf_image_data.data(), elf_image_data.size(), &interpreter_path)) {
        reportln("failed to validate ELF file");
        return false;
    }

    VERIFY(!interpreter_path.is_null());
    dbgln("interpreter: {}", interpreter_path);

    auto interpreter_file_or_error = MappedFile::map(interpreter_path);
    VERIFY(!interpreter_file_or_error.is_error());
    auto interpreter_image_data = interpreter_file_or_error.value()->bytes();
    ELF::Image interpreter_image(interpreter_image_data);

    constexpr FlatPtr interpreter_load_offset = 0x08000000;
    interpreter_image.for_each_program_header([&](const ELF::Image::ProgramHeader& program_header) {
        // Loader is not allowed to have its own TLS regions
        VERIFY(program_header.type() != PT_TLS);

        if (program_header.type() == PT_LOAD) {
            auto region = make<SimpleRegion>(program_header.vaddr().offset(interpreter_load_offset).get(), program_header.size_in_memory());
            if (program_header.is_executable() && !program_header.is_writable())
                region->set_text(true);
            memcpy(region->data(), program_header.raw_data(), program_header.size_in_image());
            memset(region->shadow_data(), 0x01, program_header.size_in_memory());
            if (program_header.is_executable()) {
                m_loader_text_base = region->base();
                m_loader_text_size = region->size();
            }
            mmu().add_region(move(region));
            return IterationDecision::Continue;
        }

        return IterationDecision::Continue;
    });

    auto entry_point = interpreter_image.entry().offset(interpreter_load_offset).get();
    m_cpu.set_eip(entry_point);

    // executable_fd will be used by the loader
    int executable_fd = open(m_executable_path.characters(), O_RDONLY);
    if (executable_fd < 0)
        return false;

    auto aux_vector = generate_auxiliary_vector(interpreter_load_offset, entry_point, m_executable_path, executable_fd);
    setup_stack(move(aux_vector));

    return true;
}

int Emulator::exec()
{
    // X86::ELFSymbolProvider symbol_provider(*m_elf);
    X86::ELFSymbolProvider* symbol_provider = nullptr;

    bool trace = false;

    while (!m_shutdown) {
        m_cpu.save_base_eip();

        auto insn = X86::Instruction::from_stream(m_cpu, true, true);

        if (trace)
            outln("{:p}  \033[33;1m{}\033[0m", m_cpu.base_eip(), insn.to_string(m_cpu.base_eip(), symbol_provider));

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

Vector<FlatPtr> Emulator::raw_backtrace()
{
    Vector<FlatPtr, 128> backtrace;
    backtrace.append(m_cpu.base_eip());

    // FIXME: Maybe do something if the backtrace has uninitialized data in the frame chain.

    u32 frame_ptr = m_cpu.ebp().value();
    while (frame_ptr) {
        u32 ret_ptr = m_mmu.read32({ 0x23, frame_ptr + 4 }).value();
        if (!ret_ptr)
            break;
        backtrace.append(ret_ptr);
        frame_ptr = m_mmu.read32({ 0x23, frame_ptr }).value();
    }
    return backtrace;
}

const MmapRegion* Emulator::find_text_region(FlatPtr address)
{
    const MmapRegion* matching_region = nullptr;
    mmu().for_each_region([&](auto& region) {
        if (!is<MmapRegion>(region))
            return IterationDecision::Continue;
        const auto& mmap_region = static_cast<const MmapRegion&>(region);
        if (!(mmap_region.is_executable() && address >= mmap_region.base() && address < mmap_region.base() + mmap_region.size()))
            return IterationDecision::Continue;
        matching_region = &mmap_region;
        return IterationDecision::Break;
    });
    return matching_region;
}

String Emulator::create_backtrace_line(FlatPtr address)
{
    String minimal = String::format("=={%d}==    %p", getpid(), (void*)address);
    const auto* region = find_text_region(address);
    if (!region)
        return minimal;
    auto separator_index = region->name().index_of(":");
    if (!separator_index.has_value())
        return minimal;

    String lib_name = region->name().substring(0, separator_index.value());
    String lib_path = lib_name;
    if (region->name().contains(".so"))
        lib_path = String::formatted("/usr/lib/{}", lib_path);

    if (!m_dynamic_library_cache.contains(lib_path)) {
        auto file_or_error = MappedFile::map(lib_path);
        if (file_or_error.is_error())
            return minimal;

        auto debug_info = make<Debug::DebugInfo>(make<ELF::Image>(file_or_error.value()->bytes()));
        m_dynamic_library_cache.set(lib_path, CachedELF { file_or_error.release_value(), move(debug_info) });
    }

    auto it = m_dynamic_library_cache.find(lib_path);
    auto& elf = it->value.debug_info->elf();
    String symbol = elf.symbolicate(address - region->base());

    auto line_without_source_info = String::format("=={%d}==    %p  [%s]: %s", getpid(), (void*)address, lib_name.characters(), symbol.characters());

    auto source_position = it->value.debug_info->get_source_position(address - region->base());
    if (source_position.has_value())
        return String::format("=={%d}==    %p  [%s]: %s (\033[34;1m%s\033[0m:%zu)", getpid(), (void*)address, lib_name.characters(), symbol.characters(), LexicalPath(source_position.value().file_path).basename().characters(), source_position.value().line_number);

    return line_without_source_info;
}

void Emulator::dump_backtrace(const Vector<FlatPtr>& backtrace)
{
    for (auto& address : backtrace) {
        reportln("{}", create_backtrace_line(address));
    }
}

void Emulator::dump_backtrace()
{
    dump_backtrace(raw_backtrace());
}

u32 Emulator::virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3)
{
#if SPAM_DEBUG
    reportln("Syscall: {} ({:x})", Syscall::to_string((Syscall::Function)function), function);
#endif
    switch (function) {
    case SC_chdir:
        return virt$chdir(arg1, arg2);
    case SC_dup2:
        return virt$dup2(arg1, arg2);
    case SC_get_stack_bounds:
        return virt$get_stack_bounds(arg1, arg2);
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
    case SC_profiling_enable:
        return virt$profiling_enable(arg1);
    case SC_profiling_disable:
        return virt$profiling_disable(arg1);
    case SC_disown:
        return virt$disown(arg1);
    case SC_purge:
        return virt$purge(arg1);
    case SC_mmap:
        return virt$mmap(arg1);
    case SC_mount:
        return virt$mount(arg1);
    case SC_munmap:
        return virt$munmap(arg1, arg2);
    case SC_mremap:
        return virt$mremap(arg1);
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
    case SC_geteuid:
        return virt$geteuid();
    case SC_getgid:
        return virt$getgid();
    case SC_getegid:
        return virt$getegid();
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
    case SC_anon_create:
        return virt$anon_create(arg1, arg2);
    case SC_sendfd:
        return virt$sendfd(arg1, arg2);
    case SC_recvfd:
        return virt$recvfd(arg1, arg2);
    case SC_open:
        return virt$open(arg1);
    case SC_pipe:
        return virt$pipe(arg1, arg2);
    case SC_fcntl:
        return virt$fcntl(arg1, arg2, arg3);
    case SC_getgroups:
        return virt$getgroups(arg1, arg2);
    case SC_setgroups:
        return virt$setgroups(arg1, arg2);
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
    case SC_chmod:
        return virt$chmod(arg1, arg2, arg3);
    case SC_fchmod:
        return virt$fchmod(arg1, arg2);
    case SC_fchown:
        return virt$fchown(arg1, arg2, arg3);
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
    case SC_exit:
        virt$exit((int)arg1);
        return 0;
    case SC_gettimeofday:
        return virt$gettimeofday(arg1);
    case SC_clock_gettime:
        return virt$clock_gettime(arg1, arg2);
    case SC_clock_settime:
        return virt$clock_settime(arg1, arg2);
    case SC_getrandom:
        return virt$getrandom(arg1, arg2, arg3);
    case SC_fork:
        return virt$fork();
    case SC_emuctl:
        return virt$emuctl(arg1, arg2, arg3);
    case SC_sched_getparam:
        return virt$sched_getparam(arg1, arg2);
    case SC_sched_setparam:
        return virt$sched_setparam(arg1, arg2);
    case SC_set_thread_name:
        return virt$set_thread_name(arg1, arg2, arg3);
    case SC_setsid:
        return virt$setsid();
    case SC_watch_file:
        return virt$watch_file(arg1, arg2);
    case SC_clock_nanosleep:
        return virt$clock_nanosleep(arg1);
    case SC_readlink:
        return virt$readlink(arg1);
    case SC_ptsname:
        return virt$ptsname(arg1, arg2, arg3);
    case SC_allocate_tls:
        return virt$allocate_tls(arg1);
    case SC_beep:
        return virt$beep();
    case SC_ftruncate:
        return virt$ftruncate(arg1, arg2);
    case SC_umask:
        return virt$umask(arg1);
    case SC_chown:
        return virt$chown(arg1);
    case SC_msyscall:
        return virt$msyscall(arg1);
    default:
        reportln("\n=={}==  \033[31;1mUnimplemented syscall: {}\033[0m, {:p}", getpid(), Syscall::to_string((Syscall::Function)function), function);
        dump_backtrace();
        TODO();
    }
}

int Emulator::virt$anon_create(size_t size, int options)
{
    return syscall(SC_anon_create, size, options);
}

int Emulator::virt$sendfd(int socket, int fd)
{
    return syscall(SC_sendfd, socket, fd);
}

int Emulator::virt$recvfd(int socket, int options)
{
    return syscall(SC_recvfd, socket, options);
}

int Emulator::virt$profiling_enable(pid_t pid)
{
    return syscall(SC_profiling_enable, pid);
}

int Emulator::virt$profiling_disable(pid_t pid)
{
    return syscall(SC_profiling_disable, pid);
}

int Emulator::virt$disown(pid_t pid)
{
    return syscall(SC_disown, pid);
}

int Emulator::virt$purge(int mode)
{
    return syscall(SC_purge, mode);
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

int Emulator::virt$chmod(FlatPtr path_addr, size_t path_length, mode_t mode)
{
    auto path = mmu().copy_buffer_from_vm(path_addr, path_length);
    return syscall(SC_chmod, path.data(), path.size(), mode);
}

int Emulator::virt$chown(FlatPtr params_addr)
{
    Syscall::SC_chown_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);
    params.path.characters = (const char*)path.data();
    params.path.length = path.size();

    return syscall(SC_chown, &params);
}

int Emulator::virt$fchmod(int fd, mode_t mode)
{
    return syscall(SC_fchmod, fd, mode);
}

int Emulator::virt$fchown(int fd, uid_t uid, gid_t gid)
{
    return syscall(SC_fchown, fd, uid, gid);
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

    if (params.option == SO_BINDTODEVICE) {
        auto ifname = mmu().copy_buffer_from_vm((FlatPtr)params.value, params.value_size);
        params.value = ifname.data();
        params.value_size = ifname.size();
        return syscall(SC_setsockopt, &params);
    }

    TODO();
}

int Emulator::virt$get_stack_bounds(FlatPtr base, FlatPtr size)
{
    auto* region = mmu().find_region({ m_cpu.ss(), m_cpu.esp().value() });
    FlatPtr b = region->base();
    size_t s = region->size();
    mmu().copy_to_vm(base, &b, sizeof(b));
    mmu().copy_to_vm(size, &s, sizeof(s));
    return 0;
}

int Emulator::virt$ftruncate(int fd, off_t length)
{
    return syscall(SC_ftruncate, fd, length);
}

mode_t Emulator::virt$umask(mode_t mask)
{
    return syscall(SC_umask, mask);
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

int Emulator::virt$clock_settime(uint32_t clock_id, FlatPtr user_ts)
{
    struct timespec user_timespec;
    mmu().copy_from_vm(&user_timespec, user_ts, sizeof(user_timespec));
    int rc = syscall(SC_clock_settime, clock_id, &user_timespec);
    return rc;
}

int Emulator::virt$set_mmap_name(FlatPtr params_addr)
{
    Syscall::SC_set_mmap_name_params params {};
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    auto name = mmu().copy_buffer_from_vm((FlatPtr)params.name.characters, params.name.length);

    auto* region = mmu().find_region({ 0x23, (FlatPtr)params.addr });
    if (!region || !is<MmapRegion>(*region))
        return -EINVAL;
    static_cast<MmapRegion&>(*region).set_name(String::copy(name));
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

int Emulator::virt$setgroups(ssize_t count, FlatPtr groups)
{
    if (!count)
        return syscall(SC_setgroups, 0, nullptr);

    auto buffer = mmu().copy_buffer_from_vm(groups, count * sizeof(gid_t));
    return syscall(SC_setgroups, count, buffer.data());
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

    Syscall::SC_open_params host_params {};
    host_params.dirfd = params.dirfd;
    host_params.mode = params.mode;
    host_params.options = params.options;
    host_params.path.characters = (const char*)path.data();
    host_params.path.length = path.size();

    return syscall(SC_open, &host_params);
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

static void round_to_page_size(FlatPtr& address, size_t& size)
{
    auto new_end = round_up_to_power_of_two(address + size, PAGE_SIZE);
    address &= ~(PAGE_SIZE - 1);
    size = new_end - address;
}

u32 Emulator::virt$munmap(FlatPtr address, size_t size)
{
    round_to_page_size(address, size);
    Vector<Region*, 4> marked_for_deletion;
    bool has_non_mmap_region = false;
    mmu().for_regions_in({ 0x23, address }, size, [&](Region* region) {
        if (region) {
            if (!is<MmapRegion>(*region)) {
                has_non_mmap_region = true;
                return IterationDecision::Break;
            }
            marked_for_deletion.append(region);
        }
        return IterationDecision::Continue;
    });
    if (has_non_mmap_region)
        return -EINVAL;

    for (Region* region : marked_for_deletion) {
        m_range_allocator.deallocate(region->range());
        mmu().remove_region(*region);
    }
    return 0;
}

u32 Emulator::virt$mmap(u32 params_addr)
{
    Syscall::SC_mmap_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    u32 requested_size = round_up_to_power_of_two(params.size, PAGE_SIZE);
    FlatPtr final_address;

    Optional<Range> result;
    if (params.flags & MAP_RANDOMIZED) {
        result = m_range_allocator.allocate_randomized(requested_size, params.alignment);
    } else if (params.flags & MAP_FIXED) {
        result = m_range_allocator.allocate_specific(VirtualAddress { params.addr }, requested_size);
    } else {
        result = m_range_allocator.allocate_anywhere(requested_size, params.alignment);
    }
    if (!result.has_value())
        return -ENOMEM;
    final_address = result.value().base().get();
    auto final_size = result.value().size();

    if (params.flags & MAP_ANONYMOUS)
        mmu().add_region(MmapRegion::create_anonymous(final_address, final_size, params.prot));
    else {
        String name_str;
        if (params.name.characters) {
            auto name = ByteBuffer::create_uninitialized(params.name.length);
            mmu().copy_from_vm(name.data(), (FlatPtr)params.name.characters, params.name.length);
            name_str = { name.data(), name.size() };
        }
        auto region = MmapRegion::create_file_backed(final_address, final_size, params.prot, params.flags, params.fd, params.offset, name_str);
        if (region->name() == "libc.so: .text (Emulated)") {
            bool rc = find_malloc_symbols(*region);
            VERIFY(rc);
        }
        mmu().add_region(move(region));
    }

    return final_address;
}

FlatPtr Emulator::virt$mremap(FlatPtr params_addr)
{
    Syscall::SC_mremap_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    // FIXME: Support regions that have been split in the past (e.g. due to mprotect or munmap).
    if (auto* region = mmu().find_region({ m_cpu.ds(), params.old_address })) {
        if (!is<MmapRegion>(*region))
            return -EINVAL;
        VERIFY(region->size() == params.old_size);
        auto& mmap_region = *(MmapRegion*)region;
        auto* ptr = mremap(mmap_region.data(), mmap_region.size(), mmap_region.size(), params.flags);
        if (ptr == MAP_FAILED)
            return -errno;
        return (FlatPtr)ptr;
    }
    return -EINVAL;
}

u32 Emulator::virt$mount(u32 params_addr)
{
    Syscall::SC_mount_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    auto target = mmu().copy_buffer_from_vm((FlatPtr)params.target.characters, params.target.length);
    auto fs_path = mmu().copy_buffer_from_vm((FlatPtr)params.fs_type.characters, params.fs_type.length);
    params.fs_type.characters = (char*)fs_path.data();
    params.fs_type.length = fs_path.size();
    params.target.characters = (char*)target.data();
    params.target.length = target.size();

    return syscall(SC_mount, &params);
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

u32 Emulator::virt$mprotect(FlatPtr base, size_t size, int prot)
{
    round_to_page_size(base, size);
    bool has_non_mmaped_region = false;

    mmu().for_regions_in({ 0x23, base }, size, [&](Region* region) {
        if (region) {
            if (!is<MmapRegion>(*region)) {
                has_non_mmaped_region = true;
                return IterationDecision::Break;
            }
            auto& mmap_region = *(MmapRegion*)region;
            mmap_region.set_prot(prot);
        }
        return IterationDecision::Continue;
    });
    if (has_non_mmaped_region)
        return -EINVAL;

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

uid_t Emulator::virt$geteuid()
{
    return geteuid();
}

gid_t Emulator::virt$getgid()
{
    return getgid();
}

gid_t Emulator::virt$getegid()
{
    return getegid();
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
    reportln("\n=={}==  \033[33;1mSyscall: exit({})\033[0m, shutting down!", getpid(), status);
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

int Emulator::virt$ioctl([[maybe_unused]] int fd, unsigned request, [[maybe_unused]] FlatPtr arg)
{
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
    if (request == TIOCNOTTY || request == TIOCSCTTY) {
        return syscall(SC_ioctl, fd, request, 0);
    }
    if (request == FB_IOCTL_GET_SIZE_IN_BYTES) {
        size_t size = 0;
        auto rc = syscall(SC_ioctl, fd, request, &size);
        mmu().copy_to_vm(arg, &size, sizeof(size));
        return rc;
    }
    if (request == FB_IOCTL_SET_RESOLUTION) {
        FBResolution user_resolution;
        mmu().copy_from_vm(&user_resolution, arg, sizeof(user_resolution));
        auto rc = syscall(SC_ioctl, fd, request, &user_resolution);
        mmu().copy_to_vm(arg, &user_resolution, sizeof(user_resolution));
        return rc;
    }
    if (request == FB_IOCTL_SET_BUFFER) {
        return syscall(SC_ioctl, fd, request, arg);
    }
    reportln("Unsupported ioctl: {}", request);
    dump_backtrace();
    TODO();
}

int Emulator::virt$emuctl(FlatPtr arg1, FlatPtr arg2, FlatPtr arg3)
{
    auto* tracer = malloc_tracer();
    if (!tracer)
        return 0;
    switch (arg1) {
    case 1:
        tracer->target_did_malloc({}, arg3, arg2);
        return 0;
    case 2:
        tracer->target_did_free({}, arg2);
        return 0;
    case 3:
        tracer->target_did_realloc({}, arg3, arg2);
        return 0;
    default:
        return -EINVAL;
    }
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

    reportln("\n=={}==  \033[33;1mSyscall:\033[0m execve", getpid());
    reportln("=={}==  @ {}", getpid(), path);
    for (auto& argument : arguments)
        reportln("=={}==    - {}", getpid(), argument);

    Vector<char*> argv;
    Vector<char*> envp;

    argv.append(const_cast<char*>("/bin/UserspaceEmulator"));
    argv.append(const_cast<char*>(path.characters()));
    if (g_report_to_debug)
        argv.append(const_cast<char*>("--report-to-debug"));
    argv.append(const_cast<char*>("--"));

    auto create_string_vector = [](auto& output_vector, auto& input_vector) {
        for (auto& string : input_vector)
            output_vector.append(const_cast<char*>(string.characters()));
        output_vector.append(nullptr);
    };

    create_string_vector(argv, arguments);
    create_string_vector(envp, environment);

    // Yoink duplicated program name.
    argv.remove(3 + (g_report_to_debug ? 1 : 0));

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

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);
    auto host_buffer = ByteBuffer::create_zeroed(params.buffer.size);

    Syscall::SC_realpath_params host_params;
    host_params.path = { (const char*)path.data(), path.size() };
    host_params.buffer = { (char*)host_buffer.data(), host_buffer.size() };
    int rc = syscall(SC_realpath, &host_params);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm((FlatPtr)params.buffer.data, host_buffer.data(), host_buffer.size());
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
        reportln("Attempted to sigaction() with SIGKILL");
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
    VERIFY(signal && signal < NSIG);

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
    VERIFY_NOT_REACHED();
}

void Emulator::dispatch_one_pending_signal()
{
    int signum = -1;
    for (signum = 1; signum < NSIG; ++signum) {
        int mask = 1 << signum;
        if (m_pending_signals & mask)
            break;
    }
    VERIFY(signum != -1);
    m_pending_signals &= ~(1 << signum);

    auto& handler = m_signal_handler[signum];

    if (handler.handler == 0) {
        // SIG_DFL
        auto action = default_signal_action(signum);
        if (action == DefaultSignalAction::Ignore)
            return;
        reportln("\n=={}== Got signal {} ({}), no handler registered", getpid(), signum, strsignal(signum));
        m_shutdown = true;
        return;
    }

    if (handler.handler == 1) {
        // SIG_IGN
        return;
    }

    reportln("\n=={}== Got signal {} ({}), handler at {:p}", getpid(), signum, strsignal(signum), handler.handler);

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

    VERIFY((m_cpu.esp().value() % 16) == 0);

    m_cpu.set_eip(m_signal_trampoline);
}

// Make sure the compiler doesn't "optimize away" this function:
extern void signal_trampoline_dummy();
void signal_trampoline_dummy()
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

int Emulator::virt$sched_getparam(pid_t pid, FlatPtr user_addr)
{
    sched_param user_param;
    mmu().copy_from_vm(&user_param, user_addr, sizeof(user_param));
    auto rc = syscall(SC_sched_getparam, pid, &user_param);
    mmu().copy_to_vm(user_addr, &user_param, sizeof(user_param));
    return rc;
}

int Emulator::virt$sched_setparam(int pid, FlatPtr user_addr)
{
    sched_param user_param;
    mmu().copy_from_vm(&user_param, user_addr, sizeof(user_param));
    return syscall(SC_sched_setparam, pid, &user_param);
}

int Emulator::virt$set_thread_name(pid_t pid, FlatPtr name_addr, size_t name_length)
{
    auto user_name = mmu().copy_buffer_from_vm(name_addr, name_length);
    auto name = String::formatted("(UE) {}", StringView { user_name.data(), user_name.size() });
    return syscall(SC_set_thread_name, pid, name.characters(), name.length());
}

pid_t Emulator::virt$setsid()
{
    return syscall(SC_setsid);
}

int Emulator::virt$watch_file(FlatPtr user_path_addr, size_t path_length)
{
    auto user_path = mmu().copy_buffer_from_vm(user_path_addr, path_length);
    return syscall(SC_watch_file, user_path.data(), user_path.size());
}

int Emulator::virt$clock_nanosleep(FlatPtr params_addr)
{
    Syscall::SC_clock_nanosleep_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    timespec requested_sleep;
    mmu().copy_from_vm(&requested_sleep, (FlatPtr)params.requested_sleep, sizeof(timespec));
    params.requested_sleep = &requested_sleep;

    auto remaining_vm_addr = params.remaining_sleep;
    auto remaining = ByteBuffer::create_zeroed(sizeof(timespec));
    params.remaining_sleep = (timespec*)remaining.data();

    int rc = syscall(SC_clock_nanosleep, &params);
    if (remaining_vm_addr)
        mmu().copy_to_vm((FlatPtr)remaining_vm_addr, remaining.data(), sizeof(timespec));

    return rc;
}

int Emulator::virt$readlink(FlatPtr params_addr)
{
    Syscall::SC_readlink_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);
    auto host_buffer = ByteBuffer::create_zeroed(params.buffer.size);

    Syscall::SC_readlink_params host_params;
    host_params.path = { (const char*)path.data(), path.size() };
    host_params.buffer = { (char*)host_buffer.data(), host_buffer.size() };
    int rc = syscall(SC_readlink, &host_params);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm((FlatPtr)params.buffer.data, host_buffer.data(), host_buffer.size());
    return rc;
}

u32 Emulator::virt$allocate_tls(size_t size)
{
    // TODO: Why is this needed? without this, the loader overflows the bounds of the TLS region.
    constexpr size_t TLS_SIZE_HACK = 8;
    auto tcb_region = make<SimpleRegion>(0x20000000, size + TLS_SIZE_HACK);
    bzero(tcb_region->data(), size);
    memset(tcb_region->shadow_data(), 0x01, size);

    auto tls_region = make<SimpleRegion>(0, 4);
    tls_region->write32(0, shadow_wrap_as_initialized(tcb_region->base() + (u32)size));
    memset(tls_region->shadow_data(), 0x01, 4);

    u32 tls_base = tcb_region->base();
    mmu().add_region(move(tcb_region));
    mmu().set_tls_region(move(tls_region));
    return tls_base;
}

int Emulator::virt$ptsname(int fd, FlatPtr buffer, size_t buffer_size)
{
    auto pts = mmu().copy_buffer_from_vm(buffer, buffer_size);
    return syscall(SC_ptsname, fd, pts.data(), pts.size());
}

int Emulator::virt$beep()
{
    return syscall(SC_beep);
}

bool Emulator::find_malloc_symbols(const MmapRegion& libc_text)
{
    auto file_or_error = MappedFile::map("/usr/lib/libc.so");
    if (file_or_error.is_error())
        return false;

    ELF::Image image(file_or_error.value()->bytes());
    auto malloc_symbol = image.find_demangled_function("malloc");
    auto free_symbol = image.find_demangled_function("free");
    auto realloc_symbol = image.find_demangled_function("realloc");
    auto malloc_size_symbol = image.find_demangled_function("malloc_size");
    if (!malloc_symbol.has_value() || !free_symbol.has_value() || !realloc_symbol.has_value() || !malloc_size_symbol.has_value())
        return false;

    m_malloc_symbol_start = malloc_symbol.value().value() + libc_text.base();
    m_malloc_symbol_end = m_malloc_symbol_start + malloc_symbol.value().size();
    m_free_symbol_start = free_symbol.value().value() + libc_text.base();
    m_free_symbol_end = m_free_symbol_start + free_symbol.value().size();
    m_realloc_symbol_start = realloc_symbol.value().value() + libc_text.base();
    m_realloc_symbol_end = m_realloc_symbol_start + realloc_symbol.value().size();
    m_malloc_size_symbol_start = malloc_size_symbol.value().value() + libc_text.base();
    m_malloc_size_symbol_end = m_malloc_size_symbol_start + malloc_size_symbol.value().size();
    return true;
}

int Emulator::virt$msyscall(FlatPtr)
{
    // FIXME: Implement this.
    return 0;
}

}
