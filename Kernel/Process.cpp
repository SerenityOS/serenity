#include "types.h"
#include "Process.h"
#include "kmalloc.h"
#include "VGA.h"
#include "StdLib.h"
#include "i386.h"
#include "system.h"
#include <VirtualFileSystem/FileDescriptor.h>
#include <VirtualFileSystem/VirtualFileSystem.h>
#include <ELFLoader/ELFLoader.h>
#include "MemoryManager.h"
#include "errno.h"
#include "i8253.h"
#include "RTC.h"
#include "ProcFileSystem.h"
#include <AK/StdLib.h>
#include <LibC/signal_numbers.h>
#include "Syscall.h"
#include "Scheduler.h"

//#define DEBUG_IO
//#define TASK_DEBUG
//#define FORK_DEBUG
#define SIGNAL_DEBUG
#define MAX_PROCESS_GIDS 32

// FIXME: Only do a single validation for accesses that don't span multiple pages.
// FIXME: Some places pass strlen(arg1) as arg2. This doesn't seem entirely perfect..
#define VALIDATE_USER_READ_WITH_RETURN_TYPE(b, s, ret_type) \
    do { \
        LinearAddress laddr((dword)(b)); \
        if (!validate_user_read(laddr) || !validate_user_read(laddr.offset((s) - 1))) { \
            dbgprintf("Bad read address passed to syscall: %p +%u\n", laddr.get(), (s)); \
            return (ret_type)-EFAULT; \
        } \
    } while(0)

#define VALIDATE_USER_READ(b, s) VALIDATE_USER_READ_WITH_RETURN_TYPE(b, s, int)

#define VALIDATE_USER_WRITE(b, s) \
    do { \
        LinearAddress laddr((dword)(b)); \
        if (!validate_user_write(laddr) || !validate_user_write(laddr.offset((s) - 1))) { \
            dbgprintf("Bad write address passed to syscall: %p +%u\n", laddr.get(), (s)); \
            return -EFAULT; \
        } \
    } while(0)

static const DWORD defaultStackSize = 16384;

static pid_t next_pid;
InlineLinkedList<Process>* g_processes;
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

CoolGlobals* g_cool_globals;

void Process::initialize()
{
#ifdef COOL_GLOBALS
    g_cool_globals = reinterpret_cast<CoolGlobals*>(0x1000);
#endif
    next_pid = 0;
    g_processes = new InlineLinkedList<Process>;
    s_hostname = new String("birx");
    Scheduler::initialize();
}

Vector<Process*> Process::allProcesses()
{
    InterruptDisabler disabler;
    Vector<Process*> processes;
    processes.ensureCapacity(g_processes->sizeSlow());
    for (auto* process = g_processes->head(); process; process = process->next())
        processes.append(process);
    return processes;
}

Region* Process::allocate_region(LinearAddress laddr, size_t size, String&& name, bool is_readable, bool is_writable)
{
    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_nextRegion;
        m_nextRegion = m_nextRegion.offset(size).offset(PAGE_SIZE);
    }

    laddr.mask(0xfffff000);

    unsigned page_count = ceilDiv(size, PAGE_SIZE);
    auto physical_pages = MM.allocate_physical_pages(page_count);
    ASSERT(physical_pages.size() == page_count);
    m_regions.append(adopt(*new Region(laddr, size, move(name), is_readable, is_writable)));
    m_regions.last()->commit(*this);
    MM.mapRegion(*this, *m_regions.last());
    return m_regions.last().ptr();
}

Region* Process::allocate_file_backed_region(LinearAddress laddr, size_t size, RetainPtr<VirtualFileSystem::Node>&& vnode, String&& name, bool is_readable, bool is_writable)
{
    ASSERT(!vnode->isCharacterDevice());

    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_nextRegion;
        m_nextRegion = m_nextRegion.offset(size).offset(PAGE_SIZE);
    }

    laddr.mask(0xfffff000);

    unsigned page_count = ceilDiv(size, PAGE_SIZE);
    Vector<RetainPtr<PhysicalPage>> physical_pages;
    physical_pages.resize(page_count); // Start out with no physical pages!

    m_regions.append(adopt(*new Region(laddr, size, move(vnode), move(name), is_readable, is_writable)));
    MM.mapRegion(*this, *m_regions.last());
    return m_regions.last().ptr();
}

bool Process::deallocate_region(Region& region)
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

