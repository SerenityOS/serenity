/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/EnumBits.h>
#include <YAK/Types.h>

enum class InodeWatcherFlags : u32 {
    None = 0,
    Nonblock = 1 << 0,
    CloseOnExec = 1 << 1,
};

AK_ENUM_BITWISE_OPERATORS(InodeWatcherFlags);
