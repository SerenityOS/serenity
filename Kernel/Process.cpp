#include "types.h"
#include "Process.h"
#include "kmalloc.h"
#include "StdLib.h"
#include "i386.h"
#include "system.h"
#include <Kernel/FileDescriptor.h>
#include <Kernel/VirtualFileSystem.h>
#include <Kernel/NullDevice.h>
#include "ELFLoader.h"
#include "MemoryManager.h"
#include "i8253.h"
#include "RTC.h"
#include <AK/StdLibExtras.h>
#include <LibC/signal_numbers.h>
#include <LibC/errno_numbers.h>
#include "Syscall.h"
#include "Scheduler.h"
#include "FIFO.h"
#include "KSyms.h"
#include <Kernel/Socket.h>
#include "MasterPTY.h"
#include "elf.h"
#include <AK/StringBuilder.h>
#include <Kernel/E1000NetworkAdapter.h>
#include <Kernel/EthernetFrameHeader.h>
#include <Kernel/ARP.h>

//#define DEBUG_IO
//#define TASK_DEBUG
//#define FORK_DEBUG
//#define SIGNAL_DEBUG
#define MAX_PROCESS_GIDS 32
//#define SHARED_BUFFER_DEBUG

static const dword default_kernel_stack_size = 16384;
static const dword default_userspace_stack_size = 65536;

static pid_t next_pid;
InlineLinkedList<Process>* g_processes;
static String* s_hostname;
static Lock* s_hostname_lock;

CoolGlobals* g_cool_globals;

void Process::initialize()
{
#ifdef COOL_GLOBALS
    g_cool_globals = reinterpret_cast<CoolGlobals*>(0x1000);
#endif
    next_pid = 0;
    g_processes = new InlineLinkedList<Process>;
    s_hostname = new String("courage");
    s_hostname_lock = new Lock;
    Scheduler::initialize();
}

Vector<pid_t> Process::all_pids()
{
    Vector<pid_t> pids;
    pids.ensure_capacity(system.nprocess);
    InterruptDisabler disabler;
    for (auto* process = g_processes->head(); process; process = process->next())
        pids.append(process->pid());
    return pids;
}

Vector<Process*> Process::all_processes()
{
    Vector<Process*> processes;
    processes.ensure_capacity(system.nprocess);
    InterruptDisabler disabler;
    for (auto* process = g_processes->head(); process; process = process->next())
        processes.append(process);
    return processes;
}

bool Process::in_group(gid_t gid) const
{
    return m_gids.contains(gid);
}

