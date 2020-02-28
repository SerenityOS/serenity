/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/FixedArray.h>
#include <AK/HashMap.h>
#include <AK/InlineLinkedList.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Forward.h>
#include <Kernel/Lock.h>
#include <Kernel/Syscall.h>
#include <Kernel/Thread.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/RangeAllocator.h>
#include <LibC/signal_numbers.h>

class ELFLoader;

namespace Kernel {

timeval kgettimeofday();
void kgettimeofday(timeval&);

extern VirtualAddress g_return_to_ring3_from_signal_trampoline;

#define ENUMERATE_PLEDGE_PROMISES      \
    __ENUMERATE_PLEDGE_PROMISE(stdio)  \
    __ENUMERATE_PLEDGE_PROMISE(rpath)  \
    __ENUMERATE_PLEDGE_PROMISE(wpath)  \
    __ENUMERATE_PLEDGE_PROMISE(cpath)  \
    __ENUMERATE_PLEDGE_PROMISE(dpath)  \
    __ENUMERATE_PLEDGE_PROMISE(inet)   \
    __ENUMERATE_PLEDGE_PROMISE(id)     \
    __ENUMERATE_PLEDGE_PROMISE(proc)   \
    __ENUMERATE_PLEDGE_PROMISE(exec)   \
    __ENUMERATE_PLEDGE_PROMISE(unix)   \
    __ENUMERATE_PLEDGE_PROMISE(fattr)  \
    __ENUMERATE_PLEDGE_PROMISE(tty)    \
    __ENUMERATE_PLEDGE_PROMISE(chown)  \
    __ENUMERATE_PLEDGE_PROMISE(chroot) \
    __ENUMERATE_PLEDGE_PROMISE(thread) \
    __ENUMERATE_PLEDGE_PROMISE(video)  \
    __ENUMERATE_PLEDGE_PROMISE(accept) \
    __ENUMERATE_PLEDGE_PROMISE(shared_buffer)

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

struct UnveiledPath {
    enum Access {
        Read = 1,
        Write = 2,
        Execute = 4,
        CreateOrRemove = 8,
    };

    String path;
    unsigned permissions { 0 };
};

class Process : public InlineLinkedListNode<Process> {
    friend class InlineLinkedListNode<Process>;
    friend class Thread;

public:
    static Process* current;

    static Process* create_kernel_process(Thread*& first_thread, String&& name, void (*entry)());
    static Process* create_user_process(Thread*& first_thread, const String& path, uid_t, gid_t, pid_t ppid, int& error, Vector<String>&& arguments = Vector<String>(), Vector<String>&& environment = Vector<String>(), TTY* = nullptr);
    ~Process();

    static Vector<pid_t> all_pids();
    static Vector<Process*> all_processes();

    bool is_profiling() const { return m_profiling; }
    void set_profiling(bool profiling) { m_profiling = profiling; }

    enum RingLevel : u8 {
        Ring0 = 0,
        Ring3 = 3,
    };

    KBuffer backtrace(ProcessInspectionHandle&) const;

    bool is_dead() const { return m_dead; }

    bool is_ring0() const { return m_ring == Ring0; }
    bool is_ring3() const { return m_ring == Ring3; }

    PageDirectory& page_directory() { return *m_page_directory; }
    const PageDirectory& page_directory() const { return *m_page_directory; }

    static Process* from_pid(pid_t);

    static void update_info_page_timestamp(const timeval&);

    const String& name() const { return m_name; }
    pid_t pid() const { return m_pid; }
    pid_t sid() const { return m_sid; }
    pid_t pgid() const { return m_pgid; }
    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    const FixedArray<gid_t>& extra_gids() const { return m_extra_gids; }
    uid_t euid() const { return m_euid; }
    gid_t egid() const { return m_egid; }
    pid_t ppid() const { return m_ppid; }

    pid_t exec_tid() const { return m_exec_tid; }

    mode_t umask() const { return m_umask; }

    bool in_group(gid_t) const;

    RefPtr<FileDescription> file_description(int fd) const;
    int fd_flags(int fd) const;

    template<typename Callback>
    static void for_each(Callback);
    template<typename Callback>
    static void for_each_in_pgrp(pid_t, Callback);
    template<typename Callback>
    void for_each_child(Callback);
    template<typename Callback>
    void for_each_thread(Callback) const;

    void die();
    void finalize();

