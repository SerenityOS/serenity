/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Demangle.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KSyms.h>
#include <Kernel/Module.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/SharedBuffer.h>
#include <Kernel/StdLib.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/SharedInodeVMObject.h>
#include <LibC/errno_numbers.h>
#include <LibC/limits.h>
#include <LibELF/Loader.h>

//#define DEBUG_IO
//#define DEBUG_POLL_SELECT
//#define MM_DEBUG
//#define PROCESS_DEBUG
//#define SIGNAL_DEBUG

namespace Kernel {

static void create_signal_trampolines();

RecursiveSpinLock g_processes_lock;
static Atomic<pid_t> next_pid;
InlineLinkedList<Process>* g_processes;
String* g_hostname;
Lock* g_hostname_lock;
VirtualAddress g_return_to_ring3_from_signal_trampoline;
HashMap<String, OwnPtr<Module>>* g_modules;

ProcessID Process::allocate_pid()
{
    // Overflow is UB, and negative PIDs wreck havoc.
    // TODO: Handle PID overflow
    // For example: Use an Atomic<u32>, mask the most significant bit,
    // retry if PID is already taken as a PID, taken as a TID,
    // takes as a PGID, taken as a SID, or zero.
    return next_pid.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
}

void Process::initialize()
{
    g_modules = new HashMap<String, OwnPtr<Module>>;

    next_pid.store(0, AK::MemoryOrder::memory_order_release);
    g_processes = new InlineLinkedList<Process>;
    g_process_groups = new InlineLinkedList<ProcessGroup>;
    g_hostname = new String("courage");
    g_hostname_lock = new Lock;

    create_signal_trampolines();
}

Vector<ProcessID> Process::all_pids()
{
    Vector<ProcessID> pids;
    ScopedSpinLock lock(g_processes_lock);
    pids.ensure_capacity((int)g_processes->size_slow());
    for (auto& process : *g_processes)
        pids.append(process.pid());
    return pids;
}

NonnullRefPtrVector<Process> Process::all_processes()
{
    NonnullRefPtrVector<Process> processes;
    ScopedSpinLock lock(g_processes_lock);
    processes.ensure_capacity((int)g_processes->size_slow());
    for (auto& process : *g_processes)
        processes.append(NonnullRefPtr<Process>(process));
    return processes;
}

bool Process::in_group(gid_t gid) const
{
    return m_gid == gid || m_extra_gids.contains_slow(gid);
}

Range Process::allocate_range(VirtualAddress vaddr, size_t size, size_t alignment)
{
    vaddr.mask(PAGE_MASK);
    size = PAGE_ROUND_UP(size);
    if (vaddr.is_null())
        return page_directory().range_allocator().allocate_anywhere(size, alignment);
    return page_directory().range_allocator().allocate_specific(vaddr, size);
}

Region& Process::allocate_split_region(const Region& source_region, const Range& range, size_t offset_in_vmobject)
{
    auto& region = add_region(Region::create_user_accessible(range, source_region.vmobject(), offset_in_vmobject, source_region.name(), source_region.access()));
    region.set_mmap(source_region.is_mmap());
    region.set_stack(source_region.is_stack());
    size_t page_offset_in_source_region = (offset_in_vmobject - source_region.offset_in_vmobject()) / PAGE_SIZE;
    for (size_t i = 0; i < region.page_count(); ++i) {
        if (source_region.should_cow(page_offset_in_source_region + i))
            region.set_should_cow(i, true);
    }
    return region;
}

Region* Process::allocate_region(const Range& range, const String& name, int prot, bool should_commit)
{
    ASSERT(range.is_valid());
    auto vmobject = AnonymousVMObject::create_with_size(range.size());
    auto region = Region::create_user_accessible(range, vmobject, 0, name, prot_to_region_access_flags(prot));
    region->map(page_directory());
    if (should_commit && !region->commit())
        return nullptr;
    return &add_region(move(region));
}

Region* Process::allocate_region(VirtualAddress vaddr, size_t size, const String& name, int prot, bool should_commit)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    return allocate_region(range, name, prot, should_commit);
}

