/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Coredump.h>
#include <Kernel/Debug.h>
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
#    include <Kernel/Devices/KCOVDevice.h>
#endif
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KSyms.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Module.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/Thread.h>
#include <Kernel/ThreadTracer.h>
#include <LibC/errno_numbers.h>
#include <LibC/limits.h>

namespace Kernel {

static void create_signal_trampoline();

RecursiveSpinlock g_profiling_lock;
static Atomic<pid_t> next_pid;
static Singleton<SpinlockProtected<Process::List>> s_processes;
READONLY_AFTER_INIT HashMap<String, OwnPtr<Module>>* g_modules;
READONLY_AFTER_INIT Memory::Region* g_signal_trampoline_region;

static Singleton<MutexProtected<String>> s_hostname;

MutexProtected<String>& hostname()
{
    return *s_hostname;
}

SpinlockProtected<Process::List>& processes()
{
    return *s_processes;
}

ProcessID Process::allocate_pid()
{
    // Overflow is UB, and negative PIDs wreck havoc.
    // TODO: Handle PID overflow
    // For example: Use an Atomic<u32>, mask the most significant bit,
    // retry if PID is already taken as a PID, taken as a TID,
    // takes as a PGID, taken as a SID, or zero.
    return next_pid.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
}

UNMAP_AFTER_INIT void Process::initialize()
{
    g_modules = new HashMap<String, OwnPtr<Module>>;

    next_pid.store(0, AK::MemoryOrder::memory_order_release);

    hostname().with_exclusive([&](auto& name) {
        name = "courage";
    });

    create_signal_trampoline();
}

NonnullRefPtrVector<Process> Process::all_processes()
{
    NonnullRefPtrVector<Process> output;
    processes().with([&](const auto& list) {
        output.ensure_capacity(list.size_slow());
        for (const auto& process : list)
            output.append(NonnullRefPtr<Process>(process));
    });
    return output;
}

bool Process::in_group(gid_t gid) const
{
    return this->gid() == gid || extra_gids().contains_slow(gid);
}

void Process::kill_threads_except_self()
{
    InterruptDisabler disabler;

    if (thread_count() <= 1)
        return;

    auto current_thread = Thread::current();
    for_each_thread([&](Thread& thread) {
        if (&thread == current_thread)
            return;

        if (auto state = thread.state(); state == Thread::State::Dead
            || state == Thread::State::Dying)
            return;

        // We need to detach this thread in case it hasn't been joined
        thread.detach();
        thread.set_should_die();
    });

    u32 dropped_lock_count = 0;
    if (big_lock().force_unlock_if_locked(dropped_lock_count) != LockMode::Unlocked)
        dbgln("Process {} big lock had {} locks", *this, dropped_lock_count);
}

void Process::kill_all_threads()
{
    for_each_thread([&](Thread& thread) {
        // We need to detach this thread in case it hasn't been joined
        thread.detach();
        thread.set_should_die();
    });
}

void Process::register_new(Process& process)
{
    // Note: this is essentially the same like process->ref()
    RefPtr<Process> new_process = process;
    processes().with([&](auto& list) {
        list.prepend(process);
    });
}

RefPtr<Process> Process::create_user_process(RefPtr<Thread>& first_thread, const String& path, uid_t uid, gid_t gid, ProcessID parent_pid, int& error, Vector<String>&& arguments, Vector<String>&& environment, TTY* tty)
{
    auto parts = path.split('/');
    if (arguments.is_empty()) {
        arguments.append(parts.last());
    }

    RefPtr<Custody> cwd;
    if (auto parent = Process::from_pid(parent_pid))
        cwd = parent->m_cwd;
    if (!cwd)
        cwd = VirtualFileSystem::the().root_custody();

    auto process = Process::create(first_thread, parts.take_last(), uid, gid, parent_pid, false, move(cwd), nullptr, tty);
    if (!first_thread)
        return {};
    if (!process->m_fds.try_resize(process->m_fds.max_open())) {
        first_thread = nullptr;
        return {};
    }
    auto& device_to_use_as_tty = tty ? (CharacterDevice&)*tty : NullDevice::the();
    auto description = device_to_use_as_tty.open(O_RDWR).value();

    auto setup_description = [&process, &description](int fd) {
        process->m_fds.m_fds_metadatas[fd].allocate();
        process->m_fds[fd].set(*description);
    };
    setup_description(0);
    setup_description(1);
    setup_description(2);

    error = process->exec(path, move(arguments), move(environment)).error();
    if (error != 0) {
        dbgln("Failed to exec {}: {}", path, error);
        first_thread = nullptr;
        return {};
    }

    register_new(*process);
    error = 0;

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    (void)process.leak_ref();

    return process;
}

RefPtr<Process> Process::create_kernel_process(RefPtr<Thread>& first_thread, String&& name, void (*entry)(void*), void* entry_data, u32 affinity, RegisterProcess do_register)
{
    auto process = Process::create(first_thread, move(name), (uid_t)0, (gid_t)0, ProcessID(0), true);
    if (!first_thread || !process)
        return {};
    first_thread->regs().set_ip((FlatPtr)entry);
#if ARCH(I386)
    first_thread->regs().esp = FlatPtr(entry_data); // entry function argument is expected to be in regs.esp
#else
    first_thread->regs().rdi = FlatPtr(entry_data); // entry function argument is expected to be in regs.rdi
#endif

    if (do_register == RegisterProcess::Yes)
        register_new(*process);

    SpinlockLocker lock(g_scheduler_lock);
    first_thread->set_affinity(affinity);
    first_thread->set_state(Thread::State::Runnable);
    return process;
}

void Process::protect_data()
{
    m_protected_data_refs.unref([&]() {
        MM.set_page_writable_direct(VirtualAddress { &this->m_protected_values }, false);
    });
}

void Process::unprotect_data()
{
    m_protected_data_refs.ref([&]() {
        MM.set_page_writable_direct(VirtualAddress { &this->m_protected_values }, true);
    });
}

RefPtr<Process> Process::create(RefPtr<Thread>& first_thread, const String& name, uid_t uid, gid_t gid, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty, Process* fork_parent)
{
    auto space = Memory::AddressSpace::try_create(fork_parent ? &fork_parent->address_space() : nullptr);
    if (!space)
        return {};
    auto process = adopt_ref_if_nonnull(new (nothrow) Process(name, uid, gid, ppid, is_kernel_process, move(cwd), move(executable), tty));
    if (!process)
        return {};
    auto result = process->attach_resources(space.release_nonnull(), first_thread, fork_parent);
    if (result.is_error())
        return {};
    return process;
}

Process::Process(const String& name, uid_t uid, gid_t gid, ProcessID ppid, bool is_kernel_process, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty)
    : m_name(move(name))
    , m_is_kernel_process(is_kernel_process)
    , m_executable(move(executable))
    , m_cwd(move(cwd))
    , m_tty(tty)
    , m_wait_blocker_set(*this)
{
    // Ensure that we protect the process data when exiting the constructor.
    ProtectedDataMutationScope scope { *this };

    m_protected_values.pid = allocate_pid();
    m_protected_values.ppid = ppid;
    m_protected_values.uid = uid;
    m_protected_values.gid = gid;
    m_protected_values.euid = uid;
    m_protected_values.egid = gid;
    m_protected_values.suid = uid;
    m_protected_values.sgid = gid;

    auto maybe_procfs_traits = ProcessProcFSTraits::try_create({}, make_weak_ptr());
    // NOTE: This can fail, but it should be very, *very* rare.
    VERIFY(!maybe_procfs_traits.is_error());
    m_procfs_traits = maybe_procfs_traits.release_value();

    dbgln_if(PROCESS_DEBUG, "Created new process {}({})", m_name, this->pid().value());
}

KResult Process::attach_resources(NonnullOwnPtr<Memory::AddressSpace>&& preallocated_space, RefPtr<Thread>& first_thread, Process* fork_parent)
{
    m_space = move(preallocated_space);
    if (fork_parent) {
        // NOTE: fork() doesn't clone all threads; the thread that called fork() becomes the only thread in the new process.
        first_thread = Thread::current()->clone(*this);
        if (!first_thread)
            return ENOMEM;
    } else {
        // NOTE: This non-forked code path is only taken when the kernel creates a process "manually" (at boot.)
        auto thread_or_error = Thread::try_create(*this);
        if (thread_or_error.is_error())
            return thread_or_error.error();
        first_thread = thread_or_error.release_value();
        first_thread->detach();
    }
    return KSuccess;
}

Process::~Process()
{
    unprotect_data();

    VERIFY(thread_count() == 0); // all threads should have been finalized
    VERIFY(!m_alarm_timer);

    PerformanceManager::add_process_exit_event(*this);
}

bool Process::unref() const
{
    // NOTE: We need to obtain the process list lock before doing anything,
    //       because otherwise someone might get in between us lowering the
    //       refcount and acquiring the lock.
    auto did_hit_zero = processes().with([&](auto& list) {
        auto new_ref_count = deref_base();
        if (new_ref_count > 0)
            return false;

        if (m_list_node.is_in_list())
            list.remove(*const_cast<Process*>(this));
        return true;
    });

    if (did_hit_zero)
        delete this;
    return did_hit_zero;
}

// Make sure the compiler doesn't "optimize away" this function:
extern void signal_trampoline_dummy() __attribute__((used));
void signal_trampoline_dummy()
{
#if ARCH(I386)
    // The trampoline preserves the current eax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // necessary to preserve it here.
    asm(
        ".intel_syntax noprefix\n"
        ".globl asm_signal_trampoline\n"
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
        ".globl asm_signal_trampoline_end\n"
        "asm_signal_trampoline_end:\n"
        ".att_syntax" ::"i"(Syscall::SC_sigreturn));
#elif ARCH(X86_64)
    // The trampoline preserves the current rax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // necessary to preserve it here.
    asm(
        ".intel_syntax noprefix\n"
        ".globl asm_signal_trampoline\n"
        "asm_signal_trampoline:\n"
        "push rbp\n"
        "mov rbp, rsp\n"
        "push rax\n"          // we have to store rax 'cause it might be the return value from a syscall
        "sub rsp, 8\n"        // align the stack to 16 bytes
        "mov rdi, [rbp+24]\n" // push the signal code
        "call [rbp+16]\n"     // call the signal handler
        "add rsp, 8\n"
        "mov rax, %P0\n"
        "int 0x82\n" // sigreturn syscall
        ".globl asm_signal_trampoline_end\n"
        "asm_signal_trampoline_end:\n"
        ".att_syntax" ::"i"(Syscall::SC_sigreturn));
#endif
}

extern "C" char const asm_signal_trampoline[];
extern "C" char const asm_signal_trampoline_end[];

void create_signal_trampoline()
{
    // NOTE: We leak this region.
    g_signal_trampoline_region = MM.allocate_kernel_region(PAGE_SIZE, "Signal trampolines", Memory::Region::Access::ReadWrite).leak_ptr();
    g_signal_trampoline_region->set_syscall_region(true);

    size_t trampoline_size = asm_signal_trampoline_end - asm_signal_trampoline;

    u8* code_ptr = (u8*)g_signal_trampoline_region->vaddr().as_ptr();
    memcpy(code_ptr, asm_signal_trampoline, trampoline_size);

    g_signal_trampoline_region->set_writable(false);
    g_signal_trampoline_region->remap();
}

void Process::crash(int signal, FlatPtr ip, bool out_of_memory)
{
    VERIFY(!is_dead());
    VERIFY(&Process::current() == this);

    if (out_of_memory) {
        dbgln("\033[31;1mOut of memory\033[m, killing: {}", *this);
    } else {
        if (ip >= kernel_load_base && g_kernel_symbols_available) {
            auto* symbol = symbolicate_kernel_address(ip);
            dbgln("\033[31;1m{:p}  {} +{}\033[0m\n", ip, (symbol ? symbol->name : "(k?)"), (symbol ? ip - symbol->address : 0));
        } else {
            dbgln("\033[31;1m{:p}  (?)\033[0m\n", ip);
        }
        dump_backtrace();
    }
    {
        ProtectedDataMutationScope scope { *this };
        m_protected_values.termination_signal = signal;
    }
    set_should_generate_coredump(!out_of_memory);
    address_space().dump_regions();
    VERIFY(is_user_process());
    die();
    // We can not return from here, as there is nowhere
    // to unwind to, so die right away.
    Thread::current()->die_if_needed();
    VERIFY_NOT_REACHED();
}

RefPtr<Process> Process::from_pid(ProcessID pid)
{
    return processes().with([&](const auto& list) -> RefPtr<Process> {
        for (auto& process : list) {
            if (process.pid() == pid)
                return &process;
        }
        return {};
    });
}

const Process::FileDescriptionAndFlags* Process::FileDescriptions::get_if_valid(size_t i) const
{
    SpinlockLocker lock(m_fds_lock);
    if (m_fds_metadatas.size() <= i)
        return nullptr;

    if (auto& metadata = m_fds_metadatas[i]; metadata.is_valid())
        return &metadata;

    return nullptr;
}
Process::FileDescriptionAndFlags* Process::FileDescriptions::get_if_valid(size_t i)
{
    SpinlockLocker lock(m_fds_lock);
    if (m_fds_metadatas.size() <= i)
        return nullptr;

    if (auto& metadata = m_fds_metadatas[i]; metadata.is_valid())
        return &metadata;

    return nullptr;
}

const Process::FileDescriptionAndFlags& Process::FileDescriptions::at(size_t i) const
{
    SpinlockLocker lock(m_fds_lock);
    VERIFY(m_fds_metadatas[i].is_allocated());
    return m_fds_metadatas[i];
}
Process::FileDescriptionAndFlags& Process::FileDescriptions::at(size_t i)
{
    SpinlockLocker lock(m_fds_lock);
    VERIFY(m_fds_metadatas[i].is_allocated());
    return m_fds_metadatas[i];
}

RefPtr<FileDescription> Process::FileDescriptions::file_description(int fd) const
{
    SpinlockLocker lock(m_fds_lock);
    if (fd < 0)
        return nullptr;
    if (static_cast<size_t>(fd) < m_fds_metadatas.size())
        return m_fds_metadatas[fd].description();
    return nullptr;
}

void Process::FileDescriptions::enumerate(Function<void(const FileDescriptionAndFlags&)> callback) const
{
    SpinlockLocker lock(m_fds_lock);
    for (auto& file_description_metadata : m_fds_metadatas) {
        callback(file_description_metadata);
    }
}

void Process::FileDescriptions::change_each(Function<void(FileDescriptionAndFlags&)> callback)
{
    SpinlockLocker lock(m_fds_lock);
    for (auto& file_description_metadata : m_fds_metadatas) {
        callback(file_description_metadata);
    }
}

size_t Process::FileDescriptions::open_count() const
{
    size_t count = 0;
    enumerate([&](auto& file_description_metadata) {
        if (file_description_metadata.is_valid())
            ++count;
    });
    return count;
}

KResultOr<Process::ScopedDescriptionAllocation> Process::FileDescriptions::allocate(int first_candidate_fd)
{
    SpinlockLocker lock(m_fds_lock);
    for (size_t i = first_candidate_fd; i < max_open(); ++i) {
        if (!m_fds_metadatas[i].is_allocated()) {
            m_fds_metadatas[i].allocate();
            return Process::ScopedDescriptionAllocation { static_cast<int>(i), &m_fds_metadatas[i] };
        }
    }
    return EMFILE;
}

Time kgettimeofday()
{
    return TimeManagement::now();
}

siginfo_t Process::wait_info()
{
    siginfo_t siginfo {};
    siginfo.si_signo = SIGCHLD;
    siginfo.si_pid = pid().value();
    siginfo.si_uid = uid();

    if (m_protected_values.termination_signal) {
        siginfo.si_status = m_protected_values.termination_signal;
        siginfo.si_code = CLD_KILLED;
    } else {
        siginfo.si_status = m_protected_values.termination_status;
        siginfo.si_code = CLD_EXITED;
    }
    return siginfo;
}

Custody& Process::current_directory()
{
    if (!m_cwd)
        m_cwd = VirtualFileSystem::the().root_custody();
    return *m_cwd;
}

KResultOr<NonnullOwnPtr<KString>> Process::get_syscall_path_argument(Userspace<char const*> user_path, size_t path_length) const
{
    if (path_length == 0)
        return EINVAL;
    if (path_length > PATH_MAX)
        return ENAMETOOLONG;
    auto string_or_error = try_copy_kstring_from_user(user_path, path_length);
    if (string_or_error.is_error())
        return string_or_error.error();
    return string_or_error.release_value();
}

KResultOr<NonnullOwnPtr<KString>> Process::get_syscall_path_argument(Syscall::StringArgument const& path) const
{
    Userspace<char const*> path_characters((FlatPtr)path.characters);
    return get_syscall_path_argument(path_characters, path.length);
}

bool Process::dump_core()
{
    VERIFY(is_dumpable());
    VERIFY(should_generate_coredump());
    dbgln("Generating coredump for pid: {}", pid().value());
    auto coredump_path = String::formatted("/tmp/coredump/{}_{}_{}", name(), pid().value(), kgettimeofday().to_truncated_seconds());
    auto coredump = Coredump::create(*this, coredump_path);
    if (!coredump)
        return false;
    return !coredump->write().is_error();
}

bool Process::dump_perfcore()
{
    VERIFY(is_dumpable());
    VERIFY(m_perf_event_buffer);
    dbgln("Generating perfcore for pid: {}", pid().value());

    // Try to generate a filename which isn't already used.
    auto base_filename = String::formatted("{}_{}", name(), pid().value());
    auto description_or_error = VirtualFileSystem::the().open(String::formatted("{}.profile", base_filename), O_CREAT | O_EXCL, 0400, current_directory(), UidAndGid { uid(), gid() });
    for (size_t attempt = 1; attempt < 10 && description_or_error.is_error(); ++attempt)
        description_or_error = VirtualFileSystem::the().open(String::formatted("{}.{}.profile", base_filename, attempt), O_CREAT | O_EXCL, 0400, current_directory(), UidAndGid { uid(), gid() });
    if (description_or_error.is_error()) {
        dbgln("Failed to generate perfcore for pid {}: Could not generate filename for the perfcore file.", pid().value());
        return false;
    }

    auto& description = *description_or_error.value();
    KBufferBuilder builder;
    if (!m_perf_event_buffer->to_json(builder)) {
        dbgln("Failed to generate perfcore for pid {}: Could not serialize performance events to JSON.", pid().value());
        return false;
    }

    auto json = builder.build();
    if (!json) {
        dbgln("Failed to generate perfcore for pid {}: Could not allocate buffer.", pid().value());
        return false;
    }
    auto json_buffer = UserOrKernelBuffer::for_kernel_buffer(json->data());
    if (description.write(json_buffer, json->size()).is_error()) {
        return false;
        dbgln("Failed to generate perfcore for pid {}: Cound not write to perfcore file.", pid().value());
    }

    dbgln("Wrote perfcore for pid {} to {}", pid().value(), description.absolute_path());
    return true;
}

void Process::finalize()
{
    VERIFY(Thread::current() == g_finalizer);

    dbgln_if(PROCESS_DEBUG, "Finalizing process {}", *this);

    if (is_dumpable()) {
        if (m_should_generate_coredump)
            dump_core();
        if (m_perf_event_buffer) {
            dump_perfcore();
            TimeManagement::the().disable_profile_timer();
        }
    }

    m_threads_for_coredump.clear();

    if (m_alarm_timer)
        TimerQueue::the().cancel_timer(m_alarm_timer.release_nonnull());
    m_fds.clear();
    m_tty = nullptr;
    m_executable = nullptr;
    m_cwd = nullptr;
    m_arguments.clear();
    m_environment.clear();

    m_state.store(State::Dead, AK::MemoryOrder::memory_order_release);

    {
        // FIXME: PID/TID BUG
        if (auto parent_thread = Thread::from_tid(ppid().value())) {
            if (!(parent_thread->m_signal_action_data[SIGCHLD].flags & SA_NOCLDWAIT))
                parent_thread->send_signal(SIGCHLD, this);
        }
    }

    if (!!ppid()) {
        if (auto parent = Process::from_pid(ppid())) {
            parent->m_ticks_in_user_for_dead_children += m_ticks_in_user + m_ticks_in_user_for_dead_children;
            parent->m_ticks_in_kernel_for_dead_children += m_ticks_in_kernel + m_ticks_in_kernel_for_dead_children;
        }
    }

    unblock_waiters(Thread::WaitBlocker::UnblockFlags::Terminated);

    m_space->remove_all_regions({});

    VERIFY(ref_count() > 0);
    // WaitBlockerSet::finalize will be in charge of dropping the last
    // reference if there are still waiters around, or whenever the last
    // waitable states are consumed. Unless there is no parent around
    // anymore, in which case we'll just drop it right away.
    m_wait_blocker_set.finalize();
}

void Process::disowned_by_waiter(Process& process)
{
    m_wait_blocker_set.disowned_by_waiter(process);
}

void Process::unblock_waiters(Thread::WaitBlocker::UnblockFlags flags, u8 signal)
{
    if (auto parent = Process::from_pid(ppid()))
        parent->m_wait_blocker_set.unblock(*this, flags, signal);
}

void Process::die()
{
    auto expected = State::Running;
    if (!m_state.compare_exchange_strong(expected, State::Dying, AK::memory_order_acquire)) {
        // It's possible that another thread calls this at almost the same time
        // as we can't always instantly kill other threads (they may be blocked)
        // So if we already were called then other threads should stop running
        // momentarily and we only really need to service the first thread
        return;
    }

    // Let go of the TTY, otherwise a slave PTY may keep the master PTY from
    // getting an EOF when the last process using the slave PTY dies.
    // If the master PTY owner relies on an EOF to know when to wait() on a
    // slave owner, we have to allow the PTY pair to be torn down.
    m_tty = nullptr;

    VERIFY(m_threads_for_coredump.is_empty());
    for_each_thread([&](auto& thread) {
        m_threads_for_coredump.append(thread);
    });

    processes().with([&](const auto& list) {
        for (auto it = list.begin(); it != list.end();) {
            auto& process = *it;
            ++it;
            if (process.has_tracee_thread(pid())) {
                dbgln_if(PROCESS_DEBUG, "Process {} ({}) is attached by {} ({}) which will exit", process.name(), process.pid(), name(), pid());
                process.stop_tracing();
                auto err = process.send_signal(SIGSTOP, this);
                if (err.is_error())
                    dbgln("Failed to send the SIGSTOP signal to {} ({})", process.name(), process.pid());
            }
        }
    });

    kill_all_threads();
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    KCOVDevice::free_process();
#endif
}

void Process::terminate_due_to_signal(u8 signal)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(signal < 32);
    VERIFY(&Process::current() == this);
    dbgln("Terminating {} due to signal {}", *this, signal);
    {
        ProtectedDataMutationScope scope { *this };
        m_protected_values.termination_status = 0;
        m_protected_values.termination_signal = signal;
    }
    die();
}

