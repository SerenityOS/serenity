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
    RefPtr<Thread> child_first_thread;
    auto child_name = TRY(m_name->try_clone());
    auto child = TRY(Process::try_create(child_first_thread, move(child_name), uid(), gid(), pid(), m_is_kernel_process, m_cwd, m_executable, m_tty, this));
    child->m_veil_state = m_veil_state;
    child->m_unveiled_paths = TRY(m_unveiled_paths.deep_copy());

    TRY(child->m_fds.with_exclusive([&](auto& child_fds) {
        return m_fds.with_exclusive([&](auto& parent_fds) {
            return child_fds.try_clone(parent_fds);
        });
    }));

    child->m_pg = m_pg;

    {
        ProtectedDataMutationScope scope { *child };
        child->m_protected_values.promises = m_protected_values.promises.load();
        child->m_protected_values.execpromises = m_protected_values.execpromises.load();
        child->m_protected_values.has_promises = m_protected_values.has_promises.load();
        child->m_protected_values.has_execpromises = m_protected_values.has_execpromises.load();
        child->m_protected_values.sid = m_protected_values.sid;
        child->m_protected_values.extra_gids = m_protected_values.extra_gids;
        child->m_protected_values.umask = m_protected_values.umask;
        child->m_protected_values.signal_trampoline = m_protected_values.signal_trampoline;
        child->m_protected_values.dumpable = m_protected_values.dumpable;
    }

    dbgln_if(FORK_DEBUG, "fork: child={}", child);
    child->address_space().set_enforces_syscall_regions(address_space().enforces_syscall_regions());

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
#else
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
#endif

    {
        SpinlockLocker lock(address_space().get_lock());
        for (auto& region : address_space().regions()) {
            dbgln_if(FORK_DEBUG, "fork: cloning Region({}) '{}' @ {}", region, region->name(), region->vaddr());
            auto region_clone = TRY(region->try_clone());
            TRY(region_clone->map(child->address_space().page_directory(), Memory::ShouldFlushTLB::No));
            auto* child_region = TRY(child->address_space().add_region(move(region_clone)));

            if (region == m_master_tls_region.unsafe_ptr())
                child->m_master_tls_region = TRY(child_region->try_make_weak_ptr());
        }
    }

    Process::register_new(*child);

    PerformanceManager::add_process_created_event(*child);

    SpinlockLocker lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    auto child_pid = child->pid().value();

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockerSet::finalize().
    (void)child.leak_ref();

    return child_pid;
}

}