Region* Process::allocate_region_with_vmobject(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, int prot)
{
    ASSERT(range.is_valid());
    size_t end_in_vmobject = offset_in_vmobject + range.size();
    if (end_in_vmobject <= offset_in_vmobject) {
        dbg() << "allocate_region_with_vmobject: Overflow (offset + size)";
        return nullptr;
    }
    if (offset_in_vmobject >= vmobject->size()) {
        dbg() << "allocate_region_with_vmobject: Attempt to allocate a region with an offset past the end of its VMObject.";
        return nullptr;
    }
    if (end_in_vmobject > vmobject->size()) {
        dbg() << "allocate_region_with_vmobject: Attempt to allocate a region with an end past the end of its VMObject.";
        return nullptr;
    }
    offset_in_vmobject &= PAGE_MASK;
    auto& region = add_region(Region::create_user_accessible(range, move(vmobject), offset_in_vmobject, name, prot_to_region_access_flags(prot)));
    region.map(page_directory());
    return &region;
}

Region* Process::allocate_region_with_vmobject(VirtualAddress vaddr, size_t size, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, int prot)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    return allocate_region_with_vmobject(range, move(vmobject), offset_in_vmobject, name, prot);
}

bool Process::deallocate_region(Region& region)
{
    OwnPtr<Region> region_protector;
    ScopedSpinLock lock(m_lock);

    if (m_region_lookup_cache.region == &region)
        m_region_lookup_cache.region = nullptr;
    for (size_t i = 0; i < m_regions.size(); ++i) {
        if (&m_regions[i] == &region) {
            region_protector = m_regions.unstable_take(i);
            return true;
        }
    }
    return false;
}

Region* Process::find_region_from_range(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    if (m_region_lookup_cache.range == range && m_region_lookup_cache.region)
        return m_region_lookup_cache.region;

    size_t size = PAGE_ROUND_UP(range.size());
    for (auto& region : m_regions) {
        if (region.vaddr() == range.base() && region.size() == size) {
            m_region_lookup_cache.range = range;
            m_region_lookup_cache.region = region.make_weak_ptr();
            return &region;
        }
    }
    return nullptr;
}

Region* Process::find_region_containing(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        if (region.contains(range))
            return &region;
    }
    return nullptr;
}

void Process::kill_threads_except_self()
{
    InterruptDisabler disabler;

    if (thread_count() <= 1)
        return;

    auto current_thread = Thread::current();
    for_each_thread([&](Thread& thread) {
        if (&thread == current_thread
            || thread.state() == Thread::State::Dead
            || thread.state() == Thread::State::Dying)
            return IterationDecision::Continue;

        // We need to detach this thread in case it hasn't been joined
        thread.detach();
        thread.set_should_die();
        return IterationDecision::Continue;
    });

    big_lock().clear_waiters();
}

void Process::kill_all_threads()
{
    for_each_thread([&](Thread& thread) {
        // We need to detach this thread in case it hasn't been joined
        thread.detach();
        thread.set_should_die();
        return IterationDecision::Continue;
    });
}

RefPtr<Process> Process::create_user_process(RefPtr<Thread>& first_thread, const String& path, uid_t uid, gid_t gid, ProcessID parent_pid, int& error, Vector<String>&& arguments, Vector<String>&& environment, TTY* tty)
{
    auto parts = path.split('/');
    if (arguments.is_empty()) {
        arguments.append(parts.last());
    }
    RefPtr<Custody> cwd;
    RefPtr<Custody> root;
    {
        ScopedSpinLock lock(g_processes_lock);
        if (auto parent = Process::from_pid(parent_pid)) {
            cwd = parent->m_cwd;
            root = parent->m_root_directory;
        }
    }

    if (!cwd)
        cwd = VFS::the().root_custody();

    if (!root)
        root = VFS::the().root_custody();

    auto process = adopt(*new Process(first_thread, parts.take_last(), uid, gid, parent_pid, false, move(cwd), nullptr, tty));
    process->m_fds.resize(m_max_open_file_descriptors);
    auto& device_to_use_as_tty = tty ? (CharacterDevice&)*tty : NullDevice::the();
    auto description = device_to_use_as_tty.open(O_RDWR).value();
    process->m_fds[0].set(*description);
    process->m_fds[1].set(*description);
    process->m_fds[2].set(*description);

    error = process->exec(path, move(arguments), move(environment));
    if (error != 0) {
        dbg() << "Failed to exec " << path << ": " << error;
        first_thread = nullptr;
        return {};
    }

    {
        ScopedSpinLock lock(g_processes_lock);
        g_processes->prepend(process);
        process->ref();
    }
    error = 0;
    return process;
}