    int sys$yield();
    int sys$sync();
    int sys$beep();
    int sys$get_process_name(char* buffer, int buffer_size);
    int sys$watch_file(const char* path, size_t path_length);
    int sys$dbgputch(u8);
    int sys$dbgputstr(const u8*, int length);
    int sys$dump_backtrace();
    int sys$gettid();
    int sys$donate(int tid);
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
    mode_t sys$umask(mode_t);
    int sys$open(const Syscall::SC_open_params*);
    int sys$close(int fd);
    ssize_t sys$read(int fd, u8*, ssize_t);
    ssize_t sys$write(int fd, const u8*, ssize_t);
    ssize_t sys$writev(int fd, const struct iovec* iov, int iov_count);
    int sys$fstat(int fd, stat*);
    int sys$stat(const Syscall::SC_stat_params*);
    int sys$lseek(int fd, off_t, int whence);
    int sys$kill(pid_t pid, int sig);
    [[noreturn]] void sys$exit(int status);
    int sys$sigreturn(RegisterState& registers);
    pid_t sys$waitid(const Syscall::SC_waitid_params*);
    void* sys$mmap(const Syscall::SC_mmap_params*);
    int sys$munmap(void*, size_t size);
    int sys$set_mmap_name(const Syscall::SC_set_mmap_name_params*);
    int sys$mprotect(void*, size_t, int prot);
    int sys$madvise(void*, size_t, int advice);
    int sys$purge(int mode);
    int sys$select(const Syscall::SC_select_params*);
    int sys$poll(pollfd*, int nfds, int timeout);
    ssize_t sys$get_dir_entries(int fd, void*, ssize_t);
    int sys$getcwd(char*, ssize_t);
    int sys$chdir(const char*, size_t);
    int sys$fchdir(int fd);
    int sys$sleep(unsigned seconds);
    int sys$usleep(useconds_t usec);
    int sys$gettimeofday(timeval*);
    int sys$clock_gettime(clockid_t, timespec*);
    int sys$clock_nanosleep(const Syscall::SC_clock_nanosleep_params*);
    int sys$gethostname(char*, ssize_t);
    int sys$uname(utsname*);
    int sys$readlink(const Syscall::SC_readlink_params*);
    int sys$ttyname_r(int fd, char*, ssize_t);
    int sys$ptsname_r(int fd, char*, ssize_t);
    pid_t sys$fork(RegisterState&);
    int sys$execve(const Syscall::SC_execve_params*);
    int sys$getdtablesize();
    int sys$dup(int oldfd);
    int sys$dup2(int oldfd, int newfd);
    int sys$sigaction(int signum, const sigaction* act, sigaction* old_act);
    int sys$sigprocmask(int how, const sigset_t* set, sigset_t* old_set);
    int sys$sigpending(sigset_t*);
    int sys$getgroups(ssize_t, gid_t*);
    int sys$setgroups(ssize_t, const gid_t*);
    int sys$pipe(int pipefd[2], int flags);
    int sys$killpg(int pgrp, int sig);
    int sys$setgid(gid_t);
    int sys$setuid(uid_t);
    unsigned sys$alarm(unsigned seconds);
    int sys$access(const char* pathname, size_t path_length, int mode);
    int sys$fcntl(int fd, int cmd, u32 extra_arg);
    int sys$ioctl(int fd, unsigned request, unsigned arg);
    int sys$mkdir(const char* pathname, size_t path_length, mode_t mode);
    clock_t sys$times(tms*);
    int sys$utime(const char* pathname, size_t path_length, const struct utimbuf*);
    int sys$link(const Syscall::SC_link_params*);
    int sys$unlink(const char* pathname, size_t path_length);
    int sys$symlink(const Syscall::SC_symlink_params*);
    int sys$rmdir(const char* pathname, size_t path_length);
    int sys$mount(const Syscall::SC_mount_params*);
    int sys$umount(const char* mountpoint, size_t mountpoint_length);
    int sys$chmod(const char* pathname, size_t path_length, mode_t);
    int sys$fchmod(int fd, mode_t);
    int sys$chown(const Syscall::SC_chown_params*);
    int sys$fchown(int fd, uid_t, gid_t);
    int sys$socket(int domain, int type, int protocol);
    int sys$bind(int sockfd, const sockaddr* addr, socklen_t);
    int sys$listen(int sockfd, int backlog);
    int sys$accept(int sockfd, sockaddr*, socklen_t*);
    int sys$connect(int sockfd, const sockaddr*, socklen_t);
    int sys$shutdown(int sockfd, int how);
    ssize_t sys$sendto(const Syscall::SC_sendto_params*);
    ssize_t sys$recvfrom(const Syscall::SC_recvfrom_params*);
    int sys$getsockopt(const Syscall::SC_getsockopt_params*);
    int sys$setsockopt(const Syscall::SC_setsockopt_params*);
    int sys$getsockname(const Syscall::SC_getsockname_params*);
    int sys$getpeername(const Syscall::SC_getpeername_params*);
    int sys$sched_setparam(pid_t pid, const struct sched_param* param);
    int sys$sched_getparam(pid_t pid, struct sched_param* param);
    int sys$create_thread(void* (*)(void*), void* argument, const Syscall::SC_create_thread_params*);
    void sys$exit_thread(void*);
    int sys$join_thread(int tid, void** exit_value);
    int sys$detach_thread(int tid);
    int sys$set_thread_name(int tid, const char* buffer, size_t buffer_size);
    int sys$get_thread_name(int tid, char* buffer, size_t buffer_size);
    int sys$rename(const Syscall::SC_rename_params*);
    int sys$systrace(pid_t);
    int sys$mknod(const Syscall::SC_mknod_params*);
    int sys$shbuf_create(int, void** buffer);
    int sys$shbuf_allow_pid(int, pid_t peer_pid);
    int sys$shbuf_allow_all(int);
    void* sys$shbuf_get(int shbuf_id, size_t* size);
    int sys$shbuf_release(int shbuf_id);
    int sys$shbuf_seal(int shbuf_id);
    int sys$shbuf_set_volatile(int shbuf_id, bool);
    int sys$halt();
    int sys$reboot();
    int sys$set_process_icon(int icon_id);
    int sys$realpath(const Syscall::SC_realpath_params*);
    ssize_t sys$getrandom(void*, size_t, unsigned int);
    int sys$setkeymap(const Syscall::SC_setkeymap_params*);
    int sys$module_load(const char* path, size_t path_length);
    int sys$module_unload(const char* name, size_t name_length);
    int sys$profiling_enable(pid_t);
    int sys$profiling_disable(pid_t);
    void* sys$get_kernel_info_page();
    int sys$futex(const Syscall::SC_futex_params*);
    int sys$set_thread_boost(int tid, int amount);
    int sys$set_process_boost(pid_t, int amount);
    int sys$chroot(const char* path, size_t path_length, int mount_flags);
    int sys$pledge(const Syscall::SC_pledge_params*);
    int sys$unveil(const Syscall::SC_unveil_params*);
    int sys$perf_event(int type, uintptr_t arg1, uintptr_t arg2);

