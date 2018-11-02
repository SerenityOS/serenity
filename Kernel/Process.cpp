#include "types.h"
#include "Process.h"
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
#include "ProcFileSystem.h"
#include <AK/StdLib.h>

//#define DEBUG_IO
//#define TASK_DEBUG
//#define SCHEDULER_DEBUG

// FIXME: Only do a single validation for accesses that don't span multiple pages.
// FIXME: Some places pass strlen(arg1) as arg2. This doesn't seem entirely perfect..
#define VALIDATE_USER_READ(b, s) \
    do { \
        LinearAddress laddr((dword)(b)); \
        if (!validate_user_read(laddr) || !validate_user_read(laddr.offset((s) - 1))) \
            return -EFAULT; \
    } while(0)

#define VALIDATE_USER_WRITE(b, s) \
    do { \
        LinearAddress laddr((dword)(b)); \
        if (!validate_user_write(laddr) || !validate_user_write(laddr.offset((s) - 1))) \
            return -EFAULT; \
    } while(0)

static const DWORD defaultStackSize = 16384;

Process* current;
Process* s_kernelProcess;

static pid_t next_pid;
static InlineLinkedList<Process>* s_processes;
static InlineLinkedList<Process>* s_deadProcesses;
static String* s_hostname;

static String& hostnameStorage(InterruptDisabler&)
{
    ASSERT(s_hostname);
    return *s_hostname;
}

static String getHostname()
{
    InterruptDisabler disabler;
    return hostnameStorage(disabler).isolatedCopy();
}

static bool contextSwitch(Process*);

static void redoKernelProcessTSS()
{
    if (!s_kernelProcess->selector())
        s_kernelProcess->setSelector(gdt_alloc_entry());

    auto& tssDescriptor = getGDTEntry(s_kernelProcess->selector());

    tssDescriptor.setBase(&s_kernelProcess->tss());
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

void Process::prepForIRETToNewProcess()
{
    redoKernelProcessTSS();
    s_kernelProcess->tss().backlink = current->selector();
    loadTaskRegister(s_kernelProcess->selector());
}

static void hlt_loop()
{
    for (;;) {
        asm volatile("hlt");
    }
}

void Process::initialize()
{
    current = nullptr;
    next_pid = 0;
    s_processes = new InlineLinkedList<Process>;
    s_deadProcesses = new InlineLinkedList<Process>;
    s_kernelProcess = Process::createKernelProcess(hlt_loop, "colonel");
    s_hostname = new String("birx");
    redoKernelProcessTSS();
    loadTaskRegister(s_kernelProcess->selector());
}

void Process::allocateLDT()
{
    ASSERT(!m_tss.ldt);
    static const WORD numLDTEntries = 4;
    m_ldt_selector = gdt_alloc_entry();
    m_ldtEntries = new Descriptor[numLDTEntries];
#if 0
    kprintf("new ldt selector = %x\n", m_ldt_selector);
    kprintf("new ldt table at = %p\n", m_ldtEntries);
    kprintf("new ldt table size = %u\n", (numLDTEntries * 8) - 1);
#endif
    Descriptor& ldt = getGDTEntry(m_ldt_selector);
    ldt.setBase(m_ldtEntries);
    ldt.setLimit(numLDTEntries * 8 - 1);
    ldt.dpl = 0;
    ldt.segment_present = 1;
    ldt.granularity = 0;
    ldt.zero = 0;
    ldt.operation_size = 1;
    ldt.descriptor_type = 0;
    ldt.type = Descriptor::LDT;
    m_tss.ldt = m_ldt_selector;
}

template<typename Callback>
static void forEachProcess(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = s_processes->head(); process; process = process->next()) {
        if (!callback(*process))
            break;
    }
}

Vector<Process*> Process::allProcesses()
{
    InterruptDisabler disabler;
    Vector<Process*> processes;
    processes.ensureCapacity(s_processes->sizeSlow());
    for (auto* process = s_processes->head(); process; process = process->next())
        processes.append(process);
    return processes;
}

Region* Process::allocateRegion(size_t size, String&& name)
{
    // FIXME: This needs sanity checks. What if this overlaps existing regions?

    auto zone = MM.createZone(size);
    ASSERT(zone);
    m_regions.append(adopt(*new Region(m_nextRegion, size, move(zone), move(name))));
    m_nextRegion = m_nextRegion.offset(size).offset(16384);
    MM.mapRegion(*this, *m_regions.last());
    return m_regions.last().ptr();
}

