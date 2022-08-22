/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Userspace.h>
#include <AK/Variant.h>
#include <Kernel/API/POSIX/sys/resource.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Assertions.h>
#include <Kernel/AtomicEdgeAction.h>
#include <Kernel/Credentials.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/Forward.h>
#include <Kernel/FutexQueue.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Library/NonnullLockRefPtrVector.h>
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
    __ENUMERATE_PLEDGE_PROMISE(getkeymap) \
    __ENUMERATE_PLEDGE_PROMISE(no_error)

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

static constexpr FlatPtr futex_key_private_flag = 0b1;
union GlobalFutexKey {
    struct {
        Memory::VMObject const* vmobject;
        FlatPtr offset;
    } shared;
    struct {
        Memory::AddressSpace const* address_space;
        FlatPtr user_address;
    } private_;
    struct {
        FlatPtr parent;
        FlatPtr offset;
    } raw;
};
static_assert(sizeof(GlobalFutexKey) == (sizeof(FlatPtr) * 2));

struct LoadResult;

class Process final
    : public ListedRefCounted<Process, LockType::Spinlock>
    , public LockWeakable<Process> {

    class ProtectedValues {
    public:
        ProcessID pid { 0 };
        ProcessID ppid { 0 };
        SessionID sid { 0 };
        // FIXME: This should be a NonnullRefPtr
        RefPtr<Credentials> credentials;
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

    auto with_protected_data(auto&& callback) const
    {
        SpinlockLocker locker(m_protected_data_lock);
        return callback(m_protected_values_do_not_access_directly);
    }

    auto with_mutable_protected_data(auto&& callback)
    {
        SpinlockLocker locker(m_protected_data_lock);
        unprotect_data();
        auto guard = ScopeGuard([&] { protect_data(); });
        return callback(m_protected_values_do_not_access_directly);
    }

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
    static LockRefPtr<Process> create_kernel_process(LockRefPtr<Thread>& first_thread, NonnullOwnPtr<KString> name, EntryFunction entry, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_process(first_thread, move(name), &Process::kernel_process_trampoline<EntryFunction>, entry_func, affinity, do_register);
    }

    static LockRefPtr<Process> create_kernel_process(LockRefPtr<Thread>& first_thread, NonnullOwnPtr<KString> name, void (*entry)(void*), void* entry_data = nullptr, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes);
    static ErrorOr<NonnullLockRefPtr<Process>> try_create_user_process(LockRefPtr<Thread>& first_thread, StringView path, UserID, GroupID, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, TTY*);
    static void register_new(Process&);

    ~Process();

    LockRefPtr<Thread> create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, NonnullOwnPtr<KString> name, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true);

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

    static LockRefPtr<Process> from_pid(ProcessID);
    static SessionID get_sid_from_pgid(ProcessGroupID pgid);

    StringView name() const { return m_name->view(); }
    ProcessID pid() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.pid; });
    }
    SessionID sid() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.sid; });
    }
    bool is_session_leader() const { return sid().value() == pid().value(); }
    ProcessGroupID pgid() const { return m_pg ? m_pg->pgid() : 0; }
    bool is_group_leader() const { return pgid().value() == pid().value(); }
    ProcessID ppid() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.ppid; });
    }

    NonnullRefPtr<Credentials> credentials() const;

    bool is_dumpable() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.dumpable; });
    }
    void set_dumpable(bool);

    mode_t umask() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.umask; });
    }

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
    ErrorOr<void> try_for_each_thread(Function<ErrorOr<void>(Thread const&)>) const;

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
    void tracer_trap(Thread&, RegisterState const&);

    ErrorOr<FlatPtr> sys$emuctl();
    ErrorOr<FlatPtr> sys$yield();
    ErrorOr<FlatPtr> sys$sync();
    ErrorOr<FlatPtr> sys$beep();
    ErrorOr<FlatPtr> sys$get_process_name(Userspace<char*> buffer, size_t buffer_size);
    ErrorOr<FlatPtr> sys$set_process_name(Userspace<char const*> user_name, size_t user_name_length);
    ErrorOr<FlatPtr> sys$create_inode_watcher(u32 flags);
    ErrorOr<FlatPtr> sys$inode_watcher_add_watch(Userspace<Syscall::SC_inode_watcher_add_watch_params const*> user_params);
    ErrorOr<FlatPtr> sys$inode_watcher_remove_watch(int fd, int wd);
    ErrorOr<FlatPtr> sys$dbgputstr(Userspace<char const*>, size_t);
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
    ErrorOr<FlatPtr> sys$getrusage(int, Userspace<rusage*>);
    ErrorOr<FlatPtr> sys$umask(mode_t);
    ErrorOr<FlatPtr> sys$open(Userspace<Syscall::SC_open_params const*>);
    ErrorOr<FlatPtr> sys$close(int fd);
    ErrorOr<FlatPtr> sys$read(int fd, Userspace<u8*>, size_t);
    ErrorOr<FlatPtr> sys$pread(int fd, Userspace<u8*>, size_t, Userspace<off_t const*>);
    ErrorOr<FlatPtr> sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count);
    ErrorOr<FlatPtr> sys$write(int fd, Userspace<u8 const*>, size_t);
    ErrorOr<FlatPtr> sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count);
    ErrorOr<FlatPtr> sys$fstat(int fd, Userspace<stat*>);
    ErrorOr<FlatPtr> sys$stat(Userspace<Syscall::SC_stat_params const*>);
    ErrorOr<FlatPtr> sys$lseek(int fd, Userspace<off_t*>, int whence);
    ErrorOr<FlatPtr> sys$ftruncate(int fd, Userspace<off_t const*>);
    ErrorOr<FlatPtr> sys$posix_fallocate(int fd, Userspace<off_t const*>, Userspace<off_t const*>);
    ErrorOr<FlatPtr> sys$kill(pid_t pid_or_pgid, int sig);
    [[noreturn]] void sys$exit(int status);
    ErrorOr<FlatPtr> sys$sigreturn(RegisterState& registers);
    ErrorOr<FlatPtr> sys$waitid(Userspace<Syscall::SC_waitid_params const*>);
    ErrorOr<FlatPtr> sys$mmap(Userspace<Syscall::SC_mmap_params const*>);
    ErrorOr<FlatPtr> sys$mremap(Userspace<Syscall::SC_mremap_params const*>);
    ErrorOr<FlatPtr> sys$munmap(Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$set_mmap_name(Userspace<Syscall::SC_set_mmap_name_params const*>);
    ErrorOr<FlatPtr> sys$mprotect(Userspace<void*>, size_t, int prot);
    ErrorOr<FlatPtr> sys$madvise(Userspace<void*>, size_t, int advice);
    ErrorOr<FlatPtr> sys$msyscall(Userspace<void*>);
    ErrorOr<FlatPtr> sys$msync(Userspace<void*>, size_t, int flags);
    ErrorOr<FlatPtr> sys$purge(int mode);
    ErrorOr<FlatPtr> sys$poll(Userspace<Syscall::SC_poll_params const*>);
    ErrorOr<FlatPtr> sys$get_dir_entries(int fd, Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$getcwd(Userspace<char*>, size_t);
    ErrorOr<FlatPtr> sys$chdir(Userspace<char const*>, size_t);
    ErrorOr<FlatPtr> sys$fchdir(int fd);
    ErrorOr<FlatPtr> sys$adjtime(Userspace<timeval const*>, Userspace<timeval*>);
    ErrorOr<FlatPtr> sys$clock_gettime(clockid_t, Userspace<timespec*>);
    ErrorOr<FlatPtr> sys$clock_settime(clockid_t, Userspace<timespec const*>);
    ErrorOr<FlatPtr> sys$clock_nanosleep(Userspace<Syscall::SC_clock_nanosleep_params const*>);
    ErrorOr<FlatPtr> sys$clock_getres(Userspace<Syscall::SC_clock_getres_params const*>);
    ErrorOr<FlatPtr> sys$gethostname(Userspace<char*>, size_t);
    ErrorOr<FlatPtr> sys$sethostname(Userspace<char const*>, size_t);
    ErrorOr<FlatPtr> sys$uname(Userspace<utsname*>);
    ErrorOr<FlatPtr> sys$readlink(Userspace<Syscall::SC_readlink_params const*>);
    ErrorOr<FlatPtr> sys$fork(RegisterState&);
    ErrorOr<FlatPtr> sys$execve(Userspace<Syscall::SC_execve_params const*>);
    ErrorOr<FlatPtr> sys$dup2(int old_fd, int new_fd);
    ErrorOr<FlatPtr> sys$sigaction(int signum, Userspace<sigaction const*> act, Userspace<sigaction*> old_act);
    ErrorOr<FlatPtr> sys$sigaltstack(Userspace<stack_t const*> ss, Userspace<stack_t*> old_ss);
    ErrorOr<FlatPtr> sys$sigprocmask(int how, Userspace<sigset_t const*> set, Userspace<sigset_t*> old_set);
    ErrorOr<FlatPtr> sys$sigpending(Userspace<sigset_t*>);
    ErrorOr<FlatPtr> sys$sigsuspend(Userspace<sigset_t const*>);
    ErrorOr<FlatPtr> sys$sigtimedwait(Userspace<sigset_t const*>, Userspace<siginfo_t*>, Userspace<timespec const*>);
    ErrorOr<FlatPtr> sys$getgroups(size_t, Userspace<GroupID*>);
    ErrorOr<FlatPtr> sys$setgroups(size_t, Userspace<GroupID const*>);
    ErrorOr<FlatPtr> sys$pipe(Userspace<int*>, int flags);
    ErrorOr<FlatPtr> sys$killpg(pid_t pgrp, int sig);
    ErrorOr<FlatPtr> sys$seteuid(UserID);
    ErrorOr<FlatPtr> sys$setegid(GroupID);
    ErrorOr<FlatPtr> sys$setuid(UserID);
    ErrorOr<FlatPtr> sys$setgid(GroupID);
    ErrorOr<FlatPtr> sys$setreuid(UserID, UserID);
    ErrorOr<FlatPtr> sys$setresuid(UserID, UserID, UserID);
    ErrorOr<FlatPtr> sys$setresgid(GroupID, GroupID, GroupID);
    ErrorOr<FlatPtr> sys$alarm(unsigned seconds);
    ErrorOr<FlatPtr> sys$access(Userspace<char const*> pathname, size_t path_length, int mode);
    ErrorOr<FlatPtr> sys$fcntl(int fd, int cmd, uintptr_t extra_arg);
    ErrorOr<FlatPtr> sys$ioctl(int fd, unsigned request, FlatPtr arg);
    ErrorOr<FlatPtr> sys$mkdir(Userspace<char const*> pathname, size_t path_length, mode_t mode);
    ErrorOr<FlatPtr> sys$times(Userspace<tms*>);
    ErrorOr<FlatPtr> sys$utime(Userspace<char const*> pathname, size_t path_length, Userspace<const struct utimbuf*>);
    ErrorOr<FlatPtr> sys$utimensat(Userspace<Syscall::SC_utimensat_params const*>);
    ErrorOr<FlatPtr> sys$link(Userspace<Syscall::SC_link_params const*>);
    ErrorOr<FlatPtr> sys$unlink(int dirfd, Userspace<char const*> pathname, size_t path_length, int flags);
    ErrorOr<FlatPtr> sys$symlink(Userspace<Syscall::SC_symlink_params const*>);
    ErrorOr<FlatPtr> sys$rmdir(Userspace<char const*> pathname, size_t path_length);
    ErrorOr<FlatPtr> sys$mount(Userspace<Syscall::SC_mount_params const*>);
    ErrorOr<FlatPtr> sys$umount(Userspace<char const*> mountpoint, size_t mountpoint_length);
    ErrorOr<FlatPtr> sys$chmod(Userspace<Syscall::SC_chmod_params const*>);
    ErrorOr<FlatPtr> sys$fchmod(int fd, mode_t);
    ErrorOr<FlatPtr> sys$chown(Userspace<Syscall::SC_chown_params const*>);
    ErrorOr<FlatPtr> sys$fchown(int fd, UserID, GroupID);
    ErrorOr<FlatPtr> sys$fsync(int fd);
    ErrorOr<FlatPtr> sys$socket(int domain, int type, int protocol);
    ErrorOr<FlatPtr> sys$bind(int sockfd, Userspace<sockaddr const*> addr, socklen_t);
    ErrorOr<FlatPtr> sys$listen(int sockfd, int backlog);
    ErrorOr<FlatPtr> sys$accept4(Userspace<Syscall::SC_accept4_params const*>);
    ErrorOr<FlatPtr> sys$connect(int sockfd, Userspace<sockaddr const*>, socklen_t);
    ErrorOr<FlatPtr> sys$shutdown(int sockfd, int how);
    ErrorOr<FlatPtr> sys$sendmsg(int sockfd, Userspace<const struct msghdr*>, int flags);
    ErrorOr<FlatPtr> sys$recvmsg(int sockfd, Userspace<struct msghdr*>, int flags);
    ErrorOr<FlatPtr> sys$getsockopt(Userspace<Syscall::SC_getsockopt_params const*>);
    ErrorOr<FlatPtr> sys$setsockopt(Userspace<Syscall::SC_setsockopt_params const*>);
    ErrorOr<FlatPtr> sys$getsockname(Userspace<Syscall::SC_getsockname_params const*>);
    ErrorOr<FlatPtr> sys$getpeername(Userspace<Syscall::SC_getpeername_params const*>);
    ErrorOr<FlatPtr> sys$socketpair(Userspace<Syscall::SC_socketpair_params const*>);
    ErrorOr<FlatPtr> sys$sched_setparam(pid_t pid, Userspace<const struct sched_param*>);
    ErrorOr<FlatPtr> sys$sched_getparam(pid_t pid, Userspace<struct sched_param*>);
    ErrorOr<FlatPtr> sys$create_thread(void* (*)(void*), Userspace<Syscall::SC_create_thread_params const*>);
    [[noreturn]] void sys$exit_thread(Userspace<void*>, Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$join_thread(pid_t tid, Userspace<void**> exit_value);
    ErrorOr<FlatPtr> sys$detach_thread(pid_t tid);
    ErrorOr<FlatPtr> sys$set_thread_name(pid_t tid, Userspace<char const*> buffer, size_t buffer_size);
    ErrorOr<FlatPtr> sys$get_thread_name(pid_t tid, Userspace<char*> buffer, size_t buffer_size);
    ErrorOr<FlatPtr> sys$kill_thread(pid_t tid, int signal);
    ErrorOr<FlatPtr> sys$rename(Userspace<Syscall::SC_rename_params const*>);
    ErrorOr<FlatPtr> sys$mknod(Userspace<Syscall::SC_mknod_params const*>);
    ErrorOr<FlatPtr> sys$realpath(Userspace<Syscall::SC_realpath_params const*>);
    ErrorOr<FlatPtr> sys$getrandom(Userspace<void*>, size_t, unsigned int);
    ErrorOr<FlatPtr> sys$getkeymap(Userspace<Syscall::SC_getkeymap_params const*>);
    ErrorOr<FlatPtr> sys$setkeymap(Userspace<Syscall::SC_setkeymap_params const*>);
    ErrorOr<FlatPtr> sys$profiling_enable(pid_t, Userspace<u64 const*>);
    ErrorOr<FlatPtr> sys$profiling_disable(pid_t);
    ErrorOr<FlatPtr> sys$profiling_free_buffer(pid_t);
    ErrorOr<FlatPtr> sys$futex(Userspace<Syscall::SC_futex_params const*>);
    ErrorOr<FlatPtr> sys$pledge(Userspace<Syscall::SC_pledge_params const*>);
    ErrorOr<FlatPtr> sys$unveil(Userspace<Syscall::SC_unveil_params const*>);
    ErrorOr<FlatPtr> sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2);
    ErrorOr<FlatPtr> sys$perf_register_string(Userspace<char const*>, size_t);
    ErrorOr<FlatPtr> sys$get_stack_bounds(Userspace<FlatPtr*> stack_base, Userspace<size_t*> stack_size);
    ErrorOr<FlatPtr> sys$ptrace(Userspace<Syscall::SC_ptrace_params const*>);
    ErrorOr<FlatPtr> sys$sendfd(int sockfd, int fd);
    ErrorOr<FlatPtr> sys$recvfd(int sockfd, int options);
    ErrorOr<FlatPtr> sys$sysconf(int name);
    ErrorOr<FlatPtr> sys$disown(ProcessID);
    ErrorOr<FlatPtr> sys$allocate_tls(Userspace<char const*> initial_data, size_t);
    ErrorOr<FlatPtr> sys$prctl(int option, FlatPtr arg1, FlatPtr arg2);
    ErrorOr<FlatPtr> sys$set_coredump_metadata(Userspace<Syscall::SC_set_coredump_metadata_params const*>);
    ErrorOr<FlatPtr> sys$anon_create(size_t, int options);
    ErrorOr<FlatPtr> sys$statvfs(Userspace<Syscall::SC_statvfs_params const*> user_params);
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

    NonnullRefPtr<Custody> current_directory();
    RefPtr<Custody> executable();
    RefPtr<Custody const> executable() const;

    static constexpr size_t max_arguments_size = Thread::default_userspace_stack_size / 8;
    static constexpr size_t max_environment_size = Thread::default_userspace_stack_size / 8;
    NonnullOwnPtrVector<KString> const& arguments() const { return m_arguments; };
    NonnullOwnPtrVector<KString> const& environment() const { return m_environment; };

    ErrorOr<void> exec(NonnullOwnPtr<KString> path, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, Thread*& new_main_thread, u32& prev_flags, int recursion_depth = 0);

    ErrorOr<LoadResult> load(NonnullLockRefPtr<OpenFileDescription> main_program_description, LockRefPtr<OpenFileDescription> interpreter_description, const ElfW(Ehdr) & main_program_header);

    void terminate_due_to_signal(u8 signal);
    ErrorOr<void> send_signal(u8 signal, Process* sender);

    u8 termination_signal() const
    {
        return with_protected_data([](auto& protected_data) -> u8 {
            return protected_data.termination_signal;
        });
    }
    u8 termination_status() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.termination_status; });
    }

    u16 thread_count() const
    {
        return with_protected_data([](auto& protected_data) {
            return protected_data.thread_count.load(AK::MemoryOrder::memory_order_relaxed);
        });
    }

    Mutex& big_lock() { return m_big_lock; }
    Mutex& ptrace_lock() { return m_ptrace_lock; }

    bool has_promises() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.has_promises.load(); });
    }
    bool has_promised(Pledge pledge) const
    {
        return with_protected_data([&](auto& protected_data) {
            return (protected_data.promises & (1U << (u32)pledge)) != 0;
        });
    }

    VeilState veil_state() const
    {
        return m_unveil_data.with([&](auto const& unveil_data) { return unveil_data.state; });
    }

    struct UnveilData {
        explicit UnveilData(UnveilNode&& p)
            : paths(move(p))
        {
        }
        VeilState state { VeilState::None };
        UnveilNode paths;
    };

    auto& unveil_data() { return m_unveil_data; }
    auto const& unveil_data() const { return m_unveil_data; }

    bool wait_for_tracer_at_next_execve() const
    {
        return m_wait_for_tracer_at_next_execve;
    }
    void set_wait_for_tracer_at_next_execve(bool val)
    {
        m_wait_for_tracer_at_next_execve = val;
    }

    ErrorOr<void> peek_user_data(Span<u8> destination, Userspace<u8 const*> address);
    ErrorOr<FlatPtr> peek_user_data(Userspace<FlatPtr const*> address);
    ErrorOr<void> poke_user_data(Userspace<FlatPtr*> address, FlatPtr data);

    void disowned_by_waiter(Process& process);
    void unblock_waiters(Thread::WaitBlocker::UnblockFlags, u8 signal = 0);
    Thread::WaitBlockerSet& wait_blocker_set() { return m_wait_blocker_set; }

    template<typename Callback>
    ErrorOr<void> for_each_coredump_property(Callback callback) const
    {
        return m_coredump_properties.with([&](auto const& coredump_properties) -> ErrorOr<void> {
            for (auto const& property : coredump_properties) {
                if (property.key && property.value)
                    TRY(callback(*property.key, *property.value));
            }

            return {};
        });
    }

    ErrorOr<void> set_coredump_property(NonnullOwnPtr<KString> key, NonnullOwnPtr<KString> value);
    ErrorOr<void> try_set_coredump_property(StringView key, StringView value);

    NonnullLockRefPtrVector<Thread> const& threads_for_coredump(Badge<Coredump>) const { return m_threads_for_coredump; }

    PerformanceEventBuffer* perf_events() { return m_perf_event_buffer; }
    PerformanceEventBuffer const* perf_events() const { return m_perf_event_buffer; }

    Memory::AddressSpace& address_space() { return *m_space; }
    Memory::AddressSpace const& address_space() const { return *m_space; }

    VirtualAddress signal_trampoline() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.signal_trampoline; });
    }

    ErrorOr<void> require_promise(Pledge);
    ErrorOr<void> require_no_promises() const;

    ErrorOr<void> validate_mmap_prot(int prot, bool map_stack, bool map_anonymous, Memory::Region const* region = nullptr) const;
    ErrorOr<void> validate_inode_mmap_prot(int prot, Inode const& inode, bool map_shared) const;

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;
    friend class PerformanceManager;

    bool add_thread(Thread&);
    bool remove_thread(Thread&);

    Process(NonnullOwnPtr<KString> name, NonnullRefPtr<Credentials>, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> current_directory, RefPtr<Custody> executable, TTY* tty, UnveilNode unveil_tree);
    static ErrorOr<NonnullLockRefPtr<Process>> try_create(LockRefPtr<Thread>& first_thread, NonnullOwnPtr<KString> name, UserID, GroupID, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> current_directory = nullptr, RefPtr<Custody> executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);
    ErrorOr<void> attach_resources(NonnullOwnPtr<Memory::AddressSpace>&&, LockRefPtr<Thread>& first_thread, Process* fork_parent);
    static ProcessID allocate_pid();

    void kill_threads_except_self();
    void kill_all_threads();
    ErrorOr<void> dump_core();
    ErrorOr<void> dump_perfcore();
    bool create_perf_events_buffer_if_needed();
    void delete_perf_events_buffer();

    ErrorOr<void> do_exec(NonnullLockRefPtr<OpenFileDescription> main_program_description, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, LockRefPtr<OpenFileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags, const ElfW(Ehdr) & main_program_header);
    ErrorOr<FlatPtr> do_write(OpenFileDescription&, UserOrKernelBuffer const&, size_t);

    ErrorOr<FlatPtr> do_statvfs(FileSystem const& path, Custody const*, statvfs* buf);

    ErrorOr<LockRefPtr<OpenFileDescription>> find_elf_interpreter_for_executable(StringView path, ElfW(Ehdr) const& main_executable_header, size_t main_executable_header_size, size_t file_size);

    ErrorOr<void> do_kill(Process&, int signal);
    ErrorOr<void> do_killpg(ProcessGroupID pgrp, int signal);
    ErrorOr<void> do_killall(int signal);
    ErrorOr<void> do_killself(int signal);

    ErrorOr<siginfo_t> do_waitid(Variant<Empty, NonnullLockRefPtr<Process>, NonnullLockRefPtr<ProcessGroup>> waitee, int options);

    static ErrorOr<NonnullOwnPtr<KString>> get_syscall_path_argument(Userspace<char const*> user_path, size_t path_length);
    static ErrorOr<NonnullOwnPtr<KString>> get_syscall_path_argument(Syscall::StringArgument const&);

    bool has_tracee_thread(ProcessID tracer_pid);

    void clear_signal_handlers_for_exec();
    void clear_futex_queues_on_exec();

    ErrorOr<GlobalFutexKey> get_futex_key(FlatPtr user_address, bool shared);

    ErrorOr<void> remap_range_as_stack(FlatPtr address, size_t size);

    ErrorOr<FlatPtr> read_impl(int fd, Userspace<u8*> buffer, size_t size);

