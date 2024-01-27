/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <Kernel/Debug.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<siginfo_t> Process::do_waitid(Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee, int options)
{
    ErrorOr<siginfo_t> result = siginfo_t {};
    if (Thread::current()->block<Thread::WaitBlocker>({}, options, move(waitee), result).was_interrupted())
        return EINTR;
    VERIFY(!result.is_error() || (options & WNOHANG));
    return result;
}

ErrorOr<FlatPtr> Process::sys$waitid(Userspace<Syscall::SC_waitid_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::proc));
    auto params = TRY(copy_typed_from_user(user_params));

    Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee;
    switch (params.idtype) {
    case P_ALL:
        break;
    case P_PID: {
        auto waitee_process = Process::from_pid_in_same_process_list(params.id);
        if (!waitee_process)
            return ECHILD;
        bool waitee_is_child = waitee_process->ppid() == Process::current().pid();
        bool waitee_is_our_tracee = waitee_process->has_tracee_thread(Process::current().pid());
        if (!waitee_is_child && !waitee_is_our_tracee)
            return ECHILD;
        waitee = waitee_process.release_nonnull();
        break;
    }
    case P_PGID: {
        auto waitee_group = ProcessGroup::from_pgid(params.id);
        if (!waitee_group) {
            return ECHILD;
        }
        waitee = waitee_group.release_nonnull();
        break;
    }
    default:
        return EINVAL;
    }

    dbgln_if(PROCESS_DEBUG, "sys$waitid({}, {}, {}, {})", params.idtype, params.id, params.infop, params.options);

    auto siginfo = TRY(do_waitid(move(waitee), params.options));
    TRY(copy_to_user(params.infop, &siginfo));
    return 0;
}

}
