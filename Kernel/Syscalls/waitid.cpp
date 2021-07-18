/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<siginfo_t> Process::do_waitid(idtype_t idtype, int id, int options)
{
    KResultOr<siginfo_t> result = KResult(KSuccess);
    if (Thread::current()->block<Thread::WaitBlocker>({}, options, idtype, id, result).was_interrupted())
        return EINTR;
    VERIFY(!result.is_error() || (options & WNOHANG) || result.error() != KSuccess);
    return result;
}

KResultOr<FlatPtr> Process::sys$waitid(Userspace<const Syscall::SC_waitid_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(proc);

    Syscall::SC_waitid_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    switch (params.idtype) {
    case P_ALL:
    case P_PID:
    case P_PGID:
        break;
    default:
        return EINVAL;
    }

    dbgln_if(PROCESS_DEBUG, "sys$waitid({}, {}, {}, {})", params.idtype, params.id, params.infop, params.options);

    auto siginfo_or_error = do_waitid(static_cast<idtype_t>(params.idtype), params.id, params.options);
    if (siginfo_or_error.is_error())
        return siginfo_or_error.error();

    if (!copy_to_user(params.infop, &siginfo_or_error.value()))
        return EFAULT;
    return 0;
}

}
