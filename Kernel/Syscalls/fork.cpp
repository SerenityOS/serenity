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
    child->m_veil_state = m_veil_state;
    child->m_unveiled_paths = m_unveiled_paths.deep_copy();

    if (auto result = child->m_fds.try_clone(m_fds); result.is_error())
        return result.error();

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
            auto maybe_region_clone = region->try_clone();
            if (maybe_region_clone.is_error()) {
                dbgln("fork: Cannot clone region, insufficient memory");
                // TODO: tear down new process?
                return maybe_region_clone.error();
            }

            auto* child_region = child->address_space().add_region(maybe_region_clone.release_value());
            if (!child_region) {
                dbgln("fork: Cannot add region, insufficient memory");
                // TODO: tear down new process?
                return ENOMEM;
            }
            child_region->map(child->address_space().page_directory(), Memory::ShouldFlushTLB::No);

            if (region == m_master_tls_region.unsafe_ptr())
                child->m_master_tls_region = child_region;
        }
    }

    Process::register_new(*child);

    PerformanceManager::add_process_created_event(*child);

    SpinlockLocker lock(g_scheduler_lock);
    child_first_thread->set_affinity(Thread::current()->affinity());
    child_first_thread->set_state(Thread::State::Runnable);

    auto child_pid = child->pid().value();

    // NOTE: All user processes have a leaked ref on them. It's balanced by Thread::WaitBlockCondition::finalize().
    (void)child.leak_ref();

    return child_pid;
}

}
