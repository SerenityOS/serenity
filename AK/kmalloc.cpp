#include <cstdio>
#include "SimpleMalloc.h"
#include "kmalloc.h"
#include <cstdlib>

#define USE_SYSTEM_MALLOC

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

}

#else

extern "C" {

void* kcalloc(size_t nmemb, size_t size)
{
    if (!nmemb || !size)
        return nullptr;
    return SimpleMalloc::allocateZeroed(nmemb * size);
}

void* kmalloc(size_t size)
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

