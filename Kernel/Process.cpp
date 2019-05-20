#include <AK/Types.h>
#include "Process.h"
#include "kmalloc.h"
#include "StdLib.h"
#include "i386.h"
#include <Kernel/FileSystem/FileDescriptor.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/ELF/ELFLoader.h>
#include <Kernel/VM/MemoryManager.h>
#include "i8253.h"
#include "RTC.h"
#include <AK/StdLibExtras.h>
#include <LibC/signal_numbers.h>
#include <LibC/errno_numbers.h>
#include "Syscall.h"
#include "Scheduler.h"
#include <Kernel/FileSystem/FIFO.h>
#include "KSyms.h"
#include <Kernel/Net/Socket.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/ELF/exec_elf.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <Kernel/SharedMemory.h>
#include <Kernel/ProcessTracer.h>

//#define DEBUG_POLL_SELECT
//#define DEBUG_IO
//#define TASK_DEBUG
//#define FORK_DEBUG
#define SIGNAL_DEBUG
//#define SHARED_BUFFER_DEBUG

static pid_t next_pid;
InlineLinkedList<Process>* g_processes;
static String* s_hostname;
static Lock* s_hostname_lock;

void Process::initialize()
{
    next_pid = 0;
    g_processes = new InlineLinkedList<Process>;
    s_hostname = new String("courage");
    s_hostname_lock = new Lock;
}

Vector<pid_t> Process::all_pids()
{
    Vector<pid_t> pids;
    InterruptDisabler disabler;
    pids.ensure_capacity(g_processes->size_slow());
    for (auto* process = g_processes->head(); process; process = process->next())
        pids.append(process->pid());
    return pids;
}

Vector<Process*> Process::all_processes()
{
    Vector<Process*> processes;
    InterruptDisabler disabler;
    processes.ensure_capacity(g_processes->size_slow());
    for (auto* process = g_processes->head(); process; process = process->next())
        processes.append(process);
    return processes;
}

bool Process::in_group(gid_t gid) const
{
    return m_gids.contains(gid);
}

Range Process::allocate_range(LinearAddress laddr, size_t size)
{
    laddr.mask(PAGE_MASK);
    size = PAGE_ROUND_UP(size);
    if (laddr.is_null())
        return m_range_allocator.allocate_anywhere(size);
    return m_range_allocator.allocate_specific(laddr, size);
}

