/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$disown(ProcessID pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(proc);
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (process->ppid() != this->pid())
        return ECHILD;
    ProtectedDataMutationScope scope(*process);
    process->m_protected_values.ppid = 0;
    process->disowned_by_waiter(*this);
    return 0;
}
}
