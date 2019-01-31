#include "types.h"
#include "Process.h"
#include "kmalloc.h"
#include "StdLib.h"
#include "i386.h"
#include "system.h"
#include <Kernel/FileDescriptor.h>
#include <Kernel/VirtualFileSystem.h>
#include "ELFLoader.h"
#include "MemoryManager.h"
#include "i8253.h"
#include "RTC.h"
#include "ProcFileSystem.h"
#include <AK/StdLibExtras.h>
#include <LibC/signal_numbers.h>
#include <LibC/errno_numbers.h>
#include "Syscall.h"
#include "Scheduler.h"
#include "FIFO.h"
#include "KSyms.h"
#include <WindowServer/WSWindow.h>
#include "MasterPTY.h"

//#define DEBUG_IO
//#define TASK_DEBUG
//#define FORK_DEBUG
#define SIGNAL_DEBUG
#define MAX_PROCESS_GIDS 32

static const dword defaultStackSize = 16384;

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
    return hostnameStorage(disabler).isolated_copy();
}

CoolGlobals* g_cool_globals;

void Process::initialize()
{
#ifdef COOL_GLOBALS
    g_cool_globals = reinterpret_cast<CoolGlobals*>(0x1000);
#endif
    next_pid = 0;
    g_processes = new InlineLinkedList<Process>;
    s_hostname = new String("courage");
    Scheduler::initialize();
    initialize_gui_statics();
}

Vector<Process*> Process::allProcesses()
{
    InterruptDisabler disabler;
    Vector<Process*> processes;
    processes.ensure_capacity(g_processes->size_slow());
    for (auto* process = g_processes->head(); process; process = process->next())
        processes.append(process);
    return processes;
}

