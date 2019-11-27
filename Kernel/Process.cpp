#include <AK/FileSystemPath.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIT.h>
#include <Kernel/Console.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/SharedMemory.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/IO.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KSyms.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessTracer.h>
#include <Kernel/RTC.h>
#include <Kernel/Scheduler.h>
#include <Kernel/SharedBuffer.h>
#include <Kernel/StdLib.h>
#include <Kernel/Syscall.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/InodeVMObject.h>
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibELF/ELFLoader.h>
#include <LibELF/exec_elf.h>

//#define DEBUG_POLL_SELECT
//#define DEBUG_IO
//#define TASK_DEBUG
//#define FORK_DEBUG
//#define SIGNAL_DEBUG
//#define SHARED_BUFFER_DEBUG

static void create_signal_trampolines();

static pid_t next_pid;
InlineLinkedList<Process>* g_processes;
static String* s_hostname;
static Lock* s_hostname_lock;
VirtualAddress g_return_to_ring3_from_signal_trampoline;
VirtualAddress g_return_to_ring0_from_signal_trampoline;

void Process::initialize()
{
    next_pid = 0;
    g_processes = new InlineLinkedList<Process>;
    s_hostname = new String("courage");
    s_hostname_lock = new Lock;

    create_signal_trampolines();
}

Vector<pid_t> Process::all_pids()
{
    Vector<pid_t> pids;
    InterruptDisabler disabler;
    pids.ensure_capacity(g_processes->size_slow());
    for (auto& process : *g_processes)
        pids.append(process.pid());
    return pids;
}

Vector<Process*> Process::all_processes()
{
    Vector<Process*> processes;
    InterruptDisabler disabler;
    processes.ensure_capacity(g_processes->size_slow());
    for (auto& process : *g_processes)
        processes.append(&process);
    return processes;
}

bool Process::in_group(gid_t gid) const
{
    return m_gids.contains(gid);
}

Range Process::allocate_range(VirtualAddress vaddr, size_t size)
{
    vaddr.mask(PAGE_MASK);
    size = PAGE_ROUND_UP(size);
    if (vaddr.is_null())
        return page_directory().range_allocator().allocate_anywhere(size);
    return page_directory().range_allocator().allocate_specific(vaddr, size);
}

static unsigned prot_to_region_access_flags(int prot)
{
    unsigned access = 0;
    if (prot & PROT_READ)
        access |= Region::Access::Read;
    if (prot & PROT_WRITE)
        access |= Region::Access::Write;
    if (prot & PROT_EXEC)
        access |= Region::Access::Execute;
    return access;
}

Region& Process::allocate_split_region(const Region& source_region, const Range& range, size_t offset_in_vmo)
{
    m_regions.append(Region::create_user_accessible(range, source_region.vmobject(), offset_in_vmo, source_region.name(), source_region.access()));
    return m_regions.last();
}

Region* Process::allocate_region(VirtualAddress vaddr, size_t size, const String& name, int prot, bool commit)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    m_regions.append(Region::create_user_accessible(range, name, prot_to_region_access_flags(prot)));
    m_regions.last().map(page_directory());
    if (commit)
        m_regions.last().commit();
    return &m_regions.last();
}

Region* Process::allocate_file_backed_region(VirtualAddress vaddr, size_t size, NonnullRefPtr<Inode> inode, const String& name, int prot)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    m_regions.append(Region::create_user_accessible(range, inode, name, prot_to_region_access_flags(prot)));
    m_regions.last().map(page_directory());
    return &m_regions.last();
}

Region* Process::allocate_region_with_vmo(VirtualAddress vaddr, size_t size, NonnullRefPtr<VMObject> vmo, size_t offset_in_vmo, const String& name, int prot)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    offset_in_vmo &= PAGE_MASK;
    m_regions.append(Region::create_user_accessible(range, move(vmo), offset_in_vmo, name, prot_to_region_access_flags(prot)));
    m_regions.last().map(page_directory());
    return &m_regions.last();
}

bool Process::deallocate_region(Region& region)
{
    InterruptDisabler disabler;
    for (int i = 0; i < m_regions.size(); ++i) {
        if (&m_regions[i] == &region) {
            m_regions.remove(i);
            return true;
        }
    }
    return false;
}

Region* Process::region_from_range(const Range& range)
{
    size_t size = PAGE_ROUND_UP(range.size());
    for (auto& region : m_regions) {
        if (region.vaddr() == range.base() && region.size() == size)
            return &region;
    }
    return nullptr;
}

Region* Process::region_containing(const Range& range)
{
    for (auto& region : m_regions) {
        if (region.contains(range))
            return &region;
    }
    return nullptr;
}

int Process::sys$set_mmap_name(void* addr, size_t size, const char* name)
{
    if (!validate_read_str(name))
        return -EFAULT;
    auto* region = region_from_range({ VirtualAddress((u32)addr), size });
    if (!region)
        return -EINVAL;
    if (!region->is_mmap())
        return -EPERM;
    region->set_name(String(name));
    return 0;
}

void* Process::sys$mmap(const Syscall::SC_mmap_params* params)
{
    if (!validate_read(params, sizeof(Syscall::SC_mmap_params)))
        return (void*)-EFAULT;

    auto& [addr, size, prot, flags, fd, offset, name] = *params;

    if (name && !validate_read_str(name))
        return (void*)-EFAULT;
    if (size == 0)
        return (void*)-EINVAL;
    if ((u32)addr & ~PAGE_MASK)
        return (void*)-EINVAL;
    if ((flags & MAP_SHARED) && (flags & MAP_PRIVATE))
        return (void*)-EINVAL;

    // EINVAL: MAP_STACK cannot be used with shared or file-backed mappings
    if ((flags & MAP_STACK) && ((flags & MAP_SHARED) || !(flags & MAP_PRIVATE) || !(flags & MAP_ANONYMOUS)))
        return (void*)-EINVAL;

    // EINVAL: MAP_STACK cannot be used with non-readable or non-writable memory
    if ((flags & MAP_STACK) && (!(prot & PROT_READ) || !(prot & PROT_WRITE)))
        return (void*)-EINVAL;

    // FIXME: The rest of this function seems like it could share more code..
    if (flags & MAP_ANONYMOUS) {
        auto* region = allocate_region(VirtualAddress((u32)addr), size, name ? name : "mmap", prot, false);
        if (!region)
            return (void*)-ENOMEM;
        if (flags & MAP_SHARED)
            region->set_shared(true);
        if (flags & MAP_STACK)
            region->set_stack(true);
        region->set_mmap(true);
        return region->vaddr().as_ptr();
    }
    if (offset & ~PAGE_MASK)
        return (void*)-EINVAL;
    auto* description = file_description(fd);
    if (!description)
        return (void*)-EBADF;
    auto region_or_error = description->mmap(*this, VirtualAddress((u32)addr), offset, size, prot);
    if (region_or_error.is_error())
        return (void*)(int)region_or_error.error();
    auto region = region_or_error.value();
    if (flags & MAP_SHARED)
        region->set_shared(true);
    if (name)
        region->set_name(name);
    region->set_mmap(true);
    return region->vaddr().as_ptr();
}

int Process::sys$munmap(void* addr, size_t size)
{
    Range range_to_unmap { VirtualAddress((u32)addr), size };
    if (auto* whole_region = region_from_range(range_to_unmap)) {
        if (!whole_region->is_mmap())
            return -EPERM;
        bool success = deallocate_region(*whole_region);
        ASSERT(success);
        return 0;
    }

    if (auto* old_region = region_containing(range_to_unmap)) {
        if (!old_region->is_mmap())
            return -EPERM;
        Range old_region_range = old_region->range();
        auto remaining_ranges_after_unmap = old_region_range.carve(range_to_unmap);
        ASSERT(!remaining_ranges_after_unmap.is_empty());
        auto make_replacement_region = [&](const Range& new_range) -> Region& {
            ASSERT(new_range.base() >= old_region_range.base());
            ASSERT(new_range.end() <= old_region_range.end());
            size_t new_range_offset_in_vmobject = old_region->offset_in_vmobject() + (new_range.base().get() - old_region_range.base().get());
            return allocate_split_region(*old_region, new_range, new_range_offset_in_vmobject);
        };
        Vector<Region*, 2> new_regions;
        for (auto& new_range : remaining_ranges_after_unmap) {
            new_regions.unchecked_append(&make_replacement_region(new_range));
        }

        // We manually unmap the old region here, specifying that we *don't* want the VM deallocated.
        old_region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);
        deallocate_region(*old_region);

        // Instead we give back the unwanted VM manually.
        page_directory().range_allocator().deallocate(range_to_unmap);

        // And finally we map the new region(s).
        for (auto* new_region : new_regions) {
            new_region->map(page_directory());
        }
        return 0;
    }

    // FIXME: We should also support munmap() across multiple regions. (#175)

    return -EINVAL;
}