Region* Process::allocate_region(LinearAddress laddr, size_t size, String&& name, bool is_readable, bool is_writable, bool commit)
{
    auto range = allocate_range(laddr, size);
    if (!range.is_valid())
        return nullptr;
    m_regions.append(adopt(*new Region(range, move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    if (commit)
        m_regions.last()->commit();
    return m_regions.last().ptr();
}

Region* Process::allocate_file_backed_region(LinearAddress laddr, size_t size, RetainPtr<Inode>&& inode, String&& name, bool is_readable, bool is_writable)
{
    auto range = allocate_range(laddr, size);
    if (!range.is_valid())
        return nullptr;
    m_regions.append(adopt(*new Region(range, move(inode), move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    return m_regions.last().ptr();
}

Region* Process::allocate_region_with_vmo(LinearAddress laddr, size_t size, Retained<VMObject>&& vmo, size_t offset_in_vmo, String&& name, bool is_readable, bool is_writable)
{
    auto range = allocate_range(laddr, size);
    if (!range.is_valid())
        return nullptr;
    offset_in_vmo &= PAGE_MASK;
    m_regions.append(adopt(*new Region(range, move(vmo), offset_in_vmo, move(name), is_readable, is_writable)));
    MM.map_region(*this, *m_regions.last());
    return m_regions.last().ptr();
}

bool Process::deallocate_region(Region& region)
{
    InterruptDisabler disabler;
    for (int i = 0; i < m_regions.size(); ++i) {
        if (m_regions[i] == &region) {
            m_range_allocator.deallocate({ region.laddr(), region.size() });
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
    if (params->name && !validate_read_str(params->name))
        return (void*)-EFAULT;
    void* addr = (void*)params->addr;
    size_t size = params->size;
    int prot = params->prot;
    int flags = params->flags;
    int fd = params->fd;
    off_t offset = params->offset;
    const char* name = params->name;
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
        if (name)
            region->set_name(name);
        return region->laddr().as_ptr();
    }
    if (offset & ~PAGE_MASK)
        return (void*)-EINVAL;
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return (void*)-EBADF;
    auto region_or_error = descriptor->mmap(*this, LinearAddress((dword)addr), offset, size, prot);
    if (region_or_error.is_error())
        return (void*)(int)region_or_error.error();
    auto region = region_or_error.value();
    if (flags & MAP_SHARED)
        region->set_shared(true);
    if (name)
        region->set_name(name);
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
    }

    for (auto gid : m_gids)
        child->m_gids.set(gid);

    auto& child_tss = child->main_thread().m_tss;
    child_tss.eax = 0; // fork() returns 0 in the child :^)
    child_tss.ebx = regs.ebx;
    child_tss.ecx = regs.ecx;
    child_tss.edx = regs.edx;
    child_tss.ebp = regs.ebp;
    child_tss.esp = regs.esp_if_crossRing;
    child_tss.esi = regs.esi;
    child_tss.edi = regs.edi;
    child_tss.eflags = regs.eflags;
    child_tss.eip = regs.eip;
    child_tss.cs = regs.cs;
    child_tss.ds = regs.ds;
    child_tss.es = regs.es;
    child_tss.fs = regs.fs;
    child_tss.gs = regs.gs;
    child_tss.ss = regs.ss_if_crossRing;

#ifdef FORK_DEBUG
    dbgprintf("fork: child will begin executing at %w:%x with stack %w:%x, kstack %w:%x\n", child_tss.cs, child_tss.eip, child_tss.ss, child_tss.esp, child_tss.ss0, child_tss.esp0);
#endif

    {
        InterruptDisabler disabler;
        g_processes->prepend(child);
    }
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) forked from %u @ %p\n", child->pid(), child->name().characters(), m_pid, child_tss.eip);
#endif

    child->main_thread().set_state(Thread::State::Skip1SchedulerPass);

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

    dbgprintf("%s(%d) do_exec(%s): thread_count() = %d\n", m_name.characters(), m_pid, path.characters(), thread_count());
    // FIXME(Thread): Kill any threads the moment we commit to the exec().
    if (thread_count() != 1) {
        dbgprintf("Gonna die because I have many threads! These are the threads:\n");
        for_each_thread([] (Thread& thread) {
            dbgprintf("Thread{%p}: TID=%d, PID=%d\n", &thread, thread.tid(), thread.pid());
            return IterationDecision::Continue;
        });
        ASSERT(thread_count() == 1);
        ASSERT_NOT_REACHED();
    }


    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    auto result = VFS::the().open(path.view(), 0, 0, cwd_inode());
    if (result.is_error())
        return result.error();
    auto descriptor = result.value();

    if (!descriptor->metadata().may_execute(m_euid, m_gids))
        return -EACCES;

    if (!descriptor->metadata().size) {
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
    ASSERT(region);

    if (this != &current->process()) {
        // FIXME: Don't force-load the entire executable at once, let the on-demand pager take care of it.
        bool success = region->page_in();
        ASSERT(success);
    }
    OwnPtr<ELFLoader> loader;
    {
        // Okay, here comes the sleight of hand, pay close attention..
        auto old_regions = move(m_regions);
        m_regions.append(*region);
        loader = make<ELFLoader>(region->laddr().as_ptr());
        loader->map_section_hook = [&] (LinearAddress laddr, size_t size, size_t alignment, size_t offset_in_image, bool is_readable, bool is_writable, const String& name) {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            (void) allocate_region_with_vmo(laddr, size, vmo.copy_ref(), offset_in_image, String(name), is_readable, is_writable);
            return laddr.as_ptr();
        };
        loader->alloc_section_hook = [&] (LinearAddress laddr, size_t size, size_t alignment, bool is_readable, bool is_writable, const String& name) {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            (void) allocate_region(laddr, size, String(name), is_readable, is_writable);
            return laddr.as_ptr();
        };
        bool success = loader->load();
        if (!success || !loader->entry().get()) {
            m_page_directory = move(old_page_directory);
            // FIXME: RAII this somehow instead.
            ASSERT(&current->process() == this);
            MM.enter_process_paging_scope(*this);
            m_regions = move(old_regions);
            kprintf("do_exec: Failure loading %s\n", path.characters());
            return -ENOEXEC;
        }

        entry_eip = loader->entry().get();
    }

    m_elf_loader = move(loader);

    current->m_kernel_stack_for_signal_handler_region = nullptr;
    current->m_signal_stack_user_region = nullptr;
    current->set_default_signal_dispositions();
    current->m_signal_mask = 0;
    current->m_pending_signals = 0;

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
    if (&current->process() == this)
        cli();

    Scheduler::prepare_to_modify_tss(main_thread());

    m_name = parts.take_last();

    // ss0 sp!!!!!!!!!
    dword old_esp0 = main_thread().m_tss.esp0;

    memset(&main_thread().m_tss, 0, sizeof(main_thread().m_tss));
    main_thread().m_tss.eflags = 0x0202;
    main_thread().m_tss.eip = entry_eip;
    main_thread().m_tss.cs = 0x1b;
    main_thread().m_tss.ds = 0x23;
    main_thread().m_tss.es = 0x23;
    main_thread().m_tss.fs = 0x23;
    main_thread().m_tss.gs = 0x23;
    main_thread().m_tss.ss = 0x23;
    main_thread().m_tss.cr3 = page_directory().cr3();
    main_thread().make_userspace_stack_for_main_thread(move(arguments), move(environment));
    main_thread().m_tss.ss0 = 0x10;
    main_thread().m_tss.esp0 = old_esp0;
    main_thread().m_tss.ss2 = m_pid;

    m_executable = descriptor->inode();

    if (descriptor->metadata().is_setuid())
        m_euid = descriptor->metadata().uid;
    if (descriptor->metadata().is_setgid())
        m_egid = descriptor->metadata().gid;

#ifdef TASK_DEBUG
    kprintf("Process %u (%s) exec'd %s @ %p\n", pid(), name().characters(), path.characters(), main_thread().tss().eip);
#endif

    main_thread().set_state(Thread::State::Skip1SchedulerPass);
    return 0;
}

int Process::exec(String path, Vector<String> arguments, Vector<String> environment)
{
    // The bulk of exec() is done by do_exec(), which ensures that all locals
    // are cleaned up by the time we yield-teleport below.
    int rc = do_exec(move(path), move(arguments), move(environment));
    if (rc < 0)
        return rc;

    if (&current->process() == this) {
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
    if (!*filename)
        return -ENOENT;
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
    }
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), process->main_thread().tss().eip);
#endif
    error = 0;
    return process;
}

Process* Process::create_kernel_process(String&& name, void (*e)())
{
    auto* process = new Process(move(name), (uid_t)0, (gid_t)0, (pid_t)0, Ring0);
    process->main_thread().tss().eip = (dword)e;

    if (process->pid() != 0) {
        InterruptDisabler disabler;
        g_processes->prepend(process);
#ifdef TASK_DEBUG
        kprintf("Kernel process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), process->main_thread().tss().eip);
#endif
    }

    process->main_thread().set_state(Thread::State::Runnable);
    return process;
}

static const dword userspace_range_base = 0x01000000;
static const dword kernelspace_range_base = 0xc0000000;

Process::Process(String&& name, uid_t uid, gid_t gid, pid_t ppid, RingLevel ring, RetainPtr<Inode>&& cwd, RetainPtr<Inode>&& executable, TTY* tty, Process* fork_parent)
    : m_name(move(name))
    , m_pid(next_pid++) // FIXME: RACE: This variable looks racy!
    , m_uid(uid)
    , m_gid(gid)
    , m_euid(uid)
    , m_egid(gid)
    , m_ring(ring)
    , m_cwd(move(cwd))
    , m_executable(move(executable))
    , m_tty(tty)
    , m_ppid(ppid)
    , m_range_allocator(LinearAddress(userspace_range_base), kernelspace_range_base - userspace_range_base)
{
    dbgprintf("Process: New process PID=%u with name=%s\n", m_pid, m_name.characters());

    m_page_directory = PageDirectory::create();
#ifdef MM_DEBUG
    dbgprintf("Process %u ctor: PD=%x created\n", pid(), m_page_directory.ptr());
#endif

    // NOTE: fork() doesn't clone all threads; the thread that called fork() becomes the main thread in the new process.
    if (fork_parent)
        m_main_thread = current->clone(*this);
    else
        m_main_thread = new Thread(*this);

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

    if (fork_parent) {
        m_sid = fork_parent->m_sid;
        m_pgid = fork_parent->m_pgid;
        m_umask = fork_parent->m_umask;
    }
}

Process::~Process()
{
    dbgprintf("~Process{%p} name=%s pid=%d, m_fds=%d\n", this, m_name.characters(), pid(), m_fds.size());
    delete m_main_thread;
    m_main_thread = nullptr;

    Vector<Thread*, 16> my_threads;
    for_each_thread([&my_threads] (auto& thread) {
        my_threads.append(&thread);
        return IterationDecision::Continue;
    });
    for (auto* thread : my_threads)
        delete thread;
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

    dump_backtrace();

    m_termination_status = status;
    m_termination_signal = 0;
    die();
    ASSERT_NOT_REACHED();
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
    current->m_signal_mask = mask;
    return 0;
}

void Process::sys$sigreturn()
{
    InterruptDisabler disabler;
    Scheduler::prepare_to_modify_tss(*current);
    current->m_tss = *current->m_tss_to_resume_kernel;
    current->m_tss_to_resume_kernel.clear();
#ifdef SIGNAL_DEBUG
    kprintf("sys$sigreturn in %s(%u)\n", name().characters(), pid());
    auto& tss = current->tss();
    kprintf(" -> resuming execution at %w:%x stack %w:%x flags %x cr3 %x\n", tss.cs, tss.eip, tss.ss, tss.esp, tss.eflags, tss.cr3);
#endif
    current->set_state(Thread::State::Skip1SchedulerPass);
    Scheduler::yield();
    kprintf("sys$sigreturn failed in %s(%u)\n", name().characters(), pid());
    ASSERT_NOT_REACHED();
}

void Process::crash()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!is_dead());

    dump_backtrace();

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

ssize_t Process::sys$writev(int fd, const struct iovec* iov, int iov_count)
{
    if (iov_count < 0)
        return -EINVAL;

    if (!validate_read_typed(iov, iov_count))
        return -EFAULT;

    // FIXME: Return EINVAL if sum of iovecs is greater than INT_MAX

    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;

    int nwritten = 0;
    for (int i = 0; i < iov_count; ++i) {
        int rc = do_write(*descriptor, (const byte*)iov[i].iov_base, iov[i].iov_len);
        if (rc < 0) {
            if (nwritten == 0)
                return rc;
            return nwritten;
        }
        nwritten += rc;
    }

    if (current->has_unmasked_pending_signals()) {
        current->block(Thread::State::BlockedSignal);
        if (nwritten == 0)
            return -EINTR;
    }

    return nwritten;
}

ssize_t Process::do_write(FileDescriptor& descriptor, const byte* data, int data_size)
{
    ssize_t nwritten = 0;
    if (!descriptor.is_blocking()) {
        if (!descriptor.can_write())
            return -EAGAIN;
    }

    while (nwritten < data_size) {
#ifdef IO_DEBUG
        dbgprintf("while %u < %u\n", nwritten, size);
#endif
        if (!descriptor.can_write()) {
#ifdef IO_DEBUG
            dbgprintf("block write on %d\n", fd);
#endif
            current->block(Thread::State::BlockedWrite, descriptor);
        }
        ssize_t rc = descriptor.write(data + nwritten, data_size - nwritten);
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
        if (current->has_unmasked_pending_signals()) {
            current->block(Thread::State::BlockedSignal);
            if (nwritten == 0)
                return -EINTR;
        }
        nwritten += rc;
    }
    return nwritten;
}

ssize_t Process::sys$write(int fd, const byte* data, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (size == 0)
        return 0;
    if (!validate_read(data, size))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u): sys$write(%d, %p, %u)\n", name().characters(), pid(), fd, data, size);
#endif
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    auto nwritten = do_write(*descriptor, data, size);
    if (current->has_unmasked_pending_signals()) {
        current->block(Thread::State::BlockedSignal);
        if (nwritten == 0)
            return -EINTR;
    }
    return nwritten;
}

ssize_t Process::sys$read(int fd, byte* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (size == 0)
        return 0;
    if (!validate_write(buffer, size))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$read(%d, %p, %u)\n", name().characters(), pid(), fd, buffer, size);
#endif
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    if (descriptor->is_blocking()) {
        if (!descriptor->can_read()) {
            current->block(Thread::State::BlockedRead, *descriptor);
            if (current->m_was_interrupted_while_blocked)
                return -EINTR;
        }
    }
    return descriptor->read(buffer, size);
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
        struct timeval now;
        kgettimeofday(now);
        mtime = now.tv_sec;
        atime = now.tv_sec;
    }
    return VFS::the().utime(StringView(pathname), cwd_inode(), atime, mtime);
}

int Process::sys$access(const char* pathname, int mode)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().access(StringView(pathname), mode, cwd_inode());
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
        int new_fd = alloc_fd(arg_fd);
        if (new_fd < 0)
            return new_fd;
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
    return VFS::the().stat(StringView(path), O_NOFOLLOW_NOERROR, cwd_inode(), *statbuf);
}

int Process::sys$stat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    return VFS::the().stat(StringView(path), O_NOFOLLOW_NOERROR, cwd_inode(), *statbuf);
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

    auto contents = descriptor->read_entire_file();
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
    auto directory_or_error = VFS::the().open_directory(StringView(path), cwd_inode());
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
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto result = VFS::the().open(path, options, mode & ~umask(), cwd_inode());
    if (result.is_error())
        return result.error();
    auto descriptor = result.value();
    if (options & O_DIRECTORY && !descriptor->is_directory())
        return -ENOTDIR; // FIXME: This should be handled by VFS::open.
    if (options & O_NONBLOCK)
        descriptor->set_blocking(false);
    dword flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(descriptor), flags);
    return fd;
}

