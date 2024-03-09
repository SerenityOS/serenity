/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$disown(ProcessID pid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));
    auto process = Process::from_pid_in_same_process_list(pid);
    if (!process)
        return ESRCH;
    TRY(process->with_mutable_protected_data([this](auto& protected_data) -> ErrorOr<void> {
        if (protected_data.ppid != this->pid())
            return ECHILD;
        protected_data.ppid = 0;
        return {};
    }));
    process->disowned_by_waiter(*this);
    return 0;
}
}