void* Process::sys$mmap(const Syscall::SC_mmap_params* params)
{
    VALIDATE_USER_READ_WITH_RETURN_TYPE(params, sizeof(Syscall::SC_mmap_params), void*);
    void* addr = (void*)params->addr;
    size_t size = params->size;
    int prot = params->prot;
    int flags = params->flags;
    int fd = params->fd;
    Unix::off_t offset = params->offset;
    if (size == 0)
        return (void*)-EINVAL;
    if ((dword)addr & ~PAGE_MASK || size & ~PAGE_MASK)
        return (void*)-EINVAL;
    if (flags & MAP_ANONYMOUS) {
        InterruptDisabler disabler;
        // FIXME: Implement mapping at a client-specified address. Most of the support is already in plcae.
        ASSERT(addr == nullptr);
        auto* region = allocate_region(LinearAddress(), size, "mmap", prot & PROT_READ, prot & PROT_WRITE);
        if (!region)
            return (void*)-ENOMEM;
        return region->linearAddress.asPtr();
    }
    if (offset & ~PAGE_MASK)
        return (void*)-EINVAL;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return (void*)-EBADF;
    if (descriptor->vnode()->isCharacterDevice())
        return (void*)-ENODEV;
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    auto region_name = descriptor->absolute_path();
    InterruptDisabler disabler;
    // FIXME: Implement mapping at a client-specified address. Most of the support is already in plcae.
    ASSERT(addr == nullptr);
    auto* region = allocate_file_backed_region(LinearAddress(), size, descriptor->vnode(), move(region_name), prot & PROT_READ, prot & PROT_WRITE);
    if (!region)
        return (void*)-ENOMEM;
    return region->linearAddress.asPtr();
}

int Process::sys$munmap(void* addr, size_t size)
{
    InterruptDisabler disabler;
    auto* region = regionFromRange(LinearAddress((dword)addr), size);
    if (!region)
        return -1;
    if (!deallocate_region(*region))
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

Process* Process::fork(RegisterDump& regs)
{
    auto* child = new Process(String(m_name), m_uid, m_gid, m_pid, m_ring, m_cwd.copyRef(), m_executable.copyRef(), m_tty, this);
#ifdef FORK_DEBUG
    dbgprintf("fork: child=%p\n", child);
#endif

#if 0
    // FIXME: An honest fork() would copy these. Needs a Vector copy ctor.
    child->m_arguments = m_arguments;
    child->m_initialEnvironment = m_initialEnvironment;
#endif

    for (auto& region : m_regions) {
#ifdef FORK_DEBUG
        dbgprintf("fork: cloning Region{%p}\n", region.ptr());
#endif
        auto cloned_region = region->clone();
        child->m_regions.append(move(cloned_region));
        MM.mapRegion(*child, *child->m_regions.last());
    }

    child->m_tss.eax = 0; // fork() returns 0 in the child :^)
    child->m_tss.ebx = regs.ebx;
    child->m_tss.ecx = regs.ecx;
    child->m_tss.edx = regs.edx;
    child->m_tss.ebp = regs.ebp;
    child->m_tss.esp = regs.esp_if_crossRing;
    child->m_tss.esi = regs.esi;
    child->m_tss.edi = regs.edi;
    child->m_tss.eflags = regs.eflags;
    child->m_tss.eip = regs.eip;
    child->m_tss.cs = regs.cs;
    child->m_tss.ds = regs.ds;
    child->m_tss.es = regs.es;
    child->m_tss.fs = regs.fs;
    child->m_tss.gs = regs.gs;
    child->m_tss.ss = regs.ss_if_crossRing;

#ifdef FORK_DEBUG
    dbgprintf("fork: child will begin executing at %w:%x with stack %w:%x\n", child->m_tss.cs, child->m_tss.eip, child->m_tss.ss, child->m_tss.esp);
#endif

    ProcFileSystem::the().addProcess(*child);

    g_processes->prepend(child);
    system.nprocess++;
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) forked from %u @ %p\n", child->pid(), child->name().characters(), m_pid, child->m_tss.eip);
#endif
    return child;
}

pid_t Process::sys$fork(RegisterDump& regs)
{
    auto* child = fork(regs);
    ASSERT(child);
    return child->pid();
}