    template<bool sockname, typename Params>
    int get_sock_or_peer_name(const Params&);

    static void initialize();

    [[noreturn]] void crash(int signal, u32 eip);
    [[nodiscard]] static siginfo_t reap(Process&);

    const TTY* tty() const { return m_tty; }
    void set_tty(TTY*);

    size_t region_count() const { return m_regions.size(); }
    const NonnullOwnPtrVector<Region>& regions() const { return m_regions; }
    void dump_regions();

    ProcessTracer* tracer() { return m_tracer.ptr(); }
    ProcessTracer& ensure_tracer();

    u32 m_ticks_in_user { 0 };
    u32 m_ticks_in_kernel { 0 };

    u32 m_ticks_in_user_for_dead_children { 0 };
    u32 m_ticks_in_kernel_for_dead_children { 0 };

    bool validate_read_from_kernel(VirtualAddress, size_t) const;

    bool validate_read(const void*, size_t) const;
    bool validate_write(void*, size_t) const;
    template<typename T>
    bool validate_read_typed(T* value, size_t count = 1) { return validate_read(value, sizeof(T) * count); }
    template<typename T>
    bool validate_read_and_copy_typed(T* dest, const T* src)
    {
        bool validated = validate_read_typed(src);
        if (validated) {
            copy_from_user(dest, src);
        }
        return validated;
    }
    template<typename T>
    bool validate_write_typed(T* value, size_t count = 1) { return validate_write(value, sizeof(T) * count); }
    template<typename DataType, typename SizeType>
    bool validate(const Syscall::MutableBufferArgument<DataType, SizeType>&);
    template<typename DataType, typename SizeType>
    bool validate(const Syscall::ImmutableBufferArgument<DataType, SizeType>&);

    String validate_and_copy_string_from_user(const char*, size_t) const;
    String validate_and_copy_string_from_user(const Syscall::StringArgument&) const;

    Custody& current_directory();
    Custody* executable() { return m_executable.ptr(); }

    int number_of_open_file_descriptors() const;
    int max_open_file_descriptors() const { return m_max_open_file_descriptors; }

