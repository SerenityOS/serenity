/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/FixedStringBuffer.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/SetOnce.h>
#include <AK/Userspace.h>
#include <AK/Variant.h>
#include <Kernel/API/POSIX/select.h>
#include <Kernel/API/POSIX/sys/resource.h>
#include <Kernel/API/Syscall.h>
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
#    include <Kernel/Devices/KCOVInstance.h>
#endif
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/Assertions.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Memory/AddressSpace.h>
#include <Kernel/Security/Credentials.h>
#include <Kernel/Tasks/AtomicEdgeAction.h>
#include <Kernel/Tasks/FutexQueue.h>
#include <Kernel/Tasks/HostnameContext.h>
#include <Kernel/Tasks/PerformanceEventBuffer.h>
#include <Kernel/Tasks/ProcessGroup.h>
#include <Kernel/Tasks/Thread.h>
#include <Kernel/UnixTypes.h>
#include <LibELF/ELFABI.h>

namespace Kernel {

UnixDateTime kgettimeofday();

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
    __ENUMERATE_PLEDGE_PROMISE(mount)     \
    __ENUMERATE_PLEDGE_PROMISE(unshare)   \
    __ENUMERATE_PLEDGE_PROMISE(no_error)

#define __ENUMERATE_PLEDGE_PROMISE(x) sizeof(#x) + 1 +
// NOTE: We truncate the last space from the string as it's not needed (with 0 - 1).
constexpr static unsigned all_promises_strings_length_with_spaces = ENUMERATE_PLEDGE_PROMISES 0 - 1;
#undef __ENUMERATE_PLEDGE_PROMISE

// NOTE: This is a sanity check because length of more than 1024 characters
// is not reasonable.
static_assert(all_promises_strings_length_with_spaces <= 1024);

enum class Pledge : u32 {
#define __ENUMERATE_PLEDGE_PROMISE(x) x,
    ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
};

enum class VeilState {
    None,
    Dropped,
    Locked,
    LockedInherited,
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
        // FIXME: This should be a NonnullRefPtr
        RefPtr<Credentials> credentials;
        RefPtr<ProcessGroup> process_group;
        RefPtr<TTY> tty;
        bool dumpable { false };
        bool executable_is_setid { false };
        bool has_promises { false };
        u32 promises { 0 };
        bool has_execpromises { false };
        u32 execpromises { 0 };
        mode_t umask { 022 };
        VirtualAddress signal_trampoline;
        Atomic<u32> thread_count { 0 };
        u8 termination_status { 0 };
        u8 termination_signal { 0 };
        SetOnce reject_transition_to_executable_from_writable_prot;
        SetOnce jailed_until_exit;
        bool jailed_until_exec { false };
    };

public:
    AK_MAKE_NONCOPYABLE(Process);
    AK_MAKE_NONMOVABLE(Process);

    MAKE_ALIGNED_ALLOCATED(Process, PAGE_SIZE);

    friend class Thread;
    friend class Coredump;
    friend class ScopedProcessList;

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
    static Process& current()
    {
        auto* current_thread = Processor::current_thread();
        VERIFY(current_thread);
        return current_thread->process();
    }

    static bool has_current()
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

    struct ProcessAndFirstThread {
        NonnullRefPtr<Process> process;
        NonnullRefPtr<Thread> first_thread;
    };

    template<typename EntryFunction>
    static ErrorOr<ProcessAndFirstThread> create_kernel_process(StringView name, EntryFunction entry, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_process(name, &Process::kernel_process_trampoline<EntryFunction>, entry_func, affinity, do_register);
    }

    static ErrorOr<ProcessAndFirstThread> create_kernel_process(StringView name, void (*entry)(void*), void* entry_data = nullptr, u32 affinity = THREAD_AFFINITY_DEFAULT, RegisterProcess do_register = RegisterProcess::Yes);
    static ErrorOr<ProcessAndFirstThread> create_user_process(StringView path, UserID, GroupID, Vector<NonnullOwnPtr<KString>> arguments, Vector<NonnullOwnPtr<KString>> environment, NonnullRefPtr<VFSRootContext>, NonnullRefPtr<HostnameContext>, RefPtr<TTY>);
    static void register_new(Process&);

