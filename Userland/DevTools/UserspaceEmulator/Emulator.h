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

#pragma once

#include "MallocTracer.h"
#include "RangeAllocator.h"
#include "Report.h"
#include "SoftCPU.h"
#include "SoftMMU.h"
#include <AK/MappedFile.h>
#include <AK/Types.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibX86/Instruction.h>
#include <signal.h>
#include <sys/types.h>

namespace UserspaceEmulator {

class MallocTracer;

class Emulator {
public:
    static Emulator& the();

    Emulator(const String& executable_path, const Vector<String>& arguments, const Vector<String>& environment);

    bool load_elf();
    void dump_backtrace();
    void dump_backtrace(const Vector<FlatPtr>&);
    Vector<FlatPtr> raw_backtrace();

    int exec();
    u32 virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3);

    SoftMMU& mmu() { return m_mmu; }

    MallocTracer* malloc_tracer() { return m_malloc_tracer; }

    bool is_in_malloc_or_free() const;
    bool is_in_loader_code() const;
    bool is_in_libsystem() const;

    void did_receive_signal(int signum) { m_pending_signals |= (1 << signum); }

private:
    const String m_executable_path;
    const Vector<String> m_arguments;
    const Vector<String> m_environment;

    SoftMMU m_mmu;
    SoftCPU m_cpu;

    OwnPtr<MallocTracer> m_malloc_tracer;

