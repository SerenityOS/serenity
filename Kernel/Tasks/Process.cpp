/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/POSIX/sys/limits.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BaseDevices.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/NullDevice.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Credentials.h>
#include <Kernel/Tasks/Coredump.h>
#include <Kernel/Tasks/HostnameContext.h>
#include <Kernel/Tasks/PerformanceEventBuffer.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ScopedProcessList.h>
#include <Kernel/Tasks/Thread.h>
#include <Kernel/Tasks/ThreadTracer.h>
#include <Kernel/Time/TimerQueue.h>
#include <Kernel/Version.h>

namespace Kernel {

static void create_signal_trampoline();

extern ProcessID g_init_pid;
extern bool g_in_system_shutdown;
extern KString* g_version_string;

RecursiveSpinlock<LockRank::None> g_profiling_lock {};
static Atomic<pid_t> next_pid;
static Singleton<SpinlockProtected<Process::AllProcessesList, LockRank::None>> s_all_instances;
READONLY_AFTER_INIT Memory::Region* g_signal_trampoline_region;

static RawPtr<HostnameContext> s_empty_kernel_hostname_context;

SpinlockProtected<Process::AllProcessesList, LockRank::None>& Process::all_instances()
{
    return *s_all_instances;
}

ErrorOr<void> Process::for_each_in_same_process_list(Function<ErrorOr<void>(Process&)> callback)
{
    return Process::current().m_scoped_process_list.with([&](auto const& list_ptr) -> ErrorOr<void> {
        ErrorOr<void> result {};
        if (list_ptr) {
            list_ptr->attached_processes().with([&](auto const& list) {
                for (auto& process : list) {
                    result = callback(process);
                    if (result.is_error())
                        break;
                }
            });
            return result;
        }
        all_instances().with([&](auto const& list) {
            for (auto& process : list) {
                result = callback(process);
                if (result.is_error())
                    break;
            }
        });
        return result;
    });
}

ErrorOr<void> Process::for_each_child_in_same_process_list(Function<ErrorOr<void>(Process&)> callback)
{
    ProcessID my_pid = pid();
    return m_scoped_process_list.with([&](auto const& list_ptr) -> ErrorOr<void> {
        ErrorOr<void> result {};
        if (list_ptr) {
            list_ptr->attached_processes().with([&](auto const& list) {
                for (auto& process : list) {
                    if (process.ppid() == my_pid || process.has_tracee_thread(pid()))
                        result = callback(process);
                    if (result.is_error())
                        break;
                }
            });
            return result;
        }
        all_instances().with([&](auto const& list) {
            for (auto& process : list) {
                if (process.ppid() == my_pid || process.has_tracee_thread(pid()))
                    result = callback(process);
                if (result.is_error())
                    break;
            }
        });
        return result;
    });
}

ErrorOr<void> Process::for_each_in_pgrp_in_same_process_list(ProcessGroupID pgid, Function<ErrorOr<void>(Process&)> callback)
{
    return m_scoped_process_list.with([&](auto const& list_ptr) -> ErrorOr<void> {
        ErrorOr<void> result {};
        if (list_ptr) {
            list_ptr->attached_processes().with([&](auto const& list) {
                for (auto& process : list) {
                    if (!process.is_dead() && process.pgid() == pgid)
                        result = callback(process);
                    if (result.is_error())
                        break;
                }
            });
            return result;
        }
        all_instances().with([&](auto const& list) {
            for (auto& process : list) {
                if (!process.is_dead() && process.pgid() == pgid)
                    result = callback(process);
                if (result.is_error())
                    break;
            }
        });
        return result;
    });
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
    next_pid.store(0, AK::MemoryOrder::memory_order_release);

    // NOTE: Initialize an empty hostname context for all kernel processes.
    s_empty_kernel_hostname_context = &MUST(HostnameContext::create_with_name(""sv)).leak_ref();

    // NOTE: Just allocate the kernel version string here so we never have to worry
    // about OOM conditions in the uname syscall.
    g_version_string = MUST(KString::formatted("{}.{}-dev", SERENITY_MAJOR_REVISION, SERENITY_MINOR_REVISION)).leak_ptr();

    create_signal_trampoline();
}

void Process::kill_threads_except_self()
{
    InterruptDisabler disabler;

    if (thread_count() <= 1)
        return;

    auto* current_thread = Thread::current();
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
    if (big_lock().force_unlock_exclusive_if_locked(dropped_lock_count) != LockMode::Unlocked)
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
    NonnullRefPtr<Process> const new_process = process;
    all_instances().with([&](auto& list) {
        list.prepend(process);
    });
}

ErrorOr<Process::ProcessAndFirstThread> Process::create_user_process(StringView path, UserID uid, GroupID gid, Vector<NonnullOwnPtr<KString>> arguments, Vector<NonnullOwnPtr<KString>> environment, NonnullRefPtr<VFSRootContext> vfs_root_context, NonnullRefPtr<HostnameContext> hostname_context, RefPtr<TTY> tty)
{
    auto parts = path.split_view('/');
    if (arguments.is_empty()) {
        auto last_part = TRY(KString::try_create(parts.last()));
        TRY(arguments.try_append(move(last_part)));
    }

    auto path_string = TRY(KString::try_create(path));

    auto vfs_root_context_root_custody = vfs_root_context->root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> {
        return custody;
    });
    auto [process, first_thread] = TRY(Process::create(parts.last(), uid, gid, ProcessID(0), false, vfs_root_context, hostname_context, vfs_root_context_root_custody, nullptr, tty));

