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
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    if (params.promises.length > 1024 || params.execpromises.length > 1024)
        return E2BIG;

    String promises;
    if (params.promises.characters) {
        promises = copy_string_from_user(params.promises);
        if (promises.is_null())
            return EFAULT;
    }

    String execpromises;
    if (params.execpromises.characters) {
        execpromises = copy_string_from_user(params.execpromises);
        if (execpromises.is_null())
            return EFAULT;
    }

    auto parse_pledge = [&](auto& pledge_spec, u32& mask) {
        auto parts = pledge_spec.split_view(' ');
        for (auto& part : parts) {
#define __ENUMERATE_PLEDGE_PROMISE(x)   \
    if (part == #x) {                   \
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

    if (!promises.is_null()) {
        u32 new_promises = 0;
        if (!parse_pledge(promises, new_promises))
            return EINVAL;
        if (m_has_promises && (new_promises & ~m_promises))
            return EPERM;

        m_has_promises = true;
        m_promises = new_promises;
    }

    if (!execpromises.is_null()) {
        u32 new_execpromises = 0;
        if (!parse_pledge(execpromises, new_execpromises))
            return EINVAL;
        if (m_has_execpromises && (new_execpromises & ~m_execpromises))
            return EPERM;
        m_has_execpromises = true;
        m_execpromises = new_execpromises;
    }

    return 0;
}

}