    void setup_stack(Vector<ELF::AuxiliaryValue>);
    Vector<ELF::AuxiliaryValue> generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, String executable_path, int executable_fd) const;
    void register_signal_handlers();
    void setup_signal_trampoline();

    int virt$emuctl(FlatPtr, FlatPtr, FlatPtr);
    int virt$fork();
    int virt$execve(FlatPtr);
    int virt$access(FlatPtr, size_t, int);
    int virt$sigaction(int, FlatPtr, FlatPtr);
    int virt$sigreturn();
    int virt$get_dir_entries(int fd, FlatPtr buffer, ssize_t);
    int virt$ioctl(int fd, unsigned, FlatPtr);
    int virt$stat(FlatPtr);
    int virt$realpath(FlatPtr);
    int virt$gethostname(FlatPtr, ssize_t);
    int virt$profiling_enable(pid_t);
    int virt$profiling_disable(pid_t);
    int virt$disown(pid_t);
    int virt$purge(int mode);
    u32 virt$mmap(u32);
    FlatPtr virt$mremap(FlatPtr);
    u32 virt$mount(u32);
    u32 virt$munmap(FlatPtr address, size_t size);
    u32 virt$gettid();
    u32 virt$getpid();
    u32 virt$unveil(u32);
    u32 virt$pledge(u32);
    uid_t virt$getuid();
    uid_t virt$geteuid();
    gid_t virt$getgid();
    gid_t virt$getegid();
    int virt$setuid(uid_t);
    int virt$setgid(gid_t);
    u32 virt$read(int, FlatPtr, ssize_t);
    u32 virt$write(int, FlatPtr, ssize_t);
    u32 virt$mprotect(FlatPtr, size_t, int);
    u32 virt$madvise(FlatPtr, size_t, int);
    u32 virt$open(u32);
    int virt$pipe(FlatPtr pipefd, int flags);
    int virt$close(int);
    int virt$mkdir(FlatPtr path, size_t path_length, mode_t mode);
    int virt$unlink(FlatPtr path, size_t path_length);
    int virt$get_process_name(FlatPtr buffer, int size);
    int virt$set_mmap_name(FlatPtr);
    int virt$gettimeofday(FlatPtr);
    int virt$clock_gettime(int, FlatPtr);
    int virt$clock_nanosleep(FlatPtr);
    int virt$dbgputstr(FlatPtr characters, int length);
    int virt$dbgputch(char);
    int virt$chmod(FlatPtr, size_t, mode_t);
    int virt$fchmod(int, mode_t);
    int virt$chown(FlatPtr);
    int virt$fchown(int, uid_t, gid_t);
    int virt$clock_settime(uint32_t clock_id, FlatPtr user_ts);
    int virt$listen(int, int);
    int virt$kill(pid_t, int);
    int virt$fstat(int, FlatPtr);
    u32 virt$fcntl(int fd, int, u32);
    int virt$getgroups(ssize_t count, FlatPtr);
    int virt$setgroups(ssize_t count, FlatPtr);
    int virt$lseek(int fd, off_t offset, int whence);
    int virt$socket(int, int, int);
    int virt$getsockopt(FlatPtr);
    int virt$setsockopt(FlatPtr);
    int virt$select(FlatPtr);
    int virt$get_stack_bounds(FlatPtr, FlatPtr);
    int virt$accept(int sockfd, FlatPtr address, FlatPtr address_length);
    int virt$bind(int sockfd, FlatPtr address, socklen_t address_length);
    int virt$recvmsg(int sockfd, FlatPtr msg_addr, int flags);
    int virt$sendmsg(int sockfd, FlatPtr msg_addr, int flags);
    int virt$connect(int sockfd, FlatPtr address, socklen_t address_size);
    void virt$exit(int);
    ssize_t virt$getrandom(FlatPtr buffer, size_t buffer_size, unsigned int flags);
    int virt$chdir(FlatPtr, size_t);
    int virt$dup2(int, int);
    int virt$getpgrp();
    int virt$getpgid(pid_t);
    int virt$setpgid(pid_t pid, pid_t pgid);
    int virt$ttyname(int fd, FlatPtr buffer, size_t buffer_size);
    int virt$getcwd(FlatPtr buffer, size_t buffer_size);
    int virt$waitid(FlatPtr);
    int virt$getsid(pid_t);
    int virt$sched_setparam(int, FlatPtr);
    int virt$sched_getparam(pid_t, FlatPtr);
    int virt$set_thread_name(pid_t, FlatPtr, size_t);
    pid_t virt$setsid();
    int virt$watch_file(FlatPtr, size_t);
    int virt$readlink(FlatPtr);
    u32 virt$allocate_tls(size_t);
    int virt$ptsname(int fd, FlatPtr buffer, size_t buffer_size);
    int virt$beep();
    int virt$ftruncate(int fd, off_t);
    mode_t virt$umask(mode_t);
    int virt$anon_create(size_t, int);
    int virt$recvfd(int, int);
    int virt$sendfd(int, int);
    int virt$msyscall(FlatPtr);

    bool find_malloc_symbols(const MmapRegion& libc_text);

    void dispatch_one_pending_signal();
    const MmapRegion* find_text_region(FlatPtr address);
    String create_backtrace_line(FlatPtr address);

    bool m_shutdown { false };
    int m_exit_status { 0 };

    FlatPtr m_malloc_symbol_start { 0 };
    FlatPtr m_malloc_symbol_end { 0 };
    FlatPtr m_realloc_symbol_start { 0 };
    FlatPtr m_realloc_symbol_end { 0 };
    FlatPtr m_calloc_symbol_start { 0 };
    FlatPtr m_calloc_symbol_end { 0 };
    FlatPtr m_free_symbol_start { 0 };
    FlatPtr m_free_symbol_end { 0 };
    FlatPtr m_malloc_size_symbol_start { 0 };
    FlatPtr m_malloc_size_symbol_end { 0 };

    FlatPtr m_libsystem_start { 0 };
    FlatPtr m_libsystem_end { 0 };

    sigset_t m_pending_signals { 0 };
    sigset_t m_signal_mask { 0 };

    struct SignalHandlerInfo {
        FlatPtr handler { 0 };
        sigset_t mask { 0 };
        int flags { 0 };
    };
    SignalHandlerInfo m_signal_handler[NSIG];

    FlatPtr m_signal_trampoline { 0 };
    Optional<FlatPtr> m_loader_text_base;
    Optional<size_t> m_loader_text_size;

    struct CachedELF {
        NonnullRefPtr<MappedFile> mapped_file;
        NonnullOwnPtr<Debug::DebugInfo> debug_info;
    };

    HashMap<String, CachedELF> m_dynamic_library_cache;

    RangeAllocator m_range_allocator;
};

ALWAYS_INLINE bool Emulator::is_in_libsystem() const
{
    return m_cpu.base_eip() >= m_libsystem_start && m_cpu.base_eip() < m_libsystem_end;
}

ALWAYS_INLINE bool Emulator::is_in_malloc_or_free() const
{
    return (m_cpu.base_eip() >= m_malloc_symbol_start && m_cpu.base_eip() < m_malloc_symbol_end)
        || (m_cpu.base_eip() >= m_free_symbol_start && m_cpu.base_eip() < m_free_symbol_end)
        || (m_cpu.base_eip() >= m_realloc_symbol_start && m_cpu.base_eip() < m_realloc_symbol_end)
        || (m_cpu.base_eip() >= m_calloc_symbol_start && m_cpu.base_eip() < m_calloc_symbol_end)
        || (m_cpu.base_eip() >= m_malloc_size_symbol_start && m_cpu.base_eip() < m_malloc_size_symbol_end);
}

ALWAYS_INLINE bool Emulator::is_in_loader_code() const
{
    if (!m_loader_text_base.has_value() || !m_loader_text_size.has_value())
        return false;
    return (m_cpu.base_eip() >= m_loader_text_base.value() && m_cpu.base_eip() < m_loader_text_base.value() + m_loader_text_size.value());
}

}
