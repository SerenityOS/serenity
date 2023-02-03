/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$pledge(Userspace<Syscall::SC_pledge_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    if (params.promises.length > 1024 || params.execpromises.length > 1024)
        return E2BIG;

    OwnPtr<KString> promises;
    if (params.promises.characters) {
        promises = TRY(try_copy_kstring_from_user(params.promises));
    }

    OwnPtr<KString> execpromises;
    if (params.execpromises.characters) {
        execpromises = TRY(try_copy_kstring_from_user(params.execpromises));
    }

    auto parse_pledge = [&](auto pledge_spec, u32& mask) {
        auto found_invalid_pledge = true;
        pledge_spec.for_each_split_view(' ', SplitBehavior::Nothing, [&mask, &found_invalid_pledge](auto const& part) {
#define __ENUMERATE_PLEDGE_PROMISE(x)   \
    if (part == #x##sv) {               \
        mask |= (1u << (u32)Pledge::x); \
        return;                         \
    }
            ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
            found_invalid_pledge = false;
        });
        return found_invalid_pledge;
    };

    u32 new_promises = 0;
    if (promises) {
        if (!parse_pledge(promises->view(), new_promises))
            return EINVAL;
    }

    u32 new_execpromises = 0;
    if (execpromises) {
        if (!parse_pledge(execpromises->view(), new_execpromises))
            return EINVAL;
    }

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        if (promises) {
            if (protected_data.has_promises && (new_promises & ~protected_data.promises)) {
                if (!(protected_data.promises & (1u << (u32)Pledge::no_error)))
                    return EPERM;
                new_promises &= protected_data.promises;
            }
        }

        if (execpromises) {
            if (protected_data.has_execpromises && (new_execpromises & ~protected_data.execpromises)) {
                if (!(protected_data.promises & (1u << (u32)Pledge::no_error)))
                    return EPERM;
                new_execpromises &= protected_data.execpromises;
            }
        }

        // Only apply promises after all validation has occurred, this ensures
        // we don't introduce logic bugs like applying the promises, and then
        // erroring out when parsing the exec promises later. Such bugs silently
        // leave the caller in an unexpected state.

        if (promises) {
            protected_data.has_promises = true;
            protected_data.promises = new_promises;
        }

        if (execpromises) {
            protected_data.has_execpromises = true;
            protected_data.execpromises = new_execpromises;
        }

        return 0;
    });
}

}
