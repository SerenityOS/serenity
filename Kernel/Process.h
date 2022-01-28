/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Userspace.h>
#include <AK/Variant.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Assertions.h>
#include <Kernel/AtomicEdgeAction.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/Forward.h>
#include <Kernel/FutexQueue.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Memory/AddressSpace.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/ProcessGroup.h>
#include <Kernel/StdLib.h>
#include <Kernel/Thread.h>
#include <Kernel/UnixTypes.h>
#include <LibC/elf.h>
#include <LibC/signal_numbers.h>

namespace Kernel {

MutexProtected<OwnPtr<KString>>& hostname();
Time kgettimeofday();

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

using FutexQueues = HashMap<FlatPtr, RefPtr<FutexQueue>>;

struct LoadResult;

class Process final
    : public ListedRefCounted<Process, LockType::Spinlock>
    , public Weakable<Process> {

    class ProtectedValues {
    public:
        ProcessID pid { 0 };
        ProcessID ppid { 0 };
        SessionID sid { 0 };
        UserID euid { 0 };
        GroupID egid { 0 };
        UserID uid { 0 };
        GroupID gid { 0 };
        UserID suid { 0 };
        GroupID sgid { 0 };
        Vector<GroupID> extra_gids;
        bool dumpable { false };
        Atomic<bool> has_promises { false };
        Atomic<u32> promises { 0 };
        Atomic<bool> has_execpromises { false };
        Atomic<u32> execpromises { 0 };
        mode_t umask { 022 };
        VirtualAddress signal_trampoline;
        Atomic<u32> thread_count { 0 };
        u8 termination_status { 0 };
        u8 termination_signal { 0 };
    };

public:
    AK_MAKE_NONCOPYABLE(Process);
    AK_MAKE_NONMOVABLE(Process);

    MAKE_ALIGNED_ALLOCATED(Process, PAGE_SIZE);

    friend class Thread;
    friend class Coredump;

    // Helper class to temporarily unprotect a process's protected data so you can write to it.
    class ProtectedDataMutationScope {
    public:
        explicit ProtectedDataMutationScope(Process& process)
            : m_process(process)
        {
            m_process.unprotect_data();
        }

        ~ProtectedDataMutationScope() { m_process.protect_data(); }

    private:
        Process& m_process;
    };

    enum class State : u8 {
        Running = 0,
        Dying,
        Dead
    };

public:
    class ProcessProcFSTraits;

    inline static Process& current()
    {
        auto* current_thread = Processor::current_thread();
        VERIFY(current_thread);
        return current_thread->process();
    }

    inline static bool has_current()
    {
        return Processor::current_thread() != nullptr;
    }

    template<typename EntryFunction>
    static void kernel_process_trampoline(void* data)
    {
        EntryFunction* func = reinterpret_cast<EntryFunction*>(data);
        (*func)();
        delete func;
    }

    enum class RegisterProcess {
        No,
        Yes
    };

    template<typename EntryFunction>
    static RefPtr<Process> create_kernel_process(RefPtr<Thread>& first_thread, NonnullOwnPtr<KString> name, EntryFunction entry, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_process(first_thread, move(name), &Process::kernel_process_trampoline<EntryFunction>, entry_func, affinity, do_register);
    }

    static RefPtr<Process> create_kernel_process(RefPtr<Thread>& first_thread, NonnullOwnPtr<KString> name, void (*entry)(void*), void* entry_data = nullptr, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes);
    static ErrorOr<NonnullRefPtr<Process>> try_create_user_process(RefPtr<Thread>& first_thread, StringView path, UserID, GroupID, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, TTY*);
    static void register_new(Process&);

    ~Process();

    RefPtr<Thread> create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, NonnullOwnPtr<KString> name, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true);

    bool is_profiling() const { return m_profiling; }
    void set_profiling(bool profiling) { m_profiling = profiling; }

    bool should_generate_coredump() const { return m_should_generate_coredump; }
    void set_should_generate_coredump(bool b) { m_should_generate_coredump = b; }

    bool is_dying() const { return m_state.load(AK::MemoryOrder::memory_order_acquire) != State::Running; }
    bool is_dead() const { return m_state.load(AK::MemoryOrder::memory_order_acquire) == State::Dead; }

    bool is_stopped() const { return m_is_stopped; }
    bool set_stopped(bool stopped) { return m_is_stopped.exchange(stopped); }

    bool is_kernel_process() const { return m_is_kernel_process; }
    bool is_user_process() const { return !m_is_kernel_process; }

    static RefPtr<Process> from_pid(ProcessID);
    static SessionID get_sid_from_pgid(ProcessGroupID pgid);

    StringView name() const { return m_name->view(); }
    ProcessID pid() const { return m_protected_values.pid; }
    SessionID sid() const { return m_protected_values.sid; }
    bool is_session_leader() const { return sid().value() == pid().value(); }
    ProcessGroupID pgid() const { return m_pg ? m_pg->pgid() : 0; }
    bool is_group_leader() const { return pgid().value() == pid().value(); }
    Vector<GroupID> const& extra_gids() const { return m_protected_values.extra_gids; }
    UserID euid() const { return m_protected_values.euid; }
    GroupID egid() const { return m_protected_values.egid; }
    UserID uid() const { return m_protected_values.uid; }
    GroupID gid() const { return m_protected_values.gid; }
    UserID suid() const { return m_protected_values.suid; }
    GroupID sgid() const { return m_protected_values.sgid; }
    ProcessID ppid() const { return m_protected_values.ppid; }

