/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Function.h>
#include <YAK/String.h>
#include <YAK/Types.h>

namespace HackStudio {

enum class BreakpointChange {
    Added,
    Removed,
};

using BreakpointChangeCallback = Function<void(const String& file, size_t line, BreakpointChange)>;
}
