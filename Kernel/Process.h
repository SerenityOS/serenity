#pragma once

#include "types.h"
#include "TSS.h"
#include "i386.h"
#include "TTY.h"
#include "Syscall.h"
#include "GUITypes.h"
#include <VirtualFileSystem/VirtualFileSystem.h>
#include <VirtualFileSystem/UnixTypes.h>
#include <AK/InlineLinkedList.h>
#include <AK/AKString.h>
#include <AK/Vector.h>

class FileDescriptor;
class PageDirectory;
class Region;
class VMObject;
class Zone;
class Window;

#define COOL_GLOBALS
#ifdef COOL_GLOBALS
struct CoolGlobals {
    pid_t current_pid;
};
extern CoolGlobals* g_cool_globals;
#endif

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
    byte* framebuffer;
};

class Process : public InlineLinkedListNode<Process> {
    friend class InlineLinkedListNode<Process>;
    friend class WindowManager; // FIXME: Make a better API for allocate_region().
    friend class GraphicsBitmap; // FIXME: Make a better API for allocate_region().
public:
    static Process* create_kernel_process(String&& name, void (*entry)());
    static Process* create_user_process(const String& path, uid_t, gid_t, pid_t ppid, int& error, Vector<String>&& arguments = Vector<String>(), Vector<String>&& environment = Vector<String>(), TTY* = nullptr);
    ~Process();

    static Vector<Process*> allProcesses();

    enum State {
        Invalid = 0,
        Runnable,
        Running,
        Skip1SchedulerPass,
        Skip0SchedulerPasses,
        Dead,
        BeingInspected,
        BlockedSleep,
        BlockedWait,
        BlockedRead,
        BlockedWrite,
        BlockedSignal,
    };

    enum RingLevel {
        Ring0 = 0,
        Ring3 = 3,
    };

    bool isRing0() const { return m_ring == Ring0; }
    bool isRing3() const { return m_ring == Ring3; }

    bool is_blocked() const
    {
        return m_state == BlockedSleep || m_state == BlockedWait || m_state == BlockedRead || m_state == BlockedSignal;
    }

    PageDirectory& page_directory() { return *m_page_directory; }
    const PageDirectory& page_directory() const { return *m_page_directory; }

    bool in_kernel() const { return (m_tss.cs & 0x03) == 0; }

    static Process* from_pid(pid_t);

