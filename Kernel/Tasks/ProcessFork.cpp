/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Kusekushi <0kusekushi0@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/API/POSIX/unistd.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ScopedProcessList.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::do_fork_common(RegisterState& regs, FlatPtr rfork_flags)
{
    // RFCFDG and RFFDG are mutually exclusive.
    if ((rfork_flags & RFCFDG) && (rfork_flags & RFFDG))
        return EINVAL;
    // rfork without RFPROC affects the calling process rather than creating a new process — we don't support that behavior yet.
    if (!(rfork_flags & RFPROC))
        return ENOTSUP;

    auto credentials = this->credentials();
    auto child_and_first_thread = TRY(Process::create_with_forked_name(credentials->uid(), credentials->gid(), pid(), m_is_kernel_process, vfs_root_context(), hostname_context(), current_directory(), executable(), tty(), this));
    auto& child = child_and_first_thread.process;
    auto& child_first_thread = child_and_first_thread.first_thread;

    ArmedScopeGuard thread_finalizer_guard = [&child_first_thread]() {
        SpinlockLocker lock(g_scheduler_lock);
        child_first_thread->detach();
        child_first_thread->set_state(Thread::State::Dying);
    };

    TRY(m_unveil_data.with([&](auto& parent_unveil_data) -> ErrorOr<void> {
        return child->m_unveil_data.with([&](auto& child_unveil_data) -> ErrorOr<void> {
            child_unveil_data.state = parent_unveil_data.state;
            child_unveil_data.paths = TRY(parent_unveil_data.paths.deep_copy());
            return {};
        });
    }));

    TRY(m_exec_unveil_data.with([&](auto& parent_exec_unveil_data) -> ErrorOr<void> {
        return child->m_exec_unveil_data.with([&](auto& child_exec_unveil_data) -> ErrorOr<void> {
            child_exec_unveil_data.state = parent_exec_unveil_data.state;
            child_exec_unveil_data.paths = TRY(parent_exec_unveil_data.paths.deep_copy());
            return {};
        });
    }));

    if (rfork_flags & RFFDG) {
        child->m_shared_fds = nullptr;
        TRY(child->m_fds.with_exclusive([&](auto& child_fds) -> ErrorOr<void> {
            return fds().with_shared([&](auto& parent_fds) -> ErrorOr<void> {
                auto result = child_fds.try_clone(parent_fds);
                return result;
            });
        }));
    } else if (rfork_flags & RFCFDG) {
        child->m_shared_fds = nullptr;
        (void)child->m_fds.with_exclusive([](auto& child_fds) -> ErrorOr<void> {
            child_fds.clear();
            return {};
        });
    } else {
        if (m_shared_fds) {
            child->m_shared_fds = m_shared_fds;
        } else {
            auto shared = adopt_ref(*new SharedFDs());
            (void)shared->fds.with_exclusive([&](auto& target_fds) -> ErrorOr<void> {
                return m_fds.with_exclusive([&](auto& source_fds) -> ErrorOr<void> {
                    return target_fds.try_clone(source_fds);
                });
            });
            m_shared_fds = shared;
            child->m_shared_fds = shared;
            (void)shared->fds.with_shared([&](auto& _) {
                (void)_;
                return ErrorOr<void> {};
            });
        }
    }

    with_protected_data([&](auto& my_protected_data) {
        child->with_mutable_protected_data([&](auto& child_protected_data) {
            child_protected_data.promises = my_protected_data.promises;
            child_protected_data.execpromises = my_protected_data.execpromises;
            child_protected_data.has_promises = my_protected_data.has_promises;
            child_protected_data.has_execpromises = my_protected_data.has_execpromises;
            child_protected_data.credentials = my_protected_data.credentials;
            child_protected_data.umask = my_protected_data.umask;
            child_protected_data.signal_trampoline = my_protected_data.signal_trampoline;
            child_protected_data.dumpable = my_protected_data.dumpable;
            child_protected_data.process_group = my_protected_data.process_group;
            // NOTE: Propagate jailed_until_exit property to child processes.
            // The jailed_until_exec property is also propagated, but will be
            // set to false once the child process is calling the execve syscall.
            if (my_protected_data.jailed_until_exit.was_set())
                child_protected_data.jailed_until_exit.set();
            child_protected_data.jailed_until_exec = my_protected_data.jailed_until_exec;
        });
    });

    dbgln_if(FORK_DEBUG, "fork: child={}", child);

    // A child created via fork(2) inherits a copy of its parent's signal mask
    child_first_thread->update_signal_mask(Thread::current()->signal_mask());

    // A child process created via fork(2) inherits a copy of its parent's alternate signal stack settings.
    child_first_thread->m_alternative_signal_stack = Thread::current()->m_alternative_signal_stack;

    auto& child_regs = child_first_thread->m_regs;
#if ARCH(X86_64)
    child_regs.rax = 0; // fork() returns 0 in the child :^)
    child_regs.rbx = regs.rbx;
    child_regs.rcx = regs.rcx;
    child_regs.rdx = regs.rdx;
    child_regs.rbp = regs.rbp;
    child_regs.rsp = regs.userspace_rsp;
    child_regs.rsi = regs.rsi;
    child_regs.rdi = regs.rdi;
    child_regs.r8 = regs.r8;
    child_regs.r9 = regs.r9;
    child_regs.r10 = regs.r10;
    child_regs.r11 = regs.r11;
    child_regs.r12 = regs.r12;
    child_regs.r13 = regs.r13;
    child_regs.r14 = regs.r14;
    child_regs.r15 = regs.r15;
    child_regs.rflags = regs.rflags;
    child_regs.rip = regs.rip;
    child_regs.cs = regs.cs;

    dbgln_if(FORK_DEBUG, "fork: child will begin executing at {:#04x}:{:p} with stack {:p}, kstack {:p}",
        child_regs.cs, child_regs.rip, child_regs.rsp, child_regs.rsp0);
#elif ARCH(AARCH64)
    child_regs.x[0] = 0; // fork() returns 0 in the child :^)
    for (size_t i = 1; i < array_size(child_regs.x); ++i)
        child_regs.x[i] = regs.x[i];
    child_regs.spsr_el1 = regs.spsr_el1;
    child_regs.elr_el1 = regs.elr_el1;
    child_regs.sp_el0 = regs.sp_el0;
    child_regs.tpidr_el0 = regs.tpidr_el0;
#elif ARCH(RISCV64)
    for (size_t i = 0; i < array_size(child_regs.x); ++i)
        child_regs.x[i] = regs.x[i];
    child_regs.x[9] = 0; // fork() returns 0 in the child :^)
    child_regs.sstatus = regs.sstatus;
    child_regs.pc = regs.sepc;
    dbgln_if(FORK_DEBUG, "fork: child will begin executing at {:p} with stack {:p}, kstack {:p}",
        child_regs.pc, child_regs.sp(), child_regs.kernel_sp);
#else
#    error Unknown architecture
#endif

    Processor::store_fpu_state(child_first_thread->fpu_state());

    TRY(address_space().with([&](auto& parent_space) {
        return child->address_space().with([&](auto& child_space) -> ErrorOr<void> {
            if (parent_space->enforces_syscall_regions())
                child_space->set_enforces_syscall_regions();
            for (auto& region : parent_space->region_tree().regions()) {
                dbgln_if(FORK_DEBUG, "rfork: cloning Region '{}' @ {}", region.name(), region.vaddr());
                auto region_clone = TRY(region.try_clone());
                TRY(region_clone->map(child_space->page_directory(), Memory::ShouldFlushTLB::No));
                TRY(child_space->region_tree().place_specifically(*region_clone, region.range()));
                (void)region_clone.leak_ptr();
            }
            return {};
        });
    }));

    thread_finalizer_guard.disarm();

    m_scoped_process_list.with([&](auto& list_ptr) {
        if (list_ptr) {
            child->m_scoped_process_list.with([&](auto& child_list_ptr) {
                child_list_ptr = list_ptr;
            });
            list_ptr->attach(*child);
        }
    });

    Process::register_new(*child);

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    child->ref();

    if (rfork_flags & RFNOWAIT) {
        auto set_ppid_result = child->with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<void> {
            protected_data.ppid = 0;
            return {};
        });
        if (set_ppid_result.is_error()) {
            dbgln("rfork: failed to set child ppid to 0: {}", set_ppid_result.error());
            return set_ppid_result.release_error();
        }
        child->disowned_by_waiter(*this);
    }

    PerformanceManager::add_process_created_event(*child);

    SpinlockLocker lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    return child->pid().value();
}

} // namespace Kernel