int Process::exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment)
{
    auto parts = path.split('/');
    if (parts.isEmpty())
        return -ENOENT;

    int error;
    auto descriptor = VirtualFileSystem::the().open(path, error, 0, m_cwd ? m_cwd->inode : InodeIdentifier());
    if (!descriptor) {
        ASSERT(error != 0);
        return error;
    }

    if (!descriptor->metadata().mayExecute(m_euid, m_gids))
        return -EACCES;

    auto elfData = descriptor->readEntireFile();
    if (!elfData)
        return -EIO; // FIXME: Get a more detailed error from VFS.

    dword entry_eip = 0;
    PageDirectory* old_page_directory;
    PageDirectory* new_page_directory;
    {
        InterruptDisabler disabler;
        // Okay, here comes the sleight of hand, pay close attention..
        auto old_regions = move(m_regions);
        old_page_directory = m_page_directory;
        new_page_directory = reinterpret_cast<PageDirectory*>(kmalloc_page_aligned(sizeof(PageDirectory)));
        MM.populate_page_directory(*new_page_directory);
        m_page_directory = new_page_directory;
        MM.enter_process_paging_scope(*this);
        ELFLoader loader(move(elfData));
        loader.alloc_section_hook = [&] (LinearAddress laddr, size_t size, size_t alignment, bool is_readable, bool is_writable, const String& name) {
            ASSERT(size);
            size = ((size / 4096) + 1) * 4096; // FIXME: Use ceil_div?
            (void) allocate_region(laddr, size, String(name), is_readable, is_writable);
            return laddr.asPtr();
        };
        bool success = loader.load();
        if (!success) {
            m_page_directory = old_page_directory;
            MM.enter_process_paging_scope(*this);
            MM.release_page_directory(*new_page_directory);
            m_regions = move(old_regions);
            kprintf("sys$execve: Failure loading %s\n", path.characters());
            return -ENOEXEC;
        }

        entry_eip = (dword)loader.symbol_ptr("_start");
        if (!entry_eip) {
            m_page_directory = old_page_directory;
            MM.enter_process_paging_scope(*this);
            MM.release_page_directory(*new_page_directory);
            m_regions = move(old_regions);
            return -ENOEXEC;
        }
    }

    InterruptDisabler disabler;
    Scheduler::prepare_to_modify_tss(*this);

    m_name = parts.takeLast();

    dword old_esp0 = m_tss.esp0;

    memset(&m_tss, 0, sizeof(m_tss));
    m_tss.eflags = 0x0202;
    m_tss.eip = entry_eip;
    m_tss.cs = 0x1b;
    m_tss.ds = 0x23;
    m_tss.es = 0x23;
    m_tss.fs = 0x23;
    m_tss.gs = 0x23;
    m_tss.ss = 0x23;
    m_tss.cr3 = (dword)m_page_directory;
    m_stack_region = allocate_region(LinearAddress(), defaultStackSize, "stack");
    ASSERT(m_stack_region);
    m_stackTop3 = m_stack_region->linearAddress.offset(defaultStackSize).get() & 0xfffffff8;
    m_tss.esp = m_stackTop3;
    m_tss.ss0 = 0x10;
    m_tss.esp0 = old_esp0;
    m_tss.ss2 = m_pid;

    MM.release_page_directory(*old_page_directory);

    m_executable = descriptor->vnode();
    m_arguments = move(arguments);
    m_initialEnvironment = move(environment);

#ifdef TASK_DEBUG
    kprintf("Process %u (%s) exec'd %s @ %p\n", pid(), name().characters(), path.characters(), m_tss.eip);
#endif

    set_state(Skip1SchedulerPass);

    if (current == this) {
        bool success = Scheduler::yield();
        ASSERT(success);
    }

    return 0;
}

int Process::sys$execve(const char* filename, const char** argv, const char** envp)
{
    VALIDATE_USER_READ(filename, strlen(filename));
    if (argv) {
        for (size_t i = 0; argv[i]; ++i) {
            VALIDATE_USER_READ(argv[i], strlen(argv[i]));
        }
    }
    if (envp) {
        for (size_t i = 0; envp[i]; ++i) {
            VALIDATE_USER_READ(envp[i], strlen(envp[i]));
        }
    }

    String path(filename);
    auto parts = path.split('/');

    Vector<String> arguments;
    if (argv) {
        for (size_t i = 0; argv[i]; ++i) {
            arguments.append(argv[i]);
        }
    } else {
        arguments.append(parts.last());
    }

    Vector<String> environment;
    if (envp) {
        for (size_t i = 0; envp[i]; ++i) {
            environment.append(envp[i]);
        }
    }

    int rc = exec(path, move(arguments), move(environment));
    ASSERT(rc < 0); // We should never continue after a successful exec!
    return rc;
}

pid_t Process::sys$spawn(const char* filename, const char** argv, const char** envp)
{
    VALIDATE_USER_READ(filename, strlen(filename));
    if (argv) {
        for (size_t i = 0; argv[i]; ++i) {
            VALIDATE_USER_READ(argv[i], strlen(argv[i]));
        }
    }
    if (envp) {
        for (size_t i = 0; envp[i]; ++i) {
            VALIDATE_USER_READ(envp[i], strlen(envp[i]));
        }
    }

    String path(filename);
    auto parts = path.split('/');

    Vector<String> arguments;
    if (argv) {
        for (size_t i = 0; argv[i]; ++i) {
            arguments.append(argv[i]);
        }
    } else {
        arguments.append(parts.last());
    }

    Vector<String> environment;
    if (envp) {
        for (size_t i = 0; envp[i]; ++i) {
            environment.append(envp[i]);
        }
    }

    int error;
    auto* child = create_user_process(path, m_uid, m_gid, m_pid, error, move(arguments), move(environment), m_tty);
    if (child)
        return child->pid();
    return error;
}