    const String& name() const { return m_name; }
    pid_t pid() const { return m_pid; }
    pid_t sid() const { return m_sid; }
    pid_t pgid() const { return m_pgid; }
    dword ticks() const { return m_ticks; }
    word selector() const { return m_farPtr.selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    uid_t euid() const { return m_euid; }
    gid_t egid() const { return m_egid; }
    pid_t ppid() const { return m_ppid; }

    const FarPtr& farPtr() const { return m_farPtr; }

    FileDescriptor* file_descriptor(int fd);
    const FileDescriptor* file_descriptor(int fd) const;

    void block(Process::State);
    void unblock();

    void setWakeupTime(dword t) { m_wakeupTime = t; }
    dword wakeupTime() const { return m_wakeupTime; }

    template<typename Callback> static void for_each(Callback);
    template<typename Callback> static void for_each_in_pgrp(pid_t, Callback);
    template<typename Callback> static void for_each_in_state(State, Callback);
    template<typename Callback> static void for_each_not_in_state(State, Callback);
    template<typename Callback> void for_each_child(Callback);

    bool tick() { ++m_ticks; return --m_ticksLeft; }
    void set_ticks_left(dword t) { m_ticksLeft = t; }

    void setSelector(word s) { m_farPtr.selector = s; }
    void set_state(State s) { m_state = s; }

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
    int sys$open(const char* path, int options);
    int sys$close(int fd);
    ssize_t sys$read(int fd, void* outbuf, size_t nread);
    ssize_t sys$write(int fd, const void*, size_t);
    int sys$fstat(int fd, Unix::stat*);
    int sys$lstat(const char*, Unix::stat*);
    int sys$stat(const char*, Unix::stat*);
    int sys$lseek(int fd, off_t, int whence);
    int sys$kill(pid_t pid, int sig);
    int sys$geterror() { return m_error; }
    void sys$exit(int status) NORETURN;
    void sys$sigreturn() NORETURN;
    pid_t sys$waitpid(pid_t, int* wstatus, int options);
    void* sys$mmap(const Syscall::SC_mmap_params*);
    int sys$munmap(void*, size_t size);
    int sys$set_mmap_name(void*, size_t, const char*);
    ssize_t sys$get_dir_entries(int fd, void*, size_t);
    int sys$getcwd(char*, size_t);
    int sys$chdir(const char*);
    int sys$sleep(unsigned seconds);
    int sys$gettimeofday(timeval*);
    int sys$gethostname(char* name, size_t length);
    int sys$get_arguments(int* argc, char*** argv);
    int sys$get_environment(char*** environ);
    int sys$uname(utsname*);
    int sys$readlink(const char*, char*, size_t);
    int sys$ttyname_r(int fd, char*, size_t);
    pid_t sys$fork(RegisterDump&);
    int sys$execve(const char* filename, const char** argv, const char** envp);
    int sys$isatty(int fd);
    int sys$getdtablesize();
    int sys$dup(int oldfd);
    int sys$dup2(int oldfd, int newfd);
    int sys$sigaction(int signum, const Unix::sigaction* act, Unix::sigaction* old_act);
    int sys$sigprocmask(int how, const Unix::sigset_t* set, Unix::sigset_t* old_set);
    int sys$sigpending(Unix::sigset_t*);
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
    Unix::clock_t sys$times(Unix::tms*);
    int sys$utime(const char* pathname, const struct Unix::utimbuf*);

    int gui$create_window(const GUI_CreateWindowParameters*);
    int gui$destroy_window(int window_id);

    DisplayInfo get_display_info();

    static void initialize();
    static void initialize_gui_statics();

    void crash() NORETURN;
    static int reap(Process&) WARN_UNUSED_RESULT;

    const TTY* tty() const { return m_tty; }

    size_t regionCount() const { return m_regions.size(); }
    const Vector<RetainPtr<Region>>& regions() const { return m_regions; }
    void dumpRegions();

    void did_schedule() { ++m_timesScheduled; }
    dword timesScheduled() const { return m_timesScheduled; }

    dword m_ticks_in_user { 0 };
    dword m_ticks_in_kernel { 0 };

    dword m_ticks_in_user_for_dead_children { 0 };
    dword m_ticks_in_kernel_for_dead_children { 0 };

    pid_t waitee_pid() const { return m_waitee_pid; }

    dword framePtr() const { return m_tss.ebp; }
    dword stackPtr() const { return m_tss.esp; }
    dword stackTop() const { return m_tss.ss == 0x10 ? m_stackTop0 : m_stackTop3; }

    bool validate_read_from_kernel(LinearAddress) const;

    bool validate_read(const void*, size_t) const;
    bool validate_write(void*, size_t) const;
    bool validate_read_str(const char* str) { return validate_read(str, strlen(str) + 1); }
    template<typename T> bool validate_read_typed(T* value, size_t count = 1) { return validate_read(value, sizeof(T) * count); }
    template<typename T> bool validate_write_typed(T* value, size_t count = 1) { return validate_write(value, sizeof(T) * count); }

    Inode* cwd_inode() { return m_cwd ? m_cwd->core_inode() : nullptr; }
    Inode* executable_inode() { return m_executable ? m_executable->core_inode() : nullptr; }

    size_t number_of_open_file_descriptors() const;
    size_t max_open_file_descriptors() const { return m_max_open_file_descriptors; }

    void send_signal(byte signal, Process* sender);
    bool dispatch_one_pending_signal();
    bool dispatch_signal(byte signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(byte signal);

    Process* fork(RegisterDump&);
    int exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment);

    bool is_root() const { return m_euid == 0; }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;

    Process(String&& name, uid_t, gid_t, pid_t ppid, RingLevel, RetainPtr<Vnode>&& cwd = nullptr, RetainPtr<Vnode>&& executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);

    int do_exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment);
    void push_value_on_stack(dword);