bool Process::deallocateRegion(Region& region)
{
    InterruptDisabler disabler;
    for (size_t i = 0; i < m_regions.size(); ++i) {
        if (m_regions[i].ptr() == &region) {
            MM.unmapRegion(*this, region);
            m_regions.remove(i);
            return true;
        }
    }
    return false;
}

Region* Process::regionFromRange(LinearAddress laddr, size_t size)
{
    for (auto& region : m_regions) {
        if (region->linearAddress == laddr && region->size == size)
            return region.ptr();
    }
    return nullptr;
}

int Process::sys$set_mmap_name(void* addr, size_t size, const char* name)
{
    VALIDATE_USER_READ(name, strlen(name));
    auto* region = regionFromRange(LinearAddress((dword)addr), size);
    if (!region)
        return -EINVAL;
    region->name = name;
    return 0;
}

void* Process::sys$mmap(void* addr, size_t size)
{
    InterruptDisabler disabler;
    // FIXME: Implement mapping at a client-preferred address.
    ASSERT(addr == nullptr);
    auto* region = allocateRegion(size, "mmap");
    if (!region)
        return (void*)-1;
    MM.mapRegion(*this, *region);
    return (void*)region->linearAddress.get();
}

int Process::sys$munmap(void* addr, size_t size)
{
    InterruptDisabler disabler;
    auto* region = regionFromRange(LinearAddress((dword)addr), size);
    if (!region)
        return -1;
    if (!deallocateRegion(*region))
        return -1;
    return 0;
}

int Process::sys$gethostname(char* buffer, size_t size)
{
    VALIDATE_USER_WRITE(buffer, size);
    auto hostname = getHostname();
    if (size < (hostname.length() + 1))
        return -ENAMETOOLONG;
    memcpy(buffer, hostname.characters(), size);
    return 0;
}

int Process::sys$spawn(const char* path, const char** args)
{
    if (args) {
        for (size_t i = 0; args[i]; ++i) {
            VALIDATE_USER_READ(args[i], strlen(args[i]));
        }
    }

    int error = 0;
    auto* child = Process::createUserProcess(path, m_uid, m_gid, m_pid, error, args, m_tty);
    if (child)
        return child->pid();
    return error;
}

Process* Process::createUserProcess(const String& path, uid_t uid, gid_t gid, pid_t parentPID, int& error, const char** args, TTY* tty)
{
    auto parts = path.split('/');
    if (parts.isEmpty()) {
        error = -ENOENT;
        return nullptr;
    }

    RetainPtr<VirtualFileSystem::Node> cwd;
    {
        InterruptDisabler disabler;
        if (auto* parentProcess = Process::fromPID(parentPID))
            cwd = parentProcess->m_cwd.copyRef();
        if (!cwd)
            cwd = VirtualFileSystem::the().root();
    }

    auto handle = VirtualFileSystem::the().open(path, error, 0, cwd ? cwd->inode : InodeIdentifier());
    if (!handle)
        return nullptr;

    if (!handle->metadata().mayExecute(uid, gid)) {
        error = -EACCES;
        return nullptr;
    }

    auto elfData = handle->readEntireFile();
    if (!elfData) {
        error = -EIO; // FIXME: Get a more detailed error from VFS.
        return nullptr;
    }

    Vector<String> processArguments;
    if (args) {
        for (size_t i = 0; args[i]; ++i) {
            processArguments.append(args[i]);
        }
    } else {
        processArguments.append(parts.last());
    }

    Vector<String> processEnvironment;
    processEnvironment.append("PATH=/bin");
    processEnvironment.append("SHELL=/bin/sh");
    processEnvironment.append("TERM=console");
    processEnvironment.append("HOME=/");

    auto* t = new Process(parts.takeLast(), uid, gid, parentPID, Ring3, move(cwd), handle->vnode(), tty);

    t->m_arguments = move(processArguments);
    t->m_initialEnvironment = move(processEnvironment);

    ExecSpace space;
    Region* region = nullptr;

    ProcessPagingScope pagingScope(*t);
    space.hookableAlloc = [&] (const String& name, size_t size) {
        if (!size)
            return (void*)nullptr;
        size = ((size / 4096) + 1) * 4096; // FIXME: Use ceil_div?
        region = t->allocateRegion(size, String(name));
        return (void*)region->linearAddress.get();
    };
    bool success = space.loadELF(move(elfData));
    if (!success) {
        delete t;
        kprintf("Failure loading ELF %s\n", path.characters());
        error = -ENOEXEC;
        return nullptr;
    }

    space.forEachArea([&] (const String& name, dword offset, size_t size, LinearAddress laddr) {
        if (laddr.isNull())
            return;
        dword roundedOffset = offset & 0xfffff000;
        size_t roundedSize = 4096 * ceilDiv((offset - roundedOffset) + size, 4096u);
        LinearAddress roundedLaddr = laddr;
        roundedLaddr.mask(0xfffff000);
        t->m_subregions.append(make<Subregion>(*region, roundedOffset, roundedSize, roundedLaddr, String(name)));
#ifdef SUBREGION_DEBUG
        kprintf("   req subregion %s (offset: %u, size: %u) @ %p\n", name.characters(), offset, size, laddr.get());
        kprintf("actual subregion %s (offset: %u, size: %u) @ %p\n", name.characters(), roundedOffset, roundedSize, roundedLaddr.get());
#endif
        MM.mapSubregion(*t, *t->m_subregions.last());
    });

    t->m_tss.eip = (dword)space.symbolPtr("_start");
    if (!t->m_tss.eip) {
        // FIXME: This is ugly. If we need to do this, it should be at a different level.
        delete t;
        error = -ENOEXEC;
        return nullptr;
    }

    ASSERT(region);

    ProcFileSystem::the().addProcess(*t);

    s_processes->prepend(t);
    system.nprocess++;
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) spawned @ %p\n", t->pid(), t->name().characters(), t->m_tss.eip);
#endif
    error = 0;
    return t;
}

