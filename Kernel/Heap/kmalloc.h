#pragma once

#include <AK/Types.h>

//#define KMALLOC_DEBUG_LARGE_ALLOCATIONS

void kmalloc_init();
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_impl(size_t);
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_eternal(size_t);
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_page_aligned(size_t);
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* kmalloc_aligned(size_t, size_t alignment);
void* krealloc(void*, size_t);
void kfree(void*);
void kfree_aligned(void*);

bool is_kmalloc_address(const void*);

extern volatile size_t sum_alloc;
extern volatile size_t sum_free;
extern volatile size_t kmalloc_sum_eternal;
extern volatile size_t kmalloc_sum_page_aligned;
extern u32 g_kmalloc_call_count;
extern u32 g_kfree_call_count;
extern bool g_dump_kmalloc_stacks;

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }

[[gnu::always_inline]] inline void* kmalloc(size_t size)
{
#ifdef KMALLOC_DEBUG_LARGE_ALLOCATIONS
    // Any kernel allocation >= 1M is 99.9% a bug.
    if (size >= 1048576)
        asm volatile("cli;hlt");
#endif
    return kmalloc_impl(size);
}
