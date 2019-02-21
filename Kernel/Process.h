#pragma once

#include "types.h"
#include "TSS.h"
#include "i386.h"
#include "TTY.h"
#include "Syscall.h"
#include <Kernel/VirtualFileSystem.h>
#include <Kernel/UnixTypes.h>
#include <AK/InlineLinkedList.h>
#include <AK/AKString.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <AK/Lock.h>

class FileDescriptor;
class PageDirectory;
class Region;
class VMObject;
class Zone;
class WSWindow;
class GraphicsBitmap;

#define COOL_GLOBALS
#ifdef COOL_GLOBALS
struct CoolGlobals {
    pid_t current_pid;
};
extern CoolGlobals* g_cool_globals;
#endif

enum class ShouldUnblockProcess { No = 0, Yes };

struct SignalActionData {
    LinearAddress handler_or_sigaction;
    dword mask { 0 };
    int flags { 0 };
    LinearAddress restorer;
};

struct DisplayInfo {
    unsigned width;
    unsigned height;
    unsigned bpp;
    unsigned pitch;
};

class Process : public InlineLinkedListNode<Process>, public Weakable<Process> {
    friend class InlineLinkedListNode<Process>;
public:
    static Process* create_kernel_process(String&& name, void (*entry)());
    static Process* create_user_process(const String& path, uid_t, gid_t, pid_t ppid, int& error, Vector<String>&& arguments = Vector<String>(), Vector<String>&& environment = Vector<String>(), TTY* = nullptr);
    ~Process();

    static Vector<pid_t> all_pids();
    static Vector<Process*> all_processes();
    static void finalize_dying_processes();

    enum State {
        Invalid = 0,
        Runnable,
        Running,
        Skip1SchedulerPass,
        Skip0SchedulerPasses,
        Dying,
        Dead,
        BeingInspected,
        BlockedLurking,
        BlockedSleep,
        BlockedWait,
        BlockedRead,
        BlockedWrite,
        BlockedSignal,
        BlockedSelect,
        BlockedConnect,
    };

    enum Priority {
        LowPriority,
        NormalPriority,
        HighPriority,
    };

    enum RingLevel {
        Ring0 = 0,
        Ring3 = 3,
    };

    bool is_ring0() const { return m_ring == Ring0; }
    bool is_ring3() const { return m_ring == Ring3; }

    bool is_blocked() const
    {
        return m_state == BlockedSleep || m_state == BlockedWait || m_state == BlockedRead || m_state == BlockedWrite || m_state == BlockedSignal || m_state == BlockedSelect;
    }

    PageDirectory& page_directory() { return *m_page_directory; }
    const PageDirectory& page_directory() const { return *m_page_directory; }

    bool in_kernel() const { return (m_tss.cs & 0x03) == 0; }

    static Process* from_pid(pid_t);

    void set_priority(Priority p) { m_priority = p; }
    Priority priority() const { return m_priority; }

