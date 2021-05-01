/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

KResultOr<pid_t> Process::sys$fork(RegisterState& regs)
{
    REQUIRE_PROMISE(proc);
    RefPtr<Thread> child_first_thread;
    auto child = adopt_ref(*new Process(child_first_thread, m_name, uid(), gid(), pid(), m_is_kernel_process, m_cwd, m_executable, m_tty, this));
    if (!child_first_thread)
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

    auto& child_tss = child_first_thread->m_tss;
    child_tss.eax = 0; // fork() returns 0 in the child :^)
    child_tss.ebx = regs.ebx;
    child_tss.ecx = regs.ecx;
    child_tss.edx = regs.edx;
    child_tss.ebp = regs.ebp;
    child_tss.esp = regs.userspace_esp;
    child_tss.esi = regs.esi;
    child_tss.edi = regs.edi;
    child_tss.eflags = regs.eflags;
    child_tss.eip = regs.eip;
    child_tss.cs = regs.cs;
    child_tss.ds = regs.ds;
    child_tss.es = regs.es;
    child_tss.fs = regs.fs;
    child_tss.gs = regs.gs;
    child_tss.ss = regs.userspace_ss;

    dbgln_if(FORK_DEBUG, "fork: child will begin executing at {:04x}:{:08x} with stack {:04x}:{:08x}, kstack {:04x}:{:08x}", child_tss.cs, child_tss.eip, child_tss.ss, child_tss.esp, child_tss.ss0, child_tss.esp0);

    {
        ScopedSpinLock lock(space().get_lock());
        for (auto& region : space().regions()) {
            dbgln_if(FORK_DEBUG, "fork: cloning Region({}) '{}' @ {}", region, region->name(), region->vaddr());
            auto region_clone = region->clone(*child);
            if (!region_clone) {
                dbgln("fork: Cannot clone region, insufficient memory");
                // TODO: tear down new process?
                return ENOMEM;
            }

            auto& child_region = child->space().add_region(region_clone.release_nonnull());
            child_region.map(child->space().page_directory(), ShouldFlushTLB::No);

            if (region == m_master_tls_region.unsafe_ptr())
                child->m_master_tls_region = child_region;
        }

        ScopedSpinLock processes_lock(g_processes_lock);
        g_processes->prepend(child);
    }

    if (g_profiling_all_threads) {
        VERIFY(g_global_perf_events);
        g_global_perf_events->add_process(*child, ProcessEventType::Create);
    }

    ScopedSpinLock lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    auto child_pid = child->pid().value();
    // We need to leak one reference so we don't destroy the Process,
    // which will be dropped by Process::reap
    (void)child.leak_ref();
    return child_pid;
}

}