int Process::sys$mprotect(void* addr, size_t size, int prot)
{
    auto* region = region_from_range({ VirtualAddress((u32)addr), size });
    if (!region)
        return -EINVAL;
    if (!region->is_mmap())
        return -EPERM;
    region->set_writable(prot & PROT_WRITE);
    region->remap();
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
    auto* child = new Process(String(m_name), m_uid, m_gid, m_pid, m_ring, m_cwd, m_executable, m_tty, this);

#ifdef FORK_DEBUG
    dbgprintf("fork: child=%p\n", child);
#endif

    for (auto& region : m_regions) {
#ifdef FORK_DEBUG
        dbg() << "fork: cloning Region{" << &region << "} '" << region.name() << "' @ " << region.vaddr();
#endif
        child->m_regions.append(region.clone());
        child->m_regions.last().map(child->page_directory());

        if (&region == m_master_tls_region)
            child->m_master_tls_region = &child->m_regions.last();
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
        for_each_thread([](Thread& thread) {
            dbgprintf("Thread{%p}: TID=%d, PID=%d\n", &thread, thread.tid(), thread.pid());
            return IterationDecision::Continue;
        });
        ASSERT(thread_count() == 1);
        ASSERT_NOT_REACHED();
    }

    size_t total_blob_size = 0;
    for (auto& a : arguments)
        total_blob_size += a.length() + 1;
    for (auto& e : environment)
        total_blob_size += e.length() + 1;

    size_t total_meta_size = sizeof(char*) * (arguments.size() + 1) + sizeof(char*) * (environment.size() + 1);

    // FIXME: How much stack space does process startup need?
    if ((total_blob_size + total_meta_size) >= Thread::default_userspace_stack_size)
        return -E2BIG;

    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    auto result = VFS::the().open(path, 0, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();
    auto metadata = description->metadata();

    if (!metadata.may_execute(m_euid, m_gids))
        return -EACCES;

    if (!metadata.size)
        return -ENOTIMPL;

    u32 entry_eip = 0;
    // FIXME: Is there a race here?
    auto old_page_directory = move(m_page_directory);
    m_page_directory = PageDirectory::create_for_userspace(*this);
#ifdef MM_DEBUG
    dbgprintf("Process %u exec: PD=%x created\n", pid(), m_page_directory.ptr());
#endif
    ProcessPagingScope paging_scope(*this);

    ASSERT(description->inode());
    auto vmo = InodeVMObject::create_with_inode(*description->inode());
    auto* region = allocate_region_with_vmo(VirtualAddress(), metadata.size, vmo, 0, description->absolute_path(), PROT_READ);
    ASSERT(region);

    // NOTE: We yank this out of 'm_regions' since we're about to manipulate the vector
    //       and we don't want it getting lost.
    auto executable_region = m_regions.take_last();

    Region* master_tls_region { nullptr };
    size_t master_tls_size = 0;
    size_t master_tls_alignment = 0;

    OwnPtr<ELFLoader> loader;
    {
        // Okay, here comes the sleight of hand, pay close attention..
        auto old_regions = move(m_regions);
        m_regions.append(move(executable_region));
        loader = make<ELFLoader>(region->vaddr().as_ptr());
        loader->map_section_hook = [&](VirtualAddress vaddr, size_t size, size_t alignment, size_t offset_in_image, bool is_readable, bool is_writable, bool is_executable, const String& name) -> u8* {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            int prot = 0;
            if (is_readable)
                prot |= PROT_READ;
            if (is_writable)
                prot |= PROT_WRITE;
            if (is_executable)
                prot |= PROT_EXEC;
            if (!allocate_region_with_vmo(vaddr, size, vmo, offset_in_image, String(name), prot))
                return nullptr;
            return vaddr.as_ptr();
        };
        loader->alloc_section_hook = [&](VirtualAddress vaddr, size_t size, size_t alignment, bool is_readable, bool is_writable, const String& name) -> u8* {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            int prot = 0;
            if (is_readable)
                prot |= PROT_READ;
            if (is_writable)
                prot |= PROT_WRITE;
            if (!allocate_region(vaddr, size, String(name), prot))
                return nullptr;
            return vaddr.as_ptr();
        };
        loader->tls_section_hook = [&](size_t size, size_t alignment) {
            ASSERT(size);
            master_tls_region = allocate_region({}, size, String(), PROT_READ | PROT_WRITE);
            master_tls_size = size;
            master_tls_alignment = alignment;
            return master_tls_region->vaddr().as_ptr();
        };
        bool success = loader->load();
        if (!success || !loader->entry().get()) {
            m_page_directory = move(old_page_directory);
            // FIXME: RAII this somehow instead.
            ASSERT(&current->process() == this);
            MM.enter_process_paging_scope(*this);
            executable_region = m_regions.take_first();
            m_regions = move(old_regions);
            kprintf("do_exec: Failure loading %s\n", path.characters());
            return -ENOEXEC;
        }

        // NOTE: At this point, we've committed to the new executable.
        entry_eip = loader->entry().get();
    }

    m_elf_loader = move(loader);
    m_executable = description->custody();

    // Copy of the master TLS region that we will clone for new threads
    m_master_tls_region = master_tls_region;

    if (metadata.is_setuid())
        m_euid = metadata.uid;
    if (metadata.is_setgid())
        m_egid = metadata.gid;

    current->set_default_signal_dispositions();
    current->m_signal_mask = 0;
    current->m_pending_signals = 0;

    for (int i = 0; i < m_fds.size(); ++i) {
        auto& daf = m_fds[i];
        if (daf.description && daf.flags & FD_CLOEXEC) {
            daf.description->close();
            daf = {};
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
    u32 old_esp0 = main_thread().m_tss.esp0;

    m_master_tls_size = master_tls_size;
    m_master_tls_alignment = master_tls_alignment;

    main_thread().make_thread_specific_region({});

    memset(&main_thread().m_tss, 0, sizeof(main_thread().m_tss));
    main_thread().m_tss.eflags = 0x0202;
    main_thread().m_tss.eip = entry_eip;
    main_thread().m_tss.cs = 0x1b;
    main_thread().m_tss.ds = 0x23;
    main_thread().m_tss.es = 0x23;
    main_thread().m_tss.fs = 0x23;
    main_thread().m_tss.gs = thread_specific_selector() | 3;
    main_thread().m_tss.ss = 0x23;
    main_thread().m_tss.cr3 = page_directory().cr3();
    main_thread().make_userspace_stack_for_main_thread(move(arguments), move(environment));
    main_thread().m_tss.ss0 = 0x10;
    main_thread().m_tss.esp0 = old_esp0;
    main_thread().m_tss.ss2 = m_pid;

#ifdef TASK_DEBUG
    kprintf("Process %u (%s) exec'd %s @ %p\n", pid(), name().characters(), path.characters(), main_thread().tss().eip);
#endif

    main_thread().set_state(Thread::State::Skip1SchedulerPass);
    big_lock().unlock_if_locked();
    return 0;
}

KResultOr<Vector<String>> Process::find_shebang_interpreter_for_executable(const String& executable_path)
{
    // FIXME: It's a bit sad that we'll open the executable twice (in case there's no shebang)
    //        Maybe we can find a way to plumb this opened FileDescription to the rest of the
    //        exec implementation..
    auto result = VFS::the().open(executable_path, 0, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();
    auto metadata = description->metadata();

    if (!metadata.may_execute(m_euid, m_gids))
        return KResult(-EACCES);

    if (metadata.size < 3)
        return KResult(-ENOEXEC);

    char first_page[PAGE_SIZE];
    int nread = description->read((u8*)&first_page, sizeof(first_page));
    int word_start = 2;
    int word_length = 0;
    if (nread > 2 && first_page[0] == '#' && first_page[1] == '!') {
        Vector<String> interpreter_words;

        for (int i = 2; i < nread; ++i) {
            if (first_page[i] == '\n') {
                break;
            }

            if (first_page[i] != ' ') {
                ++word_length;
            }

            if (first_page[i] == ' ') {
                if (word_length > 0) {
                    interpreter_words.append(String(&first_page[word_start], word_length));
                }
                word_length = 0;
                word_start = i + 1;
            }
        }

        if (word_length > 0)
            interpreter_words.append(String(&first_page[word_start], word_length));

        if (!interpreter_words.is_empty())
            return interpreter_words;
    }

    return KResult(-ENOEXEC);
}

int Process::exec(String path, Vector<String> arguments, Vector<String> environment)
{
    auto result = find_shebang_interpreter_for_executable(path);
    if (!result.is_error()) {
        Vector<String> new_arguments(result.value());

        new_arguments.append(path);

        arguments.remove(0);
        new_arguments.append(move(arguments));

        return exec(result.value().first(), move(new_arguments), move(environment));
    }

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
    RefPtr<Custody> cwd;
    {
        InterruptDisabler disabler;
        if (auto* parent = Process::from_pid(parent_pid))
            cwd = parent->m_cwd;
    }

    if (!cwd)
        cwd = VFS::the().root_custody();

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
    process->main_thread().tss().eip = (u32)e;

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

Process::Process(String&& name, uid_t uid, gid_t gid, pid_t ppid, RingLevel ring, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty, Process* fork_parent)
    : m_name(move(name))
    , m_pid(next_pid++) // FIXME: RACE: This variable looks racy!
    , m_uid(uid)
    , m_gid(gid)
    , m_euid(uid)
    , m_egid(gid)
    , m_ring(ring)
    , m_executable(move(executable))
    , m_cwd(move(cwd))
    , m_tty(tty)
    , m_ppid(ppid)
{
    dbgprintf("Process: New process PID=%u with name=%s\n", m_pid, m_name.characters());

    m_page_directory = PageDirectory::create_for_userspace(*this, fork_parent ? &fork_parent->page_directory().range_allocator() : nullptr);
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
            if (!fork_parent->m_fds[i].description)
                continue;
#ifdef FORK_DEBUG
            dbgprintf("fork: cloning fd %u... (%p) istty? %u\n", i, fork_parent->m_fds[i].description.ptr(), fork_parent->m_fds[i].description->is_tty());
#endif
            m_fds[i] = fork_parent->m_fds[i];
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
    for_each_thread([&my_threads](auto& thread) {
        my_threads.append(&thread);
        return IterationDecision::Continue;
    });
    for (auto* thread : my_threads)
        delete thread;
}

void Process::dump_regions()
{
    kprintf("Process %s(%u) regions:\n", name().characters(), pid());
    kprintf("BEGIN       END         SIZE        ACCESS  NAME\n");
    for (auto& region : m_regions) {
        kprintf("%08x -- %08x    %08x    %c%c%c%c%c     %s\n",
            region.vaddr().get(),
            region.vaddr().offset(region.size() - 1).get(),
            region.size(),
            region.is_readable() ? 'R' : ' ',
            region.is_writable() ? 'W' : ' ',
            region.is_executable() ? 'X' : ' ',
            region.is_shared() ? 'S' : ' ',
            region.is_stack() ? 'T' : ' ',
            region.name().characters());
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
    current->die_if_needed();
    ASSERT_NOT_REACHED();
}

void signal_trampoline_dummy(void)
{
    // The trampoline preserves the current eax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // neccessary to preserve it here.
    asm(
        ".intel_syntax noprefix\n"
        "asm_signal_trampoline:\n"
        "push ebp\n"
        "mov ebp, esp\n"
        "push eax\n"          // we have to store eax 'cause it might be the return value from a syscall
        "sub esp, 4\n"        // align the stack to 16 bytes
        "mov eax, [ebp+12]\n" // push the signal code
        "push eax\n"
        "call [ebp+8]\n" // call the signal handler
        "add esp, 8\n"
        "mov eax, %P0\n"
        "int 0x82\n" // sigreturn syscall
        "asm_signal_trampoline_end:\n"
        ".att_syntax" ::"i"(Syscall::SC_sigreturn));
}

extern "C" void asm_signal_trampoline(void);
extern "C" void asm_signal_trampoline_end(void);

void create_signal_trampolines()
{
    InterruptDisabler disabler;

    // NOTE: We leak this region.
    auto* trampoline_region = MM.allocate_user_accessible_kernel_region(PAGE_SIZE, "Signal trampolines").leak_ptr();
    g_return_to_ring3_from_signal_trampoline = trampoline_region->vaddr();

    u8* trampoline = (u8*)asm_signal_trampoline;
    u8* trampoline_end = (u8*)asm_signal_trampoline_end;
    size_t trampoline_size = trampoline_end - trampoline;

    u8* code_ptr = (u8*)trampoline_region->vaddr().as_ptr();
    memcpy(code_ptr, trampoline, trampoline_size);

    trampoline_region->set_writable(false);
    trampoline_region->remap();
}

int Process::sys$restore_signal_mask(u32 mask)
{
    current->m_signal_mask = mask;
    return 0;
}

int Process::sys$sigreturn(RegisterDump& registers)
{
    //Here, we restore the state pushed by dispatch signal and asm_signal_trampoline.
    u32* stack_ptr = (u32*)registers.esp_if_crossRing;
    u32 smuggled_eax = *stack_ptr;

    //pop the stored eax, ebp, return address, handler and signal code
    stack_ptr += 5;

    current->m_signal_mask = *stack_ptr;
    stack_ptr++;

    //pop edi, esi, ebp, esp, ebx, edx, ecx and eax
    memcpy(&registers.edi, stack_ptr, 8 * sizeof(u32));
    stack_ptr += 8;

    registers.eip = *stack_ptr;
    stack_ptr++;

    registers.eflags = *stack_ptr;
    stack_ptr++;

    registers.esp_if_crossRing = registers.esp;
    return smuggled_eax;
}

void Process::crash(int signal, u32 eip)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!is_dead());
    ASSERT(&current->process() == this);

    if (m_elf_loader && ksyms_ready)
        dbgprintf("\033[31;1m%p  %s\033[0m\n", eip, m_elf_loader->symbolicate(eip).characters());
    dump_backtrace();

    m_termination_signal = signal;
    dump_regions();
    ASSERT(is_ring3());
    die();
    // We can not return from here, as there is nowhere
    // to unwind to, so die right away.
    current->die_if_needed();
    ASSERT_NOT_REACHED();
}

Process* Process::from_pid(pid_t pid)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto& process : *g_processes) {
        if (process.pid() == pid)
            return &process;
    }
    return nullptr;
}

FileDescription* Process::file_description(int fd)
{
    if (fd < 0)
        return nullptr;
    if (fd < m_fds.size())
        return m_fds[fd].description.ptr();
    return nullptr;
}

const FileDescription* Process::file_description(int fd) const
{
    if (fd < 0)
        return nullptr;
    if (fd < m_fds.size())
        return m_fds[fd].description.ptr();
    return nullptr;
}

int Process::fd_flags(int fd) const
{
    if (fd < 0)
        return -1;
    if (fd < m_fds.size())
        return m_fds[fd].flags;
    return -1;
}

ssize_t Process::sys$get_dir_entries(int fd, void* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->get_dir_entries((u8*)buffer, size);
}

int Process::sys$lseek(int fd, off_t offset, int whence)
{
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->seek(offset, whence);
}

int Process::sys$ttyname_r(int fd, char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_tty())
        return -ENOTTY;
    auto tty_name = description->tty()->tty_name();
    if (size < tty_name.length() + 1)
        return -ERANGE;
    memcpy(buffer, tty_name.characters_without_null_termination(), tty_name.length());
    buffer[tty_name.length()] = '\0';
    return 0;
}

int Process::sys$ptsname_r(int fd, char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    auto* master_pty = description->master_pty();
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

    auto* description = file_description(fd);
    if (!description)
        return -EBADF;

    int nwritten = 0;
    for (int i = 0; i < iov_count; ++i) {
        int rc = do_write(*description, (const u8*)iov[i].iov_base, iov[i].iov_len);
        if (rc < 0) {
            if (nwritten == 0)
                return rc;
            return nwritten;
        }
        nwritten += rc;
    }

    return nwritten;
}

ssize_t Process::do_write(FileDescription& description, const u8* data, int data_size)
{
    ssize_t nwritten = 0;
    if (!description.is_blocking()) {
        if (!description.can_write())
            return -EAGAIN;
    }

    if (description.should_append()) {
#ifdef IO_DEBUG
        dbgprintf("seeking to end (O_APPEND)\n");
#endif
        description.seek(0, SEEK_END);
    }

    while (nwritten < data_size) {
#ifdef IO_DEBUG
        dbgprintf("while %u < %u\n", nwritten, size);
#endif
        if (!description.can_write()) {
#ifdef IO_DEBUG
            dbgprintf("block write on %d\n", fd);
#endif
            if (current->block<Thread::WriteBlocker>(description) == Thread::BlockResult::InterruptedBySignal) {
                if (nwritten == 0)
                    return -EINTR;
            }
        }
        ssize_t rc = description.write(data + nwritten, data_size - nwritten);
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
        nwritten += rc;
    }
    return nwritten;
}

ssize_t Process::sys$write(int fd, const u8* data, ssize_t size)
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
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;

    return do_write(*description, data, size);
}

