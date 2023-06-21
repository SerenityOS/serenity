/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/Types.h>

namespace HackStudio {

enum class BreakpointChange {
    Added,
    Removed,
};

using BreakpointChangeCallback = Function<void(DeprecatedString const& file, size_t line, BreakpointChange)>;
}
