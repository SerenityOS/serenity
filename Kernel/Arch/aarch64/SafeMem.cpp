/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SafeMem.h>
#include <Kernel/StdLib.h>

namespace Kernel {

bool safe_memset(void*, int, size_t, void*&)
{
    VERIFY_NOT_REACHED();
    return false;
}

ssize_t safe_strnlen(char const*, unsigned long, void*&)
{
    VERIFY_NOT_REACHED();
    return 0;
}

bool safe_memcpy(void* dest_ptr, void const* src_ptr, unsigned long n, void*&)
{
    // FIXME: Actually implement a safe memcpy.
    memcpy(dest_ptr, src_ptr, n);
    return true;
}

Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile*, u32&, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

Optional<u32> safe_atomic_load_relaxed(u32 volatile*)
{
    VERIFY_NOT_REACHED();
    return {};
}

Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile*, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

Optional<u32> safe_atomic_exchange_relaxed(u32 volatile*, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

bool safe_atomic_store_relaxed(u32 volatile*, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

}
