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
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/AtomicEdgeAction.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/InodeMetadata.h>
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

MutexProtected<String>& hostname();
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

typedef HashMap<FlatPtr, RefPtr<FutexQueue>> FutexQueues;

struct LoadResult;

class Process final
    : public AK::RefCountedBase
    , public Weakable<Process> {

    class ProtectedValues {
    public:
        ProcessID pid { 0 };
        ProcessID ppid { 0 };
        SessionID sid { 0 };
        uid_t euid { 0 };
        gid_t egid { 0 };
        uid_t uid { 0 };
        gid_t gid { 0 };
        uid_t suid { 0 };
        gid_t sgid { 0 };
        Vector<gid_t> extra_gids;
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
    friend class ProcFSProcessFileDescriptions;

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
        auto current_thread = Processor::current_thread();
        VERIFY(current_thread);
        return current_thread->process();
    }

    inline static bool has_current()
    {
        return Processor::current_thread();
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
    static RefPtr<Process> create_kernel_process(RefPtr<Thread>& first_thread, String&& name, EntryFunction entry, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_process(first_thread, move(name), &Process::kernel_process_trampoline<EntryFunction>, entry_func, affinity, do_register);
    }

    static RefPtr<Process> create_kernel_process(RefPtr<Thread>& first_thread, String&& name, void (*entry)(void*), void* entry_data = nullptr, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes);
    static RefPtr<Process> create_user_process(RefPtr<Thread>& first_thread, const String& path, uid_t, gid_t, ProcessID ppid, int& error, Vector<String>&& arguments = Vector<String>(), Vector<String>&& environment = Vector<String>(), TTY* = nullptr);
    static void register_new(Process&);

    bool unref() const;
    ~Process();

    static NonnullRefPtrVector<Process> all_processes();

    RefPtr<Thread> create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, OwnPtr<KString> name, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true);

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

    const String& name() const { return m_name; }
    ProcessID pid() const { return m_protected_values.pid; }
    SessionID sid() const { return m_protected_values.sid; }
    bool is_session_leader() const { return sid().value() == pid().value(); }
    ProcessGroupID pgid() const { return m_pg ? m_pg->pgid() : 0; }
    bool is_group_leader() const { return pgid().value() == pid().value(); }
    const Vector<gid_t>& extra_gids() const { return m_protected_values.extra_gids; }
    uid_t euid() const { return m_protected_values.euid; }
    gid_t egid() const { return m_protected_values.egid; }
    uid_t uid() const { return m_protected_values.uid; }
    gid_t gid() const { return m_protected_values.gid; }
    uid_t suid() const { return m_protected_values.suid; }
    gid_t sgid() const { return m_protected_values.sgid; }
    ProcessID ppid() const { return m_protected_values.ppid; }

    bool is_dumpable() const { return m_protected_values.dumpable; }
    void set_dumpable(bool);

    mode_t umask() const { return m_protected_values.umask; }

    bool in_group(gid_t) const;

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
    KResult start_tracing_from(ProcessID tracer);
    void stop_tracing();
    void tracer_trap(Thread&, const RegisterState&);

    KResultOr<FlatPtr> sys$emuctl();
    KResultOr<FlatPtr> sys$yield();
    KResultOr<FlatPtr> sys$sync();
    KResultOr<FlatPtr> sys$beep();
    KResultOr<FlatPtr> sys$get_process_name(Userspace<char*> buffer, size_t buffer_size);
    KResultOr<FlatPtr> sys$set_process_name(Userspace<const char*> user_name, size_t user_name_length);
    KResultOr<FlatPtr> sys$create_inode_watcher(u32 flags);
    KResultOr<FlatPtr> sys$inode_watcher_add_watch(Userspace<const Syscall::SC_inode_watcher_add_watch_params*> user_params);
    KResultOr<FlatPtr> sys$inode_watcher_remove_watch(int fd, int wd);
    KResultOr<FlatPtr> sys$dbgputch(u8);
    KResultOr<FlatPtr> sys$dbgputstr(Userspace<const char*>, size_t);
    KResultOr<FlatPtr> sys$dump_backtrace();
    KResultOr<FlatPtr> sys$gettid();
    KResultOr<FlatPtr> sys$setsid();
    KResultOr<FlatPtr> sys$getsid(pid_t);
    KResultOr<FlatPtr> sys$setpgid(pid_t pid, pid_t pgid);
    KResultOr<FlatPtr> sys$getpgrp();
    KResultOr<FlatPtr> sys$getpgid(pid_t);
    KResultOr<FlatPtr> sys$getuid();
    KResultOr<FlatPtr> sys$getgid();
    KResultOr<FlatPtr> sys$geteuid();
    KResultOr<FlatPtr> sys$getegid();
    KResultOr<FlatPtr> sys$getpid();
    KResultOr<FlatPtr> sys$getppid();
    KResultOr<FlatPtr> sys$getresuid(Userspace<uid_t*>, Userspace<uid_t*>, Userspace<uid_t*>);
    KResultOr<FlatPtr> sys$getresgid(Userspace<gid_t*>, Userspace<gid_t*>, Userspace<gid_t*>);
    KResultOr<FlatPtr> sys$umask(mode_t);
    KResultOr<FlatPtr> sys$open(Userspace<const Syscall::SC_open_params*>);
    KResultOr<FlatPtr> sys$close(int fd);
    KResultOr<FlatPtr> sys$read(int fd, Userspace<u8*>, size_t);
    KResultOr<FlatPtr> sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count);
    KResultOr<FlatPtr> sys$write(int fd, Userspace<const u8*>, size_t);
    KResultOr<FlatPtr> sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count);
    KResultOr<FlatPtr> sys$fstat(int fd, Userspace<stat*>);
    KResultOr<FlatPtr> sys$stat(Userspace<const Syscall::SC_stat_params*>);
    KResultOr<FlatPtr> sys$lseek(int fd, Userspace<off_t*>, int whence);
    KResultOr<FlatPtr> sys$ftruncate(int fd, Userspace<off_t*>);
    KResultOr<FlatPtr> sys$kill(pid_t pid_or_pgid, int sig);
    [[noreturn]] void sys$exit(int status);
    KResultOr<FlatPtr> sys$sigreturn(RegisterState& registers);
    KResultOr<FlatPtr> sys$waitid(Userspace<const Syscall::SC_waitid_params*>);
    KResultOr<FlatPtr> sys$mmap(Userspace<const Syscall::SC_mmap_params*>);
    KResultOr<FlatPtr> sys$mremap(Userspace<const Syscall::SC_mremap_params*>);
    KResultOr<FlatPtr> sys$munmap(Userspace<void*>, size_t);
    KResultOr<FlatPtr> sys$set_mmap_name(Userspace<const Syscall::SC_set_mmap_name_params*>);
    KResultOr<FlatPtr> sys$mprotect(Userspace<void*>, size_t, int prot);
    KResultOr<FlatPtr> sys$madvise(Userspace<void*>, size_t, int advice);
    KResultOr<FlatPtr> sys$msyscall(Userspace<void*>);
    KResultOr<FlatPtr> sys$purge(int mode);
    KResultOr<FlatPtr> sys$select(Userspace<const Syscall::SC_select_params*>);
    KResultOr<FlatPtr> sys$poll(Userspace<const Syscall::SC_poll_params*>);
    KResultOr<FlatPtr> sys$get_dir_entries(int fd, Userspace<void*>, size_t);
    KResultOr<FlatPtr> sys$getcwd(Userspace<char*>, size_t);
    KResultOr<FlatPtr> sys$chdir(Userspace<const char*>, size_t);
    KResultOr<FlatPtr> sys$fchdir(int fd);
    KResultOr<FlatPtr> sys$adjtime(Userspace<const timeval*>, Userspace<timeval*>);
    KResultOr<FlatPtr> sys$clock_gettime(clockid_t, Userspace<timespec*>);
    KResultOr<FlatPtr> sys$clock_settime(clockid_t, Userspace<const timespec*>);
    KResultOr<FlatPtr> sys$clock_nanosleep(Userspace<const Syscall::SC_clock_nanosleep_params*>);
    KResultOr<FlatPtr> sys$gethostname(Userspace<char*>, size_t);
    KResultOr<FlatPtr> sys$sethostname(Userspace<const char*>, size_t);
    KResultOr<FlatPtr> sys$uname(Userspace<utsname*>);
    KResultOr<FlatPtr> sys$readlink(Userspace<const Syscall::SC_readlink_params*>);
    KResultOr<FlatPtr> sys$ttyname(int fd, Userspace<char*>, size_t);
    KResultOr<FlatPtr> sys$ptsname(int fd, Userspace<char*>, size_t);
    KResultOr<FlatPtr> sys$fork(RegisterState&);
    KResultOr<FlatPtr> sys$execve(Userspace<const Syscall::SC_execve_params*>);
    KResultOr<FlatPtr> sys$dup2(int old_fd, int new_fd);
    KResultOr<FlatPtr> sys$sigaction(int signum, Userspace<const sigaction*> act, Userspace<sigaction*> old_act);
    KResultOr<FlatPtr> sys$sigprocmask(int how, Userspace<const sigset_t*> set, Userspace<sigset_t*> old_set);
    KResultOr<FlatPtr> sys$sigpending(Userspace<sigset_t*>);
    KResultOr<FlatPtr> sys$getgroups(size_t, Userspace<gid_t*>);
    KResultOr<FlatPtr> sys$setgroups(size_t, Userspace<const gid_t*>);
    KResultOr<FlatPtr> sys$pipe(int pipefd[2], int flags);
    KResultOr<FlatPtr> sys$killpg(pid_t pgrp, int sig);
    KResultOr<FlatPtr> sys$seteuid(uid_t);
    KResultOr<FlatPtr> sys$setegid(gid_t);
    KResultOr<FlatPtr> sys$setuid(uid_t);
    KResultOr<FlatPtr> sys$setgid(gid_t);
    KResultOr<FlatPtr> sys$setreuid(uid_t, uid_t);
    KResultOr<FlatPtr> sys$setresuid(uid_t, uid_t, uid_t);
    KResultOr<FlatPtr> sys$setresgid(gid_t, gid_t, gid_t);
    KResultOr<FlatPtr> sys$alarm(unsigned seconds);
    KResultOr<FlatPtr> sys$access(Userspace<const char*> pathname, size_t path_length, int mode);
    KResultOr<FlatPtr> sys$fcntl(int fd, int cmd, u32 extra_arg);
    KResultOr<FlatPtr> sys$ioctl(int fd, unsigned request, FlatPtr arg);
    KResultOr<FlatPtr> sys$mkdir(Userspace<const char*> pathname, size_t path_length, mode_t mode);
    KResultOr<FlatPtr> sys$times(Userspace<tms*>);
    KResultOr<FlatPtr> sys$utime(Userspace<const char*> pathname, size_t path_length, Userspace<const struct utimbuf*>);
    KResultOr<FlatPtr> sys$link(Userspace<const Syscall::SC_link_params*>);
    KResultOr<FlatPtr> sys$unlink(Userspace<const char*> pathname, size_t path_length);
    KResultOr<FlatPtr> sys$symlink(Userspace<const Syscall::SC_symlink_params*>);
    KResultOr<FlatPtr> sys$rmdir(Userspace<const char*> pathname, size_t path_length);
    KResultOr<FlatPtr> sys$mount(Userspace<const Syscall::SC_mount_params*>);
    KResultOr<FlatPtr> sys$umount(Userspace<const char*> mountpoint, size_t mountpoint_length);
    KResultOr<FlatPtr> sys$chmod(Userspace<const char*> pathname, size_t path_length, mode_t);
    KResultOr<FlatPtr> sys$fchmod(int fd, mode_t);
    KResultOr<FlatPtr> sys$chown(Userspace<const Syscall::SC_chown_params*>);
    KResultOr<FlatPtr> sys$fchown(int fd, uid_t, gid_t);
    KResultOr<FlatPtr> sys$socket(int domain, int type, int protocol);
    KResultOr<FlatPtr> sys$bind(int sockfd, Userspace<const sockaddr*> addr, socklen_t);
    KResultOr<FlatPtr> sys$listen(int sockfd, int backlog);
    KResultOr<FlatPtr> sys$accept4(Userspace<const Syscall::SC_accept4_params*>);
    KResultOr<FlatPtr> sys$connect(int sockfd, Userspace<const sockaddr*>, socklen_t);
    KResultOr<FlatPtr> sys$shutdown(int sockfd, int how);
    KResultOr<FlatPtr> sys$sendmsg(int sockfd, Userspace<const struct msghdr*>, int flags);
    KResultOr<FlatPtr> sys$recvmsg(int sockfd, Userspace<struct msghdr*>, int flags);
    KResultOr<FlatPtr> sys$getsockopt(Userspace<const Syscall::SC_getsockopt_params*>);
    KResultOr<FlatPtr> sys$setsockopt(Userspace<const Syscall::SC_setsockopt_params*>);
    KResultOr<FlatPtr> sys$getsockname(Userspace<const Syscall::SC_getsockname_params*>);
    KResultOr<FlatPtr> sys$getpeername(Userspace<const Syscall::SC_getpeername_params*>);
    KResultOr<FlatPtr> sys$socketpair(Userspace<const Syscall::SC_socketpair_params*>);
    KResultOr<FlatPtr> sys$sched_setparam(pid_t pid, Userspace<const struct sched_param*>);
    KResultOr<FlatPtr> sys$sched_getparam(pid_t pid, Userspace<struct sched_param*>);
    KResultOr<FlatPtr> sys$create_thread(void* (*)(void*), Userspace<const Syscall::SC_create_thread_params*>);
    [[noreturn]] void sys$exit_thread(Userspace<void*>, Userspace<void*>, size_t);
    KResultOr<FlatPtr> sys$join_thread(pid_t tid, Userspace<void**> exit_value);
    KResultOr<FlatPtr> sys$detach_thread(pid_t tid);
    KResultOr<FlatPtr> sys$set_thread_name(pid_t tid, Userspace<const char*> buffer, size_t buffer_size);
    KResultOr<FlatPtr> sys$get_thread_name(pid_t tid, Userspace<char*> buffer, size_t buffer_size);
    KResultOr<FlatPtr> sys$kill_thread(pid_t tid, int signal);
    KResultOr<FlatPtr> sys$rename(Userspace<const Syscall::SC_rename_params*>);
    KResultOr<FlatPtr> sys$mknod(Userspace<const Syscall::SC_mknod_params*>);
    KResultOr<FlatPtr> sys$halt();
    KResultOr<FlatPtr> sys$reboot();
    KResultOr<FlatPtr> sys$realpath(Userspace<const Syscall::SC_realpath_params*>);
    KResultOr<FlatPtr> sys$getrandom(Userspace<void*>, size_t, unsigned int);
    KResultOr<FlatPtr> sys$getkeymap(Userspace<const Syscall::SC_getkeymap_params*>);
    KResultOr<FlatPtr> sys$setkeymap(Userspace<const Syscall::SC_setkeymap_params*>);
    KResultOr<FlatPtr> sys$module_load(Userspace<const char*> path, size_t path_length);
    KResultOr<FlatPtr> sys$module_unload(Userspace<const char*> name, size_t name_length);
    KResultOr<FlatPtr> sys$profiling_enable(pid_t, u64);
    KResultOr<FlatPtr> sys$profiling_disable(pid_t);
    KResultOr<FlatPtr> sys$profiling_free_buffer(pid_t);
    KResultOr<FlatPtr> sys$futex(Userspace<const Syscall::SC_futex_params*>);
    KResultOr<FlatPtr> sys$pledge(Userspace<const Syscall::SC_pledge_params*>);
    KResultOr<FlatPtr> sys$unveil(Userspace<const Syscall::SC_unveil_params*>);
    KResultOr<FlatPtr> sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2);
    KResultOr<FlatPtr> sys$perf_register_string(Userspace<char const*>, size_t);
    KResultOr<FlatPtr> sys$get_stack_bounds(Userspace<FlatPtr*> stack_base, Userspace<size_t*> stack_size);
    KResultOr<FlatPtr> sys$ptrace(Userspace<const Syscall::SC_ptrace_params*>);
    KResultOr<FlatPtr> sys$sendfd(int sockfd, int fd);
    KResultOr<FlatPtr> sys$recvfd(int sockfd, int options);
    KResultOr<FlatPtr> sys$sysconf(int name);
    KResultOr<FlatPtr> sys$disown(ProcessID);
    KResultOr<FlatPtr> sys$allocate_tls(Userspace<const char*> initial_data, size_t);
    KResultOr<FlatPtr> sys$prctl(int option, FlatPtr arg1, FlatPtr arg2);
    KResultOr<FlatPtr> sys$set_coredump_metadata(Userspace<const Syscall::SC_set_coredump_metadata_params*>);
    KResultOr<FlatPtr> sys$anon_create(size_t, int options);
    KResultOr<FlatPtr> sys$statvfs(Userspace<const Syscall::SC_statvfs_params*> user_params);
    KResultOr<FlatPtr> sys$fstatvfs(int fd, statvfs* buf);
    KResultOr<FlatPtr> sys$map_time_page();

    template<bool sockname, typename Params>
    int get_sock_or_peer_name(const Params&);

    static void initialize();

    [[noreturn]] void crash(int signal, FlatPtr ip, bool out_of_memory = false);
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

    KResult exec(String path, Vector<String> arguments, Vector<String> environment, int recusion_depth = 0);

    KResultOr<LoadResult> load(NonnullRefPtr<FileDescription> main_program_description, RefPtr<FileDescription> interpreter_description, const ElfW(Ehdr) & main_program_header);

    bool is_superuser() const { return euid() == 0; }

    void terminate_due_to_signal(u8 signal);
    KResult send_signal(u8 signal, Process* sender);

    u8 termination_signal() const { return m_protected_values.termination_signal; }

    u16 thread_count() const
    {
        return m_protected_values.thread_count.load(AK::MemoryOrder::memory_order_relaxed);
    }

    Mutex& big_lock() { return m_big_lock; }
    Mutex& ptrace_lock() { return m_ptrace_lock; }

    bool has_promises() const { return m_protected_values.has_promises; }
    bool has_promised(Pledge pledge) const { return m_protected_values.promises & (1u << (u32)pledge); }

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

    template<typename Callback>
    void for_each_coredump_property(Callback callback) const
    {
        for (auto& property : m_coredump_properties) {
            if (property.key && property.value)
                callback(*property.key, *property.value);
        }
    }

    KResult set_coredump_property(NonnullOwnPtr<KString> key, NonnullOwnPtr<KString> value);
    KResult try_set_coredump_property(StringView key, StringView value);

    const NonnullRefPtrVector<Thread>& threads_for_coredump(Badge<Coredump>) const { return m_threads_for_coredump; }

    PerformanceEventBuffer* perf_events() { return m_perf_event_buffer; }

    Memory::AddressSpace& address_space() { return *m_space; }
    Memory::AddressSpace const& address_space() const { return *m_space; }

    VirtualAddress signal_trampoline() const { return m_protected_values.signal_trampoline; }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;
    friend class PerformanceManager;

    bool add_thread(Thread&);
    bool remove_thread(Thread&);

    Process(const String& name, uid_t uid, gid_t gid, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty);
    static RefPtr<Process> create(RefPtr<Thread>& first_thread, const String& name, uid_t, gid_t, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd = nullptr, RefPtr<Custody> executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);
    KResult attach_resources(NonnullOwnPtr<Memory::AddressSpace>&&, RefPtr<Thread>& first_thread, Process* fork_parent);
    static ProcessID allocate_pid();

    void kill_threads_except_self();
    void kill_all_threads();
    bool dump_core();
    bool dump_perfcore();
    bool create_perf_events_buffer_if_needed();
    void delete_perf_events_buffer();

    KResult do_exec(NonnullRefPtr<FileDescription> main_program_description, Vector<String> arguments, Vector<String> environment, RefPtr<FileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags, const ElfW(Ehdr) & main_program_header);
    KResultOr<FlatPtr> do_write(FileDescription&, const UserOrKernelBuffer&, size_t);

    KResultOr<FlatPtr> do_statvfs(String path, statvfs* buf);

    KResultOr<RefPtr<FileDescription>> find_elf_interpreter_for_executable(const String& path, const ElfW(Ehdr) & elf_header, int nread, size_t file_size);

    KResult do_kill(Process&, int signal);
    KResult do_killpg(ProcessGroupID pgrp, int signal);
    KResult do_killall(int signal);
    KResult do_killself(int signal);

    KResultOr<siginfo_t> do_waitid(idtype_t idtype, int id, int options);

    KResultOr<NonnullOwnPtr<KString>> get_syscall_path_argument(Userspace<const char*> user_path, size_t path_length) const;
    KResultOr<NonnullOwnPtr<KString>> get_syscall_path_argument(const Syscall::StringArgument&) const;

    bool has_tracee_thread(ProcessID tracer_pid);

    void clear_futex_queues_on_exec();

    void setup_socket_fd(int fd, NonnullRefPtr<FileDescription> description, int type);