ssize_t Process::sys$read(int fd, u8* buffer, ssize_t size)
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
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    if (description->is_directory())
        return -EISDIR;
    if (description->is_blocking()) {
        if (!description->can_read()) {
            if (current->block<Thread::ReadBlocker>(*description) == Thread::BlockResult::InterruptedBySignal)
                return -EINTR;
        }
    }
    return description->read(buffer, size);
}

int Process::sys$close(int fd)
{
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    int rc = description->close();
    m_fds[fd] = {};
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
    return VFS::the().utime(StringView(pathname), current_directory(), atime, mtime);
}

int Process::sys$access(const char* pathname, int mode)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().access(StringView(pathname), mode, current_directory());
}

int Process::sys$fcntl(int fd, int cmd, u32 arg)
{
    (void)cmd;
    (void)arg;
    dbgprintf("sys$fcntl: fd=%d, cmd=%d, arg=%u\n", fd, cmd, arg);
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    // NOTE: The FD flags are not shared between FileDescription objects.
    //       This means that dup() doesn't copy the FD_CLOEXEC flag!
    switch (cmd) {
    case F_DUPFD: {
        int arg_fd = (int)arg;
        if (arg_fd < 0)
            return -EINVAL;
        int new_fd = alloc_fd(arg_fd);
        if (new_fd < 0)
            return new_fd;
        m_fds[new_fd].set(*description);
        break;
    }
    case F_GETFD:
        return m_fds[fd].flags;
    case F_SETFD:
        m_fds[fd].flags = arg;
        break;
    case F_GETFL:
        return description->file_flags();
    case F_SETFL:
        description->set_file_flags(arg);
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
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->fstat(*statbuf);
}

int Process::sys$lstat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    auto metadata_or_error = VFS::the().lookup_metadata(StringView(path), current_directory(), O_NOFOLLOW_NOERROR);
    if (metadata_or_error.is_error())
        return metadata_or_error.error();
    return metadata_or_error.value().stat(*statbuf);
}

