#pragma once

void kmalloc_init();
void* kmalloc_impl(dword size) __attribute__ ((malloc));
void* kmalloc_eternal(size_t) __attribute__ ((malloc));
void* kmalloc_page_aligned(size_t) __attribute__ ((malloc));
void* kmalloc_aligned(size_t, size_t alignment) __attribute__ ((malloc));
void kfree(void*);
void kfree_aligned(void*);

bool is_kmalloc_address(void*);

extern volatile size_t sum_alloc;
extern volatile size_t sum_free;
extern volatile size_t kmalloc_sum_eternal;
extern volatile size_t kmalloc_sum_page_aligned;

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }

ALWAYS_INLINE void* kmalloc(size_t size)
{
    // Any kernel allocation >= 32K is very suspicious, catch them.
    if (size >= 0x8000)
        asm volatile("cli;hlt");
    return kmalloc_impl(size);
}