    bool is_dumpable() const { return m_protected_values.dumpable; }
    void set_dumpable(bool);

    mode_t umask() const { return m_protected_values.umask; }

    bool in_group(GroupID) const;

    // Breakable iteration functions
    template<IteratorFunction<Process&> Callback>
    static void for_each(Callback);
    template<IteratorFunction<Process&> Callback>
    static void for_each_in_pgrp(ProcessGroupID, Callback);
    template<IteratorFunction<Process&> Callback>
    void for_each_child(Callback);

    template<IteratorFunction<Thread&> Callback>
    IterationDecision for_each_thread(Callback);
    template<IteratorFunction<Thread&> Callback>
    IterationDecision for_each_thread(Callback callback) const;

    // Non-breakable iteration functions
    template<VoidFunction<Process&> Callback>
    static void for_each(Callback);
    template<VoidFunction<Process&> Callback>
    static void for_each_in_pgrp(ProcessGroupID, Callback);
    template<VoidFunction<Process&> Callback>
    void for_each_child(Callback);

    template<VoidFunction<Thread&> Callback>
    IterationDecision for_each_thread(Callback);
    template<VoidFunction<Thread&> Callback>
    IterationDecision for_each_thread(Callback callback) const;

    void die();
    void finalize();

    ThreadTracer* tracer() { return m_tracer.ptr(); }
    bool is_traced() const { return !!m_tracer; }
    ErrorOr<void> start_tracing_from(ProcessID tracer);
    void stop_tracing();
    void tracer_trap(Thread&, const RegisterState&);