int Process::sys$stat(const char* path, stat* statbuf)
{
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    auto metadata_or_error = VFS::the().lookup_metadata(StringView(path), current_directory());
    if (metadata_or_error.is_error())
        return metadata_or_error.error();
    return metadata_or_error.value().stat(*statbuf);
}

int Process::sys$readlink(const char* path, char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_read_str(path))
        return -EFAULT;
    if (!validate_write(buffer, size))
        return -EFAULT;

    auto result = VFS::the().open(path, O_RDONLY | O_NOFOLLOW_NOERROR, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();

    if (!description->metadata().is_symlink())
        return -EINVAL;

    auto contents = description->read_entire_file();
    if (!contents)
        return -EIO; // FIXME: Get a more detailed error from VFS.

    memcpy(buffer, contents.data(), min(size, (ssize_t)contents.size()));
    if (contents.size() + 1 < size)
        buffer[contents.size()] = '\0';
    return 0;
}

int Process::sys$chdir(const char* path)
{
    if (!validate_read_str(path))
        return -EFAULT;
    auto directory_or_error = VFS::the().open_directory(StringView(path), current_directory());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    m_cwd = *directory_or_error.value();
    return 0;
}

int Process::sys$fchdir(int fd)
{
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;

    if (!description->is_directory())
        return -ENOTDIR;

    if (!description->metadata().may_execute(*this))
        return -EACCES;

    m_cwd = description->custody();
    return 0;
}

int Process::sys$getcwd(char* buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto path = current_directory().absolute_path();
    if (size < path.length() + 1)
        return -ERANGE;
    strcpy(buffer, path.characters());
    return 0;
}

int Process::number_of_open_file_descriptors() const
{
    int count = 0;
    for (auto& description : m_fds) {
        if (description)
            ++count;
    }
    return count;
}

int Process::sys$open(const Syscall::SC_open_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;
    auto& [path, path_length, options, mode] = *params;
    if (!path_length)
        return -EINVAL;
    if (!validate_read(path, path_length))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$open(\"%s\")\n", name().characters(), pid(), path);
#endif
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto result = VFS::the().open(path, options, mode & ~umask(), current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();
    if (options & O_DIRECTORY && !description->is_directory())
        return -ENOTDIR; // FIXME: This should be handled by VFS::open.
    description->set_file_flags(options);
    u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(description), fd_flags);
    return fd;
}

int Process::sys$openat(const Syscall::SC_openat_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;
    auto& [dirfd, path, path_length, options, mode] = *params;
    if (!validate_read(path, path_length))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$openat(%d, \"%s\")\n", dirfd, name().characters(), pid(), path);
#endif
    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto* base_description = file_description(dirfd);
        if (!base_description)
            return -EBADF;
        if (!base_description->is_directory())
            return -ENOTDIR;
        if (!base_description->custody())
            return -EINVAL;
        base = base_description->custody();
    }

    auto result = VFS::the().open(path, options, mode & ~umask(), *base);
    if (result.is_error())
        return result.error();
    auto description = result.value();
    if (options & O_DIRECTORY && !description->is_directory())
        return -ENOTDIR; // FIXME: This should be handled by VFS::open.
    description->set_file_flags(options);
    u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(description), fd_flags);
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

int Process::sys$pipe(int pipefd[2], int flags)
{
    if (!validate_write_typed(pipefd))
        return -EFAULT;
    if (number_of_open_file_descriptors() + 2 > max_open_file_descriptors())
        return -EMFILE;
    // Reject flags other than O_CLOEXEC.
    if ((flags & O_CLOEXEC) != flags)
        return -EINVAL;

    u32 fd_flags = (flags & O_CLOEXEC) ? FD_CLOEXEC : 0;
    auto fifo = FIFO::create(m_uid);

    int reader_fd = alloc_fd();
    m_fds[reader_fd].set(fifo->open_direction(FIFO::Direction::Reader), fd_flags);
    pipefd[0] = reader_fd;

    int writer_fd = alloc_fd();
    m_fds[writer_fd].set(fifo->open_direction(FIFO::Direction::Writer), fd_flags);
    pipefd[1] = writer_fd;

    return 0;
}

int Process::sys$killpg(int pgrp, int signum)
{
    if (signum < 1 || signum >= 32)
        return -EINVAL;
    if (pgrp < 0)
        return -EINVAL;

    InterruptDisabler disabler;

    return do_killpg(pgrp, signum);
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
    unsigned previous_alarm_remaining = 0;
    if (m_alarm_deadline && m_alarm_deadline > g_uptime) {
        previous_alarm_remaining = (m_alarm_deadline - g_uptime) / TICKS_PER_SECOND;
    }
    if (!seconds) {
        m_alarm_deadline = 0;
        return previous_alarm_remaining;
    }
    m_alarm_deadline = g_uptime + seconds * TICKS_PER_SECOND;
    return previous_alarm_remaining;
}

int Process::sys$uname(utsname* buf)
{
    if (!validate_write_typed(buf))
        return -EFAULT;
    strcpy(buf->sysname, "Serenity");
    strcpy(buf->release, "1.0-dev");
    strcpy(buf->version, "FIXME");
    strcpy(buf->machine, "i686");
    LOCKER(*s_hostname_lock);
    strncpy(buf->nodename, s_hostname->characters(), sizeof(utsname::nodename));
    return 0;
}

KResult Process::do_kill(Process& process, int signal)
{
    // FIXME: Allow sending SIGCONT to everyone in the process group.
    // FIXME: Should setuid processes have some special treatment here?
    if (!is_superuser() && m_euid != process.m_uid && m_uid != process.m_uid)
        return KResult(-EPERM);
    if (process.is_ring0() && signal == SIGKILL) {
        kprintf("%s(%u) attempted to send SIGKILL to ring 0 process %s(%u)\n", name().characters(), m_pid, process.name().characters(), process.pid());
        return KResult(-EPERM);
    }
    process.send_signal(signal, this);
    return KSuccess;
}

KResult Process::do_killpg(pid_t pgrp, int signal)
{
    ASSERT(pgrp >= 0);

    // Send the signal to all processes in the given group.
    if (pgrp == 0) {
        // Send the signal to our own pgrp.
        pgrp = pgid();
    }

    bool group_was_empty = true;
    bool any_succeeded = false;
    KResult error = KSuccess;

    Process::for_each_in_pgrp(pgrp, [&](auto& process) {
        group_was_empty = false;

        KResult res = do_kill(process, signal);
        if (res.is_success())
            any_succeeded = true;
        else
            error = res;

        return IterationDecision::Continue;
    });

    if (group_was_empty)
        return KResult(-ESRCH);
    if (any_succeeded)
        return KSuccess;
    return error;
}

int Process::sys$kill(pid_t pid, int signal)
{
    if (signal < 0 || signal >= 32)
        return -EINVAL;
    if (pid <= 0) {
        return do_killpg(-pid, signal);
    }
    if (pid == -1) {
        // FIXME: Send to all processes.
        ASSERT(pid != -1);
    }
    if (pid == m_pid) {
        // FIXME: If we ignore this signal anyway, we don't need to block here, right?
        current->send_signal(signal, this);
        (void)current->block<Thread::SemiPermanentBlocker>(Thread::SemiPermanentBlocker::Reason::Signal);
        return 0;
    }
    InterruptDisabler disabler;
    auto* peer = Process::from_pid(pid);
    if (!peer)
        return -ESRCH;
    return do_kill(*peer, signal);
}

