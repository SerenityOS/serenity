#include "kmalloc.h"

#ifndef SERENITY
#include <cstdlib>
#endif

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
