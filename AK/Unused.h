/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

namespace AK::Detail {

ALWAYS_INLINE void maybe_unused(auto&&) { }
ALWAYS_INLINE void unused(auto&&) { }

}

using AK::Detail::maybe_unused;
using AK::Detail::unused;
