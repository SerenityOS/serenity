/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$create_thread(void* (*entry)(void*), Userspace<Syscall::SC_create_thread_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::thread));
    auto params = TRY(copy_typed_from_user(user_params));

    unsigned detach_state = params.detach_state;
    int schedule_priority = params.schedule_priority;
    unsigned stack_size = params.stack_size;

    auto user_sp = Checked<FlatPtr>((FlatPtr)params.stack_location);
    user_sp += stack_size;
    if (user_sp.has_overflow())
        return EOVERFLOW;

    TRY(address_space().with([&](auto& space) -> ErrorOr<void> {
        if (!MM.validate_user_stack(*space, VirtualAddress(user_sp.value())))
            return EFAULT;
        return {};
    }));

    // FIXME: return EAGAIN if Thread::all_threads().size() is greater than PTHREAD_THREADS_MAX

    int requested_thread_priority = schedule_priority;
    if (requested_thread_priority < THREAD_PRIORITY_MIN || requested_thread_priority > THREAD_PRIORITY_MAX)
        return EINVAL;

    bool is_thread_joinable = (0 == detach_state);

    // FIXME: Do something with guard pages?

    auto thread = TRY(Thread::create(*this));

    // We know this thread is not the main_thread,
    // So give it a unique name until the user calls $prctl with the PR_SET_THREAD_NAME option on it
    auto new_thread_name = TRY(name().with([&](auto& process_name) {
        return KString::formatted("{} [{}]", process_name.representable_view(), thread->tid().value());
    }));
    thread->set_name(new_thread_name->view());

    if (!is_thread_joinable)
        thread->detach();

    auto& regs = thread->regs();
    regs.set_ip((FlatPtr)entry);
    regs.set_sp(user_sp.value());

#if ARCH(X86_64)
    regs.set_flags(0x0202);
    regs.cr3 = address_space().with([](auto& space) { return space->page_directory().cr3(); });

    // Set up the argument registers expected by pthread_create_helper.
    regs.rdi = (FlatPtr)params.entry;
    regs.rsi = (FlatPtr)params.entry_argument;
    regs.rdx = (FlatPtr)params.stack_location;
    regs.rcx = (FlatPtr)params.stack_size;

    thread->arch_specific_data().fs_base = bit_cast<FlatPtr>(params.tls_pointer);
#elif ARCH(AARCH64)
    regs.ttbr0_el1 = address_space().with([](auto& space) { return space->page_directory().ttbr0(); });

    // Set up the argument registers expected by pthread_create_helper.
    regs.x[0] = (FlatPtr)params.entry;
    regs.x[1] = (FlatPtr)params.entry_argument;
    regs.x[2] = (FlatPtr)params.stack_location;
    regs.x[3] = (FlatPtr)params.stack_size;

    regs.tpidr_el0 = bit_cast<FlatPtr>(params.tls_pointer);
#elif ARCH(RISCV64)
    regs.satp = address_space().with([](auto& space) { return space->page_directory().satp(); });

    // Set up the argument registers expected by pthread_create_helper.
    regs.x[9] = (FlatPtr)params.entry;
    regs.x[10] = (FlatPtr)params.entry_argument;
    regs.x[11] = (FlatPtr)params.stack_location;
    regs.x[12] = (FlatPtr)params.stack_size;

    regs.x[3] = bit_cast<FlatPtr>(params.tls_pointer);
#else
#    error Unknown architecture
#endif

    PerformanceManager::add_thread_created_event(*thread);

    SpinlockLocker lock(g_scheduler_lock);
    thread->set_priority(requested_thread_priority);
    thread->set_state(Thread::State::Runnable);
    return thread->tid().value();
}

void Process::sys$exit_thread(Userspace<void*> exit_value, Userspace<void*> stack_location, size_t stack_size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);

    auto result = require_promise(Pledge::thread);
    if (result.is_error()) {
        // Crash now, as we will never reach back to the syscall handler.
        crash(SIGABRT, {});
    }

    if (this->thread_count() == 1) {
        // If this is the last thread, instead kill the process.
        this->sys$exit(0);
    }

    auto* current_thread = Thread::current();
    current_thread->set_profiling_suppressed();
    PerformanceManager::add_thread_exit_event(*current_thread);

    if (stack_location) {
        auto unmap_result = address_space().with([&](auto& space) {
            return space->unmap_mmap_range(stack_location.vaddr(), stack_size);
        });
        if (unmap_result.is_error())
            dbgln("Failed to unmap thread stack, terminating thread anyway. Error code: {}", unmap_result.error());
    }

    current_thread->exit(reinterpret_cast<void*>(exit_value.ptr()));
    VERIFY_NOT_REACHED();
}

ErrorOr<FlatPtr> Process::sys$detach_thread(pid_t tid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::thread));
    auto thread = TRY(get_thread_from_thread_list(tid));
    if (!thread->is_joinable())
        return EINVAL;

    thread->detach();
    return 0;
}

ErrorOr<FlatPtr> Process::sys$join_thread(pid_t tid, Userspace<void**> exit_value)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::thread));

    auto thread = TRY(get_thread_from_thread_list(tid));
    auto* current_thread = Thread::current();
    if (thread == current_thread)
        return EDEADLK;

    void* joinee_exit_value = nullptr;

    // NOTE: pthread_join() cannot be interrupted by signals. Only by death.
    for (;;) {
        ErrorOr<void> try_join_result;
        auto result = current_thread->block<Thread::JoinBlocker>({}, *thread, try_join_result, joinee_exit_value);
        if (result == Thread::BlockResult::NotBlocked) {
            if (try_join_result.is_error())
                return try_join_result.release_error();
            break;
        }
        if (result == Thread::BlockResult::InterruptedByDeath)
            break;
        dbgln("join_thread: retrying");
    }

    if (exit_value)
        TRY(copy_to_user(exit_value, &joinee_exit_value));

    return 0;
}

ErrorOr<FlatPtr> Process::sys$kill_thread(pid_t tid, int signal)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::thread));

    if (signal < 0 || signal >= NSIG)
        return EINVAL;

    auto thread = TRY(get_thread_from_thread_list(tid));
    if (signal != 0)
        thread->send_signal(signal, &Process::current());

    return 0;
}

ErrorOr<FlatPtr> Process::sys$gettid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    return Thread::current()->tid().value();
}

}
