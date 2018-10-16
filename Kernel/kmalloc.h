#pragma once

void kmalloc_init();
void *kmalloc(DWORD size) __attribute__ ((malloc));
void kfree(void*);

extern DWORD sum_alloc;
extern DWORD sum_free;

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }
