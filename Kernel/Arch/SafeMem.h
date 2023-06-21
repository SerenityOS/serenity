/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Optional.h>
#include <AK/Types.h>

namespace Kernel {

struct RegisterState;

[[nodiscard]] bool safe_memcpy(void* dest_ptr, void const* src_ptr, size_t n, void*& fault_at) __attribute__((used));
[[nodiscard]] ssize_t safe_strnlen(char const* str, size_t max_n, void*& fault_at) __attribute__((used));
[[nodiscard]] bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at) __attribute__((used));
[[nodiscard]] Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile* var, u32 val) __attribute__((used));
[[nodiscard]] Optional<u32> safe_atomic_exchange_relaxed(u32 volatile* var, u32 val) __attribute__((used));
[[nodiscard]] Optional<u32> safe_atomic_load_relaxed(u32 volatile* var) __attribute__((used));
[[nodiscard]] bool safe_atomic_store_relaxed(u32 volatile* var, u32 val) __attribute__((used));
[[nodiscard]] Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 val) __attribute__((used));

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_and_relaxed(u32 volatile* var, u32 val)
{
    auto expected_value = safe_atomic_load_relaxed(var);
    if (!expected_value.has_value())
        return {}; // fault
    u32& expected = expected_value.value();
    for (;;) {
        auto result = safe_atomic_compare_exchange_relaxed(var, expected, expected & val);
        if (!result.has_value())
            return {}; // fault
        if (result.value())
            return expected; // exchanged

        // This is only so that we don't saturate the bus...
        AK::atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
    }
}

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_and_not_relaxed(u32 volatile* var, u32 val)
{
    auto expected_value = safe_atomic_load_relaxed(var);
    if (!expected_value.has_value())
        return {}; // fault
    u32& expected = expected_value.value();
    for (;;) {
        auto result = safe_atomic_compare_exchange_relaxed(var, expected, expected & ~val);
        if (!result.has_value())
            return {}; // fault
        if (result.value())
            return expected; // exchanged

        // This is only so that we don't saturate the bus...
        AK::atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
    }
}

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_or_relaxed(u32 volatile* var, u32 val)
{
    auto expected_value = safe_atomic_load_relaxed(var);
    if (!expected_value.has_value())
        return {}; // fault
    u32& expected = expected_value.value();
    for (;;) {
        auto result = safe_atomic_compare_exchange_relaxed(var, expected, expected | val);
        if (!result.has_value())
            return {}; // fault
        if (result.value())
            return expected; // exchanged

        // This is only so that we don't saturate the bus...
        AK::atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
    }
}

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_xor_relaxed(u32 volatile* var, u32 val)
{
    auto expected_value = safe_atomic_load_relaxed(var);
    if (!expected_value.has_value())
        return {}; // fault
    u32& expected = expected_value.value();
    for (;;) {
        auto result = safe_atomic_compare_exchange_relaxed(var, expected, expected ^ val);
        if (!result.has_value())
            return {}; // fault
        if (result.value())
            return expected; // exchanged

        // This is only so that we don't saturate the bus...
        AK::atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
    }
}

bool handle_safe_access_fault(RegisterState& regs, FlatPtr fault_address);

}
