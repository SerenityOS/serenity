/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SafeMem.h>
#include <Kernel/StdLib.h>

namespace Kernel {

bool safe_memset(void* dest_ptr, int c, size_t n, void*& fault_at)
{
    // FIXME: Actually implement a safe memset.
    auto* dest = static_cast<u8*>(dest_ptr);
    for (; n--;)
        *dest++ = c;
    fault_at = nullptr;
    return true;
}

ssize_t safe_strnlen(char const* str, unsigned long max_n, void*& fault_at)
{
    // FIXME: Actually implement a safe strnlen.
    size_t len = 0;
    for (; len < max_n && *str; str++)
        len++;
    fault_at = nullptr;
    return len;
}

bool safe_memcpy(void* dest_ptr, void const* src_ptr, unsigned long n, void*& fault_at)
{
    // FIXME: Actually implement a safe memcpy.
    auto* pd = static_cast<u8*>(dest_ptr);
    auto const* ps = static_cast<u8 const*>(src_ptr);
    for (; n--;)
        *pd++ = *ps++;
    fault_at = nullptr;
    return true;
}

Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 val)
{
    // FIXME: Handle access faults.
    return AK::atomic_compare_exchange_strong(var, expected, val, AK::memory_order_relaxed);
}

Optional<u32> safe_atomic_load_relaxed(u32 volatile* var)
{
    // FIXME: Handle access faults.
    return AK::atomic_load(var, AK::memory_order_relaxed);
}

Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile* var, u32 val)
{
    // FIXME: Handle access faults.
    return AK::atomic_fetch_add(var, val, AK::memory_order_relaxed);
}

Optional<u32> safe_atomic_exchange_relaxed(u32 volatile* var, u32 val)
{
    // FIXME: Handle access faults.
    return AK::atomic_exchange(var, val, AK::memory_order_relaxed);
}

bool safe_atomic_store_relaxed(u32 volatile* var, u32 val)
{
    // FIXME: Handle access faults.
    AK::atomic_store(var, val);
    return true;
}

bool handle_safe_access_fault(RegisterState&, FlatPtr)
{
    TODO_AARCH64();
    return false;
}

}