int Process::sys$usleep(useconds_t usec)
{
    if (!usec)
        return 0;
    u64 wakeup_time = current->sleep(usec / 1000);
    if (wakeup_time > g_uptime)
        return -EINTR;
    return 0;
}

int Process::sys$sleep(unsigned seconds)
{
    if (!seconds)
        return 0;
    u64 wakeup_time = current->sleep(seconds * TICKS_PER_SECOND);
    if (wakeup_time > g_uptime) {
        u32 ticks_left_until_original_wakeup_time = wakeup_time - g_uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
    return 0;
}

timeval kgettimeofday()
{
    timeval tv;
    tv.tv_sec = RTC::boot_time() + PIT::seconds_since_boot();
    tv.tv_usec = PIT::ticks_this_second() * 1000;
    return tv;
}

void kgettimeofday(timeval& tv)
{
    tv = kgettimeofday();
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

        dbgprintf("reap: %s(%u) {%s}\n", process.name().characters(), process.pid(), process.main_thread().state_string());
        ASSERT(process.is_dead());
        g_processes->remove(&process);
    }
    delete &process;
    return exit_status;
}

pid_t Process::sys$waitpid(pid_t waitee, int* wstatus, int options)
{
    dbgprintf("sys$waitpid(%d, %p, %d)\n", waitee, wstatus, options);

    if (!options) {
        // FIXME: This can't be right.. can it? Figure out how this should actually work.
        options = WEXITED;
    }

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
        // FIXME: Figure out what WNOHANG should do with stopped children.
        if (waitee == -1) {
            pid_t reaped_pid = 0;
            InterruptDisabler disabler;
            for_each_child([&reaped_pid, &exit_status](Process& process) {
                if (process.is_dead()) {
                    reaped_pid = process.pid();
                    exit_status = reap(process);
                }
                return IterationDecision::Continue;
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

    pid_t waitee_pid = waitee;
    if (current->block<Thread::WaitBlocker>(options, waitee_pid) == Thread::BlockResult::InterruptedBySignal)
        return -EINTR;

    InterruptDisabler disabler;

    // NOTE: If waitee was -1, m_waitee_pid will have been filled in by the scheduler.
    Process* waitee_process = Process::from_pid(waitee_pid);
    if (!waitee_process)
        return -ECHILD;

    ASSERT(waitee_process);
    if (waitee_process->is_dead()) {
        exit_status = reap(*waitee_process);
    } else {
        ASSERT(waitee_process->main_thread().state() == Thread::State::Stopped);
        exit_status = 0x7f;
    }
    return waitee_pid;
}

enum class KernelMemoryCheckResult {
    NotInsideKernelMemory,
    AccessGranted,
    AccessDenied
};

static KernelMemoryCheckResult check_kernel_memory_access(VirtualAddress vaddr, bool is_write)
{
    auto& sections = multiboot_info_ptr->u.elf_sec;

    auto* kernel_program_headers = (Elf32_Phdr*)(sections.addr);
    for (unsigned i = 0; i < sections.num; ++i) {
        auto& segment = kernel_program_headers[i];
        if (segment.p_type != PT_LOAD || !segment.p_vaddr || !segment.p_memsz)
            continue;
        if (vaddr.get() < segment.p_vaddr || vaddr.get() > (segment.p_vaddr + segment.p_memsz))
            continue;
        if (is_write && !(kernel_program_headers[i].p_flags & PF_W))
            return KernelMemoryCheckResult::AccessDenied;
        if (!is_write && !(kernel_program_headers[i].p_flags & PF_R))
            return KernelMemoryCheckResult::AccessDenied;
        return KernelMemoryCheckResult::AccessGranted;
    }
    return KernelMemoryCheckResult::NotInsideKernelMemory;
}

bool Process::validate_read_from_kernel(VirtualAddress vaddr) const
{
    if (vaddr.is_null())
        return false;
    // We check extra carefully here since the first 4MB of the address space is identity-mapped.
    // This code allows access outside of the known used address ranges to get caught.
    auto kmc_result = check_kernel_memory_access(vaddr, false);
    if (kmc_result == KernelMemoryCheckResult::AccessGranted)
        return true;
    if (kmc_result == KernelMemoryCheckResult::AccessDenied)
        return false;
    if (is_kmalloc_address(vaddr.as_ptr()))
        return true;
    return validate_read(vaddr.as_ptr(), 1);
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
    VirtualAddress first_address((u32)address);
    VirtualAddress last_address = first_address.offset(size - 1);
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
    VirtualAddress first_address((u32)address);
    VirtualAddress last_address = first_address.offset(size - 1);
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
    Process::for_each_in_pgrp(pid(), [&](auto&) {
        found_process_with_same_pgid_as_my_pid = true;
        return IterationDecision::Break;
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
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->file().ioctl(*description, request, arg);
}

int Process::sys$getdtablesize()
{
    return m_max_open_file_descriptors;
}

int Process::sys$dup(int old_fd)
{
    auto* description = file_description(old_fd);
    if (!description)
        return -EBADF;
    int new_fd = alloc_fd(0);
    if (new_fd < 0)
        return new_fd;
    m_fds[new_fd].set(*description);
    return new_fd;
}

int Process::sys$dup2(int old_fd, int new_fd)
{
    auto* description = file_description(old_fd);
    if (!description)
        return -EBADF;
    if (new_fd < 0 || new_fd >= m_max_open_file_descriptors)
        return -EINVAL;
    m_fds[new_fd].set(*description);
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
    action.handler_or_sigaction = VirtualAddress((u32)act->sa_sigaction);
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
    return VFS::the().mkdir(StringView(pathname, pathname_length), mode & ~umask(), current_directory());
}

int Process::sys$realpath(const char* pathname, char* buffer, size_t size)
{
    if (!validate_read_str(pathname))
        return -EFAULT;

    size_t pathname_length = strlen(pathname);
    if (pathname_length == 0)
        return -EINVAL;
    if (pathname_length >= size)
        return -ENAMETOOLONG;
    if (!validate_write(buffer, size))
        return -EFAULT;

    auto custody_or_error = VFS::the().resolve_path(pathname, current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = custody_or_error.value();

    // FIXME: Once resolve_path is fixed to deal with .. and . , remove the use of FileSystemPath::canonical_path.
    FileSystemPath canonical_path(custody->absolute_path());
    if (!canonical_path.is_valid()) {
        dbg() << "FileSystemPath failed to canonicalize " << custody->absolute_path();
        ASSERT_NOT_REACHED();
    }

    strncpy(buffer, canonical_path.string().characters(), size);
    return 0;
};

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
    // FIXME: Return -EINVAL if timeout is invalid.
    if (!validate_read_typed(params))
        return -EFAULT;

    auto& [nfds, readfds, writefds, exceptfds, timeout] = *params;

    if (writefds && !validate_write_typed(writefds))
        return -EFAULT;
    if (readfds && !validate_write_typed(readfds))
        return -EFAULT;
    if (exceptfds && !validate_write_typed(exceptfds))
        return -EFAULT;
    if (timeout && !validate_read_typed(timeout))
        return -EFAULT;
    if (nfds < 0)
        return -EINVAL;

    timeval computed_timeout;
    bool select_has_timeout = false;
    if (timeout && (timeout->tv_sec || timeout->tv_usec)) {
        timeval_add(kgettimeofday(), *timeout, computed_timeout);
        select_has_timeout = true;
    }

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;
    Thread::SelectBlocker::FDVector efds;

    auto transfer_fds = [&](auto* fds, auto& vector) -> int {
        vector.clear_with_capacity();
        if (!fds)
            return 0;
        for (int fd = 0; fd < params->nfds; ++fd) {
            if (FD_ISSET(fd, fds)) {
                if (!file_description(fd))
                    return -EBADF;
                vector.append(fd);
            }
        }
        return 0;
    };
    if (int error = transfer_fds(writefds, wfds))
        return error;
    if (int error = transfer_fds(readfds, rfds))
        return error;
    if (int error = transfer_fds(exceptfds, efds))
        return error;

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbgprintf("%s<%u> selecting on (read:%u, write:%u), timeout=%p\n", name().characters(), pid(), rfds.size(), wfds.size(), timeout);
#endif

    if (!timeout || select_has_timeout) {
        if (current->block<Thread::SelectBlocker>(computed_timeout, select_has_timeout, rfds, wfds, efds) == Thread::BlockResult::InterruptedBySignal)
            return -EINTR;
    }

    int marked_fd_count = 0;
    auto mark_fds = [&](auto* fds, auto& vector, auto should_mark) {
        if (!fds)
            return;
        FD_ZERO(fds);
        for (int fd : vector) {
            if (auto* description = file_description(fd); description && should_mark(*description)) {
                FD_SET(fd, fds);
                ++marked_fd_count;
            }
        }
    };
    mark_fds(readfds, rfds, [](auto& description) { return description.can_read(); });
    mark_fds(writefds, wfds, [](auto& description) { return description.can_write(); });
    // FIXME: We should also mark exceptfds as appropriate.

    return marked_fd_count;
}

int Process::sys$poll(pollfd* fds, int nfds, int timeout)
{
    if (!validate_read_typed(fds))
        return -EFAULT;

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;

    for (int i = 0; i < nfds; ++i) {
        if (fds[i].events & POLLIN)
            rfds.append(fds[i].fd);
        if (fds[i].events & POLLOUT)
            wfds.append(fds[i].fd);
    }

    timeval actual_timeout;
    bool has_timeout = false;
    if (timeout >= 0) {
        // poll is in ms, we want s/us.
        struct timeval tvtimeout;
        tvtimeout.tv_sec = 0;
        while (timeout >= 1000) {
            tvtimeout.tv_sec += 1;
            timeout -= 1000;
        }
        tvtimeout.tv_usec = timeout * 1000;
        timeval_add(kgettimeofday(), tvtimeout, actual_timeout);
        has_timeout = true;
    }

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbgprintf("%s<%u> polling on (read:%u, write:%u), timeout=%d\n", name().characters(), pid(), rfds.size(), wfds.size(), timeout);
#endif

    if (has_timeout || timeout < 0) {
        if (current->block<Thread::SelectBlocker>(actual_timeout, has_timeout, rfds, wfds, Thread::SelectBlocker::FDVector()) == Thread::BlockResult::InterruptedBySignal)
            return -EINTR;
    }

    int fds_with_revents = 0;

    for (int i = 0; i < nfds; ++i) {
        auto* description = file_description(fds[i].fd);
        if (!description) {
            fds[i].revents = POLLNVAL;
            continue;
        }
        fds[i].revents = 0;
        if (fds[i].events & POLLIN && description->can_read())
            fds[i].revents |= POLLIN;
        if (fds[i].events & POLLOUT && description->can_write())
            fds[i].revents |= POLLOUT;

        if (fds[i].revents)
            ++fds_with_revents;
    }

    return fds_with_revents;
}

Custody& Process::current_directory()
{
    if (!m_cwd)
        m_cwd = VFS::the().root_custody();
    return *m_cwd;
}

int Process::sys$link(const char* old_path, const char* new_path)
{
    if (!validate_read_str(old_path))
        return -EFAULT;
    if (!validate_read_str(new_path))
        return -EFAULT;
    return VFS::the().link(StringView(old_path), StringView(new_path), current_directory());
}

int Process::sys$unlink(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().unlink(StringView(pathname), current_directory());
}

int Process::sys$symlink(const char* target, const char* linkpath)
{
    if (!validate_read_str(target))
        return -EFAULT;
    if (!validate_read_str(linkpath))
        return -EFAULT;
    return VFS::the().symlink(StringView(target), StringView(linkpath), current_directory());
}

int Process::sys$rmdir(const char* pathname)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().rmdir(StringView(pathname), current_directory());
}

int Process::sys$read_tsc(u32* lsw, u32* msw)
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
    return VFS::the().chmod(StringView(pathname), mode, current_directory());
}

int Process::sys$fchmod(int fd, mode_t mode)
{
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->fchmod(mode);
}

int Process::sys$fchown(int fd, uid_t uid, gid_t gid)
{
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->chown(uid, gid);
}

int Process::sys$chown(const char* pathname, uid_t uid, gid_t gid)
{
    if (!validate_read_str(pathname))
        return -EFAULT;
    return VFS::the().chown(StringView(pathname), uid, gid, current_directory());
}

void Process::finalize()
{
    ASSERT(current == g_finalizer);
    dbgprintf("Finalizing Process %s(%u)\n", m_name.characters(), m_pid);

    m_fds.clear();
    m_tty = nullptr;
    m_executable = nullptr;
    m_cwd = nullptr;
    m_elf_loader = nullptr;

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
    // Let go of the TTY, otherwise a slave PTY may keep the master PTY from
    // getting an EOF when the last process using the slave PTY dies.
    // If the master PTY owner relies on an EOF to know when to wait() on a
    // slave owner, we have to allow the PTY pair to be torn down.
    m_tty = nullptr;

    if (m_tracer)
        m_tracer->set_dead();

    {
        // Tell the threads to unwind and die.
        InterruptDisabler disabler;
        for_each_thread([](Thread& thread) {
            thread.set_should_die();
            return IterationDecision::Continue;
        });
    }
}

size_t Process::amount_virtual() const
{
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.size();
    }
    return amount;
}

size_t Process::amount_resident() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.amount_resident();
    }
    return amount;
}

