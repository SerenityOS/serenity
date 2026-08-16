/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

namespace Kernel {

class [[nodiscard]] EINTRError {
public:
    constexpr EINTRError() = default;
    constexpr operator Error() const
    {
        return Error::from_errno(EINTR);
    }
};

// This class should be used instead of ErrorOr in code paths shared between
// kernel and user processes that cannot fail, but may be interrupted while
// running in a user process. This generally includes code that may block on
// a WaitQueue or Mutex.
template<typename T>
using EINTROr = ErrorOr<T, EINTRError>;

}