    ErrorOr<FlatPtr> sys$emuctl();
    ErrorOr<FlatPtr> sys$yield();
    ErrorOr<FlatPtr> sys$sync();
    ErrorOr<FlatPtr> sys$beep();
    ErrorOr<FlatPtr> sys$get_process_name(Userspace<char*> buffer, size_t buffer_size);
    ErrorOr<FlatPtr> sys$set_process_name(Userspace<const char*> user_name, size_t user_name_length);
    ErrorOr<FlatPtr> sys$create_inode_watcher(u32 flags);
    ErrorOr<FlatPtr> sys$inode_watcher_add_watch(Userspace<const Syscall::SC_inode_watcher_add_watch_params*> user_params);
    ErrorOr<FlatPtr> sys$inode_watcher_remove_watch(int fd, int wd);
    ErrorOr<FlatPtr> sys$dbgputstr(Userspace<const char*>, size_t);
    ErrorOr<FlatPtr> sys$dump_backtrace();
    ErrorOr<FlatPtr> sys$gettid();
    ErrorOr<FlatPtr> sys$setsid();
    ErrorOr<FlatPtr> sys$getsid(pid_t);
    ErrorOr<FlatPtr> sys$setpgid(pid_t pid, pid_t pgid);
    ErrorOr<FlatPtr> sys$getpgrp();
    ErrorOr<FlatPtr> sys$getpgid(pid_t);
    ErrorOr<FlatPtr> sys$getuid();
    ErrorOr<FlatPtr> sys$getgid();
    ErrorOr<FlatPtr> sys$geteuid();
    ErrorOr<FlatPtr> sys$getegid();
    ErrorOr<FlatPtr> sys$getpid();
    ErrorOr<FlatPtr> sys$getppid();
    ErrorOr<FlatPtr> sys$getresuid(Userspace<UserID*>, Userspace<UserID*>, Userspace<UserID*>);
    ErrorOr<FlatPtr> sys$getresgid(Userspace<GroupID*>, Userspace<GroupID*>, Userspace<GroupID*>);
    ErrorOr<FlatPtr> sys$umask(mode_t);
    ErrorOr<FlatPtr> sys$open(Userspace<const Syscall::SC_open_params*>);
    ErrorOr<FlatPtr> sys$close(int fd);
    ErrorOr<FlatPtr> sys$read(int fd, Userspace<u8*>, size_t);
    ErrorOr<FlatPtr> sys$pread(int fd, Userspace<u8*>, size_t, Userspace<off_t const*>);
    ErrorOr<FlatPtr> sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count);
    ErrorOr<FlatPtr> sys$write(int fd, Userspace<const u8*>, size_t);
    ErrorOr<FlatPtr> sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count);
    ErrorOr<FlatPtr> sys$fstat(int fd, Userspace<stat*>);
    ErrorOr<FlatPtr> sys$stat(Userspace<const Syscall::SC_stat_params*>);
    ErrorOr<FlatPtr> sys$lseek(int fd, Userspace<off_t*>, int whence);
    ErrorOr<FlatPtr> sys$ftruncate(int fd, Userspace<off_t const*>);
    ErrorOr<FlatPtr> sys$kill(pid_t pid_or_pgid, int sig);
    [[noreturn]] void sys$exit(int status);
    ErrorOr<FlatPtr> sys$sigreturn(RegisterState& registers);
    ErrorOr<FlatPtr> sys$waitid(Userspace<const Syscall::SC_waitid_params*>);
    ErrorOr<FlatPtr> sys$mmap(Userspace<const Syscall::SC_mmap_params*>);
    ErrorOr<FlatPtr> sys$mremap(Userspace<const Syscall::SC_mremap_params*>);
    ErrorOr<FlatPtr> sys$munmap(Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$set_mmap_name(Userspace<const Syscall::SC_set_mmap_name_params*>);
    ErrorOr<FlatPtr> sys$mprotect(Userspace<void*>, size_t, int prot);
    ErrorOr<FlatPtr> sys$madvise(Userspace<void*>, size_t, int advice);
    ErrorOr<FlatPtr> sys$msyscall(Userspace<void*>);
    ErrorOr<FlatPtr> sys$msync(Userspace<void*>, size_t, int flags);
    ErrorOr<FlatPtr> sys$purge(int mode);
    ErrorOr<FlatPtr> sys$poll(Userspace<const Syscall::SC_poll_params*>);
    ErrorOr<FlatPtr> sys$get_dir_entries(int fd, Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$getcwd(Userspace<char*>, size_t);
    ErrorOr<FlatPtr> sys$chdir(Userspace<const char*>, size_t);
    ErrorOr<FlatPtr> sys$fchdir(int fd);
    ErrorOr<FlatPtr> sys$adjtime(Userspace<const timeval*>, Userspace<timeval*>);
    ErrorOr<FlatPtr> sys$clock_gettime(clockid_t, Userspace<timespec*>);
    ErrorOr<FlatPtr> sys$clock_settime(clockid_t, Userspace<const timespec*>);
    ErrorOr<FlatPtr> sys$clock_nanosleep(Userspace<const Syscall::SC_clock_nanosleep_params*>);
    ErrorOr<FlatPtr> sys$gethostname(Userspace<char*>, size_t);
    ErrorOr<FlatPtr> sys$sethostname(Userspace<const char*>, size_t);
    ErrorOr<FlatPtr> sys$uname(Userspace<utsname*>);
    ErrorOr<FlatPtr> sys$readlink(Userspace<const Syscall::SC_readlink_params*>);
    ErrorOr<FlatPtr> sys$ttyname(int fd, Userspace<char*>, size_t);
    ErrorOr<FlatPtr> sys$ptsname(int fd, Userspace<char*>, size_t);
    ErrorOr<FlatPtr> sys$fork(RegisterState&);
    ErrorOr<FlatPtr> sys$execve(Userspace<const Syscall::SC_execve_params*>);
    ErrorOr<FlatPtr> sys$dup2(int old_fd, int new_fd);
    ErrorOr<FlatPtr> sys$sigaction(int signum, Userspace<const sigaction*> act, Userspace<sigaction*> old_act);
    ErrorOr<FlatPtr> sys$sigaltstack(Userspace<const stack_t*> ss, Userspace<stack_t*> old_ss);
    ErrorOr<FlatPtr> sys$sigprocmask(int how, Userspace<const sigset_t*> set, Userspace<sigset_t*> old_set);
    ErrorOr<FlatPtr> sys$sigpending(Userspace<sigset_t*>);
    ErrorOr<FlatPtr> sys$sigtimedwait(Userspace<sigset_t const*>, Userspace<siginfo_t*>, Userspace<const timespec*>);
    ErrorOr<FlatPtr> sys$getgroups(size_t, Userspace<gid_t*>);
    ErrorOr<FlatPtr> sys$setgroups(size_t, Userspace<const gid_t*>);
    ErrorOr<FlatPtr> sys$pipe(int pipefd[2], int flags);
    ErrorOr<FlatPtr> sys$killpg(pid_t pgrp, int sig);
    ErrorOr<FlatPtr> sys$seteuid(UserID);
    ErrorOr<FlatPtr> sys$setegid(GroupID);
    ErrorOr<FlatPtr> sys$setuid(UserID);
    ErrorOr<FlatPtr> sys$setgid(GroupID);
    ErrorOr<FlatPtr> sys$setreuid(UserID, UserID);
    ErrorOr<FlatPtr> sys$setresuid(UserID, UserID, UserID);
    ErrorOr<FlatPtr> sys$setresgid(GroupID, GroupID, GroupID);
    ErrorOr<FlatPtr> sys$alarm(unsigned seconds);
    ErrorOr<FlatPtr> sys$access(Userspace<const char*> pathname, size_t path_length, int mode);
    ErrorOr<FlatPtr> sys$fcntl(int fd, int cmd, u32 extra_arg);
    ErrorOr<FlatPtr> sys$ioctl(int fd, unsigned request, FlatPtr arg);
    ErrorOr<FlatPtr> sys$mkdir(Userspace<const char*> pathname, size_t path_length, mode_t mode);
    ErrorOr<FlatPtr> sys$times(Userspace<tms*>);
    ErrorOr<FlatPtr> sys$utime(Userspace<const char*> pathname, size_t path_length, Userspace<const struct utimbuf*>);
    ErrorOr<FlatPtr> sys$link(Userspace<const Syscall::SC_link_params*>);
    ErrorOr<FlatPtr> sys$unlink(Userspace<const char*> pathname, size_t path_length);
    ErrorOr<FlatPtr> sys$symlink(Userspace<const Syscall::SC_symlink_params*>);
    ErrorOr<FlatPtr> sys$rmdir(Userspace<const char*> pathname, size_t path_length);
    ErrorOr<FlatPtr> sys$mount(Userspace<const Syscall::SC_mount_params*>);
    ErrorOr<FlatPtr> sys$umount(Userspace<const char*> mountpoint, size_t mountpoint_length);
    ErrorOr<FlatPtr> sys$chmod(Userspace<Syscall::SC_chmod_params const*>);
    ErrorOr<FlatPtr> sys$fchmod(int fd, mode_t);
    ErrorOr<FlatPtr> sys$chown(Userspace<const Syscall::SC_chown_params*>);
    ErrorOr<FlatPtr> sys$fchown(int fd, UserID, GroupID);
    ErrorOr<FlatPtr> sys$fsync(int fd);
    ErrorOr<FlatPtr> sys$socket(int domain, int type, int protocol);
    ErrorOr<FlatPtr> sys$bind(int sockfd, Userspace<const sockaddr*> addr, socklen_t);
    ErrorOr<FlatPtr> sys$listen(int sockfd, int backlog);
    ErrorOr<FlatPtr> sys$accept4(Userspace<const Syscall::SC_accept4_params*>);
    ErrorOr<FlatPtr> sys$connect(int sockfd, Userspace<const sockaddr*>, socklen_t);
    ErrorOr<FlatPtr> sys$shutdown(int sockfd, int how);
    ErrorOr<FlatPtr> sys$sendmsg(int sockfd, Userspace<const struct msghdr*>, int flags);
    ErrorOr<FlatPtr> sys$recvmsg(int sockfd, Userspace<struct msghdr*>, int flags);
    ErrorOr<FlatPtr> sys$getsockopt(Userspace<const Syscall::SC_getsockopt_params*>);
    ErrorOr<FlatPtr> sys$setsockopt(Userspace<const Syscall::SC_setsockopt_params*>);
    ErrorOr<FlatPtr> sys$getsockname(Userspace<const Syscall::SC_getsockname_params*>);
    ErrorOr<FlatPtr> sys$getpeername(Userspace<const Syscall::SC_getpeername_params*>);
    ErrorOr<FlatPtr> sys$socketpair(Userspace<const Syscall::SC_socketpair_params*>);
    ErrorOr<FlatPtr> sys$sched_setparam(pid_t pid, Userspace<const struct sched_param*>);
    ErrorOr<FlatPtr> sys$sched_getparam(pid_t pid, Userspace<struct sched_param*>);
    ErrorOr<FlatPtr> sys$create_thread(void* (*)(void*), Userspace<const Syscall::SC_create_thread_params*>);
    [[noreturn]] void sys$exit_thread(Userspace<void*>, Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$join_thread(pid_t tid, Userspace<void**> exit_value);
    ErrorOr<FlatPtr> sys$detach_thread(pid_t tid);
    ErrorOr<FlatPtr> sys$set_thread_name(pid_t tid, Userspace<const char*> buffer, size_t buffer_size);
    ErrorOr<FlatPtr> sys$get_thread_name(pid_t tid, Userspace<char*> buffer, size_t buffer_size);
    ErrorOr<FlatPtr> sys$kill_thread(pid_t tid, int signal);
    ErrorOr<FlatPtr> sys$rename(Userspace<const Syscall::SC_rename_params*>);
    ErrorOr<FlatPtr> sys$mknod(Userspace<const Syscall::SC_mknod_params*>);
    ErrorOr<FlatPtr> sys$realpath(Userspace<const Syscall::SC_realpath_params*>);
    ErrorOr<FlatPtr> sys$getrandom(Userspace<void*>, size_t, unsigned int);
    ErrorOr<FlatPtr> sys$getkeymap(Userspace<const Syscall::SC_getkeymap_params*>);
    ErrorOr<FlatPtr> sys$setkeymap(Userspace<const Syscall::SC_setkeymap_params*>);
    ErrorOr<FlatPtr> sys$profiling_enable(pid_t, u64);
    ErrorOr<FlatPtr> sys$profiling_disable(pid_t);
    ErrorOr<FlatPtr> sys$profiling_free_buffer(pid_t);
    ErrorOr<FlatPtr> sys$futex(Userspace<const Syscall::SC_futex_params*>);
    ErrorOr<FlatPtr> sys$pledge(Userspace<const Syscall::SC_pledge_params*>);
    ErrorOr<FlatPtr> sys$unveil(Userspace<const Syscall::SC_unveil_params*>);
    ErrorOr<FlatPtr> sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2);
    ErrorOr<FlatPtr> sys$perf_register_string(Userspace<char const*>, size_t);
    ErrorOr<FlatPtr> sys$get_stack_bounds(Userspace<FlatPtr*> stack_base, Userspace<size_t*> stack_size);
    ErrorOr<FlatPtr> sys$ptrace(Userspace<const Syscall::SC_ptrace_params*>);
    ErrorOr<FlatPtr> sys$sendfd(int sockfd, int fd);
    ErrorOr<FlatPtr> sys$recvfd(int sockfd, int options);
    ErrorOr<FlatPtr> sys$sysconf(int name);
    ErrorOr<FlatPtr> sys$disown(ProcessID);
    ErrorOr<FlatPtr> sys$allocate_tls(Userspace<const char*> initial_data, size_t);
    ErrorOr<FlatPtr> sys$prctl(int option, FlatPtr arg1, FlatPtr arg2);
    ErrorOr<FlatPtr> sys$set_coredump_metadata(Userspace<const Syscall::SC_set_coredump_metadata_params*>);
    ErrorOr<FlatPtr> sys$anon_create(size_t, int options);
    ErrorOr<FlatPtr> sys$statvfs(Userspace<const Syscall::SC_statvfs_params*> user_params);
    ErrorOr<FlatPtr> sys$fstatvfs(int fd, statvfs* buf);
    ErrorOr<FlatPtr> sys$map_time_page();

    template<bool sockname, typename Params>
    ErrorOr<void> get_sock_or_peer_name(Params const&);

    static void initialize();

    [[noreturn]] void crash(int signal, FlatPtr ip, bool out_of_memory = false);
    [[nodiscard]] siginfo_t wait_info() const;

    const TTY* tty() const { return m_tty; }
    void set_tty(TTY*);

    u32 m_ticks_in_user { 0 };
    u32 m_ticks_in_kernel { 0 };

    u32 m_ticks_in_user_for_dead_children { 0 };
    u32 m_ticks_in_kernel_for_dead_children { 0 };

    Custody& current_directory();
    Custody* executable() { return m_executable.ptr(); }
    const Custody* executable() const { return m_executable.ptr(); }

    NonnullOwnPtrVector<KString> const& arguments() const { return m_arguments; };
    NonnullOwnPtrVector<KString> const& environment() const { return m_environment; };

    ErrorOr<void> exec(NonnullOwnPtr<KString> path, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, Thread*& new_main_thread, u32& prev_flags, int recursion_depth = 0);

    ErrorOr<LoadResult> load(NonnullRefPtr<OpenFileDescription> main_program_description, RefPtr<OpenFileDescription> interpreter_description, const ElfW(Ehdr) & main_program_header);

    bool is_superuser() const { return euid() == 0; }

    void terminate_due_to_signal(u8 signal);
    ErrorOr<void> send_signal(u8 signal, Process* sender);

    u8 termination_signal() const { return m_protected_values.termination_signal; }

    u16 thread_count() const
    {
        return m_protected_values.thread_count.load(AK::MemoryOrder::memory_order_relaxed);
    }

    Mutex& big_lock() { return m_big_lock; }
    Mutex& ptrace_lock() { return m_ptrace_lock; }

    bool has_promises() const { return m_protected_values.has_promises; }
    bool has_promised(Pledge pledge) const { return (m_protected_values.promises & (1U << (u32)pledge)) != 0; }

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

    ErrorOr<void> peek_user_data(Span<u8> destination, Userspace<const u8*> address);
    ErrorOr<FlatPtr> peek_user_data(Userspace<const FlatPtr*> address);
    ErrorOr<void> poke_user_data(Userspace<FlatPtr*> address, FlatPtr data);

    void disowned_by_waiter(Process& process);
    void unblock_waiters(Thread::WaitBlocker::UnblockFlags, u8 signal = 0);
    Thread::WaitBlockerSet& wait_blocker_set() { return m_wait_blocker_set; }

    template<typename Callback>
    void for_each_coredump_property(Callback callback) const
    {
        for (auto const& property : m_coredump_properties) {
            if (property.key && property.value)
                callback(*property.key, *property.value);
        }
    }

    ErrorOr<void> set_coredump_property(NonnullOwnPtr<KString> key, NonnullOwnPtr<KString> value);
    ErrorOr<void> try_set_coredump_property(StringView key, StringView value);

    const NonnullRefPtrVector<Thread>& threads_for_coredump(Badge<Coredump>) const { return m_threads_for_coredump; }

    PerformanceEventBuffer* perf_events() { return m_perf_event_buffer; }
    PerformanceEventBuffer const* perf_events() const { return m_perf_event_buffer; }

    Memory::AddressSpace& address_space() { return *m_space; }
    Memory::AddressSpace const& address_space() const { return *m_space; }

    VirtualAddress signal_trampoline() const { return m_protected_values.signal_trampoline; }

    ErrorOr<void> require_promise(Pledge);
    ErrorOr<void> require_no_promises() const;

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;
    friend class PerformanceManager;

    bool add_thread(Thread&);
    bool remove_thread(Thread&);

    Process(NonnullOwnPtr<KString> name, UserID, GroupID, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty);
    static ErrorOr<NonnullRefPtr<Process>> try_create(RefPtr<Thread>& first_thread, NonnullOwnPtr<KString> name, UserID, GroupID, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd = nullptr, RefPtr<Custody> executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);
    ErrorOr<void> attach_resources(NonnullOwnPtr<Memory::AddressSpace>&&, RefPtr<Thread>& first_thread, Process* fork_parent);
    static ProcessID allocate_pid();

    void kill_threads_except_self();
    void kill_all_threads();
    ErrorOr<void> dump_core();
    ErrorOr<void> dump_perfcore();
    bool create_perf_events_buffer_if_needed();
    void delete_perf_events_buffer();

    ErrorOr<void> do_exec(NonnullRefPtr<OpenFileDescription> main_program_description, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, RefPtr<OpenFileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags, const ElfW(Ehdr) & main_program_header);
    ErrorOr<FlatPtr> do_write(OpenFileDescription&, const UserOrKernelBuffer&, size_t);

    ErrorOr<FlatPtr> do_statvfs(FileSystem const& path, Custody const*, statvfs* buf);

    ErrorOr<RefPtr<OpenFileDescription>> find_elf_interpreter_for_executable(StringView path, ElfW(Ehdr) const& main_executable_header, size_t main_executable_header_size, size_t file_size);

    ErrorOr<void> do_kill(Process&, int signal);
    ErrorOr<void> do_killpg(ProcessGroupID pgrp, int signal);
    ErrorOr<void> do_killall(int signal);
    ErrorOr<void> do_killself(int signal);

    ErrorOr<siginfo_t> do_waitid(Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee, int options);

    static ErrorOr<NonnullOwnPtr<KString>> get_syscall_path_argument(Userspace<const char*> user_path, size_t path_length);
    static ErrorOr<NonnullOwnPtr<KString>> get_syscall_path_argument(const Syscall::StringArgument&);

    bool has_tracee_thread(ProcessID tracer_pid);

    void clear_futex_queues_on_exec();

    ErrorOr<void> remap_range_as_stack(FlatPtr address, size_t size);

public:
    NonnullRefPtr<ProcessProcFSTraits> procfs_traits() const { return *m_procfs_traits; }
    ErrorOr<void> procfs_get_fds_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_perf_events(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_unveil_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_pledge_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_virtual_memory_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_binary_link(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_current_work_directory_link(KBufferBuilder& builder) const;
    mode_t binary_link_required_mode() const;
    ErrorOr<void> procfs_get_thread_stack(ThreadID thread_id, KBufferBuilder& builder) const;
    ErrorOr<void> traverse_stacks_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<NonnullRefPtr<Inode>> lookup_stacks_directory(const ProcFS&, StringView name) const;
    ErrorOr<size_t> procfs_get_file_description_link(unsigned fd, KBufferBuilder& builder) const;
    ErrorOr<void> traverse_file_descriptions_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<NonnullRefPtr<Inode>> lookup_file_descriptions_directory(const ProcFS&, StringView name) const;
    ErrorOr<void> procfs_get_tty_link(KBufferBuilder& builder) const;

private:
    inline PerformanceEventBuffer* current_perf_events_buffer()
    {
        if (g_profiling_all_threads)
            return g_global_perf_events;
        if (m_profiling)
            return m_perf_event_buffer.ptr();
        return nullptr;
    }

    IntrusiveListNode<Process> m_list_node;

    NonnullOwnPtr<KString> m_name;

    OwnPtr<Memory::AddressSpace> m_space;

    RefPtr<ProcessGroup> m_pg;

    AtomicEdgeAction<u32> m_protected_data_refs;
    void protect_data();
    void unprotect_data();

    OwnPtr<ThreadTracer> m_tracer;

public:
    class OpenFileDescriptionAndFlags {
    public:
        bool is_valid() const { return !m_description.is_null(); }
        bool is_allocated() const { return m_is_allocated; }
        void allocate()
        {
            VERIFY(!m_is_allocated);
            VERIFY(!is_valid());
            m_is_allocated = true;
        }
        void deallocate()
        {
            VERIFY(m_is_allocated);
            VERIFY(!is_valid());
            m_is_allocated = false;
        }

        OpenFileDescription* description() { return m_description; }
        const OpenFileDescription* description() const { return m_description; }
        u32 flags() const { return m_flags; }
        void set_flags(u32 flags) { m_flags = flags; }

        void clear();
        void set(NonnullRefPtr<OpenFileDescription>&&, u32 flags = 0);

    private:
        RefPtr<OpenFileDescription> m_description;
        bool m_is_allocated { false };
        u32 m_flags { 0 };
    };

    class ScopedDescriptionAllocation;
    class OpenFileDescriptions {
        AK_MAKE_NONCOPYABLE(OpenFileDescriptions);
        AK_MAKE_NONMOVABLE(OpenFileDescriptions);
        friend class Process;

    public:
        OpenFileDescriptions() { }
        ALWAYS_INLINE const OpenFileDescriptionAndFlags& operator[](size_t i) const { return at(i); }
        ALWAYS_INLINE OpenFileDescriptionAndFlags& operator[](size_t i) { return at(i); }

        ErrorOr<void> try_clone(const Kernel::Process::OpenFileDescriptions& other)
        {
            TRY(try_resize(other.m_fds_metadatas.size()));

            for (size_t i = 0; i < other.m_fds_metadatas.size(); ++i) {
                m_fds_metadatas[i] = other.m_fds_metadatas[i];
            }
            return {};
        }

        const OpenFileDescriptionAndFlags& at(size_t i) const;
        OpenFileDescriptionAndFlags& at(size_t i);

        OpenFileDescriptionAndFlags const* get_if_valid(size_t i) const;
        OpenFileDescriptionAndFlags* get_if_valid(size_t i);

        void enumerate(Function<void(const OpenFileDescriptionAndFlags&)>) const;
        void change_each(Function<void(OpenFileDescriptionAndFlags&)>);

        ErrorOr<ScopedDescriptionAllocation> allocate(int first_candidate_fd = 0);
        size_t open_count() const;

        ErrorOr<void> try_resize(size_t size) { return m_fds_metadatas.try_resize(size); }

        static constexpr size_t max_open()
        {
            return s_max_open_file_descriptors;
        }

        void clear()
        {
            m_fds_metadatas.clear();
        }

        ErrorOr<NonnullRefPtr<OpenFileDescription>> open_file_description(int fd) const;

    private:
        static constexpr size_t s_max_open_file_descriptors { FD_SETSIZE };
        Vector<OpenFileDescriptionAndFlags> m_fds_metadatas;
    };

    class ScopedDescriptionAllocation {
        AK_MAKE_NONCOPYABLE(ScopedDescriptionAllocation);

    public:
        ScopedDescriptionAllocation() = default;
        ScopedDescriptionAllocation(int tracked_fd, OpenFileDescriptionAndFlags* description)
            : fd(tracked_fd)
            , m_description(description)
        {
        }

        ScopedDescriptionAllocation(ScopedDescriptionAllocation&& other)
            : fd(other.fd)
        {
            // Take over the responsibility of tracking to deallocation.
            swap(m_description, other.m_description);
        }

        ScopedDescriptionAllocation& operator=(ScopedDescriptionAllocation&& other)
        {
            if (this != &other) {
                m_description = exchange(other.m_description, nullptr);
                fd = exchange(other.fd, -1);
            }
            return *this;
        }

        ~ScopedDescriptionAllocation()
        {
            if (m_description && m_description->is_allocated() && !m_description->is_valid()) {
                m_description->deallocate();
            }
        }

        int fd { -1 };

    private:
        OpenFileDescriptionAndFlags* m_description { nullptr };
    };

    class ProcessProcFSTraits : public ProcFSExposedComponent {
    public:
        static ErrorOr<NonnullRefPtr<ProcessProcFSTraits>> try_create(Badge<Process>, Process& process)
        {
            return adopt_nonnull_ref_or_enomem(new (nothrow) ProcessProcFSTraits(process));
        }

        virtual InodeIndex component_index() const override;
        virtual ErrorOr<NonnullRefPtr<Inode>> to_inode(const ProcFS& procfs_instance) const override;
        virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
        virtual mode_t required_mode() const override { return 0555; }

        virtual UserID owner_user() const override;
        virtual GroupID owner_group() const override;

    private:
        explicit ProcessProcFSTraits(Process& process)
            : m_process(process.make_weak_ptr())
        {
        }

        // NOTE: We need to weakly hold on to the process, because otherwise
        //       we would be creating a reference cycle.
        WeakPtr<Process> m_process;
    };

    MutexProtected<OpenFileDescriptions>& fds() { return m_fds; }
    MutexProtected<OpenFileDescriptions> const& fds() const { return m_fds; }

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open_file_description(int fd)
    {
        return m_fds.with_shared([fd](auto& fds) { return fds.open_file_description(fd); });
    }

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open_file_description(int fd) const
    {
        return m_fds.with_shared([fd](auto& fds) { return fds.open_file_description(fd); });
    }

    ErrorOr<ScopedDescriptionAllocation> allocate_fd()
    {
        return m_fds.with_exclusive([](auto& fds) { return fds.allocate(); });
    }

private:
    SpinlockProtected<Thread::ListInProcess>& thread_list() { return m_thread_list; }
    SpinlockProtected<Thread::ListInProcess> const& thread_list() const { return m_thread_list; }

    SpinlockProtected<Thread::ListInProcess> m_thread_list;

    MutexProtected<OpenFileDescriptions> m_fds;

    const bool m_is_kernel_process;
    Atomic<State> m_state { State::Running };
    bool m_profiling { false };
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_stopped { false };
    bool m_should_generate_coredump { false };

    RefPtr<Custody> m_executable;
    RefPtr<Custody> m_cwd;

    NonnullOwnPtrVector<KString> m_arguments;
    NonnullOwnPtrVector<KString> m_environment;

    RefPtr<TTY> m_tty;

    WeakPtr<Memory::Region> m_master_tls_region;
    size_t m_master_tls_size { 0 };
    size_t m_master_tls_alignment { 0 };

    Mutex m_big_lock { "Process" };
    Mutex m_ptrace_lock { "ptrace" };

    RefPtr<Timer> m_alarm_timer;

    VeilState m_veil_state { VeilState::None };
    UnveilNode m_unveiled_paths { "/", { .full_path = "/" } };

    OwnPtr<PerformanceEventBuffer> m_perf_event_buffer;

    FutexQueues m_futex_queues;
    Spinlock m_futex_lock;

    // This member is used in the implementation of ptrace's PT_TRACEME flag.
    // If it is set to true, the process will stop at the next execve syscall
    // and wait for a tracer to attach.
    bool m_wait_for_tracer_at_next_execve { false };

    Thread::WaitBlockerSet m_wait_blocker_set;

    struct CoredumpProperty {
        OwnPtr<KString> key;
        OwnPtr<KString> value;
    };

    Array<CoredumpProperty, 4> m_coredump_properties;
    NonnullRefPtrVector<Thread> m_threads_for_coredump;

    mutable RefPtr<ProcessProcFSTraits> m_procfs_traits;

    static_assert(sizeof(ProtectedValues) < (PAGE_SIZE));
    alignas(4096) ProtectedValues m_protected_values;
    u8 m_protected_values_padding[PAGE_SIZE - sizeof(ProtectedValues)];

public:
    using List = IntrusiveListRelaxedConst<&Process::m_list_node>;
    static SpinlockProtected<Process::List>& all_instances();
};

// Note: Process object should be 2 pages of 4096 bytes each.
// It's not expected that the Process object will expand further because the first
// page is used for all unprotected values (which should be plenty of space for them).
// The second page is being used exclusively for write-protected values.
static_assert(AssertSize<Process, (PAGE_SIZE * 2)>());

extern RecursiveSpinlock g_profiling_lock;

template<IteratorFunction<Process&> Callback>
inline void Process::for_each(Callback callback)
{
    VERIFY_INTERRUPTS_DISABLED();
    Process::all_instances().with([&](const auto& list) {
        for (auto it = list.begin(); it != list.end();) {
            auto& process = *it;
            ++it;
            if (callback(process) == IterationDecision::Break)
                break;
        }
    });
}

template<IteratorFunction<Process&> Callback>
inline void Process::for_each_child(Callback callback)
{
    ProcessID my_pid = pid();
    Process::all_instances().with([&](const auto& list) {
        for (auto it = list.begin(); it != list.end();) {
            auto& process = *it;
            ++it;
            if (process.ppid() == my_pid || process.has_tracee_thread(pid())) {
                if (callback(process) == IterationDecision::Break)
                    break;
            }
        }
    });
}

template<IteratorFunction<Thread&> Callback>
inline IterationDecision Process::for_each_thread(Callback callback) const
{
    return thread_list().with([&](auto& thread_list) -> IterationDecision {
        for (auto& thread : thread_list) {
            IterationDecision decision = callback(thread);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    });
}

template<IteratorFunction<Thread&> Callback>
inline IterationDecision Process::for_each_thread(Callback callback)
{
    return thread_list().with([&](auto& thread_list) -> IterationDecision {
        for (auto& thread : thread_list) {
            IterationDecision decision = callback(thread);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    });
}

template<IteratorFunction<Process&> Callback>
inline void Process::for_each_in_pgrp(ProcessGroupID pgid, Callback callback)
{
    Process::all_instances().with([&](const auto& list) {
        for (auto it = list.begin(); it != list.end();) {
            auto& process = *it;
            ++it;
            if (!process.is_dead() && process.pgid() == pgid) {
                if (callback(process) == IterationDecision::Break)
                    break;
            }
        }
    });
}

template<VoidFunction<Process&> Callback>
inline void Process::for_each(Callback callback)
{
    return for_each([&](auto& item) {
        callback(item);
        return IterationDecision::Continue;
    });
}

template<VoidFunction<Process&> Callback>
inline void Process::for_each_child(Callback callback)
{
    return for_each_child([&](auto& item) {
        callback(item);
        return IterationDecision::Continue;
    });
}

template<VoidFunction<Thread&> Callback>
inline IterationDecision Process::for_each_thread(Callback callback) const
{
    thread_list().with([&](auto& thread_list) {
        for (auto& thread : thread_list)
            callback(thread);
    });
    return IterationDecision::Continue;
}

template<VoidFunction<Thread&> Callback>
inline IterationDecision Process::for_each_thread(Callback callback)
{
    thread_list().with([&](auto& thread_list) {
        for (auto& thread : thread_list)
            callback(thread);
    });
    return IterationDecision::Continue;
}

template<VoidFunction<Process&> Callback>
inline void Process::for_each_in_pgrp(ProcessGroupID pgid, Callback callback)
{
    return for_each_in_pgrp(pgid, [&](auto& item) {
        callback(item);
        return IterationDecision::Continue;
    });
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

}

#define VERIFY_PROCESS_BIG_LOCK_ACQUIRED(process) \
    VERIFY(process->big_lock().is_exclusively_locked_by_current_thread());

#define VERIFY_NO_PROCESS_BIG_LOCK(process) \
    VERIFY(!process->big_lock().is_exclusively_locked_by_current_thread());

inline static ErrorOr<NonnullOwnPtr<KString>> try_copy_kstring_from_user(const Kernel::Syscall::StringArgument& string)
{
    Userspace<char const*> characters((FlatPtr)string.characters);
    return try_copy_kstring_from_user(characters, string.length);
}

template<>
struct AK::Formatter<Kernel::Process> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::Process const& value)
    {
        return AK::Formatter<FormatString>::format(builder, "{}({})", value.name(), value.pid().value());
    }
};
