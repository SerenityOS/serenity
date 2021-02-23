/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Checked.h>
#include <AK/HashMap.h>
#include <AK/InlineLinkedList.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <AK/Userspace.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Forward.h>
#include <Kernel/FutexQueue.h>
#include <Kernel/Lock.h>
#include <Kernel/ProcessGroup.h>
#include <Kernel/StdLib.h>
#include <Kernel/Thread.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/UnveilNode.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/RangeAllocator.h>
#include <Kernel/VM/Space.h>
#include <LibC/signal_numbers.h>
#include <LibELF/exec_elf.h>

namespace Kernel {

timeval kgettimeofday();
void kgettimeofday(timeval&);

#define ENUMERATE_PLEDGE_PROMISES         \
    __ENUMERATE_PLEDGE_PROMISE(stdio)     \
    __ENUMERATE_PLEDGE_PROMISE(rpath)     \
    __ENUMERATE_PLEDGE_PROMISE(wpath)     \
    __ENUMERATE_PLEDGE_PROMISE(cpath)     \
    __ENUMERATE_PLEDGE_PROMISE(dpath)     \
    __ENUMERATE_PLEDGE_PROMISE(inet)      \
    __ENUMERATE_PLEDGE_PROMISE(id)        \
    __ENUMERATE_PLEDGE_PROMISE(proc)      \
    __ENUMERATE_PLEDGE_PROMISE(ptrace)    \
    __ENUMERATE_PLEDGE_PROMISE(exec)      \
    __ENUMERATE_PLEDGE_PROMISE(unix)      \
    __ENUMERATE_PLEDGE_PROMISE(recvfd)    \
    __ENUMERATE_PLEDGE_PROMISE(sendfd)    \
    __ENUMERATE_PLEDGE_PROMISE(fattr)     \
    __ENUMERATE_PLEDGE_PROMISE(tty)       \
    __ENUMERATE_PLEDGE_PROMISE(chown)     \
    __ENUMERATE_PLEDGE_PROMISE(chroot)    \
    __ENUMERATE_PLEDGE_PROMISE(thread)    \
    __ENUMERATE_PLEDGE_PROMISE(video)     \
    __ENUMERATE_PLEDGE_PROMISE(accept)    \
    __ENUMERATE_PLEDGE_PROMISE(settime)   \
    __ENUMERATE_PLEDGE_PROMISE(sigaction) \
    __ENUMERATE_PLEDGE_PROMISE(setkeymap) \
    __ENUMERATE_PLEDGE_PROMISE(prot_exec) \
    __ENUMERATE_PLEDGE_PROMISE(map_fixed) \
    __ENUMERATE_PLEDGE_PROMISE(getkeymap)

enum class Pledge : u32 {
#define __ENUMERATE_PLEDGE_PROMISE(x) x,
    ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
};

enum class VeilState {
    None,
    Dropped,
    Locked,
};

typedef HashMap<FlatPtr, RefPtr<FutexQueue>> FutexQueues;

struct LoadResult;

class Process
    : public RefCounted<Process>
    , public InlineLinkedListNode<Process>
    , public Weakable<Process> {

    AK_MAKE_NONCOPYABLE(Process);
    AK_MAKE_NONMOVABLE(Process);

    friend class InlineLinkedListNode<Process>;
    friend class Thread;
    friend class CoreDump;

public:
    inline static Process* current()
    {
        auto current_thread = Processor::current_thread();
        return current_thread ? &current_thread->process() : nullptr;
    }

    template<typename EntryFunction>
    static RefPtr<Process> create_kernel_process(RefPtr<Thread>& first_thread, String&& name, EntryFunction entry, u32 affinity = THREAD_AFFINITY_DEFAULT)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_process(
            first_thread, move(name), [](void* data) {
                EntryFunction* func = reinterpret_cast<EntryFunction*>(data);
                (*func)();
                delete func;
            },
            entry_func, affinity);
    }