size_t Process::amount_shared() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    // FIXME: It doesn't work at the moment, since it relies on PhysicalPage ref counts,
    //        and each PhysicalPage is only reffed by its VMObject. This needs to be refactored
    //        so that every Region contributes +1 ref to each of its PhysicalPages.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.amount_shared();
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
    auto description = FileDescription::create(*result.value());
    unsigned flags = 0;
    if (type & SOCK_CLOEXEC)
        flags |= FD_CLOEXEC;
    if (type & SOCK_NONBLOCK)
        description->set_blocking(false);
    m_fds[fd].set(move(description), flags);
    return fd;
}

int Process::sys$bind(int sockfd, const sockaddr* address, socklen_t address_length)
{
    if (!validate_read(address, address_length))
        return -EFAULT;
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    return socket.bind(address, address_length);
}

int Process::sys$listen(int sockfd, int backlog)
{
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    if (socket.is_connected())
        return -EINVAL;
    return socket.listen(backlog);
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
    auto* accepting_socket_description = file_description(accepting_socket_fd);
    if (!accepting_socket_description)
        return -EBADF;
    if (!accepting_socket_description->is_socket())
        return -ENOTSOCK;
    auto& socket = *accepting_socket_description->socket();
    if (!socket.can_accept()) {
        if (accepting_socket_description->is_blocking()) {
            if (current->block<Thread::AcceptBlocker>(*accepting_socket_description) == Thread::BlockResult::InterruptedBySignal)
                return -EINTR;
        } else {
            return -EAGAIN;
        }
    }
    auto accepted_socket = socket.accept();
    ASSERT(accepted_socket);
    bool success = accepted_socket->get_peer_address(address, address_size);
    ASSERT(success);
    auto accepted_socket_description = FileDescription::create(*accepted_socket);
    // NOTE: The accepted socket inherits fd flags from the accepting socket.
    //       I'm not sure if this matches other systems but it makes sense to me.
    accepted_socket_description->set_blocking(accepting_socket_description->is_blocking());
    m_fds[accepted_socket_fd].set(move(accepted_socket_description), m_fds[accepting_socket_fd].flags);

    // NOTE: Moving this state to Completed is what causes connect() to unblock on the client side.
    accepted_socket->set_setup_state(Socket::SetupState::Completed);
    return accepted_socket_fd;
}

int Process::sys$connect(int sockfd, const sockaddr* address, socklen_t address_size)
{
    if (!validate_read(address, address_size))
        return -EFAULT;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    return socket.connect(*description, address, address_size, description->is_blocking() ? ShouldBlock::Yes : ShouldBlock::No);
}

ssize_t Process::sys$sendto(const Syscall::SC_sendto_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    auto& [sockfd, data, data_length, flags, addr, addr_length] = *params;

    if (!validate_read(data, data_length))
        return -EFAULT;
    if (addr && !validate_read(addr, addr_length))
        return -EFAULT;
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    kprintf("sendto %p (%u), flags=%u, addr: %p (%u)\n", data, data_length, flags, addr, addr_length);
    return socket.sendto(*description, data, data_length, flags, addr, addr_length);
}

ssize_t Process::sys$recvfrom(const Syscall::SC_recvfrom_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    auto& [sockfd, buffer, buffer_length, flags, addr, addr_length] = *params;

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
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();

    bool original_blocking = description->is_blocking();
    if (flags & MSG_DONTWAIT)
        description->set_blocking(false);

    auto nrecv = socket.recvfrom(*description, buffer, buffer_length, flags, addr, addr_length);
    if (flags & MSG_DONTWAIT)
        description->set_blocking(original_blocking);

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

    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;

    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    if (!socket.get_local_address(addr, addrlen))
        return -EINVAL; // FIXME: Should this be another error? I'm not sure.

    return 0;
}

int Process::sys$getpeername(int sockfd, sockaddr* addr, socklen_t* addrlen)
{
    if (!validate_read_typed(addrlen))
        return -EFAULT;

    if (*addrlen <= 0)
        return -EINVAL;

    if (!validate_write(addr, *addrlen))
        return -EFAULT;

    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;

    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();

    if (socket.setup_state() != Socket::SetupState::Completed)
        return -ENOTCONN;

    if (!socket.get_peer_address(addr, addrlen))
        return -EINVAL; // FIXME: Should this be another error? I'm not sure.

    return 0;
}