Process* Process::create_user_process(const String& path, uid_t uid, gid_t gid, pid_t parent_pid, int& error, Vector<String>&& arguments, Vector<String>&& environment, TTY* tty)
{
    // FIXME: Don't split() the path twice (sys$spawn also does it...)
    auto parts = path.split('/');
    if (arguments.isEmpty()) {
        arguments.append(parts.last());
    }
    RetainPtr<VirtualFileSystem::Node> cwd;
    {
        InterruptDisabler disabler;
        if (auto* parent = Process::from_pid(parent_pid))
            cwd = parent->m_cwd.copyRef();
    }
    if (!cwd)
        cwd = VirtualFileSystem::the().root();

    auto* process = new Process(parts.takeLast(), uid, gid, parent_pid, Ring3, move(cwd), nullptr, tty);

    error = process->exec(path, move(arguments), move(environment));
    if (error != 0)
        return nullptr;

    ProcFileSystem::the().addProcess(*process);

    g_processes->prepend(process);
    system.nprocess++;
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), process->m_tss.eip);
#endif
    error = 0;
    return process;
}

int Process::sys$get_environment(char*** environ)
{
    auto* region = allocate_region(LinearAddress(), PAGE_SIZE, "environ");
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
    auto* region = allocate_region(LinearAddress(), PAGE_SIZE, "argv");
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

Process* Process::create_kernel_process(void (*e)(), String&& name)
{
    auto* process = new Process(move(name), (uid_t)0, (gid_t)0, (pid_t)0, Ring0);
    process->m_tss.eip = (dword)e;

    if (process->pid() != 0) {
        InterruptDisabler disabler;
        g_processes->prepend(process);
        system.nprocess++;
        ProcFileSystem::the().addProcess(*process);
#ifdef TASK_DEBUG
        kprintf("Kernel process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), process->m_tss.eip);
#endif
    }

    return process;
}

Process::Process(String&& name, uid_t uid, gid_t gid, pid_t ppid, RingLevel ring, RetainPtr<VirtualFileSystem::Node>&& cwd, RetainPtr<VirtualFileSystem::Node>&& executable, TTY* tty, Process* fork_parent)
    : m_name(move(name))
    , m_pid(next_pid++) // FIXME: RACE: This variable looks racy!
    , m_uid(uid)
    , m_gid(gid)
    , m_euid(uid)
    , m_egid(gid)
    , m_state(Runnable)
    , m_ring(ring)
    , m_cwd(move(cwd))
    , m_executable(move(executable))
    , m_tty(tty)
    , m_ppid(ppid)
{
    m_gids.set(m_gid);

    if (fork_parent) {
        m_sid = fork_parent->m_sid;
        m_pgid = fork_parent->m_pgid;
    } else {
        // FIXME: Use a ProcessHandle? Presumably we're executing *IN* the parent right now though..
        InterruptDisabler disabler;
        if (auto* parent = Process::from_pid(m_ppid)) {
            m_sid = parent->m_sid;
            m_pgid = parent->m_pgid;
        }
    }

    m_page_directory = (PageDirectory*)kmalloc_page_aligned(sizeof(PageDirectory));
    MM.populate_page_directory(*m_page_directory);

    if (fork_parent) {
        m_file_descriptors.resize(fork_parent->m_file_descriptors.size());
        for (size_t i = 0; i < fork_parent->m_file_descriptors.size(); ++i) {
            if (!fork_parent->m_file_descriptors[i])
                continue;
#ifdef FORK_DEBUG
            dbgprintf("fork: cloning fd %u... (%p) istty? %um\n", i, fork_parent->m_file_descriptors[i].ptr(), fork_parent->m_file_descriptors[i]->isTTY());
#endif
            m_file_descriptors[i] = fork_parent->m_file_descriptors[i]->clone();
        }
    } else {
        m_file_descriptors.resize(m_max_open_file_descriptors);
        if (tty) {
            m_file_descriptors[0] = tty->open(O_RDONLY);
            m_file_descriptors[1] = tty->open(O_WRONLY);
            m_file_descriptors[2] = tty->open(O_WRONLY);
        }
    }

    if (fork_parent)
        m_nextRegion = fork_parent->m_nextRegion;
    else
        m_nextRegion = LinearAddress(0x10000000);

    if (fork_parent) {
        memcpy(&m_tss, &fork_parent->m_tss, sizeof(m_tss));
    } else {
        memset(&m_tss, 0, sizeof(m_tss));

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
    }

    m_tss.cr3 = (dword)m_page_directory;

    if (isRing0()) {
        // FIXME: This memory is leaked.
        // But uh, there's also no kernel process termination, so I guess it's not technically leaked...
        dword stackBottom = (dword)kmalloc_eternal(defaultStackSize);
        m_stackTop0 = (stackBottom + defaultStackSize) & 0xffffff8;
        m_tss.esp = m_stackTop0;
    } else {
        if (fork_parent) {
            m_stackTop3 = fork_parent->m_stackTop3;
        } else {
            auto* region = allocate_region(LinearAddress(), defaultStackSize, "stack");
            ASSERT(region);
            m_stackTop3 = region->linearAddress.offset(defaultStackSize).get() & 0xfffffff8;
            m_tss.esp = m_stackTop3;
        }
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

    gdt_free_entry(selector());

    if (m_kernelStack) {
        kfree(m_kernelStack);
        m_kernelStack = nullptr;
    }

    MM.release_page_directory(*m_page_directory);
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
}

void Process::sys$exit(int status)
{
    cli();
#ifdef TASK_DEBUG
    kprintf("sys$exit: %s(%u) exit with status %d\n", name().characters(), pid(), status);
#endif

    set_state(Dead);
    m_termination_status = status;
    m_termination_signal = 0;

    Scheduler::pick_next_and_switch_now();
    ASSERT_NOT_REACHED();
}

void Process::terminate_due_to_signal(byte signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);
    dbgprintf("terminate_due_to_signal %s(%u) <- %u\n", name().characters(), pid(), signal);
    m_termination_status = 0;
    m_termination_signal = signal;
    set_state(Dead);
}

void Process::send_signal(byte signal, Process* sender)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);

    m_pending_signals |= 1 << signal;

    if (sender)
        dbgprintf("signal: %s(%u) sent %d to %s(%u)\n", sender->name().characters(), sender->pid(), signal, name().characters(), pid());
    else
        dbgprintf("signal: kernel sent %d to %s(%u)\n", signal, name().characters(), pid());
}

