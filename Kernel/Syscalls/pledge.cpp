/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$pledge(Userspace<const Syscall::SC_pledge_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    Syscall::SC_pledge_params params;
    TRY(copy_from_user(&params, user_params));

    if (params.promises.length > 1024 || params.execpromises.length > 1024)
        return E2BIG;

    OwnPtr<KString> promises;
    if (params.promises.characters) {
        auto promises_or_error = try_copy_kstring_from_user(params.promises);
        if (promises_or_error.is_error())
            return promises_or_error.error();
        promises = promises_or_error.release_value();
    }

    OwnPtr<KString> execpromises;
    if (params.execpromises.characters) {
        auto execpromises_or_error = try_copy_kstring_from_user(params.execpromises);
        if (execpromises_or_error.is_error())
            return execpromises_or_error.error();
        execpromises = execpromises_or_error.release_value();
    }

    auto parse_pledge = [&](auto pledge_spec, u32& mask) {
        auto parts = pledge_spec.split_view(' ');
        for (auto& part : parts) {
#define __ENUMERATE_PLEDGE_PROMISE(x)   \
    if (part == StringView { #x }) {    \
        mask |= (1u << (u32)Pledge::x); \
        continue;                       \
    }
            ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
            return false;
        }
        return true;
    };

    ProtectedDataMutationScope scope { *this };

    u32 new_promises = 0;
    if (promises) {
        if (!parse_pledge(promises->view(), new_promises))
            return EINVAL;
        if (m_protected_values.has_promises && (new_promises & ~m_protected_values.promises))
            return EPERM;
    }

    u32 new_execpromises = 0;
    if (execpromises) {
        if (!parse_pledge(execpromises->view(), new_execpromises))
            return EINVAL;
        if (m_protected_values.has_execpromises && (new_execpromises & ~m_protected_values.execpromises))
            return EPERM;
    }

    // Only apply promises after all validation has occurred, this ensures
    // we don't introduce logic bugs like applying the promises, and then
    // erroring out when parsing the exec promises later. Such bugs silently
    // leave the caller in an unexpected state.

    if (promises) {
        m_protected_values.has_promises = true;
        m_protected_values.promises = new_promises;
    }

    if (execpromises) {
        m_protected_values.has_execpromises = true;
        m_protected_values.execpromises = new_execpromises;
    }

    return 0;
}

}
