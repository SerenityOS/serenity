/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fork(RegisterState& regs)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));

    auto credentials = this->credentials();
    auto child_and_first_thread = TRY(Process::create_with_forked_name(credentials->uid(), credentials->gid(), pid(), m_is_kernel_process, current_directory(), executable(), tty(), this));
    auto& child = child_and_first_thread.process;
    auto& child_first_thread = child_and_first_thread.first_thread;

    ArmedScopeGuard thread_finalizer_guard = [&child_first_thread]() {
        SpinlockLocker lock(g_scheduler_lock);
        child_first_thread->detach();
        child_first_thread->set_state(Thread::State::Dying);
    };

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    child->ref();

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

    // Note: We take the spinlock of Process::all_instances list because we need
    // to ensure that when we take the jail spinlock of two processes that we don't
    // run into a deadlock situation because both processes compete over each other Jail's
    // spinlock. Such pattern of taking 3 spinlocks in the same order happens in
    // Process::for_each* methods.
    TRY(Process::all_instances().with([&](auto const&) -> ErrorOr<void> {
        TRY(m_attached_jail.with([&](auto& parent_jail) -> ErrorOr<void> {
            return child->m_attached_jail.with([&](auto& child_jail) -> ErrorOr<void> {
                child_jail = parent_jail;
                if (child_jail) {
                    child_jail->attach_count().with([&](auto& attach_count) {
                        attach_count++;
                    });
                }
                return {};
            });
        }));
        return {};
    }));

    ArmedScopeGuard remove_from_jail_process_list = [&]() {
        m_jail_process_list.with([&](auto& list_ptr) {
            if (list_ptr) {
                list_ptr->attached_processes().with([&](auto& list) {
                    list.remove(*child);
                });
            }
        });
    };
    m_jail_process_list.with([&](auto& list_ptr) {
        if (list_ptr) {
            child->m_jail_process_list.with([&](auto& child_list_ptr) {
                child_list_ptr = list_ptr;
            });
            list_ptr->attached_processes().with([&](auto& list) {
                list.append(child);
            });
        }
    });

    TRY(child->m_fds.with_exclusive([&](auto& child_fds) {
        return m_fds.with_exclusive([&](auto& parent_fds) {
            return child_fds.try_clone(parent_fds);
        });
    }));

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
        });
    });

    dbgln_if(FORK_DEBUG, "fork: child={}", child);

    // A child created via fork(2) inherits a copy of its parent's signal mask
    child_first_thread->update_signal_mask(Thread::current()->signal_mask());

    // A child process created via fork(2) inherits a copy of its parent's alternate signal stack settings.
    child_first_thread->m_alternative_signal_stack = Thread::current()->m_alternative_signal_stack;
    child_first_thread->m_alternative_signal_stack_size = Thread::current()->m_alternative_signal_stack_size;

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
#else
#    error Unknown architecture
#endif

    TRY(address_space().with([&](auto& parent_space) {
        return child->address_space().with([&](auto& child_space) -> ErrorOr<void> {
            child_space->set_enforces_syscall_regions(parent_space->enforces_syscall_regions());
            for (auto& region : parent_space->region_tree().regions()) {
                dbgln_if(FORK_DEBUG, "fork: cloning Region '{}' @ {}", region.name(), region.vaddr());
                auto region_clone = TRY(region.try_clone());
                TRY(region_clone->map(child_space->page_directory(), Memory::ShouldFlushTLB::No));
                TRY(child_space->region_tree().place_specifically(*region_clone, region.range()));
                auto* child_region = region_clone.leak_ptr();

                if (&region == m_master_tls_region.unsafe_ptr())
                    child->m_master_tls_region = TRY(child_region->try_make_weak_ptr());
            }
            return {};
        });
    }));

    thread_finalizer_guard.disarm();
    remove_from_jail_process_list.disarm();

    Process::register_new(*child);

    PerformanceManager::add_process_created_event(*child);

    SpinlockLocker lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    auto child_pid = child->pid().value();

    return child_pid;
}
}