int Process::sys$sched_setparam(pid_t pid, const struct sched_param* param)
{
    if (!validate_read_typed(param))
        return -EFAULT;

    InterruptDisabler disabler;
    auto* peer = this;
    if (pid != 0)
        peer = Process::from_pid(pid);

    if (!peer)
        return -ESRCH;

    if (!is_superuser() && m_euid != peer->m_uid && m_uid != peer->m_uid)
        return -EPERM;

    if (param->sched_priority < (int)ThreadPriority::First || param->sched_priority > (int)ThreadPriority::Last)
        return -EINVAL;

    peer->main_thread().set_priority((ThreadPriority)param->sched_priority);
    return 0;
}

int Process::sys$sched_getparam(pid_t pid, struct sched_param* param)
{
    if (!validate_read_typed(param))
        return -EFAULT;

    InterruptDisabler disabler;
    auto* peer = this;
    if (pid != 0)
        peer = Process::from_pid(pid);

    if (!peer)
        return -ESRCH;

    if (!is_superuser() && m_euid != peer->m_uid && m_uid != peer->m_uid)
        return -EPERM;

    param->sched_priority = (int)peer->main_thread().priority();
    return 0;
}

int Process::sys$getsockopt(const Syscall::SC_getsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    auto& [sockfd, level, option, value, value_size] = *params;

    if (!validate_write_typed(value_size))
        return -EFAULT;
    if (!validate_write(value, *value_size))
        return -EFAULT;
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    return socket.getsockopt(level, option, value, value_size);
}

int Process::sys$setsockopt(const Syscall::SC_setsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    auto& [sockfd, level, option, value, value_size] = *params;

    if (!validate_read(value, value_size))
        return -EFAULT;
    auto* description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    return socket.setsockopt(level, option, value, value_size);
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

int Process::sys$create_shared_buffer(int size, void** buffer)
{
    if (!size || size < 0)
        return -EINVAL;
    size = PAGE_ROUND_UP(size);
    if (!validate_write_typed(buffer))
        return -EFAULT;

    LOCKER(shared_buffers().lock());
    static int s_next_shared_buffer_id;
    int shared_buffer_id = ++s_next_shared_buffer_id;
    auto shared_buffer = make<SharedBuffer>(shared_buffer_id, size);
    shared_buffer->share_with(m_pid);
    *buffer = shared_buffer->ref_for_process_and_get_address(*this);
    ASSERT((int)shared_buffer->size() >= size);
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Created shared buffer %d @ %p (%u bytes, vmo is %u)\n", name().characters(), pid(), shared_buffer_id, *buffer, size, shared_buffer->size());
#endif
    shared_buffers().resource().set(shared_buffer_id, move(shared_buffer));

    return shared_buffer_id;
}

int Process::sys$share_buffer_with(int shared_buffer_id, pid_t peer_pid)
{
    if (!peer_pid || peer_pid < 0 || peer_pid == m_pid)
        return -EINVAL;
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    {
        InterruptDisabler disabler;
        auto* peer = Process::from_pid(peer_pid);
        if (!peer)
            return -ESRCH;
    }
    shared_buffer.share_with(peer_pid);
    return 0;
}

int Process::sys$share_buffer_globally(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    shared_buffer.share_globally();
    return 0;
}

int Process::sys$release_shared_buffer(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Releasing shared buffer %d, buffer count: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    shared_buffer.deref_for_process(*this);
    return 0;
}

void* Process::sys$get_shared_buffer(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return (void*)-EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return (void*)-EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Retaining shared buffer %d, buffer count: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    return shared_buffer.ref_for_process_and_get_address(*this);
}

int Process::sys$seal_shared_buffer(int shared_buffer_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
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
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Get shared buffer %d size: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    return shared_buffer.size();
}

void Process::terminate_due_to_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);
    dbgprintf("terminate_due_to_signal %s(%u) <- %u\n", name().characters(), pid(), signal);
    m_termination_status = 0;
    m_termination_signal = signal;
    die();
}

void Process::send_signal(u8 signal, Process* sender)
{
    // FIXME(Thread): Find the appropriate thread to deliver the signal to.
    main_thread().send_signal(signal, sender);
}

int Process::thread_count() const
{
    int count = 0;
    for_each_thread([&count](auto&) {
        ++count;
        return IterationDecision::Continue;
    });
    return count;
}

int Process::sys$create_thread(void* (*entry)(void*), void* argument, const Syscall::SC_create_thread_params* params)
{
    if (!validate_read((const void*)entry, sizeof(void*)))
        return -EFAULT;

    if (!validate_read_typed(params))
        return -EFAULT;

    u32 user_stack_address = reinterpret_cast<u32>(params->m_stack_location) + params->m_stack_size;

    if (!MM.validate_user_stack(*this, VirtualAddress(user_stack_address - 4)))
        return -EFAULT;

    // FIXME: return EAGAIN if Thread::all_threads().size() is greater than PTHREAD_THREADS_MAX

    ThreadPriority requested_thread_priority = static_cast<ThreadPriority>(params->m_schedule_priority);
    if (requested_thread_priority < ThreadPriority::First || requested_thread_priority > ThreadPriority::Last)
        return -EINVAL;

    if (requested_thread_priority != ThreadPriority::Normal && !is_superuser())
        return -EPERM;

    bool is_thread_joinable = (0 == params->m_detach_state);

    // FIXME: Do something with guard pages?

    auto* thread = new Thread(*this);

    thread->set_priority(requested_thread_priority);
    thread->set_joinable(is_thread_joinable);

    auto& tss = thread->tss();
    tss.eip = (u32)entry;
    tss.eflags = 0x0202;
    tss.cr3 = page_directory().cr3();
    tss.esp = user_stack_address;

    // NOTE: The stack needs to be 16-byte aligned.
    thread->push_value_on_stack((u32)argument);
    thread->push_value_on_stack(0);

    thread->make_thread_specific_region({});
    thread->set_state(Thread::State::Runnable);
    return thread->tid();
}

void Process::sys$exit_thread(void* exit_value)
{
    current->m_exit_value = exit_value;
    cli();
    if (&current->process().main_thread() == current) {
        // FIXME: For POSIXy reasons, we should only sys$exit once *all* threads have exited.
        sys$exit(0);
        return;
    }
    current->set_should_die();
    big_lock().unlock_if_locked();
    current->die_if_needed();
    ASSERT_NOT_REACHED();
}

