/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MallocTracer.h"
#include "RangeAllocator.h"
#include "Report.h"
#include "SoftCPU.h"
#include "SoftMMU.h"
#include <YAK/FileStream.h>
#include <YAK/MappedFile.h>
#include <YAK/Types.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibLine/Editor.h>
#include <LibX86/Instruction.h>
#include <signal.h>
#include <sys/types.h>

namespace UserspaceEmulator {

class MallocTracer;

class Emulator {
public:
    static Emulator& the();

    Emulator(String const& executable_path, Vector<String> const& arguments, Vector<String> const& environment);

    void set_profiling_details(bool should_dump_profile, size_t instruction_interval, OutputFileStream* profile_stream)
    {
        m_is_profiling = should_dump_profile;
        m_profile_instruction_interval = instruction_interval;
        m_profile_stream = profile_stream;
    }

    void set_in_region_of_interest(bool value)
    {
        m_is_in_region_of_interest = value;
    }

    OutputFileStream& profile_stream() { return *m_profile_stream; }
    bool is_profiling() const { return m_is_profiling; }
    bool is_in_region_of_interest() const { return m_is_in_region_of_interest; }
    size_t profile_instruction_interval() const { return m_profile_instruction_interval; }
    bool is_memory_auditing_suppressed() const { return m_is_memory_auditing_suppressed; }

    bool load_elf();
    void dump_backtrace();
    void dump_backtrace(Vector<FlatPtr> const&);
    Vector<FlatPtr> raw_backtrace();

    int exec();
    void handle_repl();
    u32 virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3);

    SoftMMU& mmu() { return m_mmu; }

    MallocTracer* malloc_tracer() { return m_malloc_tracer; }

    bool is_in_loader_code() const;
    bool is_in_libsystem() const;

    void pause()
    {
        m_steps_til_pause = 0;
        m_run_til_return = false;
    }
    ALWAYS_INLINE void return_callback(FlatPtr addr)
    {
        if (m_run_til_return) [[unlikely]] {
            if (addr == m_watched_addr)
                pause();
        }
    }
    ALWAYS_INLINE void call_callback(FlatPtr addr)
    {
        if (m_run_til_call) [[unlikely]] {
            if (addr == m_watched_addr)
                pause();
        }
    }

    void did_receive_signal(int signum) { m_pending_signals |= (1 << signum); }
    void did_receive_sigint(int)
    {
        if (m_steps_til_pause == 0)
            m_shutdown = true;
        else
            pause();
    }

    struct SymbolInfo {
        String lib_name;
        String symbol;
        Optional<Debug::DebugInfo::SourcePosition> source_position;
    };

    Optional<SymbolInfo> symbol_at(FlatPtr address);

    void dump_regions() const;

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

    void emit_profile_sample(YAK::OutputStream&);
    void emit_profile_event(YAK::OutputStream&, StringView event_name, String contents);

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
    int virt$rmdir(FlatPtr path, size_t path_length);
    int virt$unlink(FlatPtr path, size_t path_length);
    int virt$symlink(FlatPtr address);
    int virt$rename(FlatPtr address);
    int virt$set_coredump_metadata(FlatPtr address);
    int virt$get_process_name(FlatPtr buffer, int size);
    int virt$set_process_name(FlatPtr buffer, int size);
    int virt$set_mmap_name(FlatPtr);
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
    int virt$killpg(int pgrp, int sig);
    int virt$fstat(int, FlatPtr);
    u32 virt$fcntl(int fd, int, u32);
    int virt$getgroups(ssize_t count, FlatPtr);
    int virt$setgroups(ssize_t count, FlatPtr);
    int virt$lseek(int fd, FlatPtr offset_addr, int whence);
    int virt$socket(int, int, int);
    int virt$getsockopt(FlatPtr);
    int virt$setsockopt(FlatPtr);
    int virt$getsockname(FlatPtr);
    int virt$getpeername(FlatPtr);
    int virt$select(FlatPtr);
    int virt$get_stack_bounds(FlatPtr, FlatPtr);
    int virt$accept4(FlatPtr);
    int virt$bind(int sockfd, FlatPtr address, socklen_t address_length);
    int virt$recvmsg(int sockfd, FlatPtr msg_addr, int flags);
    int virt$sendmsg(int sockfd, FlatPtr msg_addr, int flags);
    int virt$connect(int sockfd, FlatPtr address, socklen_t address_size);
    int virt$shutdown(int sockfd, int how);
    void virt$sync();
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
    int virt$create_inode_watcher(unsigned);
    int virt$inode_watcher_add_watch(FlatPtr);
    int virt$inode_watcher_remove_watch(int, int);
    int virt$readlink(FlatPtr);
    u32 virt$allocate_tls(FlatPtr, size_t);
    int virt$ptsname(int fd, FlatPtr buffer, size_t buffer_size);
    int virt$beep();
    int virt$ftruncate(int fd, FlatPtr length_addr);
    mode_t virt$umask(mode_t);
    int virt$anon_create(size_t, int);
    int virt$recvfd(int, int);
    int virt$sendfd(int, int);
    int virt$msyscall(FlatPtr);
    int virt$futex(FlatPtr);

    void dispatch_one_pending_signal();
    MmapRegion const* find_text_region(FlatPtr address);
    MmapRegion const* load_library_from_address(FlatPtr address);
    MmapRegion const* first_region_for_object(StringView name);
    String create_backtrace_line(FlatPtr address);
    String create_instruction_line(FlatPtr address, X86::Instruction insn);

    bool m_shutdown { false };
    int m_exit_status { 0 };

    i64 m_steps_til_pause { -1 };
    bool m_run_til_return { false };
    bool m_run_til_call { false };
    FlatPtr m_watched_addr { 0 };
    RefPtr<Line::Editor> m_editor;

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
        NonnullOwnPtr<ELF::Image> image;
    };

    HashMap<String, CachedELF> m_dynamic_library_cache;

    RangeAllocator m_range_allocator;

    OutputFileStream* m_profile_stream { nullptr };
    bool m_is_profiling { false };
    size_t m_profile_instruction_interval { 0 };
    bool m_is_in_region_of_interest { false };
    bool m_is_memory_auditing_suppressed { false };
};

ALWAYS_INLINE bool Emulator::is_in_libsystem() const
{
    return m_cpu.base_eip() >= m_libsystem_start && m_cpu.base_eip() < m_libsystem_end;
}

ALWAYS_INLINE bool Emulator::is_in_loader_code() const
{
    if (!m_loader_text_base.has_value() || !m_loader_text_size.has_value())
        return false;
    return (m_cpu.base_eip() >= m_loader_text_base.value() && m_cpu.base_eip() < m_loader_text_base.value() + m_loader_text_size.value());
}

}
