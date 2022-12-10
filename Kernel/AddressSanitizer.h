/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2022, Keegan Saunders <keegan@undefinedbehavior.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Types.h>

namespace Kernel::AddressSanitizer {

extern Atomic<bool> g_kasan_is_deadly;

enum class Poison : u8 {
    None = 0,
    Freed = 0xff
};

void poison(unsigned long address, size_t size, Poison value);

void unpoison(unsigned long address, size_t size);
inline void unpoison(unsigned long address, size_t size)
{
    poison(address, size, Poison::None);
}
}
