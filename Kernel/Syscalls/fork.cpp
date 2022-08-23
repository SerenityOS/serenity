/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fork(RegisterState& regs)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::proc));
    LockRefPtr<Thread> child_first_thread;

    ArmedScopeGuard thread_finalizer_guard = [&child_first_thread]() {
        SpinlockLocker lock(g_scheduler_lock);
        if (child_first_thread) {
            child_first_thread->detach();
            child_first_thread->set_state(Thread::State::Dying);
        }
    };

    auto child_name = TRY(m_name->try_clone());
    auto credentials = this->credentials();
    auto child = TRY(Process::try_create(child_first_thread, move(child_name), credentials->uid(), credentials->gid(), pid(), m_is_kernel_process, current_directory(), executable(), m_tty, this));

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    child->ref();

    TRY(m_unveil_data.with([&](auto& parent_unveil_data) -> ErrorOr<void> {
        return child->m_unveil_data.with([&](auto& child_unveil_data) -> ErrorOr<void> {
            child_unveil_data.state = parent_unveil_data.state;
            child_unveil_data.paths = TRY(parent_unveil_data.paths.deep_copy());
            return {};
        });
    }));

    TRY(child->m_fds.with_exclusive([&](auto& child_fds) {
        return m_fds.with_exclusive([&](auto& parent_fds) {
            return child_fds.try_clone(parent_fds);
        });
    }));

    child->m_pg = m_pg;

    with_protected_data([&](auto& my_protected_data) {
        child->with_mutable_protected_data([&](auto& child_protected_data) {
            child_protected_data.promises = my_protected_data.promises.load();
            child_protected_data.execpromises = my_protected_data.execpromises.load();
            child_protected_data.has_promises = my_protected_data.has_promises.load();
            child_protected_data.has_execpromises = my_protected_data.has_execpromises.load();
            child_protected_data.sid = my_protected_data.sid;
            child_protected_data.credentials = my_protected_data.credentials;
            child_protected_data.umask = my_protected_data.umask;
            child_protected_data.signal_trampoline = my_protected_data.signal_trampoline;
            child_protected_data.dumpable = my_protected_data.dumpable;
        });
    });

    dbgln_if(FORK_DEBUG, "fork: child={}", child);

    // A child created via fork(2) inherits a copy of its parent's signal mask
    child_first_thread->update_signal_mask(Thread::current()->signal_mask());

    // A child process created via fork(2) inherits a copy of its parent's alternate signal stack settings.
    child_first_thread->m_alternative_signal_stack = Thread::current()->m_alternative_signal_stack;
    child_first_thread->m_alternative_signal_stack_size = Thread::current()->m_alternative_signal_stack_size;

#if ARCH(I386)
    auto& child_regs = child_first_thread->m_regs;
    child_regs.eax = 0; // fork() returns 0 in the child :^)
    child_regs.ebx = regs.ebx;
    child_regs.ecx = regs.ecx;
    child_regs.edx = regs.edx;
    child_regs.ebp = regs.ebp;
    child_regs.esp = regs.userspace_esp;
    child_regs.esi = regs.esi;
    child_regs.edi = regs.edi;
    child_regs.eflags = regs.eflags;
    child_regs.eip = regs.eip;
    child_regs.cs = regs.cs;
    child_regs.ds = regs.ds;
    child_regs.es = regs.es;
    child_regs.fs = regs.fs;
    child_regs.gs = regs.gs;
    child_regs.ss = regs.userspace_ss;

    dbgln_if(FORK_DEBUG, "fork: child will begin executing at {:#04x}:{:p} with stack {:#04x}:{:p}, kstack {:#04x}:{:p}",
        child_regs.cs, child_regs.eip, child_regs.ss, child_regs.esp, child_regs.ss0, child_regs.esp0);
#elif ARCH(X86_64)
    auto& child_regs = child_first_thread->m_regs;
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

    Process::register_new(*child);

    PerformanceManager::add_process_created_event(*child);

    SpinlockLocker lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    auto child_pid = child->pid().value();

    return child_pid;
}
}
