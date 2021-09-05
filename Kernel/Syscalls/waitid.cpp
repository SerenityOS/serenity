/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<siginfo_t> Process::do_waitid(Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee, int options)
{
    KResultOr<siginfo_t> result = KResult(KSuccess);
    if (Thread::current()->block<Thread::WaitBlocker>({}, options, move(waitee), result).was_interrupted())
        return EINTR;
    VERIFY(!result.is_error() || (options & WNOHANG) || result.error() != KSuccess);
    return result;
}

KResultOr<FlatPtr> Process::sys$waitid(Userspace<const Syscall::SC_waitid_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(proc);
    auto params = TRY(copy_typed_from_user(user_params));

    Variant<Empty, NonnullRefPtr<Process>, NonnullRefPtr<ProcessGroup>> waitee = Empty {};
    switch (params.idtype) {
    case P_ALL:
        waitee = Empty {};
        break;
    case P_PID: {
        auto waitee_process = Process::from_pid(params.id);
        if (!waitee_process || waitee_process->ppid() != Process::current().pid()) {
            return ECHILD;
        }
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
    return copy_to_user(params.infop, &siginfo);
}

}
