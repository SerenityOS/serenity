#include "types.h"
#include "Task.h"
#include "kmalloc.h"
#include "VGA.h"
#include "StdLib.h"
#include "i386.h"
#include "system.h"
#include <VirtualFileSystem/FileHandle.h>
#include <VirtualFileSystem/VirtualFileSystem.h>
#include <ELFLoader/ExecSpace.h>
#include "MemoryManager.h"
#include "errno.h"
#include "i8253.h"
#include "RTC.h"

//#define DEBUG_IO
//#define TASK_DEBUG

static const DWORD defaultStackSize = 16384;

Task* current;
Task* s_kernelTask;

static pid_t next_pid;
static InlineLinkedList<Task>* s_tasks;
static InlineLinkedList<Task>* s_deadTasks;
static String* s_hostname;

static String& hostname(InterruptDisabler&)
{
    ASSERT(s_hostname);
    return *s_hostname;
}

static bool contextSwitch(Task*);

static void redoKernelTaskTSS()
{
    if (!s_kernelTask->selector())
        s_kernelTask->setSelector(allocateGDTEntry());

    auto& tssDescriptor = getGDTEntry(s_kernelTask->selector());

    tssDescriptor.setBase(&s_kernelTask->tss());
    tssDescriptor.setLimit(0xffff);
    tssDescriptor.dpl = 0;
    tssDescriptor.segment_present = 1;
    tssDescriptor.granularity = 1;
    tssDescriptor.zero = 0;
    tssDescriptor.operation_size = 1;
    tssDescriptor.descriptor_type = 0;
    tssDescriptor.type = 9;

    flushGDT();
}

void Task::prepForIRETToNewTask()
{
    redoKernelTaskTSS();
    s_kernelTask->tss().backlink = current->selector();
    loadTaskRegister(s_kernelTask->selector());
}

void Task::initialize()
{
    current = nullptr;
    next_pid = 0;
    s_tasks = new InlineLinkedList<Task>;
    s_deadTasks = new InlineLinkedList<Task>;
    s_kernelTask = Task::createKernelTask(nullptr, "colonel");
    s_hostname = new String("birx");
    redoKernelTaskTSS();
    loadTaskRegister(s_kernelTask->selector());
}