public:
    NonnullRefPtr<ProcessProcFSTraits> procfs_traits() const { return *m_procfs_traits; }
    KResult procfs_get_fds_stats(KBufferBuilder& builder) const;
    KResult procfs_get_perf_events(KBufferBuilder& builder) const;
    KResult procfs_get_unveil_stats(KBufferBuilder& builder) const;
    KResult procfs_get_pledge_stats(KBufferBuilder& builder) const;
    KResult procfs_get_virtual_memory_stats(KBufferBuilder& builder) const;
    KResult procfs_get_binary_link(KBufferBuilder& builder) const;
    KResult procfs_get_current_work_directory_link(KBufferBuilder& builder) const;
    mode_t binary_link_required_mode() const;
    KResultOr<size_t> procfs_get_thread_stack(ThreadID thread_id, KBufferBuilder& builder) const;
    KResult traverse_stacks_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const;
    KResultOr<NonnullRefPtr<Inode>> lookup_stacks_directory(const ProcFS&, StringView name) const;
    KResultOr<size_t> procfs_get_file_description_link(unsigned fd, KBufferBuilder& builder) const;
    KResult traverse_file_descriptions_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const;
    KResultOr<NonnullRefPtr<Inode>> lookup_file_descriptions_directory(const ProcFS&, StringView name) const;