Region* Process::allocate_region(LinearAddress laddr, size_t size, String&& name, bool is_readable, bool is_writable, bool commit)
{
    size = PAGE_ROUND_UP(size);
    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_nextRegion;
        m_nextRegion = m_nextRegion.offset(size).offset(PAGE_SIZE);
    }
    laddr.mask(0xfffff000);
    m_regions.append(adopt(*new Region(laddr, size, move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    if (commit)
        m_regions.last()->commit();
    return m_regions.last().ptr();
}

Region* Process::allocate_file_backed_region(LinearAddress laddr, size_t size, RetainPtr<Inode>&& inode, String&& name, bool is_readable, bool is_writable)
{
    size = PAGE_ROUND_UP(size);
    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_nextRegion;
        m_nextRegion = m_nextRegion.offset(size).offset(PAGE_SIZE);
    }
    laddr.mask(0xfffff000);
    m_regions.append(adopt(*new Region(laddr, size, move(inode), move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    return m_regions.last().ptr();
}

Region* Process::allocate_region_with_vmo(LinearAddress laddr, size_t size, RetainPtr<VMObject>&& vmo, size_t offset_in_vmo, String&& name, bool is_readable, bool is_writable)
{
    ASSERT(vmo);
    size = PAGE_ROUND_UP(size);
    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_nextRegion;
        m_nextRegion = m_nextRegion.offset(size).offset(PAGE_SIZE);
    }
    laddr.mask(0xfffff000);
    offset_in_vmo &= PAGE_MASK;
    size = ceilDiv(size, PAGE_SIZE) * PAGE_SIZE;
    m_regions.append(adopt(*new Region(laddr, size, move(vmo), offset_in_vmo, move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    return m_regions.last().ptr();
}

bool Process::deallocate_region(Region& region)
{
    InterruptDisabler disabler;
    for (size_t i = 0; i < m_regions.size(); ++i) {
        if (m_regions[i].ptr() == &region) {
            MM.unmap_region(region);
            m_regions.remove(i);
            return true;
        }
    }
    return false;
}

Region* Process::regionFromRange(LinearAddress laddr, size_t size)
{
    for (auto& region : m_regions) {
        if (region->laddr() == laddr && region->size() == size)
            return region.ptr();
    }
    return nullptr;
}

int Process::sys$set_mmap_name(void* addr, size_t size, const char* name)
{
    if (!validate_read_str(name))
        return -EFAULT;
    auto* region = regionFromRange(LinearAddress((dword)addr), size);
    if (!region)
        return -EINVAL;
    region->set_name(String(name));
    return 0;
}

void* Process::sys$mmap(const Syscall::SC_mmap_params* params)
{
    if (!validate_read(params, sizeof(Syscall::SC_mmap_params)))
        return (void*)-EFAULT;
    void* addr = (void*)params->addr;
    size_t size = params->size;
    int prot = params->prot;
    int flags = params->flags;
    int fd = params->fd;
    off_t offset = params->offset;
    if (size == 0)
        return (void*)-EINVAL;
    if ((dword)addr & ~PAGE_MASK || size & ~PAGE_MASK)
        return (void*)-EINVAL;
    if (flags & MAP_ANONYMOUS) {
        InterruptDisabler disabler;
        // FIXME: Implement mapping at a client-specified address. Most of the support is already in plcae.
        ASSERT(addr == nullptr);
        auto* region = allocate_region(LinearAddress(), size, "mmap", prot & PROT_READ, prot & PROT_WRITE, false);
        if (!region)
            return (void*)-ENOMEM;
        return region->laddr().as_ptr();
    }
    if (offset & ~PAGE_MASK)
        return (void*)-EINVAL;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return (void*)-EBADF;
    if (!descriptor->supports_mmap())
        return (void*)-ENODEV;
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    auto region_name = descriptor->absolute_path();
    InterruptDisabler disabler;
    // FIXME: Implement mapping at a client-specified address. Most of the support is already in plcae.
    ASSERT(addr == nullptr);
    auto* region = allocate_file_backed_region(LinearAddress(), size, descriptor->inode(), move(region_name), prot & PROT_READ, prot & PROT_WRITE);
    if (!region)
        return (void*)-ENOMEM;
    return region->laddr().as_ptr();
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
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto hostname = getHostname();
    if (size < (hostname.length() + 1))
        return -ENAMETOOLONG;
    memcpy(buffer, hostname.characters(), size);
    return 0;
}

Process* Process::fork(RegisterDump& regs)
{
    auto* child = new Process(String(m_name), m_uid, m_gid, m_pid, m_ring, m_cwd.copyRef(), m_executable.copyRef(), m_tty, this);
    if (!child)
        return nullptr;

    memcpy(child->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    child->m_signal_mask = m_signal_mask;
#ifdef FORK_DEBUG
    dbgprintf("fork: child=%p\n", child);
#endif

    child->m_initial_arguments = m_initial_arguments;
    child->m_initial_environment = m_initial_environment;

    for (auto& region : m_regions) {
#ifdef FORK_DEBUG
        dbgprintf("fork: cloning Region{%p} \"%s\" L%x\n", region.ptr(), region->name.characters(), region->laddr().get());
#endif
        auto cloned_region = region->clone();
        child->m_regions.append(move(cloned_region));
        MM.map_region(*child, *child->m_regions.last());
        if (region.ptr() == m_display_framebuffer_region.ptr())
            child->m_display_framebuffer_region = child->m_regions.last().copyRef();
    }

    for (auto gid : m_gids)
        child->m_gids.set(gid);

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

    child->m_fpu_state = m_fpu_state;
    child->m_has_used_fpu = m_has_used_fpu;

#ifdef FORK_DEBUG
    dbgprintf("fork: child will begin executing at %w:%x with stack %w:%x\n", child->m_tss.cs, child->m_tss.eip, child->m_tss.ss, child->m_tss.esp);
#endif

    ProcFS::the().add_process(*child);

    {
        InterruptDisabler disabler;
        g_processes->prepend(child);
        system.nprocess++;
    }
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

int Process::do_exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment)
{
    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    int error;
    auto descriptor = VFS::the().open(path, error, 0, 0, m_cwd ? m_cwd->identifier() : InodeIdentifier());
    if (!descriptor) {
        ASSERT(error != 0);
        return error;
    }

    if (!descriptor->metadata().mayExecute(m_euid, m_gids))
        return -EACCES;

    if (!descriptor->metadata().size) {
        kprintf("exec() of 0-length binaries not supported\n");
        return -ENOTIMPL;
    }

    dword entry_eip = 0;
    // FIXME: Is there a race here?
    auto old_page_directory = move(m_page_directory);
    m_page_directory = PageDirectory::create();
#ifdef MM_DEBUG
    dbgprintf("Process %u exec: PD=%x created\n", pid(), m_page_directory.ptr());
#endif
    ProcessPagingScope paging_scope(*this);

    auto vmo = VMObject::create_file_backed(descriptor->inode(), descriptor->metadata().size);
    vmo->set_name(descriptor->absolute_path());
    auto* region = allocate_region_with_vmo(LinearAddress(), descriptor->metadata().size, vmo.copyRef(), 0, "helper", true, false);

    // FIXME: Should we consider doing on-demand paging here? Is it actually useful?
    bool success = region->page_in();

    ASSERT(success);
    {
        InterruptDisabler disabler;
        // Okay, here comes the sleight of hand, pay close attention..
        auto old_regions = move(m_regions);
        ELFLoader loader(region->laddr().as_ptr());
        loader.map_section_hook = [&] (LinearAddress laddr, size_t size, size_t alignment, size_t offset_in_image, bool is_readable, bool is_writable, const String& name) {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            size = ((size / 4096) + 1) * 4096; // FIXME: Use ceil_div?
            (void) allocate_region_with_vmo(laddr, size, vmo.copyRef(), offset_in_image, String(name), is_readable, is_writable);
            return laddr.as_ptr();
        };
        loader.alloc_section_hook = [&] (LinearAddress laddr, size_t size, size_t alignment, bool is_readable, bool is_writable, const String& name) {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            size = ((size / 4096) + 1) * 4096; // FIXME: Use ceil_div?
            (void) allocate_region(laddr, size, String(name), is_readable, is_writable);
            return laddr.as_ptr();
        };
        bool success = loader.load();
        if (!success) {
            m_page_directory = move(old_page_directory);
            // FIXME: RAII this somehow instead.
            ASSERT(current == this);
            MM.enter_process_paging_scope(*this);
            m_regions = move(old_regions);
            kprintf("sys$execve: Failure loading %s\n", path.characters());
            return -ENOEXEC;
        }

        entry_eip = loader.entry().get();
        if (!entry_eip) {
            m_page_directory = move(old_page_directory);
            // FIXME: RAII this somehow instead.
            ASSERT(current == this);
            MM.enter_process_paging_scope(*this);
            m_regions = move(old_regions);
            return -ENOEXEC;
        }
    }

    m_signal_stack_kernel_region = nullptr;
    m_signal_stack_user_region = nullptr;
    m_display_framebuffer_region = nullptr;
    memset(m_signal_action_data, 0, sizeof(m_signal_action_data));
    m_signal_mask = 0xffffffff;
    m_pending_signals = 0;

    for (size_t i = 0; i < m_fds.size(); ++i) {
        auto& daf = m_fds[i];
        if (daf.descriptor && daf.flags & FD_CLOEXEC) {
            daf.descriptor->close();
            daf = { };
        }
    }

    // We cli() manually here because we don't want to get interrupted between do_exec() and Schedule::yield().
    // The reason is that the task redirection we've set up above will be clobbered by the timer IRQ.
    // If we used an InterruptDisabler that sti()'d on exit, we might timer tick'd too soon in exec().
    cli();

    Scheduler::prepare_to_modify_tss(*this);

    m_name = parts.take_last();

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
    m_tss.cr3 = page_directory().cr3();
    m_stack_region = allocate_region(LinearAddress(), defaultStackSize, "stack");
    ASSERT(m_stack_region);
    m_stackTop3 = m_stack_region->laddr().offset(defaultStackSize).get();
    m_tss.esp = m_stackTop3;
    m_tss.ss0 = 0x10;
    m_tss.esp0 = old_esp0;
    m_tss.ss2 = m_pid;

    m_executable = descriptor->inode();
    m_initial_arguments = move(arguments);
    m_initial_environment = move(environment);

#ifdef TASK_DEBUG
    kprintf("Process %u (%s) exec'd %s @ %p\n", pid(), name().characters(), path.characters(), m_tss.eip);
#endif

    set_state(Skip1SchedulerPass);
    return 0;
}

int Process::exec(const String& path, Vector<String>&& arguments, Vector<String>&& environment)
{
    // The bulk of exec() is done by do_exec(), which ensures that all locals
    // are cleaned up by the time we yield-teleport below.
    int rc = do_exec(path, move(arguments), move(environment));
    if (rc < 0)
        return rc;

    if (current == this) {
        Scheduler::yield();
        ASSERT_NOT_REACHED();
    }
    return 0;
}

int Process::sys$execve(const char* filename, const char** argv, const char** envp)
{
    if (!validate_read_str(filename))
        return -EFAULT;
    if (argv) {
        if (!validate_read_typed(argv))
            return -EFAULT;
        for (size_t i = 0; argv[i]; ++i) {
            if (!validate_read_str(argv[i]))
                return -EFAULT;
        }
    }
    if (envp) {
        if (!validate_read_typed(envp))
            return -EFAULT;
        for (size_t i = 0; envp[i]; ++i) {
            if (!validate_read_str(envp[i]))
                return -EFAULT;
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
        for (size_t i = 0; envp[i]; ++i)
            environment.append(envp[i]);
    }

    int rc = exec(path, move(arguments), move(environment));
    ASSERT(rc < 0); // We should never continue after a successful exec!
    return rc;
}

Process* Process::create_user_process(const String& path, uid_t uid, gid_t gid, pid_t parent_pid, int& error, Vector<String>&& arguments, Vector<String>&& environment, TTY* tty)
{
    // FIXME: Don't split() the path twice (sys$spawn also does it...)
    auto parts = path.split('/');
    if (arguments.is_empty()) {
        arguments.append(parts.last());
    }
    RetainPtr<Inode> cwd;
    {
        InterruptDisabler disabler;
        if (auto* parent = Process::from_pid(parent_pid))
            cwd = parent->m_cwd.copyRef();
    }

    if (!cwd)
        cwd = VFS::the().root_inode();

    auto* process = new Process(parts.take_last(), uid, gid, parent_pid, Ring3, move(cwd), nullptr, tty);

    error = process->exec(path, move(arguments), move(environment));
    if (error != 0) {
        delete process;
        return nullptr;
    }

    ProcFS::the().add_process(*process);

    {
        InterruptDisabler disabler;
        g_processes->prepend(process);
        system.nprocess++;
    }
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
    MM.map_region(*this, *region);
    char* envpage = (char*)region->laddr().get();
    *environ = (char**)envpage;
    char* bufptr = envpage + (sizeof(char*) * (m_initial_environment.size() + 1));
    for (size_t i = 0; i < m_initial_environment.size(); ++i) {
        (*environ)[i] = bufptr;
        memcpy(bufptr, m_initial_environment[i].characters(), m_initial_environment[i].length());
        bufptr += m_initial_environment[i].length();
        *(bufptr++) = '\0';
    }
    (*environ)[m_initial_environment.size()] = nullptr;
    return 0;
}

int Process::sys$get_arguments(int* argc, char*** argv)
{
    auto* region = allocate_region(LinearAddress(), PAGE_SIZE, "argv");
    if (!region)
        return -ENOMEM;
    MM.map_region(*this, *region);
    char* argpage = (char*)region->laddr().get();
    *argc = m_initial_arguments.size();
    *argv = (char**)argpage;
    char* bufptr = argpage + (sizeof(char*) * (m_initial_arguments.size() + 1));
    for (size_t i = 0; i < m_initial_arguments.size(); ++i) {
        (*argv)[i] = bufptr;
        memcpy(bufptr, m_initial_arguments[i].characters(), m_initial_arguments[i].length());
        bufptr += m_initial_arguments[i].length();
        *(bufptr++) = '\0';
    }
    (*argv)[m_initial_arguments.size()] = nullptr;
    return 0;
}

Process* Process::create_kernel_process(String&& name, void (*e)())
{
    auto* process = new Process(move(name), (uid_t)0, (gid_t)0, (pid_t)0, Ring0);
    process->m_tss.eip = (dword)e;

    if (process->pid() != 0) {
        {
            InterruptDisabler disabler;
            g_processes->prepend(process);
            system.nprocess++;
        }
        ProcFS::the().add_process(*process);
#ifdef TASK_DEBUG
        kprintf("Kernel process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), process->m_tss.eip);
#endif
    }

    return process;
}

Process::Process(String&& name, uid_t uid, gid_t gid, pid_t ppid, RingLevel ring, RetainPtr<Inode>&& cwd, RetainPtr<Inode>&& executable, TTY* tty, Process* fork_parent)
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
    memset(&m_fpu_state, 0, sizeof(FPUState));

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

    m_page_directory = PageDirectory::create();
#ifdef MM_DEBUG
    dbgprintf("Process %u ctor: PD=%x created\n", pid(), m_page_directory.ptr());
#endif

    if (fork_parent) {
        m_fds.resize(fork_parent->m_fds.size());
        for (size_t i = 0; i < fork_parent->m_fds.size(); ++i) {
            if (!fork_parent->m_fds[i].descriptor)
                continue;
#ifdef FORK_DEBUG
            dbgprintf("fork: cloning fd %u... (%p) istty? %u\n", i, fork_parent->m_fds[i].descriptor.ptr(), fork_parent->m_fds[i].descriptor->is_tty());
#endif
            m_fds[i].descriptor = fork_parent->m_fds[i].descriptor->clone();
            m_fds[i].flags = fork_parent->m_fds[i].flags;
        }
    } else {
        m_fds.resize(m_max_open_file_descriptors);
        if (tty) {
            int error;
            m_fds[0].set(tty->open(error, O_RDONLY));
            m_fds[1].set(tty->open(error, O_WRONLY));
            m_fds[2].set(tty->open(error, O_WRONLY));
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

    m_tss.cr3 = page_directory().cr3();

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
            m_stackTop3 = region->laddr().offset(defaultStackSize).get();
            m_tss.esp = m_stackTop3;
        }
    }

    if (isRing3()) {
        // Ring3 processes need a separate stack for Ring0.
        m_kernelStack = kmalloc(defaultStackSize);
        m_stackTop0 = ((dword)m_kernelStack + defaultStackSize) & 0xffffff8;
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
    ProcFS::the().remove_process(*this);
    system.nprocess--;

    if (g_last_fpu_process == this)
        g_last_fpu_process = nullptr;

    if (selector())
        gdt_free_entry(selector());

    if (m_kernelStack) {
        kfree(m_kernelStack);
        m_kernelStack = nullptr;
    }
}

void Process::dumpRegions()
{
    kprintf("Process %s(%u) regions:\n", name().characters(), pid());
    kprintf("BEGIN       END         SIZE        NAME\n");
    for (auto& region : m_regions) {
        kprintf("%x -- %x    %x    %s\n",
            region->laddr().get(),
            region->laddr().offset(region->size() - 1).get(),
            region->size(),
            region->name().characters());
    }
}

void Process::sys$exit(int status)
{
    cli();
#ifdef TASK_DEBUG
    kprintf("sys$exit: %s(%u) exit with status %d\n", name().characters(), pid(), status);
#endif

    die();
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
    die();
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
    return m_pending_signals & m_signal_mask;
}

bool Process::dispatch_one_pending_signal()
{
    ASSERT_INTERRUPTS_DISABLED();
    dword signal_candidates = m_pending_signals & m_signal_mask;
    ASSERT(signal_candidates);

    byte signal = 0;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << signal)) {
            break;
        }
    }
    return dispatch_signal(signal);
}

bool Process::dispatch_signal(byte signal)
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
        terminate_due_to_signal(signal);
        return false;
    }

    m_pending_signals &= ~(1 << signal);

    if (handler_laddr.as_ptr() == SIG_IGN) {
        dbgprintf("%s(%u) ignored signal %u\n", name().characters(), pid(), signal); return false;
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
        m_tss.esp = m_signal_stack_user_region->laddr().offset(defaultStackSize).get() & 0xfffffff8;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_signal_stack_kernel_region->laddr().offset(defaultStackSize).get() & 0xfffffff8;
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
        m_return_to_ring3_from_signal_trampoline = region->laddr();
        byte* code_ptr = m_return_to_ring3_from_signal_trampoline.as_ptr();
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

    // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
    set_state(Skip1SchedulerPass);

#ifdef SIGNAL_DEBUG
    dbgprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", name().characters(), pid(), toString(state()), m_tss.cs, m_tss.eip);
#endif
    return true;
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
    dumpRegions();
    die();
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
    if ((size_t)fd < m_fds.size())
        return m_fds[fd].descriptor.ptr();
    return nullptr;
}

const FileDescriptor* Process::file_descriptor(int fd) const
{
    if (fd < 0)
        return nullptr;
    if ((size_t)fd < m_fds.size())
        return m_fds[fd].descriptor.ptr();
    return nullptr;
}

ssize_t Process::sys$get_dir_entries(int fd, void* buffer, size_t size)
{
    if (!validate_write(buffer, size))
        return -EFAULT;
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
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_tty())
        return -ENOTTY;
    auto ttyName = descriptor->tty()->tty_name();
    if (size < ttyName.length() + 1)
        return -ERANGE;
    strcpy(buffer, ttyName.characters());
    return 0;
}

int Process::sys$ptsname_r(int fd, char* buffer, size_t size)
{
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    auto* master_pty = descriptor->master_pty();
    if (!master_pty)
        return -ENOTTY;
    auto pts_name = master_pty->pts_name();
    if (size < pts_name.length() + 1)
        return -ERANGE;
    strcpy(buffer, pts_name.characters());
    return 0;
}

ssize_t Process::sys$write(int fd, const void* data, size_t size)
{
    if (!validate_read(data, size))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u): sys$write(%d, %p, %u)\n", name().characters(), pid(), fd, data, size);
#endif
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    ssize_t nwritten = 0;
    if (descriptor->is_blocking()) {
        while (nwritten < (ssize_t)size) {
#ifdef IO_DEBUG
            dbgprintf("while %u < %u\n", nwritten, size);
#endif
            if (!descriptor->can_write(*this)) {
#ifdef IO_DEBUG
                dbgprintf("block write on %d\n", fd);
#endif
                m_blocked_fd = fd;
                block(BlockedWrite);
                Scheduler::yield();
            }
            ssize_t rc = descriptor->write(*this, (const byte*)data + nwritten, size - nwritten);
#ifdef IO_DEBUG
            dbgprintf("   -> write returned %d\n", rc);
#endif
            if (rc < 0) {
                // FIXME: Support returning partial nwritten with errno.
                ASSERT(nwritten == 0);
                return rc;
            }
            if (rc == 0)
                break;
            if (has_unmasked_pending_signals()) {
                block(BlockedSignal);
                Scheduler::yield();
                if (nwritten == 0)
                    return -EINTR;
            }
            nwritten += rc;
        }
    } else {
        nwritten = descriptor->write(*this, (const byte*)data, size);
    }
    if (has_unmasked_pending_signals()) {
        block(BlockedSignal);
        Scheduler::yield();
        if (nwritten == 0)
            return -EINTR;
    }
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$write: nwritten=%u\n", name().characters(), pid(), nwritten);
#endif
    return nwritten;
}

ssize_t Process::sys$read(int fd, void* outbuf, size_t nread)
{
    if (!validate_write(outbuf, nread))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$read(%d, %p, %u)\n", name().characters(), pid(), fd, outbuf, nread);
#endif
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
#ifdef DEBUG_IO
    dbgprintf("   > descriptor:%p, is_blocking:%u, can_read:%u\n", descriptor, descriptor->is_blocking(), descriptor->can_read(*this));
    dbgprintf("   > inode:K%x, device:K%x\n", descriptor->inode(), descriptor->character_device());
#endif
    if (descriptor->is_blocking()) {
        if (!descriptor->can_read(*this)) {
            m_blocked_fd = fd;
            block(BlockedRead);
            sched_yield();
            if (m_was_interrupted_while_blocked)
                return -EINTR;
        }
    }
    nread = descriptor->read(*this, (byte*)outbuf, nread);
#ifdef DEBUG_IO
    dbgprintf("%s(%u) Process::sys$read: nread=%u\n", name().characters(), pid(), nread);
#endif
    return nread;
}

int Process::sys$close(int fd)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    int rc = descriptor->close();
    m_fds[fd] = { };
    return rc;
}

int Process::sys$utime(const char* pathname, const utimbuf* buf)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    if (buf && !validate_read_typed(buf))
        return -EFAULT;
    String path(pathname);
    int error;
    auto descriptor = VFS::the().open(move(path), error, 0, 0, cwd_inode()->identifier());
    if (!descriptor)
        return error;
    auto& inode = *descriptor->inode();
    if (inode.fs().is_readonly())
        return -EROFS;
    time_t atime;
    time_t mtime;
    if (buf) {
        atime = buf->actime;
        mtime = buf->modtime;
    } else {
        auto now = RTC::now();
        mtime = now;
        atime = now;
    }
    inode.set_atime(atime);
    inode.set_mtime(mtime);
    return 0;
}

int Process::sys$access(const char* pathname, int mode)
{
    (void) mode;
    if (!validate_read_str(pathname))
        return -EFAULT;
    ASSERT_NOT_REACHED();
}

int Process::sys$fcntl(int fd, int cmd, dword arg)
{
    (void) cmd;
    (void) arg;
    dbgprintf("sys$fcntl: fd=%d, cmd=%d, arg=%u\n", fd, cmd, arg);
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    // NOTE: The FD flags are not shared between FileDescriptor objects.
    //       This means that dup() doesn't copy the FD_CLOEXEC flag!
    switch (cmd) {
    case F_DUPFD: {
        int arg_fd = (int)arg;
        if (arg_fd < 0)
            return -EINVAL;
        int new_fd = -1;
        for (int i = arg_fd; i < (int)m_max_open_file_descriptors; ++i) {
            if (!m_fds[i]) {
                new_fd = i;
                break;
            }
        }
        if (new_fd == -1)
            return -EMFILE;
        m_fds[new_fd].set(descriptor);
        break;
    }
    case F_GETFD:
        return m_fds[fd].flags;
    case F_SETFD:
        m_fds[fd].flags = arg;
        break;
    case F_GETFL:
        return descriptor->file_flags();
    case F_SETFL:
        // FIXME: Support changing O_NONBLOCK
        descriptor->set_file_flags(arg);
        break;
    default:
        ASSERT_NOT_REACHED();
    }
    return 0;
}

int Process::sys$fstat(int fd, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    descriptor->fstat(statbuf);
    return 0;
}

int Process::sys$lstat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    int error;
    auto descriptor = VFS::the().open(move(path), error, O_NOFOLLOW_NOERROR | O_DONT_OPEN_DEVICE, 0, cwd_inode()->identifier());
    if (!descriptor)
        return error;
    descriptor->fstat(statbuf);
    return 0;
}

