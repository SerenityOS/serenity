/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/Pledge.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$pledge(Userspace<const Syscall::SC_pledge_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto params = TRY(copy_typed_from_user(user_params));

    u32 new_promises = 0;
    if (params.mode & static_cast<u8>(PledgeMode::Promises)) {
        // The user has set invalid upper bits.
        if (params.promises & ~((1 << pledge_promise_count) - 1))
            return EINVAL;
        new_promises = params.promises & ((1 << pledge_promise_count) - 1);
        if (m_protected_values.has_promises && (new_promises & ~m_protected_values.promises))
            return EPERM;
    }

    u32 new_execpromises = 0;
    if (params.mode & static_cast<u8>(PledgeMode::ExecPromises)) {
        if (params.execpromises & ~((1 << pledge_promise_count) - 1))
            return EINVAL;
        new_execpromises = params.execpromises & ((1 << pledge_promise_count) - 1);
        if (m_protected_values.has_execpromises && (new_execpromises & ~m_protected_values.execpromises))
            return EPERM;
    }

    // Only apply promises after all validation has occurred, this ensures
    // we don't introduce logic bugs like applying the promises, and then
    // erroring out when parsing the exec promises later. Such bugs silently
    // leave the caller in an unexpected state.

    ProtectedDataMutationScope scope { *this };

    if (params.mode & static_cast<u8>(PledgeMode::Promises)) {
        m_protected_values.has_promises = true;
        m_protected_values.promises = new_promises;
    }

    if (params.mode & static_cast<u8>(PledgeMode::ExecPromises)) {
        m_protected_values.has_execpromises = true;
        m_protected_values.execpromises = new_execpromises;
    }

    return 0;
}

}