private:
    inline PerformanceEventBuffer* current_perf_events_buffer()
    {
        if (g_profiling_all_threads)
            return g_global_perf_events;
        else if (m_profiling)
            return m_perf_event_buffer.ptr();
        else
            return nullptr;
    }

    mutable IntrusiveListNode<Process> m_list_node;

    String m_name;

    OwnPtr<Memory::AddressSpace> m_space;

    RefPtr<ProcessGroup> m_pg;

    AtomicEdgeAction<u32> m_protected_data_refs;
    void protect_data();
    void unprotect_data();

    OwnPtr<ThreadTracer> m_tracer;

public:
    class FileDescriptionAndFlags {
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

        FileDescription* description() { return m_description; }
        const FileDescription* description() const { return m_description; }
        u32 flags() const { return m_flags; }
        void set_flags(u32 flags) { m_flags = flags; }

        void clear();
        void set(NonnullRefPtr<FileDescription>&&, u32 flags = 0);

    private:
        RefPtr<FileDescription> m_description;
        bool m_is_allocated { false };
        u32 m_flags { 0 };
    };

    class ScopedDescriptionAllocation;
    class FileDescriptions {
        AK_MAKE_NONCOPYABLE(FileDescriptions);
        friend class Process;

    public:
        ALWAYS_INLINE const FileDescriptionAndFlags& operator[](size_t i) const { return at(i); }
        ALWAYS_INLINE FileDescriptionAndFlags& operator[](size_t i) { return at(i); }

        KResult try_clone(const Kernel::Process::FileDescriptions& other)
        {
            SpinlockLocker lock_other(other.m_fds_lock);
            if (!try_resize(other.m_fds_metadatas.size()))
                return ENOMEM;

            for (size_t i = 0; i < other.m_fds_metadatas.size(); ++i) {
                m_fds_metadatas[i] = other.m_fds_metadatas[i];
            }
            return KSuccess;
        }

        const FileDescriptionAndFlags& at(size_t i) const;
        FileDescriptionAndFlags& at(size_t i);

        FileDescriptionAndFlags const* get_if_valid(size_t i) const;
        FileDescriptionAndFlags* get_if_valid(size_t i);

        void enumerate(Function<void(const FileDescriptionAndFlags&)>) const;
        void change_each(Function<void(FileDescriptionAndFlags&)>);

        KResultOr<ScopedDescriptionAllocation> allocate(int first_candidate_fd = 0);
        size_t open_count() const;

        bool try_resize(size_t size) { return m_fds_metadatas.try_resize(size); }

        size_t max_open() const
        {
            return m_max_open_file_descriptors;
        }

        void clear()
        {
            SpinlockLocker lock(m_fds_lock);
            m_fds_metadatas.clear();
        }

        // FIXME: Consider to remove this somehow
        RefPtr<FileDescription> file_description(int fd) const;

    private:
        FileDescriptions() = default;
        static constexpr size_t m_max_open_file_descriptors { FD_SETSIZE };
        mutable Spinlock<u8> m_fds_lock;
        Vector<FileDescriptionAndFlags> m_fds_metadatas;
    };

    class ScopedDescriptionAllocation {
        AK_MAKE_NONCOPYABLE(ScopedDescriptionAllocation);

    public:
        ScopedDescriptionAllocation() = default;
        ScopedDescriptionAllocation(int tracked_fd, FileDescriptionAndFlags* description)
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

        ~ScopedDescriptionAllocation()
        {
            if (m_description && m_description->is_allocated() && !m_description->is_valid()) {
                m_description->deallocate();
            }
        }

        const int fd { -1 };

    private:
        FileDescriptionAndFlags* m_description { nullptr };
    };

    class ProcessProcFSTraits : public ProcFSExposedComponent {
    public:
        static KResultOr<NonnullRefPtr<ProcessProcFSTraits>> try_create(Badge<Process>, WeakPtr<Process> process)
        {
            return adopt_nonnull_ref_or_enomem(new (nothrow) ProcessProcFSTraits(process));
        }

        virtual InodeIndex component_index() const override;
        virtual KResultOr<NonnullRefPtr<Inode>> to_inode(const ProcFS& procfs_instance) const override;
        virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
        virtual mode_t required_mode() const override { return 0555; }

        virtual uid_t owner_user() const override;

        virtual gid_t owner_group() const override;

    private:
        ProcessProcFSTraits(WeakPtr<Process> process)
            : m_process(process)
        {
        }

        // NOTE: We need to weakly hold on to the process, because otherwise
        //       we would be creating a reference cycle.
        WeakPtr<Process> m_process;
    };

    FileDescriptions& fds() { return m_fds; }
    const FileDescriptions& fds() const { return m_fds; }