int Process::sys$stat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    int error;
    auto descriptor = VFS::the().open(move(path), error, O_DONT_OPEN_DEVICE, 0, cwd_inode()->identifier());
    if (!descriptor)
        return error;
    descriptor->fstat(statbuf);
    return 0;
}

int Process::sys$readlink(const char* path, char* buffer, size_t size)
{
    if (!validate_read_str(path))
        return -EFAULT;
    if (!validate_write(buffer, size))
        return -EFAULT;

    int error;
    auto descriptor = VFS::the().open(path, error, O_RDONLY | O_NOFOLLOW_NOERROR, 0, cwd_inode()->identifier());
    if (!descriptor)
        return error;

    if (!descriptor->metadata().isSymbolicLink())
        return -EINVAL;

    auto contents = descriptor->read_entire_file(*this);
    if (!contents)
        return -EIO; // FIXME: Get a more detailed error from VFS.

    memcpy(buffer, contents.pointer(), min(size, contents.size()));
    if (contents.size() + 1 < size)
        buffer[contents.size()] = '\0';
    return 0;
}

int Process::sys$chdir(const char* path)
{
    if (!validate_read_str(path))
        return -EFAULT;
    int error;
    auto descriptor = VFS::the().open(path, error, 0, 0, cwd_inode()->identifier());
    if (!descriptor)
        return error;
    if (!descriptor->is_directory())
        return -ENOTDIR;
    m_cwd = descriptor->inode();
    return 0;
}