NonnullRefPtr<Process> Process::create_kernel_process(RefPtr<Thread>& first_thread, String&& name, void (*e)(), u32 affinity)
{
    auto process = adopt(*new Process(first_thread, move(name), (uid_t)0, (gid_t)0, ProcessID(0), true));
    first_thread->tss().eip = (FlatPtr)e;

    if (process->pid() != 0) {
        ScopedSpinLock lock(g_processes_lock);
        g_processes->prepend(process);
        process->ref();
    }

    first_thread->set_affinity(affinity);
    first_thread->set_state(Thread::State::Runnable);
    return process;
}

Process::Process(RefPtr<Thread>& first_thread, const String& name, uid_t uid, gid_t gid, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty, Process* fork_parent)
    : m_name(move(name))
    , m_pid(allocate_pid())
    , m_euid(uid)
    , m_egid(gid)
    , m_uid(uid)
    , m_gid(gid)
    , m_suid(uid)
    , m_sgid(gid)
    , m_is_kernel_process(is_kernel_process)
    , m_executable(move(executable))
    , m_cwd(move(cwd))
    , m_tty(tty)
    , m_ppid(ppid)
{
#ifdef PROCESS_DEBUG
    dbg() << "Created new process " << m_name << "(" << m_pid.value() << ")";
#endif

    m_page_directory = PageDirectory::create_for_userspace(*this, fork_parent ? &fork_parent->page_directory().range_allocator() : nullptr);
#ifdef MM_DEBUG
    dbg() << "Process " << pid().value() << " ctor: PD=" << m_page_directory.ptr() << " created";
#endif

    if (fork_parent) {
        // NOTE: fork() doesn't clone all threads; the thread that called fork() becomes the only thread in the new process.
        first_thread = Thread::current()->clone(*this);
    } else {
        // NOTE: This non-forked code path is only taken when the kernel creates a process "manually" (at boot.)
        first_thread = adopt(*new Thread(*this));
        first_thread->detach();
    }
}

Process::~Process()
{
    ASSERT(!m_next && !m_prev);  // should have been reaped
    ASSERT(thread_count() == 0); // all threads should have been finalized
}

void Process::dump_regions()
{
    klog() << "Process regions:";
    klog() << "BEGIN       END         SIZE        ACCESS  NAME";

    ScopedSpinLock lock(m_lock);

    Vector<Region*> sorted_regions;
    sorted_regions.ensure_capacity(m_regions.size());
    for (auto& region : m_regions)
        sorted_regions.append(&region);
    quick_sort(sorted_regions, [](auto& a, auto& b) {
        return a->vaddr() < b->vaddr();
    });

    for (auto& sorted_region : sorted_regions) {
        auto& region = *sorted_region;
        klog() << String::format("%08x", region.vaddr().get()) << " -- " << String::format("%08x", region.vaddr().offset(region.size() - 1).get()) << "    " << String::format("%08x", region.size()) << "    " << (region.is_readable() ? 'R' : ' ') << (region.is_writable() ? 'W' : ' ') << (region.is_executable() ? 'X' : ' ') << (region.is_shared() ? 'S' : ' ') << (region.is_stack() ? 'T' : ' ') << (region.vmobject().is_purgeable() ? 'P' : ' ') << "    " << region.name().characters();
    }
    MM.dump_kernel_regions();
}

