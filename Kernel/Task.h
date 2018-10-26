#pragma once

#include "types.h"
#include "InlineLinkedList.h"
#include <AK/String.h>
#include "TSS.h"
#include <AK/Vector.h>
#include "i386.h"
#include <VirtualFileSystem/VirtualFileSystem.h>

//#define TASK_SANITY_CHECKS

class FileHandle;
class Zone;

class Task : public InlineLinkedListNode<Task> {
    friend class InlineLinkedListNode<Task>;
    class Region;
public:
    static Task* createKernelTask(void (*entry)(), String&& name);
    static Task* createUserTask(const String& path, uid_t, gid_t, pid_t parentPID, int& error, const char** args = nullptr);
    ~Task();

    static Vector<Task*> allTasks();

#ifdef TASK_SANITY_CHECKS
    static void checkSanity(const char* msg = nullptr);
#else
    static void checkSanity(const char*) { }
#endif

    enum State {
        Invalid = 0,
        Runnable = 1,
        Running = 2,
        Terminated = 3,
        Crashing = 4,
        Exiting = 5,
        BlockedSleep = 6,
        BlockedWait = 7,
        BlockedRead = 8,
    };

    enum RingLevel {
        Ring0 = 0,
        Ring3 = 3,
    };

    bool isRing0() const { return m_ring == Ring0; }
    bool isRing3() const { return m_ring == Ring3; }

    static Task* fromPID(pid_t);
    static Task* kernelTask();

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

    void block(Task::State);
    void unblock();

    void setWakeupTime(DWORD t) { m_wakeupTime = t; }
    DWORD wakeupTime() const { return m_wakeupTime; }

    static void prepForIRETToNewTask();

    bool tick() { ++m_ticks; return --m_ticksLeft; }
    void setTicksLeft(DWORD t) { m_ticksLeft = t; }

    void setSelector(WORD s) { m_farPtr.selector = s; }
    void setState(State s) { m_state = s; }

    uid_t sys$getuid();
    gid_t sys$getgid();
    pid_t sys$getpid();
    int sys$open(const char* path, size_t pathLength);
    int sys$close(int fd);
    int sys$read(int fd, void* outbuf, size_t nread);
    int sys$lstat(const char*, Unix::stat*);
    int sys$seek(int fd, int offset);
    int sys$kill(pid_t pid, int sig);
    int sys$geterror() { return m_error; }
    void sys$exit(int status);
    int sys$spawn(const char* path, const char** args);
    pid_t sys$waitpid(pid_t, int* wstatus, int options);
    void* sys$mmap(void*, size_t size);
    int sys$munmap(void*, size_t size);
    int sys$get_dir_entries(int fd, void*, size_t);
    int sys$getcwd(char*, size_t);
    int sys$chdir(const char*);
    int sys$sleep(unsigned seconds);
    int sys$gettimeofday(timeval*);
    int sys$gethostname(char* name, size_t length);
    int sys$get_arguments(int* argc, char*** argv);
    int sys$uname(utsname*);

    static void initialize();

    static void taskDidCrash(Task*);

    size_t regionCount() const { return m_regions.size(); }
    const Vector<OwnPtr<Region>>& regions() const { return m_regions; }
    void dumpRegions();

    void didSchedule() { ++m_timesScheduled; }
    dword timesScheduled() const { return m_timesScheduled; }

    pid_t waitee() const { return m_waitee; }

    size_t fileHandleCount() const { return m_fileHandles.size(); }

    dword framePtr() const { return m_tss.ebp; }
    dword stackPtr() const { return m_tss.esp; }
    dword stackTop() const { return m_tss.ss == 0x10 ? m_stackTop0 : m_stackTop3; }

    bool isValidAddressForKernel(LinearAddress) const;
    bool isValidAddressForUser(LinearAddress) const;

private:
    friend class MemoryManager;
    friend bool scheduleNewTask();

    Task(String&& name, uid_t, gid_t, pid_t parentPID, RingLevel);

    void allocateLDT();

    Task* m_prev { nullptr };
    Task* m_next { nullptr };

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
    Descriptor* m_ldtEntries { nullptr };
    Vector<OwnPtr<FileHandle>> m_fileHandles;
    RingLevel m_ring { Ring0 };
    int m_error { 0 };
    void* m_kernelStack { nullptr };
    dword m_timesScheduled { 0 };
    pid_t m_waitee { -1 };
    int m_waiteeStatus { 0 };
    int m_fdBlockedOnRead { -1 };
    size_t m_maxFileHandles { 16 };

    RetainPtr<VirtualFileSystem::Node> m_cwd;

    struct Region {
        Region(LinearAddress, size_t, RetainPtr<Zone>&&, String&&);
        ~Region();
        LinearAddress linearAddress;
        size_t size { 0 };
        RetainPtr<Zone> zone;
        String name;
    };
    Region* allocateRegion(size_t, String&& name);
    bool deallocateRegion(Region& region);

    Region* regionFromRange(LinearAddress, size_t);

    Vector<OwnPtr<Region>> m_regions;

    // FIXME: Implement some kind of ASLR?
    LinearAddress m_nextRegion;

    pid_t m_parentPID { 0 };

    Vector<String> m_arguments;
};

extern void task_init();
extern void yield();
extern bool scheduleNewTask();
extern void switchNow();
extern void block(Task::State);
extern void sleep(DWORD ticks);

/* The currently executing task. NULL during kernel bootup. */
extern Task* current;