int Process::sys$get_environment(char*** environ)
{
    auto* region = allocateRegion(4096, "environ");
    if (!region)
        return -ENOMEM;
    MM.mapRegion(*this, *region);
    char* envpage = (char*)region->linearAddress.get();
    *environ = (char**)envpage;
    char* bufptr = envpage + (sizeof(char*) * (m_initialEnvironment.size() + 1));
    for (size_t i = 0; i < m_initialEnvironment.size(); ++i) {
        (*environ)[i] = bufptr;
        memcpy(bufptr, m_initialEnvironment[i].characters(), m_initialEnvironment[i].length());
        bufptr += m_initialEnvironment[i].length();
        *(bufptr++) = '\0';
    }
    (*environ)[m_initialEnvironment.size()] = nullptr;
    return 0;
}

int Process::sys$get_arguments(int* argc, char*** argv)
{
    auto* region = allocateRegion(4096, "argv");
    if (!region)
        return -ENOMEM;
    MM.mapRegion(*this, *region);
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

Process* Process::createKernelProcess(void (*e)(), String&& name)
{
    auto* process = new Process(move(name), (uid_t)0, (gid_t)0, (pid_t)0, Ring0);
    process->m_tss.eip = (dword)e;

    if (process->pid() != 0) {
        InterruptDisabler disabler;
        s_processes->prepend(process);
        system.nprocess++;
        ProcFileSystem::the().addProcess(*process);
#ifdef TASK_DEBUG
        kprintf("Kernel process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), process->m_tss.eip);
#endif
    }

    return process;
}

Process::Process(String&& name, uid_t uid, gid_t gid, pid_t parentPID, RingLevel ring, RetainPtr<VirtualFileSystem::Node>&& cwd, RetainPtr<VirtualFileSystem::Node>&& executable, TTY* tty)
    : m_name(move(name))
    , m_pid(next_pid++)
    , m_uid(uid)
    , m_gid(gid)
    , m_state(Runnable)
    , m_ring(ring)
    , m_cwd(move(cwd))
    , m_executable(move(executable))
    , m_tty(tty)
    , m_parentPID(parentPID)
{
    {
        // FIXME: Use a ProcessHandle? Presumably we're executing *IN* the parent right now though..
        InterruptDisabler disabler;
        if (auto* parent = Process::fromPID(m_parentPID)) {
            m_sid = parent->m_sid;
            m_pgid = parent->m_pgid;
        }

    }

    m_page_directory = (PageDirectory*)kmalloc_page_aligned(sizeof(PageDirectory));
    MM.populate_page_directory(*this);

    m_file_descriptors.resize(m_max_open_file_descriptors);

    if (tty) {
        m_file_descriptors[0] = tty->open(O_RDONLY);
        m_file_descriptors[1] = tty->open(O_WRONLY);
        m_file_descriptors[2] = tty->open(O_WRONLY);
    }

    m_nextRegion = LinearAddress(0x10000000);

    memset(&m_tss, 0, sizeof(m_tss));

    if (isRing3()) {
        memset(&m_ldtEntries, 0, sizeof(m_ldtEntries));
        allocateLDT();
    }

    // Only IF is set when a process boots.
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

    m_tss.cr3 = (dword)m_page_directory;

    if (isRing0()) {
        // FIXME: This memory is leaked.
        // But uh, there's also no kernel process termination, so I guess it's not technically leaked...
        dword stackBottom = (dword)kmalloc_eternal(defaultStackSize);
        m_stackTop0 = (stackBottom + defaultStackSize) & 0xffffff8;
        m_tss.esp = m_stackTop0;
    } else {
        auto* region = allocateRegion(defaultStackSize, "stack");
        ASSERT(region);
        m_stackTop3 = region->linearAddress.offset(defaultStackSize).get() & 0xfffffff8;
        m_tss.esp = m_stackTop3;
    }

    if (isRing3()) {
        // Ring3 processes need a separate stack for Ring0.
        m_kernelStack = kmalloc(defaultStackSize);
        m_stackTop0 = ((DWORD)m_kernelStack + defaultStackSize) & 0xffffff8;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_stackTop0;
    }

    // HACK: Ring2 SS in the TSS is the current PID.
    m_tss.ss2 = m_pid;
    m_farPtr.offset = 0x98765432;
}

Process::~Process()
{
    InterruptDisabler disabler;
    ProcFileSystem::the().removeProcess(*this);
    system.nprocess--;

    if (isRing3()) {
        delete [] m_ldtEntries;
        m_ldtEntries = nullptr;
        gdt_free_entry(m_ldt_selector);
    }

    gdt_free_entry(selector());

    if (m_kernelStack) {
        kfree(m_kernelStack);
        m_kernelStack = nullptr;
    }

    MM.release_page_directory(*this);
}

void Process::dumpRegions()
{
    kprintf("Process %s(%u) regions:\n", name().characters(), pid());
    kprintf("BEGIN       END         SIZE        NAME\n");
    for (auto& region : m_regions) {
        kprintf("%x -- %x    %x    %s\n",
            region->linearAddress.get(),
            region->linearAddress.offset(region->size - 1).get(),
            region->size,
            region->name.characters());
    }

    kprintf("Process %s(%u) subregions:\n", name().characters(), pid());
    kprintf("REGION    OFFSET    BEGIN       END         SIZE        NAME\n");
    for (auto& subregion : m_subregions) {
        kprintf("%x  %x  %x -- %x    %x    %s\n",
            subregion->region->linearAddress.get(),
            subregion->offset,
            subregion->linearAddress.get(),
            subregion->linearAddress.offset(subregion->size - 1).get(),
            subregion->size,
            subregion->name.characters());
    }
}

void Process::notify_waiters(pid_t waitee, int exit_status, int signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = s_processes->head(); process; process = process->next()) {
        if (process->waitee() == waitee)
            process->m_waiteeStatus = (exit_status << 8) | (signal);
    }
}

void Process::sys$exit(int status)
{
    cli();
#ifdef TASK_DEBUG
    kprintf("sys$exit: %s(%u) exit with status %d\n", name().characters(), pid(), status);
#endif

    set_state(Exiting);

    s_processes->remove(this);

    notify_waiters(m_pid, status, 0);

    if (!scheduleNewProcess()) {
        kprintf("Process::sys$exit: Failed to schedule a new process :(\n");
        HANG;
    }

    s_deadProcesses->append(this);

    switchNow();
}

void Process::murder(int signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    bool wasCurrent = current == this;
    set_state(Exiting);
    s_processes->remove(this);

    notify_waiters(m_pid, 0, signal);

    if (wasCurrent) {
        kprintf("Current process committing suicide!\n");
        if (!scheduleNewProcess()) {
            kprintf("Process::murder: Failed to schedule a new process :(\n");
            HANG;
        }
    }
    s_deadProcesses->append(this);
    if (wasCurrent)
        switchNow();
}

void Process::processDidCrash(Process* crashedProcess)
{
    ASSERT_INTERRUPTS_DISABLED();

    if (crashedProcess->state() == Crashing) {
        kprintf("Double crash :(\n");
        HANG;
    }

    crashedProcess->set_state(Crashing);
    crashedProcess->dumpRegions();

    s_processes->remove(crashedProcess);

    notify_waiters(crashedProcess->m_pid, 0, SIGSEGV);

    if (!scheduleNewProcess()) {
        kprintf("Process::processDidCrash: Failed to schedule a new process :(\n");
        HANG;
    }

    s_deadProcesses->append(crashedProcess);

    switchNow();
}

void Process::doHouseKeeping()
{
    if (s_deadProcesses->isEmpty())
        return;
    InterruptDisabler disabler;
    Process* next = nullptr;
    for (auto* deadProcess = s_deadProcesses->head(); deadProcess; deadProcess = next) {
        next = deadProcess->next();
        delete deadProcess;
    }
    s_deadProcesses->clear();
}

void yield()
{
    if (!current) {
        kprintf( "PANIC: yield() with !current" );
        HANG;
    }

    //kprintf("%s<%u> yield()\n", current->name().characters(), current->pid());

    InterruptDisabler disabler;
    if (!scheduleNewProcess())
        return;

    //kprintf("yield() jumping to new process: %x (%s)\n", current->farPtr().selector, current->name().characters());
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

bool scheduleNewProcess()
{
    ASSERT_INTERRUPTS_DISABLED();

    if (!current) {
        // XXX: The first ever context_switch() goes to the idle process.
        //      This to setup a reliable place we can return to.
        return contextSwitch(Process::kernelProcess());
    }

    // Check and unblock processes whose wait conditions have been met.
    for (auto* process = s_processes->head(); process; process = process->next()) {
        if (process->state() == Process::BlockedSleep) {
            if (process->wakeupTime() <= system.uptime) {
                process->unblock();
                continue;
            }
        }

        if (process->state() == Process::BlockedWait) {
            if (!Process::fromPID(process->waitee())) {
                process->unblock();
                continue;
            }
        }

        if (process->state() == Process::BlockedRead) {
            ASSERT(process->m_fdBlockedOnRead != -1);
            if (process->m_file_descriptors[process->m_fdBlockedOnRead]->hasDataAvailableForRead()) {
                process->unblock();
                continue;
            }
        }
    }

#ifdef SCHEDULER_DEBUG
    dbgprintf("Scheduler choices:\n");
    for (auto* process = s_processes->head(); process; process = process->next()) {
        //if (process->state() == Process::BlockedWait || process->state() == Process::BlockedSleep)
//            continue;
        dbgprintf("%w %s(%u)\n", process->state(), process->name().characters(), process->pid());
    }
#endif

    auto* prevHead = s_processes->head();
    for (;;) {
        // Move head to tail.
        s_processes->append(s_processes->removeHead());
        auto* process = s_processes->head();

        if (process->state() == Process::Runnable || process->state() == Process::Running) {
#ifdef SCHEDULER_DEBUG
            dbgprintf("switch to %s(%u) (%p vs %p)\n", process->name().characters(), process->pid(), process, current);
#endif
            return contextSwitch(process);
        }

        if (process == prevHead) {
            // Back at process_head, nothing wants to run.
            kprintf("Nothing wants to run!\n");
            kprintf("PID    OWNER      STATE  NSCHED  NAME\n");
            for (auto* process = s_processes->head(); process; process = process->next()) {
                kprintf("%w   %w:%w  %b     %w    %s\n",
                    process->pid(),
                    process->uid(),
                    process->gid(),
                    process->state(),
                    process->timesScheduled(),
                    process->name().characters());
            }
            kprintf("Switch to kernel process @ %w:%x\n", s_kernelProcess->tss().cs, s_kernelProcess->tss().eip);
            return contextSwitch(Process::kernelProcess());
        }
    }
}

static bool contextSwitch(Process* t)
{
    t->setTicksLeft(5);
    t->didSchedule();

    if (current == t)
        return false;

#ifdef SCHEDULER_DEBUG
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
#endif

    if (current) {
        // If the last process hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (current->state() == Process::Running)
            current->set_state(Process::Runnable);
    }

    current = t;
    t->set_state(Process::Running);

    if (!t->selector()) {
        t->setSelector(gdt_alloc_entry());
        auto& descriptor = getGDTEntry(t->selector());
        descriptor.setBase(&t->tss());
        descriptor.setLimit(0xffff);
        descriptor.dpl = 0;
        descriptor.segment_present = 1;
        descriptor.granularity = 1;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 0;
    }

    auto& descriptor = getGDTEntry(t->selector());
    descriptor.type = 11; // Busy TSS
    flushGDT();
    return true;
}

Process* Process::fromPID(pid_t pid)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = s_processes->head(); process; process = process->next()) {
        if (process->pid() == pid)
            return process;
    }
    return nullptr;
}