    const String& name() const { return m_name; }
    pid_t pid() const { return m_pid; }
    pid_t sid() const { return m_sid; }
    pid_t pgid() const { return m_pgid; }
    dword ticks() const { return m_ticks; }
    word selector() const { return m_far_ptr.selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    const HashTable<gid_t>& gids() const { return m_gids; }
    uid_t euid() const { return m_euid; }
    gid_t egid() const { return m_egid; }
    pid_t ppid() const { return m_ppid; }

    const FarPtr& far_ptr() const { return m_far_ptr; }

    FileDescriptor* file_descriptor(int fd);
    const FileDescriptor* file_descriptor(int fd) const;

    void block(Process::State);
    void unblock();

    void set_wakeup_time(dword t) { m_wakeup_time = t; }
    dword wakeup_time() const { return m_wakeup_time; }

    template<typename Callback> static void for_each(Callback);
    template<typename Callback> static void for_each_in_pgrp(pid_t, Callback);
    template<typename Callback> static void for_each_in_state(State, Callback);
    template<typename Callback> static void for_each_living(Callback);
    template<typename Callback> void for_each_child(Callback);

    bool tick();
    void set_ticks_left(dword t) { m_ticks_left = t; }
    dword ticks_left() const { return m_ticks_left; }

    void set_selector(word s) { m_far_ptr.selector = s; }
    void set_state(State s) { m_state = s; }
    void die();
    void finalize();

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
    int sys$open(const char* path, int options, mode_t mode = 0);
    int sys$close(int fd);
    ssize_t sys$read(int fd, void* outbuf, size_t nread);
    ssize_t sys$write(int fd, const void*, size_t);
    int sys$fstat(int fd, stat*);
    int sys$lstat(const char*, stat*);
    int sys$stat(const char*, stat*);
    int sys$lseek(int fd, off_t, int whence);
    int sys$kill(pid_t pid, int sig);
    int sys$geterror() { return m_error; }
    [[noreturn]] void sys$exit(int status);
    [[noreturn]] void sys$sigreturn();
    pid_t sys$waitpid(pid_t, int* wstatus, int options);
    void* sys$mmap(const Syscall::SC_mmap_params*);
    int sys$munmap(void*, size_t size);
    int sys$set_mmap_name(void*, size_t, const char*);
    int sys$select(const Syscall::SC_select_params*);
    int sys$poll(pollfd*, int nfds, int timeout);
    ssize_t sys$get_dir_entries(int fd, void*, size_t);
    int sys$getcwd(char*, size_t);
    int sys$chdir(const char*);
    int sys$sleep(unsigned seconds);
    int sys$usleep(useconds_t usec);
    int sys$gettimeofday(timeval*);
    int sys$gethostname(char* name, size_t length);
    int sys$get_arguments(int* argc, char*** argv);
    int sys$get_environment(char*** environ);
    int sys$uname(utsname*);
    int sys$readlink(const char*, char*, size_t);
    int sys$ttyname_r(int fd, char*, size_t);
    int sys$ptsname_r(int fd, char*, size_t);
    pid_t sys$fork(RegisterDump&);
    int sys$execve(const char* filename, const char** argv, const char** envp);
    int sys$isatty(int fd);
    int sys$getdtablesize();
    int sys$dup(int oldfd);
    int sys$dup2(int oldfd, int newfd);
    int sys$sigaction(int signum, const sigaction* act, sigaction* old_act);
    int sys$sigprocmask(int how, const sigset_t* set, sigset_t* old_set);
    int sys$sigpending(sigset_t*);
    int sys$getgroups(int size, gid_t*);
    int sys$setgroups(size_t, const gid_t*);
    int sys$pipe(int* pipefd);
    int sys$killpg(int pgrp, int sig);
    int sys$setgid(gid_t);
    int sys$setuid(uid_t);
    unsigned sys$alarm(unsigned seconds);
    int sys$access(const char* pathname, int mode);
    int sys$fcntl(int fd, int cmd, dword extra_arg);
    int sys$ioctl(int fd, unsigned request, unsigned arg);
    int sys$mkdir(const char* pathname, mode_t mode);
    clock_t sys$times(tms*);
    int sys$utime(const char* pathname, const struct utimbuf*);
    int sys$link(const char* old_path, const char* new_path);
    int sys$unlink(const char* pathname);
    int sys$rmdir(const char* pathname);
    int sys$read_tsc(dword* lsw, dword* msw);
    int sys$chmod(const char* pathname, mode_t);
    int sys$socket(int domain, int type, int protocol);
    int sys$bind(int sockfd, const sockaddr* addr, socklen_t);
    int sys$listen(int sockfd, int backlog);
    int sys$accept(int sockfd, sockaddr*, socklen_t*);
    int sys$connect(int sockfd, const sockaddr*, socklen_t);

    int sys$create_shared_buffer(pid_t peer_pid, size_t, void** buffer);
    void* sys$get_shared_buffer(int shared_buffer_id);
    int sys$release_shared_buffer(int shared_buffer_id);

    bool wait_for_connect(Socket&, int& error);

    static void initialize();

    [[noreturn]] void crash();
    [[nodiscard]] static int reap(Process&);

    const TTY* tty() const { return m_tty; }
    void set_tty(TTY* tty) { m_tty = tty; }

    size_t region_count() const { return m_regions.size(); }
    const Vector<RetainPtr<Region>>& regions() const { return m_regions; }
    void dump_regions();

    void did_schedule() { ++m_times_scheduled; }
    dword times_scheduled() const { return m_times_scheduled; }

    dword m_ticks_in_user { 0 };
    dword m_ticks_in_kernel { 0 };

    dword m_ticks_in_user_for_dead_children { 0 };
    dword m_ticks_in_kernel_for_dead_children { 0 };

    pid_t waitee_pid() const { return m_waitee_pid; }

    dword frame_ptr() const { return m_tss.ebp; }
    dword stack_ptr() const { return m_tss.esp; }
    dword stack_top() const { return m_tss.ss == 0x10 ? m_stack_top0 : m_stack_top3; }

    bool validate_read_from_kernel(LinearAddress) const;

    bool validate_read(const void*, size_t) const;
    bool validate_write(void*, size_t) const;
    bool validate_read_str(const char* str);
    template<typename T> bool validate_read_typed(T* value, size_t count = 1) { return validate_read(value, sizeof(T) * count); }
    template<typename T> bool validate_write_typed(T* value, size_t count = 1) { return validate_write(value, sizeof(T) * count); }

    Inode* cwd_inode();
    Inode* executable_inode() { return m_executable.ptr(); }

    size_t number_of_open_file_descriptors() const;
    size_t max_open_file_descriptors() const { return m_max_open_file_descriptors; }

    void send_signal(byte signal, Process* sender);

    ShouldUnblockProcess dispatch_one_pending_signal();
    ShouldUnblockProcess dispatch_signal(byte signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(byte signal);

    size_t amount_virtual() const;
    size_t amount_resident() const;
    size_t amount_shared() const;

    Process* fork(RegisterDump&);
    int exec(String path, Vector<String> arguments, Vector<String> environment);

    bool is_root() const { return m_euid == 0; }

    bool wakeup_requested() { return m_wakeup_requested; }
    void request_wakeup() { m_wakeup_requested = true; }

    FPUState& fpu_state() { return m_fpu_state; }
    bool has_used_fpu() const { return m_has_used_fpu; }
    void set_has_used_fpu(bool b) { m_has_used_fpu = b; }

    Region* allocate_region_with_vmo(LinearAddress, size_t, RetainPtr<VMObject>&&, size_t offset_in_vmo, String&& name, bool is_readable, bool is_writable);
    Region* allocate_file_backed_region(LinearAddress, size_t, RetainPtr<Inode>&&, String&& name, bool is_readable, bool is_writable);
    Region* allocate_region(LinearAddress, size_t, String&& name, bool is_readable = true, bool is_writable = true, bool commit = true);
    bool deallocate_region(Region& region);

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;

    Process(String&& name, uid_t, gid_t, pid_t ppid, RingLevel, RetainPtr<Inode>&& cwd = nullptr, RetainPtr<Inode>&& executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);

    int do_exec(String path, Vector<String> arguments, Vector<String> environment);
    void push_value_on_stack(dword);

    int alloc_fd();
    void set_default_signal_dispositions();
    void disown_all_shared_buffers();

    RetainPtr<PageDirectory> m_page_directory;

    Process* m_prev { nullptr };
    Process* m_next { nullptr };

    String m_name;
    void (*m_entry)() { nullptr };
    pid_t m_pid { 0 };
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    uid_t m_euid { 0 };
    gid_t m_egid { 0 };
    pid_t m_sid { 0 };
    pid_t m_pgid { 0 };
    dword m_ticks { 0 };
    dword m_ticks_left { 0 };
    dword m_stack_top0 { 0 };
    dword m_stack_top3 { 0 };
    FarPtr m_far_ptr;
    State m_state { Invalid };
    Priority m_priority { NormalPriority };
    dword m_wakeup_time { 0 };
    TSS32 m_tss;
    TSS32 m_tss_to_resume_kernel;
    FPUState m_fpu_state;
    struct FileDescriptorAndFlags {
        operator bool() const { return !!descriptor; }
        void clear() { descriptor = nullptr; flags = 0; }
        void set(RetainPtr<FileDescriptor>&& d, dword f = 0) { descriptor = move(d); flags = f; }
        RetainPtr<FileDescriptor> descriptor;
        dword flags { 0 };
    };
    Vector<FileDescriptorAndFlags> m_fds;
    RingLevel m_ring { Ring0 };
    int m_error { 0 };
    void* m_kernel_stack { nullptr };
    dword m_times_scheduled { 0 };
    pid_t m_waitee_pid { -1 };
    int m_blocked_fd { -1 };
    Vector<int> m_select_read_fds;
    Vector<int> m_select_write_fds;
    timeval m_select_timeout;
    bool m_select_has_timeout { false };
    size_t m_max_open_file_descriptors { 16 };
    SignalActionData m_signal_action_data[32];
    dword m_pending_signals { 0 };
    dword m_signal_mask { 0xffffffff };
    RetainPtr<Socket> m_blocked_connecting_socket;

    byte m_termination_status { 0 };
    byte m_termination_signal { 0 };

    RetainPtr<Inode> m_cwd;
    RetainPtr<Inode> m_executable;

    TTY* m_tty { nullptr };

    Region* region_from_range(LinearAddress, size_t);

    Vector<RetainPtr<Region>> m_regions;

    // FIXME: Implement some kind of ASLR?
    LinearAddress m_next_region;

    LinearAddress m_return_to_ring3_from_signal_trampoline;
    LinearAddress m_return_to_ring0_from_signal_trampoline;

    pid_t m_ppid { 0 };
    mode_t m_umask { 022 };

    bool m_was_interrupted_while_blocked { false };

    static void notify_waiters(pid_t waitee, int exit_status, int signal);

    Vector<String> m_initial_arguments;
    Vector<String> m_initial_environment;
    HashTable<gid_t> m_gids;

    Region* m_stack_region { nullptr };
    Region* m_signal_stack_user_region { nullptr };
    Region* m_signal_stack_kernel_region { nullptr };

    RetainPtr<Region> m_display_framebuffer_region;

    dword m_wakeup_requested { false };
    bool m_has_used_fpu { false };
};

extern Process* current;

class ProcessInspectionHandle {
public:
    ProcessInspectionHandle(Process& process)
        : m_process(process)
        , m_original_state(process.state())
    {
        if (&process != current)
            m_process.set_state(Process::BeingInspected);
    }

    ~ProcessInspectionHandle()
    {
        m_process.set_state(m_original_state);
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
    Process::State m_original_state { Process::Invalid };
};

static inline const char* to_string(Process::State state)
{
    switch (state) {
    case Process::Invalid: return "Invalid";
    case Process::Runnable: return "Runnable";
    case Process::Running: return "Running";
    case Process::Dying: return "Dying";
    case Process::Dead: return "Dead";
    case Process::Skip1SchedulerPass: return "Skip1";
    case Process::Skip0SchedulerPasses: return "Skip0";
    case Process::BlockedSleep: return "Sleep";
    case Process::BlockedWait: return "Wait";
    case Process::BlockedRead: return "Read";
    case Process::BlockedWrite: return "Write";
    case Process::BlockedSignal: return "Signal";
    case Process::BlockedSelect: return "Select";
    case Process::BlockedLurking: return "Lurking";
    case Process::BlockedConnect: return "Connect";
    case Process::BeingInspected: return "Inspect";
    }
    ASSERT_NOT_REACHED();
    return nullptr;
}

static inline const char* to_string(Process::Priority state)
{
    switch (state) {
    case Process::LowPriority: return "Low";
    case Process::NormalPriority: return "Normal";
    case Process::HighPriority: return "High";
    }
    ASSERT_NOT_REACHED();
    return nullptr;
}

extern void block(Process::State);
extern void sleep(dword ticks);

extern InlineLinkedList<Process>* g_processes;

template<typename Callback>
inline void Process::for_each(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (!callback(*process))
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
            if (!callback(*process))
                break;
        }
        process = next_process;
    }
}

template<typename Callback>
inline void Process::for_each_in_pgrp(pid_t pgid, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (process->pgid() == pgid) {
            if (!callback(*process))
                break;
        }
        process = next_process;
    }
}

template<typename Callback>
inline void Process::for_each_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (process->state() == state)
            callback(*process);
        process = next_process;
    }
}

template<typename Callback>
inline void Process::for_each_living(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (process->state() != Process::Dead && process->state() != Process::Dying)
            callback(*process);
        process = next_process;
    }
}

inline bool InodeMetadata::may_read(Process& process) const
{
    return may_read(process.euid(), process.gids());
}

inline bool InodeMetadata::may_write(Process& process) const
{
    return may_write(process.euid(), process.gids());
}

inline bool InodeMetadata::may_execute(Process& process) const
{
    return may_execute(process.euid(), process.gids());
}
