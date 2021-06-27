/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Video::VP9 {

#define SAFE_CALL(call)             \
    do {                            \
        if (!(call)) [[unlikely]] { \
            dbgln("FAILED " #call); \
            return false;           \
        }                           \
    } while (0)

u8 clip_3(u8 x, u8 y, u8 z);
u8 round_2(u8 x, u8 n);

}
