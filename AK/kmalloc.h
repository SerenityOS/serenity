#pragma once

#include "Compiler.h"

#if defined(SERENITY) && defined(KERNEL)
#define AK_MAKE_ETERNAL \
public: \
    void* operator new(size_t size) { return kmalloc_eternal(size); } \
private:
#else
#define AK_MAKE_ETERNAL
#endif

#ifdef KERNEL
#include <Kernel/kmalloc.h>
#else
#include <LibC/stdlib.h>

extern "C" {

void* kcalloc(size_t nmemb, size_t size);
void* kmalloc(size_t size) MALLOC_ATTR;
void kfree(void* ptr);
void* krealloc(void* ptr, size_t size);
void* kmalloc_eternal(size_t) MALLOC_ATTR;

}

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }
#endif