// Make sure the compiler doesn't "optimize away" this function:
extern void signal_trampoline_dummy(void);
void signal_trampoline_dummy(void)
{
    // The trampoline preserves the current eax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // necessary to preserve it here.
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
    auto* trampoline_region = MM.allocate_user_accessible_kernel_region(PAGE_SIZE, "Signal trampolines", Region::Access::Read | Region::Access::Write | Region::Access::Execute, false).leak_ptr();
    g_return_to_ring3_from_signal_trampoline = trampoline_region->vaddr();

    u8* trampoline = (u8*)asm_signal_trampoline;
    u8* trampoline_end = (u8*)asm_signal_trampoline_end;
    size_t trampoline_size = trampoline_end - trampoline;

    {
        SmapDisabler disabler;
        u8* code_ptr = (u8*)trampoline_region->vaddr().as_ptr();
        memcpy(code_ptr, trampoline, trampoline_size);
    }

    trampoline_region->set_writable(false);
    trampoline_region->remap();
}

void Process::crash(int signal, u32 eip, bool out_of_memory)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!is_dead());
    ASSERT(Process::current() == this);

    if (out_of_memory) {
        dbg() << "\033[31;1mOut of memory\033[m, killing: " << *this;
    } else {
        if (eip >= 0xc0000000 && g_kernel_symbols_available) {
            auto* symbol = symbolicate_kernel_address(eip);
            dbg() << "\033[31;1m" << String::format("%p", eip) << "  " << (symbol ? demangle(symbol->name) : "(k?)") << " +" << (symbol ? eip - symbol->address : 0) << "\033[0m\n";
        } else if (auto elf_bundle = this->elf_bundle()) {
            dbg() << "\033[31;1m" << String::format("%p", eip) << "  " << elf_bundle->elf_loader->symbolicate(eip) << "\033[0m\n";
        } else {
            dbg() << "\033[31;1m" << String::format("%p", eip) << "  (?)\033[0m\n";
        }
        dump_backtrace();
    }
    m_termination_signal = signal;
    dump_regions();
    ASSERT(is_user_process());
    die();
    // We can not return from here, as there is nowhere
    // to unwind to, so die right away.
    Thread::current()->die_if_needed();
    ASSERT_NOT_REACHED();
}

RefPtr<Process> Process::from_pid(ProcessID pid)
{
    ScopedSpinLock lock(g_processes_lock);
    for (auto& process : *g_processes) {
        process.pid();
        if (process.pid() == pid)
            return &process;
    }
    return {};
}

RefPtr<FileDescription> Process::file_description(int fd) const
{
    if (fd < 0)
        return nullptr;
    if (static_cast<size_t>(fd) < m_fds.size())
        return m_fds[fd].description();
    return nullptr;
}