FileHandle* Process::fileHandleIfExists(int fd)
{
    if (fd < 0)
        return nullptr;
    if ((unsigned)fd < m_file_descriptors.size())
        return m_file_descriptors[fd].ptr();
    return nullptr;
}

ssize_t Process::sys$get_dir_entries(int fd, void* buffer, size_t size)
{
    VALIDATE_USER_WRITE(buffer, size);
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -EBADF;
    return handle->get_dir_entries((byte*)buffer, size);
}

int Process::sys$lseek(int fd, off_t offset, int whence)
{
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -EBADF;
    return handle->seek(offset, whence);
}

int Process::sys$ttyname_r(int fd, char* buffer, size_t size)
{
    VALIDATE_USER_WRITE(buffer, size);
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -EBADF;
    if (!handle->isTTY())
        return -ENOTTY;
    auto ttyName = handle->tty()->ttyName();
    if (size < ttyName.length() + 1)
        return -ERANGE;
    strcpy(buffer, ttyName.characters());
    return 0;
}

ssize_t Process::sys$write(int fd, const void* data, size_t size)
{
    VALIDATE_USER_READ(data, size);
#ifdef DEBUG_IO
    kprintf("Process::sys$write: called(%d, %p, %u)\n", fd, data, size);
#endif
    auto* handle = fileHandleIfExists(fd);
#ifdef DEBUG_IO
    kprintf("Process::sys$write: handle=%p\n", handle);
#endif
    if (!handle)
        return -EBADF;
    auto nwritten = handle->write((const byte*)data, size);
#ifdef DEBUG_IO
    kprintf("Process::sys$write: nwritten=%u\n", nwritten);
#endif
    return nwritten;
}