    size_t amount_clean_inode() const;
    size_t amount_dirty_private() const;
    size_t amount_virtual() const;
    size_t amount_resident() const;
    size_t amount_shared() const;
    size_t amount_purgeable_volatile() const;
    size_t amount_purgeable_nonvolatile() const;

    int exec(String path, Vector<String> arguments, Vector<String> environment, int recusion_depth = 0);

    bool is_superuser() const { return m_euid == 0; }

    Region* allocate_region_with_vmobject(VirtualAddress, size_t, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const String& name, int prot, bool user_accessible = true);
    Region* allocate_file_backed_region(VirtualAddress, size_t, NonnullRefPtr<Inode>, const String& name, int prot);
    Region* allocate_region(VirtualAddress, size_t, const String& name, int prot = PROT_READ | PROT_WRITE, bool commit = true);
    Region* allocate_region_with_vmobject(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const String& name, int prot, bool user_accessible = true);
    Region* allocate_region(const Range&, const String& name, int prot = PROT_READ | PROT_WRITE, bool commit = true);
    bool deallocate_region(Region& region);

    Region& allocate_split_region(const Region& source_region, const Range&, size_t offset_in_vmobject);
    Vector<Region*, 2> split_region_around_range(const Region& source_region, const Range&);

    bool is_being_inspected() const { return m_inspector_count; }

    void terminate_due_to_signal(u8 signal);
    void send_signal(u8, Process* sender);

    u16 thread_count() const { return m_thread_count; }

    Thread& any_thread();

    Lock& big_lock() { return m_big_lock; }

    const ELFLoader* elf_loader() const { return m_elf_loader.ptr(); }

    int icon_id() const { return m_icon_id; }

    u32 priority_boost() const { return m_priority_boost; }

    Custody& root_directory();
    Custody& root_directory_relative_to_global_root();
    void set_root_directory(const Custody&);

    bool has_promises() const { return m_promises; }
    bool has_promised(Pledge pledge) const { return m_promises & (1u << (u32)pledge); }

    VeilState veil_state() const { return m_veil_state; }
    const Vector<UnveiledPath>& unveiled_paths() const { return m_unveiled_paths; }

    void increment_inspector_count(Badge<ProcessInspectionHandle>) { ++m_inspector_count; }
    void decrement_inspector_count(Badge<ProcessInspectionHandle>) { --m_inspector_count; }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;

    Process(Thread*& first_thread, const String& name, uid_t, gid_t, pid_t ppid, RingLevel, RefPtr<Custody> cwd = nullptr, RefPtr<Custody> executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);
    static pid_t allocate_pid();

    Range allocate_range(VirtualAddress, size_t, size_t alignment = PAGE_SIZE);

    Region& add_region(NonnullOwnPtr<Region>);

    void kill_threads_except_self();
    void kill_all_threads();

    int do_exec(NonnullRefPtr<FileDescription> main_program_description, Vector<String> arguments, Vector<String> environment, RefPtr<FileDescription> interpreter_description);
    ssize_t do_write(FileDescription&, const u8*, int data_size);

    KResultOr<NonnullRefPtr<FileDescription>> find_elf_interpreter_for_executable(const String& path, char (&first_page)[PAGE_SIZE], int nread, size_t file_size);

    int alloc_fd(int first_candidate_fd = 0);
    void disown_all_shared_buffers();

    KResult do_kill(Process&, int signal);
    KResult do_killpg(pid_t pgrp, int signal);

    KResultOr<siginfo_t> do_waitid(idtype_t idtype, int id, int options);

    KResultOr<String> get_syscall_path_argument(const char* user_path, size_t path_length) const;
    KResultOr<String> get_syscall_path_argument(const Syscall::StringArgument&) const;

    RefPtr<PageDirectory> m_page_directory;

    Process* m_prev { nullptr };
    Process* m_next { nullptr };

    String m_name;

    pid_t m_pid { 0 };
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    uid_t m_euid { 0 };
    gid_t m_egid { 0 };
    pid_t m_sid { 0 };
    pid_t m_pgid { 0 };

    pid_t m_exec_tid { 0 };

    static const int m_max_open_file_descriptors { FD_SETSIZE };

    struct FileDescriptionAndFlags {
        operator bool() const { return !!description; }
        void clear();
        void set(NonnullRefPtr<FileDescription>&& d, u32 f = 0);
        RefPtr<FileDescription> description;
        u32 flags { 0 };
    };
    Vector<FileDescriptionAndFlags> m_fds;

    RingLevel m_ring { Ring0 };
    u8 m_termination_status { 0 };
    u8 m_termination_signal { 0 };
    u16 m_thread_count { 0 };