int Process::fd_flags(int fd) const
{
    if (fd < 0)
        return -1;
    if (static_cast<size_t>(fd) < m_fds.size())
        return m_fds[fd].flags();
    return -1;
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

int Process::alloc_fd(int first_candidate_fd)
{
    for (int i = first_candidate_fd; i < (int)m_max_open_file_descriptors; ++i) {
        if (!m_fds[i])
            return i;
    }
    return -EMFILE;
}

timeval kgettimeofday()
{
    return g_timeofday;
}

void kgettimeofday(timeval& tv)
{
    tv = kgettimeofday();
}

siginfo_t Process::reap(Process& process)
{
    siginfo_t siginfo;
    memset(&siginfo, 0, sizeof(siginfo));
    siginfo.si_signo = SIGCHLD;
    siginfo.si_pid = process.pid().value();
    siginfo.si_uid = process.uid();

    if (process.m_termination_signal) {
        siginfo.si_status = process.m_termination_signal;
        siginfo.si_code = CLD_KILLED;
    } else {
        siginfo.si_status = process.m_termination_status;
        siginfo.si_code = CLD_EXITED;
    }

    ASSERT(g_processes_lock.is_locked());

    if (!!process.ppid()) {
        auto parent = Process::from_pid(process.ppid());
        if (parent) {
            parent->m_ticks_in_user_for_dead_children += process.m_ticks_in_user + process.m_ticks_in_user_for_dead_children;
            parent->m_ticks_in_kernel_for_dead_children += process.m_ticks_in_kernel + process.m_ticks_in_kernel_for_dead_children;
        }
    }

#ifdef PROCESS_DEBUG
    dbg() << "Reaping process " << process;
#endif
    ASSERT(process.is_dead());
    g_processes->remove(&process);
    process.unref();
    return siginfo;
}

Custody& Process::current_directory()
{
    if (!m_cwd)
        m_cwd = VFS::the().root_custody();
    return *m_cwd;
}

KResultOr<String> Process::get_syscall_path_argument(const char* user_path, size_t path_length) const
{
    if (path_length == 0)
        return KResult(-EINVAL);
    if (path_length > PATH_MAX)
        return KResult(-ENAMETOOLONG);
    auto copied_string = copy_string_from_user(user_path, path_length);
    if (copied_string.is_null())
        return KResult(-EFAULT);
    return copied_string;
}

KResultOr<String> Process::get_syscall_path_argument(const Syscall::StringArgument& path) const
{
    return get_syscall_path_argument(path.characters, path.length);
}

void Process::finalize()
{
    ASSERT(Thread::current() == g_finalizer);
#ifdef PROCESS_DEBUG
    dbg() << "Finalizing process " << *this;
#endif

    if (m_perf_event_buffer) {
        auto description_or_error = VFS::the().open(String::format("perfcore.%d", m_pid), O_CREAT | O_EXCL, 0400, current_directory(), UidAndGid { m_uid, m_gid });
        if (!description_or_error.is_error()) {
            auto& description = description_or_error.value();
            auto json = m_perf_event_buffer->to_json(m_pid, m_executable ? m_executable->absolute_path() : "");
            auto json_buffer = UserOrKernelBuffer::for_kernel_buffer(json.data());
            auto result = description->write(json_buffer, json.size());
            if (result.is_error()) {
                dbgln("Error while writing perfcore file: {}", result.error().error());
            }
        }
    }

    m_fds.clear();
    m_tty = nullptr;
    m_executable = nullptr;
    m_cwd = nullptr;
    m_root_directory = nullptr;
    m_root_directory_relative_to_global_root = nullptr;

    disown_all_shared_buffers();
    {
        InterruptDisabler disabler;
        // FIXME: PID/TID BUG
        if (auto parent_thread = Thread::from_tid(m_ppid.value())) {
            if (parent_thread->m_signal_action_data[SIGCHLD].flags & SA_NOCLDWAIT) {
                // NOTE: If the parent doesn't care about this process, let it go.
                m_ppid = 0;
            } else {
                parent_thread->send_signal(SIGCHLD, this);
            }
        }
    }

    {
        ScopedSpinLock lock(m_lock);
        m_regions.clear();
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

    kill_all_threads();
}

size_t Process::amount_dirty_private() const
{
    // FIXME: This gets a bit more complicated for Regions sharing the same underlying VMObject.
    //        The main issue I'm thinking of is when the VMObject has physical pages that none of the Regions are mapping.
    //        That's probably a situation that needs to be looked at in general.
    size_t amount = 0;
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        if (!region.is_shared())
            amount += region.amount_dirty();
    }
    return amount;
}

size_t Process::amount_clean_inode() const
{
    HashTable<const InodeVMObject*> vmobjects;
    {
        ScopedSpinLock lock(m_lock);
        for (auto& region : m_regions) {
            if (region.vmobject().is_inode())
                vmobjects.set(&static_cast<const InodeVMObject&>(region.vmobject()));
        }
    }
    size_t amount = 0;
    for (auto& vmobject : vmobjects)
        amount += vmobject->amount_clean();
    return amount;
}

size_t Process::amount_virtual() const
{
    size_t amount = 0;
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        amount += region.size();
    }
    return amount;
}

size_t Process::amount_resident() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    ScopedSpinLock lock(m_lock);
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
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        amount += region.amount_shared();
    }
    return amount;
}