ssize_t Process::sys$read(int fd, void* outbuf, size_t nread)
{
    VALIDATE_USER_WRITE(outbuf, nread);
#ifdef DEBUG_IO
    kprintf("Process::sys$read: called(%d, %p, %u)\n", fd, outbuf, nread);
#endif
    auto* handle = fileHandleIfExists(fd);
#ifdef DEBUG_IO
    kprintf("Process::sys$read: handle=%p\n", handle);
#endif
    if (!handle)
        return -EBADF;
    if (handle->isBlocking()) {
        if (!handle->hasDataAvailableForRead()) {
            m_fdBlockedOnRead = fd;
            block(BlockedRead);
            yield();
        }
    }
    nread = handle->read((byte*)outbuf, nread);
#ifdef DEBUG_IO
    kprintf("Process::sys$read: nread=%u\n", nread);
#endif
    return nread;
}

int Process::sys$close(int fd)
{
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -EBADF;
    int rc = handle->close();
    m_file_descriptors[fd] = nullptr;
    return rc;
}

int Process::sys$lstat(const char* path, Unix::stat* statbuf)
{
    VALIDATE_USER_WRITE(statbuf, sizeof(Unix::stat));
    int error;
    auto handle = VirtualFileSystem::the().open(move(path), error, O_NOFOLLOW_NOERROR, cwdInode());
    if (!handle)
        return error;
    handle->stat(statbuf);
    return 0;
}

