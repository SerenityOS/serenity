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

#pragma once

#include "MallocTracer.h"
#include "SoftCPU.h"
#include "SoftMMU.h"
#include <AK/Types.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/Loader.h>
#include <LibX86/Instruction.h>
#include <signal.h>
#include <sys/types.h>

namespace UserspaceEmulator {

class MallocTracer;

class Emulator {
public:
    static Emulator& the();

    Emulator(const Vector<String>& arguments, const Vector<String>& environment, NonnullRefPtr<ELF::Loader>);

    bool load_elf();
    void dump_backtrace();
    void dump_backtrace(const Vector<FlatPtr>&);
    Vector<FlatPtr> raw_backtrace();

    int exec();
    u32 virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3);

    SoftMMU& mmu() { return m_mmu; }

    MallocTracer* malloc_tracer() { return m_malloc_tracer; }

    bool is_in_malloc_or_free() const;

    void did_receive_signal(int signum) { m_pending_signals |= (1 << signum); }

private:
    NonnullRefPtr<ELF::Loader> m_elf;
    OwnPtr<Debug::DebugInfo> m_debug_info;

    SoftMMU m_mmu;
    SoftCPU m_cpu;

    OwnPtr<MallocTracer> m_malloc_tracer;

    void setup_stack(const Vector<String>& arguments, const Vector<String>& environment);
    void register_signal_handlers();
    void setup_signal_trampoline();

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
    int virt$shbuf_create(int size, FlatPtr buffer);
    int virt$shbuf_allow_pid(int, pid_t peer_pid);
    int virt$shbuf_allow_all(int);
    FlatPtr virt$shbuf_get(int shbuf_id, FlatPtr size);
    int virt$shbuf_release(int shbuf_id);
    int virt$shbuf_seal(int shbuf_id);
    int virt$shbuf_set_volatile(int shbuf_id, bool);
    u32 virt$mmap(u32);
    u32 virt$munmap(FlatPtr address, u32 size);
    u32 virt$gettid();
    u32 virt$getpid();
    u32 virt$unveil(u32);
    u32 virt$pledge(u32);
    uid_t virt$getuid();
    gid_t virt$getgid();
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
    int virt$set_process_icon(int);
    int virt$gettimeofday(FlatPtr);
    int virt$clock_gettime(int, FlatPtr);
    int virt$dbgputstr(FlatPtr characters, int length);
    int virt$dbgputch(char);
    int virt$fchmod(int, mode_t);
    int virt$listen(int, int);
    int virt$kill(pid_t, int);
    int virt$fstat(int, FlatPtr);
    u32 virt$fcntl(int fd, int, u32);
    int virt$getgroups(ssize_t count, FlatPtr);
    int virt$lseek(int fd, off_t offset, int whence);
    int virt$socket(int, int, int);
    int virt$getsockopt(FlatPtr);
    int virt$setsockopt(FlatPtr);
    int virt$select(FlatPtr);
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

    FlatPtr allocate_vm(size_t size, size_t alignment);

    void dispatch_one_pending_signal();

    bool m_shutdown { false };
    int m_exit_status { 0 };

    FlatPtr m_malloc_symbol_start { 0 };
    FlatPtr m_malloc_symbol_end { 0 };
    FlatPtr m_free_symbol_start { 0 };
    FlatPtr m_free_symbol_end { 0 };

    sigset_t m_pending_signals { 0 };
    sigset_t m_signal_mask { 0 };

    struct SignalHandlerInfo {
        FlatPtr handler { 0 };
        sigset_t mask { 0 };
        int flags { 0 };
    };
    SignalHandlerInfo m_signal_handler[NSIG];

    FlatPtr m_signal_trampoline { 0 };
};

}