bool Process::has_unmasked_pending_signals() const
{
    return m_pending_signals & ~m_signal_mask;
}

void Process::dispatch_one_pending_signal()
{
    ASSERT_INTERRUPTS_DISABLED();
    dword signal_candidates = m_pending_signals & ~m_signal_mask;
    ASSERT(signal_candidates);

    byte signal = 0;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << signal)) {
            break;
        }
    }
    dispatch_signal(signal);
}

void Process::dispatch_signal(byte signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);

    dbgprintf("dispatch_signal %s(%u) <- %u\n", name().characters(), pid(), signal);

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    auto handler_laddr = action.handler_or_sigaction;
    if (handler_laddr.is_null()) {
        // FIXME: Is termination really always the appropriate action?
        return terminate_due_to_signal(signal);
    }

    Scheduler::prepare_to_modify_tss(*this);

    word ret_cs = m_tss.cs;
    dword ret_eip = m_tss.eip;
    dword ret_eflags = m_tss.eflags;

    bool interrupting_in_kernel = (ret_cs & 3) == 0;
    if (interrupting_in_kernel) {
        dbgprintf("dispatch_signal to %s(%u) in state=%s with return to %w:%x\n", name().characters(), pid(), toString(state()), ret_cs, ret_eip);
        ASSERT(is_blocked());
        m_tss_to_resume_kernel = m_tss;
#ifdef SIGNAL_DEBUG
        dbgprintf("resume tss pc: %w:%x\n", m_tss_to_resume_kernel.cs, m_tss_to_resume_kernel.eip);
#endif
    }

    ProcessPagingScope pagingScope(*this);

    if (interrupting_in_kernel) {
        if (!m_signal_stack_user_region) {
            m_signal_stack_user_region = allocate_region(LinearAddress(), defaultStackSize, "signal stack (user)");
            ASSERT(m_signal_stack_user_region);
            m_signal_stack_kernel_region = allocate_region(LinearAddress(), defaultStackSize, "signal stack (kernel)");
            ASSERT(m_signal_stack_user_region);
        }
        m_tss.ss = 0x23;
        m_tss.esp = m_signal_stack_user_region->linearAddress.offset(defaultStackSize).get() & 0xfffffff8;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_signal_stack_kernel_region->linearAddress.offset(defaultStackSize).get() & 0xfffffff8;
        push_value_on_stack(ret_eflags);
        push_value_on_stack(ret_cs);
        push_value_on_stack(ret_eip);
    } else {
        push_value_on_stack(ret_cs);
        push_value_on_stack(ret_eip);
        push_value_on_stack(ret_eflags);
    }

    // PUSHA
    dword old_esp = m_tss.esp;
    push_value_on_stack(m_tss.eax);
    push_value_on_stack(m_tss.ecx);
    push_value_on_stack(m_tss.edx);
    push_value_on_stack(m_tss.ebx);
    push_value_on_stack(old_esp);
    push_value_on_stack(m_tss.ebp);
    push_value_on_stack(m_tss.esi);
    push_value_on_stack(m_tss.edi);

    m_tss.eax = (dword)signal;
    m_tss.cs = 0x1b;
    m_tss.ds = 0x23;
    m_tss.es = 0x23;
    m_tss.fs = 0x23;
    m_tss.gs = 0x23;
    m_tss.eip = handler_laddr.get();

    if (m_return_to_ring3_from_signal_trampoline.is_null()) {
        // FIXME: This should be a global trampoline shared by all processes, not one created per process!
        // FIXME: Remap as read-only after setup.
        auto* region = allocate_region(LinearAddress(), PAGE_SIZE, "signal_trampoline", true, true);
        m_return_to_ring3_from_signal_trampoline = region->linearAddress;
        byte* code_ptr = m_return_to_ring3_from_signal_trampoline.asPtr();
        *code_ptr++ = 0x61; // popa
        *code_ptr++ = 0x9d; // popf
        *code_ptr++ = 0xc3; // ret
        *code_ptr++ = 0x0f; // ud2
        *code_ptr++ = 0x0b;

        m_return_to_ring0_from_signal_trampoline = LinearAddress((dword)code_ptr);
        *code_ptr++ = 0x61; // popa
        *code_ptr++ = 0xb8; // mov eax, <dword>
        *(dword*)code_ptr = Syscall::SC_sigreturn;
        code_ptr += sizeof(dword);
        *code_ptr++ = 0xcd; // int 0x80
        *code_ptr++ = 0x80;
        *code_ptr++ = 0x0f; // ud2
        *code_ptr++ = 0x0b;

        // FIXME: For !SA_NODEFER, maybe we could do something like emitting an int 0x80 syscall here that
        //        unmasks the signal so it can be received again? I guess then I would need one trampoline
        //        per signal number if it's hard-coded, but it's just a few bytes per each.
    }

    if (interrupting_in_kernel)
        push_value_on_stack(m_return_to_ring0_from_signal_trampoline.get());
    else
        push_value_on_stack(m_return_to_ring3_from_signal_trampoline.get());

    m_pending_signals &= ~(1 << signal);

    // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
    set_state(Skip1SchedulerPass);