    TRY(process->m_fds.with_exclusive([&](auto& fds) -> ErrorOr<void> {
        TRY(fds.try_resize(Process::OpenFileDescriptions::max_open()));

        // NOTE: If Device::base_devices() is returning nullptr, it means the null device is not attached which is a bug.
        VERIFY(Device::base_devices() != nullptr);
        auto& device_to_use_as_tty = tty ? (CharacterDevice&)*tty : Device::base_devices()->null_device;
        auto description = TRY(device_to_use_as_tty.open(O_RDWR));
        auto setup_description = [&](int fd) {
            fds.m_fds_metadatas[fd].allocate();
            fds[fd].set(*description);
        };
        setup_description(0);
        setup_description(1);
        setup_description(2);

        return {};
    }));

    Thread* new_main_thread = nullptr;
    InterruptsState previous_interrupts_state = InterruptsState::Enabled;
    TRY(process->exec(move(path_string), move(arguments), move(environment), new_main_thread, previous_interrupts_state));

    register_new(*process);

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    process->ref();

    {
        SpinlockLocker lock(g_scheduler_lock);
        new_main_thread->set_state(Thread::State::Runnable);
    }

    return ProcessAndFirstThread { move(process), move(first_thread) };
}

ErrorOr<Process::ProcessAndFirstThread> Process::create_kernel_process(StringView name, void (*entry)(void*), void* entry_data, u32 affinity, RegisterProcess do_register)
{
    VERIFY(s_empty_kernel_hostname_context);
    auto process_and_first_thread = TRY(Process::create(name, UserID(0), GroupID(0), ProcessID(0), true, VFSRootContext::empty_context_for_kernel_processes(), *s_empty_kernel_hostname_context));
    auto& process = *process_and_first_thread.process;
    auto& thread = *process_and_first_thread.first_thread;

    thread.regs().set_entry_function((FlatPtr)entry, (FlatPtr)entry_data);

    if (do_register == RegisterProcess::Yes)
        register_new(process);

    SpinlockLocker lock(g_scheduler_lock);
    thread.set_affinity(affinity);
    thread.set_state(Thread::State::Runnable);
    return process_and_first_thread;
}

void Process::protect_data()
{
    m_protected_data_refs.unref([&]() {
        MM.set_page_writable_direct(VirtualAddress { &this->m_protected_values_do_not_access_directly }, false);
    });
}

void Process::unprotect_data()
{
    m_protected_data_refs.ref([&]() {
        MM.set_page_writable_direct(VirtualAddress { &this->m_protected_values_do_not_access_directly }, true);
    });
}

ErrorOr<Process::ProcessAndFirstThread> Process::create_with_forked_name(UserID uid, GroupID gid, ProcessID ppid, bool is_kernel_process, NonnullRefPtr<VFSRootContext> vfs_root_context, NonnullRefPtr<HostnameContext> hostname_context, RefPtr<Custody> current_directory, RefPtr<Custody> executable, RefPtr<TTY> tty, Process* fork_parent)
{
    Process::Name name {};
    Process::current().name().with([&name](auto& process_name) {
        name.store_characters(process_name.representable_view());
    });
    return TRY(Process::create(name.representable_view(), uid, gid, ppid, is_kernel_process, move(vfs_root_context), move(hostname_context), current_directory, executable, tty, fork_parent));
}

ErrorOr<Process::ProcessAndFirstThread> Process::create(StringView name, UserID uid, GroupID gid, ProcessID ppid, bool is_kernel_process, NonnullRefPtr<VFSRootContext> vfs_root_context, NonnullRefPtr<HostnameContext> hostname_context, RefPtr<Custody> current_directory, RefPtr<Custody> executable, RefPtr<TTY> tty, Process* fork_parent)
{
    auto unveil_tree = UnveilNode { TRY(KString::try_create("/"sv)), UnveilMetadata(TRY(KString::try_create("/"sv))) };
    auto exec_unveil_tree = UnveilNode { TRY(KString::try_create("/"sv)), UnveilMetadata(TRY(KString::try_create("/"sv))) };
    auto credentials = TRY(Credentials::create(uid, gid, uid, gid, uid, gid, {}, fork_parent ? fork_parent->sid() : 0, fork_parent ? fork_parent->pgid() : 0));

    auto process = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Process(name, move(credentials), ppid, is_kernel_process, move(vfs_root_context), move(hostname_context), move(current_directory), move(executable), tty, move(unveil_tree), move(exec_unveil_tree), kgettimeofday())));

    OwnPtr<Memory::AddressSpace> new_address_space;
    if (fork_parent) {
        TRY(fork_parent->address_space().with([&](auto& parent_address_space) -> ErrorOr<void> {
            new_address_space = TRY(Memory::AddressSpace::try_create(*process, parent_address_space.ptr()));
            return {};
        }));
    } else {
        new_address_space = TRY(Memory::AddressSpace::try_create(*process, nullptr));
    }

    auto first_thread = TRY(process->attach_resources(new_address_space.release_nonnull(), fork_parent));

    return ProcessAndFirstThread { move(process), move(first_thread) };
}

