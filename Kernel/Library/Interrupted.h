/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

class [[nodiscard]] Interrupted {
public:
    constexpr Interrupted() = default;
    constexpr operator Error() const
    {
        return Error::from_errno(EINTR);
    }
};

// This class should be used instead of ErrorOr in code paths shared between
// kernel and user processes that cannot fail, but may be interrupted while
// running in a user process. This generally includes code that may block on
// a WaitQueue or a Mutex.
template<typename T>
using InterruptedOr = ErrorOr<T, Interrupted>;

inline void uninterruptible(InterruptedOr<void>&& maybe_error)
{
    VERIFY(Process::current().is_kernel_process());
    VERIFY(!maybe_error.is_error());
}

}