private:
    SpinlockProtected<Thread::ListInProcess>& thread_list() { return m_thread_list; }
    SpinlockProtected<Thread::ListInProcess> const& thread_list() const { return m_thread_list; }

    SpinlockProtected<Thread::ListInProcess> m_thread_list;

    FileDescriptions m_fds;

    const bool m_is_kernel_process;
    Atomic<State> m_state { State::Running };
    bool m_profiling { false };
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_stopped { false };
    bool m_should_generate_coredump { false };

    RefPtr<Custody> m_executable;
    RefPtr<Custody> m_cwd;

    Vector<String> m_arguments;
    Vector<String> m_environment;

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
    Spinlock<u8> m_futex_lock;

    // This member is used in the implementation of ptrace's PT_TRACEME flag.
    // If it is set to true, the process will stop at the next execve syscall
    // and wait for a tracer to attach.
    bool m_wait_for_tracer_at_next_execve { false };

    Thread::WaitBlockCondition m_wait_block_condition;

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
    using List = IntrusiveListRelaxedConst<Process, RawPtr<Process>, &Process::m_list_node>;
};

// Note: Process object should be 2 pages of 4096 bytes each.
// It's not expected that the Process object will expand further because the first
// page is used for all unprotected values (which should be plenty of space for them).
// The second page is being used exclusively for write-protected values.
static_assert(sizeof(Process) == (PAGE_SIZE * 2));