Process::Process(StringView name, NonnullRefPtr<Credentials> credentials, ProcessID ppid, bool is_kernel_process, NonnullRefPtr<VFSRootContext> vfs_root_context, NonnullRefPtr<HostnameContext> hostname_context, RefPtr<Custody> current_directory, RefPtr<Custody> executable, RefPtr<TTY> tty, UnveilNode unveil_tree, UnveilNode exec_unveil_tree, UnixDateTime creation_time)
    : m_is_kernel_process(is_kernel_process)
    , m_executable(move(executable))
    , m_current_directory(move(current_directory))
    , m_creation_time(creation_time)
    , m_attached_vfs_root_context(move(vfs_root_context))
    , m_attached_hostname_context(move(hostname_context))
    , m_unveil_data(move(unveil_tree))
    , m_exec_unveil_data(move(exec_unveil_tree))
    , m_wait_blocker_set(*this)
{
    set_name(name);
    // Ensure that we protect the process data when exiting the constructor.
    with_mutable_protected_data([&](auto& protected_data) {
        protected_data.pid = allocate_pid();
        protected_data.ppid = ppid;
        protected_data.credentials = move(credentials);
        protected_data.tty = move(tty);
    });

    if constexpr (PROCESS_DEBUG) {
        this->name().with([&](auto& process_name) {
            dbgln("Created new process {}({})", process_name.representable_view(), this->pid().value());
        });
    }

    m_attached_vfs_root_context.with([](auto& context) {
        context->attach({});
    });

    m_attached_hostname_context.with([](auto& context) {
        context->set_attached({});
    });
}

ErrorOr<NonnullRefPtr<Thread>> Process::attach_resources(NonnullOwnPtr<Memory::AddressSpace>&& preallocated_space, Process* fork_parent)
{
    m_space.with([&](auto& space) {
        space = move(preallocated_space);
    });

    auto create_first_thread = [&] {
        if (fork_parent) {
            // NOTE: fork() doesn't clone all threads; the thread that called fork() becomes the only thread in the new process.
            return Thread::current()->clone(*this);
        }
        // NOTE: This non-forked code path is only taken when the kernel creates a process "manually" (at boot.)
        return Thread::create(*this);
    };

    auto first_thread = TRY(create_first_thread());

    if (!fork_parent) {
        // FIXME: Figure out if this is really necessary.
        first_thread->detach();
    }

    // This is not actually explicitly verified by any official documentation,
    // but it's not listed anywhere as being cleared, and rsync expects it to work like this.
    if (fork_parent)
        m_signal_action_data = fork_parent->m_signal_action_data;

    return first_thread;
}

Process::~Process()
{
    unprotect_data();

    VERIFY(thread_count() == 0); // all threads should have been finalized

    PerformanceManager::add_process_exit_event(*this);
}

// Make sure the compiler doesn't "optimize away" this function:
extern void signal_trampoline_dummy() __attribute__((used));
void signal_trampoline_dummy()
{
    // The trampoline preserves the current return value, and then calls the signal handler.
    // We do this because, when interrupting a blocking syscall, that syscall may return
    // some special error code; This error code would likely be overwritten by the signal handler,
    // so it's necessary to preserve it here.

    // Stack state:
    //   syscall return value (initialized with 0)  <- stack pointer + offset_to_return_value_slot
    //   __ucontext
    //   siginfo
    //   FPUState
    //   __ucontext*
    //   siginfo*
    //   signal number
    //   handler address                            <- stack pointer

    constexpr static auto offset_to_return_value_slot = sizeof(__ucontext) + sizeof(siginfo) + sizeof(FPUState) + 4 * sizeof(FlatPtr);

#if ARCH(X86_64)
    asm(
        ".intel_syntax noprefix\n"
        ".globl asm_signal_trampoline\n"
        "asm_signal_trampoline:\n"

        // we have to save rax 'cause it might be the return value from a syscall
        "mov [rsp+%P1], rax\n"
        // Pop the handler into rcx
        "pop rcx\n" // save handler
        // pop signal number into rdi (first param)
        "pop rdi\n"
        // pop siginfo* into rsi (second param)
        "pop rsi\n"
        // pop ucontext* into rdx (third param)
        "pop rdx\n"
        // Note that the stack is currently aligned to 16 bytes as we popped the extra entries above.
        // call the signal handler
        "call rcx\n"
        // Current stack state is just saved_rax, ucontext, signal_info, fpu_state.
        // syscall SC_sigreturn
        "mov rax, %P0\n"
        "syscall\n"
        ".globl asm_signal_trampoline_end\n"
        "asm_signal_trampoline_end:\n"
        ".att_syntax"
        :
        : "i"(Syscall::SC_sigreturn),
        "i"(offset_to_return_value_slot));
#elif ARCH(AARCH64)
    asm(
        ".global asm_signal_trampoline\n"
        "asm_signal_trampoline:\n"

        // Store x0 (return value from a syscall) into the register slot, such that we can return the correct value in sys$sigreturn.
        "str x0, [sp, %[offset_to_return_value_slot]]\n"
        // Load the handler address into x3.
        "ldr x3, [sp, #0]\n"
        // Load the signal number into the first argument.
        "ldr x0, [sp, #8]\n"
        // Load a pointer to the signal_info structure into the second argument.
        "ldr x1, [sp, #16]\n"
        // Load a pointer to the ucontext into the third argument.
        "ldr x2, [sp, #24]\n"
        // Pop the values off the stack.
        "add sp, sp, 32\n"
        // Call the signal handler.
        "blr x3\n"

        // Call sys$sigreturn.
        "mov x8, %[sigreturn_syscall_number]\n"
        "svc #0\n"
        // We should never return, so trap if we do return.
        "brk #0\n"
        "\n"
        ".global asm_signal_trampoline_end\n"
        "asm_signal_trampoline_end: \n" ::[sigreturn_syscall_number] "i"(Syscall::SC_sigreturn),
        [offset_to_return_value_slot] "i"(offset_to_return_value_slot));
#elif ARCH(RISCV64)
    asm(
        ".global asm_signal_trampoline\n"
        "asm_signal_trampoline:\n"

        // Store a0 (return value from a syscall) into the register slot, such that we can return the correct value in sys$sigreturn.
        "sd a0, %[offset_to_return_value_slot](sp)\n"
        // Load the handler address into t0.
        "ld t0, 0(sp)\n"
        // Load the signal number into the first argument.
        "ld a0, 8(sp)\n"
        // Load a pointer to the signal_info structure into the second argument.
        "ld a1, 16(sp)\n"
        // Load a pointer to the ucontext into the third argument.
        "ld a2, 24(sp)\n"
        // Pop the values off the stack.
        "addi sp, sp, 32\n"
        // Call the signal handler.
        "jalr t0\n"

        // Call sys$sigreturn.
        "li a7, %[sigreturn_syscall_number]\n"
        "ecall\n"
        // We should never return, so trap if we do return.
        "unimp\n"

        "\n"
        ".global asm_signal_trampoline_end\n"
        "asm_signal_trampoline_end: \n" ::[sigreturn_syscall_number] "i"(Syscall::SC_sigreturn),
        [offset_to_return_value_slot] "i"(offset_to_return_value_slot));
#else
#    error Unknown architecture
#endif
}