#ifdef SIGNAL_DEBUG
    dbgprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", name().characters(), pid(), toString(state()), m_tss.cs, m_tss.eip);
#endif
}

void Process::sys$sigreturn()
{
    InterruptDisabler disabler;
    Scheduler::prepare_to_modify_tss(*this);
    m_tss = m_tss_to_resume_kernel;
#ifdef SIGNAL_DEBUG
    dbgprintf("sys$sigreturn in %s(%u)\n", name().characters(), pid());
    dbgprintf(" -> resuming execution at %w:%x\n", m_tss.cs, m_tss.eip);
#endif
    set_state(Skip1SchedulerPass);
    Scheduler::yield();
    kprintf("sys$sigreturn failed in %s(%u)\n", name().characters(), pid());
    ASSERT_NOT_REACHED();
}

void Process::push_value_on_stack(dword value)
{
    m_tss.esp -= 4;
    dword* stack_ptr = (dword*)m_tss.esp;
    *stack_ptr = value;
}

void Process::crash()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(state() != Dead);
    m_termination_signal = SIGSEGV;
    set_state(Dead);
    dumpRegions();
    Scheduler::pick_next_and_switch_now();
    ASSERT_NOT_REACHED();
}

Process* Process::from_pid(pid_t pid)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* process = g_processes->head(); process; process = process->next()) {
        if (process->pid() == pid)
            return process;
    }
    return nullptr;
}

FileDescriptor* Process::file_descriptor(int fd)
{
    if (fd < 0)
        return nullptr;
    if ((size_t)fd < m_file_descriptors.size())
        return m_file_descriptors[fd].ptr();
    return nullptr;
}

const FileDescriptor* Process::file_descriptor(int fd) const
{
    if (fd < 0)
        return nullptr;
    if ((size_t)fd < m_file_descriptors.size())
        return m_file_descriptors[fd].ptr();
    return nullptr;
}

ssize_t Process::sys$get_dir_entries(int fd, void* buffer, size_t size)
{
    VALIDATE_USER_WRITE(buffer, size);
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    return descriptor->get_dir_entries((byte*)buffer, size);
}

int Process::sys$lseek(int fd, off_t offset, int whence)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    return descriptor->seek(offset, whence);
}

int Process::sys$ttyname_r(int fd, char* buffer, size_t size)
{
    VALIDATE_USER_WRITE(buffer, size);
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->isTTY())
        return -ENOTTY;
    auto ttyName = descriptor->tty()->ttyName();
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
    auto* descriptor = file_descriptor(fd);