int Process::sys$getcwd(char* buffer, size_t size)
{
    if (!validate_write(buffer, size))
        return -EFAULT;
    ASSERT(cwd_inode());
    auto path = VFS::the().absolute_path(*cwd_inode());
    if (path.is_null())
        return -EINVAL;
    if (size < path.length() + 1)
        return -ERANGE;
    strcpy(buffer, path.characters());
    return 0;
}

size_t Process::number_of_open_file_descriptors() const
{
    size_t count = 0;
    for (auto& descriptor : m_fds) {
        if (descriptor)
            ++count;
    }
    return count;
}

int Process::sys$open(const char* path, int options, mode_t mode)
{
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$open(\"%s\")\n", name().characters(), pid(), path);
#endif
    if (!validate_read_str(path))
        return -EFAULT;
    if (number_of_open_file_descriptors() >= m_max_open_file_descriptors)
        return -EMFILE;
    int error = -EWHYTHO;
    ASSERT(cwd_inode());
    auto descriptor = VFS::the().open(path, error, options, mode, cwd_inode()->identifier());
    if (!descriptor)
        return error;
    if (options & O_DIRECTORY && !descriptor->is_directory())
        return -ENOTDIR; // FIXME: This should be handled by VFS::open.
    if (options & O_NONBLOCK)
        descriptor->set_blocking(false);

    int fd = 0;
    for (; fd < (int)m_max_open_file_descriptors; ++fd) {
        if (!m_fds[fd])
            break;
    }
    dword flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(descriptor), flags);
    return fd;
}

