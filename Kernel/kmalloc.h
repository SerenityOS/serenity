#pragma once

void kmalloc_init();
void *kmalloc(dword size) __attribute__ ((malloc));
void* kmalloc_eternal(size_t) __attribute__ ((malloc));
void* kmalloc_page_aligned(size_t) __attribute__ ((malloc));
void kfree(void*);

bool is_kmalloc_address(void*);

extern volatile dword sum_alloc;
extern volatile dword sum_free;
extern volatile dword kmalloc_sum_eternal;
extern volatile dword kmalloc_sum_page_aligned;

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }
