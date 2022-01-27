/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Platform.h>

#ifdef maybe_discard
#    undef maybe_discard
#endif
#define maybe_discard(x) (static_cast<void>(x))

namespace AK::Detail {

void __discarded_non_discardable();

ALWAYS_INLINE void discard(auto&&) { }

void discard(Error&&) = delete;

template<typename T>
void discard(ErrorOr<T>&& error_or)
{
    if (error_or.is_error()) {
        dbgln("Discarded error: {}", error_or.error());
        __discarded_non_discardable();
    }
}

}

using AK::Detail::discard;
