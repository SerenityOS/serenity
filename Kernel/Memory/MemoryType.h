/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Kernel::Memory {

// This enum is used to control how memory accesses to mapped regions are handled by the hardware.
// NOTE: Memory types may be ignored if the architecture/platform does not support specifying memory types in page tables.
enum class MemoryType : u8 {
    // Used for normal main memory mappings
    // - Speculative accesses are allowed
    // - Accesses can be cached
    // - Accesses can be reordered
    // - Accesses can be merged
    Normal,

    // Used for framebuffers, DMA buffers etc.
    // - Speculative accesses are allowed
    // - Accesses are *not* cached
    // - Accesses can be reordered
    // - Accesses can be merged
    NonCacheable,

    // Used for MMIO (with side effects on accesses)
    // - Speculative accesses are *not* allowed
    // - Accesses are *not* cached
    // - Accesses are *not* reordered
    // - Accesses are *not* merged
    IO,
};

constexpr StringView memory_type_to_string(MemoryType memory_type)
{
    switch (memory_type) {
    case MemoryType::Normal:
        return "Normal"sv;
    case MemoryType::NonCacheable:
        return "NonCacheable"sv;
    case MemoryType::IO:
        return "IO"sv;
    }

    VERIFY_NOT_REACHED();
}

}