int Process::sys$stat(const char* path, Unix::stat* statbuf)
{
    VALIDATE_USER_WRITE(statbuf, sizeof(Unix::stat));
    int error;
    auto handle = VirtualFileSystem::the().open(move(path), error, 0, cwdInode());
    if (!handle)
        return error;
    handle->stat(statbuf);
    return 0;
}

int Process::sys$readlink(const char* path, char* buffer, size_t size)
{
    VALIDATE_USER_READ(path, strlen(path));
    VALIDATE_USER_WRITE(buffer, size);

    int error;
    auto handle = VirtualFileSystem::the().open(path, error, O_RDONLY | O_NOFOLLOW_NOERROR, cwdInode());
    if (!handle)
        return error;

    if (!handle->metadata().isSymbolicLink())
        return -EINVAL;

    auto contents = handle->readEntireFile();
    if (!contents)
        return -EIO; // FIXME: Get a more detailed error from VFS.

    memcpy(buffer, contents.pointer(), min(size, contents.size()));
    if (contents.size() + 1 < size)
        buffer[contents.size()] = '\0';
    return 0;
}

int Process::sys$chdir(const char* path)
{
    VALIDATE_USER_READ(path, strlen(path));
    int error;
    auto handle = VirtualFileSystem::the().open(path, error, 0, cwdInode());
    if (!handle)
        return error;
    if (!handle->isDirectory())
        return -ENOTDIR;
    m_cwd = handle->vnode();
    return 0;
}