extern RecursiveSpinlock g_profiling_lock;

MutexProtected<Process::List>& processes();

template<IteratorFunction<Process&> Callback>
inline void Process::for_each(Callback callback)
{
    VERIFY_INTERRUPTS_DISABLED();
    processes().with_shared([&](const auto& list) {
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
    processes().with_shared([&](const auto& list) {
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
    processes().with_shared([&](const auto& list) {
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

#define REQUIRE_NO_PROMISES                       \
    do {                                          \
        if (Process::current().has_promises()) {  \
            dbgln("Has made a promise");          \
            Process::current().crash(SIGABRT, 0); \
            VERIFY_NOT_REACHED();                 \
        }                                         \
    } while (0)

#define REQUIRE_PROMISE(promise)                                    \
    do {                                                            \
        if (Process::current().has_promises()                       \
            && !Process::current().has_promised(Pledge::promise)) { \
            dbgln("Has not pledged {}", #promise);                  \
            (void)Process::current().try_set_coredump_property(     \
                "pledge_violation"sv, #promise);                    \
            Process::current().crash(SIGABRT, 0);                   \
            VERIFY_NOT_REACHED();                                   \
        }                                                           \
    } while (0)
}

#define VERIFY_PROCESS_BIG_LOCK_ACQUIRED(process) \
    VERIFY(process->big_lock().own_lock());

#define VERIFY_NO_PROCESS_BIG_LOCK(process) \
    VERIFY(!process->big_lock().own_lock());

inline static KResultOr<NonnullOwnPtr<KString>> try_copy_kstring_from_user(const Kernel::Syscall::StringArgument& string)
{
    Userspace<char const*> characters((FlatPtr)string.characters);
    return try_copy_kstring_from_user(characters, string.length);
}

template<>
struct AK::Formatter<Kernel::Process> : AK::Formatter<String> {
    void format(FormatBuilder& builder, const Kernel::Process& value)
    {
        return AK::Formatter<String>::format(builder, String::formatted("{}({})", value.name(), value.pid().value()));
    }
};