    ~Process();

    virtual void remove_from_secondary_lists();

    template<typename EntryFunction>
    ErrorOr<NonnullRefPtr<Thread>> create_kernel_thread(StringView name, EntryFunction entry, u32 priority = THREAD_PRIORITY_NORMAL, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true)
    {
        auto* entry_func = new EntryFunction(move(entry));
        return create_kernel_thread(&Process::kernel_process_trampoline<EntryFunction>, entry_func, priority, name, affinity, joinable);
    }
    ErrorOr<NonnullRefPtr<Thread>> create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, StringView name, u32 affinity = THREAD_AFFINITY_DEFAULT, bool joinable = true);

    bool is_profiling() const { return m_profiling; }
    void set_profiling(bool profiling) { m_profiling = profiling; }

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    NO_SANITIZE_COVERAGE KCOVInstance* kcov_instance()
    {
        return m_kcov_instance;
    }
    void set_kcov_instance(KCOVInstance* kcov_instance) { m_kcov_instance = kcov_instance; }
    static bool is_kcov_busy();
#endif

    bool should_generate_coredump() const
    {
        return m_should_generate_coredump;
    }
    void set_should_generate_coredump(bool b) { m_should_generate_coredump = b; }

    bool is_dying() const { return m_state.load(AK::MemoryOrder::memory_order_acquire) != State::Running; }
    bool is_dead() const { return m_state.load(AK::MemoryOrder::memory_order_acquire) == State::Dead; }

    bool is_stopped() const { return m_is_stopped; }
    bool set_stopped(bool stopped) { return m_is_stopped.exchange(stopped); }

    bool is_kernel_process() const { return m_is_kernel_process; }
    bool is_user_process() const { return !m_is_kernel_process; }

    static RefPtr<Process> from_pid_in_same_process_list(ProcessID);
    static RefPtr<Process> from_pid_ignoring_process_lists(ProcessID);
    static SessionID get_sid_from_pgid(ProcessGroupID pgid);

    using Name = FixedStringBuffer<32>;
    SpinlockProtected<Name, LockRank::None> const& name() const;
    void set_name(StringView);

    ProcessID pid() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.pid; });
    }
    SessionID sid() const { return credentials()->sid(); }
    bool is_session_leader() const { return sid().value() == pid().value(); }
    ProcessGroupID pgid() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.process_group ? protected_data.process_group->pgid() : 0; });
    }
    bool is_group_leader() const { return pgid().value() == pid().value(); }
    ProcessID ppid() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.ppid; });
    }

    NonnullRefPtr<VFSRootContext> vfs_root_context() const
    {
        return m_attached_vfs_root_context.with([](auto& context) -> NonnullRefPtr<VFSRootContext> {
            return *context;
        });
    }

    NonnullRefPtr<HostnameContext> hostname_context() const
    {
        return m_attached_hostname_context.with([](auto& context) -> NonnullRefPtr<HostnameContext> {
            return *context;
        });
    }

    bool is_jailed() const
    {
        return with_protected_data([](auto& protected_data) {
            return protected_data.jailed_until_exit.was_set() || protected_data.jailed_until_exec;
        });
    }

    NonnullRefPtr<Credentials> credentials() const;

    bool is_dumpable() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.dumpable; });
    }

    mode_t umask() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.umask; });
    }

    // Breakable iteration functions
    template<IteratorFunction<Process&> Callback>
    static void for_each_ignoring_process_lists(Callback);

    static ErrorOr<void> for_each_in_same_process_list(Function<ErrorOr<void>(Process&)>);
    ErrorOr<void> for_each_in_pgrp_in_same_process_list(ProcessGroupID, Function<ErrorOr<void>(Process&)>);
    ErrorOr<void> for_each_child_in_same_process_list(Function<ErrorOr<void>(Process&)>);

    template<IteratorFunction<Thread&> Callback>
    IterationDecision for_each_thread(Callback);
    template<IteratorFunction<Thread&> Callback>
    IterationDecision for_each_thread(Callback callback) const;
    ErrorOr<void> try_for_each_thread(Function<ErrorOr<void>(Thread const&)>) const;

    // Non-breakable iteration functions
    template<VoidFunction<Process&> Callback>
    static void for_each_ignoring_process_lists(Callback);

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

    ErrorOr<FlatPtr> sys$yield();
    ErrorOr<FlatPtr> sys$sync();
    ErrorOr<FlatPtr> sys$beep(int tone);
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
    ErrorOr<FlatPtr> sys$pread(int fd, Userspace<u8*>, size_t, off_t);
    ErrorOr<FlatPtr> sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count);
    ErrorOr<FlatPtr> sys$write(int fd, Userspace<u8 const*>, size_t);
    ErrorOr<FlatPtr> sys$pwritev(int fd, Userspace<const struct iovec*> iov, int iov_count, off_t);
    ErrorOr<FlatPtr> sys$fstat(int fd, Userspace<stat*>);
    ErrorOr<FlatPtr> sys$stat(Userspace<Syscall::SC_stat_params const*>);
    ErrorOr<FlatPtr> sys$annotate_mapping(Userspace<void*>, int flags);
    ErrorOr<FlatPtr> sys$lseek(int fd, Userspace<off_t*>, int whence);
    ErrorOr<FlatPtr> sys$ftruncate(int fd, off_t);
    ErrorOr<FlatPtr> sys$futimens(Userspace<Syscall::SC_futimens_params const*>);
    ErrorOr<FlatPtr> sys$posix_fallocate(int fd, off_t, off_t);
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
    ErrorOr<FlatPtr> sys$setregid(GroupID, GroupID);
    ErrorOr<FlatPtr> sys$setresgid(GroupID, GroupID, GroupID);
    ErrorOr<FlatPtr> sys$alarm(unsigned seconds);
    ErrorOr<FlatPtr> sys$faccessat(Userspace<Syscall::SC_faccessat_params const*>);
    ErrorOr<FlatPtr> sys$fcntl(int fd, int cmd, uintptr_t extra_arg);
    ErrorOr<FlatPtr> sys$ioctl(int fd, unsigned request, FlatPtr arg);
    ErrorOr<FlatPtr> sys$mkdir(int dirfd, Userspace<char const*> pathname, size_t path_length, mode_t mode);
    ErrorOr<FlatPtr> sys$times(Userspace<tms*>);
    ErrorOr<FlatPtr> sys$utime(Userspace<char const*> pathname, size_t path_length, Userspace<const struct utimbuf*>);
    ErrorOr<FlatPtr> sys$utimensat(Userspace<Syscall::SC_utimensat_params const*>);
    ErrorOr<FlatPtr> sys$link(Userspace<Syscall::SC_link_params const*>);
    ErrorOr<FlatPtr> sys$unlink(int dirfd, Userspace<char const*> pathname, size_t path_length, int flags);
    ErrorOr<FlatPtr> sys$symlink(Userspace<Syscall::SC_symlink_params const*>);
    ErrorOr<FlatPtr> sys$rmdir(Userspace<char const*> pathname, size_t path_length);
    ErrorOr<FlatPtr> sys$fsmount(Userspace<Syscall::SC_fsmount_params const*>);
    ErrorOr<FlatPtr> sys$fsopen(Userspace<Syscall::SC_fsopen_params const*>);
    ErrorOr<FlatPtr> sys$umount(Userspace<Syscall::SC_umount_params const*>);
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
    ErrorOr<FlatPtr> sys$scheduler_set_parameters(Userspace<Syscall::SC_scheduler_parameters_params const*>);
    ErrorOr<FlatPtr> sys$scheduler_get_parameters(Userspace<Syscall::SC_scheduler_parameters_params*>);
    ErrorOr<FlatPtr> sys$create_thread(void* (*)(void*), Userspace<Syscall::SC_create_thread_params const*>);
    [[noreturn]] void sys$exit_thread(Userspace<void*>, Userspace<void*>, size_t);
    ErrorOr<FlatPtr> sys$join_thread(pid_t tid, Userspace<void**> exit_value);
    ErrorOr<FlatPtr> sys$detach_thread(pid_t tid);
    ErrorOr<FlatPtr> sys$kill_thread(pid_t tid, int signal);
    ErrorOr<FlatPtr> sys$rename(Userspace<Syscall::SC_rename_params const*>);
    ErrorOr<FlatPtr> sys$mknod(Userspace<Syscall::SC_mknod_params const*>);
    ErrorOr<FlatPtr> sys$copy_mount(Userspace<Syscall::SC_copy_mount_params const*> user_params);
    ErrorOr<FlatPtr> sys$realpath(Userspace<Syscall::SC_realpath_params const*>);
    ErrorOr<FlatPtr> sys$getrandom(Userspace<void*>, size_t, unsigned int);
    ErrorOr<FlatPtr> sys$getkeymap(Userspace<Syscall::SC_getkeymap_params const*>);
    ErrorOr<FlatPtr> sys$setkeymap(Userspace<Syscall::SC_setkeymap_params const*>);
    ErrorOr<FlatPtr> sys$profiling_enable(pid_t, u64);
    ErrorOr<FlatPtr> profiling_enable(pid_t, u64 event_mask);
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
    ErrorOr<FlatPtr> sys$prctl(int option, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3);
    ErrorOr<FlatPtr> sys$anon_create(size_t, int options);
    ErrorOr<FlatPtr> sys$statvfs(Userspace<Syscall::SC_statvfs_params const*> user_params);
    ErrorOr<FlatPtr> sys$fstatvfs(int fd, statvfs* buf);
    ErrorOr<FlatPtr> sys$map_time_page();
    ErrorOr<FlatPtr> sys$get_root_session_id(pid_t force_sid);
    ErrorOr<FlatPtr> sys$remount(Userspace<Syscall::SC_remount_params const*> user_params);
    ErrorOr<FlatPtr> sys$bindmount(Userspace<Syscall::SC_bindmount_params const*> user_params);
    ErrorOr<FlatPtr> sys$archctl(int option, FlatPtr arg1);
    ErrorOr<FlatPtr> sys$unshare_create(Userspace<Syscall::SC_unshare_create_params const*>);
    ErrorOr<FlatPtr> sys$unshare_attach(Userspace<Syscall::SC_unshare_attach_params const*>);

    enum SockOrPeerName {
        SockName,
        PeerName,
    };
    template<SockOrPeerName, typename Params>
    ErrorOr<void> get_sock_or_peer_name(Params const&);

    static void initialize();

    [[noreturn]] void crash(int signal, Optional<RegisterState const&> regs, bool out_of_memory = false);
    [[nodiscard]] siginfo_t wait_info() const;

    RefPtr<TTY> tty();
    RefPtr<TTY const> tty() const;
    void set_tty(RefPtr<TTY>);

    clock_t m_ticks_in_user { 0 };
    clock_t m_ticks_in_kernel { 0 };
    clock_t m_ticks_in_user_for_dead_children { 0 };
    clock_t m_ticks_in_kernel_for_dead_children { 0 };

    NonnullRefPtr<Custody> current_directory();
    RefPtr<Custody> executable();
    RefPtr<Custody const> executable() const;

    UnixDateTime creation_time() const { return m_creation_time; }

    static constexpr size_t max_arguments_size = Thread::default_userspace_stack_size / 8;
    static constexpr size_t max_environment_size = Thread::default_userspace_stack_size / 8;
    static constexpr size_t max_auxiliary_size = Thread::default_userspace_stack_size / 8;
    Vector<NonnullOwnPtr<KString>> const& arguments() const { return m_arguments; }
    Vector<NonnullOwnPtr<KString>> const& environment() const { return m_environment; }

    ErrorOr<void> exec(NonnullOwnPtr<KString> path, Vector<NonnullOwnPtr<KString>> arguments, Vector<NonnullOwnPtr<KString>> environment, Thread*& new_main_thread, InterruptsState& previous_interrupts_state, int recursion_depth = 0);

    ErrorOr<LoadResult> load(Memory::AddressSpace& new_space, NonnullRefPtr<OpenFileDescription> main_program_description, RefPtr<OpenFileDescription> interpreter_description, Elf_Ehdr const& main_program_header, Optional<size_t> minimum_stack_size = {});

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
        return with_protected_data([](auto& protected_data) { return protected_data.has_promises; });
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

    auto& exec_unveil_data() { return m_exec_unveil_data; }
    auto const& exec_unveil_data() const { return m_exec_unveil_data; }

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

    Vector<NonnullRefPtr<Thread>> const& threads_for_coredump(Badge<Coredump>) const { return m_threads_for_coredump; }

    PerformanceEventBuffer* perf_events() { return m_perf_event_buffer; }
    PerformanceEventBuffer const* perf_events() const { return m_perf_event_buffer; }

    SpinlockProtected<OwnPtr<Memory::AddressSpace>, LockRank::None>& address_space() { return m_space; }
    SpinlockProtected<OwnPtr<Memory::AddressSpace>, LockRank::None> const& address_space() const { return m_space; }

    VirtualAddress signal_trampoline() const
    {
        return with_protected_data([](auto& protected_data) { return protected_data.signal_trampoline; });
    }

    ErrorOr<void> require_promise(Pledge);
    ErrorOr<void> require_no_promises() const;

    bool should_reject_transition_to_executable_from_writable_prot() const
    {
        return with_protected_data([](auto& protected_data) {
            return protected_data.reject_transition_to_executable_from_writable_prot.was_set();
        });
    }

    ErrorOr<void> validate_mmap_prot(int prot, bool map_stack, bool map_anonymous, Memory::Region const* region = nullptr) const;
    ErrorOr<void> validate_inode_mmap_prot(int prot, bool description_readable, bool description_writable, bool map_shared) const;

    template<size_t Size>
    static ErrorOr<FixedStringBuffer<Size>> get_syscall_string_fixed_buffer(Syscall::StringArgument const& argument)
    {
        // NOTE: If the string is too much big for the FixedStringBuffer,
        // we return E2BIG error here.
        FixedStringBuffer<Size> buffer;
        TRY(try_copy_string_from_user_into_fixed_string_buffer<Size>(reinterpret_cast<FlatPtr>(argument.characters), buffer, argument.length));
        return buffer;
    }

    template<size_t Size>
    static ErrorOr<FixedStringBuffer<Size>> get_syscall_name_string_fixed_buffer(Userspace<char const*> user_buffer, size_t user_length = Size)
    {
        // NOTE: If the string is too much big for the FixedStringBuffer,
        // we return E2BIG error here.
        FixedStringBuffer<Size> buffer;
        TRY(try_copy_string_from_user_into_fixed_string_buffer<Size>(user_buffer, buffer, user_length));
        return buffer;
    }

    template<size_t Size>
    static ErrorOr<FixedStringBuffer<Size>> get_syscall_name_string_fixed_buffer(Syscall::StringArgument const& argument)
    {
        // NOTE: If the string is too much big for the FixedStringBuffer,
        // we return ENAMETOOLONG error here.
        FixedStringBuffer<Size> buffer;
        TRY(try_copy_name_from_user_into_fixed_string_buffer<Size>(reinterpret_cast<FlatPtr>(argument.characters), buffer, argument.length));
        return buffer;
    }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;
    friend class PerformanceManager;

    bool add_thread(Thread&);
    bool remove_thread(Thread&);

    Process(StringView name, NonnullRefPtr<Credentials>, ProcessID ppid, bool is_kernel_process, NonnullRefPtr<VFSRootContext>, NonnullRefPtr<HostnameContext>, RefPtr<Custody> current_directory, RefPtr<Custody> executable, RefPtr<TTY> tty, UnveilNode unveil_tree, UnveilNode exec_unveil_tree, UnixDateTime creation_time);
    static ErrorOr<ProcessAndFirstThread> create_with_forked_name(UserID, GroupID, ProcessID ppid, bool is_kernel_process, NonnullRefPtr<VFSRootContext> vfs_root_context, NonnullRefPtr<HostnameContext>, RefPtr<Custody> current_directory = nullptr, RefPtr<Custody> executable = nullptr, RefPtr<TTY> = nullptr, Process* fork_parent = nullptr);
    static ErrorOr<ProcessAndFirstThread> create(StringView name, UserID, GroupID, ProcessID ppid, bool is_kernel_process, NonnullRefPtr<VFSRootContext> vfs_root_context, NonnullRefPtr<HostnameContext>, RefPtr<Custody> current_directory = nullptr, RefPtr<Custody> executable = nullptr, RefPtr<TTY> = nullptr, Process* fork_parent = nullptr);
    ErrorOr<NonnullRefPtr<Thread>> attach_resources(NonnullOwnPtr<Memory::AddressSpace>&&, Process* fork_parent);
    static ProcessID allocate_pid();

    void kill_threads_except_self();
    void kill_all_threads();
    ErrorOr<void> dump_core();
    ErrorOr<void> dump_perfcore();
    bool create_perf_events_buffer_if_needed();
    void delete_perf_events_buffer();

    ErrorOr<void> do_exec(NonnullRefPtr<OpenFileDescription> main_program_description, Vector<NonnullOwnPtr<KString>> arguments, Vector<NonnullOwnPtr<KString>> environment, RefPtr<OpenFileDescription> interpreter_description, Thread*& new_main_thread, InterruptsState& previous_interrupts_state, Elf_Ehdr const& main_program_header, Optional<size_t> minimum_stack_size = {});
    ErrorOr<FlatPtr> do_write(OpenFileDescription&, UserOrKernelBuffer const&, size_t, Optional<off_t> = {});

    ErrorOr<FlatPtr> do_statvfs(FileSystem const& path, Custody const*, statvfs* buf);

    ErrorOr<RefPtr<OpenFileDescription>> find_elf_interpreter_for_executable(OpenFileDescription&, StringView path, Elf_Ehdr const& main_executable_header, size_t file_size, Optional<size_t>& minimum_stack_size);

    ErrorOr<void> do_kill(Process&, int signal);
    ErrorOr<void> do_killpg(ProcessGroupID pgrp, int signal);
    ErrorOr<void> do_killall(int signal);
    ErrorOr<void> do_killself(int signal);

    ErrorOr<siginfo_t> do_waitid(Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee, int options);

    static ErrorOr<NonnullOwnPtr<KString>> get_syscall_path_argument(Userspace<char const*> user_path, size_t path_length);
    static ErrorOr<NonnullOwnPtr<KString>> get_syscall_path_argument(Syscall::StringArgument const&);

    bool has_tracee_thread(ProcessID tracer_pid);

    void clear_signal_handlers_for_exec();
    void clear_futex_queues_on_exec();

    ErrorOr<GlobalFutexKey> get_futex_key(FlatPtr user_address, bool shared);

    ErrorOr<Memory::VirtualRange> remap_range_as_stack(FlatPtr address, size_t size);

    ErrorOr<FlatPtr> open_impl(Userspace<Syscall::SC_open_params const*>);
    ErrorOr<FlatPtr> close_impl(int fd);
    ErrorOr<FlatPtr> read_impl(int fd, Userspace<u8*> buffer, size_t size);
    ErrorOr<FlatPtr> pread_impl(int fd, Userspace<u8*>, size_t, off_t);
    ErrorOr<FlatPtr> readv_impl(int fd, Userspace<const struct iovec*> iov, int iov_count);