Region* Process::allocate_region(LinearAddress laddr, size_t size, String&& name, bool is_readable, bool is_writable, bool commit)
{
    size = PAGE_ROUND_UP(size);
    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_next_region;
        m_next_region = m_next_region.offset(size).offset(PAGE_SIZE);
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
        laddr = m_next_region;
        m_next_region = m_next_region.offset(size).offset(PAGE_SIZE);
    }
    laddr.mask(0xfffff000);
    m_regions.append(adopt(*new Region(laddr, size, move(inode), move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    return m_regions.last().ptr();
}

Region* Process::allocate_region_with_vmo(LinearAddress laddr, size_t size, Retained<VMObject>&& vmo, size_t offset_in_vmo, String&& name, bool is_readable, bool is_writable)
{
    size = PAGE_ROUND_UP(size);
    // FIXME: This needs sanity checks. What if this overlaps existing regions?
    if (laddr.is_null()) {
        laddr = m_next_region;
        m_next_region = m_next_region.offset(size).offset(PAGE_SIZE);
    }
    laddr.mask(0xfffff000);
    offset_in_vmo &= PAGE_MASK;
    size = ceil_div(size, PAGE_SIZE) * PAGE_SIZE;
    m_regions.append(adopt(*new Region(laddr, size, move(vmo), offset_in_vmo, move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    return m_regions.last().ptr();
}

bool Process::deallocate_region(Region& region)
{
    InterruptDisabler disabler;
    for (int i = 0; i < m_regions.size(); ++i) {
        if (m_regions[i].ptr() == &region) {
            MM.unmap_region(region);
            m_regions.remove(i);
            return true;
        }
    }
    return false;
}

Region* Process::region_from_range(LinearAddress laddr, size_t size)
{
    size = PAGE_ROUND_UP(size);
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
    auto* region = region_from_range(LinearAddress((dword)addr), size);
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
    if ((dword)addr & ~PAGE_MASK)
        return (void*)-EINVAL;
    if (flags & MAP_ANONYMOUS) {
        auto* region = allocate_region(LinearAddress((dword)addr), size, "mmap", prot & PROT_READ, prot & PROT_WRITE, false);
        if (!region)
            return (void*)-ENOMEM;
        if (flags & MAP_SHARED)
            region->set_shared(true);
        return region->laddr().as_ptr();
    }
    if (offset & ~PAGE_MASK)
        return (void*)-EINVAL;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return (void*)-EBADF;
    if (!descriptor->supports_mmap())
        return (void*)-ENODEV;
    auto* region = descriptor->mmap(*this, LinearAddress((dword)addr), offset, size, prot);
    if (!region)
        return (void*)-ENOMEM;
    if (flags & MAP_SHARED)
        region->set_shared(true);
    return region->laddr().as_ptr();
}

int Process::sys$munmap(void* addr, size_t size)
{
    auto* region = region_from_range(LinearAddress((dword)addr), size);
    if (!region)
        return -EINVAL;
    if (!deallocate_region(*region))
        return -EINVAL;
    return 0;
}

int Process::sys$gethostname(char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    LOCKER(*s_hostname_lock);
    if (size < (s_hostname->length() + 1))
        return -ENAMETOOLONG;
    strcpy(buffer, s_hostname->characters());
    return 0;
}

Process* Process::fork(RegisterDump& regs)
{
    auto* child = new Process(String(m_name), m_uid, m_gid, m_pid, m_ring, m_cwd.copy_ref(), m_executable.copy_ref(), m_tty, this);
    if (!child)
        return nullptr;

    memcpy(child->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    child->m_signal_mask = m_signal_mask;
#ifdef FORK_DEBUG
    dbgprintf("fork: child=%p\n", child);
#endif

    for (auto& region : m_regions) {
#ifdef FORK_DEBUG
        dbgprintf("fork: cloning Region{%p} \"%s\" L%x\n", region.ptr(), region->name().characters(), region->laddr().get());
#endif
        auto cloned_region = region->clone();
        child->m_regions.append(move(cloned_region));
        MM.map_region(*child, *child->m_regions.last());
        if (region.ptr() == m_display_framebuffer_region.ptr())
            child->m_display_framebuffer_region = child->m_regions.last().copy_ref();
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

int Process::do_exec(String path, Vector<String> arguments, Vector<String> environment)
{
    ASSERT(is_ring3());

    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    auto result = VFS::the().open(path, 0, 0, cwd_inode());
    if (result.is_error())
        return result.error();
    auto descriptor = result.value();

    if (!descriptor->metadata().may_execute(m_euid, m_gids))
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

    auto vmo = VMObject::create_file_backed(descriptor->inode());
#if 0
    // FIXME: I would like to do this, but it would instantiate all the damn inodes.
    vmo->set_name(descriptor->absolute_path());
#else
    vmo->set_name("ELF image");
#endif
    RetainPtr<Region> region = allocate_region_with_vmo(LinearAddress(), descriptor->metadata().size, vmo.copy_ref(), 0, "executable", true, false);

    // FIXME: Should we consider doing on-demand paging here? Is it actually useful?
    bool success = region->page_in();

    ASSERT(success);
    {
        // Okay, here comes the sleight of hand, pay close attention..
        auto old_regions = move(m_regions);
        ELFLoader loader(region->laddr().as_ptr());
        loader.map_section_hook = [&] (LinearAddress laddr, size_t size, size_t alignment, size_t offset_in_image, bool is_readable, bool is_writable, const String& name) {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            size = ((size / 4096) + 1) * 4096; // FIXME: Use ceil_div?
            (void) allocate_region_with_vmo(laddr, size, vmo.copy_ref(), offset_in_image, String(name), is_readable, is_writable);
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

    kfree(m_kernel_stack_for_signal_handler);
    m_kernel_stack_for_signal_handler = nullptr;
    m_signal_stack_user_region = nullptr;
    m_display_framebuffer_region = nullptr;
    set_default_signal_dispositions();
    m_signal_mask = 0;
    m_pending_signals = 0;

    for (int i = 0; i < m_fds.size(); ++i) {
        auto& daf = m_fds[i];
        if (daf.descriptor && daf.flags & FD_CLOEXEC) {
            daf.descriptor->close();
            daf = { };
        }
    }

    // We cli() manually here because we don't want to get interrupted between do_exec() and Schedule::yield().
    // The reason is that the task redirection we've set up above will be clobbered by the timer IRQ.
    // If we used an InterruptDisabler that sti()'d on exit, we might timer tick'd too soon in exec().
    if (current == this)
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
    make_userspace_stack(move(arguments), move(environment));
    m_tss.ss0 = 0x10;
    m_tss.esp0 = old_esp0;
    m_tss.ss2 = m_pid;

    m_executable = descriptor->inode();

    if (descriptor->metadata().is_setuid())
        m_euid = descriptor->metadata().uid;
    if (descriptor->metadata().is_setgid())
        m_egid = descriptor->metadata().gid;

#ifdef TASK_DEBUG
    kprintf("Process %u (%s) exec'd %s @ %p\n", pid(), name().characters(), path.characters(), m_tss.eip);
#endif

    set_state(Skip1SchedulerPass);
    return 0;
}

void Process::make_userspace_stack(Vector<String> arguments, Vector<String> environment)
{
    auto* region = allocate_region(LinearAddress(), default_userspace_stack_size, "stack");
    ASSERT(region);
    m_stack_top3 = region->laddr().offset(default_userspace_stack_size).get();
    m_tss.esp = m_stack_top3;

    char* stack_base = (char*)region->laddr().get();
    int argc = arguments.size();
    char** argv = (char**)stack_base;
    char** env = argv + arguments.size() + 1;
    char* bufptr = stack_base + (sizeof(char*) * (arguments.size() + 1)) + (sizeof(char*) * (environment.size() + 1));

    size_t total_blob_size = 0;
    for (auto& a : arguments)
        total_blob_size += a.length() + 1;
    for (auto& e : environment)
        total_blob_size += e.length() + 1;

    size_t total_meta_size = sizeof(char*) * (arguments.size() + 1) + sizeof(char*) * (environment.size() + 1);

    // FIXME: It would be better if this didn't make us panic.
    ASSERT((total_blob_size + total_meta_size) < default_userspace_stack_size);

    for (int i = 0; i < arguments.size(); ++i) {
        argv[i] = bufptr;
        memcpy(bufptr, arguments[i].characters(), arguments[i].length());
        bufptr += arguments[i].length();
        *(bufptr++) = '\0';
    }
    argv[arguments.size()] = nullptr;

    for (int i = 0; i < environment.size(); ++i) {
        env[i] = bufptr;
        memcpy(bufptr, environment[i].characters(), environment[i].length());
        bufptr += environment[i].length();
        *(bufptr++) = '\0';
    }
    env[environment.size()] = nullptr;

    // NOTE: The stack needs to be 16-byte aligned.
    push_value_on_stack((dword)env);
    push_value_on_stack((dword)argv);
    push_value_on_stack((dword)argc);
    push_value_on_stack(0);
}

int Process::exec(String path, Vector<String> arguments, Vector<String> environment)
{
    // The bulk of exec() is done by do_exec(), which ensures that all locals
    // are cleaned up by the time we yield-teleport below.
    int rc = do_exec(move(path), move(arguments), move(environment));
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
    // NOTE: Be extremely careful with allocating any kernel memory in exec().
    //       On success, the kernel stack will be lost.
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
    Vector<String> arguments;
    Vector<String> environment;
    {
        auto parts = path.split('/');
        if (argv) {
            for (size_t i = 0; argv[i]; ++i) {
                arguments.append(argv[i]);
            }
        } else {
            arguments.append(parts.last());
        }

        if (envp) {
            for (size_t i = 0; envp[i]; ++i)
                environment.append(envp[i]);
        }
    }

    int rc = exec(move(path), move(arguments), move(environment));
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
            cwd = parent->m_cwd.copy_ref();
    }

    if (!cwd)
        cwd = VFS::the().root_inode();

    auto* process = new Process(parts.take_last(), uid, gid, parent_pid, Ring3, move(cwd), nullptr, tty);

    error = process->exec(path, move(arguments), move(environment));
    if (error != 0) {
        delete process;
        return nullptr;
    }

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
    set_default_signal_dispositions();

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
        for (int i = 0; i < fork_parent->m_fds.size(); ++i) {
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
        auto& device_to_use_as_tty = tty ? (CharacterDevice&)*tty : NullDevice::the();
        m_fds[0].set(*device_to_use_as_tty.open(O_RDONLY).value());
        m_fds[1].set(*device_to_use_as_tty.open(O_WRONLY).value());
        m_fds[2].set(*device_to_use_as_tty.open(O_WRONLY).value());
    }

    if (fork_parent)
        m_next_region = fork_parent->m_next_region;
    else
        m_next_region = LinearAddress(0x10000000);

    if (fork_parent) {
        memcpy(&m_tss, &fork_parent->m_tss, sizeof(m_tss));
    } else {
        memset(&m_tss, 0, sizeof(m_tss));

        // Only IF is set when a process boots.
        m_tss.eflags = 0x0202;
        word cs, ds, ss;

        if (is_ring0()) {
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

    if (is_ring0()) {
        // FIXME: This memory is leaked.
        // But uh, there's also no kernel process termination, so I guess it's not technically leaked...
        dword stack_bottom = (dword)kmalloc_eternal(default_kernel_stack_size);
        m_stack_top0 = (stack_bottom + default_kernel_stack_size) & 0xffffff8;
        m_tss.esp = m_stack_top0;
    } else {
        // Ring3 processes need a separate stack for Ring0.
        m_kernel_stack = kmalloc(default_kernel_stack_size);
        m_stack_top0 = ((dword)m_kernel_stack + default_kernel_stack_size) & 0xffffff8;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_stack_top0;
    }

    if (fork_parent) {
        m_sid = fork_parent->m_sid;
        m_pgid = fork_parent->m_pgid;
        m_umask = fork_parent->m_umask;
    }

    // HACK: Ring2 SS in the TSS is the current PID.
    m_tss.ss2 = m_pid;
    m_far_ptr.offset = 0x98765432;
}

Process::~Process()
{
    {
        InterruptDisabler disabler;
        system.nprocess--;
    }

    if (g_last_fpu_process == this)
        g_last_fpu_process = nullptr;

    if (selector())
        gdt_free_entry(selector());

    if (m_kernel_stack) {
        kfree(m_kernel_stack);
        m_kernel_stack = nullptr;
    }

    if (m_kernel_stack_for_signal_handler) {
        kfree(m_kernel_stack_for_signal_handler);
        m_kernel_stack_for_signal_handler = nullptr;
    }
}

void Process::dump_regions()
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

    m_termination_status = status;
    m_termination_signal = 0;
    die();
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
    ASSERT(signal < 32);

    if (sender)
        dbgprintf("signal: %s(%u) sent %d to %s(%u)\n", sender->name().characters(), sender->pid(), signal, name().characters(), pid());
    else
        dbgprintf("signal: kernel sent %d to %s(%u)\n", signal, name().characters(), pid());

    InterruptDisabler disabler;
    m_pending_signals |= 1 << signal;
}

bool Process::has_unmasked_pending_signals() const
{
    return m_pending_signals & ~m_signal_mask;
}

ShouldUnblockProcess Process::dispatch_one_pending_signal()
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
    return dispatch_signal(signal);
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

DefaultSignalAction default_signal_action(byte signal)
{
    ASSERT(signal && signal < NSIG);

    switch (signal) {
    case SIGHUP:
    case SIGINT:
    case SIGKILL:
    case SIGPIPE:
    case SIGALRM:
    case SIGUSR1:
    case SIGUSR2:
    case SIGVTALRM:
    case SIGSTKFLT:
    case SIGIO:
    case SIGPROF:
    case SIGTERM:
    case SIGPWR:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
        return DefaultSignalAction::Ignore;
    case SIGQUIT:
    case SIGILL:
    case SIGTRAP:
    case SIGABRT:
    case SIGBUS:
    case SIGFPE:
    case SIGSEGV:
    case SIGXCPU:
    case SIGXFSZ:
    case SIGSYS:
        return DefaultSignalAction::DumpCore;
    case SIGCONT:
        return DefaultSignalAction::Continue;
    case SIGSTOP:
    case SIGTSTP:
    case SIGTTIN:
    case SIGTTOU:
        return DefaultSignalAction::Stop;
    }
    ASSERT_NOT_REACHED();
}

ShouldUnblockProcess Process::dispatch_signal(byte signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);

#ifdef SIGNAL_DEBUG
    kprintf("dispatch_signal %s(%u) <- %u\n", name().characters(), pid(), signal);
#endif

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << signal);

    if (signal == SIGSTOP) {
        set_state(Stopped);
        return ShouldUnblockProcess::No;
    }

    if (signal == SIGCONT && state() == Stopped)
        set_state(Runnable);

    auto handler_laddr = action.handler_or_sigaction;
    if (handler_laddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            set_state(Stopped);
            return ShouldUnblockProcess::No;
        case DefaultSignalAction::DumpCore:
        case DefaultSignalAction::Terminate:
            terminate_due_to_signal(signal);
            return ShouldUnblockProcess::No;
        case DefaultSignalAction::Ignore:
            return ShouldUnblockProcess::No;
        case DefaultSignalAction::Continue:
            return ShouldUnblockProcess::Yes;
        }
        ASSERT_NOT_REACHED();
    }

    if (handler_laddr.as_ptr() == SIG_IGN) {
#ifdef SIGNAL_DEBUG
        kprintf("%s(%u) ignored signal %u\n", name().characters(), pid(), signal);
#endif
        return ShouldUnblockProcess::Yes;
    }

    dword old_signal_mask = m_signal_mask;
    dword new_signal_mask = action.mask;
    if (action.flags & SA_NODEFER)
        new_signal_mask &= ~(1 << signal);
    else
        new_signal_mask |= 1 << signal;

    m_signal_mask |= new_signal_mask;

    Scheduler::prepare_to_modify_tss(*this);

    word ret_cs = m_tss.cs;
    dword ret_eip = m_tss.eip;
    dword ret_eflags = m_tss.eflags;
    bool interrupting_in_kernel = (ret_cs & 3) == 0;

    ProcessPagingScope paging_scope(*this);
    create_signal_trampolines_if_needed();

    if (interrupting_in_kernel) {
#ifdef SIGNAL_DEBUG
        kprintf("dispatch_signal to %s(%u) in state=%s with return to %w:%x\n", name().characters(), pid(), to_string(state()), ret_cs, ret_eip);
#endif
        ASSERT(is_blocked());
        m_tss_to_resume_kernel = m_tss;
#ifdef SIGNAL_DEBUG
        kprintf("resume tss pc: %w:%x stack: %w:%x flags: %x cr3: %x\n", m_tss_to_resume_kernel.cs, m_tss_to_resume_kernel.eip, m_tss_to_resume_kernel.ss, m_tss_to_resume_kernel.esp, m_tss_to_resume_kernel.eflags, m_tss_to_resume_kernel.cr3);
#endif

        if (!m_signal_stack_user_region) {
            m_signal_stack_user_region = allocate_region(LinearAddress(), default_userspace_stack_size, "Signal stack (user)");
            ASSERT(m_signal_stack_user_region);
        }
        if (!m_kernel_stack_for_signal_handler) {
            m_kernel_stack_for_signal_handler = kmalloc(default_kernel_stack_size);
            ASSERT(m_kernel_stack_for_signal_handler);
        }
        m_tss.ss = 0x23;
        m_tss.esp = m_signal_stack_user_region->laddr().offset(default_userspace_stack_size).get();
        m_tss.ss0 = 0x10;
        m_tss.esp0 = (dword)m_kernel_stack_for_signal_handler + default_kernel_stack_size;

        push_value_on_stack(0);
    } else {
        push_value_on_stack(ret_eip);
        push_value_on_stack(ret_eflags);

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

        // Align the stack.
        m_tss.esp -= 12;
    }

    // PUSH old_signal_mask
    push_value_on_stack(old_signal_mask);

    m_tss.cs = 0x1b;
    m_tss.ds = 0x23;
    m_tss.es = 0x23;
    m_tss.fs = 0x23;
    m_tss.gs = 0x23;
    m_tss.eip = handler_laddr.get();

    // FIXME: Should we worry about the stack being 16 byte aligned when entering a signal handler?
    push_value_on_stack(signal);

    if (interrupting_in_kernel)
        push_value_on_stack(m_return_to_ring0_from_signal_trampoline.get());
    else
        push_value_on_stack(m_return_to_ring3_from_signal_trampoline.get());

    ASSERT((m_tss.esp % 16) == 0);

    // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
    set_state(Skip1SchedulerPass);

#ifdef SIGNAL_DEBUG
    kprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", name().characters(), pid(), to_string(state()), m_tss.cs, m_tss.eip);
#endif
    return ShouldUnblockProcess::Yes;
}

void Process::create_signal_trampolines_if_needed()
{
    if (!m_return_to_ring3_from_signal_trampoline.is_null())
        return;
    // FIXME: This should be a global trampoline shared by all processes, not one created per process!
    // FIXME: Remap as read-only after setup.
    auto* region = allocate_region(LinearAddress(), PAGE_SIZE, "Signal trampolines", true, true);
    m_return_to_ring3_from_signal_trampoline = region->laddr();
    byte* code_ptr = m_return_to_ring3_from_signal_trampoline.as_ptr();
    *code_ptr++ = 0x58; // pop eax (Argument to signal handler (ignored here))
    *code_ptr++ = 0x5a; // pop edx (Original signal mask to restore)
    *code_ptr++ = 0xb8; // mov eax, <dword>
    *(dword*)code_ptr = Syscall::SC_restore_signal_mask;
    code_ptr += sizeof(dword);
    *code_ptr++ = 0xcd; // int 0x82
    *code_ptr++ = 0x82;

    *code_ptr++ = 0x83; // add esp, (stack alignment padding)
    *code_ptr++ = 0xc4;
    *code_ptr++ = sizeof(dword) * 3;

    *code_ptr++ = 0x61; // popa
    *code_ptr++ = 0x9d; // popf
    *code_ptr++ = 0xc3; // ret
    *code_ptr++ = 0x0f; // ud2
    *code_ptr++ = 0x0b;

    m_return_to_ring0_from_signal_trampoline = LinearAddress((dword)code_ptr);
    *code_ptr++ = 0x58; // pop eax (Argument to signal handler (ignored here))
    *code_ptr++ = 0x5a; // pop edx (Original signal mask to restore)
    *code_ptr++ = 0xb8; // mov eax, <dword>
    *(dword*)code_ptr = Syscall::SC_restore_signal_mask;
    code_ptr += sizeof(dword);
    *code_ptr++ = 0xcd; // int 0x82
    // NOTE: Stack alignment padding doesn't matter when returning to ring0.
    //       Nothing matters really, as we're returning by replacing the entire TSS.
    *code_ptr++ = 0x82;
    *code_ptr++ = 0xb8; // mov eax, <dword>
    *(dword*)code_ptr = Syscall::SC_sigreturn;
    code_ptr += sizeof(dword);
    *code_ptr++ = 0xcd; // int 0x82
    *code_ptr++ = 0x82;
    *code_ptr++ = 0x0f; // ud2
    *code_ptr++ = 0x0b;
}

int Process::sys$restore_signal_mask(dword mask)
{
    m_signal_mask = mask;
    return 0;
}

void Process::sys$sigreturn()
{
    InterruptDisabler disabler;
    Scheduler::prepare_to_modify_tss(*this);
    m_tss = m_tss_to_resume_kernel;
#ifdef SIGNAL_DEBUG
    kprintf("sys$sigreturn in %s(%u)\n", name().characters(), pid());
    kprintf(" -> resuming execution at %w:%x stack %w:%x flags %x cr3 %x\n", m_tss.cs, m_tss.eip, m_tss.ss, m_tss.esp, m_tss.eflags, m_tss.cr3);
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
    dump_regions();
    ASSERT(is_ring3());
    die();
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
    if (fd < m_fds.size())
        return m_fds[fd].descriptor.ptr();
    return nullptr;
}

const FileDescriptor* Process::file_descriptor(int fd) const
{
    if (fd < 0)
        return nullptr;
    if (fd < m_fds.size())
        return m_fds[fd].descriptor.ptr();
    return nullptr;
}

ssize_t Process::sys$get_dir_entries(int fd, void* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
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

int Process::sys$ttyname_r(int fd, char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_tty())
        return -ENOTTY;
    auto tty_name = descriptor->tty()->tty_name();
    if (size < tty_name.length() + 1)
        return -ERANGE;
    strcpy(buffer, tty_name.characters());
    return 0;
}

int Process::sys$ptsname_r(int fd, char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
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

ssize_t Process::sys$write(int fd, const byte* data, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
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
    return nwritten;
}

ssize_t Process::sys$read(int fd, byte* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$read(%d, %p, %u)\n", name().characters(), pid(), fd, buffer, size);
#endif
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (descriptor->is_blocking()) {
        if (!descriptor->can_read(*this)) {
            m_blocked_fd = fd;
            block(BlockedRead);
            Scheduler::yield();
            if (m_was_interrupted_while_blocked)
                return -EINTR;
        }
    }
    return descriptor->read(*this, buffer, size);
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
    return VFS::the().utime(String(pathname), cwd_inode(), atime, mtime);
}

int Process::sys$access(const char* pathname, int mode)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().access(String(pathname), mode, cwd_inode());
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
        m_fds[new_fd].set(*descriptor);
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
    return descriptor->fstat(*statbuf);
}

int Process::sys$lstat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    return VFS::the().stat(String(path), O_NOFOLLOW_NOERROR, cwd_inode(), *statbuf);
}

int Process::sys$stat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    return VFS::the().stat(String(path), O_NOFOLLOW_NOERROR, cwd_inode(), *statbuf);
}

int Process::sys$readlink(const char* path, char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_read_str(path))
        return -EFAULT;
    if (!validate_write(buffer, size))
        return -EFAULT;

    auto result = VFS::the().open(path, O_RDONLY | O_NOFOLLOW_NOERROR, 0, cwd_inode());
    if (result.is_error())
        return result.error();
    auto descriptor = result.value();

    if (!descriptor->metadata().is_symlink())
        return -EINVAL;

    auto contents = descriptor->read_entire_file(*this);
    if (!contents)
        return -EIO; // FIXME: Get a more detailed error from VFS.

    memcpy(buffer, contents.pointer(), min(size, (ssize_t)contents.size()));
    if (contents.size() + 1 < size)
        buffer[contents.size()] = '\0';
    return 0;
}

int Process::sys$chdir(const char* path)
{
    if (!validate_read_str(path))
        return -EFAULT;
    auto directory_or_error = VFS::the().open_directory(String(path), cwd_inode());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    m_cwd = *directory_or_error.value();
    return 0;
}

int Process::sys$getcwd(char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto path_or_error = VFS::the().absolute_path(cwd_inode());
    if (path_or_error.is_error())
        return path_or_error.error();
    auto path = path_or_error.value();
    if (size < path.length() + 1)
        return -ERANGE;
    strcpy(buffer, path.characters());
    return 0;
}

int Process::number_of_open_file_descriptors() const
{
    int count = 0;
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

    auto result = VFS::the().open(path, options, mode & ~umask(), cwd_inode());
    if (result.is_error())
        return result.error();
    auto descriptor = result.value();
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

int Process::sys$setuid(uid_t uid)
{
    if (uid != m_uid && !is_superuser())
        return -EPERM;
    m_uid = uid;
    m_euid = uid;
    return 0;
}

int Process::sys$setgid(gid_t gid)
{
    if (gid != m_gid && !is_superuser())
        return -EPERM;
    m_gid = gid;
    m_egid = gid;
    return 0;
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
    LOCKER(*s_hostname_lock);
    strncpy(buf->nodename, s_hostname->characters(), sizeof(utsname::nodename));
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
    if (signal < 0 || signal >= 32)
        return -EINVAL;
    if (pid == 0) {
        // FIXME: Send to same-group processes.
        ASSERT(pid != 0);
    }
    if (pid == -1) {
        // FIXME: Send to all processes.
        ASSERT(pid != -1);
    }
    if (pid == m_pid) {
        send_signal(signal, this);
        Scheduler::yield();
        return 0;
    }
    InterruptDisabler disabler;
    auto* peer = Process::from_pid(pid);
    if (!peer)
        return -ESRCH;
    // FIXME: Allow sending SIGCONT to everyone in the process group.
    // FIXME: Should setuid processes have some special treatment here?
    if (!is_superuser() && m_euid != peer->m_uid && m_uid != peer->m_uid)
        return -EPERM;
    if (peer->is_ring0() && signal == SIGKILL) {
        kprintf("%s(%u) attempted to send SIGKILL to ring 0 process %s(%u)\n", name().characters(), m_pid, peer->name().characters(), peer->pid());
        return -EPERM;
    }
    peer->send_signal(signal, this);
    return 0;
}

int Process::sys$usleep(useconds_t usec)
{
    if (!usec)
        return 0;

    sleep(usec / 1000);
    if (m_wakeup_time > system.uptime) {
        ASSERT(m_was_interrupted_while_blocked);
        dword ticks_left_until_original_wakeup_time = m_wakeup_time - system.uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
    return 0;
}

int Process::sys$sleep(unsigned seconds)
{
    if (!seconds)
        return 0;
    sleep(seconds * TICKS_PER_SECOND);
    if (m_wakeup_time > system.uptime) {
        ASSERT(m_was_interrupted_while_blocked);
        dword ticks_left_until_original_wakeup_time = m_wakeup_time - system.uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
    return 0;
}

int Process::sys$gettimeofday(timeval* tv)
{
    if (!validate_write_typed(tv))
        return -EFAULT;
    auto now = RTC::now();
    tv->tv_sec = now;
    tv->tv_usec = PIT::ticks_since_boot() % 1000;
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
    m_umask = mask & 0777;
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

    dbgprintf("reap: %s(%u) {%s}\n", process.name().characters(), process.pid(), to_string(process.state()));
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
            ASSERT(waitee > 0); // FIXME: Implement other PID specs.
            InterruptDisabler disabler;
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
    Scheduler::yield();
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
        kprintf("Process::block: %s(%u) block(%u/%s) with state=%u/%s\n", name().characters(), pid(), new_state, to_string(new_state), state(), to_string(state()));
    }
    ASSERT(state() == Process::Running);
    system.nblocked++;
    m_was_interrupted_while_blocked = false;
    set_state(new_state);
}

void block(Process::State state)
{
    current->block(state);
    Scheduler::yield();
}

void sleep(dword ticks)
{
    ASSERT(current->state() == Process::Running);
    current->set_wakeup_time(system.uptime + ticks);
    current->block(Process::BlockedSleep);
    Scheduler::yield();
}

enum class KernelMemoryCheckResult {
    NotInsideKernelMemory,
    AccessGranted,
    AccessDenied
};

static KernelMemoryCheckResult check_kernel_memory_access(LinearAddress laddr, bool is_write)
{
    auto* kernel_elf_header = (Elf32_Ehdr*)0xf000;
    auto* kernel_program_headers = (Elf32_Phdr*)(0xf000 + kernel_elf_header->e_phoff);
    for (unsigned i = 0; i < kernel_elf_header->e_phnum; ++i) {
        auto& segment = kernel_program_headers[i];
        if (segment.p_type != PT_LOAD || !segment.p_vaddr || !segment.p_memsz)
            continue;
        if (laddr.get() < segment.p_vaddr || laddr.get() > (segment.p_vaddr + segment.p_memsz))
            continue;
        if (is_write && !(kernel_program_headers[i].p_flags & PF_W))
            return KernelMemoryCheckResult::AccessDenied;
        if (!is_write && !(kernel_program_headers[i].p_flags & PF_R))
            return KernelMemoryCheckResult::AccessDenied;
        return KernelMemoryCheckResult::AccessGranted;
    }
    return KernelMemoryCheckResult::NotInsideKernelMemory;
}

bool Process::validate_read_from_kernel(LinearAddress laddr) const
{
    // We check extra carefully here since the first 4MB of the address space is identity-mapped.
    // This code allows access outside of the known used address ranges to get caught.
    auto kmc_result = check_kernel_memory_access(laddr, false);
    if (kmc_result == KernelMemoryCheckResult::AccessGranted)
        return true;
    if (kmc_result == KernelMemoryCheckResult::AccessDenied)
        return false;
    if (is_kmalloc_address(laddr.as_ptr()))
        return true;
    return validate_read(laddr.as_ptr(), 1);
}

bool Process::validate_read_str(const char* str)
{
    if (!validate_read(str, 1))
        return false;
    return validate_read(str, strlen(str) + 1);
}

bool Process::validate_read(const void* address, ssize_t size) const
{
    ASSERT(size >= 0);
    LinearAddress first_address((dword)address);
    LinearAddress last_address = first_address.offset(size - 1);
    if (is_ring0()) {
        auto kmc_result = check_kernel_memory_access(first_address, false);
        if (kmc_result == KernelMemoryCheckResult::AccessGranted)
            return true;
        if (kmc_result == KernelMemoryCheckResult::AccessDenied)
            return false;
        if (is_kmalloc_address(address))
            return true;
    }
    ASSERT(size);
    if (!size)
        return false;
    if (first_address.page_base() != last_address.page_base()) {
        if (!MM.validate_user_read(*this, last_address))
            return false;
    }
    return MM.validate_user_read(*this, first_address);
}

bool Process::validate_write(void* address, ssize_t size) const
{
    ASSERT(size >= 0);
    LinearAddress first_address((dword)address);
    LinearAddress last_address = first_address.offset(size - 1);
    if (is_ring0()) {
        if (is_kmalloc_address(address))
            return true;
        auto kmc_result = check_kernel_memory_access(first_address, true);
        if (kmc_result == KernelMemoryCheckResult::AccessGranted)
            return true;
        if (kmc_result == KernelMemoryCheckResult::AccessDenied)
            return false;
    }
    if (!size)
        return false;
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
    if (descriptor->is_socket() && request == 413) {
        auto* pid = (pid_t*)arg;
        if (!validate_write_typed(pid))
            return -EFAULT;
        *pid = descriptor->socket()->origin_pid();
        return 0;
    }
    if (!descriptor->is_device())
        return -ENOTTY;
    return descriptor->device()->ioctl(*this, request, arg);
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
    m_fds[new_fd].set(*descriptor);
    return new_fd;
}

int Process::sys$dup2(int old_fd, int new_fd)
{
    auto* descriptor = file_descriptor(old_fd);
    if (!descriptor)
        return -EBADF;
    if (number_of_open_file_descriptors() == m_max_open_file_descriptors)
        return -EMFILE;
    m_fds[new_fd].set(*descriptor);
    return new_fd;
}

int Process::sys$sigprocmask(int how, const sigset_t* set, sigset_t* old_set)
{
    if (old_set) {
        if (!validate_write_typed(old_set))
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
    if (!validate_write_typed(set))
        return -EFAULT;
    *set = m_pending_signals;
    return 0;
}

void Process::set_default_signal_dispositions()
{
    // FIXME: Set up all the right default actions. See signal(7).
    memset(&m_signal_action_data, 0, sizeof(m_signal_action_data));
    m_signal_action_data[SIGCHLD].handler_or_sigaction = LinearAddress((dword)SIG_IGN);
    m_signal_action_data[SIGWINCH].handler_or_sigaction = LinearAddress((dword)SIG_IGN);
}

int Process::sys$sigaction(int signum, const sigaction* act, sigaction* old_act)
{
    if (signum < 1 || signum >= 32 || signum == SIGKILL || signum == SIGSTOP)
        return -EINVAL;
    if (!validate_read_typed(act))
        return -EFAULT;
    InterruptDisabler disabler; // FIXME: This should use a narrower lock. Maybe a way to ignore signals temporarily?
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

int Process::sys$getgroups(ssize_t count, gid_t* gids)
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

int Process::sys$setgroups(ssize_t count, const gid_t* gids)
{
    if (count < 0)
        return -EINVAL;
    if (!is_superuser())
        return -EPERM;
    if (count >= MAX_PROCESS_GIDS)
        return -EINVAL;
    if (!validate_read(gids, count))
        return -EFAULT;
    m_gids.clear();
    m_gids.set(m_gid);
    for (int i = 0; i < count; ++i)
        m_gids.set(gids[i]);
    return 0;
}

int Process::sys$mkdir(const char* pathname, mode_t mode)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    size_t pathname_length = strlen(pathname);
    if (pathname_length == 0)
        return -EINVAL;
    if (pathname_length >= 255)
        return -ENAMETOOLONG;
    return VFS::the().mkdir(String(pathname, pathname_length), mode & ~umask(), cwd_inode());
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
    (void)exceptfds;

    if (timeout) {
        m_select_timeout = *timeout;
        m_select_has_timeout = true;
    } else {
        m_select_has_timeout = false;
    }

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
    error = transfer_fds(readfds, m_select_exceptional_fds);
    if (error)
        return error;

#ifdef DEBUG_IO
    dbgprintf("%s<%u> selecting on (read:%u, write:%u), timeout=%p\n", name().characters(), pid(), m_select_read_fds.size(), m_select_write_fds.size(), timeout);
#endif

    if (!timeout || (timeout->tv_sec || timeout->tv_usec)) {
        block(BlockedSelect);
        Scheduler::yield();
    }

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

    // FIXME: Check for exceptional conditions.

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

    if (timeout < 0) {
        block(BlockedSelect);
        Scheduler::yield();
    }

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

Inode& Process::cwd_inode()
{
    // FIXME: This is retarded factoring.
    if (!m_cwd)
        m_cwd = VFS::the().root_inode();
    return *m_cwd;
}

int Process::sys$link(const char* old_path, const char* new_path)
{
    if (!validate_read_str(old_path))
        return -EFAULT;
    if (!validate_read_str(new_path))
        return -EFAULT;
    return VFS::the().link(String(old_path), String(new_path), cwd_inode());
}

int Process::sys$unlink(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().unlink(String(pathname), cwd_inode());
}

int Process::sys$symlink(const char* target, const char* linkpath)
{
    if (!validate_read_str(target))
        return -EFAULT;
    if (!validate_read_str(linkpath))
        return -EFAULT;
    return VFS::the().symlink(String(target), String(linkpath), cwd_inode());
}

int Process::sys$rmdir(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().rmdir(String(pathname), cwd_inode());
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
    return VFS::the().chmod(String(pathname), mode, cwd_inode());
}

int Process::sys$fchmod(int fd, mode_t mode)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    return descriptor->fchmod(mode);
}

int Process::sys$chown(const char* pathname, uid_t uid, gid_t gid)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().chown(String(pathname), uid, gid, cwd_inode());
}

void Process::finalize()
{
    ASSERT(current == g_finalizer);

    m_fds.clear();
    m_tty = nullptr;
    disown_all_shared_buffers();
    {
        InterruptDisabler disabler;
        if (auto* parent_process = Process::from_pid(m_ppid)) {
            if (parent_process->m_signal_action_data[SIGCHLD].flags & SA_NOCLDWAIT) {
                // NOTE: If the parent doesn't care about this process, let it go.
                m_ppid = 0;
            } else {
                parent_process->send_signal(SIGCHLD, this);
            }
        }
    }

    set_state(Dead);
}

void Process::die()
{
    set_state(Dying);

    if (!Scheduler::is_active())
        Scheduler::pick_next_and_switch_now();
}

size_t Process::amount_virtual() const
{
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region->size();
    }
    return amount;
}

size_t Process::amount_resident() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region->amount_resident();
    }
    return amount;
}

size_t Process::amount_shared() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    // FIXME: It doesn't work at the moment, since it relies on PhysicalPage retain counts,
    //        and each PhysicalPage is only retained by its VMObject. This needs to be refactored
    //        so that every Region contributes +1 retain to each of its PhysicalPages.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region->amount_shared();
    }
    return amount;
}

void Process::finalize_dying_processes()
{
    Vector<Process*> dying_processes;
    {
        InterruptDisabler disabler;
        dying_processes.ensure_capacity(system.nprocess);
        for (auto* process = g_processes->head(); process; process = process->next()) {
            if (process->state() == Process::Dying)
                dying_processes.append(process);
        }
    }
    for (auto* process : dying_processes)
        process->finalize();
}

bool Process::tick()
{
    ++m_ticks;
    if (tss().cs & 3)
        ++m_ticks_in_user;
    else
        ++m_ticks_in_kernel;
    return --m_ticks_left;
}

int Process::sys$socket(int domain, int type, int protocol)
{
    if (number_of_open_file_descriptors() >= m_max_open_file_descriptors)
        return -EMFILE;
    int fd = 0;
    for (; fd < (int)m_max_open_file_descriptors; ++fd) {
        if (!m_fds[fd])
            break;
    }
    auto result = Socket::create(domain, type, protocol);
    if (result.is_error())
        return result.error();
    auto descriptor = FileDescriptor::create(*result.value());
    unsigned flags = 0;
    if (type & SOCK_CLOEXEC)
        flags |= FD_CLOEXEC;
    if (type & SOCK_NONBLOCK)
        descriptor->set_blocking(false);
    m_fds[fd].set(move(descriptor), flags);
    return fd;
}

int Process::sys$bind(int sockfd, const sockaddr* address, socklen_t address_length)
{
    if (!validate_read(address, address_length))
        return -EFAULT;
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    return socket.bind(address, address_length);
}

int Process::sys$listen(int sockfd, int backlog)
{
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    auto result = socket.listen(backlog);
    if (result.is_error())
        return result;
    descriptor->set_socket_role(SocketRole::Listener);
    return 0;
}

int Process::sys$accept(int accepting_socket_fd, sockaddr* address, socklen_t* address_size)
{
    if (!validate_write_typed(address_size))
        return -EFAULT;
    if (!validate_write(address, *address_size))
        return -EFAULT;
    if (number_of_open_file_descriptors() >= m_max_open_file_descriptors)
        return -EMFILE;
    int accepted_socket_fd = 0;
    for (; accepted_socket_fd < (int)m_max_open_file_descriptors; ++accepted_socket_fd) {
        if (!m_fds[accepted_socket_fd])
            break;
    }
    auto* accepting_socket_descriptor = file_descriptor(accepting_socket_fd);
    if (!accepting_socket_descriptor)
        return -EBADF;
    if (!accepting_socket_descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *accepting_socket_descriptor->socket();
    if (!socket.can_accept()) {
        ASSERT(!accepting_socket_descriptor->is_blocking());
        return -EAGAIN;
    }
    auto accepted_socket = socket.accept();
    ASSERT(accepted_socket);
    bool success = accepted_socket->get_address(address, address_size);
    ASSERT(success);
    auto accepted_socket_descriptor = FileDescriptor::create(move(accepted_socket), SocketRole::Accepted);
    // NOTE: The accepted socket inherits fd flags from the accepting socket.
    //       I'm not sure if this matches other systems but it makes sense to me.
    accepted_socket_descriptor->set_blocking(accepting_socket_descriptor->is_blocking());
    m_fds[accepted_socket_fd].set(move(accepted_socket_descriptor), m_fds[accepting_socket_fd].flags);
    return accepted_socket_fd;
}

int Process::sys$connect(int sockfd, const sockaddr* address, socklen_t address_size)
{
    if (!validate_read(address, address_size))
        return -EFAULT;
    if (number_of_open_file_descriptors() >= m_max_open_file_descriptors)
        return -EMFILE;
    int fd = 0;
    for (; fd < (int)m_max_open_file_descriptors; ++fd) {
        if (!m_fds[fd])
            break;
    }
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    auto result = socket.connect(address, address_size);
    if (result.is_error())
        return result;
    descriptor->set_socket_role(SocketRole::Connected);
    return 0;
}

KResult Process::wait_for_connect(Socket& socket)
{
    if (socket.is_connected())
        return KSuccess;
    m_blocked_connecting_socket = socket;
    block(BlockedConnect);
    Scheduler::yield();
    m_blocked_connecting_socket = nullptr;
    if (!socket.is_connected())
        return KResult(-ECONNREFUSED);
    return KSuccess;
}

ssize_t Process::sys$sendto(const Syscall::SC_sendto_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    int sockfd = params->sockfd;
    const void* data = params->data;
    size_t data_length = params->data_length;
    int flags = params->flags;
    auto* addr = (const sockaddr*)params->addr;
    auto addr_length = (socklen_t)params->addr_length;

    if (!validate_read(data, data_length))
        return -EFAULT;
    if (!validate_read(addr, addr_length))
        return -EFAULT;
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    kprintf("sendto %p (%u), flags=%u, addr: %p (%u)\n", data, data_length, flags, addr, addr_length);
    return socket.sendto(data, data_length, flags, addr, addr_length);
}

struct SharedBuffer {
    SharedBuffer(pid_t pid1, pid_t pid2, int size)
        : m_pid1(pid1)
        , m_pid2(pid2)
        , m_vmo(VMObject::create_anonymous(size))
    {
        ASSERT(pid1 != pid2);
    }

    void* retain(Process& process)
    {
        if (m_pid1 == process.pid()) {
            ++m_pid1_retain_count;
            if (!m_pid1_region) {
                m_pid1_region = process.allocate_region_with_vmo(LinearAddress(), size(), m_vmo.copy_ref(), 0, "SharedBuffer", true, m_pid1_writable);
                m_pid1_region->set_shared(true);
            }
            return m_pid1_region->laddr().as_ptr();
        } else if (m_pid2 == process.pid()) {
            ++m_pid2_retain_count;
            if (!m_pid2_region) {
                m_pid2_region = process.allocate_region_with_vmo(LinearAddress(), size(), m_vmo.copy_ref(), 0, "SharedBuffer", true, m_pid2_writable);
                m_pid2_region->set_shared(true);
            }
            return m_pid2_region->laddr().as_ptr();
        }
        return nullptr;
    }

    void release(Process& process)
    {
        if (m_pid1 == process.pid()) {
            ASSERT(m_pid1_retain_count);
            --m_pid1_retain_count;
            if (!m_pid1_retain_count) {
                if (m_pid1_region)
                    process.deallocate_region(*m_pid1_region);
                m_pid1_region = nullptr;
            }
            destroy_if_unused();
        } else if (m_pid2 == process.pid()) {
            ASSERT(m_pid2_retain_count);
            --m_pid2_retain_count;
            if (!m_pid2_retain_count) {
                if (m_pid2_region)
                    process.deallocate_region(*m_pid2_region);
                m_pid2_region = nullptr;
            }
            destroy_if_unused();
        }
    }

    void disown(pid_t pid)
    {
        if (m_pid1 == pid) {
            m_pid1 = 0;
            m_pid1_retain_count = 0;
            destroy_if_unused();
        } else if (m_pid2 == pid) {
            m_pid2 = 0;
            m_pid2_retain_count = 0;
            destroy_if_unused();
        }
    }

    pid_t pid1() const { return m_pid1; }
    pid_t pid2() const { return m_pid2; }
    unsigned pid1_retain_count() const { return m_pid1_retain_count; }
    unsigned pid2_retain_count() const { return m_pid2_retain_count; }
    size_t size() const { return m_vmo->size(); }
    void destroy_if_unused();

    void seal()
    {
        m_pid1_writable = false;
        m_pid2_writable = false;
        if (m_pid1_region) {
            m_pid1_region->set_writable(false);
            MM.remap_region(*m_pid1_region->page_directory(), *m_pid1_region);
        }
        if (m_pid2_region) {
            m_pid2_region->set_writable(false);
            MM.remap_region(*m_pid2_region->page_directory(), *m_pid2_region);
        }
    }

    int m_shared_buffer_id { -1 };
    pid_t m_pid1;
    pid_t m_pid2;
    unsigned m_pid1_retain_count { 1 };
    unsigned m_pid2_retain_count { 0 };
    Region* m_pid1_region { nullptr };
    Region* m_pid2_region { nullptr };
    bool m_pid1_writable { false };
    bool m_pid2_writable { false };
    Retained<VMObject> m_vmo;
};

static int s_next_shared_buffer_id;
Lockable<HashMap<int, OwnPtr<SharedBuffer>>>& shared_buffers()
{
    static Lockable<HashMap<int, OwnPtr<SharedBuffer>>>* map;
    if (!map)
        map = new Lockable<HashMap<int, OwnPtr<SharedBuffer>>>;
    return *map;
}

void SharedBuffer::destroy_if_unused()
{
    if (!m_pid1_retain_count && !m_pid2_retain_count) {
        LOCKER(shared_buffers().lock());
#ifdef SHARED_BUFFER_DEBUG
        kprintf("Destroying unused SharedBuffer{%p} id: %d (pid1: %d, pid2: %d)\n", this, m_shared_buffer_id, m_pid1, m_pid2);
#endif
        size_t count_before = shared_buffers().resource().size();
        shared_buffers().resource().remove(m_shared_buffer_id);
        ASSERT(count_before != shared_buffers().resource().size());
    }
}

void Process::disown_all_shared_buffers()
{
    LOCKER(shared_buffers().lock());
    Vector<SharedBuffer*> buffers_to_disown;
    for (auto& it : shared_buffers().resource())
        buffers_to_disown.append(it.value.ptr());
    for (auto* shared_buffer : buffers_to_disown)
        shared_buffer->disown(m_pid);
}

int Process::sys$create_shared_buffer(pid_t peer_pid, int size, void** buffer)
{
    if (!size || size < 0)
        return -EINVAL;
    size = PAGE_ROUND_UP(size);
    if (!peer_pid || peer_pid < 0 || peer_pid == m_pid)
        return -EINVAL;
    if (!validate_write_typed(buffer))
        return -EFAULT;
    {
        InterruptDisabler disabler;
        auto* peer = Process::from_pid(peer_pid);
        if (!peer)
            return -ESRCH;
    }
    LOCKER(shared_buffers().lock());
    int shared_buffer_id = ++s_next_shared_buffer_id;
    auto shared_buffer = make<SharedBuffer>(m_pid, peer_pid, size);
    shared_buffer->m_shared_buffer_id = shared_buffer_id;
    ASSERT(shared_buffer->size() >= size);
    shared_buffer->m_pid1_region = allocate_region_with_vmo(LinearAddress(), shared_buffer->size(), shared_buffer->m_vmo.copy_ref(), 0, "SharedBuffer", true, true);
    shared_buffer->m_pid1_region->set_shared(true);
    *buffer = shared_buffer->m_pid1_region->laddr().as_ptr();
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Created shared buffer %d (%u bytes, vmo is %u) for sharing with %d\n", name().characters(), pid(),shared_buffer_id, size, shared_buffer->size(), peer_pid);
#endif
    shared_buffers().resource().set(shared_buffer_id, move(shared_buffer));
    return shared_buffer_id;
}

int Process::sys$release_shared_buffer(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Releasing shared buffer %d, buffer count: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    shared_buffer.release(*this);
    return 0;
}

void* Process::sys$get_shared_buffer(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return (void*)-EINVAL;
    auto& shared_buffer = *(*it).value;
    if (shared_buffer.pid1() != m_pid && shared_buffer.pid2() != m_pid)
        return (void*)-EINVAL;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Retaining shared buffer %d, buffer count: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    return shared_buffer.retain(*this);
}

int Process::sys$seal_shared_buffer(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (shared_buffer.pid1() != m_pid && shared_buffer.pid2() != m_pid)
        return -EINVAL;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Sealing shared buffer %d\n", name().characters(), pid(), shared_buffer_id);
#endif
    shared_buffer.seal();
    return 0;
}

int Process::sys$get_shared_buffer_size(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (shared_buffer.pid1() != m_pid && shared_buffer.pid2() != m_pid)
        return -EINVAL;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Get shared buffer %d size: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    return shared_buffer.size();
}

const char* to_string(Process::State state)
{
    switch (state) {
    case Process::Invalid: return "Invalid";
    case Process::Runnable: return "Runnable";
    case Process::Running: return "Running";
    case Process::Dying: return "Dying";
    case Process::Dead: return "Dead";
    case Process::Stopped: return "Stopped";
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
    kprintf("to_string(Process::State): Invalid state: %u\n", state);
    ASSERT_NOT_REACHED();
    return nullptr;
}

const char* to_string(Process::Priority priority)
{
    switch (priority) {
    case Process::LowPriority: return "Low";
    case Process::NormalPriority: return "Normal";
    case Process::HighPriority: return "High";
    }
    kprintf("to_string(Process::Priority): Invalid priority: %u\n", priority);
    ASSERT_NOT_REACHED();
    return nullptr;
}