extern "C" char const asm_signal_trampoline[];
extern "C" char const asm_signal_trampoline_end[];

void create_signal_trampoline()
{
    // NOTE: We leak this region.
    g_signal_trampoline_region = MM.allocate_kernel_region(PAGE_SIZE, "Signal trampolines"sv, Memory::Region::Access::ReadWrite).release_value().leak_ptr();
    g_signal_trampoline_region->set_syscall_region(true);

    size_t trampoline_size = asm_signal_trampoline_end - asm_signal_trampoline;

    u8* code_ptr = (u8*)g_signal_trampoline_region->vaddr().as_ptr();
    memcpy(code_ptr, asm_signal_trampoline, trampoline_size);

    g_signal_trampoline_region->set_writable(false);
    g_signal_trampoline_region->remap();
}

void Process::crash(int signal, Optional<RegisterState const&> regs, bool out_of_memory)
{
    VERIFY(!is_dead());
    VERIFY(&Process::current() == this);

    auto ip = regs.has_value() ? regs->ip() : 0;

    if (out_of_memory) {
        dbgln("\033[31;1mOut of memory\033[m, killing: {}", *this);
    } else {
        if (ip >= g_boot_info.kernel_load_base && g_kernel_symbols_available.was_set()) {
            auto const* symbol = symbolicate_kernel_address(ip);
            dbgln("\033[31;1m{:p}  {} +{}\033[0m\n", ip, (symbol ? symbol->name : "(k?)"), (symbol ? ip - symbol->address : 0));
        } else {
            dbgln("\033[31;1m{:p}  (?)\033[0m\n", ip);
        }
#if ARCH(X86_64)
        constexpr bool userspace_backtrace = false;
#elif ARCH(AARCH64)
        constexpr bool userspace_backtrace = true;
#elif ARCH(RISCV64)
        constexpr bool userspace_backtrace = true;
#else
#    error "Unknown architecture"
#endif
        if constexpr (userspace_backtrace) {
            dbgln("Userspace backtrace:");
            auto bp = regs.has_value() ? regs->bp() : 0;
            dump_backtrace_from_base_pointer(bp);
        }

        dbgln("Kernel backtrace:");
        dump_backtrace();
    }
    with_mutable_protected_data([&](auto& protected_data) {
        protected_data.termination_signal = signal;
    });
    set_should_generate_coredump(!out_of_memory);
    if constexpr (DUMP_REGIONS_ON_CRASH) {
        address_space().with([](auto& space) { space->dump_regions(); });
    }
    VERIFY(is_user_process());
    die();
    // We can not return from here, as there is nowhere
    // to unwind to, so die right away.
    Thread::current()->die_if_needed();
    VERIFY_NOT_REACHED();
}

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
bool Process::is_kcov_busy()
{
    bool is_busy = false;
    Kernel::Process::current().for_each_thread([&](auto& thread) {
        if (thread.m_kcov_enabled) {
            is_busy = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return is_busy;
}
#endif

RefPtr<Process> Process::from_pid_in_same_process_list(ProcessID pid)
{
    return Process::current().m_scoped_process_list.with([&](auto const& list_ptr) -> RefPtr<Process> {
        if (list_ptr) {
            return list_ptr->attached_processes().with([&](auto const& list) -> RefPtr<Process> {
                for (auto& process : list) {
                    if (process.pid() == pid) {
                        return process;
                    }
                }
                return {};
            });
        }
        return all_instances().with([&](auto const& list) -> RefPtr<Process> {
            for (auto& process : list) {
                if (process.pid() == pid) {
                    return process;
                }
            }
            return {};
        });
    });
}

RefPtr<Process> Process::from_pid_ignoring_process_lists(ProcessID pid)
{
    return all_instances().with([&](auto const& list) -> RefPtr<Process> {
        for (auto const& process : list) {
            if (process.pid() == pid)
                return &process;
        }
        return {};
    });
}

Process::OpenFileDescriptionAndFlags const* Process::OpenFileDescriptions::get_if_valid(size_t i) const
{
    if (m_fds_metadatas.size() <= i)
        return nullptr;

    if (auto const& metadata = m_fds_metadatas[i]; metadata.is_valid())
        return &metadata;

    return nullptr;
}
Process::OpenFileDescriptionAndFlags* Process::OpenFileDescriptions::get_if_valid(size_t i)
{
    if (m_fds_metadatas.size() <= i)
        return nullptr;

    if (auto& metadata = m_fds_metadatas[i]; metadata.is_valid())
        return &metadata;

    return nullptr;
}

Process::OpenFileDescriptionAndFlags const& Process::OpenFileDescriptions::at(size_t i) const
{
    VERIFY(m_fds_metadatas[i].is_allocated());
    return m_fds_metadatas[i];
}

Process::OpenFileDescriptionAndFlags& Process::OpenFileDescriptions::at(size_t i)
{
    VERIFY(m_fds_metadatas[i].is_allocated());
    return m_fds_metadatas[i];
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> Process::OpenFileDescriptions::open_file_description(int fd) const
{
    if (fd < 0)
        return EBADF;
    if (static_cast<size_t>(fd) >= m_fds_metadatas.size())
        return EBADF;
    RefPtr description = m_fds_metadatas[fd].description();
    if (!description)
        return EBADF;
    return description.release_nonnull();
}

void Process::OpenFileDescriptions::enumerate(Function<void(OpenFileDescriptionAndFlags const&)> callback) const
{
    for (auto const& file_description_metadata : m_fds_metadatas) {
        callback(file_description_metadata);
    }
}

ErrorOr<void> Process::OpenFileDescriptions::try_enumerate(Function<ErrorOr<void>(OpenFileDescriptionAndFlags const&)> callback) const
{
    for (auto const& file_description_metadata : m_fds_metadatas) {
        TRY(callback(file_description_metadata));
    }
    return {};
}

void Process::OpenFileDescriptions::change_each(Function<void(OpenFileDescriptionAndFlags&)> callback)
{
    for (auto& file_description_metadata : m_fds_metadatas) {
        callback(file_description_metadata);
    }
}

size_t Process::OpenFileDescriptions::open_count() const
{
    size_t count = 0;
    enumerate([&](auto& file_description_metadata) {
        if (file_description_metadata.is_valid())
            ++count;
    });
    return count;
}

ErrorOr<NonnullRefPtr<Thread>> Process::get_thread_from_thread_list(pid_t tid)
{
    if (tid < 0)
        return ESRCH;
    return m_thread_list.with([tid](auto& list) -> ErrorOr<NonnullRefPtr<Thread>> {
        for (auto& thread : list) {
            if (thread.tid() == tid)
                return thread;
        }
        return ESRCH;
    });
}

ErrorOr<Process::ScopedDescriptionAllocation> Process::OpenFileDescriptions::allocate(int first_candidate_fd)
{
    for (size_t i = first_candidate_fd; i < max_open(); ++i) {
        if (!m_fds_metadatas[i].is_allocated()) {
            m_fds_metadatas[i].allocate();
            return Process::ScopedDescriptionAllocation { static_cast<int>(i), &m_fds_metadatas[i] };
        }
    }
    return EMFILE;
}

UnixDateTime kgettimeofday()
{
    return TimeManagement::now();
}

siginfo_t Process::wait_info() const
{
    auto credentials = this->credentials();
    siginfo_t siginfo {};
    siginfo.si_signo = SIGCHLD;
    siginfo.si_pid = pid().value();
    siginfo.si_uid = credentials->uid().value();

    with_protected_data([&](auto& protected_data) {
        if (protected_data.termination_signal != 0) {
            siginfo.si_status = protected_data.termination_signal;
            siginfo.si_code = CLD_KILLED;
        } else {
            siginfo.si_status = protected_data.termination_status;
            siginfo.si_code = CLD_EXITED;
        }
    });
    return siginfo;
}

NonnullRefPtr<Custody> Process::current_directory()
{
    return m_current_directory.with([&](auto& current_directory) -> NonnullRefPtr<Custody> {
        return m_attached_vfs_root_context.with([&](auto& context) -> NonnullRefPtr<Custody> {
            return context->root_custody().with([&](auto& custody) -> NonnullRefPtr<Custody> {
                if (!current_directory)
                    current_directory = custody;
                return *current_directory;
            });
        });
    });
}

ErrorOr<NonnullOwnPtr<KString>> Process::get_syscall_path_argument(Userspace<char const*> user_path, size_t path_length)
{
    if (path_length == 0)
        return EINVAL;
    if (path_length > PATH_MAX)
        return ENAMETOOLONG;
    return try_copy_kstring_from_user(user_path, path_length);
}

ErrorOr<NonnullOwnPtr<KString>> Process::get_syscall_path_argument(Syscall::StringArgument const& path)
{
    Userspace<char const*> path_characters((FlatPtr)path.characters);
    return get_syscall_path_argument(path_characters, path.length);
}

ErrorOr<void> Process::dump_core()
{
    VERIFY(is_dumpable());
    VERIFY(should_generate_coredump());
    dbgln("Generating coredump for pid: {}", pid().value());
    auto coredump_directory_path = TRY(Coredump::directory_path().with([&](auto& coredump_directory_path) -> ErrorOr<NonnullOwnPtr<KString>> {
        if (coredump_directory_path)
            return KString::try_create(coredump_directory_path->view());
        return KString::try_create(""sv);
    }));
    if (coredump_directory_path->view() == ""sv) {
        dbgln("Generating coredump for pid {} failed because coredump directory was not set.", pid().value());
        return {};
    }
    auto coredump_path = TRY(name().with([&](auto& process_name) {
        return KString::formatted("{}/{}_{}_{}", coredump_directory_path->view(), process_name.representable_view(), pid().value(), kgettimeofday().seconds_since_epoch());
    }));
    auto coredump = TRY(Coredump::try_create(*this, coredump_path->view()));
    return coredump->write();
}

ErrorOr<void> Process::dump_perfcore()
{
    VERIFY(is_dumpable());
    VERIFY(m_perf_event_buffer);
    dbgln("Generating perfcore for pid: {}", pid().value());

    // Try to generate a filename which isn't already used.
    auto base_filename = TRY(name().with([&](auto& process_name) {
        return KString::formatted("{}_{}", process_name.representable_view(), pid().value());
    }));
    auto perfcore_filename = TRY(KString::formatted("{}.profile", base_filename));
    RefPtr<OpenFileDescription> description;
    auto credentials = this->credentials();
    for (size_t attempt = 1; attempt <= 10; ++attempt) {
        auto description_or_error = VirtualFileSystem::open(*this, vfs_root_context(), credentials, perfcore_filename->view(), O_CREAT | O_EXCL, 0400, current_directory(), UidAndGid { 0, 0 });
        if (!description_or_error.is_error()) {
            description = description_or_error.release_value();
            break;
        }
        perfcore_filename = TRY(KString::formatted("{}.{}.profile", base_filename, attempt));
    }
    if (!description) {
        dbgln("Failed to generate perfcore for pid {}: Could not generate filename for the perfcore file.", pid().value());
        return EEXIST;
    }

    auto builder = TRY(KBufferBuilder::try_create());
    TRY(m_perf_event_buffer->to_json(builder));

    auto json = builder.build();
    if (!json) {
        dbgln("Failed to generate perfcore for pid {}: Could not allocate buffer.", pid().value());
        return ENOMEM;
    }
    auto json_buffer = UserOrKernelBuffer::for_kernel_buffer(json->data());
    TRY(description->write(json_buffer, json->size()));

    dbgln("Wrote perfcore for pid {} to {}", pid().value(), perfcore_filename);
    return {};
}

void Process::finalize()
{
    if (!g_in_system_shutdown)
        VERIFY(Thread::current() == g_finalizer);

    dbgln_if(PROCESS_DEBUG, "Finalizing process {}", *this);

    if (veil_state() == VeilState::Dropped) {
        name().with([&](auto& process_name) {
            dbgln("\x1b[01;31mProcess '{}' exited with the veil left open\x1b[0m", process_name.representable_view());
        });
    }

    if (g_init_pid != 0 && pid() == g_init_pid) {
        if (g_in_system_shutdown)
            dbgln("Init process quitting for shutdown.");
        else
            PANIC("Init process quit unexpectedly. Exit code: {}", termination_status());
    }

    if (is_dumpable()) {
        if (m_should_generate_coredump) {
            auto result = dump_core();
            if (result.is_error()) {
                dmesgln("Failed to write coredump for pid {}: {}", pid(), result.error());
            }
        }
        if (m_perf_event_buffer) {
            auto result = dump_perfcore();
            if (result.is_error())
                dmesgln("Failed to write perfcore for pid {}: {}", pid(), result.error());
            TimeManagement::the().disable_profile_timer();
        }
    }

    m_threads_for_coredump.clear();

    m_alarm_timer.with([&](auto& timer) {
        if (timer)
            TimerQueue::the().cancel_timer(timer.release_nonnull());
    });
    m_fds.with_exclusive([](auto& fds) { fds.clear(); });
    with_mutable_protected_data([&](auto& protected_data) { protected_data.tty = nullptr; });
    m_executable.with([](auto& executable) { executable = nullptr; });
    m_arguments.clear();
    m_environment.clear();

    m_attached_hostname_context.with([](auto& context) {
        context->detach({});
        context = nullptr;
    });

    m_attached_vfs_root_context.with([](auto& context) {
        context->detach({});
        context = nullptr;
    });

    m_state.store(State::Dead, AK::MemoryOrder::memory_order_release);

    {
        if (auto parent_process = Process::from_pid_ignoring_process_lists(ppid())) {
            if (parent_process->is_user_process() && (parent_process->m_signal_action_data[SIGCHLD].flags & SA_NOCLDWAIT) != SA_NOCLDWAIT)
                (void)parent_process->send_signal(SIGCHLD, this);
        }
    }

    if (!!ppid()) {
        if (auto parent = Process::from_pid_ignoring_process_lists(ppid())) {
            parent->m_ticks_in_user_for_dead_children += m_ticks_in_user + m_ticks_in_user_for_dead_children;
            parent->m_ticks_in_kernel_for_dead_children += m_ticks_in_kernel + m_ticks_in_kernel_for_dead_children;
        }
    }

    unblock_waiters(Thread::WaitBlocker::UnblockFlags::Terminated);

    m_space.with([](auto& space) { space->remove_all_regions({}); });

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
    RefPtr<Process> waiter_process;
    if (auto* my_tracer = tracer())
        waiter_process = Process::from_pid_ignoring_process_lists(my_tracer->tracer_pid());
    else
        waiter_process = Process::from_pid_ignoring_process_lists(ppid());

    if (waiter_process)
        waiter_process->m_wait_blocker_set.unblock(*this, flags, signal);
}

void Process::remove_from_secondary_lists()
{
    m_scoped_process_list.with([this](auto& list_ptr) {
        if (list_ptr) {
            list_ptr->attached_processes().with([&](auto& list) {
                list.remove(*this);
            });
            list_ptr->detach({});
        }
    });
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
    with_mutable_protected_data([&](auto& protected_data) { protected_data.tty = nullptr; });

    VERIFY(m_threads_for_coredump.is_empty());
    for_each_thread([&](auto& thread) {
        auto result = m_threads_for_coredump.try_append(thread);
        if (result.is_error())
            dbgln("Failed to add thread {} to coredump due to OOM", thread.tid());
    });

    all_instances().with([&](auto const& list) {
        for (auto it = list.begin(); it != list.end();) {
            auto& process = *it;
            ++it;
            if (process.has_tracee_thread(pid())) {
                if constexpr (PROCESS_DEBUG) {
                    process.name().with([&](auto& process_name) {
                        name().with([&](auto& name) {
                            dbgln("Process {} ({}) is attached by {} ({}) which will exit", process_name.representable_view(), process.pid(), name.representable_view(), pid());
                        });
                    });
                }
                process.stop_tracing();
                auto err = process.send_signal(SIGSTOP, this);
                if (err.is_error()) {
                    process.name().with([&](auto& process_name) {
                        dbgln("Failed to send the SIGSTOP signal to {} ({})", process_name.representable_view(), process.pid());
                    });
                }
            }
        }
    });

    kill_all_threads();
}

void Process::terminate_due_to_signal(u8 signal)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(signal < NSIG);
    VERIFY(&Process::current() == this);
    dbgln("Terminating {} due to signal {}", *this, signal);
    with_mutable_protected_data([&](auto& protected_data) {
        protected_data.termination_status = 0;
        protected_data.termination_signal = signal;
    });
    die();
}

ErrorOr<void> Process::send_signal(u8 signal, Process* sender)
{
    VERIFY(is_user_process());
    // Try to send it to the "obvious" main thread:
    auto receiver_thread = Thread::from_tid_in_same_process_list(pid().value());
    // If the main thread has died, there may still be other threads:
    if (!receiver_thread) {
        // The first one should be good enough.
        // Neither kill(2) nor kill(3) specify any selection procedure.
        for_each_thread([&receiver_thread](Thread& thread) -> IterationDecision {
            receiver_thread = &thread;
            return IterationDecision::Break;
        });
    }
    if (receiver_thread) {
        receiver_thread->send_signal(signal, sender);
        return {};
    }
    return ESRCH;
}

ErrorOr<NonnullRefPtr<Thread>> Process::create_kernel_thread(void (*entry)(void*), void* entry_data, u32 priority, StringView name, u32 affinity, bool joinable)
{
    VERIFY((priority >= THREAD_PRIORITY_MIN) && (priority <= THREAD_PRIORITY_MAX));

    // FIXME: Do something with guard pages?

    auto thread = TRY(Thread::create(*this));
    thread->set_name(name);
    thread->set_affinity(affinity);
    thread->set_priority(priority);
    if (!joinable)
        thread->detach();

    thread->regs().set_entry_function((FlatPtr)entry, (FlatPtr)entry_data);

    SpinlockLocker lock(g_scheduler_lock);
    thread->set_state(Thread::State::Runnable);
    return thread;
}

void Process::OpenFileDescriptionAndFlags::clear()
{
    m_description = nullptr;
    m_flags = 0;
}

void Process::OpenFileDescriptionAndFlags::set(NonnullRefPtr<OpenFileDescription> description, u32 flags)
{
    m_description = move(description);
    m_flags = flags;
}

RefPtr<TTY> Process::tty()
{
    return with_protected_data([&](auto& protected_data) { return protected_data.tty; });
}

RefPtr<TTY const> Process::tty() const
{
    return with_protected_data([&](auto& protected_data) { return protected_data.tty; });
}

void Process::set_tty(RefPtr<TTY> new_tty)
{
    with_mutable_protected_data([&](auto& protected_data) { protected_data.tty = move(new_tty); });
}

ErrorOr<void> Process::start_tracing_from(ProcessID tracer)
{
    m_tracer = TRY(ThreadTracer::try_create(tracer));
    return {};
}

void Process::stop_tracing()
{
    m_tracer = nullptr;
}

void Process::tracer_trap(Thread& thread, RegisterState const& regs)
{
    VERIFY(m_tracer.ptr());
    m_tracer->set_regs(regs);
    thread.send_urgent_signal_to_self(SIGTRAP);
}

bool Process::create_perf_events_buffer_if_needed()
{
    if (m_perf_event_buffer)
        return true;
    m_perf_event_buffer = PerformanceEventBuffer::try_create_with_size(4 * MiB);
    if (!m_perf_event_buffer)
        return false;
    return !m_perf_event_buffer->add_process(*this, ProcessEventType::Create).is_error();
}

void Process::delete_perf_events_buffer()
{
    if (m_perf_event_buffer)
        m_perf_event_buffer = nullptr;
}

bool Process::remove_thread(Thread& thread)
{
    u32 thread_count_before = 0;
    thread_list().with([&](auto& thread_list) {
        thread_list.remove(thread);
        with_mutable_protected_data([&](auto& protected_data) {
            thread_count_before = protected_data.thread_count.fetch_sub(1, AK::MemoryOrder::memory_order_acq_rel);
            VERIFY(thread_count_before != 0);
        });
    });
    return thread_count_before == 1;
}

bool Process::add_thread(Thread& thread)
{
    bool is_first = false;
    thread_list().with([&](auto& thread_list) {
        thread_list.append(thread);
        with_mutable_protected_data([&](auto& protected_data) {
            is_first = protected_data.thread_count.fetch_add(1, AK::MemoryOrder::memory_order_relaxed) == 0;
        });
    });
    return is_first;
}

ErrorOr<void> Process::set_coredump_property(NonnullOwnPtr<KString> key, NonnullOwnPtr<KString> value)
{
    return m_coredump_properties.with([&](auto& coredump_properties) -> ErrorOr<void> {
        // Write it into the first available property slot.
        for (auto& slot : coredump_properties) {
            if (slot.key)
                continue;
            slot.key = move(key);
            slot.value = move(value);
            return {};
        }

        return ENOBUFS;
    });
}

ErrorOr<void> Process::try_set_coredump_property(StringView key, StringView value)
{
    auto key_kstring = TRY(KString::try_create(key));
    auto value_kstring = TRY(KString::try_create(value));
    return set_coredump_property(move(key_kstring), move(value_kstring));
}

static constexpr StringView to_string(Pledge promise)
{
#define __ENUMERATE_PLEDGE_PROMISE(x) \
    case Pledge::x:                   \
        return #x##sv;
    switch (promise) {
        ENUMERATE_PLEDGE_PROMISES
    }
#undef __ENUMERATE_PLEDGE_PROMISE
    VERIFY_NOT_REACHED();
}

ErrorOr<void> Process::require_no_promises() const
{
    if (!has_promises())
        return {};
    dbgln("Has made a promise");
    Thread::current()->set_promise_violation_pending(true);
    return EPROMISEVIOLATION;
}

ErrorOr<void> Process::require_promise(Pledge promise)
{
    if (!has_promises())
        return {};

    if (has_promised(promise))
        return {};

    dbgln("\033[31;1mProcess has not pledged '{}'\033[0m", to_string(promise));
    Thread::current()->set_promise_violation_pending(true);
    (void)try_set_coredump_property("pledge_violation"sv, to_string(promise));
    return EPROMISEVIOLATION;
}

NonnullRefPtr<Credentials> Process::credentials() const
{
    return with_protected_data([&](auto& protected_data) -> NonnullRefPtr<Credentials> {
        return *protected_data.credentials;
    });
}

RefPtr<Custody> Process::executable()
{
    return m_executable.with([](auto& executable) { return executable; });
}

RefPtr<Custody const> Process::executable() const
{
    return m_executable.with([](auto& executable) { return executable; });
}

ErrorOr<NonnullRefPtr<VFSRootContext>> Process::vfs_root_context_for_id(int id)
{
    if (id == -1)
        return vfs_root_context();

    // NOTE: ID 0 is reserved for the kernel VFS root context and is not
    // addressable via the vfs root contexts list anyway.
    // Because we checked for the special ID (-1), anything not above it
    // is also considered illegal.
    if (id == 0 || id < 0)
        return EINVAL;

    // NOTE: Jailed processes should not be able to specify any vfs root context
    // besides their currently attached contexts.
    // This is a security measure to prevent jailed processes from enumerating
    // the list of VFSRootContexts.
    if (is_jailed() && id != -1)
        return EPERM;

    return VFSRootContext::all_root_contexts_list(Badge<Process> {}).with([id](auto& list) -> ErrorOr<NonnullRefPtr<VFSRootContext>> {
        for (auto& context : list) {
            if (context.id() == static_cast<u64>(id))
                return context;
        }
        return Error::from_errno(EDOM);
    });
}

ErrorOr<NonnullRefPtr<VFSRootContext>> Process::acquire_vfs_root_context_for_id_and_validate_path(bool& different_vfs_root_context, int id, StringView path)
{
    // NOTE: We don't support mount operations in different VFSRootContext(s) other
    // than the Process::current VFSRootContext when the target path
    // is not absolute, as the path probably doesn't correlate to anything
    // meaningful on the other VFSRootContext.
    auto context = TRY(vfs_root_context_for_id(id));
    return m_attached_vfs_root_context.with([&different_vfs_root_context, path, context](auto& current_context) -> ErrorOr<NonnullRefPtr<VFSRootContext>> {
        VERIFY(current_context);
        different_vfs_root_context = (current_context.ptr() != context.ptr());
        if (!KLexicalPath::is_absolute(path) && different_vfs_root_context)
            return Error::from_errno(EINVAL);
        return context;
    });
}

ErrorOr<Process::MountTargetContext> Process::context_for_mount_operation(int vfs_root_context_id, StringView path)
{
    bool different_vfs_root_context = false;
    auto vfs_root_context = TRY(acquire_vfs_root_context_for_id_and_validate_path(different_vfs_root_context, vfs_root_context_id, path));
    RefPtr<Custody> target_custody;
    if (different_vfs_root_context) {
        VERIFY(KLexicalPath::is_canonical(path));
        auto vfs_root_context_custody = vfs_root_context->root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> {
            return custody;
        });
        target_custody = TRY(VirtualFileSystem::resolve_path(vfs_root_context, credentials(), path, vfs_root_context_custody));
    } else {
        target_custody = TRY(VirtualFileSystem::resolve_path(vfs_root_context, credentials(), path, current_directory()));
    }
    return MountTargetContext { *target_custody.release_nonnull(), *vfs_root_context };
}

ErrorOr<NonnullRefPtr<Custody>> Process::custody_for_dirfd(Badge<CustodyBase>, int dirfd)
{
    return custody_for_dirfd(dirfd);
}

ErrorOr<NonnullRefPtr<Custody>> Process::custody_for_dirfd(int dirfd)
{
    if (dirfd == AT_FDCWD)
        return current_directory();
    auto description = TRY(open_file_description(dirfd));
    if (!description->custody())
        return EINVAL;
    if (!description->is_directory())
        return ENOTDIR;
    return *description->custody();
}

SpinlockProtected<Process::Name, LockRank::None> const& Process::name() const
{
    return m_name;
}

void Process::set_name(StringView name)
{
    m_name.with([name](auto& process_name) {
        process_name.store_characters(name);
    });
}

}
