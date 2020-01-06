#pragma once

#include <AK/InlineLinkedList.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Lock.h>
#include <Kernel/Syscall.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/Thread.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/RangeAllocator.h>
#include <LibC/signal_numbers.h>

class ELFLoader;
class FileDescription;
class KBuffer;
class PageDirectory;
class Region;
class VMObject;
class ProcessTracer;
class SharedBuffer;

timeval kgettimeofday();
void kgettimeofday(timeval&);

extern VirtualAddress g_return_to_ring3_from_signal_trampoline;
extern VirtualAddress g_return_to_ring0_from_signal_trampoline;

class Process : public InlineLinkedListNode<Process>
    , public Weakable<Process> {
    friend class InlineLinkedListNode<Process>;
    friend class Thread;

public:
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
    const HashTable<gid_t>& extra_gids() const { return m_extra_gids; }
    uid_t euid() const { return m_euid; }
    gid_t egid() const { return m_egid; }
    pid_t ppid() const { return m_ppid; }

    mode_t umask() const { return m_umask; }

    bool in_group(gid_t) const;

    FileDescription* file_description(int fd);
    const FileDescription* file_description(int fd) const;
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
    int sys$openat(const Syscall::SC_openat_params*);
    int sys$close(int fd);
    ssize_t sys$read(int fd, u8*, ssize_t);
    ssize_t sys$write(int fd, const u8*, ssize_t);
    ssize_t sys$writev(int fd, const struct iovec* iov, int iov_count);
    int sys$fstat(int fd, stat*);
    int sys$lstat(const char*, size_t, stat*);
    int sys$stat(const char*, size_t, stat*);
    int sys$lseek(int fd, off_t, int whence);
    int sys$kill(pid_t pid, int sig);
    [[noreturn]] void sys$exit(int status);
    int sys$sigreturn(RegisterDump& registers);
    pid_t sys$waitpid(pid_t, int* wstatus, int options);
    void* sys$mmap(const Syscall::SC_mmap_params*);
    int sys$munmap(void*, size_t size);
    int sys$set_mmap_name(void*, size_t, const char*);
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
    int sys$readlink(const char*, char*, ssize_t);
    int sys$ttyname_r(int fd, char*, ssize_t);
    int sys$ptsname_r(int fd, char*, ssize_t);
    pid_t sys$fork(RegisterDump&);
    int sys$execve(const char* filename, const char** argv, const char** envp);
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
    int sys$utime(const char* pathname, const struct utimbuf*);
    int sys$link(const char* old_path, const char* new_path);
    int sys$unlink(const char* pathname);
    int sys$symlink(const char* target, const char* linkpath);
    int sys$rmdir(const char* pathname, size_t path_length);
    int sys$mount(const char* device, const char* mountpoint, const char* fstype);
    int sys$umount(const char* mountpoint);
    int sys$chmod(const char* pathname, size_t path_length, mode_t);
    int sys$fchmod(int fd, mode_t);
    int sys$chown(const char* pathname, uid_t, gid_t);
    int sys$fchown(int fd, uid_t, gid_t);
    int sys$socket(int domain, int type, int protocol);
    int sys$bind(int sockfd, const sockaddr* addr, socklen_t);
    int sys$listen(int sockfd, int backlog);
    int sys$accept(int sockfd, sockaddr*, socklen_t*);
    int sys$connect(int sockfd, const sockaddr*, socklen_t);
    ssize_t sys$sendto(const Syscall::SC_sendto_params*);
    ssize_t sys$recvfrom(const Syscall::SC_recvfrom_params*);
    int sys$getsockopt(const Syscall::SC_getsockopt_params*);
    int sys$setsockopt(const Syscall::SC_setsockopt_params*);
    int sys$getsockname(int sockfd, sockaddr* addr, socklen_t* addrlen);
    int sys$getpeername(int sockfd, sockaddr* addr, socklen_t* addrlen);
    int sys$sched_setparam(pid_t pid, const struct sched_param* param);
    int sys$sched_getparam(pid_t pid, struct sched_param* param);
    int sys$restore_signal_mask(u32 mask);
    int sys$create_thread(void* (*)(void*), void* argument, const Syscall::SC_create_thread_params*);
    void sys$exit_thread(void*);
    int sys$join_thread(int tid, void** exit_value);
    int sys$detach_thread(int tid);
    int sys$set_thread_name(int tid, const char* buffer, int buffer_size);
    int sys$get_thread_name(int tid, char* buffer, int buffer_size);
    int sys$rename(const char* oldpath, const char* newpath);
    int sys$systrace(pid_t);
    int sys$mknod(const char* pathname, mode_t, dev_t);
    int sys$create_shared_buffer(int, void** buffer);
    int sys$share_buffer_with(int, pid_t peer_pid);
    int sys$share_buffer_globally(int);
    void* sys$get_shared_buffer(int shared_buffer_id);
    int sys$release_shared_buffer(int shared_buffer_id);
    int sys$seal_shared_buffer(int shared_buffer_id);
    int sys$get_shared_buffer_size(int shared_buffer_id);
    int sys$set_shared_buffer_volatile(int shared_buffer_id, bool);
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

    static void initialize();

    [[noreturn]] void crash(int signal, u32 eip);
    [[nodiscard]] static int reap(Process&);

    const TTY* tty() const { return m_tty; }
    void set_tty(TTY* tty) { m_tty = tty; }

    size_t region_count() const { return m_regions.size(); }
    const NonnullOwnPtrVector<Region>& regions() const { return m_regions; }
    void dump_regions();

    ProcessTracer* tracer() { return m_tracer.ptr(); }
    ProcessTracer& ensure_tracer();

    u32 m_ticks_in_user { 0 };
    u32 m_ticks_in_kernel { 0 };

    u32 m_ticks_in_user_for_dead_children { 0 };
    u32 m_ticks_in_kernel_for_dead_children { 0 };

    bool validate_read_from_kernel(VirtualAddress, ssize_t) const;

    bool validate_read(const void*, ssize_t) const;
    bool validate_write(void*, ssize_t) const;
    bool validate_read_str(const char* str);
    template<typename T>
    bool validate_read_typed(T* value, size_t count = 1) { return validate_read(value, sizeof(T) * count); }
    template<typename T>
    bool validate_write_typed(T* value, size_t count = 1) { return validate_write(value, sizeof(T) * count); }

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

    int exec(String path, Vector<String> arguments, Vector<String> environment);

    bool is_superuser() const { return m_euid == 0; }

    Region* allocate_region_with_vmobject(VirtualAddress, size_t, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const String& name, int prot);
    Region* allocate_file_backed_region(VirtualAddress, size_t, NonnullRefPtr<Inode>, const String& name, int prot);
    Region* allocate_region(VirtualAddress, size_t, const String& name, int prot = PROT_READ | PROT_WRITE, bool commit = true);
    bool deallocate_region(Region& region);

    Region& allocate_split_region(const Region& source_region, const Range&, size_t offset_in_vmobject);
    Vector<Region*, 2> split_region_around_range(const Region& source_region, const Range&);

    void set_being_inspected(bool b) { m_being_inspected = b; }
    bool is_being_inspected() const { return m_being_inspected; }

    void terminate_due_to_signal(u8 signal);
    void send_signal(u8, Process* sender);

    u16 thread_count() const { return m_thread_count; }

    Thread& any_thread();

    Lock& big_lock() { return m_big_lock; }

    const ELFLoader* elf_loader() const { return m_elf_loader.ptr(); }

    int icon_id() const { return m_icon_id; }

    u32 priority_boost() const { return m_priority_boost; }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;

    Process(Thread*& first_thread, const String& name, uid_t, gid_t, pid_t ppid, RingLevel, RefPtr<Custody> cwd = nullptr, RefPtr<Custody> executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);
    static pid_t allocate_pid();

    Range allocate_range(VirtualAddress, size_t);

    int do_exec(String path, Vector<String> arguments, Vector<String> environment);
    ssize_t do_write(FileDescription&, const u8*, int data_size);

    int alloc_fd(int first_candidate_fd = 0);
    void disown_all_shared_buffers();

    KResultOr<Vector<String>> find_shebang_interpreter_for_executable(const String& executable_path);

    KResult do_kill(Process&, int signal);
    KResult do_killpg(pid_t pgrp, int signal);

    KResultOr<String> get_syscall_path_argument(const char* user_path, size_t path_length);

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

    bool m_being_inspected { false };
    bool m_dead { false };
    bool m_profiling { false };

    RefPtr<Custody> m_executable;
    RefPtr<Custody> m_cwd;

    RefPtr<TTY> m_tty;

    Region* region_from_range(const Range&);
    Region* region_containing(const Range&);

    NonnullOwnPtrVector<Region> m_regions;

    pid_t m_ppid { 0 };
    mode_t m_umask { 022 };

    HashTable<gid_t> m_extra_gids;

    RefPtr<ProcessTracer> m_tracer;
    OwnPtr<ELFLoader> m_elf_loader;

    Region* m_master_tls_region { nullptr };
    size_t m_master_tls_size { 0 };
    size_t m_master_tls_alignment { 0 };

    Lock m_big_lock { "Process" };

    u64 m_alarm_deadline { 0 };

    int m_icon_id { -1 };

    u32 m_priority_boost { 0 };

    WaitQueue& futex_queue(i32*);
    HashMap<u32, OwnPtr<WaitQueue>> m_futex_queues;
};

class ProcessInspectionHandle {
public:
    ProcessInspectionHandle(Process& process)
        : m_process(process)
    {
        if (&process != &current->process()) {
            ASSERT(!m_process.is_being_inspected());
            m_process.set_being_inspected(true);
        }
    }
    ~ProcessInspectionHandle()
    {
        m_process.set_being_inspected(false);
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

inline bool InodeMetadata::may_read(Process& process) const
{
    return may_read(process.euid(), process.egid(), process.extra_gids());
}

inline bool InodeMetadata::may_write(Process& process) const
{
    return may_write(process.euid(), process.egid(), process.extra_gids());
}

inline bool InodeMetadata::may_execute(Process& process) const
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