size_t Process::amount_purgeable_volatile() const
{
    size_t amount = 0;
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        if (region.vmobject().is_purgeable() && static_cast<const PurgeableVMObject&>(region.vmobject()).is_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

size_t Process::amount_purgeable_nonvolatile() const
{
    size_t amount = 0;
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        if (region.vmobject().is_purgeable() && !static_cast<const PurgeableVMObject&>(region.vmobject()).is_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

void Process::terminate_due_to_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);
    dbg() << "Terminating " << *this << " due to signal " << signal;
    m_termination_status = 0;
    m_termination_signal = signal;
    die();
}

KResult Process::send_signal(u8 signal, Process* sender)
{
    InterruptDisabler disabler;
    Thread* receiver_thread;
    // Try to send it to the "obvious" main thread:
    receiver_thread = Thread::from_tid(m_pid.value());
    // If the main thread has died, there may still be other threads:
    if (!receiver_thread) {
        // The first one should be good enough.
        // Neither kill(2) nor kill(3) specify any selection precedure.
        for_each_thread([&receiver_thread](Thread& thread) -> IterationDecision {
            receiver_thread = &thread;
            return IterationDecision::Break;
        });
    }
    if (receiver_thread) {
        receiver_thread->send_signal(signal, sender);
        return KSuccess;
    }
    return KResult(-ESRCH);
}

RefPtr<Thread> Process::create_kernel_thread(void (*entry)(), u32 priority, const String& name, u32 affinity, bool joinable)
{
    ASSERT((priority >= THREAD_PRIORITY_MIN) && (priority <= THREAD_PRIORITY_MAX));

    // FIXME: Do something with guard pages?

    auto thread = adopt(*new Thread(*this));

    thread->set_name(name);
    thread->set_affinity(affinity);
    thread->set_priority(priority);
    if (!joinable)
        thread->detach();

    auto& tss = thread->tss();
    tss.eip = (FlatPtr)entry;

    thread->set_state(Thread::State::Runnable);
    return thread;
}

void Process::FileDescriptionAndFlags::clear()
{
    m_description = nullptr;
    m_flags = 0;
}

void Process::FileDescriptionAndFlags::set(NonnullRefPtr<FileDescription>&& description, u32 flags)
{
    m_description = move(description);
    m_flags = flags;
}

KBuffer Process::backtrace() const
{
    KBufferBuilder builder;
    for_each_thread([&](Thread& thread) {
        builder.appendf("Thread %d (%s):\n", thread.tid().value(), thread.name().characters());
        builder.append(thread.backtrace());
        return IterationDecision::Continue;
    });
    return builder.build();
}

Custody& Process::root_directory()
{
    if (!m_root_directory)
        m_root_directory = VFS::the().root_custody();
    return *m_root_directory;
}

Custody& Process::root_directory_relative_to_global_root()
{
    if (!m_root_directory_relative_to_global_root)
        m_root_directory_relative_to_global_root = root_directory();
    return *m_root_directory_relative_to_global_root;
}

void Process::set_root_directory(const Custody& root)
{
    m_root_directory = root;
}

Region& Process::add_region(NonnullOwnPtr<Region> region)
{
    auto* ptr = region.ptr();
    ScopedSpinLock lock(m_lock);
    m_regions.append(move(region));
    return *ptr;
}

void Process::set_tty(TTY* tty)
{
    m_tty = tty;
}

OwnPtr<Process::ELFBundle> Process::elf_bundle() const
{
    if (!m_executable)
        return nullptr;
    auto bundle = make<ELFBundle>();
    if (!m_executable->inode().shared_vmobject()) {
        return nullptr;
    }
    ASSERT(m_executable->inode().shared_vmobject());
    auto& vmobject = *m_executable->inode().shared_vmobject();
    bundle->region = MM.allocate_kernel_region_with_vmobject(const_cast<SharedInodeVMObject&>(vmobject), vmobject.size(), "ELF bundle", Region::Access::Read);
    if (!bundle->region)
        return nullptr;
    bundle->elf_loader = ELF::Loader::create(bundle->region->vaddr().as_ptr(), bundle->region->size());
    return bundle;
}

}
