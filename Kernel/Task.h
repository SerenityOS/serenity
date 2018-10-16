#pragma once

#include "types.h"
#include "IPC.h"
#include "DoublyLinkedList.h"
#include "String.h"
#include "TSS.h"
#include "Vector.h"
#include "i386.h"

//#define TASK_SANITY_CHECKS

class FileHandle;

class Task : public DoublyLinkedListNode<Task> {
    friend class DoublyLinkedListNode<Task>;
public:
#ifdef TASK_SANITY_CHECKS
    static void checkSanity(const char* msg = nullptr);
#else
    static void checkSanity(const char*) { }
#endif

    enum State {
        Invalid = 0,
        Runnable = 1,
        Running = 2,
        BlockedReceive = 3,
        BlockedSend = 4,
        BlockedSleep = 5,
        Terminated = 6,
    };

    enum RingLevel {
        Ring0 = 0,
        Ring3 = 3,
    };

    static Task* fromPID(pid_t);
    static Task* fromIPCHandle(IPC::Handle);
    static Task* kernelTask();

    Task(void (*entry)(), const char* name, IPC::Handle, RingLevel);
    ~Task();

    const String& name() const { return m_name; }
    pid_t pid() const { return m_pid; }
    DWORD ticks() const { return m_ticks; }
    WORD selector() const { return m_selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    IPC::Handle handle() const { return m_handle; }

    FileHandle* fileHandleIfExists(int fd);
    FileHandle* createFileHandle();

    bool acceptsMessageFrom(Task&);

    void block(Task::State);
    void unblock();

    void setWakeupTime(DWORD t) { m_wakeupTime = t; }
    DWORD wakeupTime() const { return m_wakeupTime; }

    bool tick() { ++m_ticks; return --m_ticksLeft; }
    void setTicksLeft(DWORD t) { m_ticksLeft = t; }

    void setSelector(WORD s) { m_selector = s; }
    void setState(State s) { m_state = s; }

    uid_t sys$getuid();
    int sys$open(const char* path, size_t pathLength);
    int sys$close(int fd);
    int sys$read(int fd, void* outbuf, size_t nread);
    int sys$seek(int fd, int offset);
    int sys$kill(pid_t pid, int sig);
    int sys$geterror() { return m_error; }
    void sys$sleep(DWORD ticks);

    struct
    {
        IPC::Message msg;
        IPC::Handle dst;
        IPC::Handle src;
        DWORD notifies { 0 };
    } ipc;

    static void initialize();
    void setError(int);

private:
    FileHandle* openFile(String&&);

    void allocateLDT();

    Task* m_prev { nullptr };
    Task* m_next { nullptr };

    String m_name;
    void (*m_entry)() { nullptr };
    pid_t m_pid { 0 };
    uid_t m_uid { 0 };
    DWORD m_ticks { 0 };
    DWORD m_ticksLeft { 0 };
    IPC::Handle m_handle { 0 };
    DWORD m_stackTop { 0 };
    WORD m_selector { 0 };
    State m_state { Invalid };
    DWORD m_wakeupTime { 0 };
    TSS32 m_tss;
    Descriptor* m_ldtEntries { nullptr };
    Vector<FileHandle*> m_fileHandles;
    RingLevel m_ring { Ring0 };
    int m_error { 0 };

    static Task* s_kernelTask;
};

extern void task_init();
extern void yield();
extern void sched();
extern void block(Task::State);
extern void sleep(DWORD ticks);

/* The currently executing task. NULL during kernel bootup. */
extern Task* current;
