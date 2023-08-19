/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/HashTable.h>
#include <AK/Singleton.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/ProcessList.h>
#include <Kernel/Tasks/ProcessManagement.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

static Singleton<ProcessManagement> s_the;
static Atomic<pid_t> next_pid;

ProcessManagement& ProcessManagement::the()
{
    VERIFY(s_the.is_initialized());
    return *s_the;
}

void ProcessManagement::initialize()
{
    VERIFY(!s_the.is_initialized());
    next_pid.store(0, AK::MemoryOrder::memory_order_release);
    s_the.ensure_instance();
}

RefPtr<Process> ProcessManagement::from_pid_in_same_jail_with_current_process(ProcessID pid)
{
    return Process::current().m_jail_process_list.with([&](auto const& list_ptr) -> RefPtr<Process> {
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
        return m_all_instances.with([&](auto const& list) -> RefPtr<Process> {
            for (auto& process : list) {
                if (process.pid() == pid) {
                    return process;
                }
            }
            return {};
        });
    });
}

RefPtr<Process> ProcessManagement::from_pid_ignoring_jails(ProcessID pid)
{
    return m_all_instances.with([&](auto const& list) -> RefPtr<Process> {
        for (auto const& process : list) {
            if (process.pid() == pid)
                return &process;
        }
        return {};
    });
}

ErrorOr<void> ProcessManagement::for_each_in_same_jail_with_current_process(Function<ErrorOr<void>(Process&)> callback)
{
    return Process::current().m_jail_process_list.with([&](auto const& list_ptr) -> ErrorOr<void> {
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
        m_all_instances.with([&](auto const& list) {
            for (auto& process : list) {
                result = callback(process);
                if (result.is_error())
                    break;
            }
        });
        return result;
    });
}

ErrorOr<void> ProcessManagement::for_each_child_in_same_jail_with_current_process(Function<ErrorOr<void>(Process&)> callback)
{
    ProcessID my_pid = Process::current().pid();
    return Process::current().m_jail_process_list.with([&](auto const& list_ptr) -> ErrorOr<void> {
        ErrorOr<void> result {};
        if (list_ptr) {
            list_ptr->attached_processes().with([&](auto const& list) {
                for (auto& process : list) {
                    if (process.ppid() == my_pid || process.has_tracee_thread(my_pid))
                        result = callback(process);
                    if (result.is_error())
                        break;
                }
            });
            return result;
        }
        m_all_instances.with([&](auto const& list) {
            for (auto& process : list) {
                if (process.ppid() == my_pid || process.has_tracee_thread(my_pid))
                    result = callback(process);
                if (result.is_error())
                    break;
            }
        });
        return result;
    });
}

size_t ProcessManagement::alive_processes_count(ProcessKind kind) const
{
    bool is_kernel_process = kind == ProcessKind::Kernel;
    size_t alive_process_count = 0;
    m_all_instances.for_each([&](Process& process) {
        if (process.pid() != Process::current().pid() && !process.is_dead() && process.is_kernel_process() == is_kernel_process)
            alive_process_count++;
    });
    return alive_process_count;
}

void ProcessManagement::attach_finalizer_process(Badge<FinalizerTask>, Process& process)
{
    m_finalizer_process = process;
}

ErrorOr<void> ProcessManagement::kill_all_user_processes(Badge<PowerStateSwitchTask>)
{
    // NOTE: The FinalizerTask should have not been terminated, because
    // it is a kernel process and we want to terminate asynchronously.
    VERIFY(m_finalizer_process);
    auto finalizer_pid = m_finalizer_process->pid();
    {
        SpinlockLocker lock(g_scheduler_lock);
        m_all_instances.for_each([&](Process& process) {
            NonnullRefPtr<Process> old_process = process;
            if (process.pid() != Process::current().pid() && process.pid() != m_finalizer_process->pid() && !process.is_kernel_process()) {
                process.die();
            }
        });
    }

    // Although we *could* finalize processes ourselves (g_in_system_shutdown allows this),
    // we're nice citizens and let the finalizer task perform final duties before we kill it.
    Scheduler::notify_finalizer();
    int alive_process_count = 1;
    MonotonicTime last_status_time = TimeManagement::the().monotonic_time();
    while (alive_process_count > 0) {
        Scheduler::yield();
        alive_process_count = 0;
        m_all_instances.for_each([&](Process& process) {
            if (process.pid() != Process::current().pid() && !process.is_dead() && process.pid() != finalizer_pid && !process.is_kernel_process())
                alive_process_count++;
        });

        if (TimeManagement::the().monotonic_time() - last_status_time > Duration::from_seconds(2)) {
            last_status_time = TimeManagement::the().monotonic_time();
            dmesgln("Waiting on {} processes to exit...", alive_process_count);

            if constexpr (PROCESS_DEBUG) {
                m_all_instances.for_each_const([&](Process const& process) {
                    if (process.pid() != Process::current().pid() && !process.is_dead() && process.pid() != finalizer_pid && !process.is_kernel_process()) {
                        dbgln("Process (user) {:2} dead={} dying={} ({})",
                            process.pid(), process.is_dead(), process.is_dying(),
                            process.name().with([](auto& name) { return name.representable_view(); }));
                    }
                });
            }
        }
    }

    return {};
}

void ProcessManagement::after_creating_process(Process& process)
{
    NonnullRefPtr<Process> const new_process = process;
    m_all_instances.with([&](auto& list) {
        list.prepend(process);
    });
}

void ProcessManagement::after_set_wait_result(Process& process)
{
    NonnullRefPtr<Process> const old_process = process;
    old_process->m_jail_process_list.with([&](auto& list_ptr) {
        if (list_ptr) {
            list_ptr->attached_processes().with([&](auto& list) {
                list.remove(process);
            });
        }
        list_ptr.clear();
    });
    m_all_instances.with([&](auto& list) {
        list.remove(process);
    });
}

ErrorOr<void> ProcessManagement::for_each_in_pgrp_in_same_jail_with_current_process(ProcessGroupID pgid, Function<ErrorOr<void>(Process&)> callback)
{
    return Process::current().m_jail_process_list.with([&](auto const& list_ptr) -> ErrorOr<void> {
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
        m_all_instances.with([&](auto const& list) {
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

ProcessID ProcessManagement::allocate_pid()
{
    // Overflow is UB, and negative PIDs wreck havoc.
    // TODO: Handle PID overflow
    // For example: Use an Atomic<u32>, mask the most significant bit,
    // retry if PID is already taken as a PID, taken as a TID,
    // takes as a PGID, taken as a SID, or zero.
    return next_pid.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
}

ProcessID ProcessManagement::allocate_pid_for_new_thread(Badge<Thread>)
{
    return allocate_pid();
}

ProcessID ProcessManagement::allocate_pid_for_new_process(Badge<Process>)
{
    return allocate_pid();
}

}
