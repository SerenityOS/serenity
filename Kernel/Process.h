#pragma once

#include "types.h"
#include "InlineLinkedList.h"
#include <AK/String.h>
#include "TSS.h"
#include <AK/Vector.h>
#include "i386.h"
#include <VirtualFileSystem/VirtualFileSystem.h>
#include "TTY.h"

class FileHandle;
class Region;
class Subregion;
class Zone;

class Process : public InlineLinkedListNode<Process> {
    friend class InlineLinkedListNode<Process>;
public:
    static Process* createKernelProcess(void (*entry)(), String&& name);
    static Process* createUserProcess(const String& path, uid_t, gid_t, pid_t parentPID, int& error, const char** args = nullptr, TTY* = nullptr);
    ~Process();

    static Vector<Process*> allProcesses();

    enum State {
        Invalid = 0,
        Runnable = 1,
        Running = 2,
        Terminated = 3,
        Crashing = 4,
        Exiting = 5,
        BeingInspected = 6,
        BlockedSleep = 7,
        BlockedWait = 8,
        BlockedRead = 9,
    };

    enum RingLevel {
        Ring0 = 0,
        Ring3 = 3,
    };

    bool isRing0() const { return m_ring == Ring0; }
    bool isRing3() const { return m_ring == Ring3; }

    static Process* fromPID(pid_t);
    static Process* kernelProcess();

    const String& name() const { return m_name; }
    pid_t pid() const { return m_pid; }
    DWORD ticks() const { return m_ticks; }
    WORD selector() const { return m_farPtr.selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    uid_t uid() const { return m_uid; }
    uid_t gid() const { return m_gid; }

    pid_t parentPID() const { return m_parentPID; }

    const FarPtr& farPtr() const { return m_farPtr; }

    FileHandle* fileHandleIfExists(int fd);

    static void doHouseKeeping();

    void block(Process::State);
    void unblock();

    void setWakeupTime(DWORD t) { m_wakeupTime = t; }
    DWORD wakeupTime() const { return m_wakeupTime; }

    static void prepForIRETToNewProcess();

    bool tick() { ++m_ticks; return --m_ticksLeft; }
    void setTicksLeft(DWORD t) { m_ticksLeft = t; }

    void setSelector(WORD s) { m_farPtr.selector = s; }
    void set_state(State s) { m_state = s; }

    uid_t sys$getuid();
    gid_t sys$getgid();
    pid_t sys$getpid();
    int sys$open(const char* path, int options);
    int sys$close(int fd);
    ssize_t sys$read(int fd, void* outbuf, size_t nread);
    ssize_t sys$write(int fd, const void*, size_t);
    int sys$lstat(const char*, Unix::stat*);
    int sys$stat(const char*, Unix::stat*);
    int sys$lseek(int fd, off_t, int whence);
    int sys$kill(pid_t pid, int sig);
    int sys$geterror() { return m_error; }
    void sys$exit(int status);
    int sys$spawn(const char* path, const char** args);
    pid_t sys$waitpid(pid_t, int* wstatus, int options);
    void* sys$mmap(void*, size_t size);
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

    static void initialize();

    static void processDidCrash(Process*);

    const TTY* tty() const { return m_tty; }

    size_t regionCount() const { return m_regions.size(); }
    const Vector<RetainPtr<Region>>& regions() const { return m_regions; }
    size_t subregionCount() const { return m_regions.size(); }
    const Vector<OwnPtr<Subregion>>& subregions() const { return m_subregions; }
    void dumpRegions();

    void didSchedule() { ++m_timesScheduled; }
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
    FileHandle* file_descriptor(size_t i) { return m_file_descriptors[i].ptr(); }
    const FileHandle* file_descriptor(size_t i) const { return m_file_descriptors[i].ptr(); }

private:
    friend class MemoryManager;
    friend bool scheduleNewProcess();

    Process(String&& name, uid_t, gid_t, pid_t parentPID, RingLevel, RetainPtr<VirtualFileSystem::Node>&& cwd = nullptr, RetainPtr<VirtualFileSystem::Node>&& executable = nullptr, TTY* = nullptr);

    void allocateLDT();

    dword* m_pageDirectory { nullptr };

    Process* m_prev { nullptr };
    Process* m_next { nullptr };

    String m_name;
    void (*m_entry)() { nullptr };
    pid_t m_pid { 0 };
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    DWORD m_ticks { 0 };
    DWORD m_ticksLeft { 0 };
    DWORD m_stackTop0 { 0 };
    DWORD m_stackTop3 { 0 };
    FarPtr m_farPtr;
    State m_state { Invalid };
    DWORD m_wakeupTime { 0 };
    TSS32 m_tss;
    word m_ldt_selector { 0 };
    Descriptor* m_ldtEntries { nullptr };
    Vector<OwnPtr<FileHandle>> m_file_descriptors;
    RingLevel m_ring { Ring0 };
    int m_error { 0 };
    void* m_kernelStack { nullptr };
    dword m_timesScheduled { 0 };
    pid_t m_waitee { -1 };
    int m_waiteeStatus { 0 };
    int m_fdBlockedOnRead { -1 };
    size_t m_max_open_file_descriptors { 16 };

    RetainPtr<VirtualFileSystem::Node> m_cwd;
    RetainPtr<VirtualFileSystem::Node> m_executable;

    TTY* m_tty { nullptr };

    Region* allocateRegion(size_t, String&& name);
    Region* allocateRegion(size_t, String&& name, LinearAddress);
    bool deallocateRegion(Region& region);

    Region* regionFromRange(LinearAddress, size_t);

    Vector<RetainPtr<Region>> m_regions;
    Vector<OwnPtr<Subregion>> m_subregions;

    // FIXME: Implement some kind of ASLR?
    LinearAddress m_nextRegion;

    pid_t m_parentPID { 0 };

    static void notify_waiters(pid_t waitee, int exit_status, int signal);
    void murder(int signal);

    Vector<String> m_arguments;
    Vector<String> m_initialEnvironment;
};

class ProcessInspectionScope {
public:
    ProcessInspectionScope(Process& process)
        : m_process(process)
        , m_original_state(process.state())
    {
        m_process.set_state(Process::BeingInspected);
    }

    ~ProcessInspectionScope()
    {
        m_process.set_state(m_original_state);
    }
private:
    Process& m_process;
    Process::State m_original_state { Process::Invalid };
};

extern void yield();
extern bool scheduleNewProcess();
extern void switchNow();
extern void block(Process::State);
extern void sleep(DWORD ticks);

extern Process* current;
