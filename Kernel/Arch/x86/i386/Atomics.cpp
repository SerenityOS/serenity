/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

extern "C" {

#if defined(__GNUC__) && !defined(__clang__) // FIXME: Remove this file once GCC supports 8-byte atomics on i686

u64 kernel__atomic_compare_exchange_8(u64 volatile*, u64*, u64, int, int);
#    pragma redefine_extname kernel__atomic_compare_exchange_8 __atomic_compare_exchange_8

u64 kernel__atomic_compare_exchange_8(u64 volatile* memory, u64* expected, u64 desired, int, int)
{
    u64 previous;
    asm volatile("lock; cmpxchg8b %1"
                 : "=A"(previous), "+m"(*memory)
                 : "b"((u32)desired), "c"((u32)(desired >> 32)), "0"(*expected));
    return previous;
}

u64 kernel__atomic_load_8(u64 volatile*, int);
#    pragma redefine_extname kernel__atomic_load_8 __atomic_load_8

u64 kernel__atomic_load_8(u64 volatile* memory, int)
{
    u64 previous;
    asm volatile("movl %%ebx, %%eax\n"
                 "movl %%ecx, %%edx\n"
                 "lock; cmpxchg8b %1"
                 : "=A"(previous), "+m"(*memory));
    return previous;
}

void kernel__atomic_store_8(u64 volatile*, u64, int);
#    pragma redefine_extname kernel__atomic_store_8 __atomic_store_8

void kernel__atomic_store_8(u64 volatile* memory, u64 value, int)
{
    u64 expected = *memory;
    asm volatile("1: lock; cmpxchg8b %0\n"
                 "   jne 1b"
                 : "=m"(*memory)
                 : "b"((u32)value), "c"((u32)(value >> 32)), "A"(expected));
}

u64 kernel__atomic_fetch_add_8(u64 volatile*, u64, int);
#    pragma redefine_extname kernel__atomic_fetch_add_8 __atomic_fetch_add_8

u64 kernel__atomic_fetch_add_8(u64 volatile* memory, u64 value, int memory_order)
{
    u64 previous = *memory;
    while (kernel__atomic_compare_exchange_8(memory, &previous, previous + value, memory_order, memory_order) != previous)
        ;
    return previous;
}

#endif
}