#ifdef DEBUG_IO
    kprintf("Process::sys$write: handle=%p\n", descriptor);
#endif
    if (!descriptor)
        return -EBADF;
    auto nwritten = descriptor->write((const byte*)data, size);
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
    auto* descriptor = file_descriptor(fd);
#ifdef DEBUG_IO
    kprintf("Process::sys$read: handle=%p\n", descriptor);
#endif
    if (!descriptor)
        return -EBADF;
    if (descriptor->isBlocking()) {
        if (!descriptor->hasDataAvailableForRead()) {
            m_fdBlockedOnRead = fd;
            block(BlockedRead);
            sched_yield();
            if (m_was_interrupted_while_blocked)
                return -EINTR;
        }
    }
    nread = descriptor->read((byte*)outbuf, nread);
#ifdef DEBUG_IO
    kprintf("Process::sys$read: nread=%u\n", nread);
#endif
    return nread;
}

int Process::sys$close(int fd)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    int rc = descriptor->close();
    m_file_descriptors[fd] = nullptr;
    return rc;
}

int Process::sys$lstat(const char* path, Unix::stat* statbuf)
{
    VALIDATE_USER_WRITE(statbuf, sizeof(Unix::stat));
    int error;
    auto descriptor = VirtualFileSystem::the().open(move(path), error, O_NOFOLLOW_NOERROR, cwdInode());
    if (!descriptor)
        return error;
    descriptor->stat(statbuf);
    return 0;
}

int Process::sys$stat(const char* path, Unix::stat* statbuf)
{
    VALIDATE_USER_WRITE(statbuf, sizeof(Unix::stat));
    int error;
    auto descriptor = VirtualFileSystem::the().open(move(path), error, 0, cwdInode());
    if (!descriptor)
        return error;
    descriptor->stat(statbuf);
    return 0;
}

int Process::sys$readlink(const char* path, char* buffer, size_t size)
{
    VALIDATE_USER_READ(path, strlen(path));
    VALIDATE_USER_WRITE(buffer, size);

    int error;
    auto descriptor = VirtualFileSystem::the().open(path, error, O_RDONLY | O_NOFOLLOW_NOERROR, cwdInode());
    if (!descriptor)
        return error;

    if (!descriptor->metadata().isSymbolicLink())
        return -EINVAL;

    auto contents = descriptor->readEntireFile();
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
    auto descriptor = VirtualFileSystem::the().open(path, error, 0, cwdInode());
    if (!descriptor)
        return error;
    if (!descriptor->isDirectory())
        return -ENOTDIR;
    m_cwd = descriptor->vnode();
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
    for (auto& descriptor : m_file_descriptors) {
        if (descriptor)
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
    auto descriptor = VirtualFileSystem::the().open(path, error, options, cwdInode());
    if (!descriptor)
        return error;
    if (options & O_DIRECTORY && !descriptor->isDirectory())
        return -ENOTDIR; // FIXME: This should be handled by VFS::open.

    int fd = 0;
    for (; fd < m_max_open_file_descriptors; ++fd) {
        if (!m_file_descriptors[fd])
            break;
    }
    m_file_descriptors[fd] = move(descriptor);
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

int Process::sys$isatty(int fd)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->isTTY())
        return -ENOTTY;
    return 1;
}

int Process::sys$kill(pid_t pid, int signal)
{
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
    auto* peer = Process::from_pid(pid);
    if (!peer)
        return -ESRCH;
    peer->send_signal(signal, this);
    return 0;
}