int Process::alloc_fd()
{
    int fd = -1;
    for (int i = 0; i < (int)m_max_open_file_descriptors; ++i) {
        if (!m_fds[i]) {
            fd = i;
            break;
        }
    }
    return fd;
}

int Process::sys$pipe(int pipefd[2])
{
    if (!validate_write_typed(pipefd))
        return -EFAULT;
    if (number_of_open_file_descriptors() + 2 > max_open_file_descriptors())
        return -EMFILE;
    auto fifo = FIFO::create();

    int reader_fd = alloc_fd();
    m_fds[reader_fd].set(FileDescriptor::create_pipe_reader(*fifo));
    pipefd[0] = reader_fd;

    int writer_fd = alloc_fd();
    m_fds[writer_fd].set(FileDescriptor::create_pipe_writer(*fifo));
    pipefd[1] = writer_fd;

    return 0;
}

int Process::sys$killpg(int pgrp, int signum)
{
    if (signum < 1 || signum >= 32)
        return -EINVAL;
    (void) pgrp;
    ASSERT_NOT_REACHED();
}

int Process::sys$setuid(uid_t)
{
    ASSERT_NOT_REACHED();
}

int Process::sys$setgid(gid_t)
{
    ASSERT_NOT_REACHED();
}

unsigned Process::sys$alarm(unsigned seconds)
{
    (void) seconds;
    ASSERT_NOT_REACHED();
}

