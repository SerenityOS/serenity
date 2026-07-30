/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK {

static inline void atomic_signal_fence(MemoryOrder order) noexcept
{
    return __atomic_signal_fence(order);
}

static inline void atomic_thread_fence(MemoryOrder order) noexcept
{
    return __atomic_thread_fence(order);
}

static inline void optimizer_fence()
{
    asm volatile("" ::: "memory");
}

}

#if USING_AK_GLOBALLY
using AK::atomic_signal_fence;
using AK::atomic_thread_fence;
using AK::optimizer_fence;
#endif