public:
    ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<NonnullRefPtr<Inode>> lookup_as_directory(ProcFS&, StringView name) const;
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
    ErrorOr<NonnullRefPtr<Inode>> lookup_stacks_directory(ProcFS&, StringView name) const;
    ErrorOr<size_t> procfs_get_file_description_link(unsigned fd, KBufferBuilder& builder) const;
    ErrorOr<void> traverse_file_descriptions_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<NonnullRefPtr<Inode>> lookup_file_descriptions_directory(ProcFS&, StringView name) const;
    ErrorOr<NonnullRefPtr<Inode>> lookup_children_directory(ProcFS&, StringView name) const;
    ErrorOr<void> traverse_children_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const;
    ErrorOr<size_t> procfs_get_child_process_link(ProcessID child_pid, KBufferBuilder& builder) const;

private:
    inline PerformanceEventBuffer* current_perf_events_buffer()
    {
        if (g_profiling_all_threads)
            return g_global_perf_events;
        if (m_profiling)
            return m_perf_event_buffer.ptr();
        return nullptr;
    }

    SpinlockProtected<Name, LockRank::None> m_name;

    SpinlockProtected<OwnPtr<Memory::AddressSpace>, LockRank::None> m_space;

    RecursiveSpinlock<LockRank::None> mutable m_protected_data_lock;
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
        void set(NonnullRefPtr<OpenFileDescription>, u32 flags = 0);

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

    MutexProtected<OpenFileDescriptions>& fds() { return m_fds; }
    MutexProtected<OpenFileDescriptions> const& fds() const { return m_fds; }

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open_file_description(int fd)
    {
        return m_fds.with_shared([fd](auto& fds) { return fds.open_file_description(fd); });
    }

    ErrorOr<RefPtr<OpenFileDescription>> open_file_description_ignoring_negative(int fd)
    {
        if (fd < 0)
            return nullptr;
        return open_file_description(fd);
    }

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open_file_description(int fd) const
    {
        return m_fds.with_shared([fd](auto& fds) { return fds.open_file_description(fd); });
    }

    ErrorOr<RefPtr<OpenFileDescription>> open_file_description_ignoring_negative(int fd) const
    {
        if (fd < 0)
            return nullptr;
        return open_file_description(fd);
    }

    ErrorOr<ScopedDescriptionAllocation> allocate_fd()
    {
        return m_fds.with_exclusive([](auto& fds) { return fds.allocate(); });
    }

    ErrorOr<NonnullRefPtr<Custody>> custody_for_dirfd(Badge<CustodyBase>, int dirfd);