KResult Process::send_signal(u8 signal, Process* sender)
{
    // Try to send it to the "obvious" main thread:
    auto receiver_thread = Thread::from_tid(pid().value());
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
    return ESRCH;
}

RefPtr<Thread> Process::create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, OwnPtr<KString> name, u32 affinity, bool joinable)
{
    VERIFY((priority >= THREAD_PRIORITY_MIN) && (priority <= THREAD_PRIORITY_MAX));

    // FIXME: Do something with guard pages?

    auto thread_or_error = Thread::try_create(*this);
    if (thread_or_error.is_error())
        return {};

    auto thread = thread_or_error.release_value();
    thread->set_name(move(name));
    thread->set_affinity(affinity);
    thread->set_priority(priority);
    if (!joinable)
        thread->detach();

    auto& regs = thread->regs();
    regs.set_ip((FlatPtr)entry);
    regs.set_sp((FlatPtr)entry_data); // entry function argument is expected to be in the SP register

    SpinlockLocker lock(g_scheduler_lock);
    thread->set_state(Thread::State::Runnable);
    return thread;
}

void Process::FileDescriptionAndFlags::clear()
{
    // FIXME: Verify Process::m_fds_lock is locked!
    m_description = nullptr;
    m_flags = 0;
}

void Process::FileDescriptionAndFlags::set(NonnullRefPtr<FileDescription>&& description, u32 flags)
{
    // FIXME: Verify Process::m_fds_lock is locked!
    m_description = move(description);
    m_flags = flags;
}