    bool m_dead { false };
    bool m_profiling { false };

    RefPtr<Custody> m_executable;
    RefPtr<Custody> m_cwd;
    RefPtr<Custody> m_root_directory;
    RefPtr<Custody> m_root_directory_relative_to_global_root;

    RefPtr<TTY> m_tty;

    Region* region_from_range(const Range&);
    Region* region_containing(const Range&);

    NonnullOwnPtrVector<Region> m_regions;
    struct RegionLookupCache {
        Range range;
        WeakPtr<Region> region;
    };
    RegionLookupCache m_region_lookup_cache;

    pid_t m_ppid { 0 };
    mode_t m_umask { 022 };

    FixedArray<gid_t> m_extra_gids;

    RefPtr<ProcessTracer> m_tracer;
    OwnPtr<ELFLoader> m_elf_loader;

    WeakPtr<Region> m_master_tls_region;
    size_t m_master_tls_size { 0 };
    size_t m_master_tls_alignment { 0 };

    Lock m_big_lock { "Process" };

    u64 m_alarm_deadline { 0 };

    int m_icon_id { -1 };

    u32 m_priority_boost { 0 };

    u32 m_promises { 0 };
    u32 m_execpromises { 0 };

    VeilState m_veil_state { VeilState::None };
    Vector<UnveiledPath> m_unveiled_paths;

    WaitQueue& futex_queue(i32*);
    HashMap<u32, OwnPtr<WaitQueue>> m_futex_queues;

    OwnPtr<PerformanceEventBuffer> m_perf_event_buffer;

    u32 m_inspector_count { 0 };
};

class ProcessInspectionHandle {
public:
    ProcessInspectionHandle(Process& process)
        : m_process(process)
    {
        if (&process != Process::current) {
            InterruptDisabler disabler;
            m_process.increment_inspector_count({});
        }
    }
    ~ProcessInspectionHandle()
    {
        if (&m_process != Process::current) {
            InterruptDisabler disabler;
            m_process.decrement_inspector_count({});
        }
    }

    Process& process() { return m_process; }

    static OwnPtr<ProcessInspectionHandle> from_pid(pid_t pid)
    {
        InterruptDisabler disabler;
        auto* process = Process::from_pid(pid);
        if (process)
            return make<ProcessInspectionHandle>(*process);
        return nullptr;
    }

    Process* operator->() { return &m_process; }
    Process& operator*() { return m_process; }

private:
    Process& m_process;
};

extern InlineLinkedList<Process>* g_processes;

template<typename Callback>
inline void Process::for_each(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
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
    ASSERT_INTERRUPTS_DISABLED();
    pid_t my_pid = pid();
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (process->ppid() == my_pid) {
            if (callback(*process) == IterationDecision::Break)
                break;
        }
        process = next_process;
    }
}

template<typename Callback>
inline void Process::for_each_thread(Callback callback) const
{
    InterruptDisabler disabler;
    pid_t my_pid = pid();

    if (my_pid == 0) {
        // NOTE: Special case the colonel process, since its main thread is not in the global thread table.
        callback(*g_colonel);
        return;
    }

    Thread::for_each([callback, my_pid](Thread& thread) -> IterationDecision {
        if (thread.pid() == my_pid)
            return callback(thread);

        return IterationDecision::Continue;
    });
}

template<typename Callback>
inline void Process::for_each_in_pgrp(pid_t pgid, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
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

inline int Thread::pid() const
{
    return m_process.pid();
}

inline const LogStream& operator<<(const LogStream& stream, const Process& process)
{
    return stream << process.name() << '(' << process.pid() << ')';
}

inline u32 Thread::effective_priority() const
{
    return m_priority + m_process.priority_boost() + m_priority_boost + m_extra_priority;
}

#define REQUIRE_NO_PROMISES                      \
    do {                                         \
        if (Process::current->has_promises()) {  \
            dbg() << "Has made a promise";       \
            cli();                               \
            Process::current->crash(SIGABRT, 0); \
            ASSERT_NOT_REACHED();                \
        }                                        \
    } while (0)

#define REQUIRE_PROMISE(promise)                                   \
    do {                                                           \
        if (Process::current->has_promises()                       \
            && !Process::current->has_promised(Pledge::promise)) { \
            dbg() << "Has not pledged " << #promise;               \
            cli();                                                 \
            Process::current->crash(SIGABRT, 0);                   \
            ASSERT_NOT_REACHED();                                  \
        }                                                          \
    } while (0)

}