int Process::alloc_fd(int first_candidate_fd)
{
    int fd = -EMFILE;
    for (int i = first_candidate_fd; i < (int)m_max_open_file_descriptors; ++i) {
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
    auto fifo = FIFO::create(m_uid);

    int reader_fd = alloc_fd();
    m_fds[reader_fd].set(fifo->open_direction(FIFO::Direction::Reader));
    pipefd[0] = reader_fd;

    int writer_fd = alloc_fd();
    m_fds[writer_fd].set(fifo->open_direction(FIFO::Direction::Writer));
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
        current->send_signal(signal, this);
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

    current->sleep(usec / 1000);
    if (current->m_wakeup_time > g_uptime) {
        ASSERT(current->m_was_interrupted_while_blocked);
        dword ticks_left_until_original_wakeup_time = current->m_wakeup_time - g_uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
    return 0;
}

int Process::sys$sleep(unsigned seconds)
{
    if (!seconds)
        return 0;
    current->sleep(seconds * TICKS_PER_SECOND);
    if (current->m_wakeup_time > g_uptime) {
        ASSERT(current->m_was_interrupted_while_blocked);
        dword ticks_left_until_original_wakeup_time = current->m_wakeup_time - g_uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
    return 0;
}

void kgettimeofday(timeval& tv)
{
    tv.tv_sec = RTC::boot_time() + PIT::seconds_since_boot();
    tv.tv_usec = PIT::ticks_this_second() * 1000;
}

int Process::sys$gettimeofday(timeval* tv)
{
    if (!validate_write_typed(tv))
        return -EFAULT;
    kgettimeofday(*tv);
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
    int exit_status;
    {
        InterruptDisabler disabler;
        exit_status = (process.m_termination_status << 8) | process.m_termination_signal;

        if (process.ppid()) {
            auto* parent = Process::from_pid(process.ppid());
            if (parent) {
                parent->m_ticks_in_user_for_dead_children += process.m_ticks_in_user + process.m_ticks_in_user_for_dead_children;
                parent->m_ticks_in_kernel_for_dead_children += process.m_ticks_in_kernel + process.m_ticks_in_kernel_for_dead_children;
            }
        }

        dbgprintf("reap: %s(%u) {%s}\n", process.name().characters(), process.pid(), to_string(process.state()));
        ASSERT(process.is_dead());
        g_processes->remove(&process);
    }
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
                if (process.is_dead()) {
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
            if (waitee_process->is_dead()) {
                exit_status = reap(*waitee_process);
                return waitee;
            }
            return 0;
        }
    }

    current->m_waitee_pid = waitee;
    current->block(Thread::State::BlockedWait);
    if (current->m_was_interrupted_while_blocked)
        return -EINTR;
    Process* waitee_process;
    {
        InterruptDisabler disabler;
        // NOTE: If waitee was -1, m_waitee will have been filled in by the scheduler.
        waitee_process = Process::from_pid(current->m_waitee_pid);
    }
    ASSERT(waitee_process);
    exit_status = reap(*waitee_process);
    return current->m_waitee_pid;
}


enum class KernelMemoryCheckResult {
    NotInsideKernelMemory,
    AccessGranted,
    AccessDenied
};

// FIXME: Nothing about this is really super...
// This structure is only present at offset 28 in the main multiboot info struct
// if bit 5 of offset 0 (flags) is set.  We're just assuming that the flag is set.
//
// Also, there's almost certainly a better way to get that information here than
// a global set by boot.S
//
// Also I'm not 100% sure any of this is correct...

struct mb_elf {
    uint32_t num;
    uint32_t size;
    uint32_t addr;
    uint32_t shndx;
};

extern "C" {
void* multiboot_ptr;
}

static KernelMemoryCheckResult check_kernel_memory_access(LinearAddress laddr, bool is_write)
{
	// FIXME: It would be better to have a proper structure for this...
    auto* sections = (const mb_elf*)((const byte*)multiboot_ptr + 28);

    auto* kernel_program_headers = (Elf32_Phdr*)(sections->addr);
    for (unsigned i = 0; i < sections->num; ++i) {
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
    if (laddr.is_null())
        return false;
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
    if (!descriptor->is_file())
        return -ENOTTY;
    return descriptor->file()->ioctl(*descriptor, request, arg);
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
    int new_fd = alloc_fd(0);
    if (new_fd < 0)
        return new_fd;
    m_fds[new_fd].set(*descriptor);
    return new_fd;
}

int Process::sys$dup2(int old_fd, int new_fd)
{
    auto* descriptor = file_descriptor(old_fd);
    if (!descriptor)
        return -EBADF;
    if (new_fd < 0 || new_fd >= m_max_open_file_descriptors)
        return -EINVAL;
    m_fds[new_fd].set(*descriptor);
    return new_fd;
}

int Process::sys$sigprocmask(int how, const sigset_t* set, sigset_t* old_set)
{
    if (old_set) {
        if (!validate_write_typed(old_set))
            return -EFAULT;
        *old_set = current->m_signal_mask;
    }
    if (set) {
        if (!validate_read_typed(set))
            return -EFAULT;
        switch (how) {
        case SIG_BLOCK:
            current->m_signal_mask &= ~(*set);
            break;
        case SIG_UNBLOCK:
            current->m_signal_mask |= *set;
            break;
        case SIG_SETMASK:
            current->m_signal_mask = *set;
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
    *set = current->m_pending_signals;
    return 0;
}

int Process::sys$sigaction(int signum, const sigaction* act, sigaction* old_act)
{
    if (signum < 1 || signum >= 32 || signum == SIGKILL || signum == SIGSTOP)
        return -EINVAL;
    if (!validate_read_typed(act))
        return -EFAULT;
    InterruptDisabler disabler; // FIXME: This should use a narrower lock. Maybe a way to ignore signals temporarily?
    auto& action = current->m_signal_action_data[signum];
    if (old_act) {
        if (!validate_write_typed(old_act))
            return -EFAULT;
        old_act->sa_flags = action.flags;
        old_act->sa_sigaction = (decltype(old_act->sa_sigaction))action.handler_or_sigaction.get();
    }
    action.flags = act->sa_flags;
    action.handler_or_sigaction = LinearAddress((dword)act->sa_sigaction);
    return 0;
}

int Process::sys$getgroups(ssize_t count, gid_t* gids)
{
    if (count < 0)
        return -EINVAL;
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
    return VFS::the().mkdir(StringView(pathname, pathname_length), mode & ~umask(), cwd_inode());
}

clock_t Process::sys$times(tms* times)
{
    if (!validate_write_typed(times))
        return -EFAULT;
    times->tms_utime = m_ticks_in_user;
    times->tms_stime = m_ticks_in_kernel;
    times->tms_cutime = m_ticks_in_user_for_dead_children;
    times->tms_cstime = m_ticks_in_kernel_for_dead_children;
    return g_uptime & 0x7fffffff;
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

    if (timeout && (timeout->tv_sec || timeout->tv_usec)) {
        struct timeval now;
        kgettimeofday(now);
        AK::timeval_add(&now, timeout, &current->m_select_timeout);
        current->m_select_has_timeout = true;
    } else {
        current->m_select_has_timeout = false;
    }

    if (nfds < 0)
        return -EINVAL;

    // FIXME: Return -EINTR if a signal is caught.
    // FIXME: Return -EINVAL if timeout is invalid.

    auto transfer_fds = [this, nfds] (fd_set* set, auto& vector) -> int {
        vector.clear_with_capacity();
        if (!set)
            return 0;
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
    error = transfer_fds(writefds, current->m_select_write_fds);
    if (error)
        return error;
    error = transfer_fds(readfds, current->m_select_read_fds);
    if (error)
        return error;
    error = transfer_fds(exceptfds, current->m_select_exceptional_fds);
    if (error)
        return error;

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbgprintf("%s<%u> selecting on (read:%u, write:%u), timeout=%p\n", name().characters(), pid(), current->m_select_read_fds.size(), current->m_select_write_fds.size(), timeout);
#endif

    if (!timeout || current->m_select_has_timeout)
        current->block(Thread::State::BlockedSelect);

    int markedfds = 0;

    if (readfds) {
        memset(readfds, 0, sizeof(fd_set));
        auto bitmap = Bitmap::wrap((byte*)readfds, FD_SETSIZE);
        for (int fd : current->m_select_read_fds) {
            auto* descriptor = file_descriptor(fd);
            if (!descriptor)
                continue;
            if (descriptor->can_read()) {
                bitmap.set(fd, true);
                ++markedfds;
            }
        }
    }

    if (writefds) {
        memset(writefds, 0, sizeof(fd_set));
        auto bitmap = Bitmap::wrap((byte*)writefds, FD_SETSIZE);
        for (int fd : current->m_select_write_fds) {
            auto* descriptor = file_descriptor(fd);
            if (!descriptor)
                continue;
            if (descriptor->can_write()) {
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

    current->m_select_write_fds.clear_with_capacity();
    current->m_select_read_fds.clear_with_capacity();
    for (int i = 0; i < nfds; ++i) {
        if (fds[i].events & POLLIN)
            current->m_select_read_fds.append(fds[i].fd);
        if (fds[i].events & POLLOUT)
            current->m_select_write_fds.append(fds[i].fd);
    }

    if (timeout >= 0) {
        // poll is in ms, we want s/us.
        struct timeval tvtimeout;
        tvtimeout.tv_sec = 0;
        while (timeout >= 1000) {
            tvtimeout.tv_sec += 1;
            timeout -= 1000;
        }
        tvtimeout.tv_usec = timeout * 1000;

        struct timeval now;
        kgettimeofday(now);
        AK::timeval_add(&now, &tvtimeout, &current->m_select_timeout);
        current->m_select_has_timeout = true;
    } else {
        current->m_select_has_timeout = false;
    }

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbgprintf("%s<%u> polling on (read:%u, write:%u), timeout=%d\n", name().characters(), pid(), current->m_select_read_fds.size(), current->m_select_write_fds.size(), timeout);
#endif

    if (current->m_select_has_timeout || timeout < 0) {
        current->block(Thread::State::BlockedSelect);
    }

    int fds_with_revents = 0;

    for (int i = 0; i < nfds; ++i) {
        auto* descriptor = file_descriptor(fds[i].fd);
        if (!descriptor) {
            fds[i].revents = POLLNVAL;
            continue;
        }
        fds[i].revents = 0;
        if (fds[i].events & POLLIN && descriptor->can_read())
            fds[i].revents |= POLLIN;
        if (fds[i].events & POLLOUT && descriptor->can_write())
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
    return VFS::the().link(StringView(old_path), StringView(new_path), cwd_inode());
}

int Process::sys$unlink(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().unlink(StringView(pathname), cwd_inode());
}

int Process::sys$symlink(const char* target, const char* linkpath)
{
    if (!validate_read_str(target))
        return -EFAULT;
    if (!validate_read_str(linkpath))
        return -EFAULT;
    return VFS::the().symlink(StringView(target), StringView(linkpath), cwd_inode());
}

int Process::sys$rmdir(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().rmdir(StringView(pathname), cwd_inode());
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
    return VFS::the().chmod(StringView(pathname), mode, cwd_inode());
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
    return VFS::the().chown(StringView(pathname), uid, gid, cwd_inode());
}

void Process::finalize()
{
    ASSERT(current == g_finalizer);
    dbgprintf("Finalizing Process %s(%u)\n", m_name.characters(), m_pid);

    m_fds.clear();
    m_tty = nullptr;
    disown_all_shared_buffers();
    {
        InterruptDisabler disabler;
        if (auto* parent_process = Process::from_pid(m_ppid)) {
            // FIXME(Thread): What should we do here? Should we look at all threads' signal actions?
            if (parent_process->main_thread().m_signal_action_data[SIGCHLD].flags & SA_NOCLDWAIT) {
                // NOTE: If the parent doesn't care about this process, let it go.
                m_ppid = 0;
            } else {
                parent_process->send_signal(SIGCHLD, this);
            }
        }
    }

    m_dead = true;
}

void Process::die()
{
    if (m_tracer)
        m_tracer->set_dead();

    {
        InterruptDisabler disabler;
        for_each_thread([] (Thread& thread) {
            if (thread.state() != Thread::State::Dead)
                thread.set_state(Thread::State::Dying);
            return IterationDecision::Continue;
        });
    }

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

int Process::sys$socket(int domain, int type, int protocol)
{
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
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
    int accepted_socket_fd = alloc_fd();
    if (accepted_socket_fd < 0)
        return accepted_socket_fd;
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
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    if (descriptor->socket_role() == SocketRole::Connected)
        return -EISCONN;
    auto& socket = *descriptor->socket();
    descriptor->set_socket_role(SocketRole::Connecting);
    auto result = socket.connect(*descriptor, address, address_size, descriptor->is_blocking() ? ShouldBlock::Yes : ShouldBlock::No);
    if (result.is_error()) {
        descriptor->set_socket_role(SocketRole::None);
        return result;
    }
    descriptor->set_socket_role(SocketRole::Connected);
    return 0;
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
    if (addr && !validate_read(addr, addr_length))
        return -EFAULT;
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    kprintf("sendto %p (%u), flags=%u, addr: %p (%u)\n", data, data_length, flags, addr, addr_length);
    return socket.sendto(*descriptor, data, data_length, flags, addr, addr_length);
}

ssize_t Process::sys$recvfrom(const Syscall::SC_recvfrom_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    int sockfd = params->sockfd;
    void* buffer = params->buffer;
    size_t buffer_length = params->buffer_length;
    int flags = params->flags;
    auto* addr = (sockaddr*)params->addr;
    auto* addr_length = (socklen_t*)params->addr_length;

    if (!validate_write(buffer, buffer_length))
        return -EFAULT;
    if (addr_length) {
        if (!validate_write_typed(addr_length))
            return -EFAULT;
        if (!validate_write(addr, *addr_length))
            return -EFAULT;
    } else if (addr) {
       return -EINVAL;
    }
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();

    bool original_blocking = descriptor->is_blocking();
    if (flags & MSG_DONTWAIT)
        descriptor->set_blocking(false);

    auto nrecv = socket.recvfrom(*descriptor, buffer, buffer_length, flags, addr, addr_length);
    if (flags & MSG_DONTWAIT)
        descriptor->set_blocking(original_blocking);

    return nrecv;
}

int Process::sys$getsockname(int sockfd, sockaddr* addr, socklen_t* addrlen)
{
    if (!validate_read_typed(addrlen))
        return -EFAULT;

    if (*addrlen <= 0)
        return -EINVAL;

    if (!validate_write(addr, *addrlen))
        return -EFAULT;

    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;

    if (!descriptor->is_socket())
        return -ENOTSOCK;

    auto& socket = *descriptor->socket();
    if (!socket.get_address(addr, addrlen))
        return -EINVAL; // FIXME: Should this be another error? I'm not sure.

    return 0;
}

int Process::sys$getsockopt(const Syscall::SC_getsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;
    int sockfd = params->sockfd;
    int level = params->level;
    int option = params->option;
    auto* value = params->value;
    auto* value_size = (socklen_t*)params->value_size;

    if (!validate_write_typed(value_size))
        return -EFAULT;
    if (!validate_write(value, *value_size))
        return -EFAULT;
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    return socket.getsockopt(level, option, value, value_size);
}

int Process::sys$setsockopt(const Syscall::SC_setsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;
    int sockfd = params->sockfd;
    int level = params->level;
    int option = params->option;
    auto* value = params->value;
    auto value_size = (socklen_t)params->value_size;

    if (!validate_read(value, value_size))
        return -EFAULT;
    auto* descriptor = file_descriptor(sockfd);
    if (!descriptor)
        return -EBADF;
    if (!descriptor->is_socket())
        return -ENOTSOCK;
    auto& socket = *descriptor->socket();
    return socket.setsockopt(level, option, value, value_size);
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
        auto count_before = shared_buffers().resource().size();
        shared_buffers().resource().remove(m_shared_buffer_id);
        ASSERT(count_before != shared_buffers().resource().size());
    }
}

void Process::disown_all_shared_buffers()
{
    LOCKER(shared_buffers().lock());
    Vector<SharedBuffer*, 32> buffers_to_disown;
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

const char* to_string(Process::Priority priority)
{
    switch (priority) {
    case Process::IdlePriority: return "Idle";
    case Process::LowPriority: return "Low";
    case Process::NormalPriority: return "Normal";
    case Process::HighPriority: return "High";
    }
    kprintf("to_string(Process::Priority): Invalid priority: %u\n", priority);
    ASSERT_NOT_REACHED();
    return nullptr;
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
    // FIXME(Thread): Find the appropriate thread to deliver the signal to.
    main_thread().send_signal(signal, sender);
}

int Process::thread_count() const
{
    int count = 0;
    for_each_thread([&count] (auto&) {
        ++count;
        return IterationDecision::Continue;
    });
    return count;
}

int Process::sys$create_thread(int(*entry)(void*), void* argument)
{
    if (!validate_read((const void*)entry, sizeof(void*)))
        return -EFAULT;
    auto* thread = new Thread(*this);
    auto& tss = thread->tss();
    tss.eip = (dword)entry;
    tss.eflags = 0x0202;
    tss.cr3 = page_directory().cr3();
    thread->make_userspace_stack_for_secondary_thread(argument);

    thread->set_state(Thread::State::Runnable);
    return 0;
}

void Process::sys$exit_thread(int code)
{
    cli();
    if (&current->process().main_thread() == current) {
        sys$exit(code);
        return;
    }
    current->set_state(Thread::State::Dying);
    big_lock().unlock_if_locked();
    Scheduler::pick_next_and_switch_now();
    ASSERT_NOT_REACHED();
}

int Process::sys$gettid()
{
    return current->tid();
}

int Process::sys$donate(int tid)
{
    if (tid < 0)
        return -EINVAL;
    InterruptDisabler disabler;
    Thread* beneficiary = nullptr;
    for_each_thread([&] (Thread& thread) {
        if (thread.tid() == tid) {
            beneficiary = &thread;
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
    if (!beneficiary)
        return -ENOTHREAD;
    Scheduler::donate_to(beneficiary, "sys$donate");
    return 0;
}

int Process::sys$rename(const char* oldpath, const char* newpath)
{
    if (!validate_read_str(oldpath))
        return -EFAULT;
    if (!validate_read_str(newpath))
        return -EFAULT;
    return VFS::the().rename(StringView(oldpath), StringView(newpath), cwd_inode());
}

int Process::sys$shm_open(const char* name, int flags, mode_t mode)
{
    if (!validate_read_str(name))
        return -EFAULT;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto shm_or_error = SharedMemory::open(String(name), flags, mode);
    if (shm_or_error.is_error())
        return shm_or_error.error();
    auto descriptor = FileDescriptor::create(shm_or_error.value().ptr());
    m_fds[fd].set(move(descriptor), FD_CLOEXEC);
    return fd;
}

int Process::sys$shm_unlink(const char* name)
{
    if (!validate_read_str(name))
        return -EFAULT;
    return SharedMemory::unlink(String(name));
}

int Process::sys$ftruncate(int fd, off_t length)
{
    auto* descriptor = file_descriptor(fd);
    if (!descriptor)
        return -EBADF;
    // FIXME: Check that fd is writable, otherwise EINVAL.
    if (!descriptor->is_file() && !descriptor->is_shared_memory())
        return -EINVAL;
    return descriptor->truncate(length);
}

int Process::sys$systrace(pid_t pid)
{
    InterruptDisabler disabler;
    auto* peer = Process::from_pid(pid);
    if (!peer)
        return -ESRCH;
    if (peer->uid() != m_euid)
        return -EACCES;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto descriptor = FileDescriptor::create(peer->ensure_tracer());
    m_fds[fd].set(move(descriptor), 0);
    return fd;
}

ProcessTracer& Process::ensure_tracer()
{
    if (!m_tracer)
        m_tracer = ProcessTracer::create(m_pid);
    return *m_tracer;
}

void Process::FileDescriptorAndFlags::clear()
{
    descriptor = nullptr;
    flags = 0;
}

void Process::FileDescriptorAndFlags::set(Retained<FileDescriptor>&& d, dword f)
{
    descriptor = move(d);
    flags = f;
}

int Process::sys$mknod(const char* pathname, mode_t mode, dev_t dev)
{
    if (!validate_read_str(pathname))
        return -EFAULT;

    return VFS::the().mknod(StringView(pathname), mode, dev, cwd_inode());
}