private:
    ErrorOr<NonnullRefPtr<ScopedProcessList>> scoped_process_list_for_id(int id);

    ErrorOr<NonnullRefPtr<Custody>> custody_for_dirfd(int dirfd);

    ErrorOr<NonnullRefPtr<VFSRootContext>> vfs_root_context_for_id(int id);
    ErrorOr<NonnullRefPtr<VFSRootContext>> acquire_vfs_root_context_for_id_and_validate_path(bool& different_vfs_root_context, int id, StringView path);

    struct MountTargetContext {
        NonnullRefPtr<Custody> custody;
        NonnullRefPtr<VFSRootContext> vfs_root_context;
    };
    ErrorOr<MountTargetContext> context_for_mount_operation(int vfs_root_context_id, StringView path);

    SpinlockProtected<Thread::ListInProcess, LockRank::None>& thread_list() { return m_thread_list; }
    SpinlockProtected<Thread::ListInProcess, LockRank::None> const& thread_list() const { return m_thread_list; }

    ErrorOr<NonnullRefPtr<Thread>> get_thread_from_pid_or_tid(pid_t pid_or_tid, Syscall::SchedulerParametersMode mode);
    ErrorOr<NonnullRefPtr<Thread>> get_thread_from_thread_list(pid_t tid);

    SpinlockProtected<Thread::ListInProcess, LockRank::None> m_thread_list {};

    MutexProtected<OpenFileDescriptions> m_fds;

    bool const m_is_kernel_process;
    Atomic<State> m_state { State::Running };
    bool m_profiling { false };
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> m_is_stopped { false };
    bool m_should_generate_coredump { false };

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    KCOVInstance* m_kcov_instance { nullptr };
#endif

    SpinlockProtected<RefPtr<Custody>, LockRank::None> m_executable;

    SpinlockProtected<RefPtr<Custody>, LockRank::None> m_current_directory;

    UnixDateTime const m_creation_time;

    Vector<NonnullOwnPtr<KString>> m_arguments;
    Vector<NonnullOwnPtr<KString>> m_environment;

    IntrusiveListNode<Process> m_scoped_process_list_node;
    IntrusiveListNode<Process> m_all_processes_list_node;

