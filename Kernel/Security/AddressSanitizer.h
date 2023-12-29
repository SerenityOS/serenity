/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Types.h>

namespace Kernel::AddressSanitizer {

extern Atomic<bool> g_kasan_is_deadly;

enum class ShadowType : u8 {
    Unpoisoned8Bytes = 0,
    Unpoisoned1Byte = 1,
    Unpoisoned2Bytes = 2,
    Unpoisoned3Bytes = 3,
    Unpoisoned4Bytes = 4,
    Unpoisoned5Bytes = 5,
    Unpoisoned6Bytes = 6,
    Unpoisoned7Bytes = 7,
    StackLeft = 0xF1,
    StackMiddle = 0xF2,
    StackRight = 0xF3,
    UseAfterReturn = 0xF5,
    UseAfterScope = 0xF8,
    Generic = 0xFA,
    Malloc = 0xFB,
    Free = 0xFC,
};

void init(FlatPtr shadow_base);
void fill_shadow(FlatPtr address, size_t size, ShadowType type);
void mark_region(FlatPtr address, size_t valid_size, size_t total_size, ShadowType type);

}