int Process::sys$join_thread(int tid, void** exit_value)
{
    if (exit_value && !validate_write_typed(exit_value))
        return -EFAULT;

    Thread* thread = nullptr;
    for_each_thread([&](auto& child_thread) {
        if (child_thread.tid() == tid) {
            thread = &child_thread;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (!thread)
        return -ESRCH;

    if (thread == current)
        return -EDEADLK;

    if (thread->m_joinee == current)
        return -EDEADLK;

    ASSERT(thread->m_joiner != current);
    if (thread->m_joiner)
        return -EINVAL;

    if (!thread->is_joinable())
        return -EINVAL;

    void* joinee_exit_value = nullptr;

    // FIXME: pthread_join() should not be interruptable. Enforce this somehow?
    auto result = current->block<Thread::JoinBlocker>(*thread, joinee_exit_value);
    (void)result;

    // NOTE: 'thread' is very possibly deleted at this point. Clear it just to be safe.
    thread = nullptr;

    if (exit_value)
        *exit_value = joinee_exit_value;
    return 0;
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
    for_each_thread([&](Thread& thread) {
        if (thread.tid() == tid) {
            beneficiary = &thread;
            return IterationDecision::Break;
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
    return VFS::the().rename(StringView(oldpath), StringView(newpath), current_directory());
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
    auto description = FileDescription::create(shm_or_error.value());
    m_fds[fd].set(move(description), FD_CLOEXEC);
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
    auto* description = file_description(fd);
    if (!description)
        return -EBADF;
    // FIXME: Check that fd is writable, otherwise EINVAL.
    return description->truncate(length);
}

int Process::sys$watch_file(const char* path, int path_length)
{
    if (!validate_read(path, path_length))
        return -EFAULT;

    auto custody_or_error = VFS::the().resolve_path({ path, path_length }, current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& custody = custody_or_error.value();
    auto& inode = custody->inode();

    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    m_fds[fd].set(FileDescription::create(*InodeWatcher::create(inode)));
    return fd;
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
    auto description = FileDescription::create(peer->ensure_tracer());
    m_fds[fd].set(move(description), 0);
    return fd;
}

int Process::sys$halt()
{
    if (!is_superuser())
        return -EPERM;

    dbgprintf("acquiring FS locks...\n");
    FS::lock_all();
    dbgprintf("syncing mounted filesystems...\n");
    FS::sync();
    dbgprintf("attempting system shutdown...\n");
    IO::out16(0x604, 0x2000);

    return ESUCCESS;
}

int Process::sys$reboot()
{
    if (!is_superuser())
        return -EPERM;

    dbgprintf("acquiring FS locks...\n");
    FS::lock_all();
    dbgprintf("syncing mounted filesystems...\n");
    FS::sync();
    dbgprintf("attempting reboot via KB Controller...\n");
    IO::out8(0x64, 0xFE);

    return ESUCCESS;
}

int Process::sys$mount(const char* device_path, const char* mountpoint, const char* fstype)
{
    if (!is_superuser())
        return -EPERM;

    if (!validate_read_str(device_path) || !validate_read_str(mountpoint) || !validate_read_str(fstype))
        return -EFAULT;

    dbg() << "mount " << fstype << ": device " << device_path << " @ " << mountpoint;

    auto custody_or_error = VFS::the().resolve_path(mountpoint, current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& mountpoint_custody = custody_or_error.value();

    RefPtr<FS> fs { nullptr };

    if (strcmp(fstype, "ext2") == 0 || strcmp(fstype, "Ext2FS") == 0) {
        auto metadata_or_error = VFS::the().lookup_metadata(device_path, current_directory());
        if (metadata_or_error.is_error())
            return metadata_or_error.error();

        auto major = metadata_or_error.value().major_device;
        auto minor = metadata_or_error.value().minor_device;

        auto* device = Device::get_device(major, minor);
        if (!device) {
            dbg() << "mount: device (" << major << "," << minor << ") not found";
            return -ENODEV;
        }

        if (!device->is_disk_device()) {
            dbg() << "mount: device (" << major << "," << minor << ") is not a DiskDevice";
            return -ENODEV;
        }

        auto& disk_device = static_cast<DiskDevice&>(*device);

        dbg() << "mount: attempting to mount device (" << major << "," << minor << ") on " << mountpoint;

        fs = Ext2FS::create(disk_device);
    } else if (strcmp(fstype, "proc") == 0 || strcmp(fstype, "ProcFS") == 0)
        fs = ProcFS::create();
    else if (strcmp(fstype, "devpts") == 0 || strcmp(fstype, "DevPtsFS") == 0)
        fs = DevPtsFS::create();
    else if (strcmp(fstype, "tmp") == 0 || strcmp(fstype, "TmpFS") == 0)
        fs = TmpFS::create();
    else
        return -ENODEV;

    if (!fs->initialize()) {
        dbg() << "mount: failed to initialize " << fstype << " filesystem on " << device_path;
        return -ENODEV;
    }

    auto result = VFS::the().mount(fs.release_nonnull(), mountpoint_custody);
    dbg() << "mount: successfully mounted " << device_path << " on " << mountpoint;
    return result;
}

int Process::sys$umount(const char* mountpoint)
{
    if (!is_superuser())
        return -EPERM;

    if (!validate_read_str(mountpoint))
        return -EFAULT;

    auto metadata_or_error = VFS::the().lookup_metadata(mountpoint, current_directory());
    if (metadata_or_error.is_error())
        return metadata_or_error.error();

    auto guest_inode_id = metadata_or_error.value().inode;
    return VFS::the().unmount(guest_inode_id);
}

ProcessTracer& Process::ensure_tracer()
{
    if (!m_tracer)
        m_tracer = ProcessTracer::create(m_pid);
    return *m_tracer;
}

void Process::FileDescriptionAndFlags::clear()
{
    description = nullptr;
    flags = 0;
}

void Process::FileDescriptionAndFlags::set(NonnullRefPtr<FileDescription>&& d, u32 f)
{
    description = move(d);
    flags = f;
}

int Process::sys$mknod(const char* pathname, mode_t mode, dev_t dev)
{
    if (!validate_read_str(pathname))
        return -EFAULT;

    return VFS::the().mknod(StringView(pathname), mode, dev, current_directory());
}

int Process::sys$dump_backtrace()
{
    dump_backtrace();
    return 0;
}

int Process::sys$dbgputch(u8 ch)
{
    IO::out8(0xe9, ch);
    return 0;
}

int Process::sys$dbgputstr(const u8* characters, int length)
{
    if (!length)
        return 0;
    if (!validate_read(characters, length))
        return -EFAULT;
    for (int i = 0; i < length; ++i)
        IO::out8(0xe9, characters[i]);
    return 0;
}

KBuffer Process::backtrace(ProcessInspectionHandle& handle) const
{
    KBufferBuilder builder;
    for_each_thread([&](Thread& thread) {
        builder.appendf("Thread %d:\n", thread.tid());
        builder.append(thread.backtrace(handle));
        return IterationDecision::Continue;
    });
    return builder.build();
}

int Process::sys$set_process_icon(int icon_id)
{
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(icon_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    m_icon_id = icon_id;
    return 0;
}

int Process::sys$get_process_name(char* buffer, int buffer_size)
{
    if (buffer_size <= 0)
        return -EINVAL;

    if (!validate_write(buffer, buffer_size))
        return -EFAULT;

    if (m_name.length() >= buffer_size)
        return -ENAMETOOLONG;

    strncpy(buffer, m_name.characters(), buffer_size);
    return 0;
}

// We don't use the flag yet, but we could use it for distinguishing
// random source like Linux, unlike the OpenBSD equivalent. However, if we
// do, we should be able of the caveats that Linux has dealt with.
int Process::sys$getrandom(void* buffer, size_t buffer_size, unsigned int flags __attribute__((unused)))
{
    if (buffer_size <= 0)
        return -EINVAL;

    if (!validate_write(buffer, buffer_size))
        return -EFAULT;

    // We prefer to get whole words of entropy.
    // If the length is unaligned, we can work with bytes instead.
    // Mask out the bottom two bits for words.
    size_t words_len = buffer_size & ~3;
    if (words_len) {
        uint32_t* words = (uint32_t*)buffer;
        for (size_t i = 0; i < words_len / 4; i++)
            words[i] = RandomDevice::random_value();
    }
    // The remaining non-whole word bytes we can fill in.
    size_t bytes_len = buffer_size & 3;
    if (bytes_len) {
        uint8_t* bytes = (uint8_t*)buffer + words_len;
        // Get a whole word of entropy to use.
        uint32_t word = RandomDevice::random_value();
        for (size_t i = 0; i < bytes_len; i++)
            bytes[i] = ((uint8_t*)&word)[i];
    }

    return 0;
}

int Process::sys$setkeymap(char* map, char* shift_map, char* alt_map)
{
    if (!is_superuser())
        return -EPERM;

    if (!validate_read(map, 0x80))
        return -EFAULT;
    if (!validate_read(shift_map, 0x80))
        return -EFAULT;
    if (!validate_read(alt_map, 0x80))
        return -EFAULT;

    KeyboardDevice::the().set_maps(map, shift_map, alt_map);
    return 0;
}

int Process::sys$clock_gettime(clockid_t clock_id, timespec* ts)
{
    if (!validate_write_typed(ts))
        return -EFAULT;

    switch (clock_id) {
    case CLOCK_MONOTONIC:
        ts->tv_sec = g_uptime / TICKS_PER_SECOND;
        ts->tv_nsec = (g_uptime % TICKS_PER_SECOND) * 1000000;
        break;
    default:
        return -EINVAL;
    }

    return 0;
}

int Process::sys$clock_nanosleep(const Syscall::SC_clock_nanosleep_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    auto& [clock_id, flags, requested_sleep, remaining_sleep] = *params;

    if (requested_sleep && !validate_read_typed(requested_sleep))
        return -EFAULT;

    if (remaining_sleep && !validate_write_typed(remaining_sleep))
        return -EFAULT;

    bool is_absolute = flags & TIMER_ABSTIME;

    switch (clock_id) {
    case CLOCK_MONOTONIC: {
        u64 wakeup_time;
        if (is_absolute) {
            u64 time_to_wake = (requested_sleep->tv_sec * 1000 + requested_sleep->tv_nsec / 1000000);
            wakeup_time = current->sleep_until(time_to_wake);
        } else {
            u32 ticks_to_sleep = (requested_sleep->tv_sec * 1000 + requested_sleep->tv_nsec / 1000000);
            if (!ticks_to_sleep)
                return 0;
            wakeup_time = current->sleep(ticks_to_sleep);
        }
        if (wakeup_time > g_uptime) {
            u32 ticks_left = wakeup_time - g_uptime;
            if (!is_absolute && remaining_sleep) {
                remaining_sleep->tv_sec = ticks_left / TICKS_PER_SECOND;
                ticks_left -= remaining_sleep->tv_sec * TICKS_PER_SECOND;
                remaining_sleep->tv_nsec = ticks_left * 1000000;
            }
            return -EINTR;
        }
        return 0;
    }
    default:
        return -EINVAL;
    }
}

int Process::sys$sync()
{
    VFS::the().sync();
    return 0;
}

int Process::sys$putch(char ch)
{
    Console::the().put_char(ch);
    return 0;
}

int Process::sys$yield()
{
    current->yield_without_holding_big_lock();
    return 0;
}

int Process::sys$beep()
{
    Scheduler::beep();
    return 0;
}