public:
    using AllProcessesList = IntrusiveListRelaxedConst<&Process::m_all_processes_list_node>;

private:
    SpinlockProtected<RefPtr<ScopedProcessList>, LockRank::None> m_scoped_process_list;

    SpinlockProtected<RefPtr<VFSRootContext>, LockRank::Process> m_attached_vfs_root_context;

    SpinlockProtected<RefPtr<HostnameContext>, LockRank::Process> m_attached_hostname_context;

    Mutex m_big_lock { "Process"sv, Mutex::MutexBehavior::BigLock };
    Mutex m_ptrace_lock { "ptrace"sv };

    SpinlockProtected<RefPtr<Timer>, LockRank::None> m_alarm_timer;

    SpinlockProtected<UnveilData, LockRank::None> m_unveil_data;
    SpinlockProtected<UnveilData, LockRank::None> m_exec_unveil_data;

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

    SpinlockProtected<Array<CoredumpProperty, 4>, LockRank::None> m_coredump_properties {};
    Vector<NonnullRefPtr<Thread>> m_threads_for_coredump;

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
    static SpinlockProtected<Process::AllProcessesList, LockRank::None>& all_instances();
};

// Note: Process object should be 2 pages of 4096 bytes each.
// It's not expected that the Process object will expand further because the first
// page is used for all unprotected values (which should be plenty of space for them).
// The second page is being used exclusively for write-protected values.
static_assert(AssertSize<Process, (PAGE_SIZE * 2)>());

