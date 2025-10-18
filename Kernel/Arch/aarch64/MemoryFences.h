/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

namespace Kernel {

// For an explanation of these functions, see <Kernel/Arch/MemoryFences.h>.

ALWAYS_INLINE void full_memory_fence()
{
    asm volatile("dsb sy" ::: "memory");
}

ALWAYS_INLINE void load_memory_fence()
{
    asm volatile("dsb ld" ::: "memory");
}

ALWAYS_INLINE void store_memory_fence()
{
    asm volatile("dsb st" ::: "memory");
}

}
