/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$fork(RegisterState& regs)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(proc);
    RefPtr<Thread> child_first_thread;
    auto child = Process::create(child_first_thread, m_name, uid(), gid(), pid(), m_is_kernel_process, m_cwd, m_executable, m_tty, this);
    if (!child || !child_first_thread)
        return ENOMEM;
    child->m_root_directory = m_root_directory;
    child->m_root_directory_relative_to_global_root = m_root_directory_relative_to_global_root;
    child->m_veil_state = m_veil_state;
    child->m_unveiled_paths = m_unveiled_paths.deep_copy();
    child->m_fds = m_fds;
    child->m_pg = m_pg;

    {
        ProtectedDataMutationScope scope { *child };
        child->m_promises = m_promises;
        child->m_execpromises = m_execpromises;
        child->m_has_promises = m_has_promises;
        child->m_has_execpromises = m_has_execpromises;
        child->m_sid = m_sid;
        child->m_extra_gids = m_extra_gids;
        child->m_umask = m_umask;
        child->m_signal_trampoline = m_signal_trampoline;
        child->m_dumpable = m_dumpable;
    }

    dbgln_if(FORK_DEBUG, "fork: child={}", child);
    child->space().set_enforces_syscall_regions(space().enforces_syscall_regions());

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
        ScopedSpinLock lock(space().get_lock());
        for (auto& region : space().regions()) {
            dbgln_if(FORK_DEBUG, "fork: cloning Region({}) '{}' @ {}", region, region->name(), region->vaddr());
            auto region_clone = region->clone();
            if (!region_clone) {
                dbgln("fork: Cannot clone region, insufficient memory");
                // TODO: tear down new process?
                return ENOMEM;
            }

            auto* child_region = child->space().add_region(region_clone.release_nonnull());
            if (!child_region) {
                dbgln("fork: Cannot add region, insufficient memory");
                // TODO: tear down new process?
                return ENOMEM;
            }
            child_region->map(child->space().page_directory(), ShouldFlushTLB::No);

            if (region == m_master_tls_region.unsafe_ptr())
                child->m_master_tls_region = child_region;
        }
    }

    Process::register_new(*child);

    PerformanceManager::add_process_created_event(*child);

    ScopedSpinLock lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    auto child_pid = child->pid().value();

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockCondition::finalize().
    (void)child.leak_ref();

    return child_pid;
}

}