extern RecursiveSpinlock<LockRank::None> g_profiling_lock;

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
inline void Process::for_each_ignoring_process_lists(Callback callback)
{
    Process::all_instances().with([&](auto const& list) {
        for (auto it = list.begin(); it != list.end();) {
            auto& process = *it;
            ++it;
            if (callback(process) == IterationDecision::Break)
                break;
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

inline ProcessID Thread::pid() const
{
    return m_process->pid();
}

}

#define VERIFY_PROCESS_BIG_LOCK_ACQUIRED(process) \
    VERIFY(process->big_lock().is_exclusively_locked_by_current_thread())

#define VERIFY_NO_PROCESS_BIG_LOCK(process) \
    VERIFY(!process->big_lock().is_exclusively_locked_by_current_thread())

inline ErrorOr<NonnullOwnPtr<KString>> try_copy_kstring_from_user(Kernel::Syscall::StringArgument const& string)
{
    Userspace<char const*> characters((FlatPtr)string.characters);
    return try_copy_kstring_from_user(characters, string.length);
}

template<>
struct AK::Formatter<Kernel::Process> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::Process const& value)
    {
        return value.name().with([&](auto& process_name) {
            return AK::Formatter<FormatString>::format(builder, "{}({})"sv, process_name.representable_view(), value.pid().value());
        });
    }
};

namespace AK {
template<>
struct Traits<Kernel::GlobalFutexKey> : public DefaultTraits<Kernel::GlobalFutexKey> {
    static unsigned hash(Kernel::GlobalFutexKey const& futex_key) { return pair_int_hash(ptr_hash(futex_key.raw.parent), ptr_hash(futex_key.raw.offset)); }
    static bool equals(Kernel::GlobalFutexKey const& a, Kernel::GlobalFutexKey const& b) { return a.raw.parent == b.raw.parent && a.raw.offset == b.raw.offset; }
};
};