public:
    NonnullLockRefPtr<ProcessProcFSTraits> procfs_traits() const { return *m_procfs_traits; }
    ErrorOr<void> procfs_get_fds_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_perf_events(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_unveil_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_pledge_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_virtual_memory_stats(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_binary_link(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_current_work_directory_link(KBufferBuilder& builder) const;
    ErrorOr<void> procfs_get_command_line(KBufferBuilder& builder) const;
    mode_t binary_link_required_mode() const;
    ErrorOr<void> procfs_get_thread_stack(ThreadID thread_id, KBufferBuilder& builder) const;
    ErrorOr<void> traverse_stacks_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<NonnullLockRefPtr<Inode>> lookup_stacks_directory(ProcFS const&, StringView name) const;
    ErrorOr<size_t> procfs_get_file_description_link(unsigned fd, KBufferBuilder& builder) const;
    ErrorOr<void> traverse_file_descriptions_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<NonnullLockRefPtr<Inode>> lookup_file_descriptions_directory(ProcFS const&, StringView name) const;
    ErrorOr<NonnullLockRefPtr<Inode>> lookup_children_directory(ProcFS const&, StringView name) const;
    ErrorOr<void> traverse_children_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<size_t> procfs_get_child_proccess_link(ProcessID child_pid, KBufferBuilder& builder) const;

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

    LockRefPtr<ProcessGroup> m_pg;

    RecursiveSpinlock mutable m_protected_data_lock;
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
        OpenFileDescription const* description() const { return m_description; }
        u32 flags() const { return m_flags; }
        void set_flags(u32 flags) { m_flags = flags; }

        void clear();
        void set(NonnullLockRefPtr<OpenFileDescription>&&, u32 flags = 0);

    private:
        LockRefPtr<OpenFileDescription> m_description;
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
        ALWAYS_INLINE OpenFileDescriptionAndFlags const& operator[](size_t i) const { return at(i); }
        ALWAYS_INLINE OpenFileDescriptionAndFlags& operator[](size_t i) { return at(i); }

        ErrorOr<void> try_clone(Kernel::Process::OpenFileDescriptions const& other)
        {
            TRY(try_resize(other.m_fds_metadatas.size()));

            for (size_t i = 0; i < other.m_fds_metadatas.size(); ++i) {
                m_fds_metadatas[i] = other.m_fds_metadatas[i];
            }
            return {};
        }

        OpenFileDescriptionAndFlags const& at(size_t i) const;
        OpenFileDescriptionAndFlags& at(size_t i);

        OpenFileDescriptionAndFlags const* get_if_valid(size_t i) const;
        OpenFileDescriptionAndFlags* get_if_valid(size_t i);

        void enumerate(Function<void(OpenFileDescriptionAndFlags const&)>) const;
        ErrorOr<void> try_enumerate(Function<ErrorOr<void>(OpenFileDescriptionAndFlags const&)>) const;
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

        ErrorOr<NonnullLockRefPtr<OpenFileDescription>> open_file_description(int fd) const;

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
        static ErrorOr<NonnullLockRefPtr<ProcessProcFSTraits>> try_create(Badge<Process>, LockWeakPtr<Process> process)
        {
            return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcessProcFSTraits(move(process)));
        }

        virtual InodeIndex component_index() const override;
        virtual ErrorOr<NonnullLockRefPtr<Inode>> to_inode(ProcFS const& procfs_instance) const override;
        virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
        virtual mode_t required_mode() const override { return 0555; }

        virtual UserID owner_user() const override;
        virtual GroupID owner_group() const override;

    private:
        explicit ProcessProcFSTraits(LockWeakPtr<Process> process)
            : m_process(move(process))
        {
        }

        // NOTE: We need to weakly hold on to the process, because otherwise
        //       we would be creating a reference cycle.
        LockWeakPtr<Process> m_process;
    };

    MutexProtected<OpenFileDescriptions>& fds() { return m_fds; }
    MutexProtected<OpenFileDescriptions> const& fds() const { return m_fds; }

    ErrorOr<NonnullLockRefPtr<OpenFileDescription>> open_file_description(int fd)
    {
        return m_fds.with_shared([fd](auto& fds) { return fds.open_file_description(fd); });
    }

    ErrorOr<NonnullLockRefPtr<OpenFileDescription>> open_file_description(int fd) const
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

    SpinlockProtected<Thread::ListInProcess> m_thread_list { LockRank::None };

    MutexProtected<OpenFileDescriptions> m_fds;

    bool const m_is_kernel_process;
    Atomic<State> m_state { State::Running };
    bool m_profiling { false };
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_stopped { false };
    bool m_should_generate_coredump { false };

    SpinlockProtected<RefPtr<Custody>> m_executable;

    SpinlockProtected<RefPtr<Custody>> m_current_directory;

    NonnullOwnPtrVector<KString> m_arguments;
    NonnullOwnPtrVector<KString> m_environment;

    LockRefPtr<TTY> m_tty;

    LockWeakPtr<Memory::Region> m_master_tls_region;
    size_t m_master_tls_size { 0 };
    size_t m_master_tls_alignment { 0 };

    Mutex m_big_lock { "Process"sv, Mutex::MutexBehavior::BigLock };
    Mutex m_ptrace_lock { "ptrace"sv };

    LockRefPtr<Timer> m_alarm_timer;

    SpinlockProtected<UnveilData> m_unveil_data;

    OwnPtr<PerformanceEventBuffer> m_perf_event_buffer;

    // This member is used in the implementation of ptrace's PT_TRACEME flag.
    // If it is set to true, the process will stop at the next execve syscall
    // and wait for a tracer to attach.
    bool m_wait_for_tracer_at_next_execve { false };

    Thread::WaitBlockerSet m_wait_blocker_set;

    struct CoredumpProperty {
        OwnPtr<KString> key;
        OwnPtr<KString> value;
    };

    SpinlockProtected<Array<CoredumpProperty, 4>> m_coredump_properties { LockRank::None };
    NonnullLockRefPtrVector<Thread> m_threads_for_coredump;

    mutable LockRefPtr<ProcessProcFSTraits> m_procfs_traits;
    struct SignalActionData {
        VirtualAddress handler_or_sigaction;
        int flags { 0 };
        u32 mask { 0 };
    };
    Array<SignalActionData, NSIG> m_signal_action_data;

    static_assert(sizeof(ProtectedValues) < (PAGE_SIZE));
    alignas(4096) ProtectedValues m_protected_values_do_not_access_directly;
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
    Process::all_instances().with([&](auto const& list) {
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
    Process::all_instances().with([&](auto const& list) {
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
    Process::all_instances().with([&](auto const& list) {
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

inline ErrorOr<void> Process::try_for_each_thread(Function<ErrorOr<void>(Thread const&)> callback) const
{
    return thread_list().with([&](auto& thread_list) -> ErrorOr<void> {
        for (auto& thread : thread_list)
            TRY(callback(thread));
        return {};
    });
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

inline ProcessID Thread::pid() const
{
    return m_process->pid();
}

}

#define VERIFY_PROCESS_BIG_LOCK_ACQUIRED(process) \
    VERIFY(process->big_lock().is_exclusively_locked_by_current_thread())

#define VERIFY_NO_PROCESS_BIG_LOCK(process) \
    VERIFY(!process->big_lock().is_exclusively_locked_by_current_thread())

inline static ErrorOr<NonnullOwnPtr<KString>> try_copy_kstring_from_user(Kernel::Syscall::StringArgument const& string)
{
    Userspace<char const*> characters((FlatPtr)string.characters);
    return try_copy_kstring_from_user(characters, string.length);
}

template<>
struct AK::Formatter<Kernel::Process> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::Process const& value)
    {
        return AK::Formatter<FormatString>::format(builder, "{}({})"sv, value.name(), value.pid().value());
    }
};

namespace AK {
template<>
struct Traits<Kernel::GlobalFutexKey> : public GenericTraits<Kernel::GlobalFutexKey> {
    static unsigned hash(Kernel::GlobalFutexKey const& futex_key) { return pair_int_hash(ptr_hash(futex_key.raw.parent), ptr_hash(futex_key.raw.offset)); }
    static bool equals(Kernel::GlobalFutexKey const& a, Kernel::GlobalFutexKey const& b) { return a.raw.parent == b.raw.parent && a.raw.offset == b.raw.offset; }
};
};
