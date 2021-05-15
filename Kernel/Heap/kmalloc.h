/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Debug.h>

#define KMALLOC_SCRUB_BYTE 0xbb
#define KFREE_SCRUB_BYTE 0xaa

#define MAKE_ALIGNED_ALLOCATED(type, alignment)                                                            \
public:                                                                                                    \
    [[nodiscard]] void* operator new(size_t) noexcept { return kmalloc_aligned<alignment>(sizeof(type)); } \
    void operator delete(void* ptr) noexcept { kfree_aligned(ptr); }                                       \
                                                                                                           \
private:

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

[[nodiscard]] void* operator new(size_t size) noexcept;
void operator delete(void* ptr) noexcept;
void operator delete(void* ptr, size_t) noexcept;
[[nodiscard]] void* operator new[](size_t size) noexcept;
void operator delete[](void* ptrs) noexcept;
void operator delete[](void* ptr, size_t) noexcept;

[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc(size_t);

template<size_t ALIGNMENT>
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] inline void* kmalloc_aligned(size_t size)
{
    static_assert(ALIGNMENT > sizeof(ptrdiff_t));
    static_assert(ALIGNMENT <= 4096);
    void* ptr = kmalloc(size + ALIGNMENT + sizeof(ptrdiff_t));
    size_t max_addr = (size_t)ptr + ALIGNMENT;
    void* aligned_ptr = (void*)(max_addr - (max_addr % ALIGNMENT));
    ((ptrdiff_t*)aligned_ptr)[-1] = (ptrdiff_t)((u8*)aligned_ptr - (u8*)ptr);
    return aligned_ptr;
}

inline void kfree_aligned(void* ptr)
{
    kfree((u8*)ptr - ((const ptrdiff_t*)ptr)[-1]);
}

size_t kmalloc_good_size(size_t);

void kmalloc_enable_expand();
