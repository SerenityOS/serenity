/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Types.h>

//#define KMALLOC_DEBUG_LARGE_ALLOCATIONS

#define KMALLOC_SCRUB_BYTE 0xbb
#define KFREE_SCRUB_BYTE 0xaa

void kmalloc_init();
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_impl(size_t);
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_eternal(size_t);

void* krealloc(void*, size_t);
void kfree(void*);

struct kmalloc_stats {
    size_t bytes_allocated;
    size_t bytes_free;
    size_t bytes_eternal;
    size_t kmalloc_call_count;
    size_t kfree_call_count;
};
void get_kmalloc_stats(kmalloc_stats&);

extern bool g_dump_kmalloc_stacks;

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }

[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] ALWAYS_INLINE void* kmalloc(size_t size)
{
#ifdef KMALLOC_DEBUG_LARGE_ALLOCATIONS
    // Any kernel allocation >= 1M is 99.9% a bug.
    if (size >= 1048576)
        asm volatile("cli;hlt");
#endif
    return kmalloc_impl(size);
}

template<size_t ALIGNMENT>
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] inline void* kmalloc_aligned(size_t size)
{
    static_assert(ALIGNMENT > 1);
    static_assert(ALIGNMENT < 255);
    void* ptr = kmalloc(size + ALIGNMENT + sizeof(u8));
    size_t max_addr = (size_t)ptr + ALIGNMENT;
    void* aligned_ptr = (void*)(max_addr - (max_addr % ALIGNMENT));
    ((u8*)aligned_ptr)[-1] = (u8)((u8*)aligned_ptr - (u8*)ptr);
    return aligned_ptr;
}

inline void kfree_aligned(void* ptr)
{
    kfree((u8*)ptr - ((u8*)ptr)[-1]);
}

void kmalloc_enable_expand();

extern u8* const kmalloc_start;
extern u8* const kmalloc_end;