int Process::sys$getcwd(char* buffer, size_t size)
{
    VALIDATE_USER_WRITE(buffer, size);
    auto path = VirtualFileSystem::the().absolutePath(cwdInode());
    if (path.isNull())
        return -EINVAL;
    if (size < path.length() + 1)
        return -ERANGE;
    strcpy(buffer, path.characters());
    return -ENOTIMPL;
}

size_t Process::number_of_open_file_descriptors() const
{
    size_t count = 0;
    for (auto& handle : m_file_descriptors) {
        if (handle)
            ++count;
    }
    return count;
}

int Process::sys$open(const char* path, int options)
{
#ifdef DEBUG_IO
    kprintf("Process::sys$open(): PID=%u, path=%s {%u}\n", m_pid, path, pathLength);
#endif
    VALIDATE_USER_READ(path, strlen(path));
    if (number_of_open_file_descriptors() >= m_max_open_file_descriptors)
        return -EMFILE;
    int error;
    auto handle = VirtualFileSystem::the().open(path, error, options, cwdInode());
    if (!handle)
        return error;
    if (options & O_DIRECTORY && !handle->isDirectory())
        return -ENOTDIR; // FIXME: This should be handled by VFS::open.

    int fd = 0;
    for (; fd < m_max_open_file_descriptors; ++fd) {
        if (!m_file_descriptors[fd])
            break;
    }
    handle->setFD(fd);
    m_file_descriptors[fd] = move(handle);
    return fd;
}

int Process::sys$uname(utsname* buf)
{
    VALIDATE_USER_WRITE(buf, sizeof(utsname));
    strcpy(buf->sysname, "Serenity");
    strcpy(buf->release, "1.0-dev");
    strcpy(buf->version, "FIXME");
    strcpy(buf->machine, "i386");
    strcpy(buf->nodename, getHostname().characters());
    return 0;
}

int Process::sys$kill(pid_t pid, int sig)
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
    ASSERT(pid != current->pid()); // FIXME: Support this scenario.
    InterruptDisabler disabler;
    auto* peer = Process::fromPID(pid);
    if (!peer)
        return -ESRCH;
    if (sig == SIGKILL) {
        peer->murder(SIGKILL);
        return 0;
    } else {
        ASSERT_NOT_REACHED();
    }
    return -1;
}

int Process::sys$sleep(unsigned seconds)
{
    if (!seconds)
        return 0;
    sleep(seconds * TICKS_PER_SECOND);
    return 0;
}

int Process::sys$gettimeofday(timeval* tv)
{
    VALIDATE_USER_WRITE(tv, sizeof(tv));
    InterruptDisabler disabler;
    auto now = RTC::now();
    tv->tv_sec = now;
    tv->tv_usec = 0;
    return 0;
}

uid_t Process::sys$getuid()
{
    return m_uid;
}

gid_t Process::sys$getgid()
{
    return m_gid;
}

pid_t Process::sys$getpid()
{
    return m_pid;
}

pid_t Process::sys$waitpid(pid_t waitee, int* wstatus, int options)
{
    if (wstatus)
        VALIDATE_USER_WRITE(wstatus, sizeof(int));

    InterruptDisabler disabler;
    if (!Process::fromPID(waitee))
        return -1;
    m_waitee = waitee;
    m_waiteeStatus = 0;
    block(BlockedWait);
    yield();
    if (wstatus)
        *wstatus = m_waiteeStatus;
    return m_waitee;
}

void Process::unblock()
{
    ASSERT(m_state != Process::Runnable && m_state != Process::Running);
    system.nblocked--;
    m_state = Process::Runnable;
}

void Process::block(Process::State state)
{
    ASSERT(current->state() == Process::Running);
    system.nblocked++;
    current->set_state(state);
}

void block(Process::State state)
{
    current->block(state);
    yield();
}

void sleep(DWORD ticks)
{
    ASSERT(current->state() == Process::Running);
    current->setWakeupTime(system.uptime + ticks);
    current->block(Process::BlockedSleep);
    yield();
}

