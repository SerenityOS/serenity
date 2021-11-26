/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Utilities.h"

namespace Video::VP9 {

u8 clip_3(u8 x, u8 y, u8 z)
{
    return clamp(z, x, y);
}

u8 round_2(u8 x, u8 n)
{
    return (x + (1 << (n - 1))) >> n;
}

}