    static RefPtr<Process> create_kernel_process(RefPtr<Thread>& first_thread, String&& name, void (*entry)(void*), void* entry_data = nullptr, u32 affinity = THREAD_AFFINITY_DEFAULT);
    static RefPtr<Process> create_user_process(RefPtr<Thread>& first_thread, const String& path, uid_t, gid_t, ProcessID ppid, int& error, Vector<String>&& arguments = Vector<String>(), Vector<String>&& environment = Vector<String>(), TTY* = nullptr);
    ~Process();

    static Vector<ProcessID> all_pids();
    static AK::NonnullRefPtrVector<Process> all_processes();

    template<typename EntryFunction>
    RefPtr<Thread> create_kernel_thread(EntryFunction entry, u32 priority, const String& name, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_thread([](void* data) {
            EntryFunction* func = reinterpret_cast<EntryFunction*>(data);
            (*func)();
            delete func;
        },
            priority, name, affinity, joinable);
    }
    RefPtr<Thread> create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, const String& name, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true);

    bool is_profiling() const { return m_profiling; }
    void set_profiling(bool profiling) { m_profiling = profiling; }
    bool should_core_dump() const { return m_should_dump_core; }
    void set_dump_core(bool dump_core) { m_should_dump_core = dump_core; }

    bool is_dead() const { return m_dead; }

    bool is_stopped() const { return m_is_stopped; }
    bool set_stopped(bool stopped) { return m_is_stopped.exchange(stopped); }

    bool is_kernel_process() const { return m_is_kernel_process; }
    bool is_user_process() const { return !m_is_kernel_process; }

    static RefPtr<Process> from_pid(ProcessID);
    static SessionID get_sid_from_pgid(ProcessGroupID pgid);

    const String& name() const { return m_name; }
    ProcessID pid() const { return m_pid; }
    SessionID sid() const { return m_sid; }
    bool is_session_leader() const { return m_sid.value() == m_pid.value(); }
    ProcessGroupID pgid() const { return m_pg ? m_pg->pgid() : 0; }
    bool is_group_leader() const { return pgid().value() == m_pid.value(); }
    Span<const gid_t> extra_gids() const { return m_extra_gids; }
    uid_t euid() const { return m_euid; }
    gid_t egid() const { return m_egid; }
    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    uid_t suid() const { return m_suid; }
    gid_t sgid() const { return m_sgid; }
    ProcessID ppid() const { return m_ppid; }

    bool is_dumpable() const { return m_dumpable; }
    void set_dumpable(bool dumpable) { m_dumpable = dumpable; }

    mode_t umask() const { return m_umask; }

    bool in_group(gid_t) const;

    RefPtr<FileDescription> file_description(int fd) const;
    int fd_flags(int fd) const;

    template<typename Callback>
    static void for_each(Callback);
    template<typename Callback>
    static void for_each_in_pgrp(ProcessGroupID, Callback);
    template<typename Callback>
    void for_each_child(Callback);
    template<typename Callback>
    IterationDecision for_each_thread(Callback) const;

    void die();
    void finalize();

    ThreadTracer* tracer() { return m_tracer.ptr(); }
    bool is_traced() const { return !!m_tracer; }
    void start_tracing_from(ProcessID tracer);
    void stop_tracing();
    void tracer_trap(Thread&, const RegisterState&);

    int sys$yield();
    int sys$sync();
    int sys$beep();
    int sys$get_process_name(Userspace<char*> buffer, size_t buffer_size);
    int sys$set_process_name(Userspace<const char*> user_name, size_t user_name_length);
    int sys$watch_file(Userspace<const char*> path, size_t path_length);
    int sys$dbgputch(u8);
    int sys$dbgputstr(Userspace<const u8*>, int length);
    int sys$dump_backtrace();
    pid_t sys$gettid();
    int sys$donate(pid_t tid);
    int sys$ftruncate(int fd, off_t);
    pid_t sys$setsid();
    pid_t sys$getsid(pid_t);
    int sys$setpgid(pid_t pid, pid_t pgid);
    pid_t sys$getpgrp();
    pid_t sys$getpgid(pid_t);
    uid_t sys$getuid();
    gid_t sys$getgid();
    uid_t sys$geteuid();
    gid_t sys$getegid();
    pid_t sys$getpid();
    pid_t sys$getppid();
    int sys$getresuid(Userspace<uid_t*>, Userspace<uid_t*>, Userspace<uid_t*>);
    int sys$getresgid(Userspace<gid_t*>, Userspace<gid_t*>, Userspace<gid_t*>);
    mode_t sys$umask(mode_t);
    int sys$open(Userspace<const Syscall::SC_open_params*>);
    int sys$close(int fd);
    ssize_t sys$read(int fd, Userspace<u8*>, ssize_t);
    ssize_t sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count);
    ssize_t sys$write(int fd, const u8*, ssize_t);
    ssize_t sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count);
    int sys$fstat(int fd, Userspace<stat*>);
    int sys$stat(Userspace<const Syscall::SC_stat_params*>);
    int sys$lseek(int fd, off_t, int whence);
    int sys$kill(pid_t pid_or_pgid, int sig);
    [[noreturn]] void sys$exit(int status);
    int sys$sigreturn(RegisterState& registers);
    pid_t sys$waitid(Userspace<const Syscall::SC_waitid_params*>);
    void* sys$mmap(Userspace<const Syscall::SC_mmap_params*>);
    void* sys$mremap(Userspace<const Syscall::SC_mremap_params*>);
    int sys$munmap(void*, size_t size);
    int sys$set_mmap_name(Userspace<const Syscall::SC_set_mmap_name_params*>);
    int sys$mprotect(void*, size_t, int prot);
    int sys$madvise(void*, size_t, int advice);
    int sys$msyscall(void*);
    int sys$purge(int mode);
    int sys$select(const Syscall::SC_select_params*);
    int sys$poll(Userspace<const Syscall::SC_poll_params*>);
    ssize_t sys$get_dir_entries(int fd, void*, ssize_t);
    int sys$getcwd(Userspace<char*>, size_t);
    int sys$chdir(Userspace<const char*>, size_t);
    int sys$fchdir(int fd);
    int sys$adjtime(Userspace<const timeval*>, Userspace<timeval*>);
    int sys$gettimeofday(Userspace<timeval*>);
    int sys$clock_gettime(clockid_t, Userspace<timespec*>);
    int sys$clock_settime(clockid_t, Userspace<const timespec*>);
    int sys$clock_nanosleep(Userspace<const Syscall::SC_clock_nanosleep_params*>);
    int sys$gethostname(Userspace<char*>, ssize_t);
    int sys$sethostname(Userspace<const char*>, ssize_t);
    int sys$uname(Userspace<utsname*>);
    int sys$readlink(Userspace<const Syscall::SC_readlink_params*>);
    int sys$ttyname(int fd, Userspace<char*>, size_t);
    int sys$ptsname(int fd, Userspace<char*>, size_t);
    pid_t sys$fork(RegisterState&);
    int sys$execve(Userspace<const Syscall::SC_execve_params*>);
    int sys$dup2(int old_fd, int new_fd);
    int sys$sigaction(int signum, const sigaction* act, sigaction* old_act);
    int sys$sigprocmask(int how, Userspace<const sigset_t*> set, Userspace<sigset_t*> old_set);
    int sys$sigpending(Userspace<sigset_t*>);
    int sys$getgroups(ssize_t, Userspace<gid_t*>);
    int sys$setgroups(ssize_t, Userspace<const gid_t*>);
    int sys$pipe(int pipefd[2], int flags);
    int sys$killpg(pid_t pgrp, int sig);
    int sys$seteuid(uid_t);
    int sys$setegid(gid_t);
    int sys$setuid(uid_t);
    int sys$setgid(gid_t);
    int sys$setresuid(uid_t, uid_t, uid_t);
    int sys$setresgid(gid_t, gid_t, gid_t);
    unsigned sys$alarm(unsigned seconds);
    int sys$access(Userspace<const char*> pathname, size_t path_length, int mode);
    int sys$fcntl(int fd, int cmd, u32 extra_arg);
    int sys$ioctl(int fd, unsigned request, FlatPtr arg);
    int sys$mkdir(Userspace<const char*> pathname, size_t path_length, mode_t mode);
    clock_t sys$times(Userspace<tms*>);
    int sys$utime(Userspace<const char*> pathname, size_t path_length, Userspace<const struct utimbuf*>);
    int sys$link(Userspace<const Syscall::SC_link_params*>);
    int sys$unlink(Userspace<const char*> pathname, size_t path_length);
    int sys$symlink(Userspace<const Syscall::SC_symlink_params*>);
    int sys$rmdir(Userspace<const char*> pathname, size_t path_length);
    int sys$mount(Userspace<const Syscall::SC_mount_params*>);
    int sys$umount(Userspace<const char*> mountpoint, size_t mountpoint_length);
    int sys$chmod(Userspace<const char*> pathname, size_t path_length, mode_t);
    int sys$fchmod(int fd, mode_t);
    int sys$chown(Userspace<const Syscall::SC_chown_params*>);
    int sys$fchown(int fd, uid_t, gid_t);
    int sys$socket(int domain, int type, int protocol);
    int sys$bind(int sockfd, Userspace<const sockaddr*> addr, socklen_t);
    int sys$listen(int sockfd, int backlog);
    int sys$accept(int sockfd, Userspace<sockaddr*>, Userspace<socklen_t*>);
    int sys$connect(int sockfd, Userspace<const sockaddr*>, socklen_t);
    int sys$shutdown(int sockfd, int how);
    ssize_t sys$sendmsg(int sockfd, Userspace<const struct msghdr*>, int flags);
    ssize_t sys$recvmsg(int sockfd, Userspace<struct msghdr*>, int flags);
    int sys$getsockopt(Userspace<const Syscall::SC_getsockopt_params*>);
    int sys$setsockopt(Userspace<const Syscall::SC_setsockopt_params*>);
    int sys$getsockname(Userspace<const Syscall::SC_getsockname_params*>);
    int sys$getpeername(Userspace<const Syscall::SC_getpeername_params*>);
    int sys$sched_setparam(pid_t pid, Userspace<const struct sched_param*>);
    int sys$sched_getparam(pid_t pid, Userspace<struct sched_param*>);
    int sys$create_thread(void* (*)(void*), Userspace<const Syscall::SC_create_thread_params*>);
    void sys$exit_thread(Userspace<void*>);
    int sys$join_thread(pid_t tid, Userspace<void**> exit_value);
    int sys$detach_thread(pid_t tid);
    int sys$set_thread_name(pid_t tid, Userspace<const char*> buffer, size_t buffer_size);
    int sys$get_thread_name(pid_t tid, Userspace<char*> buffer, size_t buffer_size);
    int sys$rename(Userspace<const Syscall::SC_rename_params*>);
    int sys$mknod(Userspace<const Syscall::SC_mknod_params*>);
    int sys$halt();
    int sys$reboot();
    int sys$realpath(Userspace<const Syscall::SC_realpath_params*>);
    ssize_t sys$getrandom(Userspace<void*>, size_t, unsigned int);
    int sys$getkeymap(Userspace<const Syscall::SC_getkeymap_params*>);
    int sys$setkeymap(Userspace<const Syscall::SC_setkeymap_params*>);
    int sys$module_load(Userspace<const char*> path, size_t path_length);
    int sys$module_unload(Userspace<const char*> name, size_t name_length);
    int sys$profiling_enable(pid_t);
    int sys$profiling_disable(pid_t);
    int sys$futex(Userspace<const Syscall::SC_futex_params*>);
    int sys$chroot(Userspace<const char*> path, size_t path_length, int mount_flags);
    int sys$pledge(Userspace<const Syscall::SC_pledge_params*>);
    int sys$unveil(Userspace<const Syscall::SC_unveil_params*>);
    int sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2);
    int sys$get_stack_bounds(FlatPtr* stack_base, size_t* stack_size);
    int sys$ptrace(Userspace<const Syscall::SC_ptrace_params*>);
    int sys$sendfd(int sockfd, int fd);
    int sys$recvfd(int sockfd, int options);
    long sys$sysconf(int name);
    int sys$disown(ProcessID);
    void* sys$allocate_tls(size_t);
    int sys$prctl(int option, FlatPtr arg1, FlatPtr arg2);
    int sys$set_coredump_metadata(Userspace<const Syscall::SC_set_coredump_metadata_params*>);
    void sys$abort();
    int sys$anon_create(size_t, int options);

    template<bool sockname, typename Params>
    int get_sock_or_peer_name(const Params&);

    static void initialize();

    [[noreturn]] void crash(int signal, u32 eip, bool out_of_memory = false);
    [[nodiscard]] siginfo_t wait_info();

    const TTY* tty() const { return m_tty; }
    void set_tty(TTY*);

    u32 m_ticks_in_user { 0 };
    u32 m_ticks_in_kernel { 0 };

    u32 m_ticks_in_user_for_dead_children { 0 };
    u32 m_ticks_in_kernel_for_dead_children { 0 };

    Custody& current_directory();
    Custody* executable() { return m_executable.ptr(); }
    const Custody* executable() const { return m_executable.ptr(); }

    const Vector<String>& arguments() const { return m_arguments; };
    const Vector<String>& environment() const { return m_environment; };

    int number_of_open_file_descriptors() const;
    int max_open_file_descriptors() const
    {
        return m_max_open_file_descriptors;
    }

    KResult exec(String path, Vector<String> arguments, Vector<String> environment, int recusion_depth = 0);

    KResultOr<LoadResult> load(NonnullRefPtr<FileDescription> main_program_description, RefPtr<FileDescription> interpreter_description, const Elf32_Ehdr& main_program_header);

    bool is_superuser() const
    {
        return m_euid == 0;
    }

    void terminate_due_to_signal(u8 signal);
    KResult send_signal(u8 signal, Process* sender);

    u8 termination_signal() const { return m_termination_signal; }

    u16 thread_count() const
    {
        return m_thread_count.load(AK::MemoryOrder::memory_order_relaxed);
    }

    Lock& big_lock() { return m_big_lock; }
    Lock& ptrace_lock() { return m_ptrace_lock; }

    Custody& root_directory();
    Custody& root_directory_relative_to_global_root();
    void set_root_directory(const Custody&);

    bool has_promises() const
    {
        return m_has_promises;
    }
    bool has_promised(Pledge pledge) const
    {
        return m_promises & (1u << (u32)pledge);
    }

    VeilState veil_state() const
    {
        return m_veil_state;
    }
    const UnveilNode& unveiled_paths() const
    {
        return m_unveiled_paths;
    }

    bool wait_for_tracer_at_next_execve() const
    {
        return m_wait_for_tracer_at_next_execve;
    }
    void set_wait_for_tracer_at_next_execve(bool val)
    {
        m_wait_for_tracer_at_next_execve = val;
    }

    KResultOr<u32> peek_user_data(Userspace<const u32*> address);
    KResult poke_user_data(Userspace<u32*> address, u32 data);

    void disowned_by_waiter(Process& process);
    void unblock_waiters(Thread::WaitBlocker::UnblockFlags, u8 signal = 0);
    Thread::WaitBlockCondition& wait_block_condition() { return m_wait_block_condition; }

    HashMap<String, String>& coredump_metadata() { return m_coredump_metadata; }
    const HashMap<String, String>& coredump_metadata() const { return m_coredump_metadata; }

    const NonnullRefPtrVector<Thread>& threads_for_coredump(Badge<CoreDump>) const { return m_threads_for_coredump; }

    PerformanceEventBuffer* perf_events() { return m_perf_event_buffer; }

    Space& space() { return *m_space; }
    const Space& space() const { return *m_space; }

    VirtualAddress signal_trampoline() const { return m_signal_trampoline; }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;

    bool add_thread(Thread&);
    bool remove_thread(Thread&);

    PerformanceEventBuffer& ensure_perf_events();

    Process(RefPtr<Thread>& first_thread, const String& name, uid_t, gid_t, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd = nullptr, RefPtr<Custody> executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);
    static ProcessID allocate_pid();

    void kill_threads_except_self();
    void kill_all_threads();
    bool dump_core();
    bool dump_perfcore();

    KResult do_exec(NonnullRefPtr<FileDescription> main_program_description, Vector<String> arguments, Vector<String> environment, RefPtr<FileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags, const Elf32_Ehdr& main_program_header);
    ssize_t do_write(FileDescription&, const UserOrKernelBuffer&, size_t);

    KResultOr<RefPtr<FileDescription>> find_elf_interpreter_for_executable(const String& path, const Elf32_Ehdr& elf_header, int nread, size_t file_size);

    int alloc_fd(int first_candidate_fd = 0);

    KResult do_kill(Process&, int signal);
    KResult do_killpg(ProcessGroupID pgrp, int signal);
    KResult do_killall(int signal);
    KResult do_killself(int signal);

    KResultOr<siginfo_t> do_waitid(idtype_t idtype, int id, int options);

    KResultOr<String> get_syscall_path_argument(const char* user_path, size_t path_length) const;
    KResultOr<String> get_syscall_path_argument(Userspace<const char*> user_path, size_t path_length) const
    {
        return get_syscall_path_argument(user_path.unsafe_userspace_ptr(), path_length);
    }
    KResultOr<String> get_syscall_path_argument(const Syscall::StringArgument&) const;

    bool has_tracee_thread(ProcessID tracer_pid);

    void clear_futex_queues_on_exec();

    Process* m_prev { nullptr };
    Process* m_next { nullptr };

    String m_name;

    OwnPtr<Space> m_space;
    VirtualAddress m_signal_trampoline;

    ProcessID m_pid { 0 };
    SessionID m_sid { 0 };
    RefPtr<ProcessGroup> m_pg;

    uid_t m_euid { 0 };
    gid_t m_egid { 0 };
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    uid_t m_suid { 0 };
    gid_t m_sgid { 0 };

    OwnPtr<ThreadTracer> m_tracer;

    static const int m_max_open_file_descriptors { FD_SETSIZE };

    class FileDescriptionAndFlags {
    public:
        operator bool() const { return !!m_description; }

        FileDescription* description() { return m_description; }
        const FileDescription* description() const { return m_description; }

        u32 flags() const { return m_flags; }
        void set_flags(u32 flags) { m_flags = flags; }

        void clear();
        void set(NonnullRefPtr<FileDescription>&&, u32 flags = 0);

    private:
        RefPtr<FileDescription> m_description;
        u32 m_flags { 0 };
    };
    Vector<FileDescriptionAndFlags> m_fds;

    u8 m_termination_status { 0 };
    u8 m_termination_signal { 0 };
    Atomic<u32> m_thread_count { 0 };
    mutable IntrusiveList<Thread, &Thread::m_process_thread_list_node> m_thread_list;
    mutable RecursiveSpinLock m_thread_list_lock;

    const bool m_is_kernel_process;
    bool m_dead { false };
    bool m_profiling { false };
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_stopped { false };
    bool m_should_dump_core { false };

    RefPtr<Custody> m_executable;
    RefPtr<Custody> m_cwd;
    RefPtr<Custody> m_root_directory;
    RefPtr<Custody> m_root_directory_relative_to_global_root;

    Vector<String> m_arguments;
    Vector<String> m_environment;

    RefPtr<TTY> m_tty;

    ProcessID m_ppid { 0 };
    mode_t m_umask { 022 };

    bool m_dumpable { true };

    Vector<gid_t> m_extra_gids;

    WeakPtr<Region> m_master_tls_region;
    size_t m_master_tls_size { 0 };
    size_t m_master_tls_alignment { 0 };

    Lock m_big_lock { "Process" };
    Lock m_ptrace_lock { "ptrace" };

    RefPtr<Timer> m_alarm_timer;

    bool m_has_promises { false };
    u32 m_promises { 0 };
    bool m_has_execpromises { false };
    u32 m_execpromises { 0 };

    VeilState m_veil_state { VeilState::None };
    UnveilNode m_unveiled_paths { "/", { .full_path = "/", .unveil_inherited_from_root = true } };

    OwnPtr<PerformanceEventBuffer> m_perf_event_buffer;

    FutexQueues m_futex_queues;
    SpinLock<u8> m_futex_lock;

    // This member is used in the implementation of ptrace's PT_TRACEME flag.
    // If it is set to true, the process will stop at the next execve syscall
    // and wait for a tracer to attach.
    bool m_wait_for_tracer_at_next_execve { false };

    Thread::WaitBlockCondition m_wait_block_condition;

    HashMap<String, String> m_coredump_metadata;

    NonnullRefPtrVector<Thread> m_threads_for_coredump;
};

