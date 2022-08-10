/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Types.h>

#include <AK/Platform.h>

namespace IO {

inline u8 in8(u16)
{
    VERIFY_NOT_REACHED();
}

inline u16 in16(u16)
{
    VERIFY_NOT_REACHED();
}

inline u32 in32(u16)
{
    VERIFY_NOT_REACHED();
}

inline void out8(u16, u8)
{
    VERIFY_NOT_REACHED();
}

inline void out16(u16, u16)
{
    VERIFY_NOT_REACHED();
}

inline void out32(u16, u32)
{
    VERIFY_NOT_REACHED();
}

inline void delay(size_t)
{
    VERIFY_NOT_REACHED();
}

}