    int alloc_fd();

    OwnPtr<PageDirectory> m_page_directory;

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
    dword m_ticksLeft { 0 };
    dword m_stackTop0 { 0 };
    dword m_stackTop3 { 0 };
    FarPtr m_farPtr;
    State m_state { Invalid };
    dword m_wakeupTime { 0 };
    TSS32 m_tss;
    TSS32 m_tss_to_resume_kernel;
    struct FileDescriptorAndFlags {
        operator bool() const { return !!descriptor; }
        void clear() { descriptor = nullptr; flags = 0; }
        void set(RetainPtr<FileDescriptor>&& d, dword f = 0) { descriptor = move(d), flags = f; }
        RetainPtr<FileDescriptor> descriptor;
        dword flags { 0 };
    };
    Vector<FileDescriptorAndFlags> m_fds;
    RingLevel m_ring { Ring0 };
    int m_error { 0 };
    void* m_kernelStack { nullptr };
    dword m_timesScheduled { 0 };
    pid_t m_waitee_pid { -1 };
    int m_blocked_fd { -1 };
    size_t m_max_open_file_descriptors { 16 };
    SignalActionData m_signal_action_data[32];
    dword m_pending_signals { 0 };
    dword m_signal_mask { 0xffffffff };

    byte m_termination_status { 0 };
    byte m_termination_signal { 0 };

    RetainPtr<Vnode> m_cwd;
    RetainPtr<Vnode> m_executable;

    TTY* m_tty { nullptr };

    Region* allocate_region(LinearAddress, size_t, String&& name, bool is_readable = true, bool is_writable = true, bool commit = true);
    Region* allocate_file_backed_region(LinearAddress, size_t, RetainPtr<Vnode>&& vnode, String&& name, bool is_readable, bool is_writable);
    Region* allocate_region_with_vmo(LinearAddress, size_t, RetainPtr<VMObject>&&, size_t offset_in_vmo, String&& name, bool is_readable, bool is_writable);
    bool deallocate_region(Region& region);

    Region* regionFromRange(LinearAddress, size_t);

    Vector<RetainPtr<Region>> m_regions;

    // FIXME: Implement some kind of ASLR?
    LinearAddress m_nextRegion;

    LinearAddress m_return_to_ring3_from_signal_trampoline;
    LinearAddress m_return_to_ring0_from_signal_trampoline;

    pid_t m_ppid { 0 };
    mode_t m_umask { 022 };

    bool m_was_interrupted_while_blocked { false };

    static void notify_waiters(pid_t waitee, int exit_status, int signal);

    Vector<String> m_arguments;
    Vector<String> m_initialEnvironment;
    HashTable<gid_t> m_gids;

    Region* m_stack_region { nullptr };
    Region* m_signal_stack_user_region { nullptr };
    Region* m_signal_stack_kernel_region { nullptr };

    RetainPtr<Region> m_display_framebuffer_region;

    Vector<Window*> m_windows;
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

    Process* operator->() { return &m_process; }
    Process& operator*() { return m_process; }
private:
    Process& m_process;
    Process::State m_original_state { Process::Invalid };
};

static inline const char* toString(Process::State state)
{
    switch (state) {
    case Process::Invalid: return "Invalid";
    case Process::Runnable: return "Runnable";
    case Process::Running: return "Running";
    case Process::Dead: return "Dead";
    case Process::Skip1SchedulerPass: return "Skip1";
    case Process::Skip0SchedulerPasses: return "Skip0";
    case Process::BlockedSleep: return "Sleep";
    case Process::BlockedWait: return "Wait";
    case Process::BlockedRead: return "Read";
    case Process::BlockedWrite: return "Write";
    case Process::BlockedSignal: return "Signal";
    case Process::BeingInspected: return "Inspect";
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
inline void Process::for_each_not_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = g_processes->head(); process;) {
        auto* next_process = process->next();
        if (process->state() != state)
            callback(*process);
        process = next_process;
    }
}
