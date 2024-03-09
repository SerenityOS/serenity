/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedStringBuffer.h>
#include <AK/StringView.h>
#include <Kernel/Library/Fuse.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

static ErrorOr<u32> parse_pledge_string_to_mask(StringView pledge_spec)
{
    u32 mask = 0;
    Fuse found_invalid_pledge;
    pledge_spec.for_each_split_view(' ', SplitBehavior::Nothing, [&mask, &found_invalid_pledge](auto const& part) {
#define __ENUMERATE_PLEDGE_PROMISE(x)   \
    if (part == #x##sv) {               \
        mask |= (1u << (u32)Pledge::x); \
        return;                         \
    }
        ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
        found_invalid_pledge.set();
    });
    if (found_invalid_pledge.was_set())
        return Error::from_errno(EINVAL);
    return mask;
}

ErrorOr<FlatPtr> Process::sys$pledge_remove_capabilities(Userspace<Syscall::SC_pledge_remove_capabilities_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));
    auto removed_capabilities = TRY(get_syscall_string_fixed_buffer<all_promises_strings_length_with_spaces>(params.removed_capabilities));

    u32 pledge_mask = TRY(parse_pledge_string_to_mask(removed_capabilities.representable_view()));
    if (pledge_mask == 0)
        return Error::from_errno(EINVAL);

    TRY(with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<void> {
        // NOTE: If we don't have promises being set, then subtracting capabilities
        // is meaningless, so return EPERM for now.
        if (!protected_data.has_promises)
            return Error::from_errno(EPERM);
        auto current_promises = protected_data.promises;
        protected_data.promises = (current_promises & ~pledge_mask);
        return {};
    }));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$pledge_set_capabilities(Userspace<Syscall::SC_pledge_set_capabilities_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    FixedStringBuffer<all_promises_strings_length_with_spaces> promises {};
    bool promises_provided { false };
    FixedStringBuffer<all_promises_strings_length_with_spaces> execpromises {};
    bool execpromises_provided { false };

    if (params.promises.characters) {
        promises_provided = true;
        promises = TRY(get_syscall_string_fixed_buffer<all_promises_strings_length_with_spaces>(params.promises));
    }

    if (params.execpromises.characters) {
        execpromises_provided = true;
        execpromises = TRY(get_syscall_string_fixed_buffer<all_promises_strings_length_with_spaces>(params.execpromises));
    }

    u32 new_promises = 0;
    if (promises_provided)
        new_promises = TRY(parse_pledge_string_to_mask(promises.representable_view()));

    u32 new_execpromises = 0;
    if (execpromises_provided)
        new_execpromises = TRY(parse_pledge_string_to_mask(execpromises.representable_view()));

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        if (promises_provided) {
            if (protected_data.has_promises && (new_promises & ~protected_data.promises)) {
                if (!(protected_data.promises & (1u << (u32)Pledge::no_error)))
                    return EPERM;
                new_promises &= protected_data.promises;
            }
        }

        if (execpromises_provided) {
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

        if (promises_provided) {
            protected_data.has_promises = true;
            protected_data.promises = new_promises;
        }

        if (execpromises_provided) {
            protected_data.has_execpromises = true;
            protected_data.execpromises = new_execpromises;
        }

        return 0;
    });
}

}