extern InlineLinkedList<Process>* g_processes;
extern RecursiveSpinLock g_processes_lock;

template<typename Callback>
inline void Process::for_each(Callback callback)
{
    VERIFY_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(g_processes_lock);
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (callback(*process) == IterationDecision::Break)
            break;
        process = next_process;
    }
}

template<typename Callback>
inline void Process::for_each_child(Callback callback)
{
    VERIFY_INTERRUPTS_DISABLED();
    ProcessID my_pid = pid();
    ScopedSpinLock lock(g_processes_lock);
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (process->ppid() == my_pid || process->has_tracee_thread(m_pid)) {
            if (callback(*process) == IterationDecision::Break)
                break;
        }
        process = next_process;
    }
}

template<typename Callback>
inline IterationDecision Process::for_each_thread(Callback callback) const
{
    ScopedSpinLock thread_list_lock(m_thread_list_lock);
    for (auto& thread : m_thread_list) {
        IterationDecision decision = callback(thread);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline void Process::for_each_in_pgrp(ProcessGroupID pgid, Callback callback)
{
    VERIFY_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(g_processes_lock);
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (!process->is_dead() && process->pgid() == pgid) {
            if (callback(*process) == IterationDecision::Break)
                break;
        }
        process = next_process;
    }
}

inline bool InodeMetadata::may_read(const Process& process) const
{
    return may_read(process.euid(), process.egid(), process.extra_gids());
}

inline bool InodeMetadata::may_write(const Process& process) const
{
    return may_write(process.euid(), process.egid(), process.extra_gids());
}

inline bool InodeMetadata::may_execute(const Process& process) const
{
    return may_execute(process.euid(), process.egid(), process.extra_gids());
}

inline ProcessID Thread::pid() const
{
    return m_process->pid();
}

inline const LogStream& operator<<(const LogStream& stream, const Process& process)
{
    return stream << process.name() << '(' << process.pid().value() << ')';
}

#define REQUIRE_NO_PROMISES                        \
    do {                                           \
        if (Process::current()->has_promises()) {  \
            dbgln("Has made a promise");           \
            cli();                                 \
            Process::current()->crash(SIGABRT, 0); \
            VERIFY_NOT_REACHED();                  \
        }                                          \
    } while (0)

#define REQUIRE_PROMISE(promise)                                     \
    do {                                                             \
        if (Process::current()->has_promises()                       \
            && !Process::current()->has_promised(Pledge::promise)) { \
            dbgln("Has not pledged {}", #promise);                   \
            cli();                                                   \
            Process::current()->coredump_metadata().set(             \
                "pledge_violation", #promise);                       \
            Process::current()->crash(SIGABRT, 0);                   \
            VERIFY_NOT_REACHED();                                    \
        }                                                            \
    } while (0)

}

inline static String copy_string_from_user(const Kernel::Syscall::StringArgument& string)
{
    return copy_string_from_user(string.characters, string.length);
}

template<>
struct AK::Formatter<Kernel::Process> : AK::Formatter<String> {
    void format(FormatBuilder& builder, const Kernel::Process& value)
    {
        return AK::Formatter<String>::format(builder, String::formatted("{}({})", value.name(), value.pid().value()));
    }
};