int Process::sys$sleep(unsigned seconds)
{
    if (!seconds)
        return 0;
    sleep(seconds * TICKS_PER_SECOND);
    if (m_wakeupTime > system.uptime) {
        ASSERT(m_was_interrupted_while_blocked);
        dword ticks_left_until_original_wakeup_time = m_wakeupTime - system.uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
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

uid_t Process::sys$geteuid()
{
    return m_euid;
}

gid_t Process::sys$getegid()
{
    return m_egid;
}

pid_t Process::sys$getpid()
{
    return m_pid;
}

pid_t Process::sys$getppid()
{
    return m_ppid;
}

mode_t Process::sys$umask(mode_t mask)
{
    auto old_mask = m_umask;
    m_umask = mask;
    return old_mask;
}

void Process::reap(pid_t pid)
{
    InterruptDisabler disabler;
    auto* process = Process::from_pid(pid);
    ASSERT(process);
    dbgprintf("reap: %s(%u) {%s}\n", process->name().characters(), process->pid(), toString(process->state()));
    ASSERT(process->state() == Dead);
    g_processes->remove(process);
    delete process;
}

pid_t Process::sys$waitpid(pid_t waitee, int* wstatus, int options)
{
    if (wstatus)
        VALIDATE_USER_WRITE(wstatus, sizeof(int));

    {
        InterruptDisabler disabler;
        if (!Process::from_pid(waitee))
            return -ECHILD;
    }
    m_waitee = waitee;
    m_waitee_status = 0;
    block(BlockedWait);
    sched_yield();
    if (m_was_interrupted_while_blocked)
        return -EINTR;
    reap(waitee);
    if (wstatus)
        *wstatus = m_waitee_status;
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
    m_was_interrupted_while_blocked = false;
    set_state(state);
}

void block(Process::State state)
{
    current->block(state);
    sched_yield();
}

void sleep(DWORD ticks)
{
    ASSERT(current->state() == Process::Running);
    current->setWakeupTime(system.uptime + ticks);
    current->block(Process::BlockedSleep);
    sched_yield();
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
    auto* process = Process::from_pid(pid);
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
    Process::for_each_in_pgrp(pid(), [&] (auto& process) {
        found_process_with_same_pgid_as_my_pid = true;
        return false;
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
    auto* process = Process::from_pid(pid);
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
    auto* group_leader = Process::from_pid(pgid);
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
    auto* process = Process::from_pid(pid);
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
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->isTTY())
        return -ENOTTY;
    auto& tty = *descriptor->tty();
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
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->isTTY())
        return -ENOTTY;
    auto& tty = *descriptor->tty();
    if (&tty != m_tty)
        return -ENOTTY;
    tty.set_pgid(pgid);
    return 0;
}

int Process::sys$getdtablesize()
{
    return m_max_open_file_descriptors;
}

int Process::sys$dup(int old_fd)
{
    auto* descriptor = file_descriptor(old_fd);
    if (!descriptor)
        return -EBADF;
    if (number_of_open_file_descriptors() == m_max_open_file_descriptors)
        return -EMFILE;
    int new_fd = 0;
    for (; new_fd < m_max_open_file_descriptors; ++new_fd) {
        if (!m_file_descriptors[new_fd])
            break;
    }
    m_file_descriptors[new_fd] = descriptor;
    return new_fd;
}

int Process::sys$dup2(int old_fd, int new_fd)
{
    auto* descriptor = file_descriptor(old_fd);
    if (!descriptor)
        return -EBADF;
    if (number_of_open_file_descriptors() == m_max_open_file_descriptors)
        return -EMFILE;
    m_file_descriptors[new_fd] = descriptor;
    return new_fd;
}

Unix::sighandler_t Process::sys$signal(int signum, Unix::sighandler_t handler)
{
    // FIXME: Fail with -EINVAL if attepmting to catch or ignore SIGKILL or SIGSTOP.
    if (signum >= 32)
        return (Unix::sighandler_t)-EINVAL;
    dbgprintf("sys$signal: %d => L%x\n", signum, handler);
    return nullptr;
}

int Process::sys$sigaction(int signum, const Unix::sigaction* act, Unix::sigaction* old_act)
{
    // FIXME: Fail with -EINVAL if attepmting to change action for SIGKILL or SIGSTOP.
    if (signum >= 32)
        return -EINVAL;
    VALIDATE_USER_READ(act, sizeof(Unix::sigaction));
    InterruptDisabler disabler; // FIXME: This should use a narrower lock.
    auto& action = m_signal_action_data[signum];
    if (old_act) {
        VALIDATE_USER_WRITE(old_act, sizeof(Unix::sigaction));
        old_act->sa_flags = action.flags;
        old_act->sa_restorer = (decltype(old_act->sa_restorer))action.restorer.get();
        old_act->sa_sigaction = (decltype(old_act->sa_sigaction))action.handler_or_sigaction.get();
    }
    action.restorer = LinearAddress((dword)act->sa_restorer);
    action.flags = act->sa_flags;
    action.handler_or_sigaction = LinearAddress((dword)act->sa_sigaction);
    return 0;
}

int Process::sys$getgroups(int count, gid_t* gids)
{
    if (count < 0)
        return -EINVAL;
    ASSERT(m_gids.size() < MAX_PROCESS_GIDS);
    if (!count)
        return m_gids.size();
    if (count != m_gids.size())
        return -EINVAL;
    VALIDATE_USER_WRITE(gids, sizeof(gid_t) * count);
    size_t i = 0;
    for (auto gid : m_gids)
        gids[i++] = gid;
    return 0;
}

int Process::sys$setgroups(size_t count, const gid_t* gids)
{
    if (!is_root())
        return -EPERM;
    if (count >= MAX_PROCESS_GIDS)
        return -EINVAL;
    VALIDATE_USER_READ(gids, sizeof(gid_t) * count);
    m_gids.clear();
    m_gids.set(m_gid);
    for (size_t i = 0; i < count; ++i)
        m_gids.set(gids[i]);
    return 0;
}