void Process::set_tty(TTY* tty)
{
    m_tty = tty;
}

KResult Process::start_tracing_from(ProcessID tracer)
{
    auto thread_tracer = ThreadTracer::create(tracer);
    if (!thread_tracer)
        return ENOMEM;
    m_tracer = move(thread_tracer);
    return KSuccess;
}

void Process::stop_tracing()
{
    m_tracer = nullptr;
}

void Process::tracer_trap(Thread& thread, const RegisterState& regs)
{
    VERIFY(m_tracer.ptr());
    m_tracer->set_regs(regs);
    thread.send_urgent_signal_to_self(SIGTRAP);
}

bool Process::create_perf_events_buffer_if_needed()
{
    if (!m_perf_event_buffer) {
        m_perf_event_buffer = PerformanceEventBuffer::try_create_with_size(4 * MiB);
        m_perf_event_buffer->add_process(*this, ProcessEventType::Create);
    }
    return !!m_perf_event_buffer;
}

void Process::delete_perf_events_buffer()
{
    if (m_perf_event_buffer)
        m_perf_event_buffer = nullptr;
}

bool Process::remove_thread(Thread& thread)
{
    ProtectedDataMutationScope scope { *this };
    auto thread_cnt_before = m_protected_values.thread_count.fetch_sub(1, AK::MemoryOrder::memory_order_acq_rel);
    VERIFY(thread_cnt_before != 0);
    thread_list().with([&](auto& thread_list) {
        thread_list.remove(thread);
    });
    return thread_cnt_before == 1;
}

bool Process::add_thread(Thread& thread)
{
    ProtectedDataMutationScope scope { *this };
    bool is_first = m_protected_values.thread_count.fetch_add(1, AK::MemoryOrder::memory_order_relaxed) == 0;
    thread_list().with([&](auto& thread_list) {
        thread_list.append(thread);
    });
    return is_first;
}

void Process::set_dumpable(bool dumpable)
{
    if (dumpable == m_protected_values.dumpable)
        return;
    ProtectedDataMutationScope scope { *this };
    m_protected_values.dumpable = dumpable;
}

KResult Process::set_coredump_property(NonnullOwnPtr<KString> key, NonnullOwnPtr<KString> value)
{
    // Write it into the first available property slot.
    for (auto& slot : m_coredump_properties) {
        if (slot.key)
            continue;
        slot.key = move(key);
        slot.value = move(value);
        return KSuccess;
    }
    return ENOBUFS;
}

KResult Process::try_set_coredump_property(StringView key, StringView value)
{
    auto key_kstring = KString::try_create(key);
    auto value_kstring = KString::try_create(value);
    if (key_kstring && value_kstring)
        return set_coredump_property(key_kstring.release_nonnull(), value_kstring.release_nonnull());
    return ENOMEM;
};

}
