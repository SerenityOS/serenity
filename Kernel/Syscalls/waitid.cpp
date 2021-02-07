/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Debug.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<siginfo_t> Process::do_waitid(idtype_t idtype, int id, int options)
{
    switch (idtype) {
    case P_ALL:
    case P_PID:
    case P_PGID:
        break;
    default:
        return EINVAL;
    }

    KResultOr<siginfo_t> result = KResult(KSuccess);
    if (Thread::current()->block<Thread::WaitBlocker>({}, options, idtype, id, result).was_interrupted())
        return EINTR;
    ASSERT(!result.is_error() || (options & WNOHANG) || result.error() != KSuccess);
    return result;
}

pid_t Process::sys$waitid(Userspace<const Syscall::SC_waitid_params*> user_params)
{
    REQUIRE_PROMISE(proc);

    Syscall::SC_waitid_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    dbgln_if(PROCESS_DEBUG, "sys$waitid({}, {}, {}, {})", params.idtype, params.id, params.infop, params.options);

    auto siginfo_or_error = do_waitid(static_cast<idtype_t>(params.idtype), params.id, params.options);
    if (siginfo_or_error.is_error())
        return siginfo_or_error.error();

    if (!copy_to_user(params.infop, &siginfo_or_error.value()))
        return -EFAULT;
    return 0;
}

}