#ifdef TASK_SANITY_CHECKS
void Task::checkSanity(const char* msg)
{
    char ch = current->name()[0];
    kprintf("<%p> %s{%u}%b [%d] :%b: sanity check <%s>\n",
            current->name().characters(),
            current->name().characters(),
            current->name().length(),
            current->name()[current->name().length() - 1],
            current->pid(), ch, msg ? msg : "");
    ASSERT((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'));
}
#endif

void Task::allocateLDT()
{
    ASSERT(!m_tss.ldt);
    static const WORD numLDTEntries = 4;
    WORD newLDTSelector = allocateGDTEntry();
    m_ldtEntries = new Descriptor[numLDTEntries];
#if 0
    kprintf("new ldt selector = %x\n", newLDTSelector);
    kprintf("new ldt table at = %p\n", m_ldtEntries);
    kprintf("new ldt table size = %u\n", (numLDTEntries * 8) - 1);
#endif
    Descriptor& ldt = getGDTEntry(newLDTSelector);
    ldt.setBase(m_ldtEntries);
    ldt.setLimit(numLDTEntries * 8 - 1);
    ldt.dpl = 0;
    ldt.segment_present = 1;
    ldt.granularity = 0;
    ldt.zero = 0;
    ldt.operation_size = 1;
    ldt.descriptor_type = 0;
    ldt.type = Descriptor::LDT;
    m_tss.ldt = newLDTSelector;
}

Vector<Task*> Task::allTasks()
{
    InterruptDisabler disabler;
    Vector<Task*> tasks;
    tasks.ensureCapacity(s_tasks->sizeSlow());
    for (auto* task = s_tasks->head(); task; task = task->next())
        tasks.append(task);
    return tasks;
}

Task::Region* Task::allocateRegion(size_t size, String&& name)
{
    // FIXME: This needs sanity checks. What if this overlaps existing regions?

    auto zone = MemoryManager::the().createZone(size);
    ASSERT(zone);
    m_regions.append(make<Region>(m_nextRegion, size, move(zone), move(name)));
    m_nextRegion = m_nextRegion.offset(size).offset(16384);
    return m_regions.last().ptr();
}

bool Task::deallocateRegion(Region& region)
{
    for (size_t i = 0; i < m_regions.size(); ++i) {
        if (m_regions[i].ptr() == &region) {
            // FIXME: This seems racy.
            MemoryManager::the().unmapRegion(*this, region);
            m_regions.remove(i);
            return true;
        }
    }
    return false;
}

Task::Region* Task::regionFromRange(LinearAddress laddr, size_t size)
{
    for (auto& region : m_regions) {
        if (region->linearAddress == laddr && region->size == size)
            return region.ptr();
    }
    return nullptr;
}

void* Task::sys$mmap(void* addr, size_t size)
{
    // FIXME: Implement mapping at a client-preferred address.
    ASSERT(addr == nullptr);
    auto* region = allocateRegion(size, "mmap");
    if (!region)
        return (void*)-1;
    MemoryManager::the().mapRegion(*this, *region);
    return (void*)region->linearAddress.get();
}

int Task::sys$munmap(void* addr, size_t size)
{
    auto* region = regionFromRange(LinearAddress((dword)addr), size);
    if (!region)
        return -1;
    if (!deallocateRegion(*region))
        return -1;
    return 0;
}

int Task::sys$gethostname(char* buffer, size_t size)
{
    String hn;
    {
        InterruptDisabler disabler;
        hn = hostname(disabler).isolatedCopy();
    }
    if (size < (hn.length() + 1))
        return -ENAMETOOLONG;
    memcpy(buffer, hn.characters(), size);
    return 0;
}

int Task::sys$spawn(const char* path, const char** args)
{
    int error = 0;
    auto* child = Task::createUserTask(path, m_uid, m_gid, m_pid, error, args);
    if (child)
        return child->pid();
    return error;
}

Task* Task::createUserTask(const String& path, uid_t uid, gid_t gid, pid_t parentPID, int& error, const char** args)
{
    auto parts = path.split('/');
    if (parts.isEmpty()) {
        error = -ENOENT;
        return nullptr;
    }

    RetainPtr<VirtualFileSystem::Node> cwd;
    {
        InterruptDisabler disabler;
        if (auto* parentTask = Task::fromPID(parentPID))
            cwd = parentTask->m_cwd.copyRef();
    }

    auto handle = VirtualFileSystem::the().open(path, cwd.ptr());
    if (!handle) {
        error = -ENOENT; // FIXME: Get a more detailed error from VFS.
        return nullptr;
    }

    auto elfData = handle->readEntireFile();
    if (!elfData) {
        error = -EIO; // FIXME: Get a more detailed error from VFS.
        return nullptr;
    }

    Vector<String> taskArguments;
    if (args) {
        for (size_t i = 0; args[i]; ++i) {
            taskArguments.append(args[i]);
        }
    } else {
        taskArguments.append(parts.last());
    }

    InterruptDisabler disabler; // FIXME: Get rid of this, jesus christ. This "critical" section is HUGE.
    Task* t = new Task(parts.takeLast(), uid, gid, parentPID, Ring3);

    t->m_arguments = move(taskArguments);

    ExecSpace space;
    space.hookableAlloc = [&] (const String& name, size_t size) {
        if (!size)
            return (void*)nullptr;
        size = ((size / 4096) + 1) * 4096;
        Region* region = t->allocateRegion(size, String(name));
        ASSERT(region);
        MemoryManager::the().mapRegion(*t, *region);
        return (void*)region->linearAddress.asPtr();
    };
    bool success = space.loadELF(move(elfData));
    if (!success) {
        // FIXME: This is ugly. If we need to do this, it should be at a different level.
        MemoryManager::the().unmapRegionsForTask(*t);
        MemoryManager::the().mapRegionsForTask(*current);
        delete t;
        kprintf("Failure loading ELF %s\n", path.characters());
        error = -ENOEXEC;
        return nullptr;
    }

    t->m_tss.eip = (dword)space.symbolPtr("_start");
    if (!t->m_tss.eip) {
        // FIXME: This is ugly. If we need to do this, it should be at a different level.
        MemoryManager::the().unmapRegionsForTask(*t);
        MemoryManager::the().mapRegionsForTask(*current);
        delete t;
        error = -ENOEXEC;
        return nullptr;
    }

    // FIXME: This is ugly. If we need to do this, it should be at a different level.
    MemoryManager::the().unmapRegionsForTask(*t);
    MemoryManager::the().mapRegionsForTask(*current);

    s_tasks->prepend(t);
    system.nprocess++;
#ifdef TASK_DEBUG
    kprintf("Task %u (%s) spawned @ %p\n", t->pid(), t->name().characters(), t->m_tss.eip);
#endif

    error = 0;
    return t;
}

int Task::sys$get_arguments(int* argc, char*** argv)
{
    auto* region = allocateRegion(4096, "argv");
    if (!region)
        return -ENOMEM;
    MemoryManager::the().mapRegion(*this, *region);
    char* argpage = (char*)region->linearAddress.get();
    *argc = m_arguments.size();
    *argv = (char**)argpage;
    char* bufptr = argpage + (sizeof(char*) * m_arguments.size());
    for (size_t i = 0; i < m_arguments.size(); ++i) {
        (*argv)[i] = bufptr;
        memcpy(bufptr, m_arguments[i].characters(), m_arguments[i].length());
        bufptr += m_arguments[i].length();
        *(bufptr++) = '\0';
    }
    return 0;
}

Task* Task::createKernelTask(void (*e)(), String&& name)
{
    Task* task = new Task(move(name), (uid_t)0, (gid_t)0, (pid_t)0, Ring0);
    task->m_tss.eip = (dword)e;

    if (task->pid() != 0) {
        InterruptDisabler disabler;
        s_tasks->prepend(task);
        system.nprocess++;
#ifdef TASK_DEBUG
        kprintf("Kernel task %u (%s) spawned @ %p\n", task->pid(), task->name().characters(), task->m_tss.eip);
#endif
    }

    return task;
}

Task::Task(String&& name, uid_t uid, gid_t gid, pid_t parentPID, RingLevel ring)
    : m_name(move(name))
    , m_pid(next_pid++)
    , m_uid(uid)
    , m_gid(gid)
    , m_state(Runnable)
    , m_ring(ring)
    , m_parentPID(parentPID)
{
    m_fileHandles.append(nullptr); // stdin
    m_fileHandles.append(nullptr); // stdout
    m_fileHandles.append(nullptr); // stderr

    auto* parentTask = Task::fromPID(parentPID);
    if (parentTask)
        m_cwd = parentTask->m_cwd.copyRef();
    else
        m_cwd = nullptr;

    m_nextRegion = LinearAddress(0x600000);

    memset(&m_tss, 0, sizeof(m_tss));

    if (isRing3()) {
        memset(&m_ldtEntries, 0, sizeof(m_ldtEntries));
        allocateLDT();
    }

    // Only IF is set when a task boots.
    m_tss.eflags = 0x0202;

    word cs, ds, ss;

    if (isRing0()) {
        cs = 0x08;
        ds = 0x10;
        ss = 0x10;
    } else {
        cs = 0x1b;
        ds = 0x23;
        ss = 0x23;
    }

    m_tss.ds = ds;
    m_tss.es = ds;
    m_tss.fs = ds;
    m_tss.gs = ds;
    m_tss.ss = ss;
    m_tss.cs = cs;

    m_tss.cr3 = MemoryManager::the().pageDirectoryBase().get();

    if (isRing0()) {
        // FIXME: This memory is leaked.
        // But uh, there's also no kernel task termination, so I guess it's not technically leaked...
        dword stackBottom = (dword)kmalloc(defaultStackSize);
        m_stackTop = (stackBottom + defaultStackSize) & 0xffffff8;
        m_tss.esp = m_stackTop;
    } else {
        auto* region = allocateRegion(defaultStackSize, "stack");
        ASSERT(region);
        m_stackTop = region->linearAddress.offset(defaultStackSize).get() & 0xfffffff8;
    }
    m_tss.esp = m_stackTop;

    if (isRing3()) {
        // Ring3 tasks need a separate stack for Ring0.
        m_kernelStack = kmalloc(defaultStackSize);
        DWORD ring0StackTop = ((DWORD)m_kernelStack + defaultStackSize) & 0xffffff8;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = ring0StackTop;
    }

    // HACK: Ring2 SS in the TSS is the current PID.
    m_tss.ss2 = m_pid;
    m_farPtr.offset = 0x98765432;
}

Task::~Task()
{
    InterruptDisabler disabler;
    system.nprocess--;
    delete [] m_ldtEntries;
    m_ldtEntries = nullptr;

    if (m_kernelStack) {
        kfree(m_kernelStack);
        m_kernelStack = nullptr;
    }
}

void Task::dumpRegions()
{
    kprintf("Task %s(%u) regions:\n", name().characters(), pid());
    kprintf("BEGIN       END         SIZE        NAME\n");
    for (auto& region : m_regions) {
        kprintf("%x -- %x    %x    %s\n",
            region->linearAddress.get(),
            region->linearAddress.offset(region->size - 1).get(),
            region->size,
            region->name.characters());
    }
}

void Task::sys$exit(int status)
{
    cli();
#ifdef TASK_DEBUG
    kprintf("sys$exit: %s(%u) exit with status %d\n", name().characters(), pid(), status);
#endif

    setState(Exiting);

    MemoryManager::the().unmapRegionsForTask(*this);

    s_tasks->remove(this);

    if (!scheduleNewTask()) {
        kprintf("Task::taskDidCrash: Failed to schedule a new task :(\n");
        HANG;
    }

    s_deadTasks->append(this);

    switchNow();
}

void Task::taskDidCrash(Task* crashedTask)
{
    ASSERT_INTERRUPTS_DISABLED();

    crashedTask->setState(Crashing);
    crashedTask->dumpRegions();

    s_tasks->remove(crashedTask);

    MemoryManager::the().unmapRegionsForTask(*crashedTask);

    if (!scheduleNewTask()) {
        kprintf("Task::taskDidCrash: Failed to schedule a new task :(\n");
        HANG;
    }

    s_deadTasks->append(crashedTask);

    switchNow();
}

void Task::doHouseKeeping()
{
    InterruptDisabler disabler;
    if (s_deadTasks->isEmpty())
        return;
    Task* next = nullptr;
    for (auto* deadTask = s_deadTasks->head(); deadTask; deadTask = next) {
        next = deadTask->next();
        delete deadTask;
    }
    s_deadTasks->clear();
}

void yield()
{
    if (!current) {
        kprintf( "PANIC: yield() with !current" );
        HANG;
    }

    //kprintf("%s<%u> yield()\n", current->name().characters(), current->pid());

    InterruptDisabler disabler;
    if (!scheduleNewTask())
        return;

    //kprintf("yield() jumping to new task: %x (%s)\n", current->farPtr().selector, current->name().characters());
    switchNow();
}

void switchNow()
{
    Descriptor& descriptor = getGDTEntry(current->selector());
    descriptor.type = 9;
    flushGDT();
    asm("sti\n"
        "ljmp *(%%eax)\n"
        ::"a"(&current->farPtr())
    );
}

bool scheduleNewTask()
{
    ASSERT_INTERRUPTS_DISABLED();

    if (!current) {
        // XXX: The first ever context_switch() goes to the idle task.
        //      This to setup a reliable place we can return to.
        return contextSwitch(Task::kernelTask());
    }

    // Check and unblock tasks whose wait conditions have been met.
    for (auto* task = s_tasks->head(); task; task = task->next()) {
        if (task->state() == Task::BlockedSleep) {
            if (task->wakeupTime() <= system.uptime) {
                task->unblock();
                continue;
            }
        }

        if (task->state() == Task::BlockedWait) {
            if (!Task::fromPID(task->waitee())) {
                task->unblock();
                continue;
            }
        }

        if (task->state() == Task::BlockedRead) {
            ASSERT(task->m_fdBlockedOnRead != -1);
            if (task->m_fileHandles[task->m_fdBlockedOnRead]->hasDataAvailableForRead()) {
                task->unblock();
                continue;
            }
        }
    }

#if 0
    kprintf("Scheduler choices:\n");
    for (auto* task = s_tasks->head(); task; task = task->next()) {
        if (task->state() == Task::BlockedWait || task->state() == Task::BlockedSleep)
            continue;
        kprintf("%w %s(%u)\n", task->state(), task->name().characters(), task->pid());
    }
#endif

    auto* prevHead = s_tasks->head();
    for (;;) {
        // Move head to tail.
        s_tasks->append(s_tasks->removeHead());
        auto* task = s_tasks->head();

        if (task->state() == Task::Runnable || task->state() == Task::Running) {
            //kprintf("switch to %s (%p vs %p)\n", task->name().characters(), task, current);
            return contextSwitch(task);
        }

        if (task == prevHead) {
            // Back at task_head, nothing wants to run.
            kprintf("Nothing wants to run!\n");
            kprintf("PID    OWNER      STATE  NSCHED  NAME\n");
            for (auto* task = s_tasks->head(); task; task = task->next()) {
                kprintf("%w   %w:%w  %b     %w    %s\n",
                    task->pid(),
                    task->uid(),
                    task->gid(),
                    task->state(),
                    task->timesScheduled(),
                    task->name().characters());
            }
            kprintf("Switch to kernel task\n");
            return contextSwitch(Task::kernelTask());
        }
    }
}

static bool contextSwitch(Task* t)
{
    //kprintf("c_s to %s (same:%u)\n", t->name().characters(), current == t);
    t->setTicksLeft(5);
    t->didSchedule();

    if (current == t)
        return false;

    // Some sanity checking to force a crash earlier.
    auto csRPL = t->tss().cs & 3;
    auto ssRPL = t->tss().ss & 3;

    if (csRPL != ssRPL) {
        kprintf("Fuckup! Switching from %s(%u) to %s(%u) has RPL mismatch\n",
                current->name().characters(), current->pid(),
                t->name().characters(), t->pid()
                );
        kprintf("code: %w:%x\n", t->tss().cs, t->tss().eip);
        kprintf(" stk: %w:%x\n", t->tss().ss, t->tss().esp);
        ASSERT(csRPL == ssRPL);
    }

    if (current) {
        // If the last task hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (current->state() == Task::Running)
            current->setState(Task::Runnable);

        bool success = MemoryManager::the().unmapRegionsForTask(*current);
        ASSERT(success);
    }

    bool success = MemoryManager::the().mapRegionsForTask(*t);
    ASSERT(success);

    current = t;
    t->setState(Task::Running);

    if (!t->selector())
        t->setSelector(allocateGDTEntry());

    auto& tssDescriptor = getGDTEntry(t->selector());

    tssDescriptor.limit_hi = 0;
    tssDescriptor.limit_lo = 0xFFFF;
    tssDescriptor.base_lo = (DWORD)(&t->tss()) & 0xFFFF;
    tssDescriptor.base_hi = ((DWORD)(&t->tss()) >> 16) & 0xFF;
    tssDescriptor.base_hi2 = ((DWORD)(&t->tss()) >> 24) & 0xFF;
    tssDescriptor.dpl = 0;
    tssDescriptor.segment_present = 1;
    tssDescriptor.granularity = 1;
    tssDescriptor.zero = 0;
    tssDescriptor.operation_size = 1;
    tssDescriptor.descriptor_type = 0;
    tssDescriptor.type = 11; // Busy TSS

    flushGDT();
    return true;
}

Task* Task::fromPID(pid_t pid)
{
    for (auto* task = s_tasks->head(); task; task = task->next()) {
        if (task->pid() == pid)
            return task;
    }
    return nullptr;
}

FileHandle* Task::fileHandleIfExists(int fd)
{
    if (fd < 0)
        return nullptr;
    if ((unsigned)fd < m_fileHandles.size())
        return m_fileHandles[fd].ptr();
    return nullptr;
}

ssize_t Task::sys$get_dir_entries(int fd, void* buffer, size_t size)
{
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -1;
    return handle->get_dir_entries((byte*)buffer, size);
}

int Task::sys$seek(int fd, int offset)
{
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -1;
    return handle->seek(offset, SEEK_SET);
}

ssize_t Task::sys$read(int fd, void* outbuf, size_t nread)
{
    Task::checkSanity("Task::sys$read");
#ifdef DEBUG_IO
    kprintf("Task::sys$read: called(%d, %p, %u)\n", fd, outbuf, nread);
#endif
    auto* handle = fileHandleIfExists(fd);
#ifdef DEBUG_IO
    kprintf("Task::sys$read: handle=%p\n", handle);
#endif
    if (!handle) {
        kprintf("Task::sys$read: handle not found :(\n");
        return -1;
    }
#ifdef DEBUG_IO
    kprintf("call read on handle=%p\n", handle);
#endif
    if (handle->isBlocking()) {
        if (!handle->hasDataAvailableForRead()) {
            m_fdBlockedOnRead = fd;
            block(BlockedRead);
            yield();
        }
    }
    nread = handle->read((byte*)outbuf, nread);
#ifdef DEBUG_IO
    kprintf("Task::sys$read: nread=%u\n", nread);
#endif
    return nread;
}

int Task::sys$close(int fd)
{
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -1;
    // FIXME: Implement.
    return 0;
}

int Task::sys$lstat(const char* path, void* statbuf)
{
    auto handle = VirtualFileSystem::the().open(move(path), m_cwd.ptr());
    if (!handle)
        return -1;
    handle->stat((Unix::stat*)statbuf);
    return 0;
}

int Task::sys$chdir(const char* path)
{
    auto handle = VirtualFileSystem::the().open(path, m_cwd.ptr());
    if (!handle)
        return -ENOENT; // FIXME: More detailed error.
    if (!handle->isDirectory())
        return -ENOTDIR;
    m_cwd = handle->vnode();
    kprintf("m_cwd <- %p (%u)\n", m_cwd.ptr(), handle->vnode()->inode.index());
    return 0;
}

int Task::sys$getcwd(char* buffer, size_t size)
{
    // FIXME: Implement!
    return -ENOTIMPL;
}

int Task::sys$open(const char* path, size_t pathLength)
{
#ifdef DEBUG_IO
    kprintf("Task::sys$open(): PID=%u, path=%s {%u}\n", m_pid, path, pathLength);
#endif
    if (m_fileHandles.size() >= m_maxFileHandles)
        return -EMFILE;
    auto handle = VirtualFileSystem::the().open(String(path, pathLength), m_cwd.ptr());
    if (!handle)
        return -ENOENT; // FIXME: Detailed error.
    int fd = m_fileHandles.size();
    handle->setFD(fd);
    m_fileHandles.append(move(handle));
    return fd;
}

int Task::sys$kill(pid_t pid, int sig)
{
    (void) sig;
    if (pid == 0) {
        // FIXME: Send to same-group processes.
        ASSERT(pid != 0);
    }
    if (pid == -1) {
        // FIXME: Send to all processes.
        ASSERT(pid != -1);
    }
    ASSERT_NOT_REACHED();
    Task* peer = Task::fromPID(pid);
    if (!peer) {
        // errno = ESRCH;
        return -1;
    }
    return -1;
}

int Task::sys$sleep(unsigned seconds)
{
    if (!seconds)
        return 0;
    sleep(seconds * TICKS_PER_SECOND);
    return 0;
}

int Task::sys$gettimeofday(timeval* tv)
{
    InterruptDisabler disabler;
    auto now = RTC::now();
    tv->tv_sec = now;
    tv->tv_usec = 0;
    return 0;
}

uid_t Task::sys$getuid()
{
    return m_uid;
}

gid_t Task::sys$getgid()
{
    return m_gid;
}

pid_t Task::sys$getpid()
{
    return m_pid;
}

pid_t Task::sys$waitpid(pid_t waitee)
{
    InterruptDisabler disabler;
    if (!Task::fromPID(waitee))
        return -1;
    m_waitee = waitee;
    block(BlockedWait);
    yield();
    return m_waitee;
}

void Task::unblock()
{
    ASSERT(m_state != Task::Runnable && m_state != Task::Running);
    system.nblocked--;
    m_state = Task::Runnable;
}

void Task::block(Task::State state)
{
    ASSERT(current->state() == Task::Running);
    system.nblocked++;
    current->setState(state);
}

void block(Task::State state)
{
    current->block(state);
    yield();
}

void sleep(DWORD ticks)
{
    ASSERT(current->state() == Task::Running);
    current->setWakeupTime(system.uptime + ticks);
    current->block(Task::BlockedSleep);
    yield();
}

Task* Task::kernelTask()
{
    ASSERT(s_kernelTask);
    return s_kernelTask;
}

Task::Region::Region(LinearAddress a, size_t s, RetainPtr<Zone>&& z, String&& n)
    : linearAddress(a)
    , size(s)
    , zone(move(z))
    , name(move(n))
{
}

Task::Region::~Region()
{
}