Process* Process::kernelProcess()
{
    ASSERT(s_kernelProcess);
    return s_kernelProcess;
}

Region::Region(LinearAddress a, size_t s, RetainPtr<Zone>&& z, String&& n)
    : linearAddress(a)
    , size(s)
    , zone(move(z))
    , name(move(n))
{
}

Region::~Region()
{
}

Subregion::Subregion(Region& r, dword o, size_t s, LinearAddress l, String&& n)\
    : region(r)
    , offset(o)
    , size(s)
    , linearAddress(l)
    , name(move(n))
{
}

Subregion::~Subregion()
{
}

bool Process::isValidAddressForKernel(LinearAddress laddr) const
{
    // We check extra carefully here since the first 4MB of the address space is identity-mapped.
    // This code allows access outside of the known used address ranges to get caught.

    InterruptDisabler disabler;
    if (laddr.get() >= ksyms().first().address && laddr.get() <= ksyms().last().address)
        return true;
    if (is_kmalloc_address((void*)laddr.get()))
        return true;
    return validate_user_read(laddr);
}

bool Process::validate_user_read(LinearAddress laddr) const
{
    InterruptDisabler disabler;
    return MM.validate_user_read(*this, laddr);
}

bool Process::validate_user_write(LinearAddress laddr) const
{
    InterruptDisabler disabler;
    return MM.validate_user_write(*this, laddr);
}

pid_t Process::sys$getsid(pid_t pid)
{
    if (pid == 0)
        return m_sid;
    InterruptDisabler disabler;
    auto* process = Process::fromPID(pid);
    if (!process)
        return -ESRCH;
    if (m_sid != process->m_sid)
        return -EPERM;
    return process->m_sid;
}

pid_t Process::sys$setsid()
{
    InterruptDisabler disabler;
    bool found_process_with_same_pgid_as_my_pid = false;
    forEachProcess([&] (auto& process) {
        if (process.pgid() == pid()) {
            found_process_with_same_pgid_as_my_pid = true;
            return false;
        }
        return true;
    });
    if (found_process_with_same_pgid_as_my_pid)
        return -EPERM;
    m_sid = m_pid;
    m_pgid = m_pid;
    return m_sid;
}

pid_t Process::sys$getpgid(pid_t pid)
{
    if (pid == 0)
        return m_pgid;
    InterruptDisabler disabler; // FIXME: Use a ProcessHandle
    auto* process = Process::fromPID(pid);
    if (!process)
        return -ESRCH;
    return process->m_pgid;
}

pid_t Process::sys$getpgrp()
{
    return m_pgid;
}

static pid_t get_sid_from_pgid(pid_t pgid)
{
    InterruptDisabler disabler;
    auto* group_leader = Process::fromPID(pgid);
    if (!group_leader)
        return -1;
    return group_leader->sid();
}

int Process::sys$setpgid(pid_t specified_pid, pid_t specified_pgid)
{
    InterruptDisabler disabler; // FIXME: Use a ProcessHandle
    pid_t pid = specified_pid ? specified_pid : m_pid;
    if (specified_pgid < 0)
        return -EINVAL;
    auto* process = Process::fromPID(pid);
    if (!process)
        return -ESRCH;
    pid_t new_pgid = specified_pgid ? specified_pgid : process->m_pid;
    pid_t current_sid = get_sid_from_pgid(process->m_pgid);
    pid_t new_sid = get_sid_from_pgid(new_pgid);
    if (current_sid != new_sid) {
        // Can't move a process between sessions.
        return -EPERM;
    }
    // FIXME: There are more EPERM conditions to check for here..
    process->m_pgid = new_pgid;
    return 0;
}

pid_t Process::sys$tcgetpgrp(int fd)
{
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -EBADF;
    if (!handle->isTTY())
        return -ENOTTY;
    auto& tty = *handle->tty();
    if (&tty != m_tty)
        return -ENOTTY;
    return tty.pgid();
}

int Process::sys$tcsetpgrp(int fd, pid_t pgid)
{
    if (pgid < 0)
        return -EINVAL;
    if (get_sid_from_pgid(pgid) != m_sid)
        return -EINVAL;
    auto* handle = fileHandleIfExists(fd);
    if (!handle)
        return -EBADF;
    if (!handle->isTTY())
        return -ENOTTY;
    auto& tty = *handle->tty();
    if (&tty != m_tty)
        return -ENOTTY;
    tty.set_pgid(pgid);
    return 0;
}
