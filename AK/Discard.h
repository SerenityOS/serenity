/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#ifdef maybe_discard
#    undef maybe_discard
#endif
#define maybe_discard(x) (static_cast<void>(x))

namespace AK::Detail {

ALWAYS_INLINE void discard(auto&&) { }

}

using AK::Detail::discard;
