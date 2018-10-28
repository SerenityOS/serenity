#pragma once

#include "Compiler.h"

#ifdef SERENITY
#ifdef KERNEL
#include <Kernel/kmalloc.h>
#else
#include <LibC/stdlib.h>

extern "C" {

void* kcalloc(size_t nmemb, size_t size);
void* kmalloc(size_t size) MALLOC_ATTR;
void kfree(void* ptr);
void* krealloc(void* ptr, size_t size);

}

inline void* operator new(size_t size)
{
    return kmalloc(size);
}

inline void* operator new[](size_t size)
{
    return kmalloc(size);
}

inline void operator delete(void* ptr)
{
    return kfree(ptr);
}

inline void operator delete[](void* ptr)
{
    return kfree(ptr);
}

inline void operator delete(void* ptr, size_t)
{
    return kfree(ptr);
}

inline void operator delete[](void* ptr, size_t)
{
    return kfree(ptr);
}

inline void* operator new(size_t, void* p) { return p; }
inline void* operator new[](size_t, void* p) { return p; }
#endif
#else
#include <new>

#include "Types.h"

extern "C" {

void* kcalloc(size_t nmemb, size_t size);
void* kmalloc(size_t size) MALLOC_ATTR;
void kfree(void* ptr);
void* krealloc(void* ptr, size_t size);

}

#endif