int Process::sys$uname(utsname* buf)
{
    if (!validate_write_typed(buf))
        return -EFAULT;
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
    if (!descriptor->is_tty())
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
    if (!validate_write_typed(tv))
        return -EFAULT;
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

int Process::reap(Process& process)
{
    InterruptDisabler disabler;
    int exit_status = (process.m_termination_status << 8) | process.m_termination_signal;

    if (process.ppid()) {
        auto* parent = Process::from_pid(process.ppid());
        if (parent) {
            parent->m_ticks_in_user_for_dead_children += process.m_ticks_in_user + process.m_ticks_in_user_for_dead_children;
            parent->m_ticks_in_kernel_for_dead_children += process.m_ticks_in_kernel + process.m_ticks_in_kernel_for_dead_children;
        }
    }

    dbgprintf("reap: %s(%u) {%s}\n", process.name().characters(), process.pid(), toString(process.state()));
    ASSERT(process.state() == Dead);
    g_processes->remove(&process);
    delete &process;
    return exit_status;
}

pid_t Process::sys$waitpid(pid_t waitee, int* wstatus, int options)
{
    dbgprintf("sys$waitpid(%d, %p, %d)\n", waitee, wstatus, options);
    // FIXME: Respect options
    (void) options;
    if (wstatus)
        if (!validate_write_typed(wstatus))
            return -EFAULT;

    int dummy_wstatus;
    int& exit_status = wstatus ? *wstatus : dummy_wstatus;

    {
        InterruptDisabler disabler;
        if (waitee != -1 && !Process::from_pid(waitee))
            return -ECHILD;
    }

    if (options & WNOHANG) {
        if (waitee == -1) {
            pid_t reaped_pid = 0;
            InterruptDisabler disabler;
            for_each_child([&reaped_pid, &exit_status] (Process& process) {
                if (process.state() == Dead) {
                    reaped_pid = process.pid();
                    exit_status = reap(process);
                }
                return true;
            });
            return reaped_pid;
        } else {
            auto* waitee_process = Process::from_pid(waitee);
            if (!waitee_process)
                return -ECHILD;
            if (waitee_process->state() == Dead) {
                exit_status = reap(*waitee_process);
                return waitee;
            }
            return 0;
        }
    }

    m_waitee_pid = waitee;
    block(BlockedWait);
    sched_yield();
    if (m_was_interrupted_while_blocked)
        return -EINTR;
    Process* waitee_process;
    {
        InterruptDisabler disabler;
        // NOTE: If waitee was -1, m_waitee will have been filled in by the scheduler.
        waitee_process = Process::from_pid(m_waitee_pid);
    }
    ASSERT(waitee_process);
    exit_status = reap(*waitee_process);
    return m_waitee_pid;
}

void Process::unblock()
{
    if (current == this) {
        system.nblocked--;
        m_state = Process::Running;
        return;
    }
    ASSERT(m_state != Process::Runnable && m_state != Process::Running);
    system.nblocked--;
    m_state = Process::Runnable;
}

void Process::block(Process::State new_state)
{
    if (state() != Process::Running) {
        kprintf("Process::block: %s(%u) block(%u/%s) with state=%u/%s\n", name().characters(), pid(), new_state, toString(new_state), state(), toString(state()));
    }
    ASSERT(state() == Process::Running);
    system.nblocked++;
    m_was_interrupted_while_blocked = false;
    set_state(new_state);
}

void block(Process::State state)
{
    current->block(state);
    sched_yield();
}

void sleep(dword ticks)
{
    ASSERT(current->state() == Process::Running);
    current->setWakeupTime(system.uptime + ticks);
    current->block(Process::BlockedSleep);
    sched_yield();
}

static bool is_inside_kernel_code(LinearAddress laddr)
{
    // FIXME: What if we're indexing into the ksym with the highest address though?
    return laddr.get() >= ksym_lowest_address && laddr.get() <= ksym_highest_address;
}

bool Process::validate_read_from_kernel(LinearAddress laddr) const
{
    // We check extra carefully here since the first 4MB of the address space is identity-mapped.
    // This code allows access outside of the known used address ranges to get caught.

    InterruptDisabler disabler;
    if (is_inside_kernel_code(laddr))
        return true;
    if (is_kmalloc_address(laddr.as_ptr()))
        return true;
    return validate_read(laddr.as_ptr(), 1);
}

bool Process::validate_read(const void* address, size_t size) const
{
    if (isRing0()) {
        if (is_inside_kernel_code(LinearAddress((dword)address)))
            return true;
        if (is_kmalloc_address(address))
            return true;
    }
    ASSERT(size);
    if (!size)
        return false;
    LinearAddress first_address((dword)address);
    LinearAddress last_address = first_address.offset(size - 1);
    if (first_address.page_base() != last_address.page_base()) {
        if (!MM.validate_user_read(*this, last_address))
            return false;
    }
    return MM.validate_user_read(*this, first_address);
}

bool Process::validate_write(void* address, size_t size) const
{
    if (isRing0()) {
        if (is_kmalloc_address(address))
            return true;
    }
    ASSERT(size);
    if (!size)
        return false;
    LinearAddress first_address((dword)address);
    LinearAddress last_address = first_address.offset(size - 1);
    if (first_address.page_base() != last_address.page_base()) {
        if (!MM.validate_user_write(*this, last_address))
            return false;
    }
    return MM.validate_user_write(*this, last_address);
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
    Process::for_each_in_pgrp(pid(), [&] (auto&) {
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

int Process::sys$ioctl(int fd, unsigned request, unsigned arg)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_character_device())
        return -ENOTTY;
    return descriptor->character_device()->ioctl(*this, request, arg);
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
    for (; new_fd < (int)m_max_open_file_descriptors; ++new_fd) {
        if (!m_fds[new_fd])
            break;
    }
    m_fds[new_fd].set(descriptor);
    return new_fd;
}

int Process::sys$dup2(int old_fd, int new_fd)
{
    auto* descriptor = file_descriptor(old_fd);
    if (!descriptor)
        return -EBADF;
    if (number_of_open_file_descriptors() == m_max_open_file_descriptors)
        return -EMFILE;
    m_fds[new_fd].set(descriptor);
    return new_fd;
}

int Process::sys$sigprocmask(int how, const sigset_t* set, sigset_t* old_set)
{
    if (old_set) {
        if (!validate_read_typed(old_set))
            return -EFAULT;
        *old_set = m_signal_mask;
    }
    if (set) {
        if (!validate_read_typed(set))
            return -EFAULT;
        switch (how) {
        case SIG_BLOCK:
            m_signal_mask &= ~(*set);
            break;
        case SIG_UNBLOCK:
            m_signal_mask |= *set;
            break;
        case SIG_SETMASK:
            m_signal_mask = *set;
            break;
        default:
            return -EINVAL;
        }
    }
    return 0;
}

int Process::sys$sigpending(sigset_t* set)
{
    if (!validate_read_typed(set))
        return -EFAULT;
    *set = m_pending_signals;
    return 0;
}

int Process::sys$sigaction(int signum, const sigaction* act, sigaction* old_act)
{
    if (signum < 1 || signum >= 32 || signum == SIGKILL || signum == SIGSTOP)
        return -EINVAL;
    if (!validate_read_typed(act))
        return -EFAULT;
    InterruptDisabler disabler; // FIXME: This should use a narrower lock.
    auto& action = m_signal_action_data[signum];
    if (old_act) {
        if (!validate_write_typed(old_act))
            return -EFAULT;
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
    if (count != (int)m_gids.size())
        return -EINVAL;
    if (!validate_write_typed(gids, m_gids.size()))
        return -EFAULT;
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
    if (!validate_read(gids, count))
        return -EFAULT;
    m_gids.clear();
    m_gids.set(m_gid);
    for (size_t i = 0; i < count; ++i)
        m_gids.set(gids[i]);
    return 0;
}

int Process::sys$mkdir(const char* pathname, mode_t mode)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    if (strlen(pathname) >= 255)
        return -ENAMETOOLONG;
    int error;
    if (!VFS::the().mkdir(pathname, mode, cwd_inode()->identifier(), error))
        return error;
    return 0;
}

clock_t Process::sys$times(tms* times)
{
    if (!validate_write_typed(times))
        return -EFAULT;
    times->tms_utime = m_ticks_in_user;
    times->tms_stime = m_ticks_in_kernel;
    times->tms_cutime = m_ticks_in_user_for_dead_children;
    times->tms_cstime = m_ticks_in_kernel_for_dead_children;
    return 0;
}

struct vbe_info_structure {
    char signature[4];	// must be "VESA" to indicate valid VBE support
    word version;			// VBE version; high byte is major version, low byte is minor version
    dword oem;			// segment:offset pointer to OEM
    dword capabilities;		// bitfield that describes card capabilities
    dword video_modes;		// segment:offset pointer to list of supported video modes
    word video_memory;		// amount of video memory in 64KB blocks
    word software_rev;		// software revision
    dword vendor;			// segment:offset to card vendor string
    dword product_name;		// segment:offset to card model name
    dword product_rev;		// segment:offset pointer to product revision
    char reserved[222];		// reserved for future expansion
    char oem_data[256];		// OEM BIOSes store their strings in this area
} __attribute__ ((packed));

struct vbe_mode_info_structure {
    word attributes;		// deprecated, only bit 7 should be of interest to you, and it indicates the mode supports a linear frame buffer.
    byte window_a;			// deprecated
    byte window_b;			// deprecated
    word granularity;		// deprecated; used while calculating bank numbers
    word window_size;
    word segment_a;
    word segment_b;
    dword win_func_ptr;		// deprecated; used to switch banks from protected mode without returning to real mode
    word pitch;			// number of bytes per horizontal line
    word width;			// width in pixels
    word height;			// height in pixels
    byte w_char;			// unused...
    byte y_char;			// ...
    byte planes;
    byte bpp;			// bits per pixel in this mode
    byte banks;			// deprecated; total number of banks in this mode
    byte memory_model;
    byte bank_size;		// deprecated; size of a bank, almost always 64 KB but may be 16 KB...
    byte image_pages;
    byte reserved0;

    byte red_mask;
    byte red_position;
    byte green_mask;
    byte green_position;
    byte blue_mask;
    byte blue_position;
    byte reserved_mask;
    byte reserved_position;
    byte direct_color_attributes;

    dword framebuffer;		// physical address of the linear frame buffer; write here to draw to the screen
    dword off_screen_mem_off;
    word off_screen_mem_size;	// size of memory in the framebuffer but not being displayed on the screen
    byte reserved1[206];
} __attribute__ ((packed));

DisplayInfo Process::get_display_info()
{
    DisplayInfo info;
    //auto* vinfo = reinterpret_cast<vbe_info_structure*>(0xc000);
    auto* vmode = reinterpret_cast<vbe_mode_info_structure*>(0x2000);
    dbgprintf("VESA framebuffer, %ux%u, %u bpp @ P%x\n", vmode->width, vmode->height, vmode->bpp, vmode->framebuffer);
    dbgprintf("Returning display info in %s<%u>\n", name().characters(), pid());
    info.width = vmode->width;
    info.height = vmode->height;
    info.bpp = vmode->bpp;
    info.pitch = vmode->pitch;
    size_t framebuffer_size = info.pitch * info.height;
    if (!m_display_framebuffer_region) {
        auto framebuffer_vmo = VMObject::create_framebuffer_wrapper(PhysicalAddress(vmode->framebuffer), framebuffer_size);
        m_display_framebuffer_region = allocate_region_with_vmo(LinearAddress(0xe0000000), framebuffer_size, move(framebuffer_vmo), 0, "framebuffer", true, true);
    }
    info.framebuffer = m_display_framebuffer_region->laddr().as_ptr();
    return info;
}

int Process::sys$select(const Syscall::SC_select_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;
    if (params->writefds && !validate_read_typed(params->writefds))
        return -EFAULT;
    if (params->readfds && !validate_read_typed(params->readfds))
        return -EFAULT;
    if (params->exceptfds && !validate_read_typed(params->exceptfds))
        return -EFAULT;
    if (params->timeout && !validate_read_typed(params->timeout))
        return -EFAULT;
    int nfds = params->nfds;
    fd_set* writefds = params->writefds;
    fd_set* readfds = params->readfds;
    fd_set* exceptfds = params->exceptfds;
    auto* timeout = params->timeout;

    // FIXME: Implement exceptfds support.
    ASSERT(!exceptfds);

    // FIXME: Implement timeout support.
    ASSERT(!timeout || (!timeout->tv_sec && !timeout->tv_usec));

    if (nfds < 0)
        return -EINVAL;

    // FIXME: Return -EINTR if a signal is caught.
    // FIXME: Return -EINVAL if timeout is invalid.

    auto transfer_fds = [this, nfds] (fd_set* set, auto& vector) -> int {
        if (!set)
            return 0;
        vector.clear_with_capacity();
        auto bitmap = Bitmap::wrap((byte*)set, FD_SETSIZE);
        for (int i = 0; i < nfds; ++i) {
            if (bitmap.get(i)) {
                if (!file_descriptor(i))
                    return -EBADF;
                vector.append(i);
            }
        }
        return 0;
    };

    int error = 0;
    error = transfer_fds(writefds, m_select_write_fds);
    if (error)
        return error;
    error = transfer_fds(readfds, m_select_read_fds);
    if (error)
        return error;

#ifdef DEBUG_IO
    dbgprintf("%s<%u> selecting on (read:%u, write:%u), wakeup_req:%u, timeout=%p\n", name().characters(), pid(), m_select_read_fds.size(), m_select_write_fds.size(), m_wakeup_requested, timeout);
#endif

    if (!m_wakeup_requested && (!timeout || (timeout->tv_sec || timeout->tv_usec))) {
        block(BlockedSelect);
        Scheduler::yield();
    }
    m_wakeup_requested = false;

    int markedfds = 0;

    if (readfds) {
        memset(readfds, 0, sizeof(fd_set));
        auto bitmap = Bitmap::wrap((byte*)readfds, FD_SETSIZE);
        for (int fd : m_select_read_fds) {
            auto* descriptor = file_descriptor(fd);
            if (!descriptor)
                continue;
            if (descriptor->can_read(*this)) {
                bitmap.set(fd, true);
                ++markedfds;
            }
        }
    }

    if (writefds) {
        memset(writefds, 0, sizeof(fd_set));
        auto bitmap = Bitmap::wrap((byte*)writefds, FD_SETSIZE);
        for (int fd : m_select_write_fds) {
            auto* descriptor = file_descriptor(fd);
            if (!descriptor)
                continue;
            if (descriptor->can_write(*this)) {
                bitmap.set(fd, true);
                ++markedfds;
            }
        }
    }

    return markedfds;
}

int Process::sys$poll(pollfd* fds, int nfds, int timeout)
{
    if (!validate_read_typed(fds))
        return -EFAULT;

    m_select_write_fds.clear_with_capacity();
    m_select_read_fds.clear_with_capacity();
    for (int i = 0; i < nfds; ++i) {
        if (fds[i].events & POLLIN)
            m_select_read_fds.append(fds[i].fd);
        if (fds[i].events & POLLOUT)
            m_select_write_fds.append(fds[i].fd);
    }

    if (!m_wakeup_requested && timeout < 0) {
        block(BlockedSelect);
        Scheduler::yield();
    }
    m_wakeup_requested = false;

    int fds_with_revents = 0;

    for (int i = 0; i < nfds; ++i) {
        auto* descriptor = file_descriptor(fds[i].fd);
        if (!descriptor) {
            fds[i].revents = POLLNVAL;
            continue;
        }
        fds[i].revents = 0;
        if (fds[i].events & POLLIN && descriptor->can_read(*this))
            fds[i].revents |= POLLIN;
        if (fds[i].events & POLLOUT && descriptor->can_write(*this))
            fds[i].revents |= POLLOUT;

        if (fds[i].revents)
            ++fds_with_revents;
    }

    return fds_with_revents;
}

Inode* Process::cwd_inode()
{
    // FIXME: This is retarded factoring.
    if (!m_cwd)
        m_cwd = VFS::the().root_inode();
    return m_cwd.ptr();
}

int Process::sys$unlink(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    int error;
    if (!VFS::the().unlink(String(pathname), *cwd_inode(), error))
        return error;
    return 0;
}

int Process::sys$rmdir(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    int error;
    if (!VFS::the().rmdir(String(pathname), *cwd_inode(), error))
        return error;
    return 0;
}

int Process::sys$read_tsc(dword* lsw, dword* msw)
{
    if (!validate_write_typed(lsw))
        return -EFAULT;
    if (!validate_write_typed(msw))
        return -EFAULT;
    read_tsc(*lsw, *msw);
    return 0;
}

int Process::sys$chmod(const char* pathname, mode_t mode)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    int error;
    if (!VFS::the().chmod(String(pathname), mode, *cwd_inode(), error))
        return error;
    return 0;
}

void Process::die()
{
    set_state(Dead);
    m_fds.clear();
    destroy_all_windows();
}
