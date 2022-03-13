/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/StdLibExtras.h>
#include <Kernel/API/Pledge.h>
#include <Kernel/API/Syscall.h>
#include <LibCore/SyscallMacros.h>
#include <LibSystem/syscall.h>

namespace Core::System {

using Kernel::Pledge;
using Kernel::PledgeMode;

template<Pledge... Promises>
class Promise {
public:
    static ErrorOr<void> pledge()
    {
        [[maybe_unused]] Promise<Promises...> promises {};
#ifdef __serenity__
        u8 mode = static_cast<u8>(PledgeMode::Promises);
        Syscall::SC_pledge_params params {
            mode,
            static_cast<u32>(promises),
            0,
        };
        int rc = syscall(SC_pledge, &params);
        HANDLE_SYSCALL_RETURN_VALUE("pledge"sv, rc, {});
#else
        return {};
#endif
    }

    // Supply some aditional execpromises.
    template<Pledge... ExecPromises>
    static ErrorOr<void> pledge_with_exec([[maybe_unused]] Promise<ExecPromises...> exec_promises = {})
    {
        [[maybe_unused]] Promise<Promises...> promises {};
#ifdef __serenity__
        u8 mode = static_cast<u8>(PledgeMode::Both);
        Syscall::SC_pledge_params params {
            mode,
            static_cast<u32>(promises),
            static_cast<u32>(exec_promises),
        };
        int rc = syscall(SC_pledge, &params);
        HANDLE_SYSCALL_RETURN_VALUE("pledge"sv, rc, {});
#else
        return {};
#endif
    }

    // Use this promise as the execpromises.
    static ErrorOr<void> pledge_as_exec()
    {
        [[maybe_unused]] Promise<Promises...> exec_promises {};
#ifdef __serenity__
        u8 mode = static_cast<u8>(PledgeMode::ExecPromises);
        Syscall::SC_pledge_params params {
            mode,
            0,
            static_cast<u32>(exec_promises),
        };
        int rc = syscall(SC_pledge, &params);
        HANDLE_SYSCALL_RETURN_VALUE("pledge"sv, rc, {});
#else
        return {};
#endif
    }

private:
    template<Pledge...>
    friend class Promise;

    consteval Promise() = default;

    consteval operator u32()
    {
        return 0 | ((1 << static_cast<u32>(Promises)) | ...);
    }
};

}
