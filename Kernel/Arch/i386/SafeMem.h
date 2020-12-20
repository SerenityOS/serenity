/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Atomic.h>
#include <AK/Optional.h>
#include <AK/Types.h>

#pragma once

namespace Kernel {

struct RegisterState;

[[nodiscard]] bool safe_memcpy(void* dest_ptr, const void* src_ptr, size_t n, void*& fault_at);
[[nodiscard]] ssize_t safe_strnlen(const char* str, size_t max_n, void*& fault_at);
[[nodiscard]] bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at);
[[nodiscard]] Optional<u32> safe_atomic_fetch_add_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> safe_atomic_exchange_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> safe_atomic_load_relaxed(volatile u32* var);
[[nodiscard]] bool safe_atomic_store_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<bool> safe_atomic_compare_exchange_relaxed(volatile u32* var, u32& expected, u32 val);

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_and_relaxed(volatile u32* var, u32 val)
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

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_and_not_relaxed(volatile u32* var, u32 val)
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

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_or_relaxed(volatile u32* var, u32 val)
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

[[nodiscard]] ALWAYS_INLINE Optional<u32> safe_atomic_fetch_xor_relaxed(volatile u32* var, u32 val)
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

bool handle_safe_access_fault(RegisterState& regs, u32 fault_address);

}
