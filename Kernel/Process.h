#pragma once

#include "types.h"
#include "InlineLinkedList.h"
#include <AK/String.h>
#include "TSS.h"
#include <AK/Vector.h>
#include "i386.h"
#include <VirtualFileSystem/VirtualFileSystem.h>
#include <VirtualFileSystem/UnixTypes.h>
#include "TTY.h"
#include "Syscall.h"

class FileDescriptor;
class PageDirectory;
class Region;
class VMObject;
class Zone;

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

class Process : public InlineLinkedListNode<Process> {
    friend class InlineLinkedListNode<Process>;
public:
    static Process* create_kernel_process(void (*entry)(), String&& name);
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
    DWORD ticks() const { return m_ticks; }
    WORD selector() const { return m_farPtr.selector; }
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

    void setWakeupTime(DWORD t) { m_wakeupTime = t; }
    DWORD wakeupTime() const { return m_wakeupTime; }

    template<typename Callback> static void for_each(Callback);
    template<typename Callback> static void for_each_in_pgrp(pid_t, Callback);
    template<typename Callback> static void for_each_in_state(State, Callback);
    template<typename Callback> static void for_each_not_in_state(State, Callback);

    bool tick() { ++m_ticks; return --m_ticksLeft; }
    void set_ticks_left(dword t) { m_ticksLeft = t; }

    void setSelector(WORD s) { m_farPtr.selector = s; }
    void set_state(State s) { m_state = s; }

    pid_t sys$setsid();
    pid_t sys$getsid(pid_t);
    int sys$setpgid(pid_t pid, pid_t pgid);
    pid_t sys$getpgrp();
    pid_t sys$getpgid(pid_t);
    pid_t sys$tcgetpgrp(int fd);
    int sys$tcsetpgrp(int fd, pid_t pgid);
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
    int sys$get_dir_entries(int fd, void*, size_t);
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
    Unix::sighandler_t sys$signal(int signum, Unix::sighandler_t);
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

    static void initialize();

    void crash() NORETURN;
    static void reap(Process&);

    const TTY* tty() const { return m_tty; }

    size_t regionCount() const { return m_regions.size(); }
    const Vector<RetainPtr<Region>>& regions() const { return m_regions; }
    void dumpRegions();

    void did_schedule() { ++m_timesScheduled; }
    dword timesScheduled() const { return m_timesScheduled; }

    pid_t waitee() const { return m_waitee; }

    dword framePtr() const { return m_tss.ebp; }
    dword stackPtr() const { return m_tss.esp; }
    dword stackTop() const { return m_tss.ss == 0x10 ? m_stackTop0 : m_stackTop3; }

    bool isValidAddressForKernel(LinearAddress) const;
    bool validate_user_read(LinearAddress) const;
    bool validate_user_write(LinearAddress) const;

    InodeIdentifier cwdInode() const { return m_cwd ? m_cwd->inode : InodeIdentifier(); }
    InodeIdentifier executableInode() const { return m_executable ? m_executable->inode : InodeIdentifier(); }

    size_t number_of_open_file_descriptors() const;
    size_t max_open_file_descriptors() const { return m_max_open_file_descriptors; }

    void send_signal(byte signal, Process* sender);
    void dispatch_one_pending_signal();
    void dispatch_signal(byte signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(byte signal);

    Process* fork(RegisterDump&);
    int exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment);

    bool is_root() const { return m_euid == 0; }

private:
    friend class MemoryManager;
    friend class Scheduler;
    friend class Region;

    Process(String&& name, uid_t, gid_t, pid_t ppid, RingLevel, RetainPtr<VirtualFileSystem::Node>&& cwd = nullptr, RetainPtr<VirtualFileSystem::Node>&& executable = nullptr, TTY* = nullptr, Process* fork_parent = nullptr);

    int do_exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment);
    void push_value_on_stack(dword);

    PageDirectory* m_page_directory { nullptr };

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
    DWORD m_ticks { 0 };
    DWORD m_ticksLeft { 0 };
    DWORD m_stackTop0 { 0 };
    DWORD m_stackTop3 { 0 };
    FarPtr m_farPtr;
    State m_state { Invalid };
    DWORD m_wakeupTime { 0 };
    TSS32 m_tss;
    TSS32 m_tss_to_resume_kernel;
    Vector<RetainPtr<FileDescriptor>> m_file_descriptors;
    RingLevel m_ring { Ring0 };
    int m_error { 0 };
    void* m_kernelStack { nullptr };
    dword m_timesScheduled { 0 };
    pid_t m_waitee { -1 };
    int m_waitee_status { 0 };
    int m_fdBlockedOnRead { -1 };
    size_t m_max_open_file_descriptors { 16 };
    SignalActionData m_signal_action_data[32];
    dword m_pending_signals { 0 };
    dword m_signal_mask { 0xffffffff };

    byte m_termination_status { 0 };
    byte m_termination_signal { 0 };

    RetainPtr<VirtualFileSystem::Node> m_cwd;
    RetainPtr<VirtualFileSystem::Node> m_executable;

    TTY* m_tty { nullptr };

    Region* allocate_region(LinearAddress, size_t, String&& name, bool is_readable = true, bool is_writable = true);
    Region* allocate_file_backed_region(LinearAddress, size_t, RetainPtr<VirtualFileSystem::Node>&& vnode, String&& name, bool is_readable, bool is_writable);
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
    case Process::BlockedSignal: return "Signal";
    case Process::BeingInspected: return "Inspect";
    }
    ASSERT_NOT_REACHED();
    return nullptr;
}

extern void block(Process::State);
extern void sleep(DWORD ticks);

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
