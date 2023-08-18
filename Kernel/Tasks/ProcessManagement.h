/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/PowerStateSwitchTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

class ProcessManagement {

public:
    ProcessManagement() {};
    static void initialize();
    static ProcessManagement& the();

    void after_creating_process(Process&);
    void after_set_wait_result(Process&);

    SpinlockProtected<Process::AllProcessesList, LockRank::None>& all_instances(Badge<Process>) { return m_all_instances; }

    template<IteratorFunction<Process&> Callback>
    void for_each_ignoring_jails(Callback callback)
    {
        m_all_instances.with([&](auto const& list) {
            for (auto it = list.begin(); it != list.end();) {
                auto& process = *it;
                ++it;
                if (callback(process) == IterationDecision::Break)
                    break;
            }
        });
    }

    enum class ProcessKind {
        User,
        Kernel,
    };
    ErrorOr<void> kill_processes(ProcessKind kind);

    size_t alive_processes_count() const;

    RefPtr<Process> from_pid_ignoring_jails(ProcessID pid);
    RefPtr<Process> from_pid_in_same_jail_with_current_process(ProcessID pid);
    ErrorOr<void> for_each_child_in_same_jail_with_current_process(Function<ErrorOr<void>(Process&)> callback);
    ErrorOr<void> for_each_in_pgrp_in_same_jail_with_current_process(ProcessGroupID pgid, Function<ErrorOr<void>(Process&)> callback);
    ErrorOr<void> for_each_in_same_jail_with_current_process(Function<ErrorOr<void>(Process&)>);

    ProcessID allocate_pid_for_new_process(Badge<Process>);
    ProcessID allocate_pid_for_new_thread(Badge<Thread>);

    void attach_finalizer_process(Badge<FinalizerTask>, Process&);
    void kill_finalizer_process(Badge<PowerStateSwitchTask>);

private:
    ProcessID allocate_pid();
    mutable SpinlockProtected<Process::AllProcessesList, LockRank::None> m_all_instances;

    RefPtr<Process> m_finalizer_process;
};

}
