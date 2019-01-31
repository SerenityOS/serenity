#include "kmalloc.h"

#ifndef SERENITY
#include <cstdio>
#include <cstdlib>
#endif

#if defined(SERENITY) && defined(USERLAND)
#define USE_SYSTEM_MALLOC
#endif

#define USE_SYSTEM_MALLOC

#ifndef USE_SYSTEM_MALLOC
#include "SimpleMalloc.h"
#endif

#ifdef USE_SYSTEM_MALLOC

extern "C" {

void* kcalloc(size_t nmemb, size_t size)
{
    return calloc(nmemb, size);
}

void* kmalloc(size_t size)
{
    return malloc(size);
}

void kfree(void* ptr)
{
    free(ptr);
}

void* krealloc(void* ptr, size_t size)
{
    return realloc(ptr, size);
}

void* kmalloc_eternal(size_t size)
{
    return kmalloc(size);
}

}

void* operator new(size_t size)
{
    return kmalloc(size);
}

void* operator new[](size_t size)
{
    return kmalloc(size);
}

void operator delete(void* ptr)
{
    return kfree(ptr);
}

void operator delete[](void* ptr)
{
    return kfree(ptr);
}

void operator delete(void* ptr, size_t)
{
    return kfree(ptr);
}

void operator delete[](void* ptr, size_t)
{
    return kfree(ptr);
}

#else

extern "C" {

void* kcalloc(size_t nmemb, size_t size)
{
    if (!nmemb || !size)
        return nullptr;
    return SimpleMalloc::allocate_zeroed(nmemb * size);
}

void* kmalloc(size_t size)
{
    if (!size)
        return nullptr;
    return SimpleMalloc::allocate(size);
}

void* kmalloc_eternal(size_t size)
{
    if (!size)
        return nullptr;
    return SimpleMalloc::allocate(size);
}

void kfree(void* ptr)
{
    if (!ptr)
        return;
    SimpleMalloc::free((byte*)ptr);
}

void* krealloc(void* ptr, size_t size)
{
    if (!ptr)
        return ptr;
    return SimpleMalloc::reallocate((byte*)ptr, size);
}

}

void* operator new(std::size_t size)
{
    return kmalloc(size);
}

void* operator new[](std::size_t size)
{
    return kmalloc(size);
}

void operator delete(void* ptr)
{
    return kfree(ptr);
}

void operator delete[](void* ptr)
{
    return kfree(ptr);
}

void operator delete(void* ptr, size_t)
{
    return kfree(ptr);
}

void operator delete[](void* ptr, size_t)
{
    return kfree(ptr);
}

#endif

